package org.sagebionetworks.table.worker;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.util.TimeUtils;

public class CSVUtilsTest {
	
	@Test
	public void isFirstLineHeaderNull(){
		assertTrue(CSVUtils.isFirstRowHeader(null));
	}
	
	@Test
	public void isFirstLineHeaderDescriptor(){
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(null);
		assertTrue(CSVUtils.isFirstRowHeader(descriptor));
	}
	
	@Test
	public void isFirstLineHeaderDescriptorNotNullTrue(){
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(Boolean.TRUE);
		assertTrue(CSVUtils.isFirstRowHeader(descriptor));
	}
	
	@Test
	public void isFirstLineHeaderDescriptorNotNullFalse(){
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(Boolean.FALSE);
		assertFalse(CSVUtils.isFirstRowHeader(descriptor));
	}
	
	@Test
	public void testCheckTypeBoolean(){
		String in = "true";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.BOOLEAN, cm.getColumnType());
		// Should yield the same type
		ColumnModel back = CSVUtils.checkType(in, cm);
		assertEquals(cm, back);
	}

	@Test
	public void testCheckTypeBooleanFalse(){
		ColumnModel cm = CSVUtils.checkType("FALSE", null);
		assertNotNull(cm);
		assertEquals(ColumnType.BOOLEAN, cm.getColumnType());
	}
	
	@Test
	public void testCheckTypeDoubleNaN(){
		ColumnModel cm = CSVUtils.checkType(Double.toString(Double.NaN), null);
		assertNotNull(cm);
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
	}
	
	@Test
	public void testCheckTypeDoubleNagativeInfinity(){
		ColumnModel cm = CSVUtils.checkType(Double.toString(Double.NEGATIVE_INFINITY), null);
		assertNotNull(cm);
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
	}
	
	@Test
	public void testCheckTypeDoubleOne(){
		ColumnModel cm = CSVUtils.checkType(Double.toString(1.123), null);
		assertNotNull(cm);
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
	}
	
	@Test
	public void testCheckTypeDoubleTwo(){
		ColumnModel cm = CSVUtils.checkType("-1.3e16", null);
		assertNotNull(cm);
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
	}
	
	@Test
	public void testCheckTypeInteger(){
		ColumnModel cm = CSVUtils.checkType("123", null);
		assertNotNull(cm);
		assertEquals(ColumnType.INTEGER, cm.getColumnType());
	}
	
	@Test
	public void testCheckTypeIntegerNegative(){
		ColumnModel cm = CSVUtils.checkType("-123", null);
		assertNotNull(cm);
		assertEquals(ColumnType.INTEGER, cm.getColumnType());
	}
	
	@Test
	public void testCheckTypeDate(){
		ColumnModel cm = CSVUtils.checkType("2014-05-30 16:29:02.999", null);
		assertNotNull(cm);
		assertEquals(ColumnType.DATE, cm.getColumnType());
	}
	
	@Test
	public void testCheckTypeEntityId(){
		String in = "syn123";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.ENTITYID, cm.getColumnType());
		// Should yield the same type
		ColumnModel back = CSVUtils.checkType(in, cm);
		assertEquals(cm, back);
	}
	
	@Test
	public void testCheckTypeString(){
		String in = "a simple string";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(new Long(in.length()), cm.getMaximumSize());
		// Should yield the same type
		ColumnModel back = CSVUtils.checkType(in, cm);
		assertEquals(cm, back);
	}
	
	@Test
	public void testCheckTypeStringLonger(){
		String in = "a simple string";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(new Long(in.length()), cm.getMaximumSize());
		// If we give it a longer string then it should grow.
		String longer = in+"more";
		ColumnModel back = CSVUtils.checkType(longer, cm);
		assertEquals(ColumnType.STRING, back.getColumnType());
		assertEquals(new Long(longer.length()), back.getMaximumSize());
	}
	
	@Test
	public void testCheckTypeStringShorter(){
		String in = "a simple string";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(new Long(in.length()), cm.getMaximumSize());
		// If we give it a longer string then it should grow.
		String shorter = in.substring(0, in.length()-3);
		ColumnModel back = CSVUtils.checkType(shorter, cm);
		assertEquals(ColumnType.STRING, back.getColumnType());
		// this type the size should not shrink
		assertEquals(new Long(in.length()), back.getMaximumSize());
		assertEquals(cm, back);
	}
	
}
