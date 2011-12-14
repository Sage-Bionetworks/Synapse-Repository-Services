package org.sagebionetworks.tool.migration.job;

public class WorkerResult {
	
	public enum JobStatus{
		SUCCEDED,
		FAILED,
	}
	
	int entitesProcessed;
	JobStatus status;
	
	public WorkerResult(int entitesProcessed, JobStatus status) {
		super();
		this.entitesProcessed = entitesProcessed;
		this.status = status;
	}
	public int getEntitesProcessed() {
		return entitesProcessed;
	}
	public JobStatus getStatus() {
		return status;
	}
	

}
