package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;

import com.google.common.collect.Sets;

public class ColumnTypeInfoTest {
	
	boolean useDepricatedUtf8ThreeBytes;
	
	@BeforeEach
	public void before(){
		useDepricatedUtf8ThreeBytes = false;
	}

	@Test
	public void testParseInteger(){
		Object dbValue = ColumnTypeInfo.INTEGER.parseValueForDatabaseWrite("123");
		assertEquals(Long.valueOf(123),dbValue);
	}
	
	@Test
	public void testParseIntegerBad(){
		assertThrows(IllegalArgumentException.class, () -> {
			ColumnTypeInfo.INTEGER.parseValueForDatabaseWrite("foo");
		});
	}
	
	@Test
	public void testParseFileHandleId(){
		Object dbValue = ColumnTypeInfo.FILEHANDLEID.parseValueForDatabaseWrite("123");
		assertEquals(Long.valueOf(123),dbValue);
	}
	
	@Test
	public void testParseFileHandleBad(){
		assertThrows(IllegalArgumentException.class, () -> {
			ColumnTypeInfo.FILEHANDLEID.parseValueForDatabaseWrite("foo");
		});
	}
	
	@Test
	public void testParseUserId(){
		Object dbValue = ColumnTypeInfo.USERID.parseValueForDatabaseWrite("123");
		assertEquals(Long.valueOf(123),dbValue);
	}
	
	@Test
	public void testParseUserIdBad(){
		assertThrows(IllegalArgumentException.class, () -> {
			ColumnTypeInfo.USERID.parseValueForDatabaseWrite("foo");
		});
	}
	
	@Test
	public void testParseDateLong(){
		Object dbValue = ColumnTypeInfo.DATE.parseValueForDatabaseWrite("123");
		assertEquals(Long.valueOf(123),dbValue);
	}
	
	@Test
	public void testParseDateString(){
		Object dbValue = ColumnTypeInfo.DATE.parseValueForDatabaseWrite("1970-1-1 00:00:00.123");
		assertEquals(Long.valueOf(123),dbValue);
	}
	
	@Test
	public void testParseDateBad(){
		assertThrows(IllegalArgumentException.class, () -> {
			ColumnTypeInfo.DATE.parseValueForDatabaseWrite("1970-1-1 00:00:00.foo");
		});
	}
	
	@Test
	public void testParseEntityId(){
		Object dbValue = ColumnTypeInfo.ENTITYID.parseValueForDatabaseWrite("syn123");
		assertEquals(Long.valueOf(123),dbValue);
	}
	
	@Test
	public void testParseLink(){
		Object dbValue = ColumnTypeInfo.LINK.parseValueForDatabaseWrite("http://google.com");
		assertEquals("http://google.com",dbValue);
	}
	
	@Test
	public void testParseString(){
		Object dbValue = ColumnTypeInfo.STRING.parseValueForDatabaseWrite("foo");
		assertEquals("foo", dbValue);
	}
	
	@Test
	public void testParseDouble(){
		Object dbValue = ColumnTypeInfo.DOUBLE.parseValueForDatabaseWrite("123.1");
		assertEquals(Double.valueOf(123.1), dbValue);
	}
	
	@Test
	public void testParseDoubleNaN(){
		Object dbValue = ColumnTypeInfo.DOUBLE.parseValueForDatabaseWrite("NaN");
		assertEquals(Double.NaN, dbValue);
	}
	
	@Test
	public void testParseDoubleBad(){
		assertThrows(IllegalArgumentException.class, () -> {
			ColumnTypeInfo.DOUBLE.parseValueForDatabaseWrite("123.1foo");
		});
	}
	
	@Test
	public void testParseBooleanTrue(){
		Object dbValue = ColumnTypeInfo.BOOLEAN.parseValueForDatabaseWrite("true");
		assertEquals(Boolean.TRUE, dbValue);
	}
	
