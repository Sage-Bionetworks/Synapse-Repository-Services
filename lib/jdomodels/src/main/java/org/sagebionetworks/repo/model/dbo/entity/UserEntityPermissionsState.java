package org.sagebionetworks.repo.model.dbo.entity;

import java.util.Objects;

import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

/**
 * Represents the user's entity permissions state from the database.
 * 
 */
public class UserEntityPermissionsState {

	private Long entityId;
	private Long benefactorId;
	private EntityType entityType;
	private DataType dataType;
	private Long entityCreatedBy;
	private Long entityParentId;
	private boolean doesEntityExist;
	private boolean hasRead;
	private boolean hasDownload;
	private boolean hasCreate;
	private boolean hasUpdate;
	private boolean hasDelete;
	private boolean hasChangePermissions;
	private boolean hasChangeSettings;
	private boolean hasModerate;
	private boolean hasPublicRead;

	public UserEntityPermissionsState(Long entityId) {
		super();
		this.entityId = entityId;
		this.benefactorId = null;
		this.entityType = null;
		this.dataType = DataType.SENSITIVE_DATA;
		this.doesEntityExist = false;
		this.hasRead = false;
		this.hasDownload = false;
		this.hasCreate = false;
		this.hasUpdate = false;
		this.hasDelete = false;
		this.hasChangePermissions = false;
		this.hasChangeSettings = false;
		this.hasModerate = false;
		this.hasPublicRead = false;
	}

	/**
	 * The ID of the entity.
	 * 
	 * @return the entityId
	 */
	public Long getEntityId() {
		return entityId;
	}
	
	public String getEntityIdAsString() {
		return KeyFactory.keyToString(entityId);
	}

