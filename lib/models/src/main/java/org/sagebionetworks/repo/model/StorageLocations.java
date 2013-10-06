package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.attachment.AttachmentData;

/**
 * Storage location data from unpacked blobs.
 */
public class StorageLocations {

	public StorageLocations(Long nodeId, Long userId,
			List<AttachmentData> attachments, List<LocationData> locations,
			Map<String, List<String>> strAnnotations) {

		if (nodeId == null) {
			throw new NullPointerException();
		}
		if (userId == null) {
			throw new NullPointerException();
		}
		if (attachments == null) {
			throw new NullPointerException();
		}
		if (locations == null) {
			throw new NullPointerException();
		}
		if (strAnnotations == null) {
			throw new NullPointerException();
		}

		this.nodeId = nodeId;
		this.userId = userId;
		this.attachments = attachments;
		this.locations = locations;
		this.strAnnotations = strAnnotations;
	}

	public Long getNodeId() {
		return nodeId;
	}

	public Long getUserId() {
		return userId;
	}

	public List<AttachmentData> getAttachments() {
		return attachments;
	}

	public List<LocationData> getLocations() {
		return locations;
	}

	/**
	 * Contains 'md5' and 'contentType' for the location data blob.
	 */
	public Map<String, List<String>> getStrAnnotations() {
		return strAnnotations;
	}

	private final Long nodeId;
	private final Long userId;
	private final List<AttachmentData> attachments;
	private final List<LocationData> locations;
	private final Map<String, List<String>> strAnnotations;
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attachments == null) ? 0 : attachments.hashCode());
		result = prime * result
				+ ((locations == null) ? 0 : locations.hashCode());
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		result = prime * result
				+ ((strAnnotations == null) ? 0 : strAnnotations.hashCode());
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
		StorageLocations other = (StorageLocations) obj;
		if (attachments == null) {
			if (other.attachments != null)
				return false;
		} else if (!attachments.equals(other.attachments))
			return false;
		if (locations == null) {
			if (other.locations != null)
				return false;
		} else if (!locations.equals(other.locations))
			return false;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		if (strAnnotations == null) {
			if (other.strAnnotations != null)
				return false;
		} else if (!strAnnotations.equals(other.strAnnotations))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}
}

