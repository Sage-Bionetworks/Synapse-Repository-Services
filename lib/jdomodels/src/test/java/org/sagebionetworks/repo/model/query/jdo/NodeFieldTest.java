package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test the mapping of NodeFields.
 * @author jmhill
 *
 */
public class NodeFieldTest {
	
	@Test
	public void testID(){
		assertEquals("id", NodeField.ID.getFieldName());
		assertEquals(SqlConstants.COL_NODE_ID, NodeField.ID.getColumnName());
		assertEquals(SqlConstants.TABLE_NODE, NodeField.ID.getTableName());
		assertEquals(SqlConstants.NODE_ALIAS, NodeField.ID.getTableAlias());
	}
	
	@Test
	public void testName(){
		assertEquals("name", NodeField.NAME.getFieldName());
		assertEquals(SqlConstants.COL_NODE_NAME, NodeField.NAME.getColumnName());
		assertEquals(SqlConstants.TABLE_NODE, NodeField.NAME.getTableName());
		assertEquals(SqlConstants.NODE_ALIAS, NodeField.NAME.getTableAlias());
	}
	
	@Test
	public void testParentId(){
		assertEquals("parentId", NodeField.PARENT_ID.getFieldName());
		assertEquals(SqlConstants.COL_NODE_PARENT_ID, NodeField.PARENT_ID.getColumnName());
		assertEquals(SqlConstants.TABLE_NODE, NodeField.PARENT_ID.getTableName());
		assertEquals(SqlConstants.NODE_ALIAS, NodeField.PARENT_ID.getTableAlias());
	}
	
	@Test
	public void testCreatedBy(){
		assertEquals("createdByPrincipalId", NodeField.CREATED_BY.getFieldName());
		assertEquals(SqlConstants.COL_NODE_CREATED_BY, NodeField.CREATED_BY.getColumnName());
		assertEquals(SqlConstants.TABLE_NODE, NodeField.CREATED_BY.getTableName());
		assertEquals(SqlConstants.NODE_ALIAS, NodeField.CREATED_BY.getTableAlias());
	}
	
	@Test
	public void testCreatedOn(){
		assertEquals("createdOn", NodeField.CREATED_ON.getFieldName());
		assertEquals(SqlConstants.COL_NODE_CREATED_ON, NodeField.CREATED_ON.getColumnName());
		assertEquals(SqlConstants.TABLE_NODE, NodeField.CREATED_ON.getTableName());
		assertEquals(SqlConstants.NODE_ALIAS, NodeField.CREATED_ON.getTableAlias());
	}
	
	@Test
	public void testModifedBy(){
		assertEquals("modifiedByPrincipalId", NodeField.MODIFIED_BY.getFieldName());
		assertEquals(SqlConstants.COL_REVISION_MODIFIED_BY, NodeField.MODIFIED_BY.getColumnName());
		assertEquals(SqlConstants.TABLE_REVISION, NodeField.MODIFIED_BY.getTableName());
		assertEquals(SqlConstants.REVISION_ALIAS, NodeField.MODIFIED_BY.getTableAlias());
	}
	
	@Test
	public void testModifedOn(){
		assertEquals("modifiedOn", NodeField.MODIFIED_ON.getFieldName());
		assertEquals(SqlConstants.COL_REVISION_MODIFIED_ON, NodeField.MODIFIED_ON.getColumnName());
		assertEquals(SqlConstants.TABLE_REVISION, NodeField.MODIFIED_ON.getTableName());
		assertEquals(SqlConstants.REVISION_ALIAS, NodeField.MODIFIED_ON.getTableAlias());
	}
	
	@Test
	public void testNodeType(){
		assertEquals("nodeType", NodeField.NODE_TYPE.getFieldName());
		assertEquals(SqlConstants.COL_NODE_TYPE, NodeField.NODE_TYPE.getColumnName());
		assertEquals(SqlConstants.TABLE_NODE, NodeField.NODE_TYPE.getTableName());
		assertEquals(SqlConstants.NODE_ALIAS, NodeField.NODE_TYPE.getTableAlias());
	}
	
	@Test
	public void testETag(){
		assertEquals("eTag", NodeField.E_TAG.getFieldName());
		assertEquals(SqlConstants.COL_NODE_ETAG, NodeField.E_TAG.getColumnName());
		assertEquals(SqlConstants.TABLE_NODE, NodeField.E_TAG.getTableName());
		assertEquals(SqlConstants.NODE_ALIAS, NodeField.E_TAG.getTableAlias());
	}
	
	@Test
	public void testVersionNumber(){
		assertEquals("versionNumber", NodeField.VERSION_NUMBER.getFieldName());
		assertEquals(SqlConstants.COL_REVISION_NUMBER, NodeField.VERSION_NUMBER.getColumnName());
		assertEquals(SqlConstants.TABLE_REVISION, NodeField.VERSION_NUMBER.getTableName());
		assertEquals(SqlConstants.REVISION_ALIAS, NodeField.VERSION_NUMBER.getTableAlias());
	}
	
	@Test
	public void testVersionComment(){
		assertEquals("versionComment", NodeField.VERSION_COMMENT.getFieldName());
		assertEquals(SqlConstants.COL_REVISION_COMMENT, NodeField.VERSION_COMMENT.getColumnName());
		assertEquals(SqlConstants.TABLE_REVISION, NodeField.VERSION_COMMENT.getTableName());
		assertEquals(SqlConstants.REVISION_ALIAS, NodeField.VERSION_COMMENT.getTableAlias());
	}
	
	@Test
	public void testVersionLabel(){
		assertEquals("versionLabel", NodeField.VERSION_LABEL.getFieldName());
		assertEquals(SqlConstants.COL_REVISION_LABEL, NodeField.VERSION_LABEL.getColumnName());
		assertEquals(SqlConstants.TABLE_REVISION, NodeField.VERSION_LABEL.getTableName());
		assertEquals(SqlConstants.REVISION_ALIAS, NodeField.VERSION_LABEL.getTableAlias());
	}
	
	
	@Test
	public void testBenefacrorId(){
		assertEquals("benefactorId", NodeField.BENEFACTOR_ID.getFieldName());
		assertEquals(null, NodeField.BENEFACTOR_ID.getColumnName());
		assertEquals(SqlConstants.TABLE_NODE, NodeField.BENEFACTOR_ID.getTableName());
		assertEquals(SqlConstants.NODE_ALIAS, NodeField.BENEFACTOR_ID.getTableAlias());
	}

}
