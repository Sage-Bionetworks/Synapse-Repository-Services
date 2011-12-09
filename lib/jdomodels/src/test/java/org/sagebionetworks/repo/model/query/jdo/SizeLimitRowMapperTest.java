package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class SizeLimitRowMapperTest {
	
	@Test
	public void testString(){
		String string = "this is a string";
		Map<String, Object> row  = new HashMap<String, Object>();
		row.put("someKey", string);
		long bytesUsed = SizeLimitRowMapper.getRowSizeBytes(row);
		long expected = string.length()*2;
		assertEquals(expected, bytesUsed);
	}
	
	@Test
	public void testLong(){
		Long someLong = new Long(123);
		Map<String, Object> row  = new HashMap<String, Object>();
		row.put("someKey", someLong);
		long bytesUsed = SizeLimitRowMapper.getRowSizeBytes(row);
		long expected = 8;
		assertEquals(expected, bytesUsed);
	}

	@Test
	public void testDouble(){
		Double some = new Double(123.5);
		Map<String, Object> row  = new HashMap<String, Object>();
		row.put("someKey", some);
		long bytesUsed = SizeLimitRowMapper.getRowSizeBytes(row);
		long expected = 8;
		assertEquals(expected, bytesUsed);
	}
	
	@Test
	public void testInteger(){
		Integer some = new Integer(4);
		Map<String, Object> row  = new HashMap<String, Object>();
		row.put("someKey", some);
		long bytesUsed = SizeLimitRowMapper.getRowSizeBytes(row);
		long expected = 4;
		assertEquals(expected, bytesUsed);
	}
	
	@Test
	public void testByteArray(){
		byte[] some = new byte[12];
		Map<String, Object> row  = new HashMap<String, Object>();
		row.put("someKey", some);
		long bytesUsed = SizeLimitRowMapper.getRowSizeBytes(row);
		long expected = 12;
		assertEquals(expected, bytesUsed);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUnknown(){
		Object some = new Object();
		Map<String, Object> row  = new HashMap<String, Object>();
		row.put("someKey", some);
		long bytesUsed = SizeLimitRowMapper.getRowSizeBytes(row);
	}
	
	/**
	 * Test that we throw an exception when over the limit.
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testCheckSizExceeded(){
		SizeLimitRowMapper mapper = new SizeLimitRowMapper(100);
		byte[] some = new byte[100];
		Map<String, Object> row  = new HashMap<String, Object>();
		row.put("someKey", some);
		// Push it over the limit
		row.put("secondKey", "a"); 
		mapper.checkSize(row);
	}
	/**
	 * Make sure we pass when under the limit.
	 */
	@Test
	public void testCheckSizUnderLimit(){
		SizeLimitRowMapper mapper = new SizeLimitRowMapper(102);
		byte[] some = new byte[100];
		Map<String, Object> row  = new HashMap<String, Object>();
		row.put("someKey", some);
		// Push it over the limit
		row.put("secondKey", "a"); 
		row = mapper.checkSize(row);
		assertNotNull(row);
	}
}
