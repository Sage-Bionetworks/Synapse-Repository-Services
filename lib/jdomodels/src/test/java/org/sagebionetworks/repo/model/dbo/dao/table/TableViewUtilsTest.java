package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.*;
import static  org.mockito.Mockito.*;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ANNOS_BLOB;





import java.io.IOException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;

import com.google.common.collect.Lists;

public class TableViewUtilsTest {
	
	@Mock
	ResultSet mockResultSet;
	
	ColumnModel colString;
	ColumnModel colDouble;
	ColumnModel colDate;
	ColumnModel colInteger;
	ColumnModel colLink;
	
	EntityType type;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		colString = new ColumnModel();
		colString.setName("stringKey");
		colString.setColumnType(ColumnType.STRING);
		
		colDate = new ColumnModel();
		colDate.setName("dateKey");
		colDate.setColumnType(ColumnType.DATE);
		
		colDouble = new ColumnModel();
		colDouble.setName("doubleKey");
		colDouble.setColumnType(ColumnType.DOUBLE);
		
		colInteger = new ColumnModel();
		colInteger.setName("integerKey");
		colInteger.setColumnType(ColumnType.INTEGER);
		
		colLink = new ColumnModel();
		colLink.setName("linkKey");
		colLink.setColumnType(ColumnType.LINK);
		
