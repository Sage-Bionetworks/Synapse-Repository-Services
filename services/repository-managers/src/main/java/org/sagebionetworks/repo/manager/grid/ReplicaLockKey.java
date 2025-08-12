package org.sagebionetworks.repo.manager.grid;

public class ReplicaLockKey {

	private final String key;

	public ReplicaLockKey(String sessionId, Long replicaId) {
		key = String.format("%s-%d", sessionId, replicaId);
	}

	public String getKey() {
		return key;
	}
}
