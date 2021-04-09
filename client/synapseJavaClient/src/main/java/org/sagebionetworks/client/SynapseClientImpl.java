package org.sagebionetworks.client;

import static org.sagebionetworks.client.Method.GET;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseResultNotReadyException;
import org.sagebionetworks.client.exceptions.SynapseTermsOfUseException;
import org.sagebionetworks.evaluation.model.BatchUploadResponse;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.EvaluationRoundListRequest;
import org.sagebionetworks.evaluation.model.EvaluationRoundListResponse;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusBatch;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.TeamSubmissionEligibility;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoRequest;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoResponse;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengePagedResults;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.ChallengeTeamPagedResults;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.InviteeVerificationSignedToken;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.LogEntry;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSignedToken;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectHeaderList;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.ProjectListTypeDeprecated;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResponseMessage;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptorResponse;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMemberTypeFilterOptions;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UserBundle;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.annotation.AnnotationsUtils;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationRequest;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationResponse;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.AccessTokenRecordList;
import org.sagebionetworks.repo.model.auth.ChangePasswordInterface;
import org.sagebionetworks.repo.model.auth.ChangePasswordWithCurrentPassword;
import org.sagebionetworks.repo.model.auth.JSONWebTokenHelper;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.SecretKey;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementConversionRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRevokeRequest;
import org.sagebionetworks.repo.model.dataaccess.CreateSubmissionRequest;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfoPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfoPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionOrder;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.ReplyCount;
import org.sagebionetworks.repo.model.discussion.ThreadCount;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.doi.v2.DoiRequest;
import org.sagebionetworks.repo.model.doi.v2.DoiResponse;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.download.QueryRequestDetails;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListRequest;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListResponse;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.entity.EntityLookupRequest;
import org.sagebionetworks.repo.model.entity.FileHandleUpdateRequest;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundleCreate;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundleRequest;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListRequest;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListResponse;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyResult;
import org.sagebionetworks.repo.model.file.BatchFileRequest;
import org.sagebionetworks.repo.model.file.BatchFileResult;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.DownloadOrder;
import org.sagebionetworks.repo.model.file.DownloadOrderSummaryRequest;
import org.sagebionetworks.repo.model.file.DownloadOrderSummaryResponse;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileHandleAssociationList;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.form.FormChangeRequest;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.form.FormRejection;
import org.sagebionetworks.repo.model.form.ListRequest;
import org.sagebionetworks.repo.model.form.ListResponse;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;
import org.sagebionetworks.repo.model.oauth.OAuthAccountCreationRequest;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientAuthorizationHistoryList;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.model.oauth.OAuthConsentGrantedResponse;
import org.sagebionetworks.repo.model.oauth.OAuthGrantType;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformationList;
import org.sagebionetworks.repo.model.oauth.OAuthTokenRevocationRequest;
import org.sagebionetworks.repo.model.oauth.OAuthUrlRequest;
import org.sagebionetworks.repo.model.oauth.OAuthUrlResponse;
import org.sagebionetworks.repo.model.oauth.OAuthValidationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
import org.sagebionetworks.repo.model.oauth.OIDConnectConfiguration;
import org.sagebionetworks.repo.model.principal.AccountSetupInfo;
import org.sagebionetworks.repo.model.principal.AliasCheckRequest;
import org.sagebionetworks.repo.model.principal.AliasCheckResponse;
import org.sagebionetworks.repo.model.principal.AliasList;
import org.sagebionetworks.repo.model.principal.EmailValidationSignedToken;
import org.sagebionetworks.repo.model.principal.NotificationEmail;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAliasResponse;
import org.sagebionetworks.repo.model.principal.TypeFilter;
import org.sagebionetworks.repo.model.principal.UserGroupHeaderResponse;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.query.QueryTableResults;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.Quiz;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.model.report.DownloadStorageReportRequest;
import org.sagebionetworks.repo.model.report.DownloadStorageReportResponse;
import org.sagebionetworks.repo.model.report.StorageReportType;
import org.sagebionetworks.repo.model.request.ReferenceList;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoResponse;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoResponse;
import org.sagebionetworks.repo.model.schema.ListOrganizationsRequest;
import org.sagebionetworks.repo.model.schema.ListOrganizationsResponse;
import org.sagebionetworks.repo.model.schema.ListValidationResultsRequest;
import org.sagebionetworks.repo.model.schema.ListValidationResultsResponse;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsRequest;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsResponse;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.sts.StsCredentials;
import org.sagebionetworks.repo.model.sts.StsPermission;
import org.sagebionetworks.repo.model.subscription.SortByType;
import org.sagebionetworks.repo.model.subscription.SubscriberCount;
import org.sagebionetworks.repo.model.subscription.SubscriberPagedResults;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.repo.model.table.AppendableRowSet;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowReferenceSetResults;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.SnapshotResponse;
import org.sagebionetworks.repo.model.table.SqlTransformRequest;
import org.sagebionetworks.repo.model.table.SqlTransformResponse;
import org.sagebionetworks.repo.model.table.TableFileHandleResults;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.sagebionetworks.repo.model.table.ViewColumnModelRequest;
import org.sagebionetworks.repo.model.table.ViewColumnModelResponse;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientConfig;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.sagebionetworks.util.FileProviderImpl;
import org.sagebionetworks.util.ValidateArgument;

import com.google.common.base.Joiner;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;

/**
 * Low-level Java Client API for Synapse REST APIs
 */
public class SynapseClientImpl extends BaseClientImpl implements SynapseClient {

	public static final String SYNAPSE_JAVA_CLIENT = "Synapse-Java-Client/";

	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

	private static final String ACCOUNT = "/account";
	private static final String EMAIL_VALIDATION = "/emailValidation";
	private static final String ACCOUNT_EMAIL_VALIDATION = ACCOUNT
			+ EMAIL_VALIDATION;
	private static final String EMAIL = "/email";
	private static final String PORTAL_ENDPOINT_PARAM = "portalEndpoint";
	private static final String SET_AS_NOTIFICATION_EMAIL_PARAM = "setAsNotificationEmail";
	private static final String EMAIL_PARAM = "email";

	private static final String PARAM_GENERATED_BY = "generatedBy";
	private static final String PARAM_NEW_VERSION = "newVersion";

	private static final String QUERY = "/query";
	private static final String QUERY_URI = QUERY + "?query=";
	private static final String REPO_SUFFIX_VERSION = "/version";
	protected static final String STACK_STATUS = "/status";
	private static final String ENTITY = "/entity";
	private static final String GENERATED_BY_SUFFIX = "/generatedBy";

	private static final String ENTITY_URI_PATH = "/entity";
	private static final String ENTITY_ACL_PATH_SUFFIX = "/acl";
	private static final String BUNDLE = "/bundle";
	private static final String BUNDLE_V2 = "/bundle2";
	private static final String BENEFACTOR = "/benefactor"; // from
	private static final String CREATE = "/create";
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

	private static final String EVALUATION_URI_PATH = "/evaluation";
	private static final String EVALUATION_ROUND = "/round";
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

	protected static final String ASYNC_START = "/async/start";
	protected static final String ASYNC_GET = "/async/get/";

	protected static final String COLUMN = "/column";
	protected static final String COLUMN_BATCH = COLUMN + "/batch";
	protected static final String COLUMN_VIEW_DEFAULT = COLUMN + "/tableview/defaults";
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
	
	protected static final String TABLE_TRANSACTION = TABLE+"/transaction";

	protected static final String ASYNCHRONOUS_JOB = "/asynchronous/job";

	private static final String USER_PROFILE_PATH = "/userProfile";
	private static final String NOTIFICATION_SETTINGS = "/notificationSettings";

	private static final String PROFILE_IMAGE = "/image";
	private static final String PROFILE_IMAGE_PREVIEW = PROFILE_IMAGE+"/preview";

	private static final String USER_GROUP_HEADER_BATCH_PATH = "/userGroupHeaders/batch?ids=";

	private static final String USER_GROUP_HEADER_PREFIX_PATH = "/userGroupHeaders?prefix=";
	
	private static final String USER_GROUP_HEADER_BY_ALIAS = "/userGroupHeaders/aliases";

	private static final String ACCESS_REQUIREMENT = "/accessRequirement";

	private static final String ACCESS_APPROVAL = "/accessApproval";

	private static final String VERSION_INFO = "/version";

	private static final String FILE_HANDLE = "/fileHandle";
	private static final String FILE = "/file";
	private static final String FILE_PREVIEW = "/filepreview";
	private static final String EXTERNAL_FILE_HANDLE = "/externalFileHandle";
	private static final String EXTERNAL_FILE_HANDLE_S3 = "/externalFileHandle/s3";
	private static final String EXTERNAL_FILE_HANDLE_GOOGLE_CLOUD = "/externalFileHandle/googleCloud";
	private static final String FILE_HANDLES = "/filehandles";
	protected static final String S3_FILE_COPY = FILE + "/s3FileCopy";
	private static final String FILE_HANDLES_COPY = FILE_HANDLES+"/copy";
	
	protected static final String FILE_BULK = FILE+"/bulk";

	private static final String TRASHCAN_TRASH = "/trashcan/trash";
	private static final String TRASHCAN_RESTORE = "/trashcan/restore";
	private static final String TRASHCAN_VIEW = "/trashcan/view";
	private static final String TRASHCAN_PURGE = "/trashcan/purge";

	private static final String CHALLENGE = "/challenge";
	private static final String REGISTRATABLE_TEAM = "/registratableTeam";
	private static final String CHALLENGE_TEAM = "/challengeTeam";
	private static final String SUBMISSION_TEAMS = "/submissionTeams";

	private static final String LOG = "/log";

	protected static final String DOI = "/doi";
	private static final String DOI_ASSOCIATION = DOI + "/association";
	private static final String DOI_LOCATE = DOI + "/locate";

	private static final String ETAG = "etag";

	private static final String PROJECT_SETTINGS = "/projectSettings";

	private static final String STORAGE_LOCATION = "/storageLocation";

	public static final String AUTH_OAUTH_2 = "/oauth2";
	public static final String AUTH_OAUTH_2_AUTH_URL = AUTH_OAUTH_2+"/authurl";
	public static final String AUTH_OAUTH_2_SESSION = AUTH_OAUTH_2+"/session";
	public static final String AUTH_OAUTH_2_ACCOUNT = AUTH_OAUTH_2+"/account";
	public static final String AUTH_OAUTH_2_ALIAS = AUTH_OAUTH_2+"/alias";
	
	public static final String AUTH_OPENID_CONFIG = "/.well-known/openid-configuration";
	public static final String AUTH_OAUTH_2_JWKS = AUTH_OAUTH_2+"/jwks";
	public static final String AUTH_OAUTH_2_CLIENT = AUTH_OAUTH_2+"/client";
	public static final String AUTH_OAUTH_2_CLIENT_SECRET = AUTH_OAUTH_2_CLIENT+"/secret/";
	public static final String AUTH_OAUTH_2_REQUEST_DESCRIPTION = AUTH_OAUTH_2+"/description";
	public static final String AUTH_OAUTH_2_REQUEST_CONSENT = AUTH_OAUTH_2+"/consent";
	private static final String AUTH_OAUTH_2_CONSENT_CHECK = AUTH_OAUTH_2+"/consentcheck";
	public static final String AUTH_OAUTH_2_TOKEN = AUTH_OAUTH_2+"/token";
	public static final String AUTH_OAUTH_2_USER_INFO = AUTH_OAUTH_2+"/userinfo";

	public static final String AUTH_OAUTH_2_AUDIT = AUTH_OAUTH_2 + "/audit";
	public static final String AUTH_OAUTH_2_AUDIT_TOKENS = AUTH_OAUTH_2_AUDIT + "/tokens";
	public static final String AUTH_OAUTH_2_AUDIT_CLIENTS = AUTH_OAUTH_2_AUDIT + "/grantedClients";
	public static final String METADATA = "/metadata";
	public static final String REVOKE = "/revoke";
	public static final String TOKENS = "/tokens";

	public static final String AUTH_OAUTH_2_GRANT_TYPE_PARAM = "grant_type";
	public static final String AUTH_OAUTH_2_CODE_PARAM = "code";
	public static final String AUTH_OAUTH_2_REDIRECT_URI_PARAM = "redirect_uri";
	public static final String AUTH_OAUTH_2_REfRESH_TOKEN_PARAM = "refresh_token";
	public static final String AUTH_OAUTH_2_SCOPE_PARAM = "scope";
	public static final String AUTH_OAUTH_2_CLAIMS_PARAM = "claims";

	public static final String AUTH_PERSONAL_ACCESS_TOKEN = "/personalAccessToken";

	private static final String VERIFICATION_SUBMISSION = "/verificationSubmission";
	private static final String CURRENT_VERIFICATION_STATE = "currentVerificationState";
	private static final String VERIFICATION_STATE = "/state";
	private static final String VERIFIED_USER_ID = "verifiedUserId";
	private static final String USER_BUNDLE = "/bundle";
	private static final String FILE_ASSOCIATE_TYPE = "fileAssociateType";
	private static final String FILE_ASSOCIATE_ID = "fileAssociateId";
	
	public static final String FILE_HANDLE_BATCH = "/fileHandle/batch";
	
	// web request pagination parameters
	public static final String LIMIT = "limit";
	public static final String OFFSET = "offset";

	// Team
	protected static final String TEAM = "/team";
	protected static final String TEAMS = "/teams";
	private static final String TEAM_LIST = "/teamList";
	private static final String MEMBER_LIST = "/memberList";
	protected static final String USER = "/user";
	protected static final String NAME_FRAGMENT_FILTER = "fragment";
	protected static final String NAME_MEMBERTYPE_FILTER = "memberType";
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
	private static final String OPEN_MEMBERSHIP_INVITATION_COUNT = MEMBERSHIP_INVITATION + "/openInvitationCount";
	// membership request
	private static final String MEMBERSHIP_REQUEST = "/membershipRequest";
	private static final String OPEN_MEMBERSHIP_REQUEST = "/openRequest";
	private static final String REQUESTOR_ID_REQUEST_PARAMETER = "requestorId";
	private static final String OPEN_MEMBERSHIP_REQUEST_COUNT = MEMBERSHIP_REQUEST + "/openRequestCount";
	
	public static final String ACCEPT_INVITATION_ENDPOINT_PARAM = "acceptInvitationEndpoint";
	public static final String ACCEPT_REQUEST_ENDPOINT_PARAM = "acceptRequestEndpoint";
	public static final String NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM = "notificationUnsubscribeEndpoint";
	public static final String TEAM_ENDPOINT_PARAM = "teamEndpoint";
	public static final String CHALLENGE_ENDPOINT_PARAM = "challengeEndpoint";
	
	private static final String CERTIFIED_USER_TEST = "/certifiedUserTest";
	private static final String CERTIFIED_USER_TEST_RESPONSE = "/certifiedUserTestResponse";
	private static final String CERTIFIED_USER_PASSING_RECORD = "/certifiedUserPassingRecord";

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
	private static final String RESTORE = "/restore";
	private static final String MODERATORS = "/moderators";
	private static final String STATISTICS = "/statistics";
	

	private static final String THREAD_COUNTS = "/threadcounts";
	private static final String ENTITY_THREAD_COUNTS = ENTITY + THREAD_COUNTS;

	private static final String SUBSCRIPTION = "/subscription";
	private static final String LIST = "/list";
	private static final String OBJECT_TYPE_PARAM = "objectType";
	private static final String OBJECT = "/object";	

	private static final String PRINCIPAL_ID_REQUEST_PARAM = "principalId";

	private static final String DOCKER_COMMIT = "/dockerCommit";
	private static final String DOCKER_TAG = "/dockerTag";

	private static final String NEXT_PAGE_TOKEN_PARAM = "nextPageToken=";

	/**
	 * Note: 5 MB is currently the minimum size of a single part of S3
	 * Multi-part upload, so any file chunk must be at least this size.
	 */
	public static final int MINIMUM_CHUNK_SIZE_BYTES = ((int) Math.pow(2, 20)) * 5;

	private static final String RESEARCH_PROJECT = "/researchProject";
	private static final String DATA_ACCESS_REQUEST = "/dataAccessRequest";
	private static final String DATA_ACCESS_SUBMISSION = "/dataAccessSubmission";
	
	public static final String DOWNLOAD_LIST = "/download/list";
	public static final String DOWNLOAD_LIST_ADD = DOWNLOAD_LIST+"/add";
	public static final String DOWNLOAD_LIST_REMOVE = DOWNLOAD_LIST+"/remove";
	public static final String DOWNLOAD_LIST_CLEAR = DOWNLOAD_LIST+"/clear";
	
	public static final String DOWNLOAD_LIST_QUERY = DOWNLOAD_LIST+"/query";
	
	public static final String DOWNLOAD_ORDER = "/download/order";
	public static final String DOWNLOAD_ORDER_ID = DOWNLOAD_ORDER+"/{orderId}";
	public static final String DOWNLOAD_ORDER_HISTORY = DOWNLOAD_ORDER+"/history";

	public static final String STORAGE_REPORT = "/storageReport";
	public static final String ANNOTATIONS_V2 = "/annotations2";
	
	public static final String SCHEMA_TYPE_CREATE = "/schema/type/create/";
	public static final String SCHEMA_TYPE_VALIDATION = "/schema/type/validation/";
	public static final String VIEW_COLUMNS = "/column/view/scope/";
	

	/**
	 * Default constructor uses the default repository and file services endpoints.
	 */
	public SynapseClientImpl() {
		this(null);
	}

	public SynapseClientImpl(SimpleHttpClientConfig config) {
		super(SYNAPSE_JAVA_CLIENT + ClientVersionInfo.getClientVersionInfo(), config);
	}

