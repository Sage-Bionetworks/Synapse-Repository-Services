package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.schema.ObjectSchema;
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
	private Node node2 = null;
	private AccessRequirement accessRequirement = null;
	private AccessRequirement accessRequirement2 = null;
	private AccessApproval accessApproval = null;
		
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
		if (node2==null) {
			node2 = NodeTestUtils.createNew("bar", Long.parseLong(individualGroup.getId()));
			node2.setId( nodeDAO.createNew(node2) );
		};
		accessRequirement = DBOAccessRequirementDAOImplTest.newAccessRequirement(individualGroup, node);
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		Long id = accessRequirement.getId();
		assertNotNull(id);
		
		accessRequirement2 = DBOAccessRequirementDAOImplTest.newAccessRequirement(individualGroup, node2);
		accessRequirement2 = accessRequirementDAO.create(accessRequirement2);
		id = accessRequirement2.getId();
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
		if (accessRequirement2!=null && accessRequirement2.getId()!=null) {
			accessRequirementDAO.delete(accessRequirement2.getId().toString());
		}
		if (node!=null && nodeDAO!=null) {
			nodeDAO.delete(node.getId());
			node = null;
		}
		if (node2!=null && nodeDAO!=null) {
			nodeDAO.delete(node2.getId());
			node2 = null;
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
	public void testCRUD() throws Exception {
		// first of all, we should see the unmet requirement
		List<Long> unmetARIds = accessRequirementDAO.unmetAccessRequirements(node.getId(), Arrays.asList(new Long[]{Long.parseLong(individualGroup.getId())}), ACCESS_TYPE.DOWNLOAD);
		assertEquals(1, unmetARIds.size());
		assertEquals(accessRequirement.getId(), unmetARIds.iterator().next());
		// while we're at it, check the edge cases:
		// same result for ficticious principal ID
		unmetARIds = accessRequirementDAO.unmetAccessRequirements(node.getId(), Arrays.asList(new Long[]{8888L}), ACCESS_TYPE.DOWNLOAD);
		assertEquals(1, unmetARIds.size());
		assertEquals(accessRequirement.getId(), unmetARIds.iterator().next());
		// no unmet requirements for ficticious node ID
		assertTrue(
				accessRequirementDAO.unmetAccessRequirements(
						"syn7890", 
						Arrays.asList(new Long[]{Long.parseLong(individualGroup.getId())}), 
						ACCESS_TYPE.DOWNLOAD).isEmpty()
				);
		// no unmet requirement for other type of access
		assertTrue(
				accessRequirementDAO.unmetAccessRequirements(
						node.getId(), 
						Arrays.asList(new Long[]{Long.parseLong(individualGroup.getId())}), 
						ACCESS_TYPE.UPDATE).isEmpty()
				);
		
		
		// Create a new object
		accessApproval = newAccessApproval(individualGroup, accessRequirement);
		
		// Create it
		accessApproval = accessApprovalDAO.create(accessApproval);
		String id = accessApproval.getId().toString();
		assertNotNull(id);
		
		// no unmet requirement anymore ...
		assertTrue(
				accessRequirementDAO.unmetAccessRequirements(
						node.getId(), 
						Arrays.asList(new Long[]{Long.parseLong(individualGroup.getId())}), 
						ACCESS_TYPE.DOWNLOAD).isEmpty()
				);
		// ... but for a different (ficticious) user, the requirement isn't met...
		unmetARIds = accessRequirementDAO.unmetAccessRequirements(node.getId(), Arrays.asList(new Long[]{8888L}), ACCESS_TYPE.DOWNLOAD);
		assertEquals(1, unmetARIds.size());
		assertEquals(accessRequirement.getId(), unmetARIds.iterator().next());
		// ... and it's still unmet for the seconde node
		unmetARIds = accessRequirementDAO.unmetAccessRequirements(node2.getId(), Arrays.asList(new Long[]{Long.parseLong(individualGroup.getId())}), ACCESS_TYPE.DOWNLOAD);
		assertEquals(1, unmetARIds.size());
		assertEquals(accessRequirement2.getId(), unmetARIds.iterator().next());
		
		// Fetch it
		AccessApproval clone = accessApprovalDAO.get(id);
		assertNotNull(clone);
		assertEquals(accessApproval, clone);
		
		// Get by Node Id
		Collection<AccessApproval> ars = accessApprovalDAO.getForAccessRequirementsAndPrincipals(
				Arrays.asList(new String[]{accessRequirement.getId().toString()}), 
				Arrays.asList(new String[]{individualGroup.getId().toString()}));
		assertEquals(1, ars.size());
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
