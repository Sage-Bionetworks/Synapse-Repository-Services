package org.sagebionetworks.repo.model.bootstrap;

import java.util.List;

import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.EntityType;

/**
 * The metadata used to bootstrap an entity.
 * 
 * @author jmhill
 *
 */
public class EntityBootstrapData {
	
	private String entityPath;
	private Long entityId;
	private String entityDescription;
	private EntityType entityType;
	private List<AccessBootstrapData> accessList;
	private ACL_SCHEME defaultChildAclScheme;
	
	public Long getEntityId() {
		return entityId;
	}
	
	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}
	
	/**
	 * The description that will be applied to the resulting entity.
	 * @return
	 */
	public String getEntityDescription() {
		return entityDescription;
	}
	/**
	 * The description that will be applied to the resulting entity.
	 * @param entityDescription
	 */
	public void setEntityDescription(String entityDescription) {
		this.entityDescription = entityDescription;
	}
	/**
	 * The entity type of the resulting entity.
	 * @return
	 */
	public EntityType getEntityType() {
		return entityType;
	}
	/**
	 * The entity type of the resulting entity.
	 * @param entityType
	 */
	public void setEntityType(EntityType entityType) {
		this.entityType = entityType;
	}
	/**
	 * AccessBootstrapData is used to setup the ACL on the resulting entity.
	 * @return
	 */
	public List<AccessBootstrapData> getAccessList() {
		return accessList;
	}
	
	/**
	 * AccessBootstrapData is used to setup the ACL on the resulting entity.
	 * @param accessList
	 */
	public void setAccessList(List<AccessBootstrapData> accessList) {
		this.accessList = accessList;
	}
	/**
	 * The full path that will be applied to the entity.  This path is 
	 * composed of a both the parent entity's path and the name of the resulting entity.
	 * For example, to create an entity with a parent of '/root' and a name of 'test'
	 * the entity path would be '/root/test'
	 * @return
	 */
	public String getEntityPath() {
		return entityPath;
	}
	/**
	 * The full path that will be applied to the entity.  This path is 
	 * composed of a both the parent entity's path and the name of the resulting entity.
	 * For example, to create an entity with a parent of '/root' and a name of 'test'
	 * the entity path would be '/root/test'
	 * @param entityPath
	 */
	public void setEntityPath(String entityPath) {
		this.entityPath = entityPath;
	}

	/**
	 * When children are added to the resulting entity what ACL scheme should
	 * be used for the child?
	 * @return
	 */
	public ACL_SCHEME getDefaultChildAclScheme() {
		return defaultChildAclScheme;
	}
	
	/**
	 * When children are added to the resulting entity what ACL scheme should
	 * be used for the child?
	 * @param defaultChildAclScheme
	 */
	public void setDefaultChildAclScheme(ACL_SCHEME defaultChildAclScheme) {
		this.defaultChildAclScheme = defaultChildAclScheme;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accessList == null) ? 0 : accessList.hashCode());
		result = prime
				* result
				+ ((defaultChildAclScheme == null) ? 0 : defaultChildAclScheme
						.hashCode());
		result = prime
				* result
				+ ((entityDescription == null) ? 0 : entityDescription
						.hashCode());
		result = prime * result
				+ ((entityId == null) ? 0 : entityId.hashCode());
		result = prime * result
				+ ((entityPath == null) ? 0 : entityPath.hashCode());
		result = prime * result
				+ ((entityType == null) ? 0 : entityType.hashCode());
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
		EntityBootstrapData other = (EntityBootstrapData) obj;
		if (accessList == null) {
			if (other.accessList != null)
				return false;
		} else if (!accessList.equals(other.accessList))
			return false;
		if (defaultChildAclScheme != other.defaultChildAclScheme)
			return false;
		if (entityDescription == null) {
			if (other.entityDescription != null)
				return false;
		} else if (!entityDescription.equals(other.entityDescription))
			return false;
		if (entityId == null) {
			if (other.entityId != null)
				return false;
		} else if (!entityId.equals(other.entityId))
			return false;
		if (entityPath == null) {
			if (other.entityPath != null)
				return false;
		} else if (!entityPath.equals(other.entityPath))
			return false;
		if (entityType == null) {
			if (other.entityType != null)
				return false;
		} else if (!entityType.equals(other.entityType))
			return false;
		return true;
	}
	
}
