package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

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
	public void testCreateUpdateMessageUrl() throws InterruptedException {
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
	}

	@Test
	public void testCreateUpdateTitle() throws InterruptedException {
		DiscussionThreadBundle dto = threadDao.createThread(forumId, "title", "messageUrl", userId);

		long threadId = Long.parseLong(dto.getId());

		String newTitle = "newTitle";
		dto.setIsEdited(true);
		dto.setTitle(newTitle);
		threadDao.updateTitle(threadId, newTitle);
		DiscussionThreadBundle returnedDto = threadDao.getThread(threadId);
		dto.setModifiedOn(returnedDto.getModifiedOn());
		dto.setLastActivity(returnedDto.getLastActivity());
		assertEquals(dto, returnedDto);
	}

	@Test
	public void testCreateDelete() throws InterruptedException {
		DiscussionThreadBundle dto = threadDao.createThread(forumId, "title", "messageUrl", userId);

		long threadId = Long.parseLong(dto.getId());

		dto.setIsDeleted(true);
		threadDao.deleteThread(threadId);
		DiscussionThreadBundle returnedDto = threadDao.getThread(threadId);
		dto.setModifiedOn(returnedDto.getModifiedOn());
		dto.setLastActivity(returnedDto.getLastActivity());
		assertEquals(dto, returnedDto);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateTitleWithInvalidArgument(){
		threadDao.updateTitle(1L, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateMessageUrlWithInvalidArgument(){
		threadDao.updateMessageUrl(1L, null);
	}

	@Test
	public void testGetThreads() throws Exception {
		List<DiscussionThreadBundle> createdThreads = createListOfThreads(3);
		assertEquals(createdThreads.size(), 3);

		assertEquals(3, threadDao.getThreadCount(forumIdLong));

		assertEquals("non order",
				new HashSet<DiscussionThreadBundle>(createdThreads),
				new HashSet<DiscussionThreadBundle>(threadDao.getThreads(forumIdLong, null, null, null).getResults()));

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
				threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, null, null).getResults());

		expected = Arrays.asList(createdThreads.get(1), createdThreads.get(2), createdThreads.get(0));
		assertEquals("sorted by last activity desc",
				expected,
				threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY_DESC, null, null).getResults());
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
				threadDao.getThreads(forumIdLong, DiscussionOrder.NUMBER_OF_VIEWS, null, null).getResults());

		expected = Arrays.asList(createdThreads.get(1), createdThreads.get(2), createdThreads.get(0));
		assertEquals("sorted by number of views desc",
				expected,
				threadDao.getThreads(forumIdLong, DiscussionOrder.NUMBER_OF_VIEWS_DESC, null, null).getResults());
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
				threadDao.getThreads(forumIdLong, DiscussionOrder.NUMBER_OF_REPLIES, null, null).getResults());

		expected = Arrays.asList(createdThreads.get(1), createdThreads.get(2), createdThreads.get(0));
		assertEquals("sorted by number of replies desc",
				expected,
				threadDao.getThreads(forumIdLong, DiscussionOrder.NUMBER_OF_REPLIES_DESC, null, null).getResults());
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
	public void testSetActiveAuthors() {
		DiscussionThreadBundle dto = threadDao.createThread(forumId, "title", "messageUrl", userId);
		long threadId = Long.parseLong(dto.getId());
		threadDao.setActiveAuthors(threadId, Arrays.asList(dto.getCreatedBy(), "123456"));
		dto.setActiveAuthors(Arrays.asList(dto.getCreatedBy(), "123456"));
		assertEquals(dto, threadDao.getThread(threadId));
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
