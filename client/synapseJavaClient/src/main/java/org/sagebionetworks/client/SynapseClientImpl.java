package org.sagebionetworks.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
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

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseTermsOfUseException;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.downloadtools.FileUtils;
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
import org.sagebionetworks.repo.model.DomainType;
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
import org.sagebionetworks.repo.model.auth.ChangePasswordRequest;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.SecretKey;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.auth.Username;
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
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.principal.AliasCheckRequest;
import org.sagebionetworks.repo.model.principal.AliasCheckResponse;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.query.QueryTableResults;
import org.sagebionetworks.repo.model.request.ReferenceList;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.utils.MD5ChecksumHelper;

import com.google.common.collect.Maps;

/**
 * Low-level Java Client API for Synapse REST APIs
 */
public class SynapseClientImpl extends BaseClientImpl implements SynapseClient {

	public static final String SYNPASE_JAVA_CLIENT = "Synpase-Java-Client/";

	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

	private static final Logger log = LogManager.getLogger(SynapseClientImpl.class.getName());
	
	private static final long MAX_UPLOAD_DAEMON_MS = 60*1000;

	private static final String DEFAULT_REPO_ENDPOINT = "https://repo-prod.prod.sagebase.org/repo/v1";
	private static final String DEFAULT_AUTH_ENDPOINT = SharedClientConnection.DEFAULT_AUTH_ENDPOINT;
	private static final String DEFAULT_FILE_ENDPOINT = "https://repo-prod.prod.sagebase.org/file/v1";

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
	
	public static final String PRINCIPAL = "/principal";
	public static final String PRINCIPAL_AVAILABLE = PRINCIPAL+"/available";
	
	private static final String WIKI_URI_TEMPLATE = "/%1$s/%2$s/wiki";
	private static final String WIKI_ID_URI_TEMPLATE = "/%1$s/%2$s/wiki/%3$s";
	private static final String WIKI_TREE_URI_TEMPLATE = "/%1$s/%2$s/wikiheadertree";
	private static final String WIKI_URI_TEMPLATE_V2 = "/%1$s/%2$s/wiki2";
	private static final String WIKI_ID_URI_TEMPLATE_V2 = "/%1$s/%2$s/wiki2/%3$s";
	private static final String WIKI_ID_VERSION_URI_TEMPLATE_V2 = "/%1$s/%2$s/wiki2/%3$s/%4$s";
	private static final String WIKI_TREE_URI_TEMPLATE_V2 = "/%1$s/%2$s/wikiheadertree2";
	private static final String WIKI_HISTORY_V2 = "/wikihistory";
	private static final String ATTACHMENT_HANDLES = "/attachmenthandles";
	private static final String ATTACHMENT_FILE = "/attachment";
	private static final String MARKDOWN_FILE = "/markdown";
	private static final String ATTACHMENT_FILE_PREVIEW = "/attachmentpreview";
	private static final String FILE_NAME_PARAMETER = "?fileName=";
	private static final String REDIRECT_PARAMETER = "redirect=";
	private static final String OFFSET_PARAMETER = "?offset=";
	private static final String LIMIT_PARAMETER = "limit=";
	private static final String VERSION_PARAMETER = "?wikiVersion=";
	private static final String AND_VERSION_PARAMETER = "&wikiVersion=";
	private static final String AND_LIMIT_PARAMETER = "&" + LIMIT_PARAMETER;
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
	
	private static final String MESSAGE                    = "/message";
	private static final String FORWARD                    = "/forward";
	private static final String CONVERSATION               = "/conversation";
	private static final String MESSAGE_STATUS             = MESSAGE + "/status";
	private static final String MESSAGE_INBOX              = MESSAGE + "/inbox";
	private static final String MESSAGE_OUTBOX             = MESSAGE + "/outbox";
	private static final String MESSAGE_INBOX_FILTER_PARAM = "inboxFilter";
	private static final String MESSAGE_ORDER_BY_PARAM     = "orderBy";
	private static final String MESSAGE_DESCENDING_PARAM   = "descending";
	
	private static final String STORAGE_SUMMARY_PATH = "/storageSummary";
	
	protected static final String COLUMN = "/column";
	protected static final String TABLE = "/table";

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

    private static final long MAX_BACKOFF_MILLIS = 5*60*1000L; // five minutes
    
    /**
     * The character encoding to use with strings which are the body of email messages
     */
    private static final Charset MESSAGE_CHARSET = Charset.forName("UTF-8");
    
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
	private static final String INVITEE_ID_REQUEST_PARAMETER = "inviteeId";
	// membership request
	private static final String MEMBERSHIP_REQUEST = "/membershipRequest";
	private static final String OPEN_MEMBERSHIP_REQUEST = "/openRequest";
	private static final String REQUESTOR_ID_REQUEST_PARAMETER = "requestorId";

