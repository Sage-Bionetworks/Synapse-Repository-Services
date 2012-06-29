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
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessApproval;
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
import org.springframework.util.SerializationUtils;

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
//		String jsonString = (String) TermsOfUseRequirementParameters.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
//		schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		if (node==null) {
			node = NodeTestUtils.createNew("foo", Long.parseLong(individualGroup.getId()));
			node.setId( nodeDAO.createNew(node) );
		};
		accessRequirement = DBOAccessRequirementDAOImplTest.newAccessRequirement(individualGroup, node);
//		AccessRequirementUtils.copyDboToDto(DBOAccessRequirementDAOImplTest.newAccessRequirement(individualGroup, node), accessRequirement);
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		Long id = accessRequirement.getId();
		assertNotNull(id);
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
	
	public static TermsOfUseAccessApproval newAccessApproval(UserGroup principal, AccessRequirement ar) throws DatastoreException {
		TermsOfUseAccessApproval accessApproval = new TermsOfUseAccessApproval();
		accessApproval.setCreatedBy(principal.getId());
		accessApproval.setCreatedOn(new Date());
		accessApproval.setModifiedBy(principal.getId());
		accessApproval.setModifiedOn(new Date());
		accessApproval.setEtag("10");
		accessApproval.setAccessorId(principal.getId());
		accessApproval.setRequirementId(ar.getId());
		accessApproval.setEntityType("com.sagebionetworks.repo.model.TermsOfUseAccessApproval");
		return accessApproval;
	}
	
	@Test
	public void testCRUD() throws Exception{
		// Create a new object
		accessApproval = newAccessApproval(individualGroup, accessRequirement);
		
		// Create it
		accessApproval = accessApprovalDAO.create(accessApproval);
		String id = accessApproval.getId().toString();
		assertNotNull(id);
		
		// Fetch it
		AccessApproval clone = accessApprovalDAO.get(id);
		assertNotNull(clone);
		assertEquals(accessApproval, clone);
		
//		// set the parameters field
//		Object params = newParametersObject();
//		accessApprovalDAO.setAccessApprovalParameters(id, clone.getEtag(), params, schema);
//		// this will increment the etag...
//		
		// Get by Node Id
		Collection<AccessApproval> ars = accessApprovalDAO.getForAccessRequirementsAndPrincipals(
				Arrays.asList(new String[]{accessRequirement.getId().toString()}), 
				Arrays.asList(new String[]{individualGroup.getId().toString()}));
		assertEquals(1, ars.size());
//		// ... so we now have to increment etag to make the comparison work
//		accessApproval.setEtag(""+(1L+Long.parseLong(accessApproval.getEtag())));
		assertEquals(accessApproval, ars.iterator().next());

		// update it
		clone = ars.iterator().next();
		AccessApproval updatedAA = accessApprovalDAO.update(clone);
		assertEquals(((TermsOfUseAccessApproval)clone).getEntityType(), ((TermsOfUseAccessApproval)updatedAA).getEntityType());

		assertTrue("etags should be incremented after an update", !clone.getEtag().equals(updatedAA.getEtag()));

		try {
			accessApprovalDAO.update(clone);
			fail("conflicting update exception not thrown");
		}
		catch(ConflictingUpdateException e){
			// We expected this exception
		}	
		
		// Delete it
		accessApprovalDAO.delete(id);
	}


}
