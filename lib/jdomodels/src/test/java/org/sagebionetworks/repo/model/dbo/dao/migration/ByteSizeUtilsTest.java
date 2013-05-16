package org.sagebionetworks.repo.model.dbo.dao.migration;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.migration.ByteSizeUtils;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;

public class ByteSizeUtilsTest {
		
	@Test
	public void testEstimateSizeInBytesLong() throws Exception{
		AllKinds object = new AllKinds();
		object.setLongField(1l);
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(object);
		int expected = Long.SIZE;
		// Get the size of this object
		int size = ByteSizeUtils.estimateSizeInBytes(source);
		assertEquals(expected, size);
	}
	
	@Test
	public void testEstimateSizeInBytesDouble() throws Exception{
		AllKinds object = new AllKinds();
		object.setDoubleField(123.556);
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(object);
		int expected = Double.SIZE;
		// Get the size of this object
		int size = ByteSizeUtils.estimateSizeInBytes(source);
		assertEquals(expected, size);
	}
	
	@Test
	public void testEstimateSizeInBytesString() throws Exception{
		AllKinds object = new AllKinds();
		object.setStringField("abc");
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(object);
		int expected = 3*Character.SIZE;
		// Get the size of this object
		int size = ByteSizeUtils.estimateSizeInBytes(source);
		assertEquals(expected, size);
	}

	@Test
	public void testEstimateSizeInBytesByteArray() throws Exception{
		AllKinds object = new AllKinds();
		object.setByteArrayField(new byte[]{1,2,3,4});
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(object);
		int expected = 4;
		// Get the size of this object
		int size = ByteSizeUtils.estimateSizeInBytes(source);
		assertEquals(expected, size);
	}
	
	@Test
	public void testEstimateSizeInBytesDate() throws Exception{
		AllKinds object = new AllKinds();
		object.setDateField(new Date());
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(object);
		int expected = Long.SIZE;
		// Get the size of this object
		int size = ByteSizeUtils.estimateSizeInBytes(source);
		assertEquals(expected, size);
	}
	
	@Test
	public void testEstimateSizeInBytesBoolean() throws Exception{
		AllKinds object = new AllKinds();
		object.setBooleanField(Boolean.TRUE);
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(object);
		int expected = 1;
		// Get the size of this object
		int size = ByteSizeUtils.estimateSizeInBytes(source);
		assertEquals(expected, size);
	}
	
	@Test
	public void testEstimateSizeInBytesTimestamp() throws Exception{
		AllKinds object = new AllKinds();
		object.setTimestampField(new Timestamp(1l));
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(object);
		int expected = Long.SIZE;
		// Get the size of this object
		int size = ByteSizeUtils.estimateSizeInBytes(source);
		assertEquals(expected, size);
	}
	
	@Test
	public void testEstimateSizeInBytesEnum() throws Exception{
		AllKinds object = new AllKinds();
		object.setEnumfield(SomeEnum.BAR);
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(object);
		int expected = 3*Character.SIZE;
		// Get the size of this object
		int size = ByteSizeUtils.estimateSizeInBytes(source);
		assertEquals(expected, size);
	}
	
	@Test
	public void testEstimateSizeInBytesShort() throws Exception{
		AllKinds object = new AllKinds();
		object.setShortField((short) 1);
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(object);
		int expected = Integer.SIZE;
		// Get the size of this object
		int size = ByteSizeUtils.estimateSizeInBytes(source);
		assertEquals(expected, size);
	}
	
	@Test
	public void testEstimateSizeAll(){
		AllKinds object = new AllKinds();
		object.setEnumfield(SomeEnum.BAR);
		object.setTimestampField(new Timestamp(1l));
		object.setBooleanField(Boolean.TRUE);
		object.setDateField(new Date());
		object.setByteArrayField(new byte[]{1,2,3,4});
		object.setStringField("abc");
		object.setDoubleField(123.556);
		object.setLongField(1l);
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(object);
		int expected = (3*Character.SIZE)
				+Long.SIZE
				+1
				+Long.SIZE
				+4
				+(3*Character.SIZE)
				+Double.SIZE
				+Long.SIZE;
		int size = ByteSizeUtils.estimateSizeInBytes(source);
		assertEquals(expected, size);
	}
	
	@Test
	public void testEstimateSizePrimitives(){
		Primitives object = new Primitives();
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(object);
		int expected = Integer.SIZE
				+Long.SIZE
				+Integer.SIZE
				+1
				+Double.SIZE;
		int size = ByteSizeUtils.estimateSizeInBytes(source);
		assertEquals(expected, size);
	}
	
	public static class Primitives {
		private int intValue = 1;
		private long longValue = 10l;
		private short shortValue = 1;
		private boolean booleanValue = false;
		private double doubleValue = 1.0;
		public int getIntValue() {
			return intValue;
		}
		public void setIntValue(int intValue) {
			this.intValue = intValue;
		}
		public long getLongValue() {
			return longValue;
		}
		public void setLongValue(long longValue) {
			this.longValue = longValue;
		}
		public short getShortValue() {
			return shortValue;
		}
		public void setShortValue(short shortValue) {
			this.shortValue = shortValue;
		}
		public boolean isBooleanValue() {
			return booleanValue;
		}
		public void setBooleanValue(boolean booleanValue) {
			this.booleanValue = booleanValue;
		}
		public double getDoubleValue() {
			return doubleValue;
		}
		public void setDoubleValue(double doubleValue) {
			this.doubleValue = doubleValue;
		}
		
	}
	
	/**
	 * A simple class with an example of all of the primitives that can be expected in a database object
	 * @author John
	 *
	 */
	public static class AllKinds {
		
		private Long longField;
		private Double doubleField;
		private String stringField;
		private byte[] byteArrayField;
		private Date dateField;
		private Boolean booleanField;
		private Timestamp timestampField;
		private SomeEnum enumfield;

		private Short shortField;
		
		public Short getShortField() {
			return shortField;
		}


		public void setShortField(Short shortField) {
			this.shortField = shortField;
		}


		public Long getLongField() {
			return longField;
		}


		public void setLongField(Long longField) {
			this.longField = longField;
		}


		public Double getDoubleField() {
			return doubleField;
		}


		public void setDoubleField(Double doubleField) {
			this.doubleField = doubleField;
		}


		public String getStringField() {
			return stringField;
		}


		public void setStringField(String stringField) {
			this.stringField = stringField;
		}


		public byte[] getByteArrayField() {
			return byteArrayField;
		}


		public void setByteArrayField(byte[] byteArrayField) {
			this.byteArrayField = byteArrayField;
		}


		public Date getDateField() {
			return dateField;
		}


		public void setDateField(Date dateField) {
			this.dateField = dateField;
		}


		public Boolean getBooleanField() {
			return booleanField;
		}


		public void setBooleanField(Boolean booleanField) {
			this.booleanField = booleanField;
		}


		public Timestamp getTimestampField() {
			return timestampField;
		}


		public void setTimestampField(Timestamp timestampField) {
			this.timestampField = timestampField;
		}


		public SomeEnum getEnumfield() {
			return enumfield;
		}


		public void setEnumfield(SomeEnum enumfield) {
			this.enumfield = enumfield;
		}
		
		
	}
	
	public static enum SomeEnum {
		FOO,BAR
	}
}
