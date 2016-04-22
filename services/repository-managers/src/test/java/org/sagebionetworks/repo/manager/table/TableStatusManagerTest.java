package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.TimeoutUtils;
import org.springframework.test.util.ReflectionTestUtils;

public class TableStatusManagerTest {

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
	TableTruthManager mockTableTruthManager;
	
	String schemaMD5Hex;
	
	TableStatusManagerImpl manager;
	String tableId;
	TableStatus status;
	String etag;
	
	@Before
	public void before() throws Exception {
		Assume.assumeTrue(StackConfiguration.singleton().getTableEnabled());
		MockitoAnnotations.initMocks(this);
		
		manager = new TableStatusManagerImpl();
		ReflectionTestUtils.setField(manager, "tableStatusDAO", mockTableStatusDAO);
		ReflectionTestUtils.setField(manager, "tableConnectionFactory", mockTableConnectionFactory);
		ReflectionTestUtils.setField(manager, "timeoutUtils", mockTimeoutUtils);
		ReflectionTestUtils.setField(manager, "transactionalMessenger", mockTransactionalMessenger);
		ReflectionTestUtils.setField(manager, "tableTruthManager", mockTableTruthManager);
		
		schemaMD5Hex = "md5Hex";
		tableId = "syn123";
		
		when(mockTableTruthManager.getSchemaMD5Hex(tableId)).thenReturn(schemaMD5Hex);
		
		when(mockTableConnectionFactory.getConnection(tableId)).thenReturn(mockTableIndexDAO);
		
		etag = "";
		
		status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.PROCESSING);
		status.setChangedOn(new Date(123));
		status.setResetToken(etag);
		
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		
		when(mockTableTruthManager.isTableAvailable(anyString())).thenReturn(true);
		
		when(mockTableTruthManager.getTableType(tableId)).thenReturn(ObjectType.TABLE);
		
		when(mockTableStatusDAO.resetTableStatusToProcessing(tableId)).thenReturn(etag);
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
		when(mockTableTruthManager.isTableAvailable(tableId)).thenReturn(true);
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
		when(mockTableTruthManager.isTableAvailable(tableId)).thenReturn(true);
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
		when(mockTableTruthManager.isTableAvailable(tableId)).thenReturn(true);
		// call under test
		TableStatus result = manager.getTableStatusOrCreateIfNotExists(tableId);
		assertNotNull(result);
		verify(mockTableStatusDAO).resetTableStatusToProcessing(tableId);
		verify(mockTableTruthManager).isTableAvailable(tableId);
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
		when(mockTableTruthManager.isTableAvailable(tableId)).thenReturn(false);
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
		when(mockTableTruthManager.isTableAvailable(tableId)).thenReturn(true);
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
		when(mockTableTruthManager.isTableAvailable(tableId)).thenReturn(true);
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
		// setup a mismatch
		when(mockTableIndexDAO.doesIndexStateMatch(tableId, currentVersion, schemaMD5Hex)).thenReturn(true);
		when(mockTableTruthManager.getTableVersion(tableId)).thenReturn(currentVersion);
		
		assertTrue(manager.isIndexSynchronizedWithTruth(tableId));
	}
	
	
	/**
	 * The node is available, the table status is available and the index is synchronized.
	 */
	@Test
	public void testIsIndexWorkRequiredFalse(){
		// node exists
		when(mockTableTruthManager.isTableAvailable(tableId)).thenReturn(true);
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
	
	/**
	 * The node is missing, the table status is available and the index is synchronized.
	 */
	@Test
	public void testIsIndexWorkRequiredNodeMissing(){
		// node is missing
		when(mockTableTruthManager.isTableAvailable(tableId)).thenReturn(false);
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
		when(mockTableTruthManager.isTableAvailable(tableId)).thenReturn(true);
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
		when(mockTableTruthManager.isTableAvailable(tableId)).thenReturn(true);
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
	public void testValidateTableIsAvailableWithStateAvailable() throws NotFoundException, TableUnavilableException, TableFailedException{
		status.setState(TableState.AVAILABLE);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// call under test
		TableStatus resultsStatus = manager.validateTableIsAvailable(tableId);
		assertNotNull(resultsStatus);
		assertEquals(status, resultsStatus);
	}
	
	@Test (expected=TableUnavilableException.class)
	public void testValidateTableIsAvailableWithStateProcessing() throws NotFoundException, TableUnavilableException, TableFailedException{
		status.setState(TableState.PROCESSING);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// call under test
		manager.validateTableIsAvailable(tableId);
	}
	
	@Test (expected=TableFailedException.class)
	public void testValidateTableIsAvailableWithStateFailed() throws NotFoundException, TableUnavilableException, TableFailedException{
		status.setState(TableState.PROCESSING_FAILED);
		when(mockTableStatusDAO.getTableStatus(tableId)).thenReturn(status);
		// call under test
		manager.validateTableIsAvailable(tableId);
	}
}
