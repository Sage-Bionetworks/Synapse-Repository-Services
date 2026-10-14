package org.sagebionetworks.repo.manager.grid.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

public class GridJsonUtilsTest {

	// ---------------------------------------------------------------------------
	// Overload 1: gridRowToJsonObject(List<String>, ConstantNode[])
	// ---------------------------------------------------------------------------

	@Test
	public void testGridRowToJsonObjectWithNodeArrayAndNullColumnNames() {
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> GridJsonUtils.gridRowToJsonObject(null, new ConstantNode[0]));
	}

	@Test
	public void testGridRowToJsonObjectWithNodeArrayAndNullNodes() {
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> GridJsonUtils.gridRowToJsonObject(List.of("col"), (ConstantNode[]) null));
	}

	@Test
	public void testGridRowToJsonObjectWithNodeArrayAndEmptyArray() {
		// call under test
		JSONObject result = GridJsonUtils.gridRowToJsonObject(List.of("col"), new ConstantNode[0]);

		assertEquals(0, result.length());
	}

	@Test
	public void testGridRowToJsonObjectWithNodeArrayAndDefinedValues() {
		List<String> columns = List.of("name", "age");
		ConstantNode[] nodes = { new ConstantNode().setValue("Alice"), new ConstantNode().setValue(42L) };

		// call under test
		JSONObject result = GridJsonUtils.gridRowToJsonObject(columns, nodes);

		assertEquals("Alice", result.get("name"));
		assertEquals(42L, result.get("age"));
	}

	@Test
	public void testGridRowToJsonObjectWithNodeArrayAndNullConValue() {
		List<String> columns = List.of("a", "b");
		ConstantNode[] nodes = { new ConstantNode().setValue("present"), new ConstantNode() }; // getConValue() returns null

		// call under test
		JSONObject result = GridJsonUtils.gridRowToJsonObject(columns, nodes);

		assertEquals("present", result.get("a"));
		assertEquals(1, result.length(), "column with null ConValue should be omitted");
	}

	@Test
	public void testGridRowToJsonObjectWithNodeArrayAndUndefinedConValue() {
		List<String> columns = List.of("a", "b");
		ConstantNode[] nodes = { new ConstantNode().setValue("defined"),
				new ConstantNode().setValue(new ConValue(ConType.UNDEFINED, null)) };

		// call under test
		JSONObject result = GridJsonUtils.gridRowToJsonObject(columns, nodes);

		assertEquals("defined", result.get("a"));
		assertEquals(1, result.length(), "undefined ConValue should be omitted");
	}

	@Test
	public void testGridRowToJsonObjectWithNodeArrayAndMissingEntry() {
		List<String> columns = List.of("a", "b", "c");
		ConstantNode[] nodes = { new ConstantNode().setValue("x"), null, new ConstantNode().setValue("z") };
		// index 1 ("b") is null


		// call under test
		JSONObject result = GridJsonUtils.gridRowToJsonObject(columns, nodes);

		assertEquals("x", result.get("a"));
		assertEquals("z", result.get("c"));
		assertEquals(2, result.length(), "column with a null entry should be omitted");
	}

	@Test
	public void testGridRowToJsonObjectWithNodeArrayShorterThanColumns() {
		List<String> columns = List.of("a", "b", "c");
		ConstantNode[] nodes = { new ConstantNode().setValue("x"), new ConstantNode().setValue("y") };
		// the array has no entry at all for "c"

		// call under test
		JSONObject result = GridJsonUtils.gridRowToJsonObject(columns, nodes);

		assertEquals("x", result.get("a"));
		assertEquals("y", result.get("b"));
		assertEquals(2, result.length(), "column past the end of the array should be omitted");
	}

	// ---------------------------------------------------------------------------
	// Overload 2: gridRowToJsonObject(List<String>, JSONArray)
	// ---------------------------------------------------------------------------

	@Test
	public void testGridRowToJsonObjectWithJsonArrayAndNullColumnNames() {
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> GridJsonUtils.gridRowToJsonObject(null, new JSONArray()));
	}

	@Test
	public void testGridRowToJsonObjectWithJsonArrayAndNullValues() {
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> GridJsonUtils.gridRowToJsonObject(List.of("col"), (JSONArray) null));
	}

	@Test
	public void testGridRowToJsonObjectWithJsonArrayAndMatchingLengths() {
		JSONArray values = new JSONArray();
		values.put("Alice");
		values.put(99);

		// call under test
		JSONObject result = GridJsonUtils.gridRowToJsonObject(List.of("name", "score"), values);

		assertEquals("Alice", result.get("name"));
		assertEquals(99, result.get("score"));
	}

	@Test
	public void testGridRowToJsonObjectWithJsonArrayAndMoreColumnsThanValues() {
		JSONArray values = new JSONArray();
		values.put("only");

		// call under test
		JSONObject result = GridJsonUtils.gridRowToJsonObject(List.of("a", "b"), values);

		assertEquals(1, result.length());
		assertEquals("only", result.get("a"));
	}

	@Test
	public void testGridRowToJsonObjectWithJsonArrayAndMoreValuesThanColumns() {
		JSONArray values = new JSONArray();
		values.put("v1");
		values.put("v2");
		values.put("v3");

		// call under test
		JSONObject result = GridJsonUtils.gridRowToJsonObject(List.of("a"), values);

		assertEquals(1, result.length());
		assertEquals("v1", result.get("a"));
	}

	@Test
	public void testGridRowToJsonObjectWithJsonArrayAndEmptyInputs() {
		// call under test
		JSONObject result = GridJsonUtils.gridRowToJsonObject(List.of(), new JSONArray());

		assertEquals(0, result.length());
	}

	// ---------------------------------------------------------------------------
	// Overload 3: gridRowToJsonObject(List<String>, Map<String, ConValue>)
	// ---------------------------------------------------------------------------

	@Test
	public void testGridCellsToJsonObjectWithNullColumnNames() {
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> GridJsonUtils.gridCellsToJsonObject(null, Map.of()));
	}

	@Test
	public void testGridCellsToJsonObjectWithNullCells() {
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> GridJsonUtils.gridCellsToJsonObject(List.of("col"), (Map<String, ConValue>) null));
	}

	@Test
	public void testGridCellsToJsonObjectWithDefinedValues() {
		List<String> columns = List.of("name", "age");
		Map<String, ConValue> cells = Map.of(
				"name", new ConValue(ConType.STRING, "Bob"),
				"age", new ConValue(ConType.LONG, 30L));

		// call under test
		JSONObject result = GridJsonUtils.gridCellsToJsonObject(columns, cells);

		assertEquals("Bob", result.get("name"));
		assertEquals(30L, result.get("age"));
	}

	@Test
	public void testGridCellsToJsonObjectWithNullConValueEntry() {
		List<String> columns = List.of("a", "b");
		Map<String, ConValue> cells = new java.util.HashMap<>();
		cells.put("a", new ConValue(ConType.STRING, "present"));
		cells.put("b", null); // null ConValue

		// call under test
		JSONObject result = GridJsonUtils.gridCellsToJsonObject(columns, cells);

		assertEquals("present", result.get("a"));
		assertEquals(1, result.length(), "null ConValue should be omitted");
	}

	@Test
	public void testGridCellsToJsonObjectWithUndefinedConValue() {
		List<String> columns = List.of("a", "b");
		Map<String, ConValue> cells = Map.of(
				"a", new ConValue(ConType.STRING, "defined"),
				"b", new ConValue(ConType.UNDEFINED, null));

		// call under test
		JSONObject result = GridJsonUtils.gridCellsToJsonObject(columns, cells);

		assertEquals("defined", result.get("a"));
		assertEquals(1, result.length(), "undefined ConValue should be omitted");
	}

	@Test
	public void testGridCellsToJsonObjectWithMissingColumn() {
		List<String> columns = List.of("a", "b", "c");
		Map<String, ConValue> cells = Map.of(
				"a", new ConValue(ConType.STRING, "x"),
				"c", new ConValue(ConType.STRING, "z"));
		// "b" is absent from the map

		// call under test
		JSONObject result = GridJsonUtils.gridCellsToJsonObject(columns, cells);

		assertEquals("x", result.get("a"));
		assertEquals("z", result.get("c"));
		assertEquals(2, result.length(), "column absent from map should be omitted");
	}

	@Test
	public void testGridCellsToJsonObjectWithColumnOrderFollowsOrderedColumnNames() {
		// JSONObject doesn't guarantee key order, but the output should contain
		// exactly the columns specified in orderedColumnNames (not extra map keys).
		List<String> columns = List.of("first", "second");
		Map<String, ConValue> cells = Map.of(
				"first", new ConValue(ConType.STRING, "1"),
				"second", new ConValue(ConType.STRING, "2"),
				"extra", new ConValue(ConType.STRING, "should-not-appear"));

		// call under test
		JSONObject result = GridJsonUtils.gridCellsToJsonObject(columns, cells);

		assertEquals(2, result.length());
		assertEquals("1", result.get("first"));
		assertEquals("2", result.get("second"));
	}

	@Test
	public void testGridCellsToJsonObjectWithEmptyColumnNames() {
		// call under test
		JSONObject result = GridJsonUtils.gridCellsToJsonObject(
				List.of(), Map.of("ignored", new ConValue(ConType.STRING, "v")));

		assertEquals(0, result.length());
	}
}
