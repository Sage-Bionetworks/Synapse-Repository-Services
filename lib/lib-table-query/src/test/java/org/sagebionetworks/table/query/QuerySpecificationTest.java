package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SetQuantifier;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.table.query.util.SqlElementUtils;

public class QuerySpecificationTest {

	@Test
	public void testQuerySpecificationToSQL() throws ParseException {
		SetQuantifier setQuantifier = null;
		SelectList selectList = SqlElementUtils.createSelectList("one, two");
		TableExpression tableExpression = SqlElementUtils.createTableExpression("from syn123");
		QuerySpecification element = new QuerySpecification(setQuantifier, selectList, tableExpression);
		assertEquals("SELECT one, two FROM syn123", element.toString());
	}

	@Test
	public void testQuerySpecificationToSQLWithSetQuantifier() throws ParseException {
		SetQuantifier setQuantifier = SetQuantifier.DISTINCT;
		SelectList selectList = SqlElementUtils.createSelectList("one, two");
		TableExpression tableExpression = SqlElementUtils.createTableExpression("from syn123");
		QuerySpecification element = new QuerySpecification(setQuantifier, selectList, tableExpression);
		assertEquals("SELECT DISTINCT one, two FROM syn123", element.toString());
	}

	@Test
	public void testIsAggregateNoDistinct() throws ParseException {
		QuerySpecification element = new TableQueryParser("select * from syn123").querySpecification();
		assertFalse(element.isElementAggregate());
	}

	@Test
	public void testIsAggregateDistinct() throws ParseException {
		QuerySpecification element = new TableQueryParser("select distinct three, four from syn123")
				.querySpecification();
		assertTrue(element.isElementAggregate());
	}

	@Test
	public void testGetSingleTableName() throws ParseException {
		QuerySpecification element = new TableQueryParser("select distinct three, four from syn123")
				.querySpecification();
		assertEquals(Optional.of("syn123"), element.getSingleTableName());
	}
	
	@Test
	public void testGetSingleTableNameWithJoin() throws ParseException {
		QuerySpecification element = new TableQueryParser("select * from syn123 join syn456")
				.querySpecification();
		assertEquals(Optional.empty(), element.getSingleTableName());
	}

	@Test
	public void testGetChildren() throws ParseException {
		QuerySpecification element = new TableQueryParser("select bar, count(*) from syn123").querySpecification();
		assertEquals(Arrays.asList(element.getSelectList(), element.getTableExpression()), element.getChildren());
	}

	@Test
	public void testRecursiveSetParent() throws ParseException {
		// the constructor should call recursiveSetParent()
		QuerySpecification querySpec = new TableQueryParser("select bar, count(*) from syn123 where bar is not null")
				.querySpecification();
		assertNull(querySpec.getParent());
		assertNotNull(querySpec);
		WhereClause whereClause = querySpec.getFirstElementOfType(WhereClause.class);
		assertNotNull(whereClause);
		assertTrue(whereClause.getParent() instanceof TableExpression);
		assertEquals(querySpec, whereClause.getParent().getParent());
	}

	@Test
	public void testIsInContextNullParent() throws ParseException {
		QuerySpecification querySpec = new TableQueryParser("select bar, count(*) from syn123 where bar is not null")
				.querySpecification();
		assertNull(querySpec.getParent());
		assertFalse(querySpec.isInContext(Element.class));
	}

	@Test
	public void testIsInContextMultipleLevels() throws ParseException {
		QuerySpecification querySpec = new TableQueryParser("select bar, count(*) from syn123 where bar is not null")
				.querySpecification();
		WhereClause whereClause = querySpec.getFirstElementOfType(WhereClause.class);
		assertTrue(whereClause.isInContext(QuerySpecification.class));
	}
	
	@Test
	public void testWithoutTableExpression() throws ParseException {
		String message = assertThrows(ParseException.class, () -> {			
			new TableQueryParser("select *").querySpecification();
		}).getMessage();
		
		assertTrue(message.contains("Encountered \"<EOF>\" at line 1, column 8."));
		assertTrue(message.contains("\"FROM\""));	
	}

}
