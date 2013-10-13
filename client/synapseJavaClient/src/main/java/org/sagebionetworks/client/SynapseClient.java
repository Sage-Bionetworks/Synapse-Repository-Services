package org.sagebionetworks.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
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
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Abstraction for Synpase.
 * 
 * @author jmhill
 *
 */
public interface SynapseClient {

	/**
	 * Get the current status of the stack.
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public StackStatus getCurrentStackStatus() throws SynapseException,	JSONObjectAdapterException;
	
	/**
	 * Get the endpoint of the repository service.
	 * @return
	 */
	public String getRepoEndpoint();

	/**
	 * Each request includes the 'User-Agent' header. This is set to:
	 * 'User-Agent':'Synpase-Java-Client/<version_number>'
	 * Addition User-Agent information can be appended to this string by calling this method.
	 * @param toAppend
	 */
	public void appendUserAgent(String toAppend);

	/**
	 * The repository end-point includes the host and version.  For example: "https://repo-prod.prod.sagebase.org/repo/v1"
	 * 
	 * @param repoEndpoint
	 */
	public void setRepositoryEndpoint(String repoEndpoint);

	public void setAuthEndpoint(String authEndpoint);

	/**
	 * Get the configured Authorization Service Endpoint
	 * @return
	 */
	public String getAuthEndpoint();

	public void setFileEndpoint(String fileEndpoint);

	public String getFileEndpoint();

	/**
	 * Authenticate the synapse client with an existing session token
	 * 
	 * @param sessionToken
	 */
	public void setSessionToken(String sessionToken);

	public AttachmentData uploadAttachmentToSynapse(String entityId, File temp,	String fileName) throws JSONObjectAdapterException, SynapseException, IOException;

	public Entity getEntityById(String entityId) throws SynapseException;

	public <T extends Entity> T putEntity(T entity) throws SynapseException;

	@Deprecated
	public PresignedUrl waitForPreviewToBeCreated(String entityId, String tokenId, int maxTimeOut) throws SynapseException, JSONObjectAdapterException;

	@Deprecated
	public PresignedUrl createAttachmentPresignedUrl(String entityId, String tokenId) throws SynapseException, JSONObjectAdapterException;

	public URL getWikiAttachmentPreviewTemporaryUrl(WikiPageKey properKey,
			String fileName) throws ClientProtocolException, IOException;

	public URL getWikiAttachmentTemporaryUrl(WikiPageKey properKey,
			String fileName) throws ClientProtocolException, IOException;

	/**
	 * Log into Synapse
	 * 
	 * @param username
	 * @param password
	 * @throws SynapseException
	 */
	UserSessionData login(String username, String password)
			throws SynapseException;

	UserSessionData login(String username, String password,
			boolean explicitlyAcceptsTermsOfUse) throws SynapseException;

	/**
	 * 
	 * Log into Synapse, do not return UserSessionData, do not request user profile, do not explicitely accept terms of use
	 * 
	 * @param userName
	 * @param password
	 * @throws SynapseException 
	 */
	void loginWithNoProfile(String userName, String password)
			throws SynapseException;

	UserSessionData getUserSessionData() throws SynapseException;

	boolean revalidateSession() throws SynapseException;
	/**
	 * Get the current session token used by this client.
	 * 
	 * @return the session token
	 */
	String getCurrentSessionToken();

	/**
	 * Create a new Entity.
	 * 
	 * @param <T>
	 * @param entity
	 * @return the newly created entity
	 * @throws SynapseException
	 */
	public <T extends Entity> T createEntity(T entity) throws SynapseException;

	JSONObject createJSONObject(String uri, JSONObject entity)
			throws SynapseException;

	public SearchResults search(SearchQuery searchQuery) throws SynapseException, UnsupportedEncodingException, JSONObjectAdapterException;

	public URL getFileEntityPreviewTemporaryUrlForCurrentVersion(String entityId) throws ClientProtocolException, MalformedURLException, IOException;

	URL getFileEntityTemporaryUrlForCurrentVersion(String entityId)
			throws ClientProtocolException, MalformedURLException, IOException;

	public URL getFileEntityPreviewTemporaryUrlForVersion(String entityId,
			Long versionNumber) throws ClientProtocolException, MalformedURLException, IOException;

	public URL getFileEntityTemporaryUrlForVersion(String entityId,
			Long versionNumber) throws ClientProtocolException, MalformedURLException, IOException;

	public S3FileHandle createFileHandle(File temp, String contentType) throws SynapseException, IOException;

	/**
	 * Get a WikiPage using its key
	 * @param key
	 * @return
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public WikiPage getWikiPage(WikiPageKey properKey) throws JSONObjectAdapterException, SynapseException;


	public VariableContentPaginatedResults<AccessRequirement> getAccessRequirements(
			RestrictableObjectDescriptor subjectId) throws SynapseException;

	WikiPage updateWikiPage(String ownerId, ObjectType ownerType,
			WikiPage toUpdate) throws JSONObjectAdapterException,
			SynapseException;

	void setRequestProfile(boolean request);

	JSONObject getProfileData();

	String getUserName();

	void setUserName(String userName);

	String getApiKey();

	void setApiKey(String apiKey);

	<T extends Entity> T createEntity(T entity, String activityId) throws SynapseException;

	public <T extends JSONEntity> T createJSONEntity(String uri, T entity)
			throws SynapseException;

	EntityBundle createEntityBundle(EntityBundleCreate ebc)
			throws SynapseException;

	EntityBundle createEntityBundle(EntityBundleCreate ebc, String activityId)
			throws SynapseException;

	EntityBundle updateEntityBundle(String entityId, EntityBundleCreate ebc)
			throws SynapseException;

	EntityBundle updateEntityBundle(String entityId, EntityBundleCreate ebc,
			String activityId) throws SynapseException;

	JSONObject getEntity(String uri) throws SynapseException;

	Entity getEntityByIdForVersion(String entityId, Long versionNumber)
			throws SynapseException;

	EntityBundle getEntityBundle(String entityId, int partsMask)
			throws SynapseException;

	EntityBundle getEntityBundle(String entityId, Long versionNumber,
			int partsMask) throws SynapseException;

	PaginatedResults<VersionInfo> getEntityVersions(String entityId,
			int offset, int limit) throws SynapseException;

	AccessControlList getACL(String entityId) throws SynapseException;

	EntityHeader getEntityBenefactor(String entityId) throws SynapseException;

	UserProfile getMyProfile() throws SynapseException;

	void updateMyProfile(UserProfile userProfile) throws SynapseException;

	UserProfile getUserProfile(String ownerId) throws SynapseException;

	UserGroupHeaderResponsePage getUserGroupHeadersByIds(List<String> ids)
			throws SynapseException;

	UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(String prefix)
			throws SynapseException, UnsupportedEncodingException;

	AccessControlList updateACL(AccessControlList acl) throws SynapseException;

	AccessControlList updateACL(AccessControlList acl, boolean recursive)
			throws SynapseException;

	void deleteACL(String entityId) throws SynapseException;

	AccessControlList createACL(AccessControlList acl) throws SynapseException;

	PaginatedResults<UserProfile> getUsers(int offset, int limit)
			throws SynapseException;

	PaginatedResults<UserGroup> getGroups(int offset, int limit)
			throws SynapseException;

	boolean canAccess(String entityId, ACCESS_TYPE accessType)
			throws SynapseException;

	boolean canAccess(String id, ObjectType type, ACCESS_TYPE accessType)
			throws SynapseException;

	UserEntityPermissions getUsersEntityPermissions(String entityId)
			throws SynapseException;

	Annotations getAnnotations(String entityId) throws SynapseException;

	Annotations updateAnnotations(String entityId, Annotations updated)
			throws SynapseException;

	public <T extends AccessRequirement> T createAccessRequirement(T ar) throws SynapseException;

	ACTAccessRequirement createLockAccessRequirement(String entityId)
			throws SynapseException;

	VariableContentPaginatedResults<AccessRequirement> getUnmetAccessRequirements(
			RestrictableObjectDescriptor subjectId) throws SynapseException;

	public <T extends AccessApproval> T createAccessApproval(T aa) throws SynapseException;

	public <T extends JSONEntity> T getEntity(String entityId, Class<? extends T> clazz) throws SynapseException;

	void deleteAccessRequirement(Long arId) throws SynapseException;

	@Deprecated
	public JSONObject updateEntity(String uri, JSONObject entity)
			throws SynapseException;

	public <T extends Entity> T putEntity(T entity, String activityId) throws SynapseException;

	JSONObject putJSONObject(String uri, JSONObject entity,
			Map<String, String> headers) throws SynapseException;

	JSONObject postUri(String uri) throws SynapseException;

	void deleteUri(String uri) throws SynapseException;
	
	public <T extends Entity> void deleteEntity(T entity) throws SynapseException;
	
	public <T extends Entity> void deleteAndPurgeEntity(T entity) throws SynapseException;
	
	public void deleteEntityById(String entityId)
			throws SynapseException;

	void deleteAndPurgeEntityById(String entityId) throws SynapseException;
	
	public <T extends Entity> void deleteEntityVersion(T entity, Long versionNumber) throws SynapseException;

	void deleteEntityVersionById(String entityId, Long versionNumber)
			throws SynapseException;

	EntityPath getEntityPath(Entity entity) throws SynapseException;

	EntityPath getEntityPath(String entityId) throws SynapseException;

	BatchResults<EntityHeader> getEntityTypeBatch(List<String> entityIds)
			throws SynapseException;

	BatchResults<EntityHeader> getEntityHeaderBatch(List<Reference> references)
			throws SynapseException;

	PaginatedResults<EntityHeader> getEntityReferencedBy(Entity entity)
			throws SynapseException;

	PaginatedResults<EntityHeader> getEntityReferencedBy(String entityId,
			String targetVersion) throws SynapseException;

	JSONObject query(String query) throws SynapseException;

	FileHandleResults createFileHandles(List<File> files)
			throws SynapseException;

	ChunkedFileToken createChunkedFileUploadToken(
			CreateChunkedFileTokenRequest ccftr) throws SynapseException;

	URL createChunkedPresignedUrl(ChunkRequest chunkRequest)
			throws SynapseException;

	String putFileToURL(URL url, File file, String contentType)
			throws SynapseException;

	@Deprecated
	ChunkResult addChunkToFile(ChunkRequest chunkRequest)
			throws SynapseException;

	@Deprecated
	S3FileHandle completeChunkFileUpload(CompleteChunkedFileRequest request)
			throws SynapseException;

	UploadDaemonStatus startUploadDeamon(CompleteAllChunksRequest cacr)
			throws SynapseException;

	UploadDaemonStatus getCompleteUploadDaemonStatus(String daemonId)
			throws SynapseException;

	ExternalFileHandle createExternalFileHandle(ExternalFileHandle efh)
			throws JSONObjectAdapterException, SynapseException;

	FileHandle getRawFileHandle(String fileHandleId) throws SynapseException;

	void deleteFileHandle(String fileHandleId) throws SynapseException;

	void clearPreview(String fileHandleId) throws SynapseException;

	WikiPage createWikiPage(String ownerId, ObjectType ownerType,
			WikiPage toCreate) throws JSONObjectAdapterException,
			SynapseException;

	WikiPage getRootWikiPage(String ownerId, ObjectType ownerType)
			throws JSONObjectAdapterException, SynapseException;

	FileHandleResults getWikiAttachmenthHandles(WikiPageKey key)
			throws JSONObjectAdapterException, SynapseException;

	File downloadWikiAttachment(WikiPageKey key, String fileName)
			throws ClientProtocolException, IOException;

	File downloadWikiAttachmentPreview(WikiPageKey key, String fileName)
			throws ClientProtocolException, FileNotFoundException, IOException;

	void deleteWikiPage(WikiPageKey key) throws SynapseException;

	PaginatedResults<WikiHeader> getWikiHeaderTree(String ownerId,
			ObjectType ownerType) throws SynapseException,
			JSONObjectAdapterException;

	FileHandleResults getEntityFileHandlesForCurrentVersion(String entityId)
			throws JSONObjectAdapterException, SynapseException;

	FileHandleResults getEntityFileHandlesForVersion(String entityId,
			Long versionNumber) throws JSONObjectAdapterException,
			SynapseException;

	@Deprecated
	File downloadLocationableFromSynapse(Locationable locationable)
			throws SynapseException;

	@Deprecated
	File downloadLocationableFromSynapse(Locationable locationable,
			File destinationFile) throws SynapseException;

	@Deprecated
	File downloadFromSynapse(LocationData location, String md5,
			File destinationFile) throws SynapseException;

	@Deprecated
	File downloadFromSynapse(String path, String md5, File destinationFile)
			throws SynapseException;

	@Deprecated
	Locationable uploadLocationableToSynapse(Locationable locationable,
			File dataFile) throws SynapseException;

	@Deprecated
	Locationable uploadLocationableToSynapse(Locationable locationable,
			File dataFile, String md5) throws SynapseException;

	@Deprecated
	Locationable updateExternalLocationableToSynapse(Locationable locationable,
			String externalUrl) throws SynapseException;
	@Deprecated
	Locationable updateExternalLocationableToSynapse(Locationable locationable,
			String externalUrl, String md5) throws SynapseException;
	@Deprecated
	AttachmentData uploadAttachmentToSynapse(String entityId, File dataFile)
			throws JSONObjectAdapterException, SynapseException, IOException;
	@Deprecated
	AttachmentData uploadUserProfileAttachmentToSynapse(String userId,
			File dataFile, String fileName) throws JSONObjectAdapterException,
			SynapseException, IOException;
	@Deprecated
	AttachmentData uploadAttachmentToSynapse(String id,
			AttachmentType attachmentType, File dataFile, String fileName)
			throws JSONObjectAdapterException, SynapseException, IOException;
	@Deprecated
	PresignedUrl createUserProfileAttachmentPresignedUrl(String id,
			String tokenOrPreviewId) throws SynapseException,
			JSONObjectAdapterException;
	@Deprecated
	PresignedUrl createAttachmentPresignedUrl(String id,
			AttachmentType attachmentType, String tokenOrPreviewId)
			throws SynapseException, JSONObjectAdapterException;
	@Deprecated
	PresignedUrl waitForUserProfilePreviewToBeCreated(String userId,
			String tokenOrPreviewId, int timeout) throws SynapseException,
			JSONObjectAdapterException;
	@Deprecated
	PresignedUrl waitForPreviewToBeCreated(String id, AttachmentType type,
			String tokenOrPreviewId, int timeout) throws SynapseException,
			JSONObjectAdapterException;
	@Deprecated
	void downloadEntityAttachment(String entityId,
			AttachmentData attachmentData, File destFile)
			throws SynapseException, JSONObjectAdapterException;
	@Deprecated
	void downloadUserProfileAttachment(String userId,
			AttachmentData attachmentData, File destFile)
			throws SynapseException, JSONObjectAdapterException;
	@Deprecated
	void downloadAttachment(String id, AttachmentType type,
			AttachmentData attachmentData, File destFile)
			throws SynapseException, JSONObjectAdapterException;
	@Deprecated
	void downloadEntityAttachmentPreview(String entityId, String previewId,
			File destFile) throws SynapseException, JSONObjectAdapterException;
	@Deprecated
	void downloadUserProfileAttachmentPreview(String userId, String previewId,
			File destFile) throws SynapseException, JSONObjectAdapterException;
	@Deprecated
	void downloadAttachmentPreview(String id, AttachmentType type,
			String previewId, File destFile) throws SynapseException,
			JSONObjectAdapterException;
	@Deprecated
	S3AttachmentToken createAttachmentS3Token(String id,
			AttachmentType attachmentType, S3AttachmentToken token)
			throws JSONObjectAdapterException, SynapseException;

	JSONObject createAuthEntity(String uri, JSONObject entity)
			throws SynapseException;

	JSONObject getAuthEntity(String uri) throws SynapseException;

	JSONObject putAuthEntity(String uri, JSONObject entity)
			throws SynapseException;

	JSONObject createJSONObjectEntity(String endpoint, String uri,
			JSONObject entity) throws SynapseException;

	JSONObject getSynapseEntity(String endpoint, String uri)
			throws SynapseException;

	JSONObject putJSONObject(String endpoint, String uri, JSONObject entity,
			Map<String, String> headers) throws SynapseException;

	JSONObject postUri(String endpoint, String uri) throws SynapseException;

	JSONObject querySynapse(String endpoint, String query)
			throws SynapseException;

	void deleteUri(String endpoint, String uri) throws SynapseException;
	
	public <T extends JSONEntity> T getJSONEntity(String uri,
			Class<? extends T> clazz) throws SynapseException,
			JSONObjectAdapterException;

	String getSynapseTermsOfUse() throws SynapseException;

	Long getChildCount(String entityId) throws SynapseException;

	SynapseVersionInfo getVersionInfo() throws SynapseException,
			JSONObjectAdapterException;

	Set<String> getAllUserAndGroupIds() throws SynapseException;

	Activity getActivityForEntity(String entityId) throws SynapseException;

	Activity getActivityForEntityVersion(String entityId, Long versionNumber)
			throws SynapseException;

	Activity setActivityForEntity(String entityId, String activityId)
			throws SynapseException;

	void deleteGeneratedByForEntity(String entityId) throws SynapseException;

	Activity createActivity(Activity activity) throws SynapseException;

	Activity getActivity(String activityId) throws SynapseException;

	Activity putActivity(Activity activity) throws SynapseException;

	void deleteActivity(String activityId) throws SynapseException;

	PaginatedResults<Reference> getEntitiesGeneratedBy(String activityId,
			Integer limit, Integer offset) throws SynapseException;

	EntityIdList getDescendants(String nodeId, int pageSize,
			String lastDescIdExcl) throws SynapseException;

	EntityIdList getDescendants(String nodeId, int generation, int pageSize,
			String lastDescIdExcl) throws SynapseException;

	Evaluation createEvaluation(Evaluation eval) throws SynapseException;

	Evaluation getEvaluation(String evalId) throws SynapseException;

	PaginatedResults<Evaluation> getEvaluationByContentSource(String id,
			int offset, int limit) throws SynapseException;

	@Deprecated
	PaginatedResults<Evaluation> getEvaluationsPaginated(int offset, int limit)
			throws SynapseException;

	@Deprecated
	PaginatedResults<Evaluation> getAvailableEvaluationsPaginated(
			EvaluationStatus status, int offset, int limit)
			throws SynapseException;

	Long getEvaluationCount() throws SynapseException;

	Evaluation findEvaluation(String name) throws SynapseException,
			UnsupportedEncodingException;

	Evaluation updateEvaluation(Evaluation eval) throws SynapseException;

	void deleteEvaluation(String evalId) throws SynapseException;

	Participant createParticipant(String evalId) throws SynapseException;

	Participant getParticipant(String evalId, String principalId)
			throws SynapseException;

	void deleteParticipant(String evalId, String principalId)
			throws SynapseException;

	PaginatedResults<Participant> getAllParticipants(String s,
			long offset, long limit) throws SynapseException;

	Long getParticipantCount(String evalId) throws SynapseException;

	Submission createSubmission(Submission sub, String etag)
			throws SynapseException;

	Submission getSubmission(String subId) throws SynapseException;

	SubmissionStatus getSubmissionStatus(String subId) throws SynapseException;

	SubmissionStatus updateSubmissionStatus(SubmissionStatus status)
			throws SynapseException;

	void deleteSubmission(String subId) throws SynapseException;

	PaginatedResults<Submission> getAllSubmissions(String evalId, long offset,
			long limit) throws SynapseException;

	PaginatedResults<SubmissionStatus> getAllSubmissionStatuses(String evalId,
			long offset, long limit) throws SynapseException;

	PaginatedResults<SubmissionBundle> getAllSubmissionBundles(String evalId,
			long offset, long limit) throws SynapseException;

	PaginatedResults<Submission> getAllSubmissionsByStatus(String evalId,
			SubmissionStatusEnum status, long offset, long limit)
			throws SynapseException;

	PaginatedResults<SubmissionStatus> getAllSubmissionStatusesByStatus(
			String evalId, SubmissionStatusEnum status, long offset, long limit)
			throws SynapseException;

	PaginatedResults<SubmissionBundle> getAllSubmissionBundlesByStatus(
			String evalId, SubmissionStatusEnum status, long offset, long limit)
			throws SynapseException;

	PaginatedResults<Submission> getMySubmissions(String evalId, long offset,
			long limit) throws SynapseException;

	PaginatedResults<SubmissionBundle> getMySubmissionBundles(String evalId,
			long offset, long limit) throws SynapseException;

	URL getFileTemporaryUrlForSubmissionFileHandle(String submissionId,
			String fileHandleId) throws ClientProtocolException,
			MalformedURLException, IOException;

	Long getSubmissionCount(String evalId) throws SynapseException;

	UserEvaluationState getUserEvaluationState(String evalId)
			throws SynapseException;

	QueryTableResults queryEvaluation(String query) throws SynapseException;

	void moveToTrash(String entityId) throws SynapseException;

	void restoreFromTrash(String entityId, String newParentId)
			throws SynapseException;

	PaginatedResults<TrashedEntity> viewTrashForUser(long offset, long limit)
			throws SynapseException;

	void purgeTrashForUser(String entityId) throws SynapseException;

	void purgeTrashForUser() throws SynapseException;

	EntityHeader addFavorite(String entityId) throws SynapseException;

	void removeFavorite(String entityId) throws SynapseException;

	PaginatedResults<EntityHeader> getFavorites(Integer limit, Integer offset)
			throws SynapseException;

	void createEntityDoi(String entityId) throws SynapseException;

	void createEntityDoi(String entityId, Long entityVersion)
			throws SynapseException;

	Doi getEntityDoi(String entityId) throws SynapseException;

	Doi getEntityDoi(String s, Long entityVersion)
			throws SynapseException;

	List<EntityHeader> getEntityHeaderByMd5(String md5) throws SynapseException;

	String retrieveApiKey() throws SynapseException;

	AccessControlList updateEvaluationAcl(AccessControlList acl)
			throws SynapseException;

	AccessControlList getEvaluationAcl(String evalId) throws SynapseException;

	UserEvaluationPermissions getUserEvaluationPermissions(String evalId)
			throws SynapseException;
	
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

	
	

}
