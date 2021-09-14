package org.sagebionetworks.repo.manager.replication;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.table.ReplicationType;

/**
 * DTO to represent a group of object ids to be added to the replication index
 * 
 * @author Marco Marasca
 *
 */
public class ReplicationDataGroup {

	private ReplicationType objectType;
	private List<Long> toDelete;
	private List<Long> createOrUpdateIds;

	public ReplicationDataGroup(ReplicationType objectType) {
		this.objectType = objectType;
		this.toDelete = new ArrayList<>();
		this.createOrUpdateIds = new ArrayList<>();
	}

	public void addForCreateOrUpdate(Long id) {
		createOrUpdateIds.add(id);
	}

	public void addForDelete(Long id) {
		toDelete.add(id);
	}

	public ReplicationType getObjectType() {
		return objectType;
	}

	public List<Long> getToDeleteIds() {
		return toDelete;
	}

	public List<Long> getCreateOrUpdateIds() {
		return createOrUpdateIds;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createOrUpdateIds, objectType, toDelete);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ReplicationDataGroup)) {
			return false;
		}
		ReplicationDataGroup other = (ReplicationDataGroup) obj;
		return Objects.equals(createOrUpdateIds, other.createOrUpdateIds) && objectType == other.objectType
				&& Objects.equals(toDelete, other.toDelete);
	}

}
