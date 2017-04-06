package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ACTAccessApproval;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.PostMessageContentAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAccessApprovalTest {
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Autowired 
	UserGroupDAO userGroupDAO;
	
	@Autowired
	NodeDAO nodeDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private static final String TEST_USER_NAME = "test-user@test.com";
	
	private Node node = null;
	private UserGroup individualGroup = null;
	private DBOAccessRequirement ar = null;
	private DBOAccessApproval accessApproval = null;
	
	
	@Before
	public void setUp() throws Exception {
		individualGroup = new UserGroup();
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(new Date());
		individualGroup.setId(userGroupDAO.create(individualGroup).toString());

		if (node==null) {
			node = NodeTestUtils.createNew("foo", Long.parseLong(individualGroup.getId()));
			node.setId( nodeDao.createNew(node) );
		};
		deleteAccessApproval();
		deleteAccessRequirement();
		DBOAccessRequirement accessRequirement = DBOAccessRequirementTest.newAccessRequirement(individualGroup, node, "foo".getBytes(), idGenerator.generateNewId(IdType.ACCESS_APPROVAL_ID));
		ar = dboBasicDao.createNew(accessRequirement);
	}
	
	private void deleteAccessRequirement() throws DatastoreException {
		if(dboBasicDao != null && ar!=null){
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", ar.getId());
			dboBasicDao.deleteObjectByPrimaryKey(DBOAccessRequirement.class, params);
			ar=null;
		}		
	}
		
	private void deleteAccessApproval() throws DatastoreException {
		if(dboBasicDao != null && accessApproval!=null){
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", accessApproval.getId());
			dboBasicDao.deleteObjectByPrimaryKey(DBOAccessApproval.class, params);
			accessApproval=null;
		}		
	}
		
	
	@After
	public void tearDown() throws Exception {
		deleteAccessApproval();
		deleteAccessRequirement();
		if (node!=null && nodeDao!=null) {
			nodeDao.delete(node.getId());
			node = null;
		}
		if (individualGroup != null) {
			// this will delete the user profile too
			userGroupDAO.delete(individualGroup.getId());
		}
	}
	
	public static DBOAccessApproval newAccessApproval(UserGroup principal, DBOAccessRequirement ar, Long id) throws DatastoreException {
		DBOAccessApproval accessApproval = new DBOAccessApproval();
		accessApproval.setCreatedBy(Long.parseLong(principal.getId()));
		accessApproval.setCreatedOn(System.currentTimeMillis());
		accessApproval.setModifiedBy(Long.parseLong(principal.getId()));
		accessApproval.setModifiedOn(System.currentTimeMillis());
		accessApproval.seteTag("10");
		accessApproval.setAccessorId(Long.parseLong(principal.getId()));
		accessApproval.setRequirementId(ar.getId());
		accessApproval.setSerializedEntity("my dog has fleas".getBytes());
		accessApproval.setId(id);
		return accessApproval;
	}
	
	@Test
	public void testCRUD() throws Exception{
		accessApproval = dboBasicDao.createNew(newAccessApproval(individualGroup, ar, idGenerator.generateNewId(IdType.ACCESS_APPROVAL_ID)));
		// Create a new object		
		
		// Create it
		assertNotNull(accessApproval);
		assertNotNull(accessApproval.getId());
		
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", accessApproval.getId());
		DBOAccessApproval clone = dboBasicDao.getObjectByPrimaryKey(DBOAccessApproval.class, params);
		assertNotNull(clone);
		assertEquals(accessApproval, clone);
		
		// Update it
		dboBasicDao.update(clone);
		clone = dboBasicDao.getObjectByPrimaryKey(DBOAccessApproval.class, params);
		assertNotNull(clone);
		
		// Delete it
		boolean result = dboBasicDao.deleteObjectByPrimaryKey(DBOAccessApproval.class,  params);
		assertTrue("Failed to delete the type created", result);
	}
	

}
