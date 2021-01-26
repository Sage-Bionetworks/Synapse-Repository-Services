package org.sagebionetworks.repo.model.dbo.entity;

import java.util.Objects;

public class EntityPermission {

	private Long entityId;
	private Long benefactorId;
	private boolean hasRead;
	private boolean hasDownload;
	
	public EntityPermission(Long entityId) {
		super();
		this.entityId = entityId;
		this.benefactorId = null;
		this.hasRead = false;
		this.hasDownload = false;
	}

	/**
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
	 * @return the benefactorId
	 */
	public Long getBenefactorId() {
		return benefactorId;
	}

	/**
	 * @param benefactorId the benefactorId to set
	 */
	public EntityPermission withBenefactorId(Long benefactorId) {
		this.benefactorId = benefactorId;
		return this;
	}

	/**
	 * @return the hasRead
	 */
	public boolean isHasRead() {
		return hasRead;
	}

	/**
	 * @param hasRead the hasRead to set
	 */
	public EntityPermission withtHasRead(boolean hasRead) {
		this.hasRead = hasRead;
		return this;
	}

	/**
	 * @return the hasDownload
	 */
	public boolean isHasDownload() {
		return hasDownload;
	}

	/**
	 * @param hasDownload the hasDownload to set
	 */
	public EntityPermission withHasDownload(boolean hasDownload) {
		this.hasDownload = hasDownload;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(benefactorId, entityId, hasDownload, hasRead);
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
		return Objects.equals(benefactorId, other.benefactorId) && Objects.equals(entityId, other.entityId)
				&& hasDownload == other.hasDownload && hasRead == other.hasRead;
	}

	@Override
	public String toString() {
		return "EntityPermission [entityId=" + entityId + ", benefactorId=" + benefactorId + ", hasRead=" + hasRead
				+ ", hasDownload=" + hasDownload + "]";
	}

}
