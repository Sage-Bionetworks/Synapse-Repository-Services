package org.sagebionetworks.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Location;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.sagebionetworks.utils.MD5ChecksumHelper;

/**
 * Low-level Java Client API for Synapse REST APIs
 */
public class Synapse {

	private static final Logger log = Logger.getLogger(Synapse.class.getName());

	private static final int DEFAULT_TIMEOUT_MSEC = 5000;

	private static final int JSON_INDENT = 2;
	private static final String ANNOTATION_URI_SUFFIX = "annotations";
	private static final String DEFAULT_REPO_ENDPOINT = "https://staging-reposervice.elasticbeanstalk.com/repo/v1";
	private static final String DEFAULT_AUTH_ENDPOINT = "https://staging-reposervice.elasticbeanstalk.com/auth/v1";
	private static final String SESSION_TOKEN_HEADER = "sessionToken";
	private static final String REQUEST_PROFILE_DATA = "profile_request";
	private static final String PROFILE_RESPONSE_OBJECT_HEADER = "profile_response_object";
	private static final String QUERY_URI = "/query?query=";

	private String repoEndpoint;
	private String authEndpoint;

	private Map<String, String> defaultGETDELETEHeaders;
	private Map<String, String> defaultPOSTPUTHeaders;

	private JSONObject profileData;
	private boolean requestProfile;
	private HttpClientProvider clientProvider;

	/**
	 * Default constructor uses the default repository and auth services
	 * endpoints.
	 */
	public Synapse() {
		// Use the default provider
		this(new HttpClientProviderImpl());
		log.setLevel(Level.WARN);
	}

	/**
	 * Used for mock testing.
	 * 
	 * @param provider
	 */
	Synapse(HttpClientProvider provider) {
		if (provider == null)
			throw new IllegalArgumentException("Provider cannot be null");
		setRepositoryEndpoint(DEFAULT_REPO_ENDPOINT);
		setAuthEndpoint(DEFAULT_AUTH_ENDPOINT);

		defaultGETDELETEHeaders = new HashMap<String, String>();
		defaultGETDELETEHeaders.put("Accept", "application/json");

		defaultPOSTPUTHeaders = new HashMap<String, String>();
		defaultPOSTPUTHeaders.putAll(defaultGETDELETEHeaders);
		defaultPOSTPUTHeaders.put("Content-Type", "application/json");

		clientProvider = provider;
		clientProvider.setGlobalConnectionTimeout(DEFAULT_TIMEOUT_MSEC);
		clientProvider.setGlobalSocketTimeout(DEFAULT_TIMEOUT_MSEC);

		requestProfile = false;

	}

	/**
	 * @param repoEndpoint
	 *            the repoEndpoint to set
	 */
	public void setRepositoryEndpoint(String repoEndpoint) {
		this.repoEndpoint = repoEndpoint;
	}

	/**
	 * @param authEndpoint
	 *            the authEndpoint to set
	 */
	public void setAuthEndpoint(String authEndpoint) {
		this.authEndpoint = authEndpoint;
	}

	/**
	 * @param request
	 */
	public void setRequestProfile(boolean request) {
		this.requestProfile = request;
	}

	/**
	 * @return JSONObject
	 */
	public JSONObject getProfileData() {
		return this.profileData;
	}

	/**
	 * Log into Synapse
	 * 
	 * @param username
	 * @param password
	 * @throws JSONException
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public void login(String username, String password) throws JSONException,
			ClientProtocolException, IOException, SynapseUserException,
			SynapseServiceException {
		JSONObject loginRequest = new JSONObject();
		loginRequest.put("email", username);
		loginRequest.put("password", password);

		boolean reqPr = requestProfile;
		requestProfile = false;

		JSONObject credentials = createAuthEntity("/session", loginRequest);

		defaultGETDELETEHeaders.put(SESSION_TOKEN_HEADER, credentials
				.getString(SESSION_TOKEN_HEADER));
		defaultPOSTPUTHeaders.put(SESSION_TOKEN_HEADER, credentials
				.getString(SESSION_TOKEN_HEADER));

		requestProfile = reqPr;
	}

	/**
	 * Authenticate the synapse client with an existing session token
	 * 
	 * @param sessionToken
	 */
	public void login(String sessionToken) {
		defaultGETDELETEHeaders.put(SESSION_TOKEN_HEADER, sessionToken);
		defaultPOSTPUTHeaders.put(SESSION_TOKEN_HEADER, sessionToken);
	}

