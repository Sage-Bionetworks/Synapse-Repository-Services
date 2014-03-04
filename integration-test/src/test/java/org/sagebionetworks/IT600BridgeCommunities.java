package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.bridge.model.versionInfo.BridgeVersionInfo;
import org.sagebionetworks.client.BridgeClient;
import org.sagebionetworks.client.BridgeClientImpl;
import org.sagebionetworks.client.BridgeProfileProxy;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Run this integration test as a sanity check to ensure our Synapse Java Client is working
 * 
 * @author alxdark, marcelblonk
 */
public class IT600BridgeCommunities {
	
	private static SynapseAdminClient adminSynapse;
	private static BridgeClient bridge = null;
	private static BridgeClient bridgeTwo = null;
	private static List<Long> usersToDelete;

	public static final int PREVIEW_TIMOUT = 10 * 1000;

	public static final int RDS_WORKER_TIMEOUT = 1000 * 60; // One min

	private List<String> communitiesToDelete;
	private List<String> handlesToDelete;

	private static BridgeClient createBridgeClient(SynapseAdminClient client) throws Exception {
		SynapseClient synapse = new SynapseClientImpl();
		usersToDelete.add(SynapseClientHelper.createUser(adminSynapse, synapse));

		BridgeClient bridge = new BridgeClientImpl(synapse);
		bridge.setBridgeEndpoint(StackConfiguration.getBridgeServiceEndpoint());

		// Return a proxy
		return BridgeProfileProxy.createProfileProxy(bridge);
	}

	private static SynapseClient createSynapse(BridgeClient bridge) {
		SynapseClient synapse = new SynapseClientImpl(bridge);
		SynapseClientHelper.setEndpoints(synapse);
		return synapse;
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		usersToDelete = new ArrayList<Long>();
		
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		
		bridge = createBridgeClient(adminSynapse);
		bridgeTwo = createBridgeClient(adminSynapse);
	}

	@Before
	public void before() throws SynapseException {
		communitiesToDelete = new ArrayList<String>();
		handlesToDelete = new ArrayList<String>();
		
		// Delete all communities that bridge and bridgeTwo are members of
		for (Community community : bridge.getCommunities(1000, 0).getResults()) {
			deleteCommunity(community.getId());
		}
		for (Community community : bridgeTwo.getCommunities(1000, 0).getResults()) {
			deleteCommunity(community.getId());
		}
		
		for (String id : handlesToDelete) {
			try {
				adminSynapse.deleteFileHandle(id);
			} catch (SynapseNotFoundException e) { }
		}
	}

	@After
	public void after() throws Exception {
		//TODO Cleanup properly after tests
		for (String communityId : communitiesToDelete) {
			deleteCommunity(communityId);
		}
	}