	/**
	 * Lookup the endpoint for a given type.
	 * @param type
	 * @return
	 */
	protected String getEndpointForType(RestEndpointType type){
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

	/********************
	 * Mid Level Repository Service APIs
	 * 
	 * @throws SynapseException
	 ********************/

	@Override
	public AliasCheckResponse checkAliasAvailable(AliasCheckRequest request)
			throws SynapseException {
		return postJSONEntity(getRepoEndpoint(), PRINCIPAL_AVAILABLE, request,
				AliasCheckResponse.class);
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
		ValidateArgument.required(user, "NewUser");
		ValidateArgument.required(portalEndpoint, "portalEndpoint");

		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put(PORTAL_ENDPOINT_PARAM, portalEndpoint);

		voidPost(getRepoEndpoint(), ACCOUNT_EMAIL_VALIDATION, user, paramMap);
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
		ValidateArgument.required(accountSetupInfo, "accountSetupInfo");
		return postJSONEntity(getRepoEndpoint(), ACCOUNT, accountSetupInfo, Session.class);
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
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(email, "email");
		ValidateArgument.required(portalEndpoint, "portalEndpoint");

		String uri = ACCOUNT + "/" + userId + EMAIL_VALIDATION;
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put(PORTAL_ENDPOINT_PARAM, portalEndpoint);

		Username emailRequestBody = new Username();
		emailRequestBody.setEmail(email);

		voidPost(getRepoEndpoint(), uri, emailRequestBody, paramMap);
	}

	/**
	 * Add a new email address to an existing account.
	 * 
	 * @param emailValidationSignedToken
	 *            the token sent to the user via email
	 * @param setAsNotificationEmail
	 *            if true then set the new email address to be the user's
	 *            notification address
	 * @throws NotFoundException
	 */
	@Override
	public void addEmail(EmailValidationSignedToken emailValidationSignedToken,
			Boolean setAsNotificationEmail) throws SynapseException {
		ValidateArgument.required(emailValidationSignedToken, "emailValidationSignedToken");

		Map<String, String> paramMap = new HashMap<String, String>();
		if (setAsNotificationEmail != null) {
			paramMap.put(SET_AS_NOTIFICATION_EMAIL_PARAM, setAsNotificationEmail.toString());
		}

		voidPost(getRepoEndpoint(), EMAIL, emailValidationSignedToken, paramMap);
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
		ValidateArgument.required(email, "email");

		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put(EMAIL_PARAM, email);

		super.deleteUri(getRepoEndpoint(), EMAIL, paramMap);
	}

	/**
	 * This sets the email used for user notifications, i.e. when a Synapse
	 * message is sent and if the user has elected to receive messages by email,
	 * then this is the email address at which the user will receive the
	 * message. Note: The given email address must already be established as
	 * being owned by the user.
	 */
	public void setNotificationEmail(String email) throws SynapseException {
		ValidateArgument.required(email, "email");
		Username username = new Username();
		username.setEmail(email);
		voidPut(getRepoEndpoint(), NOTIFICATION_EMAIL, username);
	}

	/**
	 * This gets the email used for user notifications, i.e. when a Synapse
	 * message is sent and if the user has elected to receive messages by email,
	 * then this is the email address at which the user will receive the
	 * message.
	 * 
	 * @throws SynapseException
	 */
	public NotificationEmail getNotificationEmail() throws SynapseException {
		return getJSONEntity(getRepoEndpoint(), NOTIFICATION_EMAIL, NotificationEmail.class);
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
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Entity> T createEntity(T entity, String activityId)
			throws SynapseException {
		ValidateArgument.required(entity, "entity");
		entity.setConcreteType(entity.getClass().getName());
		String uri = ENTITY_URI_PATH;
		if (activityId != null) {
			uri += "?" + PARAM_GENERATED_BY + "=" + activityId;
		}
		return (T) postJSONEntity(getRepoEndpoint(), uri, entity, entity.getClass());
	}


	/**
	 * Create an Entity, Annotations, and ACL with a single call.
	 *
	 * @param ebc
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public EntityBundle createEntityBundleV2(EntityBundleCreate ebc)
			throws SynapseException {
		return createEntityBundleV2(ebc, null);
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
	public EntityBundle createEntityBundleV2(EntityBundleCreate ebc,
											 String activityId) throws SynapseException {
		ValidateArgument.required(ebc, "EntityBundleV2Create");
		String url = ENTITY_URI_PATH + BUNDLE_V2 + CREATE;
		if (activityId != null) {
			url += "?" + PARAM_GENERATED_BY + "=" + activityId;
		}
		return postJSONEntity(getRepoEndpoint(), url, ebc, EntityBundle.class);
	}

	/**
	 * Update an Entity, Annotations, and ACL with a single call.
	 *
	 * @param ebc
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public EntityBundle updateEntityBundleV2(String entityId,
											 EntityBundleCreate ebc) throws SynapseException {
		return updateEntityBundleV2(entityId, ebc, null);
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
	public EntityBundle updateEntityBundleV2(String entityId,
											 EntityBundleCreate ebc, String activityId) throws SynapseException {
		ValidateArgument.required(ebc, "EntityBundleV2Create");
		String url = ENTITY_URI_PATH + "/" + entityId + BUNDLE_V2;
		return putJSONEntity(getRepoEndpoint(), url, ebc, EntityBundle.class);
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
		ValidateArgument.required(entityId, "entityId");
		String url = ENTITY_URI_PATH + "/" + entityId;
		if (versionNumber != null) {
			url += REPO_SUFFIX_VERSION + "/" + versionNumber;
		}
		return getJSONEntity(getRepoEndpoint(), url, Entity.class);
	}

	/**
	 * Get a bundle of information about an entity in a single call.
	 * 
	 * @param entityId
	 * @param bundleV2Request
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public EntityBundle getEntityBundleV2(String entityId, EntityBundleRequest bundleV2Request)
			throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		String url = ENTITY_URI_PATH + "/" + entityId + BUNDLE_V2;
		return postJSONEntity(getRepoEndpoint(), url, bundleV2Request, EntityBundle.class);
	}

	/**
	 * Get a bundle of information about an entity in a single call.
	 * 
	 * @param entityId
	 *            - the entity id to retrieve
	 * @param versionNumber
	 *            - the specific version to retrieve
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public EntityBundle getEntityBundleV2(String entityId, Long versionNumber,
										  EntityBundleRequest bundleV2Request) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(versionNumber, "versionNumber");
		String url = ENTITY_URI_PATH + "/" + entityId + REPO_SUFFIX_VERSION
				+ "/" + versionNumber + BUNDLE_V2;
		return postJSONEntity(getRepoEndpoint(), url, bundleV2Request, EntityBundle.class);
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
		ValidateArgument.required(entityId, "entityId");
		String url = ENTITY_URI_PATH + "/" + entityId + REPO_SUFFIX_VERSION
				+ "?" + OFFSET + "=" + offset + "&limit=" + limit;
		return getPaginatedResults(getRepoEndpoint(), url, VersionInfo.class);
	}

	@Override
	public AccessControlList getACL(String entityId) throws SynapseException {
		String uri = ENTITY_URI_PATH + "/" + entityId + ENTITY_ACL_PATH_SUFFIX;
		return getJSONEntity(getRepoEndpoint(), uri, AccessControlList.class);
	}

	@Override
	public EntityHeader getEntityBenefactor(String entityId)
			throws SynapseException {
		String uri = ENTITY_URI_PATH + "/" + entityId + BENEFACTOR;
		return getJSONEntity(getRepoEndpoint(), uri, EntityHeader.class);
	}

	@Override
	public UserProfile getMyProfile() throws SynapseException {
		return getJSONEntity(getRepoEndpoint(), USER_PROFILE_PATH, UserProfile.class);
	}

	@Override
	public void updateMyProfile(UserProfile userProfile) throws SynapseException {
		voidPut(getRepoEndpoint(), USER_PROFILE_PATH, userProfile);
	}

	@Override
	public ResponseMessage updateNotificationSettings(NotificationSettingsSignedToken token) throws SynapseException {
		return putJSONEntity(getRepoEndpoint(), NOTIFICATION_SETTINGS, token, ResponseMessage.class);
	}

	@Override
	public StsCredentials getTemporaryCredentialsForEntity(String entityId, StsPermission permission)
			throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(permission, "permission");

		String uri = ENTITY_URI_PATH + "/" + entityId + "/sts?permission=" + permission.name();
		return getJSONEntity(getRepoEndpoint(), uri, StsCredentials.class);
	}

	@Override
	public UserProfile getUserProfile(String ownerId) throws SynapseException {
		String uri = USER_PROFILE_PATH + "/" + ownerId;
		return getJSONEntity(getRepoEndpoint(), uri, UserProfile.class);
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
		return getJSONEntity(getRepoEndpoint(), uri, UserGroupHeaderResponsePage.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.client.SynapseClient#getUserProfilePictureUrl(java.lang.String)
	 */
	@Override
	public URL getUserProfilePictureUrl(String ownerId) throws ClientProtocolException, MalformedURLException, IOException, SynapseException {
		return getUrl(getRepoEndpoint(),
				USER_PROFILE_PATH+"/"+ownerId+PROFILE_IMAGE+"?"+REDIRECT_PARAMETER+"false");
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.client.SynapseClient#getUserProfilePicturePreviewUrl(java.lang.String)
	 */
	@Override
	public URL getUserProfilePicturePreviewUrl(String ownerId) throws ClientProtocolException, MalformedURLException, IOException, SynapseException {
		return getUrl(getRepoEndpoint(),
				USER_PROFILE_PATH+"/"+ownerId+PROFILE_IMAGE_PREVIEW+"?"+REDIRECT_PARAMETER+"false");
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
		return getUserGroupHeadersByPrefix(prefix, null, null, null);
	}

	@Override
	public UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(
			String prefix, TypeFilter type, Long limit, Long offset) throws SynapseException,
			UnsupportedEncodingException {
		String encodedPrefix = URLEncoder.encode(prefix, "UTF-8");
		StringBuilder builder = new StringBuilder();
		builder.append(USER_GROUP_HEADER_PREFIX_PATH);
		builder.append(encodedPrefix);
		if(limit != null){
			builder.append("&" + LIMIT_PARAMETER + limit);
		}
		if(offset != null){
			builder.append( "&" + OFFSET_PARAMETER + offset);
		}
		if(type != null){
			builder.append("&typeFilter="+type.name());
		}
		return getJSONEntity(getRepoEndpoint(), builder.toString(), UserGroupHeaderResponsePage.class);
	}
	
	@Override
	public List<UserGroupHeader> getUserGroupHeadersByAliases(
			List<String> aliases) throws SynapseException {
		ValidateArgument.required(aliases, "aliases");
		AliasList list = new AliasList();
		list.setList(aliases);
		UserGroupHeaderResponse response = postJSONEntity(getRepoEndpoint(),
				USER_GROUP_HEADER_BY_ALIAS, list, UserGroupHeaderResponse.class);
		return response.getList();
	}

	/**
	 * Update an ACL.
	 */
	@Override
	public AccessControlList updateACL(AccessControlList acl)
			throws SynapseException {
		String uri = ENTITY_URI_PATH + "/" + acl.getId() + ENTITY_ACL_PATH_SUFFIX;
		return putJSONEntity(getRepoEndpoint(), uri, acl, AccessControlList.class);
	}

	@Override
	public void deleteACL(String entityId) throws SynapseException {
		String uri = ENTITY_URI_PATH + "/" + entityId + ENTITY_ACL_PATH_SUFFIX;
		deleteUri(getRepoEndpoint(), uri);
	}

	@Override
	public AccessControlList createACL(AccessControlList acl)
			throws SynapseException {
		String uri = ENTITY_URI_PATH + "/" + acl.getId() + ENTITY_ACL_PATH_SUFFIX;
		return postJSONEntity(getRepoEndpoint(), uri, acl, AccessControlList.class);
	}

	@Override
	public PaginatedResults<UserProfile> getUsers(int offset, int limit)
			throws SynapseException {
		String uri = "/user?" + OFFSET + "=" + offset + "&limit=" + limit;
		return getPaginatedResults(getRepoEndpoint(), uri, UserProfile.class);
	}

	@Override
	public List<UserProfile> listUserProfiles(List<Long> userIds) throws SynapseException {
		IdList idList = new IdList();
		idList.setList(userIds);
		return getListOfJSONEntity(getRepoEndpoint(), USER_PROFILE_PATH, idList, UserProfile.class);
	}

	@Override
	public PaginatedResults<UserGroup> getGroups(int offset, int limit)
			throws SynapseException {
		String uri = "/userGroup?" + OFFSET + "=" + offset + "&limit=" + limit;
		return getPaginatedResults(getRepoEndpoint(), uri, UserGroup.class);
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
		return getJSONEntity(getRepoEndpoint(), url, UserEntityPermissions.class);
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
		ValidateArgument.required(id, "id");
		ValidateArgument.required(type, "ObjectType");
		ValidateArgument.required(accessType, "AccessType");

		switch (type) {
			case ENTITY:
				return canAccess(ENTITY_URI_PATH + "/" + id + "/access?accessType=" + accessType.name());
			case EVALUATION:
				return canAccess(EVALUATION_URI_PATH + "/" + id + "/access?accessType=" + accessType.name());
			default:
				throw new IllegalArgumentException("ObjectType not supported: " + type.toString());
		}
	}

	private boolean canAccess(String serviceUrl) throws SynapseException {
		return getBooleanResult(getRepoEndpoint(), serviceUrl);
	}

	/**
	 * Get the annotations for an entity.
	 *
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public Annotations getAnnotationsV2(String entityId) throws SynapseException {
		String url = ENTITY_URI_PATH + "/" + entityId + ANNOTATIONS_V2;
		return getJSONEntity(getRepoEndpoint(), url, Annotations.class);
	}

	/**
	 * Update the annotations of an entity.
	 *
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public Annotations updateAnnotationsV2(String entityId, Annotations updated)
			throws SynapseException {
		String url = ENTITY_URI_PATH + "/" + entityId + ANNOTATIONS_V2;
		return putJSONEntity(getRepoEndpoint(), url, updated, Annotations.class);
	}


	@SuppressWarnings("unchecked")
	@Override
	public <T extends AccessRequirement> T createAccessRequirement(T ar)
			throws SynapseException {
		ValidateArgument.required(ar, "AccessRequirement");
		ar.setConcreteType(ar.getClass().getName());
		return (T) postJSONEntity(getRepoEndpoint(), ACCESS_REQUIREMENT, ar, ar.getClass());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends AccessRequirement> T updateAccessRequirement(T ar)
			throws SynapseException {
		ValidateArgument.required(ar, "AccessRequirement");
		String url = createEntityUri(ACCESS_REQUIREMENT, ar.getId().toString());
		return (T) putJSONEntity(getRepoEndpoint(), url, ar, ar.getClass());
	}

	@Override
	public LockAccessRequirement createLockAccessRequirement(String entityId)
			throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		return postJSONEntity(getRepoEndpoint(),
				ENTITY + "/" + entityId + LOCK_ACCESS_REQUIREMENT, null,
				LockAccessRequirement.class);
	}

	@Override
	public void deleteAccessRequirement(Long arId) throws SynapseException {
		deleteUri(getRepoEndpoint(), ACCESS_REQUIREMENT + "/" + arId);
	}

	@Override
	public AccessRequirement getAccessRequirement(Long requirementId)
			throws SynapseException {
		String uri = ACCESS_REQUIREMENT + "/" + requirementId;
		return getJSONEntity(getRepoEndpoint(), uri, AccessRequirement.class);
	}

	@Override
	public PaginatedResults<AccessRequirement> getAccessRequirements(
			RestrictableObjectDescriptor subjectId, Long limit, Long offset) throws SynapseException {
		String uri = null;
		switch (subjectId.getType()){
			case ENTITY:
				uri = ENTITY + "/" + subjectId.getId() + ACCESS_REQUIREMENT;
				break;
			case EVALUATION:
				uri = EVALUATION_URI_PATH + "/" + subjectId.getId() + ACCESS_REQUIREMENT;
				break;
			case TEAM:
				uri = TEAM + "/" + subjectId.getId() + ACCESS_REQUIREMENT;
				break;
			default:
				throw new SynapseClientException("Unsupported type "+ subjectId.getType());
		}
		uri += "?limit="+limit+"&offset="+offset;
		return getPaginatedResults(getRepoEndpoint(), uri, AccessRequirement.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends AccessApproval> T createAccessApproval(T aa) throws SynapseException {
		ValidateArgument.required(aa, "AccessApproval");
		return (T) postJSONEntity(getRepoEndpoint(), ACCESS_APPROVAL, aa, aa.getClass());
	}

	@Override
	public AccessApproval getAccessApproval(Long approvalId)
			throws SynapseException {
		String uri = ACCESS_APPROVAL + "/" + approvalId;
		return getJSONEntity(getRepoEndpoint(), uri, AccessApproval.class);
	}

	@Override
	public void revokeAccessApprovals(String requirementId, String accessorId) throws SynapseException {
		String url = ACCESS_APPROVAL + "?requirementId=" + requirementId + "&accessorId=" + accessorId;
		deleteUri(getRepoEndpoint(), url);
	}

	@Override
	public JSONObject getEntity(String uri) throws SynapseException {
		return getJson(getRepoEndpoint(), uri);
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
	public <T extends JSONEntity> T getEntity(String entityId,
			Class<? extends T> clazz) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(clazz, "Entity class");
		String uri = createEntityUri(ENTITY_URI_PATH, entityId);
		return getJSONEntity(getRepoEndpoint(), uri, clazz);
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
		return putEntity(entity, null, null);
	}

	/**
	 * Update a dataset, layer, preview, annotations, etc...
	 * 
	 * @param <T>
	 * @param entity
	 * @param activityId
	 *            activity to create generatedBy conenction to
	 * @param newVersion
	 * 			  force update a FileEntity to a new version. May be null.
	 * @return the updated entity
	 * @throws SynapseException
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T extends Entity> T putEntity(T entity, String activityId, Boolean newVersion)
			throws SynapseException {
		ValidateArgument.required(entity, "entity");
		URIBuilder uri = new URIBuilder();
		uri.setPath(createEntityUri(ENTITY_URI_PATH, entity.getId()));
		if (activityId != null) {
			uri.setParameter(PARAM_GENERATED_BY, activityId);
		}
		if (newVersion != null) {
			uri.setParameter(PARAM_NEW_VERSION, newVersion.toString());
		}
		return (T) putJSONEntity(getRepoEndpoint(), uri.toString(), entity, entity.getClass());
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
		ValidateArgument.required(entity, "entity");
		deleteEntityById(entity.getId(), skipTrashCan);
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
		ValidateArgument.required(entityId, "entityId");
		String uri = createEntityUri(ENTITY_URI_PATH, entityId);
		if (skipTrashCan != null && skipTrashCan) {
			uri = uri + "?" + ServiceConstants.SKIP_TRASH_CAN_PARAM + "=true";
		}
		deleteUri(getRepoEndpoint(), uri);
	}

	@Override
	public <T extends Entity> void deleteEntityVersion(T entity,
			Long versionNumber) throws SynapseException {
		ValidateArgument.required(entity, "entity");
		deleteEntityVersionById(entity.getId(), versionNumber);
	}

	@Override
	public void deleteEntityVersionById(String entityId, Long versionNumber)
			throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(versionNumber, "versionNumber");
		String uri = createEntityUri(ENTITY_URI_PATH, entityId);
		uri += REPO_SUFFIX_VERSION + "/" + versionNumber;
		deleteUri(getRepoEndpoint(), uri);
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
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public EntityPath getEntityPath(String entityId) throws SynapseException {
		String url = ENTITY_URI_PATH + "/" + entityId + "/path";
		return getJSONEntity(getRepoEndpoint(), url, EntityPath.class);
	}

	@Deprecated
	/* wrong use of PaginatedResults since expected returned data is not a page */
	@Override
	public PaginatedResults<EntityHeader> getEntityTypeBatch(List<String> entityIds)
			throws SynapseException {
		String url = ENTITY_URI_PATH + "/type";
		url += "?"
				+ ServiceConstants.BATCH_PARAM
				+ "="
				+ StringUtils.join(entityIds,
						ServiceConstants.BATCH_PARAM_VALUE_SEPARATOR);
		return getPaginatedResults(getRepoEndpoint(), url, EntityHeader.class);
	}

	@Deprecated
	/* wrong use of PaginatedResults since expected returned data is not a page */
	@Override
	public PaginatedResults<EntityHeader> getEntityHeaderBatch(
			List<Reference> references) throws SynapseException {
		ReferenceList list = new ReferenceList();
		list.setReferences(references);
		String url = ENTITY_URI_PATH + "/header";
		return getPaginatedResults(getRepoEndpoint(), url, list, EntityHeader.class);
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
		String uri = getFileEndpoint() + getFileHandleTemporaryURI(fileHandleId, true);
		downloadFromSynapse(uri, null, destinationFile);
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
	public String putFileToURL(URL url, File file, String contentType) throws SynapseException {
		return super.putFileToURL(url, file, contentType);
	}

	/**
	 * Create an External File Handle. This is used to references a file that is
	 * not stored in Synapse.
	 * 
	 * @param efh
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public ExternalFileHandle createExternalFileHandle(ExternalFileHandle efh)
			throws SynapseException {
		return postJSONEntity(getFileEndpoint(), EXTERNAL_FILE_HANDLE, efh, ExternalFileHandle.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.client.SynapseClient#createExternalS3FileHandle(org.sagebionetworks.repo.model.file.S3FileHandle)
	 */
	@Override
	public S3FileHandle createExternalS3FileHandle(S3FileHandle handle) throws SynapseException{
		return postJSONEntity(getFileEndpoint(), EXTERNAL_FILE_HANDLE_S3, handle, S3FileHandle.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.client.SynapseClient#createExternalGoogleCloudFileHandle(org.sagebionetworks.repo.model.file.GoogleCloudFileHandle)
	 */
	@Override
	public GoogleCloudFileHandle createExternalGoogleCloudFileHandle(GoogleCloudFileHandle handle) throws SynapseException{
		return postJSONEntity(getFileEndpoint(), EXTERNAL_FILE_HANDLE_GOOGLE_CLOUD, handle, GoogleCloudFileHandle.class);
	}

	@Override
	public ProxyFileHandle createExternalProxyFileHandle(ProxyFileHandle handle) throws SynapseException{
		return postJSONEntity(getFileEndpoint(), EXTERNAL_FILE_HANDLE, handle, ProxyFileHandle.class);
	}

	@Override
	public ExternalObjectStoreFileHandle createExternalObjectStoreFileHandle(ExternalObjectStoreFileHandle handle) throws SynapseException{
		return postJSONEntity(getFileEndpoint(), EXTERNAL_FILE_HANDLE, handle, ExternalObjectStoreFileHandle.class);
	}

	@Override
	public S3FileHandle createS3FileHandleCopy(String originalFileHandleId, String name, String contentType)
			throws SynapseException {
		String uri = FILE_HANDLE + "/" + originalFileHandleId + "/copy";
		S3FileHandle changes = new S3FileHandle();
		changes.setFileName(name);
		changes.setContentType(contentType);
		return postJSONEntity(getFileEndpoint(), uri, changes, S3FileHandle.class);
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
	 * Get the raw file handle. Note: Only the creator of a the file handle can
	 * get the raw file handle.
	 * 
	 * @param fileHandleId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public FileHandle getRawFileHandle(String fileHandleId) throws SynapseException {
		return getJSONEntity(getFileEndpoint(), FILE_HANDLE + "/" + fileHandleId, FileHandle.class);
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
		deleteUri(getFileEndpoint(), FILE_HANDLE + "/" + fileHandleId);
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
		deleteUri(getFileEndpoint(), FILE_HANDLE + "/" + fileHandleId + FILE_PREVIEW);
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
	 */
	@Override
	public WikiPage createWikiPage(String ownerId, ObjectType ownerType,
			WikiPage toCreate) throws SynapseException {
		ValidateArgument.required(ownerId, "ownerId");
		ValidateArgument.required(ownerType, "ownerType");
		ValidateArgument.required(toCreate, "WikiPage");
		String uri = createWikiURL(ownerId, ownerType);
		return postJSONEntity(getRepoEndpoint(), uri, toCreate, WikiPage.class);
	}

	/**
	 * Helper to create a wiki URL that does not include the wiki id.
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @return
	 */
	private String createWikiURL(String ownerId, ObjectType ownerType) {
		ValidateArgument.required(ownerId, "ownerId");
		ValidateArgument.required(ownerType, "ownerType");
		return String.format(WIKI_URI_TEMPLATE, ownerType.name().toLowerCase(), ownerId);
	}

	/**
	 * Get a WikiPage using its key
	 * 
	 * @param key
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public WikiPage getWikiPage(WikiPageKey key) throws SynapseException {
		ValidateArgument.required(key, "key");
		String uri = createWikiURL(key);
		return getJSONEntity(getRepoEndpoint(), uri, WikiPage.class);
	}
	

	@Override
	public WikiPage getWikiPageForVersion(WikiPageKey key,
			Long version) throws SynapseException {
		String uri = createWikiURL(key) + VERSION_PARAMETER + version;
		return getJSONEntity(getRepoEndpoint(), uri, WikiPage.class);
	}

	@Override
	public WikiPageKey getRootWikiPageKey(String ownerId, ObjectType ownerType) throws SynapseException {
		ValidateArgument.required(ownerId, "ownerId");
		ValidateArgument.required(ownerType, "ownerType");
		String uri = createWikiURL(ownerId, ownerType)+"key";
		return getJSONEntity(getRepoEndpoint(), uri, WikiPageKey.class);
	}

	/**
	 * Get a the root WikiPage for a given owner.
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public WikiPage getRootWikiPage(String ownerId, ObjectType ownerType)
			throws SynapseException {
		ValidateArgument.required(ownerId, "ownerId");
		ValidateArgument.required(ownerType, "ownerType");
		String uri = createWikiURL(ownerId, ownerType);
		return getJSONEntity(getRepoEndpoint(), uri, WikiPage.class);
	}

	/**
	 * Get all of the FileHandles associated with a WikiPage, including any
	 * PreviewHandles.
	 * 
	 * @param key
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public FileHandleResults getWikiAttachmenthHandles(WikiPageKey key)
			throws SynapseException {
		ValidateArgument.required(key, "key");
		String uri = createWikiURL(key) + ATTACHMENT_HANDLES;
		return getJSONEntity(getRepoEndpoint(), uri, FileHandleResults.class);
	}

	private static String createWikiAttachmentURI(WikiPageKey key,
			String fileName, boolean redirect) throws SynapseClientException {
		ValidateArgument.required(key, "key");
		ValidateArgument.required(fileName, "fileName");
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
		return getUrl(getRepoEndpoint(),
				createWikiAttachmentURI(key, fileName, false));
	}

	@Override
	public void downloadWikiAttachment(WikiPageKey key, String fileName,
			File target) throws SynapseException {
		String uri = createWikiAttachmentURI(key, fileName, true);
		downloadFromSynapse(getRepoEndpoint() + uri, null, target);
	}

	private static String createWikiAttachmentPreviewURI(WikiPageKey key,
			String fileName, boolean redirect) throws SynapseClientException {
		ValidateArgument.required(key, "key");
		ValidateArgument.required(fileName, "fileName");
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
		return getUrl(getRepoEndpoint(),
				createWikiAttachmentPreviewURI(key, fileName, false));
	}

	@Override
	public void downloadWikiAttachmentPreview(WikiPageKey key, String fileName,
			File target) throws SynapseException {
		String uri = createWikiAttachmentPreviewURI(key, fileName, true);
		downloadFromSynapse(getRepoEndpoint() + uri, null, target);

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
		return getUrl(getRepoEndpoint(),
				ENTITY + "/" + entityId + FILE + QUERY_REDIRECT_PARAMETER + "false");
	}

	@Deprecated
	@Override
	public void downloadFromFileEntityCurrentVersion(String fileEntityId,
			File destinationFile) throws SynapseException {
		String uri = ENTITY + "/" + fileEntityId + FILE;
		downloadFromSynapse(getRepoEndpoint() + uri, null, destinationFile);
	}

	@Deprecated
	@Override
	public void downloadFromFileEntityForVersion(String entityId,
			Long versionNumber, File destinationFile) throws SynapseException {
		String uri = ENTITY + "/" + entityId + VERSION_INFO + "/"
				+ versionNumber + FILE;
		downloadFromSynapse(getRepoEndpoint() + uri, null, destinationFile);
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
		return getUrl(getRepoEndpoint(), uri);
	}

	@Override
	public void downloadFromFileEntityPreviewCurrentVersion(
			String fileEntityId, File destinationFile) throws SynapseException {
		String uri = ENTITY + "/" + fileEntityId + FILE_PREVIEW;
		downloadFromSynapse(getRepoEndpoint() + uri, null, destinationFile);
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
		return getUrl(getRepoEndpoint(), uri);
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
		return getUrl(getRepoEndpoint(), uri);
	}

	@Override
	public void downloadFromFileEntityPreviewForVersion(String entityId,
			Long versionNumber, File destinationFile) throws SynapseException {
		String uri = ENTITY + "/" + entityId + VERSION_INFO + "/"
				+ versionNumber + FILE_PREVIEW;
		downloadFromSynapse(getRepoEndpoint() + uri, null, destinationFile);
	}

	/**
	 * Update a WikiPage
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @param toUpdate
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public WikiPage updateWikiPage(String ownerId, ObjectType ownerType,
			WikiPage toUpdate) throws SynapseException {
		ValidateArgument.required(ownerId, "ownerId");
		ValidateArgument.required(ownerType, "ownerType");
		ValidateArgument.required(toUpdate, "WikiPage");
		ValidateArgument.required(toUpdate.getId(), "WikiPage Id");
		String uri = String.format(WIKI_ID_URI_TEMPLATE, ownerType.name()
				.toLowerCase(), ownerId, toUpdate.getId());
		return putJSONEntity(getRepoEndpoint(), uri, toUpdate, WikiPage.class);
	}

	/**
	 * Delete a WikiPage
	 * 
	 * @param key
	 * @throws SynapseException
	 */
	@Override
	public void deleteWikiPage(WikiPageKey key) throws SynapseException {
		ValidateArgument.required(key, "key");
		String uri = createWikiURL(key);
		deleteUri(getRepoEndpoint(), uri);
	}

	/**
	 * Helper to build a URL for a wiki page.
	 * 
	 * @param key
	 * @return
	 */
	private static String createWikiURL(WikiPageKey key) {
		ValidateArgument.required(key, "key");
		return String.format(WIKI_ID_URI_TEMPLATE, key.getOwnerObjectType()
				.name().toLowerCase(), key.getOwnerObjectId(),
				key.getWikiPageId());
	}

	/**
	 * Get the file handles for the current version of an entity.
	 * 
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public FileHandleResults getEntityFileHandlesForCurrentVersion(
			String entityId) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		String uri = ENTITY_URI_PATH + "/" + entityId + FILE_HANDLES;
		return getJSONEntity(getRepoEndpoint(), uri, FileHandleResults.class);
	}

	/**
	 * Get the file hanldes for a given version of an entity.
	 * 
	 * @param entityId
	 * @param versionNumber
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public FileHandleResults getEntityFileHandlesForVersion(String entityId,
			Long versionNumber) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		String uri = ENTITY_URI_PATH + "/" + entityId + "/version/"
				+ versionNumber + FILE_HANDLES;
		return getJSONEntity(getRepoEndpoint(), uri, FileHandleResults.class);
	}

	// V2 WIKIPAGE METHODS

	/**
	 * Helper to create a V2 Wiki URL (No ID)
	 */
	private String createV2WikiURL(String ownerId, ObjectType ownerType) {
		ValidateArgument.required(ownerId, "ownerId");
		ValidateArgument.required(ownerType, "ownerType");
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
		ValidateArgument.required(key, "key");
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
	 */
	@Override
	public V2WikiPage createV2WikiPage(String ownerId, ObjectType ownerType,
			V2WikiPage toCreate) throws SynapseException {
		ValidateArgument.required(ownerId, "ownerId");
		ValidateArgument.required(ownerType, "ownerType");
		ValidateArgument.required(toCreate, "WikiPage");
		String uri = createV2WikiURL(ownerId, ownerType);
		return postJSONEntity(getRepoEndpoint(), uri, toCreate, V2WikiPage.class);
	}

	/**
	 * Get a V2 WikiPage using its key
	 * 
	 * @param key
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public V2WikiPage getV2WikiPage(WikiPageKey key)
			throws SynapseException {
		ValidateArgument.required(key, "key");
		String uri = createV2WikiURL(key);
		return getJSONEntity(getRepoEndpoint(), uri, V2WikiPage.class);
	}

	/**
	 * Get a version of a V2 WikiPage using its key and version number
	 */
	@Override
	public V2WikiPage getVersionOfV2WikiPage(WikiPageKey key, Long version)
			throws SynapseException {
		ValidateArgument.required(key, "key");
		ValidateArgument.required(version, "version");

		String uri = createV2WikiURL(key) + VERSION_PARAMETER + version;
		return getJSONEntity(getRepoEndpoint(), uri, V2WikiPage.class);
	}

	/**
	 * Get a the root V2 WikiPage for a given owner.
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public V2WikiPage getV2RootWikiPage(String ownerId, ObjectType ownerType)
			throws SynapseException {
		ValidateArgument.required(ownerId, "ownerId");
		ValidateArgument.required(ownerType, "ownerType");
		String uri = createV2WikiURL(ownerId, ownerType);
		return getJSONEntity(getRepoEndpoint(), uri, V2WikiPage.class);
	}

	/**
	 * Update a V2 WikiPage
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @param toUpdate
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public V2WikiPage updateV2WikiPage(String ownerId, ObjectType ownerType,
			V2WikiPage toUpdate) throws SynapseException {
		ValidateArgument.required(ownerId, "ownerId");
		ValidateArgument.required(ownerType, "ownerType");
		ValidateArgument.required(toUpdate, "WikiPage");
		ValidateArgument.required(toUpdate.getId(), "WikiPage Id");
		String uri = String.format(WIKI_ID_URI_TEMPLATE_V2, ownerType.name()
				.toLowerCase(), ownerId, toUpdate.getId());
		return putJSONEntity(getRepoEndpoint(), uri, toUpdate, V2WikiPage.class);
	}

	@Override
	public V2WikiOrderHint updateV2WikiOrderHint(V2WikiOrderHint toUpdate)
			throws SynapseException {
		ValidateArgument.required(toUpdate, "toUpdate");
		ValidateArgument.required(toUpdate.getOwnerId(), "V2WikiOrderHint.ownerId");
		ValidateArgument.required(toUpdate.getOwnerObjectType(), "V2WikiOrderHint.ownerObjectType");
		String uri = String.format(WIKI_ORDER_HINT_URI_TEMPLATE_V2, toUpdate
				.getOwnerObjectType().name().toLowerCase(),
				toUpdate.getOwnerId());
		return putJSONEntity(getRepoEndpoint(), uri, toUpdate, V2WikiOrderHint.class);
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
	 * @throws SynapseException
	 */
	@Override
	public V2WikiPage restoreV2WikiPage(String ownerId, ObjectType ownerType,
			String wikiId, Long versionToRestore)
			throws SynapseException {
		ValidateArgument.required(ownerId, "ownerId");
		ValidateArgument.required(ownerType, "ownerType");
		ValidateArgument.required(wikiId, "wikiId");
		ValidateArgument.required(versionToRestore, "versionToRestore");
		String uri = String.format(WIKI_ID_VERSION_URI_TEMPLATE_V2, ownerType
				.name().toLowerCase(), ownerId, wikiId, String
				.valueOf(versionToRestore));
		return putJSONEntity(getRepoEndpoint(), uri, null, V2WikiPage.class);
	}

	/**
	 * Get all of the FileHandles associated with a V2 WikiPage, including any
	 * PreviewHandles.
	 * 
	 * @param key
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public FileHandleResults getV2WikiAttachmentHandles(WikiPageKey key)
			throws SynapseException {
		ValidateArgument.required(key, "key");
		String uri = createV2WikiURL(key) + ATTACHMENT_HANDLES;
		return getJSONEntity(getRepoEndpoint(), uri, FileHandleResults.class);
	}

	@Override
	public FileHandleResults getVersionOfV2WikiAttachmentHandles(
			WikiPageKey key, Long version) throws SynapseException {
		ValidateArgument.required(key, "key");
		ValidateArgument.required(version, "version");
		String uri = createV2WikiURL(key) + ATTACHMENT_HANDLES
				+ VERSION_PARAMETER + version;
		return getJSONEntity(getRepoEndpoint(), uri, FileHandleResults.class);
	}

	@Override
	public String downloadV2WikiMarkdown(WikiPageKey key)
			throws ClientProtocolException, FileNotFoundException, IOException,
			SynapseException {
		ValidateArgument.required(key, "key");
		String uri = createV2WikiURL(key) + MARKDOWN_FILE;
		return downloadZippedFileToString(getRepoEndpoint(), uri);
	}

	@Override
	public String downloadVersionOfV2WikiMarkdown(WikiPageKey key, Long version)
			throws ClientProtocolException, FileNotFoundException, IOException,
			SynapseException {

		ValidateArgument.required(key, "key");
		ValidateArgument.required(version, "version");
		String uri = createV2WikiURL(key) + MARKDOWN_FILE + VERSION_PARAMETER
				+ version;
		return downloadZippedFileToString(getRepoEndpoint(), uri);
	}

	private static String createV2WikiAttachmentURI(WikiPageKey key,
			String fileName, boolean redirect) throws SynapseClientException {
		ValidateArgument.required(key, "key");
		ValidateArgument.required(fileName, "fileName");
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
		return getUrl(getRepoEndpoint(), createV2WikiAttachmentURI(key, fileName, false));
	}

	@Override
	public void downloadV2WikiAttachment(WikiPageKey key, String fileName,
			File target) throws SynapseException {
		String uri = createV2WikiAttachmentURI(key, fileName, true);
		downloadFromSynapse(getRepoEndpoint() + uri, null, target);
	}

	private static String createV2WikiAttachmentPreviewURI(WikiPageKey key,
			String fileName, boolean redirect) throws SynapseClientException {
		ValidateArgument.required(key, "key");
		ValidateArgument.required(fileName, "fileName");
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
		return getUrl(getRepoEndpoint(), createV2WikiAttachmentPreviewURI(key, fileName, false));
	}

	@Override
	public void downloadV2WikiAttachmentPreview(WikiPageKey key,
			String fileName, File target) throws SynapseException {
		String uri = createV2WikiAttachmentPreviewURI(key, fileName, true);
		downloadFromSynapse(getRepoEndpoint() + uri, null, target);
	}

	private static String createVersionOfV2WikiAttachmentPreviewURI(
			WikiPageKey key, String fileName, Long version, boolean redirect)
			throws SynapseClientException {
		ValidateArgument.required(key, "key");
		ValidateArgument.required(fileName, "fileName");
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
		return getUrl(getRepoEndpoint(),
				createVersionOfV2WikiAttachmentPreviewURI(key, fileName, version, false));
	}

	@Override
	public void downloadVersionOfV2WikiAttachmentPreview(WikiPageKey key,
			String fileName, Long version, File target) throws SynapseException {
		String uri = createVersionOfV2WikiAttachmentPreviewURI(key, fileName, version, true);
		downloadFromSynapse(getRepoEndpoint() + uri, null, target);
	}

	private static String createVersionOfV2WikiAttachmentURI(WikiPageKey key,
			String fileName, Long version, boolean redirect)
			throws SynapseClientException {
		ValidateArgument.required(key, "key");
		ValidateArgument.required(fileName, "fileName");
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
		return getUrl(getRepoEndpoint(),
				createVersionOfV2WikiAttachmentURI(key, fileName, version, false));
	}

	// alternative to getVersionOfV2WikiAttachmentTemporaryUrl
	@Override
	public void downloadVersionOfV2WikiAttachment(WikiPageKey key,
			String fileName, Long version, File target) throws SynapseException {
		String uri = createVersionOfV2WikiAttachmentURI(key, fileName, version, true);
		downloadFromSynapse(getRepoEndpoint() + uri, null, target);
	}

	/**
	 * Delete a V2 WikiPage
	 * 
	 * @param key
	 * @throws SynapseException
	 */
	@Override
	public void deleteV2WikiPage(WikiPageKey key) throws SynapseException {
		ValidateArgument.required(key, "key");
		String uri = createV2WikiURL(key);
		deleteUri(getRepoEndpoint(), uri);
	}
	
	/**
	 * Get the WikiHeader tree for a given owner object.
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public PaginatedResults<V2WikiHeader> getV2WikiHeaderTree(String ownerId,
			ObjectType ownerType, Long limit, Long offset) throws SynapseException {
		ValidateArgument.required(ownerId, "ownerId");
		ValidateArgument.required(ownerType, "ownerType");
		String uri = String.format(WIKI_TREE_URI_TEMPLATE_V2, ownerType.name()
				.toLowerCase(), ownerId) + "?offset" + "=" + offset + "&limit="
				+ limit;
		return getPaginatedResults(getRepoEndpoint(), uri, V2WikiHeader.class);
	}

	@Override
	public V2WikiOrderHint getV2OrderHint(WikiPageKey key)
			throws SynapseException {
		ValidateArgument.required(key, "key");

		String uri = String.format(WIKI_ORDER_HINT_URI_TEMPLATE_V2, key
				.getOwnerObjectType().name().toLowerCase(),
				key.getOwnerObjectId());
		return getJSONEntity(getRepoEndpoint(), uri, V2WikiOrderHint.class);
	}

	/**
	 * Get the tree of snapshots (outlining each modification) for a particular
	 * V2 WikiPage
	 * 
	 * @param key
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public PaginatedResults<V2WikiHistorySnapshot> getV2WikiHistory(
			WikiPageKey key, Long limit, Long offset) throws SynapseException {
		ValidateArgument.required(key, "key");
		String uri = createV2WikiURL(key) + WIKI_HISTORY_V2 + "?"
				+ OFFSET_PARAMETER + offset + AND_LIMIT_PARAMETER + limit;
		return getPaginatedResults(getRepoEndpoint(), uri, V2WikiHistorySnapshot.class);
	}

	/******************** Low Level APIs ********************/

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.client.SynapseClient#startAsynchJob(org.sagebionetworks.client.AsynchJobType, org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody)
	 */
	public String startAsynchJob(AsynchJobType type, AsynchronousRequestBody request)
			throws SynapseException {
		String url = type.getStartUrl(request);
		String endpoint = getEndpointForType(type.getRestEndpoint());
		AsyncJobId jobId = postJSONEntity(endpoint, url, request, AsyncJobId.class);
		return jobId.getToken();
	}

	@Override
	public void cancelAsynchJob(String jobId) throws SynapseException {
		String url = ASYNCHRONOUS_JOB + "/" + jobId + "/cancel";
		getJson(getRepoEndpoint(), url);
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
	
	@Override
	public AsynchronousResponseBody getAsyncResult(AsynchJobType type, String jobId)
			throws SynapseException, SynapseClientException, SynapseResultNotReadyException {
		return getAsyncResult(type, jobId, (String) null);
	}

	/**
	 * Get a job response body for a url.
	 * @param url
	 * @param clazz
	 * @return
	 * @throws SynapseException
	 */
	private AsynchronousResponseBody getAsynchJobResponse(String url, Class<? extends AsynchronousResponseBody> clazz, String endpoint) throws SynapseException {
		JSONObject jsonObject = getJson(endpoint, url);
		try {
			return EntityFactory.createEntityFromJSONObject(jsonObject, AsynchronousResponseBody.class);
		} catch (JSONObjectAdapterException e) {
			try {
				AsynchronousJobStatus status = EntityFactory.createEntityFromJSONObject(jsonObject, AsynchronousJobStatus.class);
				throw new SynapseResultNotReadyException(status);
			} catch (JSONObjectAdapterException e2) {
				throw new SynapseClientException(e2.getMessage(), e2);
			}
		}
	}

	/**
	 * @return status
	 * @throws SynapseException
	 */
	@Override
	public StackStatus getCurrentStackStatus() throws SynapseException{
		return getJSONEntity(getRepoEndpoint(), STACK_STATUS, StackStatus.class);
	}

	@Override
	public SearchResults search(SearchQuery searchQuery) throws SynapseException {
		return postJSONEntity(getRepoEndpoint(), "/search", searchQuery, SearchResults.class);
	}

	/**
	 * Helper for pagination of messages
	 */
	private String setMessageParameters(String path,
			List<MessageStatusType> inboxFilter, MessageSortBy orderBy,
			Boolean descending, Long limit, Long offset) {
		ValidateArgument.required(path, "path");
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
		return postJSONEntity(getRepoEndpoint(), MESSAGE, message, MessageToUser.class);
	}

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
		message.setFileHandleId(uploadStringToS3(messageBody));
		return sendMessage(message);
	}

	private String uploadStringToS3(String toUpload) throws SynapseException {
		try {
			byte[] messageByteArray = toUpload.getBytes("UTF-8");
			return multipartUpload(new ByteArrayInputStream(messageByteArray),
					(long) messageByteArray.length, "content", "text/plain; charset=UTF-8", null, false, false).getId();
		} catch (UnsupportedEncodingException e) {
			throw new SynapseClientException(e);
		}
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
		message.setFileHandleId(uploadStringToS3(messageBody));
		return sendMessage(message, entityId);
	}

	@Override
	public MessageToUser sendMessage(MessageToUser message, String entityId)
			throws SynapseException {
		String uri = ENTITY + "/" + entityId + "/" + MESSAGE;
		return postJSONEntity(getRepoEndpoint(), uri, message, MessageToUser.class);
	}

	@Override
	public PaginatedResults<MessageBundle> getInbox(
			List<MessageStatusType> inboxFilter, MessageSortBy orderBy,
			Boolean descending, long limit, long offset)
			throws SynapseException {
		String uri = setMessageParameters(MESSAGE_INBOX, inboxFilter, orderBy,
				descending, limit, offset);
		return getPaginatedResults(getRepoEndpoint(), uri, MessageBundle.class);
	}

	@Override
	public PaginatedResults<MessageToUser> getOutbox(MessageSortBy orderBy,
			Boolean descending, long limit, long offset)
			throws SynapseException {
		String uri = setMessageParameters(MESSAGE_OUTBOX, null, orderBy,
				descending, limit, offset);
		return getPaginatedResults(getRepoEndpoint(), uri, MessageToUser.class);
	}

	@Override
	public MessageToUser getMessage(String messageId) throws SynapseException {
		String uri = MESSAGE + "/" + messageId;
		return getJSONEntity(getRepoEndpoint(), uri, MessageToUser.class);
	}

	@Override
	public MessageToUser forwardMessage(String messageId,
			MessageRecipientSet recipients) throws SynapseException {
		String uri = MESSAGE + "/" + messageId + FORWARD;
		return postJSONEntity(getRepoEndpoint(), uri, recipients, MessageToUser.class);
	}

	@Override
	public PaginatedResults<MessageToUser> getConversation(
			String associatedMessageId, MessageSortBy orderBy,
			Boolean descending, long limit, long offset)
			throws SynapseException {
		String uri = setMessageParameters(MESSAGE + "/" + associatedMessageId
				+ CONVERSATION, null, orderBy, descending, limit, offset);
		return getPaginatedResults(getRepoEndpoint(), uri, MessageToUser.class);
	}

	@Override
	public void updateMessageStatus(MessageStatus status) throws SynapseException {
		voidPut(getRepoEndpoint(), MESSAGE_STATUS, status);
	}

	private static String createDownloadMessageURI(String messageId, boolean redirect) {
		return MESSAGE + "/" + messageId + FILE + "?" + REDIRECT_PARAMETER + redirect;
	}

	@Override
	public String getMessageTemporaryUrl(String messageId)
			throws SynapseException, MalformedURLException, IOException {
		String uri = createDownloadMessageURI(messageId, false);
		return getStringDirect(getRepoEndpoint(), uri);
	}

	@Override
	public String downloadMessage(String messageId) throws SynapseException,
			MalformedURLException, IOException {
		String uri = createDownloadMessageURI(messageId, true);
		return getStringDirect(getRepoEndpoint(), uri);
	}

	@Override
	public void downloadMessageToFile(String messageId, File target)
			throws SynapseException {
		String uri = createDownloadMessageURI(messageId, true);
		downloadFromSynapse(getRepoEndpoint() + uri, null, target);
	}

	/**
	 * Get the appropriate piece of the URL based on the attachment type
	 * 
	 * @param type
	 * @return
	 */
	public static String getAttachmentTypeURL(ServiceConstants.AttachmentType type) {
		switch (type) {
			case ENTITY:
				return ENTITY;
			case USER_PROFILE:
				return USER_PROFILE_PATH;
			default:
				throw new IllegalArgumentException("Unrecognized attachment type: " + type);
		}
	}

	/**
	 * @return version
	 * @throws SynapseException
	 */
	@Override
	public SynapseVersionInfo getVersionInfo() throws SynapseException {
		return getJSONEntity(getRepoEndpoint(), VERSION_INFO, SynapseVersionInfo.class);
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
		ValidateArgument.required(entityId, "entityId");
		String url = createEntityUri(ENTITY_URI_PATH, entityId);
		if (versionNumber != null) {
			url += REPO_SUFFIX_VERSION + "/" + versionNumber;
		}
		url += GENERATED_BY_SUFFIX;
		return getJSONEntity(getRepoEndpoint(), url, Activity.class);
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
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(activityId, "activityId");
		String url = createEntityUri(ENTITY_URI_PATH, entityId) + GENERATED_BY_SUFFIX;
		if (activityId != null) {
			url += "?" + PARAM_GENERATED_BY + "=" + activityId;
		}
		return putJSONEntity(getRepoEndpoint(), url, null, Activity.class);
	}

	/**
	 * Delete the generatedBy relationship for an Entity (does not delete the
	 * activity)
	 * 
	 * @param entityId
	 * @throws SynapseException
	 */
	@Override
	public void deleteGeneratedByForEntity(String entityId) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		String uri = createEntityUri(ENTITY_URI_PATH, entityId) + GENERATED_BY_SUFFIX;
		deleteUri(getRepoEndpoint(), uri);
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
		ValidateArgument.required(activity, "activity");
		return postJSONEntity(getRepoEndpoint(), ACTIVITY_URI_PATH, activity, Activity.class);
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
		ValidateArgument.required(activityId, "Activity ID");
		String url = createEntityUri(ACTIVITY_URI_PATH, activityId);
		return getJSONEntity(getRepoEndpoint(), url, Activity.class);
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
		ValidateArgument.required(activity, "Activity");
		String url = createEntityUri(ACTIVITY_URI_PATH, activity.getId());
		return putJSONEntity(getRepoEndpoint(), url, activity, Activity.class);
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
		ValidateArgument.required(activityId, "activityId");
		String uri = createEntityUri(ACTIVITY_URI_PATH, activityId);
		deleteUri(getRepoEndpoint(), uri);
	}

	@Override
	public PaginatedResults<Reference> getEntitiesGeneratedBy(
			String activityId, Integer limit, Integer offset)
			throws SynapseException {
		ValidateArgument.required(activityId, "activityId");
		String url = createEntityUri(ACTIVITY_URI_PATH, activityId
				+ GENERATED_PATH + "?" + OFFSET + "=" + offset + "&limit="
				+ limit);
		return getPaginatedResults(getRepoEndpoint(), url, Reference.class);
	}


	@Override
	public Evaluation createEvaluation(Evaluation eval) throws SynapseException {
		return postJSONEntity(getRepoEndpoint(), EVALUATION_URI_PATH, eval, Evaluation.class);
	}

	@Override
	public Evaluation getEvaluation(String evalId) throws SynapseException {
		ValidateArgument.required(evalId, "Evaluation ID");
		String url = createEntityUri(EVALUATION_URI_PATH, evalId);
		return getJSONEntity(getRepoEndpoint(), url, Evaluation.class);
	}
	
	@Override
	public PaginatedResults<Evaluation> getEvaluationByContentSource(String id,
			int offset, int limit) throws SynapseException {
		return getEvaluationByContentSource(id, null, offset, limit);
	}

	@Override
	public PaginatedResults<Evaluation> getEvaluationByContentSource(String id, ACCESS_TYPE accessType,
			int offset, int limit) throws SynapseException {
		return getEvaluationByContentSource(id, accessType, false, null, offset, limit);
	}
	
	@Override
	public PaginatedResults<Evaluation> getEvaluationByContentSource(String id, ACCESS_TYPE accessType, 
			boolean activeOnly, List<Long> evaluationIds,
			int offset, int limit) throws SynapseException {
		
		String url = ENTITY_URI_PATH + "/" + id + EVALUATION_URI_PATH + "?"
				+ OFFSET + "=" + offset + "&limit=" + limit + "&activeOnly="+activeOnly;
		
		if (accessType != null) {
			url += "&accessType=" + accessType.name();
		}
		
		if (evaluationIds != null && !evaluationIds.isEmpty()) {
			
			String evaluationIdsString = evaluationIds.stream()
					.map(Object::toString)
					.collect(Collectors.joining(","));
			
			url += "&evaluationIds=" + evaluationIdsString;
		}
		
		return getPaginatedResults(getRepoEndpoint(), url, Evaluation.class);
	}

	@Override
	public PaginatedResults<Evaluation> getAvailableEvaluationsPaginated(
			int offset, int limit) throws SynapseException {
		String url = AVAILABLE_EVALUATION_URI_PATH + "?" + OFFSET + "="
				+ offset + "&limit=" + limit;
		return getPaginatedResults(getRepoEndpoint(), url, Evaluation.class);
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
		return getPaginatedResults(getRepoEndpoint(), url, Evaluation.class);
	}

	@Override
	public Evaluation findEvaluation(String name) throws SynapseException,
			UnsupportedEncodingException {
		ValidateArgument.required(name, "Evaluation name");
		String encodedName = URLEncoder.encode(name, "UTF-8");
		String url = EVALUATION_URI_PATH + "/" + NAME + "/" + encodedName;
		return getJSONEntity(getRepoEndpoint(), url, Evaluation.class);
	}

	@Override
	public Evaluation updateEvaluation(Evaluation eval) throws SynapseException {
		ValidateArgument.required(eval, "Evaluation");
		String url = createEntityUri(EVALUATION_URI_PATH, eval.getId());
		return putJSONEntity(getRepoEndpoint(), url, eval, Evaluation.class);
	}

	@Override
	public void deleteEvaluation(String evalId) throws SynapseException {
		ValidateArgument.required(evalId, "Evaluation ID");
		String uri = createEntityUri(EVALUATION_URI_PATH, evalId);
		deleteUri(getRepoEndpoint(), uri);
	}

	@Override
	public EvaluationRound createEvaluationRound(EvaluationRound round) throws SynapseException {
		ValidateArgument.required(round, "round");
		ValidateArgument.required(round.getEvaluationId(), "round.evaluationId");
		String uri = EVALUATION_URI_PATH + "/" + round.getEvaluationId() + EVALUATION_ROUND;
		return postJSONEntity(getRepoEndpoint(), uri, round, EvaluationRound.class);
	}

	@Override
	public EvaluationRound getEvaluationRound(String evalId, String roundId) throws SynapseException {
		ValidateArgument.required(evalId, "Evaluation ID");
		ValidateArgument.required(roundId, "EvaluationRound ID");

		String uri = EVALUATION_URI_PATH + "/" + evalId + EVALUATION_ROUND + "/" + roundId;

		return getJSONEntity(getRepoEndpoint(), uri, EvaluationRound.class);
	}

	@Override
	public EvaluationRoundListResponse getAllEvaluationRounds(String evalId, EvaluationRoundListRequest request) throws SynapseException {
		ValidateArgument.required(evalId, "Evaluation ID");
		ValidateArgument.required(request, "request");

		String uri = EVALUATION_URI_PATH + "/" + evalId + EVALUATION_ROUND + LIST;

		return postJSONEntity(getRepoEndpoint(), uri, request, EvaluationRoundListResponse.class);
	}

	@Override
	public EvaluationRound updateEvaluationRound(EvaluationRound round) throws SynapseException {
		ValidateArgument.required(round, "round");
		ValidateArgument.required(round.getEvaluationId(), "round.evaluationId");
		ValidateArgument.required(round.getId(), "round.id");

		String uri = EVALUATION_URI_PATH + "/" + round.getEvaluationId() + EVALUATION_ROUND + "/" + round.getId();

		return putJSONEntity(getRepoEndpoint(), uri, round, EvaluationRound.class);
	}

	@Override
	public void deleteEvaluationRound(String evalId, String roundId) throws SynapseException {
		ValidateArgument.required(evalId, "evalId");
		ValidateArgument.required(roundId, "roundId");

		String uri = EVALUATION_URI_PATH + "/" + evalId + EVALUATION_ROUND + "/" + roundId;

		deleteUri(getRepoEndpoint(), uri);
	}

	@Override
	public Submission createIndividualSubmission(Submission sub, String etag,
			String challengeEndpoint, String notificationUnsubscribeEndpoint)
			throws SynapseException {
		ValidateArgument.required(etag, "etag");
		ValidateArgument.requirement(sub.getTeamId()==null,
				"For an individual submission Team ID must be null.");
		ValidateArgument.requirement(sub.getContributors()==null || sub.getContributors().isEmpty(),
				"For an individual submission, contributors may not be specified.");
		String uri = EVALUATION_URI_PATH + "/" + SUBMISSION + "?" + ETAG + "=" + etag;
		if (challengeEndpoint!=null && notificationUnsubscribeEndpoint!=null) {
			uri += "&" + CHALLENGE_ENDPOINT_PARAM + "=" + urlEncode(challengeEndpoint) + 
					"&"	+ NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM + "=" + 
					urlEncode(notificationUnsubscribeEndpoint);
		}

		return postJSONEntity(getRepoEndpoint(), uri, sub, Submission.class);
	}
	
	@Override
	public TeamSubmissionEligibility getTeamSubmissionEligibility(String evaluationId, String teamId) 
			throws SynapseException {
		ValidateArgument.required(evaluationId, "evaluationId");
		ValidateArgument.required(teamId, "teamId");
		String url = EVALUATION_URI_PATH+"/"+evaluationId+TEAM+"/"+teamId+
				SUBMISSION_ELIGIBILITY;
		return getJSONEntity(getRepoEndpoint(),url, TeamSubmissionEligibility.class);
	}

	@Override
	public Submission createTeamSubmission(Submission sub, String etag, String submissionEligibilityHash,
			String challengeEndpoint, String notificationUnsubscribeEndpoint)
			throws SynapseException {
		ValidateArgument.required(etag, "etag");
		ValidateArgument.requirement(submissionEligibilityHash!=null,
				"For a Team submission 'submissionEligibilityHash' is required.");
		ValidateArgument.requirement(sub.getTeamId()!=null,
				"For a Team submission Team ID is required.");

		String uri = EVALUATION_URI_PATH + "/" + SUBMISSION + "?" + ETAG + "="
				+ etag + "&" + SUBMISSION_ELIGIBILITY_HASH+"="+submissionEligibilityHash;
		if (challengeEndpoint!=null && notificationUnsubscribeEndpoint!=null) {
			uri += "&" + CHALLENGE_ENDPOINT_PARAM + "=" + urlEncode(challengeEndpoint) + 
					"&"	+ NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM + "=" + 
					urlEncode(notificationUnsubscribeEndpoint);
		}
		return postJSONEntity(getRepoEndpoint(), uri, sub, Submission.class);
	}

	@Override
	public Submission getSubmission(String subId) throws SynapseException {
		ValidateArgument.required(subId, "Submission Id");
		String url = EVALUATION_URI_PATH + "/" + SUBMISSION + "/" + subId;
		return getJSONEntity(getRepoEndpoint(), url, Submission.class);
	}

	@Override
	public SubmissionStatus getSubmissionStatus(String subId)
			throws SynapseException {
		ValidateArgument.required(subId, "Submission Id");
		String url = EVALUATION_URI_PATH + "/" + SUBMISSION + "/" + subId + "/"
				+ STATUS;
		return getJSONEntity(getRepoEndpoint(), url, SubmissionStatus.class);
	}

	@Override
	public SubmissionStatus updateSubmissionStatus(SubmissionStatus status)
			throws SynapseException {
		ValidateArgument.required(status, "SubmissionStatus");
		if (status.getAnnotations() != null) {
			AnnotationsUtils.validateAnnotations(status.getAnnotations());
		}
		String url = EVALUATION_URI_PATH + "/" + SUBMISSION + "/" + status.getId() + STATUS;
		return putJSONEntity(getRepoEndpoint(), url, status, SubmissionStatus.class);
	}

	public BatchUploadResponse updateSubmissionStatusBatch(String evaluationId,
			SubmissionStatusBatch batch) throws SynapseException {
		ValidateArgument.required(evaluationId, "evaluationId");
		ValidateArgument.required(batch, "SubmissionStatusBatch");
		ValidateArgument.required(batch.getIsFirstBatch(), "isFirstBatch");
		ValidateArgument.required(batch.getIsLastBatch(), "isLastBatch");
		ValidateArgument.requirement(batch.getIsFirstBatch() || batch.getBatchToken() != null,
				"batchToken cannot be null for any but the first batch.");
		List<SubmissionStatus> statuses = batch.getStatuses();
		ValidateArgument.requirement(statuses != null && statuses.size() > 0,
				"SubmissionStatusBatch must contain at least one SubmissionStatus.");
		for (SubmissionStatus status : statuses) {
			if (status.getAnnotations() != null) {
				AnnotationsUtils.validateAnnotations(status.getAnnotations());
			}
		}
		String url = EVALUATION_URI_PATH + "/" + evaluationId + STATUS_BATCH;
		return putJSONEntity(getRepoEndpoint(), url, batch, BatchUploadResponse.class);
	}

	@Override
	public void deleteSubmission(String subId) throws SynapseException {
		ValidateArgument.required(subId, "Submission Id");
		String uri = EVALUATION_URI_PATH + "/" + SUBMISSION + "/" + subId;
		deleteUri(getRepoEndpoint(), uri);
	}

	@Override
	public PaginatedResults<Submission> getAllSubmissions(String evalId,
			long offset, long limit) throws SynapseException {
		ValidateArgument.required(evalId, "Evaluation ID");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/" + SUBMISSION_ALL
				+ "?offset" + "=" + offset + "&limit=" + limit;
		return getPaginatedResults(getRepoEndpoint(), url, Submission.class);
	}

	@Override
	public PaginatedResults<SubmissionStatus> getAllSubmissionStatuses(
			String evalId, long offset, long limit) throws SynapseException {
		ValidateArgument.required(evalId, "Evaluation ID");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/"
				+ SUBMISSION_STATUS_ALL + "?offset" + "=" + offset + "&limit="
				+ limit;
		return getPaginatedResults(getRepoEndpoint(), url, SubmissionStatus.class);
	}

	@Override
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundles(
			String evalId, long offset, long limit) throws SynapseException {
		ValidateArgument.required(evalId, "Evaluation ID");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/"
				+ SUBMISSION_BUNDLE_ALL + "?offset" + "=" + offset + "&limit="
				+ limit;
		return getPaginatedResults(getRepoEndpoint(), url, SubmissionBundle.class);
	}

	@Override
	public PaginatedResults<Submission> getAllSubmissionsByStatus(
			String evalId, SubmissionStatusEnum status, long offset, long limit)
			throws SynapseException {
		ValidateArgument.required(evalId, "Evaluation ID");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/" + SUBMISSION_ALL
				+ STATUS_SUFFIX + status.toString() + "&offset=" + offset
				+ "&limit=" + limit;
		return getPaginatedResults(getRepoEndpoint(), url, Submission.class);
	}

	@Override
	public PaginatedResults<SubmissionStatus> getAllSubmissionStatusesByStatus(
			String evalId, SubmissionStatusEnum status, long offset, long limit)
			throws SynapseException {
		ValidateArgument.required(evalId, "Evaluation ID");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/"
				+ SUBMISSION_STATUS_ALL + STATUS_SUFFIX + status.toString()
				+ "&offset=" + offset + "&limit=" + limit;
		return getPaginatedResults(getRepoEndpoint(), url, SubmissionStatus.class);
	}

	@Override
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundlesByStatus(
			String evalId, SubmissionStatusEnum status, long offset, long limit)
			throws SynapseException {
		ValidateArgument.required(evalId, "Evaluation ID");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/"
				+ SUBMISSION_BUNDLE_ALL + STATUS_SUFFIX + status.toString()
				+ "&offset=" + offset + "&limit=" + limit;
		return getPaginatedResults(getRepoEndpoint(), url, SubmissionBundle.class);
	}

	@Override
	public PaginatedResults<Submission> getMySubmissions(String evalId,
			long offset, long limit) throws SynapseException {
		ValidateArgument.required(evalId, "Evaluation ID");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/" + SUBMISSION
				+ "?offset" + "=" + offset + "&limit=" + limit;
		return getPaginatedResults(getRepoEndpoint(), url, Submission.class);
	}

	@Override
	public PaginatedResults<SubmissionBundle> getMySubmissionBundles(
			String evalId, long offset, long limit) throws SynapseException {
		ValidateArgument.required(evalId, "Evaluation ID");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/"
				+ SUBMISSION_BUNDLE + "?offset" + "=" + offset + "&limit="
				+ limit;
		return getPaginatedResults(getRepoEndpoint(), url, SubmissionBundle.class);
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
		return getUrl(getRepoEndpoint(), url);
	}

	@Override
	public void downloadFromSubmission(String submissionId,
			String fileHandleId, File destinationFile) throws SynapseException {
		String uri = EVALUATION_URI_PATH + "/" + SUBMISSION + "/"
				+ submissionId + FILE + "/" + fileHandleId;
		super.downloadFromSynapse(
				getRepoEndpoint() + uri, null, destinationFile);
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
		ValidateArgument.required(query, "query");
		String queryUri;
		try {
			queryUri = EVALUATION_QUERY_URI_PATH + URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new SynapseClientException(e);
		}
		return getJSONEntity(getRepoEndpoint(), queryUri, QueryTableResults.class);
	}

	/**
	 * Moves an entity and its descendants to the trash can.
	 *
	 * @param entityId
	 *            The ID of the entity to be moved to the trash can
	 */
	@Override
	public void moveToTrash(String entityId) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		String url = TRASHCAN_TRASH + "/" + entityId;
		voidPut(getRepoEndpoint(), url, null);
	}

	/**
	 * Moves an entity and its descendants out of the trash can. The entity will
	 * be restored to the specified parent. If the parent is not specified, it
	 * will be restored to the original parent.
	 */
	@Override
	public void restoreFromTrash(String entityId, String newParentId)
			throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		String url = TRASHCAN_RESTORE + "/" + entityId;
		if (newParentId != null && !newParentId.isEmpty()) {
			url = url + "/" + newParentId;
		}
		voidPut(getRepoEndpoint(), url, null);
	}

	/**
	 * Retrieves entities (in the trash can) deleted by the user.
	 */
	@Override
	public PaginatedResults<TrashedEntity> viewTrashForUser(long offset,
			long limit) throws SynapseException {
		String url = TRASHCAN_VIEW + "?" + OFFSET + "=" + offset + "&" + LIMIT + "=" + limit;
		return getPaginatedResults(getRepoEndpoint(), url, TrashedEntity.class);
	}
	
	@Override
	public void flagForPurge(String entityId) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		String url = TRASHCAN_PURGE + "/" + entityId;
		voidPut(getRepoEndpoint(), url, null);
	}

	/**
	 * Purges the specified entity from the trash can. After purging, the entity
	 * will be permanently deleted.
	 */
	@Override
	public void purgeTrashForUser(String entityId) throws SynapseException {
		flagForPurge(entityId);
	}

	@Override
	public void logError(LogEntry logEntry) throws SynapseException {
		voidPost(getRepoEndpoint(), LOG, logEntry, null);
	}

	@Override
	@Deprecated
	public List<UploadDestination> getUploadDestinations(String parentEntityId) throws SynapseException {
		// Get the json for this entity as a list wrapper
		String url = ENTITY + "/" + parentEntityId + "/uploadDestinations";
		return getListOfJSONEntity(getFileEndpoint(), url, UploadDestination.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends StorageLocationSetting> T createStorageLocationSetting(T storageLocation)
			throws SynapseException {
		return (T) postJSONEntity(getRepoEndpoint(), STORAGE_LOCATION, storageLocation, StorageLocationSetting.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends StorageLocationSetting> T getMyStorageLocationSetting(Long storageLocationId) throws SynapseException {
		String url = STORAGE_LOCATION + "/" + storageLocationId;
		return (T) getJSONEntity(getRepoEndpoint(), url, StorageLocationSetting.class);
	}

	@Override
	public List<StorageLocationSetting> getMyStorageLocationSettings() throws SynapseException {
		return getListOfJSONEntity(getRepoEndpoint(), STORAGE_LOCATION, StorageLocationSetting.class);
	}

	@Override
	public UploadDestinationLocation[] getUploadDestinationLocations(String parentEntityId) throws SynapseException {
		// Get the json for this entity as a list wrapper
		String url = ENTITY + "/" + parentEntityId + "/uploadDestinationLocations";
		List<UploadDestinationLocation> locations = getListOfJSONEntity(getFileEndpoint(), url, UploadDestinationLocation.class);
		return locations.toArray(new UploadDestinationLocation[locations.size()]);
	}

	@Override
	public UploadDestination getUploadDestination(String parentEntityId, Long storageLocationId) throws SynapseException {
		String uri = ENTITY + "/" + parentEntityId + "/uploadDestination/" + storageLocationId;
		return getJSONEntity(getFileEndpoint(), uri, UploadDestination.class);
	}

	@Override
	public UploadDestination getDefaultUploadDestination(String parentEntityId) throws SynapseException {
		String uri = ENTITY + "/" + parentEntityId + "/uploadDestination";
		return getJSONEntity(getFileEndpoint(), uri, UploadDestination.class);
	}

	@Override
	public ProjectSetting getProjectSetting(String projectId, ProjectSettingsType projectSettingsType) throws SynapseException {
		String uri = PROJECT_SETTINGS + "/" + projectId + "/type/" + projectSettingsType;
		return getJSONEntity(getRepoEndpoint(), uri, ProjectSetting.class);
	}

	@Override
	public ProjectSetting createProjectSetting(ProjectSetting projectSetting)
			throws SynapseException {
		return postJSONEntity(getRepoEndpoint(), PROJECT_SETTINGS, projectSetting, ProjectSetting.class);
	}

	@Override
	public void updateProjectSetting(ProjectSetting projectSetting) throws SynapseException {
		voidPut(getRepoEndpoint(), PROJECT_SETTINGS, projectSetting);
	}

	@Override
	public void deleteProjectSetting(String projectSettingsId) throws SynapseException {
		String uri = PROJECT_SETTINGS + "/" + projectSettingsId;
		deleteUri(getRepoEndpoint(), uri);
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
		ValidateArgument.required(entityId, "entityId");
		String url = createEntityUri(FAVORITE_URI_PATH, entityId);
		return postJSONEntity(getRepoEndpoint(), url, null, EntityHeader.class);
	}

	/**
	 * Remove the entity from this user's Favorites list
	 * 
	 * @param entityId
	 * @throws SynapseException
	 */
	@Override
	public void removeFavorite(String entityId) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		String uri = createEntityUri(FAVORITE_URI_PATH, entityId);
		deleteUri(getRepoEndpoint(), uri);
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
		return getPaginatedResults(getRepoEndpoint(), url, EntityHeader.class);
	}

	/**
	 * Retrieve this user's Projects list
	 * 
	 * @param type the type of list to get
	 * @param sortColumn the optional sort column (default by last activity)
	 * @param sortDirection the optional sort direction (default descending)
	 * @param nextPageToken
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public ProjectHeaderList getMyProjects(ProjectListType type, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			String nextPageToken) throws SynapseException {
		return getProjects(type, null, null, sortColumn, sortDirection, nextPageToken);
	}

	/**
	 * Retrieve a user's Projects list
	 * 
	 * @param userId the user for which to get the project list
	 * @param sortColumn the optional sort column (default by last activity)
	 * @param sortDirection the optional sort direction (default descending)
	 * @param nextPageToken
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public ProjectHeaderList getProjectsFromUser(Long userId, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			String nextPageToken) throws SynapseException {
		return getProjects(ProjectListType.ALL, userId, null, sortColumn, sortDirection, nextPageToken);
	}

	/**
	 * Retrieve a teams's Projects list
	 * 
	 * @param teamId the team for which to get the project list
	 * @param sortColumn the optional sort column (default by last activity)
	 * @param sortDirection the optional sort direction (default descending)
	 * @param nextPageToken
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public ProjectHeaderList getProjectsForTeam(Long teamId, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			String nextPageToken) throws SynapseException {
		return getProjects(ProjectListType.TEAM, null, teamId, sortColumn, sortDirection, nextPageToken);
	}

	private ProjectHeaderList getProjects(ProjectListType type, Long userId, Long teamId, ProjectListSortColumn sortColumn,
			SortDirection sortDirection, String nextPageToken) throws SynapseException, SynapseClientException {
		String url = PROJECTS_URI_PATH;
		if (userId != null) {
			url += USER + '/' + userId;
		}
		if (sortColumn == null) {
			sortColumn = ProjectListSortColumn.LAST_ACTIVITY;
		}
		if (sortDirection == null) {
			sortDirection = SortDirection.DESC;
		}
		url += "?sort=" + sortColumn.name() + "&sortDirection=" + sortDirection.name();
		if (nextPageToken != null) {
			url += "&" + NEXT_PAGE_TOKEN_PARAM + nextPageToken;
		}		

		if (teamId != null) {
			url += "&teamId=" + teamId;
		}
		if (type!=null) {
			url += "&filter="+type;
		}

		return getJSONEntity(getRepoEndpoint(), url, ProjectHeaderList.class);
	}

	@Deprecated
	public PaginatedResults<ProjectHeader> getMyProjectsDeprecated(ProjectListType type, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			Integer limit, Integer offset) throws SynapseException {
		return getProjectsDeprecated(type, null, null, sortColumn, sortDirection, limit, offset);
	}
	@Deprecated
	public PaginatedResults<ProjectHeader> getProjectsFromUserDeprecated(Long userId, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			Integer limit, Integer offset) throws SynapseException {
		return getProjectsDeprecated(ProjectListType.ALL, userId, null, sortColumn, sortDirection, limit, offset);
	}
	
	@Deprecated
	public PaginatedResults<ProjectHeader> getProjectsForTeamDeprecated(Long teamId, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			Integer limit, Integer offset) throws SynapseException {
		return getProjectsDeprecated(ProjectListType.TEAM, null, teamId, sortColumn, sortDirection, limit, offset);
	}

	private PaginatedResults<ProjectHeader> getProjectsDeprecated(ProjectListType type, Long userId, Long teamId, ProjectListSortColumn sortColumn,
			SortDirection sortDirection, Integer limit, Integer offset) throws SynapseException, SynapseClientException {
		String url = PROJECTS_URI_PATH+"/";
		switch (type) {
		case ALL:
		default:
			if (userId==null) {
				url += ProjectListTypeDeprecated.MY_PROJECTS;
			} else {
				url += ProjectListTypeDeprecated.OTHER_USER_PROJECTS;
			}
			break;
		case CREATED:
			url += ProjectListTypeDeprecated.MY_CREATED_PROJECTS;
			break;
		case PARTICIPATED:
			url += ProjectListTypeDeprecated.MY_PARTICIPATED_PROJECTS;
			break;
		case TEAM:
			if (teamId==null) {
				url += ProjectListTypeDeprecated.MY_TEAM_PROJECTS;
			} else {
				url += ProjectListTypeDeprecated.TEAM_PROJECTS;
			}
			break;
		}
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
		url += "?sort=" + sortColumn.name() + "&sortDirection="+ sortDirection.name();		
		if (offset!=null) {
			url += '&' + OFFSET_PARAMETER + offset ;
		}
		if (limit!=null) {
			url += '&' + LIMIT_PARAMETER + limit;
		}

		return getPaginatedResults(getRepoEndpoint(), url, ProjectHeader.class);
	}


	/**
	 * Gets the DOI Association for the specified object. If object version is null, the call will return the DOI
	 * for the current version of the object.
	 */
	@Override
	public DoiAssociation getDoiAssociation(String objectId, ObjectType objectType, Long objectVersion) throws SynapseException {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		String url = DOI_ASSOCIATION + "?id=" + objectId + "&type=" + objectType;
		if (objectVersion != null) {
			url += "&version=" + objectVersion;
		}
		return getJSONEntity(getRepoEndpoint(), url, DoiAssociation.class);
	}

	/**
	 * Gets the DOI for the specified object. If object version is null, the call will return the DOI
	 * for the current version of the object.
	 */
	@Override
	public Doi getDoi(String objectId, ObjectType objectType, Long objectVersion) throws SynapseException {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		String url = DOI_ASSOCIATION + "?id=" + objectId + "&type=" + objectType;
		if (objectVersion != null) {
			url += "&version=" + objectVersion;
		}
		return getJSONEntity(getRepoEndpoint(), url, Doi.class);
	}


	/**
	 * Creates a DOI for the specified object. Idempotent.
	 */
	@Override
	public String createOrUpdateDoiAsyncStart(Doi doi) throws SynapseException {
		DoiRequest request = new DoiRequest();
		request.setDoi(doi);
		return startAsynchJob(AsynchJobType.Doi, request);
	}

	/**
	 * Retrieves the status of a createOrUpdateDoi call.
	 */
	@Override
	public DoiResponse createOrUpdateDoiAsyncGet(String asyncJobToken) throws SynapseException {
		String url = DOI + ASYNC_GET + asyncJobToken;
		return getJSONEntity(getRepoEndpoint(), url, DoiResponse.class);
	}

	/**
	 *
	 */
	@Override
	public String getPortalUrl(String objectId, ObjectType objectType, Long objectVersion) throws SynapseException {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		String requestUrl = DOI_LOCATE + "?id=" + objectId + "&type=" + objectType;
		if (objectVersion != null) {
			requestUrl += "&version=" + objectVersion;
		}
		requestUrl += "&redirect=false";
		return getStringDirect(getRepoEndpoint(), requestUrl);
	}

	/**
	 * Gets the header information of entities whose file's MD5 matches the
	 * given MD5 checksum.
	 */
	@Deprecated
	/* wrong use of PaginatedResults since the expected returned data is not a page */
	@Override
	public List<EntityHeader> getEntityHeaderByMd5(String md5) throws SynapseException {
		ValidateArgument.required(md5, "md5");
		String url = ENTITY + "/md5/" + md5;
		return getPaginatedResults(getRepoEndpoint(), url, EntityHeader.class).getResults();
	}

	@Override
	public String retrieveApiKey() throws SynapseException {
		return getJSONEntity(getAuthEndpoint(),"/secretKey", SecretKey.class).getSecretKey();
	}

	@Override
	public String createPersonalAccessToken(AccessTokenGenerationRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		return postJSONEntity(getAuthEndpoint(), AUTH_PERSONAL_ACCESS_TOKEN, request, AccessTokenGenerationResponse.class).getToken();
	}

	@Override
	public AccessTokenRecord retrievePersonalAccessTokenRecord(String tokenId) throws SynapseException {
		ValidateArgument.required(tokenId, "tokenId");
		return getJSONEntity(getAuthEndpoint(), AUTH_PERSONAL_ACCESS_TOKEN + "/" + tokenId, AccessTokenRecord.class);
	}

	@Override
	public AccessTokenRecordList retrievePersonalAccessTokenRecords(String nextPageToken) throws SynapseException {
		String uri = AUTH_PERSONAL_ACCESS_TOKEN;
		if (nextPageToken != null) {
			uri += "?" + NEXT_PAGE_TOKEN_PARAM + nextPageToken;
		}
		return getJSONEntity(getAuthEndpoint(), uri, AccessTokenRecordList.class);
	}

	@Override
	public void revokePersonalAccessToken(String tokenId) throws SynapseException {
		ValidateArgument.required(tokenId, "tokenId");
		deleteUri(getAuthEndpoint(), AUTH_PERSONAL_ACCESS_TOKEN + "/" + tokenId);
	}

	@Override
	public AccessControlList updateEvaluationAcl(AccessControlList acl)
			throws SynapseException {
		ValidateArgument.required(acl, "acl");
		return putJSONEntity(getRepoEndpoint(), EVALUATION_ACL_URI_PATH, acl, AccessControlList.class);
	}

	@Override
	public AccessControlList getEvaluationAcl(String evalId)
			throws SynapseException {
		ValidateArgument.required(evalId, "Evaluation ID");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/acl";
		return getJSONEntity(getRepoEndpoint(), url, AccessControlList.class);
	}

	@Override
	public UserEvaluationPermissions getUserEvaluationPermissions(String evalId)
			throws SynapseException {

		ValidateArgument.required(evalId, "Evaluation ID");
		String url = EVALUATION_URI_PATH + "/" + evalId + "/permissions";
		return getJSONEntity(getRepoEndpoint(), url, UserEvaluationPermissions.class);
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
	public String startTableTransactionJob(List<TableUpdateRequest> changes,
			String tableId) throws SynapseException {
		TableUpdateTransactionRequest request = new TableUpdateTransactionRequest();
		request.setEntityId(tableId);
		request.setChanges(changes);
		return startAsynchJob(AsynchJobType.TableTransaction, request);
	}

	@Override
	public List<TableUpdateResponse> getTableTransactionJobResults(String token, String tableId)
			throws SynapseException, SynapseResultNotReadyException {
		TableUpdateTransactionResponse response = (TableUpdateTransactionResponse) getAsyncResult(
				AsynchJobType.TableTransaction, token, tableId);
		return response.getResults();
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
		throw new SynapseClientException("Timed out waiting for jobId: " + jobId);
	}

	@Override
	public RowReferenceSet deleteRowsFromTable(RowSelection toDelete) throws SynapseException {
		ValidateArgument.required(toDelete, "RowSelection");
		ValidateArgument.required(toDelete.getTableId(), "RowSelection.tableId");
		String uri = ENTITY + "/" + toDelete.getTableId() + TABLE + "/deleteRows";
		return postJSONEntity(getRepoEndpoint(), uri, toDelete, RowReferenceSet.class);
	}

	@Override
	public TableFileHandleResults getFileHandlesFromTable(
			RowReferenceSet fileHandlesToFind) throws SynapseException {
		ValidateArgument.required(fileHandlesToFind, "RowReferenceSet");
		String uri = ENTITY + "/" + fileHandlesToFind.getTableId() + TABLE + FILE_HANDLES;
		return postJSONEntity(getRepoEndpoint(), uri, fileHandlesToFind, TableFileHandleResults.class);
	}

	/**
	 * Get the temporary URL for the data file of a file handle column for a
	 * row. This is an alternative to downloading the file.
	 *
	 * @param tableId
	 * @param row
	 * @param columnId
	 * @return
	 * @throws IOException
	 * @throws SynapseException
	 */
	@Override
	public URL getTableFileHandleTemporaryUrl(String tableId, RowReference row,
			String columnId) throws IOException, SynapseException {
		String uri = getUriForFileHandle(tableId, row, columnId) + FILE
				+ QUERY_REDIRECT_PARAMETER + "false";
		return getUrl(getRepoEndpoint(), uri);
	}

	@Override
	public void downloadFromTableFileHandleTemporaryUrl(String tableId,
			RowReference row, String columnId, File destinationFile)
			throws SynapseException {
		String uri = getUriForFileHandle(tableId, row, columnId) + FILE;
		downloadFromSynapse(getRepoEndpoint() + uri, null, destinationFile);
	}

	@Override
	public URL getTableFileHandlePreviewTemporaryUrl(String tableId,
			RowReference row, String columnId) throws IOException,
			SynapseException {
		String uri = getUriForFileHandle(tableId, row, columnId) + FILE_PREVIEW
				+ QUERY_REDIRECT_PARAMETER + "false";
		return getUrl(getRepoEndpoint(), uri);
	}

	@Override
	public void downloadFromTableFileHandlePreviewTemporaryUrl(String tableId,
			RowReference row, String columnId, File destinationFile)
			throws SynapseException {
		String uri = getUriForFileHandle(tableId, row, columnId) + FILE_PREVIEW;
		downloadFromSynapse(getRepoEndpoint() + uri, null, destinationFile);
	}

	private static String getUriForFileHandle(String tableId, RowReference row,
			String columnId) {
		return ENTITY + "/" + tableId + TABLE + COLUMN + "/" + columnId
				+ ROW_ID + "/" + row.getRowId() + ROW_VERSION + "/"
				+ row.getVersionNumber();
	}
	
	@Override
	public String queryTableEntityBundleAsyncStart(Query query, QueryOptions queryOptions, String tableId)
			throws SynapseException {
		
		QueryBundleRequest bundleRequest = new QueryBundleRequest();
		
		bundleRequest.setEntityId(tableId);
		bundleRequest.setQuery(query);
		bundleRequest.setPartMask(queryOptions.getPartMask());
		
		return startAsynchJob(AsynchJobType.TableQuery, bundleRequest);
	}

	@Override
	public String queryTableEntityBundleAsyncStart(String sql, Long offset,
			Long limit, int partsMask, String tableId)
			throws SynapseException {
		
		Query query = new Query();
		
		query.setSql(sql);
		query.setOffset(offset);
		query.setLimit(limit);
		
		QueryOptions queryOptions = new QueryOptions().withMask((long) partsMask);
		
		return queryTableEntityBundleAsyncStart(query, queryOptions, tableId);
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
		ValidateArgument.required(model, "ColumnModel");
		return postJSONEntity(getRepoEndpoint(), COLUMN, model, ColumnModel.class);
	}

	@Override
	public List<ColumnModel> createColumnModels(List<ColumnModel> models)
			throws SynapseException {
		ValidateArgument.required(models, "ColumnModel");
		return getListOfJSONEntity(getRepoEndpoint(), COLUMN_BATCH,
				ListWrapper.wrap(models, ColumnModel.class), ColumnModel.class);
	}

	@Override
	public ColumnModel getColumnModel(String columnId) throws SynapseException {
		ValidateArgument.required(columnId, "ColumnId");
		String url = COLUMN + "/" + columnId;
		return getJSONEntity(getRepoEndpoint(), url, ColumnModel.class);
	}

	@Override
	public List<ColumnModel> getColumnModelsForTableEntity(String tableEntityId)
			throws SynapseException {
		ValidateArgument.required(tableEntityId, "tableEntityId");
		String url = ENTITY + "/" + tableEntityId + COLUMN;
		return getJSONEntity(getRepoEndpoint(), url, PaginatedColumnModels.class).getResults();
	}
	
	@Override
	public List<ColumnModel> getDefaultColumnsForView(ViewType viewType)
			throws SynapseException {
		ValidateArgument.required(viewType, "viewType");
		String url = COLUMN_VIEW_DEFAULT+"/"+viewType.name();
		return getListOfJSONEntity(getRepoEndpoint(), url, ColumnModel.class);
	}
	
	@Override
	public List<ColumnModel> getDefaultColumnsForView(ViewEntityType viewEntityType, Long viewTypeMask)
			throws SynapseException {
		StringBuilder builder = new StringBuilder(COLUMN_VIEW_DEFAULT);
		if (viewEntityType != null || viewTypeMask != null) {
			builder.append("?");
		}
		if (viewEntityType != null) {
			builder.append("viewEntityType=").append(viewEntityType.name());
		}
		if (viewTypeMask != null) {
			if (viewEntityType != null) {
				builder.append("&");
			}
			builder.append("viewTypeMask=").append(viewTypeMask);
		}
		String url = builder.toString();
		return getListOfJSONEntity(getRepoEndpoint(), url, ColumnModel.class);
	}

	@Override
	public PaginatedColumnModels listColumnModels(String prefix, Long limit,
			Long offset) throws SynapseException {
		String url = buildListColumnModelUrl(prefix, limit, offset);
		return getJSONEntity(getRepoEndpoint(), url, PaginatedColumnModels.class);
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
		ValidateArgument.required(jobBody, "jobBody");
		return postJSONEntity(getRepoEndpoint(), ASYNCHRONOUS_JOB, jobBody, AsynchronousJobStatus.class);
	}

	/**
	 * Get the status of an Asynchronous Job from its ID.
	 * 
	 * @param jobId
	 * @return
	 * @throws SynapseException
	 */
	@Override
	public AsynchronousJobStatus getAsynchronousJobStatus(String jobId) throws SynapseException {
		ValidateArgument.required(jobId, "jobId");
		String url = ASYNCHRONOUS_JOB + "/" + jobId;
		return getJSONEntity(getRepoEndpoint(), url, AsynchronousJobStatus.class);
	}

	@Override
	public Team createTeam(Team team) throws SynapseException {
		return postJSONEntity(getRepoEndpoint(), TEAM, team, Team.class);
	}

	@Override
	public Team getTeam(String id) throws SynapseException {
		String url = TEAM + "/" + id;
		return getJSONEntity(getRepoEndpoint(), url, Team.class);
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
		return getPaginatedResults(getRepoEndpoint(), uri, Team.class);
	}
	
	@Override
	public List<Team> listTeams(List<Long> ids) throws SynapseException {
		IdList idList = new IdList();
		idList.setList(ids);
		return getListOfJSONEntity(getRepoEndpoint(), TEAM_LIST, idList, Team.class);
	}

	@Override
	public PaginatedResults<Team> getTeamsForUser(String memberId, long limit,
			long offset) throws SynapseException {
		String uri = USER + "/" + memberId + TEAM + "?" + OFFSET + "=" + offset
				+ "&" + LIMIT + "=" + limit;
		return getPaginatedResults(getRepoEndpoint(), uri, Team.class);
	}

	private static String createGetTeamIconURI(String teamId, boolean redirect) {
		return TEAM + "/" + teamId + ICON + "?" + REDIRECT_PARAMETER + redirect;
	}

	@Override
	public URL getTeamIcon(String teamId) throws SynapseException {
		return getUrl(getRepoEndpoint(), createGetTeamIconURI(teamId, false));
	}

	// alternative to getTeamIcon
	@Override
	public void downloadTeamIcon(String teamId, File target) throws SynapseException {
		String uri = createGetTeamIconURI(teamId, true);
		downloadFromSynapse(getRepoEndpoint() + uri, null, target);
	}

	@Override
	public Team updateTeam(Team team) throws SynapseException {
		return putJSONEntity(getRepoEndpoint(), TEAM, team, Team.class);
	}

	@Override
	public void deleteTeam(String teamId) throws SynapseException {
		deleteUri(getRepoEndpoint(), TEAM + "/" + teamId);
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
		voidPut(getRepoEndpoint(), uri, null);
	}
	
	@Override
	public ResponseMessage addTeamMember(JoinTeamSignedToken joinTeamSignedToken, 
			String teamEndpoint, String notificationUnsubscribeEndpoint) 
			throws SynapseException {
		String uri = TEAM + "Member";
		if (teamEndpoint!=null && notificationUnsubscribeEndpoint!=null) {
			uri += "?" + TEAM_ENDPOINT_PARAM + "=" + urlEncode(teamEndpoint) + 
				"&"	+ NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM + "=" + urlEncode(notificationUnsubscribeEndpoint);
		}
		return putJSONEntity(getRepoEndpoint(), uri, joinTeamSignedToken, ResponseMessage.class);
		
	}
	
	private static String urlEncode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 *
	 * @deprecated  Use {@link #getTeamMembers(String, String, TeamMemberTypeFilterOptions, long, long) getTeamMembers} instead
	 */
	@Deprecated
	@Override
	public PaginatedResults<TeamMember> getTeamMembers(String teamId, String fragment, long limit, long offset) throws SynapseException {
		return getTeamMembers(teamId, fragment, TeamMemberTypeFilterOptions.ALL, limit, offset);
	}


	@Override
	public PaginatedResults<TeamMember> getTeamMembers(String teamId,
													   String fragment, TeamMemberTypeFilterOptions memberType,
													   long limit, long offset) throws SynapseException {
		URIBuilder uri = new URIBuilder();
		uri.setPath(TEAM_MEMBERS + "/" + teamId);
		if (fragment != null) {
			uri.setParameter(NAME_FRAGMENT_FILTER, urlEncode(fragment));
		}
		if (memberType != null) {
			uri.setParameter(NAME_MEMBERTYPE_FILTER, memberType.toString());
		}
		uri.setParameter(OFFSET, String.valueOf(offset));
		uri.setParameter(LIMIT, String.valueOf(limit));

		return getPaginatedResults(getRepoEndpoint(), uri.toString(), TeamMember.class);
	}

	/**
	 * 
	 * @param teamId
	 * @param fragment
	 * @return the number of members in the given team, optionally filtered by the given prefix
	 * @throws SynapseException
	 */
	@Override
	public long countTeamMembers(String teamId, String fragment) throws SynapseException {
		String uri = null;
		if (fragment == null) {
			uri = TEAM_MEMBERS + "/count/" + teamId;
		} else {
			uri = TEAM_MEMBERS + "/count/" + teamId + "?" + NAME_FRAGMENT_FILTER
					+ "=" + urlEncode(fragment) ;
		}
		Count tmc = getJSONEntity(getRepoEndpoint(), uri, Count.class);
		return tmc.getCount();
	}
	
	@Override
	public List<TeamMember> listTeamMembers(String teamId, List<Long> ids) throws SynapseException {
		IdList idList = new IdList();
		idList.setList(ids);
		String url = TEAM+"/"+teamId+MEMBER_LIST;
		return getListOfJSONEntity(getRepoEndpoint(), url, idList, TeamMember.class);
	}
	
	@Override
	public List<TeamMember> listTeamMembers(List<Long> teamIds, String userId) throws SynapseException {
		IdList idList = new IdList();
		idList.setList(teamIds);
		String url = USER+"/"+userId+MEMBER_LIST;
		return getListOfJSONEntity(getRepoEndpoint(), url, idList, TeamMember.class);
	}

	public TeamMember getTeamMember(String teamId, String memberId)
			throws SynapseException {
		String url = TEAM + "/" + teamId + MEMBER + "/" + memberId;
		return getJSONEntity(getRepoEndpoint(), url, TeamMember.class);
	}

	@Override
	public void removeTeamMember(String teamId, String memberId) throws SynapseException {
		deleteUri(getRepoEndpoint(), TEAM + "/" + teamId + MEMBER + "/" + memberId);
	}

	@Override
	public void setTeamMemberPermissions(String teamId, String memberId,
			boolean isAdmin) throws SynapseException {
		String url = TEAM + "/" + teamId + MEMBER + "/" + memberId + PERMISSION + "?"
				+ TEAM_MEMBERSHIP_PERMISSION + "=" + isAdmin;
		voidPut(getRepoEndpoint(), url, null);
	}

	@Override
	public TeamMembershipStatus getTeamMembershipStatus(String teamId,
			String principalId) throws SynapseException {
		String url = TEAM + "/" + teamId + MEMBER + "/" + principalId + MEMBERSHIP_STATUS;
		return getJSONEntity(getRepoEndpoint(), url, TeamMembershipStatus.class);
	}
	
	@Override
	public AccessControlList getTeamACL(String teamId) throws SynapseException {
		ValidateArgument.required(teamId, "teamID");
		String url = TEAM + "/" + teamId + "/acl";
		return getJSONEntity(getRepoEndpoint(), url, AccessControlList.class);
	}
	
	@Override
	public AccessControlList updateTeamACL(AccessControlList acl) throws SynapseException {
		ValidateArgument.required(acl, "acl");
		String url = TEAM+"/acl";
		return putJSONEntity(getRepoEndpoint(), url, acl, AccessControlList.class);
	}


	@Override
	public MembershipInvitation createMembershipInvitation(
			MembershipInvitation invitation,
			String acceptInvitationEndpoint,
			String notificationUnsubscribeEndpoint) throws SynapseException {
		String uri = MEMBERSHIP_INVITATION;
		if (acceptInvitationEndpoint!=null && notificationUnsubscribeEndpoint!=null) {
			uri += "?" + ACCEPT_INVITATION_ENDPOINT_PARAM + "=" + urlEncode(acceptInvitationEndpoint) +
					"&" + NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM + "=" + urlEncode(notificationUnsubscribeEndpoint);
		}
		return postJSONEntity(getRepoEndpoint(), uri, invitation, MembershipInvitation.class);
	}

	@Override
	public MembershipInvitation getMembershipInvitation(String invitationId)
			throws SynapseException {
		String url = MEMBERSHIP_INVITATION + "/" + invitationId;
		return getJSONEntity(getRepoEndpoint(), url, MembershipInvitation.class);
	}

	@Override
	public MembershipInvitation getMembershipInvitation(MembershipInvtnSignedToken token) throws SynapseException {
		String uri = MEMBERSHIP_INVITATION + "/" + token.getMembershipInvitationId();
		return postJSONEntity(getRepoEndpoint(), uri, token, MembershipInvitation.class);
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
		return getPaginatedResults(getRepoEndpoint(), uri, MembershipInvitation.class);
	}

	@Override
	public PaginatedResults<MembershipInvitation> getOpenMembershipInvitationSubmissions(
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
		return getPaginatedResults(getRepoEndpoint(), uri, MembershipInvitation.class);
	}

	@Override
	public void deleteMembershipInvitation(String invitationId) throws SynapseException {
		deleteUri(getRepoEndpoint(), MEMBERSHIP_INVITATION + "/" + invitationId);
	}

	@Override
	public Count getOpenMembershipInvitationCount() throws SynapseException {
		return getJSONEntity(getRepoEndpoint(), OPEN_MEMBERSHIP_INVITATION_COUNT, Count.class);
	}

	@Override
	public InviteeVerificationSignedToken getInviteeVerificationSignedToken(String membershipInvitationId) throws SynapseException {
		String uri = MEMBERSHIP_INVITATION + "/" + membershipInvitationId + "/inviteeVerificationSignedToken";
		return getJSONEntity(getRepoEndpoint(), uri, InviteeVerificationSignedToken.class);
	}

	@Override
	public void updateInviteeId(String membershipInvitationId, InviteeVerificationSignedToken token) throws SynapseException {
		String uri = MEMBERSHIP_INVITATION + "/" + membershipInvitationId + "/inviteeId";
		voidPut(getRepoEndpoint(), uri, token);
	}

	@Override
	public MembershipRequest createMembershipRequest(
			MembershipRequest request,
			String acceptRequestEndpoint,
			String notificationUnsubscribeEndpoint) throws SynapseException {
		String uri = MEMBERSHIP_REQUEST;
		if (acceptRequestEndpoint!=null && notificationUnsubscribeEndpoint!=null) {
			uri += 	"?" + ACCEPT_REQUEST_ENDPOINT_PARAM + "=" + urlEncode(acceptRequestEndpoint) +
					"&" + NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM + "=" + urlEncode(notificationUnsubscribeEndpoint);
		}
		return postJSONEntity(getRepoEndpoint(), uri, request, MembershipRequest.class);
	}

	@Override
	public MembershipRequest getMembershipRequest(String requestId)
			throws SynapseException {
		String url = MEMBERSHIP_REQUEST + "/" + requestId;
		return getJSONEntity(getRepoEndpoint(), url, MembershipRequest.class);
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
		return getPaginatedResults(getRepoEndpoint(), uri, MembershipRequest.class);
	}

	@Override
	public PaginatedResults<MembershipRequest> getOpenMembershipRequestSubmissions(
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
		return getPaginatedResults(getRepoEndpoint(), uri, MembershipRequest.class);
	}

	@Override
	public void deleteMembershipRequest(String requestId) throws SynapseException {
		deleteUri(getRepoEndpoint(), MEMBERSHIP_REQUEST + "/" + requestId);
	}

	@Override
	public Count getOpenMembershipRequestCount() throws SynapseException {
		return getJSONEntity(getRepoEndpoint(), OPEN_MEMBERSHIP_REQUEST_COUNT, Count.class);
	}

	@Override
	public void sendNewPasswordResetEmail(String passwordResetEndpoint, String email) throws SynapseException {
		Username user = new Username();
		user.setEmail(email);

		voidPost(getAuthEndpoint(), "/user/password/reset", user, Collections.singletonMap("passwordResetEndpoint", passwordResetEndpoint));
	}

	@Override
	public void changePassword(String username, String currentPassword, String newPassword, String authenticationReceipt)
			throws SynapseException {
		ChangePasswordWithCurrentPassword changePasswordWithCurrentPassword = new ChangePasswordWithCurrentPassword();
		changePasswordWithCurrentPassword.setUsername(username);
		changePasswordWithCurrentPassword.setCurrentPassword(currentPassword);
		changePasswordWithCurrentPassword.setNewPassword(newPassword);
		changePasswordWithCurrentPassword.setAuthenticationReceipt(authenticationReceipt);
		changePassword(changePasswordWithCurrentPassword);
	}

	@Override
	public void changePassword(ChangePasswordInterface changePasswordRequest) throws SynapseException {
		voidPost(getAuthEndpoint(), "/user/changePassword", changePasswordRequest, null);
	}

	@Override
	public void signTermsOfUse(String sessionToken, boolean acceptTerms)
			throws SynapseException {
		Session session = new Session();
		session.setSessionToken(sessionToken);
		session.setAcceptsTermsOfUse(acceptTerms);
		voidPost(getAuthEndpoint(), "/termsOfUse", session, null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.client.SynapseClient#getOAuth2AuthenticationUrl(org.sagebionetworks.repo.model.oauth.OAuthUrlRequest)
	 */
	@Override
	public OAuthUrlResponse getOAuth2AuthenticationUrl(OAuthUrlRequest request) throws SynapseException{
		return postJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_AUTH_URL, request, OAuthUrlResponse.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.client.SynapseClient#validateOAuthAuthenticationCode(org.sagebionetworks.repo.model.oauth.OAuthValidationRequest)
	 */
	@Override
	public Session validateOAuthAuthenticationCode(OAuthValidationRequest request) throws SynapseException{
		return postJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_SESSION, request, Session.class);
	}
	
	@Override
	public Session createAccountViaOAuth2(OAuthAccountCreationRequest request) throws SynapseException{
		return postJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_ACCOUNT, request, Session.class);
	}
	
	@Override
	public PrincipalAlias bindOAuthProvidersUserId(OAuthValidationRequest request)
			throws SynapseException {
		return postJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_ALIAS, request, PrincipalAlias.class);
		
	}
	
	@Override
	public void unbindOAuthProvidersUserId(OAuthProvider provider, String alias) throws SynapseException {
		ValidateArgument.required(provider, "provider");
		ValidateArgument.required(alias, "alias");
		try {
			String url = AUTH_OAUTH_2_ALIAS+"?provider="+
					URLEncoder.encode(provider.name(), "UTF-8")+
					"&"+"alias="+URLEncoder.encode(alias, "UTF-8");
			deleteUri(getAuthEndpoint(), url);
		} catch (UnsupportedEncodingException e) {
			throw new SynapseClientException(e);
		}
	}
	
	@Override
	public OIDConnectConfiguration getOIDConnectConfiguration() throws SynapseException {
		return getJSONEntity(getAuthEndpoint(), AUTH_OPENID_CONFIG, OIDConnectConfiguration.class);
	}

	@Override
	public JsonWebKeySet getOIDCJsonWebKeySet() throws SynapseException {
		return getJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_JWKS, JsonWebKeySet.class);
	}

	@Override
	public OAuthClient createOAuthClient(OAuthClient oauthClient) throws SynapseException {
		return postJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_CLIENT, oauthClient, OAuthClient.class);
	}

	@Override
	public OAuthClientIdAndSecret createOAuthClientSecret(String clientId) throws SynapseException {
		return postJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_CLIENT_SECRET+clientId, null, OAuthClientIdAndSecret.class);
	}

	@Override
	public OAuthClient getOAuthClient(String clientId) throws SynapseException {
		return getJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_CLIENT+"/"+clientId, OAuthClient.class);
	}

	@Override
	public OAuthClientList listOAuthClients(String nextPageToken) throws SynapseException {
		String uri = AUTH_OAUTH_2_CLIENT;
		if (nextPageToken != null) {
			uri += "?" + NEXT_PAGE_TOKEN_PARAM + nextPageToken;
		}
		return getJSONEntity(getAuthEndpoint(), uri, OAuthClientList.class);
	}

	@Override
	public OAuthClient updateOAuthClient(OAuthClient oauthClient) throws SynapseException {
		return putJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_CLIENT+"/"+oauthClient.getClient_id(), oauthClient, OAuthClient.class);
	}

	@Override
	public void deleteOAuthClient(String clientId) throws SynapseException {
		deleteUri(getAuthEndpoint(), AUTH_OAUTH_2_CLIENT+"/"+clientId);
	}

	@Override
	public OIDCAuthorizationRequestDescription getAuthenticationRequestDescription(
			OIDCAuthorizationRequest authorizationRequest) throws SynapseException {
		return postJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_REQUEST_DESCRIPTION, authorizationRequest, OIDCAuthorizationRequestDescription.class);
	}
	
	@Override
	public boolean hasUserAuthorizedClient(OIDCAuthorizationRequest authorizationRequest) throws SynapseException {
		OAuthConsentGrantedResponse response =  postJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_CONSENT_CHECK, 
				authorizationRequest, OAuthConsentGrantedResponse.class);
		return response.getGranted();
	}

	@Override
	public OAuthAuthorizationResponse authorizeClient(OIDCAuthorizationRequest authorizationRequest)
			throws SynapseException {
		return postJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_REQUEST_CONSENT, authorizationRequest, OAuthAuthorizationResponse.class);
	}

	@Override
	public OIDCTokenResponse getTokenResponse(OAuthGrantType grant_type, String code, String redirectUri,
			String refresh_token, String scope, String claims) throws SynapseException {
		URIBuilder uri = new URIBuilder();
		uri.setPath(AUTH_OAUTH_2_TOKEN);
		if (grant_type != null) {
			uri.setParameter(AUTH_OAUTH_2_GRANT_TYPE_PARAM, grant_type.name());
		}
		if (code != null) {
			uri.setParameter(AUTH_OAUTH_2_CODE_PARAM, code);
		}
		if (redirectUri != null) {
			uri.setParameter( AUTH_OAUTH_2_REDIRECT_URI_PARAM, redirectUri);
		}
		if (refresh_token != null) {
			uri.setParameter(AUTH_OAUTH_2_REfRESH_TOKEN_PARAM, refresh_token);
		}
		if (scope != null) {
			uri.setParameter(AUTH_OAUTH_2_SCOPE_PARAM, urlEncode(scope));
		}
		if (claims != null) {
			uri.setParameter(AUTH_OAUTH_2_CLAIMS_PARAM, urlEncode(claims));
		}
		return postJSONEntity(getAuthEndpoint(), uri.toString(), null, OIDCTokenResponse.class);
		
	}

	
	/**
	 * Get the user information for the user specified by the authorization
	 * bearer token (which must be included as the authorization header).
	 * 
	 * The result is expected to be a JWT token, which is invoked by the 
	 * client having registered a 'user info signed response algorithm'.
	 * 
	 * @return
	 */
	@Override
	public Jwt<JwsHeader,Claims> getUserInfoAsJSONWebToken() throws SynapseException {
		Map<String,String> requestHeaders = new HashMap<String,String>();
		requestHeaders.put(AuthorizationConstants.AUTHORIZATION_HEADER_NAME, getAuthorizationHeader());
		SimpleHttpResponse response = signAndDispatchSynapseRequest(
				getAuthEndpoint(), AUTH_OAUTH_2_USER_INFO, GET, null, requestHeaders, null);
		if (!ClientUtils.is200sStatusCode(response.getStatusCode())) {
			ClientUtils.throwException(response.getStatusCode(), response.getContent());
		}
		
		validateContentType(response, APPLICATION_JWT);
		
		String signedToken = response.getContent();
		
		return JSONWebTokenHelper.parseJWT(signedToken, getOIDCJsonWebKeySet());
	}
	
	/**
	 * Get the user information for the user specified by the authorization
	 * bearer token (which must be included as the authorization header).
	 * 
	 * The result is expected to be a Map, which is invoked by the 
	 * client having omitted a 'user info signed response algorithm'.
	 * 
	 * @return
	 */
	@Override
	public JSONObject getUserInfoAsJSON() throws SynapseException {
		return getJson(getAuthEndpoint(), AUTH_OAUTH_2_USER_INFO);
	}

	@Override
	public OAuthClientAuthorizationHistoryList getClientAuthorizationHistory(String nextPageToken) throws SynapseException {
		return getJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_AUDIT_CLIENTS, OAuthClientAuthorizationHistoryList.class);
	}

	@Override
	public OAuthRefreshTokenInformationList getRefreshTokenMetadataForAuthorizedClient(String clientId, String nextPageToken) throws SynapseException {
		ValidateArgument.required(clientId, "clientId");
		return getJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_AUDIT_CLIENTS + "/" + clientId + TOKENS, OAuthRefreshTokenInformationList.class);
	}

