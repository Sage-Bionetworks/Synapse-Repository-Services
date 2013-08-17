package org.sagebionetworks.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.migration.IdList;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * Java Client API for Synapse Administrative REST APIs
 */
public class SynapseAdministration extends Synapse implements SynapseAdministrationInt {

	public static final String DAEMON = ADMIN + "/daemon";
	public static final String BACKUP = "/backup";
	public static final String RESTORE = "/restore";
//	public static final String SEARCH_DOCUMENT = "/searchDocument";
	public static final String DAEMON_BACKUP = DAEMON + BACKUP;
	public static final String DAEMON_RESTORE = DAEMON + RESTORE;
	public static final String GET_ALL_BACKUP_OBJECTS = "/backupObjects";
	public static final String INCLUDE_DEPENDENCIES_PARAM = "includeDependencies";
	public static final String GET_ALL_BACKUP_COUNTS = "/backupObjectsCounts";
	private static final String ADMIN_TRASHCAN_VIEW = ADMIN + "/trashcan/view";
	private static final String ADMIN_TRASHCAN_PURGE = ADMIN + "/trashcan/purge";
	private static final String ADMIN_CHANGE_MESSAGES = ADMIN + "/messages";
	private static final String ADMIN_FIRE_MESSAGES = ADMIN + "/messages/refire";
	private static final String ADMIN_GET_CURRENT_CHANGE_NUM = ADMIN + "/messages/currentnumber";
	private static final String ADMIN_PUBLISH_MESSAGES = ADMIN_CHANGE_MESSAGES+"/rebroadcast";
	private static final String ADMIN_DOI_CLEAR = ADMIN + "/doi/clear";
	
	public static final String MIGRATION = "/migration";
	public static final String MIGRATION_COUNTS = MIGRATION+"/counts";
	public static final String MIGRATION_MAX_IDS = MIGRATION+"/maxids";
	public static final String MIGRATION_ROWS = MIGRATION+"/rows";
	public static final String MIGRATION_DELTA = MIGRATION+"/delta";
	public static final String MIGRATION_BACKUP = MIGRATION+"/backup";
	public static final String MIGRATION_RESTORE = MIGRATION+"/restore";
	public static final String MIGRATION_DELETE = MIGRATION+"/delete";
	public static final String MIGRATION_STATUS = MIGRATION+"/status";
	public static final String MIGRATION_PRIMARY = MIGRATION+"/primarytypes";

	public static final String ADMIN_DYNAMO_CLEAR = ADMIN + "/dynamo/clear";

	public SynapseAdministration() {
		super();
	}
	
