package org.sagebionetworks.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.bridge.model.versionInfo.BridgeVersionInfo;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.schema.adapter.*;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * Low-level Java Client API for Bridge REST APIs
 */
public class BridgeClientImpl extends BaseClientImpl implements BridgeClient {


	public static final String BRIDGE_JAVA_CLIENT = "Bridge-Java-Client/";

	private static final Logger log = LogManager.getLogger(BridgeClientImpl.class.getName());

	private static final String DEFAULT_BRIDGE_ENDPOINT = "https://bridge-prod.prod.sagebase.org/bridge/v1";

	private static final String VERSION_INFO = "/version";

	private static final String COMMUNITY = "/community";
	private static final String MEMBER = "/member";

	private static final String JOINED = "/joined";
	private static final String JOIN = "/join";
	private static final String LEAVE = "/leave";
	private static final String ADD_ADMIN = "/addadmin";
	private static final String REMOVE_ADMIN = "/removeadmin";

	protected String bridgeEndpoint;

	/**
	 * Default client provider.
	 * 
	 * @param clientProvider
	 */
	public BridgeClientImpl() {
		this(new HttpClientProviderImpl());
	}

	/**
	 * Will use the provided client provider
	 * 
	 * @param clientProvider
	 */
	public BridgeClientImpl(HttpClientProvider clientProvider) {
		this(new SharedClientConnection(clientProvider));
	}

	/**
	 * Will use the same connection as other client
	 * 
	 * @param clientProvider
	 */
	public BridgeClientImpl(BaseClient otherClient) {
		this(otherClient.getSharedClientConnection());
	}

	private BridgeClientImpl(SharedClientConnection sharedClientConnection) {
		super(BRIDGE_JAVA_CLIENT + ClientVersionInfo.getClientVersionInfo(), sharedClientConnection);
		this.bridgeEndpoint = DEFAULT_BRIDGE_ENDPOINT;
	}
	/**
	 * @param bridgeEndpoint the bridgeEndpoint to set
	 */
	@Override
	public void setBridgeEndpoint(String bridgeEndpoint) {
		this.bridgeEndpoint = bridgeEndpoint;
	}

	/**
	 * Get the configured Bridge Service Endpoint
	 * 
	 * @return
	 */
	@Override
	public String getBridgeEndpoint() {
		return bridgeEndpoint;
	}

	/****** general bridge info ******/

	/**
	 * @return version
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	@Override
	public BridgeVersionInfo getBridgeVersionInfo() throws SynapseException {
		return get(VERSION_INFO, BridgeVersionInfo.class);
	}

	/****** communities ******/

	@Override
	public Community createCommunity(Community community) throws SynapseException {
		if (community == null)
			throw new IllegalArgumentException("Community cannot be null");
		return create(COMMUNITY, community);
	}

	@Override
	public PaginatedResults<Community> getCommunities(long limit, long offset) throws SynapseException {
		return getList(COMMUNITY + JOINED, Community.class, limit, offset);
	}

	@Override
	public PaginatedResults<Community> getAllCommunities(long limit, long offset) throws SynapseException {
		return getList(COMMUNITY, Community.class, limit, offset);
	}

	@Override
	public PaginatedResults<UserGroupHeader> getCommunityMembers(String communityId, long limit, long offset) throws SynapseException {
		return getList(COMMUNITY + "/" + communityId + MEMBER, UserGroupHeader.class, limit, offset);
	}

	@Override
	public Community getCommunity(String communityId) throws SynapseException {
		if (communityId == null)
			throw new IllegalArgumentException("Community Id cannot be null");
		return get(COMMUNITY + "/" + communityId, Community.class);
	}

	@Override
	public Community updateCommunity(Community community) throws SynapseException {
		if (community == null)
			throw new IllegalArgumentException("Community cannot be null");
		return update(COMMUNITY + "/" + community.getId(), community);
	}

	@Override
	public void deleteCommunity(String communityId) throws SynapseException {
		if (communityId == null)
			throw new IllegalArgumentException("Community Id cannot be null");
		delete(COMMUNITY + "/" + communityId);
	}

	@Override
	public void joinCommunity(String communityId) throws SynapseException {
		String uri = COMMUNITY + "/" + communityId + JOIN;
		get(uri);
	}

	@Override
	public void leaveCommunity(String communityId) throws SynapseException {
		String uri = COMMUNITY + "/" + communityId + LEAVE;
		get(uri);
	}

	@Override
	public void addCommunityAdmin(String communityId, String memberName) throws SynapseException {
		String uri = COMMUNITY + "/" + communityId + MEMBER + "/" + memberName + ADD_ADMIN;
		get(uri);
	}

	@Override
	public void removeCommunityAdmin(String communityId, String memberName) throws SynapseException {
		String uri = COMMUNITY + "/" + communityId + MEMBER + "/" + memberName + REMOVE_ADMIN;
		get(uri);
	}

	private void get(String uri) throws SynapseException {
		getSharedClientConnection().getJson(bridgeEndpoint, uri, getUserAgent());
	}

	private <T extends JSONEntity> T get(String uri, Class<T> klass) throws SynapseException {
		// Get the json for this entity
		try {
			JSONObject jsonObject = getSharedClientConnection().getJson(bridgeEndpoint, uri, getUserAgent());
			// Now convert to Object to an entity
			return (T) EntityFactory.createEntityFromJSONObject(jsonObject, klass);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	private <T extends JSONEntity> PaginatedResults<T> getList(String uri, Class<T> klass, long limit, long offset) throws SynapseException {
		// Get the json for this entity
		try {
			JSONObject jsonObj = getSharedClientConnection().getJson(bridgeEndpoint, uri, getUserAgent());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);

			uri = uri + "?limit=" + limit + "&offset=" + offset;

			PaginatedResults<T> results = new PaginatedResults<T>(klass);
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends JSONEntity> T update(String uri, T t) throws SynapseException {
		// Get the json for this entity
		try {
			JSONObject jsonObject = EntityFactory.createJSONObjectForEntity(t);
			jsonObject = getSharedClientConnection().putJson(bridgeEndpoint, uri, jsonObject.toString(), getUserAgent());
			// Now convert to Object to an entity
			return (T) EntityFactory.createEntityFromJSONObject(jsonObject, t.getClass());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends JSONEntity> T create(String uri, T t) throws SynapseException {
		// Get the json for this entity
		try {
			JSONObject jsonObject = EntityFactory.createJSONObjectForEntity(t);
			// Create the entity
			jsonObject = getSharedClientConnection().postJson(bridgeEndpoint, uri, jsonObject.toString(), getUserAgent());
			// Now convert to Object to an entity
			return (T) EntityFactory.createEntityFromJSONObject(jsonObject, t.getClass());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	private void delete(String uri) throws SynapseException {
		// Get the json for this entity
		getSharedClientConnection().deleteUri(bridgeEndpoint, uri, getUserAgent());
	}
}
