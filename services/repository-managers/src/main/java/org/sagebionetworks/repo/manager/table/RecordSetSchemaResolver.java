package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.file.CsvFileHandleProvider;
import org.sagebionetworks.repo.manager.grid.CsvSchemaReconciler;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaProperties;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.springframework.stereotype.Service;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Infers a {@link ColumnModel} schema from a RecordSet's CSV data file and
 * reconciles it with the RecordSet's bound JSON Schema (if any). This logic is
 * shared by the grid create flow ({@code RecordSetCreateGridHandler}) and the
 * RecordSetMetadataProvider, which binds the column schema on to the table index
 * upon create/update.
 */
@Service
public class RecordSetSchemaResolver {

	private final CsvFileHandleProvider csvFileHandleProvider;
	private final EntityManager entityManager;
	private final JsonSchemaManager jsonSchemaManager;

	public RecordSetSchemaResolver(CsvFileHandleProvider csvFileHandleProvider, EntityManager entityManager,
			JsonSchemaManager jsonSchemaManager) {
		this.csvFileHandleProvider = csvFileHandleProvider;
		this.entityManager = entityManager;
		this.jsonSchemaManager = jsonSchemaManager;
	}

	/**
	 * Result of {@link #getReconciledSchema}: the reconciled schema, the indices
	 * (into that schema) of the columns the bound JSON Schema marked as required,
	 * and the full set of top-level JSON Schema property names (including properties
	 * that also appear in the CSV).
	 */
	public static class ReconciledSchema {
		private final List<ColumnModel> schema;
		private final List<Integer> requiredColumnIndices;
		private final List<String> jsonSchemaColumnNames;

		public ReconciledSchema(List<ColumnModel> schema, List<Integer> requiredColumnIndices,
				List<String> jsonSchemaColumnNames) {
			this.schema = schema;
			this.requiredColumnIndices = requiredColumnIndices;
			this.jsonSchemaColumnNames = jsonSchemaColumnNames;
		}

		public List<ColumnModel> getSchema() {
			return schema;
		}

		public List<Integer> getRequiredColumnIndices() {
			return requiredColumnIndices;
		}

		/**
		 * All top-level property names declared in the bound JSON Schema, including
		 * properties that also appear as CSV columns. Empty when there is no bound
		 * schema.
		 */
		public List<String> getJsonSchemaColumnNames() {
			return jsonSchemaColumnNames;
		}
	}

	/**
	 * Infer the schema from the CSV file and reconcile it with the RecordSet's bound
	 * JSON Schema, upgrading scalar columns to list types where the JSON Schema
	 * declares an array.
	 *
	 * @param entityId      the RecordSet entity id, used to look up the bound schema
	 * @param fileHandle    the CSV data file handle
	 * @param csvDescriptor CSV parsing options
	 * @return the reconciled schema (never null, possibly empty)
	 */
	public ReconciledSchema getReconciledSchema(String entityId, FileHandle fileHandle,
			CsvTableDescriptor csvDescriptor) {
		List<ColumnModel> schema = new ArrayList<>(inferSchemaFromCsv(fileHandle, csvDescriptor));
		Optional<JsonSchema> validationSchema = getBoundValidationSchema(entityId);
		validationSchema.ifPresent(vs -> CsvSchemaReconciler.reconcile(schema, vs));
		validationSchema.ifPresent(vs -> addJsonSchemaOnlyColumns(schema, vs));

		List<String> required = validationSchema.map(JsonSchema::getRequired).orElse(new ArrayList<>());
		Map<String, Integer> columnNameToIndex = new HashMap<>();
		for (int i = 0; i < schema.size(); i++) {
			columnNameToIndex.put(schema.get(i).getName(), i);
		}
		List<Integer> requiredColumnIndices = required.stream()
				.map(columnNameToIndex::get)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		List<String> jsonSchemaColumnNames = validationSchema
				.map(RecordSetSchemaResolver::getJsonSchemaColumns)
				.orElse(Collections.emptyList())
				.stream()
				.map(ColumnModel::getName)
				.collect(Collectors.toList());

		return new ReconciledSchema(schema, requiredColumnIndices, jsonSchemaColumnNames);
	}

