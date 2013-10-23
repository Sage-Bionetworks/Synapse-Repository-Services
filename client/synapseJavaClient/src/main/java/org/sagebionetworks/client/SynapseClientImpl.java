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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.evaluation.model.UserEvaluationState;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AutoGenFactory;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.ChangeUserPassword;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.S3Token;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.ServiceConstants.AttachmentType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.VariableContentPaginatedResults;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.annotation.AnnotationsUtils;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.attachment.URLStatus;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.RegistrationInfo;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteAllChunksRequest;
import org.sagebionetworks.repo.model.file.CompleteChunkedFileRequest;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.State;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.query.QueryTableResults;
import org.sagebionetworks.repo.model.request.ReferenceList;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
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
public class SynapseClientImpl implements SynapseClient {

	public static final String SYNPASE_JAVA_CLIENT = "Synpase-Java-Client/";

	public static final String USER_AGENT = "User-Agent";

	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

	private static final Logger log = LogManager.getLogger(SynapseClientImpl.class.getName());
	
	private static final long MAX_UPLOAD_DAEMON_MS = 60*1000;

	private static final int JSON_INDENT = 2;
	private static final String DEFAULT_REPO_ENDPOINT = "https://repo-prod.prod.sagebase.org/repo/v1";
	private static final String DEFAULT_AUTH_ENDPOINT = "https://repo-prod.prod.sagebase.org/auth/v1";
	private static final String DEFAULT_FILE_ENDPOINT = "https://repo-prod.prod.sagebase.org/file/v1";
	private static final String SESSION_TOKEN_HEADER = "sessionToken";
	private static final String REQUEST_PROFILE_DATA = "profile_request";
	private static final String PROFILE_RESPONSE_OBJECT_HEADER = "profile_response_object";

	private static final String PASSWORD_FIELD = "password";
	private static final String PARAM_GENERATED_BY = "generatedBy";
	
	private static final String QUERY_URI = "/query?query=";
	private static final String REPO_SUFFIX_VERSION = "/version";
	private static final String ANNOTATION_URI_SUFFIX = "annotations";
	protected static final String ADMIN = "/admin";
	protected static final String STACK_STATUS = ADMIN + "/synapse/status";
	private static final String ENTITY = "/entity";
	private static final String ATTACHMENT_S3_TOKEN = "/s3AttachmentToken";
	private static final String ATTACHMENT_URL = "/attachmentUrl";
	private static final String GENERATED_BY_SUFFIX = "/generatedBy";

	private static final String ENTITY_URI_PATH = "/entity";
	private static final String ENTITY_ACL_PATH_SUFFIX = "/acl";
	private static final String ENTITY_ACL_RECURSIVE_SUFFIX = "?recursive=true";
	private static final String ENTITY_BUNDLE_PATH = "/bundle?mask=";
	private static final String BUNDLE = "/bundle";
	private static final String BENEFACTOR = "/benefactor"; // from org.sagebionetworks.repo.web.UrlHelpers
	private static final String ACTIVITY_URI_PATH = "/activity";
	private static final String GENERATED_PATH = "/generated";
	private static final String FAVORITE_URI_PATH = "/favorite";
	
	private static final String WIKI_URI_TEMPLATE = "/%1$s/%2$s/wiki";
	private static final String WIKI_ID_URI_TEMPLATE = "/%1$s/%2$s/wiki/%3$s";
	private static final String WIKI_TREE_URI_TEMPLATE = "/%1$s/%2$s/wikiheadertree";
	private static final String ATTACHMENT_HANDLES = "/attachmenthandles";
	private static final String ATTACHMENT_FILE = "/attachment";
	private static final String ATTACHMENT_FILE_PREVIEW = "/attachmentpreview";
	private static final String FILE_NAME_PARAMETER = "?fileName=";
	private static final String REDIRECT_PARAMETER = "redirect=";
	private static final String AND_REDIRECT_PARAMETER = "&"+REDIRECT_PARAMETER;
	private static final String QUERY_REDIRECT_PARAMETER = "?"+REDIRECT_PARAMETER;

	private static final String EVALUATION_URI_PATH = "/evaluation";
	private static final String AVAILABLE_EVALUATION_URI_PATH = "/evaluation/available";
	private static final String NAME = "name";
	private static final String ALL = "/all";
	private static final String STATUS = "/status";
	private static final String PARTICIPANT = "participant";
	private static final String LOCK_ACCESS_REQUIREMENT = "/lockAccessRequirement";
	private static final String SUBMISSION = "submission";
	private static final String SUBMISSION_BUNDLE = SUBMISSION + BUNDLE;
	private static final String SUBMISSION_ALL = SUBMISSION + ALL;
	private static final String SUBMISSION_STATUS_ALL = SUBMISSION + STATUS + ALL;
	private static final String SUBMISSION_BUNDLE_ALL = SUBMISSION + BUNDLE + ALL;	
	private static final String STATUS_SUFFIX = "?status=";
	private static final String EVALUATION_ACL_URI_PATH = "/evaluation/acl";
	private static final String EVALUATION_QUERY_URI_PATH = EVALUATION_URI_PATH + "/" + SUBMISSION + QUERY_URI;
	
	protected static final String COLUMN = "/column";

	private static final String USER_PROFILE_PATH = "/userProfile";
	
	private static final String USER_GROUP_HEADER_BATCH_PATH = "/userGroupHeaders/batch?ids=";
	
	private static final String USER_GROUP_HEADER_PREFIX_PATH = "/userGroupHeaders?prefix=";

	private static final String TOTAL_NUM_RESULTS = "totalNumberOfResults";
	
	private static final String ACCESS_REQUIREMENT = "/accessRequirement";
	
	private static final String ACCESS_REQUIREMENT_UNFULFILLED = "/accessRequirementUnfulfilled";
	
	private static final String ACCESS_APPROVAL = "/accessApproval";
	
	private static final String VERSION_INFO = "/version";
	
	private static final String FILE_HANDLE = "/fileHandle";
	private static final String FILE = "/file";
	private static final String FILE_PREVIEW = "/filepreview";
	private static final String EXTERNAL_FILE_HANDLE = "/externalFileHandle";
	private static final String FILE_HANDLES = "/filehandles";
	
	private static final String CREATE_CHUNKED_FILE_UPLOAD_TOKEN = "/createChunkedFileUploadToken";
	private static final String CREATE_CHUNKED_FILE_UPLOAD_CHUNK_URL = "/createChunkedFileUploadChunkURL";
	private static final String ADD_CHUNK_TO_FILE = "/addChunkToFile";
	private static final String COMPLETE_CHUNK_FILE_UPLOAD = "/completeChunkFileUpload";
	private static final String START_COMPLETE_UPLOAD_DAEMON = "/startCompleteUploadDaemon";
	private static final String COMPLETE_UPLOAD_DAEMON_STATUS = "/completeUploadDaemonStatus" ;
	
	private static final String TRASHCAN_TRASH = "/trashcan/trash";
	private static final String TRASHCAN_RESTORE = "/trashcan/restore";
	private static final String TRASHCAN_VIEW = "/trashcan/view";
	private static final String TRASHCAN_PURGE = "/trashcan/purge";

	private static final String DOI = "/doi";
	
	private static final String ETAG = "etag";

	// web request pagination parameters
	public static final String LIMIT = "limit";
	public static final String OFFSET = "offset";

	private static final String LIMIT_1_OFFSET_1 = "' limit 1 offset 1";
	private static final String SELECT_ID_FROM_ENTITY_WHERE_PARENT_ID = "select id from entity where parentId == '";

	// Team
	protected static final String TEAM = "/team";
	protected static final String TEAMS = "/teams";
	protected static final String USER = "/user";
	protected static final String NAME_FRAGMENT_FILTER = "fragment";
	protected static final String ICON = "/icon";
	protected static final String TEAM_MEMBERS = "/teamMembers";
	protected static final String MEMBER = "/member";
	protected static final String PERMISSION = "/permission";
	protected static final String MEMBERSHIP_STATUS = "/membershipStatus";
	protected static final String TEAM_MEMBERSHIP_PERMISSION = "isAdmin";
	protected static final String TEAM_UPDATE_SEARCH_CACHE = "/updateTeamSearchCache";

	
	// membership invitation
	private static final String MEMBERSHIP_INVITATION = "/membershipInvitation";
	private static final String OPEN_MEMBERSHIP_INVITATION = "/openInvitation";
	private static final String TEAM_ID_REQUEST_PARAMETER = "teamId";
	// membership request
	private static final String MEMBERSHIP_REQUEST = "/membershipRequest";
	private static final String OPEN_MEMBERSHIP_REQUEST = "/openRequest";
	private static final String REQUESTOR_ID_REQUEST_PARAMETER = "requestorId";

	
	protected String repoEndpoint;
	protected String authEndpoint;
	protected String fileEndpoint;

