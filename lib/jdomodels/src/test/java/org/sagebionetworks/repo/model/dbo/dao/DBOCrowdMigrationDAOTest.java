package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOCrowdMigrationDAOTest {

	@Autowired
	private DBOCrowdMigrationDAO crowdMigrationDAO;

	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {
		
	}
	
	@Test
	public void testGetAllUsers() throws Exception {
		Long numUsers = crowdMigrationDAO.getCount();
		List<User> users = new ArrayList<User>();
		
		for (int i = 0; i < numUsers; i++) {
			users.addAll(crowdMigrationDAO.getUsersFromCrowd(1, i));
		}
		
		Assert.assertEquals(numUsers, new Long(users.size()));
	}
}
