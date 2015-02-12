package org.sagebionetworks.client;

import java.util.Date;
import java.util.List;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataCurrentRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptorWithColumns;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatusList;
import org.sagebionetworks.bridge.model.timeseries.TimeSeriesTable;
import org.sagebionetworks.bridge.model.versionInfo.BridgeVersionInfo;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * Low-level Java Client API for Bridge REST APIs
 */
public class BridgeClientImpl extends BaseClientImpl implements BridgeClient {

	private static final String NORMALIZE_DATA_PARAM = "normalizeData";

	public static final String BRIDGE_JAVA_CLIENT = "Bridge-Java-Client/";

	private static final String DEFAULT_BRIDGE_ENDPOINT = "https://bridge-prod.prod.sagebase.org/bridge/v1";

	private static final String VERSION_INFO = "/version";

	private static final String COMMUNITY = "/community";
	private static final String MEMBER = "/member";
	private static final String PARTICIPANT_DATA = "/participantData";
	private static final String PARTICIPANT_DATA_ROW = "/row";
	private static final String PARTICIPANT_DATA_CURRENT = "/currentParticipantData";
	private static final String PARTICIPANT_DATA_DESCRIPTOR = "/participantDataDescriptor";
	private static final String PARTICIPANT_DATA_DESCRIPTOR_WITH_COLUMNS = "/participantDataDescriptorWithColumns";
	private static final String PARTICIPANT_DATA_COLUMN_DESCRIPTOR = "/participantDataColumnDescriptor";
	private static final String PARTICIPANT_DATA_DELETE_ROWS = "/deleteRows";
	private static final String PARTICIPANT = "/participant";
	private static final String TIME_SERIES = "/timeSeries";

	private static final String JOINED = "/joined";

	private static final String JOIN = "/join";
	private static final String LEAVE = "/leave";
	private static final String ADD_ADMIN = "/addadmin";
	private static final String REMOVE_ADMIN = "/removeadmin";

	private static final String SEND_UPDATES = "/sendupdates";

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

	public BridgeClientImpl(SharedClientConnection sharedClientConnection) {
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
		URIBuilder builder = new URIBuilder();
		builder.setPath(COMMUNITY + JOINED);
		return getList(builder, Community.class, limit, offset);
	}

	@Override
	public PaginatedResults<Community> getAllCommunities(long limit, long offset) throws SynapseException {
		URIBuilder builder = new URIBuilder();
		builder.setPath(COMMUNITY);
		return getList(builder, Community.class, limit, offset);
	}

