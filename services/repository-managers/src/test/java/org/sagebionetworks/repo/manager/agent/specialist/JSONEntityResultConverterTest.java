package org.sagebionetworks.repo.manager.agent.specialist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.agent.TableDescription;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;

public class JSONEntityResultConverterTest {

	private JSONEntityResultConverter converter;

	@BeforeEach
	public void setup() {
		converter = new JSONEntityResultConverter();
	}

	@Test
	public void testConvertWithNull() {
		// call under test
		assertEquals("null", converter.convert(null, null));
	}

	@Test
	public void testConvertWithString() {
		// call under test
		assertEquals("hello", converter.convert("hello", String.class));
	}

	@Test
	public void testConvertWithJSONEntity() {
		ColumnModel col = new ColumnModel();
		col.setName("age");
		col.setColumnType(ColumnType.INTEGER);

		TableDescription description = new TableDescription()
				.setTableType("entityview")
				.setColumnModels(List.of(col));

		// call under test
		String json = converter.convert(description, TableDescription.class);

		assertTrue(json.contains("\"tableType\":\"entityview\""));
		assertTrue(json.contains("\"columnModels\""));
		assertTrue(json.contains("\"name\":\"age\""));
		assertTrue(json.contains("\"columnType\":\"INTEGER\""));
	}

	@Test
	public void testConvertWithToolResponse() {
		TableDescription description = new TableDescription()
				.setTableType("table");

		ToolResponse<TableDescription> response = new ToolResponse<>(description);

		// call under test
		String json = converter.convert(response, ToolResponse.class);

		assertTrue(json.contains("\"responseBody\""));
		assertTrue(json.contains("\"tableType\":\"table\""));
	}

	@Test
	public void testConvertWithToolResponseError() {
		ToolResponse<TableDescription> response = new ToolResponse<>("Something went wrong");

		// call under test
		String json = converter.convert(response, ToolResponse.class);

		assertTrue(json.contains("\"errorMessage\":\"Something went wrong\""));
	}
}
