package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.UploadDaemonStatusDao;
import org.sagebionetworks.repo.model.file.State;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOUploadDaemonStatusDaoImplAutowireTest {
	
	private static final double DELTA = 0.0001;
	@Autowired
	UploadDaemonStatusDao uploadDaemonStatusDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	private String userId;
	
	List<String> toDelete;
	
	@Before
	public void before(){
		toDelete = new LinkedList<String>();
		userId = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		assertNotNull(userId);
	}
	
	@After
	public void after(){
		for(String id: toDelete){
			uploadDaemonStatusDao.delete(id);
		}
	}
	
	@Test
	public void testCRUD() throws Exception {
		// Create a new status object
		UploadDaemonStatus status = new UploadDaemonStatus();
		status.setStartedBy(userId);
		status.setState(State.PROCESSING);
		status = uploadDaemonStatusDao.create(status);
		assertNotNull(status);
		String id = status.getDaemonId();
		assertNotNull(id);
		toDelete.add(id);
		// Now get it
		status = uploadDaemonStatusDao.get(id);
		assertNotNull(status);
		System.out.println(status);
		assertEquals(id, status.getDaemonId());
		assertEquals(null, status.getErrorMessage());
		assertEquals(null, status.getFileHandleId());
		assertEquals(0.0, status.getPercentComplete(), DELTA);
		assertEquals(new Long(0), status.getRunTimeMS());
		assertEquals(State.PROCESSING, status.getState());
		assertEquals(userId, status.getStartedBy());
		assertNotNull(status.getStartedOn());
		// Now update it
		status.setErrorMessage("Bad error!");
		status.setState(State.FAILED);
		// sleep to make sure the elapse is greater than zero
		Thread.sleep(100);
		assertTrue(uploadDaemonStatusDao.update(status));
		status = uploadDaemonStatusDao.get(id);
		assertNotNull(status);
		System.out.println(status);
		assertEquals(id, status.getDaemonId());
		assertEquals("Bad error!", status.getErrorMessage());
		assertEquals(null, status.getFileHandleId());
		assertEquals(0.0, status.getPercentComplete(), DELTA);
		assertTrue(status.getRunTimeMS() >= new Long(100));
		assertEquals(State.FAILED, status.getState());
		assertEquals(userId, status.getStartedBy());
		assertNotNull(status.getStartedOn());
		// Now delete it and confirm it is gone
		uploadDaemonStatusDao.delete(id);
		try{
			uploadDaemonStatusDao.get(id);
			fail("Should have thrown a NotFoundException");
		}catch(NotFoundException e){
			// expected;
		}
	}

	@Test
	public void testLargeErrorMessage() throws DatastoreException, NotFoundException{
		char[] bigArray = new char[3001];
		Arrays.fill(bigArray, 'a');
		String bigString = new String(bigArray);
		// Create a new status object
		UploadDaemonStatus status = new UploadDaemonStatus();
		status.setStartedBy(userId);
		status.setState(State.FAILED);
		status.setErrorMessage(bigString);
		status = uploadDaemonStatusDao.create(status);
		assertNotNull(status);
		String id = status.getDaemonId();
		assertNotNull(id);
		toDelete.add(id);
		// Now get it
		status = uploadDaemonStatusDao.get(id);
		assertNotNull(status);
		assertNotNull(status.getErrorMessage());
		assertTrue(status.getErrorMessage().length() < UploadDaemonStatusUtils.MAX_ERROR_SIZE);

	}
}
