package org.sagebionetworks.table.cluster;

import org.junit.Test;

import static org.junit.Assert.*;

public class MySqlColumnTypeTest {

	@Test
	public void testParserType(){
		for(MySqlColumnType type: MySqlColumnType.values()){
			String typeString = type.name().toLowerCase()+"(1)";
			MySqlColumnType lookup = MySqlColumnType.parserType(typeString);
			assertEquals(type, lookup);
		}
	}
	
	@Test
	public void testParserTypeNoSize(){
		for(MySqlColumnType type: MySqlColumnType.values()){
			String typeString = type.name().toLowerCase();
			MySqlColumnType lookup = MySqlColumnType.parserType(typeString);
			assertEquals(type, lookup);
		}
	}
	
	@Test
	public void testParseSizeBigInt(){
		String typeString = "BIGINT";
		Integer size = MySqlColumnType.parseSize(typeString);
		assertNull(size);
	}

	@Test
	public void testParseSizeVarChar(){
		String typeString = "VARCHAR(255)";
		Integer size = MySqlColumnType.parseSize(typeString);
		assertEquals(new Integer(255), size);
	}

	@Test
	public void testParseSizeDouble(){
		String typeString = "double";
		Integer size = MySqlColumnType.parseSize(typeString);
		assertEquals(null, size);
	}
	
	@Test
	public void testParseSizeEnum(){
		String typeString = "enum('NaN','Infinity','-Infinity')";
		Integer size = MySqlColumnType.parseSize(typeString);
		assertEquals(null, size);
	}

	@Test
	public void testParseSizeJSON(){
		String typeString = "json";
		Integer size = MySqlColumnType.parseSize(typeString);
		assertEquals(null, size);
	}

	@Test
	public void testBigIntHasSize(){
		assertFalse(MySqlColumnType.BIGINT.hasSize());
	}
	
	@Test
	public void testVarCharHasSize(){
		assertTrue(MySqlColumnType.VARCHAR.hasSize());
	}
	
	@Test
	public void testDoubleHasSize(){
		assertFalse(MySqlColumnType.DOUBLE.hasSize());
	}

	@Test
	public void testBooleanHasSize(){
		assertFalse(MySqlColumnType.BOOLEAN.hasSize());
	}
	
	@Test
	public void testMediumTextHasSize(){
		assertFalse(MySqlColumnType.MEDIUMTEXT.hasSize());
	}
	
	@Test
	public void testTinyIntHasSize(){
		assertFalse(MySqlColumnType.TINYINT.hasSize());
	}
	
	@Test
	public void testEnumHasSize(){
		assertFalse(MySqlColumnType.ENUM.hasSize());
	}

	@Test
	public void testJsonHasSize(){
		assertFalse(MySqlColumnType.JSON.hasSize());
	}
}
