package org.sagebionetworks.client;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.MigratableObjectCount;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.TrashedEntity;
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
	
	public PaginatedResults<MigratableObjectCount> getMigratableObjectCounts() throws SynapseException {
		String uri = GET_ALL_BACKUP_COUNTS;
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
		deleteUri(DAEMON_RESTORE+"?migrationType="+mod.getType()+"&id="+mod.getId());
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
		json = createJSONObject(DAEMON_BACKUP+"?migrationType="+migrationType, json);
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
		jsonObject = createJSONObject(DAEMON_RESTORE+"?migrationType="+migrationType, jsonObject);
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
}
