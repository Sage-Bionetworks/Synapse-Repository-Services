package org.sagebionetworks.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseResultNotReadyException;
import org.sagebionetworks.client.exceptions.SynapseTableUnavailableException;
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
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
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
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
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
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileCopyResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
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
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableFileHandleResults;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
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
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

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
			throws SynapseException, JSONObjectAdapterException;
	
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
	 * @param addEmailInfo the token sent to the user via email
	 * @param setAsNotificationEmail if true then set the new email address to be the user's notification address
	 * @throws NotFoundException
	 */
	void addEmail(AddEmailInfo addEmailInfo, Boolean setAsNotificationEmail) throws SynapseException;
	
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
	 * address at which the user will receive the message.
	 * @throws SynapseException
	 */
	public String getNotificationEmail() throws SynapseException;

	/**
	 * Get the endpoint of the repository service
	 */
	public String getRepoEndpoint();

	/**
	 * The repository endpoint includes the host and version. For example:
	 * "https://repo-prod.prod.sagebase.org/repo/v1"
	 */
	public void setRepositoryEndpoint(String repoEndpoint);

	/**
	 * The authorization endpoint includes the host and version. For example:
	 * "https://repo-prod.prod.sagebase.org/auth/v1"
	 */
	public void setAuthEndpoint(String authEndpoint);

	/**
	 * Get the endpoint of the authorization service
	 */
	public String getAuthEndpoint();

	/**
	 * The file endpoint includes the host and version. For example:
	 * "https://repo-prod.prod.sagebase.org/file/v1"
	 */
	public void setFileEndpoint(String fileEndpoint);

	/**
	 * Get the endpoint of the file service
	 */
	public String getFileEndpoint();

	public Entity getEntityById(String entityId) throws SynapseException;

	public <T extends Entity> T putEntity(T entity) throws SynapseException;

	public URL getWikiAttachmentPreviewTemporaryUrl(WikiPageKey properKey,
			String fileName) throws ClientProtocolException, IOException, SynapseException;
	
	public void downloadWikiAttachmentPreview(WikiPageKey properKey,
			String fileName, File target) throws SynapseException;

	public URL getWikiAttachmentTemporaryUrl(WikiPageKey properKey,
			String fileName) throws ClientProtocolException, IOException, SynapseException;
	
	public void downloadWikiAttachment(WikiPageKey properKey,
			String fileName, File target) throws SynapseException;

	/**
	 * Log into Synapse
	 */
	public Session login(String username, String password)
			throws SynapseException;

	/**
	 * Log into Synapse
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	LoginResponse login(LoginRequest request) throws SynapseException;

	/**
	 * Log out of Synapse
	 */
	public void logout() throws SynapseException;

	/**
	 * Returns a Session and UserProfile object
	 * 
	 * Note: if the user has not accepted the terms of use, the profile will not (cannot) be retrieved
	 */
	public UserSessionData getUserSessionData() throws SynapseException;

	/**
	 * Refreshes the cached session token so that it can be used for another 24 hours
	 */
	public boolean revalidateSession() throws SynapseException;
	
	/**
	 * Create a new Entity.
	 * 
	 * @return the newly created entity
	 */
	public <T extends Entity> T createEntity(T entity) throws SynapseException;

	public JSONObject createJSONObject(String uri, JSONObject entity)
			throws SynapseException;

	public SearchResults search(SearchQuery searchQuery)
			throws SynapseException, UnsupportedEncodingException,
			JSONObjectAdapterException;

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
	public WikiPage getWikiPage(WikiPageKey properKey)
			throws JSONObjectAdapterException, SynapseException;
	
	/**
	 * Get a specific version of a wikig page.
	 * @param properKey
	 * @param versionNumber
	 * @return
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public WikiPage getWikiPageForVersion(WikiPageKey properKey, Long versionNumber) throws JSONObjectAdapterException, SynapseException;
	
	/**
	 * Get the WikiPageKey for the root wiki given an ownerId and ownerType.
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public WikiPageKey getRootWikiPageKey(String ownerId, ObjectType ownerType) throws JSONObjectAdapterException, SynapseException;
	
	public AccessRequirement getAccessRequirement(Long requirementId) throws SynapseException;

	public PaginatedResults<AccessRequirement> getAccessRequirements(
			RestrictableObjectDescriptor subjectId) throws SynapseException;

	public WikiPage updateWikiPage(String ownerId, ObjectType ownerType,
			WikiPage toUpdate) throws JSONObjectAdapterException,
			SynapseException;

	public void setRequestProfile(boolean request);

	public JSONObject getProfileData();

	public String getUserName();

	public void setUserName(String userName);

	public String getApiKey();

	public void setApiKey(String apiKey);

	public <T extends Entity> T createEntity(T entity, String activityId)
			throws SynapseException;

	public <T extends JSONEntity> T createJSONEntity(String uri, T entity)
			throws SynapseException;

	public EntityBundle createEntityBundle(EntityBundleCreate ebc)
			throws SynapseException;

	public EntityBundle createEntityBundle(EntityBundleCreate ebc, String activityId)
			throws SynapseException;

	public EntityBundle updateEntityBundle(String entityId, EntityBundleCreate ebc)
			throws SynapseException;

	public EntityBundle updateEntityBundle(String entityId, EntityBundleCreate ebc,
			String activityId) throws SynapseException;

	public Entity getEntityByIdForVersion(String entityId, Long versionNumber)
			throws SynapseException;

	public EntityBundle getEntityBundle(String entityId, int partsMask)
			throws SynapseException;

	public EntityBundle getEntityBundle(String entityId, Long versionNumber,
			int partsMask) throws SynapseException;

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
	 * @param limit page size
	 * @param offset page start
	 * @return the users whose first, last or user name matches the given prefix
	 * @throws SynapseException
	 * @throws UnsupportedEncodingException
	 */
	public UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(String prefix, long limit, long offset)
			throws SynapseException, UnsupportedEncodingException;

	public AccessControlList updateACL(AccessControlList acl) throws SynapseException;

	public AccessControlList updateACL(AccessControlList acl, boolean recursive)
			throws SynapseException;

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

	public Annotations getAnnotations(String entityId) throws SynapseException;

	public Annotations updateAnnotations(String entityId, Annotations updated)
			throws SynapseException;

	public <T extends AccessRequirement> T createAccessRequirement(T ar)
			throws SynapseException;

	public <T extends AccessRequirement> T updateAccessRequirement(T ar)
			throws SynapseException;

	public ACTAccessRequirement createLockAccessRequirement(String entityId)
			throws SynapseException;

	public PaginatedResults<AccessRequirement> getUnmetAccessRequirements(
			RestrictableObjectDescriptor subjectId, ACCESS_TYPE accessType) throws SynapseException;

	public <T extends AccessApproval> T createAccessApproval(T aa)
			throws SynapseException;
	
	public AccessApproval getAccessApproval(Long approvalId) throws SynapseException;

	public JSONObject getEntity(String uri) throws SynapseException;

	public <T extends JSONEntity> T getEntity(String entityId,
			Class<? extends T> clazz) throws SynapseException;

	public void deleteAccessRequirement(Long arId) throws SynapseException;

	public <T extends Entity> T putEntity(T entity, String activityId)
			throws SynapseException;

	public <T extends Entity> void deleteEntity(T entity) throws SynapseException;

	public <T extends Entity> void deleteEntity(T entity, Boolean skipTrashCan) throws SynapseException;

	public void deleteEntityById(String entityId) throws SynapseException;

	public void deleteEntityById(String entityId, Boolean skipTrashCan) throws SynapseException;

	public <T extends Entity> void deleteAndPurgeEntity(T entity) throws SynapseException;

	public void deleteAndPurgeEntityById(String entityId) throws SynapseException;

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

	public JSONObject query(String query) throws SynapseException;

	/**
	 * Upload each file to Synapse creating a file handle for each.
	 */
	@Deprecated
	public FileHandleResults createFileHandles(List<File> files)
			throws SynapseException;

	/**
	 * The high-level API for uploading a file to Synapse.
	 */
	@Deprecated
	public S3FileHandle createFileHandle(File temp, String contentType)
			throws SynapseException, IOException;

	/**
	 * See {@link #createFileHandle(File, String)}
	 * @param shouldPreviewBeCreated Default true
	 */
	@Deprecated
	public S3FileHandle createFileHandle(File temp, String contentType, Boolean shouldPreviewBeCreated)
			throws SynapseException, IOException;

	/**
	 * Upload each file to Synapse creating a file handle for each.
	 */
	public FileHandleResults createFileHandles(List<File> files, String parentEntityId) throws SynapseException;

	/**
	 * The high-level API for uploading a file to Synapse.
	 */
	public FileHandle createFileHandle(File temp, String contentType, String parentEntityId) throws SynapseException, IOException;

	/**
	 * See {@link #createFileHandle(File, String)}
	 * 
	 * @param shouldPreviewBeCreated Default true
	 */
	public FileHandle createFileHandle(File temp, String contentType, Boolean shouldPreviewBeCreated, String parentEntityId)
			throws SynapseException, IOException;

	public FileHandle createFileHandle(File temp, String contentType, Boolean shouldPreviewBeCreated, String parentEntityId,
			Long storageLocationId) throws SynapseException, IOException;

	public ChunkedFileToken createChunkedFileUploadToken(
			CreateChunkedFileTokenRequest ccftr) throws SynapseException;

	public URL createChunkedPresignedUrl(ChunkRequest chunkRequest)
			throws SynapseException;

	public String putFileToURL(URL url, File file, String contentType)
			throws SynapseException;

	public UploadDaemonStatus startUploadDeamon(CompleteAllChunksRequest cacr)
			throws SynapseException;

	public UploadDaemonStatus getCompleteUploadDaemonStatus(String daemonId)
			throws SynapseException;

	public ExternalFileHandle createExternalFileHandle(ExternalFileHandle efh)
			throws JSONObjectAdapterException, SynapseException;
	
	/**
	 * Create a new ProxyFileHandle. Note: ProxyFileHandle.storageLocationsId
	 * must be set to reference a valid ProxyStorageLocationSettings.
	 * @param handle
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	ProxyFileHandle createExternalProxyFileHandle(ProxyFileHandle handle)
			throws JSONObjectAdapterException, SynapseException;
	
	/**
	 * Create an S3FileHandle using a pre-configured ExternalS3StorageLocationSetting ID.
	 * @param handle
	 * @return
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public S3FileHandle createExternalS3FileHandle(S3FileHandle handle) throws JSONObjectAdapterException, SynapseException;
	
	/**
	 * Create a new file handle with optionally a new name and a new content type
	 * 
	 * @param originalFileHandleId
	 * @param name
	 * @param contentType
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public S3FileHandle createS3FileHandleCopy(String originalFileHandleId, String name, String contentType)
			throws JSONObjectAdapterException, SynapseException;

	public FileHandle getRawFileHandle(String fileHandleId) throws SynapseException;

	public void deleteFileHandle(String fileHandleId) throws SynapseException;

	public void clearPreview(String fileHandleId) throws SynapseException;

	public String s3FileCopyAsyncStart(List<String> fileEntityIds, String destinationBucket, Boolean overwrite, String baseKey)
			throws SynapseException;

	public S3FileCopyResults s3FileCopyAsyncGet(String asyncJobToken) throws SynapseException, SynapseResultNotReadyException;

	public WikiPage createWikiPage(String ownerId, ObjectType ownerType,
			WikiPage toCreate) throws JSONObjectAdapterException,
			SynapseException;

	public WikiPage getRootWikiPage(String ownerId, ObjectType ownerType)
			throws JSONObjectAdapterException, SynapseException;

	public FileHandleResults getWikiAttachmenthHandles(WikiPageKey key)
			throws JSONObjectAdapterException, SynapseException;

	public void deleteWikiPage(WikiPageKey key) throws SynapseException;

	public PaginatedResults<WikiHeader> getWikiHeaderTree(String ownerId,
			ObjectType ownerType) throws SynapseException,
			JSONObjectAdapterException;

	public FileHandleResults getEntityFileHandlesForCurrentVersion(String entityId)
			throws JSONObjectAdapterException, SynapseException;

	public FileHandleResults getEntityFileHandlesForVersion(String entityId,
			Long versionNumber) throws JSONObjectAdapterException,
			SynapseException;

	public V2WikiPage createV2WikiPage(String ownerId, ObjectType ownerType,
			V2WikiPage toCreate) throws JSONObjectAdapterException,
			SynapseException;

	public V2WikiPage getV2WikiPage(WikiPageKey key)
			throws JSONObjectAdapterException, SynapseException;

	public V2WikiPage getVersionOfV2WikiPage(WikiPageKey key, Long version)
			throws JSONObjectAdapterException, SynapseException;
	
	public V2WikiPage updateV2WikiPage(String ownerId, ObjectType ownerType,
			V2WikiPage toUpdate) throws JSONObjectAdapterException,
			SynapseException;
	
	public V2WikiPage restoreV2WikiPage(String ownerId, ObjectType ownerType,
			String wikiId, Long versionToRestore) throws JSONObjectAdapterException,
			SynapseException;
	
	public V2WikiPage getV2RootWikiPage(String ownerId, ObjectType ownerType)
		throws JSONObjectAdapterException, SynapseException;

	public FileHandleResults getV2WikiAttachmentHandles(WikiPageKey key)
		throws JSONObjectAdapterException, SynapseException;

	public FileHandleResults getVersionOfV2WikiAttachmentHandles(WikiPageKey key, Long version)
		throws JSONObjectAdapterException, SynapseException;
	
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
		ObjectType ownerType) throws SynapseException,
		JSONObjectAdapterException;
	
	V2WikiOrderHint getV2OrderHint(WikiPageKey key) throws SynapseException, JSONObjectAdapterException;
	
	V2WikiOrderHint updateV2WikiOrderHint(V2WikiOrderHint toUpdate) throws JSONObjectAdapterException, SynapseException;
	
	public PaginatedResults<V2WikiHistorySnapshot> getV2WikiHistory(WikiPageKey key, Long limit, Long offset)
		throws JSONObjectAdapterException, SynapseException;
	

	
	@Deprecated
	public File downloadFromSynapse(String path, String md5, File destinationFile) throws SynapseException;

	/**
	 * Download the File attachment for an entity, following redirects as needed.
	 * 
	 * @param entityId
	 * @param destinationFile
	 * @throws SynapseException
	 */
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
	
	public String getSynapseTermsOfUse() throws SynapseException;
	
	public String getTermsOfUse(DomainType domain) throws SynapseException;
	
	/**
	 * Uploads a String to the default upload location, if S3 using the chunked file upload service Note: Strings in
	 * memory should not be large, so we limit the length of the byte array for the passed in string to be the size of
	 * one 'chunk'
	 * 
	 * @param content the byte array to upload.
	 * 
	 * @param contentType This will become the contentType field of the resulting S3FileHandle if not specified, this
	 *        method uses "text/plain"
	 * 
	 */ 
	public String uploadToFileHandle(byte[] content, ContentType contentType, String parentEntityId) throws SynapseException;

	/**
	 * Upload a file to Synapse. This is for uploading files that are not related to entities.
	 * To upload a file for a FileEntity use @see org.sagebionetworks.client.SynapseClient#uploadToFileHandle(byte[], org.apache.http.entity.ContentType, java.lang.String)
	 * @param content
	 * @param contentType
	 * @return
	 * @throws SynapseException
	 */
	public String uploadToFileHandle(byte[] content, ContentType contentType) throws SynapseException;

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
	 * Deletes a message.  Used for test cleanup only.  Admin only.
	 */
	public void deleteMessage(String messageId) throws SynapseException;
	
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

	public Long getChildCount(String entityId) throws SynapseException;

	public SynapseVersionInfo getVersionInfo() throws SynapseException,
			JSONObjectAdapterException;

	public Set<String> getAllUserAndGroupIds() throws SynapseException;

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

	public PaginatedResults<Evaluation> getEvaluationsPaginated(int offset, int limit)
			throws SynapseException;

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
	
	/**
	 * Add a contributor to an existing submission.  This is available to Synapse administrators only.
	 * @param submissionId
	 * @param contributor
	 * @return
	 */
	public SubmissionContributor addSubmissionContributor(String submissionId, SubmissionContributor contributor) throws SynapseException ;

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
	
	public Long getSubmissionCount(String evalId) throws SynapseException;

	public QueryTableResults queryEvaluation(String query) throws SynapseException;
	
	public StorageUsageSummaryList getStorageUsageSummary(List<StorageUsageDimension> aggregation) throws SynapseException;

	public void moveToTrash(String entityId) throws SynapseException;

	public void restoreFromTrash(String entityId, String newParentId)
			throws SynapseException;

	public PaginatedResults<TrashedEntity> viewTrashForUser(long offset, long limit)
			throws SynapseException;

	public void purgeTrashForUser(String entityId) throws SynapseException;

	public void purgeTrashForUser() throws SynapseException;

	public EntityHeader addFavorite(String entityId) throws SynapseException;

	public void removeFavorite(String entityId) throws SynapseException;

	public PaginatedResults<EntityHeader> getFavorites(Integer limit, Integer offset)
			throws SynapseException;

	public PaginatedResults<ProjectHeader> getMyProjects(ProjectListType type, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			Integer limit, Integer offset) throws SynapseException;

	public PaginatedResults<ProjectHeader> getProjectsFromUser(Long userId, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			Integer limit, Integer offset) throws SynapseException;

	public PaginatedResults<ProjectHeader> getProjectsForTeam(Long teamId, ProjectListSortColumn sortColumn, SortDirection sortDirection,
			Integer limit, Integer offset) throws SynapseException;

	public void createEntityDoi(String entityId) throws SynapseException;

	public void createEntityDoi(String entityId, Long entityVersion)
			throws SynapseException;

	public Doi getEntityDoi(String entityId) throws SynapseException;

	public Doi getEntityDoi(String s, Long entityVersion) throws SynapseException;

	public List<EntityHeader> getEntityHeaderByMd5(String md5) throws SynapseException;

	public String retrieveApiKey() throws SynapseException;

	public void invalidateApiKey() throws SynapseException;
	
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
	 * Get specific rows from table entity.
	 * 
	 * @param toGet
	 * @return
	 * @throws SynapseException
	 * @throws SynapseTableUnavailableException
	 */
	public RowSet getRowsFromTable(RowReferenceSet toGet) throws SynapseException, SynapseTableUnavailableException;

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
	 * @param isConsistent
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

	public String queryTableEntityBundleAsyncStart(String sql, Long offset, Long limit, boolean isConsistent, int partMask, String tableId)
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
	 * @param request The request body.
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
	 * @Deprecated
	 */
	public String uploadCsvToTableAsyncStart(String tableId, String fileHandleId, String etag, Long linesToSkip,
			CsvTableDescriptor csvDescriptor) throws SynapseException;

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
	 * @param model
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
	 * @param redirect
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
	 * 
	 * @param invitation
	 * @param acceptInvitationEndpoint the portal end-point for one-click acceptance of the membership
	 * invitation.  A signed, serialized token is appended to create the complete URL.
	 * @param notificationUnsubscribeEndpoint the portal prefix for one-click email unsubscription.
	 * A signed, serialized token is appended to create the complete URL.
	 * @return
	 * @throws SynapseException
	 */
	MembershipInvtnSubmission createMembershipInvitation(
			MembershipInvtnSubmission invitation,
			String acceptInvitationEndpoint, 
			String notificationUnsubscribeEndpoint ) throws SynapseException;

	/**
	 * 
	 * @param invitationId
	 * @return
	 * @throws SynapseException
	 */
	MembershipInvtnSubmission getMembershipInvitation(String invitationId) throws SynapseException;

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
	PaginatedResults<MembershipInvtnSubmission> getOpenMembershipInvitationSubmissions(String teamId, String inviteeId, long limit, long offset) throws SynapseException;

	/**
	 * 
	 * @param invitationId
	 * @throws SynapseException
	 */
	void deleteMembershipInvitation(String invitationId) throws SynapseException;
	
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
	MembershipRqstSubmission createMembershipRequest(MembershipRqstSubmission request,
			String acceptRequestEndpoint, 
			String notificationUnsubscribeEndpoint) throws SynapseException;
	/**
	 * 
	 * @param requestId
	 * @return
	 * @throws SynapseException
	 */
	MembershipRqstSubmission getMembershipRequest(String requestId) throws SynapseException;

	/**
	 * 
	 * @param teamId
	 * @param requestorId the id of the user requesting membership (optional)
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
	PaginatedResults<MembershipRqstSubmission> getOpenMembershipRequestSubmissions(String requesterId, String teamId, long limit, long offset) throws SynapseException;

	/**
	 * 
	 * @param requestId
	 * @throws SynapseException
	 */
	void deleteMembershipRequest(String requestId) throws SynapseException;

	

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

	/**
	 * Creates a user
	 */
	public void createUser(NewUser user) throws SynapseException;
	
	/**
	 * Changes the registering user's password
	 */
	public void changePassword(String sessionToken, String newPassword) throws SynapseException;
	
	/**
	 * Signs the terms of use for utilization of Synapse, as identified by a session token
	 */
	public void signTermsOfUse(String sessionToken, boolean acceptTerms) throws SynapseException;
	
	/**
	 * 
	 * Signs the terms of use for utilization of specific Dage application, as identified by a session token
	 */
	public void signTermsOfUse(String sessionToken, DomainType domain, boolean acceptTerms) throws SynapseException;
	
	/**
	 * Sends a password reset email to the given user as if request came from Synapse.
	 */
	public void sendPasswordResetEmail(String email) throws SynapseException;
	
	/**
	 * Performs OpenID authentication using the set of parameters from an OpenID provider
	 * @return A session token if the authentication passes
	 */
	@Deprecated
	public Session passThroughOpenIDParameters(String queryString) throws SynapseException;
	
	/**
	 * See {@link #passThroughOpenIDParameters(String)}
	 * 
	 * @param createUserIfNecessary
	 *            Whether a user should be created if the user does not already
	 *            exist
	 */
	@Deprecated
	public Session passThroughOpenIDParameters(String queryString,
			Boolean createUserIfNecessary) throws SynapseException;

	/**
	 * @param domain Which client did the user access to authenticate via a third party provider (Synapse or Other)?
	 */
	@Deprecated
	public Session passThroughOpenIDParameters(String queryString,
			Boolean createUserIfNecessary, DomainType domain)
			throws SynapseException;
	
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
	 * For integration testing only:  This allows an administrator to set the Certified user status
	 * of a user.
	 * @param prinicipalId
	 * @param status
	 * @throws SynapseException
	 */
	public void setCertifiedUserStatus(String prinicipalId, boolean status) throws SynapseException;
	
	/**
	 * Must be a Synapse admin to make this request
	 * 
	 * @param offset
	 * @param limit
	 * @param principalId (optional)
	 * @return the C.U. test responses in the system, optionally filtered by principalId
	 * @throws SynapseException 
	 */
	public PaginatedResults<QuizResponse> getCertifiedUserTestResponses(long offset, long limit, String principalId) throws SynapseException;
	
	/**
	 * Delete the Test Response indicated by the given id
	 * 
	 * Must be a Synapse admin to make this request
	 * 
	 * @param id
	 * @throws SynapseException 
	 */
	public void deleteCertifiedUserTestResponse(String id) throws SynapseException;
	
	/**
	 * Get the Passing Record on the Certified User test for the given user
	 * 
	 * @param principalId
	 * @return
	 * @throws SynapseException 
	 */
	public PassingRecord getCertifiedUserPassingRecord(String principalId) throws SynapseException;

	/**
	 * Get all Passing Records on the Certified User test for the given user
	 */
	public PaginatedResults<PassingRecord> getCertifiedUserPassingRecords(long offset, long limit, String principalId) throws SynapseException;
	
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
	 * @throws JSONObjectAdapterException 
	 */
	public AsynchronousJobStatus getAsynchronousJobStatus(String jobId) throws JSONObjectAdapterException, SynapseException;

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
	 * @param uploadDestinationSetting
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
	 * @param storageLocationId
	 * @return
	 * @throws SynapseException
	 */
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
	 * @param uploadId
	 * @return
	 * @throws SynapseException
	 */
	public UploadDestination getDefaultUploadDestination(String parentEntityId) throws SynapseException;

	/**
	 * create a project setting
	 * 
	 * @param projectId
	 * @param projectSetting
	 * @throws SynapseException
	 */
	ProjectSetting createProjectSetting(ProjectSetting projectSetting) throws SynapseException;

	/**
	 * create a project setting
	 * 
	 * @param projectId
	 * @param projectSetting
	 * @throws SynapseException
	 */
	ProjectSetting getProjectSetting(String projectId, ProjectSettingsType projectSettingsType) throws SynapseException;

	/**
	 * create a project setting
	 * 
	 * @param projectId
	 * @param projectSetting
	 * @throws SynapseException
	 */
	void updateProjectSetting(ProjectSetting projectSetting) throws SynapseException;

	/**
	 * create a project setting
	 * 
	 * @param projectId
	 * @param projectSetting
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
	 * Execute a query to find entities that meet the conditions provided query.
	 * @param query
	 * @return
	 * @throws SynapseException 
	 */
	EntityQueryResults entityQuery(EntityQuery query) throws SynapseException;
	
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
	 * @throws SynapseException.   If the caller specifies an illegal state transition a BadRequestException will be thrown.
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
	 * @param projectId
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
	 * Get the discussion thread given its ID
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
	S3FileHandle multipartUpload(InputStream input, long fileSize, String fileName, String contentType, Long storageLocationId, Boolean generatePreview, Boolean forceRestart) throws SynapseException;
	
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
	S3FileHandle multipartUpload(File file, Long storageLocationId, Boolean generatePreview, Boolean forceRestart) throws SynapseException, FileNotFoundException, IOException;

	/**
	 * Subscribe to a topic
	 * 
	 * @param toSubscribe
	 * @return
	 * @throws SynapseException 
	 */
	Subscription subscribe(Topic toSubscribe) throws SynapseException;

	/**
	 * Retrieve all subscriptions one has
	 * 
	 * @param objectType
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException 
	 */
	SubscriptionPagedResults getAllSubscriptions(SubscriptionObjectType objectType, Long limit, Long offset) throws SynapseException;

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
	 * Retrieve the current etag for a given object.
	 * 
	 * @param objectId
	 * @param objectType
	 * @return
	 * @throws SynapseException
	 */
	Etag getEtag(String objectId, ObjectType objectType) throws SynapseException;
	
	/**
	 * Get the entity ID assigned to a given alias.
	 * 
	 * @param alias
	 * @return
	 * @throws SynapseException
	 */
	EntityId getEntityIdByAlias(String alias) throws SynapseException;

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
}