	/**
	 * Get the current session token used by this client.
	 * 
	 * @return the session token
	 */
	public String getCurrentSessionToken() {
		return defaultPOSTPUTHeaders.get(SESSION_TOKEN_HEADER);
	}

	/******************** Mid Level Repository Service APIs ********************/

	/**
	 * Create a new dataset, layer, etc ...
	 * 
	 * @param uri
	 * @param entity
	 * @return the newly created entity
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public JSONObject createEntity(String uri, JSONObject entity)
			throws ClientProtocolException, IOException, JSONException,
			SynapseUserException, SynapseServiceException {
		return createSynapseEntity(repoEndpoint, uri, entity);
	}

	/**
	 * Create a new Entity.
	 * 
	 * @param <T>
	 * @param entity
	 * @return the newly created entity
	 * @throws JSONObjectAdapterException
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	@SuppressWarnings("unchecked")
	public <T extends Entity> T createEntity(T entity)
			throws JSONObjectAdapterException, ClientProtocolException,
			IOException, JSONException, SynapseUserException,
			SynapseServiceException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");
		// Look up the EntityType for this entity.
		EntityType type = EntityType.getNodeTypeForClass(entity.getClass());
		// Get the json for this entity
		JSONObject jsonObject = EntityFactory.createJSONObjectForEntity(entity);
		// Create the entity
		jsonObject = createEntity(type.getUrlPrefix(), jsonObject);
		// Now convert to Object to an entity
		return (T) EntityFactory.createEntityFromJSONObject(jsonObject, entity
				.getClass());
	}

	/**
	 * Get a dataset, layer, preview, annotations, etc...
	 * 
	 * @param uri
	 * @return the retrieved entity
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public JSONObject getEntity(String uri) throws ClientProtocolException,
			IOException, JSONException, SynapseUserException,
			SynapseServiceException {
		return getSynapseEntity(repoEndpoint, uri);
	}

	/**
	 * Given an entity get the that entity (get the current version).
	 * 
	 * @param entity
	 * @param <T>
	 * @return the entity
	 * @throws IOException
	 * @throws JSONException
	 * @throws SynapseUserException
	 * @throws SynapseServiceException
	 * @throws JSONObjectAdapterException
	 */
	@SuppressWarnings("unchecked")
	public <T extends Entity> T getEntity(T entity) throws IOException,
			JSONException, SynapseUserException, SynapseServiceException,
			JSONObjectAdapterException {
		if (entity == null)
			throw new IllegalArgumentException("entity cannot be null");
		return (T) getEntity(entity.getId(), entity.getClass());
	}

