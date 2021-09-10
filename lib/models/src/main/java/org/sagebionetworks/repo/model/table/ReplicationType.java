package org.sagebionetworks.repo.model.table;

import java.util.Arrays;
import java.util.Optional;

import org.sagebionetworks.repo.model.ObjectType;

/**
 * Defines the main type of a replicated objected. This is the sub-set of
 * ObjectTypes that can appear in a view.
 */
public enum ReplicationType {
	SUBMISSION,
	ENTITY;
	
	/**
	 * Match the given ObjectType with a ReplicationType.  Note: Not all ObjectTypes map to a ReplicationType.
	 * @param objectType
	 * @return
	 */
	public static Optional<ReplicationType> matchType(ObjectType objectType){
		return Arrays.stream(ReplicationType.values()).filter(r -> r.name().equals(objectType.name())).findFirst();
	}
	
	/**
	 * Get the ObjectType for this ReplicationType
	 * @return
	 */
	public ObjectType getObjectType() {
		return ObjectType.valueOf(this.name());
	}
}
