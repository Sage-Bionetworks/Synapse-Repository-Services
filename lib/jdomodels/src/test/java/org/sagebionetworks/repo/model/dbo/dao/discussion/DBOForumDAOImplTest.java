package org.sagebionetworks.repo.model.dbo.dao.discussion;


import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.ForumObjectType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:jdomodels-test-context.xml"})
public class DBOForumDAOImplTest {
	@Autowired
	private ForumDAO forumDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private NodeDAO nodeDao;

	private String userId = null;
	private String objectId = null;

	@BeforeEach
	public void before() {
		// create a user to create a project
		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		user.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
		userId = userGroupDAO.create(user).toString();
		// create a project
		Node project = NodeTestUtils.createNew("projectName" + "-" + new Random().nextInt(),
				Long.parseLong(userId));
		project.setParentId(StackConfigurationSingleton.singleton().getRootFolderEntityId());
		objectId = nodeDao.createNew(project);
	}

	@AfterEach
	public void cleanup() {
		if (objectId != null) nodeDao.delete(objectId);
		if (userId != null) userGroupDAO.delete(userId);
	}

	@Test
	public void testCreateGetDelete() {
		// create a forum
		Forum dto = forumDao.createForum(objectId, ForumObjectType.ENTITY);
		long forumId = Long.parseLong(dto.getId());

		// make sure we can find the forum created
		assertEquals(forumDao.getForum(forumId), dto);
		assertEquals(forumDao.getForumByObjectIdAndType(dto.getObjectId(), ForumObjectType.ENTITY), dto);

		// cannot create more than one forum for a project
		try {
			forumDao.createForum(objectId, ForumObjectType.ENTITY);
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
			forumDao.getForumByObjectIdAndType(dto.getObjectId(), ForumObjectType.ENTITY);
			fail("Should not be able to find a forum that has been deleted.");
		} catch (NotFoundException e) {
			// as expected
		}
	}

	@Test
	public void testKeyWithoutSynPrefix() {
		Forum dto = forumDao.createForum(KeyFactory.stringToKey(objectId).toString(), ForumObjectType.ENTITY);
		long forumId = Long.parseLong(dto.getId());
		//call under test
		assertEquals(forumDao.getForum(forumId), dto);
		assertEquals(forumDao.getForumByObjectIdAndType(dto.getObjectId(), ForumObjectType.ENTITY), dto);
		//Forum exists for project not for AR
		assertThrows(NotFoundException.class, () -> forumDao.getForumByObjectIdAndType(dto.getObjectId(), ForumObjectType.ACCESS_REQUIREMENT));
	}

	@Test
	public void testCreateWithNullObjectId() {
		assertThrows(IllegalArgumentException.class, () -> forumDao.createForum(null, ForumObjectType.ENTITY));
	}

	@Test
	public void testCreateWithNullObjectType() {
		assertThrows(IllegalArgumentException.class, () -> forumDao.createForum("123", null));
	}

	@Test
	public void testGetNullObjectId() {
		assertThrows(IllegalArgumentException.class, () -> forumDao.getForumByObjectIdAndType(null, ForumObjectType.ENTITY));
	}
	@Test
	public void testGetNullObjectType() {
		assertThrows(IllegalArgumentException.class, () -> forumDao.getForumByObjectIdAndType("123", null));
	}
}
