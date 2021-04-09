package org.sagebionetworks.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseResultNotReadyException;
import org.sagebionetworks.client.exceptions.SynapseTableUnavailableException;
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
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.InviteeVerificationSignedToken;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
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
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResponseMessage;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptorResponse;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
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
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationRequest;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.AccessTokenRecordList;
import org.sagebionetworks.repo.model.auth.ChangePasswordInterface;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementConversionRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupResponse;
import org.sagebionetworks.repo.model.dataaccess.CreateSubmissionRequest;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfoPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionOrder;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.ReplyCount;
import org.sagebionetworks.repo.model.discussion.ThreadCount;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.doi.v2.DoiResponse;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListRequest;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListResponse;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
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
import org.sagebionetworks.repo.model.principal.EmailValidationSignedToken;
import org.sagebionetworks.repo.model.principal.NotificationEmail;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAliasResponse;
import org.sagebionetworks.repo.model.principal.TypeFilter;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.query.QueryTableResults;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.Quiz;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.model.report.DownloadStorageReportResponse;
import org.sagebionetworks.repo.model.report.StorageReportType;
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
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.SnapshotResponse;
import org.sagebionetworks.repo.model.table.SqlTransformRequest;
import org.sagebionetworks.repo.model.table.SubmissionView;
import org.sagebionetworks.repo.model.table.TableFileHandleResults;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;


/**
 * Abstraction for Synapse.
 * 
 * @author jmhill
 * 
 */
public interface SynapseClient extends BaseClient {

	/**
	 * Get the current status of the stack
	 */
	public StackStatus getCurrentStackStatus() 
			throws SynapseException;
	
	/**
	 * Is the passed alias available and valid?
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException 
	 */
	public AliasCheckResponse checkAliasAvailable(AliasCheckRequest request) throws SynapseException;
	
	/**
	 * Send an email validation message as a precursor to creating a new user account.
	 * 
	 * @param user the first name, last name and email address for the new user
	 * @param portalEndpoint the GUI endpoint (is the basis for the link in the email message)
	 * Must generate a valid email when a set of request parameters is appended to the end.
	 */
	void newAccountEmailValidation(NewUser user, String portalEndpoint) throws SynapseException;
	
	/**
	 * Create a new account, following email validation.  Sets the password and logs the user in, returning a valid session token
	 * @param accountSetupInfo  Note:  Caller may override the first/last name, but not the email, given in 'newAccountEmailValidation' 
	 * @return session
	 * @throws NotFoundException 
	 */
	Session createNewAccount(AccountSetupInfo accountSetupInfo) throws SynapseException;
	
	/**
	 * Send an email validation as a precursor to adding a new email address to an existing account.
	 * 
	 * @param userId the user's principal ID
	 * @param email the email which is claimed by the user
	 * @param portalEndpoint the GUI endpoint (is the basis for the link in the email message)
	 * Must generate a valid email when a set of request parameters is appended to the end.
	 * @throws NotFoundException 
	 */
	void additionalEmailValidation(Long userId, String email, String portalEndpoint) throws SynapseException;
	
	/**
	 * Add a new email address to an existing account.
	 * 
	 * @param emailValidationSignedToken the token sent to the user via email
	 * @param setAsNotificationEmail if true then set the new email address to be the user's notification address
	 * @throws NotFoundException
	 */
	void addEmail(EmailValidationSignedToken emailValidationSignedToken, Boolean setAsNotificationEmail) throws SynapseException;
	
	/**
	 * Remove an email address from an existing account.
	 * 
	 * @param email the email to remove.  Must be an established email address for the user
	 * @throws NotFoundException
	 */
	void removeEmail(String email) throws SynapseException;	
	
	/**
	 * This sets the email used for user notifications, i.e. when a Synapse message is
	 * sent and if the user has elected to receive messages by email, then this is the email
	 * address at which the user will receive the message.  Note:  The given email address
	 * must already be established as being owned by the user.
	 */
	public void setNotificationEmail(String email) throws SynapseException;
	
	/**
	 * This gets the email used for user notifications, i.e. when a Synapse message is
	 * sent and if the user has elected to receive messages by email, then this is the email
	 * address at which the user will receive the message. If the email if currently in quarantine it will include
	 * the quarantine status
	 * 
	 * @throws SynapseException
	 */
	public NotificationEmail getNotificationEmail() throws SynapseException;

	public Entity getEntityById(String entityId) throws SynapseException;

	public <T extends Entity> T putEntity(T entity) throws SynapseException;

	/** Gets the temporary S3 credentials from STS for the given entity. */
	StsCredentials getTemporaryCredentialsForEntity(String entityId, StsPermission permission) throws SynapseException;

	public URL getWikiAttachmentPreviewTemporaryUrl(WikiPageKey properKey,
			String fileName) throws ClientProtocolException, IOException, SynapseException;
	
	public void downloadWikiAttachmentPreview(WikiPageKey properKey,
			String fileName, File target) throws SynapseException;

	public URL getWikiAttachmentTemporaryUrl(WikiPageKey properKey,
			String fileName) throws ClientProtocolException, IOException, SynapseException;
	
	public void downloadWikiAttachment(WikiPageKey properKey,
			String fileName, File target) throws SynapseException;

	/**
	 * Returns a Session and UserProfile object
	 * 
	 * Note: if the user has not accepted the terms of use, the profile will not (cannot) be retrieved
	 */
	public UserSessionData getUserSessionData() throws SynapseException;

	/**
	 * Create a new Entity.
	 * 
	 * @return the newly created entity
	 */
	public <T extends Entity> T createEntity(T entity) throws SynapseException;

	public SearchResults search(SearchQuery searchQuery)
			throws SynapseException, UnsupportedEncodingException;

	public URL getFileEntityPreviewTemporaryUrlForCurrentVersion(String entityId)
			throws ClientProtocolException, MalformedURLException, IOException, SynapseException;

	public URL getFileEntityTemporaryUrlForCurrentVersion(String entityId)
			throws ClientProtocolException, MalformedURLException, IOException, SynapseException;

	public URL getFileEntityPreviewTemporaryUrlForVersion(String entityId,
			Long versionNumber) throws ClientProtocolException,
			MalformedURLException, IOException, SynapseException;

	public URL getFileEntityTemporaryUrlForVersion(String entityId,
			Long versionNumber) throws ClientProtocolException,
			MalformedURLException, IOException, SynapseException;

	/**
	 * Get a WikiPage using its key
	 */
	public WikiPage getWikiPage(WikiPageKey properKey) throws SynapseException;
	
	/**
	 * Get a specific version of a wikig page.
	 * @param properKey
	 * @param versionNumber
	 * @return
	 * @throws SynapseException 
	 */
	public WikiPage getWikiPageForVersion(WikiPageKey properKey, Long versionNumber) throws SynapseException;
	
	/**
	 * Get the WikiPageKey for the root wiki given an ownerId and ownerType.
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws SynapseException 
	 */
	public WikiPageKey getRootWikiPageKey(String ownerId, ObjectType ownerType) throws SynapseException;
	
	public AccessRequirement getAccessRequirement(Long requirementId) throws SynapseException;

	public PaginatedResults<AccessRequirement> getAccessRequirements(
			RestrictableObjectDescriptor subjectId, Long limit, Long offset) throws SynapseException;

	public WikiPage updateWikiPage(String ownerId, ObjectType ownerType,
			WikiPage toUpdate) throws SynapseException;

	public <T extends Entity> T createEntity(T entity, String activityId)
			throws SynapseException;

	public Entity getEntityByIdForVersion(String entityId, Long versionNumber)
			throws SynapseException;

	public EntityBundle createEntityBundleV2(EntityBundleCreate ebc)
			throws SynapseException;

	public EntityBundle createEntityBundleV2(EntityBundleCreate ebc, String activityId)
			throws SynapseException;

	public EntityBundle updateEntityBundleV2(String entityId, EntityBundleCreate ebc)
			throws SynapseException;

	public EntityBundle updateEntityBundleV2(String entityId, EntityBundleCreate ebc,
											 String activityId) throws SynapseException;

	public EntityBundle getEntityBundleV2(String entityId, EntityBundleRequest bundleV2Request)
			throws SynapseException;

	public EntityBundle getEntityBundleV2(String entityId, Long versionNumber,
										  EntityBundleRequest bundleV2Request) throws SynapseException;

	public PaginatedResults<VersionInfo> getEntityVersions(String entityId,
			int offset, int limit) throws SynapseException;

	public AccessControlList getACL(String entityId) throws SynapseException;

	public EntityHeader getEntityBenefactor(String entityId) throws SynapseException;

	public UserProfile getMyProfile() throws SynapseException;

	public void updateMyProfile(UserProfile userProfile) throws SynapseException;
	
	/**
	 * update user profile settings
	 * 
	 * @param token
	 * @throws SynapseException
	 */
	public ResponseMessage updateNotificationSettings(NotificationSettingsSignedToken token) throws SynapseException;

	public UserProfile getUserProfile(String ownerId) throws SynapseException;

	public UserGroupHeaderResponsePage getUserGroupHeadersByIds(List<String> ids)
			throws SynapseException;
	
	/**
	 * Get the pre-signed URL for a user's profile picture.
	 * @param ownerId
	 * @return
	 * @throws SynapseException 
	 * @throws IOException 
	 * @throws MalformedURLException 
	 * @throws ClientProtocolException 
	 */
	public URL getUserProfilePictureUrl(String ownerId) throws ClientProtocolException, MalformedURLException, IOException, SynapseException;

	/**
	 * Get the pre-signed URL for a user's profile picture preview.
	 * @param ownerId
	 * @return
	 * @throws SynapseException 
	 * @throws IOException 
	 * @throws MalformedURLException 
	 * @throws ClientProtocolException 
	 */
	public URL getUserProfilePicturePreviewUrl(String ownerId) throws ClientProtocolException, MalformedURLException, IOException, SynapseException;

	/**
	 * 
	 * uses the default pagination as determined by the server
	 * @param prefix
	 * @return the users whose first, last or user name matches the given prefix
	 * @throws SynapseException
	 * @throws UnsupportedEncodingException
	 */
	public UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(String prefix)
			throws SynapseException, UnsupportedEncodingException;

	/**
	 * 
	 * @param prefix
	 * @param filter Optional filter to limit the results to a given type.
	 * @param limit page size
	 * @param offset page start
	 * @return the users whose first, last or user name matches the given prefix
	 * @throws SynapseException
	 * @throws UnsupportedEncodingException
	 */
	public UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(String prefix, TypeFilter filter, Long limit, Long offset)
			throws SynapseException, UnsupportedEncodingException;
	
	/**
	 * Lookup the UserGroupHeaders for the given aliases.
	 * @param aliases List of user or team names.  
	 * @return
	 * @throws SynapseException 
	 */
	public List<UserGroupHeader> getUserGroupHeadersByAliases(List<String> aliases) throws SynapseException;

	public AccessControlList updateACL(AccessControlList acl) throws SynapseException;

	public void deleteACL(String entityId) throws SynapseException;

	public AccessControlList createACL(AccessControlList acl) throws SynapseException;

	public PaginatedResults<UserProfile> getUsers(int offset, int limit)
			throws SynapseException;
	
	public List<UserProfile> listUserProfiles(List<Long> userIds) throws SynapseException;

	public PaginatedResults<UserGroup> getGroups(int offset, int limit)
			throws SynapseException;

	public boolean canAccess(String entityId, ACCESS_TYPE accessType)
			throws SynapseException;

	public boolean canAccess(String id, ObjectType type, ACCESS_TYPE accessType)
			throws SynapseException;

	public UserEntityPermissions getUsersEntityPermissions(String entityId)
			throws SynapseException;

	public Annotations getAnnotationsV2(String entityId) throws SynapseException;

	public Annotations updateAnnotationsV2(String entityId, Annotations updated)
			throws SynapseException;

	public <T extends AccessRequirement> T createAccessRequirement(T ar)
			throws SynapseException;

	public <T extends AccessRequirement> T updateAccessRequirement(T ar)
			throws SynapseException;

	public LockAccessRequirement createLockAccessRequirement(String entityId)
			throws SynapseException;

	public <T extends AccessApproval> T createAccessApproval(T aa)
			throws SynapseException;
	
	public AccessApproval getAccessApproval(Long approvalId) throws SynapseException;

	public void revokeAccessApprovals(String requirementId, String accessorId) throws SynapseException;

	public JSONObject getEntity(String uri) throws SynapseException;

	public <T extends JSONEntity> T getEntity(String entityId,
			Class<? extends T> clazz) throws SynapseException;

	public void deleteAccessRequirement(Long arId) throws SynapseException;

	public <T extends Entity> T putEntity(T entity, String activityId, Boolean newVersion)
			throws SynapseException;

	public <T extends Entity> void deleteEntity(T entity) throws SynapseException;

	public <T extends Entity> void deleteEntity(T entity, Boolean skipTrashCan) throws SynapseException;

	public void deleteEntityById(String entityId) throws SynapseException;

	public void deleteEntityById(String entityId, Boolean skipTrashCan) throws SynapseException;

	public <T extends Entity> void deleteEntityVersion(T entity,
			Long versionNumber) throws SynapseException;

	public void deleteEntityVersionById(String entityId, Long versionNumber)
			throws SynapseException;

	public EntityPath getEntityPath(Entity entity) throws SynapseException;

	public EntityPath getEntityPath(String entityId) throws SynapseException;

	public PaginatedResults<EntityHeader> getEntityTypeBatch(List<String> entityIds)
			throws SynapseException;

	public PaginatedResults<EntityHeader> getEntityHeaderBatch(List<Reference> references)
			throws SynapseException;

	public String putFileToURL(URL url, File file, String contentType)
			throws SynapseException;

	public ExternalFileHandle createExternalFileHandle(ExternalFileHandle efh)
			throws SynapseException;
	
	/**
	 * Create a new ProxyFileHandle. Note: ProxyFileHandle.storageLocationsId
	 * must be set to reference a valid ProxyStorageLocationSettings.
	 * @param handle
	 * @return
	 * @throws SynapseException
	 */
	ProxyFileHandle createExternalProxyFileHandle(ProxyFileHandle handle)
			throws SynapseException;

	/**
	 * Create a new ExternalObjectStoreFileHandle. Note: ExternalObjectStoreFileHandle.storageLocationId
	 * must be set to reference a valid ExternalObjectStorageLocationSetting.
	 * @param handle
	 * @return
	 * @throws SynapseException
	 */
	ExternalObjectStoreFileHandle createExternalObjectStoreFileHandle(ExternalObjectStoreFileHandle handle)
			throws SynapseException;

	/**
	 * Create an S3FileHandle using a pre-configured ExternalS3StorageLocationSetting ID.
	 * @param handle
	 * @return
	 * @throws SynapseException 
	 */
	public S3FileHandle createExternalS3FileHandle(S3FileHandle handle) throws SynapseException;

