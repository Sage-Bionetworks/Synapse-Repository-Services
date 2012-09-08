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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.sagebionetworks.client.exceptions.SynapseTermsOfUseException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AutoGenFactory;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.S3Token;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.ServiceConstants.AttachmentType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.VariableContentPaginatedResults;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.attachment.URLStatus;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.versionInfo.VersionInfo;
import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
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
	protected static final String DEFAULT_REPO_ENDPOINT = "https://repo-prod.sagebase.org/repo/v1";
	protected static final String DEFAULT_AUTH_ENDPOINT = "https://auth-prod.sagebase.org/auth/v1";
	protected static final String SESSION_TOKEN_HEADER = "sessionToken";
	protected static final String REQUEST_PROFILE_DATA = "profile_request";
	protected static final String PROFILE_RESPONSE_OBJECT_HEADER = "profile_response_object";

	protected static final String PASSWORD_FIELD = "password";
	
	protected static final String QUERY_URI = "/query?query=";
	protected static final String REPO_SUFFIX_PATH = "/path";	
	protected static final String REPO_SUFFIX_VERSION = "/version";
	protected static final String ANNOTATION_URI_SUFFIX = "annotations";
	protected static final String ADMIN = "/admin";
	protected static final String STACK_STATUS = ADMIN + "/synapse/status";
	protected static final String ENTITY = "/entity";
	protected static final String ATTACHMENT_S3_TOKEN = "/s3AttachmentToken";
	protected static final String ATTACHMENT_URL = "/attachmentUrl";

	protected static final String ENTITY_URI_PATH = "/entity";
	protected static final String ENTITY_ACL_PATH_SUFFIX = "/acl";
	protected static final String ENTITY_ACL_RECURSIVE_SUFFIX = "?recursive=true";
	protected static final String ENTITY_BUNDLE_PATH = "/bundle?mask=";
	protected static final String BENEFACTOR = "/benefactor"; // from org.sagebionetworks.repo.web.UrlHelpers

	protected static final String USER_PROFILE_PATH = "/userProfile";

	protected static final String TOTAL_NUM_RESULTS = "totalNumberOfResults";
	
	protected static final String ACCESS_REQUIREMENT = "/accessRequirement";
	
	protected static final String ACCESS_REQUIREMENT_UNFULFILLED = "/accessRequirementUnfulfilled";
	
	protected static final String ACCESS_APPROVAL = "/accessApproval";
	
	protected static final String VERSION_INFO = "/version";
	
	// web request pagination parameters
	protected static final String LIMIT = "limit";
	protected static final String OFFSET = "offset";

	protected static final String LIMIT_1_OFFSET_1 = "' limit 1 offset 1";
	protected static final String SELECT_ID_FROM_ENTITY_WHERE_PARENT_ID = "select id from entity where parentId == '";

	
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
	 * Get the configured Repository Service Endpoint
	 * @return
	 */
	public String getRepoEndpoint() {
		return repoEndpoint;
	}

	/**
	 * @param authEndpoint
	 *            the authEndpoint to set
	 */
	public void setAuthEndpoint(String authEndpoint) {
		this.authEndpoint = authEndpoint;
	}

	/**
	 * Get the configured Authorization Service Endpoint
	 * @return
	 */
	public String getAuthEndpoint() {
		return authEndpoint;
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
	public UserSessionData login(String username, String password) throws SynapseException {
		/**
		 * Log into Synapse
		 * 
		 * @param username
		 * @param password
		 * @throws SynapseException
		 */
		return login(username, password, false);
	}
		
	public UserSessionData login(String username, String password, boolean explicitlyAcceptsTermsOfUse) throws SynapseException {
		UserSessionData userData = null;
			JSONObject loginRequest = new JSONObject();
		JSONObject credentials = null;
		try {
			loginRequest.put("email", username);
			loginRequest.put(PASSWORD_FIELD, password);
			if (explicitlyAcceptsTermsOfUse) loginRequest.put("acceptsTermsOfUse", true);
			
			boolean reqPr = requestProfile;
			requestProfile = false;

			try {
				credentials = createAuthEntity("/session", loginRequest);
				String sessionToken = credentials.getString(SESSION_TOKEN_HEADER);
				defaultGETDELETEHeaders.put(SESSION_TOKEN_HEADER, sessionToken);
				defaultPOSTPUTHeaders.put(SESSION_TOKEN_HEADER, sessionToken);
				requestProfile = reqPr;

				UserProfile profile = getMyProfile();
				userData = new UserSessionData();
				userData.setIsSSO(false);
				userData.setSessionToken(sessionToken);
				userData.setProfile(profile);
			} catch (SynapseForbiddenException e) {
				//403 error
				throw new SynapseTermsOfUseException(e.getMessage());
			}
		} catch (JSONException e) {
			throw new SynapseException(e);
		}
		return userData;
	}

	public UserSessionData getUserSessionData() throws SynapseException {
		//get the UserSessionData if the session token is set
		UserSessionData userData = null;
		String sessionToken = getCurrentSessionToken();
		UserProfile profile = getMyProfile();
		userData = new UserSessionData();
		userData.setIsSSO(false);
		userData.setSessionToken(sessionToken);
		userData.setProfile(profile);
		return userData;
	}
	
	public boolean revalidateSession() throws SynapseException {
		JSONObject sessionInfo = new JSONObject();
		try {
			sessionInfo.put("sessionToken", getCurrentSessionToken());
			try {
				putAuthEntity("/session", sessionInfo);
			} catch (SynapseForbiddenException e) {
				//403 error
				throw new SynapseTermsOfUseException(e.getMessage());
			}
			
		} catch (JSONException e) {
			throw new SynapseException(e);
		}
		return true;
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
	public <T extends Entity> T createEntity(T entity)
			throws SynapseException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");
		entity.setEntityType(entity.getClass().getName());
		// Look up the EntityType for this entity.		
		return createEntity(ENTITY_URI_PATH, entity);
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
	 * Get an entity using its ID.
	 * @param entityId
	 * @return the entity
	 * @throws SynapseException
	 */
	public Entity getEntityById(String entityId) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		return getEntityByIdForVersion(entityId, null);
	}
		
	/**
	 * Get a specific version of an entity using its ID an version number.
	 * @param entityId
	 * @param versionNumber 
	 * @return the entity
	 * @throws SynapseException
	 */
	public Entity getEntityByIdForVersion(String entityId, Long versionNumber) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		String url = ENTITY_URI_PATH + "/" + entityId;
		if(versionNumber != null) {
			url += REPO_SUFFIX_VERSION + "/" + versionNumber;
		}
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
	 * Get a bundle of information about an entity in a single call.
	 * 
	 * @param entityId
	 * @param partsMask
	 * @return
	 * @throws SynapseException 
	 */
	public EntityBundle getEntityBundle(String entityId, int partsMask) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		String url = ENTITY_URI_PATH + "/" + entityId + ENTITY_BUNDLE_PATH + partsMask;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			EntityBundle eb = new EntityBundle();
			eb.initializeFromJSONObject(adapter);
			return eb;
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}
	
	/**
	 * Get a bundle of information about an entity in a single call.
	 * 
	 * @param entityId - the entity id to retrieve
	 * @param versionNumber - the specific version to retrieve
	 * @param partsMask
	 * @return
	 * @throws SynapseException 
	 */
	public EntityBundle getEntityBundle(String entityId, Long versionNumber, int partsMask) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		if (versionNumber == null)
			throw new IllegalArgumentException("versionNumber cannot be null");
		String url = ENTITY_URI_PATH + "/" + entityId + REPO_SUFFIX_VERSION + "/" + versionNumber + ENTITY_BUNDLE_PATH + partsMask;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			EntityBundle eb = new EntityBundle();
			eb.initializeFromJSONObject(adapter);
			return eb;
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}
	
	/**
	 * 
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	public PaginatedResults<EntityHeader> getEntityVersions(String entityId) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		String url = ENTITY_URI_PATH + "/" + entityId + REPO_SUFFIX_VERSION;				
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter results = new JSONObjectAdapterImpl(jsonObj);		
		try {			
			// TODO : transfer to a paginated list of entityheader. this code can go away with above service change
			List<EntityHeader> headerList = new ArrayList<EntityHeader>();
			if(results.has("results")) {
				JSONArrayAdapter list = results.getJSONArray("results");
				for(int i=0; i<list.length(); i++) {
					JSONObjectAdapter entity = list.getJSONObject(i);
					EntityHeader header = new EntityHeader();
					header.setId(entity.getString("id"));
					header.setName(entity.getString("name"));
					header.setType(entity.getString("entityType"));
					if(entity.has("versionNumber")) {
						header.setVersionNumber(entity.getLong("versionNumber"));
						header.setVersionLabel(entity.getString("versionLabel"));
					} else {
						header.setVersionNumber(new Long(1));
						header.setVersionLabel("1");						
					}
					headerList.add(header);
				}			
			}

			PaginatedResults<EntityHeader> versions = new PaginatedResults<EntityHeader>(EntityHeader.class);
			versions.setTotalNumberOfResults(results.getInt("totalNumberOfResults"));			
			versions.setResults(headerList);
			return versions;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public static <T extends JSONEntity> T initializeFromJSONObject(JSONObject o, Class<T> clazz) throws SynapseException {
		try {
			T obj = clazz.newInstance();
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(o);
			Iterator<String> it = adapter.keys();
			while (it.hasNext()) {
				String s = it.next();
				log.trace(s);
			}
			obj.initializeFromJSONObject(adapter);
			return obj;
		} catch (IllegalAccessException e) {
			throw new SynapseException(e);
		} catch (InstantiationException e) {
			throw new SynapseException(e);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
		
	public AccessControlList getACL(String entityId) throws SynapseException {
		String uri = ENTITY_URI_PATH + "/" + entityId+ ENTITY_ACL_PATH_SUFFIX;
		JSONObject json = getEntity(uri);
		return initializeFromJSONObject(json, AccessControlList.class);
	}
	
	public EntityHeader getEntityBenefactor(String entityId) throws SynapseException {
		String uri = ENTITY_URI_PATH + "/" + entityId+ BENEFACTOR;
		JSONObject json = getEntity(uri);
		return initializeFromJSONObject(json, EntityHeader.class);
	}
	
	public UserProfile getMyProfile() throws SynapseException {
		String uri = USER_PROFILE_PATH;
		JSONObject json = getEntity(uri);
		return initializeFromJSONObject(json, UserProfile.class);
	}
	
	public void updateMyProfile(UserProfile userProfile) throws SynapseException {
		try {
			String uri = USER_PROFILE_PATH;
			putEntity(uri, EntityFactory.createJSONObjectForEntity(userProfile));
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public UserProfile getUserProfile(String ownerId) throws SynapseException {
		String uri = USER_PROFILE_PATH + "/" + ownerId;
		JSONObject json = getEntity(uri);
		return initializeFromJSONObject(json, UserProfile.class);
	}
	
	/**
	 * Update an ACL. Default to non-recursive application.
	 */
	public AccessControlList updateACL(AccessControlList acl) throws SynapseException {
		return updateACL(acl, false);
	}
	
	/**
	 * Update an entity's ACL. If 'recursive' is set to true, then any child 
	 * ACLs will be deleted, such that all child entities inherit this ACL. 
	 */
	public AccessControlList updateACL(AccessControlList acl, boolean recursive) throws SynapseException {
		String entityId = acl.getId();
		String uri = ENTITY_URI_PATH + "/" + entityId+ ENTITY_ACL_PATH_SUFFIX;
		if (recursive)
			uri += ENTITY_ACL_RECURSIVE_SUFFIX;
		try {
			JSONObject jsonAcl = EntityFactory.createJSONObjectForEntity(acl);
			jsonAcl = putEntity(uri, jsonAcl);
			return initializeFromJSONObject(jsonAcl, AccessControlList.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public void deleteACL(String entityId) throws SynapseException {
		String uri = ENTITY_URI_PATH + "/" + entityId+ ENTITY_ACL_PATH_SUFFIX;
		deleteEntity(uri);
	}
	
	public AccessControlList createACL(AccessControlList acl) throws SynapseException {
		String entityId = acl.getId();
		String uri = ENTITY_URI_PATH + "/" + entityId+ ENTITY_ACL_PATH_SUFFIX;
		try {
			JSONObject jsonAcl = EntityFactory.createJSONObjectForEntity(acl);
			jsonAcl = createEntity(uri, jsonAcl);
			return initializeFromJSONObject(jsonAcl, AccessControlList.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public PaginatedResults<UserProfile> getUsers(int offset, int limit) throws SynapseException {
		String uri = "/user?"+OFFSET+"="+offset+"&limit="+limit;
		JSONObject jsonUsers = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonUsers);
		PaginatedResults<UserProfile> results = new PaginatedResults<UserProfile>(UserProfile.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}


	public PaginatedResults<UserGroup> getGroups(int offset, int limit) throws SynapseException {
		String uri = "/userGroup?"+OFFSET+"="+offset+"&limit="+limit;
		JSONObject jsonUsers = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonUsers);
		PaginatedResults<UserGroup> results = new PaginatedResults<UserGroup>(UserGroup.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	/**
	 * Get the current user's permission for a given entity.
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	public UserEntityPermissions getUsersEntityPermissions(String entityId) throws SynapseException{
		String url = ENTITY_URI_PATH + "/" + entityId+"/permissions";
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		UserEntityPermissions uep = new UserEntityPermissions();
		try {
			uep.initializeFromJSONObject(adapter);
			return uep;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	/**
	 * Get the current user's permission for a given entity.
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	public boolean canAccess(String entityId, ACCESS_TYPE accessType) throws SynapseException {
		try {
			String url = ENTITY_URI_PATH + "/" + entityId+ "/access?accessType="+accessType.name();
			JSONObject jsonObj = getEntity(url);
			String resultString = null;
			try {
				resultString = jsonObj.getString("result");
			} catch (NullPointerException e) {
				throw new SynapseException(jsonObj.toString(), e);
			}
			return Boolean.parseBoolean(resultString);
		} catch (JSONException e) {
			throw new SynapseException(e);
		}
	}
	
	/**
	 * Get the annotations for an entity.
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	public Annotations getAnnotations(String entityId) throws SynapseException{
		String url = ENTITY_URI_PATH + "/" + entityId+"/annotations";
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
	 * Update the annotations of an entity.
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	public Annotations updateAnnotations(String entityId, Annotations updated) throws SynapseException{
		try {
			String url = ENTITY_URI_PATH + "/" + entityId+"/annotations";
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
	
	private static Class<AccessRequirement> getAccessRequirementClassFromType(String s) {
		try {
			return (Class<AccessRequirement>)Class.forName(s);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public <T extends AccessRequirement> T createAccessRequirement(T ar) throws SynapseException {
	
		if (ar==null) throw new IllegalArgumentException("AccessRequirement cannot be null");
		ar.setEntityType(ar.getClass().getName());
		// Get the json for this entity
		JSONObject jsonObject;
		try {
			jsonObject = EntityFactory.createJSONObjectForEntity(ar);
			// Create the entity
			jsonObject = createEntity(ACCESS_REQUIREMENT, jsonObject);
			// Now convert to Object to an entity
			return (T)initializeFromJSONObject(jsonObject, getAccessRequirementClassFromType(ar.getEntityType()));
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
		
	}

	public VariableContentPaginatedResults<AccessRequirement> getUnmetAccessReqAccessRequirements(String entityId) throws SynapseException {
		String uri = ENTITY+"/"+entityId+ACCESS_REQUIREMENT_UNFULFILLED;
		JSONObject jsonAccessRequirements = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonAccessRequirements);
		VariableContentPaginatedResults<AccessRequirement> results = new VariableContentPaginatedResults<AccessRequirement>();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	public VariableContentPaginatedResults<AccessRequirement> getAccessRequirements(String entityId) throws SynapseException {
		String uri = ENTITY+"/"+entityId+ACCESS_REQUIREMENT;
		JSONObject jsonAccessRequirements = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonAccessRequirements);
		VariableContentPaginatedResults<AccessRequirement> results = new VariableContentPaginatedResults<AccessRequirement>();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	private static Class<AccessApproval> getAccessApprovalClassFromType(String s) {
		try {
			return (Class<AccessApproval>)Class.forName(s);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public <T extends AccessApproval> T createAccessApproval(T aa) throws SynapseException {
		
		if (aa==null) throw new IllegalArgumentException("AccessApproval cannot be null");		
		aa.setEntityType(aa.getClass().getName());
		// Get the json for this entity
		JSONObject jsonObject;
		try {
			jsonObject = EntityFactory.createJSONObjectForEntity(aa);
			// Create the entity
			jsonObject = createEntity(ACCESS_APPROVAL, jsonObject);
			// Now convert to Object to an entity
			return (T)initializeFromJSONObject(jsonObject, getAccessApprovalClassFromType(aa.getEntityType()));
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
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
		// Build the URI
		String uri = createEntityUri(ENTITY_URI_PATH, entityId);
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
			String uri = createEntityUri(ENTITY_URI_PATH, entity.getId());
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
		String uri = createEntityUri(ENTITY_URI_PATH, entity.getId());
		deleteEntity(uri);
	}

	/**
	 * Delete a dataset, layer, etc..
	 * 
	 * @param <T>
	 * @param entity
	 * @throws SynapseException
	 */
	public void deleteEntityById(String entityId)
			throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("entityId cannot be null");
		String uri = createEntityUri(ENTITY_URI_PATH, entityId);
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
		String url = ENTITY_URI_PATH + "/" + entityId+"/path";
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
		String url = ENTITY_URI_PATH + "/type"; // TODO move UrlHelpers someplace shared so that we can UrlHelpers.ENTITY_TYPE
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
		// By default we want to find anything that references any version of this entity.
		String version = null;
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
		String url = ENTITY_URI_PATH + "/" + entityId;
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
	 * Update the locationable to point to the given external url
	 * @param locationable
	 * @param externalUrl
	 * @return the updated locationable
	 * @throws SynapseException
	 */
	public Locationable updateExternalLocationableToSynapse(Locationable locationable,
			String externalUrl) throws SynapseException {
		return updateExternalLocationableToSynapse(locationable, externalUrl, null);
	}
	
	/**
	 * Update the locationable to point to the given external url
	 * @param locationable
	 * @param externalUrl
	 * @param md5 the calculated md5 checksum for the file referenced by the external url
	 * @return the updated locationable
	 * @throws SynapseException
	 */
	public Locationable updateExternalLocationableToSynapse(Locationable locationable,
			String externalUrl, String md5) throws SynapseException {
		// set the upload location in the locationable so that Synapse
		// is aware of the new data
		LocationData location = new LocationData();
		location.setPath(externalUrl);
		location.setType(LocationTypeNames.external);
		locationable.setMd5(md5);
		List<LocationData> locations = new ArrayList<LocationData>();
		locations.add(location);
		locationable.setLocations(locations);
		return putEntity(locationable);
	}
	
	/**
	 * This version will use the file name for the file name.
	 * @param entityId
	 * @param dataFile
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 * @throws IOException
	 */
	public AttachmentData uploadAttachmentToSynapse(String entityId, File dataFile) throws JSONObjectAdapterException, SynapseException, IOException{
		return uploadAttachmentToSynapse(entityId, dataFile, dataFile.getName());
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
	public AttachmentData uploadAttachmentToSynapse(String entityId, File dataFile, String fileName) throws JSONObjectAdapterException, SynapseException, IOException{
		return uploadAttachmentToSynapse(entityId, AttachmentType.ENTITY, dataFile, fileName);
	}
	
	/**
	 * Upload a user profile attachment to Synapse.
	 * @param userId
	 * @param dataFile
	 * @param md5
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 * @throws IOException 
	 */	
	public AttachmentData uploadUserProfileAttachmentToSynapse(String userId, File dataFile, String fileName) throws JSONObjectAdapterException, SynapseException, IOException{
		return uploadAttachmentToSynapse(userId, AttachmentType.USER_PROFILE, dataFile, fileName);
	}
	
	/**
	 * Upload an attachment to Synapse.
	 * @param attachmentType
	 * @param userId
	 * @param dataFile
	 * @param md5
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 * @throws IOException 
	 */	
	public AttachmentData uploadAttachmentToSynapse(String id, AttachmentType attachmentType, File dataFile, String fileName) throws JSONObjectAdapterException, SynapseException, IOException{
		// First we need to get an S3 token
		S3AttachmentToken token = new S3AttachmentToken();
		token.setFileName(fileName);
		String md5 = MD5ChecksumHelper.getMD5Checksum(dataFile
				.getAbsolutePath());
		token.setMd5(md5);
		// Create the token
		token = createAttachmentS3Token(id, attachmentType, token);
		// Upload the file
		dataUploader.uploadDataSingle(token, dataFile);
		// We are now done
		AttachmentData newData = new AttachmentData();
		newData.setContentType(token.getContentType());
		newData.setName(fileName);
		newData.setTokenId(token.getTokenId());
		newData.setMd5(token.getMd5());
		return newData;
	}
	
	/**
	 * Get the presigned URL for an entity attachment.
	 * @param entityId
	 * @param attachment data type
	 * @param newData
	 * @return
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public PresignedUrl createAttachmentPresignedUrl(String id, String tokenOrPreviewId) throws SynapseException, JSONObjectAdapterException{
		return createAttachmentPresignedUrl(id, AttachmentType.ENTITY, tokenOrPreviewId);
	}
	/**
	 * Get the presigned URL for a user profile attachment.
	 * @param userId
	 * @param attachment data type
	 * @param newData
	 * @return
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public PresignedUrl createUserProfileAttachmentPresignedUrl(String id, String tokenOrPreviewId) throws SynapseException, JSONObjectAdapterException{
		return createAttachmentPresignedUrl(id, AttachmentType.USER_PROFILE, tokenOrPreviewId);
	}
	
	/**
	 * Get the presigned URL for an attachment.
	 * @param entityId
	 * @param attachment data type
	 * @param newData
	 * @return
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public PresignedUrl createAttachmentPresignedUrl(String id, AttachmentType attachmentType, String tokenOrPreviewId) throws SynapseException, JSONObjectAdapterException{
		String url = getAttachmentTypeURL(attachmentType)+"/"+id+ATTACHMENT_URL;
		PresignedUrl preIn = new PresignedUrl();
		preIn.setTokenID(tokenOrPreviewId);
		JSONObject jsonBody = EntityFactory.createJSONObjectForEntity(preIn);
		JSONObject json = createEntity(url, jsonBody);
		return EntityFactory.createEntityFromJSONObject(json, PresignedUrl.class);
	}
	
	
	
	
	/**
	 * Wait for the given preview to be created.
	 * @param entityId
	 * @param tokenOrPreviewId
	 * @param timeout
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public PresignedUrl waitForPreviewToBeCreated(String entityId, String tokenOrPreviewId, int timeout) throws SynapseException, JSONObjectAdapterException{
		return waitForPreviewToBeCreated(entityId, AttachmentType.ENTITY, tokenOrPreviewId, timeout);
	}
	
	/**
	 * Wait for the given preview to be created.
	 * @param userId
	 * @param tokenOrPreviewId
	 * @param timeout
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public PresignedUrl waitForUserProfilePreviewToBeCreated(String userId, String tokenOrPreviewId, int timeout) throws SynapseException, JSONObjectAdapterException{
		return waitForPreviewToBeCreated(userId, AttachmentType.USER_PROFILE, tokenOrPreviewId, timeout);
	}
	
	/**
	 * Wait for the given preview to be created.
	 * @param entityId
	 * @param attachment data type
	 * @param tokenOrPreviewId
	 * @param timeout
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public PresignedUrl waitForPreviewToBeCreated(String id, AttachmentType type, String tokenOrPreviewId, int timeout) throws SynapseException, JSONObjectAdapterException{
		long start = System.currentTimeMillis();
		PresignedUrl url = createAttachmentPresignedUrl(id, type, tokenOrPreviewId);
		if(URLStatus.READ_FOR_DOWNLOAD == url.getStatus()) return url;
		while(URLStatus.DOES_NOT_EXIST == url.getStatus()){
			// Wait for it.
			try {
				Thread.sleep(1000);
				long now = System.currentTimeMillis();
				long eplase = now-start;
				if(eplase > timeout) throw new SynapseException("Timed-out wiating for a preview to be created.");
				url = createAttachmentPresignedUrl(id, type, tokenOrPreviewId);
				if(URLStatus.READ_FOR_DOWNLOAD == url.getStatus()) return url;
			} catch (InterruptedException e) {
				throw new SynapseException(e);
			}
		}
		return url;
	}
	
	/**
	 * Download an entity attachment to a local file
	 * @param entityId
	 * @param attachmentData
	 * @param destFile
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public void downloadEntityAttachment(String entityId, AttachmentData attachmentData, File destFile) throws SynapseException, JSONObjectAdapterException{
		downloadAttachment(entityId, AttachmentType.ENTITY, attachmentData, destFile);
	}
	
	/**
	 * Download an user profile attachment to a local file
	 * @param userId
	 * @param attachmentData
	 * @param destFile
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public void downloadUserProfileAttachment(String userId, AttachmentData attachmentData, File destFile) throws SynapseException, JSONObjectAdapterException{
		downloadAttachment(userId, AttachmentType.USER_PROFILE, attachmentData, destFile);
	}
	
	/**
	 * Download an entity attachment to a local file
	 * @param id
	 * @param attachmentType
	 * @param destFile
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public void downloadAttachment(String id, AttachmentType type, AttachmentData attachmentData, File destFile) throws SynapseException, JSONObjectAdapterException{
		// First get the URL
		String url = null;
		if(attachmentData.getTokenId() != null){
			// Use the token to get the file
			PresignedUrl preUrl = createAttachmentPresignedUrl(id, type, attachmentData.getTokenId());
			url = preUrl.getPresignedUrl();
		}else{
			// Just download the file.
			url = attachmentData.getUrl();
		}
		//Now download the file
		downloadFromSynapse(url, null, destFile);
	}
	
	/**
	 * Downlaod a preview to the passed file.
	 * @param entityId
	 * @param previewId
	 * @param destFile
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public void downloadEntityAttachmentPreview(String entityId, String previewId, File destFile) throws SynapseException, JSONObjectAdapterException{
		downloadAttachmentPreview(entityId, AttachmentType.ENTITY, previewId, destFile);
	}
	
	/**
	 * Downlaod a preview to the passed file.
	 * @param userId
	 * @param previewId
	 * @param destFile
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public void downloadUserProfileAttachmentPreview(String userId, String previewId, File destFile) throws SynapseException, JSONObjectAdapterException{
		downloadAttachmentPreview(userId, AttachmentType.USER_PROFILE, previewId, destFile);
	}
	
	/**
	 * Downlaod a preview to the passed file.
	 * @param entityId
	 * @param attachment data type
	 * @param previewId
	 * @param destFile
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public void downloadAttachmentPreview(String id, AttachmentType type, String previewId, File destFile) throws SynapseException, JSONObjectAdapterException{
		// First get the URL
		String url = null;
		PresignedUrl preUrl = createAttachmentPresignedUrl(id, type, previewId);
		url = preUrl.getPresignedUrl();
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
	public S3AttachmentToken createAttachmentS3Token(String id, ServiceConstants.AttachmentType attachmentType, S3AttachmentToken token) throws JSONObjectAdapterException, SynapseException{
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		if(token == null) throw new IllegalArgumentException("S3AttachmentToken cannot be null");
		JSONObject jsonObject = EntityFactory.createJSONObjectForEntity(token);
		String uri = getAttachmentTypeURL(attachmentType)+"/"+id+ATTACHMENT_S3_TOKEN;
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

	public JSONObject getAuthEntity(String uri)
			throws SynapseException {
		return getSynapseEntity(authEndpoint, uri);
	}
	
	public JSONObject putAuthEntity(String uri, JSONObject entity)
			throws SynapseException {
		if (null == authEndpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}

		Map<String, String> requestHeaders = new HashMap<String, String>();
		return putSynapseEntity(authEndpoint, uri, entity, requestHeaders);
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

			String responseBody = (null != response.getEntity()) ? EntityUtils
					.toString(response.getEntity()) : null;
			if (null != responseBody && responseBody.length()>0) {
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
		try {
			return EntityFactory.createEntityFromJSONObject(jsonObject, clazz);
		} catch (Exception e) {
			throw new SynapseException("Failed to create an Entity for <<"+jsonObject+">>", e);
		}
		
	}
	
	public SearchResults search(SearchQuery searchQuery) throws SynapseException, UnsupportedEncodingException, JSONObjectAdapterException {
		SearchResults searchResults = null;		
		String uri = "/search?q=" + URLEncoder.encode(SearchUtil.generateQueryString(searchQuery), "UTF-8");
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

	/**
	 * Get the child count for this entity
	 * @param entityId
	 * @return
	 * @throws SynapseException 
	 * @throws JSONException 
	 */
	public Long getChildCount(String entityId) throws SynapseException {
		String queryString = SELECT_ID_FROM_ENTITY_WHERE_PARENT_ID+entityId+LIMIT_1_OFFSET_1;
		JSONObject query = query(queryString);
		if(!query.has(TOTAL_NUM_RESULTS)){
			throw new SynapseException("Query results did not have "+TOTAL_NUM_RESULTS);
		}
		try {
			return query.getLong(TOTAL_NUM_RESULTS);
		} catch (JSONException e) {
			throw new SynapseException(e);
		}
	}
	
	/**
	 * Get the appropriate piece of the URL based on the attachment type
	 * @param type
	 * @return
	 */
	public static String getAttachmentTypeURL(ServiceConstants.AttachmentType type)
	{
		if (type == AttachmentType.ENTITY)
			return ENTITY;
		else if (type == AttachmentType.USER_PROFILE)
			return USER_PROFILE_PATH;
		else throw new IllegalArgumentException("Unrecognized attachment type: " + type);
	}
	/**
	 * Get the ids of all users and groups.
	 * @param client
	 * @return
	 * @throws SynapseException
	 */
	public Set<String> getAllUserAndGroupIds() throws SynapseException {
		HashSet<String> ids = new HashSet<String>();
		// Get all the users
		PaginatedResults<UserProfile> pr = this.getUsers(0, Integer.MAX_VALUE);
		for(UserProfile up : pr.getResults()){
			ids.add(up.getOwnerId());
		}
		PaginatedResults<UserGroup> groupPr = this.getGroups(0, Integer.MAX_VALUE);
		for(UserGroup ug : groupPr.getResults()){
			ids.add(ug.getId());
		}
		return ids;
	}
	
	/**
	 * @return version
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public VersionInfo getVersionInfo() throws SynapseException,
			JSONObjectAdapterException {
		JSONObject json = getEntity(VERSION_INFO);
		return EntityFactory
				.createEntityFromJSONObject(json, VersionInfo.class);
	}

}
