package org.sagebionetworks.javadoc.velocity.schema;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;

import org.sagebionetworks.javadoc.web.services.FilterUtils;
import org.sagebionetworks.schema.EnumValue;
import org.sagebionetworks.schema.HasEffectiveSchema;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.schema.generator.EffectiveSchemaUtil;
import org.sagebionetworks.server.ServerSideOnlyFactory;

import jdk.javadoc.doclet.DocletEnvironment;

public class SchemaUtils {
	
	public static void findSchemaFiles(Map<String, ObjectSchema> schemaMap,	DocletEnvironment root) {
		findSchemaFiles(schemaMap, null, root);
	}

	/**
	 * @param anchorMap When non-null, populated with the enclosing {@code $recursiveAnchor} for each
	 *                  inline named type reached through one (see {@link #recursiveAddTypes}).
	 */
	public static void findSchemaFiles(Map<String, ObjectSchema> schemaMap,
			Map<String, ObjectSchema> anchorMap, DocletEnvironment root) {
		// Add all know concrete classes from the Factory.
		ServerSideOnlyFactory autoGen = new ServerSideOnlyFactory();
		Iterator<String> keySet = autoGen.getKeySetIterator();
		while(keySet.hasNext()){
			String name = keySet.next();
			ObjectSchema schema = SchemaUtils.getSchema(name);
			SchemaUtils.recursiveAddTypes(schemaMap, name, schema, anchorMap, null);
		}
        Iterator<TypeElement> contollers = FilterUtils.controllerIterator(root);
        while(contollers.hasNext()){
        	TypeElement typeElement = contollers.next();
        	Iterator<ExecutableElement> methodIt = FilterUtils.requestMappingIterator(typeElement);
        	while(methodIt.hasNext()){
        		ExecutableElement ExecutableElement = methodIt.next();
        		SchemaUtils.findSchemaFiles(root, schemaMap, anchorMap, ExecutableElement);
        	}
        }
	}

	/**
	 * Find the schemas used by the method and add them to the set.
	 * 
	 * @param set
	 * @param method
	 */
	public static void findSchemaFiles(DocletEnvironment env, Map<String, ObjectSchema> schemaMap,
			Map<String, ObjectSchema> anchorMap, ExecutableElement method) {
		TypeMirror noType = env.getElementUtils().getTypeElement(NoType.class.getName()).asType();

		if(!env.getTypeUtils().isAssignable(method.getReturnType(), noType)) {
			TypeElement returnType = env.getElementUtils()
					.getTypeElement(env.getTypeUtils().erasure(method.getReturnType()).toString());
			recursiveAddSubTypes(env, schemaMap, anchorMap, returnType);
		}
		method.getParameters().forEach(param -> {
			TypeElement paramType = env.getElementUtils().getTypeElement(param.asType().toString());
			recursiveAddSubTypes(env, schemaMap, anchorMap, paramType);
		});
	}

