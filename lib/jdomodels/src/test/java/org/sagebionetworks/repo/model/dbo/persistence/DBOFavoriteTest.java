package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOFavoriteTest {
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private IdGenerator idGenerator;
	
	List<Long> toDelete = null;
	
	@After
	public void after() throws DatastoreException {
		if(dboBasicDao != null && toDelete != null){
			for(Long id: toDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("principalId", id);
				try {
				dboBasicDao.deleteObjectById(DBOFavorite.class, params);
				} catch (DatastoreException e) {
					// next
				}
			}
		}
	}
	
	@Before
	public void before(){
		toDelete = new LinkedList<Long>();
	}
	
	@Test
	public void testRoundTrip() throws DatastoreException, NotFoundException, UnsupportedEncodingException{
		DBOFavorite favorite = new DBOFavorite();
		favorite.setNodeId(idGenerator.generateNewId());
		Long createdById = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		favorite.setPrincipalId(createdById);
		favorite.setCreatedOn(System.currentTimeMillis());
		favorite.seteTag("1");
		// Make sure we can create it
		DBOFavorite clone = dboBasicDao.createNew(favorite);
		assertNotNull(clone);
		toDelete.add(favorite.getPrincipalId());
		assertEquals(favorite, clone);
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("principalId", favorite.getPrincipalId());
		clone = dboBasicDao.getObjectById(DBOFavorite.class, params);
		assertNotNull(clone);
		assertEquals(favorite, clone);
		
		// Make sure we can update it.
		favorite.setCreatedOn(System.currentTimeMillis() + 100000);
		clone.seteTag("2");
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		
		// Get the clone back again
		params = new MapSqlParameterSource();
		params.addValue("principalId", clone.getPrincipalId());
		DBOFavorite clone2 = dboBasicDao.getObjectById(DBOFavorite.class, params);
		assertEquals(clone, clone2);
	}

}