	@Test
	public void testParseBooleanFalse(){
		Object dbValue = ColumnTypeInfo.BOOLEAN.parseValueForDatabaseWrite("False");
		assertEquals(Boolean.FALSE, dbValue);
	}
	
	@Test
	public void testParseLargeText(){
		Object dbValue = ColumnTypeInfo.LARGETEXT.parseValueForDatabaseWrite("foo");
		assertEquals("foo", dbValue);
	}
	
	@Test
	public void testParseMediumText(){
		Object dbValue = ColumnTypeInfo.MEDIUMTEXT.parseValueForDatabaseWrite("foo");
		assertEquals("foo", dbValue);
	}
	
	@Test
	public void testParseJson(){
		Object dbValue = ColumnTypeInfo.JSON.parseValueForDatabaseWrite("{\"foo\": \"bar\"}");
		assertEquals("{\"foo\":\"bar\"}", dbValue);
	}
	
	@Test
	public void testParseAllNull(){
		for(ColumnTypeInfo info: ColumnTypeInfo.values()){
			String value = null;
			assertNull(info.parseValueForDatabaseWrite(value));
		}
	}

	@Test
	public void testIsStringType(){
		Set<ColumnTypeInfo> stringTypes = Sets.newHashSet(ColumnTypeInfo.STRING, ColumnTypeInfo.LARGETEXT, ColumnTypeInfo.LINK, ColumnTypeInfo.MEDIUMTEXT);
		for(ColumnTypeInfo type: ColumnTypeInfo.values()){
			if(stringTypes.contains(type)){
				assertTrue(type.isStringType(), "Should not be a string type: "+type.name());
			}else{
				assertFalse(type.isStringType(), "Should be a string type: "+type.name());
			}
		}
	}
	
	@Test
	public void testRequiresInputMaxSize(){
		Set<ColumnTypeInfo> requiresSize = Sets.newHashSet(ColumnTypeInfo.STRING, ColumnTypeInfo.LINK);
		for(ColumnTypeInfo type: ColumnTypeInfo.values()){
			if(requiresSize.contains(type)){
				assertTrue(type.requiresInputMaxSize(), "Should not be a string type: "+type.name());
			}else{
				assertFalse(type.requiresInputMaxSize(), "Should be a string type: "+type.name());
			}
		}
	}

	@Test
	public void testToSqlIntegerDefaultNull(){
		Long inputSize = null;
		String defaultValue = null;
		String sql = ColumnTypeInfo.INTEGER.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("BIGINT DEFAULT NULL COMMENT 'INTEGER'", sql);
	}
	
	@Test
	public void testToSqlIntegerWithDefault(){
		Long inputSize = null;
		String defaultValue = "123";
		String sql = ColumnTypeInfo.INTEGER.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("BIGINT DEFAULT 123 COMMENT 'INTEGER'", sql);
	}
	
	@Test
	public void testToSqlFileHandleIdDefaultNull(){
		Long inputSize = null;
		String defaultValue = null;
		String sql = ColumnTypeInfo.FILEHANDLEID.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("BIGINT DEFAULT NULL COMMENT 'FILEHANDLEID'", sql);
	}
	
	@Test
	public void testToSqlFileHandleIdWithDefault(){
		Long inputSize = null;
		String defaultValue = "123";
		String sql = ColumnTypeInfo.FILEHANDLEID.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("BIGINT DEFAULT 123 COMMENT 'FILEHANDLEID'", sql);
	}
	
	@Test
	public void testToSqlUserIdDefaultNull(){
		Long inputSize = null;
		String defaultValue = null;
		String sql = ColumnTypeInfo.USERID.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("BIGINT DEFAULT NULL COMMENT 'USERID'", sql);
	}
	
	@Test
	public void testToSqlUserIdWithDefault(){
		Long inputSize = null;
		String defaultValue = "123";
		String sql = ColumnTypeInfo.USERID.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("BIGINT DEFAULT 123 COMMENT 'USERID'", sql);
	}
	
