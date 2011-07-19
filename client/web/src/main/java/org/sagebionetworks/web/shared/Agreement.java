package org.sagebionetworks.web.shared;

import java.util.Date;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * This is a data transfer object that will be populated from REST JSON.
 * 
 */
public class Agreement implements IsSerializable {
	private String name;	
	private String annotations;
	private String id;
	private Date creationDate;
	private String etag;
	private String createdBy;
	private String datasetId;
	private Integer datasetVersionNumber;
	private String eulaId;
	private Integer eulaVersionNumber;
	private String parentId;
	private String uri;
	private String accessControlList;

	public Agreement() {		
	}
	
	/**
	 * Default constructor is required
	 * @param obj JSONObject of the Project object
	 */
	public Agreement(JSONObject object) {
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
				
		key = "creationDate";
		if(object.containsKey(key)) 
			if(object.get(key).isNumber() != null)
				setCreationDate(new Date(new Double(object.get(key).isNumber().doubleValue()).longValue()));

		key = "etag";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setEtag(object.get(key).isString().stringValue());		
		
		key = "createdBy";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setCreatedBy(object.get(key).isString().stringValue());		
		
		key = "datasetId";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setDatasetId(object.get(key).isString().stringValue());		
		
		key = "datasetVersionNumber";
		if(object.containsKey(key)) 
			if(object.get(key).isNumber() != null)
				setDatasetVersionNumber(((Double)object.get(key).isNumber().doubleValue()).intValue());		

		key = "eulaId";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setEulaId(object.get(key).isString().stringValue());		
		
		key = "eulaVersionNumber";
		if(object.containsKey(key)) 
			if(object.get(key).isNumber() != null)
				setEulaVersionNumber(((Double)object.get(key).isNumber().doubleValue()).intValue());		
		
		key = "parentId";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setParentId(object.get(key).isString().stringValue());

		key = "uri";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setUri(object.get(key).isString().stringValue());

		key = "accessControlList";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setAccessControlList(object.get(key).isString().stringValue());					
	}

	public String toJson() {
		JSONObject object = new JSONObject();
		
		if(getName() != null) object.put("name", new JSONString(getName()));
		if(getAnnotations() != null) object.put("annotations", new JSONString(getAnnotations()));
		if(getId() != null) object.put("id", new JSONString(getId()));
		if(getCreationDate() != null) object.put("creationDate", new JSONNumber(getCreationDate().getTime()));
		if(getEtag() != null) object.put("etag", new JSONString(getEtag()));
		if(getCreatedBy() != null) object.put("createdBy", new JSONString(getCreatedBy()));
		if(getDatasetId() != null) object.put("datasetId", new JSONString(getDatasetId()));
		if(getDatasetVersionNumber() != null) object.put("datasetVersionNumber", new JSONNumber(getDatasetVersionNumber()));
		if(getEulaId() != null) object.put("eulaId", new JSONString(getEulaId()));
		if(getEulaVersionNumber() != null) object.put("eulaVersionNumber", new JSONNumber(getEulaVersionNumber()));
		if(getParentId() != null) object.put("parentId", new JSONString(getParentId()));
		if(getUri() != null) object.put("uri", new JSONString(getUri()));
		if(getAccessControlList() != null) object.put("accessControlList", new JSONString(getAccessControlList()));
		
		return object.toString();
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAnnotations() {
		return annotations;
	}

	public void setAnnotations(String annotations) {
		this.annotations = annotations;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}	
	
	public String getDatasetId() {
		return datasetId;
	}

	public void setDatasetId(String datasetId) {
		this.datasetId = datasetId;
	}

	public Integer getDatasetVersionNumber() {
		return datasetVersionNumber;
	}

	public void setDatasetVersionNumber(Integer datasetVersionNumber) {
		this.datasetVersionNumber = datasetVersionNumber;
	}

	public String getEulaId() {
		return eulaId;
	}

	public void setEulaId(String eulaId) {
		this.eulaId = eulaId;
	}

	public Integer getEulaVersionNumber() {
		return eulaVersionNumber;
	}

	public void setEulaVersionNumber(Integer eulaVersionNumber) {
		this.eulaVersionNumber = eulaVersionNumber;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
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
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result
				+ ((datasetId == null) ? 0 : datasetId.hashCode());
		result = prime
				* result
				+ ((datasetVersionNumber == null) ? 0 : datasetVersionNumber
						.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((eulaId == null) ? 0 : eulaId.hashCode());
		result = prime
				* result
				+ ((eulaVersionNumber == null) ? 0 : eulaVersionNumber
						.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
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
		Agreement other = (Agreement) obj;
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
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
			return false;
		if (datasetId == null) {
			if (other.datasetId != null)
				return false;
		} else if (!datasetId.equals(other.datasetId))
			return false;
		if (datasetVersionNumber == null) {
			if (other.datasetVersionNumber != null)
				return false;
		} else if (!datasetVersionNumber.equals(other.datasetVersionNumber))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (eulaId == null) {
			if (other.eulaId != null)
				return false;
		} else if (!eulaId.equals(other.eulaId))
			return false;
		if (eulaVersionNumber == null) {
			if (other.eulaVersionNumber != null)
				return false;
		} else if (!eulaVersionNumber.equals(other.eulaVersionNumber))
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
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Agreement [name=" + name + ", annotations=" + annotations
				+ ", id=" + id + ", creationDate=" + creationDate + ", etag="
				+ etag + ", createdBy=" + createdBy + ", datasetId="
				+ datasetId + ", datasetVersionNumber=" + datasetVersionNumber
				+ ", eulaId=" + eulaId + ", eulaVersionNumber="
				+ eulaVersionNumber + ", parentId=" + parentId + ", uri=" + uri
				+ ", accessControlList=" + accessControlList + "]";
	}

}
