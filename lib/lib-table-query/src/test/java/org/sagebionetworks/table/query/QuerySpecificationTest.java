package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SetQuantifier;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class QuerySpecificationTest {
	
	@Test
	public void testQuerySpecificationToSQL() throws ParseException{
		SetQuantifier setQuantifier = null;
		SelectList selectList = SqlElementUntils.createSelectList("one, two");
		TableExpression tableExpression = SqlElementUntils.createTableExpression("from syn123");
		QuerySpecification element = new QuerySpecification(setQuantifier, selectList, tableExpression);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder);
		assertEquals("SELECT one, two FROM syn123", builder.toString());
	}
	
	@Test
	public void testQuerySpecificationToSQLWithSetQuantifier() throws ParseException{
		SetQuantifier setQuantifier = SetQuantifier.DISTINCT;
		SelectList selectList = SqlElementUntils.createSelectList("one, two");
		TableExpression tableExpression = SqlElementUntils.createTableExpression("from syn123");
		QuerySpecification element = new QuerySpecification(setQuantifier, selectList, tableExpression);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder);
		assertEquals("SELECT DISTINCT one, two FROM syn123", builder.toString());
	}


}
