package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.model.subscription.Subscriber;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public class DiscussionBroadcastMessageBuilderTest {
	@Mock
	MarkdownDao mockMarkdownDao;

	String actorUsername;
	String actorUserId;
	String threadTitle;
	String threadId;
	String projectName;
	String projectId;
	String markdown;
	Subscriber subscriber;
	
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
	
		builder = new DiscussionBroadcastMessageBuilder(actorUsername, actorUserId,
				threadTitle, threadId, projectId, projectName, markdown,
				ThreadMessageBuilderFactory.THREAD_TEMPLATE, ThreadMessageBuilderFactory.THREAD_CREATED_TITLE,
				mockMarkdownDao);
	}

	@Test
	public void testBuildRawBody(){
		String body = builder.buildRawBody(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("someone"));
		assertTrue(body.contains(threadTitle));
		assertTrue(body.contains(projectName));
		assertTrue(body.contains("https://www.synapse.org/#!Subscription:subscriptionID=999"));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:syn8888/discussion/threadId=333"));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:syn8888"));
	}

	@Test
	public void testBuildRawBodyWithSynapseWidget(){
		markdown = "Seen you eyes son show.\n@kimyen\n${jointeam?teamId=3319496&isChallenge=false&"
				+ "isSimpleRequestButton=false&isMemberMessage=Already a member&successMessage=Successfully "
				+ "joined&text=Join&requestOpenText=Your request to join this team has been sent%2E}";
		builder = new DiscussionBroadcastMessageBuilder(actorUsername, actorUserId,
				threadTitle, threadId, projectId, projectName, markdown,
				ThreadMessageBuilderFactory.THREAD_TEMPLATE, ThreadMessageBuilderFactory.THREAD_CREATED_TITLE,
				mockMarkdownDao);
		String body = builder.buildRawBody(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("someone"));
		assertTrue(body.contains(threadTitle));
		assertTrue(body.contains(projectName));
		assertTrue(body.contains("https://www.synapse.org/#!Subscription:subscriptionID=999"));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:syn8888/discussion/threadId=333"));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:syn8888"));
		assertTrue(body.contains("Seen you eyes son show.\n>@kimyen\n>${jointeam?teamId=3319496&isChallenge=false&"
				+ "isSimpleRequestButton=false&isMemberMessage=Already a member&successMessage=Successfully "
				+ "joined&text=Join&requestOpenText=Your request to join this team has been sent%2E}"));
	}
	
	@Test
	public void testBuildEmailForSubscriber(){
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
}
