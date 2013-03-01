package org.sagebionetworks.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
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
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
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
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.S3Token;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.ServiceConstants.AttachmentType;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.VariableContentPaginatedResults;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.attachment.URLStatus;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.request.ReferenceList;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
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


	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

	protected static final Logger log = Logger.getLogger(Synapse.class.getName());

	protected static final int JSON_INDENT = 2;
	protected static final String DEFAULT_REPO_ENDPOINT = "https://repo-prod.prod.sagebase.org/repo/v1";
	protected static final String DEFAULT_AUTH_ENDPOINT = "https://auth-prod.prod.sagebase.org/auth/v1";
	protected static final String DEFAULT_FILE_ENDPOINT = "https://file-prod.prod.sagebase.org/file/v1";
	protected static final String SESSION_TOKEN_HEADER = "sessionToken";
	protected static final String REQUEST_PROFILE_DATA = "profile_request";
	protected static final String PROFILE_RESPONSE_OBJECT_HEADER = "profile_response_object";

	protected static final String PASSWORD_FIELD = "password";
	protected static final String PARAM_GENERATED_BY = "generatedBy";
	
	protected static final String QUERY_URI = "/query?query=";
	protected static final String REPO_SUFFIX_PATH = "/path";	
	protected static final String REPO_SUFFIX_VERSION = "/version";
	protected static final String ANNOTATION_URI_SUFFIX = "annotations";
	protected static final String ADMIN = "/admin";
	protected static final String STACK_STATUS = ADMIN + "/synapse/status";
	protected static final String ENTITY = "/entity";
	protected static final String ATTACHMENT_S3_TOKEN = "/s3AttachmentToken";
	protected static final String ATTACHMENT_URL = "/attachmentUrl";
	protected static final String GENERATED_BY_SUFFIX = "/generatedBy";

	protected static final String ENTITY_URI_PATH = "/entity";
	protected static final String STORAGE_DETAILS_PATH = "/storageDetails"+ENTITY_URI_PATH;
	protected static final String ENTITY_ACL_PATH_SUFFIX = "/acl";
	protected static final String ENTITY_ACL_RECURSIVE_SUFFIX = "?recursive=true";
	protected static final String ENTITY_BUNDLE_PATH = "/bundle?mask=";
	protected static final String BUNDLE = "/bundle";
	protected static final String BENEFACTOR = "/benefactor"; // from org.sagebionetworks.repo.web.UrlHelpers
	protected static final String ACTIVITY_URI_PATH = "/activity";
	protected static final String GENERATED_PATH = "/generated";
	
	protected static final String WIKI_URI_TEMPLATE = "/%1$s/%2$s/wiki";
	protected static final String WIKI_ID_URI_TEMPLATE = "/%1$s/%2$s/wiki/%3$s";
	protected static final String WIKI_TREE_URI_TEMPLATE = "/%1$s/%2$s/wikiheadertree";
	protected static final String ATTACHMENT_HANDLES = "/attachmenthandles";
	protected static final String ATTACHMENT_FILE = "/attachment";
	protected static final String ATTACHMENT_FILE_PREVIEW = "/attachmentpreview";
	protected static final String FILE_NAME_PARAMETER = "?fileName=";
	protected static final String REDIRECT_PARAMETER = "redirect=";
	protected static final String AND_REDIRECT_PARAMETER = "&"+REDIRECT_PARAMETER;
	protected static final String QUERY_REDIRECT_PARAMETER = "?"+REDIRECT_PARAMETER;
	
	protected static final String EVALUATION_URI_PATH = "/evaluation";
	protected static final String COUNT = "count";
	protected static final String NAME = "name";
	protected static final String ALL = "/all";
	protected static final String PARTICIPANT = "participant";
	protected static final String SUBMISSION = "submission";
	protected static final String SUBMISSION_BUNDLE = SUBMISSION + BUNDLE;
	protected static final String SUBMISSION_ALL = SUBMISSION + ALL;
	protected static final String SUBMISSION_ALL_BUNDLE = SUBMISSION + BUNDLE + ALL;
	protected static final String STATUS = "status";
	protected static final String STATUS_SUFFIX = "?status=";

	protected static final String USER_PROFILE_PATH = "/userProfile";
	
	protected static final String USER_GROUP_HEADER_BATCH_PATH = "/userGroupHeaders/batch?ids=";

	protected static final String TOTAL_NUM_RESULTS = "totalNumberOfResults";
	
	protected static final String ACCESS_REQUIREMENT = "/accessRequirement";
	
	protected static final String ACCESS_REQUIREMENT_UNFULFILLED = "/accessRequirementUnfulfilled";
	
	protected static final String ACCESS_APPROVAL = "/accessApproval";
	
	protected static final String VERSION_INFO = "/version";
	
	protected static final String FILE_HANDLE = "/fileHandle";
	private static final String FILE = "/file";
	private static final String FILE_PREVIEW = "/filepreview";
	private static final String EXTERNAL_FILE_HANDLE = "/externalFileHandle";
	private static final String FILE_HANDLES = "/filehandles";

	private static final String TRASHCAN_TRASH = "/trashcan/trash";
	private static final String TRASHCAN_RESTORE = "/trashcan/restore";
	private static final String TRASHCAN_VIEW = "/trashcan/view";
	private static final String TRASHCAN_PURGE = "/trashcan/purge";

	// web request pagination parameters
	protected static final String LIMIT = "limit";
	protected static final String OFFSET = "offset";

	protected static final String LIMIT_1_OFFSET_1 = "' limit 1 offset 1";
	protected static final String SELECT_ID_FROM_ENTITY_WHERE_PARENT_ID = "select id from entity where parentId == '";

	
	protected String repoEndpoint;
	protected String authEndpoint;
	protected String fileEndpoint;

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
		setFileEndpoint(DEFAULT_FILE_ENDPOINT);

		defaultGETDELETEHeaders = new HashMap<String, String>();
		defaultGETDELETEHeaders.put("Accept", "application/json");

		defaultPOSTPUTHeaders = new HashMap<String, String>();
		defaultPOSTPUTHeaders.putAll(defaultGETDELETEHeaders);
		defaultPOSTPUTHeaders.put("Content-Type", "application/json");

		this.clientProvider = clientProvider;
		clientProvider.setGlobalConnectionTimeout(ServiceConstants.DEFAULT_CONNECT_TIMEOUT_MSEC);
		clientProvider.setGlobalSocketTimeout(ServiceConstants.DEFAULT_SOCKET_TIMEOUT_MSEC);
		
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
	 * @param fileEndpoint
	 *            the authEndpoint to set
	 */
	public void setFileEndpoint(String fileEndpoint) {
		this.fileEndpoint = fileEndpoint;
	}
	
	/**
	 * The endpoint used for file multi-part upload.
	 * 
	 * @return
	 */
	public String getFileEndpoint(){
		return this.fileEndpoint;
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
	public JSONObject createJSONObject(String uri, JSONObject entity)
			throws SynapseException {
		return createJSONObjectEntity(repoEndpoint, uri, entity);
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
		return createEntity(entity, null);
	}

	/**
	 * Create a new Entity.
	 * 
	 * @param <T>
	 * @param entity
	 * @param activityId set generatedBy relationship to the new entity
	 * @return the newly created entity
	 * @throws SynapseException
	 */
	public <T extends Entity> T createEntity(T entity, String activityId)
			throws SynapseException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");
		entity.setEntityType(entity.getClass().getName());
		String uri = ENTITY_URI_PATH;
		if(activityId != null) 
			uri += "?" + PARAM_GENERATED_BY + "=" + activityId;		
		return createJSONEntity(uri, entity);
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
	public <T extends JSONEntity> T createJSONEntity(String uri, T entity)
			throws SynapseException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");		
		// Get the json for this entity
		JSONObject jsonObject;
		try {
			jsonObject = EntityFactory.createJSONObjectForEntity(entity);
			// Create the entity
			jsonObject = createJSONObject(uri, jsonObject);
			// Now convert to Object to an entity
			return (T) EntityFactory.createEntityFromJSONObject(jsonObject,
					entity.getClass());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Create an Entity, Annotations, and ACL with a single call.
	 * 
	 * @param ebc
	 * @return
	 * @throws SynapseException
	 */
	public EntityBundle createEntityBundle(EntityBundleCreate ebc) throws SynapseException {
		return createEntityBundle(ebc, null);
	}
	
	/**
	 * Create an Entity, Annotations, and ACL with a single call.
	 * 
	 * @param ebc
	 * @param activityId the activity to create a generatedBy relationship with the entity in the Bundle. 
	 * @return
	 * @throws SynapseException
	 */
	public EntityBundle createEntityBundle(EntityBundleCreate ebc, String activityId) throws SynapseException {
		if (ebc == null)
			throw new IllegalArgumentException("EntityBundle cannot be null");
		String url = ENTITY_URI_PATH + BUNDLE;
		JSONObject jsonObject;		
		if(activityId != null) 
			url += "?" + PARAM_GENERATED_BY +"=" + activityId;
		try {
			// Convert to JSON
			jsonObject = EntityFactory.createJSONObjectForEntity(ebc);
			// Create
			jsonObject = createJSONObject(url, jsonObject);
			// Convert returned JSON to EntityBundle
			return EntityFactory.createEntityFromJSONObject(jsonObject,	EntityBundle.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	/**
	 * Update an Entity, Annotations, and ACL with a single call.
	 * 
	 * @param ebc
	 * @return
	 * @throws SynapseException
	 */
	public EntityBundle updateEntityBundle(String entityId, EntityBundleCreate ebc) throws SynapseException {
		return updateEntityBundle(entityId, ebc, null);
	}

	/**
	 * Update an Entity, Annotations, and ACL with a single call.
	 * 
	 * @param ebc
	 * @param activityId the activity to create a generatedBy relationship with the entity in the Bundle.
	 * @return
	 * @throws SynapseException
	 */
	public EntityBundle updateEntityBundle(String entityId, EntityBundleCreate ebc, String activityId) throws SynapseException {
		if (ebc == null)
			throw new IllegalArgumentException("EntityBundle cannot be null");
		String url = ENTITY_URI_PATH + "/" + entityId + BUNDLE;
		JSONObject jsonObject;
		try {
			// Convert to JSON
			jsonObject = EntityFactory.createJSONObjectForEntity(ebc);
			
			// Update. Bundles do not have their own etags, so we use an
			// empty requestHeaders object.
			Map<String, String> requestHeaders = new HashMap<String, String>();
			jsonObject = putJSONObject(repoEndpoint, url, jsonObject, requestHeaders);
			
			// Convert returned JSON to EntityBundle
			return EntityFactory.createEntityFromJSONObject(jsonObject,	EntityBundle.class);
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
	public PaginatedResults<VersionInfo> getEntityVersions(String entityId, int offset, int limit) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		String url = ENTITY_URI_PATH + "/" + entityId + REPO_SUFFIX_VERSION +
				"?" + OFFSET + "=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<VersionInfo> results = new PaginatedResults<VersionInfo>(VersionInfo.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
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
			Map<String,String> headers = new HashMap<String, String>();
			putJSONObject(uri, EntityFactory.createJSONObjectForEntity(userProfile), headers);
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
	 * Batch get headers for users/groups matching a list of Synapse IDs.
	 * 
	 * @param ids
	 * @return
	 * @throws JSONException 
	 * @throws SynapseException 
	 */
	public UserGroupHeaderResponsePage getUserGroupHeadersByIds(List<String> ids) throws SynapseException {
		String uri = listToString(ids);
		JSONObject json = getEntity(uri);
		return initializeFromJSONObject(json, UserGroupHeaderResponsePage.class);
	}

	private String listToString(List<String> ids) {
		StringBuilder sb = new StringBuilder();
		sb.append(USER_GROUP_HEADER_BATCH_PATH);
		for (String id : ids) {
			sb.append(id);
			sb.append(',');
		}
		// Remove the trailing comma
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
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
			jsonAcl = putJSONObject(uri, jsonAcl, null);
			return initializeFromJSONObject(jsonAcl, AccessControlList.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public void deleteACL(String entityId) throws SynapseException {
		String uri = ENTITY_URI_PATH + "/" + entityId+ ENTITY_ACL_PATH_SUFFIX;
		deleteUri(uri);
	}
	
	public AccessControlList createACL(AccessControlList acl) throws SynapseException {
		String entityId = acl.getId();
		String uri = ENTITY_URI_PATH + "/" + entityId+ ENTITY_ACL_PATH_SUFFIX;
		try {
			JSONObject jsonAcl = EntityFactory.createJSONObjectForEntity(acl);
			jsonAcl = createJSONObject(uri, jsonAcl);
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
			Map<String,String> headers = new HashMap<String, String>();
			jsonObject = putJSONObject(url, jsonObject, headers);
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
			jsonObject = createJSONObject(ACCESS_REQUIREMENT, jsonObject);
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
			jsonObject = createJSONObject(ACCESS_APPROVAL, jsonObject);
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
	public <T extends JSONEntity> T getEntity(String entityId, Class<? extends T> clazz) throws SynapseException {
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
		return putEntity(entity, null);
	}

	/**
	 * Update a dataset, layer, preview, annotations, etc...
	 * 
	 * @param <T> 
	 * @param entity
	 * @param activityId activity to create generatedBy conenction to
	 * @return the updated entity
	 * @throws SynapseException
	 */
	@SuppressWarnings("unchecked")
	public <T extends Entity> T putEntity(T entity, String activityId) throws SynapseException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");		
		Map<String,String> headers = new HashMap<String, String>();
		try {
			String uri = createEntityUri(ENTITY_URI_PATH, entity.getId());
			if(activityId != null) 
				uri += "?" + PARAM_GENERATED_BY + "=" + activityId;
			JSONObject jsonObject;
			jsonObject = EntityFactory.createJSONObjectForEntity(entity);
			jsonObject = putJSONObject(uri, jsonObject, headers);
			return (T) EntityFactory.createEntityFromJSONObject(jsonObject,
					entity.getClass());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Create a new version of a given versionable Entity
	 * @param <T>
	 * @param entity
	 * @param activityId
	 * @return
	 * @throws SynapseException
	 */
	public <T extends Versionable> T cereateNewEntityVersion(T entity) throws SynapseException {
		return cereateNewEntityVersion(entity, null);
	}
	
	/**
	 * Create a new version of a given versionable Entity and create generatedBy connection to the given activity
	 * @param <T>
	 * @param entity
	 * @param activityId
	 * @return
	 * @throws SynapseException
	 */
	@SuppressWarnings("unchecked")
	public <T extends Versionable> T cereateNewEntityVersion(T entity, String activityId) throws SynapseException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");

		Map<String,String> headers = new HashMap<String, String>();
		try {
			String uri = createEntityUri(ENTITY_URI_PATH, entity.getId()) + REPO_SUFFIX_VERSION;
			if(activityId != null) 
				uri += "?" + PARAM_GENERATED_BY + "=" + activityId;
			JSONObject jsonObject;
			jsonObject = EntityFactory.createJSONObjectForEntity(entity);
			jsonObject = putJSONObject(uri, jsonObject, headers);
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
	public JSONObject putJSONObject(String uri, JSONObject entity, Map<String,String> headers)
			throws SynapseException {
		return putJSONObject(repoEndpoint, uri, entity, headers);
	}
	
	/**
	 * Create a dataset, layer, etc..
	 * 
	 * @param uri
	 * @throws SynapseException
	 */
	public JSONObject postUri(String uri) throws SynapseException {
		return postUri(repoEndpoint, uri);
	}


	/**
	 * Delete a dataset, layer, etc..
	 * 
	 * @param uri
	 * @throws SynapseException
	 */
	public void deleteUri(String uri) throws SynapseException {
		deleteUri(repoEndpoint, uri);
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
		deleteUri(uri);
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
		deleteUri(uri);
	}

	public <T extends Entity> void deleteEntityVersion(T entity, Long versionNumber) throws SynapseException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");
		deleteEntityVersionById(entity.getId(), versionNumber);
	}

	public void deleteEntityVersionById(String entityId, Long versionNumber) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		if (versionNumber == null)
			throw new IllegalArgumentException("VersionNumber cannot be null");
		String uri = createEntityUri(ENTITY_URI_PATH, entityId);
		uri += REPO_SUFFIX_VERSION + "/" + versionNumber;
		deleteUri(uri);
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
	
	public BatchResults<EntityHeader> getEntityHeaderBatch(List<Reference> references) throws SynapseException {
		ReferenceList list = new ReferenceList();
		list.setReferences(references);
		String url = ENTITY_URI_PATH + "/header";
		JSONObject jsonObject;
		try {
			jsonObject = EntityFactory.createJSONObjectForEntity(list);
			// POST
			jsonObject = createJSONObject(url, jsonObject);
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObject);
			BatchResults<EntityHeader> results = new BatchResults<EntityHeader>(EntityHeader.class);
			results.initializeFromJSONObject(adapter);
			return results;

		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
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
	 * Upload each file to Synapse creating a file handle for each.
	 * 
	 * @param files
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public FileHandleResults createFileHandles(List<File> files) throws SynapseException{
		if(files == null) throw new IllegalArgumentException("File list cannot be null");
		String url = getFileEndpoint()+FILE_HANDLE;
		// This call requires a multi-part request.
		try {
			HttpPost httppost = new HttpPost(url);
			MultipartEntity reqEntity = new MultipartEntity();
			for(File file: files){
				// We need to determine the content type of the file
				String contentType = guessContentTypeFromStream(file);
				FileBody bin = new FileBody(file, contentType);
				reqEntity.addPart("file", bin);
			}
			// Add the headers
			for(String key: this.defaultPOSTPUTHeaders.keySet()){
				String value = this.defaultPOSTPUTHeaders.get(key);
				httppost.setHeader(key, value);
			}
			// Add the header that sets the content type and the boundary
			httppost.setHeader(reqEntity.getContentType());
			httppost.setHeader(reqEntity.getContentEncoding());
			httppost.setEntity(reqEntity);
			HttpResponse response = clientProvider.execute(httppost);
			String responseBody = (null != response.getEntity()) ? EntityUtils.toString(response.getEntity()) : null;
			return EntityFactory.createEntityFromJSONString(responseBody, FileHandleResults.class);
			// Get the response.
		} catch (ClientProtocolException e) {
			throw new SynapseException(e);
		} catch (IOException e) {
			throw new SynapseException(e);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}		
	}
	
	/**
	 * Create an External File Handle.  This is used to references a file that is not stored in Synpase.
	 * @param efh
	 * @return
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public ExternalFileHandle createExternalFileHandle(ExternalFileHandle efh) throws JSONObjectAdapterException, SynapseException{
		String uri = EXTERNAL_FILE_HANDLE;
		return createJSONEntity(getFileEndpoint(), uri, efh);
	}
	/**
	 * Get the raw file handle.
	 * Note: Only the creator of a the file handle can get the raw file handle.
	 * 
	 * @param fileHandleId
	 * @return
	 * @throws SynapseException 
	 */
	public FileHandle getRawFileHandle(String fileHandleId) throws SynapseException{
		JSONObject object = signAndDispatchSynapseRequest(getFileEndpoint(), FILE_HANDLE+"/"+fileHandleId, "GET", null, defaultGETDELETEHeaders);
		try {
			return EntityFactory.createEntityFromJSONObject(object, FileHandle.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	/**
	 * Delete a raw file handle.
	 * Note: Only the creator of a the file handle can delete the file handle.
	 * @param fileHandleId
	 * @throws SynapseException 
	 */
	public void deleteFileHandle(String fileHandleId) throws SynapseException{
		signAndDispatchSynapseRequest(getFileEndpoint(), FILE_HANDLE+"/"+fileHandleId, "DELETE", null, defaultGETDELETEHeaders);
	}

	/**
	 * Guess the content type of a file by reading the start of the file stream using
	 * URLConnection.guessContentTypeFromStream(is);
	 * If URLConnection fails to return a content type then "application/octet-stream" will be returned.
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static String guessContentTypeFromStream(File file)	throws FileNotFoundException, IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(file));
		try{
			// Let java guess from the stream.
			String contentType = URLConnection.guessContentTypeFromStream(is);
			// If Java fails then set the content type to be octet-stream
			if(contentType == null){
				contentType = APPLICATION_OCTET_STREAM;
			}
			return contentType;
		}finally{
			is.close();
		}
	}
	
	/**
	 * 
	 * Create a wiki page for a given owner object.
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @param toCreate
	 * @return
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public WikiPage createWikiPage(String ownerId, ObjectType ownerType, WikiPage toCreate) throws JSONObjectAdapterException, SynapseException{
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		if(toCreate == null) throw new IllegalArgumentException("WikiPage cannot be null");
		String uri = createWikiURL(ownerId, ownerType);
		return createJSONEntity(getRepoEndpoint(), uri, toCreate);
	}

	/**
	 * Helper to create a wiki URL that does not include the wiki id.
	 * @param ownerId
	 * @param ownerType
	 * @return
	 */
	private String createWikiURL(String ownerId, ObjectType ownerType) {
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		return String.format(WIKI_URI_TEMPLATE, ownerType.name().toLowerCase(), ownerId);
	}

	/**
	 * Get a WikiPage using its key
	 * @param key
	 * @return
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public WikiPage getWikiPage(WikiPageKey key) throws JSONObjectAdapterException, SynapseException{
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		String uri = createWikiURL(key);
		return getJSONEntity(getRepoEndpoint(), uri, WikiPage.class);
	}
	

	/**
	 * Get a the root WikiPage for a given owner.
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public WikiPage getRootWikiPage(String ownerId, ObjectType ownerType) throws JSONObjectAdapterException, SynapseException{
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		String uri = createWikiURL(ownerId, ownerType);
		return getJSONEntity(getRepoEndpoint(), uri, WikiPage.class);
	}
	
	/**
	 * Get all of the FileHandles associated with a WikiPage, including any PreviewHandles.
	 * @param key
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public FileHandleResults getWikiAttachmenthHandles(WikiPageKey key) throws JSONObjectAdapterException, SynapseException{
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		String uri = createWikiURL(key)+ATTACHMENT_HANDLES;
		return getJSONEntity(getRepoEndpoint(), uri, FileHandleResults.class);
	}
	

	/**
	 * 
	 * @param key - Identifies a wiki page.
	 * @param fileName - The name of the attachment file.
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public File downloadWikiAttachment(WikiPageKey key, String fileName) throws ClientProtocolException, IOException{
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(fileName == null) throw new IllegalArgumentException("fileName cannot be null");
		String encodedName = URLEncoder.encode(fileName, "UTF-8");
		String uri = createWikiURL(key)+ATTACHMENT_FILE+FILE_NAME_PARAMETER+encodedName;
		return downloadFile(getRepoEndpoint(), uri);	
	}
	
	/**
	 * Get the temporary URL for a WikiPage attachment. This is an alternative to downloading the attachment to a file.
	 * @param key - Identifies a wiki page.
	 * @param fileName - The name of the attachment file.
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public URL getWikiAttachmentTemporaryUrl(WikiPageKey key, String fileName) throws ClientProtocolException, IOException{
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(fileName == null) throw new IllegalArgumentException("fileName cannot be null");
		String encodedName = URLEncoder.encode(fileName, "UTF-8");
		String uri = getRepoEndpoint()+createWikiURL(key)+ATTACHMENT_FILE+FILE_NAME_PARAMETER+encodedName+AND_REDIRECT_PARAMETER+"false";
		return getUrl(uri);
	}
	

	/**
	 * Download the preview of a wiki attachment file.
	 * @param key - Identifies a wiki page.
	 * @param fileName - The name of the original attachment file that you want to downlaod a preview for.
	 * @return
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws ClientProtocolException 
	 */
	public File downloadWikiAttachmentPreview(WikiPageKey key, String fileName) throws ClientProtocolException, FileNotFoundException, IOException{
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(fileName == null) throw new IllegalArgumentException("fileName cannot be null");
		String encodedName = URLEncoder.encode(fileName, "UTF-8");
		String uri = createWikiURL(key)+ATTACHMENT_FILE_PREVIEW+FILE_NAME_PARAMETER+encodedName;
		return downloadFile(getRepoEndpoint(), uri);	
	}
	
	/**
	 * Get the temporary URL for a WikiPage attachment preview. This is an alternative to downloading the attachment to a file.
	 * @param key - Identifies a wiki page.
	 * @param fileName - The name of the attachment file.
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public URL getWikiAttachmentPreviewTemporaryUrl(WikiPageKey key, String fileName) throws ClientProtocolException, IOException{
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(fileName == null) throw new IllegalArgumentException("fileName cannot be null");
		String encodedName = URLEncoder.encode(fileName, "UTF-8");
		String uri = getRepoEndpoint()+createWikiURL(key)+ATTACHMENT_FILE_PREVIEW+FILE_NAME_PARAMETER+encodedName+AND_REDIRECT_PARAMETER+"false";
		return getUrl(uri);
	}
	
	/**
	 * Get the temporary URL for the data file of a FileEntity for the current version of the entity..  This is an alternative to downloading the file.
	 * 
	 * @param entityId
	 * @return
	 * @throws ClientProtocolException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public URL getFileEntityTemporaryUrlForCurrentVersion(String entityId) throws ClientProtocolException, MalformedURLException, IOException{
		String uri = getRepoEndpoint()+ENTITY+"/"+entityId+FILE+QUERY_REDIRECT_PARAMETER+"false";
		return getUrl(uri);
	}
	
	/**
	 * Get the temporary URL for the data file preview of a FileEntity for the current version of the entity..  This is an alternative to downloading the file.
	 * 
	 * @param entityId
	 * @return
	 * @throws ClientProtocolException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public URL getFileEntityPreviewTemporaryUrlForCurrentVersion(String entityId) throws ClientProtocolException, MalformedURLException, IOException{
		String uri = getRepoEndpoint()+ENTITY+"/"+entityId+FILE_PREVIEW+QUERY_REDIRECT_PARAMETER+"false";
		return getUrl(uri);
	}
	
	/**
	 * Get the temporary URL for the data file of a FileEntity for a given version number.  This is an alternative to downloading the file.
	 * 
	 * @param entityId
	 * @return
	 * @throws ClientProtocolException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public URL getFileEntityTemporaryUrlForVersion(String entityId, Long versionNumber) throws ClientProtocolException, MalformedURLException, IOException{
		String uri = getRepoEndpoint()+ENTITY+"/"+entityId+VERSION_INFO+"/"+versionNumber+FILE+QUERY_REDIRECT_PARAMETER+"false";
		return getUrl(uri);
	}
	
	/**
	 * Get the temporary URL for the data file of a FileEntity for a given version number.  This is an alternative to downloading the file.
	 * 
	 * @param entityId
	 * @return
	 * @throws ClientProtocolException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public URL getFileEntityPreviewTemporaryUrlForVersion(String entityId, Long versionNumber) throws ClientProtocolException, MalformedURLException, IOException{
		String uri = getRepoEndpoint()+ENTITY+"/"+entityId+VERSION_INFO+"/"+versionNumber+FILE_PREVIEW+QUERY_REDIRECT_PARAMETER+"false";
		return getUrl(uri);
	}


	/**
	 * Fetch a temporary url.
	 * @param uri
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	protected URL getUrl(String uri) throws ClientProtocolException, IOException,
			MalformedURLException {
		HttpGet get = new HttpGet(uri);
		for(String headerKey: this.defaultGETDELETEHeaders.keySet()){
			String value = this.defaultGETDELETEHeaders.get(headerKey);
			get.setHeader(headerKey, value);
		}
		HttpResponse response = clientProvider.execute(get);
		String responseBody = (null != response.getEntity()) ? EntityUtils.toString(response.getEntity()) : null;
		return new URL(responseBody);
	}
	/**
	 * Download the file at the given URL.
	 * @param uri
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private File downloadFile(String endpoint, String uri) throws ClientProtocolException, IOException, FileNotFoundException {
		HttpGet get = new HttpGet(endpoint+uri);
		// Add the headers
		for(String headerKey: this.defaultGETDELETEHeaders.keySet()){
			String value = this.defaultGETDELETEHeaders.get(headerKey);
			get.setHeader(headerKey, value);
		}
		// Add the header that sets the content type and the boundary
		HttpResponse response = clientProvider.execute(get);
		HttpEntity entity = response.getEntity();
		InputStream input = entity.getContent();
		File temp = File.createTempFile("downloadWikiAttachment", ".tmp");
		FileOutputStream fos = new FileOutputStream(temp);
		try{
			byte[] buffer = new byte[1024]; 
			int read = -1;
			while((read = input.read(buffer)) > 0){
				fos.write(buffer, 0, read);
			}
			return temp;
		}finally{
			if(fos != null){
				fos.flush();
				fos.close();
			}
			if(input != null){
				input.close();
			}
		}
	}
		
	/**
	 * Update a WikiPage
	 * @param ownerId
	 * @param ownerType
	 * @param toUpdate
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public WikiPage updateWikiPage(String ownerId, ObjectType ownerType, WikiPage toUpdate) throws JSONObjectAdapterException, SynapseException{
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		if(toUpdate == null) throw new IllegalArgumentException("WikiPage cannot be null");
		if(toUpdate.getId() == null) throw new IllegalArgumentException("WikiPage.getId() cannot be null");
		String uri = String.format(WIKI_ID_URI_TEMPLATE, ownerType.name().toLowerCase(), ownerId, toUpdate.getId());
		return updateJSONEntity(getRepoEndpoint(), uri, toUpdate);
	}
	
	/**
	 * Delete a WikiPage
	 * @param key
	 * @throws SynapseException
	 */
	public void deleteWikiPage(WikiPageKey key) throws SynapseException{
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		String uri = createWikiURL(key);
		deleteUri(getRepoEndpoint(), uri);
	}
	
	/**
	 * Helper to build a URL for a wiki page.
	 * @param key
	 * @return
	 */
	private String createWikiURL(WikiPageKey key) {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		return String.format(WIKI_ID_URI_TEMPLATE, key.getOwnerObjectType().name().toLowerCase(), key.getOwnerObjectId(), key.getWikiPageId());
	}
	
	/**
	 * Get the WikiHeader tree for a given owner object.
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public PaginatedResults<WikiHeader> getWikiHeaderTree(String ownerId, ObjectType ownerType) throws SynapseException, JSONObjectAdapterException{
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		String uri = String.format(WIKI_TREE_URI_TEMPLATE, ownerType.name().toLowerCase(), ownerId);
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultGETDELETEHeaders);
		JSONObject object = signAndDispatchSynapseRequest(getRepoEndpoint(), uri, "GET", null,requestHeaders);
		PaginatedResults<WikiHeader> paginated = new PaginatedResults<WikiHeader>(WikiHeader.class);
		paginated.initializeFromJSONObject(new JSONObjectAdapterImpl(object));
		return paginated;
	}

	/**
	 * Get the file handles for the current version of an entity.
	 * 
	 * @param entityId
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public FileHandleResults getEntityFileHandlesForCurrentVersion(String entityId) throws JSONObjectAdapterException, SynapseException {
		if(entityId == null) throw new IllegalArgumentException("Key cannot be null");
		String uri = ENTITY_URI_PATH+"/"+entityId+FILE_HANDLES;
		return getJSONEntity(getRepoEndpoint(), uri, FileHandleResults.class);
	}
	
	/**
	 * Get the file hanldes for a given version of an entity.
	 * @param entityId
	 * @param versionNumber
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public FileHandleResults getEntityFileHandlesForVersion(String entityId, Long versionNumber) throws JSONObjectAdapterException, SynapseException {
		if(entityId == null) throw new IllegalArgumentException("Key cannot be null");
		String uri = ENTITY_URI_PATH+"/"+entityId+"/version/"+versionNumber+FILE_HANDLES;
		return getJSONEntity(getRepoEndpoint(), uri, FileHandleResults.class);
	}
	
	
	/**
	 * Download the locationable to a tempfile
	 * 
	 * @param locationable
	 * @return destination file
	 * @throws SynapseException
	 * @throws SynapseUserException
	 */
	@Deprecated
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
	@Deprecated
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
	@Deprecated
	public File downloadFromSynapse(LocationData location, String md5,
			File destinationFile) throws SynapseException {
		return downloadFromSynapse(location.getPath(), md5, destinationFile);
	}
	
	@Deprecated
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
	@Deprecated
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
	@Deprecated
	public Locationable uploadLocationableToSynapse(Locationable locationable,
			File dataFile, String md5) throws SynapseException {

		// Step 1: get the token
		S3Token s3Token = new S3Token();
		s3Token.setPath(dataFile.getName());
		s3Token.setMd5(md5);
		s3Token = createJSONEntity(locationable.getS3Token(), s3Token);

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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
	public PresignedUrl createAttachmentPresignedUrl(String id, AttachmentType attachmentType, String tokenOrPreviewId) throws SynapseException, JSONObjectAdapterException{
		String url = getAttachmentTypeURL(attachmentType)+"/"+id+ATTACHMENT_URL;
		PresignedUrl preIn = new PresignedUrl();
		preIn.setTokenID(tokenOrPreviewId);
		JSONObject jsonBody = EntityFactory.createJSONObjectForEntity(preIn);
		JSONObject json = createJSONObject(url, jsonBody);
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
	public S3AttachmentToken createAttachmentS3Token(String id, ServiceConstants.AttachmentType attachmentType, S3AttachmentToken token) throws JSONObjectAdapterException, SynapseException{
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		if(token == null) throw new IllegalArgumentException("S3AttachmentToken cannot be null");
		JSONObject jsonObject = EntityFactory.createJSONObjectForEntity(token);
		String uri = getAttachmentTypeURL(attachmentType)+"/"+id+ATTACHMENT_S3_TOKEN;
		jsonObject = createJSONObject(uri, jsonObject);
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
		return createJSONObjectEntity(authEndpoint, uri, entity);
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
		return putJSONObject(authEndpoint, uri, entity, requestHeaders);
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
	public JSONObject createJSONObjectEntity(String endpoint, String uri,
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
		Map<String,String> headers = new HashMap<String, String>();
		if(defaultPOSTPUTHeaders != null) headers.putAll(defaultPOSTPUTHeaders);

		return signAndDispatchSynapseRequest(endpoint, uri, "POST", entity.toString(),
				headers);
	}
	
	/**
	 * Create any JSONEntity
	 * @param endpoint
	 * @param uri
	 * @param entity
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException 
	 */
	@SuppressWarnings("unchecked")
	protected <T extends JSONEntity> T createJSONEntity (String endpoint, String uri, T entity) throws JSONObjectAdapterException, SynapseException{
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}
		Map<String,String> headers = new HashMap<String, String>();
		if(defaultPOSTPUTHeaders != null) headers.putAll(defaultPOSTPUTHeaders);
		String postJSON = EntityFactory.createJSONStringForEntity(entity);
		JSONObject jsonObject = signAndDispatchSynapseRequest(endpoint, uri, "POST", postJSON,	headers);
		return (T) EntityFactory.createEntityFromJSONObject(jsonObject, entity.getClass());
	}
	
	/**
	 * Update any JSONEntity
	 * @param endpoint
	 * @param uri
	 * @param entity
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	@SuppressWarnings("unchecked")
	protected <T extends JSONEntity> T updateJSONEntity(String endpoint, String uri, T entity) throws JSONObjectAdapterException, SynapseException{
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}
		Map<String,String> headers = new HashMap<String, String>();
		if(defaultPOSTPUTHeaders != null) headers.putAll(defaultPOSTPUTHeaders);
		String postJSON = EntityFactory.createJSONStringForEntity(entity);
		JSONObject jsonObject = signAndDispatchSynapseRequest(endpoint, uri, "PUT", postJSON,	headers);
		return (T) EntityFactory.createEntityFromJSONObject(jsonObject, entity.getClass());
	}
	
	/**
	 * Get a JSONEntity.
	 * @param endpoint
	 * @param uri
	 * @param clazz
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	protected <T extends JSONEntity> T getJSONEntity(String endpoint, String uri, Class<? extends T> clazz) throws JSONObjectAdapterException, SynapseException{
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == clazz) {
			throw new IllegalArgumentException("must provide entity");
		}
		Map<String,String> headers = new HashMap<String, String>();
		if(defaultPOSTPUTHeaders != null) headers.putAll(defaultGETDELETEHeaders);;
		JSONObject jsonObject = signAndDispatchSynapseRequest(endpoint, uri, "GET", null, headers);
		return (T) EntityFactory.createEntityFromJSONObject(jsonObject, clazz);
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
		return signAndDispatchSynapseRequest(endpoint, uri, "GET", null, defaultGETDELETEHeaders);
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
	@Deprecated
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
			return putJSONObject(endpoint, uri, storedEntity, new HashMap<String,String>());
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
	public JSONObject putJSONObject(String endpoint, String uri,
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

			Map<String, String> requestHeaders = new HashMap<String, String>();
			if(headers != null) requestHeaders.putAll(headers);
			requestHeaders.putAll(defaultPOSTPUTHeaders);
			return signAndDispatchSynapseRequest(endpoint, uri, "PUT", 
					entity.toString(), requestHeaders);
	}
	
	/**
	 * Call Create on any URI
	 * 
	 * @param endpoint
	 * @param uri
	 * @throws SynapseException
	 */
	public JSONObject postUri(String endpoint, String uri) throws SynapseException {
		if (null == uri) throw new IllegalArgumentException("must provide uri");		
		return signAndDispatchSynapseRequest(endpoint, uri, "POST", null, defaultPOSTPUTHeaders);
	}

	/**
	 * Call Delete on any URI
	 * 
	 * @param endpoint
	 * @param uri
	 * @throws SynapseException
	 */
	public void deleteUri(String endpoint, String uri) throws SynapseException {
		if (null == uri) throw new IllegalArgumentException("must provide uri");		
		signAndDispatchSynapseRequest(endpoint, uri, "DELETE", null, defaultGETDELETEHeaders);
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
		
		// remove session token if it is null
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
		String uri = "/search";
		String jsonBody = EntityFactory.createJSONStringForEntity(searchQuery);
		JSONObject obj = signAndDispatchSynapseRequest(repoEndpoint, uri, "POST", jsonBody, defaultPOSTPUTHeaders);
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
	public SynapseVersionInfo getVersionInfo() throws SynapseException,
			JSONObjectAdapterException {
		JSONObject json = getEntity(VERSION_INFO);
		return EntityFactory
				.createEntityFromJSONObject(json, SynapseVersionInfo.class);
	}
	
	/**
	 * Get the activity generatedBy an Entity
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	public Activity getActivityForEntity(String entityId) throws SynapseException {
		return getActivityForEntityVersion(entityId, null);
	}

	/**
	 * Get the activity generatedBy an Entity
	 * @param entityId
	 * @param versionNumber
	 * @return
	 * @throws SynapseException
	 */
	public Activity getActivityForEntityVersion(String entityId, Long versionNumber) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		String url = createEntityUri(ENTITY_URI_PATH, entityId);
		if(versionNumber != null) {
			url += REPO_SUFFIX_VERSION + "/" + versionNumber;
		}
		url += GENERATED_BY_SUFFIX;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			return new Activity(adapter);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}

	/**
	 * Set the activity generatedBy an Entity
	 * @param entityId
	 * @param activityId
	 * @return
	 * @throws SynapseException
	 */
	public Activity setActivityForEntity(String entityId, String activityId) throws SynapseException {
		if (entityId == null) throw new IllegalArgumentException("Entity id cannot be null");
		if (activityId == null) throw new IllegalArgumentException("Activity id cannot be null");					
		String url = createEntityUri(ENTITY_URI_PATH, entityId) + GENERATED_BY_SUFFIX;
		if(activityId != null) 
			url += "?" + PARAM_GENERATED_BY + "=" + activityId;
		try {
			JSONObject jsonObject = new JSONObject(); // no need for a body
			jsonObject = putJSONObject(url, jsonObject, null);
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObject);
			return new Activity(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Delete the generatedBy relationship for an Entity (does not delete the activity)
	 * @param entityId
	 * @throws SynapseException
	 */
	public void deleteGeneratedByForEntity(String entityId) throws SynapseException {
		if (entityId == null) throw new IllegalArgumentException("Entity id cannot be null");
		String uri = createEntityUri(ENTITY_URI_PATH, entityId) + GENERATED_BY_SUFFIX;
		deleteUri(uri);
	}

	/**
	 * Get activity by id
	 * @param activityId
	 * @return
	 * @throws SynapseException
	 */
	public Activity getActivity(String activityId) throws SynapseException {
		if (activityId == null) throw new IllegalArgumentException("Activity id cannot be null");
		String url = createEntityUri(ACTIVITY_URI_PATH, activityId);		
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			return new Activity(adapter);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}

	/**
	 * Update an activity
	 * @param activity
	 * @return
	 * @throws SynapseException
	 */
	public Activity putActivity(Activity activity) throws SynapseException {
		if (activity == null) throw new IllegalArgumentException("Activity can not be null");
		String url = createEntityUri(ACTIVITY_URI_PATH, activity.getId());		
		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		try {
			obj = new JSONObject(activity.writeToJSONObject(toUpdateAdapter).toJSONString());
			JSONObject jsonObj = putJSONObject(url, obj, new HashMap<String,String>());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			return new Activity(adapter);
		} catch (JSONException e1) {
			throw new RuntimeException(e1);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}

	/**
	 * Delete an activity. This will remove all generatedBy connections to this activity as well.
	 * @param activityId
	 * @throws SynapseException
	 */
	public void deleteActivity(String activityId) throws SynapseException {
		if (activityId == null) throw new IllegalArgumentException("Activity id cannot be null");
		String uri = createEntityUri(ACTIVITY_URI_PATH, activityId);
		deleteUri(uri);
	}
	
	public PaginatedResults<Reference> getEntitiesGeneratedBy(String activityId, Integer limit, Integer offset) throws SynapseException {
		if (activityId == null) throw new IllegalArgumentException("Activity id cannot be null");
		String url = createEntityUri(ACTIVITY_URI_PATH, activityId + GENERATED_PATH +
				"?" + OFFSET + "=" + offset + "&limit=" + limit);
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Reference> results = new PaginatedResults<Reference>(Reference.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
		
	}
	
	/**
	 * 
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	public PaginatedResults<StorageUsage> getItemizedStorageUsageForNode(String entityId, int offset, int limit) throws SynapseException {
		if (entityId == null) {
			throw new IllegalArgumentException("EntityId cannot be null");
		}
		String url = createEntityUri(STORAGE_DETAILS_PATH, entityId +
				"?" + OFFSET + "=" + offset + "&limit=" + limit);
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<StorageUsage> results = new PaginatedResults<StorageUsage>(StorageUsage.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Make the given versionNumber the most recent version of this entity.
	 * @param entityId
	 * @param versionNumber
	 * @throws SynapseException
	 */
	public VersionInfo promoteEntityVersion(String entityId, Long versionNumber) throws SynapseException {
		if (entityId == null) throw new IllegalArgumentException("EntityId cannot be null");
		if (versionNumber == null) throw new IllegalArgumentException("VersionNumber cannot be null");
		String uri = createEntityUri(ENTITY_URI_PATH, entityId) + "/promoteVersion/" + versionNumber;
		JSONObject jsonObj = signAndDispatchSynapseRequest(repoEndpoint, uri, "POST", null, defaultPOSTPUTHeaders);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		VersionInfo info = new VersionInfo();
		try {
			info.initializeFromJSONObject(adapter);
			return info;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	/**
	 * Gets the paginated list of descendants for the specified node.
	 *
	 * @param lastDescIdExcl
	 *            Paging delimiter. The last descendant ID (exclusive).
	 * @throws SynapseException 
	 */
	public EntityIdList getDescendants(String nodeId, int pageSize, String lastDescIdExcl)
			throws SynapseException {
		StringBuilder url = new StringBuilder()
				.append(ENTITY_URI_PATH)
				.append("/")
				.append(nodeId)
				.append("/descendants")
				.append("?")
				.append(LIMIT)
				.append("=")
				.append(pageSize);
		if (lastDescIdExcl != null) {
			url.append("&").append("lastEntityId")
				.append("=").append(lastDescIdExcl);
		}
		JSONObject jsonObj = signAndDispatchSynapseRequest(repoEndpoint, url.toString(), "GET", null, defaultGETDELETEHeaders);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		EntityIdList idList = new EntityIdList();
		try {
			idList.initializeFromJSONObject(adapter);
			return idList;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Gets the paginated list of descendants of a particular generation for the specified node.
	 *
	 * @param generation
	 *            How many generations away from the node. Children are exactly 1 generation away.
	 * @param lastDescIdExcl
	 *            Paging delimiter. The last descendant ID (exclusive).
	 * @throws SynapseException 
	 */
	public EntityIdList getDescendants(String nodeId, int generation, int pageSize, String lastDescIdExcl)
			throws SynapseException {
		StringBuilder url = new StringBuilder()
				.append(ENTITY_URI_PATH)
				.append("/")
				.append(nodeId)
				.append("/descendants")
				.append("/")
				.append(generation)
				.append("?")
				.append(LIMIT)
				.append("=")
				.append(pageSize);
		if (lastDescIdExcl != null) {
			url.append("&").append("lastEntityId")
				.append("=").append(lastDescIdExcl);
		}
		JSONObject jsonObj = signAndDispatchSynapseRequest(repoEndpoint, url.toString(), "GET", null, defaultGETDELETEHeaders);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		EntityIdList idList = new EntityIdList();
		try {
			idList.initializeFromJSONObject(adapter);
			return idList;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public Evaluation createEvaluation(Evaluation eval) throws SynapseException {
		String uri = EVALUATION_URI_PATH;
		try {
			JSONObject jsonObj = EntityFactory.createJSONObjectForEntity(eval);
			jsonObj = createJSONObject(uri, jsonObj);
			return initializeFromJSONObject(jsonObj, Evaluation.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public Evaluation getEvaluation(String evalId) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = createEntityUri(EVALUATION_URI_PATH, evalId);		
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			return new Evaluation(adapter);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}
	
	public PaginatedResults<Evaluation> getEvaluationsPaginated(int offset, int limit) throws SynapseException {
		String url = EVALUATION_URI_PATH +	"?" + OFFSET + "=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Evaluation> results = new PaginatedResults<Evaluation>(Evaluation.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public Long getEvaluationCount() throws SynapseException {
		PaginatedResults<Evaluation> res = getEvaluationsPaginated(0,0);
		return res.getTotalNumberOfResults();
	}
	
	public Evaluation findEvaluation(String name) throws SynapseException, UnsupportedEncodingException {
		if (name == null) throw new IllegalArgumentException("Evaluation name cannot be null");
		String encodedName = URLEncoder.encode(name, "UTF-8");
		String url = EVALUATION_URI_PATH + "/" + NAME + "/" + encodedName;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			return new Evaluation(adapter);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}
	
	public Evaluation updateEvaluation(Evaluation eval) throws SynapseException {
		if (eval == null) throw new IllegalArgumentException("Evaluation can not be null");
		String url = createEntityUri(EVALUATION_URI_PATH, eval.getId());		
		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		try {
			obj = new JSONObject(eval.writeToJSONObject(toUpdateAdapter).toJSONString());
			JSONObject jsonObj = putJSONObject(url, obj, new HashMap<String,String>());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			return new Evaluation(adapter);
		} catch (JSONException e1) {
			throw new RuntimeException(e1);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}
	
	public void deleteEvaluation(String evalId) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String uri = createEntityUri(EVALUATION_URI_PATH, evalId);
		deleteUri(uri);
	}
	
	/**
	 * Adds the authenticated user as a Participant in Evaluation evalId
	 */
	public Participant createParticipant(String evalId) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String uri = createEntityUri(EVALUATION_URI_PATH, evalId) + "/" + PARTICIPANT;
		JSONObject jsonObj = postUri(uri);
		return initializeFromJSONObject(jsonObj, Participant.class);
	}
	
	/**
	 * Adds a separate user as a Participant in Evaluation evalId.
	 */
	public Participant createParticipantAsAdmin(String evalId, String participantPrincipalId) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String uri = createEntityUri(EVALUATION_URI_PATH, evalId) + "/" + PARTICIPANT
				+ "/" + participantPrincipalId;
		JSONObject jsonObj = postUri(uri);
		return initializeFromJSONObject(jsonObj, Participant.class);
	}
	
	public Participant getParticipant(String evalId, String principalId) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		if (principalId == null) throw new IllegalArgumentException("Principal ID cannot be null");
		String uri = createEntityUri(EVALUATION_URI_PATH, evalId) + "/" + PARTICIPANT
				+ "/" + principalId;		
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			return new Participant(adapter);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}
	
	/**
	 * Removes user principalId from Evaluation evalId.
	 */
	public void deleteParticipant(String evalId, String principalId) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		if (principalId == null) throw new IllegalArgumentException("Principal ID cannot be null");
		String uri = createEntityUri(EVALUATION_URI_PATH, evalId) + "/" + PARTICIPANT
				+ "/" + principalId;
		deleteUri(uri);
	}
	
	public PaginatedResults<Participant> getAllParticipants(String evalId, long offset, long limit) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH +	"/" + evalId + "/" + PARTICIPANT +
				"?" + OFFSET + "=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Participant> results = new PaginatedResults<Participant>(Participant.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public Long getParticipantCount(String evalId) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		PaginatedResults<Participant> res = getAllParticipants(evalId, 0, 0);
		return res.getTotalNumberOfResults();
	}
	
	public Submission createSubmission(Submission sub) throws SynapseException {
		String uri = EVALUATION_URI_PATH + "/" + SUBMISSION;
		try {
			JSONObject jsonObj = EntityFactory.createJSONObjectForEntity(sub);
			jsonObj = createJSONObject(uri, jsonObj);
			return initializeFromJSONObject(jsonObj, Submission.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public Submission getSubmission(String subId) throws SynapseException {
		if (subId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH + "/" + SUBMISSION + "/" + subId;		
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			return new Submission(adapter);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}
	
	public SubmissionStatus getSubmissionStatus(String subId) throws SynapseException {
		if (subId == null) throw new IllegalArgumentException("Submission id cannot be null");
		String url = EVALUATION_URI_PATH + "/" + SUBMISSION + "/" + subId + "/" + STATUS;		
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			return new SubmissionStatus(adapter);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}
	
	public SubmissionStatus updateSubmissionStatus(SubmissionStatus status) throws SynapseException {
		if (status == null) throw new IllegalArgumentException("SubmissionStatus  cannot be null");
		String url = EVALUATION_URI_PATH + "/" + SUBMISSION + "/" + status.getId() + "/" + STATUS;			
		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		try {
			obj = new JSONObject(status.writeToJSONObject(toUpdateAdapter).toJSONString());
			JSONObject jsonObj = putJSONObject(url, obj, new HashMap<String,String>());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			return new SubmissionStatus(adapter);
		} catch (JSONException e1) {
			throw new RuntimeException(e1);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}
	
	public void deleteSubmission(String subId) throws SynapseException {
		if (subId == null) throw new IllegalArgumentException("Submission id cannot be null");
		String uri = EVALUATION_URI_PATH + "/" + SUBMISSION + "/" + subId;			
		deleteUri(uri);
	}
	
	public PaginatedResults<Submission> getAllSubmissions(String evalId, long offset, long limit) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH +	"/" + evalId + "/" + SUBMISSION_ALL +
				"?offset" + "=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Submission> results = new PaginatedResults<Submission>(Submission.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundles(String evalId, long offset, long limit) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH +	"/" + evalId + "/" + SUBMISSION_ALL_BUNDLE +
				"?offset" + "=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<SubmissionBundle> results = new PaginatedResults<SubmissionBundle>(SubmissionBundle.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public PaginatedResults<Submission> getAllSubmissionsByStatus(
			String evalId, SubmissionStatusEnum status, long offset, long limit) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH +	"/" + evalId + "/" + SUBMISSION_ALL + 
				STATUS_SUFFIX + status.toString() + "&offset=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Submission> results = new PaginatedResults<Submission>(Submission.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundlesByStatus(
			String evalId, SubmissionStatusEnum status, long offset, long limit) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH +	"/" + evalId + "/" + SUBMISSION_ALL_BUNDLE + 
				STATUS_SUFFIX + status.toString() + "&offset=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<SubmissionBundle> results = new PaginatedResults<SubmissionBundle>(SubmissionBundle.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public PaginatedResults<Submission> getMySubmissions(String evalId, long offset, long limit) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH +	"/" + evalId + "/" + SUBMISSION +
				"?offset" + "=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Submission> results = new PaginatedResults<Submission>(Submission.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public PaginatedResults<SubmissionBundle> getMySubmissionBundles(String evalId, long offset, long limit) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH +	"/" + evalId + "/" + SUBMISSION_BUNDLE +
				"?offset" + "=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<SubmissionBundle> results = new PaginatedResults<SubmissionBundle>(SubmissionBundle.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}	
	
	public Long getSubmissionCount(String evalId) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		PaginatedResults<Submission> res = getAllSubmissions(evalId, 0, 0);
		return res.getTotalNumberOfResults();
	}

	/**
	 * Moves an entity and its descendants to the trash can.
	 *
	 * @param entityId The ID of the entity to be moved to the trash can
	 */
	public void moveToTrash(String entityId) throws SynapseException {
		if (entityId == null || entityId.isEmpty()) {
			throw new IllegalArgumentException("Must provide an Entity ID.");
		}
		String url = TRASHCAN_TRASH + "/" +entityId;
		signAndDispatchSynapseRequest(repoEndpoint, url, "PUT", null, defaultPOSTPUTHeaders);
	}

	/**
	 * Moves an entity and its descendants out of the trash can. The entity will be restored
	 * to the specified parent. If the parent is not specified, it will be restored to the
	 * original parent.
	 */
	public void restoreFromTrash(String entityId, String newParentId) throws SynapseException {
		if (entityId == null || entityId.isEmpty()) {
			throw new IllegalArgumentException("Must provide an Entity ID.");
		}
		String url = TRASHCAN_RESTORE + "/" + entityId;
		if (newParentId != null && !newParentId.isEmpty()) {
			url = url + "/" + newParentId;
		}
		signAndDispatchSynapseRequest(repoEndpoint, url, "PUT", null, defaultPOSTPUTHeaders);
	}

	/**
	 * Retrieves entities (in the trash can) deleted by the user.
	 */
	public PaginatedResults<TrashedEntity> viewTrash(long offset, long limit) throws SynapseException {
		String url = TRASHCAN_VIEW + "?" + OFFSET + "=" + offset + "&" + LIMIT + "=" + limit;
		JSONObject jsonObj = signAndDispatchSynapseRequest(
				repoEndpoint, url, "GET", null, defaultGETDELETEHeaders);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<TrashedEntity> results = new PaginatedResults<TrashedEntity>(TrashedEntity.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Purges the specified entity from the trash can. After purging, the entity will be permanently deleted.
	 */
	public void purge(String entityId) throws SynapseException {
		if (entityId == null || entityId.isEmpty()) {
			throw new IllegalArgumentException("Must provide an Entity ID.");
		}
		String url = TRASHCAN_PURGE + "/" + entityId;
		signAndDispatchSynapseRequest(repoEndpoint, url, "PUT", null, defaultPOSTPUTHeaders);
	}

	/**
	 * Purges the trash can for the user. All the entities in the trash will be permanently deleted.
	 */
	public void purge() throws SynapseException {
		signAndDispatchSynapseRequest(repoEndpoint, TRASHCAN_PURGE, "PUT", null, defaultPOSTPUTHeaders);
	}
}
