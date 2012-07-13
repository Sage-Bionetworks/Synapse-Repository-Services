package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.SchemaSerializationUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAccessRequirementTest {
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Autowired 
	UserGroupDAO userGroupDAO;
	
	@Autowired
	NodeDAO nodeDAO;
	
	private static final String TEST_USER_NAME = "test-user";
	
	private Node node = null;
	private UserGroup individualGroup = null;
	private DBOAccessRequirement ar = null;
	
	@Before
	public void setUp() throws Exception {
		individualGroup = userGroupDAO.findGroup(TEST_USER_NAME, true);
		if (individualGroup == null) {
			individualGroup = new UserGroup();
			individualGroup.setName(TEST_USER_NAME);
			individualGroup.setIsIndividual(true);
			individualGroup.setCreationDate(new Date());
			individualGroup.setId(userGroupDAO.create(individualGroup));
		}
		if (node==null) {
			node = NodeTestUtils.createNew("foo", Long.parseLong(individualGroup.getId()));
			node.setId( nodeDAO.createNew(node) );
		};
		deleteAccessRequirement();
	}
	
	private void deleteAccessRequirement() throws DatastoreException {
		if(dboBasicDao != null && ar!=null){
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", ar.getId());
			dboBasicDao.deleteObjectById(DBOAccessRequirement.class, params);
			ar=null;
		}		
	}
		
	
	@After
	public void tearDown() throws Exception {
		deleteAccessRequirement();
		if (node!=null && nodeDAO!=null) {
			nodeDAO.delete(node.getId());
			node = null;
		}
		individualGroup = userGroupDAO.findGroup(TEST_USER_NAME, true);
		if (individualGroup != null) {
			// this will delete the user profile too
			userGroupDAO.delete(individualGroup.getId());
		}
	}
	
	public static DBOAccessRequirement newAccessRequirement(UserGroup principal, Node node, byte[] serializedEntity) throws DatastoreException {
		DBOAccessRequirement accessRequirement = new DBOAccessRequirement();
		accessRequirement.setCreatedBy(Long.parseLong(principal.getId()));
		accessRequirement.setCreatedOn(System.currentTimeMillis());
		accessRequirement.setModifiedBy(Long.parseLong(principal.getId()));
		accessRequirement.setModifiedOn(System.currentTimeMillis());
		accessRequirement.seteTag(10L);
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD.toString());
		accessRequirement.setEntityType("com.sagebionetworks.repo.model.TermsOfUseAccessRequirements");
		accessRequirement.setSerializedEntity(serializedEntity);
		return accessRequirement;
	}
	
	@Test
	public void testCRUD() throws Exception{
		// Create a new object
		DBOAccessRequirement accessRequirement = newAccessRequirement(individualGroup, node, "foo".getBytes());
		
		// Create it
		DBOAccessRequirement clone = dboBasicDao.createNew(accessRequirement);
		assertNotNull(clone);
		assertEquals(accessRequirement, clone);
		
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", accessRequirement.getId());
		clone = dboBasicDao.getObjectById(DBOAccessRequirement.class, params);
		assertNotNull(clone);
		assertEquals(accessRequirement, clone);
		
		// Update it
		dboBasicDao.update(clone);
		clone = dboBasicDao.getObjectById(DBOAccessRequirement.class, params);
		assertNotNull(clone);
		
		// Delete it
		boolean result = dboBasicDao.deleteObjectById(DBOAccessRequirement.class,  params);
		assertTrue("Failed to delete the type created", result);
		
	}

}
