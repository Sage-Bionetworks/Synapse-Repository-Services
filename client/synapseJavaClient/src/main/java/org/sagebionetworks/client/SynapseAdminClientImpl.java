package org.sagebionetworks.client;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.List;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * Java Client API for Synapse Administrative REST APIs
 */
public class SynapseAdminClientImpl extends SynapseClientImpl implements SynapseAdminClient {

	private static final String DAEMON = ADMIN + "/daemon";
	private static final String ADMIN_TRASHCAN_VIEW = ADMIN + "/trashcan/view";
	private static final String ADMIN_TRASHCAN_PURGE = ADMIN + "/trashcan/purge";
	private static final String ADMIN_CHANGE_MESSAGES = ADMIN + "/messages";
	private static final String ADMIN_FIRE_MESSAGES = ADMIN + "/messages/refire";
	private static final String ADMIN_GET_CURRENT_CHANGE_NUM = ADMIN + "/messages/currentnumber";
	private static final String ADMIN_PUBLISH_MESSAGES = ADMIN_CHANGE_MESSAGES + "/rebroadcast";
	private static final String ADMIN_DOI_CLEAR = ADMIN + "/doi/clear";
	private static final String ADMIN_WAIT = ADMIN + "/wait";

	private static final String MIGRATION = "/migration";
	private static final String MIGRATION_COUNTS = MIGRATION + "/counts";
	private static final String MIGRATION_COUNT = MIGRATION + "/count";
	private static final String MIGRATION_ROWS = MIGRATION + "/rows";
	private static final String MIGRATION_ROWS_BY_RANGE = MIGRATION + "/rowsbyrange";
	private static final String MIGRATION_DELTA = MIGRATION + "/delta";
	private static final String MIGRATION_BACKUP = MIGRATION + "/backup";
	private static final String MIGRATION_RESTORE = MIGRATION + "/restore";
	private static final String MIGRATION_DELETE = MIGRATION + "/delete";
	private static final String MIGRATION_STATUS = MIGRATION + "/status";
	private static final String MIGRATION_PRIMARY = MIGRATION + "/primarytypes";
	private static final String MIGRATION_TYPES = MIGRATION + "/types";
	private static final String MIGRATION_RANGE_CHECKSUM = MIGRATION + "/rangechecksum";
	private static final String MIGRATION_TYPE_CHECKSUM = MIGRATION + "/typechecksum";

	private static final String ADMIN_DYNAMO_CLEAR = ADMIN + "/dynamo/clear";
	private static final String ADMIN_MIGRATE_WIKIS_TO_V2 = ADMIN + "/migrateWiki";
	
	private static final String ADMIN_USER = ADMIN + "/user";
	private static final String ADMIN_CLEAR_LOCKS = ADMIN+"/locks";
	private static final String ADMIN_CREATE_OR_UPDATE_CHANGE_MESSAGES = ADMIN+"/messages/createOrUpdate";

	public SynapseAdminClientImpl() {
		super();
	}
	
	public SynapseAdminClientImpl(HttpClientProvider clientProvider) {
		super(new SharedClientConnection(clientProvider));
	}
	