	/**
	 * Create an GoogleCloudFileHandle using a pre-configured ExternalGoogleCloudStorageLocationSetting ID.
	 * @param handle
	 * @return
	 * @throws SynapseException
	 */
	public GoogleCloudFileHandle createExternalGoogleCloudFileHandle(GoogleCloudFileHandle handle) throws SynapseException;

	/**
	 * Create a new file handle with optionally a new name and a new content type
	 * 
	 * @param originalFileHandleId
	 * @param name
	 * @param contentType
	 * @return
	 * @throws SynapseException
	 */
	public S3FileHandle createS3FileHandleCopy(String originalFileHandleId, String name, String contentType)
			throws SynapseException;

	public FileHandle getRawFileHandle(String fileHandleId) throws SynapseException;

	public void deleteFileHandle(String fileHandleId) throws SynapseException;

	public void clearPreview(String fileHandleId) throws SynapseException;

	public WikiPage createWikiPage(String ownerId, ObjectType ownerType,
			WikiPage toCreate) throws SynapseException;

	public WikiPage getRootWikiPage(String ownerId, ObjectType ownerType)
			throws SynapseException;

	public FileHandleResults getWikiAttachmenthHandles(WikiPageKey key)
			throws SynapseException;

	public void deleteWikiPage(WikiPageKey key) throws SynapseException;

	public FileHandleResults getEntityFileHandlesForCurrentVersion(String entityId)
			throws SynapseException;

	public FileHandleResults getEntityFileHandlesForVersion(String entityId,
			Long versionNumber) throws SynapseException;

	public V2WikiPage createV2WikiPage(String ownerId, ObjectType ownerType,
			V2WikiPage toCreate) throws SynapseException;

	public V2WikiPage getV2WikiPage(WikiPageKey key)
			throws SynapseException;

	public V2WikiPage getVersionOfV2WikiPage(WikiPageKey key, Long version)
			throws SynapseException;
	
	public V2WikiPage updateV2WikiPage(String ownerId, ObjectType ownerType,
			V2WikiPage toUpdate) throws SynapseException;
	
	public V2WikiPage restoreV2WikiPage(String ownerId, ObjectType ownerType,
			String wikiId, Long versionToRestore) throws SynapseException;
	
	public V2WikiPage getV2RootWikiPage(String ownerId, ObjectType ownerType)
		throws SynapseException;

	public FileHandleResults getV2WikiAttachmentHandles(WikiPageKey key)
		throws SynapseException;

	public FileHandleResults getVersionOfV2WikiAttachmentHandles(WikiPageKey key, Long version)
		throws SynapseException;
	
	public String downloadV2WikiMarkdown(WikiPageKey key) throws ClientProtocolException, FileNotFoundException, IOException, SynapseException;
	
	public String downloadVersionOfV2WikiMarkdown(WikiPageKey key, Long version) throws ClientProtocolException, FileNotFoundException, IOException, SynapseException;
	
	public URL getV2WikiAttachmentPreviewTemporaryUrl(WikiPageKey key,
			String fileName) throws ClientProtocolException, IOException, SynapseException;
	
	public void downloadV2WikiAttachmentPreview(WikiPageKey key,
			String fileName, File target) throws SynapseException;

	public URL getV2WikiAttachmentTemporaryUrl(WikiPageKey key,
			String fileName) throws ClientProtocolException, IOException, SynapseException;
	
	public void downloadV2WikiAttachment(WikiPageKey key,
			String fileName, File target) throws SynapseException;
	
	public URL getVersionOfV2WikiAttachmentPreviewTemporaryUrl(WikiPageKey key,
			String fileName, Long version) throws ClientProtocolException, IOException, SynapseException;
	
	public void downloadVersionOfV2WikiAttachmentPreview(WikiPageKey key,
			String fileName, Long version, File target) throws SynapseException;

	public URL getVersionOfV2WikiAttachmentTemporaryUrl(WikiPageKey key,
			String fileName, Long version) throws ClientProtocolException, IOException, SynapseException;
	
	public void downloadVersionOfV2WikiAttachment(WikiPageKey key,
			String fileName, Long version, File target) throws SynapseException;

	public void deleteV2WikiPage(WikiPageKey key) throws SynapseException;
	
	public PaginatedResults<V2WikiHeader> getV2WikiHeaderTree(String ownerId,
		ObjectType ownerType, Long limit, Long offset) throws SynapseException;
	
	V2WikiOrderHint getV2OrderHint(WikiPageKey key) throws SynapseException;
	
	V2WikiOrderHint updateV2WikiOrderHint(V2WikiOrderHint toUpdate) throws SynapseException;
	
	public PaginatedResults<V2WikiHistorySnapshot> getV2WikiHistory(WikiPageKey key, Long limit, Long offset)
		throws SynapseException;

	/**
	 * Download the File attachment for an entity, following redirects as needed.
	 * 
	 * @param entityId
	 * @param destinationFile
	 * @throws SynapseException
	 */
	@Deprecated
	public void downloadFromFileEntityCurrentVersion(String entityId, File destinationFile)
			throws SynapseException;
	
	/**
	 * Download the file attached to a given version of an FileEntity
	 * 
	 * @param entityId
	 * @param version
	 * @param destinationFile
	 * @throws SynapseException
	 */
	@Deprecated
	public void downloadFromFileEntityForVersion(String entityId, Long version, File destinationFile)
			throws SynapseException;
	
	/**
	 * Download the preview for a given FileEntity
	 * @param entityId
	 * @param destinationFile
	 * @throws SynapseException
	 */
	public void downloadFromFileEntityPreviewCurrentVersion(String entityId, File destinationFile)
			throws SynapseException;
	
	/**
	 * Download the preview attached to a given version of an entity
	 * @param entityId
	 * @param version
	 * @param destinationFile
	 * @throws SynapseException
	 */
	public void downloadFromFileEntityPreviewForVersion(String entityId, Long version, File destinationFile)
			throws SynapseException;

	/**
	 * Sends a message to another user
	 */
	public MessageToUser sendMessage(MessageToUser message)
			throws SynapseException;
	
	/**
	 * Convenience function to upload message body, then send message using resultant fileHandleId
	 * For an example of the message content being retrieved for email delivery, see MessageManagerImpl.downloadEmailContent().
	 * @param message
	 * @param messageBody
	 * @return
	 * @throws SynapseException
	 */
	public MessageToUser sendStringMessage(MessageToUser message, String messageBody)
			throws SynapseException;
	
	/**
	 * Sends a message to another user and the owner of the given entity
	 */
	public MessageToUser sendMessage(MessageToUser message, String entityId) 
			throws SynapseException;

	/**
	 * Convenience function to upload message body, then send message to entity owner using resultant fileHandleId.
	 * For an example of the message content being retrieved for email delivery, see MessageManagerImpl.downloadEmailContent().
	 * @param message
	 * @param entityId
	 * @param messageBody
	 * @return
	 * @throws SynapseException
	 */
	public MessageToUser sendStringMessage(MessageToUser message, String entityId, String messageBody)
			throws SynapseException;
	
	/**
	 * Gets the current authenticated user's received messages
	 */
	public PaginatedResults<MessageBundle> getInbox(
			List<MessageStatusType> inboxFilter, MessageSortBy orderBy,
			Boolean descending, long limit, long offset)
			throws SynapseException;

	/**
	 * Gets the current authenticated user's outbound messages
	 */
	public PaginatedResults<MessageToUser> getOutbox(MessageSortBy orderBy,
			Boolean descending, long limit, long offset)
			throws SynapseException;

	/**
	 * Gets a specific message
	 */
	public MessageToUser getMessage(String messageId) throws SynapseException;

	/**
	 * Sends an existing message to another set of users
	 */
	public MessageToUser forwardMessage(String messageId,
			MessageRecipientSet recipients) throws SynapseException;

	/**
	 * Gets messages associated with the specified message
	 */
	public PaginatedResults<MessageToUser> getConversation(
			String associatedMessageId, MessageSortBy orderBy,
			Boolean descending, long limit, long offset)
			throws SynapseException;

	/**
	 * Changes the status of a message in a user's inbox
	 */
	public void updateMessageStatus(MessageStatus status)
			throws SynapseException;

	/**
	 * Downloads the body of a message and returns it in a String
	 */
	public String downloadMessage(String messageId) throws SynapseException, MalformedURLException, IOException;
	
	/**
	 * Returns a temporary URL that can be used to download the body of a message
	 */
	public String getMessageTemporaryUrl(String messageId) throws SynapseException, MalformedURLException, IOException;
	
	/**
	 * Downloads the body of a message to the given target file location.
	 * 
	 * @param messageId
	 * @param target
	 * @throws SynapseException
	 */
	public void downloadMessageToFile(String messageId, File target) throws SynapseException;

	public SynapseVersionInfo getVersionInfo() throws SynapseException;

	public Activity getActivityForEntity(String entityId) throws SynapseException;

	public Activity getActivityForEntityVersion(String entityId, Long versionNumber)
			throws SynapseException;

	public Activity setActivityForEntity(String entityId, String activityId)
			throws SynapseException;

	public void deleteGeneratedByForEntity(String entityId) throws SynapseException;

	public Activity createActivity(Activity activity) throws SynapseException;

	public Activity getActivity(String activityId) throws SynapseException;

	public Activity putActivity(Activity activity) throws SynapseException;

	public void deleteActivity(String activityId) throws SynapseException;

	public PaginatedResults<Reference> getEntitiesGeneratedBy(String activityId,
			Integer limit, Integer offset) throws SynapseException;

	public Evaluation createEvaluation(Evaluation eval) throws SynapseException;

	public Evaluation getEvaluation(String evalId) throws SynapseException;
	
	public PaginatedResults<Evaluation> getEvaluationByContentSource(String id,
			int offset, int limit) throws SynapseException;

	public PaginatedResults<Evaluation> getEvaluationByContentSource(String id, ACCESS_TYPE accessType,
			int offset, int limit) throws SynapseException;
	
	PaginatedResults<Evaluation> getEvaluationByContentSource(String id, ACCESS_TYPE accessType, boolean activeOnly,
			List<Long> evaluationIds, int offset, int limit) throws SynapseException;

	public PaginatedResults<Evaluation> getAvailableEvaluationsPaginated(int offset, int limit)
			throws SynapseException;

	/**
	 * 
	 * @param offset
	 * @param limit
	 * @param evaluationIds list of evaluation IDs within which to filter the results
	 * @return
	 * @throws SynapseException
	 */
	public PaginatedResults<Evaluation> getAvailableEvaluationsPaginated(int offset, int limit, List<String> evaluationIds)
			throws SynapseException;

	public Evaluation findEvaluation(String name) throws SynapseException,
			UnsupportedEncodingException;

	public Evaluation updateEvaluation(Evaluation eval) throws SynapseException;

	public void deleteEvaluation(String evalId) throws SynapseException;

	EvaluationRound createEvaluationRound(EvaluationRound round) throws SynapseException;

	EvaluationRound getEvaluationRound(String evalId, String roundId) throws SynapseException;

	EvaluationRoundListResponse getAllEvaluationRounds(String evalId, EvaluationRoundListRequest request) throws SynapseException;

	EvaluationRound updateEvaluationRound(EvaluationRound round) throws SynapseException;

	void deleteEvaluationRound(String evalId, String roundId) throws SynapseException;

	/**
	 * 
	 * @param sub
	 * @param etag
	 * @param challengeEndpoint the prefix to an entity/challenge page.  The entity ID of the challenge project is
	 * appended to create the complete URL.
	 * @param notificationUnsubscribeEndpoint the prefix of a one-click unsubscribe link for notifications.
	 * A serialization token containing user information is appended to the given endpoint to create the complete URL.
	 * @return
	 * @throws SynapseException
	 */
	public Submission createIndividualSubmission(Submission sub, String etag,
			String challengeEndpoint, String notificationUnsubscribeEndpoint)
			throws SynapseException;
	
	public TeamSubmissionEligibility getTeamSubmissionEligibility(String evaluationId, String teamId) 
			throws SynapseException;

	/**
	 * 
	 * @param sub
	 * @param etag
	 * @param submissionEligibilityHash
	 * @param challengeEndpoint the prefix to an entity/challenge page.  The entity ID of the challenge project is
	 * appended to create the complete URL.
	 * @param notificationUnsubscribeEndpoint the prefix of a one-click unsubscribe link for notifications.
	 * A serialization token containing user information is appended to the given endpoint to create the complete URL.
	 * @return
	 * @throws SynapseException
	 */
	public Submission createTeamSubmission(Submission sub, String etag, String submissionEligibilityHash,
			String challengeEndpoint, String notificationUnsubscribeEndpoint)
			throws SynapseException;

	public Submission getSubmission(String subId) throws SynapseException;

	public SubmissionStatus getSubmissionStatus(String subId) throws SynapseException;

	public SubmissionStatus updateSubmissionStatus(SubmissionStatus status)
			throws SynapseException;

	public BatchUploadResponse updateSubmissionStatusBatch(String evaluationId, SubmissionStatusBatch batch)
			throws SynapseException;

	public void deleteSubmission(String subId) throws SynapseException;

	public PaginatedResults<Submission> getAllSubmissions(String evalId, long offset,
			long limit) throws SynapseException;

	public PaginatedResults<SubmissionStatus> getAllSubmissionStatuses(String evalId,
			long offset, long limit) throws SynapseException;

	public PaginatedResults<SubmissionBundle> getAllSubmissionBundles(String evalId,
			long offset, long limit) throws SynapseException;

	public PaginatedResults<Submission> getAllSubmissionsByStatus(String evalId,
			SubmissionStatusEnum status, long offset, long limit)
			throws SynapseException;

	public PaginatedResults<SubmissionStatus> getAllSubmissionStatusesByStatus(
			String evalId, SubmissionStatusEnum status, long offset, long limit)
			throws SynapseException;

	public PaginatedResults<SubmissionBundle> getAllSubmissionBundlesByStatus(
			String evalId, SubmissionStatusEnum status, long offset, long limit)
			throws SynapseException;

	public PaginatedResults<Submission> getMySubmissions(String evalId, long offset,
			long limit) throws SynapseException;

	public PaginatedResults<SubmissionBundle> getMySubmissionBundles(String evalId,
			long offset, long limit) throws SynapseException;

	public URL getFileTemporaryUrlForSubmissionFileHandle(String submissionId,
			String fileHandleId) throws ClientProtocolException,
			MalformedURLException, IOException, SynapseException;

	/**
	 * Download a selected file attachment for a Submission, following redirects as needed.
	 * 
	 * @param submissionId
	 * @param fileHandleId
	 * @param destinationFile
	 * @throws SynapseException
	 */
	public void downloadFromSubmission(String submissionId, String fileHandleId, File destinationFile) 
			throws SynapseException;