	private void deleteCommunity(String communityId) throws SynapseException {
		try {
			bridge.deleteCommunity(communityId);
		} catch (Exception e) {
			try {
				bridgeTwo.deleteCommunity(communityId);
			} catch (SynapseNotFoundException e2) { }
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		for (Long id : usersToDelete) {
			//TODO This delete should not need to be surrounded by a try-catch
			// This means proper cleanup was not done by the test 
			try {
				adminSynapse.deleteUser(id);
			} catch (Exception e) { }
		}
	}

	@Test
	public void testGetVersion() throws Exception {
		BridgeVersionInfo versionInfo = bridge.getBridgeVersionInfo();
		assertFalse(versionInfo.getVersion().isEmpty());
	}

	@Test
	public void testCommunityCRUD() throws Exception {
		String communityName = "my-first-community-" + System.currentTimeMillis();

		Community communityToCreate = new Community();
		communityToCreate.setName(communityName);

		Community newCommunity = bridge.createCommunity(communityToCreate);
		assertEquals(communityName, newCommunity.getName());
		assertNotNull(newCommunity.getId());
		assertNotNull(newCommunity.getTeamId());
		assertNull(newCommunity.getDescription());
		assertNotNull(newCommunity.getWelcomePageWikiId());
		assertNotNull(newCommunity.getIndexPageWikiId());

		WikiPageKey key = new WikiPageKey(newCommunity.getId(), ObjectType.ENTITY, newCommunity.getWelcomePageWikiId());
		V2WikiPage v2WikiPage = createSynapse(bridge).getV2WikiPage(key);
		assertEquals("Welcome to " + communityName, v2WikiPage.getTitle());

		key = new WikiPageKey(newCommunity.getId(), ObjectType.ENTITY, newCommunity.getIndexPageWikiId());
		v2WikiPage = createSynapse(bridge).getV2WikiPage(key);
		assertEquals("Index of " + communityName, v2WikiPage.getTitle());

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

	@Test
	public void testAddRemoveUsers() throws Exception {
		int beforeCount = (int) bridge.getAllCommunities(1000, 0).getTotalNumberOfResults();

		Community community = createCommunity();

		PaginatedResults<Community> allCommunities = bridge.getAllCommunities(1000, 0);
		assertEquals(beforeCount + 1, allCommunities.getTotalNumberOfResults());

		assertEquals(1, bridge.getCommunities(1000, 0).getTotalNumberOfResults());
		assertEquals(community, bridge.getCommunities(1000, 0).getResults().get(0));
		assertEquals(0, bridgeTwo.getCommunities(1000, 0).getTotalNumberOfResults());

		bridgeTwo.joinCommunity(community.getId());

		assertEquals(1, bridge.getCommunities(1000, 0).getTotalNumberOfResults());
		assertEquals(1, bridgeTwo.getCommunities(1000, 0).getTotalNumberOfResults());
		assertEquals(community, bridgeTwo.getCommunities(1000, 0).getResults().get(0));

		bridgeTwo.leaveCommunity(community.getId());

		assertEquals(1, bridge.getCommunities(1000, 0).getTotalNumberOfResults());
		assertEquals(0, bridgeTwo.getCommunities(1000, 0).getTotalNumberOfResults());
	}

	@Test
	public void testAddRemoveAdmin() throws Exception {
		Community community = createCommunity();

		String principalId = createSynapse(bridgeTwo).getUserSessionData().getProfile().getOwnerId();
		bridgeTwo.joinCommunity(community.getId());
		bridge.addCommunityAdmin(community.getId(), principalId);
		bridge.removeCommunityAdmin(community.getId(), principalId);
		bridgeTwo.leaveCommunity(community.getId());
	}

	@Test
	public void testAddRemoveAdminIdempotentcy() throws Exception {
		Community community = createCommunity();

		String principalId = createSynapse(bridgeTwo).getUserSessionData().getProfile().getOwnerId();
		bridgeTwo.joinCommunity(community.getId());
		bridge.addCommunityAdmin(community.getId(), principalId);
		bridge.addCommunityAdmin(community.getId(), principalId);
		bridge.removeCommunityAdmin(community.getId(), principalId);
		bridge.removeCommunityAdmin(community.getId(), principalId);
		bridgeTwo.leaveCommunity(community.getId());
	}

	@Test
	public void testAddAdminAndLeave() throws Exception {
		Community community = createCommunity();

		String principalId = createSynapse(bridgeTwo).getUserSessionData().getProfile().getOwnerId();
		bridgeTwo.joinCommunity(community.getId());
		bridge.addCommunityAdmin(community.getId(), principalId);
		bridgeTwo.leaveCommunity(community.getId());
	}

	@Test
	public void testAddOtherAdminAndLeave() throws Exception {
		Community community = createCommunity();

		String principalId = createSynapse(bridgeTwo).getUserSessionData().getProfile().getOwnerId();
		bridgeTwo.joinCommunity(community.getId());
		bridge.addCommunityAdmin(community.getId(), principalId);
		bridge.leaveCommunity(community.getId());
	}

	@Test(expected = SynapseException.class)
	public void testLeaveLastAdminNotAllowed() throws Exception {
		Community community = createCommunity();

		bridgeTwo.joinCommunity(community.getId());
		bridge.leaveCommunity(community.getId());
	}

	private Community createCommunity() throws SynapseException, JSONObjectAdapterException {
		String communityName = "my-first-community-" + System.currentTimeMillis() + "-" + communitiesToDelete.size();

		Community communityToCreate = new Community();
		communityToCreate.setName(communityName);

		Community newCommunity = bridge.createCommunity(communityToCreate);
		assertEquals(communityName, newCommunity.getName());
		assertNotNull(newCommunity.getId());
		assertNotNull(newCommunity.getTeamId());
		assertNull(newCommunity.getDescription());

		communitiesToDelete.add(newCommunity.getId());
		
		//TODO This API does not exist
		// handlesToDelete.add(adminSynapse.getV2WikiPage(
		// 		new WikiPageKey(newCommunity.getId(), ObjectType.COMMUNITY,
		// 				newCommunity.getWelcomePageWikiId()))
		// 		.getMarkdownFileHandleId());

		return newCommunity;
	}
}

