package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.http.HttpException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.bridge.model.versionInfo.BridgeVersionInfo;
import org.sagebionetworks.client.*;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.ids.UuidETagGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.Link;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.VariableContentPaginatedResults;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.utils.DefaultHttpClientSingleton;
import org.sagebionetworks.utils.HttpClientHelper;

/**
 * Run this integration test as a sanity check to ensure our Synapse Java Client
 * is working
 * 
 * @author deflaux
 */
public class IT530BridgeJavaClient {
	
	public static final int PREVIEW_TIMOUT = 10*1000;
	
	public static final int RDS_WORKER_TIMEOUT = 1000*60; // One min
	
	private List<String> toDelete = null;

	private static List<Long> accessRequirementsToDelete;

	private static BridgeClient bridge = null;
	private static BridgeClient bridgeTwo = null;
	private static Community community = null;

	public static BridgeClient createBridgeClient(String user, String pw) throws SynapseException {
		SynapseClientImpl synapse = new SynapseClientImpl();
		synapse.setAuthEndpoint(StackConfiguration.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration.getRepositoryServiceEndpoint());
		synapse.setFileEndpoint(StackConfiguration.getFileServiceEndpoint());

		synapse.login(user, pw);

		BridgeClientImpl bridge = new BridgeClientImpl(synapse);
		bridge.setBridgeEndpoint(StackConfiguration.getBridgeServiceEndpoint());

		// Return a proxy
		return BridgeProfileProxy.createProfileProxy(bridge);
	}

	public static SynapseClient createSynapse(BridgeClient bridge) {
		return new SynapseClientImpl(bridge);
	}

	/**
	 * @throws Exception
	 * 
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {
		 bridge = createBridgeClient(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());

		bridgeTwo = createBridgeClient(StackConfiguration.getIntegrationTestUserTwoName(),
				StackConfiguration.getIntegrationTestUserTwoPassword());
	}
	
	@Before
	public void before() throws SynapseException {
		// toDelete = new ArrayList<String>();
		//
		// community = bridge.createCommunity(new Community());
		//
		// toDelete.add(community.getId());
		//
		// accessRequirementsToDelete = new ArrayList<Long>();
	}

	/**
	 * @throws Exception 
	 * @throws HttpException
	 * @throws IOException
	 * @throws JSONException
	 * @throws SynapseUserException
	 * @throws SynapseServiceException
	 */
	@After
	public void after() throws Exception {
		// SynapseClient synapse = createSynapse(bridge);
		// if (toDelete != null) {
		// for (String id: toDelete) {
		// try {
		// synapse.deleteAndPurgeEntityById(id);
		// } catch (Exception e) {}
		// }
		// }
		// if (accessRequirementsToDelete!=null) {
		// // clean up Access Requirements
		// for (Long id : accessRequirementsToDelete) {
		// try {
		// synapse.deleteAccessRequirement(id);
		// } catch (Exception e) {}
		// }
		// }
	}

