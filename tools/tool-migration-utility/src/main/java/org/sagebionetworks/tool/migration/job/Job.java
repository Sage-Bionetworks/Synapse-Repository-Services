package org.sagebionetworks.tool.migration.job;

import java.util.Set;

import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;

/**
 * A job to be executed.
 * 
 * All the objects in 'objectIds' are of the type given by 'objectType' 
 * 
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
	private Set<String> objectIds;
	private MigratableObjectType objectType;
	private Type jobType;
	
	public Job(Set<String> objectIds, MigratableObjectType objectType, Type jobType) {
		super();
		this.objectIds = objectIds;
		this.objectType = objectType;
		this.jobType = jobType;
	}
	
	public Set<String> getObjectIds() {
		return objectIds;
	}

	public Type getJobType() {
		return jobType;
	}

	public MigratableObjectType getObjectType() {
		return objectType;
	}



}
