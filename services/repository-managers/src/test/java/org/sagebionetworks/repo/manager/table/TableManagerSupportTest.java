package org.sagebionetworks.repo.manager.table;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySetOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewSnapshotDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityField;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.TimeoutUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class TableManagerSupportTest {

	@Mock
	TableStatusDAO mockTableStatusDAO;
	@Mock
	ConnectionFactory mockTableConnectionFactory;
	@Mock
	TableIndexDAO mockTableIndexDAO;
	@Mock
	TimeoutUtils mockTimeoutUtils;
	@Mock
	TransactionalMessenger mockTransactionalMessenger;
	@Mock
	ColumnModelManager mockColumnModelManager;
	@Mock
	NodeDAO mockNodeDao;
	@Mock
	TableRowTruthDAO mockTableTruthDao;
	@Mock
	ViewScopeDao mockViewScopeDao;
	@Mock
	AuthorizationManager mockAuthorizationManager;
	@Mock
	ProgressCallback mockCallback;
	@Mock
	ViewSnapshotDao mockViewSnapshotDao;
	
	@InjectMocks
	TableManagerSupportImpl manager;
	
	String schemaMD5Hex;
	

	String tableId;
	IdAndVersion idAndVersion;
	Long tableIdLong;
	TableStatus status;
	String etag;
	Set<Long> scope;
	Set<Long> containersInScope;
	UserInfo userInfo;
	Long viewType;
	List<ColumnModel> columns;
	List<String> columnIds;
	
	Integer callableReturn;
	
	@BeforeEach
	public void before() throws Exception {
		callableReturn = 123;
		
		userInfo = new UserInfo(false, 8L);
		
		idAndVersion = IdAndVersion.parse("syn123");
		tableId = idAndVersion.getId().toString();
		tableIdLong = KeyFactory.stringToKey(tableId);
		viewType = ViewTypeMask.File.getMask();
		
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
		verify(mockTransactionalMessenger, never()).sendMessageAfterCommit(tableId, ObjectType.TABLE, etag, ChangeType.UPDATE);
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
		String synId = KeyFactory.keyToString(id);
		when(mockNodeDao.doesNodeExist(id)).thenReturn(true);
		// call under test
		assertTrue(manager.doesTableExist(idAndVersion));
	}
	
	@Test
	public void testDoesTableExistFalse() {
		Long id = 123L;
		String synId = KeyFactory.keyToString(id);
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
		assertEquals(ObjectType.ENTITY_VIEW, TableManagerSupportImpl.getObjectTypeForEntityType(EntityType.entityview));
	}
	
	@Test
	public void testGetObjectTypeForEntityTypeUnknownType(){
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			TableManagerSupportImpl.getObjectTypeForEntityType(EntityType.project);
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
	public void testGetAllContainerIdsForViewScope() throws LimitExceededException{
		when(mockNodeDao.getAllContainerIds(anyCollection(), anyInt())).thenReturn(containersInScope);
		// call under test.
		Set<Long> containers = manager.getAllContainerIdsForViewScope(idAndVersion, viewType);
		assertEquals(containersInScope, containers);
	}
	
	@Test
	public void testgetAllContainerIdsForScopeOverLimit(){
		Set<Long> overLimit = new HashSet<>();
		int countOverLimit = TableManagerSupportImpl.MAX_CONTAINERS_PER_VIEW+1;
		for(long i=0; i<countOverLimit; i++){
			overLimit.add(i);
		}
		viewType = ViewTypeMask.File.getMask();
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test.
			manager.getAllContainerIdsForScope(overLimit, viewType);
		});
	}
	
	@Test
	public void testgetAllContainerIdsForScopeFiewView() throws LimitExceededException{
		when(mockNodeDao.getAllContainerIds(anyCollection(), anyInt())).thenReturn(containersInScope);

		viewType = ViewTypeMask.File.getMask();
		// call under test.
		Set<Long> containers = manager.getAllContainerIdsForScope(scope, viewType);
		assertEquals(containersInScope, containers);
		verify(mockNodeDao).getAllContainerIds(scope, TableManagerSupportImpl.MAX_CONTAINERS_PER_VIEW);
	}
	
	@Test
	public void testGetAllContainerIdsForScopeProject() throws LimitExceededException{
		viewType = ViewTypeMask.Project.getMask();
		// call under test.
		Set<Long> containers = manager.getAllContainerIdsForScope(scope, viewType);
		assertEquals(scope, containers);
		verify(mockNodeDao, never()).getAllContainerIds(anySetOf(Long.class), anyInt());
	}
	
	/**
	 * For this case the number of IDs in the scope is already over the limit.
	 */
	@Test
	public void testGetAllContainerIdsForScopeOverLimit(){
		Set<Long> tooMany = new HashSet<Long>();
		for(long i=0; i<TableManagerSupportImpl.MAX_CONTAINERS_PER_VIEW+1; i++){
			tooMany.add(i);
		}
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.getAllContainerIdsForScope(tooMany, viewType);
		});
	}
	
	/**
	 * For this case the scope is under the limit, but the expanded containers
	 * would go over the limit.
	 * @throws LimitExceededException 
	 */
	@Test
	public void testGetAllContainerIdsForScopeExpandedOverLimit() throws LimitExceededException{
		// setup limit exceeded.
		LimitExceededException exception = new LimitExceededException("too many");
		doThrow(exception).when(mockNodeDao).getAllContainerIds(anyCollection(), anyInt());
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.getAllContainerIdsForScope(scope, viewType);
		});
	}
	
	@Test
	public void testValidateScopeSize() throws LimitExceededException{
		// call under test
		manager.validateScopeSize(scope, viewType);
		verify(mockNodeDao).getAllContainerIds(scope, TableManagerSupportImpl.MAX_CONTAINERS_PER_VIEW);
	}
	
	@Test
	public void testValidateScopeSizeNullScope() throws LimitExceededException{
		// The scope can be null.
		scope = null;
		// call under test
		manager.validateScopeSize(scope, viewType);
		verify(mockNodeDao, never()).getAllContainerIds(anySetOf(Long.class), anyInt());
	}
	
	@Test
	public void testcreateViewOverLimitMessageFileView(){
		// call under test
		String message = manager.createViewOverLimitMessage(ViewTypeMask.File.getMask());
		assertEquals(TableManagerSupportImpl.SCOPE_SIZE_LIMITED_EXCEEDED_FILE_VIEW, message);
	}
	
	@Test
	public void testcreateViewOverLimitMessageFileAndTableView(){
		// call under test
		String message = manager.createViewOverLimitMessage(ViewTypeMask.getMaskForDepricatedType(ViewType.file_and_table));
		assertEquals(TableManagerSupportImpl.SCOPE_SIZE_LIMITED_EXCEEDED_FILE_VIEW, message);
	}
	
	@Test
	public void testcreateViewOverLimitMessageProjectView(){
		// call under test
		String message = manager.createViewOverLimitMessage(ViewTypeMask.Project.getMask());
		assertEquals(TableManagerSupportImpl.SCOPE_SIZE_LIMITED_EXCEEDED_PROJECT_VIEW, message);
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
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.entityview);

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
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, ()->{
			//  call under test
			manager.validateTableReadAccess(userInfo, idAndVersion);
		});
		
	}
	
	@Test
	public void testValidateTableReadAccessTableEntityNoRead(){
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, ()->{
			//  call under test
			manager.validateTableReadAccess(userInfo, idAndVersion);
		});
	}
	
	@Test
	public void testValidateTableReadAccessFileView(){
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.entityview);
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		//  call under test
		manager.validateTableReadAccess(userInfo, idAndVersion);
		verify(mockAuthorizationManager).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		//  do not need download for FileView
		verify(mockAuthorizationManager, never()).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD);
	}
	
	@Test
	public void testValidateTableReadAccessFileViewNoRead(){
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, ()->{
			//  call under test
			manager.validateTableReadAccess(userInfo, idAndVersion);
		});
	}
	
	@Test
	public void testValidateTableWriteAccessTableEntity(){
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPLOAD)).thenReturn(AuthorizationStatus.authorized());
		//  call under test
		manager.validateTableWriteAccess(userInfo, idAndVersion);
		verify(mockAuthorizationManager).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
		verify(mockAuthorizationManager).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPLOAD);
	}
	
	@Test
	public void testValidateTableWriteAccessTableEntityNoUpload(){
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPLOAD)).thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, ()->{
			//  call under test
			manager.validateTableWriteAccess(userInfo, idAndVersion);
		});
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
	public void testGetColumModelNotCached(){
		ColumnModel cm = new ColumnModel();
		cm.setId("123");
		when(mockColumnModelManager.createColumnModel(any(ColumnModel.class))).thenReturn(cm);
		ColumnModel result = manager.getColumnModel(EntityField.id);
		assertEquals(cm, result);
		result = manager.getColumnModel(EntityField.id);
		assertEquals(cm, result);
		result = manager.getColumnModel(EntityField.id);
		assertEquals(cm, result);
		// The column should not be cached.
		verify(mockColumnModelManager, times(3)).createColumnModel(any(ColumnModel.class));
	}
	
	
	@Test
	public void testGetDefaultTableViewColumnsFileView() throws LimitExceededException{
		setupCreateColumn();
		List<ColumnModel> expected = new LinkedList<ColumnModel>();
		for(EntityField field: EntityField.values()){
			expected.add(field.getColumnModel());
		}
		// call under test
		List<ColumnModel> results = manager.getDefaultTableViewColumns(ViewTypeMask.File.getMask());
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetDefaultTableViewColumnsFileAntTableView(){
		setupCreateColumn();
		List<ColumnModel> expected = new LinkedList<ColumnModel>();
		for(EntityField field: EntityField.values()){
			expected.add(field.getColumnModel());
		}
		// call under test
		Long viewTypeMaks = ViewTypeMask.File.getMask() | ViewTypeMask.Table.getMask();
		List<ColumnModel> results = manager.getDefaultTableViewColumns(viewTypeMaks);
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetDefaultTableViewColumnsProjectView(){
		setupCreateColumn();
		List<ColumnModel> expected = Lists.newArrayList(
				EntityField.id.getColumnModel(),
				EntityField.name.getColumnModel(),
				EntityField.createdOn.getColumnModel(),
				EntityField.createdBy.getColumnModel(),
				EntityField.etag.getColumnModel(),
				EntityField.modifiedOn.getColumnModel(),
				EntityField.modifiedBy.getColumnModel()
				);
		// call under test
		List<ColumnModel> results = manager.getDefaultTableViewColumns(ViewTypeMask.Project.getMask());
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetDefaultTableViewColumnsMaskExcludesFiles(){
		setupCreateColumn();
		long typeMask = 0;
		for(ViewTypeMask type: ViewTypeMask.values()) {
			if(type != ViewTypeMask.File) {
				typeMask |= type.getMask();
			}
		}
		List<ColumnModel> expected = Lists.newArrayList(
				EntityField.id.getColumnModel(),
				EntityField.name.getColumnModel(),
				EntityField.createdOn.getColumnModel(),
				EntityField.createdBy.getColumnModel(),
				EntityField.etag.getColumnModel(),
				EntityField.modifiedOn.getColumnModel(),
				EntityField.modifiedBy.getColumnModel()
				);
		// call under test
		List<ColumnModel> results = manager.getDefaultTableViewColumns(typeMask);
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetDefaultTableViewColumnsMaskIncludeFiles(){
		setupCreateColumn();
		long typeMask = 0;
		for(ViewTypeMask type: ViewTypeMask.values()) {
			typeMask |= type.getMask();
		}
		List<ColumnModel> expected = new LinkedList<ColumnModel>();
		for(EntityField field: EntityField.values()){
			expected.add(field.getColumnModel());
		}
		// call under test
		List<ColumnModel> results = manager.getDefaultTableViewColumns(typeMask);
		assertEquals(expected, results);
	}



	
	@Test
	public void testGetDefaultTableViewColumnsNullMask(){
		Long typeMask = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.getDefaultTableViewColumns(typeMask);
		});
	}

	
	@Test
	public void testGetEntityPath(){
		List<Long> expected = Lists.newArrayList(123L, 456L);
		when(mockNodeDao.getEntityPathIds(tableId)).thenReturn(expected);
		// call under test
		List<Long> results = manager.getEntityPath(idAndVersion);
		assertEquals(expected, results);
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
}
