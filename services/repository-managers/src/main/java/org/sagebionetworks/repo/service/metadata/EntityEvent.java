package org.sagebionetworks.repo.service.metadata;

import java.util.List;

import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.UserInfo;

/**
 * A data object that captures information about some change to an entity.
 * 
 * @author jmhill
 *
 */
public record EntityEvent(EventType type, List<EntityHeader> newParentPath, UserInfo info, boolean skipSanitization) {

	@Deprecated
	public EntityEvent() {
		this(null, null, null);
	}

	public EntityEvent(EventType type, List<EntityHeader> newParentPath, UserInfo info) {
		// If not provided, do not skip sanitization.
		this(type, newParentPath, info, false);
	}

	/**
	 * For any entity that has a parent this lists the full path of the parent.
	 * The first EntityHeader in the list is the root of the hierarchy and 
	 * the last EntityHeader in the list is the header for the parent.
	 * 
	 * @return
	 */
	public List<EntityHeader> getNewParentPath() {
		return newParentPath;
	}


	public UserInfo getUserInfo() {
		return info;
	}

	public EventType getType() {
		return type;
	}
}
