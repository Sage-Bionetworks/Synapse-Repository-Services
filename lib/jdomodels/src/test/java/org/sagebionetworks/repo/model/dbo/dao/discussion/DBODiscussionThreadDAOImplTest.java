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
	private String projectId = null;
	private String forumId;
	private Comparator<DiscussionThreadBundle> sortByModifiedOnDesc = new Comparator<DiscussionThreadBundle>(){

		@Override
		public int compare(DiscussionThreadBundle o1, DiscussionThreadBundle o2) {
			return -o1.getModifiedOn().compareTo(o2.getModifiedOn());
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
	}

	@After
	public void cleanup() {
		if (projectId != null) nodeDao.delete(projectId);
		if (userId != null) userGroupDAO.delete(userId.toString());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithInvalidDTO() {
		// TO DO: test each case
	}

	@Test
	public void testRoundTrip() {
		 DiscussionThreadBundle dto = threadDao.createThread(forumId, "title", "messageUrl", userId);

		long threadId = Long.parseLong(dto.getId());
		assertEquals(dto, threadDao.getThread(threadId));

		dto.setIsEdited(true);
		String newMessageUrl = "newMessageUrl";
		dto.setMessageUrl(newMessageUrl);
		threadDao.updateMessageUrl(threadId, newMessageUrl);
		DiscussionThreadBundle returnedDto = threadDao.getThread(threadId);
		assertFalse(dto.equals(returnedDto));
		dto.setModifiedOn(returnedDto.getModifiedOn());
		assertEquals(dto, returnedDto);

		String newTitle = "newTitle";
		dto.setTitle(newTitle);
		threadDao.updateTitle(threadId, newTitle);
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
		List<DiscussionThreadBundle> createdThreads = new ArrayList<DiscussionThreadBundle>();
		for (int i = 0; i < 10; i++) {
			DiscussionThreadBundle dto = threadDao.createThread(forumId, "title", 
					UUID.randomUUID().toString(), userId);
			createdThreads.add(dto);
		}
		assertEquals(createdThreads.size(), 10);

		long forumIdLong = Long.parseLong(forumId);
		assertEquals(10, threadDao.getThreadCount(forumIdLong));

		assertEquals("non order",
				new HashSet<DiscussionThreadBundle>(createdThreads),
				new HashSet<DiscussionThreadBundle>(threadDao.getThreads(forumIdLong, null, null, null).getResults()));

		Collections.sort(createdThreads, sortByModifiedOnDesc);
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
}
