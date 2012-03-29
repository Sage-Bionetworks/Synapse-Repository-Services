package org.sagebionetworks.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.repo.ServiceConstants;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AutoGenFactory;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.S3Token;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.securitytools.HMACUtils;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.sagebionetworks.utils.MD5ChecksumHelper;

/**
 * Low-level Java Client API for Synapse REST APIs
 */
public class Synapse {

	protected static final Logger log = Logger.getLogger(Synapse.class.getName());

	protected static final int DEFAULT_TIMEOUT_MSEC = 5000;

	protected static final int JSON_INDENT = 2;
	protected static final String DEFAULT_REPO_ENDPOINT = "https://repo-alpha.sagebase.org/repo/v1";
	protected static final String DEFAULT_AUTH_ENDPOINT = "https://auth-alpha.sagebase.org/auth/v1";
	protected static final String SESSION_TOKEN_HEADER = "sessionToken";
	protected static final String REQUEST_PROFILE_DATA = "profile_request";
	protected static final String PROFILE_RESPONSE_OBJECT_HEADER = "profile_response_object";

	protected static final String QUERY_URI = "/query?query=";
	protected static final String REPO_SUFFIX_PATH = "/path";
	protected static final String ANNOTATION_URI_SUFFIX = "annotations";
	protected static final String ADMIN = "/admin";
	protected static final String STACK_STATUS = ADMIN + "/synapse/status";
	protected static final String ENTITY = "/entity";
	protected static final String ATTACHMENT_S3_TOKEN = "/s3AttachmentToken";
	protected static final String ATTACHMENT_URL = "/attachmentUrl";

	protected String repoEndpoint;
	protected String authEndpoint;

	protected Map<String, String> defaultGETDELETEHeaders;
	protected Map<String, String> defaultPOSTPUTHeaders;

	protected JSONObject profileData;
	protected boolean requestProfile;
	protected HttpClientProvider clientProvider;
	protected DataUploader dataUploader;

	protected AutoGenFactory autoGenFactory = new AutoGenFactory();
	/**
	 * Default constructor uses the default repository and auth services
	 * endpoints.
	 */
	public Synapse() {
		// Use the default implementations
		this(new HttpClientProviderImpl(), new DataUploaderMultipartImpl());
	}

	/**
	 * Will use the provided client provider and data uploader.
	 * 
	 * @param clientProvider 
	 * @param dataUploader 
	 */
	public Synapse(HttpClientProvider clientProvider, DataUploader dataUploader) {
		if (clientProvider == null)
			throw new IllegalArgumentException("HttpClientProvider cannot be null");

		if (dataUploader == null)
			throw new IllegalArgumentException("DataUploader cannot be null");

		setRepositoryEndpoint(DEFAULT_REPO_ENDPOINT);
		setAuthEndpoint(DEFAULT_AUTH_ENDPOINT);

		defaultGETDELETEHeaders = new HashMap<String, String>();
		defaultGETDELETEHeaders.put("Accept", "application/json");

		defaultPOSTPUTHeaders = new HashMap<String, String>();
		defaultPOSTPUTHeaders.putAll(defaultGETDELETEHeaders);
		defaultPOSTPUTHeaders.put("Content-Type", "application/json");

		this.clientProvider = clientProvider;
		clientProvider.setGlobalConnectionTimeout(DEFAULT_TIMEOUT_MSEC);
		clientProvider.setGlobalSocketTimeout(DEFAULT_TIMEOUT_MSEC);

		this.dataUploader = dataUploader;
		
		requestProfile = false;

	}

	/**
	 * Use this method to override the default implementation of {@link HttpClientProvider}
	 * @param clientProvider
	 */
	public void setHttpClientProvider(HttpClientProvider clientProvider) {
		this.clientProvider = clientProvider;
	}
	
