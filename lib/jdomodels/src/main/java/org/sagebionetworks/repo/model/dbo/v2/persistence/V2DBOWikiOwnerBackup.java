package org.sagebionetworks.repo.model.dbo.v2.persistence;

import java.util.Arrays;

import org.sagebionetworks.repo.model.ObjectType;

public class V2DBOWikiOwnerBackup {

	private Long ownerId;
	// Old field name release-71
	private ObjectType ownerType;
	// new field name as of release-72
	private ObjectType ownerTypeEnum;
	private Long rootWikiId;
	private byte[] orderHint;
	private String etag;
	public Long getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}
	public ObjectType getOwnerType() {
		return ownerType;
	}
	public void setOwnerType(ObjectType ownerType) {
		this.ownerType = ownerType;
	}
	public ObjectType getOwnerTypeEnum() {
		return ownerTypeEnum;
	}
	public void setOwnerTypeEnum(ObjectType ownerTypeEnum) {
		this.ownerTypeEnum = ownerTypeEnum;
	}
	public Long getRootWikiId() {
		return rootWikiId;
	}
	public void setRootWikiId(Long rootWikiId) {
		this.rootWikiId = rootWikiId;
	}
	public byte[] getOrderHint() {
		return orderHint;
	}
	public void setOrderHint(byte[] orderHint) {
		this.orderHint = orderHint;
	}
	public String getEtag() {
		return etag;
	}
	public void setEtag(String etag) {
		this.etag = etag;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + Arrays.hashCode(orderHint);
		result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
		result = prime * result
				+ ((ownerType == null) ? 0 : ownerType.hashCode());
		result = prime * result
				+ ((ownerTypeEnum == null) ? 0 : ownerTypeEnum.hashCode());
		result = prime * result
				+ ((rootWikiId == null) ? 0 : rootWikiId.hashCode());
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
		V2DBOWikiOwnerBackup other = (V2DBOWikiOwnerBackup) obj;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (!Arrays.equals(orderHint, other.orderHint))
			return false;
		if (ownerId == null) {
			if (other.ownerId != null)
				return false;
		} else if (!ownerId.equals(other.ownerId))
			return false;
		if (ownerType != other.ownerType)
			return false;
		if (ownerTypeEnum != other.ownerTypeEnum)
			return false;
		if (rootWikiId == null) {
			if (other.rootWikiId != null)
				return false;
		} else if (!rootWikiId.equals(other.rootWikiId))
			return false;
		return true;
	}

	
}