	/**
	 * @param updated
	 * @return status
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public StackStatus updateCurrentStackStatus(StackStatus updated)
			throws JSONObjectAdapterException, SynapseException {
		JSONObject jsonObject = EntityFactory
				.createJSONObjectForEntity(updated);
		jsonObject = getSharedClientConnection().putJson(repoEndpoint, STACK_STATUS, jsonObject.toString(), getUserAgent());
		return EntityFactory.createEntityFromJSONObject(jsonObject,
				StackStatus.class);
	}

	@Override
	public BackupRestoreStatus getDaemonStatus(String daemonId)
			throws SynapseException, JSONObjectAdapterException {
		return getJSONEntity(DAEMON + "/" + daemonId, BackupRestoreStatus.class);
	}

	@Override
	public PaginatedResults<TrashedEntity> viewTrash(long offset, long limit) throws SynapseException {
		String url = ADMIN_TRASHCAN_VIEW + "?" + OFFSET + "=" + offset + "&" + LIMIT + "=" + limit;
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, url, getUserAgent());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		PaginatedResults<TrashedEntity> results = new PaginatedResults<TrashedEntity>(TrashedEntity.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public void purgeTrash() throws SynapseException {
		getSharedClientConnection().putJson(repoEndpoint, ADMIN_TRASHCAN_PURGE, null, getUserAgent());
	}
	
	@Override
	public ChangeMessages listMessages(Long startChangeNumber, ObjectType type, Long limit) throws SynapseException, JSONObjectAdapterException{
		// Build up the URL
		String url = buildListMessagesURL(startChangeNumber, type, limit);
		return getJSONEntity(url, ChangeMessages.class);
	}
	
	// New migration client methods
	
	public MigrationTypeList getPrimaryTypes() throws SynapseException, JSONObjectAdapterException {
		String uri = MIGRATION_PRIMARY;
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		MigrationTypeList mtl = new MigrationTypeList();
		mtl.initializeFromJSONObject(adapter);
		return mtl;
	}
	
	public MigrationTypeList getMigrationTypes() throws SynapseException, JSONObjectAdapterException {
		String uri = MIGRATION_TYPES;
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		MigrationTypeList mtl = new MigrationTypeList();
		mtl.initializeFromJSONObject(adapter);
		return mtl;
	}
	
	public MigrationTypeCounts getTypeCounts() throws SynapseException, JSONObjectAdapterException {
		String uri = MIGRATION_COUNTS;
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		MigrationTypeCounts mtc = new MigrationTypeCounts();
		mtc.initializeFromJSONObject(adapter);
		return mtc;
	}
	
	public MigrationTypeCount getTypeCount(MigrationType type) throws SynapseException, JSONObjectAdapterException {
		String uri = MIGRATION_COUNT + "?type=" + type.name() ;
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		MigrationTypeCount mtc = new MigrationTypeCount();
		mtc.initializeFromJSONObject(adapter);
		return mtc;
	}
	
	public RowMetadataResult getRowMetadata(MigrationType migrationType, Long limit, Long offset) throws SynapseException, JSONObjectAdapterException {
		String uri = MIGRATION_ROWS + "?type=" + migrationType.name() + "&limit=" + limit + "&offset=" + offset;
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		RowMetadataResult results = new RowMetadataResult(); 
		results.initializeFromJSONObject(adapter);
		return results;
	}
	
	public RowMetadataResult getRowMetadataByRange(MigrationType migrationType, Long minId, Long maxId, Long limit, Long offset) throws SynapseException, JSONObjectAdapterException {
		String uri = MIGRATION_ROWS_BY_RANGE + "?type=" + migrationType.name() + "&minId=" + minId + "&maxId=" + maxId + "&limit=" + limit + "&offset=" + offset;
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		RowMetadataResult results = new RowMetadataResult(); 
		results.initializeFromJSONObject(adapter);
		return results;
	}
	
	public RowMetadataResult getRowMetadataDelta(MigrationType migrationType, IdList ids) throws JSONObjectAdapterException, SynapseException {
		String uri = MIGRATION_DELTA + "?type=" + migrationType.name();
		String jsonStr = EntityFactory.createJSONStringForEntity(ids);
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		RowMetadataResult result = new RowMetadataResult();
		result.initializeFromJSONObject(adapter);
		return result;
	}
	
	public BackupRestoreStatus startBackup(MigrationType migrationType, IdList ids) throws JSONObjectAdapterException, SynapseException {
		String uri = MIGRATION_BACKUP + "?type=" + migrationType.name();
		String jsonStr = EntityFactory.createJSONStringForEntity(ids);
		JSONObject jsonObj = getSharedClientConnection().postJson(repoEndpoint, uri, jsonStr, getUserAgent(), null);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		BackupRestoreStatus brStatus = new BackupRestoreStatus();
		brStatus.initializeFromJSONObject(adapter);
		return brStatus;
	}
	
	public BackupRestoreStatus startRestore(MigrationType migrationType, RestoreSubmission req) throws JSONObjectAdapterException, SynapseException {
		String uri = MIGRATION_RESTORE + "?type=" + migrationType.name();
		String jsonStr = EntityFactory.createJSONStringForEntity(req);
		JSONObject jsonObj = getSharedClientConnection().postJson(repoEndpoint, uri, jsonStr, getUserAgent(), null);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		BackupRestoreStatus brStatus = new BackupRestoreStatus();
		brStatus.initializeFromJSONObject(adapter);
		return brStatus;
	}
	
	public BackupRestoreStatus getStatus(String daemonId) throws JSONObjectAdapterException, SynapseException {
		String uri = MIGRATION_STATUS + "?daemonId=" + daemonId;
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		BackupRestoreStatus brStatus = new BackupRestoreStatus();
		brStatus.initializeFromJSONObject(adapter);
		return brStatus;
	}
	
	public MigrationTypeCount deleteMigratableObject(MigrationType migrationType, IdList ids) throws JSONObjectAdapterException, SynapseException {
		String uri = MIGRATION_DELETE + "?type=" + migrationType.name();
		String jsonStr = EntityFactory.createJSONStringForEntity(ids);
		JSONObject jsonObj = getSharedClientConnection().putJson(repoEndpoint, uri, jsonStr, getUserAgent());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		MigrationTypeCount mtc = new MigrationTypeCount();
		mtc.initializeFromJSONObject(adapter);
		return mtc;
	}

	@Override
	public MigrationRangeChecksum getChecksumForIdRange(MigrationType migrationType, String salt, Long minId, Long maxId) throws SynapseException, JSONObjectAdapterException {
		if (migrationType == null || minId == null || maxId == null) {
			throw new IllegalArgumentException("Arguments type, minId and maxId cannot be null");
		}
		String uri = MIGRATION_RANGE_CHECKSUM + "?migrationType=" + migrationType.name() + "&salt=" + salt + "&minId=" + minId + "&maxId=" + maxId;
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		MigrationRangeChecksum mrc = new MigrationRangeChecksum();
		mrc.initializeFromJSONObject(adapter);
		return mrc;
	}
	
	@Override
	public MigrationTypeChecksum getChecksumForType(MigrationType migrationType) throws SynapseException, JSONObjectAdapterException {
		if (migrationType == null) {
			throw new IllegalArgumentException("Arguments type cannot be null");
		}
		String uri = MIGRATION_TYPE_CHECKSUM + "?migrationType=" + migrationType.name();
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		MigrationTypeChecksum mtc = new MigrationTypeChecksum();
		mtc.initializeFromJSONObject(adapter);
		return mtc;
	}
	
	@Override
	public FireMessagesResult fireChangeMessages(Long startChangeNumber, Long limit) throws SynapseException, JSONObjectAdapterException {
		if(startChangeNumber == null) throw new IllegalArgumentException("startChangeNumber cannot be null");
		String uri = ADMIN_FIRE_MESSAGES + "?startChangeNumber=" + startChangeNumber;
		if (limit != null){
			uri = uri + "&limit=" + limit;
		}
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		FireMessagesResult res = new FireMessagesResult();
		res.initializeFromJSONObject(adapter);
		return res;
	}
	
	/**
	 * Return current change message number
	 */
	@Override
	public FireMessagesResult getCurrentChangeNumber() throws SynapseException, JSONObjectAdapterException {
		String uri = ADMIN_GET_CURRENT_CHANGE_NUM;
		JSONObject jsonObj = getSharedClientConnection().getJson(repoEndpoint, uri, getUserAgent());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		FireMessagesResult res = new FireMessagesResult();
		res.initializeFromJSONObject(adapter);
		return res;
	}

