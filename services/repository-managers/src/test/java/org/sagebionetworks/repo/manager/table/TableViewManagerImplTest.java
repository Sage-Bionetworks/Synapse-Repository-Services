package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.AnnotationUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityField;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

@RunWith(MockitoJUnitRunner.class)
public class TableViewManagerImplTest {

	@Mock
	ViewScopeDao viewScopeDao;
	@Mock
	ColumnModelManager columnModelManager;
	@Mock
	TableManagerSupport tableManagerSupport;
	@Mock
	ColumnModelDAO columnModelDao;
	@Mock
	NodeManager mockNodeManager;
	
	@InjectMocks
	TableViewManagerImpl manager;
	
	UserInfo userInfo;
	List<String> schema;
	List<String> scope;
	String viewId;
	Long viewIdLong;
	IdAndVersion idAndVersion;
	Long viewType;
	
	Set<Long> scopeIds;
	ViewScope viewScope;
	long rowCount;
	List<Row> rows;
	long viewCRC;
	List<ColumnModel> viewSchema;
	ColumnModel etagColumn;
	ColumnModel anno1;
	ColumnModel anno2;
	ColumnModel dateColumn;
	SparseRowDto row;
	
	Annotations annotations;

	@Before
	public void before(){
		userInfo = new UserInfo(false, 888L);
		schema = Lists.newArrayList("1","2","3");
		scope = Lists.newArrayList("syn123", "syn456");
		scopeIds = new HashSet<Long>(KeyFactory.stringToKey(scope));
		
		viewId = "syn555";
		viewIdLong = KeyFactory.stringToKey(viewId);
		idAndVersion = IdAndVersion.parse(viewId);
		viewType =ViewTypeMask.File.getMask();
		
		viewScope = new ViewScope();
		viewScope.setScope(scope);
		viewScope.setViewTypeMask(viewType);
		
		doAnswer(new Answer<ColumnModel>(){
			@Override
			public ColumnModel answer(InvocationOnMock invocation) throws Throwable {
				return (ColumnModel) invocation.getArguments()[0];
			}}).when(columnModelDao).createColumnModel(any(ColumnModel.class));
		
		when(tableManagerSupport.getAllContainerIdsForViewScope(idAndVersion, viewType)).thenReturn(scopeIds);
		
		rowCount = 13;
		rows = new LinkedList<Row>();
		for(long i=0; i<rowCount; i++){
			Row row = new Row();
			row.setRowId(i);
			rows.add(row);
		}
	
		doAnswer(new Answer<ColumnModel>(){
			@Override
			public ColumnModel answer(InvocationOnMock invocation)
					throws Throwable {
				EntityField field = (EntityField) invocation.getArguments()[0];
				return field.getColumnModel();
			}}).when(tableManagerSupport).getColumnModel(any(EntityField.class));
		
		doAnswer(new Answer<List<ColumnModel>>(){
			@Override
			public List<ColumnModel> answer(InvocationOnMock invocation)
					throws Throwable {
				Object[] fields =  invocation.getArguments();
				List<ColumnModel> results = new LinkedList<ColumnModel>();
				for(Object object: fields){
					EntityField field = (EntityField) object;
					results.add(field.getColumnModel());
				}
				return results;
			}}).when(tableManagerSupport).getColumnModels(any());
		
		annotations = new Annotations();
		when(mockNodeManager.getUserAnnotations(any(UserInfo.class), anyString())).thenReturn(annotations);

		anno1 = new ColumnModel();
		anno1.setColumnType(ColumnType.STRING);
		anno1.setName("foo");
		anno1.setMaximumSize(50L);
		anno1.setId("1");
		
		anno2 = new ColumnModel();
		anno2.setColumnType(ColumnType.INTEGER);
		anno2.setName("bar");
		anno2.setId("2");
		
		dateColumn = new ColumnModel();
		dateColumn.setName("aDate");
		dateColumn.setColumnType(ColumnType.DATE);
		dateColumn.setId("3");
		
		etagColumn = EntityField.etag.getColumnModel();
		etagColumn.setId("3");
		
		viewSchema = new LinkedList<ColumnModel>();
		viewSchema.add(etagColumn);
		viewSchema.add(anno1);
		viewSchema.add(anno2);
		
		Map<String, String> values = new HashMap<>();
		values.put(etagColumn.getId(), "anEtag");
		values.put(anno1.getId(), "aString");
		values.put(anno2.getId(), "123");
		row = new SparseRowDto();
		row.setRowId(111L);
		row.setValues(values);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetViewSchemaAndScopeOverLimit(){
		IllegalArgumentException overLimit = new IllegalArgumentException("Over limit");
		doThrow(overLimit).when(tableManagerSupport).validateScopeSize(anySet(), any(Long.class));
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, viewScope, viewId);
	}
	
