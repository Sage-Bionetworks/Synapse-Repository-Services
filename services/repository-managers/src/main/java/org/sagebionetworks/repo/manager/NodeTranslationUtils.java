package org.sagebionetworks.repo.manager;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONArrayAdapterImpl;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * Converts to/from datasets and nodes.
 * 
 * @author jmhill
 * 
 */

public class NodeTranslationUtils {

	private static final Logger log = Logger
			.getLogger(NodeTranslationUtils.class.getName());

	/**
	 * Keep track of the known fields of a node.
	 */
	private static Map<String, Field> nodeFieldNames = new HashMap<String, Field>();
	private static Map<String, String> nameConvertion = new HashMap<String, String>();
	private static Map<Class<? extends Entity>, Field[]> translatableEntityFieldsCache = new HashMap<>();
	// fields in Node that should be ignored while translating between node and entity
	public static Set<String> ignoredNodeFields;

	static {
		Set<String> temp = new HashSet<>();
		temp.add(ObjectSchema.EXTRA_FIELDS);
		ignoredNodeFields = Collections.unmodifiableSet(temp);
	}

	// fields in Entity that should be ignored while translating between node and entity
	public static Set<String> ignoredEntityFields;

	static {
		Set<String> temp = new HashSet<>();
		temp.add(ObjectSchema.CONCRETE_TYPE);
		ignoredEntityFields = Collections.unmodifiableSet(temp);
	}

	static {
		// Populate the nodeFieldNames
		Field[] fields = Node.class.getDeclaredFields();
		for (Field field : fields) {
			if (ignoredNodeFields.contains(field.getName())) {
				continue;
			}
			// make sure all are
			nodeFieldNames.put(field.getName(), field);
		}
		// Add the name required name conversions
		nameConvertion.put("creator", "createdBy");
		nameConvertion.put("creationDate", "createdOn");
		nameConvertion.put("etag", "eTag");
		nameConvertion.put("dataFileHandleId", "fileHandleId");
		nameConvertion.put("columnIds", "columnModelIds");
		nameConvertion.put("linksTo", "reference");
	}

	static <T extends Entity> Field[] getTranslatableEntityFields(T base){
		Class<? extends Entity> entityClass =  base.getClass();
		Field[] cachedFields = translatableEntityFieldsCache.get(entityClass);
		if(cachedFields != null){
			return cachedFields;
		}

		Field[] filteredFields = Arrays.stream(entityClass.getDeclaredFields())
				.filter( field -> !ignoredEntityFields.contains(field.getName()) )
				.toArray(Field[]::new);

		translatableEntityFieldsCache.put(entityClass, filteredFields);
		return filteredFields;
	}

	/**
	 * Create a new node from the passed base object.
	 * 
	 * @param dataset
	 * @return
	 */
	public static <T extends Entity> Node createFromEntity(T base) {
		if (base == null)
			throw new IllegalArgumentException("Base Object cannot be null");
		Node node = new Node();
		updateNodeFromObject(base, node);
		return node;
	}

