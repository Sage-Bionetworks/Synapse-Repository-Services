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
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOMembershipRqstSubmissionTest {
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Autowired
	private IdGenerator idGenerator;
		
	List<Long> toDelete = null;
	
	@After
	public void after() throws DatastoreException {
		if(dboBasicDao != null && toDelete != null){
			for(Long id: toDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBOMembershipRqstSubmission.class, params);
			}
		}
	}
	
	@Before
	public void before(){
		toDelete = new LinkedList<Long>();
	}
	
	@Test
	public void testRoundTrip() throws DatastoreException, NotFoundException, UnsupportedEncodingException{
		DBOMembershipRqstSubmission request = new DBOMembershipRqstSubmission();
		request.setId(idGenerator.generateNewId());
		request.setExpiresOn(System.currentTimeMillis());
		request.setEtag("1");
		request.setTeamId(10101L);
		request.setUserId(9999L);
		request.setProperties((new String("abcdefg")).getBytes());
		// Make sure we can create it
		DBOMembershipRqstSubmission clone = dboBasicDao.createNew(request);
		toDelete.add(request.getId());
		assertNotNull(clone);
		assertEquals(request, clone);
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", request.getId());
		clone = dboBasicDao.getObjectByPrimaryKey(DBOMembershipRqstSubmission.class, params);
		assertNotNull(clone);
		assertEquals(request, clone);
		
		// Make sure we can update it.
		clone.setProperties(new byte[] { (byte)1 });
		clone.setEtag("2");
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		
		// Get the clone back again
		params = new MapSqlParameterSource();
		params.addValue("id", clone.getId());
		DBOMembershipRqstSubmission clone2 = dboBasicDao.getObjectByPrimaryKey(DBOMembershipRqstSubmission.class, params);
		assertEquals(clone, clone2);
	}

}
