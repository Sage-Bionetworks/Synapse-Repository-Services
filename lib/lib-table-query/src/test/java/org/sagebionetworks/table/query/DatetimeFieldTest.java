package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.table.query.model.DatetimeField;

public class DatetimeFieldTest {

	@Test
	public void testYear() throws ParseException{
		DatetimeField element = new TableQueryParser("year").datetimeField();
		assertEquals(DatetimeField.YEAR, element);
	}
	
	@Test
	public void testMonth() throws ParseException{
		DatetimeField element = new TableQueryParser("month").datetimeField();
		assertEquals(DatetimeField.MONTH, element);
	}
	
	@Test
	public void testDay() throws ParseException{
		DatetimeField element = new TableQueryParser("day").datetimeField();
		assertEquals(DatetimeField.DAY, element);
	}
	
	@Test
	public void testHour() throws ParseException{
		DatetimeField element = new TableQueryParser("hour").datetimeField();
		assertEquals(DatetimeField.HOUR, element);
	}

	@Test
	public void testSecond() throws ParseException{
		DatetimeField element = new TableQueryParser("second").datetimeField();
		assertEquals(DatetimeField.SECOND, element);
	}
}
