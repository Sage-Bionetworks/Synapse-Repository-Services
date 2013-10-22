package org.sagebionetworks.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseException;
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
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
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
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.auth.NewUser;
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
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.query.QueryTableResults;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Abstraction for Synapse.
 * 
 * @author jmhill
 * 
 */
public interface SynapseClient {

	/**
	 * Get the current status of the stack
	 */
	public StackStatus getCurrentStackStatus() 
			throws SynapseException, JSONObjectAdapterException;

	/**
	 * Get the endpoint of the repository service
	 */
	public String getRepoEndpoint();

	/**
	 * Each request includes the 'User-Agent' header. This is set to:
	 * 'User-Agent':'Synpase-Java-Client/<version_number>'
	 * 
	 * @param toAppend
	 *            Addition User-Agent information can be appended to this string
	 *            via this parameter
	 */
	public void appendUserAgent(String toAppend);

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

	/**
	 * Authenticate the Synapse client with an existing session token
	 */
	public void setSessionToken(String sessionToken);

	public AttachmentData uploadAttachmentToSynapse(String entityId, File temp, String fileName) 
			throws JSONObjectAdapterException, SynapseException, IOException;

	public Entity getEntityById(String entityId) throws SynapseException;

	public <T extends Entity> T putEntity(T entity) throws SynapseException;

	@Deprecated
	public PresignedUrl waitForPreviewToBeCreated(String entityId,
			String tokenId, int maxTimeOut) throws SynapseException,
			JSONObjectAdapterException;

	@Deprecated
	public PresignedUrl createAttachmentPresignedUrl(String entityId,
			String tokenId) throws SynapseException, JSONObjectAdapterException;

	public URL getWikiAttachmentPreviewTemporaryUrl(WikiPageKey properKey,
			String fileName) throws ClientProtocolException, IOException;

	public URL getWikiAttachmentTemporaryUrl(WikiPageKey properKey,
			String fileName) throws ClientProtocolException, IOException;

	/**
	 * Log into Synapse
	 */
	public UserSessionData login(String username, String password)
			throws SynapseException;

	/**
	 * Log into Synapse and specify whether you agree to the terms of use
	 */
	public UserSessionData login(String username, String password, boolean explicitlyAcceptsTermsOfUse) 
			throws SynapseException;

	/**
	 * Log into Synapse, 
	 *   do not return UserSessionData, 
	 *   do not request a user profile, 
	 *   and do not explicitly accept the terms of use
	 */
	public void loginWithNoProfile(String userName, String password)
			throws SynapseException;
	
	/**
	 * Log out of Synapse
	 */
	public void logout() throws SynapseException;

	public UserSessionData getUserSessionData() throws SynapseException;

	/**
	 * Refreshes the cached session token so that it can be used for another 24 hours
	 */
	public boolean revalidateSession() throws SynapseException;

	/**
	 * Get the current session token used by this client.
	 * 
	 * @return the session token
	 */
	public String getCurrentSessionToken();

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
			throws ClientProtocolException, MalformedURLException, IOException;

	public URL getFileEntityTemporaryUrlForCurrentVersion(String entityId)
			throws ClientProtocolException, MalformedURLException, IOException;

	public URL getFileEntityPreviewTemporaryUrlForVersion(String entityId,
			Long versionNumber) throws ClientProtocolException,
			MalformedURLException, IOException;

	public URL getFileEntityTemporaryUrlForVersion(String entityId,
			Long versionNumber) throws ClientProtocolException,
			MalformedURLException, IOException;

	public S3FileHandle createFileHandle(File temp, String contentType)
			throws SynapseException, IOException;

	/**
	 * Get a WikiPage using its key
	 */
	public WikiPage getWikiPage(WikiPageKey properKey)
			throws JSONObjectAdapterException, SynapseException;