	private static void recursiveAddSubTypes(DocletEnvironment env, Map<String, ObjectSchema> schemaMap,
			Map<String, ObjectSchema> anchorMap, TypeElement paramClass) {
		if (implementsJSONEntityOrEnum(env, paramClass)) {
			// Lookup the schema and add sub types.
			recursiveAddTypes(schemaMap, paramClass.getQualifiedName().toString(), null, anchorMap, null);
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
		recursiveAddTypes(schemaMap, id, schema, null, null);
	}

	/**
	 * @param anchorMap        Records, per type id, the enclosing {@code $recursiveAnchor} a type was
	 *                         reached through (or {@code null}). An inline named type extracted from a
	 *                         recursive parent (e.g. {@code BoolQuery} inside the {@code Query} anchor)
	 *                         has {@code $recursiveRef} slots whose anchor lives on the parent; this
	 *                         map lets the standalone-type translation resolve them. May be {@code null}.
	 * @param enclosingAnchor  The nearest {@code $recursiveAnchor} on the path to this type.
	 */
	public static void recursiveAddTypes(Map<String, ObjectSchema> schemaMap,
			String id, ObjectSchema schema, Map<String, ObjectSchema> anchorMap, ObjectSchema enclosingAnchor) {
		ObjectSchema known = schemaMap.get(id);
		// A type may first be reached through a path without its anchor, e.g. a schema that extends the
		// anchor inherits its inline recursive types but not the $recursiveAnchor itself. Revisit the type
		// when a later path supplies the anchor, so the result does not depend on visit order.
		boolean gainsAnchor = known != null && anchorMap != null && enclosingAnchor != null
				&& !Boolean.TRUE.equals(known.get$recursiveAnchor()) && !anchorMap.containsKey(id);
		if (known == null || gainsAnchor) {
			if (known != null) {
				schema = known;
			} else if (schema == null) {
				schema = getSchema(id);
				if(schema == null) return;
			}
			schemaMap.put(id, schema);
			ObjectSchema childAnchor = Boolean.TRUE.equals(schema.get$recursiveAnchor())
					? schema : enclosingAnchor;
			if (anchorMap != null && enclosingAnchor != null
					&& !Boolean.TRUE.equals(schema.get$recursiveAnchor())) {
				anchorMap.put(id, enclosingAnchor);
			}
			// Add all interfaces
			try {
				Class clazz = Class.forName(id);
				Class[] ins = clazz.getInterfaces();
				if(ins != null){
					for(Class inter: ins){
						recursiveAddTypes(schemaMap, inter.getName(), null, anchorMap, childAnchor);
					}
				}
			} catch (ClassNotFoundException e) {
				//throw new RuntimeException(e);
			}
			Iterator<ObjectSchema> it = schema.getSubSchemaIterator();
			while (it.hasNext()) {
				ObjectSchema sub = it.next();
				if (TYPE.OBJECT == sub.getType() && sub.getId() != null) {
					recursiveAddTypes(schemaMap, sub.getId(), sub, anchorMap, childAnchor);
				}else if(TYPE.ARRAY == sub.getType()){
					if(sub.getItems() == null) throw new IllegalArgumentException("ObjectSchema.items cannot be null for TYPE.ARRAY");
					ObjectSchema arrayItems = sub.getItems();
					if ((TYPE.OBJECT == arrayItems.getType()
						/*PLFM-5723*/ || arrayItems.getEnum() != null) && arrayItems.getId() != null) {
						recursiveAddTypes(schemaMap, arrayItems.getId(), arrayItems, anchorMap, childAnchor);
					}
				}else if(TYPE.INTERFACE == sub.getType()){
					if(sub.getId() == null) throw new IllegalArgumentException("ObjectSchema.id cannot be null for TYPE.OBJECT");
					recursiveAddTypes(schemaMap, sub.getId(), sub, anchorMap, childAnchor);
				}else if(sub.getId() != null){
					// Enumeration fall into this category
					recursiveAddTypes(schemaMap, sub.getId(), sub, anchorMap, childAnchor);
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
				ObjectSchema recursieAnchor = null;
				if(Boolean.TRUE.equals(schema.get$recursiveAnchor())){
					recursieAnchor = schema;
				}
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
								list.add(typeToLinkString(schema, recursieAnchor));
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
	 * @param TypeElement
	 * @return
	 */
	public static boolean implementsJSONEntityOrEnum(DocletEnvironment env, TypeElement typeElement) {
		if(typeElement == null) {
			return false;
		}
        if (typeElement.getKind() == ElementKind.ENUM) {
            return true;
        }
		TypeMirror jsonEntityType = env.getElementUtils().getTypeElement(JSONEntity.class.getName()).asType();
		return env.getTypeUtils().isAssignable(typeElement.asType(), jsonEntityType);
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
			ObjectSchema schema = new ObjectSchemaImpl(adpater);
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
	public static ObjectSchemaModel translateToModel(ObjectSchema schema, List<TypeReference> knownImplementations) {
		return translateToModel(schema, knownImplementations, null);
	}

	/**
	 * Translate a schema to a model.
	 *
	 * @param enclosingAnchor The {@code $recursiveAnchor} of the type that this schema was reached
	 *                        from, or {@code null}. Multi-type recursion (e.g. the OpenSearch DSL,
	 *                        where {@code Query} is the anchor and the inline {@code BoolQuery} it
	 *                        contains has {@code $recursiveRef} slots) extracts the inline type and
	 *                        documents it standalone; that inline type has no {@code $recursiveAnchor}
	 *                        of its own, so its recursive references resolve to this enclosing anchor.
	 */
	public static ObjectSchemaModel translateToModel(ObjectSchema schema, List<TypeReference> knownImplementations,
			ObjectSchema enclosingAnchor) {
		ObjectSchemaModel results = new ObjectSchemaModel();
		results.setDescription(schema.getDescription());
		results.setEffectiveSchema(schema.getSchema());
		results.setId(schema.getId());
		results.setName(schema.getName());
		ObjectSchema recursiveAnchor = enclosingAnchor;
		if(Boolean.TRUE.equals(schema.get$recursiveAnchor())) {
			recursiveAnchor = schema;
		}
		// Get the fields
		Map<String, ObjectSchema> props = schema.getProperties();
		if(props != null && !props.isEmpty()){
			List<SchemaFields> fields = new LinkedList<SchemaFields>();
			results.setFields(fields);
			Iterator<String> keyIt = props.keySet().iterator();
			while(keyIt.hasNext()){
				String key = keyIt.next();
				ObjectSchema prop = props.get(key);
				SchemaFields field = translateToSchemaField(key, prop, recursiveAnchor);
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
		results.setKnownImplementations(knownImplementations);
		results.setIsInterface(knownImplementations != null);
		
		if (results.getIsInterface() && schema.getDefaultConcreteType() != null && knownImplementations != null) {
			knownImplementations
				.stream()
				.filter(typeReference -> schema.getDefaultConcreteType().equals(typeReference.getId()))
				.findFirst()
				.ifPresent(defaultImplementation -> {
					results.setDefaultImplementation(defaultImplementation);
				});
		}
		
		return results;
	}
	
	/**
	 * Translate from a schema to a SchemaFields object
	 * @param key
	 * @param prop
	 * @return
	 */
	public static SchemaFields translateToSchemaField(String key, ObjectSchema prop, ObjectSchema recursiveAnchor){
		SchemaFields field = new SchemaFields();
		field.setName(key);
		field.setDescription(prop.getDescription());
		field.setType(typeToLinkString(prop, recursiveAnchor));
		return field;
	}
	
	/**
	 * Create type link
	 * @param type
	 * @return
	 */
	public static TypeReference typeToLinkString(ObjectSchema type, ObjectSchema recursiveAnchor){
		boolean isArray = false;
		boolean isMap = false;
		boolean isUnique = false;
		String[] display = getTypeDisplay(type, recursiveAnchor);
		String[] href = getTypeHref(type, recursiveAnchor);
		if(TYPE.ARRAY == type.getType()){
			isArray = true;
			isUnique = type.getUniqueItems();
		} else if (TYPE.TUPLE_ARRAY_MAP == type.getType() || TYPE.MAP == type.getType()) {
			isMap = true;
		}
		return new TypeReference(type.getId(), isArray, isUnique, isMap, display, href);
	}
	
	/**
	 * For any schema that is not a primitive, get the link (href).
	 * @param type
	 * @return
	 */
	public static String[] getTypeHref(ObjectSchema type, ObjectSchema recursiveAnchor) {
		if("#".equals(type.get$recursiveRef())) {
			if(recursiveAnchor == null) {
				throw new IllegalArgumentException("Found a $recursiveRef without a corosponding $recursiveAnchor");
			}
			StringBuilder builder = new StringBuilder();
			builder.append("${").append(recursiveAnchor.getId()).append("}");
			return new String[] { builder.toString() };
		}
		if((TYPE.OBJECT == type.getType() || TYPE.INTERFACE == type.getType()) && type.getId() != null){
			if(type.getId() == null) throw new IllegalArgumentException("ObjectSchema.id cannot be null for TYPE.OBJECT");
			StringBuilder builder = new StringBuilder();
			builder.append("${").append(type.getId()).append("}");
			return new String[] { builder.toString() };
		} if(TYPE.ARRAY == type.getType()){
			if(type.getItems() == null) throw new IllegalArgumentException("ObjectSchema.items cannot be null for TYPE.ARRAY");
			return getTypeHref(type.getItems(), recursiveAnchor);
		}
		if (TYPE.TUPLE_ARRAY_MAP == type.getType()) {
			ObjectSchema keySchema = type.getKey();
			ObjectSchema valueSchema = type.getValue();
			if (keySchema == null) {
				throw new IllegalArgumentException("ObjectSchema.key cannot be null for TYPE.TUPLE_ARRAY_MAP");
			}
			if (valueSchema == null) {
				throw new IllegalArgumentException("ObjectSchema.value cannot be null for TYPE.TUPLE_ARRAY_MAP");
			}
			String[] keyHref = getTypeHref(keySchema, recursiveAnchor);
			String[] valueHref = getTypeHref(valueSchema, recursiveAnchor);
			if (keyHref.length != 1) {
				throw new IllegalArgumentException("ObjectSchema.key not a single type");
			}
			if (valueHref.length != 1) {
				throw new IllegalArgumentException("ObjectSchema.key not a single type");
			}
			return new String[]{keyHref[0], valueHref[0]};
		}else if (TYPE.MAP == type.getType()) {
			ObjectSchema valueSchema = type.getValue();
			if (valueSchema == null) {
				throw new IllegalArgumentException("ObjectSchema.value cannot be null for TYPE.TUPLE_ARRAY_MAP");
			}
			String[] valueHref = getTypeHref(valueSchema, recursiveAnchor);
			if (valueHref.length != 1) {
				throw new IllegalArgumentException("ObjectSchema.key not a single type");
			}
			return new String[]{null, valueHref[0]};
		} else if (type.getType() == TYPE.STRING && type.getEnum() != null && type.getId() != null) {
			String typeName = type.getId();
			return new String[] { "${" + typeName + "}" };
		}else{
			// primitives do not have links
			return new String[] { null };
		}
	}
	
	public static String[] getTypeDisplay(ObjectSchema type, ObjectSchema recursiveAnchor) {
		if("#".equals(type.get$recursiveRef())) {
			if(recursiveAnchor == null) {
				throw new IllegalArgumentException("Found a $recursiveRef without a corosponding $recursiveAnchor");
			}
			return new String[] { recursiveAnchor.getName() };
		}
		if((TYPE.OBJECT == type.getType() || TYPE.INTERFACE == type.getType()) && type.getId() != null){
			return new String[] { type.getName() };
		} if(TYPE.ARRAY == type.getType()){
			if(type.getItems() == null) throw new IllegalArgumentException("ObjectSchema.items cannot be null for TYPE.ARRAY");
			return getTypeDisplay(type.getItems(), recursiveAnchor);
		}
		if (TYPE.TUPLE_ARRAY_MAP == type.getType()) {
			ObjectSchema keySchema = type.getKey();
			ObjectSchema valueSchema = type.getValue();
			if (keySchema == null) {
				throw new IllegalArgumentException("ObjectSchema.key cannot be null for TYPE.TUPLE_ARRAY_MAP");
			}
			if (valueSchema == null) {
				throw new IllegalArgumentException("ObjectSchema.value cannot be null for TYPE.TUPLE_ARRAY_MAP");
			}
			String[] keyDisplay = getTypeDisplay(keySchema, recursiveAnchor);
			String[] valueDisplay = getTypeDisplay(valueSchema, recursiveAnchor);
			if (keyDisplay.length != 1) {
				throw new IllegalArgumentException("ObjectSchema.key not a single type");
			}
			if (valueDisplay.length != 1) {
				throw new IllegalArgumentException("ObjectSchema.key not a single type");
			}
			return new String[]{keyDisplay[0], valueDisplay[0]};
		} else if (TYPE.MAP == type.getType()){
			ObjectSchema valueSchema = type.getValue();
			if (valueSchema == null) {
				throw new IllegalArgumentException("ObjectSchema.value cannot be null for TYPE.TUPLE_ARRAY_MAP");
			}
			String[] valueDisplay = getTypeDisplay(valueSchema, recursiveAnchor);
			if (valueDisplay.length != 1) {
				throw new IllegalArgumentException("ObjectSchema.value not a single type");
			}
			return new String[]{TYPE.STRING.name(), valueDisplay[0]};
		} else if (type.getType() == TYPE.STRING && type.getEnum() != null && type.getId() != null) {
			return new String[] { type.getName() };
		}else{
			return new String[] { type.getType().toString() };
		}
	}
}
