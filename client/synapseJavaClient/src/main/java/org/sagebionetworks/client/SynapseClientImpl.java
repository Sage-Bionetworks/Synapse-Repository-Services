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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseResultNotReadyException;
import org.sagebionetworks.client.exceptions.SynapseTableUnavailableException;
import org.sagebionetworks.client.exceptions.SynapseTermsOfUseException;
import org.sagebionetworks.downloadtools.FileUtils;
import org.sagebionetworks.evaluation.model.BatchUploadResponse;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionContributor;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusBatch;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.TeamSubmissionEligibility;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengePagedResults;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.ChallengeTeamPagedResults;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityInstanceFactory;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.LogEntry;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResponseMessage;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.ServiceConstants.AttachmentType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UserBundle;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.annotation.AnnotationsUtils;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.auth.ChangePasswordRequest;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.SecretKey;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.ReplyCount;
import org.sagebionetworks.repo.model.discussion.ThreadCount;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.entity.query.EntityQuery;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResults;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteAllChunksRequest;
import org.sagebionetworks.repo.model.file.CompleteChunkedFileRequest;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileCopyRequest;
import org.sagebionetworks.repo.model.file.S3FileCopyResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.S3UploadDestination;
import org.sagebionetworks.repo.model.file.State;
import org.sagebionetworks.repo.model.file.TempFileProviderImpl;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.OAuthUrlRequest;
import org.sagebionetworks.repo.model.oauth.OAuthUrlResponse;
import org.sagebionetworks.repo.model.oauth.OAuthValidationRequest;
import org.sagebionetworks.repo.model.principal.AccountSetupInfo;
import org.sagebionetworks.repo.model.principal.AddEmailInfo;
import org.sagebionetworks.repo.model.principal.AliasCheckRequest;
import org.sagebionetworks.repo.model.principal.AliasCheckResponse;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAliasResponse;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.query.QueryTableResults;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.Quiz;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.model.request.ReferenceList;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.sagebionetworks.repo.model.subscription.Etag;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.repo.model.table.AppendableRowSet;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowReferenceSetResults;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableFileHandleResults;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.utils.MD5ChecksumHelper;

import com.google.common.collect.Maps;

/**
 * Low-level Java Client API for Synapse REST APIs
 */
public class SynapseClientImpl extends BaseClientImpl implements SynapseClient {

	public static final String SYNPASE_JAVA_CLIENT = "Synpase-Java-Client/";

	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

	private static final Logger log = LogManager
			.getLogger(SynapseClientImpl.class.getName());

	private static final long MAX_UPLOAD_DAEMON_MS = 60 * 1000;

	private static final String DEFAULT_REPO_ENDPOINT = "https://repo-prod.prod.sagebase.org/repo/v1";
	private static final String DEFAULT_AUTH_ENDPOINT = SharedClientConnection.DEFAULT_AUTH_ENDPOINT;
	private static final String DEFAULT_FILE_ENDPOINT = "https://repo-prod.prod.sagebase.org/file/v1";

	private static final String ACCOUNT = "/account";
	private static final String EMAIL_VALIDATION = "/emailValidation";
	private static final String ACCOUNT_EMAIL_VALIDATION = ACCOUNT
			+ EMAIL_VALIDATION;
	private static final String EMAIL = "/email";
	private static final String PORTAL_ENDPOINT_PARAM = "portalEndpoint";
	private static final String SET_AS_NOTIFICATION_EMAIL_PARAM = "setAsNotificationEmail";
	private static final String EMAIL_PARAM = "email";

	private static final String PARAM_GENERATED_BY = "generatedBy";

	private static final String QUERY = "/query";
	private static final String QUERY_URI = QUERY + "?query=";
	private static final String REPO_SUFFIX_VERSION = "/version";
	private static final String ANNOTATION_URI_SUFFIX = "annotations";
	protected static final String ADMIN = "/admin";
	protected static final String STACK_STATUS = ADMIN + "/synapse/status";
	private static final String ENTITY = "/entity";
	private static final String GENERATED_BY_SUFFIX = "/generatedBy";

	private static final String ENTITY_URI_PATH = "/entity";
	private static final String ENTITY_ACL_PATH_SUFFIX = "/acl";
	private static final String ENTITY_ACL_RECURSIVE_SUFFIX = "?recursive=true";
	private static final String ENTITY_BUNDLE_PATH = "/bundle?mask=";
	private static final String BUNDLE = "/bundle";
	private static final String BENEFACTOR = "/benefactor"; // from
															// org.sagebionetworks.repo.web.UrlHelpers
	private static final String ACTIVITY_URI_PATH = "/activity";
	private static final String GENERATED_PATH = "/generated";
	private static final String FAVORITE_URI_PATH = "/favorite";
	private static final String PROJECTS_URI_PATH = "/projects";

	public static final String PRINCIPAL = "/principal";
	public static final String PRINCIPAL_AVAILABLE = PRINCIPAL + "/available";
	public static final String NOTIFICATION_EMAIL = "/notificationEmail";

	private static final String WIKI_URI_TEMPLATE = "/%1$s/%2$s/wiki";
	private static final String WIKI_ID_URI_TEMPLATE = "/%1$s/%2$s/wiki/%3$s";
	private static final String WIKI_TREE_URI_TEMPLATE = "/%1$s/%2$s/wikiheadertree";
	private static final String WIKI_URI_TEMPLATE_V2 = "/%1$s/%2$s/wiki2";
	private static final String WIKI_ID_URI_TEMPLATE_V2 = "/%1$s/%2$s/wiki2/%3$s";
	private static final String WIKI_ID_VERSION_URI_TEMPLATE_V2 = "/%1$s/%2$s/wiki2/%3$s/%4$s";
	private static final String WIKI_TREE_URI_TEMPLATE_V2 = "/%1$s/%2$s/wikiheadertree2";
	private static final String WIKI_ORDER_HINT_URI_TEMPLATE_V2 = "/%1$s/%2$s/wiki2orderhint";
	private static final String WIKI_HISTORY_V2 = "/wikihistory";
	private static final String ATTACHMENT_HANDLES = "/attachmenthandles";
	private static final String ATTACHMENT_FILE = "/attachment";
	private static final String MARKDOWN_FILE = "/markdown";
	private static final String ATTACHMENT_FILE_PREVIEW = "/attachmentpreview";
	private static final String FILE_NAME_PARAMETER = "?fileName=";
	private static final String REDIRECT_PARAMETER = "redirect=";
	private static final String OFFSET_PARAMETER = "offset=";
	private static final String LIMIT_PARAMETER = "limit=";
	private static final String VERSION_PARAMETER = "?wikiVersion=";
	private static final String AND_VERSION_PARAMETER = "&wikiVersion=";
	private static final String AND_LIMIT_PARAMETER = "&" + LIMIT_PARAMETER;
	private static final String AND_REDIRECT_PARAMETER = "&"
			+ REDIRECT_PARAMETER;
	private static final String QUERY_REDIRECT_PARAMETER = "?"
			+ REDIRECT_PARAMETER;
	private static final String ACCESS_TYPE_PARAMETER = "accessType";

	private static final String EVALUATION_URI_PATH = "/evaluation";
	private static final String AVAILABLE_EVALUATION_URI_PATH = "/evaluation/available";
	private static final String NAME = "name";
	private static final String ALL = "/all";
	private static final String STATUS = "/status";
	private static final String STATUS_BATCH = "/statusBatch";
	private static final String LOCK_ACCESS_REQUIREMENT = "/lockAccessRequirement";
	private static final String SUBMISSION = "submission";
	private static final String SUBMISSION_BUNDLE = SUBMISSION + BUNDLE;
	private static final String SUBMISSION_ALL = SUBMISSION + ALL;
	private static final String SUBMISSION_STATUS_ALL = SUBMISSION + STATUS
			+ ALL;
	private static final String SUBMISSION_BUNDLE_ALL = SUBMISSION + BUNDLE
			+ ALL;
	private static final String STATUS_SUFFIX = "?status=";
	private static final String EVALUATION_ACL_URI_PATH = "/evaluation/acl";
	private static final String EVALUATION_QUERY_URI_PATH = EVALUATION_URI_PATH
			+ "/" + SUBMISSION + QUERY_URI;
	private static final String EVALUATION_IDS_FILTER_PARAM = "evaluationIds";
	private static final String SUBMISSION_ELIGIBILITY = "/submissionEligibility";
	private static final String SUBMISSION_ELIGIBILITY_HASH = "submissionEligibilityHash";

	private static final String MESSAGE = "/message";
	private static final String FORWARD = "/forward";
	private static final String CONVERSATION = "/conversation";
	private static final String MESSAGE_STATUS = MESSAGE + "/status";
	private static final String MESSAGE_INBOX = MESSAGE + "/inbox";
	private static final String MESSAGE_OUTBOX = MESSAGE + "/outbox";
	private static final String MESSAGE_INBOX_FILTER_PARAM = "inboxFilter";
	private static final String MESSAGE_ORDER_BY_PARAM = "orderBy";
	private static final String MESSAGE_DESCENDING_PARAM = "descending";

	private static final String STORAGE_SUMMARY_PATH = "/storageSummary";

	protected static final String ASYNC_START = "/async/start";
	protected static final String ASYNC_GET = "/async/get/";

	protected static final String COLUMN = "/column";
	protected static final String COLUMN_BATCH = COLUMN + "/batch";
	protected static final String TABLE = "/table";
	protected static final String ROW_ID = "/row";
	protected static final String ROW_VERSION = "/version";
	protected static final String TABLE_QUERY = TABLE + "/query";
	protected static final String TABLE_QUERY_NEXTPAGE = TABLE_QUERY
			+ "/nextPage";
	protected static final String TABLE_DOWNLOAD_CSV = TABLE + "/download/csv";
	protected static final String TABLE_UPLOAD_CSV = TABLE + "/upload/csv";
	protected static final String TABLE_UPLOAD_CSV_PREVIEW = TABLE
			+ "/upload/csv/preview";
	protected static final String TABLE_APPEND = TABLE + "/append";

	protected static final String ASYNCHRONOUS_JOB = "/asynchronous/job";

	private static final String USER_PROFILE_PATH = "/userProfile";
	private static final String NOTIFICATION_SETTINGS = "/notificationSettings";

	private static final String PROFILE_IMAGE = "/image";
	private static final String PROFILE_IMAGE_PREVIEW = PROFILE_IMAGE+"/preview";

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
	private static final String EXTERNAL_FILE_HANDLE_S3 = "/externalFileHandle/s3";
	private static final String EXTERNAL_FILE_HANDLE_PROXY = "/externalFileHandle/proxy";
	private static final String FILE_HANDLES = "/filehandles";
	protected static final String S3_FILE_COPY = FILE + "/s3FileCopy";
	
	protected static final String FILE_BULK = FILE+"/bulk";

	private static final String CREATE_CHUNKED_FILE_UPLOAD_TOKEN = "/createChunkedFileUploadToken";
	private static final String CREATE_CHUNKED_FILE_UPLOAD_CHUNK_URL = "/createChunkedFileUploadChunkURL";
	private static final String START_COMPLETE_UPLOAD_DAEMON = "/startCompleteUploadDaemon";
	private static final String COMPLETE_UPLOAD_DAEMON_STATUS = "/completeUploadDaemonStatus";

	private static final String TRASHCAN_TRASH = "/trashcan/trash";
	private static final String TRASHCAN_RESTORE = "/trashcan/restore";
	private static final String TRASHCAN_VIEW = "/trashcan/view";
	private static final String TRASHCAN_PURGE = "/trashcan/purge";

	private static final String CHALLENGE = "/challenge";
	private static final String REGISTRATABLE_TEAM = "/registratableTeam";
	private static final String CHALLENGE_TEAM = "/challengeTeam";
	private static final String SUBMISSION_TEAMS = "/submissionTeams";

	private static final String LOG = "/log";

	private static final String DOI = "/doi";

	private static final String ETAG = "etag";

	private static final String PROJECT_SETTINGS = "/projectSettings";

	private static final String STORAGE_LOCATION = "/storageLocation";

	public static final String AUTH_OAUTH_2 = "/oauth2";
	public static final String AUTH_OAUTH_2_AUTH_URL = AUTH_OAUTH_2+"/authurl";
	public static final String AUTH_OAUTH_2_SESSION = AUTH_OAUTH_2+"/session";
	public static final String AUTH_OAUTH_2_ALIAS = AUTH_OAUTH_2+"/alias";
	
	private static final String VERIFICATION_SUBMISSION = "/verificationSubmission";
	private static final String CURRENT_VERIFICATION_STATE = "currentVerificationState";
	private static final String VERIFICATION_STATE = "/state";
	private static final String VERIFIED_USER_ID = "verifiedUserId";
	private static final String USER_BUNDLE = "/bundle";
	private static final String FILE_ASSOCIATE_TYPE = "fileAssociateType";
	private static final String FILE_ASSOCIATE_ID = "fileAssociateId";
	
	// web request pagination parameters
	public static final String LIMIT = "limit";
	public static final String OFFSET = "offset";

	private static final long MAX_BACKOFF_MILLIS = 5 * 60 * 1000L; // five
																	// minutes

	/**
	 * The character encoding to use with strings which are the body of email
	 * messages
	 */
	private static final Charset MESSAGE_CHARSET = Charset.forName("UTF-8");

	private static final String LIMIT_1_OFFSET_1 = "' limit 1 offset 1";
	private static final String SELECT_ID_FROM_ENTITY_WHERE_PARENT_ID = "select id from entity where parentId == '";

	// Team
	protected static final String TEAM = "/team";
	protected static final String TEAMS = "/teams";
	private static final String TEAM_LIST = "/teamList";
	private static final String MEMBER_LIST = "/memberList";
	protected static final String USER = "/user";
	protected static final String NAME_FRAGMENT_FILTER = "fragment";
	protected static final String ICON = "/icon";
	protected static final String TEAM_MEMBERS = "/teamMembers";
	protected static final String MEMBER = "/member";
	protected static final String PERMISSION = "/permission";
	protected static final String MEMBERSHIP_STATUS = "/membershipStatus";
	protected static final String TEAM_MEMBERSHIP_PERMISSION = "isAdmin";

	// membership invitation
	private static final String MEMBERSHIP_INVITATION = "/membershipInvitation";
	private static final String OPEN_MEMBERSHIP_INVITATION = "/openInvitation";
	private static final String TEAM_ID_REQUEST_PARAMETER = "teamId";
	private static final String INVITEE_ID_REQUEST_PARAMETER = "inviteeId";
	// membership request
	private static final String MEMBERSHIP_REQUEST = "/membershipRequest";
	private static final String OPEN_MEMBERSHIP_REQUEST = "/openRequest";
	private static final String REQUESTOR_ID_REQUEST_PARAMETER = "requestorId";
	
	public static final String ACCEPT_INVITATION_ENDPOINT_PARAM = "acceptInvitationEndpoint";
	public static final String ACCEPT_REQUEST_ENDPOINT_PARAM = "acceptRequestEndpoint";
	public static final String NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM = "notificationUnsubscribeEndpoint";
	public static final String TEAM_ENDPOINT_PARAM = "teamEndpoint";
	public static final String CHALLENGE_ENDPOINT_PARAM = "challengeEndpoint";
	
	private static final String CERTIFIED_USER_TEST = "/certifiedUserTest";
	private static final String CERTIFIED_USER_TEST_RESPONSE = "/certifiedUserTestResponse";
	private static final String CERTIFIED_USER_PASSING_RECORD = "/certifiedUserPassingRecord";
	private static final String CERTIFIED_USER_PASSING_RECORDS = "/certifiedUserPassingRecords";
	private static final String CERTIFIED_USER_STATUS = "/certificationStatus";

	private static final String PROJECT = "/project";
	private static final String FORUM = "/forum";
	private static final String THREAD = "/thread";
	private static final String THREADS = "/threads";
	private static final String THREAD_COUNT = "/threadcount";
	private static final String THREAD_TITLE = "/title";
	private static final String DISCUSSION_MESSAGE = "/message";
	private static final String REPLY = "/reply";
	private static final String REPLIES = "/replies";
	private static final String REPLY_COUNT = "/replycount";
	private static final String URL = "/messageUrl";
	private static final String PIN = "/pin";
	private static final String UNPIN = "/unpin";

	private static final String SUBSCRIPTION = "/subscription";
	private static final String LIST = "/list";
	private static final String OBJECT_TYPE_PARAM = "objectType";
	private static final String OBJECT = "/object";	

	private static final String PRINCIPAL_ID_REQUEST_PARAM = "principalId";

	protected String repoEndpoint;
	protected String authEndpoint;
	protected String fileEndpoint;

	/**
	 * The maximum number of threads that should be used to upload asynchronous
	 * file chunks.
	 */
	private static final int MAX_NUMBER_OF_THREADS = 2;

	/**
	 * This thread pool is used for asynchronous file chunk uploads.
	 */
	private ExecutorService fileUplaodthreadPool = Executors
			.newFixedThreadPool(MAX_NUMBER_OF_THREADS);

	/**
	 * Note: 5 MB is currently the minimum size of a single part of S3
	 * Multi-part upload, so any file chunk must be at least this size.
	 */
	public static final int MINIMUM_CHUNK_SIZE_BYTES = ((int) Math.pow(2, 20)) * 5;

	/**
	 * Default constructor uses the default repository and auth services
	 * endpoints.
	 */
	public SynapseClientImpl() {
		// Use the default implementations
		this(new SharedClientConnection(new HttpClientProviderImpl()));
	}

	/**
	 * Will use the same connection as the other client
	 * 
	 * @param clientProvider
	 * @param dataUploader
	 */
	public SynapseClientImpl(BaseClient other) {
		this(other.getSharedClientConnection());
	}

	/**
	 * Will use the shared connection provider and data uploader.
	 * 
	 * @param clientProvider
	 * @param dataUploader
	 */
	public SynapseClientImpl(SharedClientConnection sharedClientConnection) {
		super(SYNPASE_JAVA_CLIENT + ClientVersionInfo.getClientVersionInfo(),
				sharedClientConnection);
		if (sharedClientConnection == null)
			throw new IllegalArgumentException(
					"SharedClientConnection cannot be null");
		setRepositoryEndpoint(DEFAULT_REPO_ENDPOINT);
		setAuthEndpoint(DEFAULT_AUTH_ENDPOINT);
		setFileEndpoint(DEFAULT_FILE_ENDPOINT);
	}

	/**
	 * Default constructor uses the default repository and auth services
	 * endpoints.
	 */
	public SynapseClientImpl(DomainType domain) {
		// Use the default implementations
		this(new HttpClientProviderImpl(), domain);
	}

	/**
	 * Will use the provided client provider and data uploader.
	 * 
	 * @param clientProvider
	 * @param dataUploader
	 */
	public SynapseClientImpl(HttpClientProvider clientProvider, DomainType domain) {
		this(new SharedClientConnection(clientProvider, domain));
	}

	/**
	 * Use this method to override the default implementation of
	 * {@link HttpClientProvider}
	 * 
	 * @param clientProvider
	 */
	public void setHttpClientProvider(HttpClientProvider clientProvider) {
		getSharedClientConnection().setHttpClientProvider(clientProvider);
	}
	
