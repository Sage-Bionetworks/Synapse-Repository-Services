package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.repo.manager.message.DataAccessSubmissionMessageBuilderFactory.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.markdown.MarkdownClientException;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.manager.subscription.SubscriptionManagerImpl;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public class DataAccessSubmissionBroadcastMessageBuilderTest {
	@Mock
	MarkdownDao mockMarkdownDao;

	private String actorUsername;
	private String actorUserId;
	private Subscriber subscriber;
	
	DataAccessSubmissionBroadcastMessageBuilder builder;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);

		actorUsername = "someone";
		actorUserId = "1";
	
		subscriber = new Subscriber();
		subscriber.setFirstName("subscriberFirstName");
		subscriber.setLastName("subscriberLastName");
		subscriber.setNotificationEmail("subsciber@domain.org");
		subscriber.setSubscriberId("123");
		subscriber.setUsername("subscriberUsername");
		subscriber.setSubscriptionId("999");
	
		builder = new DataAccessSubmissionBroadcastMessageBuilder(TITLE, EMAIL_TEMPLATE,
				actorUsername, actorUserId, UNSUBSCRIBE, mockMarkdownDao);
	}

	@Test
	public void testBuildRawBody(){
		String body = builder.buildRawBody(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("someone"));
		assertTrue(body.contains("Access Requirement Manager page"));
		assertTrue(body.contains("Unsubscribe from Data Access Submission"));
		// TODO: verify links are correct
	}
	
	@Test (expected = MarkdownClientException.class)
	public void testBuildEmailFailure() throws Exception{
		when(mockMarkdownDao.convertMarkdown(anyString(), anyString())).thenThrow(new MarkdownClientException(500, ""));
		builder.buildEmailForSubscriber(subscriber);
	}
	
	@Test
	public void testBuildEmailSuccess() throws Exception{
		when(mockMarkdownDao.convertMarkdown(anyString(), anyString())).thenReturn("content");
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