	public SynapseAdministration(HttpClientProvider clientProvider, DataUploader dataUploader) {
		super(clientProvider, dataUploader);
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
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultPOSTPUTHeaders);
		jsonObject = dispatchSynapseRequest(repoEndpoint, STACK_STATUS, "PUT",
				jsonObject.toString(), requestHeaders);
		return EntityFactory.createEntityFromJSONObject(jsonObject,
				StackStatus.class);
	}

	/**
	 * @param daemonId
	 * @return the status
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public BackupRestoreStatus getDaemonStatus(String daemonId)
			throws SynapseException, JSONObjectAdapterException {
		return getJSONEntity(DAEMON + "/" + daemonId, BackupRestoreStatus.class);
	}

	/**
	 * Gets everything in the trash can.
	 */
	public PaginatedResults<TrashedEntity> viewTrash(long offset, long limit) throws SynapseException {
		String url = ADMIN_TRASHCAN_VIEW + "?" + OFFSET + "=" + offset + "&" + LIMIT + "=" + limit;
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
	 * Purges everything in the trash can. All the entities in the trash will be permanently deleted.
	 */
	public void purgeTrash() throws SynapseException {
		signAndDispatchSynapseRequest(repoEndpoint, ADMIN_TRASHCAN_PURGE, "PUT", null, defaultPOSTPUTHeaders);
	}
	
	/**
	 * List change messages.
	 * @param startChangeNumber - The change number to start from.
	 * @param type - (optional) when included, only messages of this type will be listed.
	 * @param limit - (optional) limit the number of messages to fetch.
	 * @return
	 * @throws JSONObjectAdapterException 
	 * @throws SynapseException 
	 */
	public ChangeMessages listMessages(Long startChangeNumber, ObjectType type, Long limit) throws SynapseException, JSONObjectAdapterException{
		// Build up the URL
		String url = buildListMessagesURL(startChangeNumber, type, limit);
		return getJSONEntity(url, ChangeMessages.class);
	}
	
	// New migration client methods
	
	/*
	 * 
	 */
	public MigrationTypeList getPrimaryTypes() throws SynapseException, JSONObjectAdapterException {
		String uri = MIGRATION_PRIMARY;
		JSONObject jsonObj = signAndDispatchSynapseRequest(repoEndpoint, uri, "GET", null, defaultGETDELETEHeaders);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		MigrationTypeList mtl = new MigrationTypeList();
		mtl.initializeFromJSONObject(adapter);
		return mtl;
	}
	
	/*
	 * 
	 * 
	 */
	public MigrationTypeCounts getTypeCounts() throws SynapseException, JSONObjectAdapterException {
		String uri = MIGRATION_COUNTS;
		JSONObject jsonObj = signAndDispatchSynapseRequest(repoEndpoint, uri, "GET", null, defaultGETDELETEHeaders);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		MigrationTypeCounts mtc = new MigrationTypeCounts();
		mtc.initializeFromJSONObject(adapter);
		return mtc;
	}
	
	/*
	 * 
	 */
	public RowMetadataResult getRowMetadata(MigrationType migrationType, Long limit, Long offset) throws SynapseException, JSONObjectAdapterException {
		String uri = MIGRATION_ROWS + "?type=" + migrationType.name() + "&limit=" + limit + "&offset=" + offset;
		JSONObject jsonObj = signAndDispatchSynapseRequest(repoEndpoint, uri, "GET", null, defaultGETDELETEHeaders);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		RowMetadataResult results = new RowMetadataResult(); 
		results.initializeFromJSONObject(adapter);
		return results;
	}
	
	/*
	 * 
	 */
	public RowMetadataResult getRowMetadataDelta(MigrationType migrationType, IdList ids) throws JSONObjectAdapterException, SynapseException {
		String uri = MIGRATION_DELTA + "?type=" + migrationType.name();
		String jsonStr = EntityFactory.createJSONStringForEntity(ids);
		JSONObject jsonObj = signAndDispatchSynapseRequest(repoEndpoint, uri, "GET", jsonStr, defaultGETDELETEHeaders);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		RowMetadataResult result = new RowMetadataResult();
		result.initializeFromJSONObject(adapter);
		return result;
	}
	
	/*
	 * 
	 */
	public BackupRestoreStatus startBackup(MigrationType migrationType, IdList ids) throws JSONObjectAdapterException, SynapseException {
		String uri = MIGRATION_BACKUP + "?type=" + migrationType.name();
		String jsonStr = EntityFactory.createJSONStringForEntity(ids);
		JSONObject jsonObj = signAndDispatchSynapseRequest(repoEndpoint, uri, "POST", jsonStr, defaultPOSTPUTHeaders);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		BackupRestoreStatus brStatus = new BackupRestoreStatus();
		brStatus.initializeFromJSONObject(adapter);
		return brStatus;
	}
	
	/*
	 * 
	 */
	public BackupRestoreStatus startRestore(MigrationType migrationType, RestoreSubmission req) throws JSONObjectAdapterException, SynapseException {
		String uri = MIGRATION_RESTORE + "?type=" + migrationType.name();
		String jsonStr = EntityFactory.createJSONStringForEntity(req);
		JSONObject jsonObj = signAndDispatchSynapseRequest(repoEndpoint, uri, "POST", jsonStr, defaultPOSTPUTHeaders);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		BackupRestoreStatus brStatus = new BackupRestoreStatus();
		brStatus.initializeFromJSONObject(adapter);
		return brStatus;
	}
	
	/*
	 * 
	 */
	public BackupRestoreStatus getStatus(String daemonId) throws JSONObjectAdapterException, SynapseException {
		String uri = MIGRATION_STATUS + "?daemonId=" + daemonId;
		JSONObject jsonObj = signAndDispatchSynapseRequest(repoEndpoint, uri, "GET", null, defaultGETDELETEHeaders);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		BackupRestoreStatus brStatus = new BackupRestoreStatus();
		brStatus.initializeFromJSONObject(adapter);
		return brStatus;
	}
	
	/*
	 * 
	 */
	public MigrationTypeCount deleteMigratableObject(MigrationType migrationType, IdList ids) throws JSONObjectAdapterException, SynapseException {
		String uri = MIGRATION_DELETE + "?type=" + migrationType.name();
		String jsonStr = EntityFactory.createJSONStringForEntity(ids);
		JSONObject jsonObj = signAndDispatchSynapseRequest(repoEndpoint, uri, "PUT", jsonStr, defaultPOSTPUTHeaders);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObj);
		MigrationTypeCount mtc = new MigrationTypeCount();
		mtc.initializeFromJSONObject(adapter);
		return mtc;
	}

	/**
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 * 
	 */
	@Override
	public FireMessagesResult fireChangeMessages(Long startChangeNumber, Long limit) throws SynapseException, JSONObjectAdapterException {
		if(startChangeNumber == null) throw new IllegalArgumentException("startChangeNumber cannot be null");
		String uri = ADMIN_FIRE_MESSAGES + "?startChangeNumber=" + startChangeNumber;
		if (limit != null){
			uri = uri + "&limit=" + limit;
		}
		JSONObject jsonObj =  signAndDispatchSynapseRequest(repoEndpoint, uri, "GET", null, defaultGETDELETEHeaders);
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
		JSONObject jsonObj =  signAndDispatchSynapseRequest(repoEndpoint, uri, "GET", null, defaultGETDELETEHeaders);
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
	
	/**
	 * List change messages.
	 * @param queueName - The name of the queue to publishe the messages to.
	 * @param startChangeNumber - The change number to start from.
	 * @param type - (optional) when included, only messages of this type will be listed.
	 * @param limit - (optional) limit the number of messages to fetch.
	 * @return
	 * @throws JSONObjectAdapterException 
	 * @throws SynapseException 
	 */
	public PublishResults publishChangeMessages(String queueName, Long startChangeNumber, ObjectType type, Long limit) throws SynapseException, JSONObjectAdapterException{
		// Build up the URL
		String url = buildPublishMessagesURL(queueName, startChangeNumber, type, limit);
		JSONObject json = signAndDispatchSynapseRequest(repoEndpoint, url, "POST", null, defaultPOSTPUTHeaders);
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

	/**
	 * Clears the Synapse DOI table. Note this does not clear the DOIs registered outside Synapse.
	 */
	public void clearDoi() throws SynapseException {
		signAndDispatchSynapseRequest(repoEndpoint, ADMIN_DOI_CLEAR, "DELETE", null, defaultGETDELETEHeaders);
	}

	/**
	 * Clears the specified dynamo table.
	 */
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
			signAndDispatchSynapseRequest(repoEndpoint, uri, "DELETE", null, defaultGETDELETEHeaders);
		} catch (UnsupportedEncodingException e) {
			throw new SynapseException(e);
		}
	}

}
