package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
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
	public void test() throws SynapseException {
		// create a forum
		Forum forum = synapse.getForumMetadata(projectId);
		assertNotNull(forum);
		String forumId = forum.getId();
		assertNotNull(forumId);
		assertEquals(forum.getProjectId(), projectId);

		// get all threads in the forum
		PaginatedResults<DiscussionThreadBundle> threads = synapse.getThreadsForForum(forumId, 100L, 0L, null, null, DiscussionFilter.NO_FILTER);
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
		threads = synapse.getThreadsForForum(forumId, 100L, 0L, null, null, DiscussionFilter.NO_FILTER);
		assertTrue(threads.getResults().size() == 1);
		assertEquals(threads.getResults().get(0), bundle);
		assertEquals(1L, threads.getTotalNumberOfResults());
	
		assertNotNull(synapse.getThreadUrl(bundle.getMessageKey()));

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
		PaginatedResults<DiscussionReplyBundle> replies = synapse.getRepliesForThread(threadId, 100L, 0L, null, null, DiscussionFilter.NO_FILTER);
		assertTrue(replies.getResults().size() == 1);
		assertEquals(replies.getResults().get(0), replyBundle);
		assertEquals(1L, replies.getTotalNumberOfResults());

		assertNotNull(synapse.getReplyUrl(replyBundle.getMessageKey()));

		// update message
		UpdateReplyMessage updateReplyMessage = new UpdateReplyMessage();
		updateReplyMessage.setMessageMarkdown("Maybe the vet can help?");
		DiscussionReplyBundle updatedReply = synapse.updateReplyMessage(replyId, updateReplyMessage);
		assertEquals(updatedReply.getId(), replyId);
		assertTrue(updatedReply.getIsEdited());

		PaginatedResults<DiscussionReplyBundle> availableReplies = synapse.getRepliesForThread(threadId, 100L, 0L, null, null, DiscussionFilter.NOT_DELETED_ONLY);
		assertEquals(1, availableReplies.getTotalNumberOfResults());
		assertEquals(availableReplies.getResults().get(0).getId(), bundle.getId());

		// delete reply
		synapse.markReplyAsDeleted(replyId);
		PaginatedResults<DiscussionReplyBundle> deletedReplies = synapse.getRepliesForThread(threadId, 100L, 0L, null, null, DiscussionFilter.DELETED_ONLY);
		assertEquals(1, deletedReplies.getTotalNumberOfResults());
		assertEquals(deletedReplies.getResults().get(0).getId(), replyId);

		availableReplies = synapse.getRepliesForThread(threadId, 100L, 0L, null, null, DiscussionFilter.NOT_DELETED_ONLY);
		assertEquals(0, availableReplies.getTotalNumberOfResults());

		PaginatedResults<DiscussionThreadBundle> availableThreads = synapse.getThreadsForForum(forumId, 100L, 0L, null, null, DiscussionFilter.NOT_DELETED_ONLY);
		assertEquals(1, availableThreads.getTotalNumberOfResults());
		assertEquals(availableThreads.getResults().get(0).getId(), bundle.getId());

		// delete thread
		synapse.markThreadAsDeleted(threadId);

		PaginatedResults<DiscussionThreadBundle> deletedThreads = synapse.getThreadsForForum(forumId, 100L, 0L, null, null, DiscussionFilter.DELETED_ONLY);
		assertEquals(1, deletedThreads.getTotalNumberOfResults());
		assertEquals(deletedThreads.getResults().get(0).getId(), bundle.getId());

		availableThreads = synapse.getThreadsForForum(forumId, 100L, 0L, null, null, DiscussionFilter.NOT_DELETED_ONLY);
		assertEquals(0, availableThreads.getTotalNumberOfResults());
	}
}
