package org.sagebionetworks.repo.model.message;

import org.sagebionetworks.util.ValidateArgument;

/**
 * The modification key is a composite of the project and user ids
 * 
 */
class ModificationMessageKey {

	private final Long userId;
	private final Long projectId;
	private final String entityId;

	public ModificationMessageKey(ModificationMessage message) {
		ValidateArgument.required(message, "ModificationMessage");
		ValidateArgument.required(message.getUserId(), "ModificationMessage.userId");
		ValidateArgument.requiredOneOf("ModificationMessage.projectId or ModificationMessage.entityId", message.getProjectId(),
				message.getEntityId());
		this.userId = message.getUserId();
		this.projectId = message.getProjectId();
		this.entityId = message.getEntityId();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entityId == null) ? 0 : entityId.hashCode());
		result = prime * result + ((projectId == null) ? 0 : projectId.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModificationMessageKey other = (ModificationMessageKey) obj;
		if (entityId == null) {
			if (other.entityId != null)
				return false;
		} else if (!entityId.equals(other.entityId))
			return false;
		if (projectId == null) {
			if (other.projectId != null)
				return false;
		} else if (!projectId.equals(other.projectId))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}
}