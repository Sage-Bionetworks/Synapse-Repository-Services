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
import org.sagebionetworks.client.exceptions.*;
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
 * Run this integration test as a sanity check to ensure our Synapse Java Client is working
 * 
 * @author deflaux
 */
public class IT600BridgeCommunities {

	public static final int PREVIEW_TIMOUT = 10 * 1000;

	public static final int RDS_WORKER_TIMEOUT = 1000 * 60; // One min

	private List<String> toDelete = null;

	private static List<Long> accessRequirementsToDelete;

	private static BridgeClient bridge = null;
	private static BridgeClient bridgeTwo = null;

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
	public void testCommunityCRUD() throws Exception {
		String communityName = "my-first-community-" + System.currentTimeMillis();

		Community communityToCreate = new Community();
		communityToCreate.setName(communityName);

		Community newCommunity = bridge.createCommunity(communityToCreate);
		assertEquals(communityName, newCommunity.getName());
		assertNotNull(newCommunity.getId());
		assertNotNull(newCommunity.getGroupId());
		assertNull(newCommunity.getDescription());

		newCommunity.setDescription("some description");
		newCommunity = bridge.updateCommunity(newCommunity);
		assertEquals("some description", newCommunity.getDescription());

		Community community = bridge.getCommunity(newCommunity.getId());
		assertEquals(newCommunity.getId(), community.getId());
		assertEquals("some description", community.getDescription());

		bridge.deleteCommunity(newCommunity.getId());
		try {
			community = bridge.getCommunity(newCommunity.getId());
			fail("Should not have found the entity");
		} catch (SynapseNotFoundException e) {
		}
	}
}
