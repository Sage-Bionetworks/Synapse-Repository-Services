package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;

@ExtendWith(MockitoExtension.class)
public class RecordSetSchemaResolverTest {

	@Mock
	private EntityManager mockEntityManager;
	@Mock
	private JsonSchemaManager mockJsonSchemaManager;

	@Spy
	@InjectMocks
	private RecordSetSchemaResolver resolver;

	private FileHandle fileHandle;
	private CsvTableDescriptor csvDescriptor;
	private String entityId;

	@BeforeEach
	public void before() {
		fileHandle = new S3FileHandle().setId("456");
		csvDescriptor = new CsvTableDescriptor().setIsFirstLineHeader(true);
		entityId = "syn123";
	}

	private void stubInferSchema(List<ColumnModel> columns) {
		doReturn(columns).when(resolver).inferSchemaFromCsv(any(FileHandle.class), any(CsvTableDescriptor.class));
	}

	private void stubBoundSchema(JsonSchema schema) {
		String schemaId = "my.org-Schema-1.0.0";
		JsonSchemaObjectBinding binding = new JsonSchemaObjectBinding()
				.setJsonSchemaVersionInfo(new JsonSchemaVersionInfo().set$id(schemaId));
		when(mockEntityManager.findBoundSchema(entityId)).thenReturn(Optional.of(binding));
		when(mockJsonSchemaManager.getValidationSchema(schemaId)).thenReturn(schema);
	}

	@Test
	public void testGetReconciledSchemaWithNoBoundSchema() {
		stubInferSchema(List.of(
				new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER),
				new ColumnModel().setName("b").setColumnType(ColumnType.STRING)));
		when(mockEntityManager.findBoundSchema(entityId)).thenReturn(Optional.empty());

		// call under test
		RecordSetSchemaResolver.ReconciledSchema result = resolver.getReconciledSchema(entityId, fileHandle,
				csvDescriptor);
		List<ColumnModel> schema = result.getSchema();