	/**
	 * 
	 * @param query
	 * @return
	 * @throws SynapseException
	 * @Deprecated Use {@link SubmissionView} and the {@link #queryTableEntityBundleAsyncStart(Query, QueryOptions, String)}
	 */
	@Deprecated
	public QueryTableResults queryEvaluation(String query) throws SynapseException;

	public void moveToTrash(String entityId) throws SynapseException;

	public void restoreFromTrash(String entityId, String newParentId)
			throws SynapseException;

	public PaginatedResults<TrashedEntity> viewTrashForUser(long offset, long limit)
			throws SynapseException;
	
	public void flagForPurge(String entityId) throws SynapseException;

	/**
	 * Deprecated, will have the same effect as {@link #flagForPurge(String)}
	 * 
	 * @param entityId
	 * @throws SynapseException
	 */
	@Deprecated
	public void purgeTrashForUser(String entityId) throws SynapseException;

	public EntityHeader addFavorite(String entityId) throws SynapseException;

	public void removeFavorite(String entityId) throws SynapseException;

	public PaginatedResults<EntityHeader> getFavorites(Integer limit, Integer offset)
			throws SynapseException;

	public ProjectHeaderList getMyProjects(ProjectListType type, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			String nextPageToken) throws SynapseException;

	public ProjectHeaderList getProjectsFromUser(Long userId, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			String nextPageToken) throws SynapseException;

	public ProjectHeaderList getProjectsForTeam(Long teamId, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			String nextPageToken) throws SynapseException;

	@Deprecated
	public PaginatedResults<ProjectHeader> getMyProjectsDeprecated(ProjectListType type, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			Integer limit, Integer offset) throws SynapseException;
	@Deprecated
	public PaginatedResults<ProjectHeader> getProjectsFromUserDeprecated(Long userId, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			Integer limit, Integer offset) throws SynapseException;
	@Deprecated
	public PaginatedResults<ProjectHeader> getProjectsForTeamDeprecated(Long teamId, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			Integer limit, Integer offset) throws SynapseException;

	public DoiAssociation getDoiAssociation(String objectId, ObjectType objectType, Long objectVersion) throws SynapseException;

	public Doi getDoi(String objectId, ObjectType objectType, Long objectVersion) throws SynapseException;

	public String createOrUpdateDoiAsyncStart(Doi doi) throws SynapseException;

	public DoiResponse createOrUpdateDoiAsyncGet(String asyncJobToken) throws SynapseException, SynapseResultNotReadyException;

	public String getPortalUrl(String objectId, ObjectType objectType, Long objectVersion) throws SynapseException;

	public List<EntityHeader> getEntityHeaderByMd5(String md5) throws SynapseException;

	public String retrieveApiKey() throws SynapseException;

	public String createPersonalAccessToken(AccessTokenGenerationRequest request) throws SynapseException;

	public AccessTokenRecord retrievePersonalAccessTokenRecord(String tokenId) throws SynapseException;

	public AccessTokenRecordList retrievePersonalAccessTokenRecords(String nextPageToken) throws SynapseException;

	public void revokePersonalAccessToken(String tokenId) throws SynapseException;

	public AccessControlList updateEvaluationAcl(AccessControlList acl)
			throws SynapseException;

	public AccessControlList getEvaluationAcl(String evalId) throws SynapseException;

	public UserEvaluationPermissions getUserEvaluationPermissions(String evalId)
			throws SynapseException;

	/**
	 * Delete rows from table entity.
	 * 
	 * @param toDelete
	 * @return
	 * @throws SynapseException
	 * @throws SynapseTableUnavailableException
	 */
	public RowReferenceSet deleteRowsFromTable(RowSelection toDelete) throws SynapseException, SynapseTableUnavailableException;

	/**
	 * Get the file handles for the requested file handle columns for the rows.
	 * 
	 * @param fileHandlesToFind rows set for the rows and columns for which file handles need to be returned
	 * @return
	 * @throws IOException
	 * @throws SynapseException
	 */
	public TableFileHandleResults getFileHandlesFromTable(RowReferenceSet fileHandlesToFind) throws SynapseException;

	/**
	 * Get the temporary URL for the data file of a file handle column for a row. This is an alternative to downloading
	 * the file.
	 * 
	 * @param row
	 * @param column
	 * @return
	 * @throws IOException
	 * @throws SynapseException
	 */
	public URL getTableFileHandleTemporaryUrl(String tableId, RowReference row, String column) throws IOException, SynapseException;

	/**
	 * Get the temporary URL for the data file of a file handle column for a row. This is an alternative to downloading
	 * the file.
	 * 
	 * @param row
	 * @param column
	 * @return
	 * @throws IOException
	 * @throws SynapseException
	 */
	public void downloadFromTableFileHandleTemporaryUrl(String tableId, RowReference row, String column, File destinationFile)
			throws SynapseException;

	/**
	 * Get the temporary URL for the preview of a file handle column for a row. This is an alternative to downloading
	 * the file.
	 * 
	 * @param row
	 * @param column
	 * @return
	 * @throws IOException
	 * @throws SynapseException
	 */
	public URL getTableFileHandlePreviewTemporaryUrl(String tableId, RowReference row, String column) throws IOException, SynapseException;

	/**
	 * Get the temporary URL for preview of a file handle column for a row. This is an alternative to downloading the
	 * file.
	 * 
	 * @param row
	 * @param column
	 * @return
	 * @throws IOException
	 * @throws SynapseException
	 */
	public void downloadFromTableFileHandlePreviewTemporaryUrl(String tableId, RowReference row, String column, File destinationFile)
			throws SynapseException;

	/**
	 * Query for data in a table entity asynchronously. The bundled version of the query returns more information than
	 * just the query result. The parts included in the bundle are determined by the passed mask.
	 * 
	 * <p>
	 * The 'partMask' is an integer "mask" that can be combined into to request any desired part. As of this writing,
	 * the mask is defined as follows:
	 * <ul>
	 * <li>Query Results <i>(queryResults)</i> = 0x1</li>
	 * <li>Query Count <i>(queryCount)</i> = 0x2</li>
	 * <li>Select Columns <i>(selectColumns)</i> = 0x4</li>
	 * <li>Max Rows Per Page <i>(maxRowsPerPage)</i> = 0x8</li>
	 * </ul>
	 * </p>
	 * <p>
	 * For example, to request all parts, the request mask value should be: <br>
	 * 0x1 OR 0x2 OR 0x4 OR 0x8 = 0x15.
	 * </p>
	 * 
	 * @param sql
	 * @param partMask
	 * @param tableId the id of the TableEntity.
	 * @return
	 * @throws SynapseException
	 * @throws SynapseTableUnavailableException
	 */
	public static final int QUERY_PARTMASK = 0x1;
	public static final int COUNT_PARTMASK = 0x2;
	public static final int COLUMNS_PARTMASK = 0x4;
	public static final int MAXROWS_PARTMASK = 0x8;
	
	public String queryTableEntityBundleAsyncStart(Query query, QueryOptions queryOptions, String tableId)
			throws SynapseException;

	public String queryTableEntityBundleAsyncStart(String sql, Long offset, Long limit, int partMask, String tableId)
			throws SynapseException;

	/**
	 * Get the result of an asynchronous queryTableEntityBundle
	 * 
	 * @param asyncJobToken
	 * @param tableId the id of the TableEntity.
	 * @return
	 * @throws SynapseException
	 * @throws SynapseTableUnavailableException
	 */
	public QueryResultBundle queryTableEntityBundleAsyncGet(String asyncJobToken, String tableId) throws SynapseException, SynapseResultNotReadyException;

	/**
	 * Query for data in a table entity. Start an asynchronous version of queryTableEntityNextPage
	 * 
	 * @param nextPageToken
	 * @param tableId the id of the TableEntity.
	 * @return a token to get the result with
	 * @throws SynapseException
	 * @throws SynapseTableUnavailableException
	 */
	public String queryTableEntityNextPageAsyncStart(String nextPageToken, String tableId) throws SynapseException, SynapseResultNotReadyException;
	
	/**
	 * Start an Asynchronous job of the given type.
	 * @param type The type of job.
	 * @param request The request body.
	 * @return The jobId is used to get the job results.
	 */
	public String startAsynchJob(AsynchJobType type, AsynchronousRequestBody request) throws SynapseException;
	
	/**
	 * Attempt to cancel an Asynchronous job. Not all jobs can be canceled and cancelation is not immediate (wait for
	 * job to finish with ERROR if you need to make sure it was canceled)
	 *
	 * @return The jobId is used to get the job results.
	 */
	public void cancelAsynchJob(String jobId) throws SynapseException;

	/**
	 * Get the results of an Asynchronous job.
	 * 
	 * @param type The type of job.
	 * @param jobId The JobId.
	 * @param request
	 * @throws SynapseResultNotReadyException if the job is not ready.
	 * @return
	 */
	public AsynchronousResponseBody getAsyncResult(AsynchJobType type, String jobId, AsynchronousRequestBody request) throws SynapseException, SynapseResultNotReadyException;

	/**
	 * Get the results of an Asynchronous job.
	 * @param type The type of job.
	 * @param jobId The JobId.
	 * @param entityId
	 * @throws SynapseResultNotReadyException if the job is not ready.
	 * @return
	 */
	public AsynchronousResponseBody getAsyncResult(AsynchJobType type, String jobId, String entityId) throws SynapseException, SynapseResultNotReadyException;

	/**
	 * Get the results of an Asynchronous job.
	 * @param type The type of job.
	 * @param jobId The JobId.
	 * @throws SynapseResultNotReadyException if the job is not ready.
	 * @return
	 */
	public AsynchronousResponseBody getAsyncResult(AsynchJobType type, String jobId) throws SynapseException, SynapseResultNotReadyException;

	/**
	 * Get the result of an asynchronous queryTableEntityNextPage
	 * 
	 * @param asyncJobToken
	 * @param tableId the id of the TableEntity.
	 * @return
	 * @throws SynapseException
	 * @throws SynapseTableUnavailableException
	 */
	public QueryResult queryTableEntityNextPageAsyncGet(String asyncJobToken, String tableId) throws SynapseException;

	/**
	 * upload a csv into an existing table
	 * 
	 * @param tableId the table to upload into
	 * @param fileHandleId the filehandle of the csv
	 * @param etag when updating rows, the etag of the last table change must be provided
	 * @param linesToSkip The number of lines to skip from the start of the file (default 0)
	 * @param csvDescriptor The optional descriptor of the csv (default comma separators, double quotes for quoting, new
	 *        lines and backslashes for escaping)
	 * @return a token to get the result with
	 * @throws SynapseException
	 * @throws SynapseTableUnavailableException
	 */
	public String uploadCsvToTableAsyncStart(String tableId, String fileHandleId, String etag, Long linesToSkip,
			CsvTableDescriptor csvDescriptor, List<String> columnIds) throws SynapseException;

	/**
	 * get the result of a csv upload
	 * 
	 * @param asyncJobToken
	 * @param tableId the id of the TableEntity.
	 * @return
	 * @throws SynapseException
	 * @throws SynapseTableUnavailableException
	 */
	public UploadToTableResult uploadCsvToTableAsyncGet(String asyncJobToken, String tableId) throws SynapseException, SynapseResultNotReadyException;

	/**
	 * download the result of a query into a csv
	 * 
	 * @param sql the query to run
	 * @param writeHeader should the csv contain the column header as row 1
	 * @param includeRowIdAndRowVersion should the row id and row version be included as the first 2 columns
	 * @param csvDescriptor the optional descriptor of the csv (default comma separators, double quotes for quoting, new
	 *        lines and backslashes for escaping)
	 * @param tableId the id of the TableEntity.
	 * @return a token to get the result with
	 * @throws SynapseException
	 */
	public String downloadCsvFromTableAsyncStart(String sql, boolean writeHeader, boolean includeRowIdAndRowVersion,
			CsvTableDescriptor csvDescriptor, String tableId) throws SynapseException;

	/**
	 * get the results of the csv download
	 * 
	 * @param asyncJobToken
	 * @param tableId the id of the TableEntity.
	 * @return
	 * @throws SynapseException
	 * @throws SynapseResultNotReadyException
	 */
	public DownloadFromTableResult downloadCsvFromTableAsyncGet(String asyncJobToken, String tableId) throws SynapseException, SynapseResultNotReadyException;
	
	/**
	 * Start an asynchronous job to append data to a table.
	 * @param rowSet Data to append.
	 * @param tableId the id of the TableEntity.
	 * @return JobId token that can be used get the results of the append.
	 */
	public String appendRowSetToTableStart(AppendableRowSet rowSet, String tableId) throws SynapseException;
	
	/**
	 * Get the results of a table append RowSet job using the jobId token returned when the job was started.
	 * @param token
	 * @param tableId
	 * @return
	 * @throws SynapseException
	 * @throws SynapseResultNotReadyException
	 */
	public RowReferenceSet appendRowSetToTableGet(String token, String tableId) throws SynapseException, SynapseResultNotReadyException;
	
	/**
	 * Run an asynchronous to append data to a table.
	 * Note: This is a convenience function that wraps the start job and get loop of an asynchronous job.
	 * @param rowSet
	 * @param timeout
	 * @param tableId the id of the TableEntity.
	 * @return
	 * @throws SynapseException 
	 * @throws InterruptedException 
	 */
	public RowReferenceSet appendRowsToTable(AppendableRowSet rowSet, long timeout, String tableId) throws SynapseException, InterruptedException;

	/**
	 * Create a new ColumnModel. If a column already exists with the same parameters,
	 * that column will be returned.
	 * @param model
	 * @return
	 * @throws SynapseException 
	 */
	ColumnModel createColumnModel(ColumnModel model) throws SynapseException;
	
	/**
	 * Create new ColumnModels. If a column already exists with the same parameters, that column will be returned.
	 * 
	 * @param models
	 * @return
	 * @throws SynapseException
	 */
	List<ColumnModel> createColumnModels(List<ColumnModel> models) throws SynapseException;

	/**
	 * Get a ColumnModel from its ID.
	 * 
	 * @param columnId
	 * @return
	 * @throws SynapseException
	 */
	ColumnModel getColumnModel(String columnId) throws SynapseException;
	
	/**
	 * Get the default columns for a given view type.
	 * 
	 * @param viewType
	 * @return
	 * @throws SynapseException
	 */
	List<ColumnModel> getDefaultColumnsForView(ViewType viewType) throws SynapseException;
	
	/**
	 * Get the default columns for a given view entity type and type mask.
	 * 
	 * @param viewEntityType The view entity type, supports entityview and submissionview
	 * @param viewTypeMask
	 *            Bit mask representing the types to include in the view. The
	 *            following are the possible types when the viewEntityType is entityview: (type=<mask_hex>): File=0x01,
	 *            Project=0x02, Table=0x04, Folder=0x08, View=0x10, Docker=0x20. For a viewEntityType of submissionview the mask 
	 *            is not required

	 * @return
	 * @throws SynapseException
	 */
	List<ColumnModel> getDefaultColumnsForView(ViewEntityType viewEntityType, Long viewTypeMask) throws SynapseException;
	
