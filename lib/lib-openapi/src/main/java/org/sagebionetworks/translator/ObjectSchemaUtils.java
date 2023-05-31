package org.sagebionetworks.translator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.javadoc.velocity.schema.SchemaUtils;
import org.sagebionetworks.javadoc.velocity.schema.TypeReference;
import org.sagebionetworks.openapi.server.ServerSideOnlyFactory;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.util.ValidateArgument;

public class ObjectSchemaUtils {	
	/**
	 * Generates a mapping of class id to an ObjectSchema that represents that class.
	 * Starts out with all of the concrete classes found in `autoGen`
	 * 
	 * @param autoGen - the ServerSideOnlyFactory whose class id's represent all concrete classes.
	 * @return a mapping of concrete classes between class id to an ObjectSchema that represents it.
	 */
	public Map<String, ObjectSchema> getConcreteClasses(ServerSideOnlyFactory autoGen) {
		Map<String, ObjectSchema> classNameToObjectSchema = new HashMap<>();
		Iterator<String> keySet = autoGen.getKeySetIterator();
		while (keySet.hasNext()) {
			String className = keySet.next();
			ObjectSchema schema = SchemaUtils.getSchema(className);
			SchemaUtils.recursiveAddTypes(classNameToObjectSchema, className, schema);
		}
		return classNameToObjectSchema;
	}
	
	/**
	 * Translates a mapping from class id to ObjectSchema to a mapping from class id to JsonSchema;
	 * 
	 * @param classNameToObjectSchema - the mapping being translated
	 * @return a translated mapping consisting of class id to JsonSchema
	 */
	public Map<String, JsonSchema> getClassNameToJsonSchema(Map<String, ObjectSchema> classNameToObjectSchema) {
		Map<String, JsonSchema> classNameToJsonSchema = new HashMap<>();
		for (String className : classNameToObjectSchema.keySet()) {
			ObjectSchema schema = classNameToObjectSchema.get(className);
			classNameToJsonSchema.put(className, translateObjectSchemaToJsonSchema(schema));
		}
		Map<String, List<TypeReference>> interfaces = SchemaUtils.mapImplementationsToIntefaces(classNameToObjectSchema);
		insertOneOfPropertyForInterfaces(classNameToJsonSchema, interfaces);
		return classNameToJsonSchema;
	}

	/**
	 * Translates an ObjectSchema to a JsonSchema
	 * 
	 * @param objectSchema - the schema being translated.
	 * @return the resulting JsonSchema
	 */
	JsonSchema translateObjectSchemaToJsonSchema(ObjectSchema objectSchema) {
		ValidateArgument.required(objectSchema, "objectSchema");
		if (isPrimitive(objectSchema.getType())) {
			return getSchemaForPrimitiveType(objectSchema.getType());
		}
		JsonSchema jsonSchema = new JsonSchema();
		jsonSchema.setType(translateObjectSchemaTypeToJsonSchemaType(objectSchema.getType()));
		jsonSchema.setProperties(translatePropertiesFromObjectSchema(objectSchema.getProperties()));
		if (objectSchema.getDescription() != null) {
			jsonSchema.setDescription(objectSchema.getDescription());
		}

		return jsonSchema;
	}
	
	/**
	 * Translate the "properties" attribute of an ObjectSchema, which is a mapping between class id to ObjectSchema
	 * 
	 * @param properties - the properties we are translating
	 * @return an equivalent mapping between class id to JsonSchema
	 */
	Map<String, JsonSchema> translatePropertiesFromObjectSchema(Map<String, ObjectSchema> properties) {
		ValidateArgument.required(properties, "properties");
		Map<String, JsonSchema> result = new LinkedHashMap<>();
		for (String className : properties.keySet()) {
			// TODO: here we should check if the class is primitive, if it is, then output the JsonSchema, otherwise
			// 		 create a JsonSchema that is just a ref.
			result.put(className, translateObjectSchemaToJsonSchema(properties.get(className)));
		}
		return result;
	}
	