		assertEquals(2, schema.size());
		assertEquals("a", schema.get(0).getName());
		assertEquals(ColumnType.INTEGER, schema.get(0).getColumnType());
		assertEquals("b", schema.get(1).getName());
		assertEquals(ColumnType.STRING, schema.get(1).getColumnType());
		assertEquals(0, result.getRequiredColumnIndices().size());
	}

	@Test
	public void testGetReconciledSchemaUpgradesArrayColumnToList() {
		// The bound JSON Schema declares "tags" as an array, so the scalar STRING
		// inferred from the CSV must be upgraded to STRING_LIST. "a" stays scalar.
		stubInferSchema(List.of(
				new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER),
				new ColumnModel().setName("tags").setColumnType(ColumnType.STRING)));
		JsonSchema validationSchema = new JsonSchema().setProperties(Map.of(
				"tags", new JsonSchema().setType(Type.array)));
		stubBoundSchema(validationSchema);

		// call under test
		RecordSetSchemaResolver.ReconciledSchema result = resolver.getReconciledSchema(entityId, fileHandle,
				csvDescriptor);
		List<ColumnModel> schema = result.getSchema();

		assertEquals(2, schema.size());
		assertEquals("a", schema.get(0).getName());
		assertEquals(ColumnType.INTEGER, schema.get(0).getColumnType());
		assertEquals("tags", schema.get(1).getName());
		assertEquals(ColumnType.STRING_LIST, schema.get(1).getColumnType());
	}

	@Test
	public void testGetReconciledSchemaWithRequiredIndices() {
		stubInferSchema(List.of(
				new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER),
				new ColumnModel().setName("b").setColumnType(ColumnType.STRING),
				new ColumnModel().setName("c").setColumnType(ColumnType.INTEGER)));
		JsonSchema validationSchema = new JsonSchema()
				.setProperties(Map.of("a", new JsonSchema(), "c", new JsonSchema()))
				.setRequired(List.of("a", "c"));
		stubBoundSchema(validationSchema);

		// call under test
		RecordSetSchemaResolver.ReconciledSchema result = resolver.getReconciledSchema(entityId, fileHandle,
				csvDescriptor);

		assertEquals(List.of("a", "b", "c"),
				result.getSchema().stream().map(ColumnModel::getName).collect(Collectors.toList()));
		// "a" is index 0, "c" is index 2 in the inferred schema.
		assertEquals(List.of(0, 2), result.getRequiredColumnIndices());
	}

	@Test
	public void testGetReconciledSchemaWithRequiredIndicesAndNoBoundSchema() {
		stubInferSchema(List.of(
				new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER),
				new ColumnModel().setName("b").setColumnType(ColumnType.STRING)));
		when(mockEntityManager.findBoundSchema(entityId)).thenReturn(Optional.empty());

		// call under test
		RecordSetSchemaResolver.ReconciledSchema result = resolver.getReconciledSchema(entityId, fileHandle,
				csvDescriptor);

		assertEquals(2, result.getSchema().size());
		assertTrue(result.getRequiredColumnIndices().isEmpty());
	}

	@Test
	public void testToColumnModel() {
		assertEquals(ColumnType.INTEGER, RecordSetSchemaResolver.toColumnModel("i", new JsonSchema().setType(Type.integer)).getColumnType());
		assertEquals(ColumnType.DOUBLE, RecordSetSchemaResolver.toColumnModel("n", new JsonSchema().setType(Type.number)).getColumnType());
		assertEquals(ColumnType.BOOLEAN, RecordSetSchemaResolver.toColumnModel("b", new JsonSchema().setType(Type._boolean)).getColumnType());
		// An unconstrained string maps to MEDIUMTEXT.
		assertEquals(ColumnType.MEDIUMTEXT, RecordSetSchemaResolver.toColumnModel("s", new JsonSchema().setType(Type.string)).getColumnType());
		// A length-constrained string maps to a sized STRING.
		ColumnModel constrainedString = RecordSetSchemaResolver.toColumnModel("s", new JsonSchema().setType(Type.string).setMaxLength(50L));
		assertEquals(ColumnType.STRING, constrainedString.getColumnType());
		assertEquals(50L, constrainedString.getMaximumSize());
		// An array with no declared items defaults to MEDIUMTEXT.
		assertEquals(ColumnType.MEDIUMTEXT, RecordSetSchemaResolver.toColumnModel("arr", new JsonSchema().setType(Type.array)).getColumnType());
		// An object maps to JSON.
		assertEquals(ColumnType.JSON, RecordSetSchemaResolver.toColumnModel("o", new JsonSchema().setType(Type.object)).getColumnType());
		// An explicit "null" type maps to MEDIUMTEXT (distinct from a property with no type set).
		assertEquals(ColumnType.MEDIUMTEXT, RecordSetSchemaResolver.toColumnModel("nul", new JsonSchema().setType(Type._null)).getColumnType());
		// Arrays map by their element type. A length-constrained string element resolves to STRING -> STRING_LIST,
		// and the element's maximumSize is carried onto the list column.
		ColumnModel stringList = RecordSetSchemaResolver.toColumnModel("arr",
				new JsonSchema().setType(Type.array).setItems(new JsonSchema().setType(Type.string).setMaxLength(50L)));
		assertEquals(ColumnType.STRING_LIST, stringList.getColumnType());
		assertEquals(50L, stringList.getMaximumSize());
		assertEquals(ColumnType.INTEGER_LIST, RecordSetSchemaResolver.toColumnModel("arr",
				new JsonSchema().setType(Type.array).setItems(new JsonSchema().setType(Type.integer))).getColumnType());
		assertEquals(ColumnType.BOOLEAN_LIST, RecordSetSchemaResolver.toColumnModel("arr",
				new JsonSchema().setType(Type.array).setItems(new JsonSchema().setType(Type._boolean))).getColumnType());
		// An array whose element type has no list equivalent falls back to MEDIUMTEXT: an
		// unconstrained string element (-> MEDIUMTEXT) and a number element (-> DOUBLE) both do so.
		assertEquals(ColumnType.MEDIUMTEXT, RecordSetSchemaResolver.toColumnModel("arr",
				new JsonSchema().setType(Type.array).setItems(new JsonSchema().setType(Type.string))).getColumnType());
		assertEquals(ColumnType.MEDIUMTEXT, RecordSetSchemaResolver.toColumnModel("arr",
				new JsonSchema().setType(Type.array).setItems(new JsonSchema().setType(Type.number))).getColumnType());
		// An array of objects: the element resolves to JSON, which has no list equivalent -> MEDIUMTEXT.
		assertEquals(ColumnType.MEDIUMTEXT, RecordSetSchemaResolver.toColumnModel("arr",
				new JsonSchema().setType(Type.array).setItems(new JsonSchema().setType(Type.object))).getColumnType());
		// An array of arrays: the element resolves to a list type, which itself has no list equivalent -> MEDIUMTEXT.
		assertEquals(ColumnType.MEDIUMTEXT, RecordSetSchemaResolver.toColumnModel("arr",
				new JsonSchema().setType(Type.array).setItems(
						new JsonSchema().setType(Type.array).setItems(new JsonSchema().setType(Type.integer)))).getColumnType());
		assertEquals(ColumnType.MEDIUMTEXT, RecordSetSchemaResolver.toColumnModel("untyped", new JsonSchema()).getColumnType());
	}

	@Test
	public void testGetJsonSchemaColumns() {
		// A LinkedHashMap preserves insertion order so the derived columns are deterministic.
		Map<String, JsonSchema> properties = new LinkedHashMap<>();
		properties.put("a", new JsonSchema().setType(Type.integer));
		properties.put("b", new JsonSchema().setType(Type._boolean));
		JsonSchema validationSchema = new JsonSchema().setProperties(properties);

		// call under test
		List<ColumnModel> columns = RecordSetSchemaResolver.getJsonSchemaColumns(validationSchema);

		assertEquals(List.of(
				new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER),
				new ColumnModel().setName("b").setColumnType(ColumnType.BOOLEAN)), columns);
	}

	@Test
	public void testGetJsonSchemaColumnsWithNullSchema() {
		// call under test
		assertTrue(RecordSetSchemaResolver.getJsonSchemaColumns(null).isEmpty());
	}

	@Test
	public void testGetJsonSchemaColumnsWithNullProperties() {
		// call under test
		assertTrue(RecordSetSchemaResolver.getJsonSchemaColumns(new JsonSchema()).isEmpty());
	}

	@Test
	public void testGetJsonSchemaColumnsWithComposedSchema() {
		// Properties live behind an allOf + $ref, as produced by a validation schema.
		Map<String, JsonSchema> defProperties = new LinkedHashMap<>();
		defProperties.put("a", new JsonSchema().setType(Type.integer));
		defProperties.put("b", new JsonSchema().setType(Type._boolean));
		Map<String, JsonSchema> definitions = new LinkedHashMap<>();
		definitions.put("X", new JsonSchema().setProperties(defProperties));

		JsonSchema validationSchema = new JsonSchema()
				.setDefinitions(definitions)
				.setAllOf(List.of(new JsonSchema().set$ref("#/definitions/X")));

		// call under test
		List<ColumnModel> columns = RecordSetSchemaResolver.getJsonSchemaColumns(validationSchema);

		assertEquals(List.of(
				new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER),
				new ColumnModel().setName("b").setColumnType(ColumnType.BOOLEAN)), columns);
	}

	@Test
	public void testGetReconciledSchemaAddsSchemaOnlyColumn() {
		// The CSV has only "a"; the bound schema declares an additional property "c"
		// that is not in the CSV. "c" must be appended as a new column.
		stubInferSchema(List.of(new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER)));
		JsonSchema validationSchema = new JsonSchema()
				.setProperties(Map.of("c", new JsonSchema().setType(Type.integer)));
		stubBoundSchema(validationSchema);

		// call under test
		List<ColumnModel> schema = resolver.getReconciledSchema(entityId, fileHandle, csvDescriptor).getSchema();

		assertEquals(List.of("a", "c"),
				schema.stream().map(ColumnModel::getName).collect(Collectors.toList()));
		assertEquals(ColumnType.INTEGER, schema.get(1).getColumnType());
	}

	@Test
	public void testGetReconciledSchemaIncludesJsonSchemaPropertyNames() {
		// Both "a" (overlaps CSV) and "b" (schema-only) must appear in jsonSchemaColumnNames.
		stubInferSchema(List.of(new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER)));
		JsonSchema validationSchema = new JsonSchema().setProperties(Map.of(
				"a", new JsonSchema().setType(Type.integer),
				"b", new JsonSchema().setType(Type.string)));
		stubBoundSchema(validationSchema);

		// call under test
		RecordSetSchemaResolver.ReconciledSchema result = resolver.getReconciledSchema(entityId, fileHandle,
				csvDescriptor);

		List<String> names = result.getJsonSchemaColumnNames();
		assertEquals(2, names.size());
		assertTrue(names.containsAll(List.of("a", "b")));
	}

	@Test
	public void testGetReconciledSchemaWithNoBoundSchemaHasEmptyJsonSchemaColumnNames() {
		stubInferSchema(List.of(new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER)));
		when(mockEntityManager.findBoundSchema(entityId)).thenReturn(Optional.empty());

		// call under test
		RecordSetSchemaResolver.ReconciledSchema result = resolver.getReconciledSchema(entityId, fileHandle,
				csvDescriptor);

		assertTrue(result.getJsonSchemaColumnNames().isEmpty());
	}
}