	@Test
	public void testToSqlDateDefaultNull(){
		Long inputSize = null;
		String defaultValue = null;
		String sql = ColumnTypeInfo.DATE.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("BIGINT DEFAULT NULL COMMENT 'DATE'", sql);
	}
	
	@Test
	public void testToSqlDateWithDefault(){
		Long inputSize = null;
		String defaultValue = "123";
		String sql = ColumnTypeInfo.DATE.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("BIGINT DEFAULT 123 COMMENT 'DATE'", sql);
	}
	
	@Test
	public void testToSqlEntityIdDefaultNull(){
		Long inputSize = null;
		String defaultValue = null;
		String sql = ColumnTypeInfo.ENTITYID.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("BIGINT DEFAULT NULL COMMENT 'ENTITYID'", sql);
	}
	
	@Test
	public void testToSqlEntityIdWithDefault(){
		Long inputSize = null;
		String defaultValue = "syn123";
		String sql = ColumnTypeInfo.ENTITYID.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("BIGINT DEFAULT 123 COMMENT 'ENTITYID'", sql);
	}
	
	@Test
	public void testToSqlStringSizeNull(){
		Long inputSize = null;
		String defaultValue = null;
		assertThrows(IllegalArgumentException.class, () -> {
			ColumnTypeInfo.STRING.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		});
	}
	
	@Test
	public void testToSqlStringDefaultNull(){
		Long inputSize = 123L;
		String defaultValue = null;
		String sql = ColumnTypeInfo.STRING.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("VARCHAR(123) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'STRING'", sql);
	}
	
	@Test
	public void testToSqlStringWithDefault(){
		Long inputSize = 123L;
		String defaultValue = "foo";
		String sql = ColumnTypeInfo.STRING.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("VARCHAR(123) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'foo' COMMENT 'STRING'", sql);
	}
	
	@Test
	public void testToSqlStringWithUseDepricated(){
		useDepricatedUtf8ThreeBytes = true;
		Long inputSize = 123L;
		String defaultValue = "foo";
		String sql = ColumnTypeInfo.STRING.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("VARCHAR(123) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT 'foo' COMMENT 'STRING'", sql);
	}
	
	@Test
	public void testToSqlLinkSizeNull(){
		Long inputSize = null;
		String defaultValue = null;
		assertThrows(IllegalArgumentException.class, () -> {
			ColumnTypeInfo.LINK.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		});
	}
	
	@Test
	public void testToSqlLinkDefaultNull(){
		Long inputSize = 123L;
		String defaultValue = null;
		String sql = ColumnTypeInfo.LINK.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("VARCHAR(123) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'LINK'", sql);
	}
	
	@Test
	public void testToSqlLinkWithDefault(){
		Long inputSize = 123L;
		String defaultValue = "foo";
		String sql = ColumnTypeInfo.LINK.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("VARCHAR(123) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'foo' COMMENT 'LINK'", sql);
	}
	
	@Test
	public void testToSqlDoubleDefaultNull(){
		Long inputSize = null;
		String defaultValue = null;
		String sql = ColumnTypeInfo.DOUBLE.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("DOUBLE DEFAULT NULL COMMENT 'DOUBLE'", sql);
	}
	
	@Test
	public void testToSqlDoubleWithSize(){
		Long inputSize = 100L;
		String defaultValue = null;
		String sql = ColumnTypeInfo.DOUBLE.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("DOUBLE DEFAULT NULL COMMENT 'DOUBLE'", sql);
	}
	
	@Test
	public void testToSqlDoubleWithDefault(){
		Long inputSize = null;
		String defaultValue = "1.2";
		String sql = ColumnTypeInfo.DOUBLE.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("DOUBLE DEFAULT 1.2 COMMENT 'DOUBLE'", sql);
	}
	
	@Test
	public void testToSqlLargeTextDefaultNull(){
		Long inputSize = null;
		String defaultValue = null;
		String sql = ColumnTypeInfo.LARGETEXT.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'LARGETEXT'", sql);
	}
	