	@Override
	public void revokeMyRefreshTokensFromClient(String clientId) throws SynapseException {
		ValidateArgument.required(clientId, "clientId");
		voidPost(getAuthEndpoint(), AUTH_OAUTH_2_AUDIT_CLIENTS + "/" + clientId + REVOKE, null, null);
	}

	@Override
	public void revokeRefreshToken(String refreshTokenId) throws SynapseException {
		ValidateArgument.required(refreshTokenId, "refreshTokenId");
		voidPost(getAuthEndpoint(), AUTH_OAUTH_2_AUDIT_TOKENS + "/" + refreshTokenId + REVOKE, null,null);
	}

	@Override
	public void revokeToken(OAuthTokenRevocationRequest revocationRequest) throws SynapseException {
		ValidateArgument.required(revocationRequest, "revocationRequest");
		voidPost(getAuthEndpoint(), AUTH_OAUTH_2 + REVOKE, revocationRequest, null);
	}

	@Override
	public OAuthRefreshTokenInformation updateRefreshTokenMetadata(OAuthRefreshTokenInformation metadata) throws SynapseException {
		ValidateArgument.required(metadata, "metadata");
		ValidateArgument.required(metadata.getTokenId(), "tokenId");
		return putJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_AUDIT_TOKENS + "/" + metadata.getTokenId() + METADATA, metadata, OAuthRefreshTokenInformation.class);
	}

	@Override
	public OAuthRefreshTokenInformation getRefreshTokenMetadata(String tokenId) throws SynapseException {
		ValidateArgument.required(tokenId, "tokenId");
		return getJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_AUDIT_TOKENS + "/" + tokenId + METADATA, OAuthRefreshTokenInformation.class);
	}

	@Override
	public OAuthRefreshTokenInformation getRefreshTokenMetadataAsOAuthClient(String tokenId) throws SynapseException {
		ValidateArgument.required(tokenId, "tokenId");
		return getJSONEntity(getAuthEndpoint(), AUTH_OAUTH_2_TOKEN + "/" + tokenId + METADATA, OAuthRefreshTokenInformation.class);
	}

	@Override
	public Quiz getCertifiedUserTest() throws SynapseException {
		return getJSONEntity(getRepoEndpoint(), CERTIFIED_USER_TEST, Quiz.class);
	}

	@Override
	public PassingRecord submitCertifiedUserTestResponse(QuizResponse response)
			throws SynapseException {
		return postJSONEntity(getRepoEndpoint(), CERTIFIED_USER_TEST_RESPONSE,
				response, PassingRecord.class);
	}

	@Override
	public PassingRecord getCertifiedUserPassingRecord(String principalId)
			throws SynapseException {
		ValidateArgument.required(principalId, "principalId");
		String url = USER + "/" + principalId + CERTIFIED_USER_PASSING_RECORD;
		return getJSONEntity(getRepoEndpoint(), url, PassingRecord.class);
	}


	@Override
	public Challenge createChallenge(Challenge challenge) throws SynapseException {
		return postJSONEntity(getRepoEndpoint(), CHALLENGE, challenge, Challenge.class);
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
		String url = CHALLENGE+"/"+challengeId;
		return getJSONEntity(getRepoEndpoint(), url, Challenge.class);
	}

	@Override
	public Challenge getChallengeForProject(String projectId) throws SynapseException {
		ValidateArgument.required(projectId, "projectId");
		String url = ENTITY+"/"+projectId+CHALLENGE;
		return getJSONEntity(getRepoEndpoint(), url, Challenge.class);
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
		return getJSONEntity(getRepoEndpoint(), uri, PaginatedIds.class);
	}
	
	@Override
	public ChallengePagedResults listChallengesForParticipant(String participantPrincipalId, Long limit, Long offset) throws SynapseException {
		validateStringAsLong(participantPrincipalId);
		String uri = CHALLENGE+"?participantId="+participantPrincipalId;
		if  (limit!=null) {
			uri+=	"&"+LIMIT+"="+limit;
		}
		if  (offset!=null) {
			uri+="&"+OFFSET+"="+offset;
		}
		return getJSONEntity(getRepoEndpoint(), uri, ChallengePagedResults.class);
	}
	
	@Override
	public Challenge updateChallenge(Challenge challenge) throws SynapseException {
		String uri = CHALLENGE+"/"+challenge.getId();
		return putJSONEntity(getRepoEndpoint(), uri,challenge, Challenge.class);
	}

	
	@Override
	public void deleteChallenge(String id) throws SynapseException {
		deleteUri(getRepoEndpoint(), CHALLENGE + "/" + id);
	}

	
	/**
	 * Register a Team for a Challenge. The user making this request must be
	 * registered for the Challenge and be an administrator of the Team.
	 * 
	 * @param challengeTeam
	 * @throws SynapseException
	 */
	@Override
	public ChallengeTeam createChallengeTeam(ChallengeTeam challengeTeam) throws SynapseException {
		String uri = CHALLENGE+"/"+challengeTeam.getChallengeId()+CHALLENGE_TEAM;
		return postJSONEntity(getRepoEndpoint(), uri, challengeTeam, ChallengeTeam.class);
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
		return getJSONEntity(getRepoEndpoint(), uri, ChallengeTeamPagedResults.class);
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
		return getJSONEntity(getRepoEndpoint(), uri, PaginatedIds.class);
	}
	
	@Override
	public PaginatedIds listSubmissionTeams(String challengeId, String submitterPrincipalId, Long limit, Long offset) throws SynapseException {
		validateStringAsLong(challengeId);
		validateStringAsLong(submitterPrincipalId);
		String uri = CHALLENGE+"/"+challengeId+SUBMISSION_TEAMS+"?submitterPrincipalId="+submitterPrincipalId;
		if  (limit!=null) {
			uri += "&"+LIMIT+"="+limit;
		}
		if  (offset!=null) {
			uri += "&"+OFFSET+"="+offset;
		}
		return getJSONEntity(getRepoEndpoint(), uri, PaginatedIds.class);
	}
	
	@Override
	public ChallengeTeam updateChallengeTeam(ChallengeTeam challengeTeam) throws SynapseException {
		ValidateArgument.required(challengeTeam, "challengeTeam");
		String challengeId = challengeTeam.getChallengeId();
		ValidateArgument.required(challengeId, "challengeId");
		String challengeTeamId = challengeTeam.getId();
		ValidateArgument.required(challengeTeamId, "challengeTeamId");
		String uri = CHALLENGE+"/"+challengeId+CHALLENGE_TEAM+"/"+challengeTeamId;
		return putJSONEntity(getRepoEndpoint(), uri, challengeTeam, ChallengeTeam.class);
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
		deleteUri(getRepoEndpoint(), CHALLENGE_TEAM + "/" + challengeTeamId);
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
		return postJSONEntity(getRepoEndpoint(), uri, verificationSubmission, VerificationSubmission.class);
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
		return getJSONEntity(getRepoEndpoint(), uri, VerificationPagedResults.class);
	}

	@Override
	public void updateVerificationState(long verificationId,
			VerificationState verificationState,
			String notificationUnsubscribeEndpoint) throws SynapseException {
		String uri = VERIFICATION_SUBMISSION+"/"+verificationId+VERIFICATION_STATE;
		if (notificationUnsubscribeEndpoint!=null) {
			uri += "?" + NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM + "=" + urlEncode(notificationUnsubscribeEndpoint);
		}
		voidPost(getRepoEndpoint(), uri, verificationState, null);
	}

	@Override
	public void deleteVerificationSubmission(long verificationId) throws SynapseException {
		deleteUri(getRepoEndpoint(), VERIFICATION_SUBMISSION+"/"+verificationId);
	}

	@Override
	public UserBundle getMyOwnUserBundle(int mask) throws SynapseException {
		String url = USER+USER_BUNDLE+"?mask="+mask;
		return getJSONEntity(getRepoEndpoint(), url, UserBundle.class);
	}

	@Override
	public UserBundle getUserBundle(long principalId, int mask)
			throws SynapseException {
		String url = USER+"/"+principalId+USER_BUNDLE+"?mask="+mask;
		return getJSONEntity(getRepoEndpoint(), url, UserBundle.class);
	}
	
	private static String createFileDownloadUri(FileHandleAssociation fileHandleAssociation, boolean redirect) {
		return FILE + "/" + fileHandleAssociation.getFileHandleId() + "?" +
				FILE_ASSOCIATE_TYPE + "=" + fileHandleAssociation.getAssociateObjectType() +
		"&" + FILE_ASSOCIATE_ID + "=" + fileHandleAssociation.getAssociateObjectId() +
		"&" + REDIRECT_PARAMETER + redirect;
	}

	@Override
	public URL getFileURL(FileHandleAssociation fileHandleAssociation) throws SynapseException {
		return getUrl(getFileEndpoint(), createFileDownloadUri(fileHandleAssociation, false));
	}

	@Override
	public void downloadFile(FileHandleAssociation fileHandleAssociation, File target)
			throws SynapseException {
		String uri = createFileDownloadUri(fileHandleAssociation, true);
		downloadFromSynapse(getFileEndpoint() + uri, null, target);
	}

	@Override
	public Forum getForumByProjectId(String projectId) throws SynapseException {
		ValidateArgument.required(projectId, "projectId");
		return getJSONEntity(getRepoEndpoint(), PROJECT+"/"+projectId+FORUM, Forum.class);
	}

	@Override
	public Forum getForum(String forumId) throws SynapseException {
		ValidateArgument.required(forumId, "forumId");
		return getJSONEntity(getRepoEndpoint(), FORUM+"/"+forumId, Forum.class);
	}

	@Override
	public DiscussionThreadBundle createThread(CreateDiscussionThread toCreate)
			throws SynapseException {
		ValidateArgument.required(toCreate, "toCreate");
		return postJSONEntity(getRepoEndpoint(), THREAD, toCreate, DiscussionThreadBundle.class);
	}

	@Override
	public DiscussionThreadBundle getThread(String threadId) throws SynapseException {
		ValidateArgument.required(threadId, "threadId");
		String url = THREAD+"/"+threadId;
		return getJSONEntity(getRepoEndpoint(), url, DiscussionThreadBundle.class);
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
		return getPaginatedResults(getRepoEndpoint(), url, DiscussionThreadBundle.class);
	}

	@Override
	public DiscussionThreadBundle updateThreadTitle(String threadId,
			UpdateThreadTitle newTitle) throws SynapseException {
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(newTitle, "newTitle");
		return putJSONEntity(getRepoEndpoint(), THREAD+"/"+threadId+THREAD_TITLE, newTitle, DiscussionThreadBundle.class);
	}

	@Override
	public DiscussionThreadBundle updateThreadMessage(String threadId,
			UpdateThreadMessage newMessage) throws SynapseException {
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(newMessage, "newMessage");
		return putJSONEntity(getRepoEndpoint(), THREAD+"/"+threadId+DISCUSSION_MESSAGE, newMessage, DiscussionThreadBundle.class);
	}

	@Override
	public void markThreadAsDeleted(String threadId) throws SynapseException {
		deleteUri(getRepoEndpoint(), THREAD+"/"+threadId);
	}

	@Override
	public void restoreDeletedThread(String threadId) throws SynapseException {
		putUri(getRepoEndpoint(), THREAD+"/"+threadId+RESTORE);
	}

	@Override
	public DiscussionReplyBundle createReply(CreateDiscussionReply toCreate)
			throws SynapseException {
		ValidateArgument.required(toCreate, "toCreate");
		return postJSONEntity(getRepoEndpoint(), REPLY, toCreate, DiscussionReplyBundle.class);
	}

	@Override
	public DiscussionReplyBundle getReply(String replyId)
			throws SynapseException {
		ValidateArgument.required(replyId, "replyId");
		return getJSONEntity(getRepoEndpoint(), REPLY+"/"+replyId, DiscussionReplyBundle.class);
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
		return getPaginatedResults(getRepoEndpoint(), url, DiscussionReplyBundle.class);
	}

	@Override
	public DiscussionReplyBundle updateReplyMessage(String replyId,
			UpdateReplyMessage newMessage) throws SynapseException {
		ValidateArgument.required(replyId, "replyId");
		ValidateArgument.required(newMessage, "newMessage");
		return putJSONEntity(getRepoEndpoint(), REPLY+"/"+replyId+DISCUSSION_MESSAGE, newMessage, DiscussionReplyBundle.class);
	}

	@Override
	public void markReplyAsDeleted(String replyId) throws SynapseException {
		deleteUri(getRepoEndpoint(), REPLY+"/"+replyId);
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
		return postJSONEntity(getFileEndpoint(), pathBuilder.toString(), request, MultipartUploadStatus.class);
	}

	@Override
	public BatchPresignedUploadUrlResponse getMultipartPresignedUrlBatch(
			BatchPresignedUploadUrlRequest request) throws SynapseException {
		ValidateArgument.required(request, "BatchPresignedUploadUrlRequest");
		ValidateArgument.required(request.getUploadId(), "BatchPresignedUploadUrlRequest.uploadId");
		String path = String.format("/file/multipart/%1$s/presigned/url/batch", request.getUploadId());
		return postJSONEntity(getFileEndpoint(),path,request, BatchPresignedUploadUrlResponse.class);
	}

	@Override
	public AddPartResponse addPartToMultipartUpload(String uploadId,
			int partNumber, String partMD5Hex) throws SynapseException {
		ValidateArgument.required(uploadId, "uploadId");
		ValidateArgument.required(partMD5Hex, "partMD5Hex");
		String path = String.format("/file/multipart/%1$s/add/%2$d?partMD5Hex=%3$s", uploadId, partNumber, partMD5Hex);
		return putJSONEntity(getFileEndpoint(), path, null, AddPartResponse.class);
	}

	@Override
	public MultipartUploadStatus completeMultipartUpload(String uploadId) throws SynapseException {
		ValidateArgument.required(uploadId, "uploadId");
		String path = String.format("/file/multipart/%1$s/complete", uploadId);
		return putJSONEntity(getFileEndpoint(), path, null, MultipartUploadStatus.class);
	}

	@Override
	public CloudProviderFileHandleInterface multipartUpload(InputStream input, long fileSize, String fileName,
			String contentType, Long storageLocationId, Boolean generatePreview, Boolean forceRestart) throws SynapseException {
		return new MultipartUpload(this, input, fileSize, fileName, contentType, storageLocationId, generatePreview, forceRestart, new FileProviderImpl()).uploadFile();
	}


	@Override
	public CloudProviderFileHandleInterface multipartUpload(File file,
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
			String url = REPLY+URL+"?messageKey="+messageKey;
			return new URL(getJSONEntity(getRepoEndpoint(), url, MessageURL.class).getMessageUrl());
		} catch (MalformedURLException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public URL getThreadUrl(String messageKey) throws SynapseException {
		try {
			ValidateArgument.required(messageKey, "messageKey");
			String url = THREAD+URL+"?messageKey="+messageKey;
			return new URL(getJSONEntity(getRepoEndpoint(), url, MessageURL.class).getMessageUrl());
		} catch (MalformedURLException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public Subscription subscribe(Topic toSubscribe) throws SynapseException {
		ValidateArgument.required(toSubscribe, "toSubscribe");
		return postJSONEntity(getRepoEndpoint(), SUBSCRIPTION, toSubscribe, Subscription.class);
	}

	@Override
	public Subscription subscribeAll(SubscriptionObjectType toSubscribe) throws SynapseException {
		ValidateArgument.required(toSubscribe, "toSubscribe");
		String url = SUBSCRIPTION+ALL+"?"+OBJECT_TYPE_PARAM+"="+toSubscribe;
		return postJSONEntity(getRepoEndpoint(), url, null, Subscription.class);
	}

	@Override
	public SubscriptionPagedResults getAllSubscriptions(
			SubscriptionObjectType objectType, Long limit, Long offset, SortByType sortByType, org.sagebionetworks.repo.model.subscription.SortDirection sortDirection) throws SynapseException {
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		ValidateArgument.required(objectType, "objectType");
		StringBuilder builder = new StringBuilder(SUBSCRIPTION+ALL);
		builder.append("?");
		builder.append(LIMIT).append("=").append(limit);
		builder.append("&").append(OFFSET).append("=").append(offset);
		builder.append("&").append(OBJECT_TYPE_PARAM).append("=").append(objectType.name());
		if(sortByType != null) {
			builder.append("&").append("sortBy").append("=").append(sortByType.name());
		}
		if(sortDirection != null) {
			builder.append("&").append("sortDirection").append("=").append(sortDirection.name());
		}
		return getJSONEntity(getRepoEndpoint(), builder.toString(), SubscriptionPagedResults.class);
	}

	@Override
	public SubscriptionPagedResults listSubscriptions(SubscriptionRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getObjectType(), "SubscriptionRequest.objectType");
		ValidateArgument.required(request.getIdList(), "SubscriptionRequest.idList");
		return postJSONEntity(getRepoEndpoint(), SUBSCRIPTION+LIST, request, SubscriptionPagedResults.class);
	}

	@Override
	public void unsubscribe(Long subscriptionId) throws SynapseException {
		ValidateArgument.required(subscriptionId, "subscriptionId");
		deleteUri(getRepoEndpoint(), SUBSCRIPTION+"/"+subscriptionId);
	}

	@Override
	public void unsubscribeAll() throws SynapseException {
		deleteUri(getRepoEndpoint(), SUBSCRIPTION+ALL);
	}

	@Override
	public Subscription getSubscription(String subscriptionId) throws SynapseException {
		ValidateArgument.required(subscriptionId, "subscriptionId");
		return getJSONEntity(getRepoEndpoint(), SUBSCRIPTION+"/"+subscriptionId, Subscription.class);
	}

	@Override
	public EntityId getEntityIdByAlias(String alias) throws SynapseException {
		ValidateArgument.required(alias, "alias");
		String url = ENTITY+"/alias/"+alias;
		return getJSONEntity(getRepoEndpoint(), url, EntityId.class);
	}
	
	@Override
	public EntityChildrenResponse getEntityChildren(EntityChildrenRequest request) throws SynapseException{
		ValidateArgument.required(request, "request");
		String url = ENTITY+"/children";
		return postJSONEntity(getRepoEndpoint(), url, request, EntityChildrenResponse.class);
	}

	@Override
	public ThreadCount getThreadCountForForum(String forumId, DiscussionFilter filter) throws SynapseException {
		ValidateArgument.required(forumId, "forumId");
		ValidateArgument.required(filter, "filter");
		String url = FORUM+"/"+forumId+THREAD_COUNT;
		url += "?filter="+filter;
		return getJSONEntity(getRepoEndpoint(), url, ThreadCount.class);
	}

	@Override
	public ReplyCount getReplyCountForThread(String threadId, DiscussionFilter filter) throws SynapseException {
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(filter, "filter");
		String url = THREAD+"/"+threadId+REPLY_COUNT;
		url += "?filter="+filter;
		return getJSONEntity(getRepoEndpoint(), url, ReplyCount.class);
	}

	@Override
	public void pinThread(String threadId) throws SynapseException {
		putUri(getRepoEndpoint(), THREAD+"/"+threadId+PIN);
	}

	@Override
	public void unpinThread(String threadId) throws SynapseException {
		putUri(getRepoEndpoint(), THREAD+"/"+threadId+UNPIN);
	}

	@Override
	public PrincipalAliasResponse getPrincipalAlias(PrincipalAliasRequest request) throws SynapseException {
		return postJSONEntity(getRepoEndpoint(), PRINCIPAL+"/alias/", request, PrincipalAliasResponse.class);
	}

	@Override
	public void addDockerCommit(String entityId, DockerCommit dockerCommit) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		voidPost(getRepoEndpoint(), ENTITY+"/"+entityId+DOCKER_COMMIT, dockerCommit, null);
	}

	@Override
	public PaginatedResults<DockerCommit> listDockerTags(
			String entityId, Long limit, Long offset, DockerCommitSortBy sortBy, Boolean ascending) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		String url = ENTITY+"/"+entityId+DOCKER_TAG;
		List<String> requestParams = new ArrayList<String>();
		if (limit!=null) {
			requestParams.add(LIMIT+"="+limit);
		}
		if (offset!=null) {
			requestParams.add(OFFSET+"="+offset);
		}
		if (sortBy!=null) {
			requestParams.add("sort="+sortBy.name());
		}
		if (ascending!=null) {
			requestParams.add("ascending="+ascending);
		}
		if (!requestParams.isEmpty()) {
			url += "?" + Joiner.on('&').join(requestParams);
		}

		return getPaginatedResults(getRepoEndpoint(), url, DockerCommit.class);
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreadsForEntity(String entityId, Long limit, Long offset,
			DiscussionThreadOrder order, Boolean ascending, DiscussionFilter filter) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		ValidateArgument.required(filter, "filter");
		String url = ENTITY+"/"+entityId+THREADS
				+"?"+LIMIT+"="+limit+"&"+OFFSET+"="+offset;
		if (order != null) {
			url += "&sort="+order.name();
		}
		if (ascending != null) {
			url += "&ascending="+ascending;
		}
		url += "&filter="+filter.toString();
		return getPaginatedResults(getRepoEndpoint(), url, DiscussionThreadBundle.class);
	}

	@Override
	public EntityThreadCounts getEntityThreadCount(List<String> entityIds) throws SynapseException {
		EntityIdList idList = new EntityIdList();
		idList.setIdList(entityIds);
		return postJSONEntity(getRepoEndpoint(), ENTITY_THREAD_COUNTS, idList , EntityThreadCounts.class);
	}

	@Override
	public PaginatedIds getModeratorsForForum(String forumId, Long limit, Long offset) throws SynapseException {
		ValidateArgument.required(forumId, "forumId");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		String url = FORUM+"/"+forumId+MODERATORS
				+"?"+LIMIT+"="+limit+"&"+OFFSET+"="+offset;
		return getJSONEntity(getRepoEndpoint(), url, PaginatedIds.class);
	}

	@Override
	public BatchFileResult getFileHandleAndUrlBatch(BatchFileRequest request) throws SynapseException {
		return postJSONEntity(getFileEndpoint(), FILE_HANDLE_BATCH, request , BatchFileResult.class);
	}

	@Override
	public BatchFileHandleCopyResult copyFileHandles(BatchFileHandleCopyRequest request) throws SynapseException {
		return postJSONEntity(getFileEndpoint(), FILE_HANDLES_COPY, request , BatchFileHandleCopyResult.class);
	}

	@Override
	public void requestToCancelSubmission(String submissionId) throws SynapseException {
		putUri(getRepoEndpoint(), EVALUATION_URI_PATH+"/"+SUBMISSION+"/"+submissionId+"/cancellation");
	}
	
	@Override
	public ColumnModelPage getPossibleColumnModelsForViewScope(ViewScope scope, String nextPageToken) throws SynapseException{
		StringBuilder url = new StringBuilder("/column/view/scope");
		if(nextPageToken != null){
			url.append("?nextPageToken=");
			url.append(nextPageToken);
		}
		return postJSONEntity(getRepoEndpoint(), url.toString(), scope, ColumnModelPage.class);
	}
	
	@Override
	public String startGetPossibleColumnModelsForViewScope(ViewColumnModelRequest request) throws SynapseException {
		return startAsynchJob(AsynchJobType.ViewColumnModelRequest, request);
	}
	
	@Override
	public ViewColumnModelResponse getPossibleColumnModelsForViewScopeResult(String asyncJobToken)
			throws SynapseException {
		ViewColumnModelResponse response = (ViewColumnModelResponse) getAsyncResult(AsynchJobType.ViewColumnModelRequest, asyncJobToken);
		return response;
	}
	
	@Override
	public String transformSqlRequest(SqlTransformRequest request) throws SynapseException {
		SqlTransformResponse response = postJSONEntity(getRepoEndpoint(), "/table/sql/transform", request,
				SqlTransformResponse.class);
		return response.getTransformedSql();
	}

	@Override
	public SubscriberPagedResults getSubscribers(Topic topic, String nextPageToken) throws SynapseException {
		String url = SUBSCRIPTION+"/subscribers";
		if (nextPageToken != null) {
			url += "?" + NEXT_PAGE_TOKEN_PARAM + nextPageToken;
		}
		return postJSONEntity(getRepoEndpoint(), url, topic, SubscriberPagedResults.class);
	}

	@Override
	public SubscriberCount getSubscriberCount(Topic topic) throws SynapseException {
		return postJSONEntity(getRepoEndpoint(), SUBSCRIPTION+"/subscribers/count", topic, SubscriberCount.class);
	}

	@Override
	public ResearchProject createOrUpdateResearchProject(ResearchProject toCreateOrUpdate) throws SynapseException {
		ValidateArgument.required(toCreateOrUpdate, "toCreateOrUpdate");
		return postJSONEntity(getRepoEndpoint(), RESEARCH_PROJECT, toCreateOrUpdate, ResearchProject.class);
	}

	@Override
	public ResearchProject getResearchProjectForUpdate(String accessRequirementId) throws SynapseException {
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		String url = ACCESS_REQUIREMENT + "/" + accessRequirementId + "/researchProjectForUpdate";
		return getJSONEntity(getRepoEndpoint(), url, ResearchProject.class);
	}

	@Override
	public RequestInterface createOrUpdateRequest(RequestInterface toCreateOrUpdate)
			throws SynapseException {
		ValidateArgument.required(toCreateOrUpdate, "toCreateOrUpdate");
		return postJSONEntity(getRepoEndpoint(), DATA_ACCESS_REQUEST, toCreateOrUpdate, Request.class);
	}

	@Override
	public RequestInterface getRequestForUpdate(String accessRequirementId) throws SynapseException {
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		String url = ACCESS_REQUIREMENT + "/" + accessRequirementId + "/dataAccessRequestForUpdate";
		return getJSONEntity(getRepoEndpoint(), url, RequestInterface.class);
	}

	@Override
	public org.sagebionetworks.repo.model.dataaccess.SubmissionStatus submitRequest(CreateSubmissionRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getRequestId(), "requestId");
		String url = DATA_ACCESS_REQUEST+"/"+request.getRequestId()+"/submission";
		return postJSONEntity(getRepoEndpoint(), url, request, org.sagebionetworks.repo.model.dataaccess.SubmissionStatus.class);
	}

	@Override
	public org.sagebionetworks.repo.model.dataaccess.SubmissionStatus cancelSubmission(String submissionId) throws SynapseException {
		ValidateArgument.required(submissionId, "submissionId");
		String url = DATA_ACCESS_SUBMISSION+"/"+submissionId+"/cancellation";
		return putJSONEntity(getRepoEndpoint(), url, null, org.sagebionetworks.repo.model.dataaccess.SubmissionStatus.class);
	}

	@Override
	public void deleteDataAccessSubmission(String submissionId) throws SynapseException {
		ValidateArgument.required(submissionId, "submissionId");
		String url = DATA_ACCESS_SUBMISSION+"/"+submissionId;
		deleteUri(getRepoEndpoint(), url);
	}

	@Override
	public org.sagebionetworks.repo.model.dataaccess.Submission updateSubmissionState(String submissionId, SubmissionState newState, String reason)
			throws SynapseException {
		ValidateArgument.required(submissionId, "submissionId");
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(newState);
		request.setRejectedReason(reason);
		String url = DATA_ACCESS_SUBMISSION+"/"+submissionId;
		return putJSONEntity(getRepoEndpoint(), url, request, org.sagebionetworks.repo.model.dataaccess.Submission.class);
	}

	@Override
	public SubmissionPage listSubmissions(String requirementId, String nextPageToken,
			SubmissionState filter, SubmissionOrder order, Boolean isAscending)
			throws SynapseException {
		ValidateArgument.required(requirementId, "requirementId");
		SubmissionPageRequest request = new SubmissionPageRequest();
		request.setAccessRequirementId(requirementId);
		request.setFilterBy(filter);
		request.setOrderBy(order);
		request.setIsAscending(isAscending);
		request.setNextPageToken(nextPageToken);
		String url = ACCESS_REQUIREMENT + "/" + requirementId + "/submissions";
		return postJSONEntity(getRepoEndpoint(), url, request, SubmissionPage.class);
	}

	@Override
	public SubmissionInfoPage listApprovedSubmissionInfo(String requirementId, String nextPageToken) throws SynapseException {
		ValidateArgument.required(requirementId, "requirementId");
		SubmissionInfoPageRequest request = new SubmissionInfoPageRequest();
		request.setAccessRequirementId(requirementId);
		request.setNextPageToken(nextPageToken);
		String url = ACCESS_REQUIREMENT + "/" + requirementId + "/approvedSubmissionInfo";
		return postJSONEntity(getRepoEndpoint(), url, request, SubmissionInfoPage.class);
		
	}
	
	@Override
	public AccessRequirementStatus getAccessRequirementStatus(String requirementId) throws SynapseException {
		ValidateArgument.required(requirementId, "requirementId");
		String url = ACCESS_REQUIREMENT + "/" + requirementId + "/status";
		return getJSONEntity(getRepoEndpoint(), url, AccessRequirementStatus.class);
	}

	@Override
	public RestrictionInformationResponse getRestrictionInformation(RestrictionInformationRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		return postJSONEntity(getRepoEndpoint(), "/restrictionInformation", request, RestrictionInformationResponse.class);
	}

	@Override
	public OpenSubmissionPage getOpenSubmissions(String nextPageToken) throws SynapseException {
		String url = DATA_ACCESS_SUBMISSION+"/openSubmissions";
		if (nextPageToken != null) {
			url += "?nextPageToken="+nextPageToken;
		}
		return getJSONEntity(getRepoEndpoint(), url, OpenSubmissionPage.class);
	}

	@Override
	public String lookupChild(String parentId, String entityName) throws SynapseException {
		ValidateArgument.required(parentId, "parentId");
		ValidateArgument.required(entityName, "entityName");
		EntityLookupRequest request = new EntityLookupRequest();
		request.setEntityName(entityName);
		request.setParentId(parentId);
		return postJSONEntity(getRepoEndpoint(), ENTITY+"/child", request, EntityId.class).getId();
	}

	@Override
	public AccessorGroupResponse listAccessorGroup(AccessorGroupRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		return postJSONEntity(getRepoEndpoint(), ACCESS_APPROVAL+"/group", request, AccessorGroupResponse.class);
	}

	@Override
	public void revokeGroup(String accessRequirementId, String submitterId) throws SynapseException {
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(submitterId, "submitterId");
		AccessorGroupRevokeRequest request = new AccessorGroupRevokeRequest();
		request.setAccessRequirementId(accessRequirementId);
		request.setSubmitterId(submitterId);
		voidPut(getRepoEndpoint(), ACCESS_APPROVAL+"/group/revoke", request);
	}

	@Override
	public AccessRequirement convertAccessRequirement(AccessRequirementConversionRequest request)
			throws SynapseException {
		ValidateArgument.required(request, "request");
		return putJSONEntity(getRepoEndpoint(), ACCESS_REQUIREMENT+"/conversion", request, AccessRequirement.class);
	}

	@Override
	public BatchAccessApprovalInfoResponse getBatchAccessApprovalInfo(BatchAccessApprovalInfoRequest request)
			throws SynapseException {
		ValidateArgument.required(request, "request");
		return postJSONEntity(getRepoEndpoint(), ACCESS_APPROVAL+"/information", request, BatchAccessApprovalInfoResponse.class);
	}
	
	@Override
	public AccessApprovalNotificationResponse getAccessApprovalNotifications(AccessApprovalNotificationRequest request)
			throws SynapseException {
		ValidateArgument.required(request, "request");
		return postJSONEntity(getRepoEndpoint(), ACCESS_APPROVAL+"/notifications", request, AccessApprovalNotificationResponse.class);
	}

	@Override
	public RestrictableObjectDescriptorResponse getSubjects(String requirementId, String nextPageToken)
			throws SynapseException {
		ValidateArgument.required(requirementId, "requirementId");
		String uri = ACCESS_REQUIREMENT+"/"+requirementId+"/subjects";
		if (nextPageToken != null) {
			uri += "?nextPageToken="+nextPageToken;
		}
		return getJSONEntity(getRepoEndpoint(), uri, RestrictableObjectDescriptorResponse.class);
	}

	@Override
	public String startAddFilesToDownloadList(AddFileToDownloadListRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		return startAsynchJob(AsynchJobType.AddFileToDownloadList, request);
	}

	@Override
	public AddFileToDownloadListResponse getAddFilesToDownloadListResponse(String asyncJobToken)
			throws SynapseException, SynapseResultNotReadyException {
		ValidateArgument.required(asyncJobToken, "asyncJobToken");
		return (AddFileToDownloadListResponse) getAsyncResult(AsynchJobType.AddFileToDownloadList, asyncJobToken, (String) null);
	}

	@Override
	public DownloadList addFilesToDownloadList(List<FileHandleAssociation> toAdd) throws SynapseException {
		ValidateArgument.required(toAdd, "toAdd");
		FileHandleAssociationList request = new FileHandleAssociationList();
		request.setList(toAdd);
		return postJSONEntity(getFileEndpoint(), DOWNLOAD_LIST_ADD, request, DownloadList.class);
	}

	@Override
	public DownloadList removeFilesFromDownloadList(List<FileHandleAssociation> toRemove) throws SynapseException {
		ValidateArgument.required(toRemove, "toRemove");
		FileHandleAssociationList request = new FileHandleAssociationList();
		request.setList(toRemove);
		return postJSONEntity(getFileEndpoint(), DOWNLOAD_LIST_REMOVE, request, DownloadList.class);
	}

	@Override
	public void clearDownloadList() throws SynapseException {
		deleteUri(getFileEndpoint(), DOWNLOAD_LIST);
	}
	
	@Override
	public DownloadList getDownloadList() throws SynapseException {
		return getJSONEntity(getFileEndpoint(), DOWNLOAD_LIST, DownloadList.class);
	}

	@Override
	public DownloadOrder createDownloadOrderFromUsersDownloadList(String zipFileName) throws SynapseException {
		ValidateArgument.required(zipFileName, "zipFileName");
		String url = DOWNLOAD_ORDER+"?zipFileName="+zipFileName;
		return postJSONEntity(getFileEndpoint(), url, null, DownloadOrder.class);
	}

	@Override
	public DownloadOrder getDownloadOrder(String orderId) throws SynapseException {
		ValidateArgument.required(orderId, "orderId");
		String url = DOWNLOAD_ORDER+"/"+orderId;
		return getJSONEntity(getFileEndpoint(), url, DownloadOrder.class);
	}

	@Override
	public DownloadOrderSummaryResponse getDownloadOrderHistory(DownloadOrderSummaryRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		return postJSONEntity(getFileEndpoint(), DOWNLOAD_ORDER_HISTORY, request, DownloadOrderSummaryResponse.class);
	}

	@Override
	public DataTypeResponse changeEntitysDataType(String entityId, DataType newDataType) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(newDataType, "newDataType");
		String url = ENTITY + "/" + entityId + "/datatype?type="+newDataType.name();
		return putJSONEntity(getRepoEndpoint(), url, null, DataTypeResponse.class);
	}

	@Override
	public String generateStorageReportAsyncStart(StorageReportType reportType) throws SynapseException {
		DownloadStorageReportRequest request = new DownloadStorageReportRequest();
		request.setReportType(reportType);
		return startAsynchJob(AsynchJobType.DownloadStorageReport, request);
	}

	@Override
	public DownloadStorageReportResponse generateStorageReportAsyncGet(String asyncJobToken) throws SynapseException {
		String url = STORAGE_REPORT + ASYNC_GET + asyncJobToken;
		return getJSONEntity(getRepoEndpoint(), url, DownloadStorageReportResponse.class);
	}

	@Override
	public SnapshotResponse createTableSnapshot(String tableId, SnapshotRequest request) throws SynapseException {
		ValidateArgument.required(tableId, "tableId");
		String url = "/entity/"+tableId+"/table/snapshot";
		return postJSONEntity(getRepoEndpoint(), url, request, SnapshotResponse.class);
	}
	
	@Override
	public ObjectStatisticsResponse getStatistics(ObjectStatisticsRequest request) throws SynapseException {
		ValidateArgument.required(request, "The request body");
		return postJSONEntity(getRepoEndpoint(), STATISTICS, request, ObjectStatisticsResponse.class);
	}

	@Override
	public FormGroup createFormGroup(String name) throws SynapseException {
		ValidateArgument.required(name, "name");
		String url = "/form/group?name="+name;
		return postJSONEntity(getRepoEndpoint(), url, null, FormGroup.class);
	}
	
	@Override
	public FormGroup getFormGroup(String id) throws SynapseException {
		ValidateArgument.required(id, "id");
		String url = "/form/group/"+id;
		return getJSONEntity(getRepoEndpoint(), url, FormGroup.class);
	}

	@Override
	public AccessControlList getFormGroupAcl(String formGroupId) throws SynapseException {
		ValidateArgument.required(formGroupId, "formGroupId");
		String url = "/form/group/"+formGroupId+"/acl";
		return getJSONEntity(getRepoEndpoint(), url, AccessControlList.class);
	}

	@Override
	public AccessControlList updateFormGroupAcl(AccessControlList acl) throws SynapseException {
		ValidateArgument.required(acl, "acl");
		ValidateArgument.required(acl.getId(), "acl.id");
		String url = "/form/group/"+acl.getId()+"/acl";
		return putJSONEntity(getRepoEndpoint(), url, acl, AccessControlList.class);
	}

	@Override
	public FormData createFormData(String groupId, FormChangeRequest request) throws SynapseException {
		ValidateArgument.required(groupId, "groupId");
		ValidateArgument.required(request, "request");
		String url = "/form/data?groupId="+groupId;
		return postJSONEntity(getRepoEndpoint(), url, request, FormData.class);
	}

	@Override
	public FormData updateFormData(String formId, FormChangeRequest request) throws SynapseException {
		ValidateArgument.required(formId, "formId");
		ValidateArgument.required(request, "request");
		String url = "/form/data/"+formId;
		return putJSONEntity(getRepoEndpoint(), url, request, FormData.class);
	}

	@Override
	public void deleteFormData(String formId) throws SynapseException {
		ValidateArgument.required(formId, "formId");
		String url = "/form/data/"+formId;
		deleteUri(getRepoEndpoint(), url);
	}

	@Override
	public FormData submitFormData(String formId) throws SynapseException {
		ValidateArgument.required(formId, "formId");
		String url = "/form/data/"+formId+"/submit";
		return postJSONEntity(getRepoEndpoint(), url, null, FormData.class);
	}

	@Override
	public ListResponse listFormStatusForCreator(ListRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getGroupId(), "request.groupId");
		ValidateArgument.required(request.getFilterByState(), "request.filterByState");
		String url = "/form/data/list";
		return postJSONEntity(getRepoEndpoint(), url, request, ListResponse.class);
	}

	@Override
	public ListResponse listFormStatusForReviewer(ListRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getGroupId(), "request.groupId");
		ValidateArgument.required(request.getFilterByState(), "request.filterByState");
		String url = "/form/data/list/reviewer";
		return postJSONEntity(getRepoEndpoint(), url, request, ListResponse.class);
	}

	@Override
	public FormData reviewerAcceptFormData(String formDataId) throws SynapseException {
		ValidateArgument.required(formDataId, "formDataId");
		String url = "/form/data/"+formDataId+"/accept";
		return putJSONEntity(getRepoEndpoint(), url, null, FormData.class);
	}

	@Override
	public FormData reviewerRejectFormData(String formDataId, FormRejection rejection) throws SynapseException {
		ValidateArgument.required(formDataId, "formDataId");
		String url = "/form/data/"+formDataId+"/reject";
		return putJSONEntity(getRepoEndpoint(), url, rejection, FormData.class);
	}

	@Override
	public Organization createOrganization(CreateOrganizationRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getOrganizationName(), "request.organizationName");
		String url = "/schema/organization";
		return postJSONEntity(getRepoEndpoint(), url, request, Organization.class);
	}

	@Override
	public Organization getOrganizationByName(String organizationName) throws SynapseException {
		ValidateArgument.required(organizationName, "organizationName");
		String url = "/schema/organization?name="+organizationName;
		return getJSONEntity(getRepoEndpoint(), url, Organization.class);
	}

	@Override
	public void deleteOrganization(String id) throws SynapseException {
		ValidateArgument.required(id, "id");
		String url = "/schema/organization/"+id;
		deleteUri(getRepoEndpoint(), url);
	}

	@Override
	public AccessControlList getOrganizationAcl(String id) throws SynapseException {
		ValidateArgument.required(id, "organizationName");
		String url = "/schema/organization/"+id+"/acl";
		return getJSONEntity(getRepoEndpoint(), url, AccessControlList.class);
	}

	@Override
	public AccessControlList updateOrganizationAcl(String id, AccessControlList toUpdate) throws SynapseException {
		ValidateArgument.required(id, "id");
		ValidateArgument.required(toUpdate, "AccessControlList");
		String url = "/schema/organization/"+id+"/acl";
		return putJSONEntity(getRepoEndpoint(), url, toUpdate, AccessControlList.class);
	}

	@Override
	public String startCreateSchemaJob(CreateSchemaRequest request) throws SynapseException {
		return startAsynchJob(AsynchJobType.CreateJsonSchema, request);
	}

	@Override
	public CreateSchemaResponse getCreateSchemaJobResult(String asyncJobToken)
			throws SynapseException, SynapseResultNotReadyException {
		String url = SCHEMA_TYPE_CREATE + ASYNC_GET + asyncJobToken;
		return (CreateSchemaResponse) getAsynchJobResponse(url, CreateSchemaResponse.class, getRepoEndpoint());
	}

	@Override
	public JsonSchema getJsonSchema(String organizationName, String schemaName, String semanticVersion) throws SynapseException {
		ValidateArgument.required(organizationName, "organizationName");
		ValidateArgument.required(schemaName, "schemaName");
		StringBuilder builder = new StringBuilder();
		builder.append("/schema/type/registered/");
		builder.append(organizationName);
		builder.append("-");
		builder.append(schemaName);
		if(semanticVersion != null) {
			builder.append("-");
			builder.append(semanticVersion);
		}
		return getJSONEntity(getRepoEndpoint(), builder.toString(), JsonSchema.class);
	}

	@Override
	public void deleteSchema(String organizationName, String schemaName) throws SynapseException {
		ValidateArgument.required(organizationName, "organizationName");
		ValidateArgument.required(schemaName, "schemaName");
		StringBuilder builder = new StringBuilder();
		builder.append("/schema/type/registered/");
		builder.append(organizationName);
		builder.append("-");
		builder.append(schemaName);
		deleteUri(getRepoEndpoint(), builder.toString());
	}

	@Override
	public void deleteSchemaVersion(String organizationName, String schemaName, String semanticVersion)
			throws SynapseException {
		ValidateArgument.required(organizationName, "organizationName");
		ValidateArgument.required(schemaName, "schemaName");
		ValidateArgument.required(semanticVersion, "semanticVersion");
		StringBuilder builder = new StringBuilder();
		builder.append("/schema/type/registered/");
		builder.append(organizationName);
		builder.append("-");
		builder.append(schemaName);
		if(semanticVersion != null) {
			builder.append("-");
			builder.append(semanticVersion);
		}
		deleteUri(getRepoEndpoint(), builder.toString());
	}
	
	@Override
	public ListOrganizationsResponse listOrganizations(ListOrganizationsRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		String url = "/schema/organization/list";
		return postJSONEntity(getRepoEndpoint(), url, request, ListOrganizationsResponse.class);
	}
	
	@Override
	public ListJsonSchemaInfoResponse listSchemaInfo(ListJsonSchemaInfoRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		String url = "/schema/list";
		return postJSONEntity(getRepoEndpoint(), url, request, ListJsonSchemaInfoResponse.class);
	}
	
	@Override
	public ListJsonSchemaVersionInfoResponse listSchemaVersions(ListJsonSchemaVersionInfoRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		String url = "/schema/version/list";
		return postJSONEntity(getRepoEndpoint(), url, request, ListJsonSchemaVersionInfoResponse.class);
	}
	
	@Override
	public JsonSchemaObjectBinding bindJsonSchemaToEntity(BindSchemaToEntityRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getEntityId(), "request.entityId");
		String url = "/entity/"+request.getEntityId()+"/schema/binding";
		return putJSONEntity(getRepoEndpoint(), url, request, JsonSchemaObjectBinding.class);
	}
	
	@Override
	public JsonSchemaObjectBinding getJsonSchemaBindingForEntity(String entityId) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		String url = "/entity/"+entityId+"/schema/binding";
		return getJSONEntity(getRepoEndpoint(), url, JsonSchemaObjectBinding.class);
	}
	
	@Override
	public void clearSchemaBindingForEntity(String entityId) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		String url = "/entity/"+entityId+"/schema/binding";
		deleteUri(getRepoEndpoint(), url);
	}
	
	@Override
	public JSONObject getEntityJson(String entityId) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		String url = "/entity/"+entityId+"/json";
		return getJson(getRepoEndpoint(), url);
	}
	
	@Override
	public JSONObject updateEntityJson(String entityId, JSONObject json) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		String url = "/entity/"+entityId+"/json";
		return putJson(getRepoEndpoint(), url, json.toString());
	}
	
	@Override
	public ValidationResults getEntityValidationResults(String entityId) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		String url = "/entity/"+entityId+"/schema/validation";
		return getJSONEntity(getRepoEndpoint(), url, ValidationResults.class);
	}
	
	@Override
	public ValidationSummaryStatistics getEntitySchemaValidationStatistics(String entityId) throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		String url = "/entity/"+entityId+"/schema/validation/statistics";
		return getJSONEntity(getRepoEndpoint(), url, ValidationSummaryStatistics.class);
	}
	
	@Override
	public ListValidationResultsResponse getInvalidValidationResults(ListValidationResultsRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getContainerId(), "request.containerId");
		String url = "/entity/"+request.getContainerId()+"/schema/validation/invalid";
		return postJSONEntity(getRepoEndpoint(), url, request, ListValidationResultsResponse.class);
	}
	
	@Override
	public void updateEntityFileHandle(String entityId, Long versionNumber, FileHandleUpdateRequest request)
			throws SynapseException {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(versionNumber, "versionNumber");
		ValidateArgument.required(request, "request");
		
		String url = "/entity/" + entityId + "/version/" + versionNumber + "/filehandle";
		
		voidPut(getRepoEndpoint(), url, request);
		
	}

	@Override
	public AddBatchOfFilesToDownloadListResponse addFilesToDownloadList(AddBatchOfFilesToDownloadListRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		String url = "/download/list/add";
		return postJSONEntity(getRepoEndpoint(), url, request, AddBatchOfFilesToDownloadListResponse.class);
	}
	
	@Override
	public RemoveBatchOfFilesFromDownloadListResponse removeFilesFromDownloadList(RemoveBatchOfFilesFromDownloadListRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		String url = "/download/list/remove";
		return postJSONEntity(getRepoEndpoint(), url, request, RemoveBatchOfFilesFromDownloadListResponse.class);
	}
	
	@Override
	public void clearUsersDownloadList() throws SynapseException {
		String uri = "/download/list";
		deleteUri(getRepoEndpoint(), uri);
	}
	
	@Override
	public String startDownloadListQuery(DownloadListQueryRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		return startAsynchJob(AsynchJobType.QueryDownloadList, request);
	}

	@Override
	public DownloadListQueryResponse getDownloadListQueryResult(String asyncJobToken)
			throws SynapseException, SynapseResultNotReadyException {
		ValidateArgument.required(asyncJobToken, "asyncJobToken");
		String url = DOWNLOAD_LIST_QUERY + ASYNC_GET + asyncJobToken;
		return (DownloadListQueryResponse) getAsynchJobResponse(url, DownloadListQueryResponse.class, getRepoEndpoint());
	}
}
