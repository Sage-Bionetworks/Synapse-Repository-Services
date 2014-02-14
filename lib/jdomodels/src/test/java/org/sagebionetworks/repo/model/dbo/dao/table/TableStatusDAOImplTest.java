package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
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
	
	@Before
	public void before(){
		tableStatusDAO.clearAllTableState();
	}
	
	@After
	public void after(){
		tableStatusDAO.clearAllTableState();
	}
	
	@Test
	public void testResetTableStatusToPending() throws NotFoundException{
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
		assertEquals(null, status.getProgresssCurrent());
		assertEquals(null, status.getProgresssTotal());
		assertEquals(null, status.getTotalTimeMS());
		// Now if we call it again we should get a new rest-token
		String newResetToken = tableStatusDAO.resetTableStatusToProcessing("123");
		assertNotNull(newResetToken);
		assertFalse(newResetToken.equals(resetToken));
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
		// Not make available
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableId, resetToken);
		// the state should have changed
		status = tableStatusDAO.getTableStatus(tableId);
		assertNotNull(status);
		assertEquals("123", status.getTableId());
		assertEquals(TableState.AVAILABLE, status.getState());
		assertNotNull(status.getTotalTimeMS());
	}
	
	@Test (expected=NotFoundException.class)
	public void testAttemptToSetTableStatusNotFound() throws NotFoundException{
		String tableId = "syn123";
		// Not make available
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableId, "fake token");
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
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableId, resetToken+"invalidated");
	}
	
	@Test (expected=NotFoundException.class)
	public void testNotFound() throws NotFoundException{
		TableStatus clone = tableStatusDAO.getTableStatus("-99");
	}
}
