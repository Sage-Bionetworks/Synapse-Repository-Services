package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;

import com.google.common.collect.Lists;

public class FileViewUtilsTest {

	
	@Test
	public void testCreateSQLForSchemaAllFileColumns(){
		// Test all columns
		List<ColumnModel> allColumns = FileEntityFields.getAllColumnModels();
		// call under test
		String sql = FileViewUtils.createSQLForSchema(allColumns);
		String expected = 
				"SELECT N.ID as 'id'"
				+ ", N.CURRENT_REV_NUM as 'currentVersion'"
				+ ", N.NAME as 'name'"
				+ ", N.CREATED_ON as 'createdOn'"
				+ ", N.CREATED_BY as 'createdBy'"
				+ ", N.ETAG as 'etag'"
				+ ", N.PARENT_ID as 'parentId'"
				+ ", N.BENEFACTOR_ID as 'benefactorId'"
				+ ", N.PROJECT_ID as 'projectId'"
				+ ", R.MODIFIED_ON as 'modifiedOn'"
				+ ", R.MODIFIED_BY as 'modifiedBy'"
				+ ", R.FILE_HANDLE_ID as 'dataFileHandleId'"
				+ " FROM JDONODE N, JDOREVISION R"
				+ " WHERE"
				+ " N.NODE_TYPE = 'file'"
				+ " AND N.PARENT_ID IN (:ids_param)"
				+ " AND N.ID = R.OWNER_NODE_ID"
				+ " AND N.CURRENT_REV_NUM = R.NUMBER";
		assertEquals(expected, sql);
	}
	
	@Test
	public void testCreateSQLForSchemaNodeOnly(){
		// only node should be hit with this query
		List<ColumnModel> schema = Lists.newArrayList(FileEntityFields.id.getColumnModel());
		String sql = FileViewUtils.createSQLForSchema(schema);
		String expected = 
				"SELECT N.ID as 'id'"
				+ ", N.CURRENT_REV_NUM as 'currentVersion'"
				+ " FROM JDONODE N"
				+ " WHERE N.NODE_TYPE = 'file'"
				+ " AND N.PARENT_ID IN (:ids_param)";
		assertEquals(expected, sql);
	}
	
	@Test
	public void testCreateSQLForSchemaRevisionOnly(){
		// node and revision should be hit since a revision column is used.
		List<ColumnModel> schema = Lists.newArrayList(FileEntityFields.modifiedBy.getColumnModel());
		String sql = FileViewUtils.createSQLForSchema(schema);
		String expected = 
				"SELECT N.ID as 'id'"
				+ ", N.CURRENT_REV_NUM as 'currentVersion'"
				+ ", R.MODIFIED_BY as 'modifiedBy'"
				+ " FROM JDONODE N, JDOREVISION R"
				+ " WHERE N.NODE_TYPE = 'file'"
				+ " AND N.PARENT_ID IN (:ids_param)"
				+ " AND N.ID = R.OWNER_NODE_ID"
				+ " AND N.CURRENT_REV_NUM = R.NUMBER";
		assertEquals(expected, sql);
	}
	
	@Test
	public void testCreateSQLForSchemaWithAnnotation(){
		// Use an annotation column
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		cm.setName("foo");
		cm.setMaximumSize(23L);
		List<ColumnModel> schema = Lists.newArrayList(cm);
		String sql = FileViewUtils.createSQLForSchema(schema);
		String expected = 
				"SELECT N.ID as 'id'"
				+ ", N.CURRENT_REV_NUM as 'currentVersion'"
				+ ", R.ANNOTATIONS"
				+ " FROM JDONODE N"
				+ ", JDOREVISION R"
				+ " WHERE N.NODE_TYPE = 'file'"
				+ " AND N.PARENT_ID IN (:ids_param)"
				+ " AND N.ID = R.OWNER_NODE_ID"
				+ " AND N.CURRENT_REV_NUM = R.NUMBER";
		assertEquals(expected, sql);
	}
}
