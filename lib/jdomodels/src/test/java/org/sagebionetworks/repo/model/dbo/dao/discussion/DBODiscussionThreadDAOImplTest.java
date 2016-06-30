package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.sagebionetworks.repo.model.dbo.dao.discussion.DBODiscussionThreadDAOImpl.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
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
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadAuthorStat;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadReplyStat;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadViewStat;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBODiscussionThreadDAOImplTest {

	@Autowired
	private ForumDAO forumDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private IdGenerator idGenerator;

	private Long userId = null;
	private Long userId2 = null;
	private String projectId = null;
	private String forumId;
	private Long threadId;
	private long forumIdLong;

	@Before
	public void before() {
		// create a user to create a project
		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		userId = userGroupDAO.create(user);
		// create a project
		Node project = NodeTestUtils.createNew("projectName" + "-" + new Random().nextInt(), userId);
		project.setParentId(StackConfiguration.getRootFolderEntityIdStatic());
		projectId = nodeDao.createNew(project);
		// create a forum
		Forum dto = forumDao.createForum(projectId);
		forumId = dto.getId();
		forumIdLong = Long.parseLong(forumId);
		threadId = idGenerator.generateNewId(TYPE.DISCUSSION_THREAD_ID);
	}

	@After
	public void cleanup() {
		if (projectId != null) nodeDao.delete(projectId);
		if (userId != null) userGroupDAO.delete(userId.toString());
		if (userId2 != null) userGroupDAO.delete(userId.toString());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithInvalidForumId() {
		threadDao.createThread(null, threadId.toString(), "title", "messageKey", userId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithInvalidThreadId() {
		threadDao.createThread(forumId, null, "title", "messageKey", userId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithInvalidTitle() {
		threadDao.createThread(forumId, threadId.toString(), null, "messageKey", userId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithInvalidMessageKey() {
		threadDao.createThread(forumId, threadId.toString(), "title", null, userId);
	}

	@Test
	public void testCreate() {
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		assertEquals(dto.getId(), threadId.toString());
		assertEquals(dto.getForumId(), forumId);
		assertEquals(dto.getProjectId(), projectId);
		assertEquals(dto.getTitle(), "title");
		assertEquals(dto.getCreatedBy(), userId.toString());
		assertEquals(dto.getIsEdited(), false);
		assertEquals(dto.getIsDeleted(), false);
		assertEquals(dto.getIsPinned(), false);
		assertEquals("check default number of views", dto.getNumberOfViews(), (Long) 0L);
		assertEquals("check default number of replies", dto.getNumberOfReplies(), (Long) 0L);
		assertEquals("check default last activity", dto.getLastActivity(), dto.getModifiedOn());
		assertEquals("check default active authors", dto.getActiveAuthors(), new ArrayList<String>());

		long threadId = Long.parseLong(dto.getId());
		assertEquals("getThread() should return the created one", dto, threadDao.getThread(threadId, DEFAULT_FILTER));
	}

	@Test
	public void testGetProjectId() {
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		assertEquals(projectId, threadDao.getProjectId(dto.getId()));
	}

	@Test (expected=NotFoundException.class)
	public void testGetProjectIdNotFound() {
		threadDao.getProjectId("-1");
	}

	@Test
	public void testGetAuthor() {
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		assertEquals(userId.toString(), threadDao.getAuthor(dto.getId()));
	}

	@Test (expected=NotFoundException.class)
	public void testGetAuthorNotFound() {
		threadDao.getAuthor("-1");
	}

	@Test
	public void testGetEtag(){
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		long threadId = Long.parseLong(dto.getId());
		String etag = threadDao.getEtagForUpdate(threadId);
		assertNotNull(etag);
		assertEquals(etag, dto.getEtag());
	}

	@Test
	public void testUpdateMessageKey() throws InterruptedException {
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		long threadId = Long.parseLong(dto.getId());

		Thread.sleep(1000);
		dto.setIsEdited(true);
		String newMessageKey = UUID.randomUUID().toString();
		dto.setMessageKey(newMessageKey);
		threadDao.updateMessageKey(threadId, newMessageKey);
		DiscussionThreadBundle returnedDto = threadDao.getThread(threadId, DEFAULT_FILTER);
		assertFalse("after updating message key, modifiedOn should be different",
				dto.getModifiedOn().equals(returnedDto.getModifiedOn()));
		assertFalse("after updating message key, lastActivity should be different",
				dto.getLastActivity().equals(returnedDto.getLastActivity()));
		assertFalse("after updating message key, etag should be different",
				dto.getEtag().equals(returnedDto.getEtag()));
		dto.setModifiedOn(returnedDto.getModifiedOn());
		dto.setLastActivity(returnedDto.getLastActivity());
		dto.setEtag(returnedDto.getEtag());
		assertEquals(dto, returnedDto);
	}

	@Test
	public void testUpdateTitle(){
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		long threadId = Long.parseLong(dto.getId());

		String newTitle = "newTitle";
		dto.setIsEdited(true);
		dto.setTitle(newTitle);
		threadDao.updateTitle(threadId, newTitle);
		DiscussionThreadBundle returnedDto = threadDao.getThread(threadId, DEFAULT_FILTER);
		assertFalse("after updating title, etag should be different",
				dto.getEtag().equals(returnedDto.getEtag()));
		dto.setModifiedOn(returnedDto.getModifiedOn());
		dto.setLastActivity(returnedDto.getLastActivity());
		dto.setEtag(returnedDto.getEtag());
		assertEquals(dto, returnedDto);
	}

	@Test
	public void testPinning(){
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		long threadId = Long.parseLong(dto.getId());

		dto.setIsPinned(true);
		threadDao.pinThread(threadId);
		DiscussionThreadBundle returnedDto = threadDao.getThread(threadId, DEFAULT_FILTER);
		assertFalse("after pinning a thread, etag should be different",
				dto.getEtag().equals(returnedDto.getEtag()));
		dto.setModifiedOn(returnedDto.getModifiedOn());
		dto.setLastActivity(returnedDto.getLastActivity());
		dto.setEtag(returnedDto.getEtag());
		assertEquals(dto, returnedDto);

		dto.setIsPinned(false);
		threadDao.unpinThread(threadId);
		returnedDto = threadDao.getThread(threadId, DEFAULT_FILTER);
		assertFalse("after unpinning a thread, etag should be different",
				dto.getEtag().equals(returnedDto.getEtag()));
		dto.setModifiedOn(returnedDto.getModifiedOn());
		dto.setLastActivity(returnedDto.getLastActivity());
		dto.setEtag(returnedDto.getEtag());
		assertEquals(dto, returnedDto);
	}

	@Test
	public void testDelete(){
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		long threadId = Long.parseLong(dto.getId());

		dto.setIsDeleted(true);
		threadDao.markThreadAsDeleted(threadId);
		DiscussionThreadBundle returnedDto = threadDao.getThread(threadId, DEFAULT_FILTER);
		assertFalse("after marking thread as deleted, etag should be different",
				dto.getEtag().equals(returnedDto.getEtag()));
		dto.setModifiedOn(returnedDto.getModifiedOn());
		dto.setLastActivity(returnedDto.getLastActivity());
		dto.setEtag(returnedDto.getEtag());
		assertEquals(dto, returnedDto);
	}

	@Test (expected = NotFoundException.class)
	public void testDeleteWithFilter(){
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		long threadId = Long.parseLong(dto.getId());

		dto.setIsDeleted(true);
		threadDao.markThreadAsDeleted(threadId);
		threadDao.getThread(threadId, DiscussionFilter.EXCLUDE_DELETED);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateTitleWithInvalidArgument(){
		threadDao.updateTitle(1L, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateMessageUrlWithInvalidArgument(){
		threadDao.updateMessageKey(1L, null);
	}

	@Test
	public void testGetThreadsWithZeroExistingThreads() {
		assertEquals("empty threads",
				new ArrayList<DiscussionThreadBundle>(),
				threadDao.getThreads(forumIdLong, MAX_LIMIT, 0L, null, null, DiscussionFilter.NO_FILTER).getResults());
	}

	@Test
	public void testGetThreadsLimitAndOffset() throws Exception {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(3);
		assertEquals(createdThreads.size(), 3);

		assertEquals(3, threadDao.getThreadCount(forumIdLong, DiscussionFilter.NO_FILTER));

		assertEquals("non order",
				new HashSet<DiscussionThreadBundle>(createdThreads),
				new HashSet<DiscussionThreadBundle>(threadDao.getThreads(forumIdLong, MAX_LIMIT, 0L, null, null, DiscussionFilter.NO_FILTER).getResults()));

		assertEquals("order, all",
				createdThreads,
				threadDao.getThreads(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER).getResults());
		assertEquals("order, second the third",
				Arrays.asList(createdThreads.get(1), createdThreads.get(2)),
				threadDao.getThreads(forumIdLong, 2L, 1L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER).getResults());
		assertEquals("order, last",
				Arrays.asList(createdThreads.get(2)),
				threadDao.getThreads(forumIdLong, 2L, 2L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER).getResults());
		assertEquals("order, out of range",
				Arrays.asList(),
				threadDao.getThreads(forumIdLong, 2L, 3L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER).getResults());
		assertEquals("order, on limit",
				Arrays.asList(createdThreads.get(1), createdThreads.get(2)),
				threadDao.getThreads(forumIdLong, DBODiscussionThreadDAOImpl.MAX_LIMIT, 1L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER).getResults());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testNegativeOffset() {
		threadDao.getThreads(forumIdLong, 2L, -3L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testNegativeLimit() {
		threadDao.getThreads(forumIdLong, -2L, 3L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testOverLimit() {
		threadDao.getThreads(forumIdLong, DBODiscussionThreadDAOImpl.MAX_LIMIT+1, 3L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testNullOffset() {
		threadDao.getThreads(forumIdLong, 2L, null, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testNullLimit() {
		threadDao.getThreads(forumIdLong, null, 2L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testNullOrderNotNullAscending() {
		threadDao.getThreads(forumIdLong, 2L, 2L, null, true, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testNotNullOrderNullAscending() {
		threadDao.getThreads(forumIdLong, 2L, 2L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, null, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testNotNullOrderNullFilter() {
		threadDao.getThreads(forumIdLong, 2L, 2L, null, null, null);
	}

	@Test
	public void testUpdateThreadViewStat() throws InterruptedException {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(3);

		List<Long> numberOfViews = Arrays.asList(1L, 3L, 2L);
		List<DiscussionThreadViewStat> stats = new ArrayList<DiscussionThreadViewStat>();
		for (int i = 0; i < 3; i++) {
			DiscussionThreadViewStat stat = new DiscussionThreadViewStat();
			stat.setThreadId(Long.parseLong(createdThreads.get(i).getId()));
			stat.setNumberOfViews(numberOfViews.get(i));
			stats.add(stat);
			createdThreads.get(i).setNumberOfViews(numberOfViews.get(i));
		}
		threadDao.updateThreadViewStat(stats);

		List<DiscussionThreadBundle> expected = Arrays.asList(createdThreads.get(0), createdThreads.get(2), createdThreads.get(1));
		assertEquals("sorted by number of views",
				expected,
				threadDao.getThreads(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.NUMBER_OF_VIEWS, true, DiscussionFilter.NO_FILTER).getResults());

		expected = Arrays.asList(createdThreads.get(1), createdThreads.get(2), createdThreads.get(0));
		assertEquals("sorted by number of views desc",
				expected,
				threadDao.getThreads(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.NUMBER_OF_VIEWS, false, DiscussionFilter.NO_FILTER).getResults());
	}

	@Test
	public void testUpdateThreadReplyStatControlByNumberOfReplies() throws InterruptedException {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(3);
		List<Long> numberOfReplies = Arrays.asList(1L, 3L, 2L);
		Long lastActivity = System.currentTimeMillis();
		List<DiscussionThreadReplyStat> stats = new ArrayList<DiscussionThreadReplyStat>();
		for (int i = 0; i < 3; i++) {
			DiscussionThreadReplyStat stat = new DiscussionThreadReplyStat();
			stat.setThreadId(Long.parseLong(createdThreads.get(i).getId()));
			stat.setNumberOfReplies(numberOfReplies.get(i));
			stat.setLastActivity(lastActivity);
			stats.add(stat);
			createdThreads.get(i).setNumberOfReplies(numberOfReplies.get(i));
			createdThreads.get(i).setLastActivity(new Date(lastActivity));
		}
		threadDao.updateThreadReplyStat(stats);
		List<DiscussionThreadBundle> expected = new ArrayList<DiscussionThreadBundle>();
		expected.addAll(Arrays.asList(createdThreads.get(0), createdThreads.get(2), createdThreads.get(1)));
		assertEquals("sorted by number of replies",
				expected.toString(),
				threadDao.getThreads(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.NUMBER_OF_REPLIES, true, DiscussionFilter.NO_FILTER).getResults().toString());

		expected.clear();
		expected.addAll(Arrays.asList(createdThreads.get(1), createdThreads.get(2), createdThreads.get(0)));
		assertEquals("sorted by number of replies desc",
				expected.toString(),
				threadDao.getThreads(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.NUMBER_OF_REPLIES, false, DiscussionFilter.NO_FILTER).getResults().toString());
	}

	@Test
	public void testUpdateThreadReplyStatControlByLastActivity() throws InterruptedException {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(3);
		Long numberOfReplies = 2L;
		Long date1 = new Date(2015-1900, 10, 19, 0, 0, 1).getTime();
		Long date2 = new Date(2015-1900, 10, 19, 0, 0, 2).getTime();
		Long date3 = new Date(2015-1900, 10, 19, 0, 0, 3).getTime();
		List<Long> lastActivities = Arrays.asList(date1, date3, date2);
		List<DiscussionThreadReplyStat> stats = new ArrayList<DiscussionThreadReplyStat>();
		for (int i = 0; i < 3; i++) {
			DiscussionThreadReplyStat stat = new DiscussionThreadReplyStat();
			stat.setThreadId(Long.parseLong(createdThreads.get(i).getId()));
			stat.setNumberOfReplies(numberOfReplies);
			stat.setLastActivity(lastActivities.get(i));
			stats.add(stat);
			createdThreads.get(i).setNumberOfReplies(numberOfReplies);
			createdThreads.get(i).setLastActivity(new Date(lastActivities.get(i)));
		}
		threadDao.updateThreadReplyStat(stats);
		List<DiscussionThreadBundle> expected = new ArrayList<DiscussionThreadBundle>();
		expected.addAll(Arrays.asList(createdThreads.get(0), createdThreads.get(2), createdThreads.get(1)));
		assertEquals("sorted by last activity",
				expected,
				threadDao.getThreads(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER).getResults());

		expected.clear();
		expected.addAll(Arrays.asList(createdThreads.get(1), createdThreads.get(2), createdThreads.get(0)));
		assertEquals("sorted by last activity desc",
				expected,
				threadDao.getThreads(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, false, DiscussionFilter.NO_FILTER).getResults());
	}

	private List<DiscussionThreadBundle> createListOfThreads(int numberOfThreads) throws InterruptedException {
		List<DiscussionThreadBundle> createdThreads = new ArrayList<DiscussionThreadBundle>();
		for (int i = 0; i < numberOfThreads; i++) {
			Thread.sleep(1000);
			threadId = idGenerator.generateNewId(TYPE.DISCUSSION_THREAD_ID);
			DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(),
					"title", UUID.randomUUID().toString(), userId);
			createdThreads.add(dto);
		}
		return createdThreads;
	}

	@Test
	public void testSetActiveAuthors() {
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		long threadId = Long.parseLong(dto.getId());
		DiscussionThreadAuthorStat stat = new DiscussionThreadAuthorStat();
		stat.setThreadId(threadId);
		stat.setActiveAuthors(Arrays.asList(dto.getCreatedBy(), "123456"));
		threadDao.updateThreadAuthorStat(Arrays.asList(stat));
		dto.setActiveAuthors(Arrays.asList(dto.getCreatedBy(), "123456"));
		assertEquals(new HashSet<String>(dto.getActiveAuthors()),
				new HashSet<String>(threadDao.getThread(threadId, DEFAULT_FILTER).getActiveAuthors()));
	}

	@Test
	public void testGetThreadReplyStats() {
		// create some threads
		threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		Long threadId2 = idGenerator.generateNewId(TYPE.DISCUSSION_THREAD_ID);
		threadDao.createThread(forumId, threadId2 .toString(), "title", "messageKey2", userId);

		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		userId2 = userGroupDAO.create(user);

		// one user viewed thread 1 twice
		threadDao.updateThreadView(threadId, userId2);
		threadDao.updateThreadView(threadId, userId2);
		// two users viewed thread 2
		threadDao.updateThreadView(threadId2, userId);
		threadDao.updateThreadView(threadId2, userId2);

		List<DiscussionThreadViewStat> stats = threadDao.getThreadViewStat(10L, 0L);
		assertNotNull(stats);
		assertEquals(stats.size(), 2);
		DiscussionThreadViewStat stat1 = stats.get(0);
		DiscussionThreadViewStat stat2 = stats.get(1);
		assertEquals(stat1.getThreadId(), threadId);
		assertEquals(stat2.getThreadId(), threadId2);
		assertEquals(stat1.getNumberOfViews(), (Long) 1L);
		assertEquals(stat2.getNumberOfViews(), (Long) 2L);
	}

	@Test
	public void testGetAllThreadId() {
		assertTrue(threadDao.getAllThreadId(10L, 0L).isEmpty());

		// create some threads
		threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		Long threadId2 = idGenerator.generateNewId(TYPE.DISCUSSION_THREAD_ID);
		threadDao.createThread(forumId, threadId2 .toString(), "title", "messageKey2", userId);

		assertEquals(Arrays.asList(threadId, threadId2), threadDao.getAllThreadId(10L, 0L));
	}

	@Test
	public void testGetDeletedAndNonDeletedThreads() throws InterruptedException{
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(3);
		threadDao.markThreadAsDeleted(Long.parseLong(createdThreads.get(1).getId()));

		PaginatedResults<DiscussionThreadBundle> deleted = threadDao.getThreads(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.DELETED_ONLY);
		PaginatedResults<DiscussionThreadBundle> available = threadDao.getThreads(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.EXCLUDE_DELETED);
		assertEquals(1, deleted.getTotalNumberOfResults());
		assertEquals(2, available.getTotalNumberOfResults());
		assertEquals(createdThreads.get(1).getId(), deleted.getResults().get(0).getId());

		Set<String> availableThreads = new HashSet<String>();
		availableThreads.add(available.getResults().get(0).getId());
		availableThreads.add(available.getResults().get(1).getId());
		assertTrue(availableThreads.contains(createdThreads.get(0).getId()));
		assertTrue(availableThreads.contains(createdThreads.get(2).getId()));
	}

	@Test
	public void testAddCondition() {
		String query = "";
		assertEquals("no filter", "", 
				DBODiscussionThreadDAOImpl.addCondition(query, DiscussionFilter.NO_FILTER));
		assertEquals("deleted only", DELETED_CONDITION,
				DBODiscussionThreadDAOImpl.addCondition(query, DiscussionFilter.DELETED_ONLY));
		assertEquals("not deleted only", NOT_DELETED_CONDITION,
				DBODiscussionThreadDAOImpl.addCondition(query, DiscussionFilter.EXCLUDE_DELETED));
	}

	@Test
	public void testBuildGetQuery() {
		assertEquals("not ordered","SELECT DISCUSSION_THREAD.ID AS ID,"
				+ " DISCUSSION_THREAD.FORUM_ID AS FORUM_ID, FORUM.PROJECT_ID AS PROJECT_ID,"
				+ " DISCUSSION_THREAD.TITLE AS TITLE, DISCUSSION_THREAD.CREATED_ON AS CREATED_ON,"
				+ " DISCUSSION_THREAD.CREATED_BY AS CREATED_BY, DISCUSSION_THREAD.MODIFIED_ON AS MODIFIED_ON,"
				+ " DISCUSSION_THREAD.ETAG AS ETAG, DISCUSSION_THREAD.MESSAGE_KEY AS MESSAGE_KEY,"
				+ " DISCUSSION_THREAD.IS_EDITED AS IS_EDITED, DISCUSSION_THREAD.IS_DELETED AS IS_DELETED,"
				+ " DISCUSSION_THREAD.IS_PINNED AS IS_PINNED,"
				+ " DISCUSSION_THREAD_STATS.NUMBER_OF_VIEWS AS NUMBER_OF_VIEWS,"
				+ " DISCUSSION_THREAD_STATS.NUMBER_OF_REPLIES AS NUMBER_OF_REPLIES,"
				+ " IFNULL(LAST_ACTIVITY, MODIFIED_ON) AS LAST_ACTIVITY,"
				+ " DISCUSSION_THREAD_STATS.ACTIVE_AUTHORS AS ACTIVE_AUTHORS"
				+ " FROM DISCUSSION_THREAD JOIN FORUM ON DISCUSSION_THREAD.FORUM_ID = FORUM.ID"
				+ " LEFT OUTER JOIN DISCUSSION_THREAD_STATS ON DISCUSSION_THREAD.ID = DISCUSSION_THREAD_STATS.THREAD_ID"
				+ " WHERE FORUM_ID = ?"
				+ " LIMIT 10 OFFSET 0",
				DBODiscussionThreadDAOImpl.buildGetQuery(10L, 0L, null, null, DiscussionFilter.NO_FILTER));
		assertEquals("ordered by pinned and last activity","SELECT DISCUSSION_THREAD.ID AS ID,"
				+ " DISCUSSION_THREAD.FORUM_ID AS FORUM_ID, FORUM.PROJECT_ID AS PROJECT_ID,"
				+ " DISCUSSION_THREAD.TITLE AS TITLE, DISCUSSION_THREAD.CREATED_ON AS CREATED_ON,"
				+ " DISCUSSION_THREAD.CREATED_BY AS CREATED_BY, DISCUSSION_THREAD.MODIFIED_ON AS MODIFIED_ON,"
				+ " DISCUSSION_THREAD.ETAG AS ETAG, DISCUSSION_THREAD.MESSAGE_KEY AS MESSAGE_KEY,"
				+ " DISCUSSION_THREAD.IS_EDITED AS IS_EDITED, DISCUSSION_THREAD.IS_DELETED AS IS_DELETED,"
				+ " DISCUSSION_THREAD.IS_PINNED AS IS_PINNED,"
				+ " DISCUSSION_THREAD_STATS.NUMBER_OF_VIEWS AS NUMBER_OF_VIEWS,"
				+ " DISCUSSION_THREAD_STATS.NUMBER_OF_REPLIES AS NUMBER_OF_REPLIES,"
				+ " IFNULL(LAST_ACTIVITY, MODIFIED_ON) AS LAST_ACTIVITY,"
				+ " DISCUSSION_THREAD_STATS.ACTIVE_AUTHORS AS ACTIVE_AUTHORS"
				+ " FROM DISCUSSION_THREAD JOIN FORUM ON DISCUSSION_THREAD.FORUM_ID = FORUM.ID"
				+ " LEFT OUTER JOIN DISCUSSION_THREAD_STATS ON DISCUSSION_THREAD.ID = DISCUSSION_THREAD_STATS.THREAD_ID"
				+ " WHERE FORUM_ID = ?"
				+ " ORDER BY IS_PINNED DESC, LAST_ACTIVITY"
				+ " LIMIT 10 OFFSET 0",
				DBODiscussionThreadDAOImpl.buildGetQuery(10L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER));
		assertEquals("limit","SELECT DISCUSSION_THREAD.ID AS ID,"
				+ " DISCUSSION_THREAD.FORUM_ID AS FORUM_ID, FORUM.PROJECT_ID AS PROJECT_ID,"
				+ " DISCUSSION_THREAD.TITLE AS TITLE, DISCUSSION_THREAD.CREATED_ON AS CREATED_ON,"
				+ " DISCUSSION_THREAD.CREATED_BY AS CREATED_BY, DISCUSSION_THREAD.MODIFIED_ON AS MODIFIED_ON,"
				+ " DISCUSSION_THREAD.ETAG AS ETAG, DISCUSSION_THREAD.MESSAGE_KEY AS MESSAGE_KEY,"
				+ " DISCUSSION_THREAD.IS_EDITED AS IS_EDITED, DISCUSSION_THREAD.IS_DELETED AS IS_DELETED,"
				+ " DISCUSSION_THREAD.IS_PINNED AS IS_PINNED,"
				+ " DISCUSSION_THREAD_STATS.NUMBER_OF_VIEWS AS NUMBER_OF_VIEWS,"
				+ " DISCUSSION_THREAD_STATS.NUMBER_OF_REPLIES AS NUMBER_OF_REPLIES,"
				+ " IFNULL(LAST_ACTIVITY, MODIFIED_ON) AS LAST_ACTIVITY,"
				+ " DISCUSSION_THREAD_STATS.ACTIVE_AUTHORS AS ACTIVE_AUTHORS"
				+ " FROM DISCUSSION_THREAD JOIN FORUM ON DISCUSSION_THREAD.FORUM_ID = FORUM.ID"
				+ " LEFT OUTER JOIN DISCUSSION_THREAD_STATS ON DISCUSSION_THREAD.ID = DISCUSSION_THREAD_STATS.THREAD_ID"
				+ " WHERE FORUM_ID = ?"
				+ " LIMIT 100 OFFSET 0",
				DBODiscussionThreadDAOImpl.buildGetQuery(100L, 0L, null, null, DiscussionFilter.NO_FILTER));
		assertEquals("offset","SELECT DISCUSSION_THREAD.ID AS ID,"
				+ " DISCUSSION_THREAD.FORUM_ID AS FORUM_ID, FORUM.PROJECT_ID AS PROJECT_ID,"
				+ " DISCUSSION_THREAD.TITLE AS TITLE, DISCUSSION_THREAD.CREATED_ON AS CREATED_ON,"
				+ " DISCUSSION_THREAD.CREATED_BY AS CREATED_BY, DISCUSSION_THREAD.MODIFIED_ON AS MODIFIED_ON,"
				+ " DISCUSSION_THREAD.ETAG AS ETAG, DISCUSSION_THREAD.MESSAGE_KEY AS MESSAGE_KEY,"
				+ " DISCUSSION_THREAD.IS_EDITED AS IS_EDITED, DISCUSSION_THREAD.IS_DELETED AS IS_DELETED,"
				+ " DISCUSSION_THREAD.IS_PINNED AS IS_PINNED,"
				+ " DISCUSSION_THREAD_STATS.NUMBER_OF_VIEWS AS NUMBER_OF_VIEWS,"
				+ " DISCUSSION_THREAD_STATS.NUMBER_OF_REPLIES AS NUMBER_OF_REPLIES,"
				+ " IFNULL(LAST_ACTIVITY, MODIFIED_ON) AS LAST_ACTIVITY,"
				+ " DISCUSSION_THREAD_STATS.ACTIVE_AUTHORS AS ACTIVE_AUTHORS"
				+ " FROM DISCUSSION_THREAD JOIN FORUM ON DISCUSSION_THREAD.FORUM_ID = FORUM.ID"
				+ " LEFT OUTER JOIN DISCUSSION_THREAD_STATS ON DISCUSSION_THREAD.ID = DISCUSSION_THREAD_STATS.THREAD_ID"
				+ " WHERE FORUM_ID = ?"
				+ " LIMIT 10 OFFSET 2",
				DBODiscussionThreadDAOImpl.buildGetQuery(10L, 2L, null, null, DiscussionFilter.NO_FILTER));
		assertEquals("filtered","SELECT DISCUSSION_THREAD.ID AS ID,"
				+ " DISCUSSION_THREAD.FORUM_ID AS FORUM_ID, FORUM.PROJECT_ID AS PROJECT_ID,"
				+ " DISCUSSION_THREAD.TITLE AS TITLE, DISCUSSION_THREAD.CREATED_ON AS CREATED_ON,"
				+ " DISCUSSION_THREAD.CREATED_BY AS CREATED_BY, DISCUSSION_THREAD.MODIFIED_ON AS MODIFIED_ON,"
				+ " DISCUSSION_THREAD.ETAG AS ETAG, DISCUSSION_THREAD.MESSAGE_KEY AS MESSAGE_KEY,"
				+ " DISCUSSION_THREAD.IS_EDITED AS IS_EDITED, DISCUSSION_THREAD.IS_DELETED AS IS_DELETED,"
				+ " DISCUSSION_THREAD.IS_PINNED AS IS_PINNED,"
				+ " DISCUSSION_THREAD_STATS.NUMBER_OF_VIEWS AS NUMBER_OF_VIEWS,"
				+ " DISCUSSION_THREAD_STATS.NUMBER_OF_REPLIES AS NUMBER_OF_REPLIES,"
				+ " IFNULL(LAST_ACTIVITY, MODIFIED_ON) AS LAST_ACTIVITY,"
				+ " DISCUSSION_THREAD_STATS.ACTIVE_AUTHORS AS ACTIVE_AUTHORS"
				+ " FROM DISCUSSION_THREAD JOIN FORUM ON DISCUSSION_THREAD.FORUM_ID = FORUM.ID"
				+ " LEFT OUTER JOIN DISCUSSION_THREAD_STATS ON DISCUSSION_THREAD.ID = DISCUSSION_THREAD_STATS.THREAD_ID"
				+ " WHERE FORUM_ID = ?"
				+ " AND IS_DELETED = TRUE"
				+ " LIMIT 10 OFFSET 0",
				DBODiscussionThreadDAOImpl.buildGetQuery(10L, 0L, null, null, DiscussionFilter.DELETED_ONLY));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllThreadIdForForumWithNullForumId() {
		threadDao.getAllThreadIdForForum(null);
	}

	@Test
	public void testGetAllThreadIdForForum() {
		assertTrue(threadDao.getAllThreadIdForForum(forumId).isEmpty());
		threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		List<String> ids = threadDao.getAllThreadIdForForum(forumId);
		assertEquals(1L, ids.size());
		assertEquals(threadId.toString(), ids.get(0));
	}

	@Test
	public void testUpdateThreadViewTriggerThreadEtagChange() {
		DiscussionThreadBundle threadBundle = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		threadDao.updateThreadView(threadId, userId);
		DiscussionThreadBundle updated = threadDao.getThread(threadId, DEFAULT_FILTER);
		assertFalse(threadBundle.getEtag().equals(updated.getEtag()));
	}
}
