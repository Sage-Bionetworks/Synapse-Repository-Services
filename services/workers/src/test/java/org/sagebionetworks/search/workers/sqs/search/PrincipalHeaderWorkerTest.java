package org.sagebionetworks.search.workers.sqs.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PrincipalHeaderDAO;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.services.sqs.model.Message;

public class PrincipalHeaderWorkerTest {
	
	List<Message> messages;
	PrincipalHeaderDAO mockPrincipalHeaderDAO;
	UserGroupDAO mockUserGroupDAO;
	UserProfileDAO mockUserProfileDAO;
	TeamDAO mockTeamDAO;
	WorkerLogger mockWorkerLogger;

	@Before
	public void before() {
		messages = new LinkedList<Message>();
		mockPrincipalHeaderDAO = Mockito.mock(PrincipalHeaderDAO.class);
		mockUserGroupDAO = Mockito.mock(UserGroupDAO.class);
		mockUserProfileDAO = Mockito.mock(UserProfileDAO.class);
		mockTeamDAO = Mockito.mock(TeamDAO.class);
		mockWorkerLogger = Mockito.mock(WorkerLogger.class);
	}
	
	@Test
	public void testSkipNonPrincipalOrTeamMsg() throws Exception {
		ChangeMessage chgMsg = new ChangeMessage();
		chgMsg.setChangeNumber(1000L);
		chgMsg.setChangeType(ChangeType.CREATE);
		chgMsg.setObjectEtag("etag");
		chgMsg.setObjectId("id");
		chgMsg.setObjectType(ObjectType.ENTITY);
		chgMsg.setParentId("parentId");
		chgMsg.setTimestamp(new Date());
		Message msg = MessageUtils.createMessage(chgMsg, "outerId1000", "handler");
		messages.add(msg);
		PrincipalHeaderWorker worker = new PrincipalHeaderWorker(messages, mockPrincipalHeaderDAO, mockUserGroupDAO, mockUserProfileDAO, mockTeamDAO, mockWorkerLogger);
		List<Message> processedMsgs = worker.call();
		assertNotNull(processedMsgs);
		assertEquals(1, processedMsgs.size());
		assertEquals(msg, processedMsgs.get(0));
	}
	
	@Test
	public void testWorkerLoggerCalledOnNotFound() throws Exception {
		ChangeMessage chgMsg = new ChangeMessage();
		chgMsg.setChangeNumber(1000L);
		chgMsg.setChangeType(ChangeType.CREATE);
		chgMsg.setObjectEtag("etag");
		chgMsg.setObjectId("12345");
		chgMsg.setObjectType(ObjectType.PRINCIPAL);
		chgMsg.setParentId("parentId");
		chgMsg.setTimestamp(new Date());
		Message msg = MessageUtils.createMessage(chgMsg, "outerId1000", "handler");
		messages.add(msg);
		Long principalId = Long.parseLong(chgMsg.getObjectId());
		when(mockPrincipalHeaderDAO.delete(principalId)).thenReturn(1L);
		NotFoundException e = new NotFoundException();
		when(mockUserGroupDAO.get(principalId)).thenThrow(e);
		PrincipalHeaderWorker worker = new PrincipalHeaderWorker(messages, mockPrincipalHeaderDAO, mockUserGroupDAO, mockUserProfileDAO, mockTeamDAO, mockWorkerLogger);
		List<Message> processedMsgs = worker.call();
		verify(mockWorkerLogger).logWorkerFailure(PrincipalHeaderWorker.class, chgMsg, e, false);
		assertNotNull(processedMsgs);
		assertEquals(1, processedMsgs.size());
		assertEquals(msg, processedMsgs.get(0));
	}

	@Test
	public void testWorkerLoggerCalledOnThrowable() throws Exception {
		ChangeMessage chgMsg = new ChangeMessage();
		chgMsg.setChangeNumber(1000L);
		chgMsg.setChangeType(ChangeType.CREATE);
		chgMsg.setObjectEtag("etag");
		chgMsg.setObjectId("12345");
		chgMsg.setObjectType(ObjectType.PRINCIPAL);
		chgMsg.setParentId("parentId");
		chgMsg.setTimestamp(new Date());
		Message msg = MessageUtils.createMessage(chgMsg, "outerId1000", "handler");
		messages.add(msg);
		Long principalId = Long.parseLong(chgMsg.getObjectId());
		when(mockPrincipalHeaderDAO.delete(principalId)).thenReturn(1L);
		RuntimeException e = new RuntimeException();
		when(mockUserGroupDAO.get(principalId)).thenThrow(e);
		PrincipalHeaderWorker worker = new PrincipalHeaderWorker(messages, mockPrincipalHeaderDAO, mockUserGroupDAO, mockUserProfileDAO, mockTeamDAO, mockWorkerLogger);
		List<Message> processedMsgs = worker.call();
		verify(mockWorkerLogger).logWorkerFailure(PrincipalHeaderWorker.class, chgMsg, e, true);
		assertNotNull(processedMsgs);
		assertEquals(0, processedMsgs.size());
	}

}
