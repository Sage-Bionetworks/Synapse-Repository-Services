package org.sagebionetworks.repo.manager.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.dbo.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dbo.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.ForumObjectType;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;

@RunWith(MockitoJUnitRunner.class)
public class ReplyMessageBuilderFactoryTest {

	
	@Mock
	private DiscussionReplyDAO mockReplyDao;
	@Mock
	private DiscussionThreadDAO mockThreadDao;
	@Mock
	private NodeDAO mockNodeDao;
	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDAO;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private UploadContentToS3DAO mockUploadDao;
	@Mock
	private MarkdownDao mockMarkdownDao;
	
	DiscussionReplyBundle replyBundle;
	DiscussionThreadBundle threadBundle;
	String message;
	String key;
	Long actorUserId;
	String actorUsername;

	@InjectMocks
	ReplyMessageBuilderFactory factory;
	
	@Before
	public void before(){
		key = "key";
		message = "message";
		replyBundle = new DiscussionReplyBundle();
		replyBundle.setId("222");
		replyBundle.setThreadId("333");
		replyBundle.setCreatedBy("555");
		replyBundle.setMessageKey(key);
		when(mockReplyDao.getReply(anyLong(), any(DiscussionFilter.class))).thenReturn(replyBundle);
		
		threadBundle = new DiscussionThreadBundle();
		threadBundle.setId("333");
		threadBundle.setProjectId("444");
		threadBundle.setObjectId("444");
		threadBundle.setObjectType(ForumObjectType.ENTITY);
		threadBundle.setTitle("title");
		when(mockThreadDao.getThread(anyLong(), any(DiscussionFilter.class))).thenReturn(threadBundle);
		
		when(mockNodeDao.getNodeName(anyString())).thenReturn("project name");

		when(mockUploadDao.getMessage(key)).thenReturn(message);

		actorUserId = 456L;
		actorUsername = "someone";
		when(mockPrincipalAliasDAO.getUserName(actorUserId)).thenReturn(actorUsername);
	}
	
	@Test
	public void testBuildForProject(){
		String objectId = "123";
		ChangeType type = ChangeType.CREATE;
		BroadcastMessageBuilder bulider = factory.createMessageBuilder(objectId, type, actorUserId);
		assertNotNull(bulider);
		assertEquals("syn" + threadBundle.getObjectId(), ((DiscussionBroadcastMessageBuilder) bulider).projectId);
		assertEquals("project name",  ((DiscussionBroadcastMessageBuilder) bulider).projectName);
		verify(mockNodeDao).getNodeName("444");
		verify(mockUploadDao).getMessage(key);
	}

	@Test
	public void testBuildForAR(){
		threadBundle.setObjectType(ForumObjectType.ACCESS_REQUIREMENT);
		String objectId = "123";
		ChangeType type = ChangeType.CREATE;
		BroadcastMessageBuilder bulider = factory.createMessageBuilder(objectId, type, actorUserId);
		assertNotNull(bulider);
		assertEquals(threadBundle.getObjectId(), ((DiscussionBroadcastMessageBuilder) bulider).projectId);
		verifyNoMoreInteractions(mockNodeDao);
		verify(mockUploadDao).getMessage(key);
	}
}