	protected String repoEndpoint;
	protected String authEndpoint;
	protected String fileEndpoint;

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
		this(new SharedClientConnection(clientProvider), dataUploader);
	}

	/**
	 * Will use the same connection as the other client
	 * 
	 * @param clientProvider
	 * @param dataUploader
	 */
	public SynapseClientImpl(BaseClient other) {
		this(other.getSharedClientConnection(), new DataUploaderMultipartImpl());
	}

	/**
	 * Will use the shared connection provider and data uploader.
	 * 
	 * @param clientProvider
	 * @param dataUploader
	 */
	private SynapseClientImpl(SharedClientConnection sharedClientConnection, DataUploader dataUploader) {
		super(SYNPASE_JAVA_CLIENT + ClientVersionInfo.getClientVersionInfo(), sharedClientConnection);
		if (sharedClientConnection == null)
			throw new IllegalArgumentException("SharedClientConnection cannot be null");

		if (dataUploader == null)
			throw new IllegalArgumentException("DataUploader cannot be null");

		setRepositoryEndpoint(DEFAULT_REPO_ENDPOINT);
		setAuthEndpoint(DEFAULT_AUTH_ENDPOINT);
		setFileEndpoint(DEFAULT_FILE_ENDPOINT);

		this.dataUploader = dataUploader;
	}
	
	/**
	 * Use this method to override the default implementation of {@link HttpClientProvider}
	 * @param clientProvider
	 */
	public void setHttpClientProvider(HttpClientProvider clientProvider) {
		getSharedClientConnection().setHttpClientProvider(clientProvider);
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
		getSharedClientConnection().setAuthEndpoint(authEndpoint);
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
		getSharedClientConnection().setRequestProfile(request);
	}

	/**
	 * @return JSONObject
	 */
	@Override
	public JSONObject getProfileData() {
		return getSharedClientConnection().getProfileData();
	}
	
	/**
	 * @return the userName
	 */
	@Override
	public String getUserName() {
		return getSharedClientConnection().getUserName();
	}

	/**
	 * @param userName the userName to set
	 */
	@Override
	public void setUserName(String userName) {
		getSharedClientConnection().setUserName(userName);
	}

	/**
	 * @return the apiKey
	 */
	@Override
	public String getApiKey() {
		return getSharedClientConnection().getApiKey();
	}

	/**
	 * @param apiKey the apiKey to set
	 */
	@Override
	public void setApiKey(String apiKey) {
		getSharedClientConnection().setApiKey(apiKey);
	}
	
	@Override
	public Session login(String username, String password) throws SynapseException {
		return login(username, password, DomainType.SYNAPSE);
	}
	
	@Override
	public Session login(String username, String password, DomainType domain) throws SynapseException {
		return getSharedClientConnection().login(username, password, getUserAgent(), getParameterMapForDomain(domain));
	}

	@Override
	public void logout() throws SynapseException {
		getSharedClientConnection().logout(getUserAgent());
	}
	
	@Override
	public UserSessionData getUserSessionData() throws SynapseException {
		Session session = new Session();
		session.setSessionToken(getCurrentSessionToken());
		try {
			revalidateSession();
			session.setAcceptsTermsOfUse(true);
		} catch (SynapseTermsOfUseException e) {
			session.setAcceptsTermsOfUse(false);
		}
		
		UserSessionData userData = null;
		userData = new UserSessionData();
		userData.setSession(session);
		if (session.getAcceptsTermsOfUse()) {
			userData.setProfile(getMyProfile());
		}
		return userData;
	}
	
	@Override
	public boolean revalidateSession() throws SynapseException {
		return revalidateSession(DomainType.SYNAPSE);
	}
	
	@Override
	public boolean revalidateSession(DomainType domain) throws SynapseException {
		return getSharedClientConnection().revalidateSession(getUserAgent(), getParameterMapForDomain(domain));
	}
	
	/******************** Mid Level Repository Service APIs 
	 * @throws SynapseException ********************/

	@Override
	public AliasCheckResponse checkAliasAvailable(AliasCheckRequest request) throws SynapseException {
		String url = getRepoEndpoint()+PRINCIPAL_AVAILABLE;
		return asymmetricalPost(url, request, AliasCheckResponse.class);
	}
	
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
		return getSharedClientConnection().postJson(repoEndpoint, uri, entity.toString(), getUserAgent());
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
			jsonObject = getSharedClientConnection().putJson(repoEndpoint, url, jsonObject.toString(), getUserAgent());
			
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
			getSharedClientConnection().putJson(repoEndpoint, uri, EntityFactory.createJSONObjectForEntity(userProfile).toString(),
					getUserAgent());
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
			jsonAcl = getSharedClientConnection().putJson(repoEndpoint, uri, jsonAcl.toString(), getUserAgent());
			return initializeFromJSONObject(jsonAcl, AccessControlList.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public void deleteACL(String entityId) throws SynapseException {
		String uri = ENTITY_URI_PATH + "/" + entityId+ ENTITY_ACL_PATH_SUFFIX;
		getSharedClientConnection().deleteUri(repoEndpoint, uri, getUserAgent());
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
			jsonObject = getSharedClientConnection().putJson(repoEndpoint, url, jsonObject.toString(), getUserAgent());
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
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends AccessRequirement> T updateAccessRequirement(T ar) throws SynapseException {	
		if (ar==null) throw new IllegalArgumentException("AccessRequirement cannot be null");	
		String url = createEntityUri(ACCESS_REQUIREMENT+"/", ar.getId().toString());		
		try {
			JSONObject toUpdateJsonObject = EntityFactory.createJSONObjectForEntity(ar);
			JSONObject updatedJsonObject = getSharedClientConnection().putJson(repoEndpoint, url, toUpdateJsonObject.toString(), getUserAgent());
			return (T)initializeFromJSONObject(updatedJsonObject, getAccessRequirementClassFromType(ar.getEntityType()));
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}		
	}
	
	@Override
	public ACTAccessRequirement createLockAccessRequirement(String entityId) throws SynapseException {
		if (entityId == null) throw new IllegalArgumentException("Entity id cannot be null");
		JSONObject jsonObj = getSharedClientConnection().postUri(repoEndpoint, ENTITY + "/" + entityId + LOCK_ACCESS_REQUIREMENT,
				getUserAgent());
		return initializeFromJSONObject(jsonObj, ACTAccessRequirement.class);
	}

	@Override
	public void deleteAccessRequirement(Long arId) throws SynapseException {
		getSharedClientConnection().deleteUri(repoEndpoint, ACCESS_REQUIREMENT + "/" + arId, getUserAgent());
	}
	@Override
	public VariableContentPaginatedResults<AccessRequirement> getUnmetAccessRequirements(RestrictableObjectDescriptor subjectId) throws SynapseException {
		String uri = null;
		if (RestrictableObjectType.ENTITY == subjectId.getType()) {
			uri = ENTITY+"/"+subjectId.getId()+ACCESS_REQUIREMENT_UNFULFILLED;
		} else if (RestrictableObjectType.EVALUATION == subjectId.getType()) {
			uri = EVALUATION_URI_PATH+"/"+subjectId.getId()+ACCESS_REQUIREMENT_UNFULFILLED;
		} else if (RestrictableObjectType.TEAM == subjectId.getType()) {
			uri = TEAM+"/"+subjectId.getId()+ACCESS_REQUIREMENT_UNFULFILLED;
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
		} else if (RestrictableObjectType.TEAM == subjectId.getType()) {
			uri = TEAM+"/"+subjectId.getId()+ACCESS_REQUIREMENT;
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
		return getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
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
		try {
			String uri = createEntityUri(ENTITY_URI_PATH, entity.getId());
			if(activityId != null) 
				uri += "?" + PARAM_GENERATED_BY + "=" + activityId;
			JSONObject jsonObject;
			jsonObject = EntityFactory.createJSONObjectForEntity(entity);
			jsonObject = getSharedClientConnection().putJson(repoEndpoint, uri, jsonObject.toString(), getUserAgent());
			return (T) EntityFactory.createEntityFromJSONObject(jsonObject,
					entity.getClass());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
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
		getSharedClientConnection().deleteUri(repoEndpoint, uri, getUserAgent());
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
		getSharedClientConnection().deleteUri(repoEndpoint, uri, getUserAgent());
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
		getSharedClientConnection().deleteUri(repoEndpoint, uri, getUserAgent());
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
		return querySynapse(query);
	}
	
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
	
	@Override
	public S3FileHandle createFileHandle(File temp, String contentType) throws SynapseException, IOException{
		return createFileHandle(temp, contentType, null);
	}

	@Override
	public S3FileHandle createFileHandle(File temp, String contentType, Boolean shouldPreviewBeCreated)
			throws SynapseException, IOException {
		if (temp == null) {
			throw new IllegalArgumentException("File cannot be null");
		}
		if (contentType == null) {
			throw new IllegalArgumentException("Content type cannot be null");
		}
		
		CreateChunkedFileTokenRequest ccftr = new CreateChunkedFileTokenRequest();
		ccftr.setContentType(contentType);
		ccftr.setFileName(temp.getName());
		// Calculate the MD5
		String md5 = MD5ChecksumHelper.getMD5Checksum(temp);
		ccftr.setContentMD5(md5);
		// Start the upload
		ChunkedFileToken token = createChunkedFileUploadToken(ccftr);
		// Now break the file into part as needed
		List<File> fileChunks = FileUtils.chunkFile(temp, MINIMUM_CHUNK_SIZE_BYTES);
		try{
			// Upload all of the parts.
			List<Long> partNumbers = uploadChunks(fileChunks, token);
			
			// We can now complete the upload
			CompleteAllChunksRequest cacr = new CompleteAllChunksRequest();
			cacr.setChunkedFileToken(token);
			cacr.setChunkNumbers(partNumbers);
			cacr.setShouldPreviewBeGenerated(shouldPreviewBeCreated);
			
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
			FileUtils.deleteAllFilesExcludingException(temp, fileChunks);
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
			if (chunkRequest == null) {
				throw new IllegalArgumentException("ChunkRequest cannot be null");
			}
			String uri = CREATE_CHUNKED_FILE_UPLOAD_CHUNK_URL;
			String data = EntityFactory.createJSONStringForEntity(chunkRequest);
			String responseBody = getSharedClientConnection().postStringDirect(getFileEndpoint(), uri, data, getUserAgent());
			return new URL(responseBody);
		} catch (IOException e) {
			throw new SynapseException(e);
		} catch (JSONObjectAdapterException e) {
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
		return getSharedClientConnection().putFileToURL(url, file, contentType);
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
		return doCreateJSONEntity(getFileEndpoint(), uri, efh);
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
		try {
			String responseBody = getSharedClientConnection().asymmetricalPost(url, EntityFactory.createJSONStringForEntity(requestBody),
					getUserAgent());
			return EntityFactory.createEntityFromJSONString(responseBody, returnClass);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Get the raw file handle. Note: Only the creator of a the file handle can get the raw file handle.
	 * 
	 * @param fileHandleId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public FileHandle getRawFileHandle(String fileHandleId) throws SynapseException{
		JSONObject object = getSharedClientConnection().getJson(getFileEndpoint(), FILE_HANDLE + "/" + fileHandleId, getUserAgent());
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
		getSharedClientConnection().deleteUri(getFileEndpoint(), FILE_HANDLE + "/" + fileHandleId, getUserAgent());
	}
	
	/**
	 * Delete the preview associated with the given file handle.
	 * Note: Only the creator of a the file handle can delete the preview.
	 * @param fileHandleId
	 * @throws SynapseException 
	 */
	@Override
	public void clearPreview(String fileHandleId) throws SynapseException{
		getSharedClientConnection().deleteUri(getFileEndpoint(), FILE_HANDLE + "/" + fileHandleId + FILE_PREVIEW, getUserAgent());
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
		try {
			// Let java guess from the stream.
			String contentType = URLConnection.guessContentTypeFromStream(is);
			// If Java fails then set the content type to be octet-stream
			if (contentType == null) {
				contentType = APPLICATION_OCTET_STREAM;
			}
			return contentType;
		} finally {
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
		return doCreateJSONEntity(uri, toCreate);
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
		return getJSONEntity(uri, WikiPage.class);
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
		return getJSONEntity(uri, WikiPage.class);
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
		return getJSONEntity(uri, FileHandleResults.class);
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
		String uri = createWikiURL(key) + ATTACHMENT_FILE + FILE_NAME_PARAMETER + encodedName + AND_REDIRECT_PARAMETER
				+ "false";
		return getUrl(uri);
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
		String uri = createWikiURL(key) + ATTACHMENT_FILE_PREVIEW + FILE_NAME_PARAMETER + encodedName + AND_REDIRECT_PARAMETER
				+ "false";
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
		String uri = ENTITY + "/" + entityId + FILE + QUERY_REDIRECT_PARAMETER + "false";
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
		String uri = ENTITY + "/" + entityId + FILE_PREVIEW + QUERY_REDIRECT_PARAMETER + "false";
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
		String uri = ENTITY + "/" + entityId + VERSION_INFO + "/" + versionNumber + FILE + QUERY_REDIRECT_PARAMETER + "false";
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
		String uri = ENTITY + "/" + entityId + VERSION_INFO + "/" + versionNumber + FILE_PREVIEW + QUERY_REDIRECT_PARAMETER
				+ "false";
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
		return new URL(getSharedClientConnection().getDirect(repoEndpoint, uri, getUserAgent()));
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
		return updateJSONEntity(uri, toUpdate);
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
		getSharedClientConnection().deleteUri(repoEndpoint, uri, getUserAgent());
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
		JSONObject object = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
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
		return getJSONEntity(uri, FileHandleResults.class);
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
		return getJSONEntity(uri, FileHandleResults.class);
	}
	
	// V2 WIKIPAGE METHODS
	
	/**
	 * Helper to create a V2 Wiki URL (No ID)
	 */
	private String createV2WikiURL(String ownerId, ObjectType ownerType) {
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		return String.format(WIKI_URI_TEMPLATE_V2, ownerType.name().toLowerCase(), ownerId);
	}
	
	/**
	 * Helper to build a URL for a V2 Wiki page, with ID
	 * @param key
	 * @return
	 */
	private String createV2WikiURL(WikiPageKey key) {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		return String.format(WIKI_ID_URI_TEMPLATE_V2, key.getOwnerObjectType().name().toLowerCase(), 
				key.getOwnerObjectId(), key.getWikiPageId());
	}
	
	/**
	 * 
	 * Create a V2 WikiPage for a given owner object.
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @param toCreate
	 * @return
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	@Override
	public V2WikiPage createV2WikiPage(String ownerId, ObjectType ownerType,
			V2WikiPage toCreate) throws JSONObjectAdapterException,
			SynapseException {
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		if(toCreate == null) throw new IllegalArgumentException("WikiPage cannot be null");
		String uri = createV2WikiURL(ownerId, ownerType);
		return doCreateJSONEntity(uri, toCreate);
	}
	
	/**
	 * Get a V2 WikiPage using its key
	 * @param key
	 * @return
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	@Override
	public V2WikiPage getV2WikiPage(WikiPageKey key)
		throws JSONObjectAdapterException, SynapseException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		String uri = createV2WikiURL(key);
		return getJSONEntity(uri, V2WikiPage.class);
	}
	
	/**
	 * Get a version of a V2 WikiPage using its key and version number
	 */
	@Override
	public V2WikiPage getVersionOfV2WikiPage(WikiPageKey key, Long version)
		throws JSONObjectAdapterException, SynapseException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(version == null) throw new IllegalArgumentException("Version cannot be null");
		
		String uri = createV2WikiURL(key) + VERSION_PARAMETER + version;
		return getJSONEntity(uri, V2WikiPage.class);
	}
	
	/**
	 * Get a the root V2 WikiPage for a given owner.
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	@Override
	public V2WikiPage getV2RootWikiPage(String ownerId, ObjectType ownerType)
		throws JSONObjectAdapterException, SynapseException {
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		String uri = createV2WikiURL(ownerId, ownerType);
		return getJSONEntity(uri, V2WikiPage.class);
	}

	/**
	 * Update a V2 WikiPage
	 * @param ownerId
	 * @param ownerType
	 * @param toUpdate
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	@Override
	public V2WikiPage updateV2WikiPage(String ownerId, ObjectType ownerType,
		V2WikiPage toUpdate) throws JSONObjectAdapterException,
		SynapseException {
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		if(toUpdate == null) throw new IllegalArgumentException("WikiPage cannot be null");
		if(toUpdate.getId() == null) throw new IllegalArgumentException("WikiPage.getId() cannot be null");
		String uri = String.format(WIKI_ID_URI_TEMPLATE_V2, ownerType.name().toLowerCase(), ownerId, toUpdate.getId());
		return updateJSONEntity(uri, toUpdate);
	}

	/**
	 * Restore contents of a V2 WikiPage to the contents
	 * of a particular version.
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @param wikiId
	 * @param versionToRestore
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	@Override
	public V2WikiPage restoreV2WikiPage(String ownerId, ObjectType ownerType,
		String wikiId, Long versionToRestore) throws JSONObjectAdapterException,
		SynapseException {
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		if(wikiId == null) throw new IllegalArgumentException("Wiki id cannot be null");
		if(versionToRestore == null) throw new IllegalArgumentException("Version cannot be null");
		String uri = String.format(WIKI_ID_VERSION_URI_TEMPLATE_V2, ownerType.name().toLowerCase(), ownerId, wikiId, String.valueOf(versionToRestore));
		V2WikiPage mockWikiToUpdate = new V2WikiPage();
		return updateJSONEntity(uri, mockWikiToUpdate);
	}

	/**
	 * Get all of the FileHandles associated with a V2 WikiPage, including any PreviewHandles.
	 * @param key
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	@Override
	public FileHandleResults getV2WikiAttachmentHandles(WikiPageKey key)
		throws JSONObjectAdapterException, SynapseException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		String uri = createV2WikiURL(key)+ATTACHMENT_HANDLES;
		return getJSONEntity(uri, FileHandleResults.class);
	}

	@Override
	public FileHandleResults getVersionOfV2WikiAttachmentHandles(WikiPageKey key, Long version)
		throws JSONObjectAdapterException, SynapseException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(version == null) throw new IllegalArgumentException("Version cannot be null");
		String uri = createV2WikiURL(key)+ATTACHMENT_HANDLES+VERSION_PARAMETER+version;
		return getJSONEntity(uri, FileHandleResults.class);
	}
	@Override
	public String downloadV2WikiMarkdown(WikiPageKey key) throws ClientProtocolException, FileNotFoundException, IOException, SynapseException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		String uri = createV2WikiURL(key)+MARKDOWN_FILE;
		return getSharedClientConnection().downloadZippedFileString(repoEndpoint, uri, getUserAgent());
	}
	
	@Override
	public String downloadVersionOfV2WikiMarkdown(WikiPageKey key, Long version) throws ClientProtocolException, FileNotFoundException, IOException, SynapseException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(version == null) throw new IllegalArgumentException("Version cannot be null");
		String uri = createV2WikiURL(key)+MARKDOWN_FILE+VERSION_PARAMETER+version;
		return getSharedClientConnection().downloadZippedFileString(repoEndpoint, uri, getUserAgent());
	}
	
	/**
	 * Get the temporary URL for a V2 WikiPage attachment. This is an alternative to downloading the attachment to a file.
	 * @param key - Identifies a V2 wiki page.
	 * @param fileName - The name of the attachment file.
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Override
	public URL getV2WikiAttachmentTemporaryUrl(WikiPageKey key,
			String fileName) throws ClientProtocolException, IOException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(fileName == null) throw new IllegalArgumentException("fileName cannot be null");
		String encodedName = URLEncoder.encode(fileName, "UTF-8");
		String uri = createV2WikiURL(key) + ATTACHMENT_FILE + FILE_NAME_PARAMETER + encodedName + AND_REDIRECT_PARAMETER
				+ "false";
		return getUrl(uri);
	}
	
	/**
	 * Get the temporary URL for a V2 WikiPage attachment preview. This is an alternative to downloading the attachment to a file.
	 * @param key - Identifies a V2 wiki page.
	 * @param fileName - The name of the attachment file.
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Override
	public URL getV2WikiAttachmentPreviewTemporaryUrl(WikiPageKey key,
			String fileName) throws ClientProtocolException, IOException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(fileName == null) throw new IllegalArgumentException("fileName cannot be null");
		String encodedName = URLEncoder.encode(fileName, "UTF-8");
		String uri = createV2WikiURL(key) + ATTACHMENT_FILE_PREVIEW + FILE_NAME_PARAMETER + encodedName
				+ AND_REDIRECT_PARAMETER + "false";
		return getUrl(uri);
	}
	
	@Override
	public URL getVersionOfV2WikiAttachmentPreviewTemporaryUrl(WikiPageKey key,
			String fileName, Long version) throws ClientProtocolException, IOException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(fileName == null) throw new IllegalArgumentException("fileName cannot be null");
		String encodedName = URLEncoder.encode(fileName, "UTF-8");
		String uri = createV2WikiURL(key) + ATTACHMENT_FILE_PREVIEW + FILE_NAME_PARAMETER + encodedName
			+ AND_REDIRECT_PARAMETER + "false" + AND_VERSION_PARAMETER + version;
		return getUrl(uri);
	}

	@Override
	public URL getVersionOfV2WikiAttachmentTemporaryUrl(WikiPageKey key,
			String fileName, Long version) throws ClientProtocolException, IOException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(fileName == null) throw new IllegalArgumentException("fileName cannot be null");
		String encodedName = URLEncoder.encode(fileName, "UTF-8");
		String uri = createV2WikiURL(key) + ATTACHMENT_FILE + FILE_NAME_PARAMETER + encodedName + AND_REDIRECT_PARAMETER
			+ "false" + AND_VERSION_PARAMETER + version;
		return getUrl(uri);
	}
	
	/**
	 * Delete a V2 WikiPage
	 * @param key
	 * @throws SynapseException
	 */
	@Override
	public void deleteV2WikiPage(WikiPageKey key) throws SynapseException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		String uri = createV2WikiURL(key);
		getSharedClientConnection().deleteUri(repoEndpoint, uri, getUserAgent());
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
	public PaginatedResults<V2WikiHeader> getV2WikiHeaderTree(String ownerId,
		ObjectType ownerType) throws SynapseException,
		JSONObjectAdapterException {
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		String uri = String.format(WIKI_TREE_URI_TEMPLATE_V2, ownerType.name().toLowerCase(), ownerId);
		JSONObject object = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
		PaginatedResults<V2WikiHeader> paginated = new PaginatedResults<V2WikiHeader>(V2WikiHeader.class);
		paginated.initializeFromJSONObject(new JSONObjectAdapterImpl(object));
		return paginated;
	}
	
	/**
	 * Get the tree of snapshots (outlining each modification) for a particular V2 WikiPage
	 * @param key
	 * @return
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	@Override
	public PaginatedResults<V2WikiHistorySnapshot> getV2WikiHistory(WikiPageKey key, Long limit, Long offset)
		throws JSONObjectAdapterException, SynapseException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		String uri = createV2WikiURL(key) + WIKI_HISTORY_V2 + OFFSET_PARAMETER + offset + AND_LIMIT_PARAMETER + limit;
		JSONObject object = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
		PaginatedResults<V2WikiHistorySnapshot> paginated = new PaginatedResults<V2WikiHistorySnapshot>(V2WikiHistorySnapshot.class);
		paginated.initializeFromJSONObject(new JSONObjectAdapterImpl(object));
		return paginated;
	}
	
	/**
	 * Creates a V2 WikiPage model from a V1, zipping up markdown content and tracking it with
	 * a file handle.
	 * @param from
	 * @return
	 * @throws IOException
	 * @throws SynapseException
	 */
	private V2WikiPage createV2WikiPageFromV1(WikiPage from) throws IOException, SynapseException {
		if(from == null) throw new IllegalArgumentException("WikiPage cannot be null");
		// Copy over all information
		V2WikiPage wiki = new V2WikiPage();
		wiki.setId(from.getId());
		wiki.setEtag(from.getEtag());
		wiki.setCreatedOn(from.getCreatedOn());
		wiki.setCreatedBy(from.getCreatedBy());
		wiki.setModifiedBy(from.getModifiedBy());
		wiki.setModifiedOn(from.getModifiedOn());
		wiki.setParentWikiId(from.getParentWikiId());
		wiki.setTitle(from.getTitle());
		wiki.setAttachmentFileHandleIds(from.getAttachmentFileHandleIds());	
		
		// Zip up markdown
		File markdownFile = File.createTempFile("compressed", ".txt.gz");
		try{
			String markdown = from.getMarkdown();
			if(markdown != null) {
				markdownFile = FileUtils.writeStringToCompressedFile(markdownFile, markdown);
			} else {
				markdownFile = FileUtils.writeStringToCompressedFile(markdownFile, "");
			}
			String contentType = "application/x-gzip";
			// Create file handle for markdown
			S3FileHandle markdownS3Handle = createFileHandle(markdownFile, contentType);
			wiki.setMarkdownFileHandleId(markdownS3Handle.getId());
			return wiki;
		}finally{
			markdownFile.delete();
		}

	}
	
	/**
	 * Creates a V1 WikiPage model from a V2, unzipping the markdown file contents into
	 * the markdown field.
	 * @param from
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws ClientProtocolException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private WikiPage createWikiPageFromV2(V2WikiPage from, String ownerId, ObjectType ownerType, Long version) throws ClientProtocolException, 
		FileNotFoundException, IOException, SynapseException {
		if(from == null) throw new IllegalArgumentException("WikiPage cannot be null");
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		WikiPage wiki = new WikiPage();
		wiki.setId(from.getId());
		wiki.setEtag(from.getEtag());
		wiki.setCreatedOn(from.getCreatedOn());
		wiki.setCreatedBy(from.getCreatedBy());
		wiki.setModifiedBy(from.getModifiedBy());
		wiki.setModifiedOn(from.getModifiedOn());
		wiki.setParentWikiId(from.getParentWikiId());
		wiki.setTitle(from.getTitle());
		wiki.setAttachmentFileHandleIds(from.getAttachmentFileHandleIds());
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, wiki.getId());
		
		// We may be returning the most recent version of the V2 Wiki, or another version
		// Download the correct markdown file
		String markdownString = null;
		if(version == null) {
			markdownString = downloadV2WikiMarkdown(key);
		} else {
			markdownString = downloadVersionOfV2WikiMarkdown(key, version);
		}
		// Store the markdown as a string
		wiki.setMarkdown(markdownString);
		return wiki;
	}
	
	/**
	 * Creates a V1 WikiPage model from a V2 and the already unzipped/string markdown.
	 * @param model
	 * @param markdown
	 * @return
	 */
	private WikiPage mergeMarkdownAndMetadata(V2WikiPage model, String markdown) {
		WikiPage wiki = new WikiPage();
		wiki.setId(model.getId());
		wiki.setEtag(model.getEtag());
		wiki.setCreatedOn(model.getCreatedOn());
		wiki.setCreatedBy(model.getCreatedBy());
		wiki.setModifiedBy(model.getModifiedBy());
		wiki.setModifiedOn(model.getModifiedOn());
		wiki.setParentWikiId(model.getParentWikiId());
		wiki.setTitle(model.getTitle());
		wiki.setAttachmentFileHandleIds(model.getAttachmentFileHandleIds());
		wiki.setMarkdown(markdown);
		return wiki;
	}
	
	@Override
	public WikiPage createV2WikiPageWithV1(String ownerId, ObjectType ownerType,
			WikiPage toCreate) throws IOException, SynapseException, JSONObjectAdapterException{
		// Zip up markdown and create a V2 WikiPage
		V2WikiPage converted = createV2WikiPageFromV1(toCreate);
		// Create the V2 WikiPage
		V2WikiPage created = createV2WikiPage(ownerId, ownerType, converted);
		// Return the result in V1 form
		return createWikiPageFromV2(created, ownerId, ownerType, null);
	}
	
	@Override
	public WikiPage updateV2WikiPageWithV1(String ownerId, ObjectType ownerType,
			WikiPage toUpdate) throws IOException, SynapseException, JSONObjectAdapterException {
		// Zip up markdown and create a V2 WikiPage
		V2WikiPage converted = createV2WikiPageFromV1(toUpdate);
		// Update the V2 WikiPage
		V2WikiPage updated = updateV2WikiPage(ownerId, ownerType, converted);
		// Return result in V1 form
		return mergeMarkdownAndMetadata(updated, toUpdate.getMarkdown());
	}
	
	@Override
	public WikiPage getV2WikiPageAsV1(WikiPageKey key) throws JSONObjectAdapterException, SynapseException, IOException {
		// Get the V2 Wiki
		V2WikiPage v2WikiPage = getV2WikiPage(key);
		// Convert and return as a V1
		return createWikiPageFromV2(v2WikiPage, key.getOwnerObjectId(), key.getOwnerObjectType(), null);
	}
	
	@Override
	public WikiPage getVersionOfV2WikiPageAsV1(WikiPageKey key, Long version) throws JSONObjectAdapterException, SynapseException, IOException {
		// Get a version of the V2 Wiki
		V2WikiPage v2WikiPage = getVersionOfV2WikiPage(key, version);
		// Convert and return as a V1
		return createWikiPageFromV2(v2WikiPage, key.getOwnerObjectId(), key.getOwnerObjectType(), version);
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
		return getSharedClientConnection().downloadFromSynapse(path, md5, destinationFile);
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

	/******************** Low Level APIs ********************/

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
	private <T extends JSONEntity> T doCreateJSONEntity(String uri, T entity) throws JSONObjectAdapterException, SynapseException {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}
		String postJSON = EntityFactory.createJSONStringForEntity(entity);
		JSONObject jsonObject = getSharedClientConnection().postJson(repoEndpoint, uri, postJSON, getUserAgent());
		return (T) EntityFactory.createEntityFromJSONObject(jsonObject, entity.getClass());
	}

	/**
	 * Create any JSONEntity
	 * 
	 * @param endpoint
	 * @param uri
	 * @param entity
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	@SuppressWarnings("unchecked")
	private <T extends JSONEntity> T doCreateJSONEntity(String endpoint, String uri, T entity) throws JSONObjectAdapterException,
			SynapseException {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}
		String postJSON = EntityFactory.createJSONStringForEntity(entity);
		JSONObject jsonObject = getSharedClientConnection().postJson(endpoint, uri, postJSON, getUserAgent());
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
	private <T extends JSONEntity> T updateJSONEntity(String uri, T entity) throws JSONObjectAdapterException, SynapseException {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}
		String putJSON = EntityFactory.createJSONStringForEntity(entity);
		JSONObject jsonObject = getSharedClientConnection().putJson(repoEndpoint, uri, putJSON, getUserAgent());
		return (T) EntityFactory.createEntityFromJSONObject(jsonObject, entity.getClass());
	}
	
	/**
	 * Get a JSONEntity
	 */
	protected <T extends JSONEntity> T getJSONEntity(String uri, Class<? extends T> clazz) throws JSONObjectAdapterException, SynapseException{
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == clazz) {
			throw new IllegalArgumentException("must provide entity");
		}
		JSONObject jsonObject = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
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
		return getSharedClientConnection().getJson(endpoint, uri, getUserAgent());
	}

	/**
	 * Update a dataset, layer, preview, annotations, etc...
	 * 
	 * This convenience method first grabs a copy of the currently stored entity, then overwrites fields from the entity
	 * passed in on top of the stored entity we retrieved and then PUTs the entity. This essentially does a partial
	 * update from the point of view of the user of this API.
	 * 
	 * Note that users of this API may want to inspect what they are overwriting before they do so. Another approach
	 * would be to do a GET, display the field to the user, allow them to edit the fields, and then do a PUT.
	 * 
	 * @param defaultEndpoint
	 * @param uri
	 * @param entity
	 * @return the updated entity
	 * @throws SynapseException
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	public JSONObject updateSynapseEntity(String uri,
			JSONObject entity) throws SynapseException {

		JSONObject storedEntity = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());

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
			return getSharedClientConnection().putJson(repoEndpoint, uri, storedEntity.toString(), getUserAgent());
		} catch (JSONException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Perform a query
	 * 
	 * @param query
	 *            the query to perform
	 * @return the query result
	 */
	private JSONObject querySynapse(String query)
			throws SynapseException {
		try {
			if (null == query) {
				throw new IllegalArgumentException("must provide a query");
			}

			String queryUri;
			queryUri = QUERY_URI + URLEncoder.encode(query, "UTF-8");
			return getSharedClientConnection().getJson(repoEndpoint, queryUri, getUserAgent());
		} catch (UnsupportedEncodingException e) {
			throw new SynapseException(e);
		}
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
		JSONObject obj = getSharedClientConnection().postJson(repoEndpoint, uri, jsonBody, getUserAgent());
		if(obj != null) {
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(obj);
			searchResults = new SearchResults(adapter);
		}
		return searchResults;
	}
	
	@Override
	public String getSynapseTermsOfUse() throws SynapseException {
		return getTermsOfUse(DomainType.SYNAPSE);
	}
	
	@Override
	public String getTermsOfUse(DomainType domain) throws SynapseException {
		if (domain == null) {
			throw new IllegalArgumentException("Domain must be specified");
		}
		return getSharedClientConnection().getDataDirect(authEndpoint, "/"+domain.name().toLowerCase()+"TermsOfUse.html");
	}

	/**
	 * Helper for pagination of messages
	 */
	private String setMessageParameters(String path,
			List<MessageStatusType> inboxFilter, MessageSortBy orderBy,
			Boolean descending, Long limit, Long offset) {
		if (path == null) {
			throw new IllegalArgumentException("Path must be specified");
		}

		URIBuilder builder = new URIBuilder();
		builder.setPath(path);
		if (inboxFilter != null) {
			builder.setParameter(MESSAGE_INBOX_FILTER_PARAM,
					StringUtils.join(inboxFilter.toArray(), ','));
		}
		if (orderBy != null) {
			builder.setParameter(MESSAGE_ORDER_BY_PARAM, orderBy.name());
		}
		if (descending != null) {
			builder.setParameter(MESSAGE_DESCENDING_PARAM, "" + descending);
		}
		if (limit != null) {
			builder.setParameter(LIMIT, "" + limit);
		}
		if (offset != null) {
			builder.setParameter(OFFSET, "" + offset);
		}
		return builder.toString();
	}
	
	@Override
	public MessageToUser sendMessage(MessageToUser message) throws SynapseException {
		String uri = MESSAGE;
		try {
			String jsonBody = EntityFactory.createJSONStringForEntity(message);
			JSONObject obj = getSharedClientConnection().postJson(repoEndpoint, uri, jsonBody, getUserAgent());
			return EntityFactory.createEntityFromJSONObject(obj, MessageToUser.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	

	/**
	 * uploads a String to S3 using the chunked file upload service
	 * 
	 * @param content the content to upload. Strings in memory should not be large, so we limit to the size of one 'chunk'
	 * @param contentType should include the character encoding, e.g. "text/plain; charset=utf-8"
	 */
	@Override
    public String uploadToFileHandle(byte[] content, ContentType contentType) throws SynapseException {
    	if (content==null || content.length==0) throw new IllegalArgumentException("Missing content.");
		
    	if (content.length>=MINIMUM_CHUNK_SIZE_BYTES) 
    		throw new IllegalArgumentException("String must be less than "+MINIMUM_CHUNK_SIZE_BYTES+" bytes.");
		String contentMD5 = null;
		try {
			contentMD5 = MD5ChecksumHelper.getMD5ChecksumForByteArray(content);
		} catch (IOException e) {
			throw new SynapseException(e);
		}
    	 
		CreateChunkedFileTokenRequest ccftr = new CreateChunkedFileTokenRequest();
		ccftr.setFileName("content");
		ccftr.setContentType(contentType.toString());
		ccftr.setContentMD5(contentMD5);
		// Start the upload
		ChunkedFileToken token = createChunkedFileUploadToken(ccftr);
		
		// because of the restriction on string length there will be exactly one chunk
 	   	List<Long> chunkNumbers = new ArrayList<Long>();
		long currentChunkNumber = 1;
		chunkNumbers.add(currentChunkNumber);
		ChunkRequest request = new ChunkRequest();
		request.setChunkedFileToken(token);
		request.setChunkNumber((long) currentChunkNumber);
		URL presignedURL = createChunkedPresignedUrl(request);
		getSharedClientConnection().putBytesToURL(presignedURL, content, contentType.toString());

		CompleteAllChunksRequest cacr = new CompleteAllChunksRequest();
		cacr.setChunkedFileToken(token);
		cacr.setChunkNumbers(chunkNumbers);
		UploadDaemonStatus status = startUploadDeamon(cacr);
		State state = status.getState();
		if (state.equals(State.FAILED)) throw new IllegalStateException("Message creation failed: "+status.getErrorMessage());
			
		long backOffMillis = 100L; // initially just 1/10 sec, but will exponentially increase
		while (state.equals(State.PROCESSING) && backOffMillis<=MAX_BACKOFF_MILLIS) {
			try {
				Thread.sleep(backOffMillis);
			} catch (InterruptedException e) {
				// continue
			}
			status = getCompleteUploadDaemonStatus(status.getDaemonId());
			state = status.getState();
			if (state.equals(State.FAILED)) throw new IllegalStateException("Message creation failed: "+status.getErrorMessage());
			backOffMillis *= 2; // exponential backoff
		}
		
		if (!state.equals(State.COMPLETED)) throw new IllegalStateException("Message creation failed: "+status.getErrorMessage());

		return status.getFileHandleId();
    }
    
	private static final ContentType STRING_MESSAGE_CONTENT_TYPE = ContentType.create("text/plain", MESSAGE_CHARSET);
	
	/**
	 * Convenience function to upload a simple string message body, then send message using resultant fileHandleId
	 * @param message
	 * @param messageBody
	 * @return the created message
	 * @throws SynapseException
	 */
	@Override
	public MessageToUser sendStringMessage(MessageToUser message, String messageBody)
			throws SynapseException {
		if (message.getFileHandleId()!=null) throw new IllegalArgumentException("Expected null fileHandleId but found "+message.getFileHandleId());
		String fileHandleId = uploadToFileHandle(messageBody.getBytes(MESSAGE_CHARSET), STRING_MESSAGE_CONTENT_TYPE);
		message.setFileHandleId(fileHandleId);
    	return sendMessage(message);		
	}
	
	/**
	 * Convenience function to upload a simple string message body, then send message to entity owner using resultant fileHandleId
	 * @param message
	 * @param entityId
	 * @param messageBody
	 * @return the created message
	 * @throws SynapseException
	 */
	@Override
	public MessageToUser sendStringMessage(MessageToUser message, String entityId, String messageBody)
			throws SynapseException {
				if (message.getFileHandleId()!=null) throw new IllegalArgumentException("Expected null fileHandleId but found "+message.getFileHandleId());
				String fileHandleId = uploadToFileHandle(messageBody.getBytes(MESSAGE_CHARSET), STRING_MESSAGE_CONTENT_TYPE);
				message.setFileHandleId(fileHandleId);
		    	return sendMessage(message, entityId);		
	}
	
	
	@Override
	public MessageToUser sendMessage(MessageToUser message, String entityId) throws SynapseException {
		String uri = ENTITY + "/" + entityId + "/" + MESSAGE;
		try {
			String jsonBody = EntityFactory.createJSONStringForEntity(message);
			JSONObject obj = getSharedClientConnection().postJson(repoEndpoint, uri, jsonBody, getUserAgent());
			return EntityFactory.createEntityFromJSONObject(obj, MessageToUser.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public PaginatedResults<MessageBundle> getInbox(
			List<MessageStatusType> inboxFilter, MessageSortBy orderBy,
			Boolean descending, long limit, long offset) throws SynapseException {
		String uri = setMessageParameters(MESSAGE_INBOX, inboxFilter, orderBy, descending, limit, offset);
		try {
			JSONObject obj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
			PaginatedResults<MessageBundle> messages = new PaginatedResults<MessageBundle>(MessageBundle.class);
			messages.initializeFromJSONObject(new JSONObjectAdapterImpl(obj));
			return messages;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public PaginatedResults<MessageToUser> getOutbox(MessageSortBy orderBy,
			Boolean descending, long limit, long offset) throws SynapseException {
		String uri = setMessageParameters(MESSAGE_OUTBOX, null, orderBy, descending, limit, offset);
		try {
			JSONObject obj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
			PaginatedResults<MessageToUser> messages = new PaginatedResults<MessageToUser>(MessageToUser.class);
			messages.initializeFromJSONObject(new JSONObjectAdapterImpl(obj));
			return messages;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public MessageToUser getMessage(String messageId) throws SynapseException {
		String uri = MESSAGE + "/" + messageId;
		try {
			JSONObject obj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
			return EntityFactory.createEntityFromJSONObject(obj, MessageToUser.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public MessageToUser forwardMessage(String messageId,
			MessageRecipientSet recipients) throws SynapseException {
		String uri = MESSAGE + "/" + messageId + FORWARD;
		try {
			String jsonBody = EntityFactory.createJSONStringForEntity(recipients);
			JSONObject obj = getSharedClientConnection().postJson(repoEndpoint, uri, jsonBody, getUserAgent());
			return EntityFactory.createEntityFromJSONObject(obj, MessageToUser.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public PaginatedResults<MessageToUser> getConversation(
			String associatedMessageId, MessageSortBy orderBy,
			Boolean descending, long limit, long offset) throws SynapseException {
		String uri = setMessageParameters(MESSAGE + "/" + associatedMessageId + CONVERSATION, null, orderBy, descending, limit, offset);
		try {
			JSONObject obj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
			PaginatedResults<MessageToUser> messages = new PaginatedResults<MessageToUser>(MessageToUser.class);
			messages.initializeFromJSONObject(new JSONObjectAdapterImpl(obj));
			return messages;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public void updateMessageStatus(MessageStatus status) throws SynapseException {
		String uri = MESSAGE_STATUS;
		try {
			String jsonBody = EntityFactory.createJSONStringForEntity(status);
			getSharedClientConnection().putJson(repoEndpoint, uri, jsonBody, getUserAgent());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public void deleteMessage(String messageId) throws SynapseException {
		String uri = MESSAGE + "/" + messageId;
		getSharedClientConnection().deleteUri(repoEndpoint, uri, getUserAgent());
	}
	
	@Override
	public String downloadMessage(String messageId) throws SynapseException, MalformedURLException, IOException {
		String uri = MESSAGE + "/" + messageId + FILE;
		return getSharedClientConnection().getDirect(repoEndpoint, uri, getUserAgent());
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
			jsonObject = getSharedClientConnection().putJson(repoEndpoint, url, jsonObject.toString(), getUserAgent());
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
		getSharedClientConnection().deleteUri(repoEndpoint, uri, getUserAgent());
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
			JSONObject jsonObj = getSharedClientConnection().putJson(repoEndpoint, url, obj.toString(), getUserAgent());
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
		getSharedClientConnection().deleteUri(repoEndpoint, uri, getUserAgent());
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
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, url.toString(), getUserAgent());
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
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, url.toString(), getUserAgent());
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
	
	@Override
	public PaginatedResults<Evaluation> getAvailableEvaluationsPaginated(int offset, int limit) throws SynapseException {
		String url = AVAILABLE_EVALUATION_URI_PATH + "?" + OFFSET + "=" + offset + "&limit=" + limit;
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
			JSONObject jsonObj = getSharedClientConnection().putJson(repoEndpoint, url, obj.toString(), getUserAgent());
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
		getSharedClientConnection().deleteUri(repoEndpoint, uri, getUserAgent());
	}
	
	/**
	 * Adds the authenticated user as a Participant in Evaluation evalId
	 */
	@Override
	public Participant createParticipant(String evalId) throws SynapseException {
		if (evalId == null) throw new IllegalArgumentException("Evaluation id cannot be null");
		String uri = createEntityUri(EVALUATION_URI_PATH, evalId) + "/" + PARTICIPANT;
		JSONObject jsonObj = getSharedClientConnection().postUri(repoEndpoint, uri, getUserAgent());
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
		getSharedClientConnection().deleteUri(repoEndpoint, uri, getUserAgent());
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
			JSONObject jsonObj = getSharedClientConnection().putJson(repoEndpoint, url, obj.toString(), getUserAgent());
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
		getSharedClientConnection().deleteUri(repoEndpoint, uri, getUserAgent());
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
		String url = EVALUATION_URI_PATH + "/" +
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
	
			JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, queryUri, getUserAgent());
			JSONObjectAdapter joa = new JSONObjectAdapterImpl(jsonObj);
			return new QueryTableResults(joa);
		} catch (Exception e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public StorageUsageSummaryList getStorageUsageSummary(List<StorageUsageDimension> aggregation) 
			throws SynapseException {
		String uri = STORAGE_SUMMARY_PATH;
		if (aggregation != null && aggregation.size() > 0) {
			uri += "?aggregation=" + StringUtils.join(aggregation, ",");
		}
		
		try {
			JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
			return EntityFactory.createEntityFromJSONObject(jsonObj, StorageUsageSummaryList.class);
		} catch (JSONObjectAdapterException e) {
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
		getSharedClientConnection().putJson(repoEndpoint, url, null, getUserAgent());
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
		getSharedClientConnection().putJson(repoEndpoint, url, null, getUserAgent());
	}

	/**
	 * Retrieves entities (in the trash can) deleted by the user.
	 */
	@Override
	public PaginatedResults<TrashedEntity> viewTrashForUser(long offset, long limit) throws SynapseException {
		String url = TRASHCAN_VIEW + "?" + OFFSET + "=" + offset + "&" + LIMIT + "=" + limit;
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, url, getUserAgent());
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
		getSharedClientConnection().putJson(repoEndpoint, url, null, getUserAgent());
	}

	/**
	 * Purges the trash can for the user. All the entities in the trash will be permanently deleted.
	 */
	@Override
	public void purgeTrashForUser() throws SynapseException {
		getSharedClientConnection().putJson(repoEndpoint, TRASHCAN_PURGE, null, getUserAgent());
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
		JSONObject jsonObj = getSharedClientConnection().postUri(repoEndpoint, url, getUserAgent());
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
		getSharedClientConnection().deleteUri(repoEndpoint, uri, getUserAgent());
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
		getSharedClientConnection().putJson(repoEndpoint, url, null, getUserAgent());
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
			JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, url, getUserAgent());
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
			JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, url, getUserAgent());
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
			String url = "/secretKey";
			JSONObject jsonObj = getSharedClientConnection().getJson(authEndpoint, url, getUserAgent());
			SecretKey key = EntityFactory.createEntityFromJSONObject(jsonObj, SecretKey.class);
			return key.getSecretKey();
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public void invalidateApiKey() throws SynapseException {
		getSharedClientConnection().invalidateApiKey(getUserAgent());
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
			JSONObject jsonObj = getSharedClientConnection().putJson(repoEndpoint, url, obj.toString(), getUserAgent());
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
	public RowReferenceSet appendRowsToTable(RowSet toAppend) throws SynapseException {
		if(toAppend == null) throw new IllegalArgumentException("RowSet cannot be null");
		if(toAppend.getTableId() == null) throw new IllegalArgumentException("RowSet.tableId cannot be null");
		String url = getRepoEndpoint()+ENTITY+"/"+toAppend.getTableId()+TABLE;
		return asymmetricalPost(url, toAppend, RowReferenceSet.class);
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
			return getJSONEntity(url, ColumnModel.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public List<ColumnModel> getColumnModelsForTableEntity(String tableEntityId) throws SynapseException {
		if(tableEntityId == null) throw new IllegalArgumentException("tableEntityId cannot be null");
		String url = ENTITY+"/"+tableEntityId+COLUMN;
		try {
			PaginatedColumnModels pcm = getJSONEntity(url, PaginatedColumnModels.class);
			return pcm.getResults();
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public PaginatedColumnModels listColumnModels(String prefix, Long limit, Long offset) throws SynapseException {
		String url = buildListColumnModelUrl(prefix, limit, offset);
		try {
			return  getJSONEntity(url, PaginatedColumnModels.class);
			
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
			return getUrl(uri);
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
			JSONObject jsonObj = getSharedClientConnection().putJson(repoEndpoint, TEAM, obj.toString(), getUserAgent());
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
		getSharedClientConnection().deleteUri(repoEndpoint, TEAM + "/" + teamId, getUserAgent());
	}

	@Override
	public void addTeamMember(String teamId, String memberId)
			throws SynapseException {
		getSharedClientConnection().putJson(repoEndpoint, TEAM + "/" + teamId + MEMBER + "/" + memberId, new JSONObject().toString(),
				getUserAgent());
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
	public TeamMember getTeamMember(String teamId, String memberId) throws SynapseException {
		JSONObject jsonObj = getEntity(TEAM + "/" + teamId + MEMBER + "/" + memberId);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		TeamMember result = new TeamMember();
		try {
			result.initializeFromJSONObject(adapter);
			return result;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
		
	}


	@Override
	public void removeTeamMember(String teamId, String memberId)
			throws SynapseException {
		getSharedClientConnection().deleteUri(repoEndpoint, TEAM + "/" + teamId + MEMBER + "/" + memberId, getUserAgent());
	}
	
	@Override
	public void setTeamMemberPermissions(String teamId, String memberId,
			boolean isAdmin) throws SynapseException {
		getSharedClientConnection().putJson(repoEndpoint,
				TEAM + "/" + teamId + MEMBER + "/" + memberId + PERMISSION + "?"
				+ TEAM_MEMBERSHIP_PERMISSION + "="
 + isAdmin, "",
				getUserAgent());
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
	public PaginatedResults<MembershipInvtnSubmission> getOpenMembershipInvitationSubmissions(
			String teamId, String inviteeId, long limit, long offset)
			throws SynapseException {
		
		String uri = null;
		if (inviteeId==null) {
			uri = TEAM+"/"+teamId+OPEN_MEMBERSHIP_INVITATION+"?"+OFFSET+"="+offset+"&"+LIMIT+"="+limit;
		} else {
			uri = TEAM+"/"+teamId+OPEN_MEMBERSHIP_INVITATION+"?"+INVITEE_ID_REQUEST_PARAMETER+"="+inviteeId+"&"+OFFSET+"="+offset+"&"+LIMIT+"="+limit;
		
		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<MembershipInvtnSubmission> results = new PaginatedResults<MembershipInvtnSubmission>(MembershipInvtnSubmission.class);
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
		getSharedClientConnection().deleteUri(repoEndpoint, MEMBERSHIP_INVITATION + "/" + invitationId, getUserAgent());
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
	public PaginatedResults<MembershipRqstSubmission> getOpenMembershipRequestSubmissions(
			String requesterId, String teamId, long limit, long offset)
			throws SynapseException {
		String uri = null;
		if (teamId==null) {
			uri = USER+"/"+requesterId+OPEN_MEMBERSHIP_REQUEST+"?"+OFFSET+"="+offset+"&"+LIMIT+"="+limit;
		} else {
			uri = USER+"/"+requesterId+OPEN_MEMBERSHIP_REQUEST+"?"+TEAM_ID_REQUEST_PARAMETER+"="+teamId+"&"+OFFSET+"="+offset+"&"+LIMIT+"="+limit;
		
		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<MembershipRqstSubmission> results = new PaginatedResults<MembershipRqstSubmission>(MembershipRqstSubmission.class);
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
		getSharedClientConnection().deleteUri(repoEndpoint, MEMBERSHIP_REQUEST + "/" + requestId, getUserAgent());
	}
	
	@Override
	public void updateTeamSearchCache() throws SynapseException {
		getSharedClientConnection().postUri(repoEndpoint, TEAM_UPDATE_SEARCH_CACHE, getUserAgent());
	}

	@Override
	public void createUser(NewUser user) throws SynapseException {
		createUser(user, DomainType.SYNAPSE);
	}
	
	@Override
	public void createUser(NewUser user, DomainType domain) throws SynapseException {
		try {
			JSONObject obj = EntityFactory.createJSONObjectForEntity(user);
			getSharedClientConnection().postJson(authEndpoint, "/user", obj.toString(), getUserAgent(),
					getParameterMapForDomain(domain));
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public void sendPasswordResetEmail(String email) throws SynapseException{
		sendPasswordResetEmail(email, DomainType.SYNAPSE);
	}
	
	@Override
	public void sendPasswordResetEmail(String email, DomainType domain) throws SynapseException{
		try {
			Username user = new Username();
			user.setEmail(email);
			JSONObject obj = EntityFactory.createJSONObjectForEntity(user);
			getSharedClientConnection().postJson(authEndpoint, "/user/password/email", obj.toString(), getUserAgent(),
					getParameterMapForDomain(domain));
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	@Override
	public void changePassword(String sessionToken, String newPassword) throws SynapseException {
		try {
			ChangePasswordRequest change = new ChangePasswordRequest();
			change.setSessionToken(sessionToken);
			change.setPassword(newPassword);
			
			JSONObject obj = EntityFactory.createJSONObjectForEntity(change);
			getSharedClientConnection().postJson(authEndpoint, "/user/password", obj.toString(), getUserAgent());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public void signTermsOfUse(String sessionToken, boolean acceptTerms) throws SynapseException {
		signTermsOfUse(sessionToken, DomainType.SYNAPSE, acceptTerms);
	}
	
	@Override
	public void signTermsOfUse(String sessionToken, DomainType domain, boolean acceptTerms) throws SynapseException {
		try {
			Session session = new Session();
			session.setSessionToken(sessionToken);
			session.setAcceptsTermsOfUse(acceptTerms);

			JSONObject obj = EntityFactory.createJSONObjectForEntity(session);
			getSharedClientConnection().postJson(authEndpoint, "/termsOfUse", obj.toString(), getUserAgent(),
					getParameterMapForDomain(domain));
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}

	@Override
	public Session passThroughOpenIDParameters(String queryString) throws SynapseException {
		return passThroughOpenIDParameters(queryString, false);
	}
	
	@Override
	public Session passThroughOpenIDParameters(String queryString, Boolean createUserIfNecessary) throws SynapseException {
		return passThroughOpenIDParameters(queryString, createUserIfNecessary, DomainType.SYNAPSE);
	}
	
	@Override
	public Session passThroughOpenIDParameters(String queryString, Boolean createUserIfNecessary, DomainType domain) throws SynapseException {
		try {
			URIBuilder builder = new URIBuilder();
			builder.setPath("/openIdCallback");
			builder.setQuery(queryString);
			builder.setParameter("org.sagebionetworks.createUserIfNecessary", createUserIfNecessary.toString());
			JSONObject session = getSharedClientConnection().postJson(authEndpoint, builder.toString(), "",
					getUserAgent(), getParameterMapForDomain(domain));
			return EntityFactory.createEntityFromJSONObject(session, Session.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	private Map<String,String> getParameterMapForDomain(DomainType domain) {
		Map<String, String> parameters = Maps.newHashMap();
		parameters.put(AuthorizationConstants.DOMAIN_PARAM, domain.name());
		return parameters;
	}
}

