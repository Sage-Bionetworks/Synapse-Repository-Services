package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class QueryControllerAutowireTest {
	
	@Autowired
	QueryController controller;
	
	@Test
	public void testQueryForRoot() throws Exception{
		HttpServletRequest mockServlet = Mockito.mock(HttpServletRequest.class);
		String query = "select id, eTag from entity where parentId == null";
		QueryResults results = controller.query(TestUserDAO.ADMIN_USER_NAME, query, mockServlet);
		assertNotNull(results);
		assertTrue(results.getTotalNumberOfResults() > 0);
	}

}
