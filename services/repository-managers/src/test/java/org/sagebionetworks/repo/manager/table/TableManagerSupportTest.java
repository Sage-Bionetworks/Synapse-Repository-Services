package org.sagebionetworks.repo.manager.table;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModelMapper;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProviderFactory;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.MaterializedViewDao;
import org.sagebionetworks.repo.model.dbo.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewSnapshotDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.MaterializedViewIndexDescription;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.cluster.description.ViewIndexDescription;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.TimeoutUtils;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphoreRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class TableManagerSupportTest {

	@Mock
	private TableStatusDAO mockTableStatusDAO;
	@Mock
	private ConnectionFactory mockTableConnectionFactory;
	@Mock
	private TableIndexDAO mockTableIndexDAO;
	@Mock
	private TimeoutUtils mockTimeoutUtils;
	@Mock
	private TransactionalMessenger mockTransactionalMessenger;
	@Mock
	private ColumnModelManager mockColumnModelManager;
	@Mock
	private NodeDAO mockNodeDao;
	@Mock
	private TableRowTruthDAO mockTableTruthDao;
	@Mock
	private ViewScopeDao mockViewScopeDao;
	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private ViewSnapshotDao mockViewSnapshotDao;
	@Mock
	private MetadataIndexProviderFactory mockMetadataIndexProviderFactory;
	@Mock
	private MetadataIndexProvider mockMetadataIndexProvider;
	@Mock
	private DefaultColumnModelMapper mockDefaultColumnModelMapper;
	@Mock
	private MaterializedViewDao mockMaterializedViewDao;
	@Mock
	private WriteReadSemaphoreRunner mockWriteReadSemaphoreRunner;
	
	@InjectMocks
	TableManagerSupportImpl manager;
	
	@Spy
	@InjectMocks
	TableManagerSupportImpl managerSpy;
	
	@Mock
	DefaultColumnModel mockDefaultModel;
	
	@Mock
	ProgressingCallable<String> mockCallable;
	
	String schemaMD5Hex;
	String tableId;
	IdAndVersion idAndVersion;
	Long tableIdLong;
	TableStatus status;
	String etag;
	Set<Long> scope;
	Set<Long> containersInScope;
	UserInfo userInfo;
	List<ColumnModel> columns;
	List<String> columnIds;
	
	Integer callableReturn;
	ViewScopeType scopeType;
	
	@BeforeEach
	public void before() throws Exception {
		callableReturn = 123;
		
		userInfo = new UserInfo(false, 8L);
		
		idAndVersion = IdAndVersion.parse("syn123");
		tableId = idAndVersion.getId().toString();
		tableIdLong = KeyFactory.stringToKey(tableId);
		
		etag = "";
		
		status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.PROCESSING);
		status.setChangedOn(new Date(123));
		status.setResetToken(etag);
		
		ColumnModel cm = new ColumnModel();
		cm.setId("444");
		columns = Lists.newArrayList(cm);
		columnIds = TableModelUtils.getIds(columns);
		
		schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(Lists.newArrayList(cm.getId()));
		
		// setup the view scope
		scope = Sets.newHashSet(222L,333L);
		
		containersInScope = new LinkedHashSet<Long>(Arrays.asList(222L,333L,20L,21L,30L,31L));
		scopeType = new ViewScopeType(ViewObjectType.ENTITY, ViewTypeMask.File.getMask());
		
	}
	
	
	/**
	 * For this case the table status is available and the index is synchronized with the truth.
	 * The available status should be returned for this case.
	 * @throws Exception
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsAvailableSynchronized() throws Exception {
		when(mockTableConnectionFactory.getConnection(idAndVersion)).thenReturn(mockTableIndexDAO);
		when(mockTableStatusDAO.getTableStatus(idAndVersion)).thenReturn(status);		
		when(mockColumnModelManager.getColumnIdsForTable(idAndVersion)).thenReturn(columnIds);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		// Available
		status.setState(TableState.AVAILABLE);
		when(mockTableStatusDAO.getTableStatus(idAndVersion)).thenReturn(status);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(any(IdAndVersion.class), anyLong(), anyString())).thenReturn(true);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(idAndVersion);
		assertNotNull(result);
		verify(mockTableStatusDAO, never()).resetTableStatusToProcessing(idAndVersion);
		verify(mockTransactionalMessenger, never()).sendMessageAfterCommit(tableId, ObjectType.TABLE, ChangeType.UPDATE);
	}
	
	/**
	 * This is a test case for PLFM-3383, PLFM-3379, and PLFM-3762.  In all cases the table 
	 * status was available but the index was not synchronized with the truth.
	 * @throws Exception
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsAvailableNotSynchronized() throws Exception {
		when(mockTableConnectionFactory.getConnection(idAndVersion)).thenReturn(mockTableIndexDAO);
		when(mockTableStatusDAO.getTableStatus(idAndVersion)).thenReturn(status);		
		when(mockTableStatusDAO.resetTableStatusToProcessing(idAndVersion)).thenReturn(etag);
		when(mockColumnModelManager.getColumnIdsForTable(idAndVersion)).thenReturn(columnIds);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);

		// Available
		status.setState(TableState.AVAILABLE);
		when(mockTableStatusDAO.getTableStatus(idAndVersion)).thenReturn(status);
		// Not synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(any(IdAndVersion.class), anyLong(), anyString())).thenReturn(false);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(idAndVersion);
		assertNotNull(result);
		// must trigger processing
		verify(mockTableStatusDAO).resetTableStatusToProcessing(idAndVersion);
		verify(mockTransactionalMessenger)
		.sendMessageAfterCommit(new MessageToSend().withObjectId(tableId).withObjectType(ObjectType.TABLE)
				.withChangeType(ChangeType.UPDATE).withObjectVersion(null));
	}
	
	/**
	 * This is a case where the table status does not exist but the table exits.
	 * The table must be be set to processing for this case.
	 * @throws Exception
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsStatusNotFoundTableExits() throws Exception {
		when(mockTableStatusDAO.getTableStatus(idAndVersion)).thenReturn(status);		
		when(mockTableStatusDAO.resetTableStatusToProcessing(idAndVersion)).thenReturn(etag);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		// Available
		status.setState(TableState.PROCESSING);
		// Setup a case where the first time the status does not exists, but does exist the second call.
		when(mockTableStatusDAO.getTableStatus(idAndVersion)).thenThrow(new NotFoundException("No status for this table.")).thenReturn(status);
		// table exists
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(true);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(idAndVersion);
		assertNotNull(result);
		verify(mockTableStatusDAO).resetTableStatusToProcessing(idAndVersion);
		verify(mockNodeDao).isNodeAvailable(tableIdLong);
		verify(mockTransactionalMessenger)
				.sendMessageAfterCommit(new MessageToSend().withObjectId(tableId).withObjectType(ObjectType.TABLE)
						.withChangeType(ChangeType.UPDATE).withObjectVersion(null));
	}
	
	/**
	 * This is a case where the table status does not exist and the table does not exist.
	 * Should result in a NotFoundException
	 * @throws Exception
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsStatusNotFoundTableDoesNotExist() throws Exception {
		when(mockTableStatusDAO.getTableStatus(idAndVersion)).thenReturn(status);		
		// Available
		status.setState(TableState.PROCESSING);
		// Setup a case where the first time the status does not exists, but does exist the second call.
		when(mockTableStatusDAO.getTableStatus(idAndVersion)).thenThrow(new NotFoundException("No status for this table.")).thenReturn(status);
		// table does not exists
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(false);
		assertThrows(NotFoundException.class, ()->{
			// call under test
			manager.getTableStatusOrCreateIfNotExists(idAndVersion);
		});

	}
	
	/**
	 * For this case the table is processing and making progress (not expired).
	 * So the processing status should be returned without resetting.
	 * @throws Exception
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsProcessingNotExpired() throws Exception {
		// Available
		status.setState(TableState.PROCESSING);
		when(mockTableStatusDAO.getTableStatus(idAndVersion)).thenReturn(status);
		// not expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(idAndVersion);
		assertNotNull(result);
		verify(mockTableStatusDAO, never()).resetTableStatusToProcessing(idAndVersion);
		verify(mockTransactionalMessenger, never()).sendMessageAfterCommit(any(MessageToSend.class));
	}
	
	/**
	 * For this case the table is processing and not making progress .
	 * The status must be rest to processing to trigger another try.
	 * @throws Exception
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsProcessingExpired() throws Exception {
		when(mockTableStatusDAO.getTableStatus(idAndVersion)).thenReturn(status);		
		when(mockTableStatusDAO.resetTableStatusToProcessing(idAndVersion)).thenReturn(etag);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		// Available
		status.setState(TableState.PROCESSING);
		when(mockTableStatusDAO.getTableStatus(idAndVersion)).thenReturn(status);
		//expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(true);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(idAndVersion);
		assertNotNull(result);
		verify(mockTableStatusDAO).resetTableStatusToProcessing(idAndVersion);
		verify(mockTransactionalMessenger)
		.sendMessageAfterCommit(new MessageToSend().withObjectId(tableId).withObjectType(ObjectType.TABLE)
				.withChangeType(ChangeType.UPDATE).withObjectVersion(null));
	}


	
	@Test
	public void testStartTableProcessing(){
		String token = "a unique token";
		when(mockTableStatusDAO.resetTableStatusToProcessing(idAndVersion)).thenReturn(token);
		// call under test
		String resultToken = manager.startTableProcessing(idAndVersion);
		assertEquals(token, resultToken);
	}
	
	@Test
	public void testIsIndexSynchronizedWithTruth(){
		when(mockTableConnectionFactory.getConnection(idAndVersion)).thenReturn(mockTableIndexDAO);
		when(mockColumnModelManager.getColumnIdsForTable(idAndVersion)).thenReturn(columnIds);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);

		long currentVersion = 3L;

		TableRowChange lastChange = new TableRowChange();
		lastChange.setRowVersion(currentVersion);
		when(mockTableTruthDao.getLastTableChangeNumber(tableIdLong)).thenReturn(Optional.of(currentVersion));
		// setup a match
		when(mockTableIndexDAO.doesIndexStateMatch(idAndVersion, currentVersion, schemaMD5Hex)).thenReturn(true);
		
		assertTrue(manager.isIndexSynchronizedWithTruth(idAndVersion));
	}
	
	
	/**
	 * The node is available, the table status is available and the index is synchronized.
	 */
	@Test
	public void testIsIndexWorkRequiredFalse(){
		when(mockTableConnectionFactory.getConnection(idAndVersion)).thenReturn(mockTableIndexDAO);
		when(mockColumnModelManager.getColumnIdsForTable(idAndVersion)).thenReturn(columnIds);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		// node exists
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(true);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(any(IdAndVersion.class), anyLong(), anyString())).thenReturn(true);
		// available
		when(mockTableStatusDAO.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		// call under test
		boolean workRequired = manager.isIndexWorkRequired(idAndVersion);
		assertFalse(workRequired);
	}
	
	@Test
	public void testIsIndexWorkRequired_NoState(){
		when(mockTableConnectionFactory.getConnection(idAndVersion)).thenReturn(mockTableIndexDAO);
		when(mockColumnModelManager.getColumnIdsForTable(idAndVersion)).thenReturn(columnIds);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		// node exists
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(true);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(any(IdAndVersion.class), anyLong(), anyString())).thenReturn(true);
		// empty
		when(mockTableStatusDAO.getTableStatusState(idAndVersion)).thenReturn(Optional.empty());
		// call under test
		boolean workRequired = manager.isIndexWorkRequired(idAndVersion);
		assertTrue(workRequired);
	}
	
	@Test
	public void testDoesTableExistTrue() {
		Long id = 123L;
		when(mockNodeDao.doesNodeExist(id)).thenReturn(true);
		// call under test
		assertTrue(manager.doesTableExist(idAndVersion));
	}
	
	@Test
	public void testDoesTableExistFalse() {
		Long id = 123L;
		when(mockNodeDao.doesNodeExist(id)).thenReturn(false);
		// call under test
		assertFalse(manager.doesTableExist(idAndVersion));
	}
	
	/**
	 * The node is missing, the table status is available and the index is synchronized.
	 */
	@Test
	public void testIsIndexWorkRequiredNodeMissing(){
		// node is missing
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(false);
		// call under test
		boolean workRequired = manager.isIndexWorkRequired(idAndVersion);
		assertFalse(workRequired);
	}
	
	/**
	 * The node is available, the table status is available and the index is not synchronized.
	 * Processing is needed to bring the index up-to-date.
	 */
	@Test
	public void testIsIndexWorkRequiredNotSynched(){
		when(mockTableConnectionFactory.getConnection(idAndVersion)).thenReturn(mockTableIndexDAO);
		when(mockColumnModelManager.getColumnIdsForTable(idAndVersion)).thenReturn(columnIds);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);

		// node exists
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(true);
		// not synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(any(IdAndVersion.class), anyLong(), anyString())).thenReturn(false);
		// call under test
		boolean workRequired = manager.isIndexWorkRequired(idAndVersion);
		assertTrue(workRequired);
	}
	
	/**
	 * The node is available, the table status is processing and the index is synchronized.
	 * Processing is needed to make the table available.
	 */
	@Test
	public void testIsIndexWorkRequiredStatusProcessing(){
		when(mockTableConnectionFactory.getConnection(idAndVersion)).thenReturn(mockTableIndexDAO);	
		when(mockColumnModelManager.getColumnIdsForTable(idAndVersion)).thenReturn(columnIds);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);

		// node exists
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(true);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(any(IdAndVersion.class), anyLong(), anyString())).thenReturn(true);
		when(mockTableStatusDAO.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.PROCESSING));
		// call under test
		boolean workRequired = manager.isIndexWorkRequired(idAndVersion);
		assertTrue(workRequired);
	}
	
	@Test
	public void testGetSchemaMD5Hex() {
		when(mockColumnModelManager.getColumnIdsForTable(idAndVersion)).thenReturn(columnIds);

		String md5 = manager.getSchemaMD5Hex(idAndVersion);
		assertEquals(schemaMD5Hex, md5);
	}

	@Test
	public void testGetTableTypeTable() {
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		// call under test
		ObjectType type = manager.getTableType(idAndVersion);
		assertEquals(ObjectType.TABLE, type);
	}

	@Test
	public void testGetTableTypeFileView() {
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(
				EntityType.entityview);
		// call under test
		ObjectType type = manager.getTableType(idAndVersion);
		assertEquals(ObjectType.ENTITY_VIEW, type);
	}
	
	@Test
	public void testGetTableTypeMaterializedView() {
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(
				EntityType.materializedview);
		// call under test
		ObjectType type = manager.getTableType(idAndVersion);
		assertEquals(ObjectType.MATERIALIZED_VIEW, type);
	}

	@Test
	public void testGetTableTypeUnknown() {
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(
				EntityType.project);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.getTableType(idAndVersion);
		});
	}
	
	@Test
	public void testGetObjectTypeForEntityType(){
		assertEquals(ObjectType.TABLE, TableManagerSupportImpl.getObjectTypeForEntityType(EntityType.table));
		for (EntityType type : EntityType.values()) {
			if (EntityTypeUtils.isViewType(type)) {
				assertEquals(ObjectType.ENTITY_VIEW, TableManagerSupportImpl.getObjectTypeForEntityType(type));
			}
		}
	}
	
	@Test
	public void testGetObjectTypeForEntityTypeUnknownType(){
		for (EntityType type : EntityType.values()) {
			if (!EntityTypeUtils.isViewType(type) && !EntityType.table.equals(type)) {
				assertThrows(IllegalArgumentException.class, ()->{
					// call under test
					TableManagerSupportImpl.getObjectTypeForEntityType(EntityType.project);
				});
			}
		}
	}
	
	@Test
	public void testGetLastTableChangeNumberNoVersion() {
		idAndVersion = IdAndVersion.parse("syn123");
		when(mockTableTruthDao.getLastTableChangeNumber(123L)).thenReturn(Optional.of(12L));
		// call under test
		Optional<Long> result = manager.getLastTableChangeNumber(idAndVersion);
		assertNotNull(result);
		assertTrue(result.isPresent());
		assertEquals(new Long(12L), result.get());
		verify(mockTableTruthDao).getLastTableChangeNumber(123L);
		verify(mockTableTruthDao, never()).getLastTableChangeNumber(anyLong(), anyLong());
	}
	
	@Test
	public void testGetLastTableChangeNumberWithVersion() {
		idAndVersion = IdAndVersion.parse("syn123.456");
		when(mockTableTruthDao.getLastTableChangeNumber(123L, 456L)).thenReturn(Optional.of(18L));
		// call under test
		Optional<Long> result = manager.getLastTableChangeNumber(idAndVersion);
		assertNotNull(result);
		assertTrue(result.isPresent());
		assertEquals(new Long(18L), result.get());
		verify(mockTableTruthDao, never()).getLastTableChangeNumber(anyLong());
		verify(mockTableTruthDao).getLastTableChangeNumber(123L, 456L);
	}
	
	
	@Test
	public void testValidateScopeSize() throws LimitExceededException{
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		
		// call under test
		manager.validateScope(scopeType, scope);
		
		verify(mockMetadataIndexProvider).validateScopeAndType(scopeType.getTypeMask(), scope, TableConstants.MAX_CONTAINERS_PER_VIEW);
	}
	
	
	@Test
	public void testGetViewStateNumber() throws LimitExceededException{
		Long expectedNumber = 123L;
		when(mockTableConnectionFactory.getConnection(idAndVersion)).thenReturn(mockTableIndexDAO);
		when(mockTableIndexDAO.getMaxCurrentCompleteVersionForTable(idAndVersion)).thenReturn(expectedNumber);
		// call under test
		Long viewNumer = manager.getViewStateNumber(idAndVersion);
		assertEquals(expectedNumber, viewNumer);
		verify(mockTableIndexDAO).getMaxCurrentCompleteVersionForTable(idAndVersion);
		verify(mockViewSnapshotDao, never()).getSnapshot(any(IdAndVersion.class));
	}
	
	@Test
	public void testGetViewStateNumber_WithVersion(){
		idAndVersion = IdAndVersion.parse("syn123.45");
		Long snapshotId = 33L;
		when(mockViewSnapshotDao.getSnapshotId(idAndVersion)).thenReturn(snapshotId);
		// call under test
		Long result = manager.getViewStateNumber(idAndVersion);
		assertEquals(snapshotId, result);
		verify(mockTableIndexDAO, never()).getMaxCurrentCompleteVersionForTable(any(IdAndVersion.class));
		verify(mockViewSnapshotDao).getSnapshotId(idAndVersion);
	}

	@Test
	public void testGetTableVersionForTableEntityNoVersion() {
		idAndVersion = IdAndVersion.parse("syn123");
		Long changeNumber = 12L;
		when(mockTableTruthDao.getLastTableChangeNumber(123L)).thenReturn(Optional.of(changeNumber));
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		// call under test
		Long version = manager.getTableVersion(idAndVersion);
		assertEquals(changeNumber, version);
	}
	
	@Test
	public void testGetTableVersionForTableEntityNoChanges() {
		idAndVersion = IdAndVersion.parse("syn123");
		when(mockTableTruthDao.getLastTableChangeNumber(123L)).thenReturn(Optional.empty());
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		// call under test
		Long version = manager.getTableVersion(idAndVersion);
		assertEquals(new Long(-1), version);
	}
	
	@Test
	public void testGetTableVersionForTableEntityWithVersion() {
		idAndVersion = IdAndVersion.parse("syn123.456");
		Long changeNumber = 16L;
		when(mockTableTruthDao.getLastTableChangeNumber(123L, 456L)).thenReturn(Optional.of(changeNumber));
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		// call under test
		Long version = manager.getTableVersion(idAndVersion);
		assertEquals(changeNumber, version);
	}
	
	@Test
	public void testGetTableVersionForFileView() throws LimitExceededException {
		Long expectedNumber = 123L;
		when(mockTableConnectionFactory.getConnection(idAndVersion)).thenReturn(mockTableIndexDAO);
		when(mockTableIndexDAO.getMaxCurrentCompleteVersionForTable(idAndVersion)).thenReturn(expectedNumber);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.entityview);

		// call under test
		Long version = manager.getTableVersion(idAndVersion);
		assertEquals(expectedNumber, version);
	}
	
	@Test
	public void testGetTableVersionWithMaterializedView() throws LimitExceededException {
		Long expectedNumber = 123L;
		when(mockTableConnectionFactory.getConnection(idAndVersion)).thenReturn(mockTableIndexDAO);
		when(mockTableIndexDAO.getMaxCurrentCompleteVersionForTable(idAndVersion)).thenReturn(expectedNumber);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.materializedview);

		// call under test
		Long version = manager.getTableVersion(idAndVersion);
		assertEquals(expectedNumber, version);
	}
	
	@Test
	public void testGetTableVersionForUnknown() {
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.folder);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.getTableVersion(idAndVersion);
		});
	}
	
	@Test
	public void testValidateTableReadAccessTableEntityNoDownload(){
		IndexDescription indexDescription = new TableIndexDescription(idAndVersion);
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, ()->{
			//  call under test
			manager.validateTableReadAccess(userInfo, indexDescription);
		});
		verify(mockAuthorizationManager).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockAuthorizationManager).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD);
	}
	
	@Test
	public void testValidateTableReadAccessTableEntityNoRead(){
		IndexDescription indexDescription = new TableIndexDescription(idAndVersion);
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, ()->{
			//  call under test
			manager.validateTableReadAccess(userInfo, indexDescription);
		});
	}
	
	@Test
	public void testValidateTableReadAccessFileView(){
		IndexDescription indexDescription = new ViewIndexDescription(idAndVersion, EntityType.entityview);
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		//  call under test
		manager.validateTableReadAccess(userInfo, indexDescription);
		verify(mockAuthorizationManager).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		//  do not need download for FileView
		verify(mockAuthorizationManager, never()).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD);
	}
	
	@Test
	public void testValidateTableReadAccessFileViewNoRead(){
		IndexDescription indexDescription = new ViewIndexDescription(idAndVersion, EntityType.entityview);
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, ()->{
			//  call under test
			manager.validateTableReadAccess(userInfo, indexDescription);
		});
	}
	
	@Test
	public void testValidateTableReadAccessWithMaterializedView(){
		IdAndVersion tableId = IdAndVersion.parse("syn1");
		IdAndVersion viewId = IdAndVersion.parse("syn2");
		IdAndVersion materializedId = IdAndVersion.parse("syn3");
		IndexDescription tableDescription = new TableIndexDescription(tableId);
		IndexDescription viewDescription = new ViewIndexDescription(viewId, EntityType.entityview);
		IndexDescription materializedDescription = new MaterializedViewIndexDescription(materializedId,
				Arrays.asList(tableDescription, viewDescription));		
		
		when(mockAuthorizationManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());

		//  call under test
		manager.validateTableReadAccess(userInfo, materializedDescription);
		
		// check for the table
		verify(mockAuthorizationManager).canAccess(userInfo, tableId.getId().toString(), ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockAuthorizationManager).canAccess(userInfo, tableId.getId().toString(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD);
		verify(mockAuthorizationManager).canAccess(userInfo, viewId.getId().toString(), ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockAuthorizationManager).canAccess(userInfo, materializedId.getId().toString(), ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockAuthorizationManager, times(4)).canAccess(any(), any(), any(), any());
	}
	
	@Test
	public void testValidateTableWriteAccessTableEntity(){
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		//  call under test
		manager.validateTableWriteAccess(userInfo, idAndVersion);
		verify(mockAuthorizationManager).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
	}
	
	@Test
	public void testValidateTableWriteAccessTableEntityNoUpdate(){
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, ()->{
			//  call under test
			manager.validateTableWriteAccess(userInfo, idAndVersion);
		});
	}
	
	@Test
	public void testGetDefaultTableViewColumnsNullObjectType(){
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockMetadataIndexProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultModel);

		ViewEntityType viewEntityType = null;
		Long viewTypeMask = 1L;

		// call under test
		manager.getDefaultTableViewColumns(viewEntityType, viewTypeMask);

		verify(mockMetadataIndexProviderFactory).getMetadataIndexProvider(ViewObjectType.ENTITY);
		verify(mockMetadataIndexProvider).getDefaultColumnModel(scopeType.getTypeMask());
		verify(mockDefaultColumnModelMapper).map(mockDefaultModel);
	}
	
	@Test
	public void testGetDefaultTableViewColumns(){
		
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockMetadataIndexProvider.getDefaultColumnModel(any())).thenReturn(mockDefaultModel);

		ViewEntityType viewEntityType = ViewEntityType.entityview;
		Long viewTypeMask = 1L;
		
		// call under test
		manager.getDefaultTableViewColumns(viewEntityType, viewTypeMask);
		
		verify(mockMetadataIndexProviderFactory).getMetadataIndexProvider(scopeType.getObjectType());
		verify(mockMetadataIndexProvider).getDefaultColumnModel(scopeType.getTypeMask());
		verify(mockDefaultColumnModelMapper).map(mockDefaultModel);
	}


	@Test
	public void testRebuildTableUnauthorized() throws Exception {
		assertThrows(UnauthorizedException.class, ()->{
			// call under test
			manager.rebuildTable(userInfo, idAndVersion);
		});
	}

	@Test
	public void testRebuildTableAuthorizedForTableEntity() throws Exception {
		when(mockTableConnectionFactory.getConnection(idAndVersion)).thenReturn(mockTableIndexDAO);
		when(mockTableStatusDAO.resetTableStatusToProcessing(idAndVersion)).thenReturn(etag);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		UserInfo mockAdmin = Mockito.mock(UserInfo.class);
		when(mockAdmin.isAdmin()).thenReturn(true);
		// call under test
		manager.rebuildTable(mockAdmin, idAndVersion);
		verify(mockTableIndexDAO).deleteTable(idAndVersion);
		verify(mockTableStatusDAO).resetTableStatusToProcessing(idAndVersion);
		ArgumentCaptor<ChangeMessage> captor = ArgumentCaptor.forClass(ChangeMessage.class);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(captor.capture());
		ChangeMessage message = captor.getValue();
		assertEquals(message.getObjectId(), tableIdLong+"");
		assertEquals(ObjectType.TABLE, message.getObjectType());
		assertEquals(ChangeType.UPDATE, message.getChangeType());
	}

	@Test
	public void testRebuildTableAuthorizedForFileView() throws Exception {
		when(mockTableConnectionFactory.getConnection(idAndVersion)).thenReturn(mockTableIndexDAO);
		when(mockTableStatusDAO.resetTableStatusToProcessing(idAndVersion)).thenReturn(etag);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		UserInfo mockAdmin = Mockito.mock(UserInfo.class);
		when(mockAdmin.isAdmin()).thenReturn(true);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.entityview);
		// call under test
		manager.rebuildTable(mockAdmin, idAndVersion);
		verify(mockTableIndexDAO).deleteTable(idAndVersion);
		verify(mockTableStatusDAO).resetTableStatusToProcessing(idAndVersion);
		ArgumentCaptor<ChangeMessage> captor = ArgumentCaptor.forClass(ChangeMessage.class);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(captor.capture());
		ChangeMessage message = captor.getValue();
		assertEquals(message.getObjectId(), tableIdLong+"");
		assertEquals(ObjectType.ENTITY_VIEW, message.getObjectType());
		assertEquals(ChangeType.UPDATE, message.getChangeType());
	}
	
	@Test
	public void testTouch() {
		// call under test
		manager.touchTable(userInfo, tableId);
		verify(mockNodeDao).touch(userInfo.getId(), tableId);
	}
	
	@Test
	public void testTouchNullUser() {
		userInfo = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.touchTable(userInfo, tableId);
		});
	}
	
	@Test
	public void testTouchNullTable() {
		tableId = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.touchTable(userInfo, tableId);
		});
	}
	
	@Test
	public void testSetTableToProcessingAndTriggerUpdateWithVersion() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123.3");
		String resetToken = "a reset token";
		when(mockTableStatusDAO.resetTableStatusToProcessing(idAndVersion)).thenReturn(resetToken);
		EntityType type = EntityType.entityview;
		when(mockNodeDao.getNodeTypeById("123")).thenReturn(type);
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		when(mockTableStatusDAO.getTableStatus(idAndVersion)).thenReturn(status);
		// call under test
		TableStatus resultStatus = manager.setTableToProcessingAndTriggerUpdate(idAndVersion);
		assertEquals(status, resultStatus);
		verify(mockTransactionalMessenger)
				.sendMessageAfterCommit(new MessageToSend().withObjectId("123").withObjectVersion(3L)
						.withObjectType(ObjectType.ENTITY_VIEW).withChangeType(ChangeType.UPDATE));
		verify(mockTableStatusDAO).resetTableStatusToProcessing(idAndVersion);
	}
	
	@Test
	public void testSetTableToProcessingAndTriggerUpdateNoVersion() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
		String resetToken = "a reset token";
		when(mockTableStatusDAO.resetTableStatusToProcessing(idAndVersion)).thenReturn(resetToken);
		EntityType type = EntityType.entityview;
		when(mockNodeDao.getNodeTypeById("123")).thenReturn(type);
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		when(mockTableStatusDAO.getTableStatus(idAndVersion)).thenReturn(status);
		// call under test
		TableStatus resultStatus = manager.setTableToProcessingAndTriggerUpdate(idAndVersion);
		assertEquals(status, resultStatus);
		verify(mockTransactionalMessenger)
		.sendMessageAfterCommit(new MessageToSend().withObjectId("123").withObjectVersion(null)
				.withObjectType(ObjectType.ENTITY_VIEW).withChangeType(ChangeType.UPDATE));
		verify(mockTableStatusDAO).resetTableStatusToProcessing(idAndVersion);
	}
	
	@Test
	public void testIsTableIndexStateInvalidWithNoEtag() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
		when(mockTableStatusDAO.getLastChangeEtag(any())).thenReturn(Optional.empty());
		// call under test
		boolean result = manager.isTableIndexStateInvalid(idAndVersion);
		assertFalse(result);
		verify(mockTableStatusDAO).getLastChangeEtag(idAndVersion);
		verify(mockTableTruthDao, never()).isEtagInTablesChangeHistory(any(), any());
	}
	
	@Test
	public void testIsTableIndexStateInvalidWithWrongEtag() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
		String etag = "some-etag";
		when(mockTableStatusDAO.getLastChangeEtag(any())).thenReturn(Optional.of(etag));
		when(mockTableTruthDao.isEtagInTablesChangeHistory(any(), any())).thenReturn(false);
		// call under test
		boolean result = manager.isTableIndexStateInvalid(idAndVersion);
		assertTrue(result);
		verify(mockTableStatusDAO).getLastChangeEtag(idAndVersion);
		verify(mockTableTruthDao).isEtagInTablesChangeHistory(idAndVersion.getId().toString(), etag);
	}
	
	@Test
	public void testIsTableIndexStateInvalidWithMatchinggEtag() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
		String etag = "some-etag";
		when(mockTableStatusDAO.getLastChangeEtag(any())).thenReturn(Optional.of(etag));
		when(mockTableTruthDao.isEtagInTablesChangeHistory(any(), any())).thenReturn(true);
		// call under test
		boolean result = manager.isTableIndexStateInvalid(idAndVersion);
		assertFalse(result);
		verify(mockTableStatusDAO).getLastChangeEtag(idAndVersion);
		verify(mockTableTruthDao).isEtagInTablesChangeHistory(idAndVersion.getId().toString(), etag);
	}
	
	/**
	 * Setup to create a column by returning the passed column.
	 */
	void setupCreateColumn() {
		// mirror passed columns.
		doAnswer(new Answer<ColumnModel>() {
			@Override
			public ColumnModel answer(InvocationOnMock invocation)
					throws Throwable {
				return (ColumnModel) invocation.getArguments()[0];
			}
		}).when(mockColumnModelManager).createColumnModel(any(ColumnModel.class));
	}
	
	@Test
	public void testSendAsynchronousActivitySignal() {
		doReturn(ObjectType.ENTITY_VIEW).when(managerSpy).getTableType(idAndVersion);
		// call under test
		managerSpy.sendAsynchronousActivitySignal(idAndVersion);
		verify(managerSpy).getTableType(idAndVersion);
		MessageToSend expected = new MessageToSend().withObjectId(idAndVersion.getId().toString())
				.withObjectVersion(null).withObjectType(ObjectType.ENTITY_VIEW)
				.withChangeType(ChangeType.UPDATE);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(expected);
	}
	
	@Test
	public void testSendAsynchronousActivitySignalWithVersion() {
		idAndVersion = IdAndVersion.parse("syn123.1");
		doReturn(ObjectType.ENTITY_VIEW).when(managerSpy).getTableType(idAndVersion);
		// call under test
		managerSpy.sendAsynchronousActivitySignal(idAndVersion);
		verify(managerSpy).getTableType(idAndVersion);
		MessageToSend expected = new MessageToSend().withObjectId("123")
				.withObjectVersion(new Long(1)).withObjectType(ObjectType.ENTITY_VIEW)
				.withChangeType(ChangeType.UPDATE);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(expected);
	}
	
	@Test
	public void testSendAsynchronousActivitySignalWithNonView() {
		doReturn(ObjectType.ENTITY).when(managerSpy).getTableType(idAndVersion);
		// call under test
		managerSpy.sendAsynchronousActivitySignal(idAndVersion);
		verify(managerSpy).getTableType(idAndVersion);
		verifyZeroInteractions(mockTransactionalMessenger);
	}
	
	@Test
	public void testGetIndexDescriptionWithTable() {
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.table);
		// call under test
		IndexDescription result = manager.getIndexDescription(idAndVersion);
		IndexDescription expected = new TableIndexDescription(idAndVersion);
		assertEquals(expected, result);
		verify(mockNodeDao).getNodeTypeById(idAndVersion.getId().toString());
		verifyZeroInteractions(mockMaterializedViewDao);
	}
	
	@Test
	public void testGetIndexDescriptionWithEntityView() {
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.entityview);
		// call under test
		IndexDescription result = manager.getIndexDescription(idAndVersion);
		IndexDescription expected = new ViewIndexDescription(idAndVersion, EntityType.entityview);
		assertEquals(expected, result);
		verify(mockNodeDao).getNodeTypeById(idAndVersion.getId().toString());
		verifyZeroInteractions(mockMaterializedViewDao);
	}
	
	@Test
	public void testGetIndexDescriptionWithDataset() {
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.dataset);
		// call under test
		IndexDescription result = manager.getIndexDescription(idAndVersion);
		IndexDescription expected = new ViewIndexDescription(idAndVersion, EntityType.dataset);
		assertEquals(expected, result);
		verify(mockNodeDao).getNodeTypeById(idAndVersion.getId().toString());
		verifyZeroInteractions(mockMaterializedViewDao);
	}
	
	@Test
	public void testGetIndexDescriptionWithSubmissionView() {
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.submissionview);
		// call under test
		IndexDescription result = manager.getIndexDescription(idAndVersion);
		IndexDescription expected = new ViewIndexDescription(idAndVersion, EntityType.submissionview);
		assertEquals(expected, result);
		verify(mockNodeDao).getNodeTypeById(idAndVersion.getId().toString());
		verifyZeroInteractions(mockMaterializedViewDao);
	}
	
	@Test
	public void testGetIndexDescriptionWithMaterializedView() {
		IdAndVersion tableId = IdAndVersion.parse("syn111");
		IndexDescription tableIndexDescription = new TableIndexDescription(tableId);
		IdAndVersion fileViewId = IdAndVersion.parse("syn222");
		IndexDescription fileViewIndexDescription = new ViewIndexDescription(fileViewId, EntityType.entityview);
		IdAndVersion submissionViewId = IdAndVersion.parse("syn333");
		IndexDescription submissionViewIndexDescription = new ViewIndexDescription(submissionViewId, EntityType.submissionview);
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.materializedview, EntityType.table,
				EntityType.entityview, EntityType.submissionview);
		when(mockMaterializedViewDao.getSourceTablesIds(any()))
				.thenReturn(Sets.newHashSet(tableId, fileViewId, submissionViewId));
		
		// call under test
		IndexDescription result = manager.getIndexDescription(idAndVersion);
		
		IndexDescription expected = new MaterializedViewIndexDescription(idAndVersion,
				Arrays.asList(tableIndexDescription, fileViewIndexDescription, submissionViewIndexDescription));
		assertEquals(expected, result);
		
		verify(mockNodeDao).getNodeTypeById(idAndVersion.getId().toString());
		verify(mockNodeDao).getNodeTypeById(tableId.getId().toString());
		verify(mockNodeDao).getNodeTypeById(fileViewId.getId().toString());
		verify(mockNodeDao).getNodeTypeById(submissionViewId.getId().toString());
		verify(mockNodeDao, times(4)).getNodeTypeById(any());
		verify(mockMaterializedViewDao).getSourceTablesIds(idAndVersion);
	}
	
	@Test
	public void testGetIndexDescriptionWithUnsupportedType() {
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.folder);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.getIndexDescription(idAndVersion);
		}).getMessage();
		assertEquals("Unexpected type for entity with id " + idAndVersion.toString() + ": folder (expected a table or view type)", message);
	}
	
	@Test
	public void testTryRunWithTableNonexclusiveLock() throws Exception {
		IdAndVersion one = IdAndVersion.parse("syn123.4");
		IdAndVersion two = IdAndVersion.parse("syn456");
		// call under test
		manager.tryRunWithTableNonexclusiveLock(mockCallback, mockCallable, one, two);
		verify(mockWriteReadSemaphoreRunner).tryRunWithReadLock(mockCallback, mockCallable, "TABLE-LOCK-123-4",
				"TABLE-LOCK-456");
	}
}
