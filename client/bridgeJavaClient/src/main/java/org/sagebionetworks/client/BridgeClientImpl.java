package org.sagebionetworks.client;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.bridge.model.versionInfo.BridgeVersionInfo;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.PaginatedResults;
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
	private static final String USER = "/user";
	
	private static final String JOIN = "/join";
	private static final String LEAVE = "/leave";

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
	public BridgeVersionInfo getVersionInfo() throws SynapseException {
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
	public List<Community> getCommunities() throws SynapseException {
		return getList(COMMUNITY, Community.class);
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
	public List<Community> getCommunitiesByMember() throws SynapseException {
		return getList(USER + COMMUNITY, Community.class);
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

	private <T extends JSONEntity> List<T> getList(String uri, Class<T> klass) throws SynapseException {
		// Get the json for this entity
		try {
			JSONObject jsonObj = getSharedClientConnection().getJson(bridgeEndpoint, uri, getUserAgent());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			BatchResults<T> results = new BatchResults<T>(klass);
			results.initializeFromJSONObject(adapter);
			return results.getResults();
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
