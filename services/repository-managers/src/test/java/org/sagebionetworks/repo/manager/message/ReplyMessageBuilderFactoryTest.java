package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.springframework.test.util.ReflectionTestUtils;

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
	private UploadContentToS3DAO mockUploadDao;
	
	DiscussionReplyBundle replyBundle;
	DiscussionThreadBundle threadBundle;
	EntityHeader projectHeader;
	String message;
	String key;
	Long actorUserId;
	String actorUsername;
	
	ReplyMessageBuilderFactory factory;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		factory = new ReplyMessageBuilderFactory();
		ReflectionTestUtils.setField(factory, "replyDao", mockReplyDao);
		ReflectionTestUtils.setField(factory, "threadDao", mockThreadDao);
		ReflectionTestUtils.setField(factory, "nodeDao", mockNodeDao);
		ReflectionTestUtils.setField(factory, "principalAliasDAO", mockPrincipalAliasDAO);
		ReflectionTestUtils.setField(factory, "uploadDao", mockUploadDao);
		
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
		threadBundle.setTitle("title");
		when(mockThreadDao.getThread(anyLong(), any(DiscussionFilter.class))).thenReturn(threadBundle);
		
		projectHeader = new EntityHeader();
		projectHeader.setName("project name");
		when(mockNodeDao.getEntityHeader(anyString(), anyLong())).thenReturn(projectHeader);

		when(mockUploadDao.getMessage(key)).thenReturn(message);

		actorUserId = 456L;
		actorUsername = "someone";
		when(mockPrincipalAliasDAO.getUserName(actorUserId)).thenReturn(actorUsername);
	}
	
	@Test
	public void testBuild(){
		String objectId = "123";
		ChangeType type = ChangeType.CREATE;
		BroadcastMessageBuilder bulider = factory.createMessageBuilder(objectId, type, actorUserId);
		assertNotNull(bulider);
		verify(mockNodeDao).getEntityHeader("444", null);
		verify(mockUploadDao).getMessage(key);
	}
}
