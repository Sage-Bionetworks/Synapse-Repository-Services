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
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.schema.EnumValue;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.util.ValidateArgument;

public class ObjectSchemaUtils {
	/**
	 * Generates a mapping of class id to an ObjectSchema that represents that
	 * class. Starts out with all of the concrete classes found in `autoGen`
	 * 
	 * @param concreteClassNames - the iterator whose values represent ids of all
	 *                           concrete classes.
	 * @return a mapping of concrete classes between class id to an ObjectSchema
	 *         that represents it.
	 */
	public Map<String, ObjectSchema> getConcreteClasses(Iterator<String> concreteClassNames) {
		Map<String, ObjectSchema> classNameToObjectSchema = new HashMap<>();
		while (concreteClassNames.hasNext()) {
			String className = concreteClassNames.next();
			ObjectSchema schema = SchemaUtils.getSchema(className);
			SchemaUtils.recursiveAddTypes(classNameToObjectSchema, className, schema);
		}
		return classNameToObjectSchema;
	}

	/**
	 * Translates a mapping from class id to ObjectSchema to a mapping from class id
	 * to JsonSchema;
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
		Map<String, List<TypeReference>> interfaces = SchemaUtils
				.mapImplementationsToIntefaces(classNameToObjectSchema);
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

		TYPE schemaType = objectSchema.getType();

		EnumValue[] enumValues = objectSchema.getEnum();
		if (enumValues != null) {
			JsonSchema jsonSchema = new JsonSchema();
			jsonSchema.setType(translateObjectSchemaTypeToJsonSchemaType(schemaType));

			List<Object> values = new ArrayList<>();
			for (EnumValue enumValue: enumValues) {
				values.add(enumValue.getName());
			}

			jsonSchema.set_enum(values);

			return jsonSchema;
		}

		if (isPrimitive(schemaType)) {
			return getSchemaForPrimitiveType(schemaType);
		}
		JsonSchema jsonSchema = new JsonSchema();
		jsonSchema.setType(translateObjectSchemaTypeToJsonSchemaType(schemaType));

		Map<String, ObjectSchema> properties = objectSchema.getProperties();
		if (properties != null) {
			jsonSchema.setProperties(translatePropertiesFromObjectSchema(properties, objectSchema.getId()));
		}

		ObjectSchema items = objectSchema.getItems();
		if (items != null) {
			populateSchemaForArrayType(jsonSchema, items, objectSchema.getId());
		}

		if (objectSchema.getDescription() != null) {
			jsonSchema.setDescription(objectSchema.getDescription());
		}

		return jsonSchema;
	}

	/**
	 * Translate the "properties" attribute of an ObjectSchema, which is a mapping
	 * between propertyName to ObjectSchema
	 * 
	 * @param properties - the properties we are translating
	 * @param schemaId   - the id of the schema whose properties we are translating
	 * @return an equivalent mapping between class id to JsonSchema
	 */
	Map<String, JsonSchema> translatePropertiesFromObjectSchema(Map<String, ObjectSchema> properties, String schemaId) {
		ValidateArgument.required(properties, "properties");
		Map<String, JsonSchema> result = new LinkedHashMap<>();
		for (String propertyName : properties.keySet()) {
			JsonSchema property = getPropertyAsJsonSchema(properties.get(propertyName), schemaId);
			result.put(propertyName, property);
		}
		return result;
	}

	/**
	 * Translates a property of an ObjectSchema to a JsonSchema.
	 * 
	 * @param property     the ObjectSchema property
	 * @param schemaId	   the id of the schema whose property we are translating
	 * @return the JsonSchema that is equivalent to the ObjectSchema property
	 */
	public JsonSchema translateObjectSchemaPropertyToJsonSchema(ObjectSchema property, String schemaId) {
		ValidateArgument.required(property, "property");
		ValidateArgument.required(schemaId, "schemaId");
		ValidateArgument.required(property.getType(), "property.type");
		TYPE propertyType = property.getType();
		if (isPrimitive(propertyType)) {
			return getSchemaForPrimitiveType(propertyType);
		} else {
			JsonSchema schema = new JsonSchema();
			if (property.getDescription() != null) {
				schema.setDescription(property.getDescription());
			}

			switch (propertyType) {
			case ARRAY:
				populateSchemaForArrayType(schema, property.getItems(), schemaId);
				break;
			case TUPLE_ARRAY_MAP:
			case MAP:
				populateSchemaForMapType(schema, property, schemaId);			
				break;
			case OBJECT:
			case INTERFACE:
				populateSchemaForObjectType(schema, property, schemaId);
				break;
			default:
				throw new IllegalArgumentException("Unsupported propertyType " + propertyType);
			}
			return schema;
		}
	}
	
	/**
	 * Populate the schema for the Array type
	 * 
	 * @param schema the schema being populated
	 * @param items the schema which represents each element in the array
	 * @param schemaId the id of the schema which contains the property
	 */
	void populateSchemaForArrayType(JsonSchema schema, ObjectSchema items, String schemaId) {
		ValidateArgument.required(schema, "schema");
		ValidateArgument.required(items, "items");
		ValidateArgument.required(schemaId, "schemaId");
		schema.setType(Type.array);
		JsonSchema itemsSchema = getPropertyAsJsonSchema(items, schemaId);
		schema.setItems(itemsSchema);
	}
	
