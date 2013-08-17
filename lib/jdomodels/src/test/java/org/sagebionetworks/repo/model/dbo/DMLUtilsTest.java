package org.sagebionetworks.repo.model.dbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class DMLUtilsTest {
	
	// Here is our simple mapping.
	private TableMapping<Object> mapping = new AbstractTestTableMapping<Object>() {
		@Override
		public String getTableName() {
			return "SOME_TABLE";
		}
		
		@Override
		public FieldColumn[] getFieldColumns() {
			return new FieldColumn[] {
					new FieldColumn("id", "ID", true).withIsBackupId(true),
					new FieldColumn("bigName", "BIG_NAME"),
			};
		}
	};
	
	private TableMapping<Object> mappingTwoKeys = new AbstractTestTableMapping<Object>() {

		@Override
		public String getTableName() {
			return "TWO_KEY_TABLE";
		}
		
		@Override
		public FieldColumn[] getFieldColumns() {
			return new FieldColumn[] {
					new FieldColumn("owner", "OWNER_ID", true),
					new FieldColumn("revNumber", "REV_NUMBER", true),
					new FieldColumn("bigName", "BIG_NAME"),
					new FieldColumn("smallName", "SMALL_NAME"),
			};
		}
	};
	
	private TableMapping<Object> migrateableMappingSelfForeignKey = new AbstractTestTableMapping<Object>() {
		@Override
		public String getTableName() {
			return "SOME_TABLE";
		}
		
		@Override
		public FieldColumn[] getFieldColumns() {
			return new FieldColumn[] {
					new FieldColumn("id", "ID", true).withIsBackupId(true),
					new FieldColumn("etag", "ETAG").withIsEtag(true),
					new FieldColumn("parentId", "PARENT_ID").withIsSelfForeignKey(true),
			};
		}
	};
	
	private TableMapping<Object> migrateableMappingNoEtagNotSelfForeignKey = new AbstractTestTableMapping<Object>() {
		@Override
		public String getTableName() {
			return "SOME_TABLE";
		}
		
		@Override
		public FieldColumn[] getFieldColumns() {
			return new FieldColumn[] {
					new FieldColumn("id", "ID", true).withIsBackupId(true),
			};
		}
	};

	
	@Test
	public void testCreateInsertStatement(){

		String dml = DMLUtils.createInsertStatement(mapping);
		assertNotNull(dml);
		System.out.println(dml);
		assertEquals("INSERT INTO SOME_TABLE(`ID`, `BIG_NAME`) VALUES (:id, :bigName)", dml);
	}
	
	@Test
	public void testAppendPrimaryKeySingle(){
		StringBuilder builder = new StringBuilder();
		DMLUtils.appendPrimaryKey(mapping, builder);
		String result = builder.toString();
		System.out.println(result);
		assertEquals("`ID` = :id", result);
	}
	
	@Test
	public void testAppendPrimaryKeyTwoKey(){
		StringBuilder builder = new StringBuilder();
		DMLUtils.appendPrimaryKey(mappingTwoKeys, builder);
		String result = builder.toString();
		System.out.println(result);
		assertEquals("`OWNER_ID` = :owner AND `REV_NUMBER` = :revNumber", result);
	}
	
	@Test
	public void testCreateGetByIDStatement(){
		// Here is our simple mapping.
		String dml = DMLUtils.createGetByIDStatement(mapping);
		assertNotNull(dml);
		System.out.println(dml);
		assertEquals("SELECT * FROM SOME_TABLE WHERE `ID` = :id", dml);
	}
	
	@Test
	public void testDeleteStatement(){
		// Here is our simple mapping.
		String dml = DMLUtils.createDeleteStatement(mapping);
		assertNotNull(dml);
		System.out.println(dml);
		assertEquals("DELETE FROM SOME_TABLE WHERE `ID` = :id", dml);
	}
	
	@Test
	public void testCreateUpdateStatment(){
		// Here is our simple mapping.
		String dml = DMLUtils.createUpdateStatment(mapping);
		assertNotNull(dml);
		System.out.println(dml);
		assertEquals("UPDATE SOME_TABLE SET `BIG_NAME` = :bigName WHERE `ID` = :id", dml);
	}
	
	@Test
	public void testCreateGetCountStatment(){
		// Here is our simple mapping.
		String dml = DMLUtils.createGetCountStatement(mapping);
		assertNotNull(dml);
		System.out.println(dml);
		assertEquals("SELECT COUNT(ID) FROM SOME_TABLE", dml);
	}
	
	@Test
	public void testCreateGetMaxStatement() {
		String dml = DMLUtils.createGetMaxStatement(mapping);
		assertNotNull(dml);
		System.out.println(dml);
		assertEquals("SELECT MAX(ID) FROM SOME_TABLE", dml);
	}
	
	@Test
	public void testCreateUpdateStatmentTwoKeys(){
		// Here is our simple mapping.
		String dml = DMLUtils.createUpdateStatment(mappingTwoKeys);
		assertNotNull(dml);
		System.out.println(dml);
		assertEquals("UPDATE TWO_KEY_TABLE SET `BIG_NAME` = :bigName, `SMALL_NAME` = :smallName WHERE `OWNER_ID` = :owner AND `REV_NUMBER` = :revNumber", dml);
	}
	
	@Test
	public void testCreateBatchDelete(){
		String batchDelete = DMLUtils.createBatchDelete(mapping);
		assertNotNull(batchDelete);
		System.out.println(batchDelete);
		assertEquals("DELETE FROM SOME_TABLE WHERE `ID` IN ( :BVIDLIST )", batchDelete);
	}
	
	@Test
	public void testCreateBatchDeleteSelfForeign(){
		String batchDelete = DMLUtils.createBatchDelete(migrateableMappingSelfForeignKey);
		assertNotNull(batchDelete);
		System.out.println(batchDelete);
		assertEquals("DELETE FROM SOME_TABLE WHERE `ID` IN ( :BVIDLIST )", batchDelete);
	}
	
	@Test
	public void testListWithSelfForeignKey(){
		String batchDelete = DMLUtils.listRowMetadata(migrateableMappingSelfForeignKey);
		assertNotNull(batchDelete);
		System.out.println(batchDelete);
		assertEquals("SELECT `ID`, `ETAG`, `PARENT_ID` FROM SOME_TABLE ORDER BY `ID` ASC LIMIT :BCLIMIT OFFSET :BVOFFSET", batchDelete);
	}

	@Test
	public void testListWithNoEtagNoSelfForeignKey(){
		String batchDelete = DMLUtils.listRowMetadata(migrateableMappingNoEtagNotSelfForeignKey);
		assertNotNull(batchDelete);
		System.out.println(batchDelete);
		assertEquals("SELECT `ID` FROM SOME_TABLE ORDER BY `ID` ASC LIMIT :BCLIMIT OFFSET :BVOFFSET", batchDelete);
	}
	
	@Test
	public void testDeltaListWithSelfForeignKey(){
		String batchDelete = DMLUtils.deltaListRowMetadata(migrateableMappingSelfForeignKey);
		assertNotNull(batchDelete);
		System.out.println(batchDelete);
		assertEquals("SELECT `ID`, `ETAG`, `PARENT_ID` FROM SOME_TABLE WHERE `ID` IN ( :BVIDLIST ) ORDER BY `ID` ASC", batchDelete);
	}

	@Test
	public void testDeltaListWithNoEtagNoSelfForeignKey(){
		String batchDelete = DMLUtils.deltaListRowMetadata(migrateableMappingNoEtagNotSelfForeignKey);
		assertNotNull(batchDelete);
		System.out.println(batchDelete);
		assertEquals("SELECT `ID` FROM SOME_TABLE WHERE `ID` IN ( :BVIDLIST ) ORDER BY `ID` ASC", batchDelete);
	}
	
	@Test
	public void testGetBatchWithSelfForeignKey(){
		String batchDelete = DMLUtils.getBackupBatch(migrateableMappingSelfForeignKey);
		assertNotNull(batchDelete);
		System.out.println(batchDelete);
		assertEquals("SELECT * FROM SOME_TABLE WHERE `ID` IN ( :BVIDLIST ) ORDER BY `ID` ASC", batchDelete);
	}

	@Test
	public void testGetBatchNoEtagNoSelfForeignKey(){
		String batchDelete = DMLUtils.getBackupBatch(migrateableMappingNoEtagNotSelfForeignKey);
		assertNotNull(batchDelete);
		System.out.println(batchDelete);
		assertEquals("SELECT * FROM SOME_TABLE WHERE `ID` IN ( :BVIDLIST ) ORDER BY `ID` ASC", batchDelete);
	}
	
	@Test
	public void testGetBatchInsertOrUdpate(){
		String sql = DMLUtils.getBatchInsertOrUdpate(migrateableMappingSelfForeignKey);
		assertNotNull(sql);
		System.out.println(sql);
		assertEquals("INSERT INTO SOME_TABLE(`ID`, `ETAG`, `PARENT_ID`) VALUES (:id, :etag, :parentId) ON DUPLICATE KEY UPDATE `ETAG` = :etag, `PARENT_ID` = :parentId", sql);
	}
	
	@Test
	public void testGetBatchInsertOrUdpateKeyOnly(){
		String sql = DMLUtils.getBatchInsertOrUdpate(migrateableMappingNoEtagNotSelfForeignKey);
		assertNotNull(sql);
		System.out.println(sql);
		assertEquals("INSERT INTO SOME_TABLE(`ID`) VALUES (:id)", sql);
	}
	
}
