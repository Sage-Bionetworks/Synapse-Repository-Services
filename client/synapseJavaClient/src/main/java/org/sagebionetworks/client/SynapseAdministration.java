package org.sagebionetworks.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.MigratableObjectCount;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.BackupSubmission;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * Java Client API for Synapse Administrative REST APIs
 */
public class SynapseAdministration extends Synapse {

	private static final Logger log = Logger.getLogger(SynapseAdministration.class.getName());

	public static final String DAEMON = ADMIN + "/daemon";
	public static final String BACKUP = "/backup";
	public static final String RESTORE = "/restore";
	public static final String SEARCH_DOCUMENT = "/searchDocument";
	public static final String DAEMON_BACKUP = DAEMON + BACKUP;
	public static final String DAEMON_RESTORE = DAEMON + RESTORE;
	public static final String DAEMON_SEARCH_DOCUMENT = DAEMON + SEARCH_DOCUMENT;
	public static final String GET_ALL_BACKUP_OBJECTS = "/backupObjects";
	public static final String INCLUDE_DEPENDENCIES_PARAM = "includeDependencies";
	public static final String GET_ALL_BACKUP_COUNTS = "/backupObjectCounts";
	
	public SynapseAdministration() {
		super();
	}
	
	public SynapseAdministration(HttpClientProvider clientProvider, DataUploader dataUploader) {
		super(clientProvider, dataUploader);
	}
	
	public PaginatedResults<MigratableObjectData> getAllMigratableObjectsPaginated(long offset, long limit, boolean includeDependencies)  throws JSONObjectAdapterException, SynapseException  {
		String uri = GET_ALL_BACKUP_OBJECTS + "?" + 
			ServiceConstants.PAGINATION_OFFSET_PARAM+"="+offset+"&"+
			ServiceConstants.PAGINATION_LIMIT_PARAM+"="+limit+"&"+
			INCLUDE_DEPENDENCIES_PARAM+"="+includeDependencies;
		JSONObject jsonUsers = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonUsers);
		PaginatedResults<MigratableObjectData> results = new PaginatedResults<MigratableObjectData>(MigratableObjectData.class);
		try {
			results.initializeFromJSONObject(adapter);
			return results;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public PaginatedResults<MigratableObjectCount> getMigratableObjectCounts(long offset, long limit, boolean includeDependencies) throws SynapseException {
		String uri = GET_ALL_BACKUP_COUNTS + "?" +
			ServiceConstants.PAGINATION_OFFSET_PARAM + "=" + offset + "&" +
			ServiceConstants.PAGINATION_LIMIT_PARAM + "=" + limit + "&" +
			INCLUDE_DEPENDENCIES_PARAM +  "=" + includeDependencies;
		JSONObject o = getEntity(uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(o);
		PaginatedResults<MigratableObjectCount> rs = new PaginatedResults<MigratableObjectCount>(MigratableObjectCount.class);
		try {
			rs.initializeFromJSONObject(adapter);
			return rs;
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
	}
	
	public void deleteObject(MigratableObjectDescriptor mod)  throws SynapseNotFoundException, SynapseException {
		deleteEntity(DAEMON_RESTORE+"?migrationType="+mod.getType()+"&id="+mod.getId());
	}

	/**
	 * @param submission
	 * @return status
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public BackupRestoreStatus startBackupDaemon(BackupSubmission submission, MigratableObjectType migrationType)
			throws JSONObjectAdapterException, SynapseException {
		JSONObject json = EntityFactory.createJSONObjectForEntity(submission);
		json = createEntity(DAEMON_BACKUP+"?migrationType="+migrationType, json);
		return EntityFactory.createEntityFromJSONObject(json,
				BackupRestoreStatus.class);
	}

	/**
	 * @param submission
	 * @return status
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public BackupRestoreStatus startSearchDocumentDaemon(BackupSubmission submission)
			throws JSONObjectAdapterException, SynapseException {
		JSONObject json = EntityFactory.createJSONObjectForEntity(submission);
		json = createEntity(DAEMON_SEARCH_DOCUMENT, json);
		return EntityFactory.createEntityFromJSONObject(json,
				BackupRestoreStatus.class);
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
	 * @param submission
	 * @return status
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public BackupRestoreStatus startRestoreDaemon(RestoreSubmission submission, MigratableObjectType migrationType)
			throws JSONObjectAdapterException, SynapseException {
		JSONObject jsonObject = EntityFactory
				.createJSONObjectForEntity(submission);
		// Create the entity
		jsonObject = createEntity(DAEMON_RESTORE+"?migrationType="+migrationType, jsonObject);
		return EntityFactory.createEntityFromJSONObject(jsonObject,
				BackupRestoreStatus.class);
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
}
