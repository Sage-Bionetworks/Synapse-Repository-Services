package org.sagebionetworks.repo.manager.schema;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Component;

@Component
public class SchemaTranslatorImp implements SchemaTranslator {

	public static final String SYNAPSE_ORGANIZATION_NAME = "org.sagebionetworks";


	@Override
	public ObjectSchemaImpl loadSchemaFromClasspath(String id) throws IOException, JSONObjectAdapterException {
		ValidateArgument.required(id, "id");
		String fileName = "schema/" + id.replaceAll("\\.", "/") + ".json";
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
		}
	}

	@Override
	public JsonSchema translate(ObjectSchema objectSchema) {
		ValidateArgument.required(objectSchema, "objectSchema");
		JsonSchema jsonSchema = new JsonSchema();
		jsonSchema.set$id(convertFromInternalIdToExternalId(objectSchema.getId()));
		// implements maps to allOf.
		jsonSchema.setAllOf(translateSchema(objectSchema.getImplements()));
		jsonSchema.set$ref(convertFromInternalIdToExternalId(objectSchema.getRef()));
		return jsonSchema;
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
		StringJoiner joiner = new StringJoiner("/");
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
	public List<JsonSchema> translateSchema(ObjectSchema[] array) {
		if (array == null) {
			return null;
		}
		List<JsonSchema> newList = new ArrayList<JsonSchema>(array.length);
		for (ObjectSchema sub : array) {
			newList.add(translate(sub));
		}
		return newList;
	}
}
