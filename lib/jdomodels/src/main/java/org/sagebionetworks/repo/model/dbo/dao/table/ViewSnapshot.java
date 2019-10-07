package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Date;

/**
 * Data transfer object for a view snapshot.
 *
 */
public class ViewSnapshot {

	Long snapshotId;
	Long viewId;
	Long version;
	String bucket;
	String key;
	Long createdBy;
	Date createdOn;
	
	public Long getSnapshotId() {
		return snapshotId;
	}
	public ViewSnapshot withSnapshotId(Long snapshotId) {
		this.snapshotId = snapshotId;
		return this;
	}
	public Long getViewId() {
		return viewId;
	}
	public ViewSnapshot withViewId(Long viewId) {
		this.viewId = viewId;
		return this;
	}
	public Long getVersion() {
		return version;
	}
	public ViewSnapshot withVersion(Long version) {
		this.version = version;
		return this;
	}
	public String getBucket() {
		return bucket;
	}
	public ViewSnapshot withBucket(String bucket) {
		this.bucket = bucket;
		return this;
	}
	public String getKey() {
		return key;
	}
	public ViewSnapshot withKey(String key) {
		this.key = key;
		return this;
	}
	public Long getCreatedBy() {
		return createdBy;
	}
	public ViewSnapshot withCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}
	public Date getCreatedOn() {
		return createdOn;
	}
	public ViewSnapshot withCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
		return this;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((snapshotId == null) ? 0 : snapshotId.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		result = prime * result + ((viewId == null) ? 0 : viewId.hashCode());
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
		ViewSnapshot other = (ViewSnapshot) obj;
		if (bucket == null) {
			if (other.bucket != null)
				return false;
		} else if (!bucket.equals(other.bucket))
			return false;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (snapshotId == null) {
			if (other.snapshotId != null)
				return false;
		} else if (!snapshotId.equals(other.snapshotId))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		if (viewId == null) {
			if (other.viewId != null)
				return false;
		} else if (!viewId.equals(other.viewId))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "ViewSnapshot [snapshotId=" + snapshotId + ", viewId=" + viewId + ", version=" + version + ", bucket="
				+ bucket + ", key=" + key + ", createdBy=" + createdBy + ", createdOn=" + createdOn + "]";
	}

}
