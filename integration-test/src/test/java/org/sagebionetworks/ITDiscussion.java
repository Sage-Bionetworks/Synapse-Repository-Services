package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
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
		StackConfiguration config = StackConfigurationSingleton.singleton();
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(config.getMigrationAdminUsername());
		adminSynapse.setApiKey(config.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
	}

	@Before
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
		project = new Project();
		project = synapse.createEntity(project);
		assertNotNull(project);
		projectId = project.getId();
	}

	@After
	public void cleanup() throws SynapseException, JSONObjectAdapterException {
		if (project != null) adminSynapse.deleteEntity(project, true);
		synapse.unsubscribeAll();
	}

	@AfterClass
	public static void tearDown() throws SynapseException, JSONObjectAdapterException {
		if (userToDelete != null) adminSynapse.deleteUser(userToDelete);
	}

	@Test
	public void testForum() throws SynapseException {
		// create a forum
		Forum forum = synapse.getForumByProjectId(projectId);
		assertNotNull(forum);
		String forumId = forum.getId();
		assertNotNull(forumId);
		assertEquals(forum.getProjectId(), projectId);
		assertEquals(forum, synapse.getForum(forumId));

		// get forum moderators
		PaginatedIds moderators = synapse.getModeratorsForForum(forum.getId(), 10L, 0L);
		assertNotNull(moderators);
		assertTrue(moderators.getResults().contains(userToDelete.toString()));
		assertEquals((Long)1L, moderators.getTotalNumberOfResults());

		// get all threads in the forum
		PaginatedResults<DiscussionThreadBundle> threads = synapse.getThreadsForForum(forumId, 20L, 0L, null, null, DiscussionFilter.NO_FILTER);
		assertTrue(threads.getResults().isEmpty());
		assertEquals(0L, threads.getTotalNumberOfResults());
		assertEquals((Long)0L, synapse.getThreadCountForForum(forumId, DiscussionFilter.NO_FILTER).getCount());

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
		threads = synapse.getThreadsForForum(forumId, 20L, 0L, null, null, DiscussionFilter.NO_FILTER);
		assertTrue(threads.getResults().size() == 1);
		// etag has changed
		bundle.setEtag(null);
		threads.getResults().get(0).setEtag(null);
		assertEquals(threads.getResults().get(0), bundle);
		assertEquals(1L, threads.getTotalNumberOfResults());
		assertEquals((Long)1L, synapse.getThreadCountForForum(forumId, DiscussionFilter.NO_FILTER).getCount());

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

		// Pin the thread
		synapse.pinThread(threadId);
		DiscussionThreadBundle pinned = synapse.getThread(threadId);
		assertTrue(pinned.getIsPinned());

		// Unpin the thread
		synapse.unpinThread(threadId);
		DiscussionThreadBundle unpinned = synapse.getThread(threadId);
		assertFalse(unpinned.getIsPinned());

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
		PaginatedResults<DiscussionReplyBundle> replies = synapse.getRepliesForThread(threadId, 20L, 0L, null, null, DiscussionFilter.NO_FILTER);
		assertTrue(replies.getResults().size() == 1);
		assertEquals(replies.getResults().get(0), replyBundle);
		assertEquals(1L, replies.getTotalNumberOfResults());
		assertEquals((Long)1L, synapse.getReplyCountForThread(threadId, DiscussionFilter.NO_FILTER).getCount());

		assertNotNull(synapse.getReplyUrl(replyBundle.getMessageKey()));

		// update message
		UpdateReplyMessage updateReplyMessage = new UpdateReplyMessage();
		updateReplyMessage.setMessageMarkdown("Maybe the vet can help?");
		DiscussionReplyBundle updatedReply = synapse.updateReplyMessage(replyId, updateReplyMessage);
		assertEquals(updatedReply.getId(), replyId);
		assertTrue(updatedReply.getIsEdited());

		PaginatedResults<DiscussionReplyBundle> availableReplies = synapse.getRepliesForThread(threadId, 20L, 0L, null, null, DiscussionFilter.EXCLUDE_DELETED);
		assertEquals(1, availableReplies.getTotalNumberOfResults());
		assertEquals(availableReplies.getResults().get(0).getId(), replyId);

		// delete reply
		synapse.markReplyAsDeleted(replyId);
		PaginatedResults<DiscussionReplyBundle> deletedReplies = synapse.getRepliesForThread(threadId, 20L, 0L, null, null, DiscussionFilter.DELETED_ONLY);
		assertEquals(1, deletedReplies.getTotalNumberOfResults());
		assertEquals(deletedReplies.getResults().get(0).getId(), replyId);

		availableReplies = synapse.getRepliesForThread(threadId, 20L, 0L, null, null, DiscussionFilter.EXCLUDE_DELETED);
		assertEquals(0, availableReplies.getTotalNumberOfResults());
		assertEquals((Long)0L, synapse.getReplyCountForThread(threadId, DiscussionFilter.EXCLUDE_DELETED).getCount());

		PaginatedResults<DiscussionThreadBundle> availableThreads = synapse.getThreadsForForum(forumId, 20L, 0L, null, null, DiscussionFilter.EXCLUDE_DELETED);
		assertEquals(1, availableThreads.getTotalNumberOfResults());
		assertEquals(availableThreads.getResults().get(0).getId(), threadId);
		assertEquals((Long)1L, synapse.getReplyCountForThread(threadId, DiscussionFilter.NO_FILTER).getCount());

		// delete thread
		synapse.markThreadAsDeleted(threadId);

		PaginatedResults<DiscussionThreadBundle> deletedThreads = synapse.getThreadsForForum(forumId, 20L, 0L, null, null, DiscussionFilter.DELETED_ONLY);
		assertEquals(1, deletedThreads.getTotalNumberOfResults());
		assertEquals(deletedThreads.getResults().get(0).getId(), threadId);

		availableThreads = synapse.getThreadsForForum(forumId, 20L, 0L, null, null, DiscussionFilter.EXCLUDE_DELETED);
		assertEquals(0, availableThreads.getTotalNumberOfResults());
		assertEquals((Long)1L, synapse.getThreadCountForForum(forumId, DiscussionFilter.NO_FILTER).getCount());

		// restore deleted thread
		synapse.restoreDeletedThread(threadId);
		assertFalse(synapse.getThread(threadId).getIsDeleted());
	}

	@Test
	public void testEntityReferences() throws SynapseException {
		PaginatedResults<DiscussionThreadBundle> results = synapse.getThreadsForEntity(projectId, 20L, 0L, DiscussionThreadOrder.NUMBER_OF_VIEWS, true, DiscussionFilter.EXCLUDE_DELETED);
		assertNotNull(results);
		assertTrue(results.getResults().isEmpty());
		assertEquals(0L, results.getTotalNumberOfResults());
	}

	@Test
	public void testThreadCountsForEntityIdList() throws SynapseException {
		// create a forum
		Forum forum = synapse.getForumByProjectId(projectId);
		String forumId = forum.getId();
		// create a thread
		CreateDiscussionThread toCreate = new CreateDiscussionThread();
		toCreate.setForumId(forumId);
		String title = "Mention project";
		toCreate.setTitle(title);
		String message = projectId;
		toCreate.setMessageMarkdown(message);
		synapse.createThread(toCreate);

		EntityThreadCounts results = synapse.getEntityThreadCount(Arrays.asList(projectId));
		assertNotNull(results);
		assertEquals(1L, results.getList().size());
		assertEquals(projectId, results.getList().get(0).getEntityId());
		assertEquals((Long)1L, results.getList().get(0).getCount());
	}
}
