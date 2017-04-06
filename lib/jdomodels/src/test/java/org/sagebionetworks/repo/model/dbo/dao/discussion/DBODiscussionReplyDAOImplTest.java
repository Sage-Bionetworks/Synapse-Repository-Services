package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.junit.Assert.*;
import static org.sagebionetworks.repo.model.dbo.dao.discussion.DBODiscussionReplyDAOImpl.*;

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
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadReplyStat;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
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
	private Long replyIdLong;
	private String replyId;
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
		threadIdLong = idGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID);
		threadId = threadIdLong.toString();
		threadDao.createThread(forumId, threadId, "title", "messageKey", userId);
		replyIdLong = idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID);
		replyId = replyIdLong.toString();
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
		replyDao.createReply(null, replyId, "messageKey", userId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateReplyWithNullReplyId() {
		replyDao.createReply(threadId, null, "messageKey", userId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateReplyWithNullMessageKey() {
		replyDao.createReply(threadId, replyId, null, userId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateReplyWithNullUserId() {
		replyDao.createReply(threadId, replyId, "messageKey", null);
	}

	@Test
	public void testCreate() {
		String messageKey = "messageKey";
		DiscussionReplyBundle dto = replyDao.createReply(threadId, replyId, messageKey, userId);
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
		assertEquals(dto, replyDao.getReply(replyId, DEFAULT_FILTER));
	}

	@Test
	public void testGetProjectId() {
		DiscussionReplyBundle dto = replyDao.createReply(threadId, replyId, "messageKey", userId);
		assertEquals(projectId, replyDao.getProjectId(dto.getId()));
	}

	@Test (expected=NotFoundException.class)
	public void testGetProjectIdNotFound() {
		replyDao.getProjectId("-1");
	}

	@Test
	public void testGetReplyCount() {
		assertEquals(0L, replyDao.getReplyCount(threadIdLong, DiscussionFilter.NO_FILTER));
		replyDao.createReply(threadId, replyId, "messageKey", userId);
		assertEquals(1L, replyDao.getReplyCount(threadIdLong, DiscussionFilter.NO_FILTER));
	}

	@Test
	public void testGetReplyCountWithDeletedReplies() {
		assertEquals(0L, replyDao.getReplyCount(threadIdLong, DiscussionFilter.NO_FILTER));
		replyDao.createReply(threadId, replyId, "messageKey", userId);
		replyDao.markReplyAsDeleted(replyIdLong);
		assertEquals(1L, replyDao.getReplyCount(threadIdLong, DiscussionFilter.NO_FILTER));
		assertEquals(0L, replyDao.getReplyCount(threadIdLong, DiscussionFilter.EXCLUDE_DELETED));
		assertEquals(1L, replyDao.getReplyCount(threadIdLong, DiscussionFilter.DELETED_ONLY));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNullThreadId() {
		replyDao.getRepliesForThread(null, 1L, 0L, null, null, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNullLimit() {
		replyDao.getRepliesForThread(threadIdLong, null, 0L, null, null, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNullOffset() {
		replyDao.getRepliesForThread(threadIdLong, 1L, null, null, null, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNegativeLimit() {
		replyDao.getRepliesForThread(threadIdLong, -1L, 0l, null, null, DiscussionFilter.NO_FILTER);
	}
	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNegativeOffset() {
		replyDao.getRepliesForThread(threadIdLong, 1L, -1l, null, null, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithLimitOverMax() {
		replyDao.getRepliesForThread(threadIdLong, MAX_LIMIT+1, 0l, null, null, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNullOrderNotNullAscending() {
		replyDao.getRepliesForThread(threadIdLong, 1L, 0l, null, true, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNotNullOrderNullAscending() {
		replyDao.getRepliesForThread(threadIdLong, 1L, 0l, DiscussionReplyOrder.CREATED_ON, null, DiscussionFilter.NO_FILTER);
	}

	@Test
	public void testGetRepliesForThreadWithZeroExistingReplies() {
		List<DiscussionReplyBundle> results = replyDao.getRepliesForThread(threadIdLong, MAX_LIMIT, 0L, null, null, DiscussionFilter.NO_FILTER);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}

	@Test
	public void getRepliesForThreadLimitAndOffsetTest() throws InterruptedException {
		int numberOfReplies = 3;
		List<DiscussionReplyBundle> createdReplies = createReplies(numberOfReplies, threadId);

		List<DiscussionReplyBundle> results = replyDao.getRepliesForThread(threadIdLong, MAX_LIMIT, 0L, null, null, DiscussionFilter.NO_FILTER);
		assertNotNull(results);
		assertEquals("unordered replies",
				new HashSet<DiscussionReplyBundle>(results),
				new HashSet<DiscussionReplyBundle>(createdReplies));

		results = replyDao.getRepliesForThread(threadIdLong, MAX_LIMIT, 0L, DiscussionReplyOrder.CREATED_ON, true, DiscussionFilter.NO_FILTER);
		assertEquals("ordered replies", createdReplies, results);

		results = replyDao.getRepliesForThread(threadIdLong, 1L, 1L, DiscussionReplyOrder.CREATED_ON, true, DiscussionFilter.NO_FILTER);
		assertEquals("middle element", createdReplies.get(1), results.get(0));

		results = replyDao.getRepliesForThread(threadIdLong, MAX_LIMIT, 3L, DiscussionReplyOrder.CREATED_ON, true, DiscussionFilter.NO_FILTER);
		assertTrue("out of range", results.isEmpty());
	}

	@Test
	public void getRepliesForThreadDescendingTest() throws InterruptedException {
		int numberOfReplies = 3;
		List<DiscussionReplyBundle> createdReplies = createReplies(numberOfReplies, threadId);

		List<DiscussionReplyBundle> results =
				replyDao.getRepliesForThread(threadIdLong, MAX_LIMIT, 0L, DiscussionReplyOrder.CREATED_ON, false, DiscussionFilter.NO_FILTER);
		Collections.reverse(createdReplies);
		assertEquals("ordered desc replies", createdReplies, results);
	}

	@Test
	public void getRepliesForThreadTestWithDeletedReplies() throws InterruptedException{
		int numberOfReplies = 3;
		List<DiscussionReplyBundle> createdReplies = createReplies(numberOfReplies, threadId);

		List<DiscussionReplyBundle> nonDeletedReplies = replyDao.getRepliesForThread(threadIdLong, MAX_LIMIT, 0L, null, null, DiscussionFilter.NO_FILTER);
		List<DiscussionReplyBundle> includedeletedReplies = replyDao.getRepliesForThread(threadIdLong, MAX_LIMIT, 0L, null, null, DiscussionFilter.NO_FILTER);
		assertEquals(nonDeletedReplies, includedeletedReplies);

		replyDao.markReplyAsDeleted(Long.parseLong(createdReplies.get(1).getId()));

		nonDeletedReplies = replyDao.getRepliesForThread(threadIdLong, MAX_LIMIT, 0L, null, null, DiscussionFilter.EXCLUDE_DELETED);
		includedeletedReplies = replyDao.getRepliesForThread(threadIdLong, MAX_LIMIT, 0L, null, null, DiscussionFilter.NO_FILTER);
		assertFalse(nonDeletedReplies.equals(includedeletedReplies));
		assertEquals(nonDeletedReplies.size(), 2);
		assertEquals(includedeletedReplies.size(), 3);
		assertFalse(nonDeletedReplies.get(0).getId().equals(createdReplies.get(1).getId()));
		assertFalse(nonDeletedReplies.get(1).getId().equals(createdReplies.get(1).getId()));
	}

	private List<DiscussionReplyBundle> createReplies(int numberOfReplies, String threadId) throws InterruptedException {
		List<DiscussionReplyBundle> list = new ArrayList<DiscussionReplyBundle>();
		for (int i = 0; i < numberOfReplies; i++) {
			Thread.sleep(1000);
			Long replyId = idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID);
			list.add(replyDao.createReply(threadId, replyId.toString(), UUID.randomUUID().toString(), userId));
		}
		return list;
	}

	@Test
	public void testGetEtag(){
		DiscussionReplyBundle dto = replyDao.createReply(threadId, replyId, "messageKey", userId);
		long replyId = Long.parseLong(dto.getId());
		String etag = replyDao.getEtagForUpdate(replyId);
		assertNotNull(etag);
		assertEquals(dto.getEtag(), etag);
	}

	@Test
	public void testDelete(){
		DiscussionReplyBundle dto = replyDao.createReply(threadId, replyId, "messageKey", userId);
		long replyId = Long.parseLong(dto.getId());

		dto.setIsDeleted(true);
		replyDao.markReplyAsDeleted(replyId);
		DiscussionReplyBundle returnedDto = replyDao.getReply(replyId, DEFAULT_FILTER);
		assertFalse("after marking reply as deleted, etag should be different",
				dto.getEtag().equals(returnedDto.getEtag()));
		dto.setModifiedOn(returnedDto.getModifiedOn());
		dto.setEtag(returnedDto.getEtag());
		assertEquals(dto, returnedDto);
	}

	@Test (expected = NotFoundException.class)
	public void testDeleteWithFilter(){
		DiscussionReplyBundle dto = replyDao.createReply(threadId, replyId, "messageKey", userId);
		long replyId = Long.parseLong(dto.getId());

		dto.setIsDeleted(true);
		replyDao.markReplyAsDeleted(replyId);
		replyDao.getReply(replyId, DiscussionFilter.EXCLUDE_DELETED);
	}

	@Test
	public void testUpdateMessageKey() throws InterruptedException {
		DiscussionReplyBundle dto = replyDao.createReply(threadId, replyId, "messageKey", userId);
		long replyId = Long.parseLong(dto.getId());

		Thread.sleep(1000);
		dto.setIsEdited(true);
		String newMessageKey = UUID.randomUUID().toString();
		dto.setMessageKey(newMessageKey);
		replyDao.updateMessageKey(replyId, newMessageKey);
		DiscussionReplyBundle returnedDto = replyDao.getReply(replyId, DEFAULT_FILTER);
		assertFalse("after updating message key, modifiedOn should be different",
				dto.getModifiedOn().equals(returnedDto.getModifiedOn()));
		assertFalse("after updating message key, etag should be different",
				dto.getEtag().equals(returnedDto.getEtag()));
		dto.setModifiedOn(returnedDto.getModifiedOn());
		dto.setEtag(returnedDto.getEtag());
		assertEquals(dto, returnedDto);
	}

	@Test
	public void testGetThreadReplyStatsNoReplies() {
		DiscussionThreadReplyStat stat = replyDao.getThreadReplyStat(threadIdLong);
		assertNotNull(stat);
		assertNull(stat.getThreadId());
		assertEquals((Long)0L, stat.getNumberOfReplies());
		assertNull(stat.getLastActivity());
	}

	@Test
	public void testGetThreadReplyStats() throws InterruptedException {
		// create another thread
		Long threadIdLong2 = idGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID);
		String threadId2 = threadIdLong2.toString();
		threadDao.createThread(forumId, threadId2, "title", "messageKey2", userId);

		int numberOfReplies = 2;
		// create 2 replies for each thread
		createReplies(numberOfReplies, threadId);
		createReplies(numberOfReplies, threadId2);

		DiscussionThreadReplyStat stat1 = replyDao.getThreadReplyStat(threadIdLong);
		DiscussionThreadReplyStat stat2 = replyDao.getThreadReplyStat(threadIdLong2);
		assertNotNull(stat1);
		assertNotNull(stat2);
		assertEquals(stat1.getThreadId(), threadIdLong);
		assertEquals(stat2.getThreadId(), threadIdLong2);
		assertEquals(stat1.getNumberOfReplies(), (Long) 2L);
		assertEquals(stat2.getNumberOfReplies(), (Long) 2L);
		assertNotNull(stat1.getLastActivity());
		assertNotNull(stat2.getLastActivity());
	}

	@Test
	public void testGetThreadReplyStatsWithDeletedReply() throws InterruptedException {
		int numberOfReplies = 2;
		// create 2 replies for each thread
		List<DiscussionReplyBundle> replies = createReplies(numberOfReplies, threadId);
		replyDao.markReplyAsDeleted(Long.parseLong(replies.get(0).getId()));

		DiscussionThreadReplyStat stat = replyDao.getThreadReplyStat(threadIdLong);
		assertNotNull(stat);
		assertEquals(stat.getThreadId(), threadIdLong);
		assertEquals(stat.getNumberOfReplies(), (Long) 1L);
	}

	@Test
	public void testGetThreadAuthorStats() throws InterruptedException {
		List<String> activeAuthors = replyDao.getActiveAuthors(threadIdLong);
		assertNotNull(activeAuthors);
		assertTrue(activeAuthors.isEmpty());

		List<Long> users = createUsers(6);
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(0));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(0));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(1));

		activeAuthors = replyDao.getActiveAuthors(threadIdLong);
		assertEquals(activeAuthors,
				Arrays.asList(users.get(0).toString(), users.get(1).toString()));

		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(1));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(2));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(2));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(3));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(3));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(4));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(4));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(5));

		activeAuthors = replyDao.getActiveAuthors(threadIdLong);
		assertEquals(new HashSet<String>(activeAuthors),
				new HashSet<String>(Arrays.asList(users.get(0).toString(),
						users.get(1).toString(), users.get(2).toString(),
						users.get(3).toString(), users.get(4).toString())));

		usersToDelete.addAll(users);
	}

	@Test
	public void testGetThreadAuthorStatsWithDeletedReply() throws InterruptedException {
		List<String> activeAuthors = replyDao.getActiveAuthors(threadIdLong);
		assertNotNull(activeAuthors);
		assertTrue(activeAuthors.isEmpty());

		List<Long> users = createUsers(6);
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(0));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(0));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(1));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(1));
		DiscussionReplyBundle reply = replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(2));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(2));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(3));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(3));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(4));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(4));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(5));
		replyDao.createReply(threadId, idGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID).toString(),
				UUID.randomUUID().toString(), users.get(5));
		replyDao.markReplyAsDeleted(Long.parseLong(reply.getId()));

		activeAuthors = replyDao.getActiveAuthors(threadIdLong);
		assertEquals(new HashSet<String>(activeAuthors),
				new HashSet<String>(Arrays.asList(users.get(0).toString(),
						users.get(1).toString(), users.get(5).toString(),
						users.get(3).toString(), users.get(4).toString())));

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

	@Test
	public void testBuildGetRepliesQuery() {
		assertEquals("include deleted replies",
				"SELECT DISCUSSION_REPLY.ID AS ID , THREAD_ID, FORUM_ID, PROJECT_ID,"
				+ " DISCUSSION_REPLY.MESSAGE_KEY AS MESSAGE_KEY , DISCUSSION_REPLY.CREATED_BY AS CREATED_BY,"
				+ " DISCUSSION_REPLY.CREATED_ON AS CREATED_ON, DISCUSSION_REPLY.MODIFIED_ON AS MODIFIED_ON,"
				+ " DISCUSSION_REPLY.ETAG AS ETAG, DISCUSSION_REPLY.IS_EDITED AS IS_EDITED,"
				+ " DISCUSSION_REPLY.IS_DELETED AS IS_DELETED"
				+ " FROM DISCUSSION_REPLY, DISCUSSION_THREAD, FORUM"
				+ " WHERE THREAD_ID = DISCUSSION_THREAD.ID"
				+ " AND FORUM_ID = FORUM.ID"
				+ " AND THREAD_ID = ?"
				+ " LIMIT 10 OFFSET 0",
				DBODiscussionReplyDAOImpl.buildGetRepliesQuery(10L, 0L, null, null, DiscussionFilter.NO_FILTER));
		assertEquals("only non deleted replies",
				"SELECT DISCUSSION_REPLY.ID AS ID , THREAD_ID, FORUM_ID, PROJECT_ID,"
				+ " DISCUSSION_REPLY.MESSAGE_KEY AS MESSAGE_KEY , DISCUSSION_REPLY.CREATED_BY AS CREATED_BY,"
				+ " DISCUSSION_REPLY.CREATED_ON AS CREATED_ON, DISCUSSION_REPLY.MODIFIED_ON AS MODIFIED_ON,"
				+ " DISCUSSION_REPLY.ETAG AS ETAG, DISCUSSION_REPLY.IS_EDITED AS IS_EDITED,"
				+ " DISCUSSION_REPLY.IS_DELETED AS IS_DELETED"
				+ " FROM DISCUSSION_REPLY, DISCUSSION_THREAD, FORUM"
				+ " WHERE THREAD_ID = DISCUSSION_THREAD.ID"
				+ " AND FORUM_ID = FORUM.ID"
				+ " AND THREAD_ID = ?"
				+ " AND DISCUSSION_REPLY.IS_DELETED = FALSE"
				+ " LIMIT 10 OFFSET 0",
				DBODiscussionReplyDAOImpl.buildGetRepliesQuery(10L, 0L, null, null, DiscussionFilter.EXCLUDE_DELETED));
		assertEquals("order ascending",
				"SELECT DISCUSSION_REPLY.ID AS ID , THREAD_ID, FORUM_ID, PROJECT_ID,"
				+ " DISCUSSION_REPLY.MESSAGE_KEY AS MESSAGE_KEY , DISCUSSION_REPLY.CREATED_BY AS CREATED_BY,"
				+ " DISCUSSION_REPLY.CREATED_ON AS CREATED_ON, DISCUSSION_REPLY.MODIFIED_ON AS MODIFIED_ON,"
				+ " DISCUSSION_REPLY.ETAG AS ETAG, DISCUSSION_REPLY.IS_EDITED AS IS_EDITED,"
				+ " DISCUSSION_REPLY.IS_DELETED AS IS_DELETED"
				+ " FROM DISCUSSION_REPLY, DISCUSSION_THREAD, FORUM"
				+ " WHERE THREAD_ID = DISCUSSION_THREAD.ID"
				+ " AND FORUM_ID = FORUM.ID"
				+ " AND THREAD_ID = ?"
				+ " ORDER BY CREATED_ON"
				+ " LIMIT 10 OFFSET 0",
				DBODiscussionReplyDAOImpl.buildGetRepliesQuery(10L, 0L, DiscussionReplyOrder.CREATED_ON, true, DiscussionFilter.NO_FILTER));
		assertEquals("order descending",
				"SELECT DISCUSSION_REPLY.ID AS ID , THREAD_ID, FORUM_ID, PROJECT_ID,"
				+ " DISCUSSION_REPLY.MESSAGE_KEY AS MESSAGE_KEY , DISCUSSION_REPLY.CREATED_BY AS CREATED_BY,"
				+ " DISCUSSION_REPLY.CREATED_ON AS CREATED_ON, DISCUSSION_REPLY.MODIFIED_ON AS MODIFIED_ON,"
				+ " DISCUSSION_REPLY.ETAG AS ETAG, DISCUSSION_REPLY.IS_EDITED AS IS_EDITED,"
				+ " DISCUSSION_REPLY.IS_DELETED AS IS_DELETED"
				+ " FROM DISCUSSION_REPLY, DISCUSSION_THREAD, FORUM"
				+ " WHERE THREAD_ID = DISCUSSION_THREAD.ID"
				+ " AND FORUM_ID = FORUM.ID"
				+ " AND THREAD_ID = ?"
				+ " ORDER BY CREATED_ON DESC"
				+ " LIMIT 10 OFFSET 0",
				DBODiscussionReplyDAOImpl.buildGetRepliesQuery(10L, 0L, DiscussionReplyOrder.CREATED_ON, false, DiscussionFilter.NO_FILTER));
		assertEquals("limit",
				"SELECT DISCUSSION_REPLY.ID AS ID , THREAD_ID, FORUM_ID, PROJECT_ID,"
				+ " DISCUSSION_REPLY.MESSAGE_KEY AS MESSAGE_KEY , DISCUSSION_REPLY.CREATED_BY AS CREATED_BY,"
				+ " DISCUSSION_REPLY.CREATED_ON AS CREATED_ON, DISCUSSION_REPLY.MODIFIED_ON AS MODIFIED_ON,"
				+ " DISCUSSION_REPLY.ETAG AS ETAG, DISCUSSION_REPLY.IS_EDITED AS IS_EDITED,"
				+ " DISCUSSION_REPLY.IS_DELETED AS IS_DELETED"
				+ " FROM DISCUSSION_REPLY, DISCUSSION_THREAD, FORUM"
				+ " WHERE THREAD_ID = DISCUSSION_THREAD.ID"
				+ " AND FORUM_ID = FORUM.ID"
				+ " AND THREAD_ID = ?"
				+ " ORDER BY CREATED_ON DESC"
				+ " LIMIT 2 OFFSET 0",
				DBODiscussionReplyDAOImpl.buildGetRepliesQuery(2L, 0L, DiscussionReplyOrder.CREATED_ON, false, DiscussionFilter.NO_FILTER));
		assertEquals("offset",
				"SELECT DISCUSSION_REPLY.ID AS ID , THREAD_ID, FORUM_ID, PROJECT_ID,"
				+ " DISCUSSION_REPLY.MESSAGE_KEY AS MESSAGE_KEY , DISCUSSION_REPLY.CREATED_BY AS CREATED_BY,"
				+ " DISCUSSION_REPLY.CREATED_ON AS CREATED_ON, DISCUSSION_REPLY.MODIFIED_ON AS MODIFIED_ON,"
				+ " DISCUSSION_REPLY.ETAG AS ETAG, DISCUSSION_REPLY.IS_EDITED AS IS_EDITED,"
				+ " DISCUSSION_REPLY.IS_DELETED AS IS_DELETED"
				+ " FROM DISCUSSION_REPLY, DISCUSSION_THREAD, FORUM"
				+ " WHERE THREAD_ID = DISCUSSION_THREAD.ID"
				+ " AND FORUM_ID = FORUM.ID"
				+ " AND THREAD_ID = ?"
				+ " ORDER BY CREATED_ON DESC"
				+ " LIMIT 10 OFFSET 3",
				DBODiscussionReplyDAOImpl.buildGetRepliesQuery(10L, 3L, DiscussionReplyOrder.CREATED_ON, false, DiscussionFilter.NO_FILTER));
	}

	@Test
	public void testAddCondition() {
		String query = "";
		assertEquals("no filter", "", 
				DBODiscussionReplyDAOImpl.addCondition(query, DiscussionFilter.NO_FILTER));
		assertEquals("deleted only", DELETED_CONDITION, 
				DBODiscussionReplyDAOImpl.addCondition(query, DiscussionFilter.DELETED_ONLY));
		assertEquals("not deleted only", NOT_DELETED_CONDITION, 
				DBODiscussionReplyDAOImpl.addCondition(query, DiscussionFilter.EXCLUDE_DELETED));
	}
}
