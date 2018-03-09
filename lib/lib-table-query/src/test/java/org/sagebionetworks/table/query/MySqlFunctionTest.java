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
	
	@Test
	public void testMicrosecond() throws ParseException{
		MySqlFunction element = new TableQueryParser("microsecond('12:00:00.123456')").mysqlFunction();
		assertEquals("MICROSECOND('12:00:00.123456')", element.toSql());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testSecond() throws ParseException{
		MySqlFunction element = new TableQueryParser("second('12:00:00.123456')").mysqlFunction();
		assertEquals("SECOND('12:00:00.123456')", element.toSql());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testMinute() throws ParseException{
		MySqlFunction element = new TableQueryParser("minute('12:00:00.123456')").mysqlFunction();
		assertEquals("MINUTE('12:00:00.123456')", element.toSql());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testHour() throws ParseException{
		MySqlFunction element = new TableQueryParser("hour('12:00:00.123456')").mysqlFunction();
		assertEquals("HOUR('12:00:00.123456')", element.toSql());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testDay() throws ParseException{
		MySqlFunction element = new TableQueryParser("day('12:00:00.123456')").mysqlFunction();
		assertEquals("DAY('12:00:00.123456')", element.toSql());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testWeek() throws ParseException{
		MySqlFunction element = new TableQueryParser("week('12:00:00.123456')").mysqlFunction();
		assertEquals("WEEK('12:00:00.123456')", element.toSql());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testMonth() throws ParseException{
		MySqlFunction element = new TableQueryParser("month('12:00:00.123456')").mysqlFunction();
		assertEquals("MONTH('12:00:00.123456')", element.toSql());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testQuarter() throws ParseException{
		MySqlFunction element = new TableQueryParser("quarter('12:00:00.123456')").mysqlFunction();
		assertEquals("QUARTER('12:00:00.123456')", element.toSql());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testYear() throws ParseException{
		MySqlFunction element = new TableQueryParser("year('12:00:00.123456')").mysqlFunction();
		assertEquals("YEAR('12:00:00.123456')", element.toSql());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testYearWeek() throws ParseException{
		MySqlFunction element = new TableQueryParser("yearweek('12:00:00.123456')").mysqlFunction();
		assertEquals("YEARWEEK('12:00:00.123456')", element.toSql());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testDate() throws ParseException{
		MySqlFunction element = new TableQueryParser("date('2003-12-31 01:02:03')").mysqlFunction();
		assertEquals("DATE('2003-12-31 01:02:03')", element.toSql());
		assertEquals(FunctionReturnType.STRING, element.getFunctionReturnType());
	}
	
	@Test
	public void testDayName() throws ParseException{
		MySqlFunction element = new TableQueryParser("dayname('2007-02-03')").mysqlFunction();
		assertEquals("DAYNAME('2007-02-03')", element.toSql());
		assertEquals(FunctionReturnType.STRING, element.getFunctionReturnType());
	}
	
	@Test
	public void testDayOfMonth() throws ParseException{
		MySqlFunction element = new TableQueryParser("dayofmonth('2007-02-03')").mysqlFunction();
		assertEquals("DAYOFMONTH('2007-02-03')", element.toSql());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testDayOfWeek() throws ParseException{
		MySqlFunction element = new TableQueryParser("dayofweek('2007-02-03')").mysqlFunction();
		assertEquals("DAYOFWEEK('2007-02-03')", element.toSql());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testDayOfYear() throws ParseException{
		MySqlFunction element = new TableQueryParser("dayofyear('2007-02-03')").mysqlFunction();
		assertEquals("DAYOFYEAR('2007-02-03')", element.toSql());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testMonthName() throws ParseException{
		MySqlFunction element = new TableQueryParser("monthname('2007-02-03')").mysqlFunction();
		assertEquals("MONTHNAME('2007-02-03')", element.toSql());
		assertEquals(FunctionReturnType.STRING, element.getFunctionReturnType());
	}
	
	@Test
	public void testWeekOfYear() throws ParseException{
		MySqlFunction element = new TableQueryParser("weekofyear('2007-02-03')").mysqlFunction();
		assertEquals("WEEKOFYEAR('2007-02-03')", element.toSql());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
}
