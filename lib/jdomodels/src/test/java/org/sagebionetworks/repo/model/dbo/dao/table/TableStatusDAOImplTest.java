package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.IdVersionTableType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.entity.IdAndVersionBuilder;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.ColumnModelHelper;
import org.sagebionetworks.repo.model.helper.NodeDaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
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
	private TableStatusDAOImpl tableStatusDAO;

	@Autowired
	private NodeDaoObjectHelper nodeDaoObjectHelper;
	@Autowired
	private AccessControlListObjectHelper aclObjectHelper;

	@Autowired
	private ColumnModelHelper columnModelHeler;

	IdAndVersion tableIdNoVersion;
	IdAndVersion tableIdWithVersion;
	
	private boolean isResetToken;

	@BeforeEach
	public void before() {
		tableStatusDAO.clearAllTableState();

		tableIdNoVersion = IdAndVersion.parse("syn123");
		tableIdWithVersion = IdAndVersion.parse("syn123.456");
		
		isResetToken = true;
	}

	@AfterEach
	public void after() {
		nodeDaoObjectHelper.truncateAll();
		tableStatusDAO.clearAllTableState();
	}

	@Test
	public void testResetTableStatusToPending() throws NotFoundException {
		// Before we start the status should not exist
		try {
			tableStatusDAO.getTableStatus(tableIdNoVersion);
			fail("The status for this table should not exist yet");
		} catch (NotFoundException e) {
			// expected
		}
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
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
		String newResetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
		assertNotNull(newResetToken);
		assertFalse(newResetToken.equals(resetToken));
	}
	
	@Test
	public void testResetTableStatusToPendingWithResetTokenFalse() throws NotFoundException {
		// Before we start the status should not exist
		try {
			tableStatusDAO.getTableStatus(tableIdNoVersion);
			fail("The status for this table should not exist yet");
		} catch (NotFoundException e) {
			// expected
		}
		
		isResetToken = false;
		
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
		
		assertNotNull(resetToken);
		// We should now have a status for this table
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		assertNotNull(status.getChangedOn());
		assertNotNull(status.getStartedOn());
		assertEquals(resetToken, status.getResetToken());
		
		// The rest should be null
		assertEquals(null, status.getErrorDetails());
		assertEquals(null, status.getErrorMessage());
		assertEquals(null, status.getProgressCurrent());
		assertEquals(null, status.getProgressTotal());
		assertEquals(null, status.getTotalTimeMS());
		assertEquals(null, status.getVersion());
		
		// Now if we call it again we should get a new rest-token
		String newResetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
		
		assertNotNull(newResetToken);
		assertEquals(resetToken, newResetToken);
	}

	@Test
	public void testResetTableStatusToPendingNoBroadcast() throws NotFoundException {
		// Before we start the status should not exist
		try {
			tableStatusDAO.getTableStatus(tableIdNoVersion);
			fail("The status for this table should not exist yet");
		} catch (NotFoundException e) {
			// expected
		}
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
		assertNotNull(resetToken);
		// We should now have a status for this table
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());

	}

	@Test
	public void testAttemptToSetTableStatusToAvailableHappy() throws NotFoundException {
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
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
		tableStatusDAO.resetTableStatusToProcessing(tableId1, isResetToken);
		tableStatusDAO.resetTableStatusToProcessing(tableId2, isResetToken);
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
	 * 
	 * @throws NotFoundException
	 */
	@Test
	public void testAttemptToSetTableStatusToAvailableNullEtag() throws NotFoundException {
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
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
	public void testAttemptToSetTableStatusNotFound() throws NotFoundException {
		assertThrows(NotFoundException.class, () -> {
			// Not make available
			tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, "fake token",
					UUID.randomUUID().toString());
		});
	}

	@Test
	public void testGetTableStausWithVersionNotFound() throws NotFoundException {
		String message = assertThrows(NotFoundException.class, () -> {
			tableStatusDAO.getTableStatus(tableIdWithVersion);
		}).getMessage();
		assertEquals("Table status for 'syn123.456' does not exist", message);
	}

	@Test
	public void testGetTableStausWithNoVersionNotFound() throws NotFoundException {
		String message = assertThrows(NotFoundException.class, () -> {
			tableStatusDAO.getTableStatus(tableIdNoVersion);
		}).getMessage();
		assertEquals("Table status for 'syn123' does not exist", message);
	}

	@Test
	public void testAttemptToSetTableStatusToAvailableConflict() throws NotFoundException {
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertThrows(InvalidStatusTokenException.class, () -> {
			// This should fail since the passed token does not match the current token
			tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, resetToken + "invalidated",
					UUID.randomUUID().toString());
		});
	}

	@Test
	public void testAttemptToSetTableStatusToAvailableMultilpeTimes() throws NotFoundException {
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		// This should pass
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, resetToken, UUID.randomUUID().toString());
		assertThrows(InvalidStatusTokenException.class, () -> {
			// A second time should fail
			tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, resetToken,
					UUID.randomUUID().toString());
		});
	}

	@Test
	public void testAttemptToSetTableStatusToAvailableNullReset() throws NotFoundException {
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertThrows(IllegalArgumentException.class, () -> {
			String nullToken = null;
			tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, nullToken,
					UUID.randomUUID().toString());
		});
	}

	@Test
	public void testAttemptToSetTableStatusToFailedHappy() throws NotFoundException {
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
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
	public void testattemptToSetTableStatusToFailedNotFound() {
		// Call under test
		tableStatusDAO.attemptToSetTableStatusToFailed(tableIdNoVersion, "error", "error details");
		// the state should have been inserted
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING_FAILED, status.getState());
		assertNotNull(status.getTotalTimeMS());
		assertEquals("error", status.getErrorMessage());
		assertEquals("error details", status.getErrorDetails());
		assertEquals(null, status.getLastTableChangeEtag());
	}

	/**
	 * Part of the issue with PLFM-5632 was the error message was too large for the
	 * status table.
	 */
	@Test
	public void testAttemptToSetTableStatusToFailedMessageAtLimit() {
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
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
	 * Part of the issue with PLFM-5632 was the error message was too large for the
	 * status table.
	 */
	@Test
	public void testAttemptToSetTableStatusToFailedMessageOverLimit() {
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		assertNotNull(status.getChangedOn());
		String errorMessage = StringUtils.repeat("a", TableStatusDAOImpl.MAX_ERROR_MESSAGE_CHARS + 1);
		// Not make available
		tableStatusDAO.attemptToSetTableStatusToFailed(tableIdNoVersion, errorMessage, "error details");
		// the state should have changed
		status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertTrue(TableStatusDAOImpl.MAX_ERROR_MESSAGE_CHARS <= status.getErrorMessage().length());
	}

	@Test
	public void testAttemptToUpdateTableProgressHappy() throws NotFoundException {
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
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
	public void testAttemptToUpdateTableProgressNotFound() throws NotFoundException {
		assertThrows(NotFoundException.class, () -> {
			// Not make available
			tableStatusDAO.attemptToUpdateTableProgress(tableIdNoVersion, "fakeToken", "message", 0L, 100L);
		});
	}

	@Test
	public void testAttemptToUpdateTableProgressConflict() throws NotFoundException {
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(status);

		assertThrows(InvalidStatusTokenException.class, () -> {
			// This should fail since the passed token does not match the current token
			tableStatusDAO.attemptToUpdateTableProgress(tableIdNoVersion, resetToken + "invalidated", "message", 1L,
					100L);
		});
	}

	@Test
	public void testNotFound() throws NotFoundException {
		assertThrows(NotFoundException.class, () -> {
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
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			TableStatusDAOImpl.validateAndGetVersion(null);
		});
	}

	@Test
	public void testValidateAndGetVersionIdNull() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			TableStatusDAOImpl.validateAndGetVersion(IdAndVersion.newBuilder().setId(null).build());
		});
	}

	@Test
	public void testWithVersion() {
		// Before we start the status should not exist
		try {
			tableStatusDAO.getTableStatus(tableIdWithVersion);
			fail("The status for this table should not exist yet");
		} catch (NotFoundException e) {
			// expected
		}
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion, isResetToken);
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
		String newResetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion, isResetToken);
		assertNotNull(newResetToken);
		assertFalse(newResetToken.equals(resetToken));
	}

	@Test
	public void testWithAndWithoutVersion() {
		// This should insert a row for this table.
		tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion, isResetToken);
		tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);

		TableStatus withVersion = tableStatusDAO.getTableStatus(tableIdWithVersion);
		assertNotNull(withVersion);
		assertEquals(tableIdWithVersion.getVersion().get(), withVersion.getVersion());
		TableStatus noVersion = tableStatusDAO.getTableStatus(tableIdNoVersion);
		assertNotNull(noVersion);
		assertEquals(null, noVersion.getVersion());
		assertEquals(withVersion.getTableId(), noVersion.getTableId(), "Both should have the same ID");
	}

	@Test
	public void testGetTableStatusStateWithVersion() {
		// This should insert a row for this table.
		tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion, isResetToken);
		// call under test
		Optional<TableState> optional = tableStatusDAO.getTableStatusState(tableIdWithVersion);
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		assertEquals(TableState.PROCESSING, optional.get());
	}

	@Test
	public void testGetTableStatusStateWithNoVersion() {
		// This should insert a row for this table.
		tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
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
	public void testGetTableStatusToken() {
		
		String token = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
		
		// call under test
		Optional<String> result = tableStatusDAO.getTableStatusToken(tableIdNoVersion);
		
		assertEquals(Optional.of(token), result);
	}
	
	@Test
	public void testGetTableStatusTokenDoesNotExist() {
		IdAndVersion doesNotExist = IdAndVersion.parse("syn999.888");
		
		// call under test
		Optional<String> result = tableStatusDAO.getTableStatusToken(doesNotExist);
		
		assertEquals(Optional.empty(), result);
	}

	@Test
	public void testGetLastChangedOn_NoVersion() {
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
		String lastTableChangeEtag = UUID.randomUUID().toString();
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, resetToken, lastTableChangeEtag);
		TableStatus lastStatus = tableStatusDAO.getTableStatus(tableIdNoVersion);
		// call under test
		Date lastChangedOn = tableStatusDAO.getLastChangedOn(tableIdNoVersion).get();
		assertEquals(lastStatus.getChangedOn(), lastChangedOn);
	}

	@Test
	public void testGetLastChangedOn_WithVersion() {
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion, isResetToken);
		String lastTableChangeEtag = UUID.randomUUID().toString();
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdWithVersion, resetToken, lastTableChangeEtag);
		TableStatus lastStatus = tableStatusDAO.getTableStatus(tableIdWithVersion);
		// call under test
		Date lastChangedOn = tableStatusDAO.getLastChangedOn(tableIdWithVersion).get();
		assertEquals(lastStatus.getChangedOn(), lastChangedOn);
	}

	@Test
	public void testGetLastChangedOn_NotFound() {
		IdAndVersion doesNotExist = IdAndVersion.parse("syn999.888");
		// call under test
		assertEquals(Optional.empty(), tableStatusDAO.getLastChangedOn(doesNotExist));
	}

	@Test
	public void testUpdateChangedOnIfAvailable_NoVersion() throws InterruptedException {
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
		String lastTableChangeEtag = UUID.randomUUID().toString();
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdNoVersion, resetToken, lastTableChangeEtag);
		Date startingChangedOn = tableStatusDAO.getLastChangedOn(tableIdNoVersion).get();
		// Sleep to update time
		Thread.sleep(101L);

		// call under test
		boolean updated = tableStatusDAO.updateChangedOnIfAvailable(tableIdNoVersion);
		assertTrue(updated);
		Date endingChangedOne = tableStatusDAO.getLastChangedOn(tableIdNoVersion).get();
		assertTrue(endingChangedOne.after(startingChangedOn));
	}

	@Test
	public void testUpdateChangedOnIfAvailable_WithVersion() throws InterruptedException {
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion, isResetToken);
		String lastTableChangeEtag = UUID.randomUUID().toString();
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableIdWithVersion, resetToken, lastTableChangeEtag);
		Date startingChangedOn = tableStatusDAO.getLastChangedOn(tableIdWithVersion).get();
		// Sleep to update time
		Thread.sleep(101L);

		// call under test
		boolean updated = tableStatusDAO.updateChangedOnIfAvailable(tableIdWithVersion);
		assertTrue(updated);
		Date endingChangedOne = tableStatusDAO.getLastChangedOn(tableIdWithVersion).get();
		assertTrue(endingChangedOne.after(startingChangedOn));
	}

	@Test
	public void testUpdateChangedOnIfAvailable_Processing() throws InterruptedException {
		tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion, isResetToken);
		Date startingChangedOn = tableStatusDAO.getLastChangedOn(tableIdWithVersion).get();
		// Sleep to update time
		Thread.sleep(101L);

		// call under test
		boolean updated = tableStatusDAO.updateChangedOnIfAvailable(tableIdWithVersion);
		assertFalse(updated);
		Date endingChangedOne = tableStatusDAO.getLastChangedOn(tableIdWithVersion).get();
		assertEquals(endingChangedOne, startingChangedOn);
	}

	@Test
	public void testUpdateChangedOnIfAvailable_Failed() throws InterruptedException {
		tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion, isResetToken);
		tableStatusDAO.attemptToSetTableStatusToFailed(tableIdWithVersion, "error", "details");
		Date startingChangedOn = tableStatusDAO.getLastChangedOn(tableIdWithVersion).get();
		// Sleep to update time
		Thread.sleep(101L);

		// call under test
		boolean updated = tableStatusDAO.updateChangedOnIfAvailable(tableIdWithVersion);
		assertFalse(updated);
		Date endingChangedOne = tableStatusDAO.getLastChangedOn(tableIdWithVersion).get();
		assertEquals(endingChangedOne, startingChangedOn);
	}
	

	@Test
	public void testGetLastChangeEtagWithNoEtag() throws InterruptedException {
		tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion, isResetToken);
		// no etag set for this case
		// call under test
		Optional<String> optional = tableStatusDAO.getLastChangeEtag(tableIdWithVersion);
		assertNotNull(optional);
		assertFalse(optional.isPresent());
	}

	@Test
	public void testGetLastChangeEtagWithVersion() throws InterruptedException {
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdWithVersion, isResetToken);
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
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableIdNoVersion, isResetToken);
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
		assertThrows(IllegalArgumentException.class, () -> {
			tableStatusDAO.getLastChangeEtag(null);
		});
	}

	@Test
	public void testGetAllTablesAndViewsWithMissingStatusWithTable() {
		long limit = 100;
		Node project = setupProject();

		Node table = nodeDaoObjectHelper.create(n -> {
			n.setNodeType(TableType.table.getEntityType());
			n.setName("table");
			n.setParentId(project.getId());
		});
		IdAndVersion idAndVersion = new IdAndVersionBuilder().setId(KeyFactory.stringToKey(table.getId()))
				.setVersion(null).build();
		ColumnModel col = columnModelHeler.create(c -> {
		});
		columnModelHeler.bindColumnToObject(List.of(col), idAndVersion);

		// call under test
		List<IdVersionTableType> missing = tableStatusDAO.getAllTablesAndViewsWithMissingStatus(limit);
		List<IdVersionTableType> expected = List.of(new IdVersionTableType(idAndVersion, TableType.table));
		assertEquals(expected, missing);
		
		tableStatusDAO.resetTableStatusToProcessing(idAndVersion, isResetToken);
		
		// call under test
		missing = tableStatusDAO.getAllTablesAndViewsWithMissingStatus(limit);
		expected = Collections.emptyList();
		assertEquals(expected, missing);
	}
	
	@Test
	public void testGetAllTablesAndViewsWithMissingStatusWithTableWithMultipleVersions() {
		long limit = 100;
		Node project = setupProject();

		Node table = nodeDaoObjectHelper.create(n -> {
			n.setNodeType(TableType.table.getEntityType());
			n.setName("table");
			n.setParentId(project.getId());
			n.setVersionLabel("v1");
			n.setVersionNumber(1L);
		});
		
		nodeDaoObjectHelper.createNewVersion(table.setVersionNumber(2L).setVersionLabel("v2"));
		nodeDaoObjectHelper.createNewVersion(table.setVersionNumber(3L).setVersionLabel("v3"));
		
		IdAndVersion current = new IdAndVersionBuilder().setId(KeyFactory.stringToKey(table.getId()))
				.setVersion(null).build();
		IdAndVersion versionOne = new IdAndVersionBuilder().setId(KeyFactory.stringToKey(table.getId()))
				.setVersion(1L).build();
		IdAndVersion versionTwo = new IdAndVersionBuilder().setId(KeyFactory.stringToKey(table.getId()))
				.setVersion(2L).build();
		
		ColumnModel col = columnModelHeler.create(c -> {});
		columnModelHeler.bindColumnToObject(List.of(col), current);
		columnModelHeler.bindColumnToObject(List.of(col), versionOne);
		columnModelHeler.bindColumnToObject(List.of(col), versionTwo);

		// call under test
		List<IdVersionTableType> missing = tableStatusDAO.getAllTablesAndViewsWithMissingStatus(limit);
		List<IdVersionTableType> expected = List.of(new IdVersionTableType(current, TableType.table),
				new IdVersionTableType(versionOne, TableType.table),
				new IdVersionTableType(versionTwo, TableType.table));
		assertEquals(expected, missing);
		
		tableStatusDAO.resetTableStatusToProcessing(current, isResetToken);
		tableStatusDAO.resetTableStatusToProcessing(versionOne, isResetToken);
		tableStatusDAO.resetTableStatusToProcessing(versionTwo, isResetToken);
		
		// call under test
		missing = tableStatusDAO.getAllTablesAndViewsWithMissingStatus(limit);
		expected = Collections.emptyList();
		assertEquals(expected, missing);
	}
	
	
	@Test
	public void testGetAllTablesAndViewsWithMissingStatusWithTableWithLimit() {
		long limit = 2;
		Node project = setupProject();

		Node table = nodeDaoObjectHelper.create(n -> {
			n.setNodeType(TableType.table.getEntityType());
			n.setName("table");
			n.setParentId(project.getId());
			n.setVersionLabel("v1");
			n.setVersionNumber(1L);
		});
		
		nodeDaoObjectHelper.createNewVersion(table.setVersionNumber(2L).setVersionLabel("v2"));
		nodeDaoObjectHelper.createNewVersion(table.setVersionNumber(3L).setVersionLabel("v3"));
		
		IdAndVersion current = new IdAndVersionBuilder().setId(KeyFactory.stringToKey(table.getId()))
				.setVersion(null).build();
		IdAndVersion versionOne = new IdAndVersionBuilder().setId(KeyFactory.stringToKey(table.getId()))
				.setVersion(1L).build();
		IdAndVersion versionTwo = new IdAndVersionBuilder().setId(KeyFactory.stringToKey(table.getId()))
				.setVersion(2L).build();
		
		ColumnModel col = columnModelHeler.create(c -> {});
		columnModelHeler.bindColumnToObject(List.of(col), current);
		columnModelHeler.bindColumnToObject(List.of(col), versionOne);
		columnModelHeler.bindColumnToObject(List.of(col), versionTwo);

		// call under test
		List<IdVersionTableType> missing = tableStatusDAO.getAllTablesAndViewsWithMissingStatus(limit);
		List<IdVersionTableType> expected = List.of(new IdVersionTableType(current, TableType.table),
				new IdVersionTableType(versionOne, TableType.table));
		assertEquals(expected, missing);
	}
	
	@Test
	public void testGetAllTablesAndViewsWithMissingStatusWithTableWithoutSchema() {
		long limit = 100;
		Node project = setupProject();

		Node table = nodeDaoObjectHelper.create(n -> {
			n.setNodeType(TableType.table.getEntityType());
			n.setName("table");
			n.setParentId(project.getId());
		});

		columnModelHeler.truncateAll();

		// call under test
		List<IdVersionTableType> missing = tableStatusDAO.getAllTablesAndViewsWithMissingStatus(limit);
		List<IdVersionTableType> expected = Collections.emptyList();
		assertEquals(expected, missing);
	}
	
	@Test
	public void testGetAllTablesAndViewsWithMissingStatusWithNonTableType() {
		long limit = 100;
		Node project = setupProject();

		Node table = nodeDaoObjectHelper.create(n -> {
			n.setNodeType(EntityType.file);
			n.setName("not a table");
			n.setParentId(project.getId());
		});
		IdAndVersion idAndVersion = new IdAndVersionBuilder().setId(KeyFactory.stringToKey(table.getId()))
				.setVersion(null).build();
		ColumnModel col = columnModelHeler.create(c -> {
		});
		columnModelHeler.bindColumnToObject(List.of(col), idAndVersion);

		// call under test
		List<IdVersionTableType> missing = tableStatusDAO.getAllTablesAndViewsWithMissingStatus(limit);
		List<IdVersionTableType> expected = Collections.emptyList();
		assertEquals(expected, missing);
	}
	
	
	@Test
	public void testGetAllTablesAndViewsWithMissingStatusWithInTrash() {
		long limit = 100;
		Node table = nodeDaoObjectHelper.create(n -> {
			n.setNodeType(TableType.table.getEntityType());
			n.setName("table");
			n.setParentId(TableStatusDAOImpl.TRASH_FOLDER_ID.toString());
		});
		IdAndVersion idAndVersion = new IdAndVersionBuilder().setId(KeyFactory.stringToKey(table.getId()))
				.setVersion(null).build();
		ColumnModel col = columnModelHeler.create(c -> {
		});
		columnModelHeler.bindColumnToObject(List.of(col), idAndVersion);

		// call under test
		List<IdVersionTableType> missing = tableStatusDAO.getAllTablesAndViewsWithMissingStatus(limit);
		List<IdVersionTableType> expected = Collections.emptyList();
		assertEquals(expected, missing);
	}
	
	/**
	 * Helper to setup a project.
	 * @return
	 */
	private Node setupProject() {
		Node project = nodeDaoObjectHelper.create(n -> {
			n.setNodeType(EntityType.project);
			n.setName("project");
		});

		aclObjectHelper.create(a -> {
			a.setId(project.getId());
			a.getResourceAccess().add(new ResourceAccess().setAccessType(Set.of(ACCESS_TYPE.DOWNLOAD))
					.setPrincipalId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()));
		});
		return project;
	}
}
