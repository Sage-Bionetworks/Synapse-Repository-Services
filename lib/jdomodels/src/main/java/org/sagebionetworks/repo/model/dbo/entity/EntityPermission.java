package org.sagebionetworks.repo.model.dbo.entity;

import java.util.Objects;

import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.EntityType;

/**
 * Basic information about an Entity combined with a user's permissions on the
 * entity.
 *
 */
public class EntityPermission {

	private Long entityId;
	private Long benefactorId;
	private EntityType entityType;
	private DataType dataType;
	private boolean hasRead;
	private boolean hasDownload;
	private boolean hasCreate;
	private boolean hasDelete;
	private boolean hasChangePermissions;
	private boolean hasChangeSettings;
	private boolean hasModerate;

	public EntityPermission(Long entityId) {
		super();
		this.entityId = entityId;
		this.benefactorId = null;
		this.entityType = null;
		this.dataType = DataType.SENSITIVE_DATA;
		this.hasRead = false;
		this.hasDownload = false;
		this.hasCreate = false;
		this.hasDelete = false;
		this.hasChangePermissions = false;
		this.hasChangeSettings = false;
		this.hasModerate = false;
	}

	/**
	 * The ID of the entity.
	 * 
	 * @return the entityId
	 */
	public Long getEntityId() {
		return entityId;
	}

	/**
	 * @param entityId the entityId to set
	 */
	public EntityPermission withEntityId(Long entityId) {
		this.entityId = entityId;
		return this;
	}

	/**
	 * The ID of the entity that has an ACL that controls access to this entity.
	 * 
	 * @return the benefactorId
	 */
	public Long getBenefactorId() {
		return benefactorId;
	}

	/**
	 * The ID of the entity that has an ACL that controls access to this entity.
	 * 
	 * @param benefactorId the benefactorId to set
	 */
	public EntityPermission withBenefactorId(Long benefactorId) {
		this.benefactorId = benefactorId;
		return this;
	}

	/**
	 * The type of the entity.
	 * 
	 * @return the entityType
	 */
	public EntityType getEntityType() {
		return entityType;
	}

	/**
	 * @param entityType the entityType to set
	 */
	public EntityPermission withEntityType(EntityType entityType) {
		this.entityType = entityType;
		return this;
	}

	/**
	 * The data type determines if there are additional restrictions on this entity.
	 */
	public DataType getDataType() {
		return dataType;
	}

	/**
	 * The data type determines if there are additional restrictions on this entity.
	 */
	public EntityPermission withDataType(DataType dataType) {
		this.dataType = dataType;
		return this;
	}

	/**
	 * Does the user have the read permission on this entity?
	 * 
	 * @return the hasRead
	 */
	public boolean isHasRead() {
		return hasRead;
	}

	/**
	 * Does the user have the read permission on this entity?
	 * 
	 * @param hasRead the hasRead to set
	 */
	public EntityPermission withtHasRead(boolean hasRead) {
		this.hasRead = hasRead;
		return this;
	}

	/**
	 * Does the user have the download permission on this entity?
	 */
	public boolean isHasDownload() {
		return hasDownload;
	}

	/**
	 * Does the user have the download permission on this entity?
	 */
	public EntityPermission withHasDownload(boolean hasDownload) {
		this.hasDownload = hasDownload;
		return this;
	}

	/**
	 * Does the user have the create permission on this entity?
	 */
	public boolean isHasCreate() {
		return hasCreate;
	}

	/**
	 * Does the user have the create permission on this entity?
	 */
	public EntityPermission withHasCreate(boolean hasCreate) {
		this.hasCreate = hasCreate;
		return this;
	}

	/**
	 * Does the user have the delete permission on this entity?
	 */
	public boolean isHasDelete() {
		return hasDelete;
	}

	/**
	 * Does the user have the delete permission on this entity?
	 */
	public EntityPermission withHasDelete(boolean hasDelete) {
		this.hasDelete = hasDelete;
		return this;
	}

	/**
	 * Does the user have the change_permission permission on this entity?
	 */
	public boolean isHasChangePermissions() {
		return hasChangePermissions;
	}

	/**
	 * Does the user have the change_permission permission on this entity?
	 */
	public EntityPermission withHasChangePermissions(boolean hasChangePermissions) {
		this.hasChangePermissions = hasChangePermissions;
		return this;
	}

	/**
	 * Does the user have the change_settings permission on this entity?
	 */
	public boolean isHasChangeSettings() {
		return hasChangeSettings;
	}

	/**
	 * Does the user have the change_settings permission on this entity?
	 */
	public EntityPermission withHasChangeSettings(boolean hasChangeSettings) {
		this.hasChangeSettings = hasChangeSettings;
		return this;
	}

	/**
	 * Does the user have the moderate permission on this entity?
	 */
	public boolean isHasModerate() {
		return hasModerate;
	}

	/**
	 * Does the user have the moderate permission on this entity?
	 */
	public EntityPermission withHasModerate(boolean hasModerate) {
		this.hasModerate = hasModerate;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(benefactorId, dataType, entityId, entityType, hasChangePermissions, hasChangeSettings,
				hasCreate, hasDelete, hasDownload, hasModerate, hasRead);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof EntityPermission)) {
			return false;
		}
		EntityPermission other = (EntityPermission) obj;
		return Objects.equals(benefactorId, other.benefactorId) && dataType == other.dataType
				&& Objects.equals(entityId, other.entityId) && entityType == other.entityType
				&& hasChangePermissions == other.hasChangePermissions && hasChangeSettings == other.hasChangeSettings
				&& hasCreate == other.hasCreate && hasDelete == other.hasDelete && hasDownload == other.hasDownload
				&& hasModerate == other.hasModerate && hasRead == other.hasRead;
	}

	@Override
	public String toString() {
		return "EntityPermission [entityId=" + entityId + ", benefactorId=" + benefactorId + ", entityType="
				+ entityType + ", dataType=" + dataType + ", hasRead=" + hasRead + ", hasDownload=" + hasDownload
				+ ", hasCreate=" + hasCreate + ", hasDelete=" + hasDelete + ", hasChangePermissions="
				+ hasChangePermissions + ", hasChangeSettings=" + hasChangeSettings + ", hasModerate=" + hasModerate
				+ "]";
	}

}
