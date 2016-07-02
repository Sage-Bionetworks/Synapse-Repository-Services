package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

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
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.springframework.test.util.ReflectionTestUtils;

public class ThreadMessageBuilderTest {

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

	DiscussionThreadBundle threadBundle;
	EntityHeader projectHeader;
	String message;
	String key;
	Long actorUserId;
	String actorUsername;
	
	ThreadMessageBuilder builder;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);

		builder = new ThreadMessageBuilder();
		ReflectionTestUtils.setField(builder, "threadDao", mockThreadDao);
		ReflectionTestUtils.setField(builder, "nodeDao", mockNodeDao);
		ReflectionTestUtils.setField(builder, "principalAliasDAO", mockPrincipalAliasDAO);
		ReflectionTestUtils.setField(builder, "uploadDao", mockUploadDao);
		ReflectionTestUtils.setField(builder, "markdownDao", mockMarkdownDao);

		key = "key";
		message = "message";
		threadBundle = new DiscussionThreadBundle();
		threadBundle.setId("333");
		threadBundle.setProjectId("444");
		threadBundle.setTitle("title");
		threadBundle.setCreatedBy("987");
		threadBundle.setMessageKey(key);
		when(mockThreadDao.getThread(anyLong(), any(DiscussionFilter.class))).thenReturn(threadBundle);

		projectHeader = new EntityHeader();
		projectHeader.setName("project name");
		when(mockNodeDao.getEntityHeader(anyString(),  anyLong())).thenReturn(projectHeader);

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
		verify(mockThreadDao).getThread(123L, DiscussionFilter.NO_FILTER);
		verify(mockNodeDao).getEntityHeader("444", null);
		verify(mockPrincipalAliasDAO).getUserName(actorUserId);
		verify(mockUploadDao).getMessage(key);
	}

	@Test
	public void testGetBroadcastTopic() {
		Topic broadcastTopic = new Topic();
		broadcastTopic.setObjectId(threadBundle.getForumId());
		broadcastTopic.setObjectType(SubscriptionObjectType.FORUM);
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
