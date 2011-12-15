package org.sagebionetworks.tool.migration.dao;

/**
 * DTO of basic entity data.
 * 
 * @author jmhill
 *
 */
public class EntityData {
	
	public EntityData(String entityId, String eTag, String parentId) {
		super();
		this.entityId = entityId;
		this.eTag = eTag;
		this.parentId = parentId;
	}
	
	public EntityData(EntityData toClone) {
		super();
		this.entityId = toClone.entityId;
		this.eTag = toClone.eTag;
		this.parentId = toClone.parentId;
	}
	
	private String entityId = null;
	private String eTag = null;
	private String parentId = null;

	public String getEntityId() {
		return entityId;
	}
	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}
	public String geteTag() {
		return eTag;
	}
	public void seteTag(String eTag) {
		this.eTag = eTag;
	}
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result
				+ ((entityId == null) ? 0 : entityId.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
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
		EntityData other = (EntityData) obj;
		if (eTag == null) {
			if (other.eTag != null)
				return false;
		} else if (!eTag.equals(other.eTag))
			return false;
		if (entityId == null) {
			if (other.entityId != null)
				return false;
		} else if (!entityId.equals(other.entityId))
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "EntityData [entityId=" + entityId + ", eTag=" + eTag
				+ ", parentId=" + parentId + "]";
	}

}
