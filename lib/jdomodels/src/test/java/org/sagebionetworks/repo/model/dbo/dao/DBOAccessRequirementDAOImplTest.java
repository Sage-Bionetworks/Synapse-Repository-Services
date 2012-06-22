package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementType;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.TermsOfUseRequirementParameters;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
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
	private AccessRequirement accessRequirement = null;
	
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
	
	@Test
	public void testCRUD() throws Exception{
		// Create a new object
		accessRequirement = new AccessRequirement();
		AccessRequirementUtils.copyDboToDto(DBOAccessRequirementTest.newAccessRequirement(individualGroup, node), accessRequirement);
		
		// Create it
		String id = accessRequirementDAO.create(accessRequirement);
		assertNotNull(id);
		accessRequirement.setId(Long.parseLong(id)); // for the sake of comparing to 'clone'
		
		// Fetch it
		AccessRequirement clone = accessRequirementDAO.get(id);
		assertNotNull(clone);
		assertEquals(accessRequirement, clone);
		
		// set the parameters field
		TermsOfUseRequirementParameters params = new TermsOfUseRequirementParameters();
		params.setTermsOfUse("foo");
		accessRequirementDAO.setAccessRequirementParameters(id, clone.getEtag(), params, schema);
		// this will increment the etag...
		
		// Get by Node Id
		Collection<AccessRequirement> ars = accessRequirementDAO.getForNode(node.getId());
		assertEquals(1, ars.size());
		// ... so we now have to increment etag to make the comparison work
		accessRequirement.setEtag(""+(1L+Long.parseLong(accessRequirement.getEtag())));
		assertEquals(accessRequirement, ars.iterator().next());

		// update it
		clone = ars.iterator().next();
		clone.setAccessType(ACCESS_TYPE.READ);
		AccessRequirement updatedAR = accessRequirementDAO.update(clone);
		assertEquals(clone.getAccessType(), updatedAR.getAccessType());

		assertTrue("etags should be incremented after an update", !clone.getEtag().equals(updatedAR.getEtag()));

		try {
			clone.setAccessRequirementType(AccessRequirementType.ACT_Approval);
			accessRequirementDAO.update(clone);
			fail("conflicting update exception not thrown");
		}
		catch(ConflictingUpdateException e){
			// We expected this exception
		}	
		
		// get the parameters field
		TermsOfUseRequirementParameters paramsClone = new TermsOfUseRequirementParameters();
		accessRequirementDAO.getAccessRequirementParameters(id, paramsClone, schema);
		assertEquals(params, paramsClone);
		
		// Delete it
		accessRequirementDAO.delete(id);
	}


}
