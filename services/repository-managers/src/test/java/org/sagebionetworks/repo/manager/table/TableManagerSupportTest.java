package org.sagebionetworks.repo.manager.table;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModelMapper;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProviderFactory;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.MaterializedViewDao;
import org.sagebionetworks.repo.model.dbo.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableSnapshot;
import org.sagebionetworks.repo.model.dbo.dao.table.TableSnapshotDao;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.entity.IdAndVersionParser;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.semaphore.LockContext;
import org.sagebionetworks.repo.model.semaphore.LockContext.ContextType;
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
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.TimeoutUtils;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockType;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.sagebionetworks.workers.util.semaphore.ReadLock;
import org.sagebionetworks.workers.util.semaphore.ReadLockRequest;
import org.sagebionetworks.workers.util.semaphore.WriteLock;
import org.sagebionetworks.workers.util.semaphore.WriteLockRequest;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphore;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
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
	private AsyncJobProgressCallback mockAsynchCallback;
	@Mock
	private TableSnapshotDao mockViewSnapshotDao;
	@Mock
	private MetadataIndexProviderFactory mockMetadataIndexProviderFactory;
	@Mock
	private MetadataIndexProvider mockMetadataIndexProvider;
	@Mock
	private DefaultColumnModelMapper mockDefaultColumnModelMapper;
	@Mock
	private MaterializedViewDao mockMaterializedViewDao;
	@Mock
	private WriteReadSemaphore mockWriteReadSemaphore;
	@Mock
	private FileProvider mockFileProvider;
	@Mock
	private SynapseS3Client mockS3Client;
	@Mock
	private Clock mockClock;
	@Mock
	private LoggerProvider mockLoggerProvider;
	@Mock
	private Logger mockLogger;
	
	private TableManagerSupportImpl manager;
	private TableManagerSupportImpl managerSpy;
	
	@Mock
	private DefaultColumnModel mockDefaultModel;
	
	@Mock
	private ProgressingCallable<String> mockCallable;
	
	@Mock
	private File mockFile;
	@Mock
	private OutputStream mockOutStream;
	@Mock
	private GZIPOutputStream mockGzipOutStream;
	@Mock
	private InputStream mockInStream;
	@Mock
	private GZIPInputStream mockGzipInStream;
	
	@Mock
	private ReadLock mockReadLock;
	@Mock
	private WriteLock mockWriteLock;
	
	private String schemaMD5Hex;
	private String tableId;
	private IdAndVersion idAndVersion;
	private Long tableIdLong;
	private TableStatus status;
	private String etag;
	private Set<Long> scope;
	private UserInfo userInfo;
	private List<ColumnModel> columns;
	private List<String> columnIds;
	
	private ViewScopeType scopeType;
	
	private LockContext lockContext;
	
	@BeforeEach
	public void before() throws Exception {
		when(mockLoggerProvider.getLogger(any())).thenReturn(mockLogger);
		manager = new TableManagerSupportImpl(mockTableStatusDAO, mockTimeoutUtils, mockTransactionalMessenger,
				mockTableConnectionFactory, mockColumnModelManager, mockNodeDao, mockTableTruthDao, mockViewScopeDao,
				mockWriteReadSemaphore, mockAuthorizationManager, mockViewSnapshotDao, mockMetadataIndexProviderFactory,
				mockDefaultColumnModelMapper, mockMaterializedViewDao, mockFileProvider, mockS3Client, mockClock, mockLoggerProvider);
		managerSpy = Mockito.spy(manager);
			
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
		
		scopeType = new ViewScopeType(ViewObjectType.ENTITY, ViewTypeMask.File.getMask());
		
		lockContext = new LockContext(ContextType.Query, idAndVersion);
		
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
		when(mockTableIndexDAO.doesIndexStateMatch(any(IdAndVersion.class), anyLong(), anyString(), anyBoolean())).thenReturn(true);
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
		when(mockTableIndexDAO.doesIndexStateMatch(any(IdAndVersion.class), anyLong(), anyString(), anyBoolean())).thenReturn(false);
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
		when(mockNodeDao.isSearchEnabled(any(), any())).thenReturn(true);

		long currentVersion = 3L;

		TableRowChange lastChange = new TableRowChange();
		lastChange.setRowVersion(currentVersion);
		when(mockTableTruthDao.getLastTableChangeNumber(tableIdLong)).thenReturn(Optional.of(currentVersion));
		// setup a match
		when(mockTableIndexDAO.doesIndexStateMatch(idAndVersion, currentVersion, schemaMD5Hex, true)).thenReturn(true);
		
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
		when(mockTableIndexDAO.doesIndexStateMatch(any(IdAndVersion.class), anyLong(), anyString(), anyBoolean())).thenReturn(true);
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
		when(mockTableIndexDAO.doesIndexStateMatch(any(IdAndVersion.class), anyLong(), anyString(), anyBoolean())).thenReturn(true);
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
		when(mockTableIndexDAO.doesIndexStateMatch(any(IdAndVersion.class), anyLong(), anyString(), anyBoolean())).thenReturn(false);
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
		when(mockTableIndexDAO.doesIndexStateMatch(any(IdAndVersion.class), anyLong(), anyString(), anyBoolean())).thenReturn(true);
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
		ObjectType type = manager.getTableObjectType(idAndVersion);
		assertEquals(ObjectType.TABLE, type);
	}

	@Test
	public void testGetTableTypeFileView() {
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(
				EntityType.entityview);
		// call under test
		ObjectType type = manager.getTableObjectType(idAndVersion);
		assertEquals(ObjectType.ENTITY_VIEW, type);
	}
	
	@Test
	public void testGetTableTypeMaterializedView() {
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(
				EntityType.materializedview);
		// call under test
		ObjectType type = manager.getTableObjectType(idAndVersion);
		assertEquals(ObjectType.MATERIALIZED_VIEW, type);
	}

	@Test
	public void testGetTableTypeUnknown() {
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(
				EntityType.project);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.getTableObjectType(idAndVersion);
		});
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
		IndexDescription indexDescription = new ViewIndexDescription(idAndVersion, TableType.entityview);
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		//  call under test
		manager.validateTableReadAccess(userInfo, indexDescription);
		verify(mockAuthorizationManager).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		//  do not need download for FileView
		verify(mockAuthorizationManager, never()).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD);
	}
	
	@Test
	public void testValidateTableReadAccessFileViewNoRead(){
		IndexDescription indexDescription = new ViewIndexDescription(idAndVersion, TableType.entityview);
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
		IndexDescription viewDescription = new ViewIndexDescription(viewId, TableType.entityview);
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
		doReturn(ObjectType.ENTITY_VIEW).when(managerSpy).getTableObjectType(idAndVersion);
		// call under test
		managerSpy.sendAsynchronousActivitySignal(idAndVersion);
		verify(managerSpy).getTableObjectType(idAndVersion);
		MessageToSend expected = new MessageToSend().withObjectId(idAndVersion.getId().toString())
				.withObjectVersion(null).withObjectType(ObjectType.ENTITY_VIEW)
				.withChangeType(ChangeType.UPDATE);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(expected);
	}
	
	@Test
	public void testSendAsynchronousActivitySignalWithVersion() {
		idAndVersion = IdAndVersion.parse("syn123.1");
		doReturn(ObjectType.ENTITY_VIEW).when(managerSpy).getTableObjectType(idAndVersion);
		// call under test
		managerSpy.sendAsynchronousActivitySignal(idAndVersion);
		verify(managerSpy).getTableObjectType(idAndVersion);
		MessageToSend expected = new MessageToSend().withObjectId("123")
				.withObjectVersion(new Long(1)).withObjectType(ObjectType.ENTITY_VIEW)
				.withChangeType(ChangeType.UPDATE);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(expected);
	}
	
	@Test
	public void testSendAsynchronousActivitySignalWithNonView() {
		doReturn(ObjectType.ENTITY).when(managerSpy).getTableObjectType(idAndVersion);
		// call under test
		managerSpy.sendAsynchronousActivitySignal(idAndVersion);
		verify(managerSpy).getTableObjectType(idAndVersion);
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
		IndexDescription expected = new ViewIndexDescription(idAndVersion, TableType.entityview);
		assertEquals(expected, result);
		verify(mockNodeDao).getNodeTypeById(idAndVersion.getId().toString());
		verifyZeroInteractions(mockMaterializedViewDao);
	}
	
	@Test
	public void testGetIndexDescriptionWithDataset() {
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.dataset);
		// call under test
		IndexDescription result = manager.getIndexDescription(idAndVersion);
		IndexDescription expected = new ViewIndexDescription(idAndVersion, TableType.dataset);
		assertEquals(expected, result);
		verify(mockNodeDao).getNodeTypeById(idAndVersion.getId().toString());
		verifyZeroInteractions(mockMaterializedViewDao);
	}
	
	@Test
	public void testGetIndexDescriptionWithSubmissionView() {
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.submissionview);
		// call under test
		IndexDescription result = manager.getIndexDescription(idAndVersion);
		IndexDescription expected = new ViewIndexDescription(idAndVersion, TableType.submissionview);
		assertEquals(expected, result);
		verify(mockNodeDao).getNodeTypeById(idAndVersion.getId().toString());
		verifyZeroInteractions(mockMaterializedViewDao);
	}
	
	@Test
	public void testGetIndexDescriptionWithMaterializedView() {
		IdAndVersion tableId = IdAndVersion.parse("syn111");
		IndexDescription tableIndexDescription = new TableIndexDescription(tableId);
		IdAndVersion fileViewId = IdAndVersion.parse("syn222");
		IndexDescription fileViewIndexDescription = new ViewIndexDescription(fileViewId, TableType.entityview);
		IdAndVersion submissionViewId = IdAndVersion.parse("syn333");
		IndexDescription submissionViewIndexDescription = new ViewIndexDescription(submissionViewId, TableType.submissionview);
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
		assertEquals("syn123 is not a table or view", message);
	}
	
	
	@Test
	public void testStreamTableIndexToS3() throws IOException {
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		StringWriter writer = new StringWriter();
		when(mockFileProvider.createFileOutputStream(mockFile)).thenReturn(mockOutStream);
		when(mockFileProvider.createGZIPOutputStream(mockOutStream)).thenReturn(mockGzipOutStream);
		when(mockFileProvider.createWriter(mockGzipOutStream, StandardCharsets.UTF_8)).thenReturn(writer);
		when(mockTableConnectionFactory.getConnection(any())).thenReturn(mockTableIndexDAO);
		when(mockTableIndexDAO.streamTableIndexData(any(), any())).thenReturn(columnIds);
		
		String bucket = "snapshot.bucket";
		String key = "key";
		
		// call under test
		List<String> result = manager.streamTableIndexToS3(idAndVersion, bucket, key);
		
		assertEquals(columnIds, result);
		
		verify(mockFileProvider).createTempFile("table", ".csv");
		verify(mockFileProvider).createFileOutputStream(mockFile);
		verify(mockFileProvider).createGZIPOutputStream(mockOutStream);
		verify(mockFileProvider).createWriter(mockGzipOutStream, StandardCharsets.UTF_8);
		verify(mockTableConnectionFactory).getConnection(idAndVersion);
		verify(mockTableIndexDAO).streamTableIndexData(eq(idAndVersion), any());
		
		ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		
		verify(mockS3Client).putObject(putRequestCaptor.capture());
		PutObjectRequest putRequest = putRequestCaptor.getValue();
		assertEquals(bucket, putRequest.getBucketName());
		assertEquals(key, putRequest.getKey());
		assertEquals(mockFile, putRequest.getFile());
		verify(mockFile).delete();
	}
	
	@Test
	public void testStreamTableIndexToS3WithIOException() throws IOException {
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		IOException ex = new FileNotFoundException("nope");
		when(mockFileProvider.createFileOutputStream(mockFile)).thenThrow(ex);
		
		String bucket = "snapshot.bucket";
		String key = "key";
		
		RuntimeException result = assertThrows(RuntimeException.class, () -> {			
			// call under test
			manager.streamTableIndexToS3(idAndVersion, bucket, key);
		});
		
		assertEquals(ex, result.getCause());
		
		verify(mockFileProvider).createTempFile("table", ".csv");
		verify(mockTableIndexDAO, never()).streamTableIndexData(any(), any());
		verify(mockFile).delete();
	}
	
	@Test
	public void testStreamTableIndexToS3WithAmazonServiceException() throws IOException {
		when(mockTableConnectionFactory.getConnection(any())).thenReturn(mockTableIndexDAO);
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		when(mockFileProvider.createFileOutputStream(any())).thenReturn(mockOutStream);
		when(mockFileProvider.createGZIPOutputStream(any())).thenReturn(mockGzipOutStream);
		when(mockFileProvider.createWriter(any(), any())).thenReturn(new StringWriter());
		when(mockTableIndexDAO.streamTableIndexData(any(), any())).thenReturn(columnIds);
		
		AmazonServiceException ex = new AmazonServiceException("nope");
		ex.setErrorType(ErrorType.Service);
		
		when(mockS3Client.putObject(any())).thenThrow(ex);
		
		String bucket = "snapshot.bucket";
		String key = "key";
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {			
			// call under test
			manager.streamTableIndexToS3(idAndVersion, bucket, key);
		});
		
		assertEquals(ex, result.getCause());
		
		verify(mockFileProvider).createTempFile("table", ".csv");
		verify(mockTableIndexDAO).streamTableIndexData(eq(idAndVersion), any());
		verify(mockFile).delete();
	}
	
	@Test
	public void testStreamTableIndexToS3WithAmazonClientException() throws IOException {
		when(mockTableConnectionFactory.getConnection(any())).thenReturn(mockTableIndexDAO);
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		when(mockFileProvider.createFileOutputStream(any())).thenReturn(mockOutStream);
		when(mockFileProvider.createGZIPOutputStream(any())).thenReturn(mockGzipOutStream);
		when(mockFileProvider.createWriter(any(), any())).thenReturn(new StringWriter());
		when(mockTableIndexDAO.streamTableIndexData(any(), any())).thenReturn(columnIds);
		
		AmazonServiceException ex = new AmazonServiceException("nope");
		ex.setErrorType(ErrorType.Client);
		
		when(mockS3Client.putObject(any())).thenThrow(ex);
		
		String bucket = "snapshot.bucket";
		String key = "key";
		
		AmazonServiceException result = assertThrows(AmazonServiceException.class, () -> {			
			// call under test
			manager.streamTableIndexToS3(idAndVersion, bucket, key);
		});
		
		assertEquals(ex, result);
		
		verify(mockFileProvider).createTempFile("table", ".csv");
		verify(mockTableIndexDAO).streamTableIndexData(eq(idAndVersion), any());
		verify(mockFile).delete();
	}
	
	@Test
	public void testRestoreTableIndexFromS3() throws IOException {
		
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(mockFile);
		when(mockFileProvider.createFileInputStream(any())).thenReturn(mockInStream);
		when(mockFileProvider.createGZIPInputStream(any())).thenReturn(mockGzipInStream);
		when(mockFileProvider.createReader(any(), any())).thenReturn(new StringReader("foo"));
		when(mockTableConnectionFactory.getConnection(any())).thenReturn(mockTableIndexDAO);
		
		String bucket = "bucket";
		String key = "key";
		
		// Call under test
		manager.restoreTableIndexFromS3(idAndVersion, bucket, key);
		
		verify(mockFileProvider).createTempFile("TableSnapshotDownload", ".csv.gzip");
		
		ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
		verify(mockS3Client).getObject(requestCaptor.capture(), eq(mockFile));
		
		GetObjectRequest s3Request = requestCaptor.getValue();
		
		assertEquals(bucket, s3Request.getBucketName());
		assertEquals(key, s3Request.getKey());
		
		verify(mockFileProvider).createFileInputStream(mockFile);
		verify(mockFileProvider).createGZIPInputStream(mockInStream);
		verify(mockFileProvider).createReader(mockGzipInStream, StandardCharsets.UTF_8);
		verify(mockTableConnectionFactory).getConnection(idAndVersion);
		verify(mockTableIndexDAO).restoreTableIndexData(eq(idAndVersion), any(), eq(TableManagerSupportImpl.MAX_BYTES_PER_BATCH));
		verify(mockFile).delete();
	}
	
	@Test
	public void testRestoreTableIndexFromS3WithIOException() throws IOException {
		
		IOException ex = new IOException("nope");
		
		when(mockFileProvider.createTempFile(any(), any())).thenThrow(ex);
		
		String bucket = "bucket";
		String key = "key";
		
		RuntimeException result = assertThrows(RuntimeException.class, () -> {
			// Call under test
			manager.restoreTableIndexFromS3(idAndVersion, bucket, key);
		});
		
		assertEquals(ex, result.getCause());
		
		verify(mockFileProvider).createTempFile("TableSnapshotDownload", ".csv.gzip");
		verifyNoMoreInteractions(mockFileProvider);
		verifyNoMoreInteractions(mockFile);
		verifyZeroInteractions(mockS3Client);
		verifyZeroInteractions(mockTableIndexDAO);
	}
	
	@Test
	public void testRestoreTableIndexFromS3WithAmazonServiceException() throws IOException {
		
		AmazonServiceException ex = new AmazonServiceException("nope");
		ex.setErrorType(ErrorType.Service);
		
		when(mockS3Client.getObject(any(), any(File.class))).thenThrow(ex);
		
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(mockFile);
		
		String bucket = "bucket";
		String key = "key";
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			manager.restoreTableIndexFromS3(idAndVersion, bucket, key);
		});
		
		assertEquals(ex, result.getCause());
		
		verify(mockFileProvider).createTempFile("TableSnapshotDownload", ".csv.gzip");
		
		ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
		verify(mockS3Client).getObject(requestCaptor.capture(), eq(mockFile));
		
		GetObjectRequest s3Request = requestCaptor.getValue();
		
		assertEquals(bucket, s3Request.getBucketName());
		assertEquals(key, s3Request.getKey());
		
		verifyNoMoreInteractions(mockFileProvider);
		verifyZeroInteractions(mockTableIndexDAO);
		verify(mockFile).delete();
	}
	
	@Test
	public void testRestoreTableIndexFromS3WithAmazonClientException() throws IOException {
		
		AmazonServiceException ex = new AmazonServiceException("nope");
		ex.setErrorType(ErrorType.Client);
		
		when(mockS3Client.getObject(any(), any(File.class))).thenThrow(ex);
		
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(mockFile);
		
		String bucket = "bucket";
		String key = "key";
		
		AmazonServiceException result = assertThrows(AmazonServiceException.class, () -> {			
			// Call under test
			manager.restoreTableIndexFromS3(idAndVersion, bucket, key);
		});
		
		assertEquals(ex, result);
		
		verify(mockFileProvider).createTempFile("TableSnapshotDownload", ".csv.gzip");
		
		ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
		verify(mockS3Client).getObject(requestCaptor.capture(), eq(mockFile));
		
		GetObjectRequest s3Request = requestCaptor.getValue();
		
		assertEquals(bucket, s3Request.getBucketName());
		assertEquals(key, s3Request.getKey());
		
		verifyNoMoreInteractions(mockFileProvider);
		verifyZeroInteractions(mockTableIndexDAO);
		verify(mockFile).delete();
	}
	
	@Test
	public void testGetMostRecentTableSnapshot() {
		idAndVersion = IdAndVersion.parse("123.3");
		
		TableSnapshot expected = new TableSnapshot()
				.withSnapshotId(111L)
				.withTableId(idAndVersion.getId())
				.withVersion(idAndVersion.getVersion().get());
		
		when(mockViewSnapshotDao.getMostRecentTableSnapshot(any())).thenReturn(Optional.of(expected));
		
		// Call under test
		Optional<TableSnapshot> result = manager.getMostRecentTableSnapshot(idAndVersion);
		
		assertEquals(expected, result.get());
		
		verify(mockViewSnapshotDao).getMostRecentTableSnapshot(idAndVersion);
	}
	
	@Test
	public void testTryRunWithTableNonExclusiveLock() throws Exception {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(3L);
		when(mockCallable.call(any())).thenReturn("some result");
		when(mockWriteReadSemaphore.getReadLock(any())).thenReturn(mockReadLock);
		
		// call under test
		String result = managerSpy.tryRunWithTableNonExclusiveLock(mockCallback, lockContext, mockCallable, "one","two");
		assertEquals("some result", result);
		
		verify(mockWriteReadSemaphore).getReadLock(new ReadLockRequest(mockCallback, lockContext.serializeToString(), "one","two"));
		verify(mockCallable).call(mockCallback);
		verify(managerSpy).logContext(mockCallback, lockContext);
		verify(managerSpy, never()).logWaitingForContext(any(), any(), any());
		verify(mockReadLock).close();
	}
	
	@Test
	public void testTryRunWithTableNonExclusiveLockWithCallableException() throws Exception {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(3L);
		Exception exception = new IllegalArgumentException("something is wrong");
		when(mockCallable.call(any())).thenThrow(exception);
		when(mockWriteReadSemaphore.getReadLock(any())).thenReturn(mockReadLock);
		
		Exception resultException = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			managerSpy.tryRunWithTableNonExclusiveLock(mockCallback, lockContext, mockCallable, "one","two");
		});
		assertEquals(exception, resultException);
		
		verify(mockWriteReadSemaphore).getReadLock(new ReadLockRequest(mockCallback, lockContext.serializeToString(), "one","two"));
		verify(mockCallable).call(mockCallback);
		verify(managerSpy).logContext(mockCallback, lockContext);
		verify(managerSpy, never()).logWaitingForContext(any(), any(), any());
		verify(mockReadLock).close();
	}
	
	@Test
	public void testTryRunWithTableNonExclusiveLockWithLockUnavailable() throws Exception {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(3L);
		LockContext waitingOnOne = new LockContext(ContextType.BuildTableIndex, idAndVersion);
		Exception toThrow = new LockUnavilableException(LockType.Read, "one", waitingOnOne.serializeToString());
		when(mockWriteReadSemaphore.getReadLock(any())).thenThrow(toThrow);
		
		Exception thrown = assertThrows(LockUnavilableException.class, ()->{
			// call under test
			managerSpy.tryRunWithTableNonExclusiveLock(mockCallback, lockContext, mockCallable, "one","two");
		});
		assertEquals(toThrow, thrown);
		
		verify(mockWriteReadSemaphore).getReadLock(new ReadLockRequest(mockCallback, lockContext.serializeToString(), "one","two"));
		verify(mockCallable, never()).call(any());
		verify(managerSpy).logContext(mockCallback, lockContext);
		verify(managerSpy).logWaitingForContext(mockCallback, lockContext, waitingOnOne);
		verify(mockReadLock, never()).close();
	}
	
	@Test
	public void testTryRunWithTableNonExclusiveLockWithLockUnavailableNullContext() throws Exception {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(3L);
		String contextString = null;
		Exception toThrow = new LockUnavilableException(LockType.Read, "one", contextString);
		when(mockWriteReadSemaphore.getReadLock(any())).thenThrow(toThrow);
		LockContext context = new LockContext(ContextType.Query, idAndVersion);
		
		Exception thrown = assertThrows(LockUnavilableException.class, ()->{
			// call under test
			managerSpy.tryRunWithTableNonExclusiveLock(mockCallback, context, mockCallable, "one","two");
		});
		assertEquals(toThrow, thrown);
		
		verify(mockWriteReadSemaphore).getReadLock(new ReadLockRequest(mockCallback, context.serializeToString(), "one","two"));
		verify(mockCallable, never()).call(any());
		verify(managerSpy).logContext(mockCallback, context);
		verify(managerSpy, never()).logWaitingForContext(any(), any(), any());
	}
	
	@Test
	public void testTryRunWithTableNoExclusiveLockWithIdAndVersion() throws Exception {

		doReturn("some result").when(managerSpy).tryRunWithTableNonExclusiveLock(any(), any(), any(),
				any(String.class));
		IdAndVersion one = IdAndVersionParser.parseIdAndVersion("syn123");
		IdAndVersion two = IdAndVersionParser.parseIdAndVersion("syn456");

		// call under test
		String result = managerSpy.tryRunWithTableNonExclusiveLock(mockCallback, lockContext, mockCallable,
				one, two);
		assertEquals("some result", result);

		verify(managerSpy).tryRunWithTableNonExclusiveLock(mockCallback, lockContext, mockCallable,
				TableModelUtils.getTableSemaphoreKey(one), TableModelUtils.getTableSemaphoreKey(two));
	}
	
	@Test
	public void testTryRunWithTableNonExclusiveLockWithNullCallback() {
		mockCallback = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			managerSpy.tryRunWithTableNonExclusiveLock(mockCallback, lockContext, mockCallable, "one","two");
		}).getMessage();
		assertEquals("callback is required.", message);
	}
	
	@Test
	public void testTryRunWithTableNonExclusiveLockWithNullContext() {
		lockContext = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			managerSpy.tryRunWithTableNonExclusiveLock(mockCallback, lockContext, mockCallable, "one","two");
		}).getMessage();
		assertEquals("context is required.", message);
	}
	
	@Test
	public void testTryRunWithTableNonExclusiveLockWithNullCallable() {
		mockCallable = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			managerSpy.tryRunWithTableNonExclusiveLock(mockCallback, lockContext, mockCallable, "one","two");
		}).getMessage();
		assertEquals("runner is required.", message);
	}
	
	
	@Test
	public void testTryRunWithTableExclusiveLock() throws Exception {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(3L);
		when(mockCallable.call(any())).thenReturn("some result");
		when(mockWriteReadSemaphore.getWriteLock(any())).thenReturn(mockWriteLock);
		when(mockWriteLock.getExistingReadLockContext()).thenReturn(Optional.empty());
		
		// call under test
		String result = managerSpy.tryRunWithTableExclusiveLock(mockCallback, lockContext, "one", mockCallable);
		assertEquals("some result", result);
		
		verify(mockWriteReadSemaphore).getWriteLock(new WriteLockRequest(mockCallback, lockContext.serializeToString(), "one"));
		verify(mockCallable).call(mockCallback);
		verify(managerSpy).logContext(mockCallback, lockContext);
		verify(managerSpy, never()).logWaitingForContext(any(), any(), any());
		verify(mockWriteLock).close();
		verify(mockClock, never()).sleep(anyLong());
	}

	@Test
	public void testTryRunWithTableExclusiveLockWithWaitingForReadLocks() throws Exception {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(3L);
		when(mockCallable.call(any())).thenReturn("some result");
		when(mockWriteReadSemaphore.getWriteLock(any())).thenReturn(mockWriteLock);
		
		doNothing().when(managerSpy).logContext(any(), any());
		doNothing().when(managerSpy).logWaitingForContext(any(), any(), any());
		
		LockContext readLockContextOne = new LockContext(ContextType.Query, idAndVersion);
		LockContext readLockContextTwo = new LockContext(ContextType.TableSnapshot, idAndVersion);
	
		when(mockWriteLock.getExistingReadLockContext()).thenReturn(
				Optional.of(readLockContextOne.serializeToString()),
				Optional.of(readLockContextTwo.serializeToString()),
				Optional.empty());
		
		// call under test
		String result = managerSpy.tryRunWithTableExclusiveLock(mockCallback, lockContext, "one", mockCallable);
		assertEquals("some result", result);
		
		verify(mockWriteReadSemaphore).getWriteLock(new WriteLockRequest(mockCallback, lockContext.serializeToString(), "one"));
		verify(mockCallable).call(mockCallback);
		verify(managerSpy).logContext(mockCallback, lockContext);
		verify(managerSpy).logWaitingForContext(mockCallback, lockContext, readLockContextOne);
		verify(managerSpy).logWaitingForContext(mockCallback, lockContext, readLockContextTwo);
		verify(managerSpy, times(2)).logWaitingForContext(any(), any(), any());
		verify(mockWriteLock).close();
		verify(mockClock, times(2)).sleep(2000L);
	}
	
	@Test
	public void testTryRunWithTableExclusiveLockWithLockUnavailableException() throws Exception {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(3L);
		LockContext waitingOnOne = new LockContext(ContextType.BuildTableIndex, idAndVersion);
		Exception toThrow = new LockUnavilableException(LockType.Read, "one", waitingOnOne.serializeToString());
		when(mockWriteReadSemaphore.getWriteLock(any())).thenThrow(toThrow);
		
		Exception thrown = assertThrows(LockUnavilableException.class, ()->{
			// call under test
			managerSpy.tryRunWithTableExclusiveLock(mockCallback, lockContext, "one", mockCallable);
		});
		assertEquals(toThrow, thrown);
		
		verify(mockWriteReadSemaphore).getWriteLock(new WriteLockRequest(mockCallback, lockContext.serializeToString(), "one"));
		verify(mockCallable, never()).call(mockCallback);
		verify(managerSpy).logContext(mockCallback, lockContext);
		verify(managerSpy).logWaitingForContext(mockCallback, lockContext, waitingOnOne);
		verify(mockWriteLock, never()).close();
		verify(mockClock, never()).sleep(anyLong());
	}
	
	@Test
	public void testTryRunWithTableExclusiveLockWithLockUnavailableExceptionNullContext() throws Exception {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(3L);
		Exception toThrow = new LockUnavilableException(LockType.Read, "one", null);
		when(mockWriteReadSemaphore.getWriteLock(any())).thenThrow(toThrow);
		
		Exception thrown = assertThrows(LockUnavilableException.class, ()->{
			// call under test
			managerSpy.tryRunWithTableExclusiveLock(mockCallback, lockContext, "one", mockCallable);
		});
		assertEquals(toThrow, thrown);
		
		verify(mockWriteReadSemaphore).getWriteLock(new WriteLockRequest(mockCallback, lockContext.serializeToString(), "one"));
		verify(mockCallable, never()).call(mockCallback);
		verify(managerSpy).logContext(mockCallback, lockContext);
		verify(managerSpy, never()).logWaitingForContext(any(), any(), any());
		verify(mockWriteLock, never()).close();
		verify(mockClock, never()).sleep(anyLong());
	}
	
	@Test
	public void testTryRunWithTableExclusiveLockWithCallableException() throws Exception {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(3L);
		Exception toThrow = new IllegalAccessException("something is wrong");
		when(mockCallable.call(any())).thenThrow(toThrow);
		when(mockWriteReadSemaphore.getWriteLock(any())).thenReturn(mockWriteLock);
		
		Exception thrown = assertThrows(IllegalAccessException.class, ()->{
			// call under test
			managerSpy.tryRunWithTableExclusiveLock(mockCallback, lockContext, "one", mockCallable);
		});
		assertEquals(toThrow, thrown);
		
		verify(mockWriteReadSemaphore).getWriteLock(new WriteLockRequest(mockCallback, lockContext.serializeToString(), "one"));
		verify(mockCallable).call(mockCallback);
		verify(managerSpy).logContext(mockCallback, lockContext);
		verify(managerSpy, never()).logWaitingForContext(any(), any(), any());
		verify(mockWriteLock).close();
		verify(mockClock, never()).sleep(anyLong());
	}
	
	@Test
	public void testTryRunWithTableExclusiveLockWithNullCallback() {
		mockCallback = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			managerSpy.tryRunWithTableExclusiveLock(mockCallback, lockContext, "one", mockCallable);
		}).getMessage();
		assertEquals("callback is required.", message);
	}
	
	@Test
	public void testTryRunWithTableExclusiveLockWithNullContext() {
		lockContext = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			managerSpy.tryRunWithTableExclusiveLock(mockCallback, lockContext, "one", mockCallable);
		}).getMessage();
		assertEquals("context is required.", message);
	}
	
	@Test
	public void testTryRunWithTableExclusiveLockWithNullKey() {
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			managerSpy.tryRunWithTableExclusiveLock(mockCallback, lockContext, (String)null, mockCallable);
		}).getMessage();
		assertEquals("key is required.", message);
	}
	
	@Test
	public void testTryRunWithTableExclusiveLockWithNullCallable() {
		mockCallable = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			managerSpy.tryRunWithTableExclusiveLock(mockCallback, lockContext, "one", mockCallable);
		}).getMessage();
		assertEquals("callable is required.", message);
	}
	
	@Test
	public void testTryRunWithTableExclusiveLockWithIdAndVersion() throws Exception {
		
		doReturn("some result").when(managerSpy).tryRunWithTableExclusiveLock(any(),any(),any(String.class),any());
		
		// call under test
		String result = managerSpy.tryRunWithTableExclusiveLock(mockCallback, lockContext, idAndVersion, mockCallable);
		assertEquals("some result", result);
		
		verify(managerSpy).tryRunWithTableExclusiveLock(mockCallback, lockContext, TableModelUtils.getTableSemaphoreKey(idAndVersion), mockCallable);
	}
	
	@Test
	public void testLockContext() {
		
		// call under test
		managerSpy.logContext(mockCallback, lockContext);
		
		verify(mockLogger).info("Querying table/view: 'syn123' ...");
	}
	
	@Test
	public void testLockContextWithAsynchCallabck() {
		
		// call under test
		managerSpy.logContext(mockAsynchCallback, lockContext);
		
		verify(mockLogger).info("Querying table/view: 'syn123' ...");
		verify(mockAsynchCallback).updateProgress("Querying table/view: 'syn123' ...", 0L, 100L);
	}
	
	@Test
	public void testLogWaitingForContext() {
		LockContext waitingOn = new LockContext(ContextType.TableUpdate, IdAndVersionParser.parseIdAndVersion("syn456"));
		
		// call under test
		managerSpy.logWaitingForContext(mockCallback, lockContext, waitingOn);
		
		verify(mockLogger).info("[Query, syn123] waiting on [TableUpdate, syn456]");
		verify(mockLogger).info("Applying an update to table: 'syn456' ...");

	}
	
	@Test
	public void testLogWaitingForContextWithAsyncCallback() {
		LockContext waitingOn = new LockContext(ContextType.TableUpdate, IdAndVersionParser.parseIdAndVersion("syn456"));
		
		// call under test
		managerSpy.logWaitingForContext(mockAsynchCallback, lockContext, waitingOn);
		
		verify(mockLogger).info("[Query, syn123] waiting on [TableUpdate, syn456]");
		verify(mockLogger).info("Applying an update to table: 'syn456' ...");

		verify(mockAsynchCallback).updateProgress("Applying an update to table: 'syn456' ...", 0L, 100L);
	}
	
	@Test
	public void testTriggerIndexUpdate() {
		doReturn(ObjectType.ENTITY_VIEW).when(managerSpy).getTableObjectType(idAndVersion);

		// Call under test
		managerSpy.triggerIndexUpdate(idAndVersion);
		
		verify(managerSpy).getTableObjectType(idAndVersion);
		
		MessageToSend expected = new MessageToSend()
			.withObjectId(idAndVersion.getId().toString())
			.withObjectType(ObjectType.ENTITY_VIEW)
			.withObjectVersion(null)
			.withChangeType(ChangeType.UPDATE);
		
		verify(mockTransactionalMessenger).sendMessageAfterCommit(expected);
		
	}
	
}