	/**
	 * Use the passed object to update a node.
	 * 
	 * @param <T>
	 * @param base
	 * @param node
	 */
	public static <T extends Entity> void updateNodeFromObject(T base, Node node) {
		// First get the schema for this Entity
		Field[] fields = getTranslatableEntityFields(base);
		for (Field field : fields) {
			String name = field.getName();
			String nodeName = nameConvertion.get(name);
			if (nodeName == null) {
				nodeName = name;
			}
			// Only include fields that are in node.
			Field nodeField = nodeFieldNames.get(nodeName);
			if (nodeField != null) {
				// Make sure we can call it.
				field.setAccessible(true);
				nodeField.setAccessible(true);
				Object value;
				try {
					// only set non-static fields
					if((field.getModifiers() & Modifier.STATIC) == 0) {
						value = field.get(base);
						nodeField.set(node, value);
					}
				} catch (IllegalAccessException e) {
					// This should never occur
					log.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Add any fields from the object that are not on a node.
	 * 
	 * @param <T>
	 * @param base
	 * @param annos
	 * @return the reference or null if the Object does not contain a reference
	 * @throws IllegalArgumentException
	 */
	public static <T extends Entity> void updateNodeSecondaryFieldsFromObject(
			T base, Annotations annos) {
		if (base == null)
			throw new IllegalArgumentException("Base cannot be null");
		if (annos == null)
			throw new IllegalArgumentException("Annotations cannot be null");
		// Find the fields that are not on nodes.
		ObjectSchema schema = SchemaCache.getSchema(base);
		Map<String, ObjectSchema> schemaProperties = schema.getProperties();
		if (schemaProperties == null) {
			schemaProperties = new HashMap<String, ObjectSchema>();
		}
		Field[] entityFields = getTranslatableEntityFields(base);
		for (Field field : entityFields) {
			String name = field.getName();
			String nodeName = nameConvertion.get(name);
			if (nodeName == null) {
				nodeName = name;
			}
			// Is this a field already on Node?
			if (!nodeFieldNames.containsKey(nodeName)) {
				// Make sure we can call it.
				field.setAccessible(true);
				Object value;
				try {
					value = field.get(base);
					// Skip any property not defined in the schema
					ObjectSchema propSchema = schemaProperties.get(name);
					if (propSchema == null) {
						continue;
					}
					// If this is an enum then store the string
					if (propSchema.getEnum() != null) {
						value = NodeTranslationUtils.getNameFromEnum(value);
					}

					// skip any transient property as they are not stored.
					if (propSchema.isTransient())
						continue;
					// We do not store fields that are marked as @TransientField
					// The schema type will tell us how to store this
					if (value == null) {
						annos.deleteAnnotation(name);
					} else {
						if (propSchema.getContentEncoding() != null	|| value instanceof JSONEntity) {
							// This will be stored a a blob
							byte[] blob = objectToBytes(value, propSchema);
							annos.replaceAnnotation(name, blob);
						} else {
							annos.replaceAnnotation(name, value);
						}
					}
				} catch (IllegalAccessException e) {
					// This should never occur
					log.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Get the name() from the passed enumeration.
	 * 
	 * @param value
	 * @param schema
	 * @return
	 */
	public static String getNameFromEnum(Object value) {
		if (value == null)
			return null;
		try {
			Method method = value.getClass().getMethod("name");
			return (String) method.invoke(value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Calls the enum.valueOf(String) to get an instance of an enumeration.
	 * 
	 * @param value
	 *            The string value of the enumeration. See enum.name();
	 * @param enumClass
	 *            The class of the enumeration.
	 * @return
	 */
	public static Object getValueOfFromEnum(String value, Class<?> enumClass) {
		if (value == null)
			throw new IllegalArgumentException("Value cannot be null");
		if (enumClass == null)
			throw new IllegalArgumentException("Class cannot be null");
		try {
			Method method = enumClass.getMethod("valueOf", String.class);
			return method.invoke(null, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Convert an object to bytes.
	 * 
	 * @param encoding
	 * @param annos
	 * @param value
	 */
	public static byte[] objectToBytes(Object value, ObjectSchema schema) {
		if (value == null)
			throw new IllegalArgumentException("Value cannot be null");
		if (schema == null)
			throw new IllegalArgumentException("Schema cannot be null");
		// Is this a string
		if (TYPE.STRING == schema.getType()) {
			// String are stored as UTF-8 bytes
			try {
				// String are stored as UTF-8 bytes
				return ((String) value).getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		} else if (TYPE.OBJECT == schema.getType()) {
			// Extract the JSON string from this entity
			JSONEntity valueEntity = (JSONEntity) value;
			// Get the JSONString
			try {
				String jsonString = EntityFactory
						.createJSONStringForEntity(valueEntity);
				// Save the UTF-8 bytes of this string
				return jsonString.getBytes("UTF-8");
			} catch (JSONObjectAdapterException e) {
				throw new RuntimeException(e);
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		} else if (TYPE.ARRAY == schema.getType()) {
			// Extract the JSON string from this entity
			Collection<JSONEntity> valueList = (Collection<JSONEntity>) value;
			// Get the JSONString
			try {
				// Build up an array using the object.
				JSONArrayAdapterImpl adapterArray = new JSONArrayAdapterImpl();
				int index = 0;
				for (JSONEntity entity : valueList) {
					adapterArray.put(index, entity
							.writeToJSONObject(new JSONObjectAdapterImpl()));
					index++;
				}
				String jsonString = adapterArray.toJSONString();
				// Save the UTF-8 bytes of this string
				return jsonString.getBytes("UTF-8");
			} catch (JSONObjectAdapterException e) {
				throw new RuntimeException(e);
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		} else {
			// Unknown binary type
			throw new IllegalArgumentException(
					"Unknown schema type: "
							+ schema.getType()
							+ ". Can only convert Strings, JSONEntity objects to byte[]");
		}
	}

	/**
	 * Convert bytes to an object.
	 * 
	 * @param bytes
	 * @param clazz
	 * @return
	 */
	public static Object bytesToObject(byte[] bytes, ObjectSchema schema) {
		if (bytes == null)
			throw new IllegalArgumentException("Bytes cannot be null");
		if (schema == null)
			throw new IllegalArgumentException("Schema cannot be null");
		// Is this a string
		if (TYPE.STRING == schema.getType()) {
			try {
				return new String(bytes, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		} else if (TYPE.OBJECT == schema.getType()) {
			if (schema.getId() == null)
				throw new IllegalArgumentException(
						"The schema does not have an ID so we cannot lookup the class");
			String json;
			try {
				json = new String(bytes, "UTF-8");
				return EntityFactory.createEntityFromJSONString(json,
						(Class<? extends JSONEntity>) Class.forName(schema
								.getId()));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			} catch (JSONObjectAdapterException e) {
				throw new RuntimeException(e);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		} else if (TYPE.ARRAY == schema.getType()) {
			if (schema.getItems() == null)
				throw new IllegalArgumentException(
						"The schema items cannot be null for type array");
			if (schema.getItems().getId() == null)
				throw new IllegalArgumentException(
						"The schema items.getId() cannot be null for type array");
			String json;
			try {
				json = new String(bytes, "UTF-8");
				JSONArrayAdapterImpl adapterArray = new JSONArrayAdapterImpl(
						json);
				Collection<JSONEntity> collection = null;
				if (schema.getUniqueItems()) {
					collection = new HashSet<JSONEntity>();
				} else {
					collection = new ArrayList<JSONEntity>();
				}
				Class<? extends JSONEntity> clazz = (Class<? extends JSONEntity>) Class
						.forName(schema.getItems().getId());
				for (int index = 0; index < adapterArray.length(); index++) {
					JSONObjectAdapter adapter = adapterArray
							.getJSONObject(index);
					JSONEntity newInstance = clazz.newInstance();
					newInstance.initializeFromJSONObject(adapter);
					collection.add(newInstance);
				}
				return collection;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			// Unknown binary type
			throw new IllegalArgumentException(
					"Unknown schema type: "
							+ schema.getType()
							+ ". Can only convert Strings, JSONEntity objects to byte[]");
		}
	}

	/**
	 * Update an object using the a node
	 * 
	 * @param <T>
	 * @param base
	 * @param node
	 */
	public static <T extends Entity> void updateObjectFromNode(T base, Node node) {
		if (base == null)
			throw new IllegalArgumentException("Base cannot be null");
		if (node == null)
			throw new IllegalArgumentException("Node cannot be null");
		// Find the fields that are not on nodes.
		ObjectSchema schema = SchemaCache.getSchema(base);
		Map<String, ObjectSchema> schemaProperties = schema.getProperties();
		if (schemaProperties == null) {
			schemaProperties = new HashMap<String, ObjectSchema>();
		}
		Field[] fields = getTranslatableEntityFields(base);
		for (Field field : fields) {
			String name = field.getName();
			String nodeName = nameConvertion.get(name);
			if (nodeName == null) {
				nodeName = name;
			}
			// Only include fields that are in node.
			Field nodeField = nodeFieldNames.get(nodeName);
			if (nodeField != null) {
				// Make sure we can call it.
				field.setAccessible(true);
				nodeField.setAccessible(true);
				Object value;
				try {
					// only set non-static fields
					if((field.getModifiers() & Modifier.STATIC)==0) {
						value = nodeField.get(node);
						if (value != null) {
							field.set(base, value);
						}
					}
				} catch (IllegalAccessException e) {
					// This should never occur
					log.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Update an object using annotations and references.
	 * 
	 * @param <T>
	 * @param base
	 * @param annos
	 */
	public static <T extends Entity> void updateObjectFromNodeSecondaryFields(
			T base, Annotations annos) {
		if (base == null)
			throw new IllegalArgumentException("Base cannot be null");
		if (annos == null)
			throw new IllegalArgumentException("Annotations cannot be null");
		// Look up the schema for this Entity.
		ObjectSchema schema = SchemaCache.getSchema(base);
		Map<String, ObjectSchema> schemaProperties = schema.getProperties();
		if (schemaProperties == null) {
			schemaProperties = new HashMap<String, ObjectSchema>();
		}
		// Find the fields that are not on nodes.
		Field[] fields = getTranslatableEntityFields(base);
		for (Field field : fields) {
			String name = field.getName();
			String nodeName = nameConvertion.get(name);
			if (nodeName == null) {
				nodeName = name;
			}
			// Is this a field already on Node?
			if (!nodeFieldNames.containsKey(nodeName)) {
				// Make sure we can call it.
				field.setAccessible(true);
				ObjectSchema propSchema = schemaProperties.get(name);
				if (propSchema == null) {
					continue;
				}
				try {
					Object value = annos.getSingleValue(name);
					if (value != null) {
						if (field.getType() == Boolean.class) {
							// We need to convert the string to a boolean
							value = Boolean.parseBoolean((String) value);
						}
						// If this is an enum then we stored the string value,
						// so we
						// must convert back to an enumeration.
						if (propSchema.getEnum() != null) {
							// Get the class for this enumeration.
							try {
								if (propSchema.getId() == null)
									throw new IllegalArgumentException(
											"Cannot determine the class of an enumeration because the schema ID is null");
								Class<?> clazz = Class.forName(propSchema
										.getId());
								value = NodeTranslationUtils
										.getValueOfFromEnum((String) value,
												clazz);
							} catch (ClassNotFoundException e) {
								throw new RuntimeException(e);
							}
						}

						if (propSchema.isTransient())
							continue;
						// Is this an array?

						// JSONEntity and Binary are stored as blobs.
						if (propSchema.getContentEncoding() != null
								|| value instanceof JSONEntity) {
							// Convert from a
							value = NodeTranslationUtils.bytesToObject(
									(byte[]) value, propSchema);
							field.set(base, value);
							continue;
						}

						if (field.getType().isAssignableFrom(Collection.class) ) {
							List<Object> list = new ArrayList<Object>();
							list.add(value);
							field.set(base, list);
						} else {
							field.set(base, value);
						}
					}

				} catch (IllegalAccessException e) {
					// This should never occur
					log.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}

}
