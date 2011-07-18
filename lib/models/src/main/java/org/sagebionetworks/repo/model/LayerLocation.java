package org.sagebionetworks.repo.model;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author deflaux
 *
 */
@XmlRootElement
public class LayerLocation implements Nodeable, Versionable {

	private String id;
	private String uri;
	private String etag;
	private String name = "default";
	private Date creationDate;
	private String parentId;
	private String type;
	private String path;
	private String md5sum;
	@TransientField
	private String annotations;
	@TransientField
	private String accessControlList;
	private Long versionNumber;
	private String versionComment;
	private String versionLabel;
	@TransientField
	private String versions;
	@TransientField
	private String versionUrl;
	
	// TODO probably need a collection of string properties
	
	/**
	 * Allowable location type names
	 * 
	 * TODO do we want to encode allowable values here?
	 */
	public enum LocationTypeNames {
		/**
		 * Synapse-controlled locations in S3
		 */
		awss3, 
		/**
		 * AWS EBS snapshot ids
		 */
		awsebs, 
		/**
		 * Sage shared server file path
		 */
		sage,
		/**
		 * Externally hosted paths
		 */
		external;
	}

	/**
	 * Default constructor
	 */
	public LayerLocation() {
	}

	/**
	 * @param type
	 * @param path
	 * @param md5sum 
	 */
	public LayerLocation(String type, String path, String md5sum) {
		super();
		this.type = type;
		this.path = path;
		this.md5sum = md5sum;
	}
	
	public String getAccessControlList() {
		return accessControlList;
	}

	public void setAccessControlList(String accessControlList) {
		this.accessControlList = accessControlList;
	}

	public String getAnnotations() {
		return annotations;
	}

	public void setAnnotations(String annotations) {
		this.annotations = annotations;
	}

	/**
	 * @param type
	 * @throws InvalidModelException
	 */
	public void setType(String type) throws InvalidModelException {
        try {
        	LocationTypeNames.valueOf(type);
        } catch( IllegalArgumentException e ) {
        	StringBuilder helpfulErrorMessage = new StringBuilder("'type' must be one of:");
        	for(LocationTypeNames name : LocationTypeNames.values()) {
        		helpfulErrorMessage.append(" ").append(name);
        	}
        	throw new InvalidModelException(helpfulErrorMessage.toString());
        }
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the type of this location
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param path
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Note that one must look at the type to know how to use the path
	 * 
	 * @return the path for this location
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param md5sum the md5sum to set
	 */
	public void setMd5sum(String md5sum) {
		this.md5sum = md5sum;
	}

	/**
	 * @return the md5sum
	 */
	public String getMd5sum() {
		return md5sum;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public Long getVersionNumber() {
		return versionNumber;
	}

	public void setVersionNumber(Long versionNumber) {
		this.versionNumber = versionNumber;
	}

	public String getVersionComment() {
		return versionComment;
	}

	public void setVersionComment(String versionComment) {
		this.versionComment = versionComment;
	}

	public String getVersionLabel() {
		return versionLabel;
	}

	public void setVersionLabel(String versionLabel) {
		this.versionLabel = versionLabel;
	}

	public String getVersions() {
		return versions;
	}

	public void setVersions(String versions) {
		this.versions = versions;
	}

	public String getVersionUrl() {
		return versionUrl;
	}

	public void setVersionUrl(String versionUrl) {
		this.versionUrl = versionUrl;
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
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((md5sum == null) ? 0 : md5sum.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		result = prime * result
				+ ((versionComment == null) ? 0 : versionComment.hashCode());
		result = prime * result
				+ ((versionLabel == null) ? 0 : versionLabel.hashCode());
		result = prime * result
				+ ((versionNumber == null) ? 0 : versionNumber.hashCode());
		result = prime * result
				+ ((versionUrl == null) ? 0 : versionUrl.hashCode());
		result = prime * result
				+ ((versions == null) ? 0 : versions.hashCode());
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
		LayerLocation other = (LayerLocation) obj;
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (md5sum == null) {
			if (other.md5sum != null)
				return false;
		} else if (!md5sum.equals(other.md5sum))
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
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		if (versionComment == null) {
			if (other.versionComment != null)
				return false;
		} else if (!versionComment.equals(other.versionComment))
			return false;
		if (versionLabel == null) {
			if (other.versionLabel != null)
				return false;
		} else if (!versionLabel.equals(other.versionLabel))
			return false;
		if (versionNumber == null) {
			if (other.versionNumber != null)
				return false;
		} else if (!versionNumber.equals(other.versionNumber))
			return false;
		if (versionUrl == null) {
			if (other.versionUrl != null)
				return false;
		} else if (!versionUrl.equals(other.versionUrl))
			return false;
		if (versions == null) {
			if (other.versions != null)
				return false;
		} else if (!versions.equals(other.versions))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "LayerLocation [id=" + id + ", uri=" + uri + ", etag=" + etag
				+ ", name=" + name + ", creationDate=" + creationDate
				+ ", parentId=" + parentId + ", type=" + type + ", path="
				+ path + ", md5sum=" + md5sum + ", annotations=" + annotations
				+ ", accessControlList=" + accessControlList
				+ ", versionNumber=" + versionNumber + ", versionComment="
				+ versionComment + ", versionLabel=" + versionLabel
				+ ", versions=" + versions + ", versionUrl=" + versionUrl + "]";
	}

}
