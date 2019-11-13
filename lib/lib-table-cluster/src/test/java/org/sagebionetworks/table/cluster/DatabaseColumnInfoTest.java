package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.ColumnConstants;

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
	
	@Test
	public void testCARDINALITY_COMPARATOR(){
		DatabaseColumnInfo one = new DatabaseColumnInfo();
		one.setCardinality(1L);
		DatabaseColumnInfo two = new DatabaseColumnInfo();
		two.setCardinality(2L);
		int compare = DatabaseColumnInfo.CARDINALITY_COMPARATOR.compare(one, two);
		assertEquals(-1, compare);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCARDINALITY_COMPARATOROneCardinalityNull(){
		DatabaseColumnInfo one = new DatabaseColumnInfo();
		one.setCardinality(null);
		DatabaseColumnInfo two = new DatabaseColumnInfo();
		two.setCardinality(2L);
		// call under test.
		DatabaseColumnInfo.CARDINALITY_COMPARATOR.compare(one, two);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCARDINALITY_COMPARATORTwoCardinalityNull(){
		DatabaseColumnInfo one = new DatabaseColumnInfo();
		one.setCardinality(1L);
		DatabaseColumnInfo two = new DatabaseColumnInfo();
		two.setCardinality(null);
		// call under test.
		DatabaseColumnInfo.CARDINALITY_COMPARATOR.compare(one, two);
	}
	
	@Test
	public void testIsMetadataRowId(){
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName(TableConstants.ROW_ID);
		assertTrue(info.isMetadata());
	}
	
	@Test
	public void testIsMetadataRowVersion(){
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName(TableConstants.ROW_VERSION);
		assertTrue(info.isMetadata());
	}
	
	@Test
	public void testIsMetadataEtag(){
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName(TableConstants.ROW_ETAG);
		assertTrue(info.isMetadata());
	}
	
	@Test
	public void testIsMetadataBenefactor(){
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName(TableConstants.ROW_BENEFACTOR);
		assertTrue(info.isMetadata());
	}
	
	@Test
	public void testIsRowIdOrVersionNot(){
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C123_");
		assertFalse(info.isMetadata());
	}
}
