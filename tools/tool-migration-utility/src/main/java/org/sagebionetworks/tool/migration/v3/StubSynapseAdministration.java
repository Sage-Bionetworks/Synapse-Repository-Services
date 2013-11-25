package org.sagebionetworks.tool.migration.v3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONObject;
import org.sagebionetworks.client.SharedClientConnection;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.evaluation.model.Evaluation;
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
import org.sagebionetworks.repo.model.OriginatingClient;
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
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.DaemonType;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
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
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.migration.IdList;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.model.migration.WikiMigrationResult;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.query.QueryTableResults;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
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
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import com.thoughtworks.xstream.XStream;

/**
 * Stub implementation of synapse for testing. All data for the stack is stored
 * within this object.
 * 
 * @author jmhill
 * 
 */
public class StubSynapseAdministration implements SynapseAdminClient {

	Stack<StackStatus> statusHistory;
	String endpoint;
	LinkedHashMap<MigrationType, List<RowMetadata>> metadata;
	BackupRestoreStatus status;
	long statusSequence = 0;
	
	Stack<Long> currentChangeNumberStack = new Stack<Long>();
	Long maxChangeNumber = 100l;
	List<Long> replayChangeNumbersHistory = new LinkedList<Long>();
	List<Set<Long>> deleteRequestsHistory = new LinkedList<Set<Long>>();
	Set<Long> exceptionNodes = new HashSet<Long>();
	List<WikiMigrationResult> wikiMigrationResults = new ArrayList<WikiMigrationResult>();

	/**
	 * Create a new stub
	 */
	public StubSynapseAdministration(String endpoint) {
		// Start with a status of read/write
		StackStatus status = new StackStatus();
		status.setCurrentMessage("Synapse is read for read/write");
		status.setStatus(StatusEnum.READ_WRITE);
		statusHistory = new Stack<StackStatus>();
		statusHistory.push(status);
		this.endpoint = endpoint;
	}

	
	/*
	 * Methods that are not part of the interface.
	 */

	/**
	 * Create a clone of a JSONEntity.
	 * 
	 * @param toClone
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends JSONEntity> T cloneJsonEntity(T toClone)
			throws JSONObjectAdapterException {
		if (toClone == null)
			throw new IllegalArgumentException("Clone cannot be null");
		// First go to JSON
		String json = EntityFactory.createJSONStringForEntity(toClone);
		return (T) EntityFactory.createEntityFromJSONString(json,
				toClone.getClass());
	}

	public Stack<Long> getCurrentChangeNumberStack() {
		return currentChangeNumberStack;
	}


	public void setCurrentChangeNumberStack(Stack<Long> currentChangeNumberStack) {
		this.currentChangeNumberStack = currentChangeNumberStack;
	}


	public Long getMaxChangeNumber() {
		return maxChangeNumber;
	}


	public void setMaxChangeNumber(Long maxChangeNumber) {
		this.maxChangeNumber = maxChangeNumber;
	}


	public List<Long> getReplayChangeNumbersHistory() {
		return replayChangeNumbersHistory;
	}


	/**
	 * Get the full history of status changes made to this stack.
	 * 
	 * @return
	 */
	public Stack<StackStatus> getStatusHistory() {
		return statusHistory;
	}

	/**
	 * The Map<MigrationType, List<RowMetadata>> used by this stub.
	 * 
	 * @return
	 */
	public Map<MigrationType, List<RowMetadata>> getMetadata() {
		return metadata;
	}

	/**
	 * Map<MigrationType, List<RowMetadata>> used by this stub.
	 * 
	 * @param metadata
	 */
	public void setMetadata(
			LinkedHashMap<MigrationType, List<RowMetadata>> metadata) {
		this.metadata = metadata;
	}
	
	public List<Set<Long>> getDeleteRequestsHistory() {
		return this.deleteRequestsHistory;
	}
	
	public Set<Long> getExceptionNodes() {
		return this.exceptionNodes;
	}
	
	public void setExceptionNodes(Set<Long> exceptionNodes) {
		this.exceptionNodes = exceptionNodes;
	}

	/*
	 * Methods that are part of the interface.
	 */

	@Override
	public StackStatus getCurrentStackStatus() throws SynapseException,
			JSONObjectAdapterException {
		return statusHistory.lastElement();
	}

