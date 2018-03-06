package org.sagebionetworks.javadoc.velocity.schema;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.javadoc.web.services.FilterUtils;
import org.sagebionetworks.schema.EnumValue;
import org.sagebionetworks.schema.HasEffectiveSchema;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.schema.generator.EffectiveSchemaUtil;
import org.sagebionetworks.server.ServerSideOnlyFactory;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;

public class SchemaUtils {
	
	public static void findSchemaFiles(Map<String, ObjectSchema> schemaMap,	RootDoc root) {
		// Add all know concrete classes from the Factory.
		ServerSideOnlyFactory autoGen = new ServerSideOnlyFactory();
		Iterator<String> keySet = autoGen.getKeySetIterator();
		while(keySet.hasNext()){
			String name = keySet.next();
			ObjectSchema schema = SchemaUtils.getSchema(name);
			SchemaUtils.recursiveAddTypes(schemaMap, name, schema);
		}
        Iterator<ClassDoc> contollers = FilterUtils.controllerIterator(root.classes());
        while(contollers.hasNext()){
        	ClassDoc classDoc = contollers.next();
        	Iterator<MethodDoc> methodIt = FilterUtils.requestMappingIterator(classDoc.methods());
        	while(methodIt.hasNext()){
        		MethodDoc methodDoc = methodIt.next();
        		SchemaUtils.findSchemaFiles(schemaMap, methodDoc);
        	}
        }
	}