	@Test
	public void testGetVersion() throws Exception {
		BridgeVersionInfo versionInfo = bridge.getVersionInfo();
		assertFalse(versionInfo.getVersion().isEmpty());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testJavaClientCRUD() throws Exception {
		// Study aNewDataset = new Study();
		// aNewDataset.setParentId(project.getId());
		//
		// aNewDataset = bridge.createEntity(aNewDataset);
		//
		// // Get the project using just using its ID. This is useful for cases where you
		// // do not know what you are getting until it arrives.
		// Project clone = (Project) synapse.getEntityById(project.getId());
		// assertNotNull(clone);
		// assertNotNull(clone.getEntityType());
		// assertEquals(project.getId(), clone.getId());
		//
		// // Get the entity annotations
		// Annotations annos = synapse.getAnnotations(aNewDataset.getId());
		// assertNotNull(annos);
		// assertEquals(aNewDataset.getId(), annos.getId());
		// assertNotNull(annos.getEtag());
		// // Add some values
		// annos.addAnnotation("longKey", new Long(999999));
		// annos.addAnnotation("blob", "This will be converted to a blob!".getBytes("UTF-8"));
		// Annotations updatedAnnos = synapse.updateAnnotations(aNewDataset.getId(), annos);
		// assertNotNull(updatedAnnos);
		// assertEquals(aNewDataset.getId(), annos.getId());
		// assertNotNull(updatedAnnos.getEtag());
		// // The Etag should have changed
		// assertFalse(updatedAnnos.getEtag().equals(annos.getEtag()));
		//
		// // Get the "zero" e-tag for specific versions. See PLFM-1420.
		// Entity datasetEntity = synapse.getEntityByIdForVersion(aNewDataset.getId(), aNewDataset.getVersionNumber());
		// assertTrue(UuidETagGenerator.ZERO_E_TAG.equals(datasetEntity.getEtag()));
		//
		// // Get the Users permission for this entity
		// UserEntityPermissions uep = synapse.getUsersEntityPermissions(aNewDataset.getId());
		// assertNotNull(uep);
		// assertEquals(true, uep.getCanEdit());
		// assertEquals(true, uep.getCanView());
		// assertEquals(true, synapse.canAccess(aNewDataset.getId(), ACCESS_TYPE.UPDATE));
		// assertEquals(true, synapse.canAccess(aNewDataset.getId(), ACCESS_TYPE.READ));
		// assertTrue(uep.getCanChangePermissions());
		// assertTrue(uep.getCanEnableInheritance());
		//
		// UserProfile profile = synapse.getMyProfile();
		// assertNotNull(profile);
		//
		// assertEquals(profile.getOwnerId(), uep.getOwnerPrincipalId().toString());
		//
		// // should be able to download
		// assertTrue(synapse.canAccess(aNewDataset.getId(), ACCESS_TYPE.DOWNLOAD));
		//
		// // now add a ToU restriction
		// TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		//
		// RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		// rod.setId(aNewDataset.getId());
		// rod.setType(RestrictableObjectType.ENTITY);
		// ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[] { rod }));
		//
		// ar.setEntityType(ar.getClass().getName());
		// ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		// ar.setTermsOfUse("play nice");
		// ar = synapse.createAccessRequirement(ar);
		//
		// UserProfile otherProfile = synapse.getMyProfile();
		// assertNotNull(otherProfile);
		//
		// // should not be able to download
		// assertFalse(synapseTwo.canAccess(aNewDataset.getId(), ACCESS_TYPE.DOWNLOAD));
		//
		// RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		// subjectId.setType(RestrictableObjectType.ENTITY);
		// subjectId.setId(aNewDataset.getId());
		// VariableContentPaginatedResults<AccessRequirement> vcpr = synapseTwo.getUnmetAccessRequirements(subjectId);
		// assertEquals(1, vcpr.getResults().size());
		//
		// // now add the ToU approval
		// TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		// aa.setAccessorId(otherProfile.getOwnerId());
		// aa.setEntityType(TermsOfUseAccessApproval.class.getName());
		// aa.setRequirementId(ar.getId());
		//
		// synapseTwo.createAccessApproval(aa);
		//
		// vcpr = synapseTwo.getUnmetAccessRequirements(subjectId);
		// assertEquals(0, vcpr.getResults().size());
		//
		// // should be able to download
		// assertTrue(synapseTwo.canAccess(aNewDataset.getId(), ACCESS_TYPE.DOWNLOAD));
		//
		// // ACL should reflect the first User's permission
		// AccessControlList acl = synapse.getACL(project.getId());
		// Set<ResourceAccess> ras = acl.getResourceAccess();
		// boolean foundit = false;
		// List<Long> foundPrincipals = new ArrayList<Long>();
		// for (ResourceAccess ra : ras) {
		// assertNotNull(ra.getPrincipalId());
		// foundPrincipals.add(ra.getPrincipalId());
		// if (ra.getPrincipalId().equals(Long.parseLong(profile.getOwnerId()))) {
		// foundit = true;
		// Set<ACCESS_TYPE> ats = ra.getAccessType();
		// assertTrue(ats.contains(ACCESS_TYPE.READ));
		// assertTrue(ats.contains(ACCESS_TYPE.UPDATE));
		// }
		// }
		// assertTrue("didn't find " + profile.getDisplayName() + "(" + profile.getOwnerId() + ") but found " +
		// foundPrincipals, foundit);
		//
		// // Get the path
		// EntityPath path = synapse.getEntityPath(aNewDataset.getId());
		// assertNotNull(path);
		// assertNotNull(path.getPath());
		// assertEquals(3, path.getPath().size());
		// EntityHeader header = path.getPath().get(2);
		// assertNotNull(header);
		// assertEquals(aNewDataset.getId(), header.getId());
		//
		// // Get the entity headers
		// List<String> entityIds = new ArrayList<String>();
		// entityIds.add(project.getId());
		// entityIds.add(dataset.getId());
		// entityIds.add(aNewDataset.getId());
		// BatchResults<EntityHeader> entityHeaders = synapse.getEntityTypeBatch(entityIds);
		// assertNotNull(entityHeaders);
		// assertEquals(3, entityHeaders.getTotalNumberOfResults());
		// List<String> outputIds = new ArrayList<String>();
		// for (EntityHeader entityHeader : entityHeaders.getResults()) {
		// outputIds.add(entityHeader.getId());
		// }
		// assertEquals(entityIds.size(), outputIds.size());
		// assertTrue(entityIds.containsAll(outputIds));
		//
	}
}
