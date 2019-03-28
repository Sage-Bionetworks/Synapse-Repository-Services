package org.sagebionetworks.repo.manager.message.dataaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.markdown.MarkdownClientException;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.manager.subscription.SubscriptionManagerImpl;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

@RunWith(MockitoJUnitRunner.class)
public class SubmissionBroadcastMessageBuilderTest {
	@Mock
	MarkdownDao mockMarkdownDao;

	private String actorUsername;
	private String actorUserId;
	private String requirementId;
	private Subscriber subscriber;
	
	SubmissionBroadcastMessageBuilder builder;
	
	@Before
	public void before(){
		actorUsername = "someone";
		actorUserId = "1";
		requirementId = "2";
	
		subscriber = new Subscriber();
		subscriber.setFirstName("subscriberFirstName");
		subscriber.setLastName("subscriberLastName");
		subscriber.setNotificationEmail("subsciber@domain.org");
		subscriber.setSubscriberId("123");
		subscriber.setUsername("subscriberUsername");
		subscriber.setSubscriptionId("999");
	
		builder = new SubmissionBroadcastMessageBuilder(actorUsername, actorUserId, requirementId, mockMarkdownDao);
	}

	@Test
	public void testBuildRawBody(){
		String body = builder.buildRawBody(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("someone"));
		assertTrue(body.contains("Access Requirement Manager page"));
		assertTrue(body.contains("https://www.synapse.org/#!ACTDataAccessSubmissions:AR_ID=2"));
		assertTrue(body.contains("Unsubscribe from Data Access Submission"));
		assertTrue(body.contains("https://www.synapse.org/#!Subscription:subscriptionID=999"));
	}
	
	@Test (expected = MarkdownClientException.class)
	public void testBuildEmailFailure() throws Exception{
		when(mockMarkdownDao.convertMarkdown(anyString(), isNull())).thenThrow(new MarkdownClientException(500, ""));
		builder.buildEmailForSubscriber(subscriber);
	}
	
	@Test
	public void testBuildEmailSuccess() throws Exception{
		when(mockMarkdownDao.convertMarkdown(anyString(), isNull())).thenReturn("content");
		SendRawEmailRequest request = builder.buildEmailForSubscriber(subscriber);
		assertNotNull(request);
	}

	@Test
	public void testGetTopic() {
		Topic topic = builder.getBroadcastTopic();
		assertNotNull(topic);
		assertEquals(SubscriptionObjectType.DATA_ACCESS_SUBMISSION, topic.getObjectType());
		assertEquals(SubscriptionManagerImpl.ALL_OBJECT_IDS, topic.getObjectId());
	}

	@Test
	public void testGetRelatedUsersWithoutMentionedUsers() {
		assertNull(builder.getRelatedUsers());
	}
}