	/**
	 * Use this method to override the default implementation of {@link DataUploader}
	 * 
	 * @param dataUploader
	 */
	public void setDataUploader(DataUploader dataUploader) {
		this.dataUploader = dataUploader;
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
	
	protected String userName;
	protected String apiKey;
	

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the apiKey
	 */
	public String getApiKey() {
		return apiKey;
	}

	/**
	 * @param apiKey the apiKey to set
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * Log into Synapse
	 * 
	 * @param username
	 * @param password
	 * @throws SynapseException
	 */
	public void login(String username, String password) throws SynapseException {
		/**
		 * Log into Synapse
		 * 
		 * @param username
		 * @param password
		 * @throws SynapseException
		 */
		login(username, password, false);
	}
		
	public void login(String username, String password, boolean explicitlyAcceptsTermsOfUse) throws SynapseException {
		
			JSONObject loginRequest = new JSONObject();
		try {
			loginRequest.put("email", username);
			loginRequest.put("password", password);
			if (explicitlyAcceptsTermsOfUse) loginRequest.put("acceptsTermsOfUse", true);
			
			boolean reqPr = requestProfile;
			requestProfile = false;

			JSONObject credentials = createAuthEntity("/session", loginRequest);

			defaultGETDELETEHeaders.put(SESSION_TOKEN_HEADER, credentials
					.getString(SESSION_TOKEN_HEADER));
			defaultPOSTPUTHeaders.put(SESSION_TOKEN_HEADER, credentials
					.getString(SESSION_TOKEN_HEADER));
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
	 * @throws SynapseException
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
	 * @return the newly created entity
	 * @throws SynapseException
	 */
	@SuppressWarnings("unchecked")
	public <T extends JSONEntity> T createEntity(T entity)
			throws SynapseException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");
		// Look up the EntityType for this entity.
		EntityType type = EntityType.getNodeTypeForClass(entity.getClass());
		return createEntity(type.getUrlPrefix(), entity);
	}

	/**
	 * Create a new Entity.
	 * 
	 * @param <T>
	 * @param entity
	 * @return the newly created entity
	 * @throws SynapseException
	 */
	@SuppressWarnings("unchecked")
	public <T extends JSONEntity> T createEntity(String uri, T entity)
			throws SynapseException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");
		// Get the json for this entity
		JSONObject jsonObject;
		try {
			jsonObject = EntityFactory.createJSONObjectForEntity(entity);
			// Create the entity
			jsonObject = createEntity(uri, jsonObject);
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
	 * @throws SynapseException
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

		if (entity instanceof Entity) {
			return (T) getEntity(((Entity) entity).getId(), entity.getClass());
		}
		// TODO : Nicole--add non Entity Types here
		throw new SynapseException("NYI");

	}

	/**
	 * Get an entity using its ID.
	 * @param entityId
	 * @return the entity
	 * @throws SynapseException
	 */
	public Entity getEntityById(String entityId) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		String url = "/entity/" + entityId;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		// Get the type from the object
		if(!adapter.has("entityType")) throw new RuntimeException("EntityType returned was null!");
		try {
			String entityType = adapter.getString("entityType");
			Entity entity = (Entity) autoGenFactory.newInstance(entityType);
			entity.initializeFromJSONObject(adapter);
			return entity;
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}
	
	/**
	 * Get the current user's permission for a given entity.
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	public UserEntityPermissions getUsersEntityPermissions(String entityId) throws SynapseException{
		String url = "/entity/" + entityId+"/permissions";
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		UserEntityPermissions uep = new UserEntityPermissions();
		try {
			uep.initializeFromJSONObject(adapter);
			return uep;
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get the annotaions for an entity.
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	public Annotations getAnnotations(String entityId) throws SynapseException{
		String url = "/entity/" + entityId+"/annotations";
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		Annotations annos = new Annotations();
		try {
			annos.initializeFromJSONObject(adapter);
			return annos;
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Update the annotaions of an entity.
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	public Annotations updateAnnotations(String entityId, Annotations updated) throws SynapseException{
		try {
			String url = "/entity/" + entityId+"/annotations";
			JSONObject jsonObject = EntityFactory.createJSONObjectForEntity(updated);
			// Update
			jsonObject = putEntity(url, jsonObject);
			// Parse the results
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObject);
			Annotations annos = new Annotations();
			annos.initializeFromJSONObject(adapter);
			return annos;
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}

	/**
	 * Get an entity given an Entity ID and the class of the Entity.
	 * 
	 * @param <T>
	 * @param entityId
	 * @param clazz
	 * @return the entity
	 * @throws SynapseException
	 */
	@SuppressWarnings("cast")
	public <T extends JSONEntity> T getEntity(String entityId,
			Class<? extends T> clazz) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		if (clazz == null)
			throw new IllegalArgumentException("Entity class cannot be null");
		EntityType type = EntityType.getNodeTypeForClass(clazz);
		// Build the URI
		String uri = createEntityUri(type.getUrlPrefix(), entityId);
		JSONObject jsonObj = getEntity(uri);
		// Now convert to Object to an entity
		try {
			return (T) EntityFactory.createEntityFromJSONObject(jsonObj, clazz);
		} catch (JSONObjectAdapterException e) {
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
	protected static String createEntityUri(String prefix, String id) {
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
	 * @throws SynapseException
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
	 * @throws SynapseException
	 */
	@SuppressWarnings("unchecked")
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
	 * @throws SynapseException
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
	 * @throws SynapseException
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
	 * Get the hierarchical path to this entity
	 * @param entity
	 * @return
	 * @throws SynapseException 
	 */
	public EntityPath getEntityPath(Entity entity) throws SynapseException {
		return getEntityPath(entity.getId());
	}
	
	/**
	 * Get the hierarchical path to this entity via its id and urlPrefix 
	 * @param entityId
	 * @param urlPrefix
	 * @return
	 * @throws SynapseException
	 */
	public EntityPath getEntityPath(String entityId) throws SynapseException {
		String url = "/entity/" + entityId+"/path";
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		EntityPath path = new EntityPath();
		try {
			path.initializeFromJSONObject(adapter);
			return path;
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}	

	public BatchResults<EntityHeader> getEntityTypeBatch(List<String> entityIds) throws SynapseException {
		String url = "/entity/type"; // TODO move UrlHelpers someplace shared so that we can UrlHelpers.ENTITY_TYPE
		url += "?" + ServiceConstants.BATCH_PARAM + "=" + StringUtils.join(entityIds, ServiceConstants.BATCH_PARAM_VALUE_SEPARATOR);
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		BatchResults<EntityHeader> results = new BatchResults<EntityHeader>(EntityHeader.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}	
	
	/**
	 * Get the hierarchical path to this entity
	 * @param entity
	 * @return
	 * @throws SynapseException 
	 */
	public PaginatedResults<EntityHeader> getEntityReferencedBy(Entity entity) throws SynapseException {
		String version = null;
		if(entity instanceof Versionable) {
			version = ((Versionable)entity).getVersionNumber().toString();
		}
		return getEntityReferencedBy(entity.getId(), version);
	}
	
	/**
	 * Get the hierarchical path to this entity via its id and urlPrefix 
	 * @param entityId
	 * @param urlPrefix
	 * @return
	 * @throws SynapseException
	 */
	public PaginatedResults<EntityHeader> getEntityReferencedBy(String entityId, String targetVersion) throws SynapseException {
		String url = "/entity/" + entityId;
		if(targetVersion != null) {
			url += "/version/" + targetVersion;
		}
		url += "/referencedby";
		
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<EntityHeader> results = new PaginatedResults<EntityHeader>(EntityHeader.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}	
	
	/**
	 * Perform a query
	 * 
	 * @param query
	 *            the query to perform
	 * @return the query result
	 * @throws SynapseException
	 */
	public JSONObject query(String query) throws SynapseException {
		return querySynapse(repoEndpoint, query);
	}

	/**
	 * Download the locationable to a tempfile
	 * 
	 * @param locationable
	 * @return destination file
	 * @throws SynapseException
	 * @throws SynapseUserException
	 */
	public File downloadLocationableFromSynapse(Locationable locationable)
			throws SynapseException {
		// TODO do the equivalent of the R client synapse cache and file naming
		// scheme
		File file;
		try {
			// from the Java doc
			// prefix - The prefix string to be used in generating the file's name; must be at least three characters long
			String prefix = locationable.getId();
			if (prefix.length()<3) {
				prefix = "000".substring(prefix.length()) + prefix;
			}
			file = File.createTempFile(prefix, ".txt");
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
			throw new SynapseUserException(
					"No locations available for locationable " + locationable);
		}

		// TODO if there are multiple locations for this locationable look in
		// user
		// preferences to download from the appropriate location (e.g., Sage
		// Internal versus S3 versus GoogleStorage). For now we are just
		// downloading from the first location
		LocationData location = locations.get(0);
		return downloadFromSynapse(location, locationable.getMd5(),
				destinationFile);
	}

	/**
	 * Download data from Synapse to the specified destination file given a
	 * specific location from which to download
	 * 
	 * @param location
	 * @param md5
	 * @param destinationFile
	 * @return destination file
	 * @throws SynapseException
	 */
	public File downloadFromSynapse(LocationData location, String md5,
			File destinationFile) throws SynapseException {
		return downloadFromSynapse(location.getPath(), md5, destinationFile);
	}
	
	public File downloadFromSynapse(String path, String md5,
				File destinationFile) throws SynapseException {
		try {
			clientProvider.downloadFile(path, destinationFile.getAbsolutePath());
			// Check that the md5s match, if applicable
			if (null != md5) {
				String localMd5 = MD5ChecksumHelper
						.getMD5Checksum(destinationFile.getAbsolutePath());
				if (!localMd5.equals(md5)) {
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
	 * @param locationable
	 * @param dataFile
	 * 
	 * @return the updated locationable
	 * @throws SynapseException
	 */
	public Locationable uploadLocationableToSynapse(Locationable locationable,
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
	 * Dev Note: this implementation allows only one location per Locationable,
	 * ultimately we plan to support multiple locations (e.g., a copy in
	 * GoogleStorage, S3, and on a local server), but we'll save that work for
	 * later
	 * 
	 * @param locationable
	 * @param dataFile
	 * @param md5
	 * @return the updated locationable
	 * @throws SynapseException
	 */
	public Locationable uploadLocationableToSynapse(Locationable locationable,
			File dataFile, String md5) throws SynapseException {

		// Step 1: get the token
		S3Token s3Token = new S3Token();
		s3Token.setPath(dataFile.getName());
		s3Token.setMd5(md5);
		s3Token = createEntity(locationable.getS3Token(), s3Token);

		// Step 2: perform the upload
		dataUploader.uploadDataMultiPart(s3Token, dataFile);

		// Step 3: set the upload location in the locationable so that Synapse
		// is aware of the new data
		LocationData location = new LocationData();
		location.setPath(s3Token.getPath());
		location.setType(LocationTypeNames.awss3);

		List<LocationData> locations = new ArrayList<LocationData>();
		locations.add(location);

		locationable.setContentType(s3Token.getContentType());
		locationable.setMd5(s3Token.getMd5());
		locationable.setLocations(locations);
		
		return putEntity(locationable);
	}
	
	/**
	 * Upload an attachment to Synapse.
	 * @param entityId
	 * @param dataFile
	 * @param md5
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 * @throws IOException 
	 */
	public AttachmentData uploadAttachmentToSynapse(String entityId, File dataFile) throws JSONObjectAdapterException, SynapseException, IOException{
		// First we need to get an S3 token
		S3AttachmentToken token = new S3AttachmentToken();
		token.setFileName(dataFile.getName());
		String md5 = MD5ChecksumHelper.getMD5Checksum(dataFile
				.getAbsolutePath());
		token.setMd5(md5);
		// Create the token
		token = createAtachmentS3Token(entityId, token);
		// Upload the file
		dataUploader.uploadDataSingle(token, dataFile);
		// We are now done
		AttachmentData newData = new AttachmentData();
		newData.setContentType(token.getContentType());
		newData.setName(dataFile.getName());
		newData.setTokenId(token.getTokenId());
		newData.setMd5(token.getMd5());
		return newData;
	}
	
	/**
	 * Get the presigned URL for an entity attachment.
	 * @param entityId
	 * @param newData
	 * @return
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public PresignedUrl getAttachmentPresignedUrl(String entityId, AttachmentData newData) throws SynapseException, JSONObjectAdapterException{
		String url = ENTITY+"/"+entityId+ATTACHMENT_URL+"/"+newData.getTokenId();
		JSONObject json = getEntity(url);
		return EntityFactory.createEntityFromJSONObject(json, PresignedUrl.class);
	}
	
	/**
	 * Download an entity attachment to a local file
	 * @param entityId
	 * @param attachmentData
	 * @param destFile
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public void downlaodEntityAttachment(String entityId, AttachmentData attachmentData, File destFile) throws SynapseException, JSONObjectAdapterException{
		// First get the URL
		String url = null;
		if(attachmentData.getTokenId() != null){
			// Use the token to get the file
			PresignedUrl preUrl = getAttachmentPresignedUrl(entityId, attachmentData);
			url = preUrl.getPresignedUrl();
		}else{
			// Just download the file.
			url = attachmentData.getUrl();
		}
		//Now download the file
		downloadFromSynapse(url, null, destFile);
	}
	
	/**
	 * Create an Attachment s3 token.
	 * @param entityId
	 * @param token
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException 
	 */
	public S3AttachmentToken createAtachmentS3Token(String entityId, S3AttachmentToken token) throws JSONObjectAdapterException, SynapseException{
		if(entityId == null) throw new IllegalArgumentException("EntityId cannot be null");
		if(token == null) throw new IllegalArgumentException("S3AttachmentToken cannot be null");
		JSONObject jsonObject = EntityFactory.createJSONObjectForEntity(token);
		String uri = ENTITY+"/"+entityId+ATTACHMENT_S3_TOKEN;
		jsonObject = createEntity(uri, jsonObject);
		return EntityFactory.createEntityFromJSONObject(jsonObject, S3AttachmentToken.class);
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

		return signAndDispatchSynapseRequest(endpoint, uri, "POST", entity.toString(),
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

		return signAndDispatchSynapseRequest(endpoint, uri, "GET", null,
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
						storedAnnotations.put(annotationKey, entityAnnotations
								.get(annotationKey));
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
			requestHeaders.put("ETag", entity.getString("etag"));

			return putSynapseEntity(endpoint, uri, entity, requestHeaders);
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
			JSONObject entity, Map<String,String> headers) throws SynapseException {
			if (null == endpoint) {
				throw new IllegalArgumentException("must provide endpoint");
			}
			if (null == uri) {
				throw new IllegalArgumentException("must provide uri");
			}
			if (null == entity) {
				throw new IllegalArgumentException("must provide entity");
			}
			if (null == headers) {
				throw new IllegalArgumentException("must provide headers");
			}
				

			Map<String, String> requestHeaders = new HashMap<String, String>(headers);
			requestHeaders.putAll(defaultPOSTPUTHeaders);
			return signAndDispatchSynapseRequest(endpoint, uri, "PUT", 
					entity.toString(), requestHeaders);
	}

	/**
	 * Delete a dataset, layer, etc..
	 * 
	 * @param endpoint
	 * @param uri
	 * @throws SynapseException
	 */
	public void deleteSynapseEntity(String endpoint, String uri)
			throws SynapseException {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}

		signAndDispatchSynapseRequest(endpoint, uri, "DELETE", null,
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
	 * @throws SynapseException
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

			return signAndDispatchSynapseRequest(endpoint, queryUri, "GET", null,
					requestHeaders);
		} catch (UnsupportedEncodingException e) {
			throw new SynapseException(e);
		}
	}
	
	protected JSONObject signAndDispatchSynapseRequest(String endpoint, String uri,
			String requestMethod, String requestContent,
			Map<String, String> requestHeaders) throws SynapseException {
		if (apiKey!=null) {
			String timeStamp = (new DateTime()).toString();
			String uriRawPath = null; 
			try {
				uriRawPath = (new URI(endpoint+uri)).getRawPath(); // chop off the query, if any
			} catch (URISyntaxException e) {
				throw new SynapseException(e);
			}
		    String signature = HMACUtils.generateHMACSHA1Signature(userName, uriRawPath, timeStamp, apiKey);
		    Map<String, String> modHeaders = new HashMap<String, String>(requestHeaders);
		    modHeaders.put(AuthorizationConstants.USER_ID_HEADER, userName);
		    modHeaders.put(AuthorizationConstants.SIGNATURE_TIMESTAMP, timeStamp);
		    modHeaders.put(AuthorizationConstants.SIGNATURE, signature);
		    return dispatchSynapseRequest(endpoint, uri, requestMethod, requestContent, modHeaders);
		} 
		return dispatchSynapseRequest(endpoint, uri, requestMethod, requestContent, requestHeaders);
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
	protected JSONObject dispatchSynapseRequest(String endpoint, String uri,
			String requestMethod, String requestContent,
			Map<String, String> requestHeaders) throws SynapseException {

		if (requestProfile && !requestMethod.equals("DELETE")) {
			requestHeaders.put(REQUEST_PROFILE_DATA, "true");
		} else {
			if (requestHeaders.containsKey(REQUEST_PROFILE_DATA))
				requestHeaders.remove(REQUEST_PROFILE_DATA);
		}
		
		// remove session tken if it is null
		if(requestHeaders.containsKey(SESSION_TOKEN_HEADER) && requestHeaders.get(SESSION_TOKEN_HEADER) == null) {
			requestHeaders.remove(SESSION_TOKEN_HEADER);
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
				try {
					results = new JSONObject(responseBody);
				} catch (JSONException jsone) {
					throw new SynapseServiceException("responseBody: <<"+responseBody+">>", jsone);
				}
				if (log.isDebugEnabled()) {
					if(authEndpoint.equals(endpoint)) {
						log.debug(requestMethod + " " + requestUrl + " : (not logging auth request details)");
					}
					else {
						log.debug(requestMethod + " " + requestUrl + " : "
								+ results.toString(JSON_INDENT));
					}
				}
			}

		} catch (HttpClientHelperException e) {
			// Well-handled server side exceptions come back as JSON, attempt to
			// deserialize and convert the error
			int statusCode = 500; // assume a service exception
			statusCode = e.getHttpStatus();
			String response = "";
			String resultsStr = "";
			try {
				response = e.getResponse();
				if (null != response && response.length()>0) {
					try {
						results = new JSONObject(response);
					} catch (JSONException jsone) {
						throw new SynapseServiceException("Failed to parse: "+response, jsone);
					}
					if (log.isDebugEnabled()) {
						log.debug("Retrieved " + requestUrl + " : "
								+ results.toString(JSON_INDENT));
					}
					if (results != null)
						resultsStr = results.getString("reason");
				}
				String exceptionContent = "Service Error(" + statusCode + "): "
						+ resultsStr + " " + e.getMessage();

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
					throw new SynapseServiceException("request content: "+requestContent+" exception content: "+exceptionContent);
				}
			} catch (JSONException jsonEx) {
				// swallow the JSONException since its not the real problem and
				// return the response as-is since it is not JSON
				throw new SynapseServiceException(jsonEx);
			} catch (ParseException parseEx) {
				throw new SynapseServiceException(parseEx);
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

	/**
	 * @return status
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public StackStatus getCurrentStackStatus() throws SynapseException,
			JSONObjectAdapterException {
		JSONObject json = getEntity(STACK_STATUS);
		return EntityFactory
				.createEntityFromJSONObject(json, StackStatus.class);
	}
	
	/**
	 * Get a dataset, layer, preview, annotations, etc...
	 * 
	 * @param <T>
	 * 
	 * @param uri
	 * @param clazz
	 * @return the retrieved entity
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public <T extends JSONEntity> T getJSONEntity(String uri,
			Class<? extends T> clazz) throws SynapseException,
			JSONObjectAdapterException {
		JSONObject jsonObject = getEntity(uri);
		return EntityFactory.createEntityFromJSONObject(jsonObject, clazz);
	}
	
	public SearchResults search(SearchQuery searchQuery) throws SynapseException, UnsupportedEncodingException, JSONObjectAdapterException {
		SearchResults searchResults = null;		
		String uri = "/search?q=" + URLEncoder.encode(SearchUtil.generateQueryString(searchQuery), "UTF8");
		JSONObject obj = signAndDispatchSynapseRequest(repoEndpoint, uri, "GET", null, defaultGETDELETEHeaders);
		if(obj != null) {
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(obj);
			searchResults = new SearchResults(adapter);
		}

		return searchResults;
	}
	
	
	public String getSynapseTermsOfUse() throws SynapseException {
		try {
			HttpResponse response = clientProvider.performRequest(authEndpoint+"/termsOfUse.html",
					"GET", null, null);
			InputStream is = response.getEntity().getContent();
			StringBuilder sb = new StringBuilder();
			int i = is.read();
			while (i>0) {
				sb.append((char)i);
				i = is.read();
			}
			is.close();
			return sb.toString();
		} catch (IOException e) {
			throw new SynapseException(e);
		} catch (HttpClientHelperException e) {
			throw new SynapseException(e);
		}
	}
}