	/**
	 * Returns if the given TYPE is primitive.
	 * 
	 * @param type - the TYPE in question
	 * @return true if 'type' is primitive, false otherwise.
	 */
	boolean isPrimitive(TYPE type) {
		return type.equals(TYPE.BOOLEAN) || type.equals(TYPE.NUMBER) || type.equals(TYPE.STRING) || type.equals(TYPE.INTEGER);
	}
	
	/**
	 * Inserts the oneOf properties into all of the JsonSchemas which represent interfaces.
	 * 
	 * @param classNameToJsonSchema mapping from class ids to a JsonSchema that represents that class.
	 * @param interfaces the interfaces present in the application mapped to the list of classes that implement them.
	 */
	void insertOneOfPropertyForInterfaces(Map<String, JsonSchema> classNameToJsonSchema, Map<String, List<TypeReference>> interfaces) {
		for (String className : classNameToJsonSchema.keySet()) {
			if (interfaces.containsKey(className)) {
				Set<TypeReference> implementers = getImplementers(className, interfaces);
				List<JsonSchema> oneOf = new ArrayList<>();
				for (TypeReference implementer : implementers) {
					String id = implementer.getId();
					if (!classNameToJsonSchema.containsKey(id)) {
						throw new IllegalArgumentException("Implementation of " + className + " interface with name " + id + " was not found.");
					}
					JsonSchema newSchema = new JsonSchema();
					newSchema.set$ref("#/components/" + id);
					oneOf.add(newSchema);
				}
				classNameToJsonSchema.get(className).setOneOf(oneOf);
			}
		}
	}
	
	/**
	 * Recursively gets all of the concrete implementers of the given interface.
	 * 
	 * @param interfaceId the id of the interface
	 * @param interfaces a mapping between all interfaces and their implementers
	 * @return a set of all of the concrete implementers of the interface
	 */
	Set<TypeReference> getImplementers(String interfaceId, Map<String, List<TypeReference>> interfaces) {
		Set<TypeReference> allImplementers = new HashSet<>();
		
		List<TypeReference> implementers = interfaces.get(interfaceId);
		for (TypeReference implementer : implementers) {
			String implementerId = implementer.getId();
			boolean isInterface = interfaces.containsKey(implementerId);
			if (isInterface) {
				// add all of the classes that implement this interface
				Set<TypeReference> currentImplementers = getImplementers(implementerId, interfaces);
				allImplementers.addAll(currentImplementers);
			} else {
				allImplementers.add(implementer);
			}
		}
		
		return allImplementers;
	}

	/**
	 * Translates the TYPE enum used for ObjectSchema to type used for JsonSchema
	 * 
	 * @param type the ObjectSchema type being translated
	 * @return the equivalent JsonSchema type.
	 */
	Type translateObjectSchemaTypeToJsonSchemaType(TYPE type) {
		ValidateArgument.required(type, "type");
		switch (type) {
		case NULL:
			return Type._null;
		case ARRAY:
			return Type.array;
		case OBJECT:
		case INTERFACE:
			return Type.object;
		default:
			throw new IllegalArgumentException("Unable to convert non-primitive type " + type);
		}
	}
	
	/**
	 * Constructs a JsonSchema for a primitive class.
	 * 
	 * @param type - the primitive type
	 * @return the JsonSchema used to represent this type.
	 */
	JsonSchema getSchemaForPrimitiveType(TYPE type) {
		ValidateArgument.required(type, "type");
		JsonSchema schema = new JsonSchema();
		switch (type) {
		case STRING:
			schema.setType(Type.string);
			break;
		case INTEGER:
			schema.setType(Type.integer);
			schema.setFormat("int32");
			break;
		case NUMBER:
			schema.setType(Type.number);
			break;
		case BOOLEAN:
			schema.setType(Type._boolean);
			break;
		default:
			throw new IllegalArgumentException("Unable to translate primitive type " + type);
		}
		return schema;
	}
}
