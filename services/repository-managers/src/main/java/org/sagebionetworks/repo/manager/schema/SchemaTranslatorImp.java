package org.sagebionetworks.repo.manager.schema;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.FORMAT;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Component;

@Component
public class SchemaTranslatorImp implements SchemaTranslator {

	public static final String CURRENT_$SCHEMA = "http://json-schema.org/draft-07/schema#";

	public static final String SYNAPSE_ORGANIZATION_NAME = "org.sagebionetworks";

	@Override
	public ObjectSchemaImpl loadSchemaFromClasspath(String id) {
		ValidateArgument.required(id, "id");
		String fileName = "schema/" + id.replaceAll("\\.", DELIMITER) + ".json";
		try (InputStream input = SynapseSchemaBootstrapImpl.class.getClassLoader().getResourceAsStream(fileName);) {
			if (input == null) {
				throw new NotFoundException("Cannot find: '" + fileName + "' on the classpath");
			}
			StringWriter writer = new StringWriter();
			IOUtils.copy(input, writer, StandardCharsets.UTF_8);
			String jsonString = writer.toString();
			ObjectSchemaImpl schema = EntityFactory.createEntityFromJSONString(jsonString, ObjectSchemaImpl.class);
			if (schema.getId() == null) {
				schema.setId(id);
			}
			return schema;
		} catch (IOException | JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Convert from the ID used internally to the external $id. For example,
	 * 'org.sagebionetworks.repo.model.FileEntity' becomes
	 * 'org.sagebionetworks/repo.model.FileEntity'
	 * 
	 * @param id
	 * @return
	 */
	public String convertFromInternalIdToExternalId(String id) {
		if (id == null) {
			return null;
		}
		if (!id.startsWith(SYNAPSE_ORGANIZATION_NAME)) {
			throw new IllegalArgumentException("Id has an unknown organization name: '" + id + "'");
		}
		String schemaName = id.substring(SYNAPSE_ORGANIZATION_NAME.length() + 1);
		StringJoiner joiner = new StringJoiner(DELIMITER);
		joiner.add(SYNAPSE_ORGANIZATION_NAME);
		joiner.add(schemaName);
		return joiner.toString();
	}

	/**
	 * Translate an array of ObjectSchema to a list of JsonSchema
	 * 
	 * @param array
	 * @return
	 */
	public List<JsonSchema> translateArray(ObjectSchema[] array) {
		if (array == null) {
			return null;
		}
		List<JsonSchema> newList = new ArrayList<JsonSchema>(array.length);
		for (ObjectSchema sub : array) {
			newList.add(translate(sub));
		}
		return newList;
	}

	/**
	 * Convert the ObjectSchema types to JsonSchema types.
	 * @param type
	 * @return
	 */
	public Type translateType(TYPE type) {
		if (type == null) {
			return null;
		}
		switch (type) {
		case STRING:
			return Type.string;
		case INTEGER:
		case NUMBER:
			return Type.number;
		case ARRAY:
			return Type.array;
		case BOOLEAN:
			return Type._boolean;
		case NULL:
			return Type._null;
		case OBJECT:
		case INTERFACE:
			return Type.object;
		default:
			throw new IllegalArgumentException("There is no translation for type: '"+type.name()+"'");
		}
	}

	/**
	 * Translate from a map of ObjectSchema to a Map
	 * 
	 * @param inputMap
	 * @return
	 */
	public LinkedHashMap<String, JsonSchema> translateMap(Map<String, ObjectSchema> inputMap) {
		if (inputMap == null) {
			return null;
		}
		LinkedHashMap<String, JsonSchema> resultMap = new LinkedHashMap<String, JsonSchema>(inputMap.size());
		for (String key : inputMap.keySet()) {
			ObjectSchema objectSchema = inputMap.get(key);
			resultMap.put(key, translate(objectSchema));
		}
		return resultMap;
	}
	
	/**
	 * Translate from the ObjectSchema FORMAT.
	 * @param format
	 * @return
	 */
	public String translateFormat(FORMAT format) {
		if(format == null) {
			return null;
		}
		return format.getJSONValue();
	}
	
	@Override
	public JsonSchema translate(ObjectSchema objectSchema) {
		if(objectSchema == null) {
			return null;
		}
		JsonSchema jsonSchema = new JsonSchema();
		jsonSchema.set$schema(CURRENT_$SCHEMA);
		jsonSchema.set$id(convertFromInternalIdToExternalId(objectSchema.getId()));
		// implements maps to allOf.
		jsonSchema.setAllOf(translateArray(objectSchema.getImplements()));
		jsonSchema.set$ref(convertFromInternalIdToExternalId(objectSchema.getRef()));
		jsonSchema.setProperties(translateMap(objectSchema.getProperties()));
		jsonSchema.setType(translateType(objectSchema.getType()));
		jsonSchema.setTitle(objectSchema.getTitle());
		jsonSchema.setDescription(objectSchema.getDescription());
		jsonSchema.setItems(translate(objectSchema.getItems()));
		jsonSchema.setFormat(translateFormat(objectSchema.getFormat()));
		return jsonSchema;
	}
}
