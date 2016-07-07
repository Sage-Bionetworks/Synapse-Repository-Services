package org.sagebionetworks.table.cluster;

import static org.junit.Assert.*;

import org.junit.Test;

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
		String typeString = "bigint(20)";
		Integer size = MySqlColumnType.parseSize(typeString);
		assertEquals(new Integer(20), size);
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

}
