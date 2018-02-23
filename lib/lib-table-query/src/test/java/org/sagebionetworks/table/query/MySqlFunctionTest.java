package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.table.query.model.FunctionReturnType;
import org.sagebionetworks.table.query.model.MySqlFunction;

public class MySqlFunctionTest {

	@Test
	public void testNow() throws ParseException{
		MySqlFunction element = new TableQueryParser("now()").mysqlFunction();
		assertEquals("NOW()", element.toSql());
		assertEquals(FunctionReturnType.STRING, element.getFunctionReturnType());
	}
	
	@Test
	public void testCurrentTimestampNoParameters() throws ParseException{
		MySqlFunction element = new TableQueryParser("current_timestamp").mysqlFunction();
		assertEquals("CURRENT_TIMESTAMP", element.toSql());
		assertEquals(FunctionReturnType.STRING, element.getFunctionReturnType());
	}
	
	@Test
	public void testCurrentTimestampWithParameters() throws ParseException{
		MySqlFunction element = new TableQueryParser("current_timestamp(3)").mysqlFunction();
		assertEquals("CURRENT_TIMESTAMP(3)", element.toSql());
	}
	
	@Test
	public void testCurrentDate() throws ParseException{
		MySqlFunction element = new TableQueryParser("current_date").mysqlFunction();
		assertEquals("CURRENT_DATE", element.toSql());
		assertEquals(FunctionReturnType.STRING, element.getFunctionReturnType());
	}
	
	@Test
	public void testCurrenTime() throws ParseException{
		MySqlFunction element = new TableQueryParser("current_time").mysqlFunction();
		assertEquals("CURRENT_TIME", element.toSql());
		assertEquals(FunctionReturnType.STRING, element.getFunctionReturnType());
	}
	
	@Test
	public void testUnixTimestamp() throws ParseException{
		MySqlFunction element = new TableQueryParser("unix_timestamp('2015-11-13 10:20:19')").mysqlFunction();
		assertEquals("UNIX_TIMESTAMP('2015-11-13 10:20:19')", element.toSql());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testFromUnixTime() throws ParseException{
		MySqlFunction element = new TableQueryParser("from_unixtime(1111885200)").mysqlFunction();
		assertEquals("FROM_UNIXTIME(1111885200)", element.toSql());
		assertEquals(FunctionReturnType.STRING, element.getFunctionReturnType());
	}
	
	@Test
	public void testFromUnixTimeMultipleParameters() throws ParseException{
		MySqlFunction element = new TableQueryParser("from_unixtime(1111885200, '%Y %D %M %h:%i:%s %x')").mysqlFunction();
		assertEquals("FROM_UNIXTIME(1111885200,'%Y %D %M %h:%i:%s %x')", element.toSql());
		assertEquals(FunctionReturnType.STRING, element.getFunctionReturnType());
	}
	
	@Test
	public void testConcat() throws ParseException{
		MySqlFunction element = new TableQueryParser("concat('a','b','c')").mysqlFunction();
		assertEquals("CONCAT('a','b','c')", element.toSql());
		assertEquals(FunctionReturnType.STRING, element.getFunctionReturnType());
	}
	
	@Test
	public void testReplace() throws ParseException{
		MySqlFunction element = new TableQueryParser("replace('a','b')").mysqlFunction();
		assertEquals("REPLACE('a','b')", element.toSql());
		assertEquals(FunctionReturnType.STRING, element.getFunctionReturnType());
	}
	
	@Test
	public void testUpper() throws ParseException{
		MySqlFunction element = new TableQueryParser("upper('ab')").mysqlFunction();
		assertEquals("UPPER('ab')", element.toSql());
		assertEquals(FunctionReturnType.STRING, element.getFunctionReturnType());
	}
	
	@Test
	public void testLower() throws ParseException{
		MySqlFunction element = new TableQueryParser("lower('AB')").mysqlFunction();
		assertEquals("LOWER('AB')", element.toSql());
		assertEquals(FunctionReturnType.STRING, element.getFunctionName().getFunctionReturnType());
	}
	
	@Test
	public void testTrim() throws ParseException{
		MySqlFunction element = new TableQueryParser("trim(' AB ')").mysqlFunction();
		assertEquals("TRIM(' AB ')", element.toSql());
		assertEquals(FunctionReturnType.STRING, element.getFunctionName().getFunctionReturnType());
	}
	
}