	/**
	 * Get an entity given an Entity ID and the class of the Entity.
	 * 
	 * @param <T>
	 * @param entityId
	 * @param clazz
	 * @return the entity
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws JSONObjectAdapterException
	 */
	@SuppressWarnings("cast")
	public <T extends Entity> T getEntity(String entityId,
			Class<? extends T> clazz) throws IOException, JSONException,
			SynapseUserException, SynapseServiceException,
			JSONObjectAdapterException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		if (clazz == null)
			throw new IllegalArgumentException("Entity class cannot be null");
		EntityType type = EntityType.getNodeTypeForClass(clazz);
		// Build the URI
		String uri = createEntityUri(type.getUrlPrefix(), entityId);
		JSONObject object = getEntity(uri);
		return (T) EntityFactory.createEntityFromJSONObject(object, clazz);
	}

	/**
	 * Get an entity using only the entity's id
	 * 
	 * @param entityId
	 * @return the entity
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 * @throws SynapseUserException
	 * @throws SynapseServiceException
	 * @throws JSONObjectAdapterException
	 */
	public Entity getEntityById(String entityId)
			throws ClientProtocolException, IOException, JSONException,
			SynapseUserException, SynapseServiceException,
			JSONObjectAdapterException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		String url = "/entity/" + entityId + "/type";
		JSONObject jsonObj = getEntity(url);
		String objType = jsonObj.getString("type");
		EntityType type = EntityType.getFirstTypeInUrl(objType);
		return getEntity(entityId, type.getClassForType());
	}

	/**
	 * Helper to create an Entity URI.
	 * 
	 * @param prefix
	 * @param id
	 * @return
	 */
	private static String createEntityUri(String prefix, String id) {
		StringBuilder uri = new StringBuilder();
		uri.append(prefix);
		uri.append("/");
		uri.append(id);
		return uri.toString();
	}

	/**
	 * Update a dataset, layer, preview, annotations, etc...
	 * 
	 * This convenience method first grabs a copy of the currently stored
	 * entity, then overwrites fields from the entity passed in on top of the
	 * stored entity we retrieved and then PUTs the entity. This essentially
	 * does a partial update from the point of view of the user of this API.
	 * 
	 * Note that users of this API may want to inspect what they are overwriting
	 * before they do so. Another approach would be to do a GET, display the
	 * field to the user, allow them to edit the fields, and then do a PUT.
	 * 
	 * @param uri
	 * @param entity
	 * @return the updated entity
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	@Deprecated
	// Use putEntity
	public JSONObject updateEntity(String uri, JSONObject entity)
			throws ClientProtocolException, IOException, JSONException,
			SynapseUserException, SynapseServiceException {
		return updateSynapseEntity(repoEndpoint, uri, entity);
	}

	/**
	 * Update a dataset, layer, preview, annotations, etc...
	 * 
	 * @param <T>
	 * @param entity
	 * @return the updated entity
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 * @throws SynapseUserException
	 * @throws SynapseServiceException
	 * @throws JSONObjectAdapterException
	 */
	@SuppressWarnings("unchecked")
	public <T extends Entity> T putEntity(T entity)
			throws ClientProtocolException, IOException, JSONException,
			SynapseUserException, SynapseServiceException,
			JSONObjectAdapterException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");
		EntityType type = EntityType.getNodeTypeForClass(entity.getClass());
		String uri = createEntityUri(type.getUrlPrefix(), entity.getId());
		JSONObject jsonObject = EntityFactory.createJSONObjectForEntity(entity);
		jsonObject = putEntity(uri, jsonObject);
		return (T) EntityFactory.createEntityFromJSONObject(jsonObject, entity
				.getClass());
	}

	/**
	 * Update a dataset, layer, preview, annotations, etc...
	 * 
	 * @param uri
	 * @param entity
	 * @return the updated entity
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws IOException
	 * @throws JSONException
	 * @throws ClientProtocolException
	 */
	public JSONObject putEntity(String uri, JSONObject entity)
			throws ClientProtocolException, JSONException, IOException,
			SynapseUserException, SynapseServiceException {
		return putSynapseEntity(repoEndpoint, uri, entity);
	}

	/**
	 * Delete a dataset, layer, etc..
	 * 
	 * @param uri
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public void deleteEntity(String uri) throws ClientProtocolException,
			IOException, JSONException, SynapseUserException,
			SynapseServiceException {
		deleteSynapseEntity(repoEndpoint, uri);
		return;
	}

	/**
	 * Delete a dataset, layer, etc..
	 * 
	 * @param <T>
	 * @param entity
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 * @throws SynapseUserException
	 * @throws SynapseServiceException
	 */
	public <T extends Entity> void deleteEntity(T entity)
			throws ClientProtocolException, IOException, JSONException,
			SynapseUserException, SynapseServiceException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");
		EntityType type = EntityType.getNodeTypeForClass(entity.getClass());
		String uri = createEntityUri(type.getUrlPrefix(), entity.getId());
		deleteEntity(uri);
	}

	/**
	 * Perform a query
	 * 
	 * @param query
	 *            the query to perform
	 * @return the query result
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public JSONObject query(String query) throws ClientProtocolException,
			IOException, JSONException, SynapseUserException,
			SynapseServiceException {
		return querySynapse(repoEndpoint, query);
	}

	/**
	 * Download the locationable to a tempfile
	 * 
	 * @param locationable
	 * @return destination file
	 * @throws IOException
	 * @throws SynapseUserException
	 * @throws HttpClientHelperException
	 */
	public File downloadLocationableFromSynapse(Locationable locationable)
			throws IOException, HttpClientHelperException, SynapseUserException {
		// TODO do the equivalent of the R client synapse cache and file naming
		// scheme
		File file = File.createTempFile(locationable.getId(), ".txt");
		return downloadLocationableFromSynapse(locationable, file);
	}

	/**
	 * Download the locationable to the specified destination file using the
	 * default location type
	 * 
	 * @param locationable
	 * @param destinationFile
	 * @return destination file
	 * @throws HttpClientHelperException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws SynapseUserException
	 */
	public File downloadLocationableFromSynapse(Locationable locationable,
			File destinationFile) throws ClientProtocolException, IOException,
			HttpClientHelperException, SynapseUserException {
		List<LocationData> locations = locationable.getLocations();
		if ((null == locations) || (0 == locations.size())) {
			new SynapseUserException("No locations available for locationable "
					+ locationable);
		}

		// TODO if there are multiple locations for this locationable look in
		// user
		// preferences to download from the appropriate location (e.g., Sage
		// Internal versus S3 versus GoogleStorage). For now we are just
		// downloading from the first location
		LocationData location = locations.get(0);
		return downloadFromSynapse(location, destinationFile);
	}

	/**
	 * Download data from Synapse to the specified destination file given a
	 * specific location from which to download
	 * 
	 * @param location
	 * @param destinationFile
	 * @return destination file
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 * @throws SynapseUserException
	 */
	public File downloadFromSynapse(LocationData location, File destinationFile)
			throws ClientProtocolException, IOException,
			HttpClientHelperException, SynapseUserException {
		HttpClientHelper.downloadFile(location.getPath(), destinationFile
				.getAbsolutePath());
		// Check that the md5s match, if applicable
		if (null != location.getMd5()) {
			String localMd5 = MD5ChecksumHelper.getMD5Checksum(destinationFile
					.getAbsolutePath());
			if (!localMd5.equals(location.getMd5())) {
				throw new SynapseUserException(
						"md5 of downloaded file does not match the one in Synapse"
								+ destinationFile);
			}
		}

		return destinationFile;
	}

	/**
	 * TODO this will change with the collapse of layer and location
	 * 
	 * @param locationable
	 * @param dataFile
	 * 
	 * @return the newly created location
	 * @throws IOException
	 * @throws JSONException
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws DecoderException
	 * @throws HttpClientHelperException
	 * @throws JSONObjectAdapterException
	 */
	public Location uploadLocationableToSynapse(Locationable locationable,
			File dataFile) throws IOException, JSONException,
			SynapseUserException, SynapseServiceException, DecoderException,
			HttpClientHelperException, JSONObjectAdapterException {

		String md5 = MD5ChecksumHelper.getMD5Checksum(dataFile
				.getAbsolutePath());
		return uploadLocationableToSynapse(locationable, dataFile, md5);
	}

	/**
	 * TODO this will change with the collapse of layer and location
	 * 
	 * @param locationable
	 * @param dataFile
	 * @param md5
	 * @return the newly created location
	 * @throws IOException
	 * @throws JSONException
	 * @throws SynapseUserException
	 * @throws SynapseServiceException
	 * @throws DecoderException
	 * @throws HttpClientHelperException
	 * @throws JSONObjectAdapterException
	 */
	public Location uploadLocationableToSynapse(Locationable locationable,
			File dataFile, String md5) throws IOException, JSONException,
			SynapseUserException, SynapseServiceException, DecoderException,
			HttpClientHelperException, JSONObjectAdapterException {

		String s3Path = dataFile.getName();

		Location s3Location = new Location();
		s3Location.setPath("/" + s3Path);
		s3Location.setMd5sum(md5);
		s3Location.setParentId(locationable.getParentId());
		s3Location.setType(LocationTypeNames.awss3);
		s3Location = createEntity(s3Location);

		// TODO find a more direct way to go from hex to base64
		byte[] encoded = Base64.encodeBase64(Hex.decodeHex(md5.toCharArray()));
		String base64Md5 = new String(encoded, "ASCII");

		Map<String, String> headerMap = new HashMap<String, String>();
		headerMap.put("x-amz-acl", "bucket-owner-full-control");
		headerMap.put("Content-MD5", base64Md5);
		headerMap.put("Content-Type", s3Location.getContentType());

		clientProvider.uploadFile(s3Location.getPath(), dataFile
				.getAbsolutePath(), s3Location.getContentType(), headerMap);

		return getEntity(s3Location.getId(), Location.class);
	}

	/******************** Mid Level Authorization Service APIs ********************/

	/**
	 * Create a new login, etc ...
	 * 
	 * @param uri
	 * @param entity
	 * @return the newly created entity
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public JSONObject createAuthEntity(String uri, JSONObject entity)
			throws ClientProtocolException, IOException, JSONException,
			SynapseUserException, SynapseServiceException {
		return createSynapseEntity(authEndpoint, uri, entity);
	}

	/******************** Low Level APIs ********************/

	/**
	 * Create a new dataset, layer, etc ...
	 * 
	 * @param endpoint
	 * @param uri
	 * @param entity
	 * @return the newly created entity
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public JSONObject createSynapseEntity(String endpoint, String uri,
			JSONObject entity) throws ClientProtocolException, IOException,
			JSONException, SynapseUserException, SynapseServiceException {
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}

		return dispatchSynapseRequest(endpoint, uri, "POST", entity.toString(),
				defaultPOSTPUTHeaders);
	}

	/**
	 * Get a dataset, layer, preview, annotations, etc...
	 * 
	 * @param endpoint
	 * @param uri
	 * @return the retrieved entity
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public JSONObject getSynapseEntity(String endpoint, String uri)
			throws ClientProtocolException, IOException, JSONException,
			SynapseUserException, SynapseServiceException {
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}

		return dispatchSynapseRequest(endpoint, uri, "GET", null,
				defaultGETDELETEHeaders);
	}

	/**
	 * Update a dataset, layer, preview, annotations, etc...
	 * 
	 * This convenience method first grabs a copy of the currently stored
	 * entity, then overwrites fields from the entity passed in on top of the
	 * stored entity we retrieved and then PUTs the entity. This essentially
	 * does a partial update from the point of view of the user of this API.
	 * 
	 * Note that users of this API may want to inspect what they are overwriting
	 * before they do so. Another approach would be to do a GET, display the
	 * field to the user, allow them to edit the fields, and then do a PUT.
	 * 
	 * @param endpoint
	 * @param uri
	 * @param entity
	 * @return the updated entity
	 * @throws ClientProtocolException
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public JSONObject updateSynapseEntity(String endpoint, String uri,
			JSONObject entity) throws ClientProtocolException, IOException,
			JSONException, SynapseUserException, SynapseServiceException {

		JSONObject storedEntity = getSynapseEntity(endpoint, uri);

		boolean isAnnotation = uri.endsWith(ANNOTATION_URI_SUFFIX);

		Iterator<String> keyIter = entity.keys();
		while (keyIter.hasNext()) {
			String key = keyIter.next();
			if (isAnnotation) {
				// Annotations need to go one level deeper
				JSONObject storedAnnotations = storedEntity.getJSONObject(key);
				JSONObject entityAnnotations = entity.getJSONObject(key);
				Iterator<String> annotationIter = entity.getJSONObject(key)
						.keys();
				while (annotationIter.hasNext()) {
					String annotationKey = annotationIter.next();
					storedAnnotations.put(annotationKey, entityAnnotations
							.get(annotationKey));
				}
			} else {
				storedEntity.put(key, entity.get(key));
			}
		}

		return putSynapseEntity(endpoint, uri, storedEntity);

	}

	/**
	 * Update a dataset, layer, preview, annotations, etc...
	 * 
	 * @param endpoint
	 * @param uri
	 * @param entity
	 * @return the updated entity
	 * @throws JSONException
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public JSONObject putSynapseEntity(String endpoint, String uri,
			JSONObject entity) throws JSONException, ClientProtocolException,
			IOException, SynapseUserException, SynapseServiceException {
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}

		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultPOSTPUTHeaders);
		requestHeaders.put("ETag", entity.getString("etag"));

		return dispatchSynapseRequest(endpoint, uri, "PUT", entity.toString(),
				requestHeaders);
	}

	/**
	 * Delete a dataset, layer, etc..
	 * 
	 * @param endpoint
	 * @param uri
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public void deleteSynapseEntity(String endpoint, String uri)
			throws ClientProtocolException, IOException, JSONException,
			SynapseUserException, SynapseServiceException {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}

		dispatchSynapseRequest(endpoint, uri, "DELETE", null,
				defaultGETDELETEHeaders);
		return;
	}

	/**
	 * Perform a query
	 * 
	 * @param endpoint
	 * @param query
	 *            the query to perform
	 * @return the query result
	 * @throws ClientProtocolException
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 */
	public JSONObject querySynapse(String endpoint, String query)
			throws ClientProtocolException, IOException, JSONException,
			SynapseUserException, SynapseServiceException {
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == query) {
			throw new IllegalArgumentException("must provide a query");
		}

		String queryUri = QUERY_URI + URLEncoder.encode(query, "UTF-8");

		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultGETDELETEHeaders);

		return dispatchSynapseRequest(endpoint, queryUri, "GET", null,
				requestHeaders);
	}

	/**
	 * Convert exceptions emanating from the service to
	 * Synapse[User|Service]Exception but let all other types of exceptions
	 * bubble up as usual
	 * 
	 * @param requestUrl
	 * @param requestMethod
	 * @param requestContent
	 * @param requestHeaders
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws JSONException
	 * @throws SynapseUserException
	 * @throws SynapseServiceException
	 */
	private JSONObject dispatchSynapseRequest(String endpoint, String uri,
			String requestMethod, String requestContent,
			Map<String, String> requestHeaders) throws ClientProtocolException,
			IOException, JSONException, SynapseUserException,
			SynapseServiceException {

		if (requestProfile && !requestMethod.equals("DELETE")) {
			requestHeaders.put(REQUEST_PROFILE_DATA, "true");
		} else {
			if (requestHeaders.containsKey(REQUEST_PROFILE_DATA))
				requestHeaders.remove(REQUEST_PROFILE_DATA);
		}

		URL parsedEndpoint = new URL(endpoint);
		String endpointPrefix = parsedEndpoint.getPath();
		String endpointLocation = endpoint.substring(0, endpoint.length()
				- endpointPrefix.length());

		URL requestUrl = (uri.startsWith(endpointPrefix)) ? new URL(
				endpointLocation + uri) : new URL(endpoint + uri);

		JSONObject results = null;
		try {
			HttpResponse response = clientProvider.performRequest(requestUrl
					.toString(), requestMethod, requestContent, requestHeaders);
			String responseBody = (null != response.getEntity()) ? EntityUtils
					.toString(response.getEntity()) : null;

			if (requestProfile && !requestMethod.equals("DELETE")) {
				Header header = response
						.getFirstHeader(PROFILE_RESPONSE_OBJECT_HEADER);
				String encoded = header.getValue();
				String decoded = new String(Base64.decodeBase64(encoded
						.getBytes("UTF-8")), "UTF-8");
				profileData = new JSONObject(decoded);
			} else {
				profileData = null;
			}

			if (null != responseBody) {
				results = new JSONObject(responseBody);
				if (log.isDebugEnabled()) {
					log.debug(requestMethod + " " + requestUrl + " : "
							+ results.toString(JSON_INDENT));
				}
			}

		} catch (HttpClientHelperException e) {
			// Well-handled server side exceptions come back as JSON, attempt to
			// deserialize and convert the error
			int statusCode = 500; // assume a service exception
			statusCode = e.getHttpStatus();
			String response = (null != e.getResponse().getEntity()) ? EntityUtils
					.toString(e.getResponse().getEntity())
					: null;
			try {
				results = new JSONObject(response);
				if (log.isDebugEnabled()) {
					log.debug("Retrieved " + requestUrl + " : "
							+ results.toString(JSON_INDENT));
				}

				if ((400 <= statusCode) && (500 > statusCode)) {
					throw new SynapseUserException("User Error(" + statusCode
							+ "): " + results.getString("reason"));
				}
				throw new SynapseServiceException("Service Error(" + statusCode
						+ "): " + results.getString("reason"));
			} catch (JSONException jsonEx) {
				// swallow the JSONException since its not the real problem and
				// return the response as-is since it is not JSON
				throw new SynapseServiceException("Service Error(" + statusCode
						+ "): " + response);
			}
		} // end catch

		return results;
	}
}
