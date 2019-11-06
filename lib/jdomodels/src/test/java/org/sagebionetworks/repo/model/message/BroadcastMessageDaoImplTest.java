package org.sagebionetworks.repo.model.message;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class BroadcastMessageDaoImplTest {
	
	@Autowired
	BroadcastMessageDao broadcastMessageDao;
	@Autowired
	DBOChangeDAO changeDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	UserGroup user;
	ChangeMessage changeMessage;
	
	@Before
	public void before(){
		
		user = new UserGroup();
		user.setIsIndividual(true);
		user.setId(userGroupDAO.create(user).toString());
		
		changeDao.deleteAllChanges();
		
		changeMessage = new ChangeMessage();
		changeMessage.setChangeType(ChangeType.CREATE);
		changeMessage.setObjectId("123");
		changeMessage.setObjectType(ObjectType.THREAD);
		changeMessage = changeDao.replaceChange(changeMessage);
	}
	
	@After
	public void after(){
		changeDao.deleteAllChanges();
		if(user != null){
			userGroupDAO.delete(user.getId());
		}
	}
	
	@Test
	public void testWasBroadcastDoesNotExist(){
		long doesNotExist = -1;
		// call under test
		boolean wasBroadcast = broadcastMessageDao.wasBroadcast(doesNotExist);
		assertFalse(wasBroadcast);
	}
	
	@Test
	public void setBroadcastHappy(){
		// should not be broadcast yet.
		assertFalse(broadcastMessageDao.wasBroadcast(changeMessage.getChangeNumber()));
		// call under test.
		broadcastMessageDao.setBroadcast(changeMessage.getChangeNumber());
		// should be broadcast
		assertTrue(broadcastMessageDao.wasBroadcast(changeMessage.getChangeNumber()));
	}

}