	@Override
	public PaginatedResults<UserGroupHeader> getCommunityMembers(String communityId, long limit, long offset) throws SynapseException {
		URIBuilder builder = new URIBuilder();
		builder.setPath(COMMUNITY + "/" + communityId + MEMBER);
		return getList(builder, UserGroupHeader.class, limit, offset);
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
	public void addCommunityAdmin(String communityId, String principalId) throws SynapseException {
		String uri = COMMUNITY + "/" + communityId + MEMBER + "/" + principalId + ADD_ADMIN;
		get(uri);
	}

	@Override
	public void removeCommunityAdmin(String communityId, String principalId) throws SynapseException {
		String uri = COMMUNITY + "/" + communityId + MEMBER + "/" + principalId + REMOVE_ADMIN;
		get(uri);
	}

	@Override
	public List<ParticipantDataRow> appendParticipantData(String participantDataDescriptorId, List<ParticipantDataRow> data)
			throws SynapseException {
		String uri = PARTICIPANT_DATA + "/" + participantDataDescriptorId;
		return createList(uri, data, ParticipantDataRow.class);
	}

	@Override
	public List<ParticipantDataRow> appendParticipantData(String participantIdentifier, String participantDataDescriptorId,
			List<ParticipantDataRow> data) throws SynapseException {
		String uri = PARTICIPANT_DATA + "/" + participantDataDescriptorId + "/" + PARTICIPANT + "/" + participantIdentifier;
		return createList(uri, data, ParticipantDataRow.class);
	}

	@Override
	public void deleteParticipantDataRows(String participantDataDescriptorId, IdList rowIds) throws SynapseException {
		if (participantDataDescriptorId == null) {
			throw new IllegalArgumentException("No participantDataDescriptorId provided");
		}
		if (rowIds == null || rowIds.getList() == null || rowIds.getList().isEmpty()) {
			throw new IllegalArgumentException("No row IDs specified for deletion");
		}
		String uri = PARTICIPANT_DATA + "/" + participantDataDescriptorId + PARTICIPANT_DATA_DELETE_ROWS;
		delete(uri, rowIds);
	}

	@Override
	public List<ParticipantDataRow> updateParticipantData(String participantDataDescriptorId, List<ParticipantDataRow> data)
			throws SynapseException {
		String uri = PARTICIPANT_DATA + "/" + participantDataDescriptorId;
		return updateList(uri, data, ParticipantDataRow.class);
	}

	@Override
	public ParticipantDataCurrentRow getCurrentParticipantData(String participantDataDescriptorId, boolean normalizeData) throws SynapseException {
		URIBuilder builder = new URIBuilder();
		builder.setPath(PARTICIPANT_DATA_CURRENT + "/" + participantDataDescriptorId);
		builder.addParameter(NORMALIZE_DATA_PARAM, Boolean.toString(normalizeData));
		return get(builder.toString(), ParticipantDataCurrentRow.class);
	}

	@Override
	public ParticipantDataRow getParticipantDataRow(String participantDataDescriptorId, Long rowId,
			boolean normalizeData) throws SynapseException {
		URIBuilder builder = new URIBuilder();
		builder.setPath(PARTICIPANT_DATA + "/" + participantDataDescriptorId + PARTICIPANT_DATA_ROW + "/" + rowId);
		builder.setParameter(NORMALIZE_DATA_PARAM, Boolean.toString(normalizeData));
		return get(builder.toString(), ParticipantDataRow.class);
	}

	@Override
	public List<ParticipantDataRow> getCurrentRows(String participantDataDescriptorId, boolean normalizeData) throws SynapseException {
		URIBuilder builder = new URIBuilder();
		builder.setPath(PARTICIPANT_DATA + "/" + participantDataDescriptorId + "/current");
		builder.setParameter(NORMALIZE_DATA_PARAM, Boolean.toString(normalizeData));
		return getList(builder.toString(), ParticipantDataRow.class);
	}

	@Override
	public List<ParticipantDataRow> getHistoryRows(String participantDataDescriptorId, Date after, Date before,
			boolean normalizeData) throws SynapseException {
		URIBuilder builder = new URIBuilder();
		builder.setPath(PARTICIPANT_DATA + "/" + participantDataDescriptorId + "/history");
		builder.setParameter(NORMALIZE_DATA_PARAM, Boolean.toString(normalizeData));
		if (after != null) {
			builder.addParameter("after", Long.toString(after.getTime()));
		}
		if (before != null) {
			builder.addParameter("before", Long.toString(before.getTime()));
		}
		return getList(builder.toString(), ParticipantDataRow.class);
	}

	@Override
	public PaginatedResults<ParticipantDataRow> getRawParticipantData(String participantDataDescriptorId, long limit,
			long offset, boolean normalizeData) throws SynapseException {
		URIBuilder builder = new URIBuilder();
		builder.setPath(PARTICIPANT_DATA + "/" + participantDataDescriptorId);
		builder.setParameter(NORMALIZE_DATA_PARAM, Boolean.toString(normalizeData));
		return getList(builder, ParticipantDataRow.class, limit, offset);
	}

	@Override
	public ParticipantDataDescriptor createParticipantDataDescriptor(ParticipantDataDescriptor participantDataDescriptor)
			throws SynapseException {
		String uri = PARTICIPANT_DATA_DESCRIPTOR;
		return create(uri, participantDataDescriptor);
	}

	@Override
	public void updateParticipantDataDescriptor(ParticipantDataDescriptor participantDataDescriptor) throws SynapseException {
		String uri = PARTICIPANT_DATA_DESCRIPTOR;
		put(uri, participantDataDescriptor);
	}

	@Override
	public PaginatedResults<ParticipantDataDescriptor> getAllParticipantDataDescriptors(long limit, long offset) throws SynapseException {
		URIBuilder builder = new URIBuilder();
		builder.setPath(PARTICIPANT_DATA_DESCRIPTOR);
		return getList(builder, ParticipantDataDescriptor.class, limit, offset);
	}

	@Override
	public PaginatedResults<ParticipantDataDescriptor> getUserParticipantDataDescriptors(long limit, long offset) throws SynapseException {
		URIBuilder builder = new URIBuilder();
		builder.setPath(PARTICIPANT_DATA);
		return getList(builder, ParticipantDataDescriptor.class, limit, offset);
	}

	@Override
	public ParticipantDataDescriptorWithColumns getParticipantDataDescriptorWithColumns(String participantDataDescriptorId)
			throws SynapseException {
		String uri = PARTICIPANT_DATA_DESCRIPTOR_WITH_COLUMNS + "/" + participantDataDescriptorId;
		return get(uri, ParticipantDataDescriptorWithColumns.class);
	}

	@Override
	public ParticipantDataColumnDescriptor createParticipantDataColumnDescriptor(
			ParticipantDataColumnDescriptor participantDataColumnDescriptor1) throws SynapseException {
		String uri = PARTICIPANT_DATA_COLUMN_DESCRIPTOR;
		return create(uri, participantDataColumnDescriptor1);
	}

	@Override
	public PaginatedResults<ParticipantDataColumnDescriptor> getParticipantDataColumnDescriptors(String participantDataDescriptorId,
			long limit, long offset) throws SynapseException {
		URIBuilder builder = new URIBuilder();
		builder.setPath(PARTICIPANT_DATA_COLUMN_DESCRIPTOR + "/" + participantDataDescriptorId);
		return getList(builder, ParticipantDataColumnDescriptor.class, limit, offset);
	}

	@Override
	public void sendParticipantDataDescriptorUpdates(ParticipantDataStatusList statuses) throws SynapseException {
		String uri = PARTICIPANT_DATA + SEND_UPDATES;
		put(uri, statuses);
	}

	@Override
	public TimeSeriesTable getTimeSeries(String participantDataDescriptorId, List<String> columnNames,
			boolean normalizeData) throws SynapseException {
		URIBuilder builder = new URIBuilder();
		builder.setPath(TIME_SERIES + "/" + participantDataDescriptorId);
		builder.addParameter(NORMALIZE_DATA_PARAM, Boolean.toString(normalizeData));
		
		if (columnNames != null) {
			for (String columnName : columnNames) {
				builder.addParameter("columnName", columnName);
			}
		}
		return get(builder.toString(), TimeSeriesTable.class);
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
			throw new SynapseClientException(e);
		}
	}

