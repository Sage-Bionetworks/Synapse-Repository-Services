package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.EntityService;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class QueryControllerAutowireTest {
	
	@Autowired
	QueryController controller;
	
	@Autowired
	EntityManager entityManager;

	@Autowired
	public UserManager userManager;
	
	UserInfo user;
	List<String> toDelete;
	HttpServletRequest mockRequest;
	@Before
	public void before() throws DatastoreException, NotFoundException{
		mockRequest = Mockito.mock(HttpServletRequest.class);
		toDelete = new LinkedList<String>();
		user = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
	}
	
	@After
	public void after(){
		if(toDelete != null && entityManager != null){
			for(String id:toDelete){
				try {
					entityManager.deleteEntity(user, id);
				} catch (Exception e) {}
			}
		}
	}
	
	
	@Test
	public void testQueryForRoot() throws Exception{
		String query = "select id, eTag from entity where parentId == null";
		QueryResults results = controller.query(TestUserDAO.ADMIN_USER_NAME, query, mockRequest);
		assertNotNull(results);
		assertTrue(results.getTotalNumberOfResults() > 0);
	}
	
	@Test
	public void testPLFM_1272() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, ParseException, JSONObjectAdapterException{
		// Create a project
		Project p = new Project();
		p.setEntityType(Project.class.getName());
		p.setName("name");
		String id = entityManager.createEntity(user, p, null);
		p.setId(id);
		toDelete.add(p.getId());
		// Now add a data object 
		Data data = new Data();
		data.setParentId(p.getId());
		data.setName("data");
		data.setEntityType(Data.class.getName());
		id = entityManager.createEntity(user, data, null);
		data.setId(id);
		// Now query for the data object
		String queryString = "SELECT id, name FROM data WHERE data.parentId == \""+p.getId()+"\"";
		QueryResults results = controller.query(TestUserDAO.ADMIN_USER_NAME, queryString, mockRequest);
		assertNotNull(results);
		assertEquals(1l, results.getTotalNumberOfResults());
		
		queryString = "SELECT id, name FROM layer WHERE layer.parentId == \""+p.getId()+"\"";
		results = controller.query(TestUserDAO.ADMIN_USER_NAME, queryString, mockRequest);
		assertNotNull(results);
		assertEquals(1l, results.getTotalNumberOfResults());
	}
	
	@Test
	public void testQueryByPrincipal() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, ParseException, JSONObjectAdapterException{
		// Create a project
		Project p = new Project();
		p.setEntityType(Project.class.getName());
		p.setName("name");
		String id = entityManager.createEntity(user, p, null);
		p.setId(id);
		toDelete.add(p.getId());
		// Now query for the data object
		String queryString = "SELECT id, name FROM project WHERE createdByPrincipalId == \""+user.getIndividualGroup().getId()+"\"";
		QueryResults results = controller.query(TestUserDAO.ADMIN_USER_NAME, queryString, mockRequest);
		assertNotNull(results);
		assertEquals(1l, results.getTotalNumberOfResults());
	}

}
