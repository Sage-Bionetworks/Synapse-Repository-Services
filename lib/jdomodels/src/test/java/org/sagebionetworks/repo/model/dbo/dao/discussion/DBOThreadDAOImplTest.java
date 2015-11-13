package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.ForumDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ThreadDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.ThreadTestUtil;
import org.sagebionetworks.repo.model.discussion.DiscussionOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThread;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOThreadDAOImplTest {

	private static final Charset UTF8 = Charset.forName("UTF8");
	@Autowired
	private ForumDAO forumDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private ThreadDAO threadDao;

	private String userId = null;
	private String projectId = null;
	private String forumId;
	private Comparator<DiscussionThread> sortByModifiedOn = new Comparator<DiscussionThread>(){

		@Override
		public int compare(DiscussionThread o1, DiscussionThread o2) {
			return -o1.getModifiedOn().compareTo(o2.getModifiedOn());
		}
	};

	@Before
	public void before() {
		// create a user to create a project
		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		userId = userGroupDAO.create(user).toString();
		// create a project
		Node project = NodeTestUtils.createNew("projectName" + "-" + new Random().nextInt(),
				Long.parseLong(userId));
		project.setParentId(StackConfiguration.getRootFolderEntityIdStatic());
		projectId = nodeDao.createNew(project);
		// create a forum
		Forum dto = forumDao.createForum(projectId);
		forumId = dto.getId();
	}

	@After
	public void cleanup() {
		if (projectId != null) nodeDao.delete(projectId);
		if (userId != null) userGroupDAO.delete(userId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithInvalidDTO() {
		DiscussionThread dto = ThreadTestUtil.createValidThread();
		dto.setForumId(forumId);
		dto.setTitle(null);
		threadDao.createThread(dto);
	}

	@Test
	public void testRoundTrip() {
		DiscussionThread dto = ThreadTestUtil.createValidThread();
		dto.setForumId(forumId);
		dto.setCreatedBy(userId);
		dto = threadDao.createThread(dto);

		long threadId = Long.parseLong(dto.getId());
		assertEquals(dto, threadDao.getThread(threadId));

		dto.setIsEdited(true);
		String newMessageUrl = "newMessageUrl";
		dto.setMessageUrl(newMessageUrl);
		threadDao.updateMessageUrl(threadId, newMessageUrl);
		DiscussionThread returnedDto = threadDao.getThread(threadId);
		assertFalse(dto.equals(returnedDto));
		dto.setModifiedOn(returnedDto.getModifiedOn());
		assertEquals(dto, returnedDto);

		String newTitle = "newTitle";
		dto.setTitle(newTitle);
		threadDao.updateTitle(threadId, newTitle.getBytes(UTF8));
		returnedDto = threadDao.getThread(threadId);
		assertFalse(dto.equals(returnedDto));
		dto.setModifiedOn(returnedDto.getModifiedOn());
		assertEquals(dto, returnedDto);

		dto.setIsDeleted(true);
		threadDao.deleteThread(threadId);
		returnedDto = threadDao.getThread(threadId);
		assertFalse(dto.equals(returnedDto));
		dto.setModifiedOn(returnedDto.getModifiedOn());
		assertEquals(dto, returnedDto);
	}

	@Test
	public void testGetThreads() throws Exception {
		List<DiscussionThread> threadsToCreate = ThreadTestUtil.createThreadList(10, forumId, userId);
		List<DiscussionThread> createdThreads = new ArrayList<DiscussionThread>();
		for (DiscussionThread thread : threadsToCreate) {
			DiscussionThread dto = threadDao.createThread(thread);
			createdThreads.add(dto);
		}
		assertEquals(threadsToCreate, createdThreads);

		long forumIdLong = Long.parseLong(forumId);
		assertEquals(10, threadDao.getThreadCount(forumIdLong));

		assertEquals("non order",
				new HashSet<DiscussionThread>(createdThreads),
				new HashSet<DiscussionThread>(threadDao.getThreads(forumIdLong, null, null, null).getResults()));

		Collections.sort(createdThreads, sortByModifiedOn);
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
				threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, DBOThreadDAOImpl.MAX_LIMIT, 8).getResults());

		try {
			threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, 2, -3);
			fail("Both limit and offset must be greater than zero");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, -2, 3);
			fail("Both limit and offset must be greater than zero");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, DBOThreadDAOImpl.MAX_LIMIT+1, 3);
			fail("Limit must be smaller or equal to max limit");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, -2, null);
			fail("Both limit and offset must be null or not null");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			threadDao.getThreads(forumIdLong, DiscussionOrder.LAST_ACTIVITY, null, 2);
			fail("Both limit and offset must be null or not null");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}
}
