package org.sagebionetworks.repo.model.athena;

public enum AthenaQueryExecutionState {
	QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED;
}
