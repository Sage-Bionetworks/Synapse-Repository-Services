package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.sagebionetworks.repo.model.dbo.dao.discussion.DBODiscussionThreadDAOImpl.DEFAULT_FILTER;
import static org.sagebionetworks.repo.model.dbo.dao.discussion.DBODiscussionThreadDAOImpl.DELETED_CONDITION;
import static org.sagebionetworks.repo.model.dbo.dao.discussion.DBODiscussionThreadDAOImpl.NOT_DELETED_CONDITION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadEntityReference;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadStat;
import org.sagebionetworks.repo.model.discussion.EntityThreadCount;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.ForumObjectType;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestTestUtils;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectTestUtils;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBODiscussionThreadDAOImplTest {
	public static final long MAX_LIMIT = 20L;

	@Autowired
	private ForumDAO forumDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;
	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private ResearchProjectDAO researchProjectDao;
	@Autowired
	private RequestDAO requestDao;
	@Autowired
	private SubmissionDAO submissionDao;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Resource(name = "txManager")
	private PlatformTransactionManager transactionManager;

	private Long userId = null;
	private Long userId2 = null;
	private String projectId = null;
	private String managedARId = null;
	private String forumId;
	private String arForumId;
	private String researchProjectId;
	private String requestId;
	private Long threadId;
	private Long arTheadId;
	private Long forumIdLong;


	@Before
	public void before() {
		// create a user to create a project
		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		user.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
		userId = userGroupDAO.create(user);
		// create a project
		Node project = NodeTestUtils.createNew("projectName" + "-" + new Random().nextInt(), userId);
		project.setParentId(StackConfigurationSingleton.singleton().getRootFolderEntityId());
		projectId = nodeDao.createNew(project);
		// create a forum for Project
		Forum dto = forumDao.createForum(projectId, ForumObjectType.ENTITY);
		forumId = dto.getId();
		forumIdLong = Long.parseLong(forumId);
		threadId = idGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID);
	}

	@After
	public void cleanup() {
		if (projectId != null) nodeDao.delete(projectId);

		if (forumId != null) {
			forumDao.deleteForum(forumIdLong);
		}

		submissionDao.truncateAll();

		if(requestId != null) {
			requestDao.delete(requestId);
		}

		if(researchProjectId != null) {
			researchProjectDao.delete(researchProjectId);
		}

		if(arForumId != null) {
			forumDao.deleteForum(Long.parseLong(arForumId));
		}

		if(managedARId != null) {
			accessRequirementDAO.delete(managedARId);
		}

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
	public void testGetObjectId() {
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(),
				"title", "messageKey", userId);
		//call under test
		Optional<DiscussionThread> discussionThread = threadDao.getDiscussionThread(Long.parseLong(dto.getId()));
		assertTrue(discussionThread.isPresent());
		assertEquals(KeyFactory.stringToKey(projectId).toString(), discussionThread.get().getObjectId());
	}

	@Test
	public void testGetObjectIdNotFound() {
		//call under test
		assertThrows(NotFoundException.class, () ->threadDao.getThread(-1L, DEFAULT_FILTER));
	}

	@Test
	public void testGetAuthor() {
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		assertEquals(userId.toString(), threadDao.getAuthorForUpdate(dto.getId()));
	}

	@Test (expected=NotFoundException.class)
	public void testGetAuthorNotFound() {
		threadDao.getAuthorForUpdate("-1");
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

	@Test
	public void testRestore(){
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		long threadId = Long.parseLong(dto.getId());

		threadDao.markThreadAsDeleted(threadId);
		DiscussionThreadBundle returnedDto = threadDao.getThread(threadId, DEFAULT_FILTER);
		assertTrue(returnedDto.getIsDeleted());

		// undelete
		dto.setIsDeleted(false);
		threadDao.markThreadAsNotDeleted(threadId);
		returnedDto = threadDao.getThread(threadId, DEFAULT_FILTER);
		assertFalse("after marking thread as not deleted, etag should be different",
				dto.getEtag().equals(returnedDto.getEtag()));
		dto.setModifiedOn(returnedDto.getModifiedOn());
		dto.setLastActivity(returnedDto.getLastActivity());
		assertFalse(dto.equals(returnedDto));
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
				threadDao.getThreadsForForum(forumIdLong, MAX_LIMIT, 0L, null, null, DiscussionFilter.NO_FILTER));
	}

	@Test
	public void testGetThreadsLimitAndOffset() throws Exception {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(3);
		assertEquals(createdThreads.size(), 3);

		assertEquals(3, threadDao.getThreadCountForForum(forumIdLong, DiscussionFilter.NO_FILTER));

		assertEquals("non order",
				new HashSet<DiscussionThreadBundle>(createdThreads),
				new HashSet<DiscussionThreadBundle>(threadDao.getThreadsForForum(forumIdLong, MAX_LIMIT, 0L, null, null, DiscussionFilter.NO_FILTER)));

		assertEquals("order, all",
				createdThreads,
				threadDao.getThreadsForForum(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER));
		assertEquals("order, second the third",
				Arrays.asList(createdThreads.get(1), createdThreads.get(2)),
				threadDao.getThreadsForForum(forumIdLong, 2L, 1L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER));
		assertEquals("order, last",
				Arrays.asList(createdThreads.get(2)),
				threadDao.getThreadsForForum(forumIdLong, 2L, 2L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER));
		assertEquals("order, out of range",
				Arrays.asList(),
				threadDao.getThreadsForForum(forumIdLong, 2L, 3L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER));
		assertEquals("order, on limit",
				Arrays.asList(createdThreads.get(1), createdThreads.get(2)),
				threadDao.getThreadsForForum(forumIdLong, MAX_LIMIT, 1L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testNullOffset() {
		threadDao.getThreadsForForum(forumIdLong, 2L, null, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testNullLimit() {
		threadDao.getThreadsForForum(forumIdLong, null, 2L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testNullOrderNotNullAscending() {
		threadDao.getThreadsForForum(forumIdLong, 2L, 2L, null, true, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testNotNullOrderNullAscending() {
		threadDao.getThreadsForForum(forumIdLong, 2L, 2L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, null, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testNotNullOrderNullFilter() {
		threadDao.getThreadsForForum(forumIdLong, 2L, 2L, null, null, null);
	}

	@Test
	public void testSortedByThreadTitle() throws InterruptedException {
		Long threadBId = idGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID);
		DiscussionThreadBundle threadB = threadDao.createThread(forumId, threadBId.toString(),
				"B", UUID.randomUUID().toString(), userId);

		Long threadAId = idGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID);
		DiscussionThreadBundle threadA = threadDao.createThread(forumId, threadAId.toString(),
				"a", UUID.randomUUID().toString(), userId);

		Long threadCId = idGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID);
		DiscussionThreadBundle threadC = threadDao.createThread(forumId, threadCId.toString(),
				"c", UUID.randomUUID().toString(), userId);

		List<DiscussionThreadBundle> expected = Arrays.asList(threadA, threadB, threadC);
		assertEquals("sorted by title",
				expected,
				threadDao.getThreadsForForum(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.THREAD_TITLE, true, DiscussionFilter.NO_FILTER));
	}

	@Test
	public void testUpdateThreadStatByNumberOfViews() throws InterruptedException {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(3);

		List<Long> numberOfViews = Arrays.asList(1L, 3L, 2L);
		List<DiscussionThreadStat> stats = new ArrayList<DiscussionThreadStat>();
		for (int i = 0; i < 3; i++) {
			DiscussionThreadStat stat = new DiscussionThreadStat();
			stat.setThreadId(Long.parseLong(createdThreads.get(i).getId()));
			stat.setNumberOfViews(numberOfViews.get(i));
			stats.add(stat);
			createdThreads.get(i).setNumberOfViews(numberOfViews.get(i));
		}
		threadDao.updateThreadStats(stats);

		List<DiscussionThreadBundle> expected = Arrays.asList(createdThreads.get(0), createdThreads.get(2), createdThreads.get(1));
		assertEquals("sorted by number of views",
				expected,
				threadDao.getThreadsForForum(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.NUMBER_OF_VIEWS, true, DiscussionFilter.NO_FILTER));

		expected = Arrays.asList(createdThreads.get(1), createdThreads.get(2), createdThreads.get(0));
		assertEquals("sorted by number of views desc",
				expected,
				threadDao.getThreadsForForum(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.NUMBER_OF_VIEWS, false, DiscussionFilter.NO_FILTER));
	}

	@Test
	public void testUpdateThreadReplyStatControlByNumberOfReplies() throws InterruptedException {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(3);
		List<Long> numberOfReplies = Arrays.asList(1L, 3L, 2L);
		Long lastActivity = System.currentTimeMillis();
		List<DiscussionThreadStat> stats = new ArrayList<DiscussionThreadStat>();
		for (int i = 0; i < 3; i++) {
			DiscussionThreadStat stat = new DiscussionThreadStat();
			stat.setThreadId(Long.parseLong(createdThreads.get(i).getId()));
			stat.setNumberOfReplies(numberOfReplies.get(i));
			stat.setLastActivity(lastActivity);
			stats.add(stat);
			createdThreads.get(i).setNumberOfReplies(numberOfReplies.get(i));
			createdThreads.get(i).setLastActivity(new Date(lastActivity));
		}
		threadDao.updateThreadStats(stats);
		List<DiscussionThreadBundle> expected = new ArrayList<DiscussionThreadBundle>();
		expected.addAll(Arrays.asList(createdThreads.get(0), createdThreads.get(2), createdThreads.get(1)));
		assertEquals("sorted by number of replies",
				getIds(expected),
				getIds(threadDao.getThreadsForForum(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.NUMBER_OF_REPLIES, true, DiscussionFilter.NO_FILTER)));

		expected.clear();
		expected.addAll(Arrays.asList(createdThreads.get(1), createdThreads.get(2), createdThreads.get(0)));
		assertEquals("sorted by number of replies desc",
				getIds(expected),
				getIds(threadDao.getThreadsForForum(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.NUMBER_OF_REPLIES, false, DiscussionFilter.NO_FILTER)));
	}
	
	List<String> getIds(List<DiscussionThreadBundle> input){
		List<String> results = new LinkedList<>();
		for(DiscussionThreadBundle bundle: input) {
			results.add(bundle.getId());
		}
		return results;
	}

	@Test
	public void testUpdateThreadReplyStatControlByLastActivity() throws InterruptedException {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(3);
		Long numberOfReplies = 2L;
		Long date1 = new Date(2015-1900, 10, 19, 0, 0, 1).getTime();
		Long date2 = new Date(2015-1900, 10, 19, 0, 0, 2).getTime();
		Long date3 = new Date(2015-1900, 10, 19, 0, 0, 3).getTime();
		List<Long> lastActivities = Arrays.asList(date1, date3, date2);
		List<DiscussionThreadStat> stats = new ArrayList<DiscussionThreadStat>();
		for (int i = 0; i < 3; i++) {
			DiscussionThreadStat stat = new DiscussionThreadStat();
			stat.setThreadId(Long.parseLong(createdThreads.get(i).getId()));
			stat.setNumberOfReplies(numberOfReplies);
			stat.setLastActivity(lastActivities.get(i));
			stats.add(stat);
			createdThreads.get(i).setNumberOfReplies(numberOfReplies);
			createdThreads.get(i).setLastActivity(new Date(lastActivities.get(i)));
		}
		threadDao.updateThreadStats(stats);
		List<DiscussionThreadBundle> expected = new ArrayList<DiscussionThreadBundle>();
		expected.addAll(Arrays.asList(createdThreads.get(0), createdThreads.get(2), createdThreads.get(1)));
		assertEquals("sorted by last activity",
				expected,
				threadDao.getThreadsForForum(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER));

		expected.clear();
		expected.addAll(Arrays.asList(createdThreads.get(1), createdThreads.get(2), createdThreads.get(0)));
		assertEquals("sorted by last activity desc",
				expected,
				threadDao.getThreadsForForum(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, false, DiscussionFilter.NO_FILTER));
	}

	private List<DiscussionThreadBundle> createListOfThreads(int numberOfThreads) throws InterruptedException {
		List<DiscussionThreadBundle> createdThreads = new ArrayList<DiscussionThreadBundle>();
		for (int i = 0; i < numberOfThreads; i++) {
			Thread.sleep(1000);
			threadId = idGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID);
			DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(),
					"title", UUID.randomUUID().toString(), userId);
			createdThreads.add(dto);
		}
		return createdThreads;
	}

	@Test
	public void testUpdateActiveAuthors() {
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		long threadId = Long.parseLong(dto.getId());
		DiscussionThreadStat stat = new DiscussionThreadStat();
		stat.setThreadId(threadId);
		stat.setActiveAuthors(Arrays.asList(dto.getCreatedBy(), "123456"));
		threadDao.updateThreadStats(Arrays.asList(stat));
		dto.setActiveAuthors(Arrays.asList(dto.getCreatedBy(), "123456"));
		assertEquals(new HashSet<String>(dto.getActiveAuthors()),
				new HashSet<String>(threadDao.getThread(threadId, DEFAULT_FILTER).getActiveAuthors()));
	}

	@Test
	public void testCountThreadView() {
		// create some threads
		threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		Long threadId2 = idGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID);
		threadDao.createThread(forumId, threadId2 .toString(), "title", "messageKey2", userId);

		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		user.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
		userId2 = userGroupDAO.create(user);

		// one user viewed thread 1 twice
		threadDao.updateThreadView(threadId, userId2);
		threadDao.updateThreadView(threadId, userId2);
		// two users viewed thread 2
		threadDao.updateThreadView(threadId2, userId);
		threadDao.updateThreadView(threadId2, userId2);

		assertEquals(1L, threadDao.countThreadView(threadId));
		assertEquals(2L, threadDao.countThreadView(threadId2));
	}

	@Test
	public void testCountThreadViewForNonExistingThread() {
		assertEquals(0L, threadDao.countThreadView(threadId));
	}

	@Test
	public void testCountThreadViewForExistingThreadZeroView() {
		threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		assertEquals(0L, threadDao.countThreadView(threadId));
	}

	@Test
	public void testGetAllThreadId() {
		// create some threads
		threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		Long threadId2 = idGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID);
		threadDao.createThread(forumId, threadId2 .toString(), "title", "messageKey2", userId);

		assertTrue(threadDao.getAllThreadId(10L, 0L).containsAll(Arrays.asList(threadId, threadId2)));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetThreadCountForForumWithNullFilter() {
		threadDao.getThreadCountForForum(forumIdLong, null);
	}

	@Test
	public void testGetDeletedAndNonDeletedThreadCount() throws InterruptedException{
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(3);
		threadDao.markThreadAsDeleted(Long.parseLong(createdThreads.get(1).getId()));

		assertEquals(1, threadDao.getThreadCountForForum(forumIdLong, DiscussionFilter.DELETED_ONLY));
		assertEquals(2, threadDao.getThreadCountForForum(forumIdLong, DiscussionFilter.EXCLUDE_DELETED));
	}

	@Test
	public void testGetDeletedAndNonDeletedThreads() throws InterruptedException{
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(3);
		threadDao.markThreadAsDeleted(Long.parseLong(createdThreads.get(1).getId()));

		List<DiscussionThreadBundle> deletedThreads = threadDao.getThreadsForForum(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.DELETED_ONLY);
		List<DiscussionThreadBundle> availableThreads = threadDao.getThreadsForForum(forumIdLong, MAX_LIMIT, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.EXCLUDE_DELETED);
		assertEquals(createdThreads.get(1).getId(), deletedThreads.get(0).getId());

		assertTrue(availableThreads.contains(createdThreads.get(0)));
		assertTrue(availableThreads.contains(createdThreads.get(2)));
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
		String baseQuery = DBODiscussionThreadDAOImpl.SQL_SELECT_THREADS_BY_FORUM_ID;
		assertEquals("not ordered","SELECT DISCUSSION_THREAD.ID AS ID,"
				+ " DISCUSSION_THREAD.FORUM_ID AS FORUM_ID, FORUM.OBJECT_ID AS OBJECT_ID, FORUM.OBJECT_TYPE AS OBJECT_TYPE,"
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
				DBODiscussionThreadDAOImpl.buildGetQuery(baseQuery, 10L, 0L, null, null, DiscussionFilter.NO_FILTER));
		assertEquals("ordered by pinned and last activity","SELECT DISCUSSION_THREAD.ID AS ID,"
				+ " DISCUSSION_THREAD.FORUM_ID AS FORUM_ID, FORUM.OBJECT_ID AS OBJECT_ID, FORUM.OBJECT_TYPE AS OBJECT_TYPE,"
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
				DBODiscussionThreadDAOImpl.buildGetQuery(baseQuery, 10L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER));
		assertEquals("limit","SELECT DISCUSSION_THREAD.ID AS ID,"
				+ " DISCUSSION_THREAD.FORUM_ID AS FORUM_ID, FORUM.OBJECT_ID AS OBJECT_ID, FORUM.OBJECT_TYPE AS OBJECT_TYPE,"
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
				DBODiscussionThreadDAOImpl.buildGetQuery(baseQuery, 100L, 0L, null, null, DiscussionFilter.NO_FILTER));
		assertEquals("offset","SELECT DISCUSSION_THREAD.ID AS ID,"
				+ " DISCUSSION_THREAD.FORUM_ID AS FORUM_ID, FORUM.OBJECT_ID AS OBJECT_ID, FORUM.OBJECT_TYPE AS OBJECT_TYPE,"
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
				DBODiscussionThreadDAOImpl.buildGetQuery(baseQuery, 10L, 2L, null, null, DiscussionFilter.NO_FILTER));
		assertEquals("filtered","SELECT DISCUSSION_THREAD.ID AS ID,"
				+ " DISCUSSION_THREAD.FORUM_ID AS FORUM_ID, FORUM.OBJECT_ID AS OBJECT_ID, FORUM.OBJECT_TYPE AS OBJECT_TYPE,"
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
				DBODiscussionThreadDAOImpl.buildGetQuery(baseQuery, 10L, 0L, null, null, DiscussionFilter.DELETED_ONLY));
	}

	@Test
	public void testUpdateThreadViewTriggerThreadEtagChange() {
		DiscussionThreadBundle threadBundle = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		threadDao.updateThreadView(threadId, userId);
		DiscussionThreadBundle updated = threadDao.getThread(threadId, DEFAULT_FILTER);
		assertFalse(threadBundle.getEtag().equals(updated.getEtag()));
	}

	@Test (expected = NotFoundException.class)
	public void testGetAuthorForUpdateDeletedThread() throws InterruptedException {
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		long threadId = Long.parseLong(dto.getId());
		threadDao.markThreadAsDeleted(threadId);
		threadDao.getAuthorForUpdate(""+threadId);
	}

	@Test (expected = NotFoundException.class)
	public void testIsThreadDeletedForNonExistingThread() {
		threadDao.isThreadDeleted(threadId.toString());
	}

	@Test
	public void testIsThreadDeletedForNotDeletedThread() {
		threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		assertEquals(false, threadDao.isThreadDeleted(threadId.toString()));
	}

	@Test
	public void testIsThreadDeletedForDeletedThread() {
		threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		threadDao.markThreadAsDeleted(threadId);
		assertEquals(true, threadDao.isThreadDeleted(threadId.toString()));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetThreadCountsNullEntityIdList(){
		threadDao.getThreadCounts(null, new HashSet<Long>());
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetThreadCountsNullProjectSet(){
		threadDao.getThreadCounts(new ArrayList<Long>(), null);
	}

	@Test
	public void testGetThreadCountsEmptyEntityIdList(){
		Set<Long> projectIds = new HashSet<Long>();
		projectIds.add(1L);
		EntityThreadCounts threadCounts = threadDao.getThreadCounts(new ArrayList<Long>(), projectIds);
		assertNotNull(threadCounts);
		List<EntityThreadCount> list = threadCounts.getList();
		assertNotNull(list);
		assertTrue(list.isEmpty());
	}

	@Test
	public void testGetThreadCountsEmptyProjectIdSet(){
		List<Long> entityIds = new ArrayList<Long>();
		entityIds.add(1L);
		EntityThreadCounts threadCounts = threadDao.getThreadCounts(entityIds, new HashSet<Long>());
		assertNotNull(threadCounts);
		List<EntityThreadCount> list = threadCounts.getList();
		assertNotNull(list);
		assertTrue(list.isEmpty());
	}

	@Test
	public void testGetThreadCountsForEntityWithoutReference(){
		Long projectIdLong = KeyFactory.stringToKey(projectId);
		List<Long> entityIds = new ArrayList<Long>();
		entityIds.add(projectIdLong);
		Set<Long> projectIds = new HashSet<Long>();
		projectIds.add(projectIdLong);
		EntityThreadCounts threadCounts = threadDao.getThreadCounts(entityIds, projectIds);
		assertNotNull(threadCounts);
		List<EntityThreadCount> list = threadCounts.getList();
		assertNotNull(list);
		assertTrue(list.isEmpty());
	}

	@Test
	public void testGetThreadCountsForEntityNotInProjectList() {
		Long projectIdLong = KeyFactory.stringToKey(projectId);
		List<Long> entityIds = new ArrayList<Long>();
		entityIds.add(projectIdLong);
		Set<Long> projectIds = new HashSet<Long>();
		projectIds.add(1L);
		threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		DiscussionThreadEntityReference entityRef = createEntityRef(threadId.toString(), projectId);
		threadDao.insertEntityReference(Arrays.asList(entityRef));
		EntityThreadCounts threadCounts = threadDao.getThreadCounts(entityIds, projectIds);
		assertNotNull(threadCounts);
		List<EntityThreadCount> list = threadCounts.getList();
		assertNotNull(list);
		assertEquals(list.size(), 0);
	}

	@Test
	public void testGetThreadCountsForOneEntity() {
		Long projectIdLong = KeyFactory.stringToKey(projectId);
		List<Long> entityIds = new ArrayList<Long>();
		entityIds.add(projectIdLong);
		Set<Long> projectIds = new HashSet<Long>();
		projectIds.add(projectIdLong);
		threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		DiscussionThreadEntityReference entityRef = createEntityRef(threadId.toString(), projectId);
		threadDao.insertEntityReference(Arrays.asList(entityRef));
		EntityThreadCounts threadCounts = threadDao.getThreadCounts(entityIds, projectIds);
		assertNotNull(threadCounts);
		List<EntityThreadCount> list = threadCounts.getList();
		assertNotNull(list);
		assertEquals(list.size(), 1);
		assertEquals(list.get(0).getEntityId(), projectId);
		assertEquals(list.get(0).getCount(), (Long) 1L);
	}

	@Test
	public void testGetThreadCountsForMultipleEntities() {
		Long entity1 = 1L;
		Long entity2 = 2L;
		Long entity3 = 3L;
		Long threadId2 = idGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID);
		threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		threadDao.createThread(forumId, threadId2.toString(), "title", "messageKey2", userId);
		Set<Long> projectIds = new HashSet<Long>();
		projectIds.add(KeyFactory.stringToKey(projectId));

		// entity1 is mentioned by 2 threads
		DiscussionThreadEntityReference entityRef1 = createEntityRef(threadId.toString(), entity1.toString());
		DiscussionThreadEntityReference entityRef2 = createEntityRef(threadId2.toString(), entity1.toString());
		// entity2 is mentioned by 1 thread twice
		DiscussionThreadEntityReference entityRef3 = createEntityRef(threadId.toString(), entity2.toString());
		DiscussionThreadEntityReference entityRef4 = createEntityRef(threadId.toString(), entity2.toString());
		// entity3 is not mentioned
		threadDao.insertEntityReference(Arrays.asList(entityRef1, entityRef2, entityRef3, entityRef4));

		EntityThreadCounts threadCounts = threadDao.getThreadCounts(Arrays.asList(entity1, entity2, entity3), projectIds);
		assertNotNull(threadCounts);
		List<EntityThreadCount> list = threadCounts.getList();
		assertNotNull(list);
		assertEquals(list.size(), 2);

		EntityThreadCount entity1Count = createEntityThreadCount(KeyFactory.keyToString(entity1), 2L);
		EntityThreadCount entity2Count = createEntityThreadCount(KeyFactory.keyToString(entity2), 1L);

		assertTrue(list.contains(entity1Count));
		assertTrue(list.contains(entity2Count));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetThreadCountForEntityWithNullFilter() {
		threadDao.getThreadCountForEntity(KeyFactory.stringToKey(projectId), null, new HashSet<Long>());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetThreadCountForEntityWithNullProjectIds() {
		threadDao.getThreadCountForEntity(KeyFactory.stringToKey(projectId), DEFAULT_FILTER, null);
	}

	@Test
	public void testGetThreadCountForEntityWithEmptyProjectIds() {
		assertEquals(0L, threadDao.getThreadCountForEntity(KeyFactory.stringToKey(projectId), DEFAULT_FILTER, new HashSet<Long>()));
	}

	@Test
	public void testGetThreadCountForEntityWithNoReference() {
		Set<Long> projectIds = new HashSet<Long>();
		projectIds.add(KeyFactory.stringToKey(projectId));
		assertEquals(0L, threadDao.getThreadCountForEntity(KeyFactory.stringToKey(projectId), DEFAULT_FILTER, projectIds));
	}

	@Test
	public void testGetThreadCountForEntityNotInProjectIdList() {
		Set<Long> projectIds = new HashSet<Long>();
		projectIds.add(1L);
		threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		DiscussionThreadEntityReference entityRef = createEntityRef(threadId.toString(), projectId);
		threadDao.insertEntityReference(Arrays.asList(entityRef));

		assertEquals(0L, threadDao.getThreadCountForEntity(KeyFactory.stringToKey(projectId), DEFAULT_FILTER, projectIds));
	}

	@Test
	public void testGetThreadCountForEntityWithReferences() {
		Set<Long> projectIds = new HashSet<Long>();
		projectIds.add(KeyFactory.stringToKey(projectId));
		Long threadId2 = idGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID);
		threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		threadDao.createThread(forumId, threadId2.toString(), "title", "messageKey2", userId);

		DiscussionThreadEntityReference entityRef1 = createEntityRef(threadId.toString(), projectId);
		DiscussionThreadEntityReference entityRef2 = createEntityRef(threadId2.toString(), projectId);
		threadDao.insertEntityReference(Arrays.asList(entityRef1, entityRef2));

		assertEquals(2L, threadDao.getThreadCountForEntity(KeyFactory.stringToKey(projectId), DEFAULT_FILTER, projectIds));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetThreadForEntityWithNullProjectIds(){
		threadDao.getThreadsForEntity(KeyFactory.stringToKey(projectId), MAX_LIMIT, 0L, null, null, DEFAULT_FILTER, null);
	}

	@Test
	public void testGetThreadForEntityWithEmptyProjectIds(){
		List<DiscussionThreadBundle> result = threadDao.getThreadsForEntity(KeyFactory.stringToKey(projectId), MAX_LIMIT, 0L, null, null, DEFAULT_FILTER, new HashSet<Long>());
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	public void testGetThreadsForEntityWithNoReference() {
		Set<Long> projectIds = new HashSet<Long>();
		projectIds.add(KeyFactory.stringToKey(projectId));
		List<DiscussionThreadBundle> result = threadDao.getThreadsForEntity(KeyFactory.stringToKey(projectId), MAX_LIMIT, 0L, null, null, DEFAULT_FILTER, projectIds);
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	public void testGetThreadsForEntityNotInProjectIdList() {
		Set<Long> projectIds = new HashSet<Long>();
		projectIds.add(1L);
		threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		DiscussionThreadEntityReference entityRef = createEntityRef(threadId.toString(), projectId);
		threadDao.insertEntityReference(Arrays.asList(entityRef));

		List<DiscussionThreadBundle> result = threadDao.getThreadsForEntity(KeyFactory.stringToKey(projectId), MAX_LIMIT, 0L, null, null, DEFAULT_FILTER, projectIds);
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	public void testGetThreadsForEntityWithReferences() {
		Set<Long> projectIds = new HashSet<Long>();
		projectIds.add(KeyFactory.stringToKey(projectId));
		Long threadId2 = idGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID);
		DiscussionThreadBundle bundle1 = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		DiscussionThreadBundle bundle2 = threadDao.createThread(forumId, threadId2.toString(), "title", "messageKey2", userId);

		DiscussionThreadEntityReference entityRef1 = createEntityRef(threadId.toString(), projectId);
		DiscussionThreadEntityReference entityRef2 = createEntityRef(threadId2.toString(), projectId);
		threadDao.insertEntityReference(Arrays.asList(entityRef1, entityRef2));

		List<DiscussionThreadBundle> result = threadDao.getThreadsForEntity(KeyFactory.stringToKey(projectId), MAX_LIMIT, 0L, null, null, DEFAULT_FILTER, projectIds);
		assertNotNull(result);
		assertEquals(result.size(), 2L);
		assertTrue(result.contains(bundle1));
		assertTrue(result.contains(bundle2));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetProjectIdsWithNullEntityIdList(){
		threadDao.getDistinctProjectIdsOfThreadsReferencesEntityIds(null);
	}

	@Test
	public void testGetProjectIdsWithEmptyEntityIdList(){
		Set<Long> projectIds = threadDao.getDistinctProjectIdsOfThreadsReferencesEntityIds(new ArrayList<Long>());
		assertNotNull(projectIds);
		assertTrue(projectIds.isEmpty());
	}

	@Test
	public void testGetProjectIds(){
		Long projectIdLong = KeyFactory.stringToKey(projectId);
		List<Long> entityIds = new ArrayList<Long>();
		entityIds.add(projectIdLong);

		threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		DiscussionThreadEntityReference entityRef1 = createEntityRef(threadId.toString(), projectId);
		DiscussionThreadEntityReference entityRef2 = createEntityRef(threadId.toString(), projectId);
		threadDao.insertEntityReference(Arrays.asList(entityRef1, entityRef2));

		Set<Long> projectIds = threadDao.getDistinctProjectIdsOfThreadsReferencesEntityIds(entityIds);
		assertNotNull(projectIds);
		assertEquals(projectIds.size(), 1);
		assertTrue(projectIds.contains(projectIdLong));
	}

	private EntityThreadCount createEntityThreadCount(String entityId, long count) {
		EntityThreadCount threadCount = new EntityThreadCount();
		threadCount.setEntityId(entityId);
		threadCount.setCount(count);
		return threadCount;
	}

	private DiscussionThreadEntityReference createEntityRef(String threadId, String entityId) {
		DiscussionThreadEntityReference entityRef = new DiscussionThreadEntityReference();
		entityRef.setEntityId(entityId);
		entityRef.setThreadId(threadId);
		return entityRef;
	}

	@Test
	public void testGetThreadForSubmissionAndGetSubmissionIdForThread() {
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(projectId);
		rod.setType(RestrictableObjectType.ENTITY);

		ManagedACTAccessRequirement managedAR = new ManagedACTAccessRequirement();
		managedAR.setCreatedBy(String.valueOf(userId));
		managedAR.setCreatedOn(new Date());
		managedAR.setModifiedBy(String.valueOf(userId));
		managedAR.setModifiedOn(new Date());
		managedAR.setEtag("etag");
		managedAR.setAccessType(ACCESS_TYPE.DOWNLOAD);
		managedAR.setVersionNumber(1L);
		managedAR.setSubjectIds(Arrays.asList(rod));
		managedAR.setIsTwoFaRequired(false);
		managedAR = accessRequirementDAO.create(managedAR);
		managedARId = String.valueOf(managedAR.getId());

		Forum dto1 = forumDao.createForum(managedAR.getId().toString(), ForumObjectType.ACCESS_REQUIREMENT);
		arForumId = dto1.getId();
		arTheadId = idGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID);

		// Create a submission to reference
		ResearchProject rp = ResearchProjectTestUtils.createNewDto();
		rp.setAccessRequirementId(managedARId);
		rp = researchProjectDao.create(rp);
		researchProjectId = rp.getId();

		Request request = RequestTestUtils.createNewRequest();
		request.setAccessRequirementId(this.managedARId);
		request.setResearchProjectId(rp.getId());
		AccessorChange accessorChange = new AccessorChange();
		accessorChange.setUserId(String.valueOf(userId));
		accessorChange.setType(AccessType.GAIN_ACCESS);
		request.setAccessorChanges(Arrays.asList(accessorChange));
		request = requestDao.create(request);
		requestId = request.getId();

		Submission submission = new Submission();
		submission.setAccessRequirementId(this.managedARId);
		submission.setAccessRequirementVersion(managedAR.getVersionNumber());
		submission.setRequestId(request.getId());
		submission.setResearchProjectSnapshot(rp);
		submission.setAccessorChanges(Arrays.asList(accessorChange));
		submission.setSubmittedBy(String.valueOf(userId));
		submission.setSubmittedOn(new Date());
		submission.setModifiedBy(String.valueOf(userId));
		submission.setModifiedOn(new Date());
		submission.setState(SubmissionState.SUBMITTED);
		SubmissionStatus status = submissionDao.createSubmission(submission);
		String submissionId = status.getSubmissionId();

		DiscussionThreadBundle thread = threadDao.createThread(arForumId, arTheadId.toString(),
				"submissionId:" + submissionId, UUID.randomUUID().toString(), userId);
		// Insert the reference
		threadDao.insertSubmissionReference(thread.getId(), submissionId);

		// call under test
		Optional<DiscussionThreadBundle> result = threadDao.getThreadForSubmission(submissionId);
		assertTrue(result.isPresent());
		assertEquals(thread.getId(), result.get().getId());
		assertEquals(thread.getForumId(), result.get().getForumId());

		// call under test
		Optional<String> resultSubmissionId = threadDao.getSubmissionIdForThread(thread.getId());
		assertTrue(resultSubmissionId.isPresent());
		assertEquals(submissionId, resultSubmissionId.get());
	}

	@Test
	public void testGetThreadForSubmissionWithNonExistentSubmission() {
		// call under test
		assertFalse(threadDao.getThreadForSubmission("999999").isPresent());
	}

	@Test
	public void testGetSubmissionIdForNonExistingThread() {
		// call under test
		assertFalse(threadDao.getSubmissionIdForThread("999999").isPresent());
	}

	/**
	 * The two writes in updateThreadView (INSERT IGNORE INTO DISCUSSION_THREAD_VIEW
	 * then UPDATE DISCUSSION_THREAD SET ETAG) take an FK-induced shared lock on the
	 * parent thread row before upgrading to an exclusive lock; concurrent callers
	 * therefore deadlock on the upgrade. Each iteration deletes the prior view row
	 * so the INSERT IGNORE actually inserts and the FK shared lock is taken.
	 * ConcurrencyFailureException covers both DeadlockLoserDataAccessException
	 * (deadlock victim) and CannotAcquireLockException (lock-wait timeout).
	 */
	@Test
	public void testUpdateThreadViewWithConcurrentCallers() throws Exception {
		final int iterations = 200;

		UserGroup other = new UserGroup();
		other.setIsIndividual(true);
		other.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
		Long secondUserId = userGroupDAO.create(other);

		try {
			threadDao.createThread(forumId, threadId.toString(), "deadlock-title",
					"deadlock-key-" + UUID.randomUUID(), userId);

			ExecutorService pool = Executors.newFixedThreadPool(2);
			try {
				CountDownLatch start = new CountDownLatch(1);
				AtomicInteger deadlocks = new AtomicInteger();

				// call under test
				Future<?> fa = pool.submit(buildUpdateThreadViewLoop(start, userId, iterations, deadlocks));
				Future<?> fb = pool.submit(buildUpdateThreadViewLoop(start, secondUserId, iterations, deadlocks));

				start.countDown();

				fa.get(2, TimeUnit.MINUTES);
				fb.get(2, TimeUnit.MINUTES);

				if (deadlocks.get() > 0) {
					org.junit.Assert.fail("Observed " + deadlocks.get() + " deadlocks across "
							+ (iterations * 2) + " concurrent updateThreadView calls; "
							+ "updateThreadView is not safe under concurrency.");
				}
			} finally {
				pool.shutdownNow();
			}
		} finally {
			userGroupDAO.delete(secondUserId.toString());
		}
	}

	private Runnable buildUpdateThreadViewLoop(CountDownLatch start, Long viewerId, int iterations,
			AtomicInteger deadlocks) {
		final TransactionTemplate tx = new TransactionTemplate(transactionManager,
				new DefaultTransactionDefinition());
		return () -> {
			try {
				start.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			for (int i = 0; i < iterations; i++) {
				jdbcTemplate.update("DELETE FROM DISCUSSION_THREAD_VIEW WHERE THREAD_ID = ?", threadId);
				try {
					tx.executeWithoutResult(status -> threadDao.updateThreadView(threadId, viewerId));
				} catch (ConcurrencyFailureException e) {
					deadlocks.incrementAndGet();
				}
			}
		};
	}
}
