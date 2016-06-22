package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.cluster.utils.ColumnConstants;

public class DatabaseColumnInfoTest {
	
	@Test
	public void testCreateIndexDefinitionMediumText(){
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C123_");
		info.setIndexName("_C123_IDX");
		info.setType(MySqlColumnType.MEDIUMTEXT);
		// call under test
		String results = info.createIndexDefinition();
		assertEquals("_C123_IDX (_C123_(255))", results);
	}
	
	@Test
	public void testCreateIndexDefinitionVarcharUnderMax(){
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C123_");
		info.setIndexName("_C123_IDX");
		info.setType(MySqlColumnType.VARCHAR);
		info.setMaxSize(ColumnConstants.MAX_MYSQL_VARCHAR_INDEX_LENGTH-1);
		// call under test
		String results = info.createIndexDefinition();
		assertEquals("_C123_IDX (_C123_)", results);
	}
	
	@Test
	public void testCreateIndexDefinitionVarcharAtMax(){
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C123_");
		info.setIndexName("_C123_IDX");
		info.setType(MySqlColumnType.VARCHAR);
		info.setMaxSize(ColumnConstants.MAX_MYSQL_VARCHAR_INDEX_LENGTH);
		// call under test
		String results = info.createIndexDefinition();
		assertEquals("_C123_IDX (_C123_(255))", results);
	}
	
	@Test
	public void testCreateIndexDefinitionVarcharOverMax(){
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C123_");
		info.setIndexName("_C123_IDX");
		info.setType(MySqlColumnType.VARCHAR);
		info.setMaxSize(ColumnConstants.MAX_MYSQL_VARCHAR_INDEX_LENGTH+1);
		// call under test
		String results = info.createIndexDefinition();
		assertEquals("_C123_IDX (_C123_(255))", results);
	}
	
	@Test
	public void testCreateIndexDefinitionBigInt(){
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C123_");
		info.setIndexName("_C123_IDX");
		info.setType(MySqlColumnType.BIGINT);
		// call under test
		String results = info.createIndexDefinition();
		assertEquals("_C123_IDX (_C123_)", results);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateIndexDefinitionNullColumnName(){
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName(null);
		info.setIndexName("_C123_IDX");
		info.setType(MySqlColumnType.BIGINT);
		// call under test
		info.createIndexDefinition();
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateIndexDefinitionNullIndexName(){
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C123_");
		info.setIndexName(null);
		info.setType(MySqlColumnType.BIGINT);
		// call under test
		info.createIndexDefinition();
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateIndexDefinitionNullType(){
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C123_");
		info.setIndexName("_C123_IDX");
		info.setType(null);
		// call under test
		info.createIndexDefinition();
	}
}
