package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.TableIndexDAOImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class TableStatusDAOImplTest {

	@Autowired
	TableStatusDAO tableStatusDAO;
	
	IdAndVersion tableIdNoVersion;
	IdAndVersion tableIdWithVersion;
	
	@Before
	public void before(){
		tableStatusDAO.clearAllTableState();
		
		tableIdNoVersion = IdAndVersion.parse("syn123");
		tableIdWithVersion = IdAndVersion.parse("syn123.456");
	}
	
	@After
	public void after(){
		tableStatusDAO.clearAllTableState();
	}
	
	@Test
	public void testResetTableStatusToPending() throws NotFoundException{
		// Before we start the status should not exist
		try{
			tableStatusDAO.getTableStatus(tableIdNoVersion);
			fail("The status for this table should not exist yet");
		}catch(NotFoundException e){
			// expected
		}
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		assertNotNull(resetToken);
		// We should now have a status for this table
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		assertNotNull(status.getChangedOn());
		assertNotNull(status.getStartedOn());
		// The rest should be null
		assertEquals(null, status.getErrorDetails());
		assertEquals(null, status.getErrorMessage());
		assertEquals(null, status.getProgressCurrent());
		assertEquals(null, status.getProgressTotal());
		assertEquals(null, status.getTotalTimeMS());
		assertEquals(null, status.getVersion());
		// Now if we call it again we should get a new rest-token
		String newResetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		assertNotNull(newResetToken);
		assertFalse(newResetToken.equals(resetToken));
	}
	
	@Test
	public void testResetTableStatusToPendingNoBroadcast() throws NotFoundException{
		// Before we start the status should not exist
		try{
			tableStatusDAO.getTableStatus(tableIdNoVersion);
			fail("The status for this table should not exist yet");
		}catch(NotFoundException e){
			// expected
		}
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		assertNotNull(resetToken);
		// We should now have a status for this table
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		
	}
	
	@Test
	public void testAttemptToSetTableStatusToAvailableHappy() throws NotFoundException{
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		assertNotNull(status.getChangedOn());
		String lastTableChangeEtag = UUID.randomUUID().toString();
		// Not make available
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, resetToken, lastTableChangeEtag);
		// the state should have changed
		status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.AVAILABLE, status.getState());
		assertNotNull(status.getTotalTimeMS());
		assertEquals(lastTableChangeEtag, status.getLastTableChangeEtag());
	}
	
	@Test
	public void testDeleteTableStatus() throws NotFoundException {
		IdAndVersion tableId1 = IdAndVersion.parse("syn1");
		IdAndVersion tableId2 = IdAndVersion.parse("syn2");
		// This should insert a row for this table.
		tableStatusDAO.resetTableStatusToProcessing(tableId1);
		tableStatusDAO.resetTableStatusToProcessing(tableId2);
		TableStatus status = tableStatusDAO.getTableStatus(tableId1);
		assertNotNull(status);
		status = tableStatusDAO.getTableStatus(tableId2);
		assertNotNull(status);

		tableStatusDAO.deleteTableStatus(tableId2);
		status = tableStatusDAO.getTableStatus(tableId1);
		assertNotNull(status);
		try {
			tableStatusDAO.getTableStatus(tableId2);
			fail("Should have been deleted");
		} catch (NotFoundException e) {
		}
	}

	/**
	 * This is a test for PLFM-2634 and PLFM-2636
	 * @throws NotFoundException
	 */
	@Test
	public void testAttemptToSetTableStatusToAvailableNullEtag() throws NotFoundException{
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		assertNotNull(status.getChangedOn());
		// Not make available
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, resetToken, null);
		// the state should have changed
		status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.AVAILABLE, status.getState());
		assertNotNull(status.getTotalTimeMS());
		assertEquals(null, status.getLastTableChangeEtag());
	}
	
	@Test (expected=NotFoundException.class)
	public void testAttemptToSetTableStatusNotFound() throws NotFoundException{
		// Not make available
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, "fake token", UUID.randomUUID().toString());
	}
	
	@Test (expected=ConflictingUpdateException.class)
	public void testAttemptToSetTableStatusToAvailableConflict() throws NotFoundException{
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		// This should fail since the passed token does not match the current token
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, resetToken+"invalidated", UUID.randomUUID().toString());
	}
	
	@Test
	public void testAttemptToSetTableStatusToFailedHappy() throws NotFoundException{
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		assertNotNull(status.getChangedOn());
		// Not make available
		tableStatusDAO.attemptToSetTableStatusToFailed(tableIdNoVersion, resetToken, "error", "error details");
		// the state should have changed
		status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING_FAILED, status.getState());
		assertNotNull(status.getTotalTimeMS());
		assertEquals("error", status.getErrorMessage());
		assertEquals("error details", status.getErrorDetails());
		assertEquals(null, status.getLastTableChangeEtag());
	}
	
	@Test (expected=NotFoundException.class)
	public void testattemptToSetTableStatusToFailedNotFound() throws NotFoundException{
		// Not make available
		tableStatusDAO.attemptToSetTableStatusToFailed(tableIdNoVersion, "fake token", "error", "error details");
	}
	
	@Test (expected=ConflictingUpdateException.class)
	public void testAttemptToSetTableStatusToFailedConflict() throws NotFoundException{
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		// This should fail since the passed token does not match the current token
		tableStatusDAO.attemptToSetTableStatusToFailed(tableIdNoVersion, resetToken+"invalidated", "error", "error details");
	}
	
	/**
	 * Part of the issue with PLFM-5632 was the error message was too large for the status table.
	 */
	@Test
	public void testAttemptToSetTableStatusToFailedMessageAtLimit() {
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		assertNotNull(status.getChangedOn());
		String errorMessage = StringUtils.repeat("a", TableStatusDAOImpl.MAX_ERROR_MESSAGE_CHARS);
		// Not make available
		tableStatusDAO.attemptToSetTableStatusToFailed(tableIdNoVersion, resetToken, errorMessage, "error details");
		// the state should have changed
		status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertEquals(errorMessage, status.getErrorMessage());
	}
	
	/**
	 * Part of the issue with PLFM-5632 was the error message was too large for the status table.
	 */
	@Test
	public void testAttemptToSetTableStatusToFailedMessageOverLimit() {
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		assertNotNull(status.getChangedOn());
		String errorMessage = StringUtils.repeat("a", TableStatusDAOImpl.MAX_ERROR_MESSAGE_CHARS+1);
		// Not make available
		tableStatusDAO.attemptToSetTableStatusToFailed(tableIdNoVersion, resetToken, errorMessage, "error details");
		// the state should have changed
		status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertTrue(TableStatusDAOImpl.MAX_ERROR_MESSAGE_CHARS <= status.getErrorMessage().length());
	}
	
	@Test
	public void testAttemptToUpdateTableProgressHappy() throws NotFoundException{
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		assertNotNull(status.getChangedOn());
		// Not make available
		tableStatusDAO.attemptToUpdateTableProgress(tableIdNoVersion, resetToken, "message", 0l, 100L);
		// the state should have changed
		status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		assertNotNull(status.getTotalTimeMS());
		assertEquals("message", status.getProgressMessage());
		assertEquals(new Long(0), status.getProgressCurrent());
		assertEquals(new Long(100), status.getProgressTotal());
	}
	
	@Test (expected=NotFoundException.class)
	public void testAttemptToUpdateTableProgressNotFound() throws ConflictingUpdateException, NotFoundException{
		// Not make available
		tableStatusDAO.attemptToUpdateTableProgress(tableIdNoVersion,"fakeToken", "message", 0L, 100L);
	}
	
	@Test (expected=ConflictingUpdateException.class)
	public void testAttemptToUpdateTableProgressConflict() throws NotFoundException{
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		// This should fail since the passed token does not match the current token
		tableStatusDAO.attemptToUpdateTableProgress(tableIdNoVersion, resetToken+"invalidated", "message", 1L, 100L);
	}
	
	@Test (expected=NotFoundException.class)
	public void testNotFound() throws NotFoundException{
		tableStatusDAO.getTableStatus(IdAndVersion.newBuilder().setId(-99L).build());
	}
	
	@Test
	public void testValidateAndGetVersionWithVersion() {
		// call under test
		Long version = TableStatusDAOImpl.validateAndGetVersion(tableIdWithVersion);
		assertEquals(tableIdWithVersion.getVersion().get(), version);
	}
	
	@Test
	public void testValidateAndGetVersionWithoutVersion() {
		// call under test
		long version = TableStatusDAOImpl.validateAndGetVersion(tableIdNoVersion);
		assertEquals(TableStatusDAOImpl.NULL_VERSION, version);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAndGetVersionNull() {
		// call under test
		TableStatusDAOImpl.validateAndGetVersion(null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateAndGetVersionIdNull() {
		// call under test
		TableStatusDAOImpl.validateAndGetVersion(IdAndVersion.newBuilder().setId(null).build());
	}
	
	@Test
	public void testWithVersion() {
		// Before we start the status should not exist
		try{
			tableStatusDAO.getTableStatus(tableIdWithVersion);
			fail("The status for this table should not exist yet");
		}catch(NotFoundException e){
			// expected
		}
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion);
		assertNotNull(resetToken);
		// We should now have a status for this table
		TableStatus status = tableStatusDAO.getTableStatus(tableIdWithVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		assertNotNull(status.getChangedOn());
		assertNotNull(status.getStartedOn());
		// The rest should be null
		assertEquals(null, status.getErrorDetails());
		assertEquals(null, status.getErrorMessage());
		assertEquals(null, status.getProgressCurrent());
		assertEquals(null, status.getProgressTotal());
		assertEquals(null, status.getTotalTimeMS());
		assertEquals(tableIdWithVersion.getVersion().get(), status.getVersion());
		// Now if we call it again we should get a new rest-token
		String newResetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion);
		assertNotNull(newResetToken);
		assertFalse(newResetToken.equals(resetToken));
	}
	
	@Test
	public void testWithAndWithoutVersion() {
		// This should insert a row for this table.
		tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion);
		tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
	
		TableStatus withVersion = tableStatusDAO.getTableStatus(tableIdWithVersion);
		assertNotNull(withVersion);
		assertEquals(tableIdWithVersion.getVersion().get(), withVersion.getVersion());
		TableStatus noVersion = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(noVersion);
		assertEquals(null, noVersion.getVersion());
		assertEquals("Both should have the same ID", withVersion.getTableId(), noVersion.getTableId());
	}
}
