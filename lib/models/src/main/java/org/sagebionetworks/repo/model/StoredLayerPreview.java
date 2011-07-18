package org.sagebionetworks.repo.model;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This DTO returns a subset of the fields contained in a persisted layer
 * 
 * @author deflaux
 *
 */
@XmlRootElement
public class StoredLayerPreview implements Nodeable {
	
	private String id;
	private String uri;
	private String etag;
	private String name = "default";
	private Date creationDate;
	private String parentId;
	private byte[] previewBlob;
	@TransientField
	private String previewString;
	@TransientField
	private String annotations;
	@TransientField
	private String accessControlList;
	@TransientField
	private String[] headers;
	@TransientField
	private List<Map<String, String>> rows;

	/** 
	 * The following members are set by the service layer and should not be persisted.
	 */
	
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}
	
	/**
	 * @param uri the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	/**
	 * @return the etag
	 */
	public String getEtag() {
		return etag;
	}
	
	/**
	 * @param etag the etag to set
	 */
	public void setEtag(String etag) {
		this.etag = etag;
	}

	/**
	 * @return the creationDate
	 */
	public Date getCreationDate() {
		return creationDate;
	}

	/**
	 * @param creationDate the creationDate to set
	 */
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public byte[] getPreviewBlob() {
		return previewBlob;
	}

	public void setPreviewBlob(byte[] previewBlob) {
		this.previewBlob = previewBlob;
	}
	public String getAnnotations() {
		return annotations;
	}

	public void setAnnotations(String annotations) {
		this.annotations = annotations;
	}
	
	public String getPreviewString() {
		return previewString;
	}

	public void setPreviewString(String previewString) {
		this.previewString = previewString;
	}

	public String getAccessControlList() {
		return accessControlList;
	}

	public void setAccessControlList(String accessControlList) {
		this.accessControlList = accessControlList;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((accessControlList == null) ? 0 : accessControlList
						.hashCode());
		result = prime * result
				+ ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result
				+ ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + Arrays.hashCode(headers);
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
		result = prime * result + Arrays.hashCode(previewBlob);
		result = prime * result
				+ ((previewString == null) ? 0 : previewString.hashCode());
		result = prime * result + ((rows == null) ? 0 : rows.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
		StoredLayerPreview other = (StoredLayerPreview) obj;
		if (accessControlList == null) {
			if (other.accessControlList != null)
				return false;
		} else if (!accessControlList.equals(other.accessControlList))
			return false;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (!Arrays.equals(headers, other.headers))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		if (!Arrays.equals(previewBlob, other.previewBlob))
			return false;
		if (previewString == null) {
			if (other.previewString != null)
				return false;
		} else if (!previewString.equals(other.previewString))
			return false;
		if (rows == null) {
			if (other.rows != null)
				return false;
		} else if (!rows.equals(other.rows))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	public String[] getHeaders() {
		return headers;
	}

	public void setHeaders(String[] headers) {
		this.headers = headers;
	}

	public List<Map<String, String>> getRows() {
		return rows;
	}

	public void setRows(List<Map<String, String>> rows) {
		this.rows = rows;
	}

	@Override
	public String toString() {
		return "StoredLayerPreview [id=" + id + ", uri=" + uri + ", etag="
				+ etag + ", name=" + name + ", creationDate=" + creationDate
				+ ", parentId=" + parentId + ", previewBlob="
				+ Arrays.toString(previewBlob) + ", previewString="
				+ previewString + ", annotations=" + annotations
				+ ", accessControlList=" + accessControlList + ", headers="
				+ Arrays.toString(headers) + ", rows=" + rows + "]";
	}

}
