package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyCollectionOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.markdown.MarkdownClientException;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.broadcast.UserNotificationInfo;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
public class DiscussionBroadcastMessageBuilderTest {
	@Mock
	MarkdownDao mockMarkdownDao;
	@Mock
	UserManager mockUserManager;

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
				ThreadMessageBuilderFactory.UNSUBSCRIBE_FORUM, mockMarkdownDao, topic, mockUserManager);
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
		assertTrue(body.contains("Unsubscribe from the forum"));
	}

	@Test
	public void testBuildReplyRawBodyForSubscriber(){
		topic.setObjectType(SubscriptionObjectType.THREAD);
		builder = new DiscussionBroadcastMessageBuilder(actorUsername, actorUserId,
				threadTitle, threadId, projectId, projectName, markdown,
				ReplyMessageBuilderFactory.REPLY_TEMPLATE, ReplyMessageBuilderFactory.REPLY_CREATED_TITLE,
				ReplyMessageBuilderFactory.UNSUBSCRIBE_THREAD, mockMarkdownDao, topic, mockUserManager);
	
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
		assertTrue(body.contains("Unsubscribe from the thread"));
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
		assertFalse(body.contains("Unsubscribe from the forum"));
	}

	@Test
	public void testBuildRawBodyForSubscriberWithSynapseWidget(){
		markdown = "Seen you eyes son show.\n@kimyen\n${jointeam?teamId=3319496&isChallenge=false&"
				+ "isSimpleRequestButton=false&isMemberMessage=Already a member&successMessage=Successfully "
				+ "joined&text=Join&requestOpenText=Your request to join this team has been sent%2E}";
		builder = new DiscussionBroadcastMessageBuilder(actorUsername, actorUserId,
				threadTitle, threadId, projectId, projectName, markdown,
				ThreadMessageBuilderFactory.THREAD_TEMPLATE, ThreadMessageBuilderFactory.THREAD_CREATED_TITLE,
				ThreadMessageBuilderFactory.UNSUBSCRIBE_FORUM, mockMarkdownDao, topic, mockUserManager);
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
	
	@Test (expected = MarkdownClientException.class)
	public void testBuildEmailForSubscriberFailure() throws Exception{
		when(mockMarkdownDao.convertMarkdown(anyString(), isNull())).thenThrow(new MarkdownClientException(500, ""));
		builder.buildEmailForSubscriber(subscriber);
	}
	
	@Test
	public void testBuildEmailForSubscriberSuccess() throws Exception{
		when(mockMarkdownDao.convertMarkdown(anyString(), isNull())).thenReturn("content");
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
	public void testGetRelatedUsersOverAtLimit(){
		long count = DiscussionBroadcastMessageBuilder.MAX_USER_IDS_PER_MESSAGE;
		Set<String> set = new HashSet<String>((int)count);
		for(int i=0; i<count; i++){
			set.add(""+i);
		}
		when(mockUserManager.getDistinctUserIdsForAliases(anyCollectionOf(String.class), anyLong(), anyLong())).thenReturn(set);
		// call under test
		Set<String> results = builder.getRelatedUsers();
		assertEquals(set, results);
	}
	
	@Test 
	public void testGetRelatedUsersOverLimit(){
		long count = DiscussionBroadcastMessageBuilder.MAX_USER_IDS_PER_MESSAGE+1;
		Set<String> set = new HashSet<String>((int)count);
		for(int i=0; i<count; i++){
			set.add(""+i);
		}
		when(mockUserManager.getDistinctUserIdsForAliases(anyCollectionOf(String.class), anyLong(), anyLong())).thenReturn(set);
		// call under test
		try {
			builder.getRelatedUsers();
			fail();
		} catch (IllegalArgumentException e) {
			// expected
			assertTrue(e.getMessage().contains(""+DiscussionBroadcastMessageBuilder.MAX_USER_IDS_PER_MESSAGE));
		}
		// validate paging
		verify(mockUserManager).getDistinctUserIdsForAliases(anyCollectionOf(String.class), eq(DiscussionBroadcastMessageBuilder.MAX_USER_IDS_PER_MESSAGE+1), eq(0L));
	}

	@Test
	public void testGetRelatedUsersWithMentionedUsers() {
		Set<String> usernameSet = new HashSet<String>();
		usernameSet.add("user");
		Set<String> userIdSet = new HashSet<String>();
		userIdSet.add("101");
		Set<String> idSet = Sets.newHashSet("101");
		when(mockUserManager.getDistinctUserIdsForAliases(anyCollectionOf(String.class), anyLong(), anyLong())).thenReturn(idSet);
		builder = new DiscussionBroadcastMessageBuilder(actorUsername, actorUserId,
				threadTitle, threadId, projectId, projectName, "@user",
				ThreadMessageBuilderFactory.THREAD_TEMPLATE, ThreadMessageBuilderFactory.THREAD_CREATED_TITLE,
				ThreadMessageBuilderFactory.UNSUBSCRIBE_FORUM, mockMarkdownDao, topic, mockUserManager);
		assertEquals(userIdSet, builder.getRelatedUsers());
	}

	@Test
	public void testBuildEmailForNonSubscriber() throws Exception{
		when(mockMarkdownDao.convertMarkdown(anyString(), isNull())).thenReturn("content");
		SendRawEmailRequest emailRequest = builder.buildEmailForNonSubscriber(user);
		assertNotNull(emailRequest);
	}
}