	// Team services
	
	/**
	 * 
	 * @param team
	 * @return
	 * @throws SynapseException
	 */
	Team createTeam(Team team) throws SynapseException;
	
	/**
	 * 
	 * @param id
	 * @return
	 * @throws SynapseException
	 */
	Team getTeam(String id) throws SynapseException;
	
	/**
	 * 
	 * @param fragment if null then return all teams
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 */
	PaginatedResults<Team> getTeams(String fragment, long limit, long offset) throws SynapseException;
	
	/**
	 * Return a list of Teams given a list of Team IDs.
	 * 
	 * Note: Invalid IDs in the list are ignored:  The results list is simply
	 * smaller than the set of IDs passed in.
	 *
	 * 
	 * @param ids
	 * @return
	 * @throws SynapseException
	 */
	public List<Team> listTeams(List<Long> ids) throws SynapseException;
	
	/**
	 * 
	 * @param memberId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 */
	PaginatedResults<Team> getTeamsForUser(String memberId, long limit, long offset) throws SynapseException;
	
	/**
	 * Get the URL to follow to download the icon
	 * 
	 * @param teamId
	 * @return
	 * @throws SynapseException if no icon for team (service throws 404)
	 */
	URL getTeamIcon(String teamId) throws SynapseException;
	
	/**
	 * 
	 * @param teamId
	 * @param target
	 * @throws SynapseException
	 */
	public void downloadTeamIcon(String teamId, File target) throws SynapseException;
	
	/**
	 * 
	 * @param team
	 * @return
	 * @throws SynapseException
	 */
	Team updateTeam(Team team) throws SynapseException;
	
	/**
	 * 
	 * @param teamId
	 * @throws SynapseException
	 */
	void deleteTeam(String teamId) throws SynapseException;
	
	/**
	 * Add a member to a Team
	 * @param teamId
	 * @param memberId
	 * @param teamEndpoint the portal prefix for the Team URL. The team ID is appended to create the complete URL.
	 * @param notificationUnsubscribeEndpoint the portal prefix for one-click email unsubscription.
	 * @throws SynapseException
	 */
	void addTeamMember(String teamId, String memberId, 
			String teamEndpoint,
			String notificationUnsubscribeEndpoint) throws SynapseException;
	
	/**
	 * Add a member to a Team
	 * @param joinTeamSignedToken an object, signed by Synapse, containing the team and 
	 * member Ids as well as the Id of the user authenticated in the request.
	 * @param teamEndpoint the portal prefix for the Team URL. The team ID is appended to create the complete URL.
	 * @param notificationUnsubscribeEndpoint the portal prefix for one-click email unsubscription.
	 * @throws SynapseException
	 */
	ResponseMessage addTeamMember(JoinTeamSignedToken joinTeamSignedToken, 
			String teamEndpoint,
			String notificationUnsubscribeEndpoint) throws SynapseException;

	/**
	 * Return the members of the given team matching the given name fragment.
	 *
	 * @param teamId
	 * @param fragment if null then return all members in the team
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 */
	PaginatedResults<TeamMember> getTeamMembers(String teamId, String fragment, long limit, long offset) throws SynapseException;

	/**
	 * Return the members of the given team matching the given name fragment.
	 * 
	 * @param teamId
	 * @param fragment if null then return all members in the team
	 * @param memberType if null then return all members in the team (that match the fragment)
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 */
	PaginatedResults<TeamMember> getTeamMembers(String teamId, String fragment, TeamMemberTypeFilterOptions memberType, long limit, long offset) throws SynapseException;
	
	/**
	 * 
	 * @param teamId
	 * @param fragment
	 * @return the number of members in the given team, optionally filtered by the given prefix
	 * @throws SynapseException
	 */
	long countTeamMembers(String teamId, String fragment) throws SynapseException;
	
	/**
	 * Return the TeamMember object for the given team and member
	 * @param teamId
	 * @param memberId
	 * @return
	 * @throws SynapseException
	 */
	TeamMember getTeamMember(String teamId, String memberId) throws SynapseException;

	/**
	 * Return a TeamMember list for a given Team and list of member IDs.
	 * 
	 * Note: Any invalid ID causes a 404 NOT FOUND
	 * 
	 * @param teamId
	 * @param ids
	 * @return
	 * @throws SynapseException
	 */
	public List<TeamMember> listTeamMembers(String teamId, List<Long> ids) throws SynapseException;

	
	/**
	 * Return a TeamMember list for a set of Team IDs and a given user
	 * 
	 * Note: Any invalid ID causes a 404 NOT FOUND
	 * 
	 * @param teamIds
	 * @param userId
	 * @return
	 * @throws SynapseException
	 */
	public List<TeamMember> listTeamMembers(List<Long> teamIds, String userId) throws SynapseException;

	/**
	 * 
	 * @param teamId
	 * @param memberId
	 * @throws SynapseException
	 */
	void removeTeamMember(String teamId, String memberId) throws SynapseException;
	
	/**
	 * @param teamId
	 * @param memberId
	 * @param isAdmin
	 * @throws SynapseException
	 */
	void setTeamMemberPermissions(String teamId, String memberId, boolean isAdmin) throws SynapseException;

	/**
	 * Get the membership status of the given member (principalId) in the given Team
	 * @param teamId
	 * @param principalId
	 * @return
	 * @throws SynapseException
	 */
	TeamMembershipStatus getTeamMembershipStatus(String teamId, String principalId) throws SynapseException;
	
	/**
	 * Get the ACL for the given Team
	 * @param teamId
	 * @return
	 * @throws SynapseException 
	 */
	AccessControlList getTeamACL(String teamId) throws SynapseException;
	
	/**
	 * Update the ACL for the given Team
	 * @param acl
	 * @return
	 * @throws SynapseException 
	 */
	AccessControlList updateTeamACL(AccessControlList acl) throws SynapseException;

	/**
	 * Create a membership invitation. The team must be specified. Also, either an inviteeId or an inviteeEmail must be
	 * specified. If an inviteeId is specified, the invitee is notified of the invitation through a notification.
	 * If an inviteeEmail is specified instead, an email containing an invitation link is sent to the invitee. The link
	 * will contain a serialized MembershipInvtnSignedToken.
	 * Optionally, the creator may include an invitation message and/or expiration date for the invitation.
	 * If no expiration date is specified then the invitation never expires.
	 * Note:  The client must be an administrator of the specified Team to make this request.
	 *
	 * @param invitation
	 * @param acceptInvitationEndpoint the portal end-point for one-click acceptance of the membership
	 * invitation.  A signed, serialized token is appended to create the complete URL.
	 * @param notificationUnsubscribeEndpoint the portal prefix for one-click email unsubscription.
	 * A signed, serialized token is appended to create the complete URL.
	 * @return
	 * @throws SynapseException
	 */
	MembershipInvitation createMembershipInvitation(
			MembershipInvitation invitation,
			String acceptInvitationEndpoint, 
			String notificationUnsubscribeEndpoint ) throws SynapseException;

	/**
	 *
	 * @param invitationId
	 * @return
	 * @throws SynapseException
	 */
	MembershipInvitation getMembershipInvitation(String invitationId) throws SynapseException;

	/**
	 * Retrieve membership invitation using a signed token for authorization
	 *
	 * @param token
	 * @return
	 * @throws SynapseException
	 */
	MembershipInvitation getMembershipInvitation(MembershipInvtnSignedToken token) throws SynapseException;

	/**
	 * 
	 * @param memberId
	 * @param teamId the team for which the invitations are extended (optional)
	 * @param limit
	 * @param offset
	 * @return a list of open invitations to the given member, optionally filtered by team
	 * @throws SynapseException
	 */
	PaginatedResults<MembershipInvitation> getOpenMembershipInvitations(String memberId, String teamId, long limit, long offset) throws SynapseException;

	/**
	 * 
	 * @param teamId
	 * @param inviteeId
	 * @param limit
	 * @param offset
	 * @return a list of open invitations issued by a team, optionally filtered by invitee
	 * @throws SynapseException
	 */
	PaginatedResults<MembershipInvitation> getOpenMembershipInvitationSubmissions(String teamId, String inviteeId, long limit, long offset) throws SynapseException;

	/**
	 * 
	 * @param invitationId
	 * @throws SynapseException
	 */
	void deleteMembershipInvitation(String invitationId) throws SynapseException;

	/**
	 * Retrieve the number of pending Membership Invitations
	 * @return
	 * @throws SynapseException
	 */
	Count getOpenMembershipInvitationCount() throws SynapseException;

	/**
	 * Verify whether the inviteeEmail of the indicated MembershipInvitation is associated with the authenticated user.
	 * If it is, the response body will contain an InviteeVerificationSignedToken.
	 * If it is not, a response status 403 Forbidden will be returned.
	 * @param membershipInvitationId
	 * @return
	 */
	InviteeVerificationSignedToken getInviteeVerificationSignedToken(String membershipInvitationId) throws SynapseException;

	/**
	 * Set the inviteeId of a MembershipInvitation.
	 * A valid InviteeVerificationSignedToken must have an inviteeId equal to the id of
	 * the authenticated user and a membershipInvitationId equal to the id in the URI.
	 * This call will only succeed if the indicated MembershipInvitation has a
	 * null inviteeId and a non null inviteeEmail.
	 * @param membershipInvitationId
	 * @param token
	 */
	void updateInviteeId(String membershipInvitationId, InviteeVerificationSignedToken token) throws SynapseException;

	/**
	 * 
	 * @param request
	 * @param acceptRequestEndpoint the portal end-point for one-click acceptance of the membership
	 * request.  A signed, serialized token is appended to create the complete URL.
	 * @param notificationUnsubscribeEndpoint the portal prefix for one-click email unsubscription.
	 * A signed, serialized token is appended to create the complete URL.
	 * @return
	 * @throws SynapseException
	 */
	MembershipRequest createMembershipRequest(MembershipRequest request,
	                                          String acceptRequestEndpoint,
	                                          String notificationUnsubscribeEndpoint) throws SynapseException;
	/**
	 * 
	 * @param requestId
	 * @return
	 * @throws SynapseException
	 */
	MembershipRequest getMembershipRequest(String requestId) throws SynapseException;

	/**
	 * 
	 * @param teamId
	 * @param requesterId the id of the user requesting membership (optional)
	 * @param limit
	 * @param offset
	 * @return a list of membership requests sent to the given team, optionally filtered by the requester
	 * @throws SynapseException
	 */
	PaginatedResults<MembershipRequest> getOpenMembershipRequests(String teamId, String requesterId, long limit, long offset) throws SynapseException;

	/**
	 * 
	 * @param requesterId
	 * @param teamId
	 * @param limit
	 * @param offset
	 * @return a list of membership requests from a requester, optionally filtered by the team to which the request was sent
	 * @throws SynapseException
	 */
	PaginatedResults<MembershipRequest> getOpenMembershipRequestSubmissions(String requesterId, String teamId, long limit, long offset) throws SynapseException;

	/**
	 * 
	 * @param requestId
	 * @throws SynapseException
	 */
	void deleteMembershipRequest(String requestId) throws SynapseException;

	

	/**
	 * Retrieve the number of pending Membership Requests for teams that user is admin
	 * @return
	 * @throws SynapseException
	 */
	Count getOpenMembershipRequestCount() throws SynapseException;

	/** Get the List of ColumnModels for TableEntity given the TableEntity's ID.
	 * 
	 * @param tableEntityId
	 * @return
	 * @throws SynapseException 
	 */
	List<ColumnModel> getColumnModelsForTableEntity(String tableEntityId) throws SynapseException;
	
	/**
	 * List all of the ColumnModes in Synapse with pagination.
	 * @param prefix - When provided, only ColumnModels with names that start with this prefix will be returned.
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException 
	 */
	PaginatedColumnModels listColumnModels(String prefix, Long limit, Long offset) throws SynapseException;

	public void sendNewPasswordResetEmail(String passwordResetEndpoint, String email) throws SynapseException;

	/**
	 * Change password for a user
	 * @param username username to identify the user
	 * @param currentPassword the user's current password
	 * @param newPassword the new password for the user
	 * @param authenticationReceipt Optional. Authentication receipt from a previous, successful login.
	 * @throws SynapseException
	 */
	public void changePassword(String username, String currentPassword, String newPassword, String authenticationReceipt)
			throws SynapseException;

	/**
	 * Change password for user
	 * @param changePasswordRequest the request object for changing the user's password
	 * @throws SynapseException
	 */
	public void changePassword(ChangePasswordInterface changePasswordRequest) throws SynapseException;

	/**
	 * Signs the terms of use for utilization of Synapse, as identified by a session token
	 */
	public void signTermsOfUse(String sessionToken, boolean acceptTerms) throws SynapseException;

	/**
	 * The first step in OAuth authentication involves sending the user to
	 * authenticate on an OAuthProvider's web page. Use this method to get a
	 * properly formed URL to redirect the browser to an OAuthProvider's
	 * authentication page.
	 * 
	 * Upon successful authentication at the OAuthProvider's page, the provider
	 * will redirect the browser to the redirectURL. The provider will add a query
	 * parameter to the redirect URL named "code". The code parameter's value is
	 * an authorization code that must be provided to Synapse to validate a
	 * user.
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	OAuthUrlResponse getOAuth2AuthenticationUrl(OAuthUrlRequest request)
			throws SynapseException;
	
	/**
	 * After a user has been authenticated at an OAuthProvider's web page, the
	 * provider will redirect the browser to the provided redirectUrl. The
	 * provider will add a query parameter to the redirectUrl called "code" that
	 * represent the authorization code for the user. This method will use the
	 * authorization code to validate the user and fetch information about the
	 * user from the OAuthProvider. If successful, a session token for the user
	 * will be returned.
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException
	 * @throws NotFoundException if the user does not exist in Synapse.
	 */
	Session validateOAuthAuthenticationCode(OAuthValidationRequest request)
			throws SynapseException;
	

	/**
	 * 
	 * After a user has been authenticated at an OAuthProvider's web page, the
	 * provider will redirect the browser to the provided redirectUrl. The
	 * provider will add a query parameter to the redirectUrl called "code" that
	 * represent the authorization code for the user. This method will use the
	 * authorization code to validate the user and fetch the user's email address
	 * from the OAuthProvider. If there is no existing account using the email address
	 * from the provider then a new account will be created, the user will be authenticated,
	 * and a session will be returned.
	 * 
	 * If the email address from the provider is already associated with an account or
	 * if the passed user name is used by another account then the request will
	 * return HTTP Status 409 Conflict.
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	Session createAccountViaOAuth2(OAuthAccountCreationRequest request) throws SynapseException;
	
	/**
	 * After a user has been authenticated at an OAuthProvider's web page, the
	 * provider will redirect the browser to the provided redirectUrl. The
	 * provider will add a query parameter to the redirectUrl called "code" that
	 * represent the authorization code for the user. This method will use the
	 * authorization code to validate the code, retrieve the provider's ID for
	 * the user and bind it to the user's Synapse account.
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException
	 * @throws NotFoundException if the user does not exist in Synapse.
	 */
	PrincipalAlias bindOAuthProvidersUserId(OAuthValidationRequest request)
			throws SynapseException;
	