	protected Map<String, String> defaultGETDELETEHeaders;
	protected Map<String, String> defaultPOSTPUTHeaders;

	private JSONObject profileData;
	private boolean requestProfile;
	private HttpClientProvider clientProvider;
	private DataUploader dataUploader;

	private AutoGenFactory autoGenFactory = new AutoGenFactory();
	
	/**
	 * The maximum number of threads that should be used to upload asynchronous file chunks.
	 */
	private static final int MAX_NUMBER_OF_THREADS = 2;
	
	/**
	 * This thread pool is used for asynchronous file chunk uploads.
	 */
	private ExecutorService fileUplaodthreadPool = Executors.newFixedThreadPool(MAX_NUMBER_OF_THREADS);
	
	/**
	 * Note: 5 MB is currently the minimum size of a single part of S3 Multi-part upload, so any file chunk must be at
	 * least this size.
	 */
	public static final int MINIMUM_CHUNK_SIZE_BYTES = ((int) Math.pow(2, 20))*5;
	
	/**
	 * Default constructor uses the default repository and auth services
	 * endpoints.
	 */
	public SynapseClientImpl() {
		// Use the default implementations
		this(new HttpClientProviderImpl(), new DataUploaderMultipartImpl());
	}

	/**
	 * Will use the provided client provider and data uploader.
	 * 
	 * @param clientProvider 
	 * @param dataUploader 
	 */
	public SynapseClientImpl(HttpClientProvider clientProvider, DataUploader dataUploader) {
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
		
		// Setup the user agent
		String userAgent = SYNPASE_JAVA_CLIENT+ClientVersionInfo.getClientVersionInfo();
		setUserAgent(userAgent);
		
		this.clientProvider = clientProvider;
		clientProvider.setGlobalConnectionTimeout(ServiceConstants.DEFAULT_CONNECT_TIMEOUT_MSEC);
		clientProvider.setGlobalSocketTimeout(ServiceConstants.DEFAULT_SOCKET_TIMEOUT_MSEC);
		
		this.dataUploader = dataUploader;
		
		requestProfile = false;
	}
	
	/**
	 * Each request includes the 'User-Agent' header. This is set to:
	 * 'User-Agent':'Synpase-Java-Client/<version_number>'
	 * Addition User-Agent information can be appended to this string by calling this method.
	 * @param toAppend
	 */
	@Override
	public void appendUserAgent(String toAppend){
		String currentUserAgent = defaultGETDELETEHeaders.get(USER_AGENT);
		if(currentUserAgent == null) throw new RuntimeException("User-Agent header is missing");
		// Only append if it is not already there
		if(currentUserAgent.indexOf(toAppend) < 0){
			StringBuilder builder = new StringBuilder();
			builder.append(currentUserAgent);
			builder.append("  ");
			builder.append(toAppend);
			setUserAgent(builder.toString());
		}
	}
	