	/**
	 * Find the schemas used by the method and add them to the set.
	 * 
	 * @param set
	 * @param method
	 */
	public static void findSchemaFiles(Map<String, ObjectSchema> schemaMap,	MethodDoc method) {
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

	private static void recursiveAddSubTypes(Map<String, ObjectSchema> schemaMap, ClassDoc paramClass) {
		if (implementsJSONEntityOrEnum(paramClass)) {
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
				if(schema == null) return;
			}
			schemaMap.put(id, schema);
			// Add all interfaces
			try {
				Class clazz = Class.forName(id);
				Class[] ins = clazz.getInterfaces();
				if(ins != null){
					for(Class inter: ins){
						recursiveAddTypes(schemaMap, inter.getName(), null);
					}
				}
			} catch (ClassNotFoundException e) {
				//throw new RuntimeException(e);
			}
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
				}else if(TYPE.INTERFACE == sub.getType()){
					if(sub.getId() == null) throw new IllegalArgumentException("ObjectSchema.id cannot be null for TYPE.OBJECT");
					recursiveAddTypes(schemaMap, sub.getId(), sub);
				}else if(sub.getId() != null){
					// Enumeration fall into this category
					recursiveAddTypes(schemaMap, sub.getId(), sub);
				}
			}
		}
	}
	
	/**
	 * Map all known implementations of each interface.
	 * @param schemaMap
	 * @return
	 */
	public static Map<String, List<TypeReference>> mapImplementationsToIntefaces(Map<String, ObjectSchema> schemaMap){
		Map<String, List<TypeReference>> map = new HashMap<String, List<TypeReference>>();
		Iterator<String> it =  schemaMap.keySet().iterator();
		while(it.hasNext()){
			String key = it.next();
			ObjectSchema schema = schemaMap.get(key);
			if(schema.getId() != null){
				try {
					Class clazz = Class.forName(schema.getId());
					Class[] interfaces = clazz.getInterfaces();
					if(interfaces != null){
						for(Class inter: interfaces){
							String interfaceName =inter.getName();
							if(schemaMap.containsKey(interfaceName)){
								List<TypeReference> list = map.get(interfaceName);
								if(list == null){
									list = new LinkedList<TypeReference>();
									map.put(interfaceName, list);
								}
								list.add(typeToLinkString(schema));
							}
						}
					}
				} catch (ClassNotFoundException e) {
					continue;
				}
			}
		}
		return map;
	}

	/**
	 * Does the given class implement JSONEntity.
	 * 
	 * @param classDoc
	 * @return
	 */
	public static boolean implementsJSONEntityOrEnum(ClassDoc classDoc) {
		// primitives will not have a class and do not implement JSONEntity
		if (classDoc == null)
			return false;
		if (classDoc.isEnum()) {
			return true;
		}
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
			String json = null;
			try {
				json = EffectiveSchemaUtil.loadEffectiveSchemaFromClasspath(clazz);
			} catch (IllegalArgumentException e) {
				if(!clazz.isInterface()) {
					JSONEntity entity = clazz.newInstance();
					if(entity instanceof HasEffectiveSchema) {
						HasEffectiveSchema hasSchema = (HasEffectiveSchema) entity;
						json = hasSchema.getEffectiveSchema();
					}
				}
			}
			if(json == null) return null;
			if(!json.startsWith("{")) return null;
			return json;
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
		String json = null;
		try {
			json = getEffectiveSchema(name);
			if(json == null) return null;
			JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl(json);
			ObjectSchema schema = new ObjectSchema(adpater);
			return schema;
		} catch (Exception e) {
			System.out.println(json);
			throw new RuntimeException(e);
		}

	}

	/**
	 * Translate an object schema to a model.
	 * 
	 * @param schema
	 * @return
	 */
	public static ObjectSchemaModel translateToModel(ObjectSchema schema, List<TypeReference> knownImplementaions) {
		ObjectSchemaModel results = new ObjectSchemaModel();
		results.setDescription(schema.getDescription());
		results.setEffectiveSchema(schema.getSchema());
		results.setId(schema.getId());
		results.setName(schema.getName());
		// Get the fields
		Map<String, ObjectSchema> props = schema.getProperties();
		if(props != null && !props.isEmpty()){
			List<SchemaFields> fields = new LinkedList<SchemaFields>();
			results.setFields(fields);
			Iterator<String> keyIt = props.keySet().iterator();
			while(keyIt.hasNext()){
				String key = keyIt.next();
				ObjectSchema prop = props.get(key);
				SchemaFields field = translateToSchemaField(key, prop);
				fields.add(field);
			}
		}
		if(schema.getEnum() != null){
			// This is an enumeration.
			List<EnumValue> enumValues = new LinkedList<EnumValue>();
			results.setEnumValues(enumValues);
			for(EnumValue en: schema.getEnum()){
				enumValues.add(en);
			}
		}
		results.setKnownImplementations(knownImplementaions);
		results.setIsInterface(knownImplementaions != null);
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
		boolean isMap = false;
		boolean isUnique = false;
		String[] display = getTypeDisplay(type);
		String[] href = getTypeHref(type);
		if(TYPE.ARRAY == type.getType()){
			isArray = true;
			isUnique = type.getUniqueItems();
		} else if (TYPE.MAP == type.getType()) {
			isMap = true;
		}
		return new TypeReference(isArray, isUnique, isMap, display, href);
	}
	
	/**
	 * For any schema that is not a primitive, get the link (href).
	 * @param type
	 * @return
	 */
	public static String[] getTypeHref(ObjectSchema type) {
		if(TYPE.OBJECT == type.getType() || TYPE.INTERFACE == type.getType()){
			if(type.getId() == null) throw new IllegalArgumentException("ObjectSchema.id cannot be null for TYPE.OBJECT");
			StringBuilder builder = new StringBuilder();
			builder.append("${").append(type.getId()).append("}");
			return new String[] { builder.toString() };
		} if(TYPE.ARRAY == type.getType()){
			if(type.getItems() == null) throw new IllegalArgumentException("ObjectSchema.items cannot be null for TYPE.ARRAY");
			return getTypeHref(type.getItems());
		}
		if (TYPE.MAP == type.getType()) {
			ObjectSchema keySchema = type.getKey();
			ObjectSchema valueSchema = type.getValue();
			if (keySchema == null) {
				throw new IllegalArgumentException("ObjectSchema.key cannot be null for TYPE.MAP");
			}
			if (valueSchema == null) {
				throw new IllegalArgumentException("ObjectSchema.value cannot be null for TYPE.MAP");
			}
			String[] keyHref = getTypeHref(keySchema);
			String[] valueHref = getTypeHref(valueSchema);
			if (keyHref.length != 1) {
				throw new IllegalArgumentException("ObjectSchema.key not a single type");
			}
			if (valueHref.length != 1) {
				throw new IllegalArgumentException("ObjectSchema.key not a single type");
			}
			return new String[] { keyHref[0], valueHref[0] };
		} else if (type.getType() == TYPE.STRING && type.getEnum() != null && type.getId() != null) {
			String typeName = type.getId();
			return new String[] { "${" + typeName + "}" };
		}else{
			// primitives do not have links
			return new String[] { null };
		}
	}
	
	public static String[] getTypeDisplay(ObjectSchema type) {
		if(TYPE.OBJECT == type.getType() || TYPE.INTERFACE == type.getType()){
			return new String[] { type.getName() };
		} if(TYPE.ARRAY == type.getType()){
			if(type.getItems() == null) throw new IllegalArgumentException("ObjectSchema.items cannot be null for TYPE.ARRAY");
			return getTypeDisplay(type.getItems());
		}
		if (TYPE.MAP == type.getType()) {
			ObjectSchema keySchema = type.getKey();
			ObjectSchema valueSchema = type.getValue();
			if (keySchema == null) {
				throw new IllegalArgumentException("ObjectSchema.key cannot be null for TYPE.MAP");
			}
			if (valueSchema == null) {
				throw new IllegalArgumentException("ObjectSchema.value cannot be null for TYPE.MAP");
			}
			String[] keyDisplay = getTypeDisplay(keySchema);
			String[] valueDisplay = getTypeDisplay(valueSchema);
			if (keyDisplay.length != 1) {
				throw new IllegalArgumentException("ObjectSchema.key not a single type");
			}
			if (valueDisplay.length != 1) {
				throw new IllegalArgumentException("ObjectSchema.key not a single type");
			}
			return new String[] { keyDisplay[0], valueDisplay[0] };
		} else if (type.getType() == TYPE.STRING && type.getEnum() != null && type.getId() != null) {
			return new String[] { type.getName() };
		}else{
			return new String[] { type.getType().toString() };
		}
	}
}
