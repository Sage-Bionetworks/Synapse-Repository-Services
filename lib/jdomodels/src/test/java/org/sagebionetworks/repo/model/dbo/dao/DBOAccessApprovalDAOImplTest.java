package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessApprovalType;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.TermsOfUseRequirementParameters;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessApprovalTest;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirementTest;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAccessApprovalDAOImplTest {

	@Autowired 
	UserGroupDAO userGroupDAO;
	
	@Autowired
	AccessRequirementDAO accessRequirementDAO;
		
	@Autowired
	AccessApprovalDAO accessApprovalDAO;
		
	@Autowired
	NodeDAO nodeDAO;
	
	private static final String TEST_USER_NAME = "test-user";
	
	private UserGroup individualGroup = null;
	private Node node = null;
	private AccessRequirement accessRequirement = null;
	private AccessApproval accessApproval = null;
	
	// need a class (something with a schema) for Approval parameters.  
	// since none has been defined for AccessApproval, just use the AccessRequirements one
	private static final Class approvalParametersClass = TermsOfUseRequirementParameters.class;
	private static Object newParametersObject() {
		TermsOfUseRequirementParameters p = new TermsOfUseRequirementParameters();
		p.setTermsOfUse("foo");
		return p;
	}
	
	private ObjectSchema schema = null;
	
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
		String jsonString = (String) TermsOfUseRequirementParameters.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
		schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		if (node==null) {
			node = NodeTestUtils.createNew("foo", Long.parseLong(individualGroup.getId()));
			node.setId( nodeDAO.createNew(node) );
		};
		accessRequirement = new AccessRequirement();
		AccessRequirementUtils.copyDboToDto(DBOAccessRequirementTest.newAccessRequirement(individualGroup, node), accessRequirement);
		String id = accessRequirementDAO.create(accessRequirement);
		assertNotNull(id);
		accessRequirement.setId(Long.parseLong(id)); 
	}
		
	
	@After
	public void tearDown() throws Exception{
		if (accessApproval!=null && accessApproval.getId()!=null) {
			accessApprovalDAO.delete(accessApproval.getId().toString());
		}
		if (accessRequirement!=null && accessRequirement.getId()!=null) {
			accessRequirementDAO.delete(accessRequirement.getId().toString());
		}
		if (node!=null && nodeDAO!=null) {
			nodeDAO.delete(node.getId());
			node = null;
		}
		individualGroup = userGroupDAO.findGroup(TEST_USER_NAME, true);
		if (individualGroup != null) {
			userGroupDAO.delete(individualGroup.getId());
		}
	}
	
	@Test
	public void testCRUD() throws Exception{
		// Create a new object
		accessApproval = new AccessApproval();
		{
			DBOAccessRequirement dboAccessRequirement = new DBOAccessRequirement();
			dboAccessRequirement.setId(accessRequirement.getId()); // only need to set id, for the following ussage
			AccessApprovalUtils.copyDboToDto(DBOAccessApprovalTest.newAccessApproval(individualGroup, dboAccessRequirement), accessApproval);
		}
		
		// Create it
		String id = accessApprovalDAO.create(accessApproval);
		assertNotNull(id);
		accessApproval.setId(Long.parseLong(id)); // for the sake of comparing to 'clone'
		
		// Fetch it
		AccessApproval clone = accessApprovalDAO.get(id);
		assertNotNull(clone);
		assertEquals(accessApproval, clone);
		
		// set the parameters field
		Object params = newParametersObject();
		accessApprovalDAO.setAccessApprovalParameters(id, clone.getEtag(), params, schema);
		// this will increment the etag...
		
		// Get by Node Id
		Collection<AccessApproval> ars = accessApprovalDAO.getForAccessRequirementsAndPrincipals(
				Arrays.asList(new String[]{accessRequirement.getId().toString()}), 
				Arrays.asList(new String[]{individualGroup.getId().toString()}));
		assertEquals(1, ars.size());
		// ... so we now have to increment etag to make the comparison work
		accessApproval.setEtag(""+(1L+Long.parseLong(accessApproval.getEtag())));
		assertEquals(accessApproval, ars.iterator().next());

		// update it
		clone = ars.iterator().next();
		clone.setApprovalType(AccessApprovalType.ACT_Approval);
		AccessApproval updatedAA = accessApprovalDAO.update(clone);
		assertEquals(clone.getApprovalType(), updatedAA.getApprovalType());

		assertTrue("etags should be incremented after an update", !clone.getEtag().equals(updatedAA.getEtag()));

		try {
			clone.setApprovalType(AccessApprovalType.ACT_Approval);
			accessApprovalDAO.update(clone);
			fail("conflicting update exception not thrown");
		}
		catch(ConflictingUpdateException e){
			// We expected this exception
		}	
		
		// get the parameters field
		Object paramsClone = newParametersObject();
		accessApprovalDAO.getAccessApprovalParameters(id, paramsClone, schema);
		assertEquals(params, paramsClone);
		
		// Delete it
		accessApprovalDAO.delete(id);
	}


}
