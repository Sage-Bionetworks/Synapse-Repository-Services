package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.MaterializedViewDao;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.semaphore.LockContext;
import org.sagebionetworks.repo.model.semaphore.LockContext.ContextType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.MaterializedViewIndexDescription;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.cluster.description.ViewIndexDescription;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;

import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
public class MaterializedViewManagerImplTest {

	@Mock
	private ColumnModelManager mockColumnModelManager;
	
	@Mock
	private TableManagerSupport mockTableManagerSupport;
	
	@Mock
	private ProgressCallback mockProgressCallback;
	
	@Mock
	private TableIndexConnectionFactory mockConnectionFactory;
	
	@Mock
	private TableIndexManager mockTableIndexManager;
	
	@Mock
	private MaterializedViewDao mockMaterializedViewDao;
	
	@InjectMocks
	private MaterializedViewManagerImpl manager;
	
	private MaterializedViewManagerImpl managerSpy;

	@Mock
	private MaterializedView mockView;
	
	@Captor
	private ArgumentCaptor<QueryTranslator> queryCaptor;

	private IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
	private IdAndVersion temporaryId = IdAndVersion.newBuilder().setId(-idAndVersion.getId()).build(); 

	private List<ColumnModel> syn123Schema;
	private LockContext expectedLockContext;

	@BeforeEach
	public void before() {
		syn123Schema = Arrays.asList(TableModelTestUtils.createColumn(111L, "foo", ColumnType.INTEGER),
				TableModelTestUtils.createColumn(222L, "bar", ColumnType.STRING));
		
		managerSpy = Mockito.spy(manager);
		expectedLockContext = new LockContext(ContextType.BuildMaterializedView, idAndVersion); 
	}

	@Test
	public void testValidate() {
		String sql = "SELECT * FROM syn123";

		when(mockView.getDefiningSQL()).thenReturn(sql);

		// Call under test
		manager.validate(mockView);

		verify(mockView, atLeastOnce()).getDefiningSQL();
	}