	@Test
	public void testSetViewSchemaAndScope(){
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, viewScope, viewId);
		// the size should be validated
		verify(tableManagerSupport).validateScopeSize(scopeIds, viewType);
		verify(viewScopeDao).setViewScopeAndType(555L, Sets.newHashSet(123L, 456L), viewType);
		verify(columnModelManager).bindColumnsToDefaultVersionOfObject(schema, viewId);
		verify(tableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);
	}
	
	@Test
	public void testSetViewSchemaAndScopeTooManyColumns(){
		this.schema = new LinkedList<>();
		int columnCount = TableViewManagerImpl.MAX_COLUMNS_PER_VIEW+1;
		for(int i=0; i<columnCount; i++) {
			schema.add(""+i);
		}
		try {
			// call under test
			manager.setViewSchemaAndScope(userInfo, schema, viewScope, viewId);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
			assertTrue(e.getMessage().contains(""+TableViewManagerImpl.MAX_COLUMNS_PER_VIEW));
		}
	}
	
	
	@Test
	public void testSetViewSchemaAndScopeWithNullSchema(){
		schema = null;
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, viewScope, viewId);
		verify(viewScopeDao).setViewScopeAndType(555L, Sets.newHashSet(123L, 456L), viewType);
		verify(columnModelManager).bindColumnsToDefaultVersionOfObject(null, viewId);
		verify(tableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);
	}
	
	@Test
	public void testSetViewSchemaAndScopeWithNullScope(){
		viewScope.setScope(null);
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, viewScope, viewId);
		verify(viewScopeDao).setViewScopeAndType(555L, null, viewType);
		verify(columnModelManager).bindColumnsToDefaultVersionOfObject(schema, viewId);
		verify(tableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetViewSchemaAndScopeWithNullType(){
		viewScope.setViewType(null);
		viewScope.setViewTypeMask(null);
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, viewScope, viewId);
		
	}
	
	/**
	 * Project cannot be combined with anything else.
	 */
	@Test
	public void testSetViewSchemaAndScopeWithProjectCombinedWithOtherTypes(){
		long mask = ViewTypeMask.Project.getMask() | ViewTypeMask.File.getMask();
		viewScope.setViewTypeMask(mask);
		try {
			// call under test
			manager.setViewSchemaAndScope(userInfo, schema, viewScope, viewId);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals(TableViewManagerImpl.PROJECT_TYPE_CANNOT_BE_COMBINED_WITH_ANY_OTHER_TYPE, e.getMessage());
		}
	}
	
	/**
	 * Project cannot be combined with anything else.
	 */
	@Test
	public void testSetViewSchemaAndScopeWithProjectOnly(){
		long mask = ViewTypeMask.Project.getMask();
		viewScope.setViewTypeMask(mask);
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, viewScope, viewId);
		verify(viewScopeDao).setViewScopeAndType(555L, Sets.newHashSet(123L, 456L), mask);
	}
	
	@Test
	public void testFindViewsContainingEntity(){
		Set<Long> path = Sets.newHashSet(123L,456L);
		when(tableManagerSupport.getEntityPath(idAndVersion)).thenReturn(path);
		Set<Long> expected = Sets.newHashSet(789L);
		when(viewScopeDao.findViewScopeIntersectionWithPath(path)).thenReturn(expected);
		// call under test
		Set<Long> results = manager.findViewsContainingEntity(viewId);
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetViewSchemaWithRequiredColumnsNoAdditions(){
		List<ColumnModel> rawSchema = Lists.newArrayList(
				EntityField.benefactorId.getColumnModel(),
				EntityField.createdBy.getColumnModel(),
				EntityField.etag.getColumnModel()
				);
		when(tableManagerSupport.getColumnModelsForTable(idAndVersion)).thenReturn(rawSchema);
		// call under test
		List<ColumnModel> result = manager.getViewSchema(viewId);
		assertEquals(rawSchema, result);
	}
	
	@Test
	public void testGetViewSchema(){
		List<ColumnModel> rawSchema = Lists.newArrayList(
				EntityField.createdBy.getColumnModel(),
				EntityField.createdOn.getColumnModel(),
				EntityField.benefactorId.getColumnModel()
				);
		when(tableManagerSupport.getColumnModelsForTable(idAndVersion)).thenReturn(rawSchema);
		// call under test
		List<ColumnModel> result = manager.getViewSchema(viewId);
		
		List<ColumnModel> expected = Lists.newArrayList(
				EntityField.createdBy.getColumnModel(),
				EntityField.createdOn.getColumnModel(),
				EntityField.benefactorId.getColumnModel()
				);
		assertEquals(expected, result);
	}
	
	@Test
	public void testApplySchemaChange(){
		ColumnChange change = new ColumnChange();
		change.setOldColumnId(null);
		change.setNewColumnId("456");
		List<ColumnChange> changes = Lists.newArrayList(change);
		ColumnModel model = EntityField.benefactorId.getColumnModel();
		model.setId(change.getNewColumnId());
		List<ColumnModel> schema = Lists.newArrayList(model);
		List<String> newColumnIds = Lists.newArrayList(change.getNewColumnId());
		when(columnModelManager.calculateNewSchemaIdsAndValidate(viewId, changes, newColumnIds)).thenReturn(newColumnIds);
		when(columnModelManager.bindColumnsToDefaultVersionOfObject(newColumnIds, viewId)).thenReturn(schema);
		
		// call under test
		List<ColumnModel> newSchema = manager.applySchemaChange(userInfo, viewId, changes, newColumnIds);
		assertEquals(schema, newSchema);
		verify(columnModelManager).calculateNewSchemaIdsAndValidate(viewId, changes, newColumnIds);
		verify(tableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);
	}
	
	/**
	 * Test for PLFM-4733. Schema change on a view should enforce the view max.
	 * 
	 */
	@Test 
	public void testApplySchemaChangeOverLimit(){
		String viewId = "syn123";
		ColumnChange change = new ColumnChange();
		change.setOldColumnId(null);
		change.setNewColumnId("456");
		List<ColumnChange> changes = Lists.newArrayList(change);
		ColumnModel model = EntityField.benefactorId.getColumnModel();
		model.setId(change.getNewColumnId());
		List<ColumnModel> schema = Lists.newArrayList(model);
		// the new schema should be over the limit
		List<String> newSchemaColumnIds = new LinkedList<>();
		int columnCount = TableViewManagerImpl.MAX_COLUMNS_PER_VIEW+1;
		for(int i=0; i<columnCount; i++) {
			newSchemaColumnIds.add(""+i);
		}
		List<String> newColumnIds = Lists.newArrayList(change.getNewColumnId());
		when(columnModelManager.calculateNewSchemaIdsAndValidate(viewId, changes, newColumnIds)).thenReturn(newSchemaColumnIds);
		when(columnModelManager.getAndValidateColumnModels(newColumnIds)).thenReturn(schema);
		
		try {
			// call under test
			manager.applySchemaChange(userInfo, viewId, changes, newColumnIds);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
			assertTrue(e.getMessage().contains(""+TableViewManagerImpl.MAX_COLUMNS_PER_VIEW));
		}
	}
	
	@Test
	public void testGetTableSchema(){
		when(columnModelManager.getColumnIdForTable(idAndVersion)).thenReturn(schema);
		List<String> retrievedSchema = manager.getTableSchema(viewId);
		assertEquals(schema, retrievedSchema);
	}
	
	@Test
	public void testUpdateAnnotationsFromValues(){
		Annotations annos = new Annotations();
		Map<String, String> values = new HashMap<>();
		values.put(EntityField.etag.name(), "anEtag");
		values.put(anno1.getId(), "aString");
		values.put(anno2.getId(), "123");
		// call under test
		boolean updated = TableViewManagerImpl.updateAnnotationsFromValues(annos, viewSchema, values);
		assertTrue(updated);
		assertEquals("aString",annos.getSingleValue(anno1.getName()));
		assertEquals(new Long(123),annos.getSingleValue(anno2.getName()));
		// etag should not be included.
		assertNull(annos.getSingleValue(EntityField.etag.name()));
	}
	
	@Test
	public void testUpdateAnnotationsFromValuesSameNameDifferntType(){
		Annotations annos = new Annotations();
		annos.addAnnotation(anno1.getName(), 456L);
		annos.addAnnotation(anno2.getName(), "not a long");
		// update the values.
		Map<String, String> values = new HashMap<>();
		values.put(EntityField.etag.name(), "anEtag");
		values.put(anno1.getId(), "aString");
		values.put(anno2.getId(), "123");
		// call under test
		boolean updated = TableViewManagerImpl.updateAnnotationsFromValues(annos, viewSchema, values);
		// the resulting annotations must be valid.
		AnnotationUtils.validateAnnotations(annos);
		assertTrue(updated);
		assertEquals("aString",annos.getSingleValue(anno1.getName()));
		assertEquals(new Long(123),annos.getSingleValue(anno2.getName()));
		// etag should not be included.
		assertNull(annos.getSingleValue(EntityField.etag.name()));
	}
	
	/**
	 * This test was added for PLFM-4706
	 * 
	 */
	@Test
	public void testUpdateAnnotationsDate() throws IOException, JSONObjectAdapterException {
		Date date = new Date(1509744902000L);
		viewSchema = Lists.newArrayList(dateColumn);
		Annotations annos = new Annotations();
		// update the values.
		Map<String, String> values = new HashMap<>();
		values.put(dateColumn.getId(), ""+date.getTime());
		// call under test
		boolean updated = TableViewManagerImpl.updateAnnotationsFromValues(annos, viewSchema, values);
		// the resulting annotations must be valid.
		AnnotationUtils.validateAnnotations(annos);
		assertTrue(updated);
		/*
		 * Copy the annotations by writing to XML.
		 * Note: With PLFM-4706 this is where the date gets 
		 * converted to the same day at time 0.
		 */
		Annotations annotationCopy = EntityFactory.createEntityFromJSONObject(EntityFactory.createJSONObjectForEntity(annos), Annotations.class);

		Object value = annotationCopy.getSingleValue(dateColumn.getName());
		assertNotNull(value);
		assertTrue(value instanceof Date);
		Date dateValue = (Date)value;
		assertEquals(date.getTime(), dateValue.getTime());
	}
	
	@Test
	public void testUpdateAnnotationsFromValuesEmptyScheam(){
		Annotations annos = new Annotations();
		Map<String, String> values = new HashMap<>();
		// call under test
		boolean updated = TableViewManagerImpl.updateAnnotationsFromValues(annos, new LinkedList<ColumnModel>(), values);
		assertFalse(updated);
	}
	
	@Test
	public void testUpdateAnnotationsFromValuesEmptyValues(){
		Annotations annos = new Annotations();
		Map<String, String> values = new HashMap<>();
		// call under test
		boolean updated = TableViewManagerImpl.updateAnnotationsFromValues(annos, viewSchema, values);
		assertFalse(updated);
	}
	
	@Test
	public void testUpdateAnnotationsFromValuesDelete(){
		Annotations annos = new Annotations();
		// start with an annotation.
		annos.addAnnotation(anno1.getName(), "startValue");
		Map<String, String> values = new HashMap<>();
		values.put(anno1.getId(), null);
		// call under test
		boolean updated = TableViewManagerImpl.updateAnnotationsFromValues(annos, viewSchema, values);
		assertTrue(updated);
		assertFalse(annos.getStringAnnotations().containsKey(anno1.getName()));
		assertEquals(null, annos.getSingleValue(anno1.getName()));
	}
	
	@Test
	public void testGetEtagColumn(){
		ColumnModel etag = TableViewManagerImpl.getEtagColumn(viewSchema);
		assertEquals(etagColumn, etag);
	}
	
	@Test 
	public void testGetEtagColumnMissing(){
		try {
			TableViewManagerImpl.getEtagColumn(new LinkedList<ColumnModel>());
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals(TableViewManagerImpl.ETG_COLUMN_MISSING, e.getMessage());
		}
	}
	
	@Test
	public void testUpdateEntityInView(){
		// call under test
		manager.updateEntityInView(userInfo, viewSchema, row);
		// this should trigger an update
		verify(mockNodeManager).updateUserAnnotations(userInfo, "syn111", annotations);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateEntityInViewNullRow(){
		row = null;
		// call under test
		manager.updateEntityInView(userInfo, viewSchema, row);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateEntityInViewNullRowId(){
		row.setRowId(null);
		// call under test
		manager.updateEntityInView(userInfo, viewSchema, row);
	}
	
	@Test
	public void testUpdateEntityInViewNullValues(){
		row.setValues(null);
		// call under test
		manager.updateEntityInView(userInfo, viewSchema, row);
		// this should not trigger an update
		verify(mockNodeManager, never()).updateUserAnnotations(any(UserInfo.class), anyString(), any(Annotations.class));
	}
	
	@Test
	public void testUpdateEntityInViewEmptyValues(){
		row.setValues(new HashMap<String, String>());
		// call under test
		manager.updateEntityInView(userInfo, viewSchema, row);
		// this should not trigger an update
		verify(mockNodeManager, never()).updateUserAnnotations(any(UserInfo.class), anyString(), any(Annotations.class));
	}
	
	@Test
	public void testUpdateEntityInViewNoChanges(){
		row.getValues().remove(anno1.getId());
		row.getValues().remove(anno2.getId());
		// call under test
		manager.updateEntityInView(userInfo, viewSchema, row);
		// this should not trigger an update
		verify(mockNodeManager, never()).updateUserAnnotations(any(UserInfo.class), anyString(), any(Annotations.class));
	}
	
	@Test
	public void testUpdateEntityInViewMissingEtag(){
		// Each row must include the etag.
		row.getValues().remove(etagColumn.getId());
		// call under test
		try {
			manager.updateEntityInView(userInfo, viewSchema, row);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals(TableViewManagerImpl.ETAG_MISSING_MESSAGE, e.getMessage());
		}
	}

}
