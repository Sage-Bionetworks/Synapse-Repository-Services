package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.repo.manager.message.DataAccessSubmissionStatusMessageBuilderFactory.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.markdown.MarkdownClientException;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public class DataAccessSubmissionStatusBroadcastMessageBuilderTest {
	@Mock
	MarkdownDao mockMarkdownDao;

	private Subscriber subscriber;
	private String objectId;
	private String requirementId;
	private String rejectedReason;
	
	DataAccessSubmissionStatusBroadcastMessageBuilder builder;

	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);

		requirementId = "1";
	
		subscriber = new Subscriber();
		subscriber.setFirstName("subscriberFirstName");
		subscriber.setLastName("subscriberLastName");
		subscriber.setNotificationEmail("subsciber@domain.org");
		subscriber.setSubscriberId("123");
		subscriber.setUsername("subscriberUsername");
		subscriber.setSubscriptionId("999");
	
		builder = new DataAccessSubmissionStatusBroadcastMessageBuilder(APPROVED_TITLE, APPROVED_TEMPLATE,
				objectId, null, requirementId, mockMarkdownDao, false);
	}

	@Test
	public void testBuildRawBodyForApprovedMessage(){
		String body = builder.buildRawBody(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("A member of the Synapse Access and Compliance Team has reviewed and approved your request."));
		assertTrue(body.contains("View your request"));
		// TODO: verify the link is correct
	}

	@Test
	public void testBuildRawBodyForRejectedMessage(){
		rejectedReason = "some reason";
		builder = new DataAccessSubmissionStatusBroadcastMessageBuilder(REJECTED_TITLE, REJECTED_TEMPLATE,
				objectId, rejectedReason, requirementId, mockMarkdownDao, true);
		String body = builder.buildRawBody(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("A member of the Synapse Access and Compliance Team has reviewed your request and left a comment:"));
		assertTrue(body.contains(rejectedReason));
		assertTrue(body.contains("your request"));
		// TODO: verify the link is correct
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
		assertEquals(SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS, topic.getObjectType());
		assertEquals(objectId, topic.getObjectId());
	}

	@Test
	public void testGetRelatedUsersWithoutMentionedUsers() {
		assertNull(builder.getRelatedUsers());
	}
}
