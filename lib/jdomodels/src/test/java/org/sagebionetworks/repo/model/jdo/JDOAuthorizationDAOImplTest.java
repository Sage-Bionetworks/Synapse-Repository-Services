package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodles-test-context.xml" })
public class JDOAuthorizationDAOImplTest {

	@Autowired
	AuthorizationDAO authorizationDao;
	
	// things to delete in the after
	List<String> toDelete = new ArrayList<String>();
	
	@Before
	public void before(){
		assertNotNull(authorizationDao);
		toDelete = new ArrayList<String>();
	}
	
	@Test
	public void testStub(){
		
	}
}