	@Override
	public StackStatus updateCurrentStackStatus(StackStatus updated)
			throws JSONObjectAdapterException, SynapseException {
		if (updated == null)
			throw new IllegalArgumentException("StackStatus cannot be null");
		StackStatus status = cloneJsonEntity(updated);
		statusHistory.push(status);
		return status;
	}

	@Override
	public RowMetadataResult getRowMetadata(MigrationType migrationType,
			Long limit, Long offset) throws SynapseException,
			JSONObjectAdapterException {
		if (migrationType == null)
			throw new IllegalArgumentException("Type cannot be null");
		List<RowMetadata> list = this.metadata.get(migrationType);
		RowMetadataResult result = new RowMetadataResult();
		result.setTotalCount(new Long(list.size()));
		if (offset < list.size()) {
			long endIndex = Math.min(list.size(), offset + limit);
			List<RowMetadata> subList = list.subList(offset.intValue(),
					(int) endIndex);
			result.setList(subList);
		} else {
			result.setList(new LinkedList<RowMetadata>());
		}
		return result;
	}

	@Override
	public String getRepoEndpoint() {
		return this.endpoint;
	}

	@Override
	public MigrationTypeCounts getTypeCounts() throws SynapseException,
			JSONObjectAdapterException {
		// Get the counts for each type
		List<MigrationTypeCount> list = new LinkedList<MigrationTypeCount>();
		Iterator<Entry<MigrationType, List<RowMetadata>>> it = this.metadata
				.entrySet().iterator();
		while (it.hasNext()) {
			Entry<MigrationType, List<RowMetadata>> entry = it.next();
			MigrationType type = entry.getKey();
			List<RowMetadata> values = entry.getValue();
			MigrationTypeCount count = new MigrationTypeCount();
			count.setCount(new Long(values.size()));
			count.setType(type);
			list.add(count);
		}
		MigrationTypeCounts result = new MigrationTypeCounts();
		result.setList(list);
		return result;
	}

	@Override
	public MigrationTypeList getPrimaryTypes() throws SynapseException,
			JSONObjectAdapterException {
		// treat all types as primary
		List<MigrationType> list = new LinkedList<MigrationType>();
		Iterator<Entry<MigrationType, List<RowMetadata>>> it = this.metadata
				.entrySet().iterator();
		while (it.hasNext()) {
			Entry<MigrationType, List<RowMetadata>> entry = it.next();
			MigrationType type = entry.getKey();
			list.add(type);
		}
		MigrationTypeList result = new MigrationTypeList();
		result.setList(list);
		return result;
	}

	@Override
	public MigrationTypeCount deleteMigratableObject(
			MigrationType migrationType, IdList ids)
			throws JSONObjectAdapterException, SynapseException {
		// Get the type
		Set<Long> toDelete = new HashSet<Long>();
		toDelete.addAll(ids.getList());
		deleteRequestsHistory.add(toDelete);
		
		List<RowMetadata> newList = new LinkedList<RowMetadata>();
		List<RowMetadata> current = this.metadata.get(migrationType);
		long count = 0;
		for (RowMetadata row : current) {
			if (!toDelete.contains(row.getId())) {
				newList.add(row);
			} else {
				// Row should be deleted, should it raise an exception?
				if (exceptionNodes.contains(row.getId())) {
					throw new SynapseException("SynapseException on node " + row.getId());
				}
				count++;
			}
		}
		// Save the new list
		this.metadata.put(migrationType, newList);
		MigrationTypeCount mtc = new MigrationTypeCount();
		mtc.setCount(count);
		mtc.setType(migrationType);
		return mtc;
	}

