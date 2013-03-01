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
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOActivityTest {
	
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
				params.addValue("id", id);
				dboBasicDao.deleteObjectById(DBOActivity.class, params);
			}
		}
	}
	
	@Before
	public void before(){
		toDelete = new LinkedList<Long>();
	}
	
	@Test
	public void testRoundTrip() throws DatastoreException, NotFoundException, UnsupportedEncodingException{
		DBOActivity activity = new DBOActivity();
		activity.setId(idGenerator.generateNewId());
		Long createdById = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		activity.setCreatedBy(createdById);
		activity.setCreatedOn(System.currentTimeMillis());
		activity.setModifiedBy(createdById);
		activity.setModifiedOn(System.currentTimeMillis());
		activity.seteTag("1");
		// Make sure we can create it
		DBOActivity clone = dboBasicDao.createNew(activity);
		assertNotNull(clone);
		toDelete.add(activity.getId());
		assertEquals(activity, clone);
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", activity.getId());
		clone = dboBasicDao.getObjectById(DBOActivity.class, params);
		assertNotNull(clone);
		assertEquals(activity, clone);
		
		// Make sure we can update it.
		clone.setSerializedObject(new byte[] { (byte)1 });
		clone.seteTag("2");
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		
		// Get the clone back again
		params = new MapSqlParameterSource();
		params.addValue("id", clone.getId());
		DBOActivity clone2 = dboBasicDao.getObjectById(DBOActivity.class, params);
		assertEquals(clone, clone2);
	}

}
