package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.RowBatchHandler;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dbo.dao.table.FileEntityFields;
import org.sagebionetworks.repo.model.dbo.dao.table.TableViewDao;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
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
	@Mock
	TableViewDao mockTableViewDao;
	
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
		ReflectionTestUtils.setField(manager, "tableViewDao", mockTableViewDao);
		
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
		// simulate a row handler.
		doAnswer(new Answer<Void>(){
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				RowHandler handler = (RowHandler) invocation.getArguments()[3];
				// send all rows
				for(Row row: rows){
					handler.nextRow(row);
				}
				return null;
			}}).when(mockTableViewDao).streamOverEntities(anySetOf(Long.class), any(ViewType.class), anyListOf(ColumnModel.class), any(RowHandler.class));
		
		viewCRC = 999L;
		when(mockTableViewDao.countAllEntitiesInView(anySetOf(Long.class), any(ViewType.class))).thenReturn(rowCount);
		when(mockTableViewDao.calculateCRCForAllEntitiesWithinContainers(anySetOf(Long.class), any(ViewType.class))).thenReturn(viewCRC);
		
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
	public void testGetViewSchemaNoBenefactor(){
		List<ColumnModel> rawSchema = Lists.newArrayList(FileEntityFields.id.getColumnModel());
		when(tableManagerSupport.getColumnModelsForTable(viewId)).thenReturn(rawSchema);
		when(tableManagerSupport.getColumModel(FileEntityFields.benefactorId)).thenReturn(FileEntityFields.benefactorId.getColumnModel());
		// the results should contain both ID and benefactor.
		List<ColumnModel> expected = Lists.newArrayList(FileEntityFields.id.getColumnModel(), FileEntityFields.benefactorId.getColumnModel());
		// call under test
		List<ColumnModel> results = manager.getViewSchemaWithBenefactor(viewId);
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetViewSchemaWithBenefactor(){
		List<ColumnModel> rawSchema = Lists.newArrayList(FileEntityFields.id.getColumnModel(), FileEntityFields.benefactorId.getColumnModel());
		when(tableManagerSupport.getColumnModelsForTable(viewId)).thenReturn(rawSchema);
		// the results should contain both ID and benefactor.
		List<ColumnModel> expected = Lists.newArrayList(FileEntityFields.id.getColumnModel(), FileEntityFields.benefactorId.getColumnModel());
		// call under test
		List<ColumnModel> results = manager.getViewSchemaWithBenefactor(viewId);
		assertEquals(expected, results);
	}
	
	@Test
	public void testStreamOverAllFilesInViewAsBatchRowsLessThanBatch(){
		List<ColumnModel> schema = FileEntityFields.getAllColumnModels();

		final int rowsPerBatch = (int)(rowCount-1);
		final List<Row> gatheredRows = new LinkedList<Row>();
		// call under test
		long crcResult = manager.streamOverAllEntitiesInViewAsBatch(viewId, ViewType.file, schema, rowsPerBatch, new RowBatchHandler() {
			
			long count = 0;
			@Override
			public void nextBatch(List<Row> batch, long currentProgress,
					long totalProgress) {
				gatheredRows.addAll(batch);
				count += batch.size();
				assertEquals(count, currentProgress);
				assertEquals(rowCount, totalProgress);
			}
		});
		assertEquals(viewCRC, crcResult);
		//  all row should be gathered
		assertEquals(rows, gatheredRows);
	}
	
	@Test
	public void testStreamOverAllFilesInViewAsBatchRowsGreaterThanBatch(){
		List<ColumnModel> schema = FileEntityFields.getAllColumnModels();
		final int rowsPerBatch = (int) (rowCount+1);
		final List<Row> gatheredRows = new LinkedList<Row>();
		// call under test
		long crcResult = manager.streamOverAllEntitiesInViewAsBatch(viewId, ViewType.file, schema, rowsPerBatch, new RowBatchHandler() {
			
			long count = 0;
			@Override
			public void nextBatch(List<Row> batch, long currentProgress,
					long totalProgress) {
				gatheredRows.addAll(batch);
				count += batch.size();
				assertEquals(count, currentProgress);
				assertEquals(rowCount, totalProgress);
			}
		});
		assertEquals(viewCRC, crcResult);
		//  all row should be gathered
		assertEquals(rows, gatheredRows);
	}
	
	@Test
	public void testStreamOverAllFilesInViewAsBatchRowsEqualsToBatch(){
		List<ColumnModel> schema = FileEntityFields.getAllColumnModels();
		final int rowsPerBatch = (int) (rowCount);
		final List<Row> gatheredRows = new LinkedList<Row>();
		// call under test
		long crcResult = manager.streamOverAllEntitiesInViewAsBatch(viewId, ViewType.file, schema, rowsPerBatch, new RowBatchHandler() {
			
			long count = 0;
			@Override
			public void nextBatch(List<Row> batch, long currentProgress,
					long totalProgress) {
				gatheredRows.addAll(batch);
				count += batch.size();
				assertEquals(count, currentProgress);
				assertEquals(rowCount, totalProgress);
			}
		});
		assertEquals(viewCRC, crcResult);
		//  all row should be gathered
		assertEquals(rows, gatheredRows);
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

}
