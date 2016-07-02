package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.markdown.MarkdownDao;
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
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.springframework.test.util.ReflectionTestUtils;

public class ReplyMessageBuilderTest {

	
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
	@Mock
	private MarkdownDao mockMarkdownDao;
	
	DiscussionReplyBundle replyBundle;
	DiscussionThreadBundle threadBundle;
	EntityHeader projectHeader;
	String message;
	String key;
	Long actorUserId;
	String actorUsername;
	
	ReplyMessageBuilder builder;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		builder = new ReplyMessageBuilder();
		ReflectionTestUtils.setField(builder, "replyDao", mockReplyDao);
		ReflectionTestUtils.setField(builder, "threadDao", mockThreadDao);
		ReflectionTestUtils.setField(builder, "nodeDao", mockNodeDao);
		ReflectionTestUtils.setField(builder, "principalAliasDAO", mockPrincipalAliasDAO);
		ReflectionTestUtils.setField(builder, "uploadDao", mockUploadDao);
		ReflectionTestUtils.setField(builder, "markdownDao", mockMarkdownDao);
		
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
		builder.createMessageBuilder(objectId, type, actorUserId);
		verify(mockReplyDao).getReply(123L, DiscussionFilter.NO_FILTER);
		verify(mockNodeDao).getEntityHeader("444", null);
		verify(mockPrincipalAliasDAO).getUserName(actorUserId);
		verify(mockUploadDao).getMessage(key);
	}

	@Test
	public void testGetBroadcastTopic() {
		Topic broadcastTopic = new Topic();
		broadcastTopic.setObjectId(replyBundle.getThreadId());
		broadcastTopic.setObjectType(SubscriptionObjectType.THREAD);
		String objectId = "123";
		ChangeType type = ChangeType.CREATE;
		builder.createMessageBuilder(objectId, type, actorUserId);
		assertEquals(broadcastTopic, builder.getBroadcastTopic());
	}

	@Test
	public void testGetRelatedUsersWithMentionedUser() {
		when(mockUploadDao.getMessage(key)).thenReturn("@user");
		String objectId = "123";
		ChangeType type = ChangeType.CREATE;
		Set<String> usernameList = new HashSet<String>();
		usernameList.add("user");
		when(mockPrincipalAliasDAO.lookupPrincipalIds(usernameList)).thenReturn(usernameList);
		builder.createMessageBuilder(objectId, type, actorUserId);
		assertEquals(usernameList , builder.getRelatedUsers());
		verify(mockPrincipalAliasDAO).lookupPrincipalIds(usernameList);
	}

	@Test
	public void testGetRelatedUsersWithoutMentionedUser() {
		String objectId = "123";
		ChangeType type = ChangeType.CREATE;
		when(mockPrincipalAliasDAO.lookupPrincipalIds(eq(new HashSet<String>()))).thenReturn(new HashSet<String>());
		builder.createMessageBuilder(objectId, type, actorUserId);
		assertEquals(new HashSet<String>(), builder.getRelatedUsers());
		verify(mockPrincipalAliasDAO).lookupPrincipalIds(new HashSet<String>());
	}
}
