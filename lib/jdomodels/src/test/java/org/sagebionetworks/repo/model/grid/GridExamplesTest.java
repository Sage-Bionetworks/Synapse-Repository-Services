package org.sagebionetworks.repo.model.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.GridExamples.Example;
import org.sagebionetworks.repo.model.grid.query.QueryRequest;
import org.sagebionetworks.repo.model.grid.update.GridUpdateRequest;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class GridExamplesTest {

	@Test
	public void testGetQueryExamplesRoundTrip() {
		// call under test
		List<Example> examples = GridExamples.getQueryExamples();

		assertFalse(examples.isEmpty());
		for (Example example : examples) {
			assertNotNull(example.getDescription());
			assertFalse(example.getDescription().isBlank());
			// Each example must be clean nested JSON that parses back into a QueryRequest.
			QueryRequest parsed = JDOSecondaryPropertyUtils.createObjectFromJSON(QueryRequest.class, example.getJson());
			assertNotNull(parsed.getQuery(), "Example is missing a query: " + example.getDescription());
			// Verify the payload is nested (not a Bedrock-escaped string): "query" is an object.
			JSONObject json = new JSONObject(example.getJson());
			assertTrue(json.get("query") instanceof JSONObject,
					"Example query must be a nested object, not an escaped string: " + example.getDescription());
		}
	}

	@Test
	public void testGetUpdateExamplesRoundTrip() {
		// call under test
		List<Example> examples = GridExamples.getUpdateExamples();

		assertFalse(examples.isEmpty());
		for (Example example : examples) {
			assertNotNull(example.getDescription());
			assertFalse(example.getDescription().isBlank());
			GridUpdateRequest parsed = JDOSecondaryPropertyUtils.createObjectFromJSON(GridUpdateRequest.class,
					example.getJson());
			assertNotNull(parsed.getUpdate(), "Example is missing an update: " + example.getDescription());
			JSONObject json = new JSONObject(example.getJson());
			assertTrue(json.get("update") instanceof JSONObject,
					"Example update must be a nested object, not an escaped string: " + example.getDescription());
		}
	}

	@Test
	public void testUpdateExampleWithExplicitNullValue() {
		// The "footing = null" example must serialize an explicit JSON null for value, to
		// demonstrate the difference between an omitted value (undefined) and JSON null.
		Example nullValueExample = GridExamples.getUpdateExamples().stream()
				.filter(e -> e.getJson().contains("\"footing\"")).findFirst().orElseThrow();

		JSONObject setEntry = new JSONObject(nullValueExample.getJson()).getJSONObject("update").getJSONArray("batch")
				.getJSONObject(0).getJSONArray("set").getJSONObject(1);

		assertEquals("footing", setEntry.getString("columnName"));
		assertTrue(setEntry.has("value"), "The explicit-null example must include the value property");
		assertTrue(setEntry.isNull("value"), "The value property must be an explicit JSON null");
	}

	@Test
	public void testUpdateExampleWithOmittedValue() {
		// The "color = undefined" example must OMIT the value property entirely (undefined),
		// which is distinct from an explicit JSON null.
		Example omittedValueExample = GridExamples.getUpdateExamples().stream()
				.filter(e -> e.getDescription().contains("color to undefined")).findFirst().orElseThrow();

		JSONObject setEntry = new JSONObject(omittedValueExample.getJson()).getJSONObject("update").getJSONArray("batch")
				.getJSONObject(0).getJSONArray("set").getJSONObject(0);

		assertEquals("color", setEntry.getString("columnName"));
		assertFalse(setEntry.has("value"), "The undefined example must omit the value property entirely");
	}
}
