package org.sagebionetworks.javadoc.velocity.schema;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Type;

public class SchemaUtils {

	/**
	 * Find the schemas used by the method and add them to the set.
	 * 
	 * @param set
	 * @param method
	 */
	public static void findSchemaFiles(Map<String, ObjectSchema> schemaMap,
			MethodDoc method) {
		// A schema class can be used for the return type or a parameters
		Type returnType = method.returnType();
		ClassDoc returnClassDoc = returnType.asClassDoc();
		if (returnClassDoc != null) {
			// Get the full name
			recursiveAddSubTypes(schemaMap, returnClassDoc);
		}
		// Apply the same test to all parameters
		Parameter[] params = method.parameters();
		if (params != null) {
			for (Parameter param : params) {
				ClassDoc paramClass = param.type().asClassDoc();
				recursiveAddSubTypes(schemaMap, paramClass);
			}
		}
	}

	private static void recursiveAddSubTypes(
			Map<String, ObjectSchema> schemaMap, ClassDoc paramClass) {
		if (implementsJSONEntity(paramClass)) {
			// Lookup the schema and add sub types.
			recursiveAddTypes(schemaMap, paramClass.qualifiedName(), null);
		}
	}

	/**
	 * Recursively add all types associated with the given schema.
	 * @param schemaMap
	 * @param id
	 * @param schema
	 */
	public static void recursiveAddTypes(Map<String, ObjectSchema> schemaMap,
			String id, ObjectSchema schema) {
		if (!schemaMap.containsKey(id)) {
			if (schema == null) {
				schema = getSchema(id);
			}
			schemaMap.put(id, schema);
			Iterator<ObjectSchema> it = schema.getSubSchemaIterator();
			while (it.hasNext()) {
				ObjectSchema sub = it.next();
				if (TYPE.OBJECT == sub.getType()) {
					if(sub.getId() == null) throw new IllegalArgumentException("ObjectSchema.id cannot be null for TYPE.OBJECT");
					recursiveAddTypes(schemaMap, sub.getId(), sub);
				}else if(TYPE.ARRAY == sub.getType()){
					if(sub.getItems() == null) throw new IllegalArgumentException("ObjectSchema.items cannot be null for TYPE.ARRAY");
					ObjectSchema arrayItems = sub.getItems();
					if (TYPE.OBJECT == arrayItems.getType()) {
						if(arrayItems.getId() == null) throw new IllegalArgumentException("ObjectSchema.id cannot be null for TYPE.OBJECT");
						recursiveAddTypes(schemaMap, arrayItems.getId(), arrayItems);
					}
				}else if(sub.getId() != null){
					// Enumeration fall into this category
					recursiveAddTypes(schemaMap, sub.getId(), sub);
				}
			}
		}
	}

	/**
	 * Does the given class implement JSONEntity.
	 * 
	 * @param classDoc
	 * @return
	 */
	public static boolean implementsJSONEntity(ClassDoc classDoc) {
		// primitives will not have a class and do not implement JSONEntity
		if (classDoc == null)
			return false;
		ClassDoc[] interfaces = classDoc.interfaces();
		if (interfaces != null) {
			for (ClassDoc doc : interfaces) {
				if (JSONEntity.class.getName().equals(doc.qualifiedName())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Get the effective schema for a class.
	 * 
	 * @param name
	 * @return
	 */
	public static String getEffectiveSchema(String name) {
		Class<JSONEntity> clazz;
		try {
			clazz = (Class<JSONEntity>) Class.forName(name);
			JSONEntity entity = clazz.newInstance();
			return entity.getJSONSchema();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Get the schema for a class.
	 * 
	 * @param name
	 * @return
	 */
	public static ObjectSchema getSchema(String name) {
		try {
			String json = getEffectiveSchema(name);
			JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl(json);
			ObjectSchema schema = new ObjectSchema(adpater);
			return schema;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Translate an object schema to a model.
	 * 
	 * @param schema
	 * @return
	 */
	public static ObjectSchemaModel translateToModel(ObjectSchema schema) {
		ObjectSchemaModel results = new ObjectSchemaModel();
		results.setDescription(schema.getDescription());
		results.setEffectiveSchema(schema.getSchema());
		results.setId(schema.getId());
		results.setName(schema.getName());
		// Get the fields
		Map<String, ObjectSchema> props = schema.getProperties();
		if(props != null){
			List<SchemaFields> fields = new LinkedList<SchemaFields>();
			results.setFields(fields);
			Iterator<String> keyIt = props.keySet().iterator();
			while(keyIt.hasNext()){
				String key = keyIt.next();
				ObjectSchema prop = props.get(key);
				SchemaFields field = translateToSchemaField(key, prop);
				fields.add(field);
			}
		}else if(schema.getEnum() != null){
			// This is an enumeration.
			List<String> enumValues = new LinkedList<String>();
			results.setEnumValues(enumValues);
			for(String en: schema.getEnum()){
				enumValues.add(en);
			}
		}
		return results;
	}
	
	/**
	 * Translate from a schema to a SchemaFields object
	 * @param key
	 * @param prop
	 * @return
	 */
	public static SchemaFields translateToSchemaField(String key, ObjectSchema prop){
		SchemaFields field = new SchemaFields();
		field.setName(key);
		field.setDescription(prop.getDescription());
		field.setType(typeToLinkString(prop));
		return field;
	}
	
	/**
	 * Create type link
	 * @param type
	 * @return
	 */
	public static TypeReference typeToLinkString(ObjectSchema type){
		boolean isArray = false;
		boolean isUnique = false;
		String display = getTypeDisplay(type);
		String href = getTypeHref(type);
		if(TYPE.ARRAY == type.getType()){
			isArray = true;
			isUnique = type.getUniqueItems();
		}
		return new  TypeReference(isArray, isUnique, display, href);
	}
	
	/**
	 * For any schema that is not a primitive, get the link (href).
	 * @param type
	 * @return
	 */
	public static String getTypeHref(ObjectSchema type){
		if(TYPE.OBJECT == type.getType()){
			if(type.getId() == null) throw new IllegalArgumentException("ObjectSchema.id cannot be null for TYPE.OBJECT");
			StringBuilder builder = new StringBuilder();
			builder.append("${").append(type.getId()).append("}");
			return builder.toString();
		} if(TYPE.ARRAY == type.getType()){
			if(type.getItems() == null) throw new IllegalArgumentException("ObjectSchema.items cannot be null for TYPE.ARRAY");
			return getTypeHref(type.getItems());
		}else{
			// primitives do not have links
			return null;
		}
	}
	
	public static String getTypeDisplay(ObjectSchema type){
		if(TYPE.OBJECT == type.getType()){
			return type.getName();
		} if(TYPE.ARRAY == type.getType()){
			if(type.getItems() == null) throw new IllegalArgumentException("ObjectSchema.items cannot be null for TYPE.ARRAY");
			return getTypeDisplay(type.getItems());
		}else{
			return type.getType().toString();
		}
	}
}
