package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;

public class QueryControllerAutowireTest extends AbstractAutowiredControllerTestBase {
	@Autowired
	StackConfiguration config;
		
	@Autowired
	private QueryController controller;
	
	@Autowired
	private EntityManager entityManager;

	@Autowired
	public UserManager userManager;
	
	private Long adminUserId;
	private UserInfo adminUserInfo;
	private Long testUserId;
	private UserInfo testUserInfo;
	
	private List<String> toDelete;
	private HttpServletRequest mockRequest;
	
	@Before
	public void before() throws DatastoreException, NotFoundException{
		mockRequest = Mockito.mock(HttpServletRequest.class);
		toDelete = new LinkedList<String>();
		
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserInfo = userManager.getUserInfo(adminUserId);
		
		// The user can't be an admin, since admins can see trash-canned entities
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		testUserId = userManager.createUser(user);
		testUserInfo = userManager.getUserInfo(testUserId);
		testUserInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
	}
	
	@After
	public void after() throws Exception {
		for (String id : toDelete) {
			try {
				entityManager.deleteEntity(adminUserInfo, id);
			} catch (NotFoundException e) {
			}
		}
		
		userManager.deletePrincipal(adminUserInfo, testUserId);
	}
	
	
	@Test
	public void testQueryForRoot() throws Exception{
		// Only an admin can see the root node
		String query = "select id, eTag from entity where parentId == null";
		QueryResults results = controller.query(adminUserId, query, mockRequest);
		assertNotNull(results);
		assertEquals(0, results.getTotalNumberOfResults());
	}
	
	@Ignore
	@Test
	public void testQueryByPrincipal() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, ParseException, JSONObjectAdapterException{
		// Create a project
		Project p = new Project();
		p.setEntityType(Project.class.getName());
		p.setName("name");
		String id = entityManager.createEntity(testUserInfo, p, null);
		p.setId(id);
		toDelete.add(p.getId());
		// Now query for the data object
		String queryString = "SELECT id, name FROM project WHERE createdByPrincipalId == \""+testUserId+"\"";
		QueryResults results = controller.query(testUserId, queryString, mockRequest);
		assertNotNull(results);
		assertEquals(1l, results.getTotalNumberOfResults());
	}

}
