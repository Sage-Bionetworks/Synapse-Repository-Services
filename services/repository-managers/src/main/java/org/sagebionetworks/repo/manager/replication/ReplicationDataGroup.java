package org.sagebionetworks.repo.manager.replication;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.table.ViewObjectType;

/**
 * DTO to represent a group of object ids to be added to the replication index
 * 
 * @author Marco Marasca
 *
 */
public class ReplicationDataGroup {

	private ViewObjectType objectType;
	private List<Long> allIds;
	private List<Long> createOrUpdateIds;

	public ReplicationDataGroup(ViewObjectType objectType) {
		this.objectType = objectType;
		this.allIds = new ArrayList<>();
		this.createOrUpdateIds = new ArrayList<>();
	}

	public void addForCreateOrUpdate(Long id) {
		addForDelete(id);
		createOrUpdateIds.add(id);
	}

	public void addForDelete(Long id) {
		allIds.add(id);
	}

	public ViewObjectType getObjectType() {
		return objectType;
	}

	public List<Long> getAllIds() {
		return allIds;
	}

	public List<Long> getCreateOrUpdateIds() {
		return createOrUpdateIds;
	}

	@Override
	public int hashCode() {
		return Objects.hash(allIds, createOrUpdateIds, objectType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ReplicationDataGroup other = (ReplicationDataGroup) obj;
		return Objects.equals(allIds, other.allIds) && Objects.equals(createOrUpdateIds, other.createOrUpdateIds)
				&& objectType == other.objectType;
	}

}
