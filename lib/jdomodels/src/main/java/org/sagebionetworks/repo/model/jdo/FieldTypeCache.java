package org.sagebionetworks.repo.model.jdo;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.schema.FORMAT;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.server.ServerSideOnlyFactory;

/**
 * Singleton cache of column information for entity fields name and annotation
 * names. This is used by the query pre-processor to determine how a query will
 * be written.
 * 
 * @author John
 * 
 */
public class FieldTypeCache {

	// match one or more whitespace characters
	private static final Pattern ALLOWABLE_CHARS = Pattern
			.compile("^[a-z,A-Z,0-9,_,.]+");

	/**
	 * Since the types never change once they are set, we can safely cache the
	 * results.
	 */
	private Map<String, FieldType> localCache = Collections
			.synchronizedMap(new HashMap<String, FieldType>());

	/**
	 * This instance can be treated as a singleton. Create a new field type
	 * cache.
	 * 
	 * @throws JSONObjectAdapterException
	 */
	private FieldTypeCache() {
		// Make sure the primary Node fields are in place
		Field[] fields = Node.class.getDeclaredFields();
		for (Field field : fields) {
			// Add the primary fields from the node class
			localCache.put(field.getName(), FieldType.PRIMARY_FIELD);
		}
		// Map all of the Entity field names to the schema type.
		ServerSideOnlyFactory factory = new ServerSideOnlyFactory();
		try {
			addEntityTypeNamesToCache(factory, localCache);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Singleton holder will not be initialized until called.
	 * 
	 */
	private static class SingletonHolder {
		private static final FieldTypeCache cache = new FieldTypeCache();
	}

	/**
	 * Get the singleton instance of this cache.
	 * 
	 * @return
	 */
	public static FieldTypeCache getInstance() {
		return SingletonHolder.cache;
	}

	/**
	 * Get the field type for a name
	 * 
	 * @param name
	 * @return
	 */
	public FieldType getTypeForName(String name) {
		FieldType type = localCache.get(name);
		if (type == null) {
			type = FieldType.STRING_ATTRIBUTE;
		}
		return type;
	}

	/**
	 * Validate the name
	 * 
	 * @param key
	 * @throws InvalidModelException
	 */
	public static String checkKeyName(String key) throws InvalidModelException {
		if (key == null)
			throw new InvalidModelException("Annotation names cannot be null");
		key = key.trim();
		if ("".equals(key))
			throw new InvalidModelException(
					"Annotation names cannot be empty strings");
		Matcher matcher = ALLOWABLE_CHARS.matcher(key);
		if (!matcher.matches()) {
			throw new InvalidModelException(
					"Invalid annotation name: '"
							+ key
							+ "'. Annotation names may only contain; letters, numbers, '_' and '.'");
		}
		return key;
	}

	/**
	 * Walk over all of the entity types and map the field type to column types.
	 * 
	 * @param localCache
	 * @throws JSONObjectAdapterException
	 */
	protected static void addEntityTypeNamesToCache(ServerSideOnlyFactory factory,
			Map<String, FieldType> localCache)
			throws JSONObjectAdapterException {
		Iterator<String> it = factory.getKeySetIterator();
		while (it.hasNext()) {
			JSONEntity jsonEntity = factory.newInstance(it.next());
			if (jsonEntity instanceof Entity) {
				// Load the schema
				ObjectSchema schema = EntityFactory.createEntityFromJSONString(
						jsonEntity.getJSONSchema(), ObjectSchema.class);
				// Iterate over the properties.
				Map<String, ObjectSchema> propMap = schema.getProperties();
				for (String key : schema.getProperties().keySet()) {
					ObjectSchema propSchema = propMap.get(key);
					// Lookup the field type for the property type.
					FieldType fieldType = getFieldTypeForProperty(key,
							schema.getId(), propSchema);
					validateAndAddToCache(key, fieldType, localCache);
				}
			}
		}
	}

	/**
	 * Validate a type and add it to the cache.
	 * 
	 * @param key
	 * @param fieldType
	 * @param localCache
	 */
	protected static void validateAndAddToCache(String key,
			FieldType fieldType, Map<String, FieldType> localCache) {
		FieldType currentType = localCache.get(key);
		// If this is already a primary fields then do not change it.
		if (currentType == FieldType.PRIMARY_FIELD) {
			// skip all primary fields
			// they must remain primary fields
			return;
		}
		// Make sure we do not have a conflict
		if (currentType != null && currentType != fieldType) {
			// we currently do not allow conflicting type for entity names.
			throw new IllegalArgumentException(
					"Conflicting name types.  Name: " + key
							+ " was mapped to: " + fieldType.name() + " and: "
							+ currentType.name());
		}
		// Add it to the cache.
		localCache.put(key, fieldType);
	}

	/**
	 * Maps an object schema type to a field type.
	 * 
	 * @param key
	 * @param id
	 * @param propSchema
	 * @return
	 */
	protected static FieldType getFieldTypeForProperty(String key, String id,
			ObjectSchema propSchema) {
		if (TYPE.INTEGER == propSchema.getType()) {
			if (FORMAT.UTC_MILLISEC == propSchema.getFormat()) {
				return FieldType.DATE_ATTRIBUTE;
			} else {
				return FieldType.LONG_ATTRIBUTE;
			}
		} else if (TYPE.STRING == propSchema.getType()) {
			if (FORMAT.DATE_TIME == propSchema.getFormat()) {
				// Some strings are actually dates.
				return FieldType.DATE_ATTRIBUTE;
			} else {
				return FieldType.STRING_ATTRIBUTE;
			}
		} else if (TYPE.NUMBER == propSchema.getType()) {
			// Object fields will not be index
			return FieldType.DOUBLE_ATTRIBUTE;
		} else if (TYPE.BOOLEAN == propSchema.getType()) {
			// Object fields will not be index
			return FieldType.STRING_ATTRIBUTE;
		} else if (TYPE.OBJECT == propSchema.getType()) {
			// Object fields will not be index
			return FieldType.DOES_NOT_EXIST;
		} else if (TYPE.ARRAY == propSchema.getType()) {
			ObjectSchema arrayType = propSchema.getItems();
			if (arrayType == null)
				throw new IllegalArgumentException(
						"Items was null for an ARRAY on: " + id);
			return getFieldTypeForProperty(key, id, arrayType);
		} else {
			throw new IllegalArgumentException("Unknown type for: " + key
					+ " on Schema: " + id + " type: " + propSchema.getType());
		}
	}

	/**
	 * Check all of the annotaion names.
	 * @param updated
	 * @throws InvalidModelException
	 */
	public static void validateAnnotations(Annotations updated)	throws InvalidModelException {
		if (updated == null)
			throw new IllegalArgumentException("Annotations cannot be null");
		// Validate the annotation names

		// Validate the strings
		if (updated.getStringAnnotations() != null) {
			Iterator<String> it = updated.getStringAnnotations().keySet().iterator();
			while (it.hasNext()) {
				checkKeyName(it.next());
			}
		}
		// Validate the longs
		if (updated.getLongAnnotations() != null) {
			Iterator<String> it = updated.getLongAnnotations().keySet()
					.iterator();
			while (it.hasNext()) {
				checkKeyName(it.next());
			}
		}
		// Validate the dates
		if (updated.getDateAnnotations() != null) {
			Iterator<String> it = updated.getDateAnnotations().keySet()
					.iterator();
			while (it.hasNext()) {
				checkKeyName(it.next());
			}
		}
		// Validate the Doubles
		if (updated.getDoubleAnnotations() != null) {
			Iterator<String> it = updated.getDoubleAnnotations().keySet()
					.iterator();
			while (it.hasNext()) {
				checkKeyName(it.next());
			}
		}
		// Validate the Doubles
		if (updated.getBlobAnnotations() != null) {
			Iterator<String> it = updated.getBlobAnnotations().keySet()
					.iterator();
			while (it.hasNext()) {
				checkKeyName(it.next());
			}
		}

	}

}
