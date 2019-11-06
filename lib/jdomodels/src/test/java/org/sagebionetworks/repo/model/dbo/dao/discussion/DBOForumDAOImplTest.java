package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOForumDAOImplTest {
	@Autowired
	private ForumDAO forumDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private NodeDAO nodeDao;

	private String userId = null;
	private String projectId = null;

	@Before
	public void before() {
		// create a user to create a project
		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		userId = userGroupDAO.create(user).toString();
		// create a project
		Node project = NodeTestUtils.createNew("projectName" + "-" + new Random().nextInt(),
				Long.parseLong(userId));
		project.setParentId(StackConfigurationSingleton.singleton().getRootFolderEntityId());
		projectId = nodeDao.createNew(project);
	}

	@After
	public void cleanup() {
		if (projectId != null) nodeDao.delete(projectId);
		if (userId != null) userGroupDAO.delete(userId);
	}

	@Test
	public void testCreateGetDelete() {
		// create a forum
		Forum dto = forumDao.createForum(projectId);
		long forumId = Long.parseLong(dto.getId());

		// make sure we can find the forum created
		assertEquals(forumDao.getForum(forumId), dto);
		assertEquals(forumDao.getForumByProjectId(dto.getProjectId()), dto);

		// cannot create more than one forum for a project
		try {
			forumDao.createForum(projectId);
		} catch (IllegalArgumentException e) {
			// as expected
		}

		// delete the forum
		forumDao.deleteForum(forumId);
		// make sure that we can no longer find it
		try {
			forumDao.getForum(forumId);
			fail("Should not be able to find a forum that has been deleted.");
		} catch (NotFoundException e) {
			// as expected
		}
		// make sure that we can no longer find it with the project Id
		try {
			forumDao.getForumByProjectId(dto.getProjectId());
			fail("Should not be able to find a forum that has been deleted.");
		} catch (NotFoundException e) {
			// as expected
		}
	}

	@Test
	public void testKeyWithoutSynPrefix() {
		Forum dto = forumDao.createForum(KeyFactory.stringToKey(projectId).toString());
		long forumId = Long.parseLong(dto.getId());
		assertEquals(forumDao.getForum(forumId), dto);
		assertEquals(forumDao.getForumByProjectId(dto.getProjectId()), dto);
	}

	@Test (expected = IllegalArgumentException.class)
	public void createWithNullProjectId() {
		forumDao.createForum(null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void createGetNullProjectId() {
		forumDao.getForumByProjectId(null);
	}
}