	/**
	 * Build up the URL for the list change message call.
	 * @param startChangeNumber
	 * @param type
	 * @param limit
	 * @return
	 */
	static String buildListMessagesURL(Long startChangeNumber, ObjectType type, Long limit){
		if(startChangeNumber == null) throw new IllegalArgumentException("startChangeNumber cannot be null");
		StringBuilder builder = new StringBuilder();
		builder.append(ADMIN_CHANGE_MESSAGES);
		builder.append("?");
		builder.append("startChangeNumber=").append(startChangeNumber);
		if(type != null){
			builder.append("&type=").append(type.name());
		}
		if(limit != null){
			builder.append("&limit=").append(limit);
		}
		return builder.toString();
	}
	
	@Override
	public PublishResults publishChangeMessages(String queueName, Long startChangeNumber, ObjectType type, Long limit) throws SynapseException, JSONObjectAdapterException{
		// Build up the URL
		String url = buildPublishMessagesURL(queueName, startChangeNumber, type, limit);
		JSONObject json = getSharedClientConnection().postJson(repoEndpoint, url, null, getUserAgent(), null);
		return EntityFactory.createEntityFromJSONObject(json, PublishResults.class);
	}
	
	/**
	 * Build up the URL for publishing messages.
	 * @param startChangeNumber
	 * @param type
	 * @param limit
	 * @return
	 */
	static String buildPublishMessagesURL(String queueName, Long startChangeNumber, ObjectType type, Long limit){
		if(queueName == null) throw new IllegalArgumentException("queueName cannot be null");
		if(startChangeNumber == null) throw new IllegalArgumentException("startChangeNumber cannot be null");
		StringBuilder builder = new StringBuilder();
		builder.append(ADMIN_PUBLISH_MESSAGES);
		builder.append("?");
		builder.append("queueName=").append(queueName);
		builder.append("&startChangeNumber=").append(startChangeNumber);
		if(type != null){
			builder.append("&type=").append(type.name());
		}
		if(limit != null){
			builder.append("&limit=").append(limit);
		}
		return builder.toString();
	}

