package org.sagebionetworks.repo.model.grid;

import org.sagebionetworks.util.ValidateArgument;

public class GridConstants {

	/**
	 * JSON-Joy defined minimum replica ID. See: <a
	 * href=https://jsonjoy.com/specs/json-crdt-patch/patch-document/logical-clock>logical-clock</a>
	 */
	public static final Long MIN_REPICA_ID = (long) 0xffff;

	/**
	 * The starting user replica ID for a grid session. Client replicas will
	 * increment this value.
	 */
	public static final Long START_REPLICA_ID_CLIENT = MIN_REPICA_ID + 1001;
	/**
	 * The starting internal replica ID for grid session. Service replicas will
	 * decrement this value. This ensure all service replica ID are smaller than all
	 * client replica ID. This means when an service and client replica have a
	 * conflict, the client's replica always wins.
	 */
	public static final Long START_REPLICA_ID_SERVICE = START_REPLICA_ID_CLIENT - 1;

	/**
	 * Returns true if the passed replica ID is a USER replica (clients or agents).
	 * Returns false if the passed replica ID is a SERVICE (like internal replica or
	 * validation replica)
	 * 
	 * @param replicaId
	 * @return
	 */
	public static boolean isUserReplica(Long replicaId) {
		ValidateArgument.required(replicaId, "replicaId");
		return replicaId >= START_REPLICA_ID_CLIENT;
	}

}