	/**
	 * Remove an alias associated with an account via the OAuth mechanism.
	 * 
	 * @param provider
	 * @param alias
	 * @throws SynapseException
	 */
	void unbindOAuthProvidersUserId(OAuthProvider provider, String alias) throws SynapseException;
	
	/**
	 * Get the Open ID Configuration ("Discovery Document") for the Synapse OIDC service.
	 * 
	 * @return the configuration
	 * @throws SynapseException
	 */
	OIDConnectConfiguration getOIDConnectConfiguration() throws SynapseException;
	
	/**
	 * Get the JSON Web Key Set for the Synapse OIDC service.  This is the set of public keys
	 * used to verify signed JSON Web tokens generated by Synapse.
	 * 
	 * @return
	 * @throws SynapseException
	 */
	JsonWebKeySet getOIDCJsonWebKeySet() throws SynapseException;
	
	/**
	 * Create an OAuth 2.0 client.
	 * 
	 * @param oauthClient
	 * @return
	 * @throws SynapseException
	 */
	OAuthClient createOAuthClient(OAuthClient oauthClient) throws SynapseException;
	
	/**
	 * Get a secret credential to use when requesting an access token.  Only the creator
	 * of a client can (re)set its secret.
	 * 
	 * <br>
	 * See https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication
	 * <br>
	 * Synapse supports 'client_secret_basic'.
	 * <br>
	 * <em>NOTE:  This request will invalidate any previously issued secrets.</em>
	 * 
	 * @param clientId
	 * 
	 * @return
	 * @throws SynapseException
	 */
	OAuthClientIdAndSecret createOAuthClientSecret(String clientId) throws SynapseException;
	
	/**
	 * Get an existing OAuth 2.0 client.  Note: If the request is made by anyone other
	 * than the creator, only 'public' fields are returned.
	 * 
	 * @param clientId
	 * @return
	 * @throws SynapseException
	 */
	OAuthClient getOAuthClient(String clientId) throws SynapseException;
	
	/**
	 * List the OAuth 2.0 clients created by the given user.
	 * 
	 * @param nextPageToken returned along with a page of results, this is passed to 
	 * the server to retrieve the next page.
	 * 
	 * @return
	 * @throws SynapseException
	 */
	OAuthClientList listOAuthClients(String nextPageToken) throws SynapseException;
	
	/**
	 * Update the metadata for an existing OAuth 2.0 client
	 * 
	 * @param oauthClient
	 * @return
	 * @throws SynapseException
	 */
	OAuthClient updateOAuthClient(OAuthClient oauthClient) throws SynapseException;
	
	/**
	 * Delete OAuth 2.0 client
	 * 
	 * @param id
	 * @throws SynapseException
	 */
	void deleteOAuthClient(String id) throws SynapseException;
	
	/**
	 * Get a user-readable description of the authentication request.
	 * This request does not need to be authenticated.
	 * 
	 * @param authorizationRequest
	 * @return
	 * @throws SynapseException
	 */
	OIDCAuthorizationRequestDescription getAuthenticationRequestDescription(
			OIDCAuthorizationRequest authorizationRequest) throws SynapseException;
	
	/**
	 * Checks whether user has already authorized the OAuth client for the given scope/claims.
	 * 
	 * @param authorizationRequest
	 * @return true iff the user has already given their authorization
	 * @throws SynapseException
	 */
	boolean hasUserAuthorizedClient(OIDCAuthorizationRequest authorizationRequest) throws SynapseException;
	
	/**
	 * get access code for a given client, scopes, response type(s), and extra claim(s).
	 * This request does not need to be authenticated.
	 * 
	 * See:
	 * https://openid.net/specs/openid-connect-core-1_0.html#Consent
	 * https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest
	 * 
	 * @param authorizationRequest
	 * @return
	 * @throws SynapseException
	 */
	OAuthAuthorizationResponse authorizeClient(OIDCAuthorizationRequest authorizationRequest) throws SynapseException;
			
	 /** 
	 *  Get access, refresh and id tokens, as per https://openid.net/specs/openid-connect-core-1_0.html#TokenResponse
	 *  
	 *  Request must include client ID and Secret in Basic Authentication header, i.e. the 'client_secret_basic' authentication method: 
	 *  https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication
	 *  
	 * @param grant_type  authorization_code or refresh_token
	 * @param code required if grant_type is authorization_code
	 * @param redirectUri required if grant_type is authorization_code
	 * @param refresh_token required if grant_type is refresh_token
	 * @param scope required if grant_type is refresh_token
	 * @param claims optional if grant_type is refresh_token
	*/
	OIDCTokenResponse getTokenResponse(
			OAuthGrantType grant_type,
			String code,
			String redirectUri,
			String refresh_token,
			String scope,
			String claims) throws SynapseException;
	
	/**
	 * Get the user information for the user specified by the authorization
	 * bearer token (which must be included as the authorization header).
	 * 
	 * The result is expected to be a JWT token, which is invoked by the 
	 * client having registered a 'user info signed response algorithm'.
	 * 
	 * @return
	 */
	Jwt<JwsHeader,Claims> getUserInfoAsJSONWebToken() throws SynapseException;
	
	/**
	 * Get the user information for the user specified by the authorization
	 * bearer token (which must be included as the authorization header).
	 * 
	 * The result is expected to be a Map, which is invoked by the 
	 * client having omitted a 'user info signed response algorithm'.
	 * 
	 * @return
	 */
	JSONObject getUserInfoAsJSON() throws SynapseException;

	/**
	 * Get a paginated record of the OAuth clients that currently have access to the
	 * logged-in user's Synapse resources via OAuth 2.0 refresh tokens.
	 * @param nextPageToken
	 * @return a paginated list containing OAuth 2 clients and relevant authorization and access dates.
	 */
	OAuthClientAuthorizationHistoryList getClientAuthorizationHistory(String nextPageToken) throws SynapseException;

	/**
	 * Get a paginated list of metadata related to active refresh tokens that an OAuth 2.0 client
	 * may use to access the logged-in user's resources.
	 * @param clientId
	 * @param nextPageToken
	 * @return
	 */
	OAuthRefreshTokenInformationList getRefreshTokenMetadataForAuthorizedClient(String clientId, String nextPageToken) throws SynapseException;

	/**
	 * Revokes all refresh tokens that the logged in user has granted to the specified client. Note that access tokens that
	 * have been granted without a refresh token will continue to work until they expire.
	 * @param clientId
	 * @return
	 */
	void revokeMyRefreshTokensFromClient(String clientId) throws SynapseException;

	/**
	 * Revokes a particular refresh token. Note: any access tokens granted via this refresh token will also
	 * be revoked.
	 */
	void revokeRefreshToken(String refreshTokenId) throws SynapseException;

	/**
	 * Revokes a refresh token using the token itself, or a supplied access token.
	 * Note: if the access token is not associated with a refresh token, it cannot be revoked.
	 */
	void revokeToken(OAuthTokenRevocationRequest revocationRequest) throws SynapseException;

	/**
	 * Updates the metadata for a particular refresh token.
	 * @param metadata
	 * @return
	 */
	OAuthRefreshTokenInformation updateRefreshTokenMetadata(OAuthRefreshTokenInformation metadata) throws SynapseException;

	/**
	 * Gets the metadata for a particular refresh token.
	 * @param tokenId
	 * @return
	 */
	OAuthRefreshTokenInformation getRefreshTokenMetadata(String tokenId) throws SynapseException;

	/**
	 * Gets the metadata for a particular refresh token.
	 * @param tokenId
	 * @return
	 */
	OAuthRefreshTokenInformation getRefreshTokenMetadataAsOAuthClient(String tokenId) throws SynapseException;

	/**
	 * Get the Quiz specifically intended to be the Certified User test
	 * @return
	 * @throws SynapseException
	 */
	public Quiz getCertifiedUserTest() throws SynapseException;
	
	/**
	 * Submit the response to the Certified User test
	 * @param response
	 * @return
	 * @throws SynapseException 
	 */
	public PassingRecord submitCertifiedUserTestResponse(QuizResponse response) throws SynapseException;
	
	/**
	 * Get the Passing Record on the Certified User test for the given user
	 * 
	 * @param principalId
	 * @return
	 * @throws SynapseException 
	 */
	public PassingRecord getCertifiedUserPassingRecord(String principalId) throws SynapseException;

	/**
	 * Start a new Asynchronous Job
	 * @param jobBody
	 * @return
	 * @throws SynapseException 
	 */
	public AsynchronousJobStatus startAsynchronousJob(AsynchronousRequestBody jobBody)
			throws SynapseException;

	/**
	 * Get the status of an Asynchronous Job from its ID.
	 * @param jobId
	 * @return
	 * @throws SynapseException 
	 */
	public AsynchronousJobStatus getAsynchronousJobStatus(String jobId) throws SynapseException;

	/**
	 * Get a Temporary URL that can be used to download a FileHandle.  Only the creator of a FileHandle can use this method.
	 * 
	 * @param fileHandleId
	 * @return
	 * @throws IOException
	 * @throws SynapseException
	 */
	URL getFileHandleTemporaryUrl(String fileHandleId) throws IOException,
			SynapseException;

	/**
	 * Download A file contain 
	 * @param fileHandleId
	 * @param destinationFile
	 * @throws SynapseException
	 */
	void downloadFromFileHandleTemporaryUrl(String fileHandleId, File destinationFile) throws SynapseException;

	/**
	 * Log an error
	 * 
	 * @param logEntry
	 * @throws SynapseException
	 */
	void logError(LogEntry logEntry) throws SynapseException;

	@Deprecated
	public List<UploadDestination> getUploadDestinations(String parentEntityId) throws SynapseException;

	/**
	 * create a new upload destination setting
	 *
	 * @param storageLocationSetting
	 * @return
	 * @throws SynapseException
	 */
	public <T extends StorageLocationSetting> T createStorageLocationSetting(T storageLocationSetting)
			throws SynapseException;

	/**
	 * get an upload destination setting (owned by me)
	 * 
	 * @param storageLocationId
	 * @return
	 * @throws SynapseException
	 */
	public <T extends StorageLocationSetting> T getMyStorageLocationSetting(Long storageLocationId) throws SynapseException;

	/**
	 * get a list of my upload destination settings
	 *
	 * @return
	 * @throws SynapseException
	 */
	@Deprecated
	public <T extends StorageLocationSetting> List<T> getMyStorageLocationSettings() throws SynapseException;

	/**
	 * get all upload destination locations for a container
	 * 
	 * @param parentEntityId
	 * @return
	 * @throws SynapseException
	 */
	public UploadDestinationLocation[] getUploadDestinationLocations(String parentEntityId) throws SynapseException;

	/**
	 * get the upload destination for a container and upload location id
	 * 
	 * @param parentEntityId
	 * @param uploadId
	 * @return
	 * @throws SynapseException
	 */
	public UploadDestination getUploadDestination(String parentEntityId, Long uploadId) throws SynapseException;

	/**
	 * get the default upload destination for a container
	 * 
	 * @param parentEntityId
	 * @return
	 * @throws SynapseException
	 */
	public UploadDestination getDefaultUploadDestination(String parentEntityId) throws SynapseException;

	/**
	 * create a project setting
	 *
	 * @param projectSetting
	 * @throws SynapseException
	 */
	ProjectSetting createProjectSetting(ProjectSetting projectSetting) throws SynapseException;

	/**
	 * create a project setting
	 * 
	 * @param projectId
	 * @param projectSettingsType
	 * @throws SynapseException
	 */
	ProjectSetting getProjectSetting(String projectId, ProjectSettingsType projectSettingsType) throws SynapseException;

	/**
	 * create a project setting
	 *
	 * @param projectSetting
	 * @throws SynapseException
	 */
	void updateProjectSetting(ProjectSetting projectSetting) throws SynapseException;

	/**
	 * create a project setting
	 * 
	 * @param projectSettingsId
	 * @throws SynapseException
	 */
	void deleteProjectSetting(String projectSettingsId) throws SynapseException;

	/**
	 * Start a job to generate a preview for an upload CSV to Table.
	 * Get the results using {@link #uploadCsvToTablePreviewAsyncGet(String)}
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	String uploadCsvTablePreviewAsyncStart(UploadToTablePreviewRequest request) throws SynapseException;

	/**
	 * Get the resulting preview from the job started with {@link #uploadCsvTablePreviewAsyncStart(UploadToTablePreviewRequest)}
	 * @param asyncJobToken
	 * @return
	 * @throws SynapseException
	 * @throws SynapseResultNotReadyException
	 */
	UploadToTablePreviewResult uploadCsvToTablePreviewAsyncGet(String asyncJobToken)
			throws SynapseException, SynapseResultNotReadyException;

	/**
	 * Creates and returns a new Challenge.  Caller must have CREATE
	 * permission on the associated Project.
	 * 
	 * @param challenge
	 * @return
	 * @throws SynapseException
	 */
	Challenge createChallenge(Challenge challenge) throws SynapseException;

	/**
	 * Returns the Challenge given its ID.  Caller must
	 * have READ permission on the associated Project.
	 * 
	 * @param challengeId
	 * @return
	 * @throws SynapseException
	 */
	Challenge getChallenge(String challengeId) throws SynapseException;

	/**
	 * Returns the Challenge for a given project.  Caller must
	 * have READ permission on the Project.
	 * 
	 * @param projectId
	 * @return
	 * @throws SynapseException
	 */
	Challenge getChallengeForProject(String projectId) throws SynapseException;

	/**
	 * List the Challenges for which a participant is registered.   
	 * To be in the returned list the caller must have READ permission 
	 * on the project 'owning' the Challenge.
	 * 
	 * @param participantPrincipalId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 */
	ChallengePagedResults listChallengesForParticipant(
			String participantPrincipalId, Long limit, Long offset)
			throws SynapseException;

	/**
	 * Update an existing challenge.  Caller must have UPDATE permission
	 * on the associated Project.
	 * 
	 * @param challenge
	 * @return
	 * @throws SynapseException
	 */
	Challenge updateChallenge(Challenge challenge) throws SynapseException;

	/**
	 * Delete a Challenge object.  Caller must have DELETE permission on
	 * the associated Project.
	 * @param id
	 * @throws SynapseException
	 */
	void deleteChallenge(String id) throws SynapseException;

	/**
	 * List the Teams registered for the Challenge.  Caller must have READ permission in 
	 * the Challenge Project.
	 * 
	 * @param challengeId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 */
	ChallengeTeamPagedResults listChallengeTeams(String challengeId, Long limit,
			Long offset) throws SynapseException;

