package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.attachment.AttachmentData;

/**
 * Storage location data from unpacked blobs.
 *
 * @author ewu
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
}
