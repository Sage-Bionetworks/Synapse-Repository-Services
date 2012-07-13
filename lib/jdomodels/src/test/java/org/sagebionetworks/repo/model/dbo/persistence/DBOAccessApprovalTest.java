package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.schema.ObjectSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.SerializationUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAccessApprovalTest {
	
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
	private DBOAccessApproval accessApproval = null;
	
	
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
		deleteAccessApproval();
		deleteAccessRequirement();
		DBOAccessRequirement accessRequirement = DBOAccessRequirementTest.newAccessRequirement(individualGroup, node, "foo".getBytes());
		ar = dboBasicDao.createNew(accessRequirement);
	}
	
	private void deleteAccessRequirement() throws DatastoreException {
		if(dboBasicDao != null && ar!=null){
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", ar.getId());
			dboBasicDao.deleteObjectById(DBOAccessRequirement.class, params);
			ar=null;
		}		
	}
		
	private void deleteAccessApproval() throws DatastoreException {
		if(dboBasicDao != null && accessApproval!=null){
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", accessApproval.getId());
			dboBasicDao.deleteObjectById(DBOAccessApproval.class, params);
			accessApproval=null;
		}		
	}
		
	
	@After
	public void tearDown() throws Exception {
		deleteAccessApproval();
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
	
	public static DBOAccessApproval newAccessApproval(UserGroup principal, DBOAccessRequirement ar) throws DatastoreException {
		DBOAccessApproval accessApproval = new DBOAccessApproval();
		accessApproval.setCreatedBy(Long.parseLong(principal.getId()));
		accessApproval.setCreatedOn(System.currentTimeMillis());
		accessApproval.setModifiedBy(Long.parseLong(principal.getId()));
		accessApproval.setModifiedOn(System.currentTimeMillis());
		accessApproval.seteTag(10L);
		accessApproval.setAccessorId(Long.parseLong(principal.getId()));
		accessApproval.setRequirementId(ar.getId());
		accessApproval.setEntityType("com.sagebionetworks.repo.model.TermsOfUseAccessApproval");
		accessApproval.setSerializedEntity("my dog has fleas".getBytes());
		return accessApproval;
	}
	
	@Test
	public void testCRUD() throws Exception{
		accessApproval = dboBasicDao.createNew(newAccessApproval(individualGroup, ar));
		// Create a new object		
		
		// Create it
		assertNotNull(accessApproval);
		assertNotNull(accessApproval.getId());
		
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", accessApproval.getId());
		DBOAccessApproval clone = dboBasicDao.getObjectById(DBOAccessApproval.class, params);
		assertNotNull(clone);
		assertEquals(accessApproval, clone);
		
		// Update it
		dboBasicDao.update(clone);
		clone = dboBasicDao.getObjectById(DBOAccessApproval.class, params);
		assertNotNull(clone);
		
		// Delete it
		boolean result = dboBasicDao.deleteObjectById(DBOAccessApproval.class,  params);
		assertTrue("Failed to delete the type created", result);
		
	}

}
