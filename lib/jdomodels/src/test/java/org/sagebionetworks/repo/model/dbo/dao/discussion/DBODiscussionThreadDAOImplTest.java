package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.junit.Assert.*;
import static org.sagebionetworks.repo.model.dbo.dao.discussion.DBODiscussionThreadDAOImpl.MAX_LIMIT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import org.sagebionetworks.repo.model.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.ForumDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
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
		assertEquals("check default number of views", dto.getNumberOfViews(), (Long) 0L);
		assertEquals("check default number of replies", dto.getNumberOfReplies(), (Long) 0L);
		assertEquals("check default last activity", dto.getLastActivity(), dto.getModifiedOn());
		assertEquals("check default active authors", dto.getActiveAuthors(), Arrays.asList(dto.getCreatedBy()));

		long threadId = Long.parseLong(dto.getId());
		assertEquals("getThread() should return the created one", dto, threadDao.getThread(threadId));
	}

	@Test
	public void testGetEtag(){
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		long threadId = Long.parseLong(dto.getId());
		assertNotNull(threadDao.getEtagForUpdate(threadId));
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
		DiscussionThreadBundle returnedDto = threadDao.getThread(threadId);
		assertFalse("after updating message url, modifiedOn should be different",
				dto.getModifiedOn().equals(returnedDto.getModifiedOn()));
		assertFalse("after updating message url, lastActivity should be different",
				dto.getLastActivity().equals(returnedDto.getLastActivity()));
		assertFalse("after updating message url, etag should be different",
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
		DiscussionThreadBundle returnedDto = threadDao.getThread(threadId);
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
		DiscussionThreadBundle returnedDto = threadDao.getThread(threadId);
		dto.setModifiedOn(returnedDto.getModifiedOn());
		dto.setLastActivity(returnedDto.getLastActivity());
		dto.setEtag(returnedDto.getEtag());
		assertEquals(dto, returnedDto);
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
	public void testGetThreadsLimitAndOffset() throws Exception {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(3);
		assertEquals(createdThreads.size(), 3);

		assertEquals(3, threadDao.getThreadCount(forumIdLong));

		assertEquals("non order",
				new HashSet<DiscussionThreadBundle>(createdThreads),
				new HashSet<DiscussionThreadBundle>(threadDao.getThreads(forumIdLong, null, MAX_LIMIT, 0).getResults()));

		assertEquals("order, all",
				createdThreads,
				threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, MAX_LIMIT, 0).getResults());
		assertEquals("order, second the third",
				Arrays.asList(createdThreads.get(1), createdThreads.get(2)),
				threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, 2, 1).getResults());
		assertEquals("order, last",
				Arrays.asList(createdThreads.get(2)),
				threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, 2, 2).getResults());
		assertEquals("order, out of range",
				Arrays.asList(),
				threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, 2, 3).getResults());
		assertEquals("order, on limit",
				Arrays.asList(createdThreads.get(1), createdThreads.get(2)),
				threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, DBODiscussionThreadDAOImpl.MAX_LIMIT, 1).getResults());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testNegativeOffset() {
		threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, 2, -3);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testNegativeLimit() {
		threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, -2, 3);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testOverLimit() {
		threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, DBODiscussionThreadDAOImpl.MAX_LIMIT+1, 3);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testNullOffset() {
		threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, 2, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testNullLimit() {
		threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, null, 2);
	}

	@Test
	public void testSetLastActivity() {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(3);

		Date date1 = new Date(2015-1900, 10, 19, 0, 0, 1);
		Date date2 = new Date(2015-1900, 10, 19, 0, 0, 2);
		Date date3 = new Date(2015-1900, 10, 19, 0, 0, 3);
		List<Date> lastActivities = Arrays.asList(date1, date3, date2);
		for (int i = 0; i < 3; i++) {
			threadDao.setLastActivity(Long.parseLong(createdThreads.get(i).getId()), lastActivities.get(i));
			createdThreads.get(i).setLastActivity(lastActivities.get(i));
		}

		List<DiscussionThreadBundle> expected = Arrays.asList(createdThreads.get(0), createdThreads.get(2), createdThreads.get(1));
		assertEquals("sorted by last activity",
				expected,
				threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, MAX_LIMIT, 0).getResults());

		expected = Arrays.asList(createdThreads.get(1), createdThreads.get(2), createdThreads.get(0));
		assertEquals("sorted by last activity desc",
				expected,
				threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY_DESC, MAX_LIMIT, 0).getResults());
	}

	@Test
	public void testSetNumberOfViews() {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(3);

		List<Long> numberOfViews = Arrays.asList(1L, 3L, 2L);
		for (int i = 0; i < 3; i++) {
			threadDao.setNumberOfViews(Long.parseLong(createdThreads.get(i).getId()), numberOfViews.get(i));
			createdThreads.get(i).setNumberOfViews(numberOfViews.get(i));
		}

		List<DiscussionThreadBundle> expected = Arrays.asList(createdThreads.get(0), createdThreads.get(2), createdThreads.get(1));
		assertEquals("sorted by number of views",
				expected,
				threadDao.getThreads(forumIdLong, DiscussionOrder.NUMBER_OF_VIEWS, MAX_LIMIT, 0).getResults());

		expected = Arrays.asList(createdThreads.get(1), createdThreads.get(2), createdThreads.get(0));
		assertEquals("sorted by number of views desc",
				expected,
				threadDao.getThreads(forumIdLong, DiscussionOrder.NUMBER_OF_VIEWS_DESC, MAX_LIMIT, 0).getResults());
	}

	@Test
	public void testSetNumberOfReplies() {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(3);

		List<Long> numberOfReplies = Arrays.asList(1L, 3L, 2L);
		for (int i = 0; i < 3; i++) {
			threadDao.setNumberOfReplies(Long.parseLong(createdThreads.get(i).getId()), numberOfReplies.get(i));
			createdThreads.get(i).setNumberOfReplies(numberOfReplies.get(i));
		}

		List<DiscussionThreadBundle> expected = Arrays.asList(createdThreads.get(0), createdThreads.get(2), createdThreads.get(1));
		assertEquals("sorted by number of replies",
				expected,
				threadDao.getThreads(forumIdLong, DiscussionOrder.NUMBER_OF_REPLIES, MAX_LIMIT, 0).getResults());

		expected = Arrays.asList(createdThreads.get(1), createdThreads.get(2), createdThreads.get(0));
		assertEquals("sorted by number of replies desc",
				expected,
				threadDao.getThreads(forumIdLong, DiscussionOrder.NUMBER_OF_REPLIES_DESC, MAX_LIMIT, 0).getResults());
	}

	private List<DiscussionThreadBundle> createListOfThreads(int numberOfThreads) {
		List<DiscussionThreadBundle> createdThreads = new ArrayList<DiscussionThreadBundle>();
		for (int i = 0; i < numberOfThreads; i++) {
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
		threadDao.setActiveAuthors(threadId, Arrays.asList(dto.getCreatedBy(), "123456"));
		dto.setActiveAuthors(Arrays.asList(dto.getCreatedBy(), "123456"));
		assertEquals(dto, threadDao.getThread(threadId));
	}

	@Test
	public void threadViewTest() {
		DiscussionThreadBundle dto = threadDao.createThread(forumId, threadId.toString(), "title", "messageKey", userId);
		long threadId = Long.parseLong(dto.getId());
		threadDao.updateThreadView(threadId, userId);
		assertEquals(1L, threadDao.countThreadView(threadId));
		threadDao.updateThreadView(threadId, userId);
		assertEquals(1L, threadDao.countThreadView(threadId));

		// create a user to create a project
		UserGroup user2 = new UserGroup();
		user2.setIsIndividual(true);
		userId2 = userGroupDAO.create(user2);

		threadDao.updateThreadView(threadId, userId2);
		assertEquals(2L, threadDao.countThreadView(threadId));
	}
}
