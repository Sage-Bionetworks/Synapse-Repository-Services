package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AccessRequirementManagerImplAutoWiredTest {
	@Autowired
	public NodeManager nodeManager;
	@Autowired
	public UserProvider testUserProvider;
	@Autowired
	public AccessRequirementManager accessRequirementManager;
	
	private UserInfo adminUserInfo;
	
	private static final String TERMS_OF_USE = "my dog has fleas";

	List<String> nodesToDelete;
	
	String entityId;
	
	TermsOfUseAccessRequirement ar;

	@Before
	public void before() throws Exception{
		adminUserInfo = testUserProvider.getTestAdminUserInfo();
		assertNotNull(nodeManager);
		nodesToDelete = new ArrayList<String>();
		
		Node rootProject = new Node();
		rootProject.setName("root "+System.currentTimeMillis());
		rootProject.setNodeType(EntityType.project.name());
		String rootId = nodeManager.createNewNode(rootProject, adminUserInfo);
		nodesToDelete.add(rootId); // the deletion of 'rootId' will cascade to its children
		Node node = new Node();
		node.setName("A");
		node.setNodeType(EntityType.layer.name());
		node.setParentId(rootId);
		entityId = nodeManager.createNewNode(node, adminUserInfo);
	}
	
	@After
	public void after() throws Exception {
		if(nodeManager != null && nodesToDelete != null){
			for(String id: nodesToDelete){
				try {
					nodeManager.delete(adminUserInfo, id);
				} catch (Exception e) {
					e.printStackTrace();
				} 				
			}
		}
		
		if (ar!=null && ar.getId()!=null && accessRequirementManager!=null) {
			accessRequirementManager.deleteAccessRequirement(adminUserInfo, ar.getId().toString());
		}
	}
	
	private static TermsOfUseAccessRequirement newAccessRequirement(String entityId) {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		ar.setEntityType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse(TERMS_OF_USE);
		return ar;
	}
	
	@Test
	public void testCreateAccessRequirement() throws Exception {
		ar = newAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		assertNotNull(ar.getCreatedBy());
		assertNotNull(ar.getCreatedOn());
		assertNotNull(ar.getSubjectIds());
		assertNotNull(ar.getId());
		assertNotNull(ar.getModifiedBy());
		assertNotNull(ar.getModifiedOn());
	}
	
	@Test(expected=InvalidModelException.class)
	public void testCreateAccessRequirementBadParam1() throws Exception {
		ar = newAccessRequirement(entityId);
		ar.setSubjectIds(null);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
	}
	
	@Test(expected=InvalidModelException.class)
	public void testCreateAccessRequirementBadParam2() throws Exception {
		ar = newAccessRequirement(entityId);
		ar.setEntityType(ACTAccessRequirement.class.getName());
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
	}
	
	@Test(expected=InvalidModelException.class)
	public void testCreateAccessRequirementBadParam3() throws Exception {
		ar = newAccessRequirement(entityId);
		ar.setAccessType(null);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
	}
	
	@Test(expected=ForbiddenException.class)
	public void testCreateAccessRequirementForbidden() throws Exception {
		ar = newAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(testUserProvider.getTestUserInfo(), ar);
	}
	
	@Test
	public void testUpdateAccessRequirement() throws Exception {
		ar = newAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);

		// ensure that the 'modifiedOn' date is later
		Thread.sleep(100L);
		long arModifiedOn = ar.getModifiedOn().getTime();
		ar.setSubjectIds(new ArrayList<RestrictableObjectDescriptor>()); // change the entity id list
		TermsOfUseAccessRequirement ar2 = accessRequirementManager.updateAccessRequirement(adminUserInfo, ar);
		assertTrue(ar2.getModifiedOn().getTime()-arModifiedOn>0);
		assertTrue(ar2.getSubjectIds().isEmpty());
	}
	
	@Test
	public void testGetAccessRequirements() throws Exception {
		ar = newAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getAccessRequirementsForSubject(adminUserInfo, rod);
		assertEquals(1L, ars.getTotalNumberOfResults());
		assertEquals(1, ars.getResults().size());
	}
	
	@Test
	public void testGetUnmetAccessRequirements() throws Exception {
		ar = newAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		UserInfo otherUserInfo = testUserProvider.getTestUserInfo();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getUnmetAccessRequirements(otherUserInfo, rod);
		assertEquals(1L, ars.getTotalNumberOfResults());
		assertEquals(1, ars.getResults().size());
	}
	
	// entity owner never has unmet access requirements
	@Test
	public void testGetUnmetAccessRequirementsOwner() throws Exception {
		ar = newAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getUnmetAccessRequirements(adminUserInfo, rod);
		assertEquals(0L, ars.getTotalNumberOfResults());
		assertEquals(0, ars.getResults().size());
	}
	
	@Test
	public void testDeleteAccessRequirements() throws Exception {
		ar = newAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		accessRequirementManager.deleteAccessRequirement(adminUserInfo, ar.getId().toString());
		ar=null;
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getAccessRequirementsForSubject(adminUserInfo, rod);
		assertEquals(0L, ars.getTotalNumberOfResults());
		assertEquals(0, ars.getResults().size());
	}
	
	
}
