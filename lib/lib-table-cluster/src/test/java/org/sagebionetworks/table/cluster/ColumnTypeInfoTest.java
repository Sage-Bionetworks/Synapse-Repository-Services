package org.sagebionetworks.table.cluster;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.utils.ColumnConstants;

import com.google.common.collect.Sets;

public class ColumnTypeInfoTest {
	
	@Test
	public void testParseInteger(){
		Object dbValue = ColumnTypeInfo.INTEGER.parseValueForDB("123");
		assertEquals(new Long(123),dbValue);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testParseIntegerBad(){
		ColumnTypeInfo.INTEGER.parseValueForDB("foo");
	}
	
	@Test
	public void testParseFileHandleId(){
		Object dbValue = ColumnTypeInfo.FILEHANDLEID.parseValueForDB("123");
		assertEquals(new Long(123),dbValue);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testParseFileHandleBad(){
		ColumnTypeInfo.FILEHANDLEID.parseValueForDB("foo");
	}
	
	@Test
	public void testParseDateLong(){
		Object dbValue = ColumnTypeInfo.DATE.parseValueForDB("123");
		assertEquals(new Long(123),dbValue);
	}
	
	@Test
	public void testParseDateString(){
		Object dbValue = ColumnTypeInfo.DATE.parseValueForDB("1970-1-1 00:00:00.123");
		assertEquals(new Long(123),dbValue);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testParseDateBad(){
		ColumnTypeInfo.DATE.parseValueForDB("1970-1-1 00:00:00.foo");
	}
	
	@Test
	public void testParseEntityId(){
		Object dbValue = ColumnTypeInfo.ENTITYID.parseValueForDB("syn123");
		assertEquals("syn123",dbValue);
	}
	
	@Test
	public void testParseLink(){
		Object dbValue = ColumnTypeInfo.LINK.parseValueForDB("http://google.com");
		assertEquals("http://google.com",dbValue);
	}
	
	@Test
	public void testParseString(){
		Object dbValue = ColumnTypeInfo.STRING.parseValueForDB("foo");
		assertEquals("foo", dbValue);
	}
	
	@Test
	public void testParseDouble(){
		Object dbValue = ColumnTypeInfo.DOUBLE.parseValueForDB("123.1");
		assertEquals(new Double(123.1), dbValue);
	}
	
	@Test
	public void testParseDoubleNaN(){
		Object dbValue = ColumnTypeInfo.DOUBLE.parseValueForDB("NaN");
		assertEquals(Double.NaN, dbValue);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testParseDoubleBad(){
		Object dbValue = ColumnTypeInfo.DOUBLE.parseValueForDB("123.1foo");
		assertEquals(new Double(123.1), dbValue);
	}
	
	@Test
	public void testParseBooleanTrue(){
		Object dbValue = ColumnTypeInfo.BOOLEAN.parseValueForDB("true");
		assertEquals(Boolean.TRUE, dbValue);
	}
	
	@Test
	public void testParseBooleanFalse(){
		Object dbValue = ColumnTypeInfo.BOOLEAN.parseValueForDB("False");
		assertEquals(Boolean.FALSE, dbValue);
	}
	
	@Test
	public void testParseLargeText(){
		Object dbValue = ColumnTypeInfo.LARGETEXT.parseValueForDB("foo");
		assertEquals("foo", dbValue);
	}
	
	@Test
	public void testParseAllNull(){
		for(ColumnTypeInfo info: ColumnTypeInfo.values()){
			String value = null;
			assertNull(info.parseValueForDB(value));
		}
	}

	@Test
	public void testIsStringType(){
		Set<ColumnTypeInfo> stringTypes = Sets.newHashSet(ColumnTypeInfo.ENTITYID, ColumnTypeInfo.STRING, ColumnTypeInfo.LARGETEXT, ColumnTypeInfo.LINK);
		for(ColumnTypeInfo type: ColumnTypeInfo.values()){
			if(stringTypes.contains(type)){
				assertTrue("Should not be a string type: "+type.name(),type.isStringType());
			}else{
				assertFalse("Should be a string type: "+type.name(),type.isStringType());
			}
		}
	}
	
	@Test
	public void testRequiresInputMaxSize(){
		Set<ColumnTypeInfo> requiresSize = Sets.newHashSet(ColumnTypeInfo.STRING, ColumnTypeInfo.LINK);
		for(ColumnTypeInfo type: ColumnTypeInfo.values()){
			if(requiresSize.contains(type)){
				assertTrue("Should not be a string type: "+type.name(), type.requiresInputMaxSize());
			}else{
				assertFalse("Should be a string type: "+type.name(),type.requiresInputMaxSize());
			}
		}
	}

	@Test
	public void testToSqlIntegerDefaultNull(){
		Long inputSize = null;
		String defaultValue = null;
		String sql = ColumnTypeInfo.INTEGER.toSql(inputSize, defaultValue);
		assertEquals("BIGINT(20) DEFAULT NULL", sql);
	}
	
	@Test
	public void testToSqlIntegerWithDefault(){
		Long inputSize = null;
		String defaultValue = "123";
		String sql = ColumnTypeInfo.INTEGER.toSql(inputSize, defaultValue);
		assertEquals("BIGINT(20) DEFAULT 123", sql);
	}
	
	@Test
	public void testToSqlFileHandleIdDefaultNull(){
		Long inputSize = null;
		String defaultValue = null;
		String sql = ColumnTypeInfo.FILEHANDLEID.toSql(inputSize, defaultValue);
		assertEquals("BIGINT(20) DEFAULT NULL", sql);
	}
	
	@Test
	public void testToSqlFileHandleIdWithDefault(){
		Long inputSize = null;
		String defaultValue = "123";
		String sql = ColumnTypeInfo.FILEHANDLEID.toSql(inputSize, defaultValue);
		assertEquals("BIGINT(20) DEFAULT 123", sql);
	}
	
	@Test
	public void testToSqlDateDefaultNull(){
		Long inputSize = null;
		String defaultValue = null;
		String sql = ColumnTypeInfo.DATE.toSql(inputSize, defaultValue);
		assertEquals("BIGINT(20) DEFAULT NULL", sql);
	}
	
	@Test
	public void testToSqlDateWithDefault(){
		Long inputSize = null;
		String defaultValue = "123";
		String sql = ColumnTypeInfo.DATE.toSql(inputSize, defaultValue);
		assertEquals("BIGINT(20) DEFAULT 123", sql);
	}
	
	@Test
	public void testToSqlEntityIdDefaultNull(){
		Long inputSize = null;
		String defaultValue = null;
		String sql = ColumnTypeInfo.ENTITYID.toSql(inputSize, defaultValue);
		assertEquals("VARCHAR(44) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL", sql);
	}
	
	@Test
	public void testToSqlEntityIdWithDefault(){
		Long inputSize = null;
		String defaultValue = "syn123";
		String sql = ColumnTypeInfo.ENTITYID.toSql(inputSize, defaultValue);
		assertEquals("VARCHAR(44) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT 'syn123'", sql);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testToSqlStringSizeNull(){
		Long inputSize = null;
		String defaultValue = null;
		ColumnTypeInfo.STRING.toSql(inputSize, defaultValue);
	}
	
	@Test
	public void testToSqlStringDefaultNull(){
		Long inputSize = 123L;
		String defaultValue = null;
		String sql = ColumnTypeInfo.STRING.toSql(inputSize, defaultValue);
		assertEquals("VARCHAR(123) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL", sql);
	}
	
	@Test
	public void testToSqlStringWithDefault(){
		Long inputSize = 123L;
		String defaultValue = "foo";
		String sql = ColumnTypeInfo.STRING.toSql(inputSize, defaultValue);
		assertEquals("VARCHAR(123) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT 'foo'", sql);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testToSqlLinkSizeNull(){
		Long inputSize = null;
		String defaultValue = null;
		ColumnTypeInfo.LINK.toSql(inputSize, defaultValue);
	}
	
	@Test
	public void testToSqlLinkDefaultNull(){
		Long inputSize = 123L;
		String defaultValue = null;
		String sql = ColumnTypeInfo.LINK.toSql(inputSize, defaultValue);
		assertEquals("VARCHAR(123) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL", sql);
	}
	
	@Test
	public void testToSqlLinkWithDefault(){
		Long inputSize = 123L;
		String defaultValue = "foo";
		String sql = ColumnTypeInfo.LINK.toSql(inputSize, defaultValue);
		assertEquals("VARCHAR(123) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT 'foo'", sql);
	}
	
	@Test
	public void testToSqlDoubleDefaultNull(){
		Long inputSize = null;
		String defaultValue = null;
		String sql = ColumnTypeInfo.DOUBLE.toSql(inputSize, defaultValue);
		assertEquals("DOUBLE DEFAULT NULL", sql);
	}
	
	@Test
	public void testToSqlDoubleWithDefault(){
		Long inputSize = null;
		String defaultValue = "1.2";
		String sql = ColumnTypeInfo.DOUBLE.toSql(inputSize, defaultValue);
		assertEquals("DOUBLE DEFAULT 1.2", sql);
	}
	
	@Test
	public void testToSqlLargeTextDefaultNull(){
		Long inputSize = null;
		String defaultValue = null;
		String sql = ColumnTypeInfo.LARGETEXT.toSql(inputSize, defaultValue);
		assertEquals("MEDIUMTEXT CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL", sql);
	}
	
	@Test
	public void testToSqlLargeTextWithDefault(){
		Long inputSize = null;
		String defaultValue = "bar";
		String sql = ColumnTypeInfo.LARGETEXT.toSql(inputSize, defaultValue);
		assertEquals("MEDIUMTEXT CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT 'bar'", sql);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetInfoForTypeNull(){
		ColumnType type = null;
		ColumnTypeInfo.getInfoForType(type);
	}
	
	@Test
	public void testGetInfoForTypeAllTypes(){
		for(ColumnType type: ColumnType.values()){
			ColumnTypeInfo info = ColumnTypeInfo.getInfoForType(type);
			assertNotNull(info);
		}
	}

	@Test
	public void testAppendDefaultValueAllTypes(){
		for(ColumnTypeInfo info: ColumnTypeInfo.values()){
			StringBuilder builder = new StringBuilder();
			String defaultValue = null;
			info.appendDefaultValue(builder, defaultValue);
			assertEquals("DEFAULT NULL", builder.toString());
		}
	}
	
	@Test
	public void testAppendDefaultInteger(){
		StringBuilder builder = new StringBuilder();
		String defaultValue = "123";
		ColumnTypeInfo.INTEGER.appendDefaultValue(builder, defaultValue);
		assertEquals("DEFAULT 123", builder.toString());
	}
	
	@Test
	public void testAppendDefaultString(){
		StringBuilder builder = new StringBuilder();
		String defaultValue = "123";
		ColumnTypeInfo.STRING.appendDefaultValue(builder, defaultValue);
		assertEquals("DEFAULT '123'", builder.toString());
	}
	
	@Test
	public void testAppendDefaultStringSqlInjection(){
		StringBuilder builder = new StringBuilder();
		String defaultValue = "DROP TABLE 'T123'";
		ColumnTypeInfo.STRING.appendDefaultValue(builder, defaultValue);
		assertEquals("DEFAULT 'DROP TABLE ''T123'''", builder.toString());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testAppendDefaultIntegerBad(){
		StringBuilder builder = new StringBuilder();
		String defaultValue = "bar";
		ColumnTypeInfo.INTEGER.appendDefaultValue(builder, defaultValue);
	}
	
}