	@Test
	public void testToSqlLargeTextWithDefault(){
		Long inputSize = null;
		String defaultValue = "bar";
		String sql = ColumnTypeInfo.LARGETEXT.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'bar' COMMENT 'LARGETEXT'", sql);
	}
	
	@Test
	public void testToSqlMediumTextWithDefault(){
		Long inputSize = null;
		String defaultValue = "bar";
		String sql = ColumnTypeInfo.MEDIUMTEXT.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'bar' COMMENT 'MEDIUMTEXT'", sql);
	}
	
	@Test
	public void testToSqlBoolean(){
		Long inputSize = null;
		String defaultValue = null;
		String sql = ColumnTypeInfo.BOOLEAN.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("BOOLEAN DEFAULT NULL COMMENT 'BOOLEAN'", sql);
	}
	
	@Test
	public void testToSqlBooleanDefault(){
		Long inputSize = null;
		String defaultValue = Boolean.TRUE.toString();
		String sql = ColumnTypeInfo.BOOLEAN.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("BOOLEAN DEFAULT true COMMENT 'BOOLEAN'", sql);
	}
	
	@Test
	public void testToSqlBooleanWithSize(){
		Long inputSize = 19L;
		String defaultValue = null;
		String sql = ColumnTypeInfo.BOOLEAN.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("BOOLEAN DEFAULT NULL COMMENT 'BOOLEAN'", sql);
	}
	
	@Test
	public void testToSqlJsonDefaultNull(){
		Long inputSize = null;
		String defaultValue = null;
		String sql = ColumnTypeInfo.JSON.toSql(inputSize, defaultValue, useDepricatedUtf8ThreeBytes);
		assertEquals("JSON DEFAULT NULL COMMENT 'JSON'", sql);
	}
	
	@Test
	public void testGetInfoForTypeNull(){
		ColumnType type = null;
		assertThrows(IllegalArgumentException.class, () -> {
			ColumnTypeInfo.getInfoForType(type);
		});
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
	public void testAppendDefault_StringList(){
		StringBuilder builder = new StringBuilder();
		String defaultValue = "[\"a\", \"b\", \"c\"]";
		ColumnTypeInfo.STRING_LIST.appendDefaultValue(builder, defaultValue);
		assertEquals("DEFAULT ('[\"a\",\"b\",\"c\"]')", builder.toString());
	}

	/**
	 * If an empty list is the default value, it should be converted to a null default value
	 * Newly created column models should already prevent an empty list from being assigned as
	 * the default value, but there exists legacy column models in the database before this
	 * was enforced
	 */
	@Test
	public void testAppendDefault_EmptyList(){
		String defaultValue = "[]";
		for(ColumnTypeInfo typeInfo : ColumnTypeInfo.values()){
			if (ColumnTypeListMappings.isList(typeInfo.getType())){
				StringBuilder builder = new StringBuilder();
				typeInfo.appendDefaultValue(builder, defaultValue);
				assertEquals("DEFAULT NULL", builder.toString());
			}
		}

	}


	@Test
	public void testAppendDefault_IntegerList(){
		StringBuilder builder = new StringBuilder();
		String defaultValue = "[1, 2, 3]";
		ColumnTypeInfo.INTEGER_LIST.appendDefaultValue(builder, defaultValue);
		assertEquals("DEFAULT ('[1,2,3]')", builder.toString());
	}
	
	@Test
	public void testAppendDefaultStringSqlInjection(){
		StringBuilder builder = new StringBuilder();
		String defaultValue = "DROP TABLE 'T123'";
		ColumnTypeInfo.STRING.appendDefaultValue(builder, defaultValue);
		assertEquals("DEFAULT 'DROP TABLE ''T123'''", builder.toString());
	}
	
	@Test
	public void testAppendDefaultIntegerBad(){
		StringBuilder builder = new StringBuilder();
		String defaultValue = "bar";
		
		assertThrows(IllegalArgumentException.class, () -> {			
			ColumnTypeInfo.INTEGER.appendDefaultValue(builder, defaultValue);
		});
	}
	
}
