package org.sagebionetworks.web.unitshared;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.web.shared.WhereCondition;
import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;

public class WhereConditionTest {
	
	@Test
	public void testBasicToSQL(){
		String id = "someId";
		String inValue = "Not A Number Dude!";
		String whiteSpace = "+";
		WhereCondition condition = new WhereCondition(id, WhereOperator.NOT_EQUALS, inValue);
		String sqlValue = condition.toSql(whiteSpace);
		assertNotNull(sqlValue);
		// Split the string
		String[] split = sqlValue.split("\\+");
		assertEquals(3, split.length);
		assertEquals(id, split[0]);
		assertEquals(WhereOperator.NOT_EQUALS.toSql(), split[1]);
		assertEquals("\""+inValue+"\"", split[2]);
	}
	
	@Test
	public void testIsANumber(){
		assertTrue(WhereCondition.isNumber("1230"));
		assertFalse(WhereCondition.isNumber("Not a number"));
		assertTrue(WhereCondition.isNumber(new Double("-1.56e-3").toString()));
		assertFalse(WhereCondition.isNumber(null));
	}
	
	@Test
	public void testValueLongToSQL(){
		String id = "someId";
		String inValue = "1011";
		String whiteSpace = "+";
		WhereCondition condition = new WhereCondition(id, WhereOperator.GREATER_THAN, inValue);
		String sqlValue = condition.toSql(whiteSpace);
		assertNotNull(sqlValue);
		// Split the string
		String[] split = sqlValue.split("\\+");
		assertEquals(3, split.length);
		assertEquals(id, split[0]);
		assertEquals(WhereOperator.GREATER_THAN.toSql(), split[1]);
		// Since the value is a number it should not be in quotes
		assertEquals(inValue, split[2]);
	}
	
	@Test
	public void testMultipeWhere(){
		List<WhereCondition> list = new ArrayList<WhereCondition>();
		list.add(new WhereCondition("id1", WhereOperator.GREATER_THAN_OR_EQUALS, "970.0"));
		list.add(new WhereCondition("id2", WhereOperator.EQUALS, "Some String"));
		String sqlValue = WhereCondition.toSql(list, "+");
		assertNotNull(sqlValue);
		// Split the string
		String[] split = sqlValue.split("\\+");
		assertEquals(7, split.length);
		assertEquals("and", split[3]);
	}
	
	@Test
	public void testMultipeWhereOneValue(){
		List<WhereCondition> list = new ArrayList<WhereCondition>();
		list.add(new WhereCondition("id1", WhereOperator.GREATER_THAN_OR_EQUALS, "970.0"));
		String sqlValue = WhereCondition.toSql(list, "+");
		assertNotNull(sqlValue);
		// Split the string
		String[] split = sqlValue.split("\\+");
		assertEquals(3, split.length);
	}

}
