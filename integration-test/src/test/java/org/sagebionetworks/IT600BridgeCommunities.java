package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpException;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.bridge.model.versionInfo.BridgeVersionInfo;
import org.sagebionetworks.client.BridgeClient;
import org.sagebionetworks.client.BridgeClientImpl;
import org.sagebionetworks.client.BridgeProfileProxy;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;

import com.google.common.collect.Lists;

/**
 * Run this integration test as a sanity check to ensure our Synapse Java Client is working
 * 
 * @author deflaux
 */
public class IT600BridgeCommunities {

	public static final int PREVIEW_TIMOUT = 10 * 1000;

	public static final int RDS_WORKER_TIMEOUT = 1000 * 60; // One min

	private List<String> communitiesToDelete = Lists.newArrayList();

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
		SynapseClient synapse = new SynapseClientImpl(bridge);
		synapse.setRepositoryEndpoint(StackConfiguration.getRepositoryServiceEndpoint());
		synapse.setFileEndpoint(StackConfiguration.getFileServiceEndpoint());
		return synapse;
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
		// Delete all communities that bridge and bridgeTwo are members of
		for (Community community : bridge.getCommunities(1000, 0).getResults()) {
			deleteCommunity(community.getId());
		}
		for (Community community : bridgeTwo.getCommunities(1000, 0).getResults()) {
			deleteCommunity(community.getId());
		}
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
		for (String communityId : communitiesToDelete) {
			deleteCommunity(communityId);
		}
	}

	private void deleteCommunity(String communityId) {
		try {
			bridge.deleteCommunity(communityId);
		} catch (Exception e) {
			try {
				bridgeTwo.deleteCommunity(communityId);
			} catch (Exception e2) {
				System.err.println(e.getMessage());
				System.err.println(e2.getMessage());
			}
		}
	}

	@Test
	public void testGetVersion() throws Exception {
		BridgeVersionInfo versionInfo = bridge.getBridgeVersionInfo();
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

		bridgeTwo.joinCommunity(community.getId());
		bridge.addCommunityAdmin(community.getId(), StackConfiguration.getIntegrationTestUserTwoName());
		bridge.removeCommunityAdmin(community.getId(), StackConfiguration.getIntegrationTestUserTwoName());
		bridgeTwo.leaveCommunity(community.getId());
	}

	@Test
	public void testAddRemoveAdminIdempotentcy() throws Exception {
		Community community = createCommunity();

		bridgeTwo.joinCommunity(community.getId());
		bridge.addCommunityAdmin(community.getId(), StackConfiguration.getIntegrationTestUserTwoName());
		bridge.addCommunityAdmin(community.getId(), StackConfiguration.getIntegrationTestUserTwoName());
		bridge.removeCommunityAdmin(community.getId(), StackConfiguration.getIntegrationTestUserTwoName());
		bridge.removeCommunityAdmin(community.getId(), StackConfiguration.getIntegrationTestUserTwoName());
		bridgeTwo.leaveCommunity(community.getId());
	}

	@Test
	public void testAddAdminAndLeave() throws Exception {
		Community community = createCommunity();

		bridgeTwo.joinCommunity(community.getId());
		bridge.addCommunityAdmin(community.getId(), StackConfiguration.getIntegrationTestUserTwoName());
		bridgeTwo.leaveCommunity(community.getId());
	}

	@Test
	public void testAddOtherAdminAndLeave() throws Exception {
		Community community = createCommunity();

		bridgeTwo.joinCommunity(community.getId());
		bridge.addCommunityAdmin(community.getId(), StackConfiguration.getIntegrationTestUserTwoName());
		bridge.leaveCommunity(community.getId());
	}

	@Test(expected = SynapseException.class)
	public void testLeaveLastAdminNotAllowed() throws Exception {
		Community community = createCommunity();

		bridgeTwo.joinCommunity(community.getId());
		bridge.leaveCommunity(community.getId());
	}

	private Community createCommunity() throws SynapseException {
		String communityName = "my-first-community-" + System.currentTimeMillis() + "-" + communitiesToDelete.size();

		Community communityToCreate = new Community();
		communityToCreate.setName(communityName);

		Community newCommunity = bridge.createCommunity(communityToCreate);
		assertEquals(communityName, newCommunity.getName());
		assertNotNull(newCommunity.getId());
		assertNotNull(newCommunity.getTeamId());
		assertNull(newCommunity.getDescription());

		communitiesToDelete.add(newCommunity.getId());

		return newCommunity;
	}
}