	/**
	 * Returns a helper class for making specialized Http requests
	 * 
	 */
	private HttpClientHelper getHttpClientHelper() {
		return new HttpClientHelper(getSharedClientConnection().getHttpClientProvider());
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
	 * 
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
	 * 
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
	public String getFileEndpoint() {
		return this.fileEndpoint;
	}
	

	/**
	 * Lookup the endpoint for a given type.
	 * @param type
	 * @return
	 */
	String getEndpointForType(RestEndpointType type){
		switch(type){
		case auth:
			return getAuthEndpoint();
		case repo:
			return getRepoEndpoint();
		case file:
			return getFileEndpoint();
		default:
			throw new IllegalArgumentException("Unknown type: "+type);
		}
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
	 * @param userName
	 *            the userName to set
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
	 * @param apiKey
	 *            the apiKey to set
	 */
	@Override
	public void setApiKey(String apiKey) {
		getSharedClientConnection().setApiKey(apiKey);
	}

	@Deprecated
	@Override
	public Session login(String username, String password)
			throws SynapseException {
		return getSharedClientConnection().login(username, password,
				getUserAgent());
	}

	@Override
	public LoginResponse login(LoginRequest request)
			throws SynapseException {
		return getSharedClientConnection().login(request, getUserAgent());
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
		return getSharedClientConnection().revalidateSession(getUserAgent());
	}

	/********************
	 * Mid Level Repository Service APIs
	 * 
	 * @throws SynapseException
	 ********************/

	@Override
	public AliasCheckResponse checkAliasAvailable(AliasCheckRequest request)
			throws SynapseException {
		String url = PRINCIPAL_AVAILABLE;
		return asymmetricalPost(getRepoEndpoint(), url, request,
				AliasCheckResponse.class, null);
	}

	/**
	 * Send an email validation message as a precursor to creating a new user
	 * account.
	 * 
	 * @param user
	 *            the first name, last name and email address for the new user
	 * @param portalEndpoint
	 *            the GUI endpoint (is the basis for the link in the email
	 *            message) Must generate a valid email when a set of request
	 *            parameters is appended to the end.
	 */
	@Override
	public void newAccountEmailValidation(NewUser user, String portalEndpoint)
			throws SynapseException {
		if (user == null)
			throw new IllegalArgumentException("email can not be null.");
		if (portalEndpoint == null)
			throw new IllegalArgumentException(
					"portalEndpoint can not be null.");

		String uri = ACCOUNT_EMAIL_VALIDATION;
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put(PORTAL_ENDPOINT_PARAM, portalEndpoint);

		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		try {
			JSONObject obj = new JSONObject(user.writeToJSONObject(
					toUpdateAdapter).toJSONString());
			getSharedClientConnection().postJson(repoEndpoint, uri,
					obj.toString(), getUserAgent(), paramMap);
		} catch (JSONException e) {
			throw new SynapseClientException(e);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Create a new account, following email validation. Sets the password and
	 * logs the user in, returning a valid session token
	 * 
	 * @param accountSetupInfo
	 *            Note: Caller may override the first/last name, but not the
	 *            email, given in 'newAccountEmailValidation'
	 * @return session
	 * @throws NotFoundException
	 */
	@Override
	public Session createNewAccount(AccountSetupInfo accountSetupInfo)
			throws SynapseException {
		if (accountSetupInfo == null)
			throw new IllegalArgumentException(
					"accountSetupInfo can not be null.");

		String uri = ACCOUNT;

		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		try {
			JSONObject obj = new JSONObject(accountSetupInfo.writeToJSONObject(
					toUpdateAdapter).toJSONString());
			JSONObject result = getSharedClientConnection().postJson(
					repoEndpoint, uri, obj.toString(), getUserAgent(), null);
			return EntityFactory.createEntityFromJSONObject(result,
					Session.class);
		} catch (JSONException e) {
			throw new SynapseClientException(e);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Send an email validation as a precursor to adding a new email address to
	 * an existing account.
	 * 
	 * @param email
	 *            the email which is claimed by the user
	 * @param portalEndpoint
	 *            the GUI endpoint (is the basis for the link in the email
	 *            message) Must generate a valid email when a set of request
	 *            parameters is appended to the end.
	 * @throws NotFoundException
	 */
	@Override
	public void additionalEmailValidation(Long userId, String email,
			String portalEndpoint) throws SynapseException {
		if (userId == null)
			throw new IllegalArgumentException("userId can not be null.");
		if (email == null)
			throw new IllegalArgumentException("email can not be null.");
		if (portalEndpoint == null)
			throw new IllegalArgumentException(
					"portalEndpoint can not be null.");

		String uri = ACCOUNT + "/" + userId + EMAIL_VALIDATION;
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put(PORTAL_ENDPOINT_PARAM, portalEndpoint);

		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		try {
			Username emailRequestBody = new Username();
			emailRequestBody.setEmail(email);
			JSONObject obj = new JSONObject(emailRequestBody.writeToJSONObject(
					toUpdateAdapter).toJSONString());
			getSharedClientConnection().postJson(repoEndpoint, uri,
					obj.toString(), getUserAgent(), paramMap);
		} catch (JSONException e) {
			throw new SynapseClientException(e);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Add a new email address to an existing account.
	 * 
	 * @param addEmailInfo
	 *            the token sent to the user via email
	 * @param setAsNotificationEmail
	 *            if true then set the new email address to be the user's
	 *            notification address
	 * @throws NotFoundException
	 */
	@Override
	public void addEmail(AddEmailInfo addEmailInfo,
			Boolean setAsNotificationEmail) throws SynapseException {
		if (addEmailInfo == null)
			throw new IllegalArgumentException("addEmailInfo can not be null.");

		String uri = EMAIL;
		Map<String, String> paramMap = new HashMap<String, String>();
		if (setAsNotificationEmail != null)
			paramMap.put(SET_AS_NOTIFICATION_EMAIL_PARAM,
					setAsNotificationEmail.toString());

		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		try {
			JSONObject obj = new JSONObject(addEmailInfo.writeToJSONObject(
					toUpdateAdapter).toJSONString());
			getSharedClientConnection().postJson(repoEndpoint, uri,
					obj.toString(), getUserAgent(), paramMap);
		} catch (JSONException e) {
			throw new SynapseClientException(e);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Remove an email address from an existing account.
	 * 
	 * @param email
	 *            the email to remove. Must be an established email address for
	 *            the user
	 * @throws NotFoundException
	 */
	@Override
	public void removeEmail(String email) throws SynapseException {
		if (email == null)
			throw new IllegalArgumentException("email can not be null.");

		String uri = EMAIL;
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put(EMAIL_PARAM, email);
		getSharedClientConnection().deleteUri(repoEndpoint, uri,
				getUserAgent(), paramMap);

	}

	/**
	 * This sets the email used for user notifications, i.e. when a Synapse
	 * message is sent and if the user has elected to receive messages by email,
	 * then this is the email address at which the user will receive the
	 * message. Note: The given email address must already be established as
	 * being owned by the user.
	 */
	public void setNotificationEmail(String email) throws SynapseException {
		if (email == null) {
			throw new IllegalArgumentException("email can not be null.");
		}

		String url = NOTIFICATION_EMAIL;
		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		try {
			Username un = new Username();
			un.setEmail(email);
			obj = new JSONObject(un.writeToJSONObject(toUpdateAdapter)
					.toJSONString());
			getSharedClientConnection().putJson(repoEndpoint, url,
					obj.toString(), getUserAgent());
		} catch (JSONException e) {
			throw new SynapseClientException(e);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * This gets the email used for user notifications, i.e. when a Synapse
	 * message is sent and if the user has elected to receive messages by email,
	 * then this is the email address at which the user will receive the
	 * message.
	 * 
	 * @throws SynapseException
	 */
	public String getNotificationEmail() throws SynapseException {
		String url = NOTIFICATION_EMAIL;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			Username un = new Username();
			un.initializeFromJSONObject(adapter);
			return un.getEmail();
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}

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
		return getSharedClientConnection().postJson(repoEndpoint, uri,
				entity.toString(), getUserAgent(), null);
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
	public <T extends Entity> T createEntity(T entity) throws SynapseException {
		return createEntity(entity, null);
	}

	/**
	 * Create a new Entity.
	 * 
	 * @param <T>
	 * @param entity
	 * @param activityId
	 *            set generatedBy relationship to the new entity
	 * @return the newly created entity
	 * @throws SynapseException
	 */
	@Override
	public <T extends Entity> T createEntity(T entity, String activityId)
			throws SynapseException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");
		entity.setConcreteType(entity.getClass().getName());
		String uri = ENTITY_URI_PATH;
		if (activityId != null)
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
			throw new SynapseClientException(e);
		}
	}
	
	public <T extends JSONEntity> List<T> createJSONEntityFromListWrapper(String uri, ListWrapper<T> entity, Class<T> elementType)
			throws SynapseException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");
		try {
			String jsonString = EntityFactory.createJSONStringForEntity(entity);
			JSONObject responseBody = getSharedClientConnection().postJson(
					getRepoEndpoint(), uri, jsonString, getUserAgent(), null, null);
			return ListWrapper.unwrap(new JSONObjectAdapterImpl(responseBody), elementType);

		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
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
	public EntityBundle createEntityBundle(EntityBundleCreate ebc)
			throws SynapseException {
		return createEntityBundle(ebc, null);
	}

	/**
	 * Create an Entity, Annotations, and ACL with a single call.
	 * 
	 * @param ebc
	 * @param activityId
	 *            the activity to create a generatedBy relationship with the
	 *            entity in the Bundle.
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public EntityBundle createEntityBundle(EntityBundleCreate ebc,
			String activityId) throws SynapseException {
		if (ebc == null)
			throw new IllegalArgumentException("EntityBundle cannot be null");
		String url = ENTITY_URI_PATH + BUNDLE;
		JSONObject jsonObject;
		if (activityId != null)
			url += "?" + PARAM_GENERATED_BY + "=" + activityId;
		try {
			// Convert to JSON
			jsonObject = EntityFactory.createJSONObjectForEntity(ebc);
			// Create
			jsonObject = createJSONObject(url, jsonObject);
			// Convert returned JSON to EntityBundle
			return EntityFactory.createEntityFromJSONObject(jsonObject,
					EntityBundle.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
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
	public EntityBundle updateEntityBundle(String entityId,
			EntityBundleCreate ebc) throws SynapseException {
		return updateEntityBundle(entityId, ebc, null);
	}

	/**
	 * Update an Entity, Annotations, and ACL with a single call.
	 * 
	 * @param ebc
	 * @param activityId
	 *            the activity to create a generatedBy relationship with the
	 *            entity in the Bundle.
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public EntityBundle updateEntityBundle(String entityId,
			EntityBundleCreate ebc, String activityId) throws SynapseException {
		if (ebc == null)
			throw new IllegalArgumentException("EntityBundle cannot be null");
		String url = ENTITY_URI_PATH + "/" + entityId + BUNDLE;
		JSONObject jsonObject;
		try {
			// Convert to JSON
			jsonObject = EntityFactory.createJSONObjectForEntity(ebc);

			// Update. Bundles do not have their own etags, so we use an
			// empty requestHeaders object.
			jsonObject = getSharedClientConnection().putJson(repoEndpoint, url,
					jsonObject.toString(), getUserAgent());

			// Convert returned JSON to EntityBundle
			return EntityFactory.createEntityFromJSONObject(jsonObject,
					EntityBundle.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Get an entity using its ID.
	 * 
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
	 * 
	 * @param entityId
	 * @param versionNumber
	 * @return the entity
	 * @throws SynapseException
	 */
	@Override
	public Entity getEntityByIdForVersion(String entityId, Long versionNumber)
			throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		String url = ENTITY_URI_PATH + "/" + entityId;
		if (versionNumber != null) {
			url += REPO_SUFFIX_VERSION + "/" + versionNumber;
		}
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		// Get the type from the object
		if (!adapter.has("entityType"))
			throw new RuntimeException("EntityType returned was null!");
		try {
			String entityType = adapter.getString("entityType");
			Entity entity = (Entity) EntityInstanceFactory.singleton().newInstance(entityType);
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
	public EntityBundle getEntityBundle(String entityId, int partsMask)
			throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		String url = ENTITY_URI_PATH + "/" + entityId + ENTITY_BUNDLE_PATH
				+ partsMask;
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
	 * @param entityId
	 *            - the entity id to retrieve
	 * @param versionNumber
	 *            - the specific version to retrieve
	 * @param partsMask
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public EntityBundle getEntityBundle(String entityId, Long versionNumber,
			int partsMask) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		if (versionNumber == null)
			throw new IllegalArgumentException("versionNumber cannot be null");
		String url = ENTITY_URI_PATH + "/" + entityId + REPO_SUFFIX_VERSION
				+ "/" + versionNumber + ENTITY_BUNDLE_PATH + partsMask;
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
	public PaginatedResults<VersionInfo> getEntityVersions(String entityId,
			int offset, int limit) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		String url = ENTITY_URI_PATH + "/" + entityId + REPO_SUFFIX_VERSION
				+ "?" + OFFSET + "=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<VersionInfo> results = new PaginatedResults<VersionInfo>(
				VersionInfo.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	public static <T extends JSONEntity> T initializeFromJSONObject(
			JSONObject o, Class<T> clazz) throws SynapseException {
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
			throw new SynapseClientException(e);
		} catch (InstantiationException e) {
			throw new SynapseClientException(e);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public AccessControlList getACL(String entityId) throws SynapseException {
		String uri = ENTITY_URI_PATH + "/" + entityId + ENTITY_ACL_PATH_SUFFIX;
		JSONObject json = getEntity(uri);
		return initializeFromJSONObject(json, AccessControlList.class);
	}

	@Override
	public EntityHeader getEntityBenefactor(String entityId)
			throws SynapseException {
		String uri = ENTITY_URI_PATH + "/" + entityId + BENEFACTOR;
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
	public void updateMyProfile(UserProfile userProfile)
			throws SynapseException {
		try {
			String uri = USER_PROFILE_PATH;
			getSharedClientConnection().putJson(
					repoEndpoint,
					uri,
					EntityFactory.createJSONObjectForEntity(userProfile)
							.toString(), getUserAgent());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public ResponseMessage updateNotificationSettings(NotificationSettingsSignedToken token) throws SynapseException {
		return asymmetricalPut(getRepoEndpoint(), NOTIFICATION_SETTINGS, token,
				ResponseMessage.class);
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
	public UserGroupHeaderResponsePage getUserGroupHeadersByIds(List<String> ids)
			throws SynapseException {
		String uri = listToString(ids);
		JSONObject json = getEntity(uri);
		return initializeFromJSONObject(json, UserGroupHeaderResponsePage.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.client.SynapseClient#getUserProfilePictureUrl(java.lang.String)
	 */
	@Override
	public URL getUserProfilePictureUrl(String ownerId) throws ClientProtocolException, MalformedURLException, IOException, SynapseException {
		String url = USER_PROFILE_PATH+"/"+ownerId+PROFILE_IMAGE+"?"+REDIRECT_PARAMETER+"false";
		return getUrl(url);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.client.SynapseClient#getUserProfilePicturePreviewUrl(java.lang.String)
	 */
	@Override
	public URL getUserProfilePicturePreviewUrl(String ownerId) throws ClientProtocolException, MalformedURLException, IOException, SynapseException {
		String url = USER_PROFILE_PATH+"/"+ownerId+PROFILE_IMAGE_PREVIEW+"?"+REDIRECT_PARAMETER+"false";
		return getUrl(url);
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
	public UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(String prefix)
			throws SynapseException, UnsupportedEncodingException {
		String encodedPrefix = URLEncoder.encode(prefix, "UTF-8");
		JSONObject json = getEntity(USER_GROUP_HEADER_PREFIX_PATH
				+ encodedPrefix);
		return initializeFromJSONObject(json, UserGroupHeaderResponsePage.class);
	}

	@Override
	public UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(
			String prefix, long limit, long offset) throws SynapseException,
			UnsupportedEncodingException {
		String encodedPrefix = URLEncoder.encode(prefix, "UTF-8");
		JSONObject json = getEntity(USER_GROUP_HEADER_PREFIX_PATH
				+ encodedPrefix + "&" + LIMIT_PARAMETER + limit + "&"
				+ OFFSET_PARAMETER + offset);
		return initializeFromJSONObject(json, UserGroupHeaderResponsePage.class);
	}

	/**
	 * Update an ACL. Default to non-recursive application.
	 */
	@Override
	public AccessControlList updateACL(AccessControlList acl)
			throws SynapseException {
		return updateACL(acl, false);
	}

	/**
	 * Update an entity's ACL. If 'recursive' is set to true, then any child
	 * ACLs will be deleted, such that all child entities inherit this ACL.
	 */
	@Override
	public AccessControlList updateACL(AccessControlList acl, boolean recursive)
			throws SynapseException {
		String entityId = acl.getId();
		String uri = ENTITY_URI_PATH + "/" + entityId + ENTITY_ACL_PATH_SUFFIX;
		if (recursive)
			uri += ENTITY_ACL_RECURSIVE_SUFFIX;
		try {
			JSONObject jsonAcl = EntityFactory.createJSONObjectForEntity(acl);
			jsonAcl = getSharedClientConnection().putJson(repoEndpoint, uri,
					jsonAcl.toString(), getUserAgent());
			return initializeFromJSONObject(jsonAcl, AccessControlList.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void deleteACL(String entityId) throws SynapseException {
		String uri = ENTITY_URI_PATH + "/" + entityId + ENTITY_ACL_PATH_SUFFIX;
		getSharedClientConnection()
				.deleteUri(repoEndpoint, uri, getUserAgent());
	}

	@Override
	public AccessControlList createACL(AccessControlList acl)
			throws SynapseException {
		String entityId = acl.getId();
		String uri = ENTITY_URI_PATH + "/" + entityId + ENTITY_ACL_PATH_SUFFIX;
		try {
			JSONObject jsonAcl = EntityFactory.createJSONObjectForEntity(acl);
			jsonAcl = createJSONObject(uri, jsonAcl);
			return initializeFromJSONObject(jsonAcl, AccessControlList.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<UserProfile> getUsers(int offset, int limit)
			throws SynapseException {
		String uri = "/user?" + OFFSET + "=" + offset + "&limit=" + limit;
		JSONObject jsonUsers = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonUsers);
		PaginatedResults<UserProfile> results = new PaginatedResults<UserProfile>(
				UserProfile.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	@Override
	public List<UserProfile> listUserProfiles(List<Long> userIds) throws SynapseException {
		try {
			IdList idList = new IdList();
			idList.setList(userIds);
			String jsonString = EntityFactory.createJSONStringForEntity(idList);
			JSONObject responseBody = getSharedClientConnection().postJson(
					getRepoEndpoint(), USER_PROFILE_PATH, jsonString, getUserAgent(), null, null);
			return ListWrapper.unwrap(new JSONObjectAdapterImpl(responseBody), UserProfile.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	@Override
	public PaginatedResults<UserGroup> getGroups(int offset, int limit)
			throws SynapseException {
		String uri = "/userGroup?" + OFFSET + "=" + offset + "&limit=" + limit;
		JSONObject jsonUsers = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonUsers);
		PaginatedResults<UserGroup> results = new PaginatedResults<UserGroup>(
				UserGroup.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Get the current user's permission for a given entity.
	 * 
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public UserEntityPermissions getUsersEntityPermissions(String entityId)
			throws SynapseException {
		String url = ENTITY_URI_PATH + "/" + entityId + "/permissions";
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		UserEntityPermissions uep = new UserEntityPermissions();
		try {
			uep.initializeFromJSONObject(adapter);
			return uep;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Get the current user's permission for a given entity.
	 * 
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public boolean canAccess(String entityId, ACCESS_TYPE accessType)
			throws SynapseException {
		return canAccess(entityId, ObjectType.ENTITY, accessType);
	}

	@Override
	public boolean canAccess(String id, ObjectType type, ACCESS_TYPE accessType)
			throws SynapseException {
		if (id == null)
			throw new IllegalArgumentException("id cannot be null");
		if (type == null)
			throw new IllegalArgumentException("ObjectType cannot be null");
		if (accessType == null)
			throw new IllegalArgumentException("AccessType cannot be null");

		if (ObjectType.ENTITY.equals(type)) {
			return canAccess(ENTITY_URI_PATH + "/" + id + "/access?accessType="
					+ accessType.name());
		} else if (ObjectType.EVALUATION.equals(type)) {
			return canAccess(EVALUATION_URI_PATH + "/" + id
					+ "/access?accessType=" + accessType.name());
		} else
			throw new IllegalArgumentException("ObjectType not supported: "
					+ type.toString());
	}

	private boolean canAccess(String serviceUrl) throws SynapseException {
		try {
			JSONObject jsonObj = getEntity(serviceUrl);
			String resultString = null;
			try {
				resultString = jsonObj.getString("result");
			} catch (NullPointerException e) {
				throw new SynapseClientException(jsonObj.toString(), e);
			}
			return Boolean.parseBoolean(resultString);
		} catch (JSONException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Get the annotations for an entity.
	 * 
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public Annotations getAnnotations(String entityId) throws SynapseException {
		String url = ENTITY_URI_PATH + "/" + entityId + "/annotations";
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
	 * 
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public Annotations updateAnnotations(String entityId, Annotations updated)
			throws SynapseException {
		try {
			String url = ENTITY_URI_PATH + "/" + entityId + "/annotations";
			JSONObject jsonObject = EntityFactory
					.createJSONObjectForEntity(updated);
			// Update
			jsonObject = getSharedClientConnection().putJson(repoEndpoint, url,
					jsonObject.toString(), getUserAgent());
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
	private static Class<AccessRequirement> getAccessRequirementClassFromType(
			String s) {
		try {
			return (Class<AccessRequirement>) Class.forName(s);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends AccessRequirement> T createAccessRequirement(T ar)
			throws SynapseException {

		if (ar == null)
			throw new IllegalArgumentException(
					"AccessRequirement cannot be null");
		ar.setConcreteType(ar.getClass().getName());
		// Get the json for this entity
		JSONObject jsonObject;
		try {
			jsonObject = EntityFactory.createJSONObjectForEntity(ar);
			// Create the entity
			jsonObject = createJSONObject(ACCESS_REQUIREMENT, jsonObject);
			// Now convert to Object to an entity
			return (T) initializeFromJSONObject(jsonObject,
					getAccessRequirementClassFromType(ar.getConcreteType()));
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends AccessRequirement> T updateAccessRequirement(T ar)
			throws SynapseException {
		if (ar == null)
			throw new IllegalArgumentException(
					"AccessRequirement cannot be null");
		String url = createEntityUri(ACCESS_REQUIREMENT + "/", ar.getId()
				.toString());
		try {
			JSONObject toUpdateJsonObject = EntityFactory
					.createJSONObjectForEntity(ar);
			JSONObject updatedJsonObject = getSharedClientConnection().putJson(
					repoEndpoint, url, toUpdateJsonObject.toString(),
					getUserAgent());
			return (T) initializeFromJSONObject(updatedJsonObject,
					getAccessRequirementClassFromType(ar.getConcreteType()));
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}

	@Override
	public ACTAccessRequirement createLockAccessRequirement(String entityId)
			throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("Entity id cannot be null");
		JSONObject jsonObj = getSharedClientConnection().postUri(repoEndpoint,
				ENTITY + "/" + entityId + LOCK_ACCESS_REQUIREMENT,
				getUserAgent());
		return initializeFromJSONObject(jsonObj, ACTAccessRequirement.class);
	}

	@Override
	public void deleteAccessRequirement(Long arId) throws SynapseException {
		getSharedClientConnection().deleteUri(repoEndpoint,
				ACCESS_REQUIREMENT + "/" + arId, getUserAgent());
	}

	@Override
	public PaginatedResults<AccessRequirement> getUnmetAccessRequirements(
			RestrictableObjectDescriptor subjectId, ACCESS_TYPE accessType)
			throws SynapseException {
		String uri = null;
		if (RestrictableObjectType.ENTITY == subjectId.getType()) {
			uri = ENTITY + "/" + subjectId.getId()
					+ ACCESS_REQUIREMENT_UNFULFILLED;
		} else if (RestrictableObjectType.EVALUATION == subjectId.getType()) {
			uri = EVALUATION_URI_PATH + "/" + subjectId.getId()
					+ ACCESS_REQUIREMENT_UNFULFILLED;
		} else if (RestrictableObjectType.TEAM == subjectId.getType()) {
			uri = TEAM + "/" + subjectId.getId()
					+ ACCESS_REQUIREMENT_UNFULFILLED;
		} else {
			throw new SynapseClientException("Unsupported type "
					+ subjectId.getType());
		}
		if (accessType != null) {
			uri += "?" + ACCESS_TYPE_PARAMETER + "=" + accessType;
		}
		JSONObject jsonAccessRequirements = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(
				jsonAccessRequirements);
		PaginatedResults<AccessRequirement> results = new PaginatedResults<AccessRequirement>();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public AccessRequirement getAccessRequirement(Long requirementId)
			throws SynapseException {
		String uri = ACCESS_REQUIREMENT + "/" + requirementId;
		JSONObject jsonObj = getEntity(uri);
		try {
			return EntityFactory.createEntityFromJSONObject(jsonObj,
					AccessRequirement.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<AccessRequirement> getAccessRequirements(
			RestrictableObjectDescriptor subjectId) throws SynapseException {
		String uri = null;
		if (RestrictableObjectType.ENTITY == subjectId.getType()) {
			uri = ENTITY + "/" + subjectId.getId() + ACCESS_REQUIREMENT;
		} else if (RestrictableObjectType.EVALUATION == subjectId.getType()) {
			uri = EVALUATION_URI_PATH + "/" + subjectId.getId()
					+ ACCESS_REQUIREMENT;
		} else if (RestrictableObjectType.TEAM == subjectId.getType()) {
			uri = TEAM + "/" + subjectId.getId() + ACCESS_REQUIREMENT;
		} else {
			throw new SynapseClientException("Unsupported type "
					+ subjectId.getType());
		}
		JSONObject jsonAccessRequirements = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(
				jsonAccessRequirements);
		PaginatedResults<AccessRequirement> results = new PaginatedResults<AccessRequirement>(AccessRequirement.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static Class<AccessApproval> getAccessApprovalClassFromType(String s) {
		try {
			return (Class<AccessApproval>) Class.forName(s);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends AccessApproval> T createAccessApproval(T aa)
			throws SynapseException {

		if (aa == null)
			throw new IllegalArgumentException("AccessApproval cannot be null");
		aa.setConcreteType(aa.getClass().getName());
		// Get the json for this entity
		JSONObject jsonObject;
		try {
			jsonObject = EntityFactory.createJSONObjectForEntity(aa);
			// Create the entity
			jsonObject = createJSONObject(ACCESS_APPROVAL, jsonObject);
			// Now convert to Object to an entity
			return (T) initializeFromJSONObject(jsonObject,
					getAccessApprovalClassFromType(aa.getConcreteType()));
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}

	}

	@Override
	public AccessApproval getAccessApproval(Long approvalId)
			throws SynapseException {
		String uri = ACCESS_APPROVAL + "/" + approvalId;
		JSONObject jsonObj = getEntity(uri);
		try {
			return EntityFactory.createEntityFromJSONObject(jsonObj,
					AccessApproval.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Get a dataset, layer, preview, annotations, etc...
	 * 
	 * @return the retrieved entity
	 */
	@Override
	public JSONObject getEntity(String uri) throws SynapseException {
		return getSharedClientConnection().getJson(repoEndpoint, uri,
				getUserAgent());
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
			throw new SynapseClientException(e);
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
	 * @param activityId
	 *            activity to create generatedBy conenction to
	 * @return the updated entity
	 * @throws SynapseException
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T extends Entity> T putEntity(T entity, String activityId)
			throws SynapseException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");
		try {
			String uri = createEntityUri(ENTITY_URI_PATH, entity.getId());
			if (activityId != null)
				uri += "?" + PARAM_GENERATED_BY + "=" + activityId;
			JSONObject jsonObject;
			jsonObject = EntityFactory.createJSONObjectForEntity(entity);
			jsonObject = getSharedClientConnection().putJson(repoEndpoint, uri,
					jsonObject.toString(), getUserAgent());
			return (T) EntityFactory.createEntityFromJSONObject(jsonObject,
					entity.getClass());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Deletes a dataset, layer, etc.. This only moves the entity to the trash
	 * can. To permanently delete the entity, use deleteAndPurgeEntity().
	 */
	@Override
	public <T extends Entity> void deleteEntity(T entity)
			throws SynapseException {
		deleteEntity(entity, null);
	}

	/**
	 * Deletes a dataset, layer, etc.. By default, it moves the entity to the
	 * trash can. There is the option to skip the trash and delete the entity
	 * permanently.
	 */
	@Override
	public <T extends Entity> void deleteEntity(T entity, Boolean skipTrashCan)
			throws SynapseException {
		if (entity == null) {
			throw new IllegalArgumentException("Entity cannot be null");
		}
		deleteEntityById(entity.getId(), skipTrashCan);
	}

	/**
	 * Deletes a dataset, layer, etc..
	 */
	@Override
	public <T extends Entity> void deleteAndPurgeEntity(T entity)
			throws SynapseException {
		deleteEntity(entity);
		purgeTrashForUser(entity.getId());
	}

	/**
	 * Deletes a dataset, layer, etc.. This only moves the entity to the trash
	 * can. To permanently delete the entity, use deleteAndPurgeEntity().
	 */
	@Override
	public void deleteEntityById(String entityId) throws SynapseException {
		deleteEntityById(entityId, null);
	}

	/**
	 * Deletes a dataset, layer, etc.. By default, it moves the entity to the
	 * trash can. There is the option to skip the trash and delete the entity
	 * permanently.
	 */
	@Override
	public void deleteEntityById(String entityId, Boolean skipTrashCan)
			throws SynapseException {
		if (entityId == null) {
			throw new IllegalArgumentException("entityId cannot be null");
		}
		String uri = createEntityUri(ENTITY_URI_PATH, entityId);
		if (skipTrashCan != null && skipTrashCan) {
			uri = uri + "?" + ServiceConstants.SKIP_TRASH_CAN_PARAM + "=true";
		}
		getSharedClientConnection()
				.deleteUri(repoEndpoint, uri, getUserAgent());
	}

	/**
	 * Deletes a dataset, layer, etc..
	 */
	@Override
	public void deleteAndPurgeEntityById(String entityId)
			throws SynapseException {
		deleteEntityById(entityId);
		purgeTrashForUser(entityId);
	}

	@Override
	public <T extends Entity> void deleteEntityVersion(T entity,
			Long versionNumber) throws SynapseException {
		if (entity == null)
			throw new IllegalArgumentException("Entity cannot be null");
		deleteEntityVersionById(entity.getId(), versionNumber);
	}

	@Override
	public void deleteEntityVersionById(String entityId, Long versionNumber)
			throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		if (versionNumber == null)
			throw new IllegalArgumentException("VersionNumber cannot be null");
		String uri = createEntityUri(ENTITY_URI_PATH, entityId);
		uri += REPO_SUFFIX_VERSION + "/" + versionNumber;
		getSharedClientConnection()
				.deleteUri(repoEndpoint, uri, getUserAgent());
	}

	/**
	 * Get the hierarchical path to this entity
	 * 
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
	 * 
	 * @param entityId
	 * @param urlPrefix
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public EntityPath getEntityPath(String entityId) throws SynapseException {
		String url = ENTITY_URI_PATH + "/" + entityId + "/path";
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
	public PaginatedResults<EntityHeader> getEntityTypeBatch(List<String> entityIds)
			throws SynapseException {
		String url = ENTITY_URI_PATH + "/type"; // TODO move UrlHelpers
												// someplace shared so that we
												// can UrlHelpers.ENTITY_TYPE
		url += "?"
				+ ServiceConstants.BATCH_PARAM
				+ "="
				+ StringUtils.join(entityIds,
						ServiceConstants.BATCH_PARAM_VALUE_SEPARATOR);
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<EntityHeader> results = new PaginatedResults<EntityHeader>(
				EntityHeader.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public PaginatedResults<EntityHeader> getEntityHeaderBatch(
			List<Reference> references) throws SynapseException {
		ReferenceList list = new ReferenceList();
		list.setReferences(references);
		String url = ENTITY_URI_PATH + "/header";
		JSONObject jsonObject;
		try {
			jsonObject = EntityFactory.createJSONObjectForEntity(list);
			// POST
			jsonObject = createJSONObject(url, jsonObject);
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObject);
			PaginatedResults<EntityHeader> results = new PaginatedResults<EntityHeader>(
					EntityHeader.class);
			results.initializeFromJSONObject(adapter);
			return results;

		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
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
	@Deprecated
	public FileHandleResults createFileHandles(List<File> files)
			throws SynapseException {
		if (files == null)
			throw new IllegalArgumentException("File list cannot be null");
		try {
			List<FileHandle> list = new LinkedList<FileHandle>();
			for (File file : files) {
				// We need to determine the content type of the file
				String contentType = guessContentTypeFromStream(file);
				S3FileHandle handle = createFileHandle(file, contentType);
				list.add(handle);
			}
			FileHandleResults results = new FileHandleResults();
			results.setList(list);
			return results;
		} catch (IOException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public FileHandleResults createFileHandles(List<File> files,
			String parentEntityId) throws SynapseException {
		if (files == null)
			throw new IllegalArgumentException("File list cannot be null");
		try {
			List<FileHandle> list = new LinkedList<FileHandle>();
			for (File file : files) {
				// We need to determine the content type of the file
				String contentType = guessContentTypeFromStream(file);
				FileHandle handle = createFileHandle(file, contentType,
						parentEntityId);
				list.add(handle);
			}
			FileHandleResults results = new FileHandleResults();
			results.setList(list);
			return results;
		} catch (IOException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public URL getFileHandleTemporaryUrl(String fileHandleId)
			throws IOException, SynapseException {
		String uri = getFileHandleTemporaryURI(fileHandleId, false);
		return getUrl(getFileEndpoint(), uri);
	}

	private String getFileHandleTemporaryURI(String fileHandleId,
			boolean redirect) {
		return FILE_HANDLE + "/" + fileHandleId + "/url"
				+ QUERY_REDIRECT_PARAMETER + redirect;
	}

	@Override
	public void downloadFromFileHandleTemporaryUrl(String fileHandleId,
			File destinationFile) throws SynapseException {
		String uri = getFileEndpoint()
				+ getFileHandleTemporaryURI(fileHandleId, true);
		getSharedClientConnection().downloadFromSynapse(uri, null,
				destinationFile, getUserAgent());
	}

	@Override
	@Deprecated
	public S3FileHandle createFileHandle(File temp, String contentType)
			throws SynapseException, IOException {
		return createFileHandleUsingChunkedUpload(temp, contentType, null, null);
	}

	@Override
	@Deprecated
	public S3FileHandle createFileHandle(File temp, String contentType,
			Boolean shouldPreviewBeCreated) throws SynapseException,
			IOException {
		return createFileHandleUsingChunkedUpload(temp, contentType,
				shouldPreviewBeCreated, null);
	}

	@Override
	public FileHandle createFileHandle(File temp, String contentType,
			String parentEntityId) throws SynapseException, IOException {
		return createFileHandle(temp, contentType, null, parentEntityId);
	}

	@Override
	public FileHandle createFileHandle(File temp, String contentType, Boolean shouldPreviewBeCreated, String parentEntityId)
			throws SynapseException, IOException {
		UploadDestination uploadDestination = getDefaultUploadDestination(parentEntityId);
		return createFileHandle(temp, contentType, shouldPreviewBeCreated, uploadDestination.getStorageLocationId(),
				uploadDestination.getUploadType());
	}

	@Override
	public FileHandle createFileHandle(File temp, String contentType, Boolean shouldPreviewBeCreated, String parentEntityId,
			Long storageLocationId) throws SynapseException, IOException {
		UploadDestination uploadDestination = getUploadDestination(parentEntityId, storageLocationId);
		return createFileHandle(temp, contentType, shouldPreviewBeCreated, storageLocationId, uploadDestination.getUploadType());
	}

	private FileHandle createFileHandle(File temp, String contentType, Boolean shouldPreviewBeCreated, Long storageLocationId,
			UploadType uploadType) throws SynapseException, IOException {
		if (storageLocationId == null) {
			// default to S3
			return createFileHandleUsingChunkedUpload(temp, contentType, shouldPreviewBeCreated, null);
		}

		switch (uploadType) {
		case HTTPS:
		case SFTP:
			throw new NotImplementedException("SFTP and HTTPS uploads not implemented yet");
		case S3:
			return createFileHandleUsingChunkedUpload(temp, contentType, shouldPreviewBeCreated, storageLocationId);
		default:
			throw new NotImplementedException(uploadType.name() + " uploads not implemented yet");
		}
	}

	private S3FileHandle createFileHandleUsingChunkedUpload(File temp, String contentType, Boolean shouldPreviewBeCreated,
			Long storageLocationId) throws SynapseException,
			IOException {
		if (temp == null) {
			throw new IllegalArgumentException("File cannot be null");
		}
		if (contentType == null) {
			throw new IllegalArgumentException("Content type cannot be null");
		}

		CreateChunkedFileTokenRequest ccftr = new CreateChunkedFileTokenRequest();
		ccftr.setStorageLocationId(storageLocationId);
		ccftr.setContentType(contentType);
		ccftr.setFileName(temp.getName());
		// Calculate the MD5
		String md5 = MD5ChecksumHelper.getMD5Checksum(temp);
		ccftr.setContentMD5(md5);
		// Start the upload
		ChunkedFileToken token = createChunkedFileUploadToken(ccftr);
		// Now break the file into part as needed
		List<File> fileChunks = FileUtils.chunkFile(temp,
				MINIMUM_CHUNK_SIZE_BYTES);
		try {
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
			while (State.COMPLETED != status.getState()) {
				// Check for failure
				if (State.FAILED == status.getState()) {
					throw new SynapseClientException("Upload failed: "
							+ status.getErrorMessage());
				}
				log.debug("Waiting for upload daemon: " + status.toString());
				Thread.sleep(1000);
				status = getCompleteUploadDaemonStatus(status.getDaemonId());
				if (System.currentTimeMillis() - start > MAX_UPLOAD_DAEMON_MS) {
					throw new SynapseClientException(
							"Timed out waiting for upload daemon: "
									+ status.toString());
				}
			}
			// Complete the upload
			return (S3FileHandle) getRawFileHandle(status.getFileHandleId());
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			// Delete any tmep files created by this method. The original file
			// will not be deleted.
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
	private List<Long> uploadChunks(List<File> fileChunks,
			ChunkedFileToken token) throws SynapseException {
		try {
			List<Long> results = new LinkedList<Long>();
			// The future list
			List<Future<Long>> futureList = new ArrayList<Future<Long>>();
			// For each chunk create a worker and add it to the thread pool
			long chunkNumber = 1;
			for (File file : fileChunks) {
				// create a worker for each chunk
				ChunkRequest request = new ChunkRequest();
				request.setChunkedFileToken(token);
				request.setChunkNumber(chunkNumber);
				FileChunkUploadWorker worker = new FileChunkUploadWorker(this,
						request, file);
				// Add this the the thread pool
				Future<Long> future = fileUplaodthreadPool.submit(worker);
				futureList.add(future);
				chunkNumber++;
			}
			// Get all of the results
			for (Future<Long> future : futureList) {
				Long partNumber = future.get();
				results.add(partNumber);
			}
			return results;
		} catch (Exception e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * <P>
	 * This is a low-level API call for uploading large files. We recomend using
	 * the high-level API call for uploading files
	 * {@link #createFileHandle(File, String)}.
	 * </P>
	 * This is the first step in the low-level API used to upload large files to
	 * Synapse. The resulting {@link ChunkedFileToken} is required for all
	 * subsequent steps. Large file upload is exectued as follows:
	 * <ol>
	 * <li>{@link #createChunkedFileUploadToken(CreateChunkedFileTokenRequest)}</li>
	 * <li>{@link #createChunkedPresignedUrl(ChunkRequest)}</li>
	 * <li>{@link #addChunkToFile(ChunkRequest)}</li>
	 * <li>{@link #completeChunkFileUpload(CompleteChunkedFileRequest)}</li>
	 * </ol>
	 * Steps 2 & 3 are repated in for each file chunk. Note: All chunks can be
	 * sent asynchronously.
	 * 
	 * @param ccftr
	 * @return The @link {@link ChunkedFileToken} is required for all subsequent
	 *         steps.
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	@Override
	public ChunkedFileToken createChunkedFileUploadToken(
			CreateChunkedFileTokenRequest ccftr) throws SynapseException {
		if (ccftr == null)
			throw new IllegalArgumentException(
					"CreateChunkedFileTokenRequest cannot be null");
		if (ccftr.getFileName() == null)
			throw new IllegalArgumentException("FileName cannot be null");
		if (ccftr.getContentType() == null)
			throw new IllegalArgumentException("ContentType cannot be null");
		String url = CREATE_CHUNKED_FILE_UPLOAD_TOKEN;
		return asymmetricalPost(getFileEndpoint(), url, ccftr,
				ChunkedFileToken.class, null);
	}

	/**
	 * <P>
	 * This is a low-level API call for uploading large files. We recomend using
	 * the high-level API call for uploading files
	 * {@link #createFileHandle(File, String)}.
	 * </P>
	 * The second step in the low-level API used to upload large files to
	 * Synapse. This method is used to get a pre-signed URL that can be used to
	 * PUT the data of a single chunk to S3.
	 * 
	 * @param chunkRequest
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	@Override
	public URL createChunkedPresignedUrl(ChunkRequest chunkRequest)
			throws SynapseException {
		try {
			if (chunkRequest == null) {
				throw new IllegalArgumentException(
						"ChunkRequest cannot be null");
			}
			String uri = CREATE_CHUNKED_FILE_UPLOAD_CHUNK_URL;
			String data = EntityFactory.createJSONStringForEntity(chunkRequest);
			String responseBody = getSharedClientConnection().postStringDirect(
					getFileEndpoint(), uri, data, getUserAgent());
			return new URL(responseBody);
		} catch (IOException e) {
			throw new SynapseClientException(e);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	/**
	 * Put the contents of the passed file to the passed URL.
	 * 
	 * @param url
	 * @param file
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	@Override
	public String putFileToURL(URL url, File file, String contentType)
			throws SynapseException {
		return getHttpClientHelper().putFileToURL(url, file, contentType);
	}

	/**
	 * Start a daemon that will asycnrhounsously complete the multi-part upload.
	 * 
	 * @param cacr
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public UploadDaemonStatus startUploadDeamon(CompleteAllChunksRequest cacr)
			throws SynapseException {
		String url = START_COMPLETE_UPLOAD_DAEMON;
		return asymmetricalPost(getFileEndpoint(), url, cacr,
				UploadDaemonStatus.class, null);
	}

	/**
	 * Get the status of daemon used to complete the multi-part upload.
	 * 
	 * @param daemonId
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	@Override
	public UploadDaemonStatus getCompleteUploadDaemonStatus(String daemonId)
			throws SynapseException {
		String url = COMPLETE_UPLOAD_DAEMON_STATUS + "/" + daemonId;
		JSONObject json = getSynapseEntity(getFileEndpoint(), url);
		try {
			return EntityFactory.createEntityFromJSONObject(json,
					UploadDaemonStatus.class);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create an External File Handle. This is used to references a file that is
	 * not stored in Synpase.
	 * 
	 * @param efh
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	@Override
	public ExternalFileHandle createExternalFileHandle(ExternalFileHandle efh)
			throws JSONObjectAdapterException, SynapseException {
		String uri = EXTERNAL_FILE_HANDLE;
		return doCreateJSONEntity(getFileEndpoint(), uri, efh);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.client.SynapseClient#createExternalS3FileHandle(org.sagebionetworks.repo.model.file.S3FileHandle)
	 */
	@Override
	public S3FileHandle createExternalS3FileHandle(S3FileHandle handle) throws JSONObjectAdapterException, SynapseException{
		String uri = EXTERNAL_FILE_HANDLE_S3;
		return doCreateJSONEntity(getFileEndpoint(), uri, handle);
	}
	
	@Override
	public ProxyFileHandle createExternalProxyFileHandle(ProxyFileHandle handle) throws JSONObjectAdapterException, SynapseException{
		return doCreateJSONEntity(getFileEndpoint(), EXTERNAL_FILE_HANDLE_PROXY, handle);
	}

	@Override
	public S3FileHandle createS3FileHandleCopy(String originalFileHandleId, String name, String contentType)
			throws JSONObjectAdapterException, SynapseException {
		String uri = FILE_HANDLE + "/" + originalFileHandleId + "/copy";
		S3FileHandle changes = new S3FileHandle();
		changes.setFileName(name);
		changes.setContentType(contentType);
		return doCreateJSONEntity(getFileEndpoint(), uri, changes);
	}

	@Override
	public String s3FileCopyAsyncStart(List<String> fileEntityIds, String destinationBucket, Boolean overwrite, String baseKey)
			throws SynapseException {
		S3FileCopyRequest s3FileCopyRequest = new S3FileCopyRequest();
		s3FileCopyRequest.setFiles(fileEntityIds);
		s3FileCopyRequest.setBucket(destinationBucket);
		s3FileCopyRequest.setOverwrite(overwrite);
		s3FileCopyRequest.setBaseKey(baseKey);
		return startAsynchJob(AsynchJobType.S3FileCopy, s3FileCopyRequest);
	}

	@Override
	public S3FileCopyResults s3FileCopyAsyncGet(String asyncJobToken) throws SynapseException, SynapseResultNotReadyException {
		return (S3FileCopyResults) getAsyncResult(AsynchJobType.S3FileCopy, asyncJobToken, (String) null);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.client.SynapseClient#startBulkFileDownload(org.sagebionetworks.repo.model.file.BulkFileDownloadRequest)
	 */
	@Override
	public String startBulkFileDownload(BulkFileDownloadRequest request) throws SynapseException{
		return startAsynchJob(AsynchJobType.BulkFileDownload, request);
	}
	
	@Override
	public BulkFileDownloadResponse getBulkFileDownloadResults(String asyncJobToken) throws SynapseException, SynapseResultNotReadyException {
		return (BulkFileDownloadResponse) getAsyncResult(AsynchJobType.BulkFileDownload, asyncJobToken, (String) null);
	}

	/**
	 * Asymmetrical post where the request and response are not of the same
	 * type.
	 * 
	 * @param url
	 * @param reqeust
	 * @param calls
	 * @throws SynapseException
	 */
	private <T extends JSONEntity> T asymmetricalPost(String endpoint,
			String url, JSONEntity requestBody, Class<? extends T> returnClass,
			SharedClientConnection.ErrorHandler errorHandler)
			throws SynapseException {
		try {
			String jsonString = EntityFactory
					.createJSONStringForEntity(requestBody);
			JSONObject responseBody = getSharedClientConnection().postJson(
					endpoint, url, jsonString, getUserAgent(), null,
					errorHandler);
			return EntityFactory.createEntityFromJSONObject(responseBody,
					returnClass);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Asymmetrical put where the request and response are not of the same
	 * type.
	 * 
	 * @param url
	 * @param reqeust
	 * @param calls
	 * @throws SynapseException
	 */
	private <T extends JSONEntity> T asymmetricalPut(String endpoint,
			String url, JSONEntity requestBody, Class<? extends T> returnClass)
			throws SynapseException {
		try {
			String jsonString = null;
			if(requestBody != null){
				jsonString = EntityFactory
						.createJSONStringForEntity(requestBody);
			}
			JSONObject responseBody = getSharedClientConnection().putJson(
					endpoint, url, jsonString, getUserAgent());
			return EntityFactory.createEntityFromJSONObject(responseBody,
					returnClass);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Get the raw file handle. Note: Only the creator of a the file handle can
	 * get the raw file handle.
	 * 
	 * @param fileHandleId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public FileHandle getRawFileHandle(String fileHandleId)
			throws SynapseException {
		JSONObject object = getSharedClientConnection().getJson(
				getFileEndpoint(), FILE_HANDLE + "/" + fileHandleId,
				getUserAgent());
		try {
			return EntityFactory.createEntityFromJSONObject(object,
					FileHandle.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Delete a raw file handle. Note: Only the creator of a the file handle can
	 * delete the file handle.
	 * 
	 * @param fileHandleId
	 * @throws SynapseException
	 */
	@Override
	public void deleteFileHandle(String fileHandleId) throws SynapseException {
		getSharedClientConnection().deleteUri(getFileEndpoint(),
				FILE_HANDLE + "/" + fileHandleId, getUserAgent());
	}

	/**
	 * Delete the preview associated with the given file handle. Note: Only the
	 * creator of a the file handle can delete the preview.
	 * 
	 * @param fileHandleId
	 * @throws SynapseException
	 */
	@Override
	public void clearPreview(String fileHandleId) throws SynapseException {
		getSharedClientConnection()
				.deleteUri(getFileEndpoint(),
						FILE_HANDLE + "/" + fileHandleId + FILE_PREVIEW,
						getUserAgent());
	}

	/**
	 * Guess the content type of a file by reading the start of the file stream
	 * using URLConnection.guessContentTypeFromStream(is); If URLConnection
	 * fails to return a content type then "application/octet-stream" will be
	 * returned.
	 * 
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static String guessContentTypeFromStream(File file)
			throws FileNotFoundException, IOException {
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
	public WikiPage createWikiPage(String ownerId, ObjectType ownerType,
			WikiPage toCreate) throws JSONObjectAdapterException,
			SynapseException {
		if (ownerId == null)
			throw new IllegalArgumentException("ownerId cannot be null");
		if (ownerType == null)
			throw new IllegalArgumentException("ownerType cannot be null");
		if (toCreate == null)
			throw new IllegalArgumentException("WikiPage cannot be null");
		String uri = createWikiURL(ownerId, ownerType);
		return doCreateJSONEntity(uri, toCreate);
	}

	/**
	 * Helper to create a wiki URL that does not include the wiki id.
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @return
	 */
	private String createWikiURL(String ownerId, ObjectType ownerType) {
		if (ownerId == null)
			throw new IllegalArgumentException("ownerId cannot be null");
		if (ownerType == null)
			throw new IllegalArgumentException("ownerType cannot be null");
		return String.format(WIKI_URI_TEMPLATE, ownerType.name().toLowerCase(),
				ownerId);
	}

	/**
	 * Get a WikiPage using its key
	 * 
	 * @param key
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	@Override
	public WikiPage getWikiPage(WikiPageKey key)
			throws JSONObjectAdapterException, SynapseException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		String uri = createWikiURL(key);
		return getJSONEntity(uri, WikiPage.class);
	}
	

	@Override
	public WikiPage getWikiPageForVersion(WikiPageKey key,
			Long version) throws JSONObjectAdapterException, SynapseException {
		String uri = createWikiURL(key) + VERSION_PARAMETER + version;
		return getJSONEntity(uri, WikiPage.class);
	}

	@Override
	public WikiPageKey getRootWikiPageKey(String ownerId, ObjectType ownerType) throws JSONObjectAdapterException, SynapseException {
		if (ownerId == null)
			throw new IllegalArgumentException("ownerId cannot be null");
		if (ownerType == null)
			throw new IllegalArgumentException("ownerType cannot be null");
		String uri = createWikiURL(ownerId, ownerType)+"key";
		return getJSONEntity(uri, WikiPageKey.class);
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
	public WikiPage getRootWikiPage(String ownerId, ObjectType ownerType)
			throws JSONObjectAdapterException, SynapseException {
		if (ownerId == null)
			throw new IllegalArgumentException("ownerId cannot be null");
		if (ownerType == null)
			throw new IllegalArgumentException("ownerType cannot be null");
		String uri = createWikiURL(ownerId, ownerType);
		return getJSONEntity(uri, WikiPage.class);
	}

	/**
	 * Get all of the FileHandles associated with a WikiPage, including any
	 * PreviewHandles.
	 * 
	 * @param key
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	@Override
	public FileHandleResults getWikiAttachmenthHandles(WikiPageKey key)
			throws JSONObjectAdapterException, SynapseException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		String uri = createWikiURL(key) + ATTACHMENT_HANDLES;
		return getJSONEntity(uri, FileHandleResults.class);
	}

	private static String createWikiAttachmentURI(WikiPageKey key,
			String fileName, boolean redirect) throws SynapseClientException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		if (fileName == null)
			throw new IllegalArgumentException("fileName cannot be null");
		String encodedName;
		try {
			encodedName = URLEncoder.encode(fileName, "UTF-8");
		} catch (IOException e) {
			throw new SynapseClientException("Failed to encode " + fileName, e);
		}
		return createWikiURL(key) + ATTACHMENT_FILE + FILE_NAME_PARAMETER
				+ encodedName + AND_REDIRECT_PARAMETER + redirect;
	}

	/**
	 * Get the temporary URL for a WikiPage attachment. This is an alternative
	 * to downloading the attachment to a file.
	 * 
	 * @param key
	 *            - Identifies a wiki page.
	 * @param fileName
	 *            - The name of the attachment file.
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws SynapseException
	 */
	@Override
	public URL getWikiAttachmentTemporaryUrl(WikiPageKey key, String fileName)
			throws ClientProtocolException, IOException, SynapseException {
		return getUrl(createWikiAttachmentURI(key, fileName, false));
	}

	@Override
	public void downloadWikiAttachment(WikiPageKey key, String fileName,
			File target) throws SynapseException {
		String uri = createWikiAttachmentURI(key, fileName, true);
		getSharedClientConnection().downloadFromSynapse(
				getRepoEndpoint() + uri, null, target, getUserAgent());
	}

	private static String createWikiAttachmentPreviewURI(WikiPageKey key,
			String fileName, boolean redirect) throws SynapseClientException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		if (fileName == null)
			throw new IllegalArgumentException("fileName cannot be null");
		String encodedName;
		try {
			encodedName = URLEncoder.encode(fileName, "UTF-8");
		} catch (IOException e) {
			throw new SynapseClientException("Failed to encode " + fileName, e);
		}
		return createWikiURL(key) + ATTACHMENT_FILE_PREVIEW
				+ FILE_NAME_PARAMETER + encodedName + AND_REDIRECT_PARAMETER
				+ redirect;

	}

	/**
	 * Get the temporary URL for a WikiPage attachment preview. This is an
	 * alternative to downloading the attachment to a file.
	 * 
	 * @param key
	 *            - Identifies a wiki page.
	 * @param fileName
	 *            - The name of the attachment file.
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws SynapseException
	 */
	@Override
	public URL getWikiAttachmentPreviewTemporaryUrl(WikiPageKey key,
			String fileName) throws ClientProtocolException, IOException,
			SynapseException {
		return getUrl(createWikiAttachmentPreviewURI(key, fileName, false));
	}

	@Override
	public void downloadWikiAttachmentPreview(WikiPageKey key, String fileName,
			File target) throws SynapseException {
		String uri = createWikiAttachmentPreviewURI(key, fileName, true);
		getSharedClientConnection().downloadFromSynapse(
				getRepoEndpoint() + uri, null, target, getUserAgent());

	}

	/**
	 * Get the temporary URL for the data file of a FileEntity for the current
	 * version of the entity.. This is an alternative to downloading the file.
	 * 
	 * @param entityId
	 * @return
	 * @throws ClientProtocolException
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws SynapseException
	 */
	@Override
	public URL getFileEntityTemporaryUrlForCurrentVersion(String entityId)
			throws ClientProtocolException, MalformedURLException, IOException,
			SynapseException {
		String uri = ENTITY + "/" + entityId + FILE + QUERY_REDIRECT_PARAMETER
				+ "false";
		return getUrl(uri);
	}

	@Override
	public void downloadFromFileEntityCurrentVersion(String fileEntityId,
			File destinationFile) throws SynapseException {
		String uri = ENTITY + "/" + fileEntityId + FILE;
		getSharedClientConnection().downloadFromSynapse(
				getRepoEndpoint() + uri, null, destinationFile, getUserAgent());
	}

	/**
	 * Get the temporary URL for the data file preview of a FileEntity for the
	 * current version of the entity.. This is an alternative to downloading the
	 * file.
	 * 
	 * @param entityId
	 * @return
	 * @throws ClientProtocolException
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws SynapseException
	 */
	@Override
	public URL getFileEntityPreviewTemporaryUrlForCurrentVersion(String entityId)
			throws ClientProtocolException, MalformedURLException, IOException,
			SynapseException {
		String uri = ENTITY + "/" + entityId + FILE_PREVIEW
				+ QUERY_REDIRECT_PARAMETER + "false";
		return getUrl(uri);
	}

	@Override
	public void downloadFromFileEntityPreviewCurrentVersion(
			String fileEntityId, File destinationFile) throws SynapseException {
		String uri = ENTITY + "/" + fileEntityId + FILE_PREVIEW;
		getSharedClientConnection().downloadFromSynapse(
				getRepoEndpoint() + uri, null, destinationFile, getUserAgent());
	}

	/**
	 * Get the temporary URL for the data file of a FileEntity for a given
	 * version number. This is an alternative to downloading the file.
	 * 
	 * @param entityId
	 * @return
	 * @throws ClientProtocolException
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws SynapseException
	 */
	@Override
	public URL getFileEntityTemporaryUrlForVersion(String entityId,
			Long versionNumber) throws ClientProtocolException,
			MalformedURLException, IOException, SynapseException {
		String uri = ENTITY + "/" + entityId + VERSION_INFO + "/"
				+ versionNumber + FILE + QUERY_REDIRECT_PARAMETER + "false";
		return getUrl(uri);
	}

	@Override
	public void downloadFromFileEntityForVersion(String entityId,
			Long versionNumber, File destinationFile) throws SynapseException {
		String uri = ENTITY + "/" + entityId + VERSION_INFO + "/"
				+ versionNumber + FILE;
		getSharedClientConnection().downloadFromSynapse(
				getRepoEndpoint() + uri, null, destinationFile, getUserAgent());
	}

	/**
	 * Get the temporary URL for the data file of a FileEntity for a given
	 * version number. This is an alternative to downloading the file.
	 * 
	 * @param entityId
	 * @return
	 * @throws ClientProtocolException
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws SynapseException
	 */
	@Override
	public URL getFileEntityPreviewTemporaryUrlForVersion(String entityId,
			Long versionNumber) throws ClientProtocolException,
			MalformedURLException, IOException, SynapseException {
		String uri = ENTITY + "/" + entityId + VERSION_INFO + "/"
				+ versionNumber + FILE_PREVIEW + QUERY_REDIRECT_PARAMETER
				+ "false";
		return getUrl(uri);
	}

	@Override
	public void downloadFromFileEntityPreviewForVersion(String entityId,
			Long versionNumber, File destinationFile) throws SynapseException {
		String uri = ENTITY + "/" + entityId + VERSION_INFO + "/"
				+ versionNumber + FILE_PREVIEW;
		getSharedClientConnection().downloadFromSynapse(
				getRepoEndpoint() + uri, null, destinationFile, getUserAgent());
	}

	/**
	 * Fetch a temporary url.
	 * 
	 * @param uri
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws SynapseException
	 */
	private URL getUrl(String uri) throws ClientProtocolException, IOException,
			MalformedURLException, SynapseException {
		return new URL(getSharedClientConnection().getDirect(repoEndpoint, uri,
				getUserAgent()));
	}

	private URL getUrl(String endpoint, String uri)
			throws ClientProtocolException, IOException, MalformedURLException,
			SynapseException {
		return new URL(getSharedClientConnection().getDirect(endpoint, uri,
				getUserAgent()));
	}

	/**
	 * Update a WikiPage
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @param toUpdate
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	@Override
	public WikiPage updateWikiPage(String ownerId, ObjectType ownerType,
			WikiPage toUpdate) throws JSONObjectAdapterException,
			SynapseException {
		if (ownerId == null)
			throw new IllegalArgumentException("ownerId cannot be null");
		if (ownerType == null)
			throw new IllegalArgumentException("ownerType cannot be null");
		if (toUpdate == null)
			throw new IllegalArgumentException("WikiPage cannot be null");
		if (toUpdate.getId() == null)
			throw new IllegalArgumentException(
					"WikiPage.getId() cannot be null");
		String uri = String.format(WIKI_ID_URI_TEMPLATE, ownerType.name()
				.toLowerCase(), ownerId, toUpdate.getId());
		return updateJSONEntity(uri, toUpdate);
	}

	/**
	 * Delete a WikiPage
	 * 
	 * @param key
	 * @throws SynapseException
	 */
	@Override
	public void deleteWikiPage(WikiPageKey key) throws SynapseException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		String uri = createWikiURL(key);
		getSharedClientConnection()
				.deleteUri(repoEndpoint, uri, getUserAgent());
	}

	/**
	 * Helper to build a URL for a wiki page.
	 * 
	 * @param key
	 * @return
	 */
	private static String createWikiURL(WikiPageKey key) {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		return String.format(WIKI_ID_URI_TEMPLATE, key.getOwnerObjectType()
				.name().toLowerCase(), key.getOwnerObjectId(),
				key.getWikiPageId());
	}

	/**
	 * Get the WikiHeader tree for a given owner object.
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	@Override
	public PaginatedResults<WikiHeader> getWikiHeaderTree(String ownerId,
			ObjectType ownerType) throws SynapseException,
			JSONObjectAdapterException {
		if (ownerId == null)
			throw new IllegalArgumentException("ownerId cannot be null");
		if (ownerType == null)
			throw new IllegalArgumentException("ownerType cannot be null");
		String uri = String.format(WIKI_TREE_URI_TEMPLATE, ownerType.name()
				.toLowerCase(), ownerId);
		JSONObject object = getSharedClientConnection().getJson(repoEndpoint,
				uri, getUserAgent());
		PaginatedResults<WikiHeader> paginated = new PaginatedResults<WikiHeader>(
				WikiHeader.class);
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
	public FileHandleResults getEntityFileHandlesForCurrentVersion(
			String entityId) throws JSONObjectAdapterException,
			SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("Key cannot be null");
		String uri = ENTITY_URI_PATH + "/" + entityId + FILE_HANDLES;
		return getJSONEntity(uri, FileHandleResults.class);
	}

	/**
	 * Get the file hanldes for a given version of an entity.
	 * 
	 * @param entityId
	 * @param versionNumber
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	@Override
	public FileHandleResults getEntityFileHandlesForVersion(String entityId,
			Long versionNumber) throws JSONObjectAdapterException,
			SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("Key cannot be null");
		String uri = ENTITY_URI_PATH + "/" + entityId + "/version/"
				+ versionNumber + FILE_HANDLES;
		return getJSONEntity(uri, FileHandleResults.class);
	}

	// V2 WIKIPAGE METHODS

	/**
	 * Helper to create a V2 Wiki URL (No ID)
	 */
	private String createV2WikiURL(String ownerId, ObjectType ownerType) {
		if (ownerId == null)
			throw new IllegalArgumentException("ownerId cannot be null");
		if (ownerType == null)
			throw new IllegalArgumentException("ownerType cannot be null");
		return String.format(WIKI_URI_TEMPLATE_V2, ownerType.name()
				.toLowerCase(), ownerId);
	}

	/**
	 * Helper to build a URL for a V2 Wiki page, with ID
	 * 
	 * @param key
	 * @return
	 */
	private static String createV2WikiURL(WikiPageKey key) {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		return String.format(WIKI_ID_URI_TEMPLATE_V2, key.getOwnerObjectType()
				.name().toLowerCase(), key.getOwnerObjectId(),
				key.getWikiPageId());
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
		if (ownerId == null)
			throw new IllegalArgumentException("ownerId cannot be null");
		if (ownerType == null)
			throw new IllegalArgumentException("ownerType cannot be null");
		if (toCreate == null)
			throw new IllegalArgumentException("WikiPage cannot be null");
		String uri = createV2WikiURL(ownerId, ownerType);
		return doCreateJSONEntity(uri, toCreate);
	}

	/**
	 * Get a V2 WikiPage using its key
	 * 
	 * @param key
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	@Override
	public V2WikiPage getV2WikiPage(WikiPageKey key)
			throws JSONObjectAdapterException, SynapseException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		String uri = createV2WikiURL(key);
		return getJSONEntity(uri, V2WikiPage.class);
	}

	/**
	 * Get a version of a V2 WikiPage using its key and version number
	 */
	@Override
	public V2WikiPage getVersionOfV2WikiPage(WikiPageKey key, Long version)
			throws JSONObjectAdapterException, SynapseException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		if (version == null)
			throw new IllegalArgumentException("Version cannot be null");

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
		if (ownerId == null)
			throw new IllegalArgumentException("ownerId cannot be null");
		if (ownerType == null)
			throw new IllegalArgumentException("ownerType cannot be null");
		String uri = createV2WikiURL(ownerId, ownerType);
		return getJSONEntity(uri, V2WikiPage.class);
	}

	/**
	 * Update a V2 WikiPage
	 * 
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
		if (ownerId == null)
			throw new IllegalArgumentException("ownerId cannot be null");
		if (ownerType == null)
			throw new IllegalArgumentException("ownerType cannot be null");
		if (toUpdate == null)
			throw new IllegalArgumentException("WikiPage cannot be null");
		if (toUpdate.getId() == null)
			throw new IllegalArgumentException(
					"WikiPage.getId() cannot be null");
		String uri = String.format(WIKI_ID_URI_TEMPLATE_V2, ownerType.name()
				.toLowerCase(), ownerId, toUpdate.getId());
		return updateJSONEntity(uri, toUpdate);
	}

	@Override
	public V2WikiOrderHint updateV2WikiOrderHint(V2WikiOrderHint toUpdate)
			throws JSONObjectAdapterException, SynapseException {
		if (toUpdate == null)
			throw new IllegalArgumentException("toUpdate cannot be null");
		if (toUpdate.getOwnerId() == null)
			throw new IllegalArgumentException(
					"V2WikiOrderHint.getOwnerId() cannot be null");
		if (toUpdate.getOwnerObjectType() == null)
			throw new IllegalArgumentException(
					"V2WikiOrderHint.getOwnerObjectType() cannot be null");

		String uri = String.format(WIKI_ORDER_HINT_URI_TEMPLATE_V2, toUpdate
				.getOwnerObjectType().name().toLowerCase(),
				toUpdate.getOwnerId());
		return updateJSONEntity(uri, toUpdate);
	}

	/**
	 * Restore contents of a V2 WikiPage to the contents of a particular
	 * version.
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
			String wikiId, Long versionToRestore)
			throws JSONObjectAdapterException, SynapseException {
		if (ownerId == null)
			throw new IllegalArgumentException("ownerId cannot be null");
		if (ownerType == null)
			throw new IllegalArgumentException("ownerType cannot be null");
		if (wikiId == null)
			throw new IllegalArgumentException("Wiki id cannot be null");
		if (versionToRestore == null)
			throw new IllegalArgumentException("Version cannot be null");
		String uri = String.format(WIKI_ID_VERSION_URI_TEMPLATE_V2, ownerType
				.name().toLowerCase(), ownerId, wikiId, String
				.valueOf(versionToRestore));
		V2WikiPage mockWikiToUpdate = new V2WikiPage();
		return updateJSONEntity(uri, mockWikiToUpdate);
	}

	/**
	 * Get all of the FileHandles associated with a V2 WikiPage, including any
	 * PreviewHandles.
	 * 
	 * @param key
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	@Override
	public FileHandleResults getV2WikiAttachmentHandles(WikiPageKey key)
			throws JSONObjectAdapterException, SynapseException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		String uri = createV2WikiURL(key) + ATTACHMENT_HANDLES;
		return getJSONEntity(uri, FileHandleResults.class);
	}

	@Override
	public FileHandleResults getVersionOfV2WikiAttachmentHandles(
			WikiPageKey key, Long version) throws JSONObjectAdapterException,
			SynapseException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		if (version == null)
			throw new IllegalArgumentException("Version cannot be null");
		String uri = createV2WikiURL(key) + ATTACHMENT_HANDLES
				+ VERSION_PARAMETER + version;
		return getJSONEntity(uri, FileHandleResults.class);
	}

	@Override
	public String downloadV2WikiMarkdown(WikiPageKey key)
			throws ClientProtocolException, FileNotFoundException, IOException,
			SynapseException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		String uri = createV2WikiURL(key) + MARKDOWN_FILE;
		return getSharedClientConnection().downloadZippedFileString(
				repoEndpoint, uri, getUserAgent());
	}

	@Override
	public String downloadVersionOfV2WikiMarkdown(WikiPageKey key, Long version)
			throws ClientProtocolException, FileNotFoundException, IOException,
			SynapseException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		if (version == null)
			throw new IllegalArgumentException("Version cannot be null");
		String uri = createV2WikiURL(key) + MARKDOWN_FILE + VERSION_PARAMETER
				+ version;
		return getSharedClientConnection().downloadZippedFileString(
				repoEndpoint, uri, getUserAgent());
	}

	private static String createV2WikiAttachmentURI(WikiPageKey key,
			String fileName, boolean redirect) throws SynapseClientException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		if (fileName == null)
			throw new IllegalArgumentException("fileName cannot be null");
		String encodedName;
		try {
			encodedName = URLEncoder.encode(fileName, "UTF-8");
		} catch (IOException e) {
			throw new SynapseClientException("Failed to encode " + fileName, e);
		}
		return createV2WikiURL(key) + ATTACHMENT_FILE + FILE_NAME_PARAMETER
				+ encodedName + AND_REDIRECT_PARAMETER + redirect;

	}

	/**
	 * Get the temporary URL for a V2 WikiPage attachment. This is an
	 * alternative to downloading the attachment to a file.
	 * 
	 * @param key
	 *            - Identifies a V2 wiki page.
	 * @param fileName
	 *            - The name of the attachment file.
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws SynapseException
	 */
	@Override
	public URL getV2WikiAttachmentTemporaryUrl(WikiPageKey key, String fileName)
			throws ClientProtocolException, IOException, SynapseException {
		return getUrl(createV2WikiAttachmentURI(key, fileName, false));
	}

	@Override
	public void downloadV2WikiAttachment(WikiPageKey key, String fileName,
			File target) throws SynapseException {
		String uri = createV2WikiAttachmentURI(key, fileName, true);
		getSharedClientConnection().downloadFromSynapse(
				getRepoEndpoint() + uri, null, target, getUserAgent());
	}

	private static String createV2WikiAttachmentPreviewURI(WikiPageKey key,
			String fileName, boolean redirect) throws SynapseClientException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		if (fileName == null)
			throw new IllegalArgumentException("fileName cannot be null");
		String encodedName;
		try {
			encodedName = URLEncoder.encode(fileName, "UTF-8");
		} catch (IOException e) {
			throw new SynapseClientException("Failed to encode " + fileName, e);
		}
		return createV2WikiURL(key) + ATTACHMENT_FILE_PREVIEW
				+ FILE_NAME_PARAMETER + encodedName + AND_REDIRECT_PARAMETER
				+ redirect;
	}

	/**
	 * Get the temporary URL for a V2 WikiPage attachment preview. This is an
	 * alternative to downloading the attachment to a file.
	 * 
	 * @param key
	 *            - Identifies a V2 wiki page.
	 * @param fileName
	 *            - The name of the attachment file.
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws SynapseException
	 */
	@Override
	public URL getV2WikiAttachmentPreviewTemporaryUrl(WikiPageKey key,
			String fileName) throws ClientProtocolException, IOException,
			SynapseException {
		return getUrl(createV2WikiAttachmentPreviewURI(key, fileName, false));
	}

	@Override
	public void downloadV2WikiAttachmentPreview(WikiPageKey key,
			String fileName, File target) throws SynapseException {
		String uri = createV2WikiAttachmentPreviewURI(key, fileName, true);
		getSharedClientConnection().downloadFromSynapse(
				getRepoEndpoint() + uri, null, target, getUserAgent());
	}

	private static String createVersionOfV2WikiAttachmentPreviewURI(
			WikiPageKey key, String fileName, Long version, boolean redirect)
			throws SynapseClientException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		if (fileName == null)
			throw new IllegalArgumentException("fileName cannot be null");
		String encodedName;
		try {
			encodedName = URLEncoder.encode(fileName, "UTF-8");
		} catch (IOException e) {
			throw new SynapseClientException("Failed to encode " + fileName, e);
		}
		return createV2WikiURL(key) + ATTACHMENT_FILE_PREVIEW
				+ FILE_NAME_PARAMETER + encodedName + AND_REDIRECT_PARAMETER
				+ redirect + AND_VERSION_PARAMETER + version;
	}

	@Override
	public URL getVersionOfV2WikiAttachmentPreviewTemporaryUrl(WikiPageKey key,
			String fileName, Long version) throws ClientProtocolException,
			IOException, SynapseException {
		return getUrl(createVersionOfV2WikiAttachmentPreviewURI(key, fileName,
				version, false));
	}

	@Override
	public void downloadVersionOfV2WikiAttachmentPreview(WikiPageKey key,
			String fileName, Long version, File target) throws SynapseException {
		String uri = createVersionOfV2WikiAttachmentPreviewURI(key, fileName,
				version, true);
		getSharedClientConnection().downloadFromSynapse(
				getRepoEndpoint() + uri, null, target, getUserAgent());
	}

	private static String createVersionOfV2WikiAttachmentURI(WikiPageKey key,
			String fileName, Long version, boolean redirect)
			throws SynapseClientException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		if (fileName == null)
			throw new IllegalArgumentException("fileName cannot be null");
		String encodedName;
		try {
			encodedName = URLEncoder.encode(fileName, "UTF-8");
		} catch (IOException e) {
			throw new SynapseClientException("Failed to encode " + fileName, e);
		}
		return createV2WikiURL(key) + ATTACHMENT_FILE + FILE_NAME_PARAMETER
				+ encodedName + AND_REDIRECT_PARAMETER + redirect
				+ AND_VERSION_PARAMETER + version;
	}

	@Override
	public URL getVersionOfV2WikiAttachmentTemporaryUrl(WikiPageKey key,
			String fileName, Long version) throws ClientProtocolException,
			IOException, SynapseException {
		return getUrl(createVersionOfV2WikiAttachmentURI(key, fileName,
				version, false));
	}

	// alternative to getVersionOfV2WikiAttachmentTemporaryUrl
	@Override
	public void downloadVersionOfV2WikiAttachment(WikiPageKey key,
			String fileName, Long version, File target) throws SynapseException {
		String uri = createVersionOfV2WikiAttachmentURI(key, fileName, version,
				true);
		getSharedClientConnection().downloadFromSynapse(
				getRepoEndpoint() + uri, null, target, getUserAgent());
	}

	/**
	 * Delete a V2 WikiPage
	 * 
	 * @param key
	 * @throws SynapseException
	 */
	@Override
	public void deleteV2WikiPage(WikiPageKey key) throws SynapseException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		String uri = createV2WikiURL(key);
		getSharedClientConnection()
				.deleteUri(repoEndpoint, uri, getUserAgent());
	}

	/**
	 * Get the WikiHeader tree for a given owner object.
	 * 
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
		if (ownerId == null)
			throw new IllegalArgumentException("ownerId cannot be null");
		if (ownerType == null)
			throw new IllegalArgumentException("ownerType cannot be null");
		String uri = String.format(WIKI_TREE_URI_TEMPLATE_V2, ownerType.name()
				.toLowerCase(), ownerId);
		JSONObject object = getSharedClientConnection().getJson(repoEndpoint,
				uri, getUserAgent());
		PaginatedResults<V2WikiHeader> paginated = new PaginatedResults<V2WikiHeader>(
				V2WikiHeader.class);
		paginated.initializeFromJSONObject(new JSONObjectAdapterImpl(object));
		return paginated;
	}

	@Override
	public V2WikiOrderHint getV2OrderHint(WikiPageKey key)
			throws SynapseException, JSONObjectAdapterException {
		if (key == null)
			throw new IllegalArgumentException("key cannot be null");

		String uri = String.format(WIKI_ORDER_HINT_URI_TEMPLATE_V2, key
				.getOwnerObjectType().name().toLowerCase(),
				key.getOwnerObjectId());
		JSONObject object = getSharedClientConnection().getJson(repoEndpoint,
				uri, getUserAgent());
		V2WikiOrderHint orderHint = new V2WikiOrderHint();
		orderHint.initializeFromJSONObject(new JSONObjectAdapterImpl(object));
		return orderHint;
	}

	/**
	 * Get the tree of snapshots (outlining each modification) for a particular
	 * V2 WikiPage
	 * 
	 * @param key
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	@Override
	public PaginatedResults<V2WikiHistorySnapshot> getV2WikiHistory(
			WikiPageKey key, Long limit, Long offset)
			throws JSONObjectAdapterException, SynapseException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		String uri = createV2WikiURL(key) + WIKI_HISTORY_V2 + "?"
				+ OFFSET_PARAMETER + offset + AND_LIMIT_PARAMETER + limit;
		JSONObject object = getSharedClientConnection().getJson(repoEndpoint,
				uri, getUserAgent());
		PaginatedResults<V2WikiHistorySnapshot> paginated = new PaginatedResults<V2WikiHistorySnapshot>(
				V2WikiHistorySnapshot.class);
		paginated.initializeFromJSONObject(new JSONObjectAdapterImpl(object));
		return paginated;
	}

	public File downloadFromSynapse(String path, String md5,
			File destinationFile) throws SynapseException {
		return getSharedClientConnection().downloadFromSynapse(path, md5,
				destinationFile, null);
	}

	/******************** Low Level APIs ********************/

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
	<T extends JSONEntity> T doCreateJSONEntity(String uri, T entity)
			throws JSONObjectAdapterException, SynapseException {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}
		String postJSON = EntityFactory.createJSONStringForEntity(entity);
		JSONObject jsonObject = getSharedClientConnection().postJson(
				repoEndpoint, uri, postJSON, getUserAgent(), null);
		return (T) EntityFactory.createEntityFromJSONObject(jsonObject,
				entity.getClass());
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
	private <T extends JSONEntity> T doCreateJSONEntity(String endpoint,
			String uri, T entity) throws JSONObjectAdapterException,
			SynapseException {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}
		String postJSON = EntityFactory.createJSONStringForEntity(entity);
		JSONObject jsonObject = getSharedClientConnection().postJson(endpoint,
				uri, postJSON, getUserAgent(), null);
		return (T) EntityFactory.createEntityFromJSONObject(jsonObject,
				entity.getClass());
	}

	/**
	 * Update any JSONEntity
	 * 
	 * @param endpoint
	 * @param uri
	 * @param entity
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	@SuppressWarnings("unchecked")
	private <T extends JSONEntity> T updateJSONEntity(String uri, T entity)
			throws JSONObjectAdapterException, SynapseException {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}
		String putJSON = EntityFactory.createJSONStringForEntity(entity);
		JSONObject jsonObject = getSharedClientConnection().putJson(
				repoEndpoint, uri, putJSON, getUserAgent());
		return (T) EntityFactory.createEntityFromJSONObject(jsonObject,
				entity.getClass());
	}

	/**
	 * Get a JSONEntity
	 */
	protected <T extends JSONEntity> T getJSONEntity(String uri,
			Class<? extends T> clazz) throws JSONObjectAdapterException,
			SynapseException {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == clazz) {
			throw new IllegalArgumentException("must provide entity");
		}
		JSONObject jsonObject = getSharedClientConnection().getJson(
				repoEndpoint, uri, getUserAgent());
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
		return getSharedClientConnection().getJson(endpoint, uri,
				getUserAgent());
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.client.SynapseClient#startAsynchJob(org.sagebionetworks.client.AsynchJobType, org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody)
	 */
	public String startAsynchJob(AsynchJobType type, AsynchronousRequestBody request) throws SynapseException {
		String url = type.getStartUrl(request);
		String endpoint = getEndpointForType(type.getRestEndpoint());
		AsyncJobId jobId = asymmetricalPost(endpoint, url, request,
				AsyncJobId.class, null);
		return jobId.getToken();
	}

	@Override
	public void cancelAsynchJob(String jobId) throws SynapseException {
		String url = ASYNCHRONOUS_JOB + "/" + jobId + "/cancel";
		getSharedClientConnection().getJson(getRepoEndpoint(), url, getUserAgent());
	}
	
	@Override
	public AsynchronousResponseBody getAsyncResult(AsynchJobType type, String jobId, AsynchronousRequestBody request)
			throws SynapseException, SynapseClientException, SynapseResultNotReadyException {
		String url = type.getResultUrl(jobId, request);
		String endpoint = getEndpointForType(type.getRestEndpoint());
		return getAsynchJobResponse(url, type.getReponseClass(), endpoint);
	}

	@Override
	public AsynchronousResponseBody getAsyncResult(AsynchJobType type, String jobId, String entityId)
			throws SynapseException, SynapseClientException, SynapseResultNotReadyException {
		String url = type.getResultUrl(jobId, entityId);
		String endpoint = getEndpointForType(type.getRestEndpoint());
		return getAsynchJobResponse(url, type.getReponseClass(), endpoint);
	}

	/**
	 * Get a job response body for a url.
	 * @param url
	 * @param clazz
	 * @return
	 * @throws SynapseException
	 */
	private AsynchronousResponseBody getAsynchJobResponse(String url, Class<? extends AsynchronousResponseBody> clazz, String endpoint) throws SynapseException {
		JSONObject responseBody = getSharedClientConnection().getJson(
				endpoint, url, getUserAgent(),
				new SharedClientConnection.ErrorHandler() {
					@Override
					public void handleError(int code, String responseBody)
							throws SynapseException {
						if (code == HttpStatus.SC_ACCEPTED) {
							try {
								AsynchronousJobStatus status = EntityFactory
										.createEntityFromJSONString(
												responseBody,
												AsynchronousJobStatus.class);
								throw new SynapseResultNotReadyException(
										status);
							} catch (JSONObjectAdapterException e) {
								throw new SynapseClientException(e
										.getMessage(), e);
							}
						}
					}
				});
		try {
			return EntityFactory.createEntityFromJSONObject(responseBody, clazz);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
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
	 * @param defaultEndpoint
	 * @param uri
	 * @param entity
	 * @return the updated entity
	 * @throws SynapseException
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	public JSONObject updateSynapseEntity(String uri, JSONObject entity)
			throws SynapseException {

		JSONObject storedEntity = getSharedClientConnection().getJson(
				repoEndpoint, uri, getUserAgent());

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
			return getSharedClientConnection().putJson(repoEndpoint, uri,
					storedEntity.toString(), getUserAgent());
		} catch (JSONException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Perform a query
	 * 
	 * @param query
	 *            the query to perform
	 * @return the query result
	 */
	private JSONObject querySynapse(String query) throws SynapseException {
		try {
			if (null == query) {
				throw new IllegalArgumentException("must provide a query");
			}

			String queryUri;
			queryUri = QUERY_URI + URLEncoder.encode(query, "UTF-8");
			return getSharedClientConnection().getJson(repoEndpoint, queryUri,
					getUserAgent());
		} catch (UnsupportedEncodingException e) {
			throw new SynapseClientException(e);
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
	public SearchResults search(SearchQuery searchQuery)
			throws SynapseException, UnsupportedEncodingException,
			JSONObjectAdapterException {
		SearchResults searchResults = null;
		String uri = "/search";
		String jsonBody = EntityFactory.createJSONStringForEntity(searchQuery);
		JSONObject obj = getSharedClientConnection().postJson(repoEndpoint,
				uri, jsonBody, getUserAgent(), null);
		if (obj != null) {
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
		return getHttpClientHelper().getDataDirect(authEndpoint,
				"/" + domain.name().toLowerCase() + "TermsOfUse.html");
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
	public MessageToUser sendMessage(MessageToUser message)
			throws SynapseException {
		String uri = MESSAGE;
		try {
			String jsonBody = EntityFactory.createJSONStringForEntity(message);
			JSONObject obj = getSharedClientConnection().postJson(repoEndpoint,
					uri, jsonBody, getUserAgent(), null);
			return EntityFactory.createEntityFromJSONObject(obj,
					MessageToUser.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.client.SynapseClient#uploadToFileHandle(byte[], org.apache.http.entity.ContentType, java.lang.String)
	 */
	@Override
	public String uploadToFileHandle(byte[] content, ContentType contentType,
			String parentEntityId) throws SynapseException {
		List<UploadDestination> uploadDestinations = getUploadDestinations(parentEntityId);
		if (uploadDestinations.isEmpty()) {
			// default to S3
			return uploadToS3FileHandle(content, contentType, null);
		}

		UploadDestination uploadDestination = uploadDestinations.get(0);
		switch (uploadDestination.getUploadType()) {
		case HTTPS:
		case SFTP:
			throw new NotImplementedException(
					"SFTP and HTTPS uploads not implemented yet");
		case S3:
			return uploadToS3FileHandle(content, contentType,
					(S3UploadDestination) uploadDestination);
		default:
			throw new NotImplementedException(uploadDestination.getUploadType()
					.name() + " uploads not implemented yet");
		}
	}

	/**
	 * uploads a String to S3 using the chunked file upload service
	 * 
	 * @param content
	 *            the content to upload. Strings in memory should not be large,
	 *            so we limit to the size of one 'chunk'
	 * @param contentType
	 *            should include the character encoding, e.g.
	 *            "text/plain; charset=utf-8"
	 */
	@Override
	public String uploadToFileHandle(byte[] content, ContentType contentType)
			throws SynapseException {
		return uploadToS3FileHandle(content, contentType, null);
	}

	/**
	 * uploads a String to S3 using the chunked file upload service
	 * 
	 * @param content
	 *            the content to upload. Strings in memory should not be large,
	 *            so we limit to the size of one 'chunk'
	 * @param contentType
	 *            should include the character encoding, e.g.
	 *            "text/plain; charset=utf-8"
	 */
	private String uploadToS3FileHandle(byte[] content,
			ContentType contentType, UploadDestination uploadDestination)
			throws SynapseClientException, SynapseException {
		if (content == null || content.length == 0)
			throw new IllegalArgumentException("Missing content.");

		if (content.length >= MINIMUM_CHUNK_SIZE_BYTES)
			throw new IllegalArgumentException("String must be less than "
					+ MINIMUM_CHUNK_SIZE_BYTES + " bytes.");

		String contentMD5 = null;
		try {
			contentMD5 = MD5ChecksumHelper.getMD5ChecksumForByteArray(content);
		} catch (IOException e) {
			throw new SynapseClientException(e);
		}

		CreateChunkedFileTokenRequest ccftr = new CreateChunkedFileTokenRequest();
		ccftr.setFileName("content");
		ccftr.setContentType(contentType.toString());
		ccftr.setContentMD5(contentMD5);
		ccftr.setUploadDestination(uploadDestination);
		ccftr.setStorageLocationId(uploadDestination == null ? null : uploadDestination.getStorageLocationId());
		// Start the upload
		ChunkedFileToken token = createChunkedFileUploadToken(ccftr);

		// because of the restriction on string length there will be exactly one
		// chunk
		List<Long> chunkNumbers = new ArrayList<Long>();
		long currentChunkNumber = 1;
		chunkNumbers.add(currentChunkNumber);
		ChunkRequest request = new ChunkRequest();
		request.setChunkedFileToken(token);
		request.setChunkNumber((long) currentChunkNumber);
		URL presignedURL = createChunkedPresignedUrl(request);
		getHttpClientHelper().putBytesToURL(presignedURL, content,
				contentType.toString());

		CompleteAllChunksRequest cacr = new CompleteAllChunksRequest();
		cacr.setChunkedFileToken(token);
		cacr.setChunkNumbers(chunkNumbers);
		UploadDaemonStatus status = startUploadDeamon(cacr);
		State state = status.getState();
		if (state.equals(State.FAILED))
			throw new IllegalStateException("Message creation failed: "
					+ status.getErrorMessage());

		long backOffMillis = 100L; // initially just 1/10 sec, but will
									// exponentially increase
		while (state.equals(State.PROCESSING)
				&& backOffMillis <= MAX_BACKOFF_MILLIS) {
			try {
				Thread.sleep(backOffMillis);
			} catch (InterruptedException e) {
				// continue
			}
			status = getCompleteUploadDaemonStatus(status.getDaemonId());
			state = status.getState();
			if (state.equals(State.FAILED))
				throw new IllegalStateException("Message creation failed: "
						+ status.getErrorMessage());
			backOffMillis *= 2; // exponential backoff
		}

		if (!state.equals(State.COMPLETED))
			throw new IllegalStateException("Message creation failed: "
					+ status.getErrorMessage());

		return status.getFileHandleId();
	}

	private static final ContentType STRING_MESSAGE_CONTENT_TYPE = ContentType
			.create("text/plain", MESSAGE_CHARSET);

	/**
	 * Convenience function to upload a simple string message body, then send
	 * message using resultant fileHandleId
	 * 
	 * @param message
	 * @param messageBody
	 * @return the created message
	 * @throws SynapseException
	 */
	@Override
	public MessageToUser sendStringMessage(MessageToUser message,
			String messageBody) throws SynapseException {
		if (message.getFileHandleId() != null)
			throw new IllegalArgumentException(
					"Expected null fileHandleId but found "
							+ message.getFileHandleId());
		String fileHandleId = uploadToFileHandle(
				messageBody.getBytes(MESSAGE_CHARSET),
				STRING_MESSAGE_CONTENT_TYPE);
		message.setFileHandleId(fileHandleId);
		return sendMessage(message);
	}

	/**
	 * Convenience function to upload a simple string message body, then send
	 * message to entity owner using resultant fileHandleId
	 * 
	 * @param message
	 * @param entityId
	 * @param messageBody
	 * @return the created message
	 * @throws SynapseException
	 */
	@Override
	public MessageToUser sendStringMessage(MessageToUser message,
			String entityId, String messageBody) throws SynapseException {
		if (message.getFileHandleId() != null)
			throw new IllegalArgumentException(
					"Expected null fileHandleId but found "
							+ message.getFileHandleId());
		String fileHandleId = uploadToFileHandle(
				messageBody.getBytes(MESSAGE_CHARSET),
				STRING_MESSAGE_CONTENT_TYPE);
		message.setFileHandleId(fileHandleId);
		return sendMessage(message, entityId);
	}

	@Override
	public MessageToUser sendMessage(MessageToUser message, String entityId)
			throws SynapseException {
		String uri = ENTITY + "/" + entityId + "/" + MESSAGE;
		try {
			String jsonBody = EntityFactory.createJSONStringForEntity(message);
			JSONObject obj = getSharedClientConnection().postJson(repoEndpoint,
					uri, jsonBody, getUserAgent(), null);
			return EntityFactory.createEntityFromJSONObject(obj,
					MessageToUser.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<MessageBundle> getInbox(
			List<MessageStatusType> inboxFilter, MessageSortBy orderBy,
			Boolean descending, long limit, long offset)
			throws SynapseException {
		String uri = setMessageParameters(MESSAGE_INBOX, inboxFilter, orderBy,
				descending, limit, offset);
		try {
			JSONObject obj = getSharedClientConnection().getJson(repoEndpoint,
					uri, getUserAgent());
			PaginatedResults<MessageBundle> messages = new PaginatedResults<MessageBundle>(
					MessageBundle.class);
			messages.initializeFromJSONObject(new JSONObjectAdapterImpl(obj));
			return messages;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<MessageToUser> getOutbox(MessageSortBy orderBy,
			Boolean descending, long limit, long offset)
			throws SynapseException {
		String uri = setMessageParameters(MESSAGE_OUTBOX, null, orderBy,
				descending, limit, offset);
		try {
			JSONObject obj = getSharedClientConnection().getJson(repoEndpoint,
					uri, getUserAgent());
			PaginatedResults<MessageToUser> messages = new PaginatedResults<MessageToUser>(
					MessageToUser.class);
			messages.initializeFromJSONObject(new JSONObjectAdapterImpl(obj));
			return messages;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public MessageToUser getMessage(String messageId) throws SynapseException {
		String uri = MESSAGE + "/" + messageId;
		try {
			JSONObject obj = getSharedClientConnection().getJson(repoEndpoint,
					uri, getUserAgent());
			return EntityFactory.createEntityFromJSONObject(obj,
					MessageToUser.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public MessageToUser forwardMessage(String messageId,
			MessageRecipientSet recipients) throws SynapseException {
		String uri = MESSAGE + "/" + messageId + FORWARD;
		try {
			String jsonBody = EntityFactory
					.createJSONStringForEntity(recipients);
			JSONObject obj = getSharedClientConnection().postJson(repoEndpoint,
					uri, jsonBody, getUserAgent(), null);
			return EntityFactory.createEntityFromJSONObject(obj,
					MessageToUser.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<MessageToUser> getConversation(
			String associatedMessageId, MessageSortBy orderBy,
			Boolean descending, long limit, long offset)
			throws SynapseException {
		String uri = setMessageParameters(MESSAGE + "/" + associatedMessageId
				+ CONVERSATION, null, orderBy, descending, limit, offset);
		try {
			JSONObject obj = getSharedClientConnection().getJson(repoEndpoint,
					uri, getUserAgent());
			PaginatedResults<MessageToUser> messages = new PaginatedResults<MessageToUser>(
					MessageToUser.class);
			messages.initializeFromJSONObject(new JSONObjectAdapterImpl(obj));
			return messages;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void updateMessageStatus(MessageStatus status)
			throws SynapseException {
		String uri = MESSAGE_STATUS;
		try {
			String jsonBody = EntityFactory.createJSONStringForEntity(status);
			getSharedClientConnection().putJson(repoEndpoint, uri, jsonBody,
					getUserAgent());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void deleteMessage(String messageId) throws SynapseException {
		String uri = MESSAGE + "/" + messageId;
		getSharedClientConnection()
				.deleteUri(repoEndpoint, uri, getUserAgent());
	}

	private static String createDownloadMessageURI(String messageId,
			boolean redirect) {
		return MESSAGE + "/" + messageId + FILE + "?" + REDIRECT_PARAMETER
				+ redirect;
	}

	@Override
	public String getMessageTemporaryUrl(String messageId)
			throws SynapseException, MalformedURLException, IOException {
		String uri = createDownloadMessageURI(messageId, false);
		return getSharedClientConnection().getDirect(repoEndpoint, uri,
				getUserAgent());
	}

	@Override
	public String downloadMessage(String messageId) throws SynapseException,
			MalformedURLException, IOException {
		String uri = createDownloadMessageURI(messageId, true);
		return getSharedClientConnection().getDirect(repoEndpoint, uri,
				getUserAgent());
	}

	@Override
	public void downloadMessageToFile(String messageId, File target)
			throws SynapseException {
		String uri = createDownloadMessageURI(messageId, true);
		getSharedClientConnection().downloadFromSynapse(
				getRepoEndpoint() + uri, null, target, getUserAgent());
	}

	/**
	 * Get the child count for this entity
	 * 
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 * @throws JSONException
	 */
	@Override
	public Long getChildCount(String entityId) throws SynapseException {
		String queryString = SELECT_ID_FROM_ENTITY_WHERE_PARENT_ID + entityId
				+ LIMIT_1_OFFSET_1;
		JSONObject query = query(queryString);
		if (!query.has(TOTAL_NUM_RESULTS)) {
			throw new SynapseClientException("Query results did not have "
					+ TOTAL_NUM_RESULTS);
		}
		try {
			return query.getLong(TOTAL_NUM_RESULTS);
		} catch (JSONException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Get the appropriate piece of the URL based on the attachment type
	 * 
	 * @param type
	 * @return
	 */
	public static String getAttachmentTypeURL(
			ServiceConstants.AttachmentType type) {
		if (type == AttachmentType.ENTITY)
			return ENTITY;
		else if (type == AttachmentType.USER_PROFILE)
			return USER_PROFILE_PATH;
		else
			throw new IllegalArgumentException("Unrecognized attachment type: "
					+ type);
	}

	/**
	 * Get the ids of all users and groups.
	 * 
	 * @param client
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public Set<String> getAllUserAndGroupIds() throws SynapseException {
		HashSet<String> ids = new HashSet<String>();
		// Get all the users
		PaginatedResults<UserProfile> pr = this.getUsers(0, Integer.MAX_VALUE);
		for (UserProfile up : pr.getResults()) {
			ids.add(up.getOwnerId());
		}
		PaginatedResults<UserGroup> groupPr = this.getGroups(0,
				Integer.MAX_VALUE);
		for (UserGroup ug : groupPr.getResults()) {
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
		return EntityFactory.createEntityFromJSONObject(json,
				SynapseVersionInfo.class);
	}

	/**
	 * Get the activity generatedBy an Entity
	 * 
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public Activity getActivityForEntity(String entityId)
			throws SynapseException {
		return getActivityForEntityVersion(entityId, null);
	}

	/**
	 * Get the activity generatedBy an Entity
	 * 
	 * @param entityId
	 * @param versionNumber
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public Activity getActivityForEntityVersion(String entityId,
			Long versionNumber) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		String url = createEntityUri(ENTITY_URI_PATH, entityId);
		if (versionNumber != null) {
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
	 * 
	 * @param entityId
	 * @param activityId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public Activity setActivityForEntity(String entityId, String activityId)
			throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("Entity id cannot be null");
		if (activityId == null)
			throw new IllegalArgumentException("Activity id cannot be null");
		String url = createEntityUri(ENTITY_URI_PATH, entityId)
				+ GENERATED_BY_SUFFIX;
		if (activityId != null)
			url += "?" + PARAM_GENERATED_BY + "=" + activityId;
		try {
			JSONObject jsonObject = new JSONObject(); // no need for a body
			jsonObject = getSharedClientConnection().putJson(repoEndpoint, url,
					jsonObject.toString(), getUserAgent());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObject);
			return new Activity(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Delete the generatedBy relationship for an Entity (does not delete the
	 * activity)
	 * 
	 * @param entityId
	 * @throws SynapseException
	 */
	@Override
	public void deleteGeneratedByForEntity(String entityId)
			throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("Entity id cannot be null");
		String uri = createEntityUri(ENTITY_URI_PATH, entityId)
				+ GENERATED_BY_SUFFIX;
		getSharedClientConnection()
				.deleteUri(repoEndpoint, uri, getUserAgent());
	}

	/**
	 * Create an activity
	 * 
	 * @param activity
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public Activity createActivity(Activity activity) throws SynapseException {
		if (activity == null)
			throw new IllegalArgumentException("Activity can not be null");
		String url = ACTIVITY_URI_PATH;
		JSONObjectAdapter toCreateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		try {
			obj = new JSONObject(activity.writeToJSONObject(toCreateAdapter)
					.toJSONString());
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
	 * 
	 * @param activityId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public Activity getActivity(String activityId) throws SynapseException {
		if (activityId == null)
			throw new IllegalArgumentException("Activity id cannot be null");
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
	 * 
	 * @param activity
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public Activity putActivity(Activity activity) throws SynapseException {
		if (activity == null)
			throw new IllegalArgumentException("Activity can not be null");
		String url = createEntityUri(ACTIVITY_URI_PATH, activity.getId());
		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		try {
			obj = new JSONObject(activity.writeToJSONObject(toUpdateAdapter)
					.toJSONString());
			JSONObject jsonObj = getSharedClientConnection().putJson(
					repoEndpoint, url, obj.toString(), getUserAgent());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			return new Activity(adapter);
		} catch (JSONException e1) {
			throw new RuntimeException(e1);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}

	/**
	 * Delete an activity. This will remove all generatedBy connections to this
	 * activity as well.
	 * 
	 * @param activityId
	 * @throws SynapseException
	 */
	@Override
	public void deleteActivity(String activityId) throws SynapseException {
		if (activityId == null)
			throw new IllegalArgumentException("Activity id cannot be null");
		String uri = createEntityUri(ACTIVITY_URI_PATH, activityId);
		getSharedClientConnection()
				.deleteUri(repoEndpoint, uri, getUserAgent());
	}

	@Override
	public PaginatedResults<Reference> getEntitiesGeneratedBy(
			String activityId, Integer limit, Integer offset)
			throws SynapseException {
		if (activityId == null)
			throw new IllegalArgumentException("Activity id cannot be null");
		String url = createEntityUri(ACTIVITY_URI_PATH, activityId
				+ GENERATED_PATH + "?" + OFFSET + "=" + offset + "&limit="
				+ limit);
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Reference> results = new PaginatedResults<Reference>(
				Reference.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
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
			throw new SynapseClientException(e);
		}
	}

	@Override
	public Evaluation getEvaluation(String evalId) throws SynapseException {
		if (evalId == null)
			throw new IllegalArgumentException("Evaluation id cannot be null");
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
	public PaginatedResults<Evaluation> getEvaluationByContentSource(String id,
			int offset, int limit) throws SynapseException {
		String url = ENTITY_URI_PATH + "/" + id + EVALUATION_URI_PATH + "?"
				+ OFFSET + "=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Evaluation> results = new PaginatedResults<Evaluation>(
				Evaluation.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Deprecated
	@Override
	public PaginatedResults<Evaluation> getEvaluationsPaginated(int offset,
			int limit) throws SynapseException {
		String url = EVALUATION_URI_PATH + "?" + OFFSET + "=" + offset
				+ "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Evaluation> results = new PaginatedResults<Evaluation>(
				Evaluation.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<Evaluation> getAvailableEvaluationsPaginated(
			int offset, int limit) throws SynapseException {
		String url = AVAILABLE_EVALUATION_URI_PATH + "?" + OFFSET + "="
				+ offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Evaluation> results = new PaginatedResults<Evaluation>(
				Evaluation.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	private String idsToString(List<String> ids) {
		StringBuilder sb = new StringBuilder();
		boolean firsttime = true;
		for (String s : ids) {
			if (firsttime) {
				firsttime = false;
			} else {
				sb.append(",");
			}
			sb.append(s);
		}
		return sb.toString();
	}

	@Override
	public PaginatedResults<Evaluation> getAvailableEvaluationsPaginated(
			int offset, int limit, List<String> evaluationIds)
			throws SynapseException {
		String url = AVAILABLE_EVALUATION_URI_PATH + "?" + OFFSET + "="
				+ offset + "&limit=" + limit + "&"
				+ EVALUATION_IDS_FILTER_PARAM + "="
				+ idsToString(evaluationIds);
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Evaluation> results = new PaginatedResults<Evaluation>(
				Evaluation.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public Evaluation findEvaluation(String name) throws SynapseException,
			UnsupportedEncodingException {
		if (name == null)
			throw new IllegalArgumentException("Evaluation name cannot be null");
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
		if (eval == null)
			throw new IllegalArgumentException("Evaluation can not be null");
		String url = createEntityUri(EVALUATION_URI_PATH, eval.getId());
		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		try {
			obj = new JSONObject(eval.writeToJSONObject(toUpdateAdapter)
					.toJSONString());
			JSONObject jsonObj = getSharedClientConnection().putJson(
					repoEndpoint, url, obj.toString(), getUserAgent());
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
		if (evalId == null)
			throw new IllegalArgumentException("Evaluation id cannot be null");
		String uri = createEntityUri(EVALUATION_URI_PATH, evalId);
		getSharedClientConnection()
				.deleteUri(repoEndpoint, uri, getUserAgent());
	}

	@Override
	public Submission createIndividualSubmission(Submission sub, String etag,
			String challengeEndpoint, String notificationUnsubscribeEndpoint)
			throws SynapseException {
		if (etag==null)
			throw new IllegalArgumentException("etag is required.");
		if (sub.getTeamId()!=null) 
			throw new IllegalArgumentException("For an individual submission Team ID must be null.");
		if (sub.getContributors()!=null && !sub.getContributors().isEmpty())
			throw new IllegalArgumentException("For an individual submission, contributors may not be specified.");
			
		String uri = EVALUATION_URI_PATH + "/" + SUBMISSION + "?" + ETAG + "="
				+ etag;
		if (challengeEndpoint!=null && notificationUnsubscribeEndpoint!=null) {
			uri += "&" + CHALLENGE_ENDPOINT_PARAM + "=" + urlEncode(challengeEndpoint) + 
					"&"	+ NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM + "=" + 
					urlEncode(notificationUnsubscribeEndpoint);
		}

		try {
			JSONObject jsonObj = EntityFactory.createJSONObjectForEntity(sub);
			jsonObj = createJSONObject(uri, jsonObj);
			return initializeFromJSONObject(jsonObj, Submission.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	@Override
	public TeamSubmissionEligibility getTeamSubmissionEligibility(String evaluationId, String teamId) 
			throws SynapseException {
		if (evaluationId==null)
			throw new IllegalArgumentException("evaluationId is required.");
		if (teamId==null)
			throw new IllegalArgumentException("teamId is required.");
		String url = EVALUATION_URI_PATH+"/"+evaluationId+TEAM+"/"+teamId+
				SUBMISSION_ELIGIBILITY;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			return new TeamSubmissionEligibility(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public Submission createTeamSubmission(Submission sub, String etag, String submissionEligibilityHash,
			String challengeEndpoint, String notificationUnsubscribeEndpoint)
			throws SynapseException {
		if (etag==null)
			throw new IllegalArgumentException("etag is required.");
		if (submissionEligibilityHash==null)
			throw new IllegalArgumentException("For a Team submission 'submissionEligibilityHash' is required.");
		if (sub.getTeamId()==null) 
			throw new IllegalArgumentException("For a Team submission Team ID is required.");
			
		String uri = EVALUATION_URI_PATH + "/" + SUBMISSION + "?" + ETAG + "="
				+ etag + "&" + SUBMISSION_ELIGIBILITY_HASH+"="+submissionEligibilityHash;
		if (challengeEndpoint!=null && notificationUnsubscribeEndpoint!=null) {
			uri += "&" + CHALLENGE_ENDPOINT_PARAM + "=" + urlEncode(challengeEndpoint) + 
					"&"	+ NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM + "=" + 
					urlEncode(notificationUnsubscribeEndpoint);
		}
		try {
			JSONObject jsonObj = EntityFactory.createJSONObjectForEntity(sub);
			jsonObj = createJSONObject(uri, jsonObj);
			return initializeFromJSONObject(jsonObj, Submission.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	/**
	 * Add a contributor to an existing submission.  This is available to Synapse administrators only.
	 * @param submissionId
	 * @param contributor
	 * @return
	 */
	public SubmissionContributor addSubmissionContributor(String submissionId, SubmissionContributor contributor)
			throws SynapseException {
		validateStringAsLong(submissionId);
		String uri = EVALUATION_URI_PATH + "/" + SUBMISSION + "/" + submissionId + "/contributor";
		try {
			JSONObject jsonObj = EntityFactory.createJSONObjectForEntity(contributor);
			jsonObj = createJSONObject(uri, jsonObj);
			return initializeFromJSONObject(jsonObj, SubmissionContributor.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	
	@Override
	public Submission getSubmission(String subId) throws SynapseException {
		if (subId == null)
			throw new IllegalArgumentException("Evaluation id cannot be null");
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
	public SubmissionStatus getSubmissionStatus(String subId)
			throws SynapseException {
		if (subId == null)
			throw new IllegalArgumentException("Submission id cannot be null");
		String url = EVALUATION_URI_PATH + "/" + SUBMISSION + "/" + subId + "/"
				+ STATUS;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			return new SubmissionStatus(adapter);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}

	@Override
	public SubmissionStatus updateSubmissionStatus(SubmissionStatus status)
			throws SynapseException {
		if (status == null) {
			throw new IllegalArgumentException(
					"SubmissionStatus cannot be null.");
		}
		if (status.getAnnotations() != null) {
			AnnotationsUtils.validateAnnotations(status.getAnnotations());
		}
		String url = EVALUATION_URI_PATH + "/" + SUBMISSION + "/"
				+ status.getId() + STATUS;
		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		try {
			obj = new JSONObject(status.writeToJSONObject(toUpdateAdapter)
					.toJSONString());
			JSONObject jsonObj = getSharedClientConnection().putJson(
					repoEndpoint, url, obj.toString(), getUserAgent());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			return new SubmissionStatus(adapter);
		} catch (JSONException e1) {
			throw new RuntimeException(e1);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}

	public BatchUploadResponse updateSubmissionStatusBatch(String evaluationId,
			SubmissionStatusBatch batch) throws SynapseException {
		if (evaluationId == null) {
			throw new IllegalArgumentException("evaluationId is required.");
		}
		if (batch == null) {
			throw new IllegalArgumentException(
					"SubmissionStatusBatch cannot be null.");
		}
		if (batch.getIsFirstBatch() == null) {
			throw new IllegalArgumentException(
					"isFirstBatch must be set to true or false.");
		}
		if (batch.getIsLastBatch() == null) {
			throw new IllegalArgumentException(
					"isLastBatch must be set to true or false.");
		}
		if (!batch.getIsFirstBatch() && batch.getBatchToken() == null) {
			throw new IllegalArgumentException(
					"batchToken cannot be null for any but the first batch.");
		}
		List<SubmissionStatus> statuses = batch.getStatuses();
		if (statuses == null || statuses.size() == 0) {
			throw new IllegalArgumentException(
					"SubmissionStatusBatch must contain at least one SubmissionStatus.");
		}
		for (SubmissionStatus status : statuses) {
			if (status.getAnnotations() != null) {
				AnnotationsUtils.validateAnnotations(status.getAnnotations());
			}
		}
		String url = EVALUATION_URI_PATH + "/" + evaluationId + STATUS_BATCH;
		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		try {
			obj = new JSONObject(batch.writeToJSONObject(toUpdateAdapter)
					.toJSONString());
			JSONObject jsonObj = getSharedClientConnection().putJson(
					repoEndpoint, url, obj.toString(), getUserAgent());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			return new BatchUploadResponse(adapter);
		} catch (JSONException e1) {
			throw new RuntimeException(e1);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}

	@Override
	public void deleteSubmission(String subId) throws SynapseException {
		if (subId == null)
			throw new IllegalArgumentException("Submission id cannot be null");
		String uri = EVALUATION_URI_PATH + "/" + SUBMISSION + "/" + subId;
		getSharedClientConnection()
				.deleteUri(repoEndpoint, uri, getUserAgent());
	}

	@Override
	public PaginatedResults<Submission> getAllSubmissions(String evalId,
			long offset, long limit) throws SynapseException {
		if (evalId == null)
			throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/" + SUBMISSION_ALL
				+ "?offset" + "=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Submission> results = new PaginatedResults<Submission>(
				Submission.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<SubmissionStatus> getAllSubmissionStatuses(
			String evalId, long offset, long limit) throws SynapseException {
		if (evalId == null)
			throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/"
				+ SUBMISSION_STATUS_ALL + "?offset" + "=" + offset + "&limit="
				+ limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<SubmissionStatus> results = new PaginatedResults<SubmissionStatus>(
				SubmissionStatus.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundles(
			String evalId, long offset, long limit) throws SynapseException {
		if (evalId == null)
			throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/"
				+ SUBMISSION_BUNDLE_ALL + "?offset" + "=" + offset + "&limit="
				+ limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<SubmissionBundle> results = new PaginatedResults<SubmissionBundle>(
				SubmissionBundle.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<Submission> getAllSubmissionsByStatus(
			String evalId, SubmissionStatusEnum status, long offset, long limit)
			throws SynapseException {
		if (evalId == null)
			throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/" + SUBMISSION_ALL
				+ STATUS_SUFFIX + status.toString() + "&offset=" + offset
				+ "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Submission> results = new PaginatedResults<Submission>(
				Submission.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<SubmissionStatus> getAllSubmissionStatusesByStatus(
			String evalId, SubmissionStatusEnum status, long offset, long limit)
			throws SynapseException {
		if (evalId == null)
			throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/"
				+ SUBMISSION_STATUS_ALL + STATUS_SUFFIX + status.toString()
				+ "&offset=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<SubmissionStatus> results = new PaginatedResults<SubmissionStatus>(
				SubmissionStatus.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundlesByStatus(
			String evalId, SubmissionStatusEnum status, long offset, long limit)
			throws SynapseException {
		if (evalId == null)
			throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/"
				+ SUBMISSION_BUNDLE_ALL + STATUS_SUFFIX + status.toString()
				+ "&offset=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<SubmissionBundle> results = new PaginatedResults<SubmissionBundle>(
				SubmissionBundle.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<Submission> getMySubmissions(String evalId,
			long offset, long limit) throws SynapseException {
		if (evalId == null)
			throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/" + SUBMISSION
				+ "?offset" + "=" + offset + "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Submission> results = new PaginatedResults<Submission>(
				Submission.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<SubmissionBundle> getMySubmissionBundles(
			String evalId, long offset, long limit) throws SynapseException {
		if (evalId == null)
			throw new IllegalArgumentException("Evaluation id cannot be null");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/"
				+ SUBMISSION_BUNDLE + "?offset" + "=" + offset + "&limit="
				+ limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<SubmissionBundle> results = new PaginatedResults<SubmissionBundle>(
				SubmissionBundle.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
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
	 * @throws SynapseException
	 */
	@Override
	public URL getFileTemporaryUrlForSubmissionFileHandle(String submissionId,
			String fileHandleId) throws ClientProtocolException,
			MalformedURLException, IOException, SynapseException {
		String url = EVALUATION_URI_PATH + "/" + SUBMISSION + "/"
				+ submissionId + FILE + "/" + fileHandleId
				+ QUERY_REDIRECT_PARAMETER + "false";
		return getUrl(url);
	}

	@Override
	public void downloadFromSubmission(String submissionId,
			String fileHandleId, File destinationFile) throws SynapseException {
		String uri = EVALUATION_URI_PATH + "/" + SUBMISSION + "/"
				+ submissionId + FILE + "/" + fileHandleId;
		getSharedClientConnection().downloadFromSynapse(
				getRepoEndpoint() + uri, null, destinationFile, getUserAgent());
	}

	@Override
	public Long getSubmissionCount(String evalId) throws SynapseException {
		if (evalId == null)
			throw new IllegalArgumentException("Evaluation id cannot be null");
		PaginatedResults<Submission> res = getAllSubmissions(evalId, 0, 0);
		return res.getTotalNumberOfResults();
	}

	/**
	 * Execute a user query over the Submissions of a specified Evaluation.
	 * 
	 * @param query
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public QueryTableResults queryEvaluation(String query)
			throws SynapseException {
		try {
			if (null == query) {
				throw new IllegalArgumentException("must provide a query");
			}
			String queryUri;
			queryUri = EVALUATION_QUERY_URI_PATH
					+ URLEncoder.encode(query, "UTF-8");

			JSONObject jsonObj = getSharedClientConnection().getJson(
					repoEndpoint, queryUri, getUserAgent());
			JSONObjectAdapter joa = new JSONObjectAdapterImpl(jsonObj);
			return new QueryTableResults(joa);
		} catch (Exception e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public StorageUsageSummaryList getStorageUsageSummary(
			List<StorageUsageDimension> aggregation) throws SynapseException {
		String uri = STORAGE_SUMMARY_PATH;
		if (aggregation != null && aggregation.size() > 0) {
			uri += "?aggregation=" + StringUtils.join(aggregation, ",");
		}

		try {
			JSONObject jsonObj = getSharedClientConnection().getJson(
					repoEndpoint, uri, getUserAgent());
			return EntityFactory.createEntityFromJSONObject(jsonObj,
					StorageUsageSummaryList.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Moves an entity and its descendants to the trash can.
	 *
	 * @param entityId
	 *            The ID of the entity to be moved to the trash can
	 */
	@Override
	public void moveToTrash(String entityId) throws SynapseException {
		if (entityId == null || entityId.isEmpty()) {
			throw new IllegalArgumentException("Must provide an Entity ID.");
		}
		String url = TRASHCAN_TRASH + "/" + entityId;
		getSharedClientConnection().putJson(repoEndpoint, url, null,
				getUserAgent());
	}

	/**
	 * Moves an entity and its descendants out of the trash can. The entity will
	 * be restored to the specified parent. If the parent is not specified, it
	 * will be restored to the original parent.
	 */
	@Override
	public void restoreFromTrash(String entityId, String newParentId)
			throws SynapseException {
		if (entityId == null || entityId.isEmpty()) {
			throw new IllegalArgumentException("Must provide an Entity ID.");
		}
		String url = TRASHCAN_RESTORE + "/" + entityId;
		if (newParentId != null && !newParentId.isEmpty()) {
			url = url + "/" + newParentId;
		}
		getSharedClientConnection().putJson(repoEndpoint, url, null,
				getUserAgent());
	}

	/**
	 * Retrieves entities (in the trash can) deleted by the user.
	 */
	@Override
	public PaginatedResults<TrashedEntity> viewTrashForUser(long offset,
			long limit) throws SynapseException {
		String url = TRASHCAN_VIEW + "?" + OFFSET + "=" + offset + "&" + LIMIT
				+ "=" + limit;
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint,
				url, getUserAgent());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<TrashedEntity> results = new PaginatedResults<TrashedEntity>(
				TrashedEntity.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Purges the specified entity from the trash can. After purging, the entity
	 * will be permanently deleted.
	 */
	@Override
	public void purgeTrashForUser(String entityId) throws SynapseException {
		if (entityId == null || entityId.isEmpty()) {
			throw new IllegalArgumentException("Must provide an Entity ID.");
		}
		String url = TRASHCAN_PURGE + "/" + entityId;
		getSharedClientConnection().putJson(repoEndpoint, url, null,
				getUserAgent());
	}

	/**
	 * Purges the trash can for the user. All the entities in the trash will be
	 * permanently deleted.
	 */
	@Override
	public void purgeTrashForUser() throws SynapseException {
		getSharedClientConnection().putJson(repoEndpoint, TRASHCAN_PURGE, null,
				getUserAgent());
	}

	@Override
	public void logError(LogEntry logEntry) throws SynapseException {
		try {
			JSONObject jsonObject = EntityFactory
					.createJSONObjectForEntity(logEntry);
			jsonObject = createJSONObject(LOG, jsonObject);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	@Deprecated
	public List<UploadDestination> getUploadDestinations(String parentEntityId) throws SynapseException {
		// Get the json for this entity as a list wrapper
		String url = ENTITY + "/" + parentEntityId + "/uploadDestinations";
		JSONObject json = getSynapseEntity(getFileEndpoint(), url);
		try {
			return ListWrapper.unwrap(new JSONObjectAdapterImpl(json), UploadDestination.class);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends StorageLocationSetting> T createStorageLocationSetting(T storageLocation)
			throws SynapseException {
		try {
			JSONObject jsonObject = EntityFactory.createJSONObjectForEntity(storageLocation);
			jsonObject = getSharedClientConnection().postJson(repoEndpoint, STORAGE_LOCATION, jsonObject.toString(),
					getUserAgent(), null);
			return (T) createJsonObjectFromInterface(jsonObject, StorageLocationSetting.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends StorageLocationSetting> T getMyStorageLocationSetting(Long storageLocationId) throws SynapseException {
		try {
			String url = STORAGE_LOCATION + "/" + storageLocationId;
			JSONObject jsonObject = getSharedClientConnection().getJson(repoEndpoint, url, getUserAgent(), null);
			return (T) createJsonObjectFromInterface(jsonObject, StorageLocationSetting.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public List<StorageLocationSetting> getMyStorageLocationSettings() throws SynapseException {
		try {
			String url = STORAGE_LOCATION;
			JSONObject jsonObject = getSharedClientConnection().getJson(repoEndpoint, url, getUserAgent(), null);
			return ListWrapper.unwrap(new JSONObjectAdapterImpl(jsonObject), StorageLocationSetting.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public UploadDestinationLocation[] getUploadDestinationLocations(String parentEntityId) throws SynapseException {
		// Get the json for this entity as a list wrapper
		String url = ENTITY + "/" + parentEntityId + "/uploadDestinationLocations";
		JSONObject json = getSynapseEntity(getFileEndpoint(), url);
		try {
			List<UploadDestinationLocation> locations = ListWrapper.unwrap(new JSONObjectAdapterImpl(json), UploadDestinationLocation.class);
			return locations.toArray(new UploadDestinationLocation[locations.size()]);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public UploadDestination getUploadDestination(String parentEntityId, Long storageLocationId) throws SynapseException {
		try {
			String uri = ENTITY + "/" + parentEntityId + "/uploadDestination/" + storageLocationId;
			JSONObject jsonObject = getSharedClientConnection().getJson(fileEndpoint, uri, getUserAgent());
			return createJsonObjectFromInterface(jsonObject, UploadDestination.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public UploadDestination getDefaultUploadDestination(String parentEntityId) throws SynapseException {
		try {
			String uri = ENTITY + "/" + parentEntityId + "/uploadDestination";
			JSONObject jsonObject = getSharedClientConnection().getJson(fileEndpoint, uri, getUserAgent());
			return createJsonObjectFromInterface(jsonObject, UploadDestination.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public ProjectSetting getProjectSetting(String projectId, ProjectSettingsType projectSettingsType) throws SynapseException {
		try {
			String uri = PROJECT_SETTINGS + "/" + projectId + "/type/" + projectSettingsType;
			JSONObject jsonObject = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
			return createJsonObjectFromInterface(jsonObject, ProjectSetting.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public ProjectSetting createProjectSetting(ProjectSetting projectSetting)
			throws SynapseException {
		try {
			JSONObject jsonObject = EntityFactory
					.createJSONObjectForEntity(projectSetting);
			jsonObject = getSharedClientConnection().postJson(repoEndpoint,
					PROJECT_SETTINGS, jsonObject.toString(), getUserAgent(),
					null);
			return createJsonObjectFromInterface(jsonObject,
					ProjectSetting.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void updateProjectSetting(ProjectSetting projectSetting)
			throws SynapseException {
		try {
			JSONObject jsonObject = EntityFactory
					.createJSONObjectForEntity(projectSetting);
			getSharedClientConnection().putJson(repoEndpoint, PROJECT_SETTINGS,
					jsonObject.toString(), getUserAgent());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void deleteProjectSetting(String projectSettingsId)
			throws SynapseException {
		String uri = PROJECT_SETTINGS + "/" + projectSettingsId;
		getSharedClientConnection()
				.deleteUri(repoEndpoint, uri, getUserAgent());
	}

	@SuppressWarnings("unchecked")
	private <T extends JSONEntity> T createJsonObjectFromInterface(
			JSONObject jsonObject, Class<T> expectedType)
			throws JSONObjectAdapterException {
		if (jsonObject == null) {
			return null;
		}
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObject);
		String concreteType = adapter.getString("concreteType");
		try {
			JSONEntity obj = (JSONEntity) Class.forName(concreteType).newInstance();
			obj.initializeFromJSONObject(adapter);
			return (T) obj;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Add the entity to this user's Favorites list
	 * 
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public EntityHeader addFavorite(String entityId) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("Entity id cannot be null");
		String url = createEntityUri(FAVORITE_URI_PATH, entityId);
		JSONObject jsonObj = getSharedClientConnection().postUri(repoEndpoint,
				url, getUserAgent());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			return new EntityHeader(adapter);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}

	/**
	 * Remove the entity from this user's Favorites list
	 * 
	 * @param entityId
	 * @throws SynapseException
	 */
	@Override
	public void removeFavorite(String entityId) throws SynapseException {
		if (entityId == null)
			throw new IllegalArgumentException("Entity id cannot be null");
		String uri = createEntityUri(FAVORITE_URI_PATH, entityId);
		getSharedClientConnection()
				.deleteUri(repoEndpoint, uri, getUserAgent());
	}

	/**
	 * Retrieve this user's Favorites list
	 * 
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public PaginatedResults<EntityHeader> getFavorites(Integer limit,
			Integer offset) throws SynapseException {
		String url = FAVORITE_URI_PATH + "?" + OFFSET + "=" + offset
				+ "&limit=" + limit;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<EntityHeader> results = new PaginatedResults<EntityHeader>(
				EntityHeader.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}

	}

	/**
	 * Retrieve this user's Projects list
	 * 
	 * @param type the type of list to get
	 * @param sortColumn the optional sort column (default by last activity)
	 * @param sortDirection the optional sort direction (default descending)
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public PaginatedResults<ProjectHeader> getMyProjects(ProjectListType type, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			Integer limit, Integer offset) throws SynapseException {
		return getProjects(type, null, null, sortColumn, sortDirection, limit, offset);
	}

	/**
	 * Retrieve a user's Projects list
	 * 
	 * @param userId the user for which to get the project list
	 * @param sortColumn the optional sort column (default by last activity)
	 * @param sortDirection the optional sort direction (default descending)
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public PaginatedResults<ProjectHeader> getProjectsFromUser(Long userId, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			Integer limit, Integer offset) throws SynapseException {
		return getProjects(ProjectListType.OTHER_USER_PROJECTS, userId, null, sortColumn, sortDirection, limit, offset);
	}

	/**
	 * Retrieve a teams's Projects list
	 * 
	 * @param teamId the team for which to get the project list
	 * @param sortColumn the optional sort column (default by last activity)
	 * @param sortDirection the optional sort direction (default descending)
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public PaginatedResults<ProjectHeader> getProjectsForTeam(Long teamId, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			Integer limit, Integer offset) throws SynapseException {
		return getProjects(ProjectListType.TEAM_PROJECTS, null, teamId, sortColumn, sortDirection, limit, offset);
	}

	private PaginatedResults<ProjectHeader> getProjects(ProjectListType type, Long userId, Long teamId, ProjectListSortColumn sortColumn,
			SortDirection sortDirection, Integer limit, Integer offset) throws SynapseException, SynapseClientException {
		String url = PROJECTS_URI_PATH + '/' + type.name();
		if (userId != null) {
			url += USER + '/' + userId;
		}
		if (teamId != null) {
			url += TEAM + '/' + teamId;
		}

		if (sortColumn == null) {
			sortColumn = ProjectListSortColumn.LAST_ACTIVITY;
		}
		if (sortDirection == null) {
			sortDirection = SortDirection.DESC;
		}

		url += '?' + OFFSET_PARAMETER + offset + '&' + LIMIT_PARAMETER + limit + "&sort=" + sortColumn.name() + "&sortDirection="
				+ sortDirection.name();
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<ProjectHeader> results = new PaginatedResults<ProjectHeader>(ProjectHeader.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Creates a DOI for the specified entity. The DOI will always be associated
	 * with the current version of the entity.
	 */
	@Override
	public void createEntityDoi(String entityId) throws SynapseException {
		createEntityDoi(entityId, null);
	}

	/**
	 * Creates a DOI for the specified entity version. If version is null, the
	 * DOI will always be associated with the current version of the entity.
	 */
	@Override
	public void createEntityDoi(String entityId, Long entityVersion)
			throws SynapseException {

		if (entityId == null || entityId.isEmpty()) {
			throw new IllegalArgumentException("Must provide entity ID.");
		}

		String url = ENTITY + "/" + entityId;
		if (entityVersion != null) {
			url = url + REPO_SUFFIX_VERSION + "/" + entityVersion;
		}
		url = url + DOI;
		getSharedClientConnection().putJson(repoEndpoint, url, null,
				getUserAgent());
	}

	/**
	 * Gets the DOI for the specified entity version. The DOI is for the current
	 * version of the entity.
	 */
	@Override
	public Doi getEntityDoi(String entityId) throws SynapseException {
		return getEntityDoi(entityId, null);
	}

	/**
	 * Gets the DOI for the specified entity version. If version is null, the
	 * DOI is for the current version of the entity.
	 */
	@Override
	public Doi getEntityDoi(String entityId, Long entityVersion)
			throws SynapseException {

		if (entityId == null || entityId.isEmpty()) {
			throw new IllegalArgumentException("Must provide entity ID.");
		}

		try {
			String url = ENTITY + "/" + entityId;
			if (entityVersion != null) {
				url = url + REPO_SUFFIX_VERSION + "/" + entityVersion;
			}
			url = url + DOI;
			JSONObject jsonObj = getSharedClientConnection().getJson(
					repoEndpoint, url, getUserAgent());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			Doi doi = new Doi();
			doi.initializeFromJSONObject(adapter);
			return doi;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Gets the header information of entities whose file's MD5 matches the
	 * given MD5 checksum.
	 */
	@Override
	public List<EntityHeader> getEntityHeaderByMd5(String md5)
			throws SynapseException {

		if (md5 == null || md5.isEmpty()) {
			throw new IllegalArgumentException(
					"Must provide a nonempty MD5 string.");
		}

		try {
			String url = ENTITY + "/md5/" + md5;
			JSONObject jsonObj = getSharedClientConnection().getJson(
					repoEndpoint, url, getUserAgent());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			PaginatedResults<EntityHeader> results = new PaginatedResults<EntityHeader>(
					EntityHeader.class);
			results.initializeFromJSONObject(adapter);
			return results.getResults();
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public String retrieveApiKey() throws SynapseException {
		try {
			String url = "/secretKey";
			JSONObject jsonObj = getSharedClientConnection().getJson(
					authEndpoint, url, getUserAgent());
			SecretKey key = EntityFactory.createEntityFromJSONObject(jsonObj,
					SecretKey.class);
			return key.getSecretKey();
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void invalidateApiKey() throws SynapseException {
		getSharedClientConnection().invalidateApiKey(getUserAgent());
	}

	@Override
	public AccessControlList updateEvaluationAcl(AccessControlList acl)
			throws SynapseException {

		if (acl == null) {
			throw new IllegalArgumentException("ACL can not be null.");
		}

		String url = EVALUATION_ACL_URI_PATH;
		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		try {
			obj = new JSONObject(acl.writeToJSONObject(toUpdateAdapter)
					.toJSONString());
			JSONObject jsonObj = getSharedClientConnection().putJson(
					repoEndpoint, url, obj.toString(), getUserAgent());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			return new AccessControlList(adapter);
		} catch (JSONException e) {
			throw new SynapseClientException(e);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public AccessControlList getEvaluationAcl(String evalId)
			throws SynapseException {

		if (evalId == null) {
			throw new IllegalArgumentException("Evaluation ID cannot be null.");
		}

		String url = EVALUATION_URI_PATH + "/" + evalId + "/acl";
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			return new AccessControlList(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public UserEvaluationPermissions getUserEvaluationPermissions(String evalId)
			throws SynapseException {

		if (evalId == null) {
			throw new IllegalArgumentException("Evaluation ID cannot be null.");
		}

		String url = EVALUATION_URI_PATH + "/" + evalId + "/permissions";
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			return new UserEvaluationPermissions(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public String appendRowSetToTableStart(AppendableRowSet rowSet,
			String tableId) throws SynapseException {
		AppendableRowSetRequest request = new AppendableRowSetRequest();
		request.setEntityId(tableId);
		request.setToAppend(rowSet);
		return startAsynchJob(AsynchJobType.TableAppendRowSet, request);
	}

	@Override
	public RowReferenceSet appendRowSetToTableGet(String token, String tableId)
			throws SynapseException, SynapseResultNotReadyException {
		RowReferenceSetResults rrs = (RowReferenceSetResults) getAsyncResult(
				AsynchJobType.TableAppendRowSet, token, tableId);
		return rrs.getRowReferenceSet();
	}

	@Override
	public RowReferenceSet appendRowsToTable(AppendableRowSet rowSet,
			long timeout, String tableId) throws SynapseException,
			InterruptedException {
		long start = System.currentTimeMillis();
		// Start the job
		String jobId = appendRowSetToTableStart(rowSet, tableId);
		do {
			try {
				return appendRowSetToTableGet(jobId, tableId);
			} catch (SynapseResultNotReadyException e) {
				Thread.sleep(1000);
			}
		} while (System.currentTimeMillis() - start < timeout);
		// ran out of time.
		throw new SynapseClientException("Timed out waiting for jobId: "
				+ jobId);
	}

	@Override
	public RowReferenceSet deleteRowsFromTable(RowSelection toDelete)
			throws SynapseException {
		if (toDelete == null)
			throw new IllegalArgumentException("RowSelection cannot be null");
		if (toDelete.getTableId() == null)
			throw new IllegalArgumentException(
					"RowSelection.tableId cannot be null");
		String uri = ENTITY + "/" + toDelete.getTableId() + TABLE
				+ "/deleteRows";
		try {
			String jsonBody = EntityFactory.createJSONStringForEntity(toDelete);
			JSONObject obj = getSharedClientConnection().postJson(repoEndpoint,
					uri, jsonBody, getUserAgent(), null);
			return EntityFactory.createEntityFromJSONObject(obj,
					RowReferenceSet.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public RowSet getRowsFromTable(RowReferenceSet toGet)
			throws SynapseException, SynapseTableUnavailableException {
		if (toGet == null)
			throw new IllegalArgumentException("RowReferenceSet cannot be null");
		if (toGet.getTableId() == null)
			throw new IllegalArgumentException(
					"RowReferenceSet.tableId cannot be null");
		String uri = ENTITY + "/" + toGet.getTableId() + TABLE + "/getRows";
		try {
			String jsonBody = EntityFactory.createJSONStringForEntity(toGet);
			JSONObject obj = getSharedClientConnection().postJson(repoEndpoint,
					uri, jsonBody, getUserAgent(), null);
			return EntityFactory.createEntityFromJSONObject(obj, RowSet.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public TableFileHandleResults getFileHandlesFromTable(
			RowReferenceSet fileHandlesToFind) throws SynapseException {
		if (fileHandlesToFind == null)
			throw new IllegalArgumentException("RowReferenceSet cannot be null");
		String uri = ENTITY + "/" + fileHandlesToFind.getTableId() + TABLE
				+ FILE_HANDLES;
		return asymmetricalPost(getRepoEndpoint(), uri, fileHandlesToFind,
				TableFileHandleResults.class, null);
	}

	/**
	 * Get the temporary URL for the data file of a file handle column for a
	 * row. This is an alternative to downloading the file.
	 * 
	 * @param entityId
	 * @return
	 * @throws IOException
	 * @throws SynapseException
	 */
	@Override
	public URL getTableFileHandleTemporaryUrl(String tableId, RowReference row,
			String columnId) throws IOException, SynapseException {
		String uri = getUriForFileHandle(tableId, row, columnId) + FILE
				+ QUERY_REDIRECT_PARAMETER + "false";
		return getUrl(uri);
	}

	@Override
	public void downloadFromTableFileHandleTemporaryUrl(String tableId,
			RowReference row, String columnId, File destinationFile)
			throws SynapseException {
		String uri = getUriForFileHandle(tableId, row, columnId) + FILE;
		getSharedClientConnection().downloadFromSynapse(
				getRepoEndpoint() + uri, null, destinationFile, getUserAgent());
	}

	@Override
	public URL getTableFileHandlePreviewTemporaryUrl(String tableId,
			RowReference row, String columnId) throws IOException,
			SynapseException {
		String uri = getUriForFileHandle(tableId, row, columnId) + FILE_PREVIEW
				+ QUERY_REDIRECT_PARAMETER + "false";
		return getUrl(uri);
	}

	@Override
	public void downloadFromTableFileHandlePreviewTemporaryUrl(String tableId,
			RowReference row, String columnId, File destinationFile)
			throws SynapseException {
		String uri = getUriForFileHandle(tableId, row, columnId) + FILE_PREVIEW;
		getSharedClientConnection().downloadFromSynapse(
				getRepoEndpoint() + uri, null, destinationFile, getUserAgent());
	}

	private static String getUriForFileHandle(String tableId, RowReference row,
			String columnId) {
		return ENTITY + "/" + tableId + TABLE + COLUMN + "/" + columnId
				+ ROW_ID + "/" + row.getRowId() + ROW_VERSION + "/"
				+ row.getVersionNumber();
	}

	@Override
	public String queryTableEntityBundleAsyncStart(String sql, Long offset,
			Long limit, boolean isConsistent, int partsMask, String tableId)
			throws SynapseException {
		Query query = new Query();
		query.setSql(sql);
		query.setIsConsistent(isConsistent);
		query.setOffset(offset);
		query.setLimit(limit);
		QueryBundleRequest bundleRequest = new QueryBundleRequest();
		bundleRequest.setEntityId(tableId);
		bundleRequest.setQuery(query);
		bundleRequest.setPartMask((long) partsMask);
		return startAsynchJob(AsynchJobType.TableQuery, bundleRequest);
	}

	@Override
	public QueryResultBundle queryTableEntityBundleAsyncGet(
			String asyncJobToken, String tableId) throws SynapseException,
			SynapseResultNotReadyException {
		return (QueryResultBundle) getAsyncResult(AsynchJobType.TableQuery,
				asyncJobToken, tableId);
	}

	@Override
	public String queryTableEntityNextPageAsyncStart(String nextPageToken,
			String tableId) throws SynapseException {
		QueryNextPageToken queryNextPageToken = new QueryNextPageToken();
		queryNextPageToken.setEntityId(tableId);
		queryNextPageToken.setToken(nextPageToken);
		return startAsynchJob(AsynchJobType.TableQueryNextPage, queryNextPageToken);
	}

	@Override
	public QueryResult queryTableEntityNextPageAsyncGet(String asyncJobToken,
			String tableId) throws SynapseException,
			SynapseResultNotReadyException {
		return (QueryResult) getAsyncResult(AsynchJobType.TableQueryNextPage,
				asyncJobToken, tableId);
	}

	@Override
	public String downloadCsvFromTableAsyncStart(String sql,
			boolean writeHeader, boolean includeRowIdAndRowVersion,
			CsvTableDescriptor csvDescriptor, String tableId)
			throws SynapseException {
		DownloadFromTableRequest downloadRequest = new DownloadFromTableRequest();
		downloadRequest.setEntityId(tableId);
		downloadRequest.setSql(sql);
		downloadRequest.setWriteHeader(writeHeader);
		downloadRequest.setIncludeRowIdAndRowVersion(includeRowIdAndRowVersion);
		downloadRequest.setCsvTableDescriptor(csvDescriptor);
		return startAsynchJob(AsynchJobType.TableCSVDownload, downloadRequest);
	}

	@Override
	public DownloadFromTableResult downloadCsvFromTableAsyncGet(
			String asyncJobToken, String tableId) throws SynapseException,
			SynapseResultNotReadyException {
		return (DownloadFromTableResult) getAsyncResult(
				AsynchJobType.TableCSVDownload, asyncJobToken, tableId);
	}

	@Override
	public String uploadCsvToTableAsyncStart(String tableId, String fileHandleId, String etag, Long linesToSkip,
			CsvTableDescriptor csvDescriptor) throws SynapseException {
		return uploadCsvToTableAsyncStart(tableId, fileHandleId, etag, linesToSkip, csvDescriptor, null);
	}

	@Override
	public String uploadCsvToTableAsyncStart(String tableId,
			String fileHandleId, String etag, Long linesToSkip,
			CsvTableDescriptor csvDescriptor, List<String> columnIds) throws SynapseException {
		UploadToTableRequest uploadRequest = new UploadToTableRequest();
		uploadRequest.setTableId(tableId);
		uploadRequest.setUploadFileHandleId(fileHandleId);
		uploadRequest.setUpdateEtag(etag);
		uploadRequest.setLinesToSkip(linesToSkip);
		uploadRequest.setCsvTableDescriptor(csvDescriptor);
		uploadRequest.setColumnIds(columnIds);
		return startAsynchJob(AsynchJobType.TableCSVUpload, uploadRequest);
	}

	@Override
	public UploadToTableResult uploadCsvToTableAsyncGet(String asyncJobToken,
			String tableId) throws SynapseException,
			SynapseResultNotReadyException {
		return (UploadToTableResult) getAsyncResult(
				AsynchJobType.TableCSVUpload, asyncJobToken, tableId);
	}

	@Override
	public String uploadCsvTablePreviewAsyncStart(UploadToTablePreviewRequest request) throws SynapseException {
		return startAsynchJob(AsynchJobType.TableCSVUploadPreview, request);
	}

	@Override
	public UploadToTablePreviewResult uploadCsvToTablePreviewAsyncGet(String asyncJobToken) 
			throws SynapseException, SynapseResultNotReadyException {
		String entityId = null;
		return (UploadToTablePreviewResult) getAsyncResult(
				AsynchJobType.TableCSVUploadPreview, asyncJobToken, entityId);
	}

	@Override
	public ColumnModel createColumnModel(ColumnModel model)
			throws SynapseException {
		if (model == null)
			throw new IllegalArgumentException("ColumnModel cannot be null");
		String url = COLUMN;
		return createJSONEntity(url, model);
	}

	@Override
	public List<ColumnModel> createColumnModels(List<ColumnModel> models)
			throws SynapseException {
		if (models == null) {
			throw new IllegalArgumentException("ColumnModel cannot be null");
		}
		String url = COLUMN_BATCH;
		List<ColumnModel> results = createJSONEntityFromListWrapper(url,
				ListWrapper.wrap(models, ColumnModel.class), ColumnModel.class);
		return results;
	}

	@Override
	public ColumnModel getColumnModel(String columnId) throws SynapseException {
		if (columnId == null)
			throw new IllegalArgumentException("ColumnId cannot be null");
		String url = COLUMN + "/" + columnId;
		try {
			return getJSONEntity(url, ColumnModel.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public List<ColumnModel> getColumnModelsForTableEntity(String tableEntityId)
			throws SynapseException {
		if (tableEntityId == null)
			throw new IllegalArgumentException("tableEntityId cannot be null");
		String url = ENTITY + "/" + tableEntityId + COLUMN;
		try {
			PaginatedColumnModels pcm = getJSONEntity(url,
					PaginatedColumnModels.class);
			return pcm.getResults();
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedColumnModels listColumnModels(String prefix, Long limit,
			Long offset) throws SynapseException {
		String url = buildListColumnModelUrl(prefix, limit, offset);
		try {
			return getJSONEntity(url, PaginatedColumnModels.class);

		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Build up the URL for listing all ColumnModels
	 * 
	 * @param prefix
	 * @param limit
	 * @param offset
	 * @return
	 */
	static String buildListColumnModelUrl(String prefix, Long limit, Long offset) {
		StringBuilder builder = new StringBuilder();
		builder.append(COLUMN);
		int count = 0;
		if (prefix != null || limit != null || offset != null) {
			builder.append("?");
		}
		if (prefix != null) {
			builder.append("prefix=");
			builder.append(prefix);
			count++;
		}
		if (limit != null) {
			if (count > 0) {
				builder.append("&");
			}
			builder.append("limit=");
			builder.append(limit);
			count++;
		}
		if (offset != null) {
			if (count > 0) {
				builder.append("&");
			}
			builder.append("offset=");
			builder.append(offset);
		}
		return builder.toString();
	}

	/**
	 * Start a new Asynchronous Job
	 * 
	 * @param jobBody
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public AsynchronousJobStatus startAsynchronousJob(
			AsynchronousRequestBody jobBody) throws SynapseException {
		if (jobBody == null)
			throw new IllegalArgumentException("JobBody cannot be null");
		String url = ASYNCHRONOUS_JOB;
		return asymmetricalPost(getRepoEndpoint(), url, jobBody,
				AsynchronousJobStatus.class, null);
	}

	/**
	 * Get the status of an Asynchronous Job from its ID.
	 * 
	 * @param jobId
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	@Override
	public AsynchronousJobStatus getAsynchronousJobStatus(String jobId)
			throws JSONObjectAdapterException, SynapseException {
		if (jobId == null)
			throw new IllegalArgumentException("JobId cannot be null");
		String url = ASYNCHRONOUS_JOB + "/" + jobId;
		return getJSONEntity(url, AsynchronousJobStatus.class);
	}

	@Override
	public Team createTeam(Team team) throws SynapseException {
		try {
			JSONObject jsonObj = EntityFactory.createJSONObjectForEntity(team);
			jsonObj = createJSONObject(TEAM, jsonObj);
			return initializeFromJSONObject(jsonObj, Team.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public Team getTeam(String id) throws SynapseException {
		JSONObject jsonObj = getEntity(TEAM + "/" + id);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		Team results = new Team();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<Team> getTeams(String fragment, long limit,
			long offset) throws SynapseException {
		String uri = null;
		if (fragment == null) {
			uri = TEAMS + "?" + OFFSET + "=" + offset + "&" + LIMIT + "="
					+ limit;
		} else {
			uri = TEAMS + "?" + NAME_FRAGMENT_FILTER + "="
					+ urlEncode(fragment) + "&" + OFFSET + "=" + offset + "&"
					+ LIMIT + "=" + limit;
		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Team> results = new PaginatedResults<Team>(Team.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	@Override
	public List<Team> listTeams(List<Long> ids) throws SynapseException {
		try {
			IdList idList = new IdList();
			idList.setList(ids);
			String jsonString = EntityFactory.createJSONStringForEntity(idList);
			JSONObject responseBody = getSharedClientConnection().postJson(
					getRepoEndpoint(), TEAM_LIST, jsonString, getUserAgent(), null, null);
			return ListWrapper.unwrap(new JSONObjectAdapterImpl(responseBody), Team.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<Team> getTeamsForUser(String memberId, long limit,
			long offset) throws SynapseException {
		String uri = USER + "/" + memberId + TEAM + "?" + OFFSET + "=" + offset
				+ "&" + LIMIT + "=" + limit;
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<Team> results = new PaginatedResults<Team>(Team.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	private static String createGetTeamIconURI(String teamId, boolean redirect) {
		return TEAM + "/" + teamId + ICON + "?" + REDIRECT_PARAMETER + redirect;
	}

	@Override
	public URL getTeamIcon(String teamId) throws SynapseException {
		try {
			return getUrl(createGetTeamIconURI(teamId, false));
		} catch (IOException e) {
			throw new SynapseClientException(e);
		}
	}

	// alternative to getTeamIcon
	@Override
	public void downloadTeamIcon(String teamId, File target)
			throws SynapseException {
		String uri = createGetTeamIconURI(teamId, true);
		getSharedClientConnection().downloadFromSynapse(
				getRepoEndpoint() + uri, null, target, getUserAgent());
	}

	@Override
	public Team updateTeam(Team team) throws SynapseException {
		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		try {
			obj = new JSONObject(team.writeToJSONObject(toUpdateAdapter)
					.toJSONString());
			JSONObject jsonObj = getSharedClientConnection().putJson(
					repoEndpoint, TEAM, obj.toString(), getUserAgent());
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
		getSharedClientConnection().deleteUri(repoEndpoint,
				TEAM + "/" + teamId, getUserAgent());
	}
	
	@Override
	public void addTeamMember(String teamId, String memberId, 
			String teamEndpoint, String notificationUnsubscribeEndpoint)
			throws SynapseException {
		String uri = TEAM + "/" + teamId + MEMBER + "/" + memberId;
		if (teamEndpoint!=null && notificationUnsubscribeEndpoint!=null) {
			uri += "?" + 	TEAM_ENDPOINT_PARAM + "=" + urlEncode(teamEndpoint) + 
					"&"	+ NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM + "=" + 
					urlEncode(notificationUnsubscribeEndpoint);
		}
		getSharedClientConnection().putJson(repoEndpoint, uri,
				new JSONObject().toString(), getUserAgent());
	}
	
	@Override
	public ResponseMessage addTeamMember(JoinTeamSignedToken joinTeamSignedToken, 
			String teamEndpoint,
			String notificationUnsubscribeEndpoint) 
			throws SynapseException {
		
		String uri = TEAM + "Member";
		
		if (teamEndpoint!=null && notificationUnsubscribeEndpoint!=null) {
			uri += "?" + TEAM_ENDPOINT_PARAM + "=" + urlEncode(teamEndpoint) + 
				"&"	+ NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM + "=" + urlEncode(notificationUnsubscribeEndpoint);
		}
		
		return asymmetricalPut(getRepoEndpoint(), uri, joinTeamSignedToken,
				ResponseMessage.class);
		
	}
	
	private static String urlEncode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public PaginatedResults<TeamMember> getTeamMembers(String teamId,
			String fragment, long limit, long offset) throws SynapseException {
		String uri = null;
		if (fragment == null) {
			uri = TEAM_MEMBERS + "/" + teamId + "?" + OFFSET + "=" + offset
					+ "&" + LIMIT + "=" + limit;
		} else {
			uri = TEAM_MEMBERS + "/" + teamId + "?" + NAME_FRAGMENT_FILTER
					+ "=" + urlEncode(fragment) + "&" + OFFSET + "=" + offset
					+ "&" + LIMIT + "=" + limit;
		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<TeamMember> results = new PaginatedResults<TeamMember>(
				TeamMember.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public List<TeamMember> listTeamMembers(String teamId, List<Long> ids) throws SynapseException {
		try {
			IdList idList = new IdList();
			idList.setList(ids);
			String jsonString = EntityFactory.createJSONStringForEntity(idList);
			JSONObject responseBody = getSharedClientConnection().postJson(
					getRepoEndpoint(), TEAM+"/"+teamId+MEMBER_LIST, jsonString, getUserAgent(), null, null);
			return ListWrapper.unwrap(new JSONObjectAdapterImpl(responseBody), TeamMember.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}

	}
	
	@Override
	public List<TeamMember> listTeamMembers(List<Long> teamIds, String userId) throws SynapseException {
		try {
			IdList idList = new IdList();
			idList.setList(teamIds);
			String jsonString = EntityFactory.createJSONStringForEntity(idList);
			JSONObject responseBody = getSharedClientConnection().postJson(
					getRepoEndpoint(), USER+"/"+userId+MEMBER_LIST, jsonString, getUserAgent(), null, null);
			return ListWrapper.unwrap(new JSONObjectAdapterImpl(responseBody), TeamMember.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	public TeamMember getTeamMember(String teamId, String memberId)
			throws SynapseException {
		JSONObject jsonObj = getEntity(TEAM + "/" + teamId + MEMBER + "/"
				+ memberId);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		TeamMember result = new TeamMember();
		try {
			result.initializeFromJSONObject(adapter);
			return result;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}

	}

	@Override
	public void removeTeamMember(String teamId, String memberId)
			throws SynapseException {
		getSharedClientConnection().deleteUri(repoEndpoint,
				TEAM + "/" + teamId + MEMBER + "/" + memberId, getUserAgent());
	}

	@Override
	public void setTeamMemberPermissions(String teamId, String memberId,
			boolean isAdmin) throws SynapseException {
		getSharedClientConnection().putJson(repoEndpoint,
				TEAM + "/" + teamId + MEMBER + "/" + memberId + PERMISSION + "?"
				+ TEAM_MEMBERSHIP_PERMISSION + "=" + isAdmin, "",
				getUserAgent());
	}

	@Override
	public TeamMembershipStatus getTeamMembershipStatus(String teamId,
			String principalId) throws SynapseException {
		JSONObject jsonObj = getEntity(TEAM + "/" + teamId + MEMBER + "/"
				+ principalId + MEMBERSHIP_STATUS);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		TeamMembershipStatus results = new TeamMembershipStatus();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	
	@Override
	public AccessControlList getTeamACL(String teamId) throws SynapseException {
		if (teamId == null) {
			throw new IllegalArgumentException("Team ID cannot be null.");
		}

		String url = TEAM + "/" + teamId + "/acl";
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		try {
			return new AccessControlList(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	
	@Override
	public AccessControlList updateTeamACL(AccessControlList acl) throws SynapseException {
		if (acl == null) {
			throw new IllegalArgumentException("ACL can not be null.");
		}

		String url = TEAM+"/acl";
		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		try {
			obj = new JSONObject(acl.writeToJSONObject(toUpdateAdapter)
					.toJSONString());
			JSONObject jsonObj = getSharedClientConnection().putJson(
					repoEndpoint, url, obj.toString(), getUserAgent());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			return new AccessControlList(adapter);
		} catch (JSONException e) {
			throw new SynapseClientException(e);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
		
	}


	@Override
	public MembershipInvtnSubmission createMembershipInvitation(
			MembershipInvtnSubmission invitation,
			String acceptInvitationEndpoint,
			String notificationUnsubscribeEndpoint) throws SynapseException {
		try {
			JSONObject jsonObj = EntityFactory
					.createJSONObjectForEntity(invitation);
			String uri = MEMBERSHIP_INVITATION;
			if (acceptInvitationEndpoint!=null && notificationUnsubscribeEndpoint!=null) {
				uri += "?" + ACCEPT_INVITATION_ENDPOINT_PARAM + "=" + urlEncode(acceptInvitationEndpoint) +
						"&" + NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM + "=" + urlEncode(notificationUnsubscribeEndpoint);
			}
			jsonObj = createJSONObject(uri, jsonObj);
			return initializeFromJSONObject(jsonObj,
					MembershipInvtnSubmission.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public MembershipInvtnSubmission getMembershipInvitation(String invitationId)
			throws SynapseException {
		JSONObject jsonObj = getEntity(MEMBERSHIP_INVITATION + "/"
				+ invitationId);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		MembershipInvtnSubmission results = new MembershipInvtnSubmission();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<MembershipInvitation> getOpenMembershipInvitations(
			String memberId, String teamId, long limit, long offset)
			throws SynapseException {

		String uri = null;
		if (teamId == null) {
			uri = USER + "/" + memberId + OPEN_MEMBERSHIP_INVITATION + "?"
					+ OFFSET + "=" + offset + "&" + LIMIT + "=" + limit;
		} else {
			uri = USER + "/" + memberId + OPEN_MEMBERSHIP_INVITATION + "?"
					+ TEAM_ID_REQUEST_PARAMETER + "=" + teamId + "&" + OFFSET
					+ "=" + offset + "&" + LIMIT + "=" + limit;

		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<MembershipInvitation> results = new PaginatedResults<MembershipInvitation>(
				MembershipInvitation.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<MembershipInvtnSubmission> getOpenMembershipInvitationSubmissions(
			String teamId, String inviteeId, long limit, long offset)
			throws SynapseException {

		String uri = null;
		if (inviteeId == null) {
			uri = TEAM + "/" + teamId + OPEN_MEMBERSHIP_INVITATION + "?"
					+ OFFSET + "=" + offset + "&" + LIMIT + "=" + limit;
		} else {
			uri = TEAM + "/" + teamId + OPEN_MEMBERSHIP_INVITATION + "?"
					+ INVITEE_ID_REQUEST_PARAMETER + "=" + inviteeId + "&"
					+ OFFSET + "=" + offset + "&" + LIMIT + "=" + limit;

		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<MembershipInvtnSubmission> results = new PaginatedResults<MembershipInvtnSubmission>(
				MembershipInvtnSubmission.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void deleteMembershipInvitation(String invitationId)
			throws SynapseException {
		getSharedClientConnection().deleteUri(repoEndpoint,
				MEMBERSHIP_INVITATION + "/" + invitationId, getUserAgent());
	}

	@Override
	public MembershipRqstSubmission createMembershipRequest(
			MembershipRqstSubmission request,
			String acceptRequestEndpoint,
			String notificationUnsubscribeEndpoint) throws SynapseException {
		try {
			String uri = MEMBERSHIP_REQUEST;
			if (acceptRequestEndpoint!=null && notificationUnsubscribeEndpoint!=null) {
				uri += 	"?" + ACCEPT_REQUEST_ENDPOINT_PARAM + "=" + urlEncode(acceptRequestEndpoint) +
						"&" + NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM + "=" + urlEncode(notificationUnsubscribeEndpoint);
			}
			JSONObject jsonObj = EntityFactory.createJSONObjectForEntity(request);
			jsonObj = createJSONObject(uri, jsonObj);
			return initializeFromJSONObject(jsonObj,
					MembershipRqstSubmission.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public MembershipRqstSubmission getMembershipRequest(String requestId)
			throws SynapseException {
		JSONObject jsonObj = getEntity(MEMBERSHIP_REQUEST + "/" + requestId);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		MembershipRqstSubmission results = new MembershipRqstSubmission();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<MembershipRequest> getOpenMembershipRequests(
			String teamId, String requestorId, long limit, long offset)
			throws SynapseException {
		String uri = null;
		if (requestorId == null) {
			uri = TEAM + "/" + teamId + OPEN_MEMBERSHIP_REQUEST + "?" + OFFSET
					+ "=" + offset + "&" + LIMIT + "=" + limit;
		} else {
			uri = TEAM + "/" + teamId + OPEN_MEMBERSHIP_REQUEST + "?"
					+ REQUESTOR_ID_REQUEST_PARAMETER + "=" + requestorId + "&"
					+ OFFSET + "=" + offset + "&" + LIMIT + "=" + limit;

		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<MembershipRequest> results = new PaginatedResults<MembershipRequest>(
				MembershipRequest.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<MembershipRqstSubmission> getOpenMembershipRequestSubmissions(
			String requesterId, String teamId, long limit, long offset)
			throws SynapseException {
		String uri = null;
		if (teamId == null) {
			uri = USER + "/" + requesterId + OPEN_MEMBERSHIP_REQUEST + "?"
					+ OFFSET + "=" + offset + "&" + LIMIT + "=" + limit;
		} else {
			uri = USER + "/" + requesterId + OPEN_MEMBERSHIP_REQUEST + "?"
					+ TEAM_ID_REQUEST_PARAMETER + "=" + teamId + "&" + OFFSET
					+ "=" + offset + "&" + LIMIT + "=" + limit;

		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<MembershipRqstSubmission> results = new PaginatedResults<MembershipRqstSubmission>(
				MembershipRqstSubmission.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void deleteMembershipRequest(String requestId)
			throws SynapseException {
		getSharedClientConnection().deleteUri(repoEndpoint,
				MEMBERSHIP_REQUEST + "/" + requestId, getUserAgent());
	}

	@Override
	public void createUser(NewUser user) throws SynapseException {
		try {
			JSONObject obj = EntityFactory.createJSONObjectForEntity(user);
			getSharedClientConnection().postJson(authEndpoint, "/user",
					obj.toString(), getUserAgent(), null);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void sendPasswordResetEmail(String email) throws SynapseException {
		try {
			Username user = new Username();
			user.setEmail(email);
			JSONObject obj = EntityFactory.createJSONObjectForEntity(user);
			getSharedClientConnection().postJson(authEndpoint,
					"/user/password/email", obj.toString(), getUserAgent(),
					null);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void changePassword(String sessionToken, String newPassword)
			throws SynapseException {
		try {
			ChangePasswordRequest change = new ChangePasswordRequest();
			change.setSessionToken(sessionToken);
			change.setPassword(newPassword);

			JSONObject obj = EntityFactory.createJSONObjectForEntity(change);
			getSharedClientConnection().postJson(authEndpoint,
					"/user/password", obj.toString(), getUserAgent(), null);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void signTermsOfUse(String sessionToken, boolean acceptTerms)
			throws SynapseException {
		signTermsOfUse(sessionToken, DomainType.SYNAPSE, acceptTerms);
	}

	@Override
	public void signTermsOfUse(String sessionToken, DomainType domain,
			boolean acceptTerms) throws SynapseException {
		try {
			Session session = new Session();
			session.setSessionToken(sessionToken);
			session.setAcceptsTermsOfUse(acceptTerms);

			Map<String, String> parameters = domainToParameterMap(domain);

			JSONObject obj = EntityFactory.createJSONObjectForEntity(session);
			getSharedClientConnection().postJson(authEndpoint, "/termsOfUse",
					obj.toString(), getUserAgent(), parameters);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Deprecated
	@Override
	public Session passThroughOpenIDParameters(String queryString)
			throws SynapseException {
		return passThroughOpenIDParameters(queryString, false);
	}

	@Deprecated
	@Override
	public Session passThroughOpenIDParameters(String queryString,
			Boolean createUserIfNecessary) throws SynapseException {
		return passThroughOpenIDParameters(queryString, createUserIfNecessary,
				DomainType.SYNAPSE);
	}

	@Deprecated
	@Override
	public Session passThroughOpenIDParameters(String queryString,
			Boolean createUserIfNecessary, DomainType domain)
			throws SynapseException {
		try {
			URIBuilder builder = new URIBuilder();
			builder.setPath("/openIdCallback");
			builder.setQuery(queryString);
			builder.setParameter("org.sagebionetworks.createUserIfNecessary",
					createUserIfNecessary.toString());

			Map<String, String> parameters = domainToParameterMap(domain);

			JSONObject session = getSharedClientConnection().postJson(
					authEndpoint, builder.toString(), "", getUserAgent(),
					parameters);
			return EntityFactory.createEntityFromJSONObject(session,
					Session.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.client.SynapseClient#getOAuth2AuthenticationUrl(org.sagebionetworks.repo.model.oauth.OAuthUrlRequest)
	 */
	@Override
	public OAuthUrlResponse getOAuth2AuthenticationUrl(OAuthUrlRequest request) throws SynapseException{
		return asymmetricalPost(getAuthEndpoint(), AUTH_OAUTH_2_AUTH_URL, request, OAuthUrlResponse.class, null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.client.SynapseClient#validateOAuthAuthenticationCode(org.sagebionetworks.repo.model.oauth.OAuthValidationRequest)
	 */
	@Override
	public Session validateOAuthAuthenticationCode(OAuthValidationRequest request) throws SynapseException{
		return asymmetricalPost(getAuthEndpoint(), AUTH_OAUTH_2_SESSION, request, Session.class, null);
	}
	
	@Override
	public PrincipalAlias bindOAuthProvidersUserId(OAuthValidationRequest request)
			throws SynapseException {
		return asymmetricalPost(authEndpoint, AUTH_OAUTH_2_ALIAS, request, PrincipalAlias.class, null);
		
	}
	
	@Override
	public void unbindOAuthProvidersUserId(OAuthProvider provider, String alias) throws SynapseException {
		if (provider==null) throw new IllegalArgumentException("provider is required.");
		if (alias==null) throw new IllegalArgumentException("alias is required.");
		try {
		getSharedClientConnection().deleteUri(authEndpoint,
				AUTH_OAUTH_2_ALIAS+"?provider="+
						URLEncoder.encode(provider.name(), "UTF-8")+
						"&"+"alias="+URLEncoder.encode(alias, "UTF-8"), 
				getUserAgent());
		} catch (UnsupportedEncodingException e) {
			throw new SynapseClientException(e);
		}
	}

	private Map<String, String> domainToParameterMap(DomainType domain) {
		Map<String, String> parameters = Maps.newHashMap();
		parameters.put(AuthorizationConstants.DOMAIN_PARAM, domain.name());
		return parameters;
	}

	@Override
	public Quiz getCertifiedUserTest() throws SynapseException {
		JSONObject jsonObj = getEntity(CERTIFIED_USER_TEST);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		Quiz results = new Quiz();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PassingRecord submitCertifiedUserTestResponse(QuizResponse response)
			throws SynapseException {
		try {
			JSONObject jsonObj = EntityFactory
					.createJSONObjectForEntity(response);
			jsonObj = createJSONObject(CERTIFIED_USER_TEST_RESPONSE, jsonObj);
			return initializeFromJSONObject(jsonObj, PassingRecord.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void setCertifiedUserStatus(String principalId, boolean status)
			throws SynapseException {
		String url = USER + "/" + principalId + CERTIFIED_USER_STATUS
				+ "?isCertified=" + status;
		getSharedClientConnection().putJson(repoEndpoint, url, null,
				getUserAgent());
	}

	@Override
	public PaginatedResults<QuizResponse> getCertifiedUserTestResponses(
			long offset, long limit, String principalId)
			throws SynapseException {

		String uri = null;
		if (principalId == null) {
			uri = CERTIFIED_USER_TEST_RESPONSE + "?" + OFFSET + "=" + offset
					+ "&" + LIMIT + "=" + limit;
		} else {
			uri = CERTIFIED_USER_TEST_RESPONSE + "?"
					+ PRINCIPAL_ID_REQUEST_PARAM + "=" + principalId + "&"
					+ OFFSET + "=" + offset + "&" + LIMIT + "=" + limit;
		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<QuizResponse> results = new PaginatedResults<QuizResponse>(
				QuizResponse.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void deleteCertifiedUserTestResponse(String id)
			throws SynapseException {
		getSharedClientConnection().deleteUri(repoEndpoint,
				CERTIFIED_USER_TEST_RESPONSE + "/" + id, getUserAgent());
	}

	@Override
	public PassingRecord getCertifiedUserPassingRecord(String principalId)
			throws SynapseException {
		if (principalId == null)
			throw new IllegalArgumentException("principalId may not be null.");
		JSONObject jsonObj = getEntity(USER + "/" + principalId
				+ CERTIFIED_USER_PASSING_RECORD);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PassingRecord results = new PassingRecord();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<PassingRecord> getCertifiedUserPassingRecords(
			long offset, long limit, String principalId)
			throws SynapseException {
		if (principalId == null)
			throw new IllegalArgumentException("principalId may not be null.");
		String uri = USER + "/" + principalId + CERTIFIED_USER_PASSING_RECORDS
				+ "?" + OFFSET + "=" + offset + "&" + LIMIT + "=" + limit;
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<PassingRecord> results = new PaginatedResults<PassingRecord>(
				PassingRecord.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public EntityQueryResults entityQuery(EntityQuery query)
			throws SynapseException {
		return asymmetricalPost(getRepoEndpoint(), QUERY, query,
				EntityQueryResults.class, null);
	}
	
	@Override
	public Challenge createChallenge(Challenge challenge) throws SynapseException {
		try {
			JSONObject jsonObj = EntityFactory.createJSONObjectForEntity(challenge);
			jsonObj = createJSONObject(CHALLENGE, jsonObj);
			return initializeFromJSONObject(jsonObj, Challenge.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	/**
	 * Returns the Challenge given its ID.  Caller must
	 * have READ permission on the associated Project.
	 * 
	 * @param challengeId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public Challenge getChallenge(String challengeId) throws SynapseException {
		validateStringAsLong(challengeId);
		JSONObject jsonObj = getEntity(CHALLENGE+"/"+challengeId);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		Challenge results = new Challenge();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public Challenge getChallengeForProject(String projectId) throws SynapseException {
		if (projectId==null) throw new IllegalArgumentException("projectId may not be null.");
		JSONObject jsonObj = getEntity(ENTITY+"/"+projectId+CHALLENGE);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		Challenge results = new Challenge();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	private static final void validateStringAsLong(String s) throws SynapseClientException {
		if (s==null) throw new NullPointerException();
		try {
			Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new SynapseClientException("Expected integer but found "+s, e);
		}
	}
	
	@Override
	public PaginatedIds listChallengeParticipants(String challengeId, Boolean affiliated, Long limit, Long offset)  throws SynapseException {
		validateStringAsLong(challengeId);
		String uri = CHALLENGE+"/"+challengeId+"/participant";
		boolean anyParameters = false;
		if (affiliated!=null) {
			uri += "?affiliated="+affiliated;
			anyParameters = true;
		}
		if  (limit!=null) {
			uri+=(anyParameters ?"&":"?")+LIMIT+"="+limit;
			anyParameters = true;
		}
		if  (offset!=null) {
			uri+=(anyParameters ?"&":"?")+OFFSET+"="+offset;
			anyParameters = true;
		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedIds results = new PaginatedIds();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	@Override
	public ChallengePagedResults listChallengesForParticipant(String participantPrincipalId, Long limit, Long offset) throws SynapseException {
		validateStringAsLong(participantPrincipalId);
		String uri = CHALLENGE+"?participantId="+participantPrincipalId;
		if  (limit!=null) uri+=	"&"+LIMIT+"="+limit;
		if  (offset!=null) uri+="&"+OFFSET+"="+offset;
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		ChallengePagedResults results = new ChallengePagedResults();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	@Override
	public Challenge updateChallenge(Challenge challenge) throws SynapseException {
		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		String uri = CHALLENGE+"/"+challenge.getId();
		try {
			obj = new JSONObject(challenge.writeToJSONObject(toUpdateAdapter).toJSONString());
			JSONObject jsonObj = getSharedClientConnection().putJson(repoEndpoint, uri, obj.toString(), getUserAgent());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			return new Challenge(adapter);
		} catch (JSONException e1) {
			throw new RuntimeException(e1);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}

	
	@Override
	public void deleteChallenge(String id) throws SynapseException {
		getSharedClientConnection().deleteUri(repoEndpoint, CHALLENGE + "/" + id, getUserAgent());
	}

	
	/**
	 * Register a Team for a Challenge. The user making this request must be
	 * registered for the Challenge and be an administrator of the Team.
	 * 
	 * @param challengeId
	 * @param teamId
	 * @throws SynapseException
	 */
	@Override
	public ChallengeTeam createChallengeTeam(ChallengeTeam challengeTeam) throws SynapseException {
		try {
			if (challengeTeam.getChallengeId()==null) throw new IllegalArgumentException("challenge ID is required.");
			JSONObject jsonObj = EntityFactory.createJSONObjectForEntity(challengeTeam);
			jsonObj = createJSONObject(CHALLENGE+"/"+challengeTeam.getChallengeId()+CHALLENGE_TEAM, jsonObj);
			return initializeFromJSONObject(jsonObj, ChallengeTeam.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	@Override
	public ChallengeTeamPagedResults listChallengeTeams(String challengeId, Long limit, Long offset) throws SynapseException {
		validateStringAsLong(challengeId);
		String uri = CHALLENGE+"/"+challengeId+CHALLENGE_TEAM;
		boolean anyParameters = false;
		if  (limit!=null) {
			uri+= (anyParameters?"&":"?")+LIMIT+"="+limit;
			anyParameters = true;
		}
		if  (offset!=null) {
			uri+=(anyParameters?"&":"?")+OFFSET+"="+offset;
			anyParameters = true;
		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		ChallengeTeamPagedResults results = new ChallengeTeamPagedResults();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	@Override
	public PaginatedIds listRegistratableTeams(String challengeId, Long limit, Long offset) throws SynapseException {
		validateStringAsLong(challengeId);
		String uri = CHALLENGE+"/"+challengeId+REGISTRATABLE_TEAM;
		boolean anyParameters = false;
		if  (limit!=null) {
			uri+=(anyParameters ?"&":"?")+LIMIT+"="+limit;
			anyParameters = true;
		}
		if  (offset!=null) {
			uri+=(anyParameters ?"&":"?")+OFFSET+"="+offset;
			anyParameters = true;
		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedIds results = new PaginatedIds();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	@Override
	public PaginatedIds listSubmissionTeams(String challengeId, String submitterPrincipalId, Long limit, Long offset) throws SynapseException {
		validateStringAsLong(challengeId);
		validateStringAsLong(submitterPrincipalId);
		String uri = CHALLENGE+"/"+challengeId+SUBMISSION_TEAMS+"?submitterPrincipalId="+submitterPrincipalId;
		if  (limit!=null) uri+=	"&"+LIMIT+"="+limit;
		if  (offset!=null) uri+="&"+OFFSET+"="+offset;
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedIds results = new PaginatedIds();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	@Override
	public ChallengeTeam updateChallengeTeam(ChallengeTeam challengeTeam) throws SynapseException {
		JSONObjectAdapter toUpdateAdapter = new JSONObjectAdapterImpl();
		JSONObject obj;
		String challengeId = challengeTeam.getChallengeId();
		if (challengeId==null) throw new IllegalArgumentException("challenge ID is required.");
		String challengeTeamId = challengeTeam.getId();
		if (challengeTeamId==null) throw new IllegalArgumentException("ChallengeTeam ID is required.");
		String uri = CHALLENGE+"/"+challengeId+CHALLENGE_TEAM+"/"+challengeTeamId;
		try {
			obj = new JSONObject(challengeTeam.writeToJSONObject(toUpdateAdapter).toJSONString());
			JSONObject jsonObj = getSharedClientConnection().putJson(repoEndpoint, uri, obj.toString(), getUserAgent());
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
			return new ChallengeTeam(adapter);
		} catch (JSONException e1) {
			throw new RuntimeException(e1);
		} catch (JSONObjectAdapterException e1) {
			throw new RuntimeException(e1);
		}
	}

	
	/**
	 * Remove a registered Team from a Challenge.
	 * The user making this request must be registered for the Challenge and
	 * be an administrator of the Team.
	 * 
	 * @param challengeTeamId
	 * @throws SynapseException
	 */
	@Override
	public void deleteChallengeTeam(String challengeTeamId) throws SynapseException {
		validateStringAsLong(challengeTeamId);
		getSharedClientConnection().deleteUri(repoEndpoint, 
				CHALLENGE_TEAM + "/" + challengeTeamId, getUserAgent());
	}
	
	public void addTeamToChallenge(String challengeId, String teamId)
			throws SynapseException {
		throw new RuntimeException("Not Yet Implemented");
	}

	/**
	 * Remove a registered Team from a Challenge. The user making this request
	 * must be registered for the Challenge and be an administrator of the Team.
	 * 
	 * @param challengeId
	 * @param teamId
	 * @throws SynapseException
	 */
	public void removeTeamFromChallenge(String challengeId, String teamId)
			throws SynapseException {
		throw new RuntimeException("Not Yet Implemented");
	}
	
	@Override
	public VerificationSubmission createVerificationSubmission(
			VerificationSubmission verificationSubmission,
			String notificationUnsubscribeEndpoint)
			throws SynapseException {
		String uri = VERIFICATION_SUBMISSION;
		if (notificationUnsubscribeEndpoint!=null) {
			uri += "?" + NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM + "=" + urlEncode(notificationUnsubscribeEndpoint);
		}

		try {
			JSONObject jsonObj = EntityFactory.createJSONObjectForEntity(verificationSubmission);
			jsonObj = createJSONObject(uri, jsonObj);
			return initializeFromJSONObject(jsonObj, VerificationSubmission.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	@Override
	public VerificationPagedResults listVerificationSubmissions(
			VerificationStateEnum currentState, Long submitterId, Long limit,
			Long offset) throws SynapseException {
		String uri = VERIFICATION_SUBMISSION;
		boolean anyParameters = false;
		if  (currentState!=null) {
			uri+=(anyParameters ?"&":"?")+CURRENT_VERIFICATION_STATE+"="+currentState;
			anyParameters = true;
		}
		if  (submitterId!=null) {
			uri+=(anyParameters ?"&":"?")+VERIFIED_USER_ID+"="+submitterId;
			anyParameters = true;
		}
		if  (limit!=null) {
			uri+=(anyParameters ?"&":"?")+LIMIT+"="+limit;
			anyParameters = true;
		}
		if  (offset!=null) {
			uri+=(anyParameters ?"&":"?")+OFFSET+"="+offset;
			anyParameters = true;
		}
		JSONObject jsonObj = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		VerificationPagedResults results = new VerificationPagedResults();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void updateVerificationState(long verificationId,
			VerificationState verificationState,
			String notificationUnsubscribeEndpoint) throws SynapseException {
		String uri = VERIFICATION_SUBMISSION+"/"+verificationId+VERIFICATION_STATE;
		if (notificationUnsubscribeEndpoint!=null) {
			uri += "?" + NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM + "=" + urlEncode(notificationUnsubscribeEndpoint);
		}
		try {
			JSONObject jsonObj = EntityFactory.createJSONObjectForEntity(verificationState);
			createJSONObject(uri, jsonObj);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void deleteVerificationSubmission(long verificationId) throws SynapseException {
		getSharedClientConnection().deleteUri(repoEndpoint, 
				VERIFICATION_SUBMISSION+"/"+verificationId, getUserAgent());
	}

	@Override
	public UserBundle getMyOwnUserBundle(int mask) throws SynapseException {
		JSONObject jsonObj = getEntity(USER+USER_BUNDLE+"?mask="+mask);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		UserBundle results = new UserBundle();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public UserBundle getUserBundle(long principalId, int mask)
			throws SynapseException {
		JSONObject jsonObj = getEntity(USER+"/"+principalId+USER_BUNDLE+"?mask="+mask);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		UserBundle results = new UserBundle();
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}
	
	private static String createFileDownloadUri(FileHandleAssociation fileHandleAssociation, boolean redirect) {
		return FILE + "/" + fileHandleAssociation.getFileHandleId() + "?" +
				FILE_ASSOCIATE_TYPE + "=" + fileHandleAssociation.getAssociateObjectType() +
		"&" + FILE_ASSOCIATE_ID + "=" + fileHandleAssociation.getAssociateObjectId() +
		"&" + REDIRECT_PARAMETER + redirect;
	}

	@Override
	public URL getFileURL(FileHandleAssociation fileHandleAssociation)
			throws SynapseException {
		try {
			return getUrl(getFileEndpoint(), createFileDownloadUri(fileHandleAssociation, false));
		} catch (IOException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void downloadFile(FileHandleAssociation fileHandleAssociation, File target)
			throws SynapseException {
		String uri = createFileDownloadUri(fileHandleAssociation, true);
		getSharedClientConnection().downloadFromSynapse(
				getFileEndpoint() + uri, null, target, getUserAgent());
	}

	@Override
	public Forum getForumByProjectId(String projectId) throws SynapseException {
		try {
			ValidateArgument.required(projectId, "projectId");
			return getJSONEntity(PROJECT+"/"+projectId+FORUM, Forum.class);
		} catch (Exception e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public Forum getForum(String forumId) throws SynapseException {
		try {
			ValidateArgument.required(forumId, "forumId");
			return getJSONEntity(FORUM+"/"+forumId, Forum.class);
		} catch (Exception e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public DiscussionThreadBundle createThread(CreateDiscussionThread toCreate)
			throws SynapseException {
		ValidateArgument.required(toCreate, "toCreate");
		return asymmetricalPost(repoEndpoint, THREAD, toCreate, DiscussionThreadBundle.class, null);
	}

	@Override
	public DiscussionThreadBundle getThread(String threadId)
			throws SynapseException{
		try {
			ValidateArgument.required(threadId, "threadId");
			return getJSONEntity(THREAD+"/"+threadId, DiscussionThreadBundle.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreadsForForum(
			String forumId, Long limit, Long offset, DiscussionThreadOrder order,
			Boolean ascending, DiscussionFilter filter) throws SynapseException {
		ValidateArgument.required(forumId, "forumId");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		ValidateArgument.required(filter, "filter");
		String url = FORUM+"/"+forumId+THREADS
				+"?"+LIMIT+"="+limit+"&"+OFFSET+"="+offset;
		if (order != null) {
			url += "&sort="+order.name();
		}
		if (ascending != null) {
			url += "&ascending="+ascending;
		}
		url += "&filter="+filter.toString();
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<DiscussionThreadBundle> results =
				new PaginatedResults<DiscussionThreadBundle>(DiscussionThreadBundle.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public DiscussionThreadBundle updateThreadTitle(String threadId,
			UpdateThreadTitle newTitle) throws SynapseException {
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(newTitle, "newTitle");
		return asymmetricalPut(repoEndpoint, THREAD+"/"+threadId+THREAD_TITLE, newTitle, DiscussionThreadBundle.class);
	}

	@Override
	public DiscussionThreadBundle updateThreadMessage(String threadId,
			UpdateThreadMessage newMessage) throws SynapseException {
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(newMessage, "newMessage");
		return asymmetricalPut(repoEndpoint, THREAD+"/"+threadId+DISCUSSION_MESSAGE, newMessage, DiscussionThreadBundle.class);
	}

	@Override
	public void markThreadAsDeleted(String threadId) throws SynapseException {
		getSharedClientConnection().deleteUri(repoEndpoint, THREAD+"/"+threadId, getUserAgent());
	}

	@Override
	public DiscussionReplyBundle createReply(CreateDiscussionReply toCreate)
			throws SynapseException {
		ValidateArgument.required(toCreate, "toCreate");
		return asymmetricalPost(repoEndpoint, REPLY, toCreate, DiscussionReplyBundle.class, null);
	}

	@Override
	public DiscussionReplyBundle getReply(String replyId)
			throws SynapseException {
		try {
			ValidateArgument.required(replyId, "replyId");
			return getJSONEntity(REPLY+"/"+replyId, DiscussionReplyBundle.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public PaginatedResults<DiscussionReplyBundle> getRepliesForThread(
			String threadId, Long limit, Long offset,
			DiscussionReplyOrder order, Boolean ascending, DiscussionFilter filter)
			throws SynapseException {
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		ValidateArgument.required(filter, "filter");
		String url = THREAD+"/"+threadId+REPLIES
				+"?"+LIMIT+"="+limit+"&"+OFFSET+"="+offset;
		if (order != null) {
			url += "&sort="+order.name();
		}
		if (ascending != null) {
			url += "&ascending="+ascending;
		}
		url += "&filter="+filter;
		JSONObject jsonObj = getEntity(url);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<DiscussionReplyBundle> results =
				new PaginatedResults<DiscussionReplyBundle>(DiscussionReplyBundle.class);

		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public DiscussionReplyBundle updateReplyMessage(String replyId,
			UpdateReplyMessage newMessage) throws SynapseException {
		ValidateArgument.required(replyId, "replyId");
		ValidateArgument.required(newMessage, "newMessage");
		return asymmetricalPut(repoEndpoint, REPLY+"/"+replyId+DISCUSSION_MESSAGE, newMessage, DiscussionReplyBundle.class);
	}

	@Override
	public void markReplyAsDeleted(String replyId) throws SynapseException {
		getSharedClientConnection().deleteUri(repoEndpoint, REPLY+"/"+replyId, getUserAgent());
	}

	@Override
	public MultipartUploadStatus startMultipartUpload(
			MultipartUploadRequest request, Boolean forceRestart) throws SynapseException {
		ValidateArgument.required(request, "MultipartUploadRequest");
		StringBuilder pathBuilder = new StringBuilder();
		pathBuilder.append("/file/multipart");
		//the restart parameter is optional.
		if(forceRestart != null){
			pathBuilder.append("?forceRestart=");
			pathBuilder.append(forceRestart.toString());
		}
		return asymmetricalPost(fileEndpoint, pathBuilder.toString(), request, MultipartUploadStatus.class, null);
	}

	@Override
	public BatchPresignedUploadUrlResponse getMultipartPresignedUrlBatch(
			BatchPresignedUploadUrlRequest request) throws SynapseException {
		ValidateArgument.required(request, "BatchPresignedUploadUrlRequest");
		ValidateArgument.required(request.getUploadId(), "BatchPresignedUploadUrlRequest.uploadId");
		String path = String.format("/file/multipart/%1$s/presigned/url/batch", request.getUploadId());
		return asymmetricalPost(fileEndpoint,path,request, BatchPresignedUploadUrlResponse.class, null);
	}

	@Override
	public AddPartResponse addPartToMultipartUpload(String uploadId,
			int partNumber, String partMD5Hex) throws SynapseException {
		ValidateArgument.required(uploadId, "uploadId");
		ValidateArgument.required(partMD5Hex, "partMD5Hex");
		String path = String.format("/file/multipart/%1$s/add/%2$d?partMD5Hex=%3$s", uploadId, partNumber, partMD5Hex);
		return asymmetricalPut(fileEndpoint, path, null, AddPartResponse.class);
	}

	@Override
	public MultipartUploadStatus completeMultipartUpload(String uploadId) throws SynapseException {
		ValidateArgument.required(uploadId, "uploadId");
		String path = String.format("/file/multipart/%1$s/complete", uploadId);
		return asymmetricalPut(fileEndpoint, path, null, MultipartUploadStatus.class);
	}

	@Override
	public S3FileHandle multipartUpload(InputStream input, long fileSize, String fileName,
			String contentType, Long storageLocationId, Boolean generatePreview, Boolean forceRestart) throws SynapseException {
		return new MultipartUpload(this, input, fileSize, fileName, contentType, storageLocationId, generatePreview, forceRestart, new TempFileProviderImpl()).uploadFile();
	}


	@Override
	public S3FileHandle multipartUpload(File file,
			Long storageLocationId, Boolean generatePreview,
			Boolean forceRestart) throws SynapseException, IOException {
		InputStream fileInputStream = null;
		try{
			fileInputStream = new FileInputStream(file);
			String fileName = file.getName();
			long fileSize = file.length();
			String contentType = guessContentTypeFromStream(file);
			return multipartUpload(fileInputStream, fileSize, fileName, contentType, storageLocationId, generatePreview, forceRestart);
		}finally{
			IOUtils.closeQuietly(fileInputStream);
		}
	}

	@Override
	public URL getReplyUrl(String messageKey) throws SynapseException {
		try {
			ValidateArgument.required(messageKey, "messageKey");
			return new URL(getJSONEntity(REPLY+URL+"?messageKey="+messageKey, MessageURL.class).getMessageUrl());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		} catch (MalformedURLException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public URL getThreadUrl(String messageKey) throws SynapseException {
		try {
			ValidateArgument.required(messageKey, "messageKey");
			return new URL(getJSONEntity(THREAD+URL+"?messageKey="+messageKey, MessageURL.class).getMessageUrl());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		} catch (MalformedURLException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public Subscription subscribe(Topic toSubscribe) throws SynapseException {
		ValidateArgument.required(toSubscribe, "toSubscribe");
		return asymmetricalPost(repoEndpoint, SUBSCRIPTION, toSubscribe, Subscription.class, null);
	}

	@Override
	public SubscriptionPagedResults getAllSubscriptions(
			SubscriptionObjectType objectType, Long limit, Long offset) throws SynapseException {
		try {
			ValidateArgument.required(limit, "limit");
			ValidateArgument.required(offset, "offset");
			ValidateArgument.required(objectType, "objectType");
			String url = SUBSCRIPTION+ALL+"?"+LIMIT+"="+limit+"&"+OFFSET+"="+offset;
			url += "&"+OBJECT_TYPE_PARAM+"="+objectType.name();
			return getJSONEntity(url, SubscriptionPagedResults.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public SubscriptionPagedResults listSubscriptions(SubscriptionRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getObjectType(), "SubscriptionRequest.objectType");
		ValidateArgument.required(request.getIdList(), "SubscriptionRequest.idList");
		return asymmetricalPost(repoEndpoint, SUBSCRIPTION+LIST, request, SubscriptionPagedResults.class, null);
	}

	@Override
	public void unsubscribe(Long subscriptionId) throws SynapseException {
		ValidateArgument.required(subscriptionId, "subscriptionId");
		getSharedClientConnection().deleteUri(repoEndpoint, SUBSCRIPTION+"/"+subscriptionId, getUserAgent());
	}

	@Override
	public void unsubscribeAll() throws SynapseException {
		getSharedClientConnection().deleteUri(repoEndpoint, SUBSCRIPTION+ALL, getUserAgent());
	}

	@Override
	public Subscription getSubscription(String subscriptionId) throws SynapseException {
		try {
			ValidateArgument.required(subscriptionId, "subscriptionId");
			return getJSONEntity(SUBSCRIPTION+"/"+subscriptionId, Subscription.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public Etag getEtag(String objectId, ObjectType objectType) throws SynapseException {
		try {
			ValidateArgument.required(objectId, "objectId");
			ValidateArgument.required(objectType, "objectType");
			return getJSONEntity(OBJECT+"/"+objectId+"/"+objectType.name()+"/"+ETAG, Etag.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public EntityId getEntityIdByAlias(String alias) throws SynapseException {
		ValidateArgument.required(alias, "alias");
		try {
			return getJSONEntity(ENTITY+"/alias/"+alias, EntityId.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public ThreadCount getThreadCountForForum(String forumId, DiscussionFilter filter) throws SynapseException {
		ValidateArgument.required(forumId, "forumId");
		ValidateArgument.required(filter, "filter");
		String url = FORUM+"/"+forumId+THREAD_COUNT;
		url += "?filter="+filter;
		try {
			return getJSONEntity(url, ThreadCount.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public ReplyCount getReplyCountForThread(String threadId, DiscussionFilter filter) throws SynapseException {
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(filter, "filter");
		String url = THREAD+"/"+threadId+REPLY_COUNT;
		url += "?filter="+filter;
		try {
			return getJSONEntity(url, ReplyCount.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void pinThread(String threadId) throws SynapseException {
		getSharedClientConnection().putUri(repoEndpoint, THREAD+"/"+threadId+PIN, getUserAgent());
	}

	@Override
	public void unpinThread(String threadId) throws SynapseException {
		getSharedClientConnection().putUri(repoEndpoint, THREAD+"/"+threadId+UNPIN, getUserAgent());
	}

	@Override
	public PrincipalAliasResponse getPrincipalAlias(PrincipalAliasRequest request) throws SynapseException {
		return asymmetricalPost(repoEndpoint, PRINCIPAL+"/alias/", request, PrincipalAliasResponse.class, null);
	}
}
