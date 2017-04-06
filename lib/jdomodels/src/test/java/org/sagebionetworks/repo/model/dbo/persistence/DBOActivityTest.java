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
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
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
	private DBOBasicDao dboBasicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private List<Long> toDelete = null;
	
	@After
	public void after() throws DatastoreException {
		if(dboBasicDao != null && toDelete != null){
			for(Long id: toDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBOActivity.class, params);
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
		activity.setId(idGenerator.generateNewId(IdType.ACTIVITY_ID));
		Long createdById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		activity.setCreatedBy(createdById);
		activity.setCreatedOn(System.currentTimeMillis());
		activity.setModifiedBy(createdById);
		activity.setModifiedOn(System.currentTimeMillis());
		activity.seteTag("1");
		// Make sure we can create it
		DBOActivity clone = dboBasicDao.createNew(activity);
		toDelete.add(activity.getId());
		assertNotNull(clone);
		assertEquals(activity, clone);
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", activity.getId());
		clone = dboBasicDao.getObjectByPrimaryKey(DBOActivity.class, params);
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
		DBOActivity clone2 = dboBasicDao.getObjectByPrimaryKey(DBOActivity.class, params);
		assertEquals(clone, clone2);
	}

}