	/**
	 * List the Teams the caller may register for the Challenge, i.e. the Teams which are 
	 * currently not registered for the challenge and on which is current user is an administrator.
	 * 
	 * @param challengeId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 */
	PaginatedIds listRegistratableTeams(String challengeId, Long limit,
			Long offset) throws SynapseException;

	
	/**
	 * Register a Team for a Challenge.
	 * The user making this request must be registered for the Challenge and
	 * be an administrator of the Team.
	 * 
	 * @param challengeTeam
	 * @return
	 * @throws SynapseException
	 */
	public ChallengeTeam createChallengeTeam(ChallengeTeam challengeTeam) throws SynapseException;
	
	/**
	 * Update the ChallengeTeam.
	 * The user making this request must be registered for the Challenge and
	 * be an administrator of the Team.
	 * 
	 * @param challengeTeam
	 * @return
	 * @throws SynapseException
	 */
	ChallengeTeam updateChallengeTeam(ChallengeTeam challengeTeam)
			throws SynapseException;

	/**
	 * Remove a registered Team from a Challenge.
	 * The user making this request must be registered for the Challenge and
	 * be an administrator of the Team.
	 * @param challengeTeamId
	 * @throws SynapseException
	 */
	public void deleteChallengeTeam(String challengeTeamId) throws SynapseException;

	/**
	 * Return challenge participants.  If affiliated=true, return just participants 
	 * affiliated with some registered Team.  If false, return those not affiliated with 
	 * any registered Team.  If missing return all participants. 
	 * 
	 * @param challengeId
	 * @param affiliated
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 */
	PaginatedIds listChallengeParticipants(String challengeId,
			Boolean affiliated, Long limit, Long offset)
			throws SynapseException;

	/**
	 * List the Teams for which the given submitter may submit in the given challenge,
	 * i.e. those teams in which the submitter is a member and which are registered for
	 * the challenge.
	 * 
	 * @param challengeId
	 * @param submitterPrincipalId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 */
	PaginatedIds listSubmissionTeams(String challengeId,
			String submitterPrincipalId, Long limit, Long offset)
			throws SynapseException;
	
	/**
	 * Start an asynchronous job to download multiple files as a bundled zip file.
	 * @see #getBulkFileDownloadResults(String)
	 * @param request Describes the files to be included in the resulting zip file.
	 * @return JobId token used to get the results. See: {@link #getBulkFileDownloadResults(String)}
	 * @throws SynapseException
	 */
	String startBulkFileDownload(BulkFileDownloadRequest request)
			throws SynapseException;

	/**
	 * Get the results of an asynchronous job to download multiple files as a bundled zip file.
	 * @see #startBulkFileDownload(BulkFileDownloadRequest)
	 * @param asyncJobToken The JobId returned from: {@link #startBulkFileDownload(BulkFileDownloadRequest)}
	 * @return
	 * @throws SynapseException
	 * @throws SynapseResultNotReadyException
	 */
	BulkFileDownloadResponse getBulkFileDownloadResults(String asyncJobToken)
			throws SynapseException, SynapseResultNotReadyException;
	
	/**
	 *Request identity verification by the Synapse Access and Compliance Team
	 *
	 * @param verificationSubmission
	 * @param notificationUnsubscribeEndpoint the portal prefix for one-click email unsubscription (optional)
	 * @return the created submission
	 * @throws SynapseException
	 */
	VerificationSubmission createVerificationSubmission(VerificationSubmission verificationSubmission,
			String notificationUnsubscribeEndpoint) throws SynapseException;
	
	/**
	 * Retrieve a list of verification submissions, optionally filtering by the
	 * state of the submission (SUBMITTED, REJECTED, APPROVED, or SUSPENDED) and/or
	 * the ID of the user who requested verification. If limit or offset is not
	 * provided then a default page will be returned.
	 * 
	 * Note:  This service is available only the Synapse Access and Compliance Team
	 * 
	 * @param currentState (optional)
	 * @param submitterId (optional)
	 * @param limit (optional)
	 * @param offset (optional)
	 * @return
	 * @throws SynapseException
	 */
	VerificationPagedResults listVerificationSubmissions(VerificationStateEnum currentState, Long submitterId, Long limit, Long offset) throws SynapseException;
	
	/**
	 * Update the state of a verification request.  The allowed state transitions are:
	 * <ul>
	 * <li>SUBMITTED->REJECTED</li>
	 * <li>SUBMITTED->APPROVED</li>
	 * <li>APPROVED->SUSPENDED</li>
	 * </ul>
	 * 
	 * Note:  This service is available only the Synapse Access and Compliance Team
	 * 
	 * @param verificationId
	 * @param verificationState the new state for the verification request
	 * @param notificationUnsubscribeEndpoint the portal prefix for one-click email unsubscription (optional)
	 * @throws SynapseException   If the caller specifies an illegal state transition a BadRequestException will be thrown.
	 */
	void updateVerificationState(long verificationId, 
			VerificationState verificationState,
			String notificationUnsubscribeEndpoint) throws SynapseException;
	
	/**
	 * Delete a verification submission. The caller must be the creator of the object.
	 * 
	 * @param verificationId
	 * @throws SynapseException 
	 */
	void deleteVerificationSubmission(long verificationId) throws SynapseException;
	
	/**
	 * Get ones own user bundle.  Private fields in the UserProfile and 
	 * VerificationSubmission (if one exists) are not scrubbed.  The mask bits
	 * are defined as:
	 * <li>	UserProfile  = 0x1 </li>
	 * <li> ORCID  = 0x2 </li>
	 * <li> VerificationSubmission  = 0x4 </li>
	 * <li> IsCertified = 0x8 </li>
	 * <li> Is Verified  = 0x10 </li>
	 * <li> Is ACT Member = 0x20 </li>
	 * 
	 * @param mask
	 * @return
	 * @throws SynapseException
	 */
	UserBundle getMyOwnUserBundle(int mask) throws SynapseException;
	
	/**
	 * 
	 * Get the user bundle of another user.  If the subject is not oneself,
	 * private fields in the User Profile are scrubbed.  If the subject is
	 * not oneself and the caller is not an ACT member, then private fields
	 * in the VerificationSubmission are scrubbed.
	 * 
	 * Private fields in the UserProfile and 
	 * VerificationSubmission (if one exists) scrubbed.  The mask bits
	 * are defined as:
	 * <li>	UserProfile  = 0x1 </li>
	 * <li> ORCID  = 0x2 </li>
	 * <li> VerificationSubmission  = 0x4 </li>
	 * <li> IsCertified = 0x8 </li>
	 * <li> Is Verified  = 0x10 </li>
	 * <li> Is ACT Member = 0x20 </li>
	 *
	 * @param principalId
	 * @param mask
	 * @return
	 * @throws SynapseException
	 */
	UserBundle getUserBundle(long principalId, int mask) throws SynapseException;
	
	/**
	 * Get the temporary URL from which the specified file handle may be downloaded.
	 * The associateObjectType and associateObjectId give the context of the request
	 * and are used to perform the authorization check.
	 * 
	 * @param fileHandleAssociation
	 * @return
	 * @throws SynapseException
	 */
	URL getFileURL(FileHandleAssociation fileHandleAssociation) throws SynapseException;
	
	/**
	 * Download the specified file handle.
	 * The associateObjectType and associateObjectId give the context of the request
	 * and are used to perform the authorization check.
	 * 
	 * @param fileHandleAssociation
	 * @param target the location to download the File to
	 * @return
	 * @throws SynapseException
	 */
	void downloadFile(FileHandleAssociation fileHandleAssociation, File target) throws SynapseException;

	/**
	 * Get the forum metadata for a given project
	 * 
	 * @param projectId
	 * @return
	 * @throws SynapseException
	 */
	Forum getForumByProjectId(String projectId) throws SynapseException;

	/**
	 * Get the forum metadata for a given ID
	 * 
	 * @param forumId
	 * @return
	 * @throws SynapseException
	 */
	Forum getForum(String forumId) throws SynapseException;

	/**
	 * Create a new Discussion Reply
	 * 
	 * @param toCreate
	 * @return
	 * @throws SynapseException
	 */
	DiscussionReplyBundle createReply(CreateDiscussionReply toCreate) throws SynapseException;

	/**
	 * Get the discussion reply given its ID
	 * 
	 * @param replyId
	 * @return
	 * @throws SynapseException
	 */
	DiscussionReplyBundle getReply(String replyId) throws SynapseException;

	/**
	 * Get replies for a given thread
	 * 
	 * @param threadId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @param filter
	 * @return
	 * @throws SynapseException
	 */
	PaginatedResults<DiscussionReplyBundle> getRepliesForThread(String threadId, Long limit, Long offset, DiscussionReplyOrder order, Boolean ascending, DiscussionFilter filter) throws SynapseException;

	/**
	 * Get total number of replies for a given threadID
	 * 
	 * @param threadId
	 * @param filter
	 * @return
	 * @throws SynapseException
	 */
	ReplyCount getReplyCountForThread(String threadId, DiscussionFilter filter) throws SynapseException;

	/**
	 * Update the message of an existing reply
	 * 
	 * @param replyId
	 * @param newMessage
	 * @return
	 * @throws SynapseException
	 */
	DiscussionReplyBundle updateReplyMessage(String replyId, UpdateReplyMessage newMessage) throws SynapseException;

	/**
	 * Mark a reply as deleted
	 * 
	 * @param replyId
	 * @throws SynapseException
	 */
	void markReplyAsDeleted(String replyId) throws SynapseException;

	/**
	 * Get the message URL for a reply
	 * 
	 * @param messageKey
	 * @throws SynapseException
	 */
	URL getReplyUrl(String messageKey) throws SynapseException;

	/**
	 * Create a new Discussion Thread
	 * 
	 * @param toCreate
	 * @return
	 * @throws SynapseException
	 */
	DiscussionThreadBundle createThread(CreateDiscussionThread toCreate) throws SynapseException;

	/**
	 * Get an available discussion thread given its ID
	 * 
	 * @param threadId
	 * @return
	 * @throws SynapseException
	 */
	DiscussionThreadBundle getThread(String threadId) throws SynapseException;

	/**
	 * Get threads for a given forum
	 * 
	 * @param forumId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @param filter
	 * @return
	 * @throws SynapseException
	 */
	PaginatedResults<DiscussionThreadBundle> getThreadsForForum(String forumId, Long limit, Long offset, DiscussionThreadOrder order, Boolean ascending, DiscussionFilter filter) throws SynapseException;

	/**
	 * Get moderators for a given forum
	 * 
	 * @param forumId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 */
	PaginatedIds getModeratorsForForum(String forumId, Long limit, Long offset) throws SynapseException;

	/**
	 * Get total number of threads for a given forumID
	 * 
	 * @param forumId
	 * @param filter
	 * @return
	 * @throws SynapseException
	 */
	ThreadCount getThreadCountForForum(String forumId, DiscussionFilter filter) throws SynapseException;

	/**
	 * Update the title of an existing thread
	 * 
	 * @param threadId
	 * @param newTitle
	 * @return
	 * @throws SynapseException
	 */
	DiscussionThreadBundle updateThreadTitle(String threadId, UpdateThreadTitle newTitle) throws SynapseException;

	/**
	 * Update the message of an existing thread
	 * 
	 * @param threadId
	 * @param newMessage
	 * @return
	 * @throws SynapseException
	 */
	DiscussionThreadBundle updateThreadMessage(String threadId, UpdateThreadMessage newMessage) throws SynapseException;

	/**
	 * Mark a thread as deleted
	 * 
	 * @param threadId
	 * @throws SynapseException
	 */
	void markThreadAsDeleted(String threadId) throws SynapseException;

	/**
	 * Restore a deleted thread
	 * 
	 * @param threadId
	 * @throws SynapseException
	 */
	void restoreDeletedThread(String threadId) throws SynapseException;

	/**
	 * Get the message URL for a thread
	 * 
	 * @param messageKey
	 * @throws SynapseException
	 */
	URL getThreadUrl(String messageKey) throws SynapseException;
	
	/**
	 * Low-level API to start a mutli-part upload.  Start or resume a mutli-part upload.
	 * @param request
	 * @param forceRestart Optional parameter.  When forceRestart=true all upload state will be cleared and the upload will start over.
	 * @return
	 * @throws SynapseException 
	 */
	MultipartUploadStatus startMultipartUpload(MultipartUploadRequest request, Boolean forceRestart) throws SynapseException;
	
	/**
	 *  Low-level API to start a mutli-part upload. Get a batch of pre-signed URLs for multi-part upload.
	 * @param request
	 * @return
	 * @throws SynapseException 
	 */
	BatchPresignedUploadUrlResponse getMultipartPresignedUrlBatch(BatchPresignedUploadUrlRequest request) throws SynapseException;
	
	/**
	 *  Low-level API for mutli-part upload.  After uploading a part to a pre-signed URL, it must be added to the multi-part upload.
	 * @param uploadId
	 * @param partNumber
	 * @param partMD5Hex
	 * @return
	 * @throws SynapseException 
	 */
	AddPartResponse addPartToMultipartUpload(String uploadId, int partNumber, String partMD5Hex) throws SynapseException;
	
	/**
	 * Low-level API for mutli-part upload. Complete a multi-part upload.
	 * @param uploadId
	 * @return
	 * @throws SynapseException 
	 */
	MultipartUploadStatus completeMultipartUpload(String uploadId) throws SynapseException;
	
	/**
	 * Upload a file using multi-part upload.
	 * @param input
	 * @param fileSize
	 * @param fileName
	 * @param contentType
	 * @param storageLocationId
	 * @return
	 * @throws SynapseException 
	 */
	CloudProviderFileHandleInterface multipartUpload(InputStream input, long fileSize, String fileName, String contentType, Long storageLocationId, Boolean generatePreview, Boolean forceRestart) throws SynapseException;
	
	/**
	 * Upload the passed file with mutli-part upload.
	 * @param file
	 * @param storageLocationId
	 * @param generatePreview
	 * @param forceRestart
	 * @return
	 * @throws SynapseException
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 */
	CloudProviderFileHandleInterface multipartUpload(File file, Long storageLocationId, Boolean generatePreview, Boolean forceRestart) throws SynapseException, FileNotFoundException, IOException;

	/**
	 * Subscribe to a topic
	 * 
	 * @param toSubscribe
	 * @return
	 * @throws SynapseException 
	 */
	Subscription subscribe(Topic toSubscribe) throws SynapseException;

	/**
	 * Subscribe to all topics of the same SubscriptionObjectType
	 * 
	 * @param toSubscribe
	 * @return
	 * @throws SynapseException 
	 */
	Subscription subscribeAll(SubscriptionObjectType toSubscribe) throws SynapseException;

