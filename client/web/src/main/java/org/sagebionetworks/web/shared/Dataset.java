package org.sagebionetworks.web.shared;

import java.util.Date;

import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * This is a data transfer object that will be populated from REST JSON.
 * 
 */
public class Dataset implements IsSerializable {
	private String name;
	private String annotations;
	private String id;
	private String version;
	private String description;
	private String status;
	private Date creationDate;
	private String parentId;
	private String etag;
	private String uri;
	private String locations;
	private String accessControlList;
	private String creator;
	private Date releaseDate;
	private Boolean hasExpressionData = false;
	private Boolean hasGeneticData = false;
	private Boolean hasClinicalData = false;
	private String layers;

	/**
	 * Default constructor is required
	 */
	public Dataset() {

	}

	public Dataset(JSONObject object) {
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
		
		key = "version";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setVersion(object.get(key).isString().stringValue());
		
		key = "description";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setDescription(object.get(key).isString().stringValue());		
		
		key = "status";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setStatus(object.get(key).isString().stringValue());
		
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
		
		key = "uri";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setUri(object.get(key).isString().stringValue());

		key = "locations";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setLocations(object.get(key).isString().stringValue());		
				
		key = "accessControlList";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setAccessControlList(object.get(key).isString().stringValue());					
		
		key = "creator";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setCreator(object.get(key).isString().stringValue());					
		
		key = "releaseDate";
		if(object.containsKey(key)) 
			if(object.get(key).isNumber() != null)
				setReleaseDate(new Date(new Double(object.get(key).isNumber().doubleValue()).longValue()));
		
		key = "hasExpressionData";
		if(object.containsKey(key)) 
			if(object.get(key).isBoolean() != null)
				setHasExpressionData(object.get(key).isBoolean().booleanValue());
		
		key = "hasGeneticData";
		if(object.containsKey(key)) 
			if(object.get(key).isBoolean() != null)
				setHasGeneticData(object.get(key).isBoolean().booleanValue());
		
		key = "hasClinicalData";
		if(object.containsKey(key)) 
			if(object.get(key).isBoolean() != null)
				setHasClinicalData(object.get(key).isBoolean().booleanValue());
	}
	
	public String toJson() {
		JSONObject object = new JSONObject();
		
		object.put("name", new JSONString(getName()));
		object.put("annotations", new JSONString(getAnnotations()));
		object.put("id", new JSONString(getId()));
		object.put("version", new JSONString(getVersion()));
		object.put("description", new JSONString(getDescription()));
		object.put("status", new JSONString(getStatus()));		
		object.put("creationDate", new JSONNumber(getCreationDate().getTime()));
		object.put("parentId", new JSONString(getParentId()));
		object.put("etag", new JSONString(getEtag()));
		object.put("uri", new JSONString(getUri()));
		object.put("locations", new JSONString(getLocations()));
		object.put("accessControlList", new JSONString(getAccessControlList()));
		object.put("creator", new JSONString(getCreator()));
		object.put("releaseDate", new JSONNumber(getReleaseDate().getTime()));
		object.put("hasExpressionData", JSONBoolean.getInstance(getHasExpressionData()));
		object.put("hasGeneticData", JSONBoolean.getInstance(getHasGeneticData()));
		object.put("hasClinicalData", JSONBoolean.getInstance(getHasClinicalData()));
		object.put("layers", new JSONString(getLayers()));
		
		return object.toString();
	}
	
	public String getUri() {
		return uri;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getAnnotations() {
		return annotations;
	}

	public String getAccessControlList() {
		return accessControlList;
	}

	public void setAccessControlList(String accessControlList) {
		this.accessControlList = accessControlList;
	}

	public void setAnnotations(String annotations) {
		this.annotations = annotations;
	}

	public String getLocations() {
		return locations;
	}

	public void setLocations(String locations) {
		this.locations = locations;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(Date releaseDate) {
		this.releaseDate = releaseDate;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	public Boolean getHasExpressionData() {
		return hasExpressionData;
	}

	public void setHasExpressionData(Boolean hasExpressionData) {
		this.hasExpressionData = hasExpressionData;
	}

	public Boolean getHasGeneticData() {
		return hasGeneticData;
	}

	public void setHasGeneticData(Boolean hasGeneticData) {
		this.hasGeneticData = hasGeneticData;
	}

	public Boolean getHasClinicalData() {
		return hasClinicalData;
	}

	public void setHasClinicalData(Boolean hasClinicalData) {
		this.hasClinicalData = hasClinicalData;
	}

	public String getLayers() {
		return layers;
	}

	public void setLayers(String layers) {
		this.layers = layers;
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
		result = prime * result + ((creator == null) ? 0 : creator.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result
				+ ((hasClinicalData == null) ? 0 : hasClinicalData.hashCode());
		result = prime
				* result
				+ ((hasExpressionData == null) ? 0 : hasExpressionData
						.hashCode());
		result = prime * result
				+ ((hasGeneticData == null) ? 0 : hasGeneticData.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((layers == null) ? 0 : layers.hashCode());
		result = prime * result
				+ ((locations == null) ? 0 : locations.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
		result = prime * result
				+ ((releaseDate == null) ? 0 : releaseDate.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		Dataset other = (Dataset) obj;
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
		if (creator == null) {
			if (other.creator != null)
				return false;
		} else if (!creator.equals(other.creator))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (hasClinicalData == null) {
			if (other.hasClinicalData != null)
				return false;
		} else if (!hasClinicalData.equals(other.hasClinicalData))
			return false;
		if (hasExpressionData == null) {
			if (other.hasExpressionData != null)
				return false;
		} else if (!hasExpressionData.equals(other.hasExpressionData))
			return false;
		if (hasGeneticData == null) {
			if (other.hasGeneticData != null)
				return false;
		} else if (!hasGeneticData.equals(other.hasGeneticData))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (layers == null) {
			if (other.layers != null)
				return false;
		} else if (!layers.equals(other.layers))
			return false;
		if (locations == null) {
			if (other.locations != null)
				return false;
		} else if (!locations.equals(other.locations))
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
		if (releaseDate == null) {
			if (other.releaseDate != null)
				return false;
		} else if (!releaseDate.equals(other.releaseDate))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Dataset [id=" + id + ", uri=" + uri + ", etag=" + etag
				+ ", name=" + name + ", description=" + description
				+ ", creator=" + creator + ", creationDate=" + creationDate
				+ ", status=" + status + ", releaseDate=" + releaseDate
				+ ", version=" + version + ", annotations=" + annotations
				+ ", locations=" + locations + ", layers=" + layers
				+ ", hasExpressionData=" + hasExpressionData
				+ ", hasGeneticData=" + hasGeneticData + ", hasClinicalData="
				+ hasClinicalData + ", parentId=" + parentId
				+ ", accessControlList=" + accessControlList + "]";
	}

	
}