	@Override
	public BackupRestoreStatus startBackup(MigrationType migrationType,
			IdList ids) throws JSONObjectAdapterException, SynapseException {
		// Create a tempFile that will contain the backup data.
		try {
			// Find the data in question and write it to a backup file
			Set<Long> toBackup = new HashSet<Long>();
			toBackup.addAll(ids.getList());
			List<RowMetadata> backupList = new LinkedList<RowMetadata>();
			List<RowMetadata> current = this.metadata.get(migrationType);
			for (RowMetadata row : current) {
				if (toBackup.contains(row.getId())) {
					backupList.add(row);
				}
			}
			File temp = writeBackupFile(backupList);
			status = new BackupRestoreStatus();
			status.setStatus(DaemonStatus.STARTED);
			status.setId(""+statusSequence++);
			status.setType(DaemonType.BACKUP);
			status.setBackupUrl(temp.getAbsolutePath().replace("\\", "/"));
			status.setProgresssCurrent(0l);
			status.setProgresssTotal(10l);
			return status;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Helper to write a backup file.
	 * @param backupList
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private File writeBackupFile(List<RowMetadata> backupList)
			throws IOException, FileNotFoundException {
		// write to a file
		File temp = File.createTempFile("tempBackupFile", ".tmp");
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(temp);
			XStream xstream = new XStream();
			xstream.toXML(backupList, out);
		} finally {
			if (out != null) {
				out.close();
			}
		}
		return temp;
	}

	@Override
	public BackupRestoreStatus startRestore(MigrationType migrationType, RestoreSubmission req) throws JSONObjectAdapterException,
			SynapseException {
		// First read the backup file
		List<RowMetadata> restoreList = readRestoreFile(req);
		// Build up the new list of values
		List<RowMetadata> current = this.metadata.get(migrationType);
		for(RowMetadata toAdd: restoreList){
			// Is this already in the list
			boolean updated = false;
			for(RowMetadata existing: current){
				if(toAdd.getId().equals(existing.getId())){
					existing.setEtag(toAdd.getEtag());
					updated = true;
				}
			}
			// If not updated then add it
			if(!updated){
				current.add(toAdd);
			}
		}
		// Sort it to put it back in its natural order
		Collections.sort(current, new Comparator<RowMetadata>() {
			@Override
			public int compare(RowMetadata one, RowMetadata two) {
				Long oneL = one.getId();
				Long twoL = two.getId();
				return oneL.compareTo(twoL);
			}
		});
		status = new BackupRestoreStatus();
		status.setStatus(DaemonStatus.STARTED);
		status.setType(DaemonType.RESTORE);
		status.setId(""+statusSequence++);
		status.setProgresssCurrent(0l);
		status.setProgresssTotal(10l);
		return status;

	}

	@Override
	public BackupRestoreStatus getStatus(String daemonId)
			throws JSONObjectAdapterException, SynapseException {
		// Change the status to finished
		status.setStatus(DaemonStatus.COMPLETED);
		status.setProgresssCurrent(9l);
		return status;
	}

	/**
	 * Read a restore file
	 * 
	 * @param req
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<RowMetadata> readRestoreFile(RestoreSubmission req) {
		try {
			File placeHolder = File.createTempFile("notUsed", ".tmp");
			req.getFileName();
			File temp = new File(placeHolder.getParentFile(), req.getFileName());
			if (!temp.exists())
				throw new RuntimeException("file does not exist: "
						+ temp.getAbsolutePath());
			// Read the temp file
			InputStream in = new FileInputStream(temp);
			try {
				XStream xstream = new XStream();
				return  (List<RowMetadata>) xstream.fromXML(in);
			} finally {
				in.close();
				placeHolder.delete();
				temp.delete();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public FireMessagesResult fireChangeMessages(Long startChangeNumber, Long limit) throws SynapseException, JSONObjectAdapterException {
		// Add this call to the history
		replayChangeNumbersHistory.add(startChangeNumber);
		FireMessagesResult result = new FireMessagesResult();
		long nextChangeNumber = -1;
		if(startChangeNumber + limit > maxChangeNumber){
			nextChangeNumber = -1;
		}else{
			nextChangeNumber = startChangeNumber + limit + 1;
		}
		result.setNextChangeNumber(nextChangeNumber);
		return result;
	}

	@Override
	public FireMessagesResult getCurrentChangeNumber() throws SynapseException,
			JSONObjectAdapterException {
		FireMessagesResult result = new FireMessagesResult();
		// Pop a number off of the stack
		result.setNextChangeNumber(currentChangeNumberStack.pop());
		return result;
	}


	@Override
	public void appendUserAgent(String toAppend) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setRepositoryEndpoint(String repoEndpoint) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setAuthEndpoint(String authEndpoint) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public String getAuthEndpoint() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setFileEndpoint(String fileEndpoint) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public String getFileEndpoint() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setSessionToken(String sessionToken) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public AttachmentData uploadAttachmentToSynapse(String entityId, File temp,
			String fileName) throws JSONObjectAdapterException,
			SynapseException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Entity getEntityById(String entityId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public <T extends Entity> T putEntity(T entity) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PresignedUrl waitForPreviewToBeCreated(String entityId,
			String tokenId, int maxTimeOut) throws SynapseException,
			JSONObjectAdapterException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PresignedUrl createAttachmentPresignedUrl(String entityId,
			String tokenId) throws SynapseException, JSONObjectAdapterException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public URL getWikiAttachmentPreviewTemporaryUrl(WikiPageKey properKey,
			String fileName) throws ClientProtocolException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public URL getWikiAttachmentTemporaryUrl(WikiPageKey properKey,
			String fileName) throws ClientProtocolException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Session login(String username, String password)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public UserSessionData getUserSessionData() throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean revalidateSession() throws SynapseException {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public String getCurrentSessionToken() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public <T extends Entity> T createEntity(T entity) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public JSONObject createJSONObject(String uri, JSONObject entity)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public SearchResults search(SearchQuery searchQuery)
			throws SynapseException, UnsupportedEncodingException,
			JSONObjectAdapterException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public URL getFileEntityPreviewTemporaryUrlForCurrentVersion(String entityId)
			throws ClientProtocolException, MalformedURLException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public URL getFileEntityTemporaryUrlForCurrentVersion(String entityId)
			throws ClientProtocolException, MalformedURLException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public URL getFileEntityPreviewTemporaryUrlForVersion(String entityId,
			Long versionNumber) throws ClientProtocolException,
			MalformedURLException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public URL getFileEntityTemporaryUrlForVersion(String entityId,
			Long versionNumber) throws ClientProtocolException,
			MalformedURLException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public S3FileHandle createFileHandle(File temp, String contentType)
			throws SynapseException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public WikiPage getWikiPage(WikiPageKey properKey)
			throws JSONObjectAdapterException, SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public VariableContentPaginatedResults<AccessRequirement> getAccessRequirements(
			RestrictableObjectDescriptor subjectId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public WikiPage updateWikiPage(String ownerId, ObjectType ownerType,
			WikiPage toUpdate) throws JSONObjectAdapterException,
			SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setRequestProfile(boolean request) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public JSONObject getProfileData() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String getUserName() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setUserName(String userName) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public String getApiKey() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setApiKey(String apiKey) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public <T extends Entity> T createEntity(T entity, String activityId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public <T extends JSONEntity> T createJSONEntity(String uri, T entity)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public EntityBundle createEntityBundle(EntityBundleCreate ebc)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public EntityBundle createEntityBundle(EntityBundleCreate ebc,
			String activityId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public EntityBundle updateEntityBundle(String entityId,
			EntityBundleCreate ebc) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public EntityBundle updateEntityBundle(String entityId,
			EntityBundleCreate ebc, String activityId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Entity getEntityByIdForVersion(String entityId, Long versionNumber)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public EntityBundle getEntityBundle(String entityId, int partsMask)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public EntityBundle getEntityBundle(String entityId, Long versionNumber,
			int partsMask) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<VersionInfo> getEntityVersions(String entityId,
			int offset, int limit) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public AccessControlList getACL(String entityId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public EntityHeader getEntityBenefactor(String entityId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public UserProfile getMyProfile() throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void updateMyProfile(UserProfile userProfile)
			throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public UserProfile getUserProfile(String ownerId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public UserGroupHeaderResponsePage getUserGroupHeadersByIds(List<String> ids)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(String prefix)
			throws SynapseException, UnsupportedEncodingException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public AccessControlList updateACL(AccessControlList acl)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public AccessControlList updateACL(AccessControlList acl, boolean recursive)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void deleteACL(String entityId) throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public AccessControlList createACL(AccessControlList acl)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<UserProfile> getUsers(int offset, int limit)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<UserGroup> getGroups(int offset, int limit)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean canAccess(String entityId, ACCESS_TYPE accessType)
			throws SynapseException {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean canAccess(String id, ObjectType type, ACCESS_TYPE accessType)
			throws SynapseException {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public UserEntityPermissions getUsersEntityPermissions(String entityId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Annotations getAnnotations(String entityId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Annotations updateAnnotations(String entityId, Annotations updated)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public <T extends AccessRequirement> T createAccessRequirement(T ar)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public ACTAccessRequirement createLockAccessRequirement(String entityId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public VariableContentPaginatedResults<AccessRequirement> getUnmetAccessRequirements(
			RestrictableObjectDescriptor subjectId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public <T extends AccessApproval> T createAccessApproval(T aa)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public JSONObject getEntity(String uri) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public <T extends JSONEntity> T getEntity(String entityId,
			Class<? extends T> clazz) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void deleteAccessRequirement(Long arId) throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public <T extends Entity> T putEntity(T entity, String activityId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public <T extends Entity> void deleteEntity(T entity)
			throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public <T extends Entity> void deleteAndPurgeEntity(T entity)
			throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void deleteEntityById(String entityId) throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void deleteAndPurgeEntityById(String entityId)
			throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public <T extends Entity> void deleteEntityVersion(T entity,
			Long versionNumber) throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void deleteEntityVersionById(String entityId, Long versionNumber)
			throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public EntityPath getEntityPath(Entity entity) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public EntityPath getEntityPath(String entityId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public BatchResults<EntityHeader> getEntityTypeBatch(List<String> entityIds)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public BatchResults<EntityHeader> getEntityHeaderBatch(
			List<Reference> references) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<EntityHeader> getEntityReferencedBy(Entity entity)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<EntityHeader> getEntityReferencedBy(
			String entityId, String targetVersion) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public JSONObject query(String query) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public FileHandleResults createFileHandles(List<File> files)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public ChunkedFileToken createChunkedFileUploadToken(
			CreateChunkedFileTokenRequest ccftr) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public URL createChunkedPresignedUrl(ChunkRequest chunkRequest)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String putFileToURL(URL url, File file, String contentType)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public ChunkResult addChunkToFile(ChunkRequest chunkRequest)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public S3FileHandle completeChunkFileUpload(
			CompleteChunkedFileRequest request) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public UploadDaemonStatus startUploadDeamon(CompleteAllChunksRequest cacr)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public UploadDaemonStatus getCompleteUploadDaemonStatus(String daemonId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public ExternalFileHandle createExternalFileHandle(ExternalFileHandle efh)
			throws JSONObjectAdapterException, SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public FileHandle getRawFileHandle(String fileHandleId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void deleteFileHandle(String fileHandleId) throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void clearPreview(String fileHandleId) throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public WikiPage createWikiPage(String ownerId, ObjectType ownerType,
			WikiPage toCreate) throws JSONObjectAdapterException,
			SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public WikiPage getRootWikiPage(String ownerId, ObjectType ownerType)
			throws JSONObjectAdapterException, SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public FileHandleResults getWikiAttachmenthHandles(WikiPageKey key)
			throws JSONObjectAdapterException, SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public File downloadWikiAttachment(WikiPageKey key, String fileName)
			throws ClientProtocolException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public File downloadWikiAttachmentPreview(WikiPageKey key, String fileName)
			throws ClientProtocolException, FileNotFoundException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void deleteWikiPage(WikiPageKey key) throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public PaginatedResults<WikiHeader> getWikiHeaderTree(String ownerId,
			ObjectType ownerType) throws SynapseException,
			JSONObjectAdapterException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public V2WikiPage createV2WikiPage(String ownerId, ObjectType ownerType,
			V2WikiPage toCreate) throws JSONObjectAdapterException,
			SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public V2WikiPage getV2WikiPage(WikiPageKey key)
			throws JSONObjectAdapterException, SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public V2WikiPage updateV2WikiPage(String ownerId, ObjectType ownerType,
			V2WikiPage toUpdate) throws JSONObjectAdapterException,
			SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public V2WikiPage restoreV2WikiPage(String ownerId, ObjectType ownerType,
			V2WikiPage toUpdate, Long versionToRestore)
			throws JSONObjectAdapterException, SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public V2WikiPage getV2RootWikiPage(String ownerId, ObjectType ownerType)
			throws JSONObjectAdapterException, SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public FileHandleResults getV2WikiAttachmentHandles(WikiPageKey key)
			throws JSONObjectAdapterException, SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public File downloadV2WikiAttachment(WikiPageKey key, String fileName)
			throws ClientProtocolException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public File downloadV2WikiAttachmentPreview(WikiPageKey key, String fileName)
			throws ClientProtocolException, FileNotFoundException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public URL getV2WikiAttachmentPreviewTemporaryUrl(WikiPageKey key,
			String fileName) throws ClientProtocolException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public URL getV2WikiAttachmentTemporaryUrl(WikiPageKey key, String fileName)
			throws ClientProtocolException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void deleteV2WikiPage(WikiPageKey key) throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public PaginatedResults<V2WikiHeader> getV2WikiHeaderTree(String ownerId,
			ObjectType ownerType) throws SynapseException,
			JSONObjectAdapterException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<V2WikiHistorySnapshot> getV2WikiHistory(
			WikiPageKey key, Long limit, Long offset) throws JSONObjectAdapterException,
			SynapseException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public FileHandleResults getEntityFileHandlesForCurrentVersion(
			String entityId) throws JSONObjectAdapterException,
			SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public FileHandleResults getEntityFileHandlesForVersion(String entityId,
			Long versionNumber) throws JSONObjectAdapterException,
			SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public File downloadLocationableFromSynapse(Locationable locationable)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public File downloadLocationableFromSynapse(Locationable locationable,
			File destinationFile) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public File downloadFromSynapse(LocationData location, String md5,
			File destinationFile) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public File downloadFromSynapse(String path, String md5,
			File destinationFile) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Locationable uploadLocationableToSynapse(Locationable locationable,
			File dataFile) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Locationable uploadLocationableToSynapse(Locationable locationable,
			File dataFile, String md5) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Locationable updateExternalLocationableToSynapse(
			Locationable locationable, String externalUrl)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Locationable updateExternalLocationableToSynapse(
			Locationable locationable, String externalUrl, String md5)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public AttachmentData uploadAttachmentToSynapse(String entityId,
			File dataFile) throws JSONObjectAdapterException, SynapseException,
			IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public AttachmentData uploadUserProfileAttachmentToSynapse(String userId,
			File dataFile, String fileName) throws JSONObjectAdapterException,
			SynapseException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public AttachmentData uploadAttachmentToSynapse(String id,
			AttachmentType attachmentType, File dataFile, String fileName)
			throws JSONObjectAdapterException, SynapseException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PresignedUrl createUserProfileAttachmentPresignedUrl(String id,
			String tokenOrPreviewId) throws SynapseException,
			JSONObjectAdapterException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PresignedUrl createAttachmentPresignedUrl(String id,
			AttachmentType attachmentType, String tokenOrPreviewId)
			throws SynapseException, JSONObjectAdapterException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PresignedUrl waitForUserProfilePreviewToBeCreated(String userId,
			String tokenOrPreviewId, int timeout) throws SynapseException,
			JSONObjectAdapterException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PresignedUrl waitForPreviewToBeCreated(String id,
			AttachmentType type, String tokenOrPreviewId, int timeout)
			throws SynapseException, JSONObjectAdapterException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void downloadEntityAttachment(String entityId,
			AttachmentData attachmentData, File destFile)
			throws SynapseException, JSONObjectAdapterException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void downloadUserProfileAttachment(String userId,
			AttachmentData attachmentData, File destFile)
			throws SynapseException, JSONObjectAdapterException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void downloadAttachment(String id, AttachmentType type,
			AttachmentData attachmentData, File destFile)
			throws SynapseException, JSONObjectAdapterException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void downloadEntityAttachmentPreview(String entityId,
			String previewId, File destFile) throws SynapseException,
			JSONObjectAdapterException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void downloadUserProfileAttachmentPreview(String userId,
			String previewId, File destFile) throws SynapseException,
			JSONObjectAdapterException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void downloadAttachmentPreview(String id, AttachmentType type,
			String previewId, File destFile) throws SynapseException,
			JSONObjectAdapterException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public S3AttachmentToken createAttachmentS3Token(String id,
			AttachmentType attachmentType, S3AttachmentToken token)
			throws JSONObjectAdapterException, SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String getSynapseTermsOfUse() throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public MessageToUser sendMessage(MessageToUser message) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<MessageBundle> getInbox(
			List<MessageStatusType> inboxFilter, MessageSortBy orderBy,
			Boolean descending, long limit, long offset) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<MessageToUser> getOutbox(MessageSortBy orderBy,
			Boolean descending, long limit, long offset) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public MessageToUser getMessage(String messageId) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public MessageToUser forwardMessage(String messageId,
			MessageRecipientSet recipients) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<MessageToUser> getConversation(
			String associatedMessageId, MessageSortBy orderBy,
			Boolean descending, long limit, long offset) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void updateMessageStatus(MessageStatus status) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void deleteMessage(String messageId) throws SynapseException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Long getChildCount(String entityId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public SynapseVersionInfo getVersionInfo() throws SynapseException,
			JSONObjectAdapterException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Set<String> getAllUserAndGroupIds() throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Activity getActivityForEntity(String entityId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Activity getActivityForEntityVersion(String entityId,
			Long versionNumber) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Activity setActivityForEntity(String entityId, String activityId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void deleteGeneratedByForEntity(String entityId)
			throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Activity createActivity(Activity activity) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Activity getActivity(String activityId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Activity putActivity(Activity activity) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void deleteActivity(String activityId) throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public PaginatedResults<Reference> getEntitiesGeneratedBy(
			String activityId, Integer limit, Integer offset)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public EntityIdList getDescendants(String nodeId, int pageSize,
			String lastDescIdExcl) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public EntityIdList getDescendants(String nodeId, int generation,
			int pageSize, String lastDescIdExcl) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Evaluation createEvaluation(Evaluation eval) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Evaluation getEvaluation(String evalId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<Evaluation> getEvaluationByContentSource(
			String projectId, int offset, int limit) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<Evaluation> getEvaluationsPaginated(int offset,
			int limit) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Long getEvaluationCount() throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Evaluation findEvaluation(String name) throws SynapseException,
			UnsupportedEncodingException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Evaluation updateEvaluation(Evaluation eval) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void deleteEvaluation(String evalId) throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Participant createParticipant(String evalId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Participant getParticipant(String evalId, String principalId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void deleteParticipant(String evalId, String principalId)
			throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public PaginatedResults<Participant> getAllParticipants(String s,
			long offset, long limit) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Long getParticipantCount(String evalId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Submission createSubmission(Submission sub, String etag)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Submission getSubmission(String subId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public SubmissionStatus getSubmissionStatus(String subId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public SubmissionStatus updateSubmissionStatus(SubmissionStatus status)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void deleteSubmission(String subId) throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public PaginatedResults<Submission> getAllSubmissions(String evalId,
			long offset, long limit) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<SubmissionStatus> getAllSubmissionStatuses(
			String evalId, long offset, long limit) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundles(
			String evalId, long offset, long limit) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<Submission> getAllSubmissionsByStatus(
			String evalId, SubmissionStatusEnum status, long offset, long limit)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<SubmissionStatus> getAllSubmissionStatusesByStatus(
			String evalId, SubmissionStatusEnum status, long offset, long limit)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundlesByStatus(
			String evalId, SubmissionStatusEnum status, long offset, long limit)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<Submission> getMySubmissions(String evalId,
			long offset, long limit) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<SubmissionBundle> getMySubmissionBundles(
			String evalId, long offset, long limit) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public URL getFileTemporaryUrlForSubmissionFileHandle(String submissionId,
			String fileHandleId) throws ClientProtocolException,
			MalformedURLException, IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Long getSubmissionCount(String evalId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public UserEvaluationState getUserEvaluationState(String evalId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public QueryTableResults queryEvaluation(String query)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public StorageUsageSummaryList getStorageUsageSummary(
			List<StorageUsageDimension> aggregation) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void moveToTrash(String entityId) throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void restoreFromTrash(String entityId, String newParentId)
			throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public PaginatedResults<TrashedEntity> viewTrashForUser(long offset,
			long limit) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void purgeTrashForUser(String entityId) throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void purgeTrashForUser() throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public EntityHeader addFavorite(String entityId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void removeFavorite(String entityId) throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public PaginatedResults<EntityHeader> getFavorites(Integer limit,
			Integer offset) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void createEntityDoi(String entityId) throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void createEntityDoi(String entityId, Long entityVersion)
			throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Doi getEntityDoi(String entityId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Doi getEntityDoi(String s, Long entityVersion)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public List<EntityHeader> getEntityHeaderByMd5(String md5)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String retrieveApiKey() throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public AccessControlList updateEvaluationAcl(AccessControlList acl)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public AccessControlList getEvaluationAcl(String evalId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public UserEvaluationPermissions getUserEvaluationPermissions(String evalId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public ColumnModel createColumnModel(ColumnModel model)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public RowReferenceSet appendRowsToTable(RowSet toAppend) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public ColumnModel getColumnModel(String columnId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Team createTeam(Team team) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Team getTeam(String id) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<Team> getTeams(String fragment, long limit,
			long offset) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<Team> getTeamsForUser(String memberId, long limit,
			long offset) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public URL getTeamIcon(String teamId, Boolean redirect)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Team updateTeam(Team team) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void deleteTeam(String teamId) throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void addTeamMember(String teamId, String memberId)
			throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public PaginatedResults<TeamMember> getTeamMembers(String teamId, String fragment,
			long limit, long offset) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void removeTeamMember(String teamId, String memberId)
			throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public MembershipInvtnSubmission createMembershipInvitation(
			MembershipInvtnSubmission invitation) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public MembershipInvtnSubmission getMembershipInvitation(String invitationId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<MembershipInvitation> getOpenMembershipInvitations(
			String memberId, String teamId, long limit, long offset)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void deleteMembershipInvitation(String invitationId)
			throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public MembershipRqstSubmission createMembershipRequest(
			MembershipRqstSubmission request) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public MembershipRqstSubmission getMembershipRequest(String requestId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<MembershipRequest> getOpenMembershipRequests(
			String teamId, String requestorId, long limit, long offset)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void deleteMembershipRequest(String requestId)
			throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setTeamMemberPermissions(String teamId, String memberId,
			boolean isAdmin) throws SynapseException {
		// TODO Auto-generated method stub
	}

	public List<ColumnModel> getColumnModelsForTableEntity(String tableEntityId)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public TeamMembershipStatus getTeamMembershipStatus(String teamId,
			String principalId) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public PaginatedColumnModels listColumnModels(String prefix, Long limit,
			Long offset) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateTeamSearchCache() throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void logout() throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void invalidateApiKey() throws SynapseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void createUser(NewUser user) throws SynapseException {
		// TODO Auto-generated method stub
	}

	@Override
	public void createUser(NewUser user, OriginatingClient originClient) throws SynapseException {
		// TODO Auto-generated method stub
	}


	@Override
	public void changePassword(String sessionToken, String newPassword)
			throws SynapseException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void signTermsOfUse(String sessionToken, boolean acceptTerms)
			throws SynapseException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendPasswordResetEmail(String email) throws SynapseException {
		// TODO Auto-generated method stub
	}

	@Override
	public void sendPasswordResetEmail(String email, OriginatingClient originClient) throws SynapseException {
		// TODO Auto-generated method stub
	}

	@Override
	public Session passThroughOpenIDParameters(String queryString, Boolean acceptsTermsOfUse)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PaginatedResults<Evaluation> getAvailableEvaluationsPaginated(
			int offset, int limit) throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Session passThroughOpenIDParameters(String queryString)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PaginatedResults<WikiMigrationResult> migrateWikisToV2(long offset,
			long limit) throws SynapseException, JSONObjectAdapterException {
		// Return the requested migration results for processing
		PaginatedResults<WikiMigrationResult> results = new PaginatedResults<WikiMigrationResult>();
		List<WikiMigrationResult> subResults = createSubResultAtOffset(offset, limit);
		results.setResults(subResults);
		results.setTotalNumberOfResults(wikiMigrationResults.size());
		return results;
	}

	/**
	 * Returns a subset of all the migration results, specified by the
	 * offset and limit parameters.
	 * 
	 * @param offset
	 * @param limit
	 * @return
	 */
	private List<WikiMigrationResult> createSubResultAtOffset(long offset, long limit) {
		List<WikiMigrationResult> subResults = new ArrayList<WikiMigrationResult>();
		long counter = offset;
		while(counter < offset + limit && counter < wikiMigrationResults.size()) {
			subResults.add(wikiMigrationResults.get((int) counter));
			counter++;
		}
		return subResults;
	}
	
	/**
	 * Keep track of all the migration results.
	 * @param results
	 */
	public void setWikiMigrationResults(List<WikiMigrationResult> results) {
		wikiMigrationResults = results;
	}

	@Override
	public SharedClientConnection getSharedClientConnection() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Session passThroughOpenIDParameters(String queryString,
			Boolean createUserIfNecessary, OriginatingClient originClient)
			throws SynapseException {
		// TODO Auto-generated method stub
		return null;
	}
}