	/**
	 * Retrieve all subscriptions one has
	 * 
	 * @param objectType
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException 
	 */
	SubscriptionPagedResults getAllSubscriptions(SubscriptionObjectType objectType, Long limit, Long offset, SortByType sortByType, org.sagebionetworks.repo.model.subscription.SortDirection sortDirection) throws SynapseException;

	/**
	 * List all subscriptions one has based on a list of topic
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException 
	 */
	SubscriptionPagedResults listSubscriptions(SubscriptionRequest request) throws SynapseException;

	/**
	 * Unsubscribe to a topic
	 * 
	 * @param subscriptionId
	 * @throws SynapseException 
	 */
	void unsubscribe(Long subscriptionId) throws SynapseException;

	/**
	 * Unsubscribe to all topics
	 * @throws SynapseException 
	 * 
	 */
	void unsubscribeAll() throws SynapseException;

	/**
	 * Retrieve a subscription given its ID
	 * 
	 * @param subscriptionId
	 * @throws SynapseException 
	 */
	Subscription getSubscription(String subscriptionId) throws SynapseException;

	/**
	 * Retrieve a page of subscribers for a given topic
	 * 
	 * @param topic
	 * @param nextPageToken
	 * @return
	 * @throws SynapseException
	 */
	SubscriberPagedResults getSubscribers(Topic topic, String nextPageToken) throws SynapseException;

	/**
	 * Retrieve number of subscribers for a given topic
	 * 
	 * @param topic
	 * @return
	 * @throws SynapseException
	 */
	SubscriberCount getSubscriberCount(Topic topic) throws SynapseException;


	/**
	 * Get the entity ID assigned to a given alias.
	 * 
	 * @param alias
	 * @return
	 * @throws SynapseException
	 */
	EntityId getEntityIdByAlias(String alias) throws SynapseException;
	
	/**
	 * Get a page of children for an Entity.
	 * @param request
	 * @return
	 * @throws SynapseException 
	 */
	EntityChildrenResponse getEntityChildren(EntityChildrenRequest request) throws SynapseException;

	/**
	 * Retrieve an entityId given its name and parentId.
	 *
	 * @param parentId
	 * @param entityName
	 * @return
	 * @throws SynapseException
	 */
	String lookupChild(String parentId, String entityName) throws SynapseException;

	/**
	 * Pin a thread
	 * 
	 * @param threadId
	 * @throws SynapseException
	 */
	void pinThread(String threadId) throws SynapseException;

	/**
	 * Remove pinning from a thread
	 * 
	 * @param threadId
	 * @throws SynapseException
	 */
	void unpinThread(String threadId) throws SynapseException;

	/**
	 * Return the PrincipalID for a given alias and alias type
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	PrincipalAliasResponse getPrincipalAlias(PrincipalAliasRequest request) throws SynapseException;
	
	/**
	 * Add a new DockerCommit to an existing Docker repository entity.  This can only be called
	 * for external / unmanaged Docker repositories.
	 * 
	 * @param entityId the ID of the Docker repository
	 * @param dockerCommit the new commit, including tag and digest
	 * @throws SynapseException
	 */
	void addDockerCommit(String entityId, DockerCommit dockerCommit) throws SynapseException;

	/**
	 * Return a paginated list of tagged commits (tag/digest pairs) for the given Docker repository.
	 *
	 * @param entityId the ID of the Docker repository entity
	 * @param limit pagination parameter, optional (default is 20)
	 * @param offset pagination parameter, optional (default is 0)
	 * @param sortBy TAG or CREATED_ON, optional (default is CREATED_ON)
	 * @param ascending, optional (default is false)
	 * @return a paginated list of tagged commits (tag/digest pairs) for the given Docker repository.
	 * @throws SynapseException
	 */
	PaginatedResults<DockerCommit> listDockerTags(String entityId, Long limit, Long offset, DockerCommitSortBy sortBy, Boolean ascending) throws SynapseException;

	/**
	 * Get threads that reference the given entity
	 * 
	 * @param entityId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @param filter
	 * @return a paginated list of threads that the user can view
	 * @throws SynapseException
	 */
	PaginatedResults<DiscussionThreadBundle> getThreadsForEntity(String entityId, Long limit, Long offset, DiscussionThreadOrder order, Boolean ascending, DiscussionFilter filter) throws SynapseException;

	/**
	 * Provides the number of threads that reference each entity in the given id list
	 * 
	 * @param entityIds
	 * @return the number of threads the user can view
	 * @throws SynapseException 
	 */
	EntityThreadCounts getEntityThreadCount(List<String> entityIds) throws SynapseException;
	
	/**
	 * Start a table transaction job.  Either all of the passed requests will be applied
	 * or none of the requests will be applied. 
	 * 
	 * @param changes
	 * @param tableId
	 * @return
	 * @throws SynapseException
	 */
	String startTableTransactionJob(List<TableUpdateRequest> changes,
			String tableId) throws SynapseException;

	/**
	 * Get the results of a started table transaction job.
	 * There will be one response for each request.
	 * @param token
	 * @param tableId
	 * @return
	 * @throws SynapseException
	 * @throws SynapseResultNotReadyException
	 */
	List<TableUpdateResponse> getTableTransactionJobResults(String token,
			String tableId) throws SynapseException,
			SynapseResultNotReadyException;

	/**
	 * Get a batch of pre-signed URLs and/or FileHandles for the given list of FileHandleAssociations 
	 * @param request
	 * @return
	 * @throws SynapseException 
	 */
	public BatchFileResult getFileHandleAndUrlBatch(BatchFileRequest request) throws SynapseException;

	/**
	 * Copy a batch of FileHandles.
	 * This API will check for DOWNLOAD permission on each FileHandle. If the user
	 * has DOWNLOAD permission on a FileHandle, we will make a copy of the FileHandle,
	 * replace the fileName and contentType of the file if they are specified in
	 * the request, and return the new FileHandle.
	 * 
	 * @param request
	 * @return
	 * @throws SynapseBadRequestException for request with duplicated FileHandleId.
	 * @throws SynapseException
	 */
	public BatchFileHandleCopyResult copyFileHandles(BatchFileHandleCopyRequest request) throws SynapseException;

	/**
	 * Make a request to cancel a submission.
	 * 
	 * @param submissionId
	 * @throws SynapseException 
	 */
	public void requestToCancelSubmission(String submissionId) throws SynapseException;

	/**
	 * Get the possible ColumnModel definitions based on annotation within a
	 * given scope.
	 * 
	 * @deprecated This is replaced by an asynchronous job, see {@link #startGetPossibleColumnModelsForViewScope(ViewColumnModelRequest)}
	 * @param scope
	 *            List of parent IDs that define the scope.
	 * @param nextPageToken
	 *            Optional: When the results include a next page token, the
	 *            token can be provided to get subsequent pages.
	 * 
	 * @return A ColumnModel for each distinct annotation for the given scope. A returned nextPageToken can be used to get subsequent pages
	 * of ColumnModels for the given scope.  The nextPageToken will be null when there are no more pages of results.
	 * 
	 */
	@Deprecated
	ColumnModelPage getPossibleColumnModelsForViewScope(ViewScope scope, String nextPageToken) throws SynapseException;

	/**
	 * Starts an asynchronous job that computes the possible {@link ColumnModel} definitions based on the annotations
	 * within the scope in the request. The result of the job get be fetched using the {@link #getPossibleColumnModelsForViewScopeResult(String)}
	 * 
	 * @param request The request should include the scope and an optional nextPageToken to fetch subsequent pages
	 * @return The async job token that can be used to fetch the result, {@link #getPossibleColumnModelsForViewScopeResult(String)}
	 * @throws SynapseException
	 */
	String startGetPossibleColumnModelsForViewScope(ViewColumnModelRequest request) throws SynapseException;
	
	ViewColumnModelResponse getPossibleColumnModelsForViewScopeResult(String asyncJobToken) throws SynapseException;
	
	/**
	 * Create new or update an existing ResearchProject.
	 * 
	 * @param toCreateOrUpdate
	 * @return
	 * @throws SynapseException
	 */
	ResearchProject createOrUpdateResearchProject(ResearchProject toCreateOrUpdate) throws SynapseException;

	/**
	 * Retrieve the current ResearchProject to update.
	 * If one does not exist, an empty ResearchProject will be returned.
	 * 
	 * @param accessRequirementId
	 * @return
	 * @throws SynapseException
	 */
	ResearchProject getResearchProjectForUpdate(String accessRequirementId) throws SynapseException;

	/**
	 * Create new or update an existing RequestInterface.
	 * 
	 * @param toCreateOrUpdate
	 * @return
	 * @throws SynapseException
	 */
	RequestInterface createOrUpdateRequest(RequestInterface toCreateOrUpdate) throws SynapseException;

	/**
	 * Retrieve the current RequestInterface to update.
	 * If one does not exist, an empty Request will be returned.
	 * If a submission associated with the request is approved, and the requirement
	 * requires renewal, a refilled Renewal is returned.
	 * 
	 * @param accessRequirementId
	 * @return
	 * @throws SynapseException
	 */
	RequestInterface getRequestForUpdate(String accessRequirementId) throws SynapseException;

	/**
	 * Submit a submission
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	org.sagebionetworks.repo.model.dataaccess.SubmissionStatus submitRequest(CreateSubmissionRequest request) throws SynapseException;

	/**
	 * Cancel a submission.
	 * 
	 * @param submissionId
	 * @return
	 * @throws SynapseException
	 */
	org.sagebionetworks.repo.model.dataaccess.SubmissionStatus cancelSubmission(String submissionId) throws SynapseException;
	
	/**
	 * Delete a Data Access Submission.
	 * 
	 * @param submissionId
	 * @throws SynapseException
	 */
	void deleteDataAccessSubmission(String submissionId) throws SynapseException;

	/**
	 * Request to update the state of a submission.
	 * 
	 * @param submissionId
	 * @param newState
	 * @param reason
	 * @return
	 * @throws SynapseException
	 */
	org.sagebionetworks.repo.model.dataaccess.Submission updateSubmissionState(String submissionId, SubmissionState newState, String reason) throws SynapseException;

	/**
	 * Retrieve a page of submissions.
	 * Only ACT member can perform this action.
	 * 
	 * @param requirementId
	 * @param nextPageToken
	 * @param filter
	 * @param order
	 * @param isAscending
	 * @return
	 * @throws SynapseException
	 */
	SubmissionPage listSubmissions(String requirementId, String nextPageToken, SubmissionState filter, SubmissionOrder order, Boolean isAscending) throws SynapseException;

	/**
	 * List the research projects for approved data access submissions, 
	 * ordered by modifiedOn date, ascending

	 * @param requirementId
	 * @param nextPageToken
	 * @return
	 * @throws SynapseException
	 */
	SubmissionInfoPage listApprovedSubmissionInfo(String requirementId, String nextPageToken) throws SynapseException;

	/**
	 * Retrieve the status for a given access requirement.
	 * 
	 * @param requirementId
	 * @return
	 * @throws SynapseException
	 */
	AccessRequirementStatus getAccessRequirementStatus(String requirementId) throws SynapseException;

	/**
	 * Retrieve the restriction information on a restrictable object.
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	RestrictionInformationResponse getRestrictionInformation(RestrictionInformationRequest request) throws SynapseException;

	/**
	 * Retrieve the information about submitted Submissions.
	 * @param nextPageToken
	 * @return
	 * @throws SynapseException
	 */
	OpenSubmissionPage getOpenSubmissions(String nextPageToken) throws SynapseException;

	/**
	 * Retrieve a page of AccessorGroup.
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	AccessorGroupResponse listAccessorGroup(AccessorGroupRequest request) throws SynapseException;

	/**
	 * Revoke a group of accessors.
	 * 
	 * @param accessRequirementId
	 * @param submitterId
	 * @throws SynapseException
	 */
	void revokeGroup(String accessRequirementId, String submitterId) throws SynapseException;

	/**
	 * Convert an ACTAccessRequirement to a ManagedACTAccessRequirement.
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	AccessRequirement convertAccessRequirement(AccessRequirementConversionRequest request) throws SynapseException;

	/**
	 * Retrieve a batch of AccessApprovalInfo
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	BatchAccessApprovalInfoResponse getBatchAccessApprovalInfo(BatchAccessApprovalInfoRequest request) throws SynapseException;
	
	/**
	 * Fetch the notifications for an AR and a list of recipients
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	AccessApprovalNotificationResponse getAccessApprovalNotifications(AccessApprovalNotificationRequest request) throws SynapseException;

	/**
	 * Retrieve a page of subjects for a given access requirement ID.
	 * 
	 * @param requirementId
	 * @param nextPageToken
	 * @return
	 * @throws SynapseException
	 */
	RestrictableObjectDescriptorResponse getSubjects(String requirementId, String nextPageToken) throws SynapseException;
	
	/**
	 * Start an asynchronous job to add files to a user's download list.
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	String startAddFilesToDownloadList(AddFileToDownloadListRequest request)
			throws SynapseException;


	/**
	 * Get the results of the asynchronous job to add files to a user's download list.
	 * 
	 * @param asyncJobToken
	 * @return
	 * @throws SynapseException
	 * @throws SynapseResultNotReadyException
	 */
	AddFileToDownloadListResponse getAddFilesToDownloadListResponse(String asyncJobToken)
			throws SynapseException, SynapseResultNotReadyException;
	
	/**
	 * Add the given list of files to the user's download list.
	 * 
	 * @param toAdd
	 * @return
	 * @throws SynapseException 
	 */
	DownloadList addFilesToDownloadList(List<FileHandleAssociation> toAdd) throws SynapseException;
	
	/**
	 * Remove the given list of files from the user's download list.
	 * 
	 * @param toRemove
	 * @return
	 * @throws SynapseException 
	 */
	DownloadList removeFilesFromDownloadList(List<FileHandleAssociation> toRemove) throws SynapseException;
	
	/**
	 * Clear the user's download list.
	 * 
	 * @return
	 * @throws SynapseException 
	 */
	void clearDownloadList() throws SynapseException;
	
	/**
	 * Get a user's download list.
	 * 
	 * @return
	 * @throws SynapseException 
	 */
	DownloadList getDownloadList() throws SynapseException;
	
	/**
	 * Create a download Order from the user's current download list. Only files that
	 * the user has permission to download will be added to the download order. Any
	 * file that cannot be added to the order will remain in the user's download
	 * list.
	 * <p>
	 * The resulting download order can then be downloaded using
	 * {@link #startBulkFileDownload(BulkFileDownloadRequest)}.
	 * </p>
	 * 
	 * <p>
	 * Note: A single download order is limited to 1 GB of uncompressed file data.
	 * This method will attempt to create the largest possible order that is within
	 * the limit. Any file that cannot be added to the order will remain in the
	 * user's download list.
	 * </p>
	 * @return
	 * @throws SynapseException 
	 */
	DownloadOrder createDownloadOrderFromUsersDownloadList(String zipFileName) throws SynapseException;
	
