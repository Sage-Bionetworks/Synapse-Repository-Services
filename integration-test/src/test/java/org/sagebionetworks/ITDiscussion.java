package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class ITDiscussion {

	private static SynapseClient synapse;
	private static SynapseAdminClient adminSynapse;
	private static Long userToDelete;
	private Project project;
	private String projectId;

	@BeforeClass
	public static void beforeClass() throws SynapseException, JSONObjectAdapterException {
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
	}

	@Before
	public void before() throws SynapseException {
		project = new Project();
		project = synapse.createEntity(project);
		assertNotNull(project);
		projectId = project.getId();
	}

	@After
	public void cleanup() throws SynapseException, JSONObjectAdapterException {
		if (project != null) adminSynapse.deleteEntity(project, true);
		if (userToDelete != null) adminSynapse.deleteUser(userToDelete);
	}

	@Test
	public void test() throws SynapseException, IOException {
		// create a forum
		Forum forum = synapse.getForumMetadata(projectId);
		assertNotNull(forum);
		String forumId = forum.getId();
		assertNotNull(forumId);
		assertEquals(forum.getProjectId(), projectId);

		// get all threads in the forum
		PaginatedResults<DiscussionThreadBundle> threads = synapse.getThreadsForForum(forumId, 100L, 0L, null, null);
		assertTrue(threads.getResults().isEmpty());
		assertEquals(0L, threads.getTotalNumberOfResults());

		// create a thread
		CreateDiscussionThread toCreate = new CreateDiscussionThread();
		toCreate.setForumId(forumId);
		String title = "Question about file missing";
		toCreate.setTitle(title);
		String message = "I think my cats ate my files. Please help!";
		toCreate.setMessageMarkdown(message);
		DiscussionThreadBundle bundle = synapse.createThread(toCreate);
		assertNotNull(bundle);
		String threadId = bundle.getId();
		assertEquals(bundle.getForumId(), forumId);
		assertEquals(bundle.getProjectId(), projectId);
		assertEquals(bundle.getTitle(), title);

		assertEquals(synapse.getThread(threadId), bundle);
		threads = synapse.getThreadsForForum(forumId, 100L, 0L, null, null);
		assertTrue(threads.getResults().size() == 1);
		assertEquals(threads.getResults().get(0), bundle);
		assertEquals(1L, threads.getTotalNumberOfResults());

		// get message URL
		URL url = synapse.getThreadMessageUrl(bundle.getMessageKey());
		assertNotNull(url);
		String returnedMessage = synapse.getThreadMessage(bundle.getMessageKey());
		assertNotNull(returnedMessage);
		assertEquals(returnedMessage, message);

		// update title
		UpdateThreadTitle updateTitle = new UpdateThreadTitle();
		updateTitle.setTitle("Need help with file missing");
		DiscussionThreadBundle updateThreadTitle = synapse.updateThreadTitle(threadId, updateTitle);
		assertFalse(updateThreadTitle.equals(bundle));
		assertEquals(updateThreadTitle.getId(), threadId);
		assertTrue(updateThreadTitle.getIsEdited());

		// update message
		UpdateThreadMessage updateMessage = new UpdateThreadMessage();
		updateMessage.setMessageMarkdown("My cats ate my files. Please help!");
		DiscussionThreadBundle updateThreadMessage = synapse.updateThreadMessage(threadId, updateMessage);
		assertFalse(updateThreadMessage.equals(updateThreadTitle));
		assertEquals(updateThreadMessage.getId(), threadId);
		assertTrue(updateThreadMessage.getIsEdited());

		// delete
		synapse.markThreadAsDeleted(threadId);
		DiscussionThreadBundle deleted = synapse.getThread(threadId);
		assertFalse(deleted.equals(updateThreadMessage));
		assertEquals(deleted.getId(), threadId);
		assertTrue(deleted.getIsDeleted());

		// create a reply
		CreateDiscussionReply replyToCreate = new CreateDiscussionReply();
		replyToCreate.setThreadId(threadId);
		message = "Bring the cat to the vet.";
		replyToCreate.setMessageMarkdown(message);
		DiscussionReplyBundle replyBundle = synapse.createReply(replyToCreate);
		assertNotNull(replyBundle);
		String replyId = replyBundle.getId();
		assertEquals(replyBundle.getThreadId(), threadId);
		assertEquals(replyBundle.getProjectId(), projectId);
		assertEquals(replyBundle.getForumId(), forumId);

		assertEquals(synapse.getReply(replyId), replyBundle);
		PaginatedResults<DiscussionReplyBundle> replies = synapse.getRepliesForThread(threadId, 100L, 0L, null, null);
		assertTrue(replies.getResults().size() == 1);
		assertEquals(replies.getResults().get(0), replyBundle);
		assertEquals(1L, replies.getTotalNumberOfResults());

		// get message URL
		url = synapse.getReplyMessageUrl(replyBundle.getMessageKey());
		assertNotNull(url);
		returnedMessage = synapse.getReplyMessage(replyBundle.getMessageKey());
		assertNotNull(returnedMessage);
		assertEquals(returnedMessage, message);

		// update message
		UpdateReplyMessage updateReplyMessage = new UpdateReplyMessage();
		updateReplyMessage.setMessageMarkdown("Maybe the vet can help?");
		DiscussionReplyBundle updatedReply = synapse.updateReplyMessage(replyId, updateReplyMessage);
		assertEquals(updatedReply.getId(), replyId);
		assertTrue(updatedReply.getIsEdited());

		// delete
		synapse.markReplyAsDeleted(replyId);
		DiscussionReplyBundle deletedReply = synapse.getReply(replyId);
		assertFalse(deletedReply.equals(updatedReply));
		assertEquals(deletedReply.getId(), replyId);
		assertTrue(deletedReply.getIsDeleted());
	}
}
