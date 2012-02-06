package org.sagebionetworks.tool.migration.job;

import java.util.Set;

/**
 * A job to be executed.
 * @author John
 *
 */
public class Job {
	
	public enum Type {
		CREATE,
		UPDATE,
		DELETE,
		SEARCH_ADD, 
		SEARCH_DELETE
	}
	private Set<String> entityIds;
	private Type jobType;
	
	public Job(Set<String> entityIds, Type jobType) {
		super();
		this.entityIds = entityIds;
		this.jobType = jobType;
	}
	
	public Set<String> getEntityIds() {
		return entityIds;
	}

	public Type getJobType() {
		return jobType;
	}
	
	
	

}
