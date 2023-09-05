package org.sagebionetworks.table.query.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;

public class JsonTableTest {

	@Test
	public void testJsonTable() throws ParseException {
		ColumnReference columnReference = new TableQueryParser("T123._C123_").columnReference();
		List<JsonTableColumn> columns = List.of(
			new JsonTableColumn("foo", "VARCHAR(50)")
		);
		
		JsonTable jsonTable = new JsonTable(columnReference, columns, null);
		
		assertEquals("JSON_TABLE(T123._C123_, '$[*]' COLUMNS(foo VARCHAR(50) PATH '$' ERROR ON ERROR))", jsonTable.toSql());
	}
	
	@Test
	public void testJsonTableWithMultipleColumns() throws ParseException {
		ColumnReference columnReference = new TableQueryParser("T123._C123_").columnReference();
		List<JsonTableColumn> columns = List.of(
			new JsonTableColumn("foo", "VARCHAR(50)"),
			new JsonTableColumn("bar", "BIGINT")			
		);
		
		JsonTable jsonTable = new JsonTable(columnReference, columns, null);
		
		assertEquals("JSON_TABLE(T123._C123_, '$[*]' COLUMNS(foo VARCHAR(50) PATH '$' ERROR ON ERROR,bar BIGINT PATH '$' ERROR ON ERROR))", jsonTable.toSql());
	}
	
	@Test
	public void testJsonTableWithCorrelationSpec() throws ParseException {
		ColumnReference columnReference = new TableQueryParser("T123._C123_").columnReference();
		
		List<JsonTableColumn> columns = List.of(
			new JsonTableColumn("foo", "VARCHAR(50)")
		);
		
		CorrelationSpecification correlationSpec = new TableQueryParser("AS T456").correlationSpecification();
		
		JsonTable jsonTable = new JsonTable(columnReference, columns, correlationSpec);
		
		assertEquals("JSON_TABLE(T123._C123_, '$[*]' COLUMNS(foo VARCHAR(50) PATH '$' ERROR ON ERROR)) AS T456", jsonTable.toSql());
	}

}
