package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.subscription.Subscriber;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public class ThreadBroadcastMessageBuilderTest {

	DiscussionThreadBundle threadBundle;
	EntityHeader projectHeader;
	ChangeType changeType;
	String threadUsername;
	Subscriber subscriber;
	
	ThreadBroadcastMessageBuilder builder;
	
	@Before
	public void before(){
		threadBundle = new DiscussionThreadBundle();
		threadBundle.setId("333");
		threadBundle.setTitle("thread-title");
		projectHeader = new EntityHeader();
		projectHeader.setId("syn8888");
		projectHeader.setName("projectName");
		changeType = ChangeType.CREATE;
		threadUsername = "thread-username";
	
		subscriber = new Subscriber();
		subscriber.setFirstName("subscriberFirstName");
		subscriber.setLastName("subscriberLastName");
		subscriber.setNotificationEmail("subsciber@domain.org");
		subscriber.setSubscriberId("123");
		subscriber.setUsername("subscriberUsername");
		subscriber.setSubscriptionId("999");
	
		builder = new ThreadBroadcastMessageBuilder(threadBundle, projectHeader, changeType, threadUsername);
	}
	
	@Test
	public void testSubjectCreate(){
		String title = "A-title-that-is-too-long-to-show-so-we-truncate-it-to-a-much-smaller-string";
		String subject = ThreadBroadcastMessageBuilder.buildSubject(title, ChangeType.CREATE);
		assertEquals("Synapse Notification: New thread 'A-title-that-is-too-long-to-show-so-we-truncate-it...'", subject);
	}
	
	@Test
	public void testSubjectUpdate(){
		String title = "A-title";
		String subject = ThreadBroadcastMessageBuilder.buildSubject(title, ChangeType.UPDATE);
		assertEquals("Synapse Notification: 'A-title' thread has been updated", subject);
	}
	
	@Test
	public void testSubjectDelete(){
		String title = "A-title";
		String subject = ThreadBroadcastMessageBuilder.buildSubject(title, ChangeType.DELETE);
		assertEquals("Synapse Notification: 'A-title' thread has been removed", subject);
	}
	
	@Test
	public void testBuildBody(){
		String body = builder.buildBody(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("thread-username"));
		assertTrue(body.contains(threadBundle.getTitle()));
		assertTrue(body.contains(projectHeader.getName()));
		assertTrue(body.contains("https://www.synapse.org/#!Subscription:subscriptionID=999"));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:syn8888/discussion/threadId=333"));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:syn8888"));
		assertTrue(body.contains("created"));
	}
	
	@Test
	public void testBuildEmailForSubscriber(){
		SendRawEmailRequest request = builder.buildEmailForSubscriber(subscriber);
		assertNotNull(request);
	}

}
