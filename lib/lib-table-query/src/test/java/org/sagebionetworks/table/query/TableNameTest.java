package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.TableName;

public class TableNameTest {

	@Test
	public void testRegularExpression() throws ParseException {
		TableName name = new TableQueryParser("T123_456").tableName();
		assertEquals("T123_456", name.toSql());
	}

	@Test
	public void testEntityId() throws ParseException {
		TableName name = new TableQueryParser("syn4.5").tableName();
		assertEquals("syn4.5", name.toSql());
	}

	@Test
	public void testGetChildren() throws ParseException {
		TableName element = new TableQueryParser("syn4.5").tableName();
		assertEquals(Collections.singleton(element.getChild()), element.getChildren());
	}

}
