package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class UserGroupCacheImplTest {
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private UserGroupCache userGroupCache;
	
	private List<UserGroup> userGroups;
	
	@Before
	public void before() throws DatastoreException, InvalidModelException{
		userGroups = new ArrayList<UserGroup>();
		// Add a few users
		int number = 5;
		for(int i=0; i<number; i++){
			String name = "UserGroupCacheImplTest."+i;
			UserGroup ug = new UserGroup();
			ug.setCreationDate(new Date(System.currentTimeMillis()));
			ug.setName(name);
			ug.setIsIndividual(i % 2 == 0);
			if(ug.getIsIndividual()){
				ug.setName(ug.getName()+"@test.com");
			}
			String id  = userGroupDAO.create(ug);
			ug.setId(id);
			userGroups.add(ug);
		}
		
	}
	
	@After
	public void after() throws DatastoreException, NotFoundException{
		if(userGroups != null && userGroupDAO != null){
			for(UserGroup ug: userGroups){
				userGroupDAO.delete(ug.getId());
			}
		}
	}
	
	@Test
	public void testCache() throws DatastoreException, NotFoundException{
		assertNotNull(userGroupCache);
		// Look up even object using the ID and Odd using the name
		for(int i=0; i<userGroups.size(); i++){
			UserGroup ug = userGroups.get(i);
			Long expectedId = KeyFactory.stringToKey(ug.getId());
			if(i%2 == 0){
				Long id = userGroupCache.getIdForUserGroupName(ug.getName());
				assertNotNull(id);
				assertEquals(id, expectedId);
				// Try the other way
				String name = userGroupCache.getUserGroupNameForId(expectedId);
				assertNotNull(name);
				assertEquals(ug.getName(), name);
			}else{
				String name = userGroupCache.getUserGroupNameForId(expectedId);
				assertNotNull(name);
				assertEquals(ug.getName(), name);
				// try the other way
				Long id = userGroupCache.getIdForUserGroupName(ug.getName());
				assertNotNull(id);
				assertEquals(id, expectedId);
			}
		}
	}

}
