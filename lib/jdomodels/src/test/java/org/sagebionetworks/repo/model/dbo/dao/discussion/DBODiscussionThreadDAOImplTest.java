package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
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

	private Long userId = null;
	private Long userId2 = null;
	private String projectId = null;
	private String forumId;
	private long forumIdLong;
	private Comparator<DiscussionThreadBundle> sortByLastActivityDesc = new Comparator<DiscussionThreadBundle>(){

		@Override
		public int compare(DiscussionThreadBundle o1, DiscussionThreadBundle o2) {
			return -o1.getLastActivity().compareTo(o2.getLastActivity());
		}
	};
	private Comparator<DiscussionThreadBundle> sortByNumberOfViews = new Comparator<DiscussionThreadBundle>(){

		@Override
		public int compare(DiscussionThreadBundle o1, DiscussionThreadBundle o2) {
			return -o1.getNumberOfViews().compareTo(o2.getNumberOfViews());
		}
	};
	private Comparator<DiscussionThreadBundle> sortByNumberOfReplies = new Comparator<DiscussionThreadBundle>(){

		@Override
		public int compare(DiscussionThreadBundle o1, DiscussionThreadBundle o2) {
			return -o1.getNumberOfReplies().compareTo(o2.getNumberOfReplies());
		}
	};

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
	}

	@After
	public void cleanup() {
		if (projectId != null) nodeDao.delete(projectId);
		if (userId != null) userGroupDAO.delete(userId.toString());
		if (userId2 != null) userGroupDAO.delete(userId.toString());
	}

	@Test
	public void testCreateWithInvalidArguments() {
		try {
			threadDao.createThread(null, "title", "messageUrl", userId);
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			threadDao.createThread(forumId, null, "messageUrl", userId);
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			threadDao.createThread(forumId, "title", null, userId);
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	@Test
	public void testRoundTrip() throws InterruptedException {
		DiscussionThreadBundle dto = threadDao.createThread(forumId, "title", "messageUrl", userId);

		long threadId = Long.parseLong(dto.getId());
		assertEquals("getThread() should return the created one", dto, threadDao.getThread(threadId));

		Thread.sleep(1000);
		dto.setIsEdited(true);
		String newMessageUrl = UUID.randomUUID().toString();
		dto.setMessageUrl(newMessageUrl);
		threadDao.updateMessageUrl(threadId, newMessageUrl);
		DiscussionThreadBundle returnedDto = threadDao.getThread(threadId);
		assertFalse("after updating message url, modifiedOn should be different", dto.equals(returnedDto));
		dto.setModifiedOn(returnedDto.getModifiedOn());
		dto.setLastActivity(returnedDto.getLastActivity());
		assertEquals(dto, returnedDto);

		String newTitle = "newTitle";
		dto.setTitle(newTitle);
		threadDao.updateTitle(threadId, newTitle);
		returnedDto = threadDao.getThread(threadId);
		dto.setModifiedOn(returnedDto.getModifiedOn());
		dto.setLastActivity(returnedDto.getLastActivity());
		assertEquals(dto, returnedDto);

		dto.setIsDeleted(true);
		threadDao.deleteThread(threadId);
		returnedDto = threadDao.getThread(threadId);
		dto.setModifiedOn(returnedDto.getModifiedOn());
		dto.setLastActivity(returnedDto.getLastActivity());
		assertEquals(dto, returnedDto);

		try {
			threadDao.updateTitle(threadId, null);
			fail("Must throw exception when the title is null");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			threadDao.updateMessageUrl(threadId, null);
			fail("Must throw exception when the message Url is null");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	@Test
	public void testGetThreads() throws Exception {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(10);
		assertEquals(createdThreads.size(), 10);

		assertEquals(10, threadDao.getThreadCount(forumIdLong));

		assertEquals("non order",
				new HashSet<DiscussionThreadBundle>(createdThreads),
				new HashSet<DiscussionThreadBundle>(threadDao.getThreads(forumIdLong, null, null, null).getResults()));

		Collections.sort(createdThreads, sortByLastActivityDesc);
		assertEquals("order, all",
				createdThreads,
				threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, null, null).getResults());
		assertEquals("order, second the third",
				Arrays.asList(createdThreads.get(1), createdThreads.get(2)),
				threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, 2, 1).getResults());
		assertEquals("order, last",
				Arrays.asList(createdThreads.get(9)),
				threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, 2, 9).getResults());
		assertEquals("order, out of range",
				Arrays.asList(),
				threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, 2, 10).getResults());
		assertEquals("order, on limit",
				Arrays.asList(createdThreads.get(8), createdThreads.get(9)),
				threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, DBODiscussionThreadDAOImpl.MAX_LIMIT, 8).getResults());

		try {
			threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, 2, -3);
			fail("Must throw exception when limit or offset smaller than zero");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, -2, 3);
			fail("Must throw exception when limit or offset smaller than zero");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, DBODiscussionThreadDAOImpl.MAX_LIMIT+1, 3);
			fail("Must throw exception when limit greater to max limit");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, 2, null);
			fail("Must throw exception when limit is not null and offset is null");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, null, 2);
			fail("Must throw exception when limit is null and offset is not null");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	@Test
	public void testSetLastActivity() {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(10);

		List<Long> lastActivities = Arrays.asList(3L, 4L, 2L, 9L, 7L, 8L, 1L, 0L, 5L, 6L);
		for (int i = 0; i < 10; i++) {
			threadDao.setLastActivity(Long.parseLong(createdThreads.get(i).getId()), lastActivities.get(i));
		}
		Collections.sort(createdThreads, sortByLastActivityDesc);
		assertEquals("sorted by last activity",
				createdThreads,
				threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, null, null).getResults());
	}

	@Test
	public void testSetNumberOfViews() {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(10);

		List<Long> numberOfViews = Arrays.asList(3L, 5L, 2L, 9L, 1L, 8L, 7L, 0L, 4L, 6L);
		for (int i = 0; i < 10; i++) {
			threadDao.setNumberOfViews(Long.parseLong(createdThreads.get(i).getId()), numberOfViews.get(i));
		}
		Collections.sort(createdThreads, sortByNumberOfViews);
		assertEquals("sorted by number of views",
				createdThreads,
				threadDao.getThreads(forumIdLong, DiscussionOrder.NUMBER_OF_VIEWS, null, null).getResults());
	}

	@Test
	public void testSetNumberOfReplies() {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(10);

		List<Long> numberOfReplies = Arrays.asList(4L, 3L, 2L, 6L, 7L, 0L, 1L, 8L, 5L, 9L);
		for (int i = 0; i < 10; i++) {
			threadDao.setNumberOfReplies(Long.parseLong(createdThreads.get(i).getId()), numberOfReplies.get(i));
		}
		Collections.sort(createdThreads, sortByNumberOfReplies);
		assertEquals("sorted by number of replies",
				createdThreads,
				threadDao.getThreads(forumIdLong, DiscussionOrder.NUMBER_OF_REPLIES, null, null).getResults());
	}

	private List<DiscussionThreadBundle> createListOfThreads(int numberOfThreads) {
		List<DiscussionThreadBundle> createdThreads = new ArrayList<DiscussionThreadBundle>();
		for (int i = 0; i < numberOfThreads; i++) {
			DiscussionThreadBundle dto = threadDao.createThread(forumId, "title", 
					UUID.randomUUID().toString(), userId);
			createdThreads.add(dto);
		}
		return createdThreads;
	}

	
	@Test
	public void threadViewTest() {
		DiscussionThreadBundle dto = threadDao.createThread(forumId, "title", "messageUrl", userId);
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
