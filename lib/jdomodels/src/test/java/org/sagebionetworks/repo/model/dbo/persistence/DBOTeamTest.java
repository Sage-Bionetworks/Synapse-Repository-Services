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
public class DBOTeamTest {
	
	@Autowired
	private DBOBasicDao dboBasicDao;
	
	List<Long> toDelete = null;
	
	@After
	public void after() throws DatastoreException {
		if(dboBasicDao != null && toDelete != null){
			for(Long id: toDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBOTeam.class, params);
			}
		}
	}
	
	@Before
	public void before(){
		toDelete = new LinkedList<Long>();
	}
	
	public static DBOTeam newTeam() {
		Long id = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		DBOTeam team = new DBOTeam();
		team.setId(id);
		team.setEtag("1");
		team.setProperties((new String("12345")).getBytes());
		return team;
	}
	
	@Test
	public void testRoundTrip() throws DatastoreException, NotFoundException, UnsupportedEncodingException{
		// Make sure we can create it
		DBOTeam team = newTeam();
		toDelete.add(team.getId());
		DBOTeam clone = dboBasicDao.createNew(team);		
		assertNotNull(clone);
		assertEquals(team, clone);
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", team.getId());
		clone = dboBasicDao.getObjectByPrimaryKey(DBOTeam.class, params);
		assertNotNull(clone);
		assertEquals(team, clone);
		
		// Make sure we can update it.
		clone.setProperties(new byte[] { (byte)1 });
		clone.setEtag("2");
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		
		// Get the clone back again
		params = new MapSqlParameterSource();
		params.addValue("id", clone.getId());
		DBOTeam clone2 = dboBasicDao.getObjectByPrimaryKey(DBOTeam.class, params);
		assertEquals(clone, clone2);
	}

}
