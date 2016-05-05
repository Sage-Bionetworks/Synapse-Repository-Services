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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.RowBatchHandler;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dbo.dao.table.FileEntityFields;
import org.sagebionetworks.repo.model.dbo.dao.table.FileViewDao;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class FileViewManagerImplTest {

	@Mock
	ViewScopeDao viewScopeDao;
	@Mock
	ColumnModelManager columnModelManager;
	@Mock
	TableManagerSupport tableManagerSupport;
	@Mock
	ColumnModelDAO columnModelDao;
	@Mock
	FileViewDao fileViewDao;
	
	FileViewManagerImpl manager;
	
	UserInfo userInfo;
	List<String> schema;
	List<String> scope;
	String viewId;

	
	Set<Long> scopeIds;
	long rowCount;
	List<Row> rows;

	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		manager = new FileViewManagerImpl();
		ReflectionTestUtils.setField(manager, "viewScopeDao", viewScopeDao);
		ReflectionTestUtils.setField(manager, "columModelManager", columnModelManager);
		ReflectionTestUtils.setField(manager, "tableManagerSupport", tableManagerSupport);
		ReflectionTestUtils.setField(manager, "columnModelDao", columnModelDao);
		ReflectionTestUtils.setField(manager, "fileViewDao", fileViewDao);
		
		userInfo = new UserInfo(false, 888L);
		schema = Lists.newArrayList("1","2","3");
		scope = Lists.newArrayList("syn123", "syn456");
		scopeIds = new HashSet<Long>(KeyFactory.stringToKey(scope));
		
		viewId = "syn555";
		
		doAnswer(new Answer<ColumnModel>(){
			@Override
			public ColumnModel answer(InvocationOnMock invocation) throws Throwable {
				return (ColumnModel) invocation.getArguments()[0];
			}}).when(columnModelDao).createColumnModel(any(ColumnModel.class));
		
		when(tableManagerSupport.getAllContainerIdsForViewScope(viewId)).thenReturn(scopeIds);
		
		rowCount = 100;
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
				RowHandler handler = (RowHandler) invocation.getArguments()[2];
				// send all rows
				for(Row row: rows){
					handler.nextRow(row);
				}
				return null;
			}}).when(fileViewDao).streamOverFileEntities(any(Set.class), any(List.class), any(RowHandler.class));
		
	}
	
	@Test
	public void testSetViewSchemaAndScope(){
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, scope, viewId);
		verify(viewScopeDao).setViewScope(555L, Sets.newHashSet(123L, 456L));
		verify(columnModelManager).bindColumnToObject(userInfo, schema, viewId);
		verify(tableManagerSupport).setTableToProcessingAndTriggerUpdate(viewId);
	}
	
	@Test
	public void testSetViewSchemaAndScopeWithNullSchema(){
		schema = null;
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, scope, viewId);
		verify(viewScopeDao).setViewScope(555L, Sets.newHashSet(123L, 456L));
		verify(columnModelManager).bindColumnToObject(userInfo, null, viewId);
		verify(tableManagerSupport).setTableToProcessingAndTriggerUpdate(viewId);
	}
	
	@Test
	public void testSetViewSchemaAndScopeWithNullScope(){
		scope = null;
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, scope, viewId);
		verify(viewScopeDao).setViewScope(555L, null);
		verify(columnModelManager).bindColumnToObject(userInfo, schema, viewId);
		verify(tableManagerSupport).setTableToProcessingAndTriggerUpdate(viewId);
	}
	
	@Test
	public void testGetColumModel(){
		ColumnModel cm = new ColumnModel();
		cm.setId("123");
		when(columnModelDao.createColumnModel(any(ColumnModel.class))).thenReturn(cm);
		ColumnModel result = manager.getColumModel(FileEntityFields.id);
		assertEquals(cm, result);
	}
	
	@Test
	public void testGetViewSchemaNoBenefactor(){
		List<ColumnModel> rawSchema = Lists.newArrayList(FileEntityFields.id.getColumnModel());
		when(tableManagerSupport.getColumnModelsForTable(viewId)).thenReturn(rawSchema);
		// the results should contain both ID and benefactor.
		List<ColumnModel> expected = Lists.newArrayList(FileEntityFields.id.getColumnModel(), FileEntityFields.benefactorId.getColumnModel());
		// call under test
		List<ColumnModel> results = manager.getViewSchema(viewId);
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetViewSchemaWithBenefactor(){
		List<ColumnModel> rawSchema = Lists.newArrayList(FileEntityFields.id.getColumnModel(), FileEntityFields.benefactorId.getColumnModel());
		when(tableManagerSupport.getColumnModelsForTable(viewId)).thenReturn(rawSchema);
		// the results should contain both ID and benefactor.
		List<ColumnModel> expected = Lists.newArrayList(FileEntityFields.id.getColumnModel(), FileEntityFields.benefactorId.getColumnModel());
		// call under test
		List<ColumnModel> results = manager.getViewSchema(viewId);
		assertEquals(expected, results);
	}
	
	@Test
	public void testStreamOverAllFilesInViewAsBatch(){
		List<ColumnModel> schema = FileEntityFields.getAllColumnModels();
		long viewCRC = 999L;
		when(fileViewDao.countAllFilesInView(any(Set.class))).thenReturn(rowCount);
		when(fileViewDao.calculateCRCForAllFilesWithinContainers(any(Set.class))).thenReturn(viewCRC);
		final int rowsPerBatch = 3;
		final List<Row> gatheredRows = new LinkedList<Row>();
		// call under test
		long crcResult = manager.streamOverAllFilesInViewAsBatch(viewId, schema, rowsPerBatch, new RowBatchHandler() {
			
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

}
