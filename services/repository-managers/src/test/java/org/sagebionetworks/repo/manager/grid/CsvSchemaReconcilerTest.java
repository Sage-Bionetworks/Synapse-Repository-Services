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
	public void testReconcileWithEntityIdToStringSchema() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.ENTITYID)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.string)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		// an unbounded string has no size to carry over
		assertEquals(new ColumnModel().setName("col1").setColumnType(ColumnType.STRING), csvSchema.get(0));
	}

	@Test
	public void testReconcileWithEntityIdToStringSchemaWithMaxLength() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.ENTITYID)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.string).setMaxLength(64L)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(new ColumnModel().setName("col1").setColumnType(ColumnType.STRING).setMaximumSize(64L),
				csvSchema.get(0));
	}

	@Test
	public void testReconcileWithEntityIdToStringSchemaWithLargeMaxLength() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.ENTITYID)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.string).setMaxLength(5000L)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		// the size a table index allows for a STRING is not a concern of the reconciler
		assertEquals(new ColumnModel().setName("col1").setColumnType(ColumnType.STRING).setMaximumSize(5000L),
				csvSchema.get(0));
	}

	@Test
	public void testReconcileWithEntityIdToIntegerSchema() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.ENTITYID)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.integer)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(new ColumnModel().setName("col1").setColumnType(ColumnType.INTEGER), csvSchema.get(0));
	}

	@Test
	public void testReconcileWithIntegerToStringSchema() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.INTEGER)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.string).setMaxLength(20L)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(new ColumnModel().setName("col1").setColumnType(ColumnType.STRING).setMaximumSize(20L),
				csvSchema.get(0));
	}

	@Test
	public void testReconcileWithDateToStringSchema() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.DATE)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.string)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(new ColumnModel().setName("col1").setColumnType(ColumnType.STRING), csvSchema.get(0));
	}

	@Test
	public void testReconcileWithStringSizeKeptForStringSchema() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.STRING).setMaximumSize(7L)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.string).setMaxLength(64L)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(new ColumnModel().setName("col1").setColumnType(ColumnType.STRING).setMaximumSize(7L),
				csvSchema.get(0));
	}

	@Test
	public void testReconcileWithMediumTextToStringSchema() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.MEDIUMTEXT)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.string).setMaxLength(64L)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(new ColumnModel().setName("col1").setColumnType(ColumnType.MEDIUMTEXT), csvSchema.get(0));
	}

	@Test
	public void testReconcileWithIntegerToNumberSchema() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.INTEGER)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.number)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(new ColumnModel().setName("col1").setColumnType(ColumnType.INTEGER), csvSchema.get(0));
	}

	@Test
	public void testReconcileWithStringToIntegerSchema() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.STRING).setMaximumSize(7L)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.integer)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(new ColumnModel().setName("col1").setColumnType(ColumnType.STRING).setMaximumSize(7L),
				csvSchema.get(0));
	}

	@Test
	public void testReconcileWithEntityIdToArrayOfStringSchema() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.ENTITYID)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.array)
						.setItems(new JsonSchema().setType(Type.string).setMaxLength(64L))
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(new ColumnModel().setName("col1").setColumnType(ColumnType.STRING_LIST).setMaximumSize(64L),
				csvSchema.get(0));
	}

	@Test
	public void testReconcileWithEntityIdToArrayOfUnboundedStringSchema() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.ENTITYID)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.array).setItems(new JsonSchema().setType(Type.string))
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(new ColumnModel().setName("col1").setColumnType(ColumnType.STRING_LIST), csvSchema.get(0));
	}

	@Test
	public void testReconcileWithIntegerListToArrayOfStringSchema() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.INTEGER_LIST)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.array)
						.setItems(new JsonSchema().setType(Type.string).setMaxLength(64L))
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(new ColumnModel().setName("col1").setColumnType(ColumnType.STRING_LIST).setMaximumSize(64L),
				csvSchema.get(0));
	}

	@Test
	public void testReconcileWithStringListToArrayOfStringSchema() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.STRING_LIST).setMaximumSize(7L)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.array)
						.setItems(new JsonSchema().setType(Type.string).setMaxLength(64L))
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(new ColumnModel().setName("col1").setColumnType(ColumnType.STRING_LIST).setMaximumSize(7L),
				csvSchema.get(0));
	}

	@Test
	public void testReconcileWithMediumTextToArraySchema() {
		List<ColumnModel> csvSchema = Arrays.asList(
				new ColumnModel().setName("col1").setColumnType(ColumnType.MEDIUMTEXT)
		);
		JsonSchema validationSchema = new JsonSchema().setProperties(Collections.singletonMap(
				"col1", new JsonSchema().setType(Type.array)
		));
		// call under test
		CsvSchemaReconciler.reconcile(csvSchema, validationSchema);
		assertEquals(new ColumnModel().setName("col1").setColumnType(ColumnType.MEDIUMTEXT), csvSchema.get(0));
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
