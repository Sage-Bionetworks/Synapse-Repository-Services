package org.sagebionetworks.repo.model.dbo.persistence;

import java.util.List;

import org.sagebionetworks.repo.model.ACTAccessRequirement;

public class TransACTAccessRequirement extends ACTAccessRequirement {
	private List<String> entityIds;

	public List<String> getEntityIds() {
		return entityIds;
	}

	public void setEntityIds(List<String> entityIds) {
		this.entityIds = entityIds;
	}

}
