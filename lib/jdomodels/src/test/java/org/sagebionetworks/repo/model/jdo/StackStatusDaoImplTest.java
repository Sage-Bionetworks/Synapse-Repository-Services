package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test for the StackStatusDaoImpl.
 * @author jmhill
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class StackStatusDaoImplTest {
	
	@Autowired
	StackStatusDao stackStatusDao;
	
	@Test
	public void testGetFullCurrentStatus(){
		StackStatus status = stackStatusDao.getFullCurrentStatus();
		assertNotNull(status);
		// The current status should be read-write
		assertEquals(StatusEnum.READ_WRITE, status.getStatus());
	}
	
	@Test
	public void testGetCurrentStatus(){
		StatusEnum status = stackStatusDao.getCurrentStatus();
		assertEquals(StatusEnum.READ_WRITE, status);
		assertTrue(stackStatusDao.isStackReadWrite());
	}
	
	@Test
	public void testUpdate(){
		StackStatus starting = stackStatusDao.getFullCurrentStatus();
		// Change the status
		StackStatus newStatus = new StackStatus();
		newStatus.setStatus(StatusEnum.READ_ONLY);
		newStatus.setCurrentMessage("New Current message");
		newStatus.setPendingMaintenanceMessage("Some big mantenace job will occur soon!");
		
		// Update the status
		stackStatusDao.updateStatus(newStatus);
		
		StackStatus current = stackStatusDao.getFullCurrentStatus();
		assertEquals(newStatus, current);
		
		assertFalse(stackStatusDao.isStackReadWrite());
		
		// Change it back
		stackStatusDao.updateStatus(starting);
		current = stackStatusDao.getFullCurrentStatus();
		assertEquals(starting, current);
	}

}
