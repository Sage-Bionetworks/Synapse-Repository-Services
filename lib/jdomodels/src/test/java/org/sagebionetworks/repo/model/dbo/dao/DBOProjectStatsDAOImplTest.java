package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectStat;
import org.sagebionetworks.repo.model.ProjectStatsDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOProjectStatsDAOImplTest {

	@Autowired
	NodeDAO nodeDao;

	@Autowired
	UserGroupDAO userGroupDAO;

	@Autowired
	ProjectStatsDAO projectStatsDao;

	private Long projectId1;
	private Long projectId2;

	private Long userId;

	@Before
	public void setup() throws Exception {
		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		user.setCreationDate(new Date());
		userId = userGroupDAO.create(user);

		Node project = new Node();
		project.setName("project1");
		project.setNodeType(EntityType.project);
		project.setCreatedByPrincipalId(userId);
		project.setCreatedOn(new Date());
		project.setModifiedByPrincipalId(userId);
		project.setModifiedOn(new Date());
		projectId1 = KeyFactory.stringToKey(nodeDao.createNew(project));
		project.setName("project2");
		projectId2 = KeyFactory.stringToKey(nodeDao.createNew(project));
	}

	@After
	public void teardown() throws Exception {
		if (projectId1 != null) {
			nodeDao.delete(projectId1.toString());
		}
		if (projectId2 != null) {
			nodeDao.delete(projectId2.toString());
		}
		if (userId != null) {
			userGroupDAO.delete(userId.toString());
		}
	}

	@Test
	public void testUpdateAndList() throws Exception {
		assertEquals(0, projectStatsDao.getProjectStatsForUser(userId).size());
		// project 1
		ProjectStat projectStat = new ProjectStat(projectId1, userId, new Date(1000));
		projectStatsDao.update(projectStat);

		// project 2
		projectStat = new ProjectStat(projectId2, userId, new Date(1000));
		projectStatsDao.update(projectStat);

		// project 1 update
		projectStat = new ProjectStat(projectId1, userId, new Date(2000));
		projectStatsDao.update(projectStat);

		List<ProjectStat> stats = projectStatsDao.getProjectStatsForUser(userId);
		assertEquals(2, stats.size());
		if (stats.get(0).getProjectId() == projectId1.longValue()) {
			assertEquals(new Date(2000), stats.get(0).getLastAccessed());
			assertEquals(new Date(1000), stats.get(1).getLastAccessed());
		} else {
			assertEquals(new Date(2000), stats.get(1).getLastAccessed());
			assertEquals(new Date(1000), stats.get(0).getLastAccessed());
		}
	}

	@Test
	public void testCascadeDeleteOnProject() throws Exception {
		assertEquals(0, projectStatsDao.getProjectStatsForUser(userId).size());

		ProjectStat projectStat = new ProjectStat(projectId1, userId, new Date(1000));
		projectStatsDao.update(projectStat);

		assertEquals(1, projectStatsDao.getProjectStatsForUser(userId).size());

		nodeDao.delete(projectId1.toString());
		projectId1 = null;

		assertEquals(0, projectStatsDao.getProjectStatsForUser(userId).size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLastAccessedMustBeSet() {
		ProjectStat projectStat = new ProjectStat(projectId1, userId, null);
		projectStatsDao.update(projectStat);
	}
}
