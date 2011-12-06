package org.sagebionetworks.client;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
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
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Location;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.schema.adapter.JSONEntity;
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
	 * @throws SynapseException
	 */
	public void login(String username, String password) throws SynapseException {
		JSONObject loginRequest = new JSONObject();
		try {
			loginRequest.put("email", username);
			loginRequest.put("password", password);

			boolean reqPr = requestProfile;
			requestProfile = false;

			JSONObject credentials = createAuthEntity("/session", loginRequest);

			defaultGETDELETEHeaders.put(SESSION_TOKEN_HEADER,
					credentials.getString(SESSION_TOKEN_HEADER));
			defaultPOSTPUTHeaders.put(SESSION_TOKEN_HEADER,
					credentials.getString(SESSION_TOKEN_HEADER));
			requestProfile = reqPr;
		} catch (JSONException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Authenticate the synapse client with an existing session token
	 * 
	 * @param sessionToken
	 */
	public void setSessionToken(String sessionToken) {
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
	 */
	public JSONObject createEntity(String uri, JSONObject entity)
			throws SynapseException {
		return createSynapseEntity(repoEndpoint, uri, entity);
	}

	/**
	 * Create a new Entity.
	 * 
	 * @param <T>
	 * @param entity
	 * @return
	 * @throws SynapseException
	 */
	@SuppressWarnings("unchecked")
	public <T extends JSONEntity> T createEntity(T entity) throws SynapseException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");
		// Look up the EntityType for this entity.
		EntityType type = EntityType.getNodeTypeForClass(entity.getClass());
		// Get the json for this entity
		JSONObject jsonObject;
		try {
			jsonObject = EntityFactory.createJSONObjectForEntity(entity);
			// Create the entity
			jsonObject = createEntity(type.getUrlPrefix(), jsonObject);
			// Now convert to Object to an entity
			return (T) EntityFactory.createEntityFromJSONObject(jsonObject,
					entity.getClass());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Get a dataset, layer, preview, annotations, etc...
	 * 
	 * @param uri
	 * @return the retrieved entity
	 */
	public JSONObject getEntity(String uri) throws SynapseException {
		return getSynapseEntity(repoEndpoint, uri);
	}


	/**
	 * Given an entity get the that entity (get the current version).
	 * 
	 * @param entity
	 * @param <T>
	 * @return the entity
	 * @throws SynapseException
	 */
	@SuppressWarnings("unchecked")
	public <T extends JSONEntity> T getEntity(T entity) throws SynapseException {
		if (entity == null)
			throw new IllegalArgumentException("entity cannot be null");

		if(entity instanceof Entity) {
			return (T) getEntity(((Entity) entity).getId(), entity.getClass());
		} else {
			// TODO : Nicole--add non Entity Types here
			throw new SynapseException("NYI");
		}
	
	}

	/**
	 * Get an entity given an Entity ID and the class of the Entity.
	 * 
	 * @param <T>
	 * @param entityId
	 * @param clazz
	 * @return
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws JSONObjectAdapterException
	 */
	public <T extends JSONEntity> T getEntity(String entityId,
			Class<? extends T> clazz) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		if (clazz == null)
			throw new IllegalArgumentException("Entity class cannot be null");
		EntityType type = EntityType.getNodeTypeForClass(clazz);
		// Build the URI
		String uri = createEntityUri(type.getUrlPrefix(), entityId);
		JSONObject object = getEntity(uri);
		try {
			return (T) EntityFactory.createEntityFromJSONObject(object, clazz);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	public Entity getEntityById(String entityId) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		String url = "/entity/" + entityId + "/type";
		JSONObject jsonObj = getEntity(url);
		String objType;
		try {
			objType = jsonObj.getString("type");
			EntityType type = EntityType.getFirstTypeInUrl(objType);
			return getEntity(entityId, type.getClassForType());
		} catch (JSONException e) {
			throw new SynapseException(e);
		}
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
			throws SynapseException {
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
	public <T extends Entity> T putEntity(T entity) throws SynapseException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");
		try {
			EntityType type = EntityType.getNodeTypeForClass(entity.getClass());
			String uri = createEntityUri(type.getUrlPrefix(), entity.getId());
			JSONObject jsonObject;
			jsonObject = EntityFactory.createJSONObjectForEntity(entity);
			jsonObject = putEntity(uri, jsonObject);
			return (T) EntityFactory.createEntityFromJSONObject(jsonObject,
					entity.getClass());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Update a dataset, layer, preview, annotations, etc...
	 * 
	 * @param uri
	 * @param entity
	 * @return the updated entity
	 * @throws SynapseException
	 */
	public JSONObject putEntity(String uri, JSONObject entity)
			throws SynapseException {
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
	public void deleteEntity(String uri) throws SynapseException {
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
			throws SynapseException {
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
	public JSONObject query(String query) throws SynapseException {
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
			throws SynapseException {
		// TODO do the equivalent of the R client synapse cache and file naming
		// scheme
		File file;
		try {
			file = File.createTempFile(locationable.getId(), ".txt");
			return downloadLocationableFromSynapse(locationable, file);
		} catch (IOException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Download the locationable to the specified destination file using the
	 * default location type
	 * 
	 * @param locationable
	 * @param destinationFile
	 * @return destination file
	 * @throws SynapseException
	 */
	public File downloadLocationableFromSynapse(Locationable locationable,
			File destinationFile) throws SynapseException {
		List<LocationData> locations = locationable.getLocations();
		if ((null == locations) || (0 == locations.size())) {
			throw new SynapseUserException("No locations available for locationable "
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
	 * @throws SynapseException
	 */
	public File downloadFromSynapse(LocationData location, File destinationFile)
			throws SynapseException {
		try {
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
		} catch (ClientProtocolException e) {
			throw new SynapseException(e);
		} catch (IOException e) {
			throw new SynapseException(e);
		} catch (HttpClientHelperException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * TODO this will change with the collapse of layer and location
	 * 
	 * @param locationable
	 * @param dataFile
	 * 
	 * @return the newly created location
	 * @throws SynapseException
	 */
	public Location uploadLocationableToSynapse(Locationable locationable,
			File dataFile) throws SynapseException {

		try {
			String md5 = MD5ChecksumHelper.getMD5Checksum(dataFile
					.getAbsolutePath());
			return uploadLocationableToSynapse(locationable, dataFile, md5);
		} catch (IOException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * TODO this will change with the collapse of layer and location
	 * 
	 * @param locationable
	 * @param dataFile
	 * @param md5
	 * @return the newly created location
	 * @throws SynapseException
	 */
	public Location uploadLocationableToSynapse(Locationable locationable,
			File dataFile, String md5) throws SynapseException {
		try {
			String s3Path = dataFile.getName();
			
			Location s3Location = new Location();
			s3Location.setPath("/" + s3Path);
			s3Location.setMd5sum(md5);
			s3Location.setParentId(locationable.getParentId());
			s3Location.setType(LocationTypeNames.awss3);
			s3Location = createEntity(s3Location);
			
			// TODO find a more direct way to go from hex to base64
			byte[] encoded;
			encoded = Base64.encodeBase64(Hex.decodeHex(md5.toCharArray()));
			String base64Md5 = new String(encoded, "ASCII");
			
			Map<String, String> headerMap = new HashMap<String, String>();
			headerMap.put("x-amz-acl", "bucket-owner-full-control");
			headerMap.put("Content-MD5", base64Md5);
			headerMap.put("Content-Type", s3Location.getContentType());
			
			clientProvider.uploadFile(s3Location.getPath(), dataFile
					.getAbsolutePath(), s3Location.getContentType(), headerMap);
			
			return getEntity(s3Location.getId(), Location.class);
		} catch (DecoderException e) {
			throw new SynapseException(e);
		} catch (UnsupportedEncodingException e) {
			throw new SynapseException(e);
		} catch (ClientProtocolException e) {
			throw new SynapseException(e);
		} catch (IOException e) {
			throw new SynapseException(e);
		} catch (HttpClientHelperException e) {
			throw new SynapseException(e);
		}
	}

	/******************** Mid Level Authorization Service APIs ********************/

	/**
	 * Create a new login, etc ...
	 * 
	 * @param uri
	 * @param entity
	 * @return the newly created entity
	 * @throws SynapseException
	 */
	public JSONObject createAuthEntity(String uri, JSONObject entity)
			throws SynapseException {
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
	 * @throws SynapseException
	 */
	public JSONObject createSynapseEntity(String endpoint, String uri,
			JSONObject entity) throws SynapseException {
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
	 * @throws SynapseException
	 */
	public JSONObject getSynapseEntity(String endpoint, String uri)
			throws SynapseException {
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
	 * @throws SynapseException
	 */
	@SuppressWarnings("unchecked")
	public JSONObject updateSynapseEntity(String endpoint, String uri,
			JSONObject entity) throws SynapseException {

		JSONObject storedEntity = getSynapseEntity(endpoint, uri);

		boolean isAnnotation = uri.endsWith(ANNOTATION_URI_SUFFIX);
		try {
			Iterator<String> keyIter = entity.keys();
			while (keyIter.hasNext()) {
				String key = keyIter.next();
				if (isAnnotation) {
					// Annotations need to go one level deeper
					JSONObject storedAnnotations = storedEntity
							.getJSONObject(key);
					JSONObject entityAnnotations = entity.getJSONObject(key);
					Iterator<String> annotationIter = entity.getJSONObject(key)
							.keys();
					while (annotationIter.hasNext()) {
						String annotationKey = annotationIter.next();
						storedAnnotations.put(annotationKey,
								entityAnnotations.get(annotationKey));
					}
				} else {
					storedEntity.put(key, entity.get(key));
				}
			}
			return putSynapseEntity(endpoint, uri, storedEntity);
		} catch (JSONException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Update a dataset, layer, preview, annotations, etc...
	 * 
	 * @param endpoint
	 * @param uri
	 * @param entity
	 * @return the updated entity
	 * @throws SynapseException
	 */
	public JSONObject putSynapseEntity(String endpoint, String uri,
			JSONObject entity) throws SynapseException {
		try {
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

			return dispatchSynapseRequest(endpoint, uri, "PUT",
					entity.toString(), requestHeaders);
		} catch (JSONException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Delete a dataset, layer, etc..
	 * 
	 * @param endpoint
	 * @param uri
	 */
	public void deleteSynapseEntity(String endpoint, String uri)
			throws SynapseException {
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
	 */
	public JSONObject querySynapse(String endpoint, String query)
			throws SynapseException {
		try {
			if (null == endpoint) {
				throw new IllegalArgumentException("must provide endpoint");
			}
			if (null == query) {
				throw new IllegalArgumentException("must provide a query");
			}

			String queryUri;
			queryUri = QUERY_URI + URLEncoder.encode(query, "UTF-8");

			Map<String, String> requestHeaders = new HashMap<String, String>();
			requestHeaders.putAll(defaultGETDELETEHeaders);

			return dispatchSynapseRequest(endpoint, queryUri, "GET", null,
					requestHeaders);
		} catch (UnsupportedEncodingException e) {
			throw new SynapseException(e);
		}
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
	 */
	private JSONObject dispatchSynapseRequest(String endpoint, String uri,
			String requestMethod, String requestContent,
			Map<String, String> requestHeaders) throws SynapseException {

		if (requestProfile && !requestMethod.equals("DELETE")) {
			requestHeaders.put(REQUEST_PROFILE_DATA, "true");
		} else {
			if (requestHeaders.containsKey(REQUEST_PROFILE_DATA))
				requestHeaders.remove(REQUEST_PROFILE_DATA);
		}
		JSONObject results = null;
		URL requestUrl = null;

		try {
			URL parsedEndpoint = new URL(endpoint);
			String endpointPrefix = parsedEndpoint.getPath();
			String endpointLocation = endpoint.substring(0, endpoint.length()
					- endpointPrefix.length());

			requestUrl = (uri.startsWith(endpointPrefix)) ? new URL(
					endpointLocation + uri) : new URL(endpoint + uri);

			HttpResponse response = clientProvider.performRequest(
					requestUrl.toString(), requestMethod, requestContent,
					requestHeaders);
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
			String response = null;
			try {
				String exceptionContent = "Service Error(" + statusCode + "): "
						+ results.getString("reason");

				response = (null != e.getResponse().getEntity()) ? EntityUtils
						.toString(e.getResponse().getEntity()) : null;

				results = new JSONObject(response);
				if (log.isDebugEnabled()) {
					log.debug("Retrieved " + requestUrl + " : "
							+ results.toString(JSON_INDENT));
				}

				if (statusCode == 401) {
					throw new SynapseUnauthorizedException(exceptionContent);
				} else if (statusCode == 403) {
					throw new SynapseForbiddenException(exceptionContent);
				} else if (statusCode == 404) {
					throw new SynapseNotFoundException(exceptionContent);
				} else if (statusCode == 400) {
					throw new SynapseBadRequestException(exceptionContent);
				} else if (statusCode >= 400 && statusCode < 500) {
					throw new SynapseUserException(exceptionContent);
				} else {
					throw new SynapseServiceException(exceptionContent);
				}

			} catch (JSONException jsonEx) {
				// swallow the JSONException since its not the real problem and
				// return the response as-is since it is not JSON
				throw new SynapseServiceException(jsonEx);
			} catch (ParseException parseEx) {
				throw new SynapseServiceException(parseEx);
			} catch (IOException ioEx) {
				throw new SynapseServiceException(ioEx);
			}
		} // end catch
		catch (MalformedURLException e) {
			throw new SynapseServiceException(e);
		} catch (ClientProtocolException e) {
			throw new SynapseServiceException(e);
		} catch (IOException e) {
			throw new SynapseServiceException(e);
		} catch (JSONException e) {
			throw new SynapseServiceException(e);
		}

		return results;
	}
}
