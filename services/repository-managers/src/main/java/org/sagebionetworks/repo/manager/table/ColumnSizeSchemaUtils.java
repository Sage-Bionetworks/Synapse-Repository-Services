package org.sagebionetworks.repo.manager.table;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Translates the size limits of a table or view's columns into JSON Schema
 * rules, so values can be checked against them before they are written to a
 * source that the table or view must then hold. A value longer than its view
 * column fails the view build (PLFM-10141).
 */
public class ColumnSizeSchemaUtils {

	/**
	 * @param columns the columns of a table or view
	 * @return a schema with a property for each sized column: a STRING column
	 *         limits the length of its value; a STRING_LIST column limits the
	 *         number of items and the length of each. Empty when no column is
	 *         sized.
	 */
	public static Optional<JsonSchema> getColumnSizeSchema(List<ColumnModel> columns) {
		ValidateArgument.required(columns, "columns");
		Map<String, JsonSchema> properties = new LinkedHashMap<>();
		for (ColumnModel column : columns) {
			if (ColumnType.STRING.equals(column.getColumnType()) && column.getMaximumSize() != null) {
				properties.put(column.getName(), new JsonSchema().setMaxLength(column.getMaximumSize()));
			} else if (ColumnType.STRING_LIST.equals(column.getColumnType())
					&& (column.getMaximumSize() != null || column.getMaximumListLength() != null)) {
				JsonSchema property = new JsonSchema().setMaxItems(column.getMaximumListLength());
				if (column.getMaximumSize() != null) {
					property.setItems(new JsonSchema().setMaxLength(column.getMaximumSize()));
				}
				properties.put(column.getName(), property);
			}
		}
		return properties.isEmpty() ? Optional.empty() : Optional.of(new JsonSchema().setProperties(properties));
	}

	/**
	 * Combine a schema with additional rules so a subject must satisfy both.
	 * <p>
	 * The rules are appended to a copy of the schema's own allOf rather than
	 * wrapping both in a new schema: a validation schema references its
	 * dependencies with '#/definitions/...', which only resolve while the schema
	 * stays at the root.
	 *
	 * @param schema the schema to extend; not modified
	 * @param rules  the rules the subject must also satisfy
	 * @return a copy of the schema that also requires the rules
	 */
	public static JsonSchema addRules(JsonSchema schema, JsonSchema rules) {
		ValidateArgument.required(schema, "schema");
		ValidateArgument.required(rules, "rules");
		JsonSchema combined = copy(schema);
		List<JsonSchema> allOf = combined.getAllOf() == null ? new ArrayList<>() : new ArrayList<>(combined.getAllOf());
		allOf.add(rules);
		return combined.setAllOf(allOf);
	}

	static JsonSchema copy(JsonSchema schema) {
		try {
			return EntityFactory.createEntityFromJSONObject(EntityFactory.createJSONObjectForEntity(schema),
					JsonSchema.class);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
	}
}