		type = EntityType.file;
	}

	@Test
	public void testGetFileEntityFields(){
		ColumnModel annotation = new ColumnModel();
		annotation.setName("foo");
		annotation.setColumnType(ColumnType.INTEGER);
		
		ColumnModel primary = FileEntityFields.id.getColumnModel();
		List<ColumnModel> input = Lists.newArrayList(annotation, primary);
		// call under test
		List<FileEntityFields> results = TableViewUtils.getFileEntityFields(input);
		assertEquals(Lists.newArrayList(FileEntityFields.id), results);
	}
	
	@Test
	public void testGetFileEntityFieldsEmpty(){
		ColumnModel annotation = new ColumnModel();
		annotation.setName("foo");
		annotation.setColumnType(ColumnType.INTEGER);
		
		List<ColumnModel> input = Lists.newArrayList(annotation);
		// call under test
		List<FileEntityFields> results = TableViewUtils.getFileEntityFields(input);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}

	
	@Test
	public void testGetNonFileEntityFieldColumns(){
		ColumnModel annotation = new ColumnModel();
		annotation.setName("foo");
		annotation.setColumnType(ColumnType.INTEGER);
		
		ColumnModel primary = FileEntityFields.id.getColumnModel();
		List<ColumnModel> input = Lists.newArrayList(annotation, primary);
		// call under test
		List<ColumnModel> results = TableViewUtils.getNonFileEntityFieldColumns(input);
		assertEquals(Lists.newArrayList(annotation), results);
	}
	
	@Test
	public void testGetNonFileEntityFieldColumnsEmpty(){
		ColumnModel primary = FileEntityFields.id.getColumnModel();
		List<ColumnModel> input = Lists.newArrayList(primary);
		// call under test
		List<ColumnModel> results = TableViewUtils.getNonFileEntityFieldColumns(input);
		assertNotNull(results);
		assertTrue(results.isEmpty());;
	}
	
	@Test
	public void testCreateSQLForSchemaAllFileColumns(){
		// Test all columns
		List<ColumnModel> allColumns = FileEntityFields.getAllColumnModels();
		// call under test
		String sql = TableViewUtils.createSQLForSchema(allColumns, type);
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
		String sql = TableViewUtils.createSQLForSchema(schema, type);
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
		String sql = TableViewUtils.createSQLForSchema(schema, type);
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
		String sql = TableViewUtils.createSQLForSchema(schema, type);
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
	
	@Test
	public void testExtractAnnotations(){
		NamedAnnotations annos = new NamedAnnotations();
		annos.getAdditionalAnnotations().addAnnotation(colDate.getName(), new Date(23L));
		annos.getAdditionalAnnotations().addAnnotation(colString.getName(), "someString");
		annos.getAdditionalAnnotations().addAnnotation(colInteger.getName(), new Long(99));
		annos.getAdditionalAnnotations().addAnnotation(colDouble.getName(), new Double(1.234));
		
		List<ColumnModel> annotations = Lists.newArrayList(colDate, colDouble, colInteger, colLink, colString);
		
		// call under test
		Map<String, String> results = TableViewUtils.extractAnnotations(annotations, annos);
		assertNotNull(results);
		assertEquals("23", results.get(colDate.getName()));
		assertEquals("someString", results.get(colString.getName()));
		assertEquals("99", results.get(colInteger.getName()));
		assertEquals("1.234", results.get(colDouble.getName()));
		assertEquals(null, results.get(colLink.getName()));
	}
	
	/**
	 * It is possible to create multiple annotations with the same name
	 * but different data type.  For example, it is possible to have a
	 * string annotation of foo="someString" and a double annotation
	 * of foo=1.23 on the same Entity.  Therefore, annotation values
	 * must be extracted using both the name and type.
	 */
	@Test
	public void testExtractAnnotationsWithDuplicateAnnotationsString(){
		// Setup multiple annotations with the same name but different type.
		NamedAnnotations annos = new NamedAnnotations();
		annos.getAdditionalAnnotations().addAnnotation(colString.getName(), new Date(23L));
		annos.getAdditionalAnnotations().addAnnotation(colString.getName(), "someString");
		annos.getAdditionalAnnotations().addAnnotation(colString.getName(), new Long(99));
		annos.getAdditionalAnnotations().addAnnotation(colString.getName(), new Double(1.234));
		
		List<ColumnModel> annotations = Lists.newArrayList(colString);
		
		// call under test
		Map<String, String> results = TableViewUtils.extractAnnotations(annotations, annos);
		assertNotNull(results);
		assertEquals("someString", results.get(colString.getName()));
	}
	
	@Test
	public void testExtractAnnotationsWithDuplicateAnnotationsDate(){
		// Setup multiple annotations with the same name but different type.
		NamedAnnotations annos = new NamedAnnotations();
		annos.getAdditionalAnnotations().addAnnotation(colDate.getName(), new Date(23L));
		annos.getAdditionalAnnotations().addAnnotation(colDate.getName(), "someString");
		annos.getAdditionalAnnotations().addAnnotation(colDate.getName(), new Long(99));
		annos.getAdditionalAnnotations().addAnnotation(colDate.getName(), new Double(1.234));
		
		List<ColumnModel> annotations = Lists.newArrayList(colDate);
		
		// call under test
		Map<String, String> results = TableViewUtils.extractAnnotations(annotations, annos);
		assertNotNull(results);
		assertEquals("23", results.get(colDate.getName()));
	}
	
	@Test
	public void testExtractAnnotationsWithDuplicateAnnotationsLong(){
		// Setup multiple annotations with the same name but different type.
		NamedAnnotations annos = new NamedAnnotations();
		annos.getAdditionalAnnotations().addAnnotation(colInteger.getName(), new Date(23L));
		annos.getAdditionalAnnotations().addAnnotation(colInteger.getName(), "someString");
		annos.getAdditionalAnnotations().addAnnotation(colInteger.getName(), new Long(99));
		annos.getAdditionalAnnotations().addAnnotation(colInteger.getName(), new Double(1.234));
		
		List<ColumnModel> annotations = Lists.newArrayList(colInteger);
		
		// call under test
		Map<String, String> results = TableViewUtils.extractAnnotations(annotations, annos);
		assertNotNull(results);
		assertEquals("99", results.get(colInteger.getName()));
	}
	
	@Test
	public void testExtractAnnotationsWithDuplicateAnnotationsDouble(){
		// Setup multiple annotations with the same name but different type.
		NamedAnnotations annos = new NamedAnnotations();
		annos.getAdditionalAnnotations().addAnnotation(colDouble.getName(), new Date(23L));
		annos.getAdditionalAnnotations().addAnnotation(colDouble.getName(), "someString");
		annos.getAdditionalAnnotations().addAnnotation(colDouble.getName(), new Long(99));
		annos.getAdditionalAnnotations().addAnnotation(colDouble.getName(), new Double(1.234));
		
		List<ColumnModel> annotations = Lists.newArrayList(colDouble);
		
		// call under test
		Map<String, String> results = TableViewUtils.extractAnnotations(annotations, annos);
		assertNotNull(results);
		assertEquals("1.234", results.get(colDouble.getName()));
	}
	
	@Test
	public void testExtractRowWithAllWithAnnotations() throws Exception {
		List<ColumnModel> schema = FileEntityFields.getAllColumnModels();
		// add an annotation
		schema.add(colString);
		List<ColumnModel> annotationColumns = TableViewUtils.getNonFileEntityFieldColumns(schema);
		Row expectedRow = setupSampleRowHelper(schema, annotationColumns);
		// call under test
		Row row = TableViewUtils.extractRow(schema, annotationColumns, mockResultSet);
		assertNotNull(row);
		assertEquals(expectedRow, row);
	}
	
	@Test
	public void testExtractRowWithNoAnnotations() throws Exception {
		List<ColumnModel> schema = Lists.newArrayList(FileEntityFields.createdBy.getColumnModel());
		List<ColumnModel> annotationColumns = TableViewUtils.getNonFileEntityFieldColumns(schema);
		Row expectedRow = setupSampleRowHelper(schema, annotationColumns);
		// call under test
		Row row = TableViewUtils.extractRow(schema, annotationColumns, mockResultSet);
		assertNotNull(row);
		assertEquals(expectedRow, row);
	}
	
	@Test
	public void testExtractRowWithAnnotationsOnly() throws Exception {
		List<ColumnModel> schema = Lists.newArrayList(colDate);
		List<ColumnModel> annotationColumns = TableViewUtils.getNonFileEntityFieldColumns(schema);
		Row expectedRow = setupSampleRowHelper(schema, annotationColumns);
		// call under test
		Row row = TableViewUtils.extractRow(schema, annotationColumns, mockResultSet);
		assertNotNull(row);
		assertEquals(expectedRow, row);
	}
	
	/**
	 * Help to setup the mocking for a query and capture the values in an expected Row.
	 * @param schema
	 * @param annotations
	 * @return
	 * @throws SQLException
	 * @throws IOException 
	 */
	private Row setupSampleRowHelper(List<ColumnModel> schema, List<ColumnModel> annotations) throws SQLException, IOException{
		long rowId = 123;
		long version = 456;
		when(mockResultSet.getLong(FileEntityFields.id.name())).thenReturn(rowId);
		when(mockResultSet.getLong(FileEntityFields.currentVersion.name())).thenReturn(version);
		
		Row row = new Row();
		row.setRowId(rowId);
		row.setVersionNumber(version);
		
		// add the annotations
		Map<String, String> annosMap = new HashMap<String, String>();
		if(!annotations.isEmpty()){
			NamedAnnotations annos = new NamedAnnotations();
			for(ColumnModel anno: annotations){
				if(ColumnType.STRING.equals(anno.getColumnType())){
					annos.getAdditionalAnnotations().addAnnotation(anno.getName(), "aString");
					annosMap.put(anno.getName(), "aString");
				}else if(ColumnType.DATE.equals(anno.getColumnType())){
					annos.getAdditionalAnnotations().addAnnotation(anno.getName(), new Date(555));
					annosMap.put(anno.getName(), "555");
				}else if(ColumnType.DOUBLE.equals(anno.getColumnType())){
					annos.getAdditionalAnnotations().addAnnotation(anno.getName(), new Double(3.44));
					annosMap.put(anno.getName(), "3.44");
				}else if(ColumnType.INTEGER.equals(anno.getColumnType())){
					annos.getAdditionalAnnotations().addAnnotation(anno.getName(), new Long(987));
					annosMap.put(anno.getName(), "987");
				}
			}
			byte[] bytes = JDOSecondaryPropertyUtils.compressAnnotations(annos);
			Blob mockBlob = Mockito.mock(Blob.class);
			when(mockBlob.getBytes(anyLong(), anyInt())).thenReturn(bytes);
			when(mockResultSet.getBlob(COL_REVISION_ANNOS_BLOB)).thenReturn(mockBlob);
		}
		// Add the rest of the data
		List<String> values = new LinkedList<String>();
		for(ColumnModel column: schema){
			String value = null;
			if(FileEntityFields.id.name().equals(column.getName())){
				value = ""+rowId;
				when(mockResultSet.getString(column.getName())).thenReturn(value);
			}else if(FileEntityFields.currentVersion.name().equals(column.getName())){
				value = ""+version;
				when(mockResultSet.getString(column.getName())).thenReturn(value);
			}else if(annosMap.containsKey(column.getName())){
				value = annosMap.get(column.getName());
			}else{
				value = column.getName()+"Value";
				when(mockResultSet.getString(column.getName())).thenReturn(value);
			}
			values.add(value);
		}
		row.setValues(values);
		return row;
	}
	
	@Test
	public void testContainsBenefactor(){
		List<ColumnModel> all = FileEntityFields.getAllColumnModels();
		// all under test
		assertTrue(TableViewUtils.containsBenefactor(all));
		// remove benefactor from the list.
		all.remove(FileEntityFields.benefactorId.getColumnModel());
		// call under test
		assertFalse(TableViewUtils.containsBenefactor(all));
	}
	
	@Test
	public void testGetFilterTypeForViewTypeFileView(){
		// call under test.
		EntityType entityType = TableViewUtils.getFilterTypeForViewType(EntityType.fileview);
		assertEquals(EntityType.file, entityType);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetEntityTypeForViewUnknown(){
		// call under test.
		TableViewUtils.getFilterTypeForViewType(EntityType.project);
	}
	
}
