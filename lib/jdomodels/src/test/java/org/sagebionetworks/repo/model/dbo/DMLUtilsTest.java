package org.sagebionetworks.repo.model.dbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;

public class DMLUtilsTest {
	
	// Here is our simple mapping.
	private TableMapping<Object> mapping = new TableMapping<Object>() {
		
		@Override
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return null;
		}
		
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
		
		@Override
		public String getDDLFileName() {
			return "Example.sql";
		}

		@Override
		public Class<? extends Object> getDBOClass() {
			// TODO Auto-generated method stub
			return null;
		}
	};
	
	private TableMapping<Object> mappingTwoKeys = new TableMapping<Object>() {
		
		@Override
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return null;
		}
		
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
		@Override
		public String getDDLFileName() {
			return "Example.sql";
		}

		@Override
		public Class<? extends Object> getDBOClass() {
			// TODO Auto-generated method stub
			return null;
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

}