	@Override
	public void clearDoi() throws SynapseException {
		getSharedClientConnection().deleteUri(repoEndpoint, ADMIN_DOI_CLEAR, getUserAgent());
	}

	@Override
	public void clearDynamoTable(String tableName, String hashKeyName, String rangeKeyName)
			throws SynapseException {

		if (tableName == null || tableName.isEmpty()) {
			throw new IllegalArgumentException("Table name cannot be null or empty.");
		}
		if (hashKeyName == null || hashKeyName.isEmpty()) {
			throw new IllegalArgumentException("Hash key name cannot be null or empty.");
		}
		if (rangeKeyName == null || rangeKeyName.isEmpty()) {
			throw new IllegalArgumentException("Range key name cannot be null or empty.");
		}

		try {
			String uri = ADMIN_DYNAMO_CLEAR + "/" + URLEncoder.encode(tableName, "UTF-8");
			uri += "?hashKeyName=" + URLEncoder.encode(hashKeyName, "UTF-8");
			uri += "&rangeKeyName=" + URLEncoder.encode(rangeKeyName, "UTF-8");
			getSharedClientConnection().deleteUri(repoEndpoint, uri, getUserAgent());
		} catch (UnsupportedEncodingException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public long createUser(NewIntegrationTestUser user) throws SynapseException, JSONObjectAdapterException {
		if(user.getEmail() == null) throw new IllegalArgumentException("New users must have an email");
		if(user.getUsername() == null) throw new IllegalArgumentException("New users must have a username");
		JSONObject json = getSharedClientConnection().postJson(repoEndpoint, ADMIN_USER,
				EntityFactory.createJSONStringForEntity(user), getUserAgent(), null);
		
		EntityId id = EntityFactory.createEntityFromJSONObject(json, EntityId.class);
		return Long.parseLong(id.getId());
	}


	@Override
	public void deleteUser(Long id) throws SynapseException, JSONObjectAdapterException {
		String url = ADMIN_USER + "/" + id; 
		getSharedClientConnection().deleteUri(repoEndpoint, url, getUserAgent());
	}

	@Override
	public void rebuildTableCacheAndIndex(String tableId) throws SynapseException, JSONObjectAdapterException {
		String url = ADMIN + "/entity/" + tableId + "/table/rebuild";
		getSharedClientConnection().getJson(repoEndpoint, url, getUserAgent(), null);
	}

	@Override
	public void clearAllLocks() throws SynapseException{
		getSharedClientConnection().deleteUri(repoEndpoint, ADMIN_CLEAR_LOCKS, getUserAgent());
	}

	@Override
	public void waitForTesting(boolean release) throws SynapseException {
		try {
			URIBuilder uri = new URIBuilder(ADMIN_WAIT).setParameter("release", Boolean.toString(release));
			getSharedClientConnection().getJson(repoEndpoint, uri.build().toString(), getUserAgent());
		} catch (URISyntaxException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public ChangeMessages createOrUpdateChangeMessages(ChangeMessages batch)
			throws SynapseException {
		try {
			return doCreateJSONEntity(ADMIN_CREATE_OR_UPDATE_CHANGE_MESSAGES, batch);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public int throwException(String exceptionClassName, boolean inTransaction, boolean inBeforeCommit) throws SynapseException {
		String url = ADMIN + "/exception?exception=" + exceptionClassName + "&inTransaction=" + inTransaction + "&inBeforeCommit="
				+ inBeforeCommit;
		try {
			getSharedClientConnection().getJson(repoEndpoint, url, getUserAgent(), new SharedClientConnection.ErrorHandler() {
				@Override
				public void handleError(int code, String responseBody) throws SynapseException {
					if (code >= 200 && code < 300) {
						// client code handles these as non-errors
						throw new SynapseServerException(code);
					}
				}
			});
			return -1;
		} catch (SynapseServerException e) {
			return e.getStatusCode();
		}
	}

}