	/**
	 * @param entityId the entityId to set
	 */
	public UserEntityPermissionsState withEntityId(Long entityId) {
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
	public UserEntityPermissionsState withBenefactorId(Long benefactorId) {
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
	public UserEntityPermissionsState withEntityType(EntityType entityType) {
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
	public UserEntityPermissionsState withDataType(DataType dataType) {
		this.dataType = dataType;
		return this;
	}

	/**
	 * Does this entity exist?
	 */
	public boolean doesEntityExist() {
		return doesEntityExist;
	}

	/**
	 * Does this entity exist?
	 */
	public UserEntityPermissionsState withtDoesEntityExist(boolean doesEntityExist) {
		this.doesEntityExist = doesEntityExist;
		return this;
	}

	/**
	 * Does the user have the read permission on this entity?
	 * 
	 * @return the hasRead
	 */
	public boolean hasRead() {
		return hasRead;
	}

	/**
	 * Does the user have the update permission on this entity?
	 */
	public boolean hasUpdate() {
		return hasUpdate;
	}

	/**
	 * Does the user have the update permission on this entity?
	 */
	public UserEntityPermissionsState withHasUpdate(boolean hasUpdate) {
		this.hasUpdate = hasUpdate;
		return this;
	}

	/**
	 * Does the user have the read permission on this entity?
	 * 
	 * @param hasRead the hasRead to set
	 */
	public UserEntityPermissionsState withHasRead(boolean hasRead) {
		this.hasRead = hasRead;
		return this;
	}

	/**
	 * Does the user have the download permission on this entity?
	 */
	public boolean hasDownload() {
		return hasDownload;
	}

	/**
	 * Does the user have the download permission on this entity?
	 */
	public UserEntityPermissionsState withHasDownload(boolean hasDownload) {
		this.hasDownload = hasDownload;
		return this;
	}

	/**
	 * Does the user have the create permission on this entity?
	 */
	public boolean hasCreate() {
		return hasCreate;
	}

	/**
	 * Does the user have the create permission on this entity?
	 */
	public UserEntityPermissionsState withHasCreate(boolean hasCreate) {
		this.hasCreate = hasCreate;
		return this;
	}

	/**
	 * Does the user have the delete permission on this entity?
	 */
	public boolean hasDelete() {
		return hasDelete;
	}

	/**
	 * Does the user have the delete permission on this entity?
	 */
	public UserEntityPermissionsState withHasDelete(boolean hasDelete) {
		this.hasDelete = hasDelete;
		return this;
	}

	/**
	 * Does the user have the change_permission permission on this entity?
	 */
	public boolean hasChangePermissions() {
		return hasChangePermissions;
	}

	/**
	 * Does the user have the change_permission permission on this entity?
	 */
	public UserEntityPermissionsState withHasChangePermissions(boolean hasChangePermissions) {
		this.hasChangePermissions = hasChangePermissions;
		return this;
	}

	/**
	 * Does the user have the change_settings permission on this entity?
	 */
	public boolean hasChangeSettings() {
		return hasChangeSettings;
	}

	/**
	 * Does the user have the change_settings permission on this entity?
	 */
	public UserEntityPermissionsState withHasChangeSettings(boolean hasChangeSettings) {
		this.hasChangeSettings = hasChangeSettings;
		return this;
	}

	/**
	 * Does the user have the moderate permission on this entity?
	 */
	public boolean hasModerate() {
		return hasModerate;
	}

	/**
	 * Does the user have the moderate permission on this entity?
	 */
	public UserEntityPermissionsState withHasModerate(boolean hasModerate) {
		this.hasModerate = hasModerate;
		return this;
	}
	
	/**
	 * Has the READ permission been granted to PUBLIC for this entity?
	 * @return the hasPublicRead
	 */
	public boolean hasPublicRead() {
		return hasPublicRead;
	}

	/**
	 * Has the READ permission been granted to PUBLIC for this entity?
	 */
	public UserEntityPermissionsState withHasPublicRead(boolean hasPublicRead) {
		this.hasPublicRead = hasPublicRead;
		return this;
	}

	/**
	 * @return the entityCreatedBy
	 */
	public Long getEntityCreatedBy() {
		return entityCreatedBy;
	}

	/**
	 * @param entityCreatedBy the entityCreatedBy to set
	 */
	public UserEntityPermissionsState withEntityCreatedBy(Long entityCreatedBy) {
		this.entityCreatedBy = entityCreatedBy;
		return this;
	}

	/**
	 * @return the entityParentId
	 */
	public Long getEntityParentId() {
		return entityParentId;
	}

	/**
	 * @param entityParentId the entityParentId to set
	 */
	public UserEntityPermissionsState withEntityParentId(Long entityParentId) {
		this.entityParentId = entityParentId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(benefactorId, dataType, doesEntityExist, entityCreatedBy, entityId, entityParentId,
				entityType, hasChangePermissions, hasChangeSettings, hasCreate, hasDelete, hasDownload, hasModerate,
				hasPublicRead, hasRead, hasUpdate);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof UserEntityPermissionsState)) {
			return false;
		}
		UserEntityPermissionsState other = (UserEntityPermissionsState) obj;
		return Objects.equals(benefactorId, other.benefactorId) && dataType == other.dataType
				&& doesEntityExist == other.doesEntityExist && Objects.equals(entityCreatedBy, other.entityCreatedBy)
				&& Objects.equals(entityId, other.entityId) && Objects.equals(entityParentId, other.entityParentId)
				&& entityType == other.entityType && hasChangePermissions == other.hasChangePermissions
				&& hasChangeSettings == other.hasChangeSettings && hasCreate == other.hasCreate
				&& hasDelete == other.hasDelete && hasDownload == other.hasDownload && hasModerate == other.hasModerate
				&& hasPublicRead == other.hasPublicRead && hasRead == other.hasRead && hasUpdate == other.hasUpdate;
	}

	@Override
	public String toString() {
		return "UserEntityPermissionsState [entityId=" + entityId + ", benefactorId=" + benefactorId + ", entityType="
				+ entityType + ", dataType=" + dataType + ", entityCreatedBy=" + entityCreatedBy + ", entityParentId="
				+ entityParentId + ", doesEntityExist=" + doesEntityExist + ", hasRead=" + hasRead + ", hasDownload="
				+ hasDownload + ", hasCreate=" + hasCreate + ", hasUpdate=" + hasUpdate + ", hasDelete=" + hasDelete
				+ ", hasChangePermissions=" + hasChangePermissions + ", hasChangeSettings=" + hasChangeSettings
				+ ", hasModerate=" + hasModerate + ", hasPublicRead=" + hasPublicRead + "]";
	}

}
