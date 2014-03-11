package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class SqlElementUntilsTest {
	
	@Test
	public void testConvertToCount() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select foo, bar from syn123 where foo = 1 order by bar limit 1 offset 2");
		QuerySpecification converted = SqlElementUntils.convertToCountQuery(model);
		assertNotNull(converted);
		StringBuilder builder = new StringBuilder();
		converted.toSQL(builder);
		assertEquals("SELECT COUNT(*) FROM syn123 WHERE foo = 1", builder.toString());
	}

}
