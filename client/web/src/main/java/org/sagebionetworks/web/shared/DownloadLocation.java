package org.sagebionetworks.web.shared;

import java.util.Date;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.rpc.IsSerializable;

public class DownloadLocation implements IsSerializable {

	private String name = "default";
	private String annotations;
	private String id;
	private String type;
	private String path;
	private Date creationDate;
	private String parentId;
	private String etag;
	private String md5sum;
	private String uri;
	private String accessControlList;
	private String contentType;
	
	public DownloadLocation() {	
	}

	public DownloadLocation(JSONObject object) {
		String key = null; 
		
		key = "name";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setName(object.get(key).isString().stringValue());		

		key = "annotations";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setAnnotations(object.get(key).isString().stringValue());		
		
		key = "id";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setId(object.get(key).isString().stringValue());		
		
		key = "type";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null) 
				setType(object.get(key).isString().stringValue());					

		key = "path";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null) 
				setPath(object.get(key).isString().stringValue());					
		
		key = "creationDate";
		if(object.containsKey(key)) 
			if(object.get(key).isNumber() != null)
				setCreationDate(new Date(new Double(object.get(key).isNumber().doubleValue()).longValue()));
		
		key = "parentId";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setParentId(object.get(key).isString().stringValue());
		
		key = "etag";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setEtag(object.get(key).isString().stringValue());		
		
		key = "md5sum";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setMd5sum(object.get(key).isString().stringValue());
		
		key = "uri";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setUri(object.get(key).isString().stringValue());
		
		key = "accessControlList";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setAccessControlList(object.get(key).isString().stringValue());					

		key = "contentType";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setContentType(object.get(key).isString().stringValue());					

	}

	public String getMd5sum() {
		return md5sum;
	}

	public void setMd5sum(String md5sum) {
		this.md5sum = md5sum;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public String getAnnotations() {
		return annotations;
	}

	public void setAnnotations(String annotations) {
		this.annotations = annotations;
	}

	public String getAccessControlList() {
		return accessControlList;
	}

	public void setAccessControlList(String accessControlList) {
		this.accessControlList = accessControlList;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
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
				+ ((contentType == null) ? 0 : contentType.hashCode());
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
		DownloadLocation other = (DownloadLocation) obj;
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
		if (contentType == null) {
			if (other.contentType != null)
				return false;
		} else if (!contentType.equals(other.contentType))
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
		return true;
	}

	@Override
	public String toString() {
		return "DownloadLocation [name=" + name + ", annotations="
				+ annotations + ", id=" + id + ", type=" + type + ", path="
				+ path + ", creationDate=" + creationDate + ", parentId="
				+ parentId + ", etag=" + etag + ", md5sum=" + md5sum + ", uri="
				+ uri + ", accessControlList=" + accessControlList
				+ ", contentType=" + contentType + "]";
	}

}
