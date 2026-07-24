package org.sagebionetworks.repo.manager.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;

public class CsvSchemaReconcilerTest {

	@Test
	public void testReconcileWithStringToStringList() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.STRING)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.array)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(ColumnType.STRING_LIST, csvSchema.get(0).getColumnType());
	}

	@Test
	public void testReconcileWithIntegerToIntegerList() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.INTEGER)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.array)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(ColumnType.INTEGER_LIST, csvSchema.get(0).getColumnType());
	}

	@Test
	public void testReconcileWithBooleanToBooleanList() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.BOOLEAN)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.array)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(ColumnType.BOOLEAN_LIST, csvSchema.get(0).getColumnType());
	}

	@Test
	public void testReconcileWithNoMatchingSchemaProperty() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.STRING)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"other_col", new JsonSchema().setType(Type.array)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(ColumnType.STRING, csvSchema.get(0).getColumnType());
	}

	@Test
	public void testReconcileWithNonArraySchemaProperty() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.STRING)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.string)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(ColumnType.STRING, csvSchema.get(0).getColumnType());
	}

	@Test
	public void testReconcileWithNullValidationSchema() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.STRING)
		);
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, null);
		assertEquals(ColumnType.STRING, csvSchema.get(0).getColumnType());
	}

	@Test
	public void testReconcileWithNullProperties() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.STRING)
		);
		JsonSchema validationSchema = new JsonSchema();
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(ColumnType.STRING, csvSchema.get(0).getColumnType());
	}

	@Test
	public void testReconcileWithAlreadyListType() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.STRING_LIST)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.array)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(ColumnType.STRING_LIST, csvSchema.get(0).getColumnType());
	}

	@Test
	public void testReconcileWithMultipleColumns() {
		Map<String, JsonSchema> properties = new HashMap<>();
		properties.put("string_col", new JsonSchema().setType(Type.string));
		properties.put("array_col", new JsonSchema().setType(Type.array));
		properties.put("int_array_col", new JsonSchema().setType(Type.array));

		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("string_col").setColumnType(ColumnType.STRING),
				new ColumnModel().setName("array_col").setColumnType(ColumnType.STRING),
				new ColumnModel().setName("int_array_col").setColumnType(ColumnType.INTEGER)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(properties);
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(ColumnType.STRING, csvSchema.get(0).getColumnType());
		assertEquals(ColumnType.STRING_LIST, csvSchema.get(1).getColumnType());
		assertEquals(ColumnType.INTEGER_LIST, csvSchema.get(2).getColumnType());
	}

	@Test
	public void testReconcileWithComposedSchema() {
		// the array property lives behind an allOf + $ref, as in a validation schema
		Map<String, JsonSchema> defProperties = Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.array));
		Map<String, JsonSchema> definitions = Collections.singletonMap(
				"X", new JsonSchema().setProperties(defProperties));

		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.STRING)
		);
		JsonSchema validationSchema = new JsonSchema()
				.setDefinitions(definitions)
				.setAllOf(Arrays.asList(new JsonSchema().set$ref("#/definitions/X")));

		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(ColumnType.STRING_LIST, csvSchema.get(0).getColumnType());
	}
}
