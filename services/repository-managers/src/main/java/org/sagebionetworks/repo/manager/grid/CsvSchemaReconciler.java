package org.sagebionetworks.repo.manager.grid;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaProperties;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;

/**
 * Determines how the values of a CSV are read into a grid, by reconciling the
 * types inferred from the CSV data with the bound JSON schema. Inference only
 * sees the rows of a single file, so it can land on a type that is more specific
 * than the type the schema declares (e.g. ENTITYID for a property defined as a
 * string). Where the two disagree the schema wins:
 * - A property of {@code "type": "array"} upgrades the column to the list
 *   equivalent of its element type (e.g. STRING to STRING_LIST).
 * - A property of {@code "type": "string"} replaces an inferred ENTITYID,
 *   INTEGER or other non-text type with STRING.
 * - A property of {@code "type": "integer"} replaces an inferred
 *   ENTITYID with INTEGER.
 * A column that does not match a top-level schema property keeps its inferred
 * type.
 * The result describes the shape of the data only. The limits a Synapse table
 * index imposes on a column type (such as the maximum size of a STRING) are not
 * considered here; a caller that also makes the CSV queryable through the table
 * services is responsible for capping the result to what that index can store.
 */
public class CsvSchemaReconciler {

	/**
	 * The inferred types that already carry a JSON schema {@code string} value
	 * without loss, and so are never replaced by a string property.
	 */
	private static final Set<ColumnType> TEXT_COLUMN_TYPES = Set.of(ColumnType.STRING, ColumnType.MEDIUMTEXT,
			ColumnType.LARGETEXT);

	/**
	 * Reconcile the given CSV-inferred schema with the provided JSON schema. Each
	 * column whose name matches a top-level JSON schema property is re-typed
	 * in-place to the type declared by that property.
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
			if (property == null) {
				continue;
			}
			if (Type.array.equals(property.getType())) {
				applyListType(column, property.getItems());
			} else {
				applyScalarType(column, property);
			}
		}
	}

	/**
	 * Re-type a column declared as an array. The element type is reconciled against
	 * the array's {@code items} first, so a column of entity ids declared as an
	 * array of strings lands on STRING_LIST rather than ENTITYID_LIST.
	 */
	private static void applyListType(ColumnModel column, JsonSchema items) {
		if (ColumnTypeListMappings.isList(column.getColumnType())) {
			column.setColumnType(ColumnTypeListMappings.nonListType(column.getColumnType()));
		}
		applyScalarType(column, items);
		try {
			column.setColumnType(ColumnTypeListMappings.listType(column.getColumnType()));
		} catch (IllegalArgumentException e) {
			// Types such as MEDIUMTEXT and DOUBLE have no list equivalent, so the raw JSON
			// array is kept as text.
			column.setColumnType(ColumnType.MEDIUMTEXT).setMaximumSize(null);
		}
	}

	/**
	 * Re-type a column declared as a scalar. Only the string and integer types are
	 * applied; for the remaining types the inferred type is either already
	 * equivalent (boolean, number) or cannot be narrowed to the declared type
	 * without losing the CSV values (object, null).
	 */
	private static void applyScalarType(ColumnModel column, JsonSchema property) {
		Type type = property == null ? null : property.getType();
		if (type == null) {
			return;
		}
		switch (type) {
			case string -> applyStringType(column, property.getMaxLength());
			case integer -> applyIntegerType(column);
			default -> {
				// the inferred type is kept as-is
			}
		}
	}

	/**
	 * A string property replaces any inferred type that is more specific than text.
	 * The declared {@code maxLength} rides along as the column size, unset when the
	 * property does not bound its length.
	 */
	private static void applyStringType(ColumnModel column, Long maxLength) {
		if (TEXT_COLUMN_TYPES.contains(column.getColumnType())) {
			return;
		}
		column.setColumnType(ColumnType.STRING).setMaximumSize(maxLength);
	}

	/**
	 * An integer property only replaces an inferred ENTITYID: the other inferred
	 * types either already hold integers or hold values that are not integers at
	 * all (e.g. a DATE inferred from date strings).
	 */
	private static void applyIntegerType(ColumnModel column) {
		if (ColumnType.ENTITYID.equals(column.getColumnType())) {
			column.setColumnType(ColumnType.INTEGER).setMaximumSize(null);
		}
	}
}
