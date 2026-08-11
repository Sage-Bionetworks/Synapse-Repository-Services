package org.sagebionetworks.repo.manager.grid;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaProperties;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;

/**
 * Reconciles CSV-inferred column types with the bound JSON schema on a
 * RecordSet. When the JSON schema defines a property as {@code "type": "array"}
 * but the CSV inference detected a scalar type, the column type is upgraded to
 * its list equivalent (e.g. STRING to STRING_LIST).
 */
public class CsvSchemaReconciler {

	/**
	 * Reconcile the given CSV-inferred schema with the provided JSON schema. Each
	 * column whose name matches a JSON schema property of type
	 * {@link Type#array} is upgraded in-place from its scalar type to its list
	 * equivalent via {@link ColumnTypeListMappings}.
	 *
	 * @param csvSchema        The CSV-inferred column models to reconcile.
	 * @param validationSchema The bound JSON schema, may be null.
	 */
	public static void reconcile(List<ColumnModel> csvSchema, JsonSchema validationSchema) {
		if (validationSchema == null) {
			return;
		}
		Map<String, JsonSchema> properties = JsonSchemaProperties.collectTopLevelProperties(validationSchema);
		if (properties.isEmpty()) {
			return;
		}
		for (ColumnModel column : csvSchema) {
			JsonSchema property = properties.get(column.getName());
			if (property != null && Type.array.equals(property.getType())) {
				if (!ColumnTypeListMappings.isList(column.getColumnType())) {
					column.setColumnType(ColumnTypeListMappings.listType(column.getColumnType()));
				}
			}
		}
	}
}
