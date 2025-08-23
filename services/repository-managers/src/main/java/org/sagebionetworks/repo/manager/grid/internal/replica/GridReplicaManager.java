package org.sagebionetworks.repo.manager.grid.internal.replica;

import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.util.progress.ProgressCallback;

public interface GridReplicaManager {

	public static final String SYNCHRONIZE_CLOCK = "synchronize-clock";

	/**
	 * Called when a new connection to an internal replica is established.
	 * 
	 * @param callback
	 * @param connection
	 */
	void onConnected(ProgressCallback callback, GridConnectionInfo connection);

	/**
	 * Called when a new patch is available for an internal replica.
	 * 
	 * @param callback
	 * @param connection
	 */
	void onNewPatch(ProgressCallback callback, GridConnectionInfo connection);

	/**
	 * Called to apply a new patch to an internal grid replica.
	 * 
	 * @param callback
	 * @param connection
	 * @param messageId
	 * @param patch
	 */
	void onApplyPatch(ProgressCallback callback, GridConnectionInfo connection, Integer messageId, Patch patch);

	/**
	 * Called when a response message chain is completed.
	 * @param callback
	 * @param connection
	 * @param methodId
	 */
	void onResponseComplete(ProgressCallback callback, GridConnectionInfo connection, Integer methodId);

}
