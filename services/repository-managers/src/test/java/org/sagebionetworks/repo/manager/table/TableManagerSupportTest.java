package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.manager.entity.ReplicationMessageManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
	ColumnModelDAO mockColumnModelDao;
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
	ReplicationMessageManager mockReplicationMessageManager;
	
	
	String schemaMD5Hex;
	
	TableManagerSupportImpl manager;
	String tableId;
	Long tableIdLong;
	TableStatus status;
	String etag;
	Set<Long> scope;
	Set<Long> containersInScope;
	UserInfo userInfo;
	Long viewType;
	
	Integer callableReturn;
	
	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		manager = new TableManagerSupportImpl();
		ReflectionTestUtils.setField(manager, "tableStatusDAO", mockTableStatusDAO);
		ReflectionTestUtils.setField(manager, "tableConnectionFactory", mockTableConnectionFactory);
		ReflectionTestUtils.setField(manager, "timeoutUtils", mockTimeoutUtils);
		ReflectionTestUtils.setField(manager, "transactionalMessenger", mockTransactionalMessenger);
		ReflectionTestUtils.setField(manager, "columnModelDao",
				mockColumnModelDao);
		ReflectionTestUtils.setField(manager, "nodeDao", mockNodeDao);
		ReflectionTestUtils.setField(manager, "tableTruthDao", mockTableTruthDao);
		ReflectionTestUtils.setField(manager, "viewScopeDao", mockViewScopeDao);
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(manager, "replicationMessageManager", mockReplicationMessageManager);
		
		callableReturn = 123;
		
		userInfo = new UserInfo(false, 8L);
		
		tableId = "syn123";
		tableIdLong = KeyFactory.stringToKey(tableId);
		viewType = ViewTypeMask.File.getMask();
		
		when(mockTableConnectionFactory.getConnection(tableId)).thenReturn(mockTableIndexDAO);
		
		etag = "";
		
		status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.PROCESSING);
		status.setChangedOn(new Date(123));
		status.setResetToken(etag);
		
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);		
		when(mockTableStatusDAO.resetTableStatusToProcessing(tableId)).thenReturn(etag);
		
		ColumnModel cm = new ColumnModel();
		cm.setId("444");
		List<ColumnModel> columns = Lists.newArrayList(cm);
		when(mockColumnModelDao.getColumnModelsForObject(tableId)).thenReturn(
				columns);
		List<String> columnIds = TableModelUtils.getIds(columns);
		when(mockColumnModelDao.getColumnModelIdsForObject(tableId)).thenReturn(columnIds);
		schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(Lists.newArrayList(cm.getId()));

		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		
		// setup the view scope
		scope = Sets.newHashSet(222L,333L);
		when(mockViewScopeDao.getViewScope(tableIdLong)).thenReturn(scope);
		
		containersInScope = new LinkedHashSet<Long>(Arrays.asList(222L,333L,20L,21L,30L,31L));
		
		when(mockNodeDao.getAllContainerIds(anyListOf(Long.class), anyInt())).thenReturn(containersInScope);
		
		
		// mirror passed columns.
		doAnswer(new Answer<ColumnModel>() {
			@Override
			public ColumnModel answer(InvocationOnMock invocation)
					throws Throwable {
				return (ColumnModel) invocation.getArguments()[0];
			}
		}).when(mockColumnModelDao).createColumnModel(any(ColumnModel.class));
	}
	
	
	/**
	 * For this case the table status is available and the index is synchronized with the truth.
	 * The available status should be returned for this case.
	 * @throws Exception
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsAvailableSynchronized() throws Exception {
		// Available
		status.setState(TableState.AVAILABLE);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(true);
		// not expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		// table exists
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(true);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
		assertNotNull(result);
		verify(mockTableStatusDAO, never()).resetTableStatusToProcessing(tableId);
		verify(mockTransactionalMessenger, never()).sendMessageAfterCommit(tableId, ObjectType.TABLE, etag, ChangeType.UPDATE);
	}
	
	/**
	 * This is a test case for PLFM-3383, PLFM-3379, and PLFM-3762.  In all cases the table 
	 * status was available but the index was not synchronized with the truth.
	 * @throws Exception
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsAvailableNotSynchronized() throws Exception {
		// Available
		status.setState(TableState.AVAILABLE);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// Not synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(false);
		// not expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		// table exists
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(true);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
		assertNotNull(result);
		// must trigger processing
		verify(mockTableStatusDAO).resetTableStatusToProcessing(tableId);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(tableId, ObjectType.TABLE, etag, ChangeType.UPDATE);
	}
	
	/**
	 * This is a case where the table status does not exist but the table exits.
	 * The table must be be set to processing for this case.
	 * @throws Exception
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsStatusNotFoundTableExits() throws Exception {
		// Available
		status.setState(TableState.PROCESSING);
		// Setup a case where the first time the status does not exists, but does exist the second call.
		when(mockTableStatusDAO.getTableStatus(tableId)).thenThrow(new NotFoundException("No status for this table.")).thenReturn(status);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(true);
		// not expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		// table exists
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(true);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
		assertNotNull(result);
		verify(mockTableStatusDAO).resetTableStatusToProcessing(tableId);
		verify(mockNodeDao).isNodeAvailable(tableIdLong);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(tableId, ObjectType.TABLE, etag, ChangeType.UPDATE);
	}
	
	/**
	 * This is a case where the table status does not exist and the table does not exist.
	 * Should result in a NotFoundException
	 * @throws Exception
	 */
	@Test (expected=NotFoundException.class)
	public void testGetTableStatusOrCreateIfNotExistsStatusNotFoundTableDoesNotExist() throws Exception {
		// Available
		status.setState(TableState.PROCESSING);
		// Setup a case where the first time the status does not exists, but does exist the second call.
		when(mockTableStatusDAO.getTableStatus(tableId)).thenThrow(new NotFoundException("No status for this table.")).thenReturn(status);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(true);
		// not expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		// table does not exists
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(false);
		// call under test
		manager.getTableStatusOrCreateIfNotExists(tableId);
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
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(true);
		// not expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		// table exists
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(true);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
		assertNotNull(result);
		verify(mockTableStatusDAO, never()).resetTableStatusToProcessing(tableId);
		verify(mockTransactionalMessenger, never()).sendMessageAfterCommit(tableId, ObjectType.TABLE, etag, ChangeType.UPDATE);
	}
	
	/**
	 * For this case the table is processing and not making progress .
	 * The status must be rest to processing to trigger another try.
	 * @throws Exception
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsProcessingExpired() throws Exception {
		// Available
		status.setState(TableState.PROCESSING);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(true);
		//expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(true);
		// table exists
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(true);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
		assertNotNull(result);
		verify(mockTableStatusDAO).resetTableStatusToProcessing(tableId);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(tableId, ObjectType.TABLE, etag, ChangeType.UPDATE);
	}


	
	@Test
	public void testStartTableProcessing(){
		String token = "a unique token";
		when(mockTableStatusDAO.resetTableStatusToProcessing(tableId)).thenReturn(token);
		// call under test
		String resultToken = manager.startTableProcessing(tableId);
		assertEquals(token, resultToken);
	}
	
	@Test
	public void testIsIndexSynchronizedWithTruth(){
		long currentVersion = 123L;

		TableRowChange lastChange = new TableRowChange();
		lastChange.setRowVersion(currentVersion);
		when(mockTableTruthDao.getLastTableRowChange(tableId)).thenReturn(lastChange);
		// setup a match
		when(mockTableIndexDAO.doesIndexStateMatch(tableId, currentVersion, schemaMD5Hex)).thenReturn(true);
		
		assertTrue(manager.isIndexSynchronizedWithTruth(tableId));
	}
	
	
	/**
	 * The node is available, the table status is available and the index is synchronized.
	 */
	@Test
	public void testIsIndexWorkRequiredFalse(){
		// node exists
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(true);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(true);
		// available
		TableStatus status = new TableStatus();
		status.setState(TableState.AVAILABLE);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// call under test
		boolean workRequired = manager.isIndexWorkRequired(tableId);
		assertFalse(workRequired);
	}
	
	@Test
	public void testDoesTableExistTrue() {
		Long id = 123L;
		String synId = KeyFactory.keyToString(id);
		when(mockNodeDao.doesNodeExist(id)).thenReturn(true);
		// call under test
		assertTrue(manager.doesTableExist(synId));
	}
	
	@Test
	public void testDoesTableExistFalse() {
		Long id = 123L;
		String synId = KeyFactory.keyToString(id);
		when(mockNodeDao.doesNodeExist(id)).thenReturn(false);
		// call under test
		assertFalse(manager.doesTableExist(synId));
	}
	
	/**
	 * The node is missing, the table status is available and the index is synchronized.
	 */
	@Test
	public void testIsIndexWorkRequiredNodeMissing(){
		// node is missing
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(false);
		// not synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(false);
		// failed
		TableStatus status = new TableStatus();
		status.setState(TableState.PROCESSING_FAILED);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// call under test
		boolean workRequired = manager.isIndexWorkRequired(tableId);
		assertFalse(workRequired);
	}
	
	/**
	 * The node is available, the table status is available and the index is not synchronized.
	 * Processing is needed to bring the index up-to-date.
	 */
	@Test
	public void testIsIndexWorkRequiredNotSynched(){
		// node exists
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(true);
		// not synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(false);
		// available
		TableStatus status = new TableStatus();
		status.setState(TableState.AVAILABLE);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// call under test
		boolean workRequired = manager.isIndexWorkRequired(tableId);
		assertTrue(workRequired);
	}
	
	/**
	 * The node is available, the table status is processing and the index is synchronized.
	 * Processing is needed to make the table available.
	 */
	@Test
	public void testIsIndexWorkRequiredStatusProcessing(){
		// node exists
		when(mockNodeDao.isNodeAvailable(tableIdLong)).thenReturn(true);
		// synchronized
		when(mockTableIndexDAO.doesIndexStateMatch(anyString(), anyLong(), anyString())).thenReturn(true);
		// available
		TableStatus status = new TableStatus();
		status.setState(TableState.PROCESSING);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// call under test
		boolean workRequired = manager.isIndexWorkRequired(tableId);
		assertTrue(workRequired);
	}
	
	@Test
	public void testGetSchemaMD5Hex() {
		String md5 = manager.getSchemaMD5Hex(tableId);
		assertEquals(schemaMD5Hex, md5);
	}

	@Test
	public void testGetTableTypeTable() {
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		// call under test
		ObjectType type = manager.getTableType(tableId);
		assertEquals(ObjectType.TABLE, type);
	}

	@Test
	public void testGetTableTypeFileView() {
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(
				EntityType.entityview);
		// call under test
		ObjectType type = manager.getTableType(tableId);
		assertEquals(ObjectType.ENTITY_VIEW, type);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetTableTypeUnknown() {
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(
				EntityType.project);
		// call under test
		manager.getTableType(tableId);
	}
	
	@Test
	public void testGetObjectTypeForEntityType(){
		assertEquals(ObjectType.TABLE, TableManagerSupportImpl.getObjectTypeForEntityType(EntityType.table));
		assertEquals(ObjectType.ENTITY_VIEW, TableManagerSupportImpl.getObjectTypeForEntityType(EntityType.entityview));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetObjectTypeForEntityTypeUnknownType(){
		TableManagerSupportImpl.getObjectTypeForEntityType(EntityType.project);
	}
	
	@Test
	public void testGetVersionOfLastTableChangeNull() throws NotFoundException, IOException{
		// no last version
		when(mockTableTruthDao.getLastTableRowChange(tableId)).thenReturn(null);
		//call under test
		assertEquals(-1, manager.getVersionOfLastTableEntityChange(tableId));
	}
	
	@Test
	public void testGetVersionOfLastTableChange() throws NotFoundException, IOException{
		long currentVersion = 123L;
		TableRowChange lastChange = new TableRowChange();
		lastChange.setRowVersion(currentVersion);
		when(mockTableTruthDao.getLastTableRowChange(tableId)).thenReturn(lastChange);
		// call under test
		assertEquals(currentVersion, manager.getVersionOfLastTableEntityChange(tableId));
	}
	
	
	@Test
	public void testGetAllContainerIdsForViewScope(){
		// call under test.
		Set<Long> containers = manager.getAllContainerIdsForViewScope(tableId, viewType);
		assertEquals(containersInScope, containers);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testgetAllContainerIdsForScopeOverLimit(){
		Set<Long> overLimit = new HashSet<>();
		int countOverLimit = TableManagerSupportImpl.MAX_CONTAINERS_PER_VIEW+1;
		for(long i=0; i<countOverLimit; i++){
			overLimit.add(i);
		}
		viewType = ViewTypeMask.File.getMask();
		// call under test.
		manager.getAllContainerIdsForScope(overLimit, viewType);
	}
	
	@Test
	public void testgetAllContainerIdsForScopeFiewView() throws LimitExceededException{
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
	@Test (expected=IllegalArgumentException.class)
	public void testGetAllContainerIdsForScopeOverLimit(){
		Set<Long> tooMany = new HashSet<Long>();
		for(long i=0; i<TableManagerSupportImpl.MAX_CONTAINERS_PER_VIEW+1; i++){
			tooMany.add(i);
		}
		// call under test
		manager.getAllContainerIdsForScope(tooMany, viewType);
	}
	
	/**
	 * For this case the scope is under the limit, but the expanded containers
	 * would go over the limit.
	 * @throws LimitExceededException 
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testGetAllContainerIdsForScopeExpandedOverLimit() throws LimitExceededException{
		// setup limit exceeded.
		LimitExceededException exception = new LimitExceededException("too many");
		doThrow(exception).when(mockNodeDao).getAllContainerIds(anyListOf(Long.class), anyInt());
		// call under test
		manager.getAllContainerIdsForScope(scope, viewType);
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
	public void calculateFileViewCRC32(){
		Long crc32 = 45678L;
		Long type = ViewTypeMask.File.getMask();
		when(mockViewScopeDao.getViewTypeMask(tableIdLong)).thenReturn(type);
		when(mockTableIndexDAO.calculateCRC32ofEntityReplicationScope(type, containersInScope)).thenReturn(crc32);
		List<Long> toReconcile = new LinkedList<Long>(containersInScope);
		Long crcResult = manager.calculateViewCRC32(tableId);
		assertEquals(crc32, crcResult);
		verify(mockReplicationMessageManager).pushContainerIdsToReconciliationQueue(toReconcile);
	}
	
	@Test
	public void testTriggerScopeReconciliationFileView(){
		Long type = ViewTypeMask.File.getMask();
		List<Long> toReconcile = new LinkedList<Long>(containersInScope);
		// call under test
		manager.triggerScopeReconciliation(type, containersInScope);
		// the scope should be sent
		verify(mockReplicationMessageManager).pushContainerIdsToReconciliationQueue(toReconcile);
	}
	
	@Test
	public void testTriggerScopeReconciliationProjectView(){
		Long type = ViewTypeMask.Project.getMask();
		Long rootId = KeyFactory.stringToKey(StackConfigurationSingleton.singleton().getRootFolderEntityId());
		// project views reconcile on root.
		List<Long> toReconcile = Lists.newArrayList(rootId);
		// call under test
		manager.triggerScopeReconciliation(type, containersInScope);
		// the scope should be sent
		verify(mockReplicationMessageManager).pushContainerIdsToReconciliationQueue(toReconcile);
	}
	
	@Test
	public void testGetTableVersionForTableEntity() {
		TableRowChange lastChange = new TableRowChange();
		lastChange.setRowVersion(999L);
		when(mockTableTruthDao.getLastTableRowChange(tableId)).thenReturn(
				lastChange);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		// call under test
		Long version = manager.getTableVersion(tableId);
		assertEquals(lastChange.getRowVersion(), version);
	}
	
	@Test
	public void testGetTableVersionForFileView() {
		Long crc32 = 45678L;
		Long type = ViewTypeMask.File.getMask();
		when(mockViewScopeDao.getViewTypeMask(tableIdLong)).thenReturn(type);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.entityview);
		when(mockTableIndexDAO.calculateCRC32ofEntityReplicationScope(type, containersInScope)).thenReturn(crc32);
		// call under test
		Long version = manager.getTableVersion(tableId);
		assertEquals(crc32, version);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetTableVersionForUnknown() {
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.folder);
		// call under test
		manager.getTableVersion(tableId);
	}
	
	@Test
	public void testValidateTableReadAccessTableEntity(){
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(new AuthorizationStatus(true, ""));
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(new AuthorizationStatus(true, ""));
		//  call under test
		EntityType type = manager.validateTableReadAccess(userInfo, tableId);
		assertEquals(EntityType.table, type);
		verify(mockAuthorizationManager).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockAuthorizationManager).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testValidateTableReadAccessTableEntityNoDownload(){
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(new AuthorizationStatus(true, ""));
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(new AuthorizationStatus(false, ""));
		//  call under test
		manager.validateTableReadAccess(userInfo, tableId);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testValidateTableReadAccessTableEntityNoRead(){
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(new AuthorizationStatus(false, ""));
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(new AuthorizationStatus(true, ""));
		//  call under test
		manager.validateTableReadAccess(userInfo, tableId);
	}
	
	@Test
	public void testValidateTableReadAccessFileView(){
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.entityview);
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(new AuthorizationStatus(true, ""));
		//  call under test
		manager.validateTableReadAccess(userInfo, tableId);
		verify(mockAuthorizationManager).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		//  do not need download for FileView
		verify(mockAuthorizationManager, never()).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testValidateTableReadAccessFileViewNoRead(){
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.entityview);
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(new AuthorizationStatus(false, ""));
		//  call under test
		manager.validateTableReadAccess(userInfo, tableId);
	}
	
	@Test
	public void testValidateTableWriteAccessTableEntity(){
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(new AuthorizationStatus(true, ""));
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPLOAD)).thenReturn(new AuthorizationStatus(true, ""));
		//  call under test
		manager.validateTableWriteAccess(userInfo, tableId);
		verify(mockAuthorizationManager).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
		verify(mockAuthorizationManager).canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPLOAD);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testValidateTableWriteAccessTableEntityNoUpload(){
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(new AuthorizationStatus(true, ""));
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPLOAD)).thenReturn(new AuthorizationStatus(false, ""));
		//  call under test
		manager.validateTableWriteAccess(userInfo, tableId);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testValidateTableWriteAccessTableEntityNoUpdate(){
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(new AuthorizationStatus(false, ""));
		when(mockAuthorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPLOAD)).thenReturn(new AuthorizationStatus(true, ""));
		//  call under test
		manager.validateTableWriteAccess(userInfo, tableId);
	}
	
	@Test
	public void testGetColumModelCached(){
		ColumnModel cm = new ColumnModel();
		cm.setId("123");
		when(mockColumnModelDao.createColumnModel(any(ColumnModel.class))).thenReturn(cm);
		ColumnModel result = manager.getColumnModel(EntityField.id);
		assertEquals(cm, result);
		result = manager.getColumnModel(EntityField.id);
		assertEquals(cm, result);
		result = manager.getColumnModel(EntityField.id);
		assertEquals(cm, result);
		// The first call should cache the column so create should only be called once.
		verify(mockColumnModelDao, times(1)).createColumnModel(any(ColumnModel.class));
	}
	
	
	@Test
	public void testGetDefaultTableViewColumnsFileView(){
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
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetDefaultTableViewColumnsNullMask(){
		Long typeMask = null;
		// call under test
		manager.getDefaultTableViewColumns(typeMask);
	}

	
	@Test
	public void testGetEntityPath(){
		EntityHeader one = new EntityHeader();
		one.setId("syn123");
		EntityHeader two = new EntityHeader();
		two.setId("syn456");
		when(mockNodeDao.getEntityPath(tableId)).thenReturn(Lists.newArrayList(one, two));
		Set<Long> expected = Sets.newHashSet(123L, 456L);
		// call under test
		Set<Long> results = manager.getEntityPath(tableId);
		assertEquals(expected, results);
	}

	@Test (expected = UnauthorizedException.class)
	public void testRebuildTableUnauthorized() throws Exception {
		manager.rebuildTable(userInfo, tableId);
	}

	@Test
	public void testRebuildTableAuthorizedForTableEntity() throws Exception {
		UserInfo mockAdmin = Mockito.mock(UserInfo.class);
		when(mockAdmin.isAdmin()).thenReturn(true);
		manager.rebuildTable(mockAdmin, tableId);
		verify(mockTableIndexDAO).deleteTable(tableId);
		verify(mockTableIndexDAO).deleteSecondaryTables(tableId);
		verify(mockTableStatusDAO).resetTableStatusToProcessing(tableId);
		ArgumentCaptor<ChangeMessage> captor = ArgumentCaptor.forClass(ChangeMessage.class);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(captor.capture());
		ChangeMessage message = captor.getValue();
		assertEquals(message.getObjectId(), tableIdLong+"");
		assertEquals(ObjectType.TABLE, message.getObjectType());
		assertEquals(etag, message.getObjectEtag());
		assertEquals(ChangeType.UPDATE, message.getChangeType());
	}

	@Test
	public void testRebuildTableAuthorizedForFileView() throws Exception {
		UserInfo mockAdmin = Mockito.mock(UserInfo.class);
		when(mockAdmin.isAdmin()).thenReturn(true);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.entityview);
		manager.rebuildTable(mockAdmin, tableId);
		verify(mockTableIndexDAO).deleteTable(tableId);
		verify(mockTableIndexDAO).deleteSecondaryTables(tableId);
		verify(mockTableStatusDAO).resetTableStatusToProcessing(tableId);
		ArgumentCaptor<ChangeMessage> captor = ArgumentCaptor.forClass(ChangeMessage.class);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(captor.capture());
		ChangeMessage message = captor.getValue();
		assertEquals(message.getObjectId(), tableIdLong+"");
		assertEquals(ObjectType.ENTITY_VIEW, message.getObjectType());
		assertEquals(etag, message.getObjectEtag());
		assertEquals(ChangeType.UPDATE, message.getChangeType());
	}
	
}