	public static List<ColumnModel> getJsonSchemaColumns(JsonSchema validationSchema) {
		return JsonSchemaProperties.collectTopLevelProperties(validationSchema)
				.entrySet()
				.stream()
				.map(e -> toColumnModel(e.getKey(), e.getValue()))
				.toList();
	}

    /**
     * Append a column for each top-level JSON Schema property that is not already
     * present in the CSV-inferred schema. This ensures that columns declared only
     * in the bound JSON Schema (not yet present in the CSV data) are surfaced. The
     * column type is derived from the property's declared {@link Type}; properties
     * are appended in a deterministic (name-sorted) order.
     *
     * @param schema           the schema to append to (CSV-inferred, already reconciled)
     * @param validationSchema the bound JSON Schema
     */
    static void addJsonSchemaOnlyColumns(List<ColumnModel> schema, JsonSchema validationSchema) {
        List<ColumnModel> properties = getJsonSchemaColumns(validationSchema);
        Set<String> existingNames = schema.stream().map(ColumnModel::getName).collect(Collectors.toSet());
        properties.stream()
                .filter(e -> !existingNames.contains(e.getName()))
                .sorted(Comparator.comparing(ColumnModel::getName))
                .forEach(schema::add);
    }

	/**
	 * Map a single JSON Schema property to a {@link ColumnModel}. Scalar types map
	 * to their column equivalents; length-constrained strings map to strings, arrays
	 * map to a string list; objects, nulls, untyped properties and non-length-constrained
	 * strings map to MEDIUMTEXT.
	 *
	 * @param name     the property (column) name
	 * @param property the property's JSON Schema
	 * @return a ColumnModel for the property
	 */
	static ColumnModel toColumnModel(String name, JsonSchema property) {
		ColumnModel column = new ColumnModel().setName(name);

		Type type = property == null ? null : property.getType();
		if (type == null) {
			return column.setColumnType(ColumnType.MEDIUMTEXT);
		}
		return switch (type) {
			case integer -> column.setColumnType(ColumnType.INTEGER);
			case number -> column.setColumnType(ColumnType.DOUBLE);
			case _boolean -> column.setColumnType(ColumnType.BOOLEAN);
			case object -> column.setColumnType(ColumnType.JSON);
			case array ->  {
				column = toColumnModel(name, property.getItems());
				try {
					column.setColumnType(ColumnTypeListMappings.listType(column.getColumnType()));
				} catch (IllegalArgumentException e) {
					column.setColumnType(ColumnType.MEDIUMTEXT);
				}
				yield column;
			}
			case string -> {
				if (property.getMaxLength() != null) {
					yield column.setColumnType(ColumnType.STRING).setMaximumSize(property.getMaxLength());
				}
				yield column.setColumnType(ColumnType.MEDIUMTEXT);
			}
			case _null -> column.setColumnType(ColumnType.MEDIUMTEXT);
		};
	}

	List<ColumnModel> inferSchemaFromCsv(FileHandle fileHandle, CsvTableDescriptor csvDescriptor) {
		try (CSVReader csvReader = csvFileHandleProvider.getCsvReader(fileHandle, csvDescriptor)) {
			UploadToTablePreviewRequest request = new UploadToTablePreviewRequest()
					.setCsvTableDescriptor(csvDescriptor)
					.setDoFullFileScan(true);
			UploadPreviewBuilder builder = new UploadPreviewBuilder(csvReader, request);
			List<ColumnModel> suggested = builder.buildResult().getSuggestedColumns();
			return suggested == null ? Collections.emptyList() : suggested;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public Optional<JsonSchema> getBoundValidationSchema(String entityId) {
		return entityManager.findBoundSchema(entityId)
				.map(binding -> binding.getJsonSchemaVersionInfo().get$id())
				.map(jsonSchemaManager::getValidationSchema);
	}

}
