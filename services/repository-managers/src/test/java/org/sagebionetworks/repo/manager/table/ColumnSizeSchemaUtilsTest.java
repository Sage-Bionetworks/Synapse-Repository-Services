package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.schema.JsonSchemaValidatorFactory;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;

public class ColumnSizeSchemaUtilsTest {

	@Test
	public void testGetColumnSizeSchema() {
		List<ColumnModel> columns = List.of(
				new ColumnModel().setName("aString").setColumnType(ColumnType.STRING).setMaximumSize(50L),
				new ColumnModel().setName("aLink").setColumnType(ColumnType.LINK).setMaximumSize(100L),
				new ColumnModel().setName("aList").setColumnType(ColumnType.STRING_LIST).setMaximumSize(10L)
						.setMaximumListLength(3L),
				new ColumnModel().setName("aListOfAnySize").setColumnType(ColumnType.STRING_LIST)
						.setMaximumListLength(4L),
				new ColumnModel().setName("unsized").setColumnType(ColumnType.STRING),
				new ColumnModel().setName("text").setColumnType(ColumnType.MEDIUMTEXT),
				new ColumnModel().setName("anInt").setColumnType(ColumnType.INTEGER));

		Map<String, JsonSchema> expectedProperties = new LinkedHashMap<>();
		expectedProperties.put("aString", new JsonSchema().setMaxLength(50L));
		expectedProperties.put("aLink", new JsonSchema().setMaxLength(100L));
		expectedProperties.put("aList", new JsonSchema().setMaxItems(3L).setItems(new JsonSchema().setMaxLength(10L)));
		expectedProperties.put("aListOfAnySize", new JsonSchema().setMaxItems(4L));

		// call under test
		assertEquals(Optional.of(new JsonSchema().setProperties(expectedProperties)),
				ColumnSizeSchemaUtils.getColumnSizeSchema(columns));
	}

	@Test
	public void testGetColumnSizeSchemaWithNoSizedColumns() {
		List<ColumnModel> columns = List.of(new ColumnModel().setName("anInt").setColumnType(ColumnType.INTEGER),
				new ColumnModel().setName("text").setColumnType(ColumnType.LARGETEXT));
		// call under test
		assertEquals(Optional.empty(), ColumnSizeSchemaUtils.getColumnSizeSchema(columns));
	}

	@Test
	public void testAddRules() {
		JsonSchema existing = new JsonSchema().setRequired(List.of("a"));
		JsonSchema schema = new JsonSchema().set$id("org-x.tpl-1.0.0").setAllOf(List.of(existing))
				.setDefinitions(Map.of("dep", new JsonSchema().setMaxLength(5L)));
		JsonSchema rules = new JsonSchema().setProperties(Map.of("a", new JsonSchema().setMaxLength(50L)));

		// call under test
		JsonSchema combined = ColumnSizeSchemaUtils.addRules(schema, rules);

		assertEquals(new JsonSchema().set$id("org-x.tpl-1.0.0").setAllOf(List.of(existing, rules))
				.setDefinitions(Map.of("dep", new JsonSchema().setMaxLength(5L))), combined);
		// the passed schema is unchanged
		assertEquals(List.of(existing), schema.getAllOf());
		assertNotSame(schema, combined);
	}

	@Test
	public void testAddRulesWithNoAllOf() {
		JsonSchema schema = new JsonSchema().set$id("org-x.tpl-1.0.0");
		JsonSchema rules = new JsonSchema().setProperties(Map.of("a", new JsonSchema().setMaxLength(50L)));
		// call under test
		assertEquals(new JsonSchema().set$id("org-x.tpl-1.0.0").setAllOf(List.of(rules)),
				ColumnSizeSchemaUtils.addRules(schema, rules));
	}

	/**
	 * A validation schema references its dependencies with '#/definitions/...'.
	 * The combined schema must still resolve them, and report violations of both
	 * schemas with the paths the grid client maps to cells.
	 */
	@Test
	public void testAddRulesValidatesWithDefinitionReferences() {
		JsonSchema schema = new JsonSchema().set$id("org-x.tpl-1.0.0")
				.setDefinitions(Map.of("dep", new JsonSchema().setMaxLength(5L)))
				.setProperties(Map.of("d", new JsonSchema().set$ref("#/definitions/dep")));
		JsonSchema rules = new JsonSchema().setProperties(Map.of("a", new JsonSchema().setMaxLength(50L)));
		Schema validator = new JsonSchemaValidatorFactory().buildValidator(ColumnSizeSchemaUtils.addRules(schema, rules),
				false);

		ValidationException exception = assertThrows(ValidationException.class,
				() -> validator.validate(new JSONObject().put("a", "x".repeat(51)).put("d", "toolong")));

		assertEquals(Set.of("#/a: expected maxLength: 50, actual: 51", "#/d: expected maxLength: 5, actual: 7"),
				Set.copyOf(exception.getAllMessages()));
	}

	/**
	 * A root $ref ignores its other keywords, so rules appended beside it would
	 * never be checked.
	 */
	@Test
	public void testAddRulesWithRootRef() {
		JsonSchema schema = new JsonSchema().set$id("org-x.tpl-1.0.0").set$ref("#/definitions/dep")
				.setDefinitions(Map.of("dep", new JsonSchema().setProperties(Map.of("d", new JsonSchema().setMaxLength(5L)))));
		JsonSchema rules = new JsonSchema().setProperties(Map.of("a", new JsonSchema().setMaxLength(50L)));
		Schema validator = new JsonSchemaValidatorFactory().buildValidator(ColumnSizeSchemaUtils.addRules(schema, rules),
				false);

		ValidationException exception = assertThrows(ValidationException.class,
				() -> validator.validate(new JSONObject().put("a", "x".repeat(51)).put("d", "toolong")));

		assertEquals(Set.of("#/a: expected maxLength: 50, actual: 51", "#/d: expected maxLength: 5, actual: 7"),
				Set.copyOf(exception.getAllMessages()));
		// the passed schema is unchanged
		assertEquals("#/definitions/dep", schema.get$ref());
	}
}