	/**
	 * Set the User-Agent header. See http://en.wikipedia.org/wiki/List_of_HTTP_header_fields
	 * This should not be public. The caller can append to User-Agent (see {@link#appendUserAgent(toAppend)})
	 * but they cannot override it.
	 * @param userAgent
	 */
	private void setUserAgent(String userAgent){
		defaultGETDELETEHeaders.put(USER_AGENT, userAgent);
		defaultPOSTPUTHeaders.put(USER_AGENT, userAgent);
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
	@Override
	public void setRepositoryEndpoint(String repoEndpoint) {
		this.repoEndpoint = repoEndpoint;
	}

	/**
	 * Get the configured Repository Service Endpoint
	 * @return
	 */
	@Override
	public String getRepoEndpoint() {
		return repoEndpoint;
	}

	/**
	 * @param authEndpoint
	 *            the authEndpoint to set
	 */
	@Override
	public void setAuthEndpoint(String authEndpoint) {
		this.authEndpoint = authEndpoint;
	}

	/**
	 * Get the configured Authorization Service Endpoint
	 * @return
	 */
	@Override
	public String getAuthEndpoint() {
		return authEndpoint;
	}
	
	/**
	 * @param fileEndpoint
	 *            the authEndpoint to set
	 */
	@Override
	public void setFileEndpoint(String fileEndpoint) {
		this.fileEndpoint = fileEndpoint;
	}
	
	/**
	 * The endpoint used for file multi-part upload.
	 * 
	 * @return
	 */
	@Override
	public String getFileEndpoint(){
		return this.fileEndpoint;
	}
	/**
	 * @param request
	 */
	@Override
	public void setRequestProfile(boolean request) {
		this.requestProfile = request;
	}

	/**
	 * @return JSONObject
	 */
	@Override
	public JSONObject getProfileData() {
		return this.profileData;
	}
	
	private String userName;
	private String apiKey;
	

	/**
	 * @return the userName
	 */
	@Override
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	@Override
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the apiKey
	 */
	@Override
	public String getApiKey() {
		return apiKey;
	}

	/**
	 * @param apiKey the apiKey to set
	 */
	@Override
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
	@Override
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
	@Override
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
	
	/**
	 * 
	 * Log into Synapse, do not return UserSessionData, do not request user profile, do not explicitely accept terms of use
	 * 
	 * @param userName
	 * @param password
	 * @throws SynapseException 
	 */
	@Override
	public void loginWithNoProfile(String userName, String password) throws SynapseException {
		JSONObject loginRequest = new JSONObject();
		JSONObject credentials = null;
		try {
			loginRequest.put("email", userName);
			loginRequest.put(PASSWORD_FIELD, password);
			
			credentials = createAuthEntity("/session", loginRequest);
			String sessionToken = credentials.getString(SESSION_TOKEN_HEADER);
			defaultGETDELETEHeaders.put(SESSION_TOKEN_HEADER, sessionToken);
			defaultPOSTPUTHeaders.put(SESSION_TOKEN_HEADER, sessionToken);

		} catch (JSONException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public void logout() throws SynapseException {
		deleteUri(authEndpoint, "/session");
		defaultGETDELETEHeaders.remove(SESSION_TOKEN_HEADER);
		defaultPOSTPUTHeaders.remove(SESSION_TOKEN_HEADER);
	}
	
	@Override
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
	
	@Override
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
	@Override
	public void setSessionToken(String sessionToken) {
		defaultGETDELETEHeaders.put(SESSION_TOKEN_HEADER, sessionToken);
		defaultPOSTPUTHeaders.put(SESSION_TOKEN_HEADER, sessionToken);
	}

	/**
	 * Get the current session token used by this client.
	 * 
	 * @return the session token
	 */
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	 * Get an entity using its ID.
	 * @param entityId
	 * @return the entity
	 * @throws SynapseException
	 */
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	
	@Override
	public AccessControlList getACL(String entityId) throws SynapseException {
		String uri = ENTITY_URI_PATH + "/" + entityId+ ENTITY_ACL_PATH_SUFFIX;
		JSONObject json = getEntity(uri);
		return initializeFromJSONObject(json, AccessControlList.class);
	}
	
	@Override
	public EntityHeader getEntityBenefactor(String entityId) throws SynapseException {
		String uri = ENTITY_URI_PATH + "/" + entityId+ BENEFACTOR;
		JSONObject json = getEntity(uri);
		return initializeFromJSONObject(json, EntityHeader.class);
	}
	
	@Override
	public UserProfile getMyProfile() throws SynapseException {
		String uri = USER_PROFILE_PATH;
		JSONObject json = getEntity(uri);
		return initializeFromJSONObject(json, UserProfile.class);
	}
	@Override
	public void updateMyProfile(UserProfile userProfile) throws SynapseException {
		try {
			String uri = USER_PROFILE_PATH;
			Map<String,String> headers = new HashMap<String, String>();
			putJSONObject(uri, EntityFactory.createJSONObjectForEntity(userProfile), headers);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	@Override
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
	@Override
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
	@Override
	public UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(String prefix) throws SynapseException, UnsupportedEncodingException {
		String encodedPrefix = URLEncoder.encode(prefix, "UTF-8");
		JSONObject json = getEntity(USER_GROUP_HEADER_PREFIX_PATH+encodedPrefix);
		return initializeFromJSONObject(json, UserGroupHeaderResponsePage.class);
	}
	
	/**
	 * Update an ACL. Default to non-recursive application.
	 */
	@Override
	public AccessControlList updateACL(AccessControlList acl) throws SynapseException {
		return updateACL(acl, false);
	}
	
	/**
	 * Update an entity's ACL. If 'recursive' is set to true, then any child 
	 * ACLs will be deleted, such that all child entities inherit this ACL. 
	 */
	@Override
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
	
	@Override
	public void deleteACL(String entityId) throws SynapseException {
		String uri = ENTITY_URI_PATH + "/" + entityId+ ENTITY_ACL_PATH_SUFFIX;
		deleteUri(uri);
	}
	
	@Override
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
	
	@Override
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

	@Override
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
	@Override
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
	@Override
	public boolean canAccess(String entityId, ACCESS_TYPE accessType) throws SynapseException {
		return canAccess(entityId, ObjectType.ENTITY, accessType);
	}
	@Override
	public boolean canAccess(String id, ObjectType type, ACCESS_TYPE accessType) throws SynapseException{
		if(id == null) throw new IllegalArgumentException("id cannot be null");
		if (type == null) throw new IllegalArgumentException("ObjectType cannot be null");
		if (accessType == null) throw new IllegalArgumentException("AccessType cannot be null");
		
		if (ObjectType.ENTITY.equals(type)) {
			return canAccess(ENTITY_URI_PATH + "/" + id+ "/access?accessType="+accessType.name());
		}
		else if (ObjectType.EVALUATION.equals(type)) {
			return canAccess(EVALUATION_URI_PATH + "/" + id+ "/access?accessType="+accessType.name());
		}
		else
			throw new IllegalArgumentException("ObjectType not supported: " + type.toString());
	}
	
	private boolean canAccess(String serviceUrl) throws SynapseException {
		try {
			JSONObject jsonObj = getEntity(serviceUrl);
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
	@Override
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
	@Override
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
	
	@SuppressWarnings("unchecked")
	private static Class<AccessRequirement> getAccessRequirementClassFromType(String s) {
		try {
			return (Class<AccessRequirement>)Class.forName(s);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	@SuppressWarnings("unchecked")
	@Override
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
	@Override
	public ACTAccessRequirement createLockAccessRequirement(String entityId) throws SynapseException {
		if (entityId == null) throw new IllegalArgumentException("Entity id cannot be null");
		JSONObject jsonObj = postUri(ENTITY+"/"+entityId+LOCK_ACCESS_REQUIREMENT);
		return initializeFromJSONObject(jsonObj, ACTAccessRequirement.class);
	}

	@Override
	public void deleteAccessRequirement(Long arId) throws SynapseException {
		deleteUri(ACCESS_REQUIREMENT+"/"+arId);
	}
	@Override
	public VariableContentPaginatedResults<AccessRequirement> getUnmetAccessRequirements(RestrictableObjectDescriptor subjectId) throws SynapseException {
		String uri = null;
		if (RestrictableObjectType.ENTITY == subjectId.getType()) {
			uri = ENTITY+"/"+subjectId.getId()+ACCESS_REQUIREMENT_UNFULFILLED;
		} else if (RestrictableObjectType.EVALUATION == subjectId.getType()) {
			uri = EVALUATION_URI_PATH+"/"+subjectId.getId()+ACCESS_REQUIREMENT_UNFULFILLED;
		} else {
			throw new SynapseException("Unsupported type "+subjectId.getType());
		}
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
	
	@Override
	public VariableContentPaginatedResults<AccessRequirement> getAccessRequirements(RestrictableObjectDescriptor subjectId) throws SynapseException {
		String uri = null;
		if (RestrictableObjectType.ENTITY == subjectId.getType()) {
			uri = ENTITY+"/"+subjectId.getId()+ACCESS_REQUIREMENT;
		} else if (RestrictableObjectType.EVALUATION == subjectId.getType()) {
			uri = EVALUATION_URI_PATH+"/"+subjectId.getId()+ACCESS_REQUIREMENT;
		} else {
			throw new SynapseException("Unsupported type "+subjectId.getType());
		}
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

	@SuppressWarnings("unchecked")
	private static Class<AccessApproval> getAccessApprovalClassFromType(String s) {
		try {
			return (Class<AccessApproval>)Class.forName(s);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
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
	 * Get a dataset, layer, preview, annotations, etc...
	 * 
	 * @return the retrieved entity
	 */
	@Override
	public JSONObject getEntity(String uri) throws SynapseException {
		return getSynapseEntity(repoEndpoint, uri);
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
	@Override
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
	 * @param <T>
	 * @param entity
	 * @return the updated entity
	 * @throws SynapseException
	 */
	@Override
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
	@Override
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
	 * Update a dataset, layer, preview, annotations, etc...
	 * 
	 * @return the updated entity
	 */
	private JSONObject putJSONObject(String uri, JSONObject entity, Map<String,String> headers)
			throws SynapseException {
		return putJSONObject(repoEndpoint, uri, entity, headers);
	}
	
	/**
	 * Create a dataset, layer, etc...
	 */
	protected JSONObject postUri(String uri) throws SynapseException {
		return postUri(repoEndpoint, uri);
	}


	/**
	 * Delete a dataset, layer, etc...
	 */
	private void deleteUri(String uri) throws SynapseException {
		deleteUri(repoEndpoint, uri);
		return;
	}

	/**
	 * Deletes a dataset, layer, etc.. This only moves the entity
	 * to the trash can.  To permanently delete the entity, use
	 * deleteAndPurgeEntity().
	 * 
	 * @param <T>
	 * @param entity
	 * @throws SynapseException
	 */
	@Override
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
	@Override
	public <T extends Entity> void deleteAndPurgeEntity(T entity)
			throws SynapseException {
		deleteEntity(entity);
		purgeTrashForUser(entity.getId());
	}

	/**
	 * Delete a dataset, layer, etc.. This only moves the entity
	 * to the trash can.  To permanently delete the entity, use
	 * deleteAndPurgeEntity().
	 * 
	 * @param <T>
	 * @param entity
	 * @throws SynapseException
	 */
	@Override
	public void deleteEntityById(String entityId)
			throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("entityId cannot be null");
		String uri = createEntityUri(ENTITY_URI_PATH, entityId);
		deleteUri(uri);
	}

	/**
	 * Delete a dataset, layer, etc..
	 * 
	 * @param <T>
	 * @param entity
	 * @throws SynapseException
	 */
	@Override
	public void deleteAndPurgeEntityById(String entityId)
			throws SynapseException {
		deleteEntityById(entityId);
		purgeTrashForUser(entityId);
	}

	@Override
	public <T extends Entity> void deleteEntityVersion(T entity, Long versionNumber) throws SynapseException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");
		deleteEntityVersionById(entity.getId(), versionNumber);
	}

	@Override
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
	@Override
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
	@Override
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

	@Override
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
	
	@Override
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
	@Override
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
	@Override
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
	@Override
	public JSONObject query(String query) throws SynapseException {
		return querySynapse(repoEndpoint, query);
	}
	
	/**
	 * Upload each file to Synapse creating a file handle for each.
	 * 
	 * @param files
	 * @return
	 * @throws InterruptedException 
	 * @throws JSONObjectAdapterException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Override
	public FileHandleResults createFileHandles(List<File> files) throws SynapseException{
		if(files == null) throw new IllegalArgumentException("File list cannot be null");
		try {
			List<FileHandle> list = new LinkedList<FileHandle>();
			for(File file: files){
				// We need to determine the content type of the file
				String contentType = guessContentTypeFromStream(file);
				S3FileHandle handle = createFileHandle(file, contentType);
				list.add(handle);
			}
			FileHandleResults results = new FileHandleResults();
			results.setList(list);
			return results;
		} 
		catch (IOException e) {
			throw new SynapseException(e);
		}	
	}
	
	/**
	 * The high-level API for uploading a file to Synapse.
	 * 
	 * @param file
	 * @param contentType
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException 
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	@Override
	public S3FileHandle createFileHandle(File file, String contentType) throws SynapseException, IOException{
		if(file == null) throw new IllegalArgumentException("File cannot be null");
		if(contentType == null) throw new IllegalArgumentException("Content type cannot be null");
		CreateChunkedFileTokenRequest ccftr = new CreateChunkedFileTokenRequest();
		ccftr.setContentType(contentType);
		ccftr.setFileName(file.getName());
		// Calculate the MD5
		String md5 = MD5ChecksumHelper.getMD5Checksum(file);
		ccftr.setContentMD5(md5);
		// Start the upload
		ChunkedFileToken token = createChunkedFileUploadToken(ccftr);
		// Now break the file into part as needed
		List<File> fileChunks = FileUtils.chunkFile(file, MINIMUM_CHUNK_SIZE_BYTES);
		try{
			// Upload all of the parts.
			List<Long> partNumbers = uploadChunks(fileChunks, token);
			// We can now complete the upload
			CompleteAllChunksRequest cacr = new CompleteAllChunksRequest();
			cacr.setChunkedFileToken(token);
			cacr.setChunkNumbers(partNumbers);
			// Start the daemon
			UploadDaemonStatus status = startUploadDeamon(cacr);
			// Wait for it to complete
			long start = System.currentTimeMillis();
			while(State.COMPLETED != status.getState()){
				// Check for failure
				if(State.FAILED == status.getState()){
					throw new SynapseException("Upload failed: "+status.getErrorMessage());
				}
				log.debug("Waiting for upload daemon: "+status.toString());
				Thread.sleep(1000);
				status = getCompleteUploadDaemonStatus(status.getDaemonId());
				if(System.currentTimeMillis() -start > MAX_UPLOAD_DAEMON_MS){
					throw new SynapseException("Timed out waiting for upload daemon: "+status.toString());
				}
			}
			// Complete the upload
			return (S3FileHandle) getRawFileHandle(status.getFileHandleId());
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}finally{
			// Delete any tmep files created by this method.  The original file will not be deleted.
			FileUtils.deleteAllFilesExcludingException(file, fileChunks);
		}
	}
	
	/**
	 * Upload all of the passed file chunks.
	 * 
	 * @param fileChunks
	 * @param token
	 * @return
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private List<Long> uploadChunks(List<File> fileChunks, ChunkedFileToken token) throws SynapseException{
		try{
			List<Long> results = new LinkedList<Long>();
			// The future list
			List<Future<Long>> futureList = new ArrayList<Future<Long>>();
			// For each chunk create a worker and add it to the thread pool
			long chunkNumber = 1;
			for(File file: fileChunks){
				// create a worker for each chunk
				ChunkRequest request = new ChunkRequest();
				request.setChunkedFileToken(token);
				request.setChunkNumber(chunkNumber);
				FileChunkUploadWorker worker = new FileChunkUploadWorker(this, request, file);
				// Add this the the thread pool
				Future<Long> future = fileUplaodthreadPool.submit(worker);
				futureList.add(future);
				chunkNumber++;
			}
			// Get all of the results
			for(Future<Long> future: futureList){
				Long partNumber = future.get();
				results.add(partNumber);
			}
			return results;
		} catch (Exception e) {
			throw new SynapseException(e);
		} 
	}
	
	/**
	 * <P>
	 * This is a low-level API call for uploading large files. We recomend using the high-level
	 * API call for uploading files {@link #createFileHandle(File, String)}.
	 * </P>
	 * This is the first step in the low-level API used to upload large files to Synapse. The
	 * resulting {@link ChunkedFileToken} is required for all subsequent steps.
	 * Large file upload is exectued as follows:
	 * <ol>
	 * <li>{@link #createChunkedFileUploadToken(CreateChunkedFileTokenRequest)}</li>
	 * <li>{@link #createChunkedPresignedUrl(ChunkRequest)}</li>
	 * <li>{@link #addChunkToFile(ChunkRequest)}</li>
	 * <li>{@link #completeChunkFileUpload(CompleteChunkedFileRequest)}</li>
	 * </ol>
	 * Steps 2 & 3 are repated in for each file chunk. Note: All chunks can be sent asynchronously.
	 * @param ccftr
	 * @return The @link {@link ChunkedFileToken} is required for all subsequent steps.
	 * @throws JSONObjectAdapterException 
	 * @throws SynapseException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Override
	public ChunkedFileToken createChunkedFileUploadToken(CreateChunkedFileTokenRequest ccftr) throws SynapseException{
		if(ccftr == null) throw new IllegalArgumentException("CreateChunkedFileTokenRequest cannot be null");
		if(ccftr.getFileName() == null) throw new IllegalArgumentException("FileName cannot be null");
		if(ccftr.getContentType() == null) throw new IllegalArgumentException("ContentType cannot be null");
		String url = getFileEndpoint()+CREATE_CHUNKED_FILE_UPLOAD_TOKEN;
		return asymmetricalPost(url, ccftr, ChunkedFileToken.class);
	}
	
	/**
	 * <P>
	 * This is a low-level API call for uploading large files. We recomend using the high-level
	 * API call for uploading files {@link #createFileHandle(File, String)}.
	 * </P>
	 * The second step in the low-level API used to upload large files to Synapse. This method is used to 
	 * get a pre-signed URL that can be used to PUT the data of a single chunk to S3.
	 * 
	 * @param chunkRequest
	 * @return
	 * @throws JSONObjectAdapterException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Override
	public URL createChunkedPresignedUrl(ChunkRequest chunkRequest) throws SynapseException {
		try {
			if(chunkRequest == null) throw new IllegalArgumentException("ChunkRequest cannot be null");
			String url = getFileEndpoint()+CREATE_CHUNKED_FILE_UPLOAD_CHUNK_URL;
			HttpPost post = createPost(url, chunkRequest);
			HttpResponse response;
			response = clientProvider.execute(post);
			String responseBody = (null != response.getEntity()) ? EntityUtils.toString(response.getEntity()) : null;
			return new URL(responseBody);
		} catch (ClientProtocolException e) {
			throw new SynapseException(e);
		} catch (IOException e) {
			throw new SynapseException(e);
		}
	}
	
	/**
	 * Put the contents of the passed file to the passed URL.
	 * @param url
	 * @param file
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Override
	public String putFileToURL(URL url, File file, String contentType) throws SynapseException{
		try{
			if(url == null) throw new IllegalArgumentException("URL cannot be null");
			if(file == null) throw new IllegalArgumentException("File cannot be null");
			HttpPut httppost = new HttpPut(url.toString());
			// There must not be any headers added or Amazon will return a 403.
			// Therefore, we must clear the content type.
			org.apache.http.entity.FileEntity fe = new org.apache.http.entity.FileEntity(file, contentType);
			httppost.setEntity(fe);
			HttpResponse response = clientProvider.execute(httppost);
			int code = response.getStatusLine().getStatusCode();
			if(code < 200 || code > 299){
				throw new SynapseException("Response code: "+code+" "+response.getStatusLine().getReasonPhrase()+" for "+url+" File: "+file.getName());
			}
			return EntityUtils.toString(response.getEntity());
		} catch (ClientProtocolException e) {
			throw new SynapseException(e);
		} catch (IOException e) {
			throw new SynapseException(e);
		}

	}
	/**
	 * <P>
	 * This is a low-level API call for uploading large files. We recomend using the high-level
	 * API call for uploading files {@link #createFileHandle(File, String)}.
	 * </P>
	 * 
	 * The thrid step in the low-level API used to upload large files to Synapse. After a chunk is PUT to 
	 * a pre-signed URL (see: {@link #createChunkedPresignedUrl(ChunkRequest)}, it must be added to the file.
	 * The resulting {@link ChunkResult} is required to complte the with {@link #completeChunkFileUpload(CompleteChunkedFileRequest)}.
	 * 
	 * @param chunkRequest
	 * @return
	 * @throws SynapseException 
	 */
	@Deprecated
	@Override
	public ChunkResult addChunkToFile(ChunkRequest chunkRequest) throws SynapseException{
		String url = getFileEndpoint()+ADD_CHUNK_TO_FILE;
		return asymmetricalPost(url, chunkRequest, ChunkResult.class);
	}
	
	/**
	 * <P>
	 * This is a low-level API call for uploading large files. We recomend using the high-level
	 * API call for uploading files {@link #createFileHandle(File, String)}.
	 * </P>
	 * 
	 * The final step in the low-level API used to upload large files to Synapse. After all of the chunks have
	 * been added to the file (see: {@link #addChunkToFile(ChunkRequest)}) the upload is complted by calling this method.
	 * 
	 * @param request
	 * @return Returns the resulting {@link S3FileHandle} that can be used for any opperation that accepts {@link FileHandle} objects.
	 * @throws SynapseException 
	 */
	@Deprecated
	@Override
	public S3FileHandle completeChunkFileUpload(CompleteChunkedFileRequest request) throws SynapseException{
		String url = getFileEndpoint()+COMPLETE_CHUNK_FILE_UPLOAD;
		return asymmetricalPost(url, request, S3FileHandle.class);
	}
	
	/**
	 * Start a daemon that will asycnrhounsously complete the multi-part upload.
	 * @param cacr
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public UploadDaemonStatus startUploadDeamon(CompleteAllChunksRequest cacr) throws SynapseException{
		String url = getFileEndpoint()+START_COMPLETE_UPLOAD_DAEMON;
		return asymmetricalPost(url, cacr, UploadDaemonStatus.class);
	}
	
	/**
	 * Get the status of daemon used to complete the multi-part upload.
	 * @param daemonId
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	@Override
	public UploadDaemonStatus getCompleteUploadDaemonStatus(String daemonId) throws SynapseException{
		String url = COMPLETE_UPLOAD_DAEMON_STATUS+"/"+daemonId;
		JSONObject json = getSynapseEntity(getFileEndpoint(), url);
		try {
			return EntityFactory.createEntityFromJSONObject(json, UploadDaemonStatus.class);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * Create an External File Handle.  This is used to references a file that is not stored in Synpase.
	 * @param efh
	 * @return
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	@Override
	public ExternalFileHandle createExternalFileHandle(ExternalFileHandle efh) throws JSONObjectAdapterException, SynapseException{
		String uri = EXTERNAL_FILE_HANDLE;
		return createJSONEntity(getFileEndpoint(), uri, efh);
	}
	
	/**
	 * Asymmetrical post where the request and response are not of the same type.
	 * 
	 * @param url
	 * @param reqeust
	 * @param calls
	 * @throws SynapseException 
	 */
	private <T extends JSONEntity> T asymmetricalPost(String url, JSONEntity requestBody, Class<? extends T> returnClass) throws SynapseException{
		HttpPost post;
		try {
			post = createPost(url, requestBody);
			HttpResponse response = clientProvider.execute(post);
			int code = response.getStatusLine().getStatusCode();
			String responseBody = (null != response.getEntity()) ? EntityUtils.toString(response.getEntity()) : null;
			if(code < 200 || code > 299){
				throw new SynapseException("Response code: "+code+" "+response.getStatusLine().getReasonPhrase()+" for "+url+" body: "+requestBody);
			}
			return EntityFactory.createEntityFromJSONString(responseBody, returnClass);
		} catch (UnsupportedEncodingException e) {
			throw new SynapseException(e);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		} catch (ClientProtocolException e) {
			throw new SynapseException(e);
		} catch (IOException e) {
			throw new SynapseException(e);
		}
	}
	
	/**
	 * Helper to create a post from an object.
	 * @param url
	 * @param body
	 * @return
	 * @throws JSONObjectAdapterException 
	 * @throws UnsupportedEncodingException 
	 */
	private HttpPost createPost(String url, JSONEntity body) throws SynapseException{
		try {
			HttpPost post = new HttpPost(url);
			for(String headerKey: this.defaultPOSTPUTHeaders.keySet()){
				String value = this.defaultPOSTPUTHeaders.get(headerKey);
				post.setHeader(headerKey, value);
			}
			StringEntity stringEntity = new StringEntity(EntityFactory.createJSONStringForEntity(body));
			post.setEntity(stringEntity);
			return post;
		} catch (UnsupportedEncodingException e) {
			throw new SynapseException(e);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	/**
	 * Get the raw file handle.
	 * Note: Only the creator of a the file handle can get the raw file handle.
	 * 
	 * @param fileHandleId
	 * @return
	 * @throws SynapseException 
	 */
	@Override
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
	@Override
	public void deleteFileHandle(String fileHandleId) throws SynapseException{
		signAndDispatchSynapseRequest(getFileEndpoint(), FILE_HANDLE+"/"+fileHandleId, "DELETE", null, defaultGETDELETEHeaders);
	}
	
	/**
	 * Delete the preview associated with the given file handle.
	 * Note: Only the creator of a the file handle can delete the preview.
	 * @param fileHandleId
	 * @throws SynapseException 
	 */
	@Override
	public void clearPreview(String fileHandleId) throws SynapseException{
		signAndDispatchSynapseRequest(getFileEndpoint(), FILE_HANDLE+"/"+fileHandleId+FILE_PREVIEW, "DELETE", null, defaultGETDELETEHeaders);
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	private URL getUrl(String uri) throws ClientProtocolException, IOException,
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
	public File downloadFromSynapse(LocationData location, String md5,
			File destinationFile) throws SynapseException {
		return downloadFromSynapse(location.getPath(), md5, destinationFile);
	}
	
	@Deprecated
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	 * @return the newly created entity
	 */
	private JSONObject createAuthEntity(String uri, JSONObject entity)
			throws SynapseException {
		return createJSONObjectEntity(authEndpoint, uri, entity);
	}

	private JSONObject putAuthEntity(String uri, JSONObject entity)
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
	 * @return the newly created entity
	 */
	private JSONObject createJSONObjectEntity(String endpoint, String uri,
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
	private <T extends JSONEntity> T createJSONEntity (String endpoint, String uri, T entity) throws JSONObjectAdapterException, SynapseException{
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
	private <T extends JSONEntity> T updateJSONEntity(String endpoint, String uri, T entity) throws JSONObjectAdapterException, SynapseException{
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
	 * Get a JSONEntity
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
	 * @return the retrieved entity
	 */
	private JSONObject getSynapseEntity(String endpoint, String uri)
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
	 * @return the updated entity
	 */
	private JSONObject putJSONObject(String endpoint, String uri,
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
	 */
	private JSONObject postUri(String endpoint, String uri) throws SynapseException {
		if (null == uri) throw new IllegalArgumentException("must provide uri");		
		return signAndDispatchSynapseRequest(endpoint, uri, "POST", null, defaultPOSTPUTHeaders);
	}

	/**
	 * Call Delete on any URI
	 */
	private void deleteUri(String endpoint, String uri) throws SynapseException {
		if (null == uri) throw new IllegalArgumentException("must provide uri");		
		signAndDispatchSynapseRequest(endpoint, uri, "DELETE", null, defaultGETDELETEHeaders);
	}

	/**
	 * Perform a query
	 * 
	 * @param query
	 *            the query to perform
	 * @return the query result
	 */
	private JSONObject querySynapse(String endpoint, String query)
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
					throw new SynapseServiceException("request content: "+requestContent+" exception content: "+exceptionContent+" status code: "+statusCode);
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
	@Override
	public StackStatus getCurrentStackStatus() throws SynapseException,
			JSONObjectAdapterException {
		JSONObject json = getEntity(STACK_STATUS);
		return EntityFactory
				.createEntityFromJSONObject(json, StackStatus.class);
	}
	
	@Override
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
	
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
	public void deleteGeneratedByForEntity(String entityId) throws SynapseException {
		if (entityId == null) throw new IllegalArgumentException("Entity id cannot be null");
		String uri = createEntityUri(ENTITY_URI_PATH, entityId) + GENERATED_BY_SUFFIX;
		deleteUri(uri);
	}

	/**
	 * Create an activity
	 * @param activity
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public Activity createActivity(Activity activity) throws SynapseException {
		if (activity == null) throw new IllegalArgumentException("Activity can not be null");
		String url = ACTIVITY_URI_PATH;		
		JSONObjectAdapter toCreateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		try {
			obj = new JSONObject(activity.writeToJSONObject(toCreateAdapter).toJSONString());
			JSONObject jsonObj = createJSONObject(url, obj);
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			return new Activity(adapter);
		} catch (JSONException e1) {
			throw new RuntimeException(e1);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}

	
	/**
	 * Get activity by id
	 * @param activityId
	 * @return
	 * @throws SynapseException
	 */
	@Override
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
	@Override
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
	@Override
	public void deleteActivity(String activityId) throws SynapseException {
		if (activityId == null) throw new IllegalArgumentException("Activity id cannot be null");
		String uri = createEntityUri(ACTIVITY_URI_PATH, activityId);
		deleteUri(uri);
	}
	
	@Override
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
	 * Gets the paginated list of descendants for the specified node.
	 *
	 * @param lastDescIdExcl
	 *            Paging delimiter. The last descendant ID (exclusive).
	 * @throws SynapseException 
	 */
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
	public PaginatedResults<Evaluation> getEvaluationByContentSource(String id, int offset, int limit) throws SynapseException {
		String url = ENTITY_URI_PATH + "/" + id + EVALUATION_URI_PATH + "?" + OFFSET + "=" + offset + "&limit=" + limit;
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
	
	@Deprecated
	@Override
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
	
	@Deprecated
	@Override
	public PaginatedResults<Evaluation> getAvailableEvaluationsPaginated(EvaluationStatus status, int offset, int limit) throws SynapseException {
		String url = null;
		if (null==status) {
			url = AVAILABLE_EVALUATION_URI_PATH + "?" + OFFSET + "=" + offset + "&limit=" + limit;
		} else {
			url = AVAILABLE_EVALUATION_URI_PATH + "?" + OFFSET + "=" + offset + "&limit=" + limit +"&status=" + status;			
		}
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
	
	@Deprecated
	@Override
	public Long getEvaluationCount() throws SynapseException {
		PaginatedResults<Evaluation> res = getEvaluationsPaginated(0,0);
		return res.getTotalNumberOfResults();
	}
	@Override
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
	@Override
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
	@Override
	public void deleteEvaluation(String evalId) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String uri = createEntityUri(EVALUATION_URI_PATH, evalId);
		deleteUri(uri);
	}
	
	/**
	 * Adds the authenticated user as a Participant in Evaluation evalId
	 */
	@Override
	public Participant createParticipant(String evalId) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String uri = createEntityUri(EVALUATION_URI_PATH, evalId) + "/" + PARTICIPANT;
		JSONObject jsonObj = postUri(uri);
		return initializeFromJSONObject(jsonObj, Participant.class);
	}
	@Override
	public Participant getParticipant(String evalId, String principalId) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		if (principalId == null) throw new IllegalArgumentException("Principal ID cannot be null");
		// Make sure we are passing in the ID, not the user name
		try {
			Long.parseLong(principalId);
		} catch (NumberFormatException e) {
			throw new SynapseException("Please pass in the pricipal ID, not the user name.", e);
		}
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
	@Override
	public void deleteParticipant(String evalId, String principalId) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		if (principalId == null) throw new IllegalArgumentException("Principal ID cannot be null");
		String uri = createEntityUri(EVALUATION_URI_PATH, evalId) + "/" + PARTICIPANT
				+ "/" + principalId;
		deleteUri(uri);
	}
	@Override
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
	@Override
	public Long getParticipantCount(String evalId) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		PaginatedResults<Participant> res = getAllParticipants(evalId, 0, 0);
		return res.getTotalNumberOfResults();
	}
	@Override
	public Submission createSubmission(Submission sub, String etag) throws SynapseException {
		String uri = EVALUATION_URI_PATH + "/" + SUBMISSION + "?" + ETAG + "=" + etag;
		try {
			JSONObject jsonObj = EntityFactory.createJSONObjectForEntity(sub);
			jsonObj = createJSONObject(uri, jsonObj);
			return initializeFromJSONObject(jsonObj, Submission.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	@Override
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
	@Override
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
	@Override
	public SubmissionStatus updateSubmissionStatus(SubmissionStatus status) throws SynapseException {
		if (status == null) {
			throw new IllegalArgumentException("SubmissionStatus  cannot be null");
		}
		if (status.getAnnotations() != null) {
			AnnotationsUtils.validateAnnotations(status.getAnnotations());
		}
		String url = EVALUATION_URI_PATH + "/" + SUBMISSION + "/" + status.getId() + STATUS;			
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
	@Override
	public void deleteSubmission(String subId) throws SynapseException {
		if (subId == null) throw new IllegalArgumentException("Submission id cannot be null");
		String uri = EVALUATION_URI_PATH + "/" + SUBMISSION + "/" + subId;			
		deleteUri(uri);
	}
	@Override
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
	@Override
	public PaginatedResults<SubmissionStatus> getAllSubmissionStatuses(String evalId, long offset, long limit) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH +	"/" + evalId + "/" + SUBMISSION_STATUS_ALL +
				"?offset" + "=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<SubmissionStatus> results = new PaginatedResults<SubmissionStatus>(SubmissionStatus.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	@Override
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundles(String evalId, long offset, long limit) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH +	"/" + evalId + "/" + SUBMISSION_BUNDLE_ALL +
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
	@Override
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
	@Override
	public PaginatedResults<SubmissionStatus> getAllSubmissionStatusesByStatus(String evalId, 
			SubmissionStatusEnum status, long offset, long limit) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH +	"/" + evalId + "/" + SUBMISSION_STATUS_ALL  + 
				STATUS_SUFFIX + status.toString() + "&offset=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<SubmissionStatus> results = new PaginatedResults<SubmissionStatus>(SubmissionStatus.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	@Override
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundlesByStatus(
			String evalId, SubmissionStatusEnum status, long offset, long limit) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH +	"/" + evalId + "/" + SUBMISSION_BUNDLE_ALL + 
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
	@Override
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
	@Override
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
	
	/**
	 * Get a temporary URL to access a File contained in a Submission.
	 * 
	 * @param submissionId
	 * @param fileHandleId
	 * @return
	 * @throws ClientProtocolException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	@Override
	public URL getFileTemporaryUrlForSubmissionFileHandle(String submissionId, String fileHandleId)
			throws ClientProtocolException, MalformedURLException, IOException {
		String url = getRepoEndpoint() + EVALUATION_URI_PATH + "/" + 
				SUBMISSION + "/" + submissionId + FILE + "/" + fileHandleId +
				QUERY_REDIRECT_PARAMETER + "false";
		return getUrl(url);
	}
	@Override
	public Long getSubmissionCount(String evalId) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		PaginatedResults<Submission> res = getAllSubmissions(evalId, 0, 0);
		return res.getTotalNumberOfResults();
	}
	@Override
	public UserEvaluationState getUserEvaluationState(String evalId) throws SynapseException{
		UserEvaluationState returnState = UserEvaluationState.EVAL_REGISTRATION_UNAVAILABLE;
		//TODO: replace with single call to getAvailableEvaluations() (PLFM-1858, simply check to see if evalId is in the return set) 
		//		instead of these three calls
		Evaluation evaluation = getEvaluation(evalId);
		EvaluationStatus status  = evaluation.getStatus();
		if (EvaluationStatus.OPEN.equals(status)) {
			//is the user registered for this?
			UserProfile profile = getMyProfile();
			if (profile != null && profile.getOwnerId() != null) {
				//try to get the participant
				returnState = UserEvaluationState.EVAL_OPEN_USER_NOT_REGISTERED;
				try {
					Participant user = getParticipant(evalId, profile.getOwnerId());
					if (user != null) {
						returnState = UserEvaluationState.EVAL_OPEN_USER_REGISTERED;
					}
				} catch (Exception e) {e.printStackTrace();}
			}
			//else user principle id unavailable, returnState = EVAL_REGISTRATION_UNAVAILABLE
		}
		//else registration is not OPEN, returnState = EVAL_REGISTRATION_UNAVAILABLE
		return returnState;
	}
	
	/**
	 * Execute a user query over the Submissions of a specified Evaluation.
	 * 
	 * @param query
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public QueryTableResults queryEvaluation(String query) throws SynapseException {
		try {
			if (null == query) {
				throw new IllegalArgumentException("must provide a query");
			}
			String queryUri;
			queryUri = EVALUATION_QUERY_URI_PATH + URLEncoder.encode(query, "UTF-8");
	
			Map<String, String> requestHeaders = new HashMap<String, String>();
			requestHeaders.putAll(defaultGETDELETEHeaders);
	
			JSONObject jsonObj = signAndDispatchSynapseRequest(repoEndpoint, queryUri, "GET", null,
					requestHeaders);
			JSONObjectAdapter joa = new JSONObjectAdapterImpl(jsonObj);
			return new QueryTableResults(joa);
		} catch (Exception e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Moves an entity and its descendants to the trash can.
	 *
	 * @param entityId The ID of the entity to be moved to the trash can
	 */
	@Override
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
	@Override
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
	@Override
	public PaginatedResults<TrashedEntity> viewTrashForUser(long offset, long limit) throws SynapseException {
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
	@Override
	public void purgeTrashForUser(String entityId) throws SynapseException {
		if (entityId == null || entityId.isEmpty()) {
			throw new IllegalArgumentException("Must provide an Entity ID.");
		}
		String url = TRASHCAN_PURGE + "/" + entityId;
		signAndDispatchSynapseRequest(repoEndpoint, url, "PUT", null, defaultPOSTPUTHeaders);
	}

	/**
	 * Purges the trash can for the user. All the entities in the trash will be permanently deleted.
	 */
	@Override
	public void purgeTrashForUser() throws SynapseException {
		signAndDispatchSynapseRequest(repoEndpoint, TRASHCAN_PURGE, "PUT", null, defaultPOSTPUTHeaders);
	}
	
	/**
	 * Add the entity to this user's Favorites list
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public EntityHeader addFavorite(String entityId) throws SynapseException {
		if (entityId == null) throw new IllegalArgumentException("Entity id cannot be null");
		String url = createEntityUri(FAVORITE_URI_PATH, entityId);		
		JSONObject jsonObj = postUri(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			return new EntityHeader(adapter);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}

	/**
	 * Remove the entity from this user's Favorites list
	 * @param entityId
	 * @throws SynapseException
	 */
	@Override
	public void removeFavorite(String entityId) throws SynapseException {
		if (entityId == null) throw new IllegalArgumentException("Entity id cannot be null");
		String uri = createEntityUri(FAVORITE_URI_PATH, entityId);		
		deleteUri(uri);
	}
	
	/**
	 * Retrieve this user's Favorites list
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public PaginatedResults<EntityHeader> getFavorites(Integer limit, Integer offset) throws SynapseException {
		String url = FAVORITE_URI_PATH + "?" + OFFSET + "=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<EntityHeader> results = new PaginatedResults<EntityHeader>(EntityHeader.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
		
	}

	/**
	 * Creates a DOI for the specified entity. The DOI will always be associated with
	 * the current version of the entity.
	 */
	@Override
	public void createEntityDoi(String entityId) throws SynapseException {
		createEntityDoi(entityId, null);
	}

	/**
	 * Creates a DOI for the specified entity version. If version is null, the DOI
	 * will always be associated with the current version of the entity.
	 */
	@Override
	public void createEntityDoi(String entityId, Long entityVersion) throws SynapseException {

		if (entityId == null || entityId.isEmpty()) {
			throw new IllegalArgumentException("Must provide entity ID.");
		}

		String url = ENTITY + "/" + entityId;
		if (entityVersion != null) {
			url = url + REPO_SUFFIX_VERSION + "/" + entityVersion;
		}
		url = url + DOI;
		signAndDispatchSynapseRequest(repoEndpoint, url, "PUT", null, defaultPOSTPUTHeaders);
	}

	/**
	 * Gets the DOI for the specified entity version. The DOI is for the current version of the entity.
	 */
	@Override
	public Doi getEntityDoi(String entityId) throws SynapseException {
		return getEntityDoi(entityId, null);
	}

	/**
	 * Gets the DOI for the specified entity version. If version is null, the DOI
	 * is for the current version of the entity.
	 */
	@Override
	public Doi getEntityDoi(String entityId, Long entityVersion) throws SynapseException {

		if (entityId == null || entityId.isEmpty()) {
			throw new IllegalArgumentException("Must provide entity ID.");
		}

		try {
			String url = ENTITY + "/" + entityId;
			if (entityVersion != null) {
				url = url + REPO_SUFFIX_VERSION + "/" + entityVersion;
			}
			url = url + DOI;
			JSONObject jsonObj = signAndDispatchSynapseRequest(repoEndpoint, url, "GET", null, defaultGETDELETEHeaders);
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			Doi doi = new Doi();
			doi.initializeFromJSONObject(adapter);
			return doi;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Gets the header information of entities whose file's MD5 matches the given MD5 checksum.
	 */
	@Override
	public List<EntityHeader> getEntityHeaderByMd5(String md5) throws SynapseException {

		if (md5 == null || md5.isEmpty()) {
			throw new IllegalArgumentException("Must provide a nonempty MD5 string.");
		}

		try {
			String url = ENTITY + "/md5/" + md5;
			JSONObject jsonObj = signAndDispatchSynapseRequest(repoEndpoint, url, "GET", null, defaultGETDELETEHeaders);
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			BatchResults<EntityHeader> results = new BatchResults<EntityHeader>(EntityHeader.class);
			results.initializeFromJSONObject(adapter);
			return results.getResults();
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public String retrieveApiKey() throws SynapseException {
		try {
			final String ATTRIBUTE_NAME = "secretKey";
			String url = "/secretKey";
			JSONObject jsonObj = signAndDispatchSynapseRequest(authEndpoint, url, "GET", null, defaultGETDELETEHeaders);
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			String apiKey = null;
			if(adapter != null && adapter.has(ATTRIBUTE_NAME)) {
				apiKey = adapter.getString(ATTRIBUTE_NAME);
			}
			return apiKey;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public void invalidateApiKey() throws SynapseException {
		deleteUri(authEndpoint, "/secretKey");
		setApiKey(null);
	}
	
	@Override
	public AccessControlList updateEvaluationAcl(AccessControlList acl) throws SynapseException {

		if (acl == null) {
			throw new IllegalArgumentException("ACL can not be null.");
		}

		String url = EVALUATION_ACL_URI_PATH;	
		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		try {
			obj = new JSONObject(acl.writeToJSONObject(toUpdateAdapter).toJSONString());
			JSONObject jsonObj = putJSONObject(url, obj, new HashMap<String,String>());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			return new AccessControlList(adapter);
		} catch (JSONException e) {
			throw new SynapseException(e);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	@Override
	public AccessControlList getEvaluationAcl(String evalId) throws SynapseException {

		if (evalId == null) {
			throw new IllegalArgumentException("Evaluation ID cannot be null.");
		}

		String url = EVALUATION_URI_PATH + "/" + evalId + "/acl";		
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			return new AccessControlList(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	@Override
	public UserEvaluationPermissions getUserEvaluationPermissions(String evalId) throws SynapseException {

		if (evalId == null) {
			throw new IllegalArgumentException("Evaluation ID cannot be null.");
		}

		String url = EVALUATION_URI_PATH + "/" + evalId + "/permissions";
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			return new UserEvaluationPermissions(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public ColumnModel createColumnModel(ColumnModel model) throws SynapseException {
		if(model == null) throw new IllegalArgumentException("ColumnModel cannot be null");
		String url = COLUMN;
		return createJSONEntity(url, model);
	}

	@Override
	public ColumnModel getColumnModel(String columnId) throws SynapseException {
		if(columnId == null) throw new IllegalArgumentException("ColumnId cannot be null");
		String url = COLUMN+"/"+columnId;
		try {
			return getJSONEntity(repoEndpoint, url, ColumnModel.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public List<ColumnModel> getColumnModelsForTableEntity(String tableEntityId) throws SynapseException {
		if(tableEntityId == null) throw new IllegalArgumentException("tableEntityId cannot be null");
		String url = ENTITY+"/"+tableEntityId+COLUMN;
		try {
			PaginatedColumnModels pcm = getJSONEntity(repoEndpoint, url, PaginatedColumnModels.class);
			return pcm.getResults();
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public PaginatedColumnModels listColumnModels(String prefix, Long limit, Long offset) throws SynapseException {
		String url = buildListColumnModelUrl(prefix, limit, offset);
		try {
			return  getJSONEntity(repoEndpoint, url, PaginatedColumnModels.class);
			
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Build up the URL for listing all ColumnModels
	 * @param prefix
	 * @param limit
	 * @param offset
	 * @return
	 */
	static String buildListColumnModelUrl(String prefix, Long limit, Long offset) {
		StringBuilder builder = new StringBuilder();
		builder.append(COLUMN);
		int count =0;
		if(prefix != null || limit != null || offset != null){
			builder.append("?");
		}
		if(prefix != null){
			builder.append("prefix=");
			builder.append(prefix);
			count++;
		}
		if(limit != null){
			if(count > 0){
				builder.append("&");
			}
			builder.append("limit=");
			builder.append(limit);
			count++;
		}
		if(offset != null){
			if(count > 0){
				builder.append("&");
			}
			builder.append("offset=");
			builder.append(offset);
		}
		return builder.toString();
	}
	
	@Override
	public Team createTeam(Team team)  throws SynapseException {
		try {
			JSONObject jsonObj = EntityFactory.createJSONObjectForEntity(team);
			jsonObj = createJSONObject(TEAM, jsonObj);
			return initializeFromJSONObject(jsonObj, Team.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public Team getTeam(String id) throws SynapseException {
		JSONObject jsonObj = getEntity(TEAM+"/"+id);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		Team results = new Team();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}


	@Override
	public PaginatedResults<Team> getTeams(String fragment, long limit,
			long offset) throws SynapseException {
		String uri = null;
		if (fragment==null) {
			uri = TEAMS+"?"+OFFSET+"="+offset+"&"+LIMIT+"="+limit;
		} else {
			uri = TEAMS+"?"+NAME_FRAGMENT_FILTER+"="+urlEncode(fragment)+"&"+OFFSET+"="+offset+"&"+LIMIT+"="+limit;
		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Team> results = new PaginatedResults<Team>(Team.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public PaginatedResults<Team> getTeamsForUser(String memberId, long limit,
			long offset) throws SynapseException {
		String uri = USER+"/"+memberId+TEAM+"?"+OFFSET+"="+offset+"&"+LIMIT+"="+limit;
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Team> results = new PaginatedResults<Team>(Team.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public URL getTeamIcon(String teamId, Boolean redirect)
			throws SynapseException {
		String uri = null;
		if (redirect==null) {
			uri = TEAM+"/"+teamId+ICON;
		} else {
			uri = TEAM+"/"+teamId+ICON+"?"+REDIRECT_PARAMETER+redirect;
		}
		try {
			return getUrl(getRepoEndpoint()+uri);
		} catch (IOException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public Team updateTeam(Team team) throws SynapseException {
		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		try {
			obj = new JSONObject(team.writeToJSONObject(toUpdateAdapter).toJSONString());
			JSONObject jsonObj = putJSONObject(TEAM, obj, new HashMap<String,String>());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			return new Team(adapter);
		} catch (JSONException e1) {
			throw new RuntimeException(e1);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}

	@Override
	public void deleteTeam(String teamId) throws SynapseException {
		deleteUri(TEAM+"/"+teamId);
	}

	@Override
	public void addTeamMember(String teamId, String memberId)
			throws SynapseException {
		putJSONObject(TEAM+"/"+teamId+MEMBER+"/"+memberId, new JSONObject(), new HashMap<String,String>());
	}
	
	private static String urlEncode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public PaginatedResults<TeamMember> getTeamMembers(String teamId, String fragment,
			long limit, long offset) throws SynapseException {
		String uri = null;
		if (fragment==null) {
			uri = TEAM_MEMBERS+"/"+teamId+"?"+OFFSET+"="+offset+"&"+LIMIT+"="+limit;
		} else {
			uri = TEAM_MEMBERS+"/"+teamId+"?"+NAME_FRAGMENT_FILTER+"="+urlEncode(fragment)+
					"&"+OFFSET+"="+offset+"&"+LIMIT+"="+limit;
		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<TeamMember> results = new PaginatedResults<TeamMember>(TeamMember.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public void removeTeamMember(String teamId, String memberId)
			throws SynapseException {
		deleteUri(TEAM+"/"+teamId+MEMBER+"/"+memberId);
	}
	
	@Override
	public void setTeamMemberPermissions(String teamId, String memberId,
			boolean isAdmin) throws SynapseException {
		putJSONObject(TEAM+"/"+teamId+MEMBER+"/"+memberId+
				PERMISSION+"?"+TEAM_MEMBERSHIP_PERMISSION+"="+isAdmin, 
				new JSONObject(), new HashMap<String,String>());
	}

	@Override
	public TeamMembershipStatus getTeamMembershipStatus(String teamId,
			String principalId) throws SynapseException {
		JSONObject jsonObj = getEntity(TEAM+"/"+teamId+MEMBER+"/"+principalId+MEMBERSHIP_STATUS);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		TeamMembershipStatus results = new TeamMembershipStatus();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public MembershipInvtnSubmission createMembershipInvitation(
			MembershipInvtnSubmission invitation) throws SynapseException {
		try {
			JSONObject jsonObj = EntityFactory.createJSONObjectForEntity(invitation);
			jsonObj = createJSONObject(MEMBERSHIP_INVITATION, jsonObj);
			return initializeFromJSONObject(jsonObj, MembershipInvtnSubmission.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public MembershipInvtnSubmission getMembershipInvitation(String invitationId)
			throws SynapseException {
		JSONObject jsonObj = getEntity(MEMBERSHIP_INVITATION+"/"+invitationId);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		MembershipInvtnSubmission results = new MembershipInvtnSubmission();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public PaginatedResults<MembershipInvitation> getOpenMembershipInvitations(
			String memberId, String teamId, long limit, long offset)
			throws SynapseException {
		
		String uri = null;
		if (teamId==null) {
			uri = USER+"/"+memberId+OPEN_MEMBERSHIP_INVITATION+"?"+OFFSET+"="+offset+"&"+LIMIT+"="+limit;
		} else {
			uri = USER+"/"+memberId+OPEN_MEMBERSHIP_INVITATION+"?"+TEAM_ID_REQUEST_PARAMETER+"="+teamId+"&"+OFFSET+"="+offset+"&"+LIMIT+"="+limit;
		
		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<MembershipInvitation> results = new PaginatedResults<MembershipInvitation>(MembershipInvitation.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public void deleteMembershipInvitation(String invitationId)
			throws SynapseException {
		deleteUri(MEMBERSHIP_INVITATION+"/"+invitationId);
	}

	@Override
	public MembershipRqstSubmission createMembershipRequest(
			MembershipRqstSubmission request) throws SynapseException {
		try {
			JSONObject jsonObj = EntityFactory.createJSONObjectForEntity(request);
			jsonObj = createJSONObject(MEMBERSHIP_REQUEST, jsonObj);
			return initializeFromJSONObject(jsonObj, MembershipRqstSubmission.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public MembershipRqstSubmission getMembershipRequest(String requestId)
			throws SynapseException {
		JSONObject jsonObj = getEntity(MEMBERSHIP_REQUEST+"/"+requestId);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		MembershipRqstSubmission results = new MembershipRqstSubmission();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public PaginatedResults<MembershipRequest> getOpenMembershipRequests(
			String teamId, String requestorId, long limit, long offset)
			throws SynapseException {
		String uri = null;
		if (requestorId==null) {
			uri = TEAM+"/"+teamId+OPEN_MEMBERSHIP_REQUEST+"?"+OFFSET+"="+offset+"&"+LIMIT+"="+limit;
		} else {
			uri = TEAM+"/"+teamId+OPEN_MEMBERSHIP_REQUEST+"?"+REQUESTOR_ID_REQUEST_PARAMETER+"="+requestorId+"&"+OFFSET+"="+offset+"&"+LIMIT+"="+limit;
		
		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<MembershipRequest> results = new PaginatedResults<MembershipRequest>(MembershipRequest.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public void deleteMembershipRequest(String requestId)
			throws SynapseException {
		deleteUri(MEMBERSHIP_REQUEST+"/"+requestId);
	}
	
	@Override
	public void updateTeamSearchCache() throws SynapseException {
		postUri(TEAM_UPDATE_SEARCH_CACHE);
	}

	@Override
	public void createUser(NewUser user) throws SynapseException {
		try {
			JSONObject obj = EntityFactory.createJSONObjectForEntity(user);
			createAuthEntity("/user", obj);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public NewUser getAuthUserInfo() throws SynapseException {
		try {
			JSONObject obj = getSynapseEntity(authEndpoint, "/user");
			return EntityFactory.createEntityFromJSONObject(obj, NewUser.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public void changePassword(String newPassword) throws SynapseException {
		try {
			ChangeUserPassword change = new ChangeUserPassword();
			change.setNewPassword(newPassword);
			
			JSONObject obj = EntityFactory.createJSONObjectForEntity(change);
			createAuthEntity("/userPassword", obj);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public void changePassword(String sessionToken, String newPassword) throws SynapseException {
		try {
			RegistrationInfo info = new RegistrationInfo();
			info.setRegistrationToken(AuthorizationConstants.REGISTRATION_TOKEN_PREFIX + sessionToken);
			info.setPassword(newPassword);
			
			JSONObject obj = EntityFactory.createJSONObjectForEntity(info);
			createAuthEntity("/registeringUserPassword", obj);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public void changeEmail(String sessionToken, String newPassword) throws SynapseException {
		try {
			RegistrationInfo info = new RegistrationInfo();
			info.setRegistrationToken(AuthorizationConstants.CHANGE_EMAIL_TOKEN_PREFIX + sessionToken);
			info.setPassword(newPassword);
			
			JSONObject obj = EntityFactory.createJSONObjectForEntity(info);
			createAuthEntity("/changeEmail", obj);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public void sendPasswordResetEmail() throws SynapseException {
		createAuthEntity("/apiPasswordEmail", new JSONObject());	
	}
	
	@Override
	public void sendPasswordResetEmail(String email) throws SynapseException {
		try {
			NewUser user = new NewUser();
			user.setEmail(email);
			
			JSONObject obj = EntityFactory.createJSONObjectForEntity(user);
			createAuthEntity("/userPasswordEmail", obj);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public Session passThroughOpenIDParameters(String queryString) throws SynapseException {
		try {
			URI uri = new URI(null, null, "/openIdCallback", queryString, null);
			JSONObject session = createAuthEntity(uri.toString(), new JSONObject());
			return EntityFactory.createEntityFromJSONObject(session, Session.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		} catch (URISyntaxException e) {
			throw new SynapseException(e);
		}
	}
}