	/**
	 * Get a download order given the order's ID
	 * @param orderId
	 * @return
	 * @throws SynapseException 
	 */
	DownloadOrder getDownloadOrder(String orderId) throws SynapseException;
	
	/**
	 * Get the download order history for a user in reverse chronological order.
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException 
	 */
	DownloadOrderSummaryResponse getDownloadOrderHistory(DownloadOrderSummaryRequest request) throws SynapseException;
	
	/**
	 * Change the {@link DataType} of the given Entity.
	 * Note: The caller must be a member of the 'Synapse Access and Compliance Team' to change
	 * an Entity's data type to OPEN_DATA.  The caller must be grated the UPDATE permission
	 * to change an Entity's data type to any value other than  OPEN_DATA.
	 * @param entityId
	 * @param newDataType
	 * @return
	 * @throws SynapseException 
	 */
	DataTypeResponse changeEntitysDataType(String entityId, DataType newDataType) throws SynapseException;

	String generateStorageReportAsyncStart(StorageReportType reportType) throws SynapseException;

	DownloadStorageReportResponse generateStorageReportAsyncGet(String asyncJobToken) throws SynapseException;

	/**
	 * Request to transform the provided SQL based on the request parameters. For
	 * example, a
	 * {@linkplain org.sagebionetworks.repo.model.tabe.TransformSqlWithFacetsRequest}
	 * can be used to alter the where clause of the provided SQL based on the
	 * provided selected facets.
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException 
	 */
	public String transformSqlRequest(SqlTransformRequest request) throws SynapseException;
	
	/**
	 * Request to create a new snapshot of a table or view. The provided comment,
	 * label, and activity ID will be applied to the current version thereby
	 * creating a snapshot and locking the current version. After the snapshot is
	 * created a new version will be started with an 'in-progress' label.
	 * @param tableId
	 * @param request
	 * @return
	 * @throws SynapseException 
	 */
	public SnapshotResponse createTableSnapshot(String tableId, SnapshotRequest request) throws SynapseException;
	
	/**
	 * Request to retrieve statistics about specific objects. The user should have
	 * {@link ACCESS_TYPE#READ} access on the {@link ObjectStatisticsRequest#getObjectId()
	 * objectId} referenced by the request.
	 * 
	 * @param request The request body
	 * @return The statistics according to the given request
	 * @throws SynapseException
	 */
	public ObjectStatisticsResponse getStatistics(ObjectStatisticsRequest request) throws SynapseException;

	/**
	 * Create a FormGroup with provided name. This method is idempotent. If a group
	 * with the provided name already exists and the caller has ACCESS_TYPE.READ
	 * permission the existing FormGroup will be returned.
	 * </p>
	 * The created FormGroup will have an Access Control
	 * List (ACL) with the creator listed as an administrator.
	 * 
	 * @param userId
	 * @param name   A globally unique name for the group. Required. Between 3 and
	 *               256 characters.
	 * @param name
	 * @return
	 * @throws SynapseException 
	 */
	FormGroup createFormGroup(String name) throws SynapseException;
	

	/**
	 * Get a FormGroup for the given group ID.
	 * @param id
	 * @return
	 * @throws SynapseException
	 */
	FormGroup getFormGroup(String id) throws SynapseException;
	
	/**
	 * Get the Access Control List (ACL) for a FormGroup.
	 * </p>
	 * Note: The caller must have the ACCESS_TYPE.READ permission on the identified group.
	 * @param formGroupId The identifier of the group.
	 * @return
	 * @throws SynapseException 
	 */
	AccessControlList getFormGroupAcl(String formGroupId) throws SynapseException;
	
	/**
	 * Update the Access Control List (ACL) for a FormGroup.
	 * <p>
	 * The following define the permissions in this context:
	 * <ul>
	 * <li>ACCESS_TYPE.READ - Grants read access to the group. READ does <b>not</b>
	 * grant access to FormData of the group.</li>
	 * <li>ACCESS_TYPE.CHANGE_PERMISSIONS - Grants access to update the group's
	 * ACL.</li>
	 * <li>ACCESS_TYPE.SUBMIT - Grants access to both create and submit FormData to
	 * the group.</li>
	 * <li>ACCESS_TYPE.READ_PRIVATE_SUBMISSION - Grants administrator's access to
	 * the submitted FormData, including both FormData reads and status updates.
	 * This permission should be reserved for the service account that evaluates
	 * submissions.</li>
	 * </ul>
	 * 
	 * Users automatically have read/update access to FormData that they create.
	 * </p>
	 * 
	 * 
	 * Note: The caller must have the ACCESS_TYPE.CHANGE_PERMISSIONS permission on
	 * the identified group to update the group's ACL.
	 * 
	 * @param userId
	 * @param id     The identifier of the FormGroup.
	 * @param acl    The updated ACL.
	 * @return
	 * @throws SynapseException 
	 */
	AccessControlList updateFormGroupAcl(AccessControlList acl) throws SynapseException;

	/**
	 * Create a new FormData object. The caller will own the resulting object and
	 * will have access to read, update, and delete the FormData object.
	 * <p>
	 * Note: The caller must have the ACCESS_TYPE.SUBMIT permission on the FormGrup
	 * to create/update/submit FormData.
	 * 
	 * @param groupId The identifier of the group that manages this data. Required.
	 * @param name    User provided name for this submission. Required. Between 3
	 *                and 256 characters.
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	FormData createFormData(String groupId, FormChangeRequest request) throws SynapseException;
	
	/**
	 * Update an existing FormData object. The caller must be the creator of the
	 * FormData object. Once a FormData object has been submitted, it cannot be
	 * updated until it has been processed. If the submission is accepted it becomes
	 * immutable. Rejected submission are editable. Updating a rejected submission
	 * will change its status back to waiting_for_submission.
	 * <p>
	 * Note: The caller must have the ACCESS_TYPE.SUBMIT permission on the FormGrup
	 * to create/update/submit FormData.
	 * 
	 * @param formId  The identifier of the FormData to update.
	 * @param name    Rename this submission. Optional. Between 3 and 256
	 *                characters.
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	FormData updateFormData(String formId, FormChangeRequest request) throws SynapseException;
	
	/**
	 * Delete an existing FormData object. The caller must be the creator of the
	 * FormData object.
	 * <p>
	 * Note: Cannot delete a FormData object once it has been submitted and caller
	 * must have the ACCESS_TYPE.SUBMIT permission on the identified group to update
	 * the group's ACL.
	 * 
	 * @param formId Id of the FormData object to delete
	 * @return
	 * @throws SynapseException 
	 */
	void deleteFormData(String formId) throws SynapseException;
	
	/**
	 * Submit the identified FormData from review.
	 * <p>
	 * Note: The caller must have the ACCESS_TYPE.SUBMIT permission on the
	 * identified group to update the group's ACL.
	 * 
	 * @param userId
	 * @param id
	 * @return
	 * @throws SynapseException 
	 */
	FormData submitFormData(String formId) throws SynapseException;
	
	/**
	 * List FormData objects and their associated status that match the filters of
	 * the provided request that are owned by the caller. Note: Only objects owned
	 * by the caller will be returned.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 * @throws SynapseException 
	 */
	ListResponse listFormStatusForCreator(ListRequest request) throws SynapseException;
	
	/**
	 * List FormData objects and their associated status that match the filters of
	 * the provided request for the entire group. This is used by service accounts
	 * to review submissions. Filtering by WAITING_FOR_SUBMISSION is not allowed for
	 * this call.
	 * <p>
	 * Note: The caller must have the ACCESS_TYPE.READ_PRIVATE_SUBMISSION permission
	 * on the identified group to update the group's ACL.
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException 
	 */
	ListResponse listFormStatusForReviewer(ListRequest request) throws SynapseException;
	
	/**
	 * Called by the form reviewing service to accept a submitted data.
	 * <p>
	 * ACCESS_TYPE.READ_PRIVATE_SUBMISSION permission on the identified group to
	 * update the group's ACL.
	 * 
	 * @param formDataId Identifier of the FormData to accept.
	 * @return
	 * @throws SynapseException 
	 */
	FormData reviewerAcceptFormData(String formDataId) throws SynapseException;
	
	/**
	 * Called by the form reviewing service to reject a submitted data.
	 * <p>
	 * Note: The caller must have the ACCESS_TYPE.READ_PRIVATE_SUBMISSION permission
	 * on the identified group to update the group's ACL.
	 * 
	 * @param formDataId Identifier of the FormData to accept.
	 * @param reason     The reason for the rejection. Limit 500 characters or less.
	 * @return
	 * @throws SynapseException 
	 */
	FormData reviewerRejectFormData(String formDataId, FormRejection rejection) throws SynapseException;
	
	/**
	 * Create a new Organization.
	 * @param request
	 * @return
	 * @throws SynapseException 
	 */
	Organization createOrganization(CreateOrganizationRequest request) throws SynapseException;
	
	/**
	 * Lookup an Organization by name
	 * @param organizationName
	 * @return
	 * @throws SynapseException 
	 */
	Organization getOrganizationByName(String organizationName) throws SynapseException;
	
	/**
	 * Delete the identified Organization.
	 * 
	 * @param id
	 * @throws SynapseException 
	 */
	void deleteOrganization(String id) throws SynapseException;
	
	/**
	 * Get the ACL for the identified Organization.
	 * @param id
	 * @return
	 * @throws SynapseException 
	 */
	AccessControlList getOrganizationAcl(String id) throws SynapseException;
	
	/**
	 * Update the given Organization's ACL.
	 * @parm id The ID of the organization to update.
	 * @param toUpdate
	 * @return
	 * @throws SynapseException 
	 */
	AccessControlList updateOrganizationAcl(String id, AccessControlList toUpdate) throws SynapseException;
	
	/**
	 * Start an asynchronous job to create a new JSON schema.
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	public String startCreateSchemaJob(CreateSchemaRequest request) throws SynapseException;

	/**
	 * Get the results of a create schema asynchronous request.
	 * @param asyncJobToken
	 * @return
	 * @throws SynapseException
	 * @throws SynapseResultNotReadyException
	 */
	public CreateSchemaResponse getCreateSchemaJobResult(String asyncJobToken) throws SynapseException, SynapseResultNotReadyException;
	
	/**
	 * Get the JSON schema for the given organization, schema, and version.
	 * @param organizationName
	 * @param schemaName
	 * @param semanticVersion
	 * @return
	 * @throws SynapseException 
	 */
	JsonSchema getJsonSchema(String organizationName, String schemaName, String semanticVersion) throws SynapseException;
	
	/**
	 * Delete the given schema and all of its versions.
	 * @param organizationName
	 * @param schemaName
	 * @throws SynapseException 
	 */
	public void deleteSchema(String organizationName, String schemaName) throws SynapseException;
	
	/**
	 * Delete a specific version of a schema.
	 * @param organizationName
	 * @param schemaName
	 * @param semanticVersion
	 * @throws SynapseException
	 */
	public void deleteSchemaVersion(String organizationName, String schemaName, String semanticVersion) throws SynapseException;

	/**
	 * Paginated list of Organizations.
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	ListOrganizationsResponse listOrganizations(ListOrganizationsRequest request) throws SynapseException;

	/**
	 * Paginated list of JsonSchemaInfo for a given Organization.;
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	ListJsonSchemaInfoResponse listSchemaInfo(ListJsonSchemaInfoRequest request) throws SynapseException;

	/**
	 * Paginated list of JsonSchemaVersionInfo for a given schema.
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	ListJsonSchemaVersionInfoResponse listSchemaVersions(ListJsonSchemaVersionInfoRequest request)
			throws SynapseException;

	/**
	 * Bind a JSON schema to an Entity. The schema will be used to validate metadata
	 * on the Entity and its children.
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	JsonSchemaObjectBinding bindJsonSchemaToEntity(BindSchemaToEntityRequest request) throws SynapseException;

	/**
	 * Get the JSON schema bound to an Entity.
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	JsonSchemaObjectBinding getJsonSchemaBindingForEntity(String entityId) throws SynapseException;

	/**
	 * Clear the JSON schema binding for an Entity.
	 * @param entityId
	 * @throws SynapseException
	 */
	void clearSchemaBindingForEntity(String entityId) throws SynapseException;

	/**
	 * Get the JSONObject representation of an Entity that can be used to validate
	 * the entity against a JSON schema.
	 * 
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	JSONObject getEntityJson(String entityId) throws SynapseException;

	/**
	 * Update an Entity's annotations using the JSONObject representation of the Entity.
	 * @param entityId
	 * @param json
	 * @return
	 * @throws SynapseException
	 */
	JSONObject updateEntityJson(String entityId, JSONObject json) throws SynapseException;

	/**
	 * Get the validation results of an Entity against its bound JSON schema.
	 * 
	 * @param entityId
	 * @return
	 * @throws SynapseException
	 */
	ValidationResults getEntityValidationResults(String entityId) throws SynapseException;

	/**
	 * Get the The summary statistics of the JSON schema validation results for a
	 * single container Entity such as a Project or Folder. Only direct children of
	 * the container are included in the results. The statistics include the total
	 * number of children in the container, and the counts for both the invalid and
	 * valid children.
	 * @param containerId
	 * @return
	 * @throws SynapseException
	 */
	ValidationSummaryStatistics getEntitySchemaValidationStatistics(String containerId) throws SynapseException;

	/**
	 * Get a single page of invalid JSON schema validation results for a container
	 * Entity (Project or Folder).
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	ListValidationResultsResponse getInvalidValidationResults(ListValidationResultsRequest request)
			throws SynapseException;
	
	/**
	 * Updates the file handle of the version of the entity with the given id
	 * 
	 * @param request Body of the request containing the old and new file handle
	 * @throws SynapseException
	 */
	void updateEntityFileHandle(String entityId, Long versionNumber, FileHandleUpdateRequest request) throws SynapseException;

	/**
	 * Add a batch of files to the user's download list.
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	AddBatchOfFilesToDownloadListResponse addFilesToDownloadList(AddBatchOfFilesToDownloadListRequest request) throws SynapseException;

	/**
	 * Remove a batch of files from the user's download list.
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	RemoveBatchOfFilesFromDownloadListResponse removeFilesFromDownloadList(
			RemoveBatchOfFilesFromDownloadListRequest request) throws SynapseException;

	/**
	 * Clear all files from the user's download list.
	 * @throws SynapseException
	 */
	void clearUsersDownloadList() throws SynapseException;

	/**
	 * Start an asynchronous job to query the user's download list.
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	String startDownloadListQuery(DownloadListQueryRequest request) throws SynapseException;

	/**
	 * Get the results of an asynchronous job to query the user's download list.
	 * @param asyncJobToken
	 * @return
	 * @throws SynapseException
	 * @throws SynapseResultNotReadyException
	 */
	DownloadListQueryResponse getDownloadListQueryResult(String asyncJobToken)
			throws SynapseException, SynapseResultNotReadyException;

}
