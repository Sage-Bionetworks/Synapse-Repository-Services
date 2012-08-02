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
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAccessRequirementDAOImplTest {

	@Autowired 
	UserGroupDAO userGroupDAO;
	
	@Autowired
	AccessRequirementDAO accessRequirementDAO;
		
	@Autowired
	NodeDAO nodeDAO;
	
	private static final String TEST_USER_NAME = "test-user";
	
	private UserGroup individualGroup = null;
	private Node node = null;
	private TermsOfUseAccessRequirement accessRequirement = null;
	
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

	}
		
	
	@After
	public void tearDown() throws Exception{
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
	
	public static TermsOfUseAccessRequirement newAccessRequirement(UserGroup principal, Node node) throws DatastoreException {
		TermsOfUseAccessRequirement accessRequirement = new TermsOfUseAccessRequirement();
		accessRequirement.setCreatedBy(principal.getId());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(principal.getId());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setEtag("10");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setEntityIds(Arrays.asList(new String[]{node.getId()}));
		accessRequirement.setEntityType("com.sagebionetworks.repo.model.TermsOfUseAccessRequirements");
		return accessRequirement;
	}
	

	
	@Test
	public void testCRUD() throws Exception{
		// Create a new object
		accessRequirement = newAccessRequirement(individualGroup, node);
		
		long initialCount = accessRequirementDAO.getCount();
		
		// Create it
		TermsOfUseAccessRequirement accessRequirementCopy = accessRequirementDAO.create(accessRequirement);
		accessRequirement = accessRequirementCopy;
		assertNotNull(accessRequirementCopy.getId());
		
		assertEquals(1+initialCount, accessRequirementDAO.getCount());
		
		// Fetch it
		AccessRequirement clone = accessRequirementDAO.get(accessRequirement.getId().toString());
		assertNotNull(clone);
		assertEquals(accessRequirement, clone);
				
		// Get by Node Id
		Collection<AccessRequirement> ars = accessRequirementDAO.getForNode(node.getId());
		assertEquals(1, ars.size());
		assertEquals(accessRequirement, ars.iterator().next());

		// update it
		clone = ars.iterator().next();
		clone.setAccessType(ACCESS_TYPE.READ);
		AccessRequirement updatedAR = accessRequirementDAO.update(clone);
		assertEquals(clone.getAccessType(), updatedAR.getAccessType());

		assertTrue("etags should be incremented after an update", !clone.getEtag().equals(updatedAR.getEtag()));

		try {
			((TermsOfUseAccessRequirement)clone).setTermsOfUse("bar");
			accessRequirementDAO.update(clone);
			fail("conflicting update exception not thrown");
		}
		catch(ConflictingUpdateException e){
			// We expected this exception
		}	
		
		QueryResults<MigratableObjectData> migrationData = accessRequirementDAO.getMigrationObjectData(0, 10000, true);
		
		List<MigratableObjectData> results = migrationData.getResults();
		assertEquals(1+initialCount, results.size());
		assertEquals(migrationData.getTotalNumberOfResults(), results.size());
		
		boolean foundAr = false;
		for (MigratableObjectData od : results) {
			MigratableObjectDescriptor obj = od.getId();
			assertNotNull(obj.getId());
			assertNotNull(od.getEtag());
			assertEquals(MigratableObjectType.ACCESSREQUIREMENT, obj.getType());
			assertNotNull(od.getDependencies());
			if (obj.getId().equals(clone.getId().toString())) {
				foundAr=true;
				Collection<MigratableObjectDescriptor> deps = od.getDependencies();
				assertTrue(deps.size()>0); // is dependent on 'node'
				boolean foundNode = false;
				for (MigratableObjectDescriptor d : deps) {
					if (d.getId().equals(node.getId())) {
						foundNode = true;
					}
					assertEquals(MigratableObjectType.ENTITY, d.getType());
				}
				assertTrue("dependencies: "+deps, foundNode);
			}
		}
		assertTrue(foundAr);
				
		// Delete it
		accessRequirementDAO.delete(accessRequirement.getId().toString());

		assertEquals(initialCount, accessRequirementDAO.getCount());
	}


}