	/**
	 * Populate the schema for the Map and Tuple_Array_Map types
	 * 
	 * @param schema the schema being populated
	 * @param property the property being looked at
	 * @param schemaId the id of the schema which contains the property
	 */
	void populateSchemaForMapType(JsonSchema schema, ObjectSchema property, String schemaId) {
		ValidateArgument.required(schema, "schema");
		ValidateArgument.required(property, "property");
		ValidateArgument.required(schemaId, "schemaId");
		schema.setType(Type.object);
		JsonSchema additionalProperty = getPropertyAsJsonSchema(property.getValue(), schemaId);
		schema.setAdditionalProperties(additionalProperty);
	}
	
	/**
	 * Get the JsonSchema representation of a property.
	 * 
	 * @param property the property to be translated
	 * @param schemaId the id of the original schema which contains this property
	 * @return the JsonSchema representation of the property
	 */
	JsonSchema getPropertyAsJsonSchema(ObjectSchema property, String schemaId) {
		ValidateArgument.required(property, "property");
		ValidateArgument.required(schemaId, "schemaId");
		if (isSelfReferencing(property)) {
			return generateReferenceSchema(schemaId);
		}
		return translateObjectSchemaPropertyToJsonSchema(property, schemaId);
	}
	
	/**
	 * Populate the schema for the Object and Interface types.
	 * 
	 * @param schema the schema being populated
	 * @param property the property being looked at
	 * @param schemaId the id of the schema which contains the property
	 */
	void populateSchemaForObjectType(JsonSchema schema, ObjectSchema property, String schemaId) {
		ValidateArgument.required(schema, "schema");
		ValidateArgument.required(property, "property");
		ValidateArgument.required(schemaId, "schemaId");
		String referenceId = isSelfReferencing(property) ? schemaId : property.getId();
		if (referenceId != null) {
			schema.set$ref(getPathInComponents(referenceId));
		}
	}
	
	/**
	 * Generates a JsonSchema that is a reference to schema with schemaId
	 * 
	 * @param schemaId the id of the schema
	 * @return a JsonSchema that is a reference to schemaId
	 */
	JsonSchema generateReferenceSchema(String schemaId) {
		ValidateArgument.required(schemaId, "schemaId");
		JsonSchema schema = new JsonSchema();
		schema.set$ref(getPathInComponents(schemaId));
		return schema;
	}

	/**
	 * Returns whether a property in an ObjectSchema is referencing the ObjectSchema itself.
	 * 
	 * @param property the property in question
	 * @return true if the property is referencing the original ObjectSchema, false otherwise.
	 */
	boolean isSelfReferencing(ObjectSchema property) {
		if (property.get$recursiveRef() == null) {
			return false;
		}
		return property.get$recursiveRef().equals("#");
	}

	/**
	 * Returns if the given TYPE is primitive.
	 * 
	 * @param type - the TYPE in question
	 * @return true if 'type' is primitive, false otherwise.
	 */
	boolean isPrimitive(TYPE type) {
		return type.equals(TYPE.BOOLEAN) || type.equals(TYPE.NUMBER) || type.equals(TYPE.STRING)
				|| type.equals(TYPE.INTEGER);
	}

	/**
	 * Inserts the oneOf properties into all of the JsonSchemas which represent
	 * interfaces.
	 * 
	 * @param classNameToJsonSchema mapping from class ids to a JsonSchema that
	 *                              represents that class.
	 * @param interfaces            the interfaces present in the application mapped
	 *                              to the list of classes that implement them.
	 */
	void insertOneOfPropertyForInterfaces(Map<String, JsonSchema> classNameToJsonSchema,
			Map<String, List<TypeReference>> interfaces) {
		for (String className : classNameToJsonSchema.keySet()) {
			if (interfaces.containsKey(className)) {
				Set<TypeReference> implementers = getImplementers(className, interfaces);
				List<JsonSchema> oneOf = new ArrayList<>();
				for (TypeReference implementer : implementers) {
					String id = implementer.getId();
					if (!classNameToJsonSchema.containsKey(id)) {
						throw new IllegalArgumentException(
								"Implementation of " + className + " interface with name " + id + " was not found.");
					}
					JsonSchema newSchema = new JsonSchema();
					newSchema.set$ref(getPathInComponents(id));
					oneOf.add(newSchema);
				}
				classNameToJsonSchema.get(className).setOneOf(oneOf);
			}
		}
	}

	/**
	 * Generated the path to a class name that exists in the "components" section of
	 * the OpenAPI specification.
	 * 
	 * @param className the className in question
	 * @return the path in the componenets section where the className exists.
	 */
	String getPathInComponents(String className) {
		ValidateArgument.required(className, "className");
		return "#/components/schemas/" + className;
	}

	/**
	 * Recursively gets all of the concrete implementers of the given interface.
	 * 
	 * @param interfaceId the id of the interface
	 * @param interfaces  a mapping between all interfaces and their implementers
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
		case STRING:
			return Type.string;
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
