package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityField;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.ViewType;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TableViewManagerImplTest {

	@Mock
	ViewScopeDao viewScopeDao;
	@Mock
	ColumnModelManager columnModelManager;
	@Mock
	TableManagerSupport tableManagerSupport;
	@Mock
	ColumnModelDAO columnModelDao;
	
	TableViewManagerImpl manager;
	
	UserInfo userInfo;
	List<String> schema;
	List<String> scope;
	String viewId;
	ViewType viewType;
	
	Set<Long> scopeIds;
	long rowCount;
	List<Row> rows;
	long viewCRC;

	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		manager = new TableViewManagerImpl();
		ReflectionTestUtils.setField(manager, "viewScopeDao", viewScopeDao);
		ReflectionTestUtils.setField(manager, "columModelManager", columnModelManager);
		ReflectionTestUtils.setField(manager, "tableManagerSupport", tableManagerSupport);
		ReflectionTestUtils.setField(manager, "columnModelDao", columnModelDao);
		
		userInfo = new UserInfo(false, 888L);
		schema = Lists.newArrayList("1","2","3");
		scope = Lists.newArrayList("syn123", "syn456");
		scopeIds = new HashSet<Long>(KeyFactory.stringToKey(scope));
		
		viewId = "syn555";
		viewType = ViewType.file;
		
		doAnswer(new Answer<ColumnModel>(){
			@Override
			public ColumnModel answer(InvocationOnMock invocation) throws Throwable {
				return (ColumnModel) invocation.getArguments()[0];
			}}).when(columnModelDao).createColumnModel(any(ColumnModel.class));
		
		when(tableManagerSupport.getAllContainerIdsForViewScope(viewId)).thenReturn(scopeIds);
		
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
			}}).when(tableManagerSupport).getColumnModels(Matchers.<EntityField>anyVararg());
		
		when(tableManagerSupport.getScopeContainerCount(anySetOf(Long.class))).thenReturn(10);
	}
	
	@Test
	public void testSetViewSchemaAndScopeOverLimit(){
		int containerCount = TableViewManagerImpl.MAX_CONTAINERS_PER_VIEW+1;
		when(tableManagerSupport.getScopeContainerCount(anySetOf(Long.class))).thenReturn(containerCount);
		try {
			// call under test
			manager.setViewSchemaAndScope(userInfo, schema, scope, viewType, viewId);
			fail("Should have failed");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains(""+containerCount));
		}
	}
	
	@Test
	public void testSetViewSchemaAndScope(){
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, scope, viewType, viewId);
		verify(viewScopeDao).setViewScopeAndType(555L, Sets.newHashSet(123L, 456L), viewType);
		verify(columnModelManager).bindColumnToObject(userInfo, schema, viewId);
		verify(tableManagerSupport).setTableToProcessingAndTriggerUpdate(viewId);
	}
	
	@Test
	public void testSetViewSchemaAndScopeWithNullSchema(){
		schema = null;
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, scope, viewType, viewId);
		verify(viewScopeDao).setViewScopeAndType(555L, Sets.newHashSet(123L, 456L), viewType);
		verify(columnModelManager).bindColumnToObject(userInfo, null, viewId);
		verify(tableManagerSupport).setTableToProcessingAndTriggerUpdate(viewId);
	}
	
	@Test
	public void testSetViewSchemaAndScopeWithNullScope(){
		scope = null;
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, scope, viewType, viewId);
		verify(viewScopeDao).setViewScopeAndType(555L, null, viewType);
		verify(columnModelManager).bindColumnToObject(userInfo, schema, viewId);
		verify(tableManagerSupport).setTableToProcessingAndTriggerUpdate(viewId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetViewSchemaAndScopeWithNullType(){
		viewType = null;
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, scope, viewType, viewId);
	}
	
	@Test
	public void testFindViewsContainingEntity(){
		Set<Long> path = Sets.newHashSet(123L,456L);
		when(tableManagerSupport.getEntityPath(viewId)).thenReturn(path);
		Set<Long> expected = Sets.newHashSet(789L);
		when(viewScopeDao.findViewScopeIntersectionWithPath(path)).thenReturn(expected);
		// call under test
		Set<Long> results = manager.findViewsContainingEntity(viewId);
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetViewSchemaWithRequiredColumnsNoAdditions(){
		String tableId = "syn123";
		List<ColumnModel> rawSchema = Lists.newArrayList(
				EntityField.benefactorId.getColumnModel(),
				EntityField.createdBy.getColumnModel(),
				EntityField.etag.getColumnModel()
				);
		when(tableManagerSupport.getColumnModelsForTable(tableId)).thenReturn(rawSchema);
		// call under test
		List<ColumnModel> result = manager.getViewSchemaWithRequiredColumns(tableId);
		assertEquals(rawSchema, result);
	}
	
	@Test
	public void testGetViewSchemaWithRequiredColumnsAllMissing(){
		String tableId = "syn123";
		List<ColumnModel> rawSchema = Lists.newArrayList(
				EntityField.createdBy.getColumnModel(),
				EntityField.createdOn.getColumnModel()
				);
		when(tableManagerSupport.getColumnModelsForTable(tableId)).thenReturn(rawSchema);
		// call under test
		List<ColumnModel> result = manager.getViewSchemaWithRequiredColumns(tableId);
		
		List<ColumnModel> expected = Lists.newArrayList(
				EntityField.createdBy.getColumnModel(),
				EntityField.createdOn.getColumnModel(),
				EntityField.etag.getColumnModel(),
				EntityField.benefactorId.getColumnModel()
				);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetViewSchemaWithRequiredColumnsMissingOne(){
		String tableId = "syn123";
		List<ColumnModel> rawSchema = Lists.newArrayList(
				EntityField.createdBy.getColumnModel(),
				EntityField.createdOn.getColumnModel(),
				EntityField.benefactorId.getColumnModel()
				);
		when(tableManagerSupport.getColumnModelsForTable(tableId)).thenReturn(rawSchema);
		// call under test
		List<ColumnModel> result = manager.getViewSchemaWithRequiredColumns(tableId);
		
		List<ColumnModel> expected = Lists.newArrayList(
				EntityField.createdBy.getColumnModel(),
				EntityField.createdOn.getColumnModel(),
				EntityField.benefactorId.getColumnModel(),
				EntityField.etag.getColumnModel()
				);
		assertEquals(expected, result);
	}
	
	@Test
	public void testApplySchemaChange(){
		String viewId = "syn123";
		ColumnChange change = new ColumnChange();
		change.setOldColumnId(null);
		change.setNewColumnId("456");
		List<ColumnChange> changes = Lists.newArrayList(change);
		ColumnModel model = EntityField.benefactorId.getColumnModel();
		model.setId(change.getNewColumnId());
		List<ColumnModel> schema = Lists.newArrayList(model);
		List<String> newColumnIds = Lists.newArrayList(change.getNewColumnId());
		when(columnModelManager.calculateNewSchemaIdsAndValidate(viewId, changes)).thenReturn(newColumnIds);
		when(columnModelManager.getColumnModel(userInfo, newColumnIds, true)).thenReturn(schema);
		
		// call under test
		List<ColumnModel> newSchema = manager.applySchemaChange(userInfo, viewId, changes);
		assertEquals(schema, newSchema);
		verify(columnModelManager).calculateNewSchemaIdsAndValidate(viewId, changes);
		verify(tableManagerSupport).setTableToProcessingAndTriggerUpdate(viewId);
	}

}
