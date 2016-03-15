package org.sagebionetworks.repo.model.message;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageRecipient;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class BroadcastMessageDaoImplTest {
	
	@Autowired
	BroadcastMessageDao broadcastMessageDao;
	@Autowired
	DBOChangeDAO changeDao;
	@Autowired
	private MessageDAO messageDAO;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private FileHandleDao fileDAO;
	
	UserGroup user;
	String fileHandleId;
	MessageToUser messageToUser;
	ChangeMessage changeMessage;
	Long messageId;
	
	@Before
	public void before(){
		
		user = new UserGroup();
		user.setIsIndividual(true);
		user.setId(userGroupDAO.create(user).toString());
		
		S3FileHandle handle = TestUtils.createS3FileHandle(user.getId());
		handle = fileDAO.createFile(handle);
		fileHandleId = handle.getId();
		
		messageToUser = new MessageToUser();
		messageToUser.setFileHandleId(fileHandleId);
		messageToUser.setCreatedBy(user.getId());
		messageToUser.setCreatedOn(new Date());
		messageToUser.setSubject("A Test");
		messageToUser.setRecipients(Sets.newHashSet(user.getId()));
		
		messageToUser = messageDAO.createMessage(messageToUser);
		messageId = Long.parseLong(messageToUser.getId());
		
		changeDao.deleteAllChanges();
		
		changeMessage = new ChangeMessage();
		changeMessage.setChangeType(ChangeType.CREATE);
		changeMessage.setObjectEtag("etag");
		changeMessage.setObjectId("123");
		changeMessage.setObjectType(ObjectType.THREAD);
		changeMessage = changeDao.replaceChange(changeMessage);
	}
	
	@After
	public void after(){
		if(messageToUser != null && messageToUser.getId() != null){
			messageDAO.deleteMessage(messageToUser.getId());
		}
		if(fileHandleId != null){
			fileDAO.delete(fileHandleId);
		}
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
		broadcastMessageDao.setBroadcast(changeMessage.getChangeNumber(), messageId);
		// should be broadcast
		assertTrue(broadcastMessageDao.wasBroadcast(changeMessage.getChangeNumber()));
	}
	
	@Test (expected=NotFoundException.class)
	public void setBroadcastChangeDoesNotExist(){
		Long changeDoesNotExist = -1L;
		// call under test.
		broadcastMessageDao.setBroadcast(changeDoesNotExist, messageId);
	}

	@Test (expected=NotFoundException.class)
	public void setBroadcastMessageDoesNotExist(){
		Long messageDoesNotExist = -1L;
		// call under test.
		broadcastMessageDao.setBroadcast(changeMessage.getChangeNumber(), messageDoesNotExist);
	}
}