	@Test
	public void testValidateWithNullSQL() {
		String sql = null;

		when(mockView.getDefiningSQL()).thenReturn(sql);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockView);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be the empty string.", message);
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}

	@Test
	public void testValidateWithEmptySQL() {
		String sql = "";

		when(mockView.getDefiningSQL()).thenReturn(sql);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockView);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be the empty string.", message);
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}

	@Test
	public void testValidateWithBlankSQL() {
		String sql = "   ";

		when(mockView.getDefiningSQL()).thenReturn(sql);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockView);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be a blank string.", message);
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}

	@Test
	public void testValidateWithInvalidSQL() {
		String sql = "invalid SQL";

		when(mockView.getDefiningSQL()).thenReturn(sql);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockView);
		});

		assertTrue(ex.getCause() instanceof ParseException);

		assertTrue(ex.getMessage().startsWith("Encountered \" <regular_identifier> \"invalid"));
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}

	@Test
	public void testValidateWithWithNoTable() {
		String sql = "SELECT foo";

		when(mockView.getDefiningSQL()).thenReturn(sql);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockView);
		});

		assertTrue(ex.getCause() instanceof ParseException);

		assertTrue(ex.getMessage().startsWith("Encountered \"<EOF>\" at line 1, column 10."));
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}

	@Test
	public void testRegisterSourceTables() {

		Set<IdAndVersion> currentSourceTables = Collections.emptySet();
		String sql = "SELECT * FROM syn123";

		Set<IdAndVersion> expectedDeletes = Collections.emptySet();
		Set<IdAndVersion> expectedSources = ImmutableSet.of(IdAndVersion.parse("syn123"));

		when(mockMaterializedViewDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		
		doNothing().when(managerSpy).bindSchemaToView(any(), any(QueryExpression.class));

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockMaterializedViewDao).getSourceTablesIds(idAndVersion);
		verify(mockMaterializedViewDao).deleteSourceTablesIds(idAndVersion, expectedDeletes);
		verify(mockMaterializedViewDao).addSourceTablesIds(idAndVersion, expectedSources);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any(QueryExpression.class));
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);

	}

	@Test
	public void testRegisterSourceTablesWithNonOverlappingAssociations() {

		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(IdAndVersion.parse("syn456"));

		String sql = "SELECT * FROM syn123";

		Set<IdAndVersion> expectedDeletes = currentSourceTables;
		Set<IdAndVersion> expectedSources = ImmutableSet.of(IdAndVersion.parse("syn123"));

		when(mockMaterializedViewDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		doNothing().when(managerSpy).bindSchemaToView(any(), any(QueryExpression.class));

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockMaterializedViewDao).getSourceTablesIds(idAndVersion);
		verify(mockMaterializedViewDao).deleteSourceTablesIds(idAndVersion, expectedDeletes);
		verify(mockMaterializedViewDao).addSourceTablesIds(idAndVersion, expectedSources);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any(QueryExpression.class));
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);
		
	}

	@Test
	public void testRegisterSourceTablesWithOverlappingAssociations() {

		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(IdAndVersion.parse("syn123"),
				IdAndVersion.parse("syn456"));

		String sql = "SELECT * FROM syn123";

		Set<IdAndVersion> expectedDeletes = ImmutableSet.of(IdAndVersion.parse("syn456"));
		Set<IdAndVersion> expectedSources = ImmutableSet.of(IdAndVersion.parse("syn123"));

		when(mockMaterializedViewDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		doNothing().when(managerSpy).bindSchemaToView(any(), any(QueryExpression.class));

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockMaterializedViewDao).getSourceTablesIds(idAndVersion);
		verify(mockMaterializedViewDao).deleteSourceTablesIds(idAndVersion, expectedDeletes);
		verify(mockMaterializedViewDao).addSourceTablesIds(idAndVersion, expectedSources);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any(QueryExpression.class));
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);

	}

	@Test
	public void testRegisterSourceTablesWithNoChanges() {

		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(IdAndVersion.parse("syn123"));

		String sql = "SELECT * FROM syn123";

		when(mockMaterializedViewDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		doNothing().when(managerSpy).bindSchemaToView(any(), any(QueryExpression.class));

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockMaterializedViewDao).getSourceTablesIds(idAndVersion);
		verifyNoMoreInteractions(mockMaterializedViewDao);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any(QueryExpression.class));
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);

	}

	@Test
	public void testRegisterSourceTablesWithMultipleTables() {

		Set<IdAndVersion> currentSourceTables = Collections.emptySet();
		String sql = "SELECT * FROM syn123 JOIN syn456";

		Set<IdAndVersion> expectedDeletes = Collections.emptySet();
		Set<IdAndVersion> expectedSources = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456"));

		when(mockMaterializedViewDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		doNothing().when(managerSpy).bindSchemaToView(any(), any(QueryExpression.class));

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockMaterializedViewDao).getSourceTablesIds(idAndVersion);
		verify(mockMaterializedViewDao).deleteSourceTablesIds(idAndVersion, expectedDeletes);
		verify(mockMaterializedViewDao).addSourceTablesIds(idAndVersion, expectedSources);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any(QueryExpression.class));
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);

	}

	@Test
	public void testRegisterSourceTablesWithMultipleTablesWithNonOverlappingAssociations() {

		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(IdAndVersion.parse("syn789"),
				IdAndVersion.parse("syn101112"));
		String sql = "SELECT * FROM syn123 JOIN syn456";

		Set<IdAndVersion> expectedDeletes = currentSourceTables;
		Set<IdAndVersion> expectedSources = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456"));

		when(mockMaterializedViewDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		doNothing().when(managerSpy).bindSchemaToView(any(), any(QueryExpression.class));

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockMaterializedViewDao).getSourceTablesIds(idAndVersion);
		verify(mockMaterializedViewDao).deleteSourceTablesIds(idAndVersion, expectedDeletes);
		verify(mockMaterializedViewDao).addSourceTablesIds(idAndVersion, expectedSources);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any(QueryExpression.class));
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);
		
	}

	@Test
	public void testRegisterSourceTablesWithMultipleTablesWithOverlappingAssociations() {

		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(IdAndVersion.parse("syn456"),
				IdAndVersion.parse("syn789"), IdAndVersion.parse("syn101112"));
		String sql = "SELECT * FROM syn123 JOIN syn456";

		Set<IdAndVersion> expectedDeletes = ImmutableSet.of(IdAndVersion.parse("syn789"),
				IdAndVersion.parse("syn101112"));
		Set<IdAndVersion> expectedSources = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456"));

		when(mockMaterializedViewDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		doNothing().when(managerSpy).bindSchemaToView(any(), any(QueryExpression.class));

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockMaterializedViewDao).getSourceTablesIds(idAndVersion);
		verify(mockMaterializedViewDao).deleteSourceTablesIds(idAndVersion, expectedDeletes);
		verify(mockMaterializedViewDao).addSourceTablesIds(idAndVersion, expectedSources);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any(QueryExpression.class));
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);

	}

	@Test
	public void testRegisterSourceTablesWithMultipleTablesAndNoChanges() {

		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(IdAndVersion.parse("syn456"),
				IdAndVersion.parse("syn123"));

		String sql = "SELECT * FROM syn123 JOIN syn456";

		when(mockMaterializedViewDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		doNothing().when(managerSpy).bindSchemaToView(any(), any(QueryExpression.class));

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockMaterializedViewDao).getSourceTablesIds(idAndVersion);
		verifyNoMoreInteractions(mockMaterializedViewDao);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any(QueryExpression.class));
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);

	}

	@Test
	public void testRegisterSourceTablesWithVersions() {

		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(IdAndVersion.parse("syn456"),
				IdAndVersion.parse("syn123.2"));

		String sql = "SELECT * FROM syn123.3 JOIN syn456";

		Set<IdAndVersion> expectedDeletes = ImmutableSet.of(IdAndVersion.parse("syn123.2"));

		Set<IdAndVersion> expectedSources = ImmutableSet.of(IdAndVersion.parse("syn123.3"),
				IdAndVersion.parse("syn456"));

		when(mockMaterializedViewDao.getSourceTablesIds(any())).thenReturn(currentSourceTables);
		doNothing().when(managerSpy).bindSchemaToView(any(), any(QueryExpression.class));

		// Call under test
		managerSpy.registerSourceTables(idAndVersion, sql);

		verify(mockMaterializedViewDao).getSourceTablesIds(idAndVersion);
		verify(mockMaterializedViewDao).deleteSourceTablesIds(idAndVersion, expectedDeletes);
		verify(mockMaterializedViewDao).addSourceTablesIds(idAndVersion, expectedSources);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any(QueryExpression.class));
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);

	}

	@Test
	public void testRegisterSourceTablesWithNoIdAndVersion() {

		idAndVersion = null;
		String sql = "SELECT * FROM syn123";

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.registerSourceTables(idAndVersion, sql);
		}).getMessage();

		assertEquals("The id of the materialized view is required.", message);

		verifyZeroInteractions(mockMaterializedViewDao);

	}

	@Test
	public void testRegisterSourceTablesWithNoSql() {

		String sql = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.registerSourceTables(idAndVersion, sql);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be the empty string.", message);

		verifyZeroInteractions(mockMaterializedViewDao);

	}

	@Test
	public void testRegisterSourceTablesWithEmptySql() {

		String sql = "";

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.registerSourceTables(idAndVersion, sql);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be the empty string.", message);

		verifyZeroInteractions(mockMaterializedViewDao);

	}

	@Test
	public void testRegisterSourceTablesWithBlankSql() {

		String sql = "   ";

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.registerSourceTables(idAndVersion, sql);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be a blank string.", message);

		verifyZeroInteractions(mockMaterializedViewDao);

	}

	@Test
	public void testGetQuerySpecification() {
		String sql = "SELECT * FROM syn123";

		QueryExpression result = MaterializedViewManagerImpl.getQuerySpecification(sql);

		assertNotNull(result);

	}

	@Test
	public void testGetQuerySpecificationWithParingException() {
		String sql = "invalid query";

		String message = assertThrows(IllegalArgumentException.class, () -> {
			MaterializedViewManagerImpl.getQuerySpecification(sql);
		}).getMessage();

		assertTrue(message.startsWith("Encountered \" <regular_identifier>"));
	}

	@Test
	public void testGetQuerySpecificationWithNullQuery() {
		String sql = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			MaterializedViewManagerImpl.getQuerySpecification(sql);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be the empty string.", message);
	}

	@Test
	public void testGetQuerySpecificationWithEmptyQuery() {
		String sql = "";

		String message = assertThrows(IllegalArgumentException.class, () -> {
			MaterializedViewManagerImpl.getQuerySpecification(sql);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be the empty string.", message);
	}

	@Test
	public void testGetQuerySpecificationWithBlankQuery() {
		String sql = "   ";

		String message = assertThrows(IllegalArgumentException.class, () -> {
			MaterializedViewManagerImpl.getQuerySpecification(sql);
		}).getMessage();

		assertEquals("The definingSQL of the materialized view is required and must not be a blank string.", message);
	}

	@Test
	public void testGetSourceTableIds() {

		QueryExpression query = MaterializedViewManagerImpl.getQuerySpecification("SELECT * FROM syn123");

		Set<IdAndVersion> expected = ImmutableSet.of(IdAndVersion.parse("syn123"));
		Set<IdAndVersion> result = MaterializedViewManagerImpl.getSourceTableIds(query);

		assertEquals(expected, result);
	}

	@Test
	public void testGetSourceTableIdsWithVersion() {

		QueryExpression query = MaterializedViewManagerImpl.getQuerySpecification("SELECT * FROM syn123.1");

		Set<IdAndVersion> expected = ImmutableSet.of(IdAndVersion.parse("syn123.1"));
		Set<IdAndVersion> result = MaterializedViewManagerImpl.getSourceTableIds(query);

		assertEquals(expected, result);
	}

	@Test
	public void testGetSourceTableIdsWithMultiple() {

		QueryExpression query = MaterializedViewManagerImpl
				.getQuerySpecification("SELECT * FROM syn123.1 JOIN syn456 JOIN syn123");

		Set<IdAndVersion> expected = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("syn123.1"),
				IdAndVersion.parse("456"));
		Set<IdAndVersion> result = MaterializedViewManagerImpl.getSourceTableIds(query);

		assertEquals(expected, result);
	}

	@Test
	public void testBindSchemaToView() {
		when(mockColumnModelManager.getTableSchema(any())).thenReturn(syn123Schema);
		when(mockColumnModelManager.createColumnModel(any())).thenReturn(
				TableModelTestUtils.createColumn(333L, "foo", ColumnType.INTEGER),
				TableModelTestUtils.createColumn(444L, "bar", ColumnType.STRING));
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
		QueryExpression query = MaterializedViewManagerImpl.getQuerySpecification("SELECT * FROM syn123");
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(new MaterializedViewIndexDescription(
				idAndVersion, Arrays.asList(new TableIndexDescription(IdAndVersion.parse("syn1")))));
		// call under test
		manager.bindSchemaToView(idAndVersion, query);
		verify(mockColumnModelManager).getTableSchema(idAndVersion);
		verify(mockColumnModelManager)
				.createColumnModel(new ColumnModel().setName("foo").setColumnType(ColumnType.INTEGER).setId(null));
		verify(mockColumnModelManager).createColumnModel(
				new ColumnModel().setName("bar").setColumnType(ColumnType.STRING).setMaximumSize(50L).setId(null));
		verify(mockColumnModelManager).bindColumnsToVersionOfObject(Arrays.asList("333", "444"), idAndVersion);
		verify(mockTableManagerSupport).getIndexDescription(idAndVersion);
	}
	
	@Test
	public void testRefreshDependentMaterializedViews() {
		
		List<IdAndVersion> dependencies = Arrays.asList(
			IdAndVersion.parse("syn123"),
			IdAndVersion.parse("234"),
			IdAndVersion.parse("syn456.2")
		);	
		
		// The second return must be an empty list because of the PaginationIterator that performs an additional call to check if there are more results
		when(mockMaterializedViewDao.getMaterializedViewIdsPage(any(), anyLong(), anyLong())).thenReturn(dependencies, Collections.emptyList());
				
		// Call under test
		manager.refreshDependentMaterializedViews(idAndVersion);
		
		for (IdAndVersion dependentView : dependencies) {
			verify(mockTableManagerSupport).triggerIndexUpdate(dependentView);
		}
		
		verifyNoMoreInteractions(mockTableIndexManager);
	}
	
	@Test
	public void testRefreshDependentMaterializedViewsWithNoIdAndVersion() {
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.refreshDependentMaterializedViews(null);
		}).getMessage();
		
		assertEquals("The tableId is required.", message);

		verifyZeroInteractions(mockMaterializedViewDao);
		verifyZeroInteractions(mockTableIndexManager);
	}
	
	@Test
	public void testDeleteViewIndex() {
		when(mockConnectionFactory.connectToTableIndex(any())).thenReturn(mockTableIndexManager);
		
		// call under test
		manager.deleteViewIndex(idAndVersion);
		
		verify(mockConnectionFactory).connectToTableIndex(idAndVersion);
		verify(mockTableIndexManager).deleteTableIndex(idAndVersion);
	}
	
	@Test
	public void testCreateOrUpdateViewIndexWithSnapshot() throws Exception {
		idAndVersion = IdAndVersion.parse("123.1");
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			manager.createOrUpdateViewIndex(mockProgressCallback, idAndVersion);
		}).getMessage();
		
		assertEquals("MaterializedView snapshots not currently supported", message);
		
		verifyZeroInteractions(mockTableManagerSupport);
		verifyZeroInteractions(mockTableIndexManager);
	}
	
	@Test
	public void testCreateOrUpdateViewIndexWithNonExistingTable() throws Exception {
		when(mockTableManagerSupport.isTableAvailable(any())).thenReturn(false);
		
		// call under test
		manager.createOrUpdateViewIndex(mockProgressCallback, idAndVersion);
		
		verify(mockTableManagerSupport).isTableAvailable(idAndVersion);
		verifyNoMoreInteractions(mockTableManagerSupport);
		verifyNoMoreInteractions(mockTableIndexManager);
	}
	
	@Test
	public void testCreateOrUpdateViewIndexWithNoTableState() throws Exception {
		when(mockTableManagerSupport.isTableAvailable(any())).thenReturn(true);
		when(mockTableManagerSupport.getTableStatusState(any())).thenReturn(Optional.empty());
		
		doAnswer(invocation -> {
			ProgressCallback callback = (ProgressCallback) invocation.getArguments()[0];
			ProgressingCallable runner = (ProgressingCallable) invocation.getArguments()[3];
			runner.call(callback);
			return null;
		}).when(mockTableManagerSupport).tryRunWithTableExclusiveLock(any(), any(), any(IdAndVersion.class), any());
		
		doNothing().when(managerSpy).createOrRebuildViewHoldingExclusiveLock(any(), any(), any());
		
		// call under test
		managerSpy.createOrUpdateViewIndex(mockProgressCallback, idAndVersion);
		
		verify(mockTableManagerSupport).isTableAvailable(idAndVersion);
		verify(mockTableManagerSupport).getTableStatusState(idAndVersion);
		verify(mockTableManagerSupport).tryRunWithTableExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), eq(temporaryId), any());
		verify(mockTableManagerSupport).tryRunWithTableExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), eq(idAndVersion), any());
		verify(managerSpy).createOrRebuildViewHoldingExclusiveLock(mockProgressCallback, expectedLockContext, idAndVersion);
	}

	@ParameterizedTest
	@EnumSource(value = TableState.class, names = {"AVAILABLE"})
	public void testCreateOrUpdateViewIndexWithAvailableState(TableState state) throws Exception {
		when(mockTableManagerSupport.isTableAvailable(any())).thenReturn(true);
		when(mockTableManagerSupport.getTableStatusState(any())).thenReturn(Optional.of(state));
		
		doAnswer(invocation -> {
			ProgressCallback callback = (ProgressCallback) invocation.getArguments()[0];
			ProgressingCallable runner = (ProgressingCallable) invocation.getArguments()[3];
			runner.call(callback);
			return null;
		}).when(mockTableManagerSupport).tryRunWithTableExclusiveLock(any(), any(), any(IdAndVersion.class), any());
		
		doNothing().when(managerSpy).rebuildAvailableViewHoldingTemporaryExclusiveLock(any(), any(), any(), any());
		
		IdAndVersion temporaryId = IdAndVersion.newBuilder().setId(-idAndVersion.getId()).setVersion(idAndVersion.getVersion().orElse(null)).build();
		expectedLockContext = new LockContext(ContextType.UpdatingMaterializedView, idAndVersion);
		
		// call under test
		managerSpy.createOrUpdateViewIndex(mockProgressCallback, idAndVersion);
		
		verify(mockTableManagerSupport).isTableAvailable(idAndVersion);
		verify(mockTableManagerSupport).getTableStatusState(idAndVersion);
		verify(mockTableManagerSupport).tryRunWithTableExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), eq(temporaryId), any());
		verify(managerSpy).rebuildAvailableViewHoldingTemporaryExclusiveLock(mockProgressCallback, expectedLockContext, idAndVersion, temporaryId);
	}
	
	@ParameterizedTest
	@EnumSource(value = TableState.class, mode = Mode.EXCLUDE, names = {"AVAILABLE"})
	public void testCreateOrUpdateViewIndexWithNonAvailableState(TableState state) throws Exception {
		when(mockTableManagerSupport.isTableAvailable(any())).thenReturn(true);
		when(mockTableManagerSupport.getTableStatusState(any())).thenReturn(Optional.of(state));
		
		doAnswer(invocation -> {
			ProgressCallback callback = (ProgressCallback) invocation.getArguments()[0];
			ProgressingCallable runner = (ProgressingCallable) invocation.getArguments()[3];
			runner.call(callback);
			return null;
		}).when(mockTableManagerSupport).tryRunWithTableExclusiveLock(any(), any(), any(IdAndVersion.class), any());
		
		doNothing().when(managerSpy).createOrRebuildViewHoldingExclusiveLock(any(), any(), any());
		
		// call under test
		managerSpy.createOrUpdateViewIndex(mockProgressCallback, idAndVersion);
		
		verify(mockTableManagerSupport).isTableAvailable(idAndVersion);
		verify(mockTableManagerSupport).getTableStatusState(idAndVersion);
		verify(mockTableManagerSupport).tryRunWithTableExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), eq(temporaryId), any());
		verify(mockTableManagerSupport).tryRunWithTableExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), eq(idAndVersion), any());
		verify(managerSpy).createOrRebuildViewHoldingExclusiveLock(mockProgressCallback, expectedLockContext, idAndVersion);
	}
	
	@Test
	public void testCreateOrRebuildViewHoldingExclusiveLock() throws Exception {
		idAndVersion = IdAndVersion.parse("syn123");
		
		doAnswer(invocation -> {
			ProgressCallback callback = (ProgressCallback) invocation.getArguments()[0];
			ProgressingCallable runner = (ProgressingCallable) invocation.getArguments()[2];
			runner.call(callback);
			return null;
		}).when(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(any(), any(), any(), any(IdAndVersion.class));
		
		when(mockMaterializedViewDao.getMaterializedViewDefiningSql(any())).thenReturn(Optional.of("select * from syn456"));
		when(mockColumnModelManager.getTableSchema(any())).thenReturn(syn123Schema);
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, Collections.emptyList());
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(any())).thenReturn(new TableStatus().setState(TableState.AVAILABLE));
		when(mockTableManagerSupport.isTableSearchEnabled(any())).thenReturn(false);
		doNothing().when(managerSpy).bindSchemaToView(any(), any(QueryTranslator.class));
		doNothing().when(managerSpy).createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(any(), any(), anyBoolean());
		
		IdAndVersion dependentIdAndVersion = IdAndVersion.parse("syn456");
				
		// call under test
		managerSpy.createOrRebuildViewHoldingExclusiveLock(mockProgressCallback, expectedLockContext, idAndVersion);
		
		verify(mockMaterializedViewDao).getMaterializedViewDefiningSql(idAndVersion);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any(QueryTranslator.class));
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(dependentIdAndVersion);
		verify(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), any(), eq( dependentIdAndVersion));
		verify(mockColumnModelManager).getTableSchema(idAndVersion);
		verify(mockTableManagerSupport).isTableSearchEnabled(idAndVersion);
		verify(managerSpy).createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(queryCaptor.capture(), eq(syn123Schema), eq(false));
		
		QueryTranslator query = queryCaptor.getValue();
		
		assertEquals("select * from syn456", query.getInputSql());
		assertEquals(indexDescription, query.getIndexDescription());
	}
	
	@Test
	public void testCreateOrRebuildViewHoldingExclusiveLockWithMultipleDependencies() throws Exception {
		idAndVersion = IdAndVersion.parse("syn123");
		doAnswer(invocation -> {
			ProgressCallback callback = (ProgressCallback) invocation.getArguments()[0];
			ProgressingCallable runner = (ProgressingCallable) invocation.getArguments()[2];
			runner.call(callback);
			return null;
		}).when(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(any(), any(), any(), any(IdAndVersion.class));

		when(mockMaterializedViewDao.getMaterializedViewDefiningSql(any()))
				.thenReturn(Optional.of("select * from syn456 join syn789"));
		when(mockColumnModelManager.getTableSchema(any())).thenReturn(syn123Schema);
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, Collections.emptyList());
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(any())).thenReturn(new TableStatus().setState(TableState.AVAILABLE));
		when(mockTableManagerSupport.isTableSearchEnabled(any())).thenReturn(false);
		doNothing().when(managerSpy).bindSchemaToView(any(), any(QueryTranslator.class));
		doNothing().when(managerSpy).createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(any(), any(), anyBoolean());
		
		IdAndVersion[] dependentIdAndVersions = new IdAndVersion[] { IdAndVersion.parse("syn456"),
				IdAndVersion.parse("syn789") };

		// call under test
		managerSpy.createOrRebuildViewHoldingExclusiveLock(mockProgressCallback, expectedLockContext, idAndVersion);

		verify(mockMaterializedViewDao).getMaterializedViewDefiningSql(idAndVersion);
		verify(mockTableManagerSupport).getIndexDescription(idAndVersion);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any(QueryTranslator.class));
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(dependentIdAndVersions[0]);
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(dependentIdAndVersions[1]);
		verify(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), any(),
				eq(dependentIdAndVersions[0]), eq(dependentIdAndVersions[1]));
		verify(mockColumnModelManager).getTableSchema(idAndVersion);
		verify(mockTableManagerSupport).isTableSearchEnabled(idAndVersion);
		verify(managerSpy).createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(queryCaptor.capture(), eq(syn123Schema), eq(false));
		
		QueryTranslator query = queryCaptor.getValue();
		
		assertEquals("select * from syn456 join syn789", query.getInputSql());
		assertEquals(indexDescription, query.getIndexDescription());
	}
	
	@Test
	public void testCreateOrRebuildViewHoldingExclusiveLockWithMultipleDependenciesWithUnavailable() throws Exception {
		idAndVersion = IdAndVersion.parse("syn123");

		when(mockMaterializedViewDao.getMaterializedViewDefiningSql(any()))
				.thenReturn(Optional.of("select * from syn456 join syn789"));
		when(mockColumnModelManager.getTableSchema(any())).thenReturn(syn123Schema);
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, Collections.emptyList());
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(any())).thenReturn(new TableStatus().setState(TableState.AVAILABLE),
				new TableStatus().setState(TableState.PROCESSING));
		doNothing().when(managerSpy).bindSchemaToView(any(), any(QueryTranslator.class));

		IdAndVersion[] dependentIdAndVersions = new IdAndVersion[] { IdAndVersion.parse("syn456"),
				IdAndVersion.parse("syn789") };

		assertThrows(RecoverableMessageException.class, ()->{
			// call under test
			managerSpy.createOrRebuildViewHoldingExclusiveLock(mockProgressCallback, expectedLockContext, idAndVersion);
		});
		verify(mockMaterializedViewDao).getMaterializedViewDefiningSql(idAndVersion);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any(QueryTranslator.class));
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(dependentIdAndVersions[0]);
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(dependentIdAndVersions[1]);
		verify(mockTableManagerSupport, never()).tryRunWithTableNonExclusiveLock(any(), any(), any(), any(IdAndVersion.class));
		verify(managerSpy, never()).createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(any(), any(), anyBoolean());
	}
	
	@Test
	public void testCreateOrRebuildViewHoldingExclusiveLockWithMultipleDependenciesWithProcessingFailed() throws Exception {
		idAndVersion = IdAndVersion.parse("syn123");

		when(mockMaterializedViewDao.getMaterializedViewDefiningSql(any()))
				.thenReturn(Optional.of("select * from syn456 join syn789"));
		when(mockColumnModelManager.getTableSchema(any())).thenReturn(syn123Schema);
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, Collections.emptyList());
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(any())).thenReturn(new TableStatus().setState(TableState.AVAILABLE),
				new TableStatus().setState(TableState.PROCESSING_FAILED));
		
		doNothing().when(managerSpy).bindSchemaToView(any(), any(QueryTranslator.class));
		IdAndVersion[] dependentIdAndVersions = new IdAndVersion[] { IdAndVersion.parse("syn456"),
				IdAndVersion.parse("syn789") };

		String result = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			managerSpy.createOrRebuildViewHoldingExclusiveLock(mockProgressCallback, expectedLockContext, idAndVersion);
		}).getMessage();
		
		assertEquals("Cannot build materialized view syn123, the dependent table syn789 failed to build", result);
		
		verify(mockMaterializedViewDao).getMaterializedViewDefiningSql(idAndVersion);
		verify(managerSpy).bindSchemaToView(eq(idAndVersion), any(QueryTranslator.class));
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(dependentIdAndVersions[0]);
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(dependentIdAndVersions[1]);
		verify(mockTableManagerSupport, never()).tryRunWithTableNonExclusiveLock(any(), any(), any(), any(IdAndVersion.class));
		verify(managerSpy, never()).createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(any(), any(), anyBoolean());
	}
	
	@Test
	public void testCreateOrRebuildViewHoldingExclusiveLockWithNoDefiningSql() throws Exception {
		idAndVersion = IdAndVersion.parse("syn123");
		
		when(mockMaterializedViewDao.getMaterializedViewDefiningSql(any())).thenReturn(Optional.empty());
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			managerSpy.createOrRebuildViewHoldingExclusiveLock(mockProgressCallback, expectedLockContext, idAndVersion);
		}).getMessage();
		assertEquals("No defining SQL for: syn123", message);
		
		verify(mockMaterializedViewDao).getMaterializedViewDefiningSql(any());
		verify(mockColumnModelManager, never()).bindColumnsToVersionOfObject(any(), any());
		verify(mockTableManagerSupport, never()).tryRunWithTableNonExclusiveLock(any(), any(), any(), any(IdAndVersion.class));
		verify(managerSpy, never()).createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(any(), any(), anyBoolean());
	}
	
	@Test
	public void testRebuildAvailableViewHoldingTemporaryExclusiveLock() throws Exception {		
		doAnswer(invocation -> {
			ProgressCallback callback = (ProgressCallback) invocation.getArguments()[0];
			ProgressingCallable runner = (ProgressingCallable) invocation.getArguments()[2];
			return runner.call(callback);
		}).when(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(any(), any(), any(), any(IdAndVersion.class));
		
		doAnswer(invocation -> {
			ProgressCallback callback = (ProgressCallback) invocation.getArguments()[0];
			ProgressingCallable runner = (ProgressingCallable) invocation.getArguments()[3];
			return runner.call(callback);
		}).when(mockTableManagerSupport).tryRunWithTableExclusiveLock(any(), any(), any(IdAndVersion.class), any());

		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, Collections.emptyList());
		IndexDescription temporaryIndex = new MaterializedViewIndexDescription(temporaryId, Collections.emptyList());
		
		List<ColumnModel> syn456Schema = List.of(
			TableModelTestUtils.createColumn(333L, "c", ColumnType.INTEGER),
			TableModelTestUtils.createColumn(444L, "d", ColumnType.STRING_LIST)
		);
		
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockMaterializedViewDao.getMaterializedViewDefiningSql(any())).thenReturn(Optional.of("select * from syn123 join syn456"));
		when(mockColumnModelManager.getTableSchema(any())).thenReturn(syn456Schema);		
		when(mockColumnModelManager.createColumnModel(any())).thenReturn(syn123Schema.get(0), syn123Schema.get(1), syn456Schema.get(0), syn456Schema.get(1));
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(any())).thenReturn(new TableStatus().setState(TableState.AVAILABLE));
		when(mockConnectionFactory.connectToTableIndex(any())).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.getVersionFromIndexDependencies(any())).thenReturn(10L);
		when(mockTableManagerSupport.isTableSearchEnabled(any())).thenReturn(false);
		when(mockTableManagerSupport.isIndexSynchronized(any(), any(), anyLong(), anyBoolean())).thenReturn(false);
		doNothing().when(managerSpy).createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(any(), any(), anyBoolean());
		when(mockTableManagerSupport.getTableStatusState(any())).thenReturn(Optional.of(TableState.AVAILABLE));
		doNothing().when(mockTableIndexManager).swapTableIndex(any(), any());
		when(mockColumnModelManager.bindColumnsToVersionOfObject(any(), any())).thenReturn(syn123Schema);
		
		IdAndVersion[] dependentIdAndVersions = new IdAndVersion[] { IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456") };

		List<ColumnModel> viewSchema = List.of(syn123Schema.get(0), syn123Schema.get(1), syn456Schema.get(0), syn456Schema.get(1));

		// call under test
		managerSpy.rebuildAvailableViewHoldingTemporaryExclusiveLock(mockProgressCallback, expectedLockContext, idAndVersion, temporaryId);

		verify(mockTableManagerSupport).getIndexDescription(idAndVersion);
		verify(mockMaterializedViewDao).getMaterializedViewDefiningSql(idAndVersion);
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(dependentIdAndVersions[0]);
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(dependentIdAndVersions[1]);
		verify(mockTableManagerSupport).getTableStatusState(idAndVersion);
		verify(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), any(), eq(dependentIdAndVersions[0]), eq(dependentIdAndVersions[1]));
		verify(mockTableIndexManager).getVersionFromIndexDependencies(indexDescription);
		verify(mockTableManagerSupport).isTableSearchEnabled(idAndVersion);
		verify(mockTableManagerSupport).isIndexSynchronized(idAndVersion, List.of("111", "222", "333", "444"), 10, false);
		verify(managerSpy).createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(queryCaptor.capture(), eq(viewSchema), eq(false));
		
		QueryTranslator query = queryCaptor.getValue();
		
		assertEquals("select * from syn123 join syn456", query.getInputSql());
		assertEquals(temporaryIndex, query.getIndexDescription());
		
		verify(mockTableManagerSupport).tryRunWithTableExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), eq(idAndVersion), any());
		verify(mockTableIndexManager).swapTableIndex(temporaryIndex, indexDescription);
		verify(mockColumnModelManager).bindColumnsToVersionOfObject(viewSchema.stream().map(ColumnModel::getId).collect(Collectors.toList()), idAndVersion);
		verify(mockTableManagerSupport).updateChangedOnIfAvailable(idAndVersion);
		
	}
	
	@Test
	public void testRebuildAvailableViewHoldingTemporaryExclusiveLockWithIndexSynchronized() throws Exception {		
		doAnswer(invocation -> {
			ProgressCallback callback = (ProgressCallback) invocation.getArguments()[0];
			ProgressingCallable runner = (ProgressingCallable) invocation.getArguments()[2];
			return runner.call(callback);
		}).when(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(any(), any(), any(), any(IdAndVersion.class));
		
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, Collections.emptyList());
		
		List<ColumnModel> syn456Schema = List.of(
			TableModelTestUtils.createColumn(333L, "c", ColumnType.INTEGER),
			TableModelTestUtils.createColumn(444L, "d", ColumnType.STRING_LIST)
		);
		
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockMaterializedViewDao.getMaterializedViewDefiningSql(any())).thenReturn(Optional.of("select * from syn123 join syn456"));
		when(mockColumnModelManager.getTableSchema(any())).thenReturn(syn456Schema);		
		when(mockColumnModelManager.createColumnModel(any())).thenReturn(syn123Schema.get(0), syn123Schema.get(1), syn456Schema.get(0), syn456Schema.get(1));
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(any())).thenReturn(new TableStatus().setState(TableState.AVAILABLE));
		when(mockConnectionFactory.connectToTableIndex(any())).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.getVersionFromIndexDependencies(any())).thenReturn(10L);
		when(mockTableManagerSupport.isTableSearchEnabled(any())).thenReturn(false);
		when(mockTableManagerSupport.isIndexSynchronized(any(), any(), anyLong(), anyBoolean())).thenReturn(true);
		
		IdAndVersion[] dependentIdAndVersions = new IdAndVersion[] { IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456") };

		// call under test
		managerSpy.rebuildAvailableViewHoldingTemporaryExclusiveLock(mockProgressCallback, expectedLockContext, idAndVersion, temporaryId);

		verify(mockTableManagerSupport).getIndexDescription(idAndVersion);
		verify(mockMaterializedViewDao).getMaterializedViewDefiningSql(idAndVersion);
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(dependentIdAndVersions[0]);
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(dependentIdAndVersions[1]);
		verify(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), any(), eq(dependentIdAndVersions[0]), eq(dependentIdAndVersions[1]));
		verify(mockTableIndexManager).getVersionFromIndexDependencies(indexDescription);
		verify(mockTableManagerSupport).isTableSearchEnabled(idAndVersion);
		verify(mockTableManagerSupport).isIndexSynchronized(idAndVersion, List.of("111", "222", "333", "444"), 10, false);
		verifyNoMoreInteractions(mockTableManagerSupport);
		verifyNoMoreInteractions(mockTableIndexManager);
		
	}
	
	@Test
	public void testRebuildAvailableViewHoldingTemporaryExclusiveLockWithNonAvailableState() throws Exception {
		doAnswer(invocation -> {
			ProgressCallback callback = (ProgressCallback) invocation.getArguments()[0];
			ProgressingCallable runner = (ProgressingCallable) invocation.getArguments()[2];
			return runner.call(callback);
		}).when(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(any(), any(), any(), any(IdAndVersion.class));
		
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, Collections.emptyList());
		IndexDescription temporaryIndex = new MaterializedViewIndexDescription(temporaryId, Collections.emptyList());
		
		List<ColumnModel> syn456Schema = List.of(
			TableModelTestUtils.createColumn(333L, "c", ColumnType.INTEGER),
			TableModelTestUtils.createColumn(444L, "d", ColumnType.STRING_LIST)
		);
		
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockMaterializedViewDao.getMaterializedViewDefiningSql(any())).thenReturn(Optional.of("select * from syn123 join syn456"));
		when(mockColumnModelManager.getTableSchema(any())).thenReturn(syn456Schema);		
		when(mockColumnModelManager.createColumnModel(any())).thenReturn(syn123Schema.get(0), syn123Schema.get(1), syn456Schema.get(0), syn456Schema.get(1));
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(any())).thenReturn(new TableStatus().setState(TableState.AVAILABLE));
		when(mockConnectionFactory.connectToTableIndex(any())).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.getVersionFromIndexDependencies(any())).thenReturn(10L);
		when(mockTableManagerSupport.isTableSearchEnabled(any())).thenReturn(false);
		when(mockTableManagerSupport.isIndexSynchronized(any(), any(), anyLong(), anyBoolean())).thenReturn(false);
		doNothing().when(managerSpy).createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(any(), any(), anyBoolean());
		when(mockTableManagerSupport.getTableStatusState(any())).thenReturn(Optional.of(TableState.PROCESSING));
		
		IdAndVersion[] dependentIdAndVersions = new IdAndVersion[] { IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456") };

		List<ColumnModel> viewSchema = List.of(syn123Schema.get(0), syn123Schema.get(1), syn456Schema.get(0), syn456Schema.get(1));

		// call under test
		managerSpy.rebuildAvailableViewHoldingTemporaryExclusiveLock(mockProgressCallback, expectedLockContext, idAndVersion, temporaryId);

		verify(mockTableManagerSupport).getIndexDescription(idAndVersion);
		verify(mockMaterializedViewDao).getMaterializedViewDefiningSql(idAndVersion);
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(dependentIdAndVersions[0]);
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(dependentIdAndVersions[1]);
		verify(mockTableManagerSupport).getTableStatusState(idAndVersion);
		verify(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), any(), eq(dependentIdAndVersions[0]), eq(dependentIdAndVersions[1]));
		verify(mockTableIndexManager).getVersionFromIndexDependencies(indexDescription);
		verify(mockTableManagerSupport).isTableSearchEnabled(idAndVersion);
		verify(mockTableManagerSupport).isIndexSynchronized(idAndVersion, List.of("111", "222", "333", "444"), 10, false);
		verify(managerSpy).createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(queryCaptor.capture(), eq(viewSchema), eq(false));
		
		QueryTranslator query = queryCaptor.getValue();
		
		assertEquals("select * from syn123 join syn456", query.getInputSql());
		assertEquals(temporaryIndex, query.getIndexDescription());
		
		verifyNoMoreInteractions(mockTableManagerSupport);
		verifyNoMoreInteractions(mockTableIndexManager);
		
	}
	
	@Test
	public void testRebuildAvailableViewHoldingTemporaryExclusiveLockWithNonAvailableDependent() throws Exception {
		idAndVersion = IdAndVersion.parse("syn123");
		IdAndVersion temporaryId = IdAndVersion.parse("syn-123");
		
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, Collections.emptyList());
				
		List<ColumnModel> syn456Schema = List.of(
			TableModelTestUtils.createColumn(333L, "c", ColumnType.INTEGER),
			TableModelTestUtils.createColumn(444L, "d", ColumnType.STRING_LIST)
		);
		
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockMaterializedViewDao.getMaterializedViewDefiningSql(any())).thenReturn(Optional.of("select * from syn123 join syn456"));
		when(mockColumnModelManager.getTableSchema(any())).thenReturn(syn456Schema);		
		when(mockColumnModelManager.createColumnModel(any())).thenReturn(syn123Schema.get(0), syn123Schema.get(1), syn456Schema.get(0), syn456Schema.get(1));
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(any())).thenReturn(new TableStatus().setState(TableState.PROCESSING));
		
		assertThrows(RecoverableMessageException.class, ()->{
			// call under test
			managerSpy.rebuildAvailableViewHoldingTemporaryExclusiveLock(mockProgressCallback, expectedLockContext, idAndVersion, temporaryId);
		});

		verify(mockTableManagerSupport).getIndexDescription(idAndVersion);
		verify(mockMaterializedViewDao).getMaterializedViewDefiningSql(idAndVersion);
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(IdAndVersion.parse("syn123"));
		verifyNoMoreInteractions(mockTableManagerSupport);
		verifyNoMoreInteractions(mockTableIndexManager);
		
	}
		
	@Test
	public void testRebuildAvailableViewHoldingTemporaryExclusiveLockWithNonExclusiveLockUnavailable() throws Exception {
		idAndVersion = IdAndVersion.parse("syn123");
		IdAndVersion temporaryId = IdAndVersion.parse("syn-123");

		doThrow(LockUnavilableException.class).when(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(any(), any(), any(), any(IdAndVersion.class));
		
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, Collections.emptyList());
		
		List<ColumnModel> syn456Schema = List.of(
			TableModelTestUtils.createColumn(333L, "c", ColumnType.INTEGER),
			TableModelTestUtils.createColumn(444L, "d", ColumnType.STRING_LIST)
		);
		
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockMaterializedViewDao.getMaterializedViewDefiningSql(any())).thenReturn(Optional.of("select * from syn123 join syn456"));
		when(mockColumnModelManager.getTableSchema(any())).thenReturn(syn456Schema);		
		when(mockColumnModelManager.createColumnModel(any())).thenReturn(syn123Schema.get(0), syn123Schema.get(1), syn456Schema.get(0), syn456Schema.get(1));
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(any())).thenReturn(new TableStatus().setState(TableState.AVAILABLE));
				
		IdAndVersion[] dependentIdAndVersions = new IdAndVersion[] { IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456") };

		assertThrows(RecoverableMessageException.class, () -> {			
			// call under test
			managerSpy.rebuildAvailableViewHoldingTemporaryExclusiveLock(mockProgressCallback, expectedLockContext, idAndVersion, temporaryId);
		});

		verify(mockTableManagerSupport).getIndexDescription(idAndVersion);
		verify(mockMaterializedViewDao).getMaterializedViewDefiningSql(idAndVersion);
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(dependentIdAndVersions[0]);
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(dependentIdAndVersions[1]);
		verify(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), any(), eq(dependentIdAndVersions[0]), eq(dependentIdAndVersions[1]));
		verifyNoMoreInteractions(mockTableManagerSupport);
		verifyNoMoreInteractions(mockTableIndexManager);
	}
		
	@Test
	public void testCreateOrRebuildViewHoldingWriteLockAndAllDependentReadLocks() {
		idAndVersion = IdAndVersion.parse("syn123");
		
		QueryTranslator mockQuery = Mockito.mock(QueryTranslator.class);
		IndexDescription index = new ViewIndexDescription(idAndVersion, TableType.entityview);
		when(mockQuery.getIndexDescription()).thenReturn(index);
		
		when(mockTableManagerSupport.startTableProcessing(any())).thenReturn("token");
		when(mockConnectionFactory.connectToTableIndex(any())).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.resetTableIndex(any(), any(), anyBoolean())).thenReturn(syn123Schema);
		doNothing().when(mockTableManagerSupport).attemptToUpdateTableProgress(any(), any(), any(), any(), any());
		when(mockTableIndexManager.populateMaterializedViewFromDefiningSql(any(), any())).thenReturn(123L);
		doNothing().when(mockTableIndexManager).buildTableIndexIndices(any(), any());
		doNothing().when(mockTableIndexManager).setIndexVersion(any(), any());
		doNothing().when(mockTableManagerSupport).attemptToSetTableStatusToAvailable(any(), any(), any());
		
		// Call under test
		manager.createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(mockQuery, syn123Schema, false);
		
		verify(mockTableManagerSupport).startTableProcessing(idAndVersion);
		verify(mockTableIndexManager).resetTableIndex(index, syn123Schema, false);
		verify(mockTableManagerSupport).attemptToUpdateTableProgress(idAndVersion, "token", "Building MaterializedView...", 0L, 1L);
		verify(mockTableIndexManager).populateMaterializedViewFromDefiningSql(syn123Schema, mockQuery);
		verify(mockTableIndexManager).buildTableIndexIndices(index, syn123Schema);
		verify(mockTableIndexManager).setIndexVersion(idAndVersion, 123L);
		verify(mockTableManagerSupport).attemptToSetTableStatusToAvailable(idAndVersion, "token", "DEFAULT");
	}
	
	@Test
	public void testCreateOrRebuildViewHoldingWriteLockAndAllDependentReadLocksWithSearchEnabled() {
		idAndVersion = IdAndVersion.parse("syn123");
		
		QueryTranslator mockQuery = Mockito.mock(QueryTranslator.class);
		IndexDescription index = new ViewIndexDescription(idAndVersion, TableType.entityview);
		when(mockQuery.getIndexDescription()).thenReturn(index);
		
		when(mockTableManagerSupport.startTableProcessing(any())).thenReturn("token");
		when(mockConnectionFactory.connectToTableIndex(any())).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.resetTableIndex(any(), any(), anyBoolean())).thenReturn(syn123Schema);
		doNothing().when(mockTableManagerSupport).attemptToUpdateTableProgress(any(), any(), any(), any(), any());
		when(mockTableIndexManager.populateMaterializedViewFromDefiningSql(any(), any())).thenReturn(123L);
		doNothing().when(mockTableIndexManager).buildTableIndexIndices(any(), any());
		doNothing().when(mockTableIndexManager).setIndexVersion(any(), any());
		doNothing().when(mockTableManagerSupport).attemptToSetTableStatusToAvailable(any(), any(), any());
		
		// Call under test
		manager.createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(mockQuery, syn123Schema, true);
		
		verify(mockTableManagerSupport).startTableProcessing(idAndVersion);
		verify(mockTableIndexManager).resetTableIndex(index, syn123Schema, true);
		verify(mockTableManagerSupport).attemptToUpdateTableProgress(idAndVersion, "token", "Building MaterializedView...", 0L, 1L);
		verify(mockTableIndexManager).populateMaterializedViewFromDefiningSql(syn123Schema, mockQuery);
		verify(mockTableIndexManager).buildTableIndexIndices(index, syn123Schema);
		verify(mockTableIndexManager).setIndexVersion(idAndVersion, 123L);
		verify(mockTableManagerSupport).attemptToSetTableStatusToAvailable(idAndVersion, "token", "DEFAULT");
	}
}
