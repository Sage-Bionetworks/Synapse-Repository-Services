package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.junit.Assert.*;
import static org.sagebionetworks.repo.model.dbo.dao.discussion.DBODiscussionReplyDAOImpl.MAX_LIMIT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadAuthorStat;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadReplyStat;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBODiscussionReplyDAOImplTest {

	@Autowired
	private ForumDAO forumDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private DiscussionReplyDAO replyDao;
	@Autowired
	private IdGenerator idGenerator;

	private Long userId = null;
	private String projectId = null;
	private String forumId;
	private String threadId;
	private Long threadIdLong;
	private List<Long> usersToDelete = new ArrayList<Long>();

	@Before
	public void before() {
		// create a user to create a project
		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		userId = userGroupDAO.create(user);
		usersToDelete.add(userId);
		// create a project
		Node project = NodeTestUtils.createNew("projectName" + "-" + new Random().nextInt(), userId);
		project.setParentId(StackConfiguration.getRootFolderEntityIdStatic());
		projectId = nodeDao.createNew(project);
		// create a forum
		Forum dto = forumDao.createForum(projectId);
		forumId = dto.getId();
		// create a thread
		threadIdLong = idGenerator.generateNewId(TYPE.DISCUSSION_THREAD_ID);
		threadId = threadIdLong.toString();
		threadDao.createThread(forumId, threadId, "title", "messageKey", userId);
	}

	@After
	public void after() {
		if (projectId != null) nodeDao.delete(projectId);
		for (Long userId: usersToDelete) {
			if (userId != null) {
				userGroupDAO.delete(userId.toString());
			}
		}
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateReplyWithNullThreadId() {
		replyDao.createReply(null, "messageKey", userId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateReplyWithNullMessageKey() {
		replyDao.createReply(threadId, null, userId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateReplyWithNullUserId() {
		replyDao.createReply(threadId, "messageKey", null);
	}

	@Test
	public void testCreate() {
		String messageKey = "messageKey";
		DiscussionReplyBundle dto = replyDao.createReply(threadId, messageKey, userId);
		assertNotNull(dto);
		assertEquals(threadId, dto.getThreadId());
		assertEquals(forumId, dto.getForumId());
		assertEquals(projectId, dto.getProjectId());
		assertEquals(messageKey, dto.getMessageKey());
		assertEquals(userId.toString(), dto.getCreatedBy());
		assertFalse(dto.getIsEdited());
		assertFalse(dto.getIsDeleted());
		assertNotNull(dto.getId());
		assertNotNull(dto.getEtag());
		Long replyId = Long.parseLong(dto.getId());
		assertEquals(dto, replyDao.getReply(replyId));
	}

	@Test
	public void testGetReplyCount() {
		assertEquals(0L, replyDao.getReplyCount(threadIdLong));
		replyDao.createReply(threadId, "messageKey", userId);
		assertEquals(1L, replyDao.getReplyCount(threadIdLong));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNullThreadId() {
		replyDao.getRepliesForThread(null, 1L, 0L, null, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNullLimit() {
		replyDao.getRepliesForThread(threadIdLong, null, 0L, null, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNullOffset() {
		replyDao.getRepliesForThread(threadIdLong, 1L, null, null, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNegativeLimit() {
		replyDao.getRepliesForThread(threadIdLong, -1L, 0l, null, null);
	}
	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNegativeOffset() {
		replyDao.getRepliesForThread(threadIdLong, 1L, -1l, null, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithLimitOverMax() {
		replyDao.getRepliesForThread(threadIdLong, MAX_LIMIT+1, 0l, null, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNullOrderNotNullAscending() {
		replyDao.getRepliesForThread(threadIdLong, 1L, 0l, null, true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNotNullOrderNullAscending() {
		replyDao.getRepliesForThread(threadIdLong, 1L, 0l, DiscussionReplyOrder.CREATED_ON, null);
	}

	@Test
	public void testGetRepliesForThreadWithZeroExistingReplies() {
		PaginatedResults<DiscussionReplyBundle> results = replyDao.getRepliesForThread(threadIdLong, MAX_LIMIT, 0L, null, null);
		assertNotNull(results);
		assertEquals(0L, results.getTotalNumberOfResults());
		assertTrue(results.getResults().isEmpty());
	}

	@Test
	public void getRepliesForThreadLimitAndOffsetTest() throws InterruptedException {
		int numberOfReplies = 3;
		List<DiscussionReplyBundle> createdReplies = createReplies(numberOfReplies, threadId);

		PaginatedResults<DiscussionReplyBundle> results = replyDao.getRepliesForThread(threadIdLong, MAX_LIMIT, 0L, null, null);
		assertNotNull(results);
		assertEquals("unordered replies", numberOfReplies, results.getTotalNumberOfResults());
		assertEquals("unordered replies",
				new HashSet<DiscussionReplyBundle>(results.getResults()),
				new HashSet<DiscussionReplyBundle>(createdReplies));

		results = replyDao.getRepliesForThread(threadIdLong, MAX_LIMIT, 0L, DiscussionReplyOrder.CREATED_ON, true);
		assertEquals("ordered replies", numberOfReplies, results.getTotalNumberOfResults());
		assertEquals("ordered replies", createdReplies, results.getResults());

		results = replyDao.getRepliesForThread(threadIdLong, 1L, 1L, DiscussionReplyOrder.CREATED_ON, true);
		assertEquals("middle element", numberOfReplies, results.getTotalNumberOfResults());
		assertEquals("middle element", createdReplies.get(1), results.getResults().get(0));

		results = replyDao.getRepliesForThread(threadIdLong, MAX_LIMIT, 3L, DiscussionReplyOrder.CREATED_ON, true);
		assertEquals("out of range", numberOfReplies, results.getTotalNumberOfResults());
		assertTrue("out of range", results.getResults().isEmpty());
	}

	@Test
	public void getRepliesForThreadDescendingTest() throws InterruptedException {
		int numberOfReplies = 3;
		List<DiscussionReplyBundle> createdReplies = createReplies(numberOfReplies, threadId);

		PaginatedResults<DiscussionReplyBundle> results =
				replyDao.getRepliesForThread(threadIdLong, MAX_LIMIT, 0L, DiscussionReplyOrder.CREATED_ON, false);
		assertEquals("ordered desc replies", numberOfReplies, results.getTotalNumberOfResults());
		Collections.reverse(createdReplies);
		assertEquals("ordered desc replies", createdReplies, results.getResults());
	}

	private List<DiscussionReplyBundle> createReplies(int numberOfReplies, String threadId) throws InterruptedException {
		List<DiscussionReplyBundle> list = new ArrayList<DiscussionReplyBundle>();
		for (int i = 0; i < numberOfReplies; i++) {
			Thread.sleep(1000);
			list.add(replyDao.createReply(threadId, UUID.randomUUID().toString(), userId));
		}
		return list;
	}

	@Test
	public void testGetEtag(){
		DiscussionReplyBundle dto = replyDao.createReply(threadId, "messageKey", userId);
		long replyId = Long.parseLong(dto.getId());
		String etag = replyDao.getEtagForUpdate(replyId);
		assertNotNull(etag);
		assertEquals(dto.getEtag(), etag);
	}

	@Test
	public void testDelete(){
		DiscussionReplyBundle dto = replyDao.createReply(threadId, "messageKey", userId);
		long replyId = Long.parseLong(dto.getId());

		dto.setIsDeleted(true);
		replyDao.markReplyAsDeleted(replyId);
		DiscussionReplyBundle returnedDto = replyDao.getReply(replyId);
		assertFalse("after marking reply as deleted, etag should be different",
				dto.getEtag().equals(returnedDto.getEtag()));
		dto.setModifiedOn(returnedDto.getModifiedOn());
		dto.setEtag(returnedDto.getEtag());
		assertEquals(dto, returnedDto);
	}

	@Test
	public void testUpdateMessageKey() throws InterruptedException {
		DiscussionReplyBundle dto = replyDao.createReply(threadId, "messageKey", userId);
		long replyId = Long.parseLong(dto.getId());

		Thread.sleep(1000);
		dto.setIsEdited(true);
		String newMessageKey = UUID.randomUUID().toString();
		dto.setMessageKey(newMessageKey);
		replyDao.updateMessageKey(replyId, newMessageKey);
		DiscussionReplyBundle returnedDto = replyDao.getReply(replyId);
		assertFalse("after updating message key, modifiedOn should be different",
				dto.getModifiedOn().equals(returnedDto.getModifiedOn()));
		assertFalse("after updating message key, etag should be different",
				dto.getEtag().equals(returnedDto.getEtag()));
		dto.setModifiedOn(returnedDto.getModifiedOn());
		dto.setEtag(returnedDto.getEtag());
		assertEquals(dto, returnedDto);
	}

	@Test
	public void testGetThreadReplyStats() throws InterruptedException {
		// create another thread
		Long threadIdLong2 = idGenerator.generateNewId(TYPE.DISCUSSION_THREAD_ID);
		String threadId2 = threadIdLong2.toString();
		threadDao.createThread(forumId, threadId2, "title", "messageKey2", userId);

		int numberOfReplies = 2;
		// create 2 replies for each thread
		createReplies(numberOfReplies, threadId);
		createReplies(numberOfReplies, threadId2);

		List<DiscussionThreadReplyStat> stats = replyDao.getThreadReplyStat(10L, 0L);
		assertNotNull(stats);
		assertEquals(stats.size(), 2);
		DiscussionThreadReplyStat stat1 = stats.get(0);
		DiscussionThreadReplyStat stat2 = stats.get(1);
		assertEquals(stat1.getThreadId(), threadIdLong);
		assertEquals(stat2.getThreadId(), threadIdLong2);
		assertEquals(stat1.getNumberOfReplies(), (Long) 2L);
		assertEquals(stat2.getNumberOfReplies(), (Long) 2L);
		assertNotNull(stat1.getLastActivity());
		assertNotNull(stat2.getLastActivity());
	}

	@Test
	public void testGetThreadAuthorStats() throws InterruptedException {
		DiscussionThreadAuthorStat stat = replyDao.getDiscussionThreadAuthorStat(threadIdLong);
		assertNotNull(stat);
		assertEquals(stat.getThreadId(), threadIdLong);
		assertTrue(stat.getActiveAuthors().isEmpty());

		List<Long> users = createUsers(6);
		replyDao.createReply(threadId, UUID.randomUUID().toString(), users.get(0));
		replyDao.createReply(threadId, UUID.randomUUID().toString(), users.get(0));
		replyDao.createReply(threadId, UUID.randomUUID().toString(), users.get(1));

		stat = replyDao.getDiscussionThreadAuthorStat(threadIdLong);
		assertEquals(stat.getActiveAuthors(),
				Arrays.asList(users.get(0).toString(), users.get(1).toString()));

		replyDao.createReply(threadId, UUID.randomUUID().toString(), users.get(1));
		replyDao.createReply(threadId, UUID.randomUUID().toString(), users.get(2));
		replyDao.createReply(threadId, UUID.randomUUID().toString(), users.get(2));
		replyDao.createReply(threadId, UUID.randomUUID().toString(), users.get(3));
		replyDao.createReply(threadId, UUID.randomUUID().toString(), users.get(3));
		replyDao.createReply(threadId, UUID.randomUUID().toString(), users.get(4));
		replyDao.createReply(threadId, UUID.randomUUID().toString(), users.get(4));
		replyDao.createReply(threadId, UUID.randomUUID().toString(), users.get(5));

		stat = replyDao.getDiscussionThreadAuthorStat(threadIdLong);
		assertEquals(stat.getActiveAuthors(),
				Arrays.asList(users.get(0).toString(),
						users.get(1).toString(), users.get(2).toString(),
						users.get(3).toString(), users.get(4).toString()));

		usersToDelete.addAll(users);
	}

	private List<Long> createUsers(int numberOfUsers) {
		List<Long> createdUsers = new ArrayList<Long>();
		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		for (int i = 0; i < numberOfUsers; i++) {
			createdUsers.add(userGroupDAO.create(user));
		}
		return createdUsers;
	}
}