	public VariableContentPaginatedResults<AccessRequirement> getAccessRequirements(
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

	public UserProfile getUserProfile(String ownerId) throws SynapseException;

	public UserGroupHeaderResponsePage getUserGroupHeadersByIds(List<String> ids)
			throws SynapseException;

	public UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(String prefix)
			throws SynapseException, UnsupportedEncodingException;

	public AccessControlList updateACL(AccessControlList acl) throws SynapseException;

	public AccessControlList updateACL(AccessControlList acl, boolean recursive)
			throws SynapseException;

	public void deleteACL(String entityId) throws SynapseException;

	public AccessControlList createACL(AccessControlList acl) throws SynapseException;

	public PaginatedResults<UserProfile> getUsers(int offset, int limit)
			throws SynapseException;

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

	public ACTAccessRequirement createLockAccessRequirement(String entityId)
			throws SynapseException;

	public VariableContentPaginatedResults<AccessRequirement> getUnmetAccessRequirements(
			RestrictableObjectDescriptor subjectId) throws SynapseException;

	public <T extends AccessApproval> T createAccessApproval(T aa)
			throws SynapseException;
	
	public JSONObject getEntity(String uri) throws SynapseException;

	public <T extends JSONEntity> T getEntity(String entityId,
			Class<? extends T> clazz) throws SynapseException;

	public void deleteAccessRequirement(Long arId) throws SynapseException;

	public <T extends Entity> T putEntity(T entity, String activityId)
			throws SynapseException;

	public <T extends Entity> void deleteEntity(T entity)
			throws SynapseException;

	public <T extends Entity> void deleteAndPurgeEntity(T entity)
			throws SynapseException;

	public void deleteEntityById(String entityId) throws SynapseException;

	public void deleteAndPurgeEntityById(String entityId) throws SynapseException;

	public <T extends Entity> void deleteEntityVersion(T entity,
			Long versionNumber) throws SynapseException;

	public void deleteEntityVersionById(String entityId, Long versionNumber)
			throws SynapseException;

	public EntityPath getEntityPath(Entity entity) throws SynapseException;

	public EntityPath getEntityPath(String entityId) throws SynapseException;

	public BatchResults<EntityHeader> getEntityTypeBatch(List<String> entityIds)
			throws SynapseException;

	public BatchResults<EntityHeader> getEntityHeaderBatch(List<Reference> references)
			throws SynapseException;

	public PaginatedResults<EntityHeader> getEntityReferencedBy(Entity entity)
			throws SynapseException;

	public PaginatedResults<EntityHeader> getEntityReferencedBy(String entityId,
			String targetVersion) throws SynapseException;

	public JSONObject query(String query) throws SynapseException;

	public FileHandleResults createFileHandles(List<File> files)
			throws SynapseException;

	public ChunkedFileToken createChunkedFileUploadToken(
			CreateChunkedFileTokenRequest ccftr) throws SynapseException;

	public URL createChunkedPresignedUrl(ChunkRequest chunkRequest)
			throws SynapseException;

	public String putFileToURL(URL url, File file, String contentType)
			throws SynapseException;

	@Deprecated
	public ChunkResult addChunkToFile(ChunkRequest chunkRequest)
			throws SynapseException;

	@Deprecated
	public S3FileHandle completeChunkFileUpload(CompleteChunkedFileRequest request)
			throws SynapseException;

	public UploadDaemonStatus startUploadDeamon(CompleteAllChunksRequest cacr)
			throws SynapseException;

	public UploadDaemonStatus getCompleteUploadDaemonStatus(String daemonId)
			throws SynapseException;

	public ExternalFileHandle createExternalFileHandle(ExternalFileHandle efh)
			throws JSONObjectAdapterException, SynapseException;

	public FileHandle getRawFileHandle(String fileHandleId) throws SynapseException;

	public void deleteFileHandle(String fileHandleId) throws SynapseException;

	public void clearPreview(String fileHandleId) throws SynapseException;

	public WikiPage createWikiPage(String ownerId, ObjectType ownerType,
			WikiPage toCreate) throws JSONObjectAdapterException,
			SynapseException;

	public WikiPage getRootWikiPage(String ownerId, ObjectType ownerType)
			throws JSONObjectAdapterException, SynapseException;

	public FileHandleResults getWikiAttachmenthHandles(WikiPageKey key)
			throws JSONObjectAdapterException, SynapseException;

	public File downloadWikiAttachment(WikiPageKey key, String fileName)
			throws ClientProtocolException, IOException;

	public File downloadWikiAttachmentPreview(WikiPageKey key, String fileName)
			throws ClientProtocolException, FileNotFoundException, IOException;

	public void deleteWikiPage(WikiPageKey key) throws SynapseException;

	public PaginatedResults<WikiHeader> getWikiHeaderTree(String ownerId,
			ObjectType ownerType) throws SynapseException,
			JSONObjectAdapterException;

	public FileHandleResults getEntityFileHandlesForCurrentVersion(String entityId)
			throws JSONObjectAdapterException, SynapseException;

	public FileHandleResults getEntityFileHandlesForVersion(String entityId,
			Long versionNumber) throws JSONObjectAdapterException,
			SynapseException;

	@Deprecated
	public File downloadLocationableFromSynapse(Locationable locationable)
			throws SynapseException;

	@Deprecated
	public File downloadLocationableFromSynapse(Locationable locationable,
			File destinationFile) throws SynapseException;

	@Deprecated
	public File downloadFromSynapse(LocationData location, String md5,
			File destinationFile) throws SynapseException;

	@Deprecated
	public File downloadFromSynapse(String path, String md5, File destinationFile)
			throws SynapseException;

	@Deprecated
	public Locationable uploadLocationableToSynapse(Locationable locationable,
			File dataFile) throws SynapseException;

	@Deprecated
	public Locationable uploadLocationableToSynapse(Locationable locationable,
			File dataFile, String md5) throws SynapseException;

	@Deprecated
	public Locationable updateExternalLocationableToSynapse(Locationable locationable,
			String externalUrl) throws SynapseException;

	@Deprecated
	public Locationable updateExternalLocationableToSynapse(Locationable locationable,
			String externalUrl, String md5) throws SynapseException;

	@Deprecated
	public AttachmentData uploadAttachmentToSynapse(String entityId, File dataFile)
			throws JSONObjectAdapterException, SynapseException, IOException;

	@Deprecated
	public AttachmentData uploadUserProfileAttachmentToSynapse(String userId,
			File dataFile, String fileName) throws JSONObjectAdapterException,
			SynapseException, IOException;

	@Deprecated
	public AttachmentData uploadAttachmentToSynapse(String id,
			AttachmentType attachmentType, File dataFile, String fileName)
			throws JSONObjectAdapterException, SynapseException, IOException;

	@Deprecated
	public PresignedUrl createUserProfileAttachmentPresignedUrl(String id,
			String tokenOrPreviewId) throws SynapseException,
			JSONObjectAdapterException;

	@Deprecated
	public PresignedUrl createAttachmentPresignedUrl(String id,
			AttachmentType attachmentType, String tokenOrPreviewId)
			throws SynapseException, JSONObjectAdapterException;

	@Deprecated
	public PresignedUrl waitForUserProfilePreviewToBeCreated(String userId,
			String tokenOrPreviewId, int timeout) throws SynapseException,
			JSONObjectAdapterException;

	@Deprecated
	public PresignedUrl waitForPreviewToBeCreated(String id, AttachmentType type,
			String tokenOrPreviewId, int timeout) throws SynapseException,
			JSONObjectAdapterException;

	@Deprecated
	public void downloadEntityAttachment(String entityId,
			AttachmentData attachmentData, File destFile)
			throws SynapseException, JSONObjectAdapterException;

	@Deprecated
	public void downloadUserProfileAttachment(String userId,
			AttachmentData attachmentData, File destFile)
			throws SynapseException, JSONObjectAdapterException;

	@Deprecated
	public void downloadAttachment(String id, AttachmentType type,
			AttachmentData attachmentData, File destFile)
			throws SynapseException, JSONObjectAdapterException;

	@Deprecated
	public void downloadEntityAttachmentPreview(String entityId, String previewId,
			File destFile) throws SynapseException, JSONObjectAdapterException;

	@Deprecated
	public void downloadUserProfileAttachmentPreview(String userId, String previewId,
			File destFile) throws SynapseException, JSONObjectAdapterException;

	@Deprecated
	public void downloadAttachmentPreview(String id, AttachmentType type,
			String previewId, File destFile) throws SynapseException,
			JSONObjectAdapterException;

	@Deprecated
	public S3AttachmentToken createAttachmentS3Token(String id,
			AttachmentType attachmentType, S3AttachmentToken token)
			throws JSONObjectAdapterException, SynapseException;
	
	public String getSynapseTermsOfUse() throws SynapseException;

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

	public EntityIdList getDescendants(String nodeId, int pageSize,
			String lastDescIdExcl) throws SynapseException;

	public EntityIdList getDescendants(String nodeId, int generation, int pageSize,
			String lastDescIdExcl) throws SynapseException;

	public Evaluation createEvaluation(Evaluation eval) throws SynapseException;

	public Evaluation getEvaluation(String evalId) throws SynapseException;

	public PaginatedResults<Evaluation> getEvaluationByContentSource(String id,
			int offset, int limit) throws SynapseException;

	@Deprecated
	public PaginatedResults<Evaluation> getEvaluationsPaginated(int offset, int limit)
			throws SynapseException;

	@Deprecated
	public PaginatedResults<Evaluation> getAvailableEvaluationsPaginated(
			EvaluationStatus status, int offset, int limit)
			throws SynapseException;

	public Long getEvaluationCount() throws SynapseException;

	public Evaluation findEvaluation(String name) throws SynapseException,
			UnsupportedEncodingException;

	public Evaluation updateEvaluation(Evaluation eval) throws SynapseException;

	public void deleteEvaluation(String evalId) throws SynapseException;

	public Participant createParticipant(String evalId) throws SynapseException;

	public Participant getParticipant(String evalId, String principalId)
			throws SynapseException;

	public void deleteParticipant(String evalId, String principalId)
			throws SynapseException;

	public PaginatedResults<Participant> getAllParticipants(String s, long offset,
			long limit) throws SynapseException;

	public Long getParticipantCount(String evalId) throws SynapseException;

	public Submission createSubmission(Submission sub, String etag)
			throws SynapseException;

	public Submission getSubmission(String subId) throws SynapseException;

	public SubmissionStatus getSubmissionStatus(String subId) throws SynapseException;

	public SubmissionStatus updateSubmissionStatus(SubmissionStatus status)
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
			MalformedURLException, IOException;

	public Long getSubmissionCount(String evalId) throws SynapseException;

	public UserEvaluationState getUserEvaluationState(String evalId)
			throws SynapseException;

	public QueryTableResults queryEvaluation(String query) throws SynapseException;

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
	 * Create a new ColumnModel. If a column already exists with the same parameters,
	 * that column will be returned.
	 * @param model
	 * @return
	 * @throws SynapseException 
	 */
	ColumnModel createColumnModel(ColumnModel model) throws SynapseException;
	
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
	 * 
	 * @param memberId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 */
	PaginatedResults<Team> getTeamsForUser(String memberId, long limit, long offset) throws SynapseException;
	
	/**
	 * 
	 * @param teamId
	 * @param redirect
	 * @return
	 * @throws SynapseException if no icon for team (service throws 404)
	 */
	URL getTeamIcon(String teamId, Boolean redirect) throws SynapseException;
	
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
	 * 
	 * @param teamId
	 * @param memberId
	 * @throws SynapseException
	 */
	void addTeamMember(String teamId, String memberId) throws SynapseException;
	
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
	 * 
	 * @param teamId
	 * @param memberId
	 * @throws SynapseException
	 */
	void removeTeamMember(String teamId, String memberId) throws SynapseException;
	
	/**
	 * 
	 * @param teamId
	 * @param memberId
	 * @param isAdmin
	 * @throws SynapseException
	 */
	void setTeamMemberPermissions(String teamId, String memberId, boolean isAdmin) throws SynapseException;
	
	/**
	 * 
	 * @param teamId
	 * @param principalId
	 * @return
	 * @throws SynapseException
	 */
	TeamMembershipStatus getTeamMembershipStatus(String teamId, String principalId) throws SynapseException;

	/**
	 * 
	 * @param invitation
	 * @return
	 * @throws SynapseException
	 */
	MembershipInvtnSubmission createMembershipInvitation(MembershipInvtnSubmission invitation) throws SynapseException;

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
	 * @param invitationId
	 * @throws SynapseException
	 */
	void deleteMembershipInvitation(String invitationId) throws SynapseException;
	
	/**
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	MembershipRqstSubmission createMembershipRequest(MembershipRqstSubmission request) throws SynapseException;
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
	 * @return a list of membership requests sent to the given team, optionally filtered by the requestor
	 * @throws SynapseException
	 */
	PaginatedResults<MembershipRequest> getOpenMembershipRequests(String teamId, String requestorId, long limit, long offset) throws SynapseException;

	/**
	 * 
	 * @param requestId
	 * @throws SynapseException
	 */
	void deleteMembershipRequest(String requestId) throws SynapseException;


	/**
	 * Refesh the prefix-cache for retrieving teams and team members
	 * @throws SynapseException
	 */
	void updateTeamSearchCache() throws SynapseException;
	

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
	 * Retrieves the bare-minimum amount of information about the current user
	 * i.e. email and name
	 */
	public NewUser getAuthUserInfo() throws SynapseException;
	
	/**
	 * Changes the current user's password
	 */
	public void changePassword(String newPassword) throws SynapseException;

	/**
	 * Changes the registering user's password
	 */
	public void changePassword(String sessionToken, String newPassword) throws SynapseException;
	
	/**
	 * Changes the current user's email to the email corresponding to the supplied session token
	 */
	public void changeEmail(String sessionToken, String newPassword) throws SynapseException;
	
	/**
	 * Sends a password reset email to the current user 
	 */
	public void sendPasswordResetEmail() throws SynapseException;
	
	/**
	 * Sends a password reset email to the given user
	 */
	public void sendPasswordResetEmail(String email) throws SynapseException;
	
	/**
	 * Performs OpenID authentication using the set of parameters from an OpenID provider
	 * @return A session token if the authentication passes
	 */
	public Session passThroughOpenIDParameters(String queryString, Boolean acceptsTermsOfUse) throws SynapseException;
}
