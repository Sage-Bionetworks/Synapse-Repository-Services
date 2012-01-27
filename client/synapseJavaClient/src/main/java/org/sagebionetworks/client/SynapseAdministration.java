package org.sagebionetworks.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.BackupSubmission;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

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

	/**
	 * @param submission
	 * @return status
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public BackupRestoreStatus startBackupDaemon(BackupSubmission submission)
			throws JSONObjectAdapterException, SynapseException {
		JSONObject json = EntityFactory.createJSONObjectForEntity(submission);
		json = createEntity(DAEMON_BACKUP, json);
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
	public BackupRestoreStatus startRestoreDaemon(RestoreSubmission submission)
			throws JSONObjectAdapterException, SynapseException {
		JSONObject jsonObject = EntityFactory
				.createJSONObjectForEntity(submission);
		// Create the entity
		jsonObject = createEntity(DAEMON_RESTORE, jsonObject);
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
