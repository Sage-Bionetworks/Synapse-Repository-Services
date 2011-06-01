package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FieldTypeDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class JDONodeQueryAuthorizationTest {

	@Autowired
	private NodeQueryDao nodeQueryDao;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private FieldTypeDAO fieldTypeDao;

	@Autowired
	UserGroupDAO userGroupDAO;

	// The users for this test
	List<String> usersToDelete;
	List<String> groupsToDelete;
	private UserInfo adminUser;

	@Before
	public void before() throws Exception {
		assertNotNull(nodeQueryDao);
		assertNotNull(nodeDao);
		assertNotNull(fieldTypeDao);
		assertNotNull(userGroupDAO);
		// Keeps track of the users to delete
		usersToDelete = new ArrayList<String>();
		groupsToDelete = new ArrayList<String>();
		// Create some users
		adminUser= createUser("admin@JDONodeQueryAuthorizationTest.org", true);
	}
	
	/**
	 * Helper for creating a new user.
	 * @param name
	 * @param isAdmin
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	private UserInfo createUser(String name, boolean isAdmin) throws DatastoreException, InvalidModelException, NotFoundException{
		User user = new User();
		user.setUserId(name);
		// Create a group for this user
		UserGroup group = new UserGroup();
		group.setName(name+"group");
		group.setIndividual(true);
		String id = userGroupDAO.create(group);
		groupsToDelete.add(id);
		group = userGroupDAO.get(id);
		UserInfo info = new UserInfo(isAdmin);
		info.setUser(user);
		info.setIndividualGroup(group);
		info.setGroups(new ArrayList<UserGroup>());
		info.getGroups().add(group);
		if(isAdmin){
			UserGroup adminGroup = userGroupDAO.findGroup(AuthorizationConstants.ADMIN_GROUP_NAME, false);
			info.getGroups().add(adminGroup);
		}
		return info;
	}

	@After
	public void after() {
		// Cleanup groups
		if (userGroupDAO != null) {
			if(groupsToDelete != null){
				for(String id: groupsToDelete){
					try{
						userGroupDAO.delete(id);
					}catch(Throwable e){
						
					}
				}
			}
		}

	}
	
	@Test
	public void test(){
		assertNotNull(adminUser);
	}

}
