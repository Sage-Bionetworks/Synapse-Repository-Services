package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.Context;
import org.sagebionetworks.repo.model.grid.query.CellValueFilter;
import org.sagebionetworks.repo.model.grid.query.CellValueOperator;

public class CellValueFilterElementTest {

	private static Context contextWithColumn(String columnName) {
		return new Context(new GridHeader().setOrderedColumns(List.of(new Column().setName(columnName))));
	}

	/**
	 * This is case that failed for PLFM-9309.
	 */
	@Test
	public void testTranslateWithNullValue() {
		CellValueFilter toClone = new CellValueFilter().setColumnName("c1").setOperator(CellValueOperator.IS_NOT_NULL);
		// call under test
		CellValueFilterElement clone = new CellValueFilterElement(toClone);

		assertEquals(new CellValueFilterElement().setColumnName("c1").setOperator(CellValueOperatorElement.IS_NOT_NULL),
				clone);
	}

	@Test
	public void testTranslate() {
		CellValueFilter toClone = new CellValueFilter().setColumnName("c1").setOperator(CellValueOperator.EQUALS)
				.setValue("one");
		// call under test
		CellValueFilterElement clone = new CellValueFilterElement(toClone);

		assertEquals(new CellValueFilterElement().setColumnName("c1").setOperator(CellValueOperatorElement.EQUALS)
				.setValue("one"), clone);
	}

	@Test
	public void testTranslateWithNullFilter() {
		CellValueFilter toClone = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CellValueFilterElement(toClone);
		}).getMessage();
		assertEquals("filter is required.", message);

	}

	@Test
	public void testTranslateWithNullOperator() {
		CellValueFilter toClone = new CellValueFilter().setColumnName("c1").setOperator(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CellValueFilterElement(toClone);
		}).getMessage();
		assertEquals("filter.operator is required.", message);
	}

	@Test
	public void testToSqlWithEqualsScalarValue() {
		CellValueFilterElement element = new CellValueFilterElement(
				new CellValueFilter().setColumnName("c1").setOperator(CellValueOperator.EQUALS).setValue("A"));
		StringBuilder sqlBuilder = new StringBuilder();
		Map<String, Object> params = new HashMap<>();

		// call under test
		element.toSql(sqlBuilder, params, contextWithColumn("c1"));

		assertEquals("( JSON_LENGTH(VALS, '$[0].v') = 1 AND VALS->>'$[0].v[0]' = :val0)", sqlBuilder.toString());
		assertEquals(Map.of("val0", "A"), params);
	}

	/**
	 * PLFM-9831: a value is matched strictly by its JSON type. A single-element array value is a genuine
	 * JSON array, so EQUALS compares it against a LIST cell (CAST to JSON) and does not match a scalar
	 * cell. To match a scalar cell the caller sends a scalar value instead.
	 */
	@Test
	public void testToSqlWithEqualsSingleElementArrayValue() {
		CellValueFilterElement element = new CellValueFilterElement(new CellValueFilter().setColumnName("c1")
				.setOperator(CellValueOperator.EQUALS).setValue(new JSONArray(List.of("A"))));
		StringBuilder sqlBuilder = new StringBuilder();
		Map<String, Object> params = new HashMap<>();

		// call under test
		element.toSql(sqlBuilder, params, contextWithColumn("c1"));

		assertEquals("( JSON_LENGTH(VALS, '$[0].v') = 1 AND VALS->'$[0].v[0]' = CAST(:val0 AS JSON))",
				sqlBuilder.toString());
		assertEquals(Map.of("val0", "[\"A\"]"), params);
	}

	/**
	 * NOT_EQUALS with a single-element array value is matched by type the same way as EQUALS: the cell
	 * differs when it is not the JSON array [A] (a scalar cell inherently differs from the array).
	 */
	@Test
	public void testToSqlWithNotEqualsSingleElementArrayValue() {
		CellValueFilterElement element = new CellValueFilterElement(new CellValueFilter().setColumnName("c1")
				.setOperator(CellValueOperator.NOT_EQUALS).setValue(new JSONArray(List.of("A"))));
		StringBuilder sqlBuilder = new StringBuilder();
		Map<String, Object> params = new HashMap<>();

		// call under test
		element.toSql(sqlBuilder, params, contextWithColumn("c1"));

		assertEquals("( JSON_LENGTH(VALS, '$[0].v') != 1 OR VALS->'$[0].v[0]' <> CAST(:val0 AS JSON))",
				sqlBuilder.toString());
		assertEquals(Map.of("val0", "[\"A\"]"), params);
	}

	/**
	 * For ordering and text operators, comparing against a JSON array is not meaningful, so a
	 * single-element array value is unwrapped to its scalar element.
	 */
	@Test
	public void testToSqlWithLikeSingleElementArrayValue() {
		CellValueFilterElement element = new CellValueFilterElement(new CellValueFilter().setColumnName("c1")
				.setOperator(CellValueOperator.LIKE).setValue(new JSONArray(List.of("A%"))));
		StringBuilder sqlBuilder = new StringBuilder();
		Map<String, Object> params = new HashMap<>();

		// call under test
		element.toSql(sqlBuilder, params, contextWithColumn("c1"));

		assertEquals("( JSON_LENGTH(VALS, '$[0].v') = 1 AND VALS->>'$[0].v[0]' LIKE :val0)", sqlBuilder.toString());
		assertEquals(Map.of("val0", "A%"), params);
	}

	/**
	 * PLFM-9831: EQUALS with a multi-element array must still compare against the whole JSON array
	 * (matching a LIST cell that stores that exact array), so the value is bound as a CAST to JSON.
	 */
	@Test
	public void testToSqlWithEqualsMultiElementArrayValue() {
		CellValueFilterElement element = new CellValueFilterElement(new CellValueFilter().setColumnName("c1")
				.setOperator(CellValueOperator.EQUALS).setValue(new JSONArray(List.of("A", "B"))));
		StringBuilder sqlBuilder = new StringBuilder();
		Map<String, Object> params = new HashMap<>();

		// call under test
		element.toSql(sqlBuilder, params, contextWithColumn("c1"));

		assertEquals("( JSON_LENGTH(VALS, '$[0].v') = 1 AND VALS->'$[0].v[0]' = CAST(:val0 AS JSON))",
				sqlBuilder.toString());
		assertEquals(Map.of("val0", "[\"A\",\"B\"]"), params);
	}

}
