package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class RestrictionInformationManagerImplAutoWiredTest {
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private NodeManager nodeManager;
	
	@Autowired
	private AccessRequirementManager accessRequirementManager;

	@Autowired
	private RestrictionInformationManager restrictionInformationManager;
	
	private UserInfo adminUserInfo;
	
	private static final String TERMS_OF_USE = "my dog has fleas";

	private List<String> nodesToDelete;
	
	private String entityId;
	
	private String childId;
	
	private AccessRequirement ar;

	@BeforeEach
	public void before() throws Exception {
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		assertNotNull(nodeManager);
		nodesToDelete = new ArrayList<String>();
		
		Node rootProject = new Node();
		rootProject.setName("root "+System.currentTimeMillis());
		rootProject.setNodeType(EntityType.project);
		String rootId = nodeManager.createNewNode(rootProject, adminUserInfo);
		nodesToDelete.add(rootId); // the deletion of 'rootId' will cascade to its children
		Node node = new Node();
		node.setName("A");
		node.setNodeType(EntityType.link);
		node.setParentId(rootId);
		entityId = nodeManager.createNewNode(node, adminUserInfo);

		Node childNode = new Node();
		childNode.setName("Child");
		childNode.setNodeType(EntityType.link);
		childNode.setParentId(entityId);
		childId = nodeManager.createNewNode(childNode, adminUserInfo);
	}

	@AfterEach
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
	
	private static TermsOfUseAccessRequirement newEntityAccessRequirement(String entityId) {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		ar.setConcreteType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse(TERMS_OF_USE);
		return ar;
	}

	@Test
	public void testGetRestrictionInformationInherited() {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(childId);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse info = restrictionInformationManager.getRestrictionInformation(adminUserInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, info.getRestrictionLevel());
	}
}