	private <T extends JSONEntity> PaginatedResults<T> getList(URIBuilder builder, Class<T> klass, long limit, long offset) throws SynapseException {
		// Get the json for this entity
		builder.addParameter("limit", Long.toString(limit));
		builder.addParameter("offset", Long.toString(offset));
		try {
			JSONObject jsonObj = getSharedClientConnection().getJson(bridgeEndpoint, builder.toString(), getUserAgent());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);

			PaginatedResults<T> results = new PaginatedResults<T>(klass);
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	private <T extends JSONEntity> List<T> getList(String uri, Class<T> klass) throws SynapseException {
		// Get the json for this entity as a list wrapper
		try {
			JSONObject jsonObj = getSharedClientConnection().getJson(bridgeEndpoint, uri, getUserAgent());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			return ListWrapper.unwrap(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
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
			throw new SynapseClientException(e);
		}
	}

	private <T extends JSONEntity> List<T> updateList(String uri, List<T> t, Class<? extends T> clazz) throws SynapseException {
		// Get the json for this entity
		try {
			JSONObject jsonObject = EntityFactory.createJSONObjectForEntity(ListWrapper.wrap(t, clazz));
			jsonObject = getSharedClientConnection().putJson(bridgeEndpoint, uri, jsonObject.toString(), getUserAgent());
			// Now convert to Object to an entity list
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObject);
			return ListWrapper.unwrap(adapter, clazz);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends JSONEntity> T create(String uri, T t) throws SynapseException {
		// Get the json for this entity
		try {
			JSONObject jsonObject = EntityFactory.createJSONObjectForEntity(t);
			// Create the entity
			jsonObject = getSharedClientConnection().postJson(bridgeEndpoint, uri, jsonObject.toString(), getUserAgent(), null);
			// Now convert to Object to an entity
			return (T) EntityFactory.createEntityFromJSONObject(jsonObject, t.getClass());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	private <T extends JSONEntity> List<T> createList(String uri, List<T> t, Class<? extends T> clazz) throws SynapseException {
		// Get the json for this entity
		try {
			JSONObject jsonObject = EntityFactory.createJSONObjectForEntity(ListWrapper.wrap(t, clazz));
			// Create the entity
			jsonObject = getSharedClientConnection().postJson(bridgeEndpoint, uri, jsonObject.toString(), getUserAgent(), null);
			// Now convert to Object to an entity list
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObject);
			return ListWrapper.unwrap(adapter, clazz);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	private <T extends JSONEntity> void put(String uri, T t) throws SynapseException {
		// Get the json for this entity
		try {
			JSONObject jsonObject = EntityFactory.createJSONObjectForEntity(t);
			// Send the entity
			getSharedClientConnection().putJson(bridgeEndpoint, uri, jsonObject.toString(), getUserAgent());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	private void put(String uri) throws SynapseException {
		getSharedClientConnection().putJson(bridgeEndpoint, uri, "", getUserAgent());
	}

	private void delete(String uri) throws SynapseException {
		// Get the json for this entity
		getSharedClientConnection().deleteUri(bridgeEndpoint, uri, getUserAgent());
	}

	private <T extends JSONEntity> void delete(String uri, T t) throws SynapseException {
		try {
			JSONObject jsonObject = EntityFactory.createJSONObjectForEntity(t);
			getSharedClientConnection().postJson(bridgeEndpoint, uri, jsonObject.toString(), getUserAgent(), null);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
}
