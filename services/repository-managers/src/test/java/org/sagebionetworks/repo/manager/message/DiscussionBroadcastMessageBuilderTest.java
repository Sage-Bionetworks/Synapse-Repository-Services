package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.model.broadcast.UserNotificationInfo;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.utils.HttpClientHelperException;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public class DiscussionBroadcastMessageBuilderTest {
	@Mock
	MarkdownDao mockMarkdownDao;
	@Mock
	PrincipalAliasDAO mockPrincipalAliasDAO;

	String actorUsername;
	String actorUserId;
	String threadTitle;
	String threadId;
	String projectName;
	String projectId;
	String markdown;
	Subscriber subscriber;
	UserNotificationInfo user;
	Topic topic;
	
	DiscussionBroadcastMessageBuilder builder;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);

		actorUsername = "someone";
		actorUserId = "1";
		threadTitle = "How to use Synapse?";
		threadId = "333";
		projectName = "Synapse Help";
		projectId = "syn8888";
		markdown = "How do I get started?";
	
		subscriber = new Subscriber();
		subscriber.setFirstName("subscriberFirstName");
		subscriber.setLastName("subscriberLastName");
		subscriber.setNotificationEmail("subsciber@domain.org");
		subscriber.setSubscriberId("123");
		subscriber.setUsername("subscriberUsername");
		subscriber.setSubscriptionId("999");

		user = new UserNotificationInfo();
		user.setFirstName("firstName");
		user.setLastName("lastName");
		user.setNotificationEmail("notificationEmail@domain.org");
		user.setUserId("456");
		user.setUsername("username");

		topic = new Topic();
		topic.setObjectId("777");
		topic.setObjectType(SubscriptionObjectType.FORUM);
	
		builder = new DiscussionBroadcastMessageBuilder(actorUsername, actorUserId,
				threadTitle, threadId, projectId, projectName, markdown,
				ThreadMessageBuilderFactory.THREAD_TEMPLATE, ThreadMessageBuilderFactory.THREAD_CREATED_TITLE,
				ThreadMessageBuilderFactory.UNSUBSCRIBE_FORUM, mockMarkdownDao, topic, mockPrincipalAliasDAO);
	}

	@Test
	public void testBuildRawBodyForSubscriber(){
		String body = builder.buildRawBodyForSubscriber(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("someone"));
		assertTrue(body.contains(threadTitle));
		assertTrue(body.contains(projectName));
		assertTrue(body.contains("https://www.synapse.org/#!Subscription:objectID=333&objectType=THREAD"));
		assertTrue(body.contains("https://www.synapse.org/#!Subscription:subscriptionID=999"));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:syn8888/discussion/threadId=333"));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:syn8888/discussion"));
		assertTrue(body.contains("Subscribe to the thread"));
		assertTrue(body.contains("Unsubscribe to the forum"));
	}

	@Test
	public void testBuildReplyRawBodyForSubscriber(){
		topic.setObjectType(SubscriptionObjectType.THREAD);
		builder = new DiscussionBroadcastMessageBuilder(actorUsername, actorUserId,
				threadTitle, threadId, projectId, projectName, markdown,
				ReplyMessageBuilderFactory.REPLY_TEMPLATE, ReplyMessageBuilderFactory.REPLY_CREATED_TITLE,
				ReplyMessageBuilderFactory.UNSUBSCRIBE_THREAD, mockMarkdownDao, topic, mockPrincipalAliasDAO);
	
		String body = builder.buildRawBodyForSubscriber(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("someone"));
		assertTrue(body.contains(threadTitle));
		assertTrue(body.contains(projectName));
		assertFalse(body.contains("https://www.synapse.org/#!Subscription:objectID=333&objectType=THREAD"));
		assertTrue(body.contains("https://www.synapse.org/#!Subscription:subscriptionID=999"));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:syn8888/discussion/threadId=333"));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:syn8888/discussion"));
		assertFalse(body.contains("Subscribe to the thread"));
		assertTrue(body.contains("Unsubscribe to the thread"));
	}

	@Test
	public void testBuildRawBodyForNoneSubscriber(){
		String body = builder.buildRawBodyForNonSubscriber(user);
		assertNotNull(body);
		assertTrue(body.contains("firstName lastName (username)"));
		assertTrue(body.contains("someone"));
		assertTrue(body.contains(threadTitle));
		assertTrue(body.contains(projectName));
		assertTrue(body.contains("https://www.synapse.org/#!Subscription:objectID=333&objectType=THREAD"));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:syn8888/discussion/threadId=333"));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:syn8888/discussion"));
		assertTrue(body.contains("Subscribe to the thread"));
		assertFalse(body.contains("Unsubscribe to the forum"));
	}

	@Test
	public void testBuildRawBodyForSubscriberWithSynapseWidget(){
		markdown = "Seen you eyes son show.\n@kimyen\n${jointeam?teamId=3319496&isChallenge=false&"
				+ "isSimpleRequestButton=false&isMemberMessage=Already a member&successMessage=Successfully "
				+ "joined&text=Join&requestOpenText=Your request to join this team has been sent%2E}";
		builder = new DiscussionBroadcastMessageBuilder(actorUsername, actorUserId,
				threadTitle, threadId, projectId, projectName, markdown,
				ThreadMessageBuilderFactory.THREAD_TEMPLATE, ThreadMessageBuilderFactory.THREAD_CREATED_TITLE,
				ThreadMessageBuilderFactory.UNSUBSCRIBE_FORUM, mockMarkdownDao, topic, mockPrincipalAliasDAO);
		String body = builder.buildRawBodyForSubscriber(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("someone"));
		assertTrue(body.contains(threadTitle));
		assertTrue(body.contains(projectName));
		assertTrue(body.contains("https://www.synapse.org/#!Subscription:subscriptionID=999"));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:syn8888/discussion/threadId=333"));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:syn8888/discussion"));
		assertTrue(body.contains("Seen you eyes son show.\n>@kimyen\n>${jointeam?teamId=3319496&isChallenge=false&"
				+ "isSimpleRequestButton=false&isMemberMessage=Already a member&successMessage=Successfully "
				+ "joined&text=Join&requestOpenText=Your request to join this team has been sent%2E}"));
	}
	
	@Test (expected = HttpClientHelperException.class)
	public void testBuildEmailForSubscriberFailure() throws Exception{
		when(mockMarkdownDao.convertMarkdown(anyString(), anyString())).thenThrow(new HttpClientHelperException("", 500, ""));
		builder.buildEmailForSubscriber(subscriber);
	}
	
	@Test
	public void testBuildEmailForSubscriberSuccess() throws Exception{
		when(mockMarkdownDao.convertMarkdown(anyString(), anyString())).thenReturn("content");
		SendRawEmailRequest request = builder.buildEmailForSubscriber(subscriber);
		assertNotNull(request);
	}

	@Test
	public void testTruncateStringOver(){
		String input = "123456789";
		String truncate = DiscussionBroadcastMessageBuilder.truncateString(input, 4);
		assertEquals("1234...", truncate);
	}

	@Test
	public void testTruncateStringUnder(){
		String input = "123456789";
		String truncate = DiscussionBroadcastMessageBuilder.truncateString(input, input.length());
		assertEquals(input, truncate);
	}

	@Test
	public void testGetTopic() {
		assertEquals(topic, builder.getBroadcastTopic());
	}

	@Test
	public void testGetRelatedUsersWithoutMentionedUsers() {
		assertEquals(new HashSet<String>(), builder.getRelatedUsers());
	}

	@Test
	public void testGetRelatedUsersWithMentionedUsers() {
		Set<String> usernameSet = new HashSet<String>();
		usernameSet.add("user");
		Set<String> userIdSet = new HashSet<String>();
		userIdSet.add("101");
		when(mockPrincipalAliasDAO.lookupPrincipalIds(usernameSet)).thenReturn(userIdSet);
		builder = new DiscussionBroadcastMessageBuilder(actorUsername, actorUserId,
				threadTitle, threadId, projectId, projectName, "@user",
				ThreadMessageBuilderFactory.THREAD_TEMPLATE, ThreadMessageBuilderFactory.THREAD_CREATED_TITLE,
				ThreadMessageBuilderFactory.UNSUBSCRIBE_FORUM, mockMarkdownDao, topic, mockPrincipalAliasDAO);
		assertEquals(userIdSet, builder.getRelatedUsers());
	}

	@Test
	public void testBuildEmailForSubscriber() throws Exception{
		when(mockMarkdownDao.convertMarkdown(anyString(), anyString())).thenReturn("content");
		SendRawEmailRequest emailRequest = builder.buildEmailForSubscriber(subscriber);
		assertNotNull(emailRequest);
	}

	@Test
	public void testBuildEmailForNonSubscriber() throws Exception{
		when(mockMarkdownDao.convertMarkdown(anyString(), anyString())).thenReturn("content");
		SendRawEmailRequest emailRequest = builder.buildEmailForNonSubscriber(user);
		assertNotNull(emailRequest);
	}
}
