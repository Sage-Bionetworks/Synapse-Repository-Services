package org.sagebionetworks.repo.web.service.metadata;

import java.util.List;

import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.UserInfo;

/**
 * A data object that captures information about some change to an entity.
 * 
 * @author jmhill
 *
 */
public class EntityEvent {
	
	private EventType type;
	private List<EntityHeader> newParentPath;
	private UserInfo userInfo;

	public EntityEvent() {
		this(null, null,null);
	}

	public EntityEvent(EventType type, List<EntityHeader> newParentPath, UserInfo info) {
		super();
		this.type = type;
		this.newParentPath = newParentPath;
		this.userInfo = info;
	}
	public EventType getType() {
		return type;
	}
	public void setType(EventType type) {
		this.type = type;
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
	/**
	 * For any entity that has a parent this lists the full path of the parent.
	 * The first EntityHeader in the list is the root of the hierarchy and 
	 * the last EntityHeader in the list is the header for the parent.
	 * 
	 * @param newParentPath
	 */
	public void setNewParentPath(List<EntityHeader> newParentPath) {
		this.newParentPath = newParentPath;
	}
	public UserInfo getUserInfo() {
		return userInfo;
	}
	public void setUserInfo(UserInfo userInfo) {
		this.userInfo = userInfo;
	}


}
