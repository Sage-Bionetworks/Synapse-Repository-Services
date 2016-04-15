package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class TableStatusDAOImplTest {

	@Autowired
	TableStatusDAO tableStatusDAO;
	@Autowired
	DBOChangeDAO changeDAO;
	
	@Before
	public void before(){
		tableStatusDAO.clearAllTableState();
		changeDAO.deleteAllChanges();
	}
	
	@After
	public void after(){
		tableStatusDAO.clearAllTableState();
	}
	
	@Test
	public void testResetTableStatusToPending() throws NotFoundException{
		long startNumber = changeDAO.getCurrentChangeNumber();
		String tableId = "syn123";
		// Before we start the status should not exist
		try{
			tableStatusDAO.getTableStatus(tableId);
			fail("The status for this table should not exist yet");
		}catch(NotFoundException e){
			// expected
		}
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing("syn123");
		assertNotNull(resetToken);
		// We should now have a status for this table
		TableStatus status = tableStatusDAO.getTableStatus(tableId);
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
		// Now if we call it again we should get a new rest-token
		String newResetToken = tableStatusDAO.resetTableStatusToProcessing("123");
		assertNotNull(newResetToken);
		assertFalse(newResetToken.equals(resetToken));
		
		// Did a message get sent?
		List<ChangeMessage> changes = changeDAO.listChanges(startNumber+1, ObjectType.TABLE, Long.MAX_VALUE);
		assertNotNull(changes);
		assertEquals("Changing the column binding of a table did not fire a change message",1, changes.size());
		ChangeMessage message = changes.get(0);
		assertNotNull(message);
		assertEquals("123", message.getObjectId());
		assertEquals(ChangeType.UPDATE, message.getChangeType());
		assertEquals(ObjectType.TABLE, message.getObjectType());
		assertEquals(newResetToken, message.getObjectEtag());
	}
	
	@Test
	public void testResetTableStatusToPendingNoBroadcast() throws NotFoundException{
		long startNumber = changeDAO.getCurrentChangeNumber();
		String tableId = "syn123";
		// Before we start the status should not exist
		try{
			tableStatusDAO.getTableStatus(tableId);
			fail("The status for this table should not exist yet");
		}catch(NotFoundException e){
			// expected
		}
		String resetToken = tableStatusDAO.resetTableStatusToProcessing("syn123");
		assertNotNull(resetToken);
		// We should now have a status for this table
		TableStatus status = tableStatusDAO.getTableStatus(tableId);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		
		// no changes should have been sent
		long endChangeNumber = changeDAO.getCurrentChangeNumber();
		assertEquals(startNumber, endChangeNumber);
	}
	
	@Test
	public void testAttemptToSetTableStatusToAvailableHappy() throws NotFoundException{
		String tableId = "syn123";
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing("syn123");
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableId);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		assertNotNull(status.getChangedOn());
		String lastTableChangeEtag = UUID.randomUUID().toString();
		// Not make available
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableId, resetToken, lastTableChangeEtag);
		// the state should have changed
		status = tableStatusDAO.getTableStatus(tableId);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.AVAILABLE, status.getState());
		assertNotNull(status.getTotalTimeMS());
		assertEquals(lastTableChangeEtag, status.getLastTableChangeEtag());
	}
	
	@Test
	public void testDeleteTableStatus() throws NotFoundException {
		String tableId1 = "syn1";
		String tableId2 = "syn2";
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
		String tableId = "syn123";
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing("syn123");
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableId);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		assertNotNull(status.getChangedOn());
		// Not make available
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableId, resetToken, null);
		// the state should have changed
		status = tableStatusDAO.getTableStatus(tableId);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.AVAILABLE, status.getState());
		assertNotNull(status.getTotalTimeMS());
		assertEquals(null, status.getLastTableChangeEtag());
	}
	
	@Test (expected=NotFoundException.class)
	public void testAttemptToSetTableStatusNotFound() throws NotFoundException{
		String tableId = "syn123";
		// Not make available
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableId, "fake token", UUID.randomUUID().toString());
	}
	
	@Test (expected=ConflictingUpdateException.class)
	public void testAttemptToSetTableStatusToAvailableConflict() throws NotFoundException{
		String tableId = "syn123";
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing("syn123");
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableId);
		assertNotNull(status);
		// This should fail since the passed token does not match the current token
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableId, resetToken+"invalidated", UUID.randomUUID().toString());
	}
	
	@Test
	public void testAttemptToSetTableStatusToFailedHappy() throws NotFoundException{
		String tableId = "syn123";
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing("syn123");
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableId);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		assertNotNull(status.getChangedOn());
		// Not make available
		tableStatusDAO.attemptToSetTableStatusToFailed(tableId, resetToken, "error", "error details");
		// the state should have changed
		status = tableStatusDAO.getTableStatus(tableId);
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
		String tableId = "syn123";
		// Not make available
		tableStatusDAO.attemptToSetTableStatusToFailed(tableId, "fake token", "error", "error details");
	}
	
	@Test (expected=ConflictingUpdateException.class)
	public void testAttemptToSetTableStatusToFailedConflict() throws NotFoundException{
		String tableId = "syn123";
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing("syn123");
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableId);
		assertNotNull(status);
		// This should fail since the passed token does not match the current token
		tableStatusDAO.attemptToSetTableStatusToFailed(tableId, resetToken+"invalidated", "error", "error details");
	}
	
	@Test
	public void testAttemptToUpdateTableProgressHappy() throws NotFoundException{
		String tableId = "syn123";
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing("syn123");
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableId);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.PROCESSING, status.getState());
		assertNotNull(status.getChangedOn());
		// Not make available
		tableStatusDAO.attemptToUpdateTableProgress(tableId, resetToken, "message", 0l, 100L);
		// the state should have changed
		status = tableStatusDAO.getTableStatus(tableId);
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
		String tableId = "syn123";
		// Not make available
		tableStatusDAO.attemptToUpdateTableProgress(tableId,"fakeToken", "message", 0L, 100L);
	}
	
	@Test (expected=ConflictingUpdateException.class)
	public void testAttemptToUpdateTableProgressConflict() throws NotFoundException{
		String tableId = "syn123";
		// This should insert a row for this table.
		String resetToken = tableStatusDAO.resetTableStatusToProcessing("syn123");
		// Status should start as processing
		TableStatus status = tableStatusDAO.getTableStatus(tableId);
		assertNotNull(status);
		// This should fail since the passed token does not match the current token
		tableStatusDAO.attemptToUpdateTableProgress(tableId, resetToken+"invalidated", "message", 1L, 100L);
	}
	
	@Test (expected=NotFoundException.class)
	public void testNotFound() throws NotFoundException{
		TableStatus clone = tableStatusDAO.getTableStatus("-99");
	}
}
