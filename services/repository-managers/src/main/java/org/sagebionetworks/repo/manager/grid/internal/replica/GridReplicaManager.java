package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.net.URL;
import java.util.List;

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
	 * @param patches
	 */
	void onApplyPatches(ProgressCallback callback, GridConnectionInfo connection, Integer messageId, List<Patch> patches);

	/**
	 * Called to apply a new snapshot to an internal grid replica.
	 *
	 * @param callback
	 * @param connection
	 * @param messageId
	 * @param snapshotPresignedUrl
	 */
	void onApplySnapshot(ProgressCallback callback, GridConnectionInfo connection, Integer messageId, URL snapshotPresignedUrl);

	/**
	 * Called when a response message chain is completed.
	 * @param callback
	 * @param connection
	 * @param methodId
	 */
	void onResponseComplete(ProgressCallback callback, GridConnectionInfo connection, Integer methodId);

}
