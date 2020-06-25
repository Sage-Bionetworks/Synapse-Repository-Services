package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class TableStatusDAOImplTest {

	@Autowired
	TableStatusDAO tableStatusDAO;
	
	IdAndVersion tableIdNoVersion;
	IdAndVersion tableIdWithVersion;
	
	@BeforeEach
	public void before(){
		tableStatusDAO.clearAllTableState();
		
		tableIdNoVersion = IdAndVersion.parse("syn123");
		tableIdWithVersion = IdAndVersion.parse("syn123.456");
	}
	
	@AfterEach
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
	
	@Test
	public void testAttemptToSetTableStatusNotFound() throws NotFoundException{
		assertThrows(NotFoundException.class, ()->{
			// Not make available
			tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, "fake token", UUID.randomUUID().toString());
		});
	}
	
	@Test
	public void testAttemptToSetTableStatusToAvailableConflict() throws NotFoundException{
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertThrows(InvalidStatusTokenException.class, ()->{
			// This should fail since the passed token does not match the current token
			tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, resetToken+"invalidated", UUID.randomUUID().toString());
		});
	}
	
	@Test
	public void testAttemptToSetTableStatusToAvailableMultilpeTimes() throws NotFoundException{
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		// This should pass
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, resetToken, UUID.randomUUID().toString());
		assertThrows(InvalidStatusTokenException.class, ()->{
			// A second time should fail
			tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, resetToken, UUID.randomUUID().toString());
		});
	}
	
	@Test
	public void testAttemptToSetTableStatusToAvailableNullReset() throws NotFoundException{
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertThrows(IllegalArgumentException.class, ()->{
			String nullToken = null;
			tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, nullToken, UUID.randomUUID().toString());
		});
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
		tableStatusDAO.attemptToSetTableStatusToFailed(tableIdNoVersion, "error", "error details");
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
	
	@Test
	public void testattemptToSetTableStatusToFailedNotFound() throws NotFoundException{
		assertThrows(NotFoundException.class, ()->{
			// Not make available
			tableStatusDAO.attemptToSetTableStatusToFailed(tableIdNoVersion, "error", "error details");
		});
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
		tableStatusDAO.attemptToSetTableStatusToFailed(tableIdNoVersion, errorMessage, "error details");
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
		tableStatusDAO.attemptToSetTableStatusToFailed(tableIdNoVersion, errorMessage, "error details");
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
	
	@Test
	public void testAttemptToUpdateTableProgressNotFound() throws NotFoundException{
		assertThrows(NotFoundException.class, ()->{
			// Not make available
			tableStatusDAO.attemptToUpdateTableProgress(tableIdNoVersion,"fakeToken", "message", 0L, 100L);
		});
	}
	
	@Test
	public void testAttemptToUpdateTableProgressConflict() throws NotFoundException{
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);

		assertThrows(InvalidStatusTokenException.class, ()->{
			// This should fail since the passed token does not match the current token
			tableStatusDAO.attemptToUpdateTableProgress(tableIdNoVersion, resetToken+"invalidated", "message", 1L, 100L);
		});
	}
	
	@Test
	public void testNotFound() throws NotFoundException{
		assertThrows(NotFoundException.class, ()->{
			tableStatusDAO.getTableStatus(IdAndVersion.newBuilder().setId(-99L).build());
		});
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
	
	@Test
	public void testValidateAndGetVersionNull() {
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			TableStatusDAOImpl.validateAndGetVersion(null);
		});
	}

	@Test
	public void testValidateAndGetVersionIdNull() {
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			TableStatusDAOImpl.validateAndGetVersion(IdAndVersion.newBuilder().setId(null).build());
		});
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
		assertEquals(withVersion.getTableId(), noVersion.getTableId(),"Both should have the same ID");
	}
	
	@Test
	public void testGetTableStatusStateWithVersion() {
		// This should insert a row for this table.
		tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion);
		// call under test
		Optional<TableState> optional = tableStatusDAO.getTableStatusState(tableIdWithVersion);
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		assertEquals(TableState.PROCESSING, optional.get());
	}
	
	@Test
	public void testGetTableStatusStateWithNoVersion() {
		// This should insert a row for this table.
		tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		// call under test
		Optional<TableState> optional = tableStatusDAO.getTableStatusState(tableIdNoVersion);
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		assertEquals(TableState.PROCESSING, optional.get());
	}
	
	@Test
	public void testGetTableStatusStateDoesNotExist() {
		IdAndVersion doesNotExist = IdAndVersion.parse("syn999.888");
		// call under test
		Optional<TableState> optional = tableStatusDAO.getTableStatusState(doesNotExist);
		assertNotNull(optional);
		assertFalse(optional.isPresent());
	}
	
	@Test
	public void testGetLastChangedOn_NoVersion() {
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		String lastTableChangeEtag = UUID.randomUUID().toString();
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, resetToken, lastTableChangeEtag);
		TableStatus lastStatus = tableStatusDAO.getTableStatus(tableIdNoVersion);
		// call under test
		Date lastChangedOn = tableStatusDAO.getLastChangedOn(tableIdNoVersion);
		assertEquals(lastStatus.getChangedOn(), lastChangedOn);
	}
	
	@Test
	public void testGetLastChangedOn_WithVersion() {
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion);
		String lastTableChangeEtag = UUID.randomUUID().toString();
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdWithVersion, resetToken, lastTableChangeEtag);
		TableStatus lastStatus = tableStatusDAO.getTableStatus(tableIdWithVersion);
		// call under test
		Date lastChangedOn = tableStatusDAO.getLastChangedOn(tableIdWithVersion);
		assertEquals(lastStatus.getChangedOn(), lastChangedOn);
	}
	
	@Test
	public void testGetLastChangedOn_NotFound() {
		IdAndVersion doesNotExist = IdAndVersion.parse("syn999.888");
		assertThrows(NotFoundException.class, ()->{
			// call under test
			tableStatusDAO.getLastChangedOn(doesNotExist);
		});
	}
	
	@Test
	public void testUpdateChangedOnIfAvailable_NoVersion() throws InterruptedException {
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		String lastTableChangeEtag = UUID.randomUUID().toString();
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, resetToken, lastTableChangeEtag);
		Date startingChangedOn = tableStatusDAO.getLastChangedOn(tableIdNoVersion);
		// Sleep to update time
		Thread.sleep(101L);
		
		// call under test
		boolean updated = tableStatusDAO.updateChangedOnIfAvailable(tableIdNoVersion);
		assertTrue(updated);
		Date endingChangedOne = tableStatusDAO.getLastChangedOn(tableIdNoVersion);
		assertTrue(endingChangedOne.after(startingChangedOn));
	}
	
	@Test
	public void testUpdateChangedOnIfAvailable_WithVersion() throws InterruptedException {
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion);
		String lastTableChangeEtag = UUID.randomUUID().toString();
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdWithVersion, resetToken, lastTableChangeEtag);
		Date startingChangedOn = tableStatusDAO.getLastChangedOn(tableIdWithVersion);
		// Sleep to update time
		Thread.sleep(101L);
		
		// call under test
		boolean updated = tableStatusDAO.updateChangedOnIfAvailable(tableIdWithVersion);
		assertTrue(updated);
		Date endingChangedOne = tableStatusDAO.getLastChangedOn(tableIdWithVersion);
		assertTrue(endingChangedOne.after(startingChangedOn));
	}
	
	@Test
	public void testUpdateChangedOnIfAvailable_Processing() throws InterruptedException {
		tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion);
		Date startingChangedOn = tableStatusDAO.getLastChangedOn(tableIdWithVersion);
		// Sleep to update time
		Thread.sleep(101L);
		
		// call under test
		boolean updated = tableStatusDAO.updateChangedOnIfAvailable(tableIdWithVersion);
		assertFalse(updated);
		Date endingChangedOne = tableStatusDAO.getLastChangedOn(tableIdWithVersion);
		assertEquals(endingChangedOne, startingChangedOn);
	}
	
	@Test
	public void testUpdateChangedOnIfAvailable_Failed() throws InterruptedException {
		tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion);
		tableStatusDAO.attemptToSetTableStatusToFailed(tableIdWithVersion, "error", "details");
		Date startingChangedOn = tableStatusDAO.getLastChangedOn(tableIdWithVersion);
		// Sleep to update time
		Thread.sleep(101L);
		
		// call under test
		boolean updated = tableStatusDAO.updateChangedOnIfAvailable(tableIdWithVersion);
		assertFalse(updated);
		Date endingChangedOne = tableStatusDAO.getLastChangedOn(tableIdWithVersion);
		assertEquals(endingChangedOne, startingChangedOn);
	}
	
	@Test
	public void testGetLastChangeEtagWithNoEtag() throws InterruptedException {
		tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion);
		// no etag set for this case
		// call under test
		Optional<String> optional = tableStatusDAO.getLastChangeEtag(tableIdWithVersion);
		assertNotNull(optional);
		assertFalse(optional.isPresent());
	}
	
	@Test
	public void testGetLastChangeEtagWithVersion() throws InterruptedException {
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion);
		String lastTableChangeEtag = UUID.randomUUID().toString();
		// set the etag
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdWithVersion, resetToken, lastTableChangeEtag);
		// call under test
		Optional<String> optional = tableStatusDAO.getLastChangeEtag(tableIdWithVersion);
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		assertEquals(lastTableChangeEtag, optional.get());
	}
	
	@Test
	public void testGetLastChangeEtagWithVersionWithDoesNotExist() throws InterruptedException {
		// call under test
		Optional<String> optional = tableStatusDAO.getLastChangeEtag(tableIdWithVersion);
		assertNotNull(optional);
		assertFalse(optional.isPresent());
	}
	
	@Test
	public void testGetLastChangeEtagWithNoVersion() throws InterruptedException {
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion);
		String lastTableChangeEtag = UUID.randomUUID().toString();
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, resetToken, lastTableChangeEtag);
		// call under test
		Optional<String> optional = tableStatusDAO.getLastChangeEtag(tableIdNoVersion);
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		assertEquals(lastTableChangeEtag, optional.get());
	}
	
	@Test
	public void testGetLastChangeEtagWithVersionWithNoVersion() throws InterruptedException {
		// call under test
		Optional<String> optional = tableStatusDAO.getLastChangeEtag(tableIdNoVersion);
		assertNotNull(optional);
		assertFalse(optional.isPresent());
	}
	
	@Test
	public void testGetLastChangeEtagWithNull() {
		assertThrows(IllegalArgumentException.class, ()->{
			tableStatusDAO.getLastChangeEtag(null);
		});
	}
}
