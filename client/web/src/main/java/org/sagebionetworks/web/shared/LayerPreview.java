package org.sagebionetworks.web.shared;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * This is a data transfer object that will be populated from REST JSON.
 * 
 */
public class LayerPreview implements IsSerializable {
		
	private String name;
	private String annotations;
	private String id;
	private String etag;
	private List<String> headers;
	private Date creationDate;
	private String parentId;	
	private String previewString;	
	private String uri;
	private String accessControlList;
	private List<Map<String,String>> rows;
	
	/**
	 * Default constructor is required
	 */
	public LayerPreview() {

	}

	public LayerPreview(JSONObject layerPreviewObj) {
		if(layerPreviewObj != null) {
		String key = null;					
		
		key = "name";
		if(layerPreviewObj.containsKey(key)) 
			if(layerPreviewObj.get(key).isString() != null)
				setName(layerPreviewObj.get(key).isString().stringValue());		
						
		key = "annotations";
		if(layerPreviewObj.containsKey(key)) 
			if(layerPreviewObj.get(key).isString() != null)
				setAnnotations(layerPreviewObj.get(key).isString().stringValue());		
		
		key = "id";
		if(layerPreviewObj.containsKey(key)) 
			if(layerPreviewObj.get(key).isString() != null)
				setId(layerPreviewObj.get(key).isString().stringValue());		
		
		key = "etag";
		if(layerPreviewObj.containsKey(key)) 
			if(layerPreviewObj.get(key).isString() != null)
				setEtag(layerPreviewObj.get(key).isString().stringValue());		

		key = "headers";
		if(layerPreviewObj.containsKey(key)) 
			if(layerPreviewObj.get(key).isArray() != null) {
				JSONArray list = layerPreviewObj.get(key).isArray();
				List<String> stringList = new ArrayList<String>();
				for(int i=0; i<list.size(); i++) {
					stringList.add(list.get(i).isString().stringValue());
				}
				setHeaders(stringList);
			}
		
		key = "creationDate";
		if(layerPreviewObj.containsKey(key)) 
			if(layerPreviewObj.get(key).isNumber() != null)
				setCreationDate(new Date(new Double(layerPreviewObj.get(key).isNumber().doubleValue()).longValue()));
		
		key = "parentId";
		if(layerPreviewObj.containsKey(key)) 
			if(layerPreviewObj.get(key).isString() != null)
				setParentId(layerPreviewObj.get(key).isString().stringValue());
		
		key = "previewString";
		if(layerPreviewObj.containsKey(key)) 
			if(layerPreviewObj.get(key).isString() != null)
				setPreviewString(layerPreviewObj.get(key).isString().stringValue());

		key = "uri";
		if(layerPreviewObj.containsKey(key)) 
			if(layerPreviewObj.get(key).isString() != null)
				setUri(layerPreviewObj.get(key).isString().stringValue());

		key = "accessControlList";
		if(layerPreviewObj.containsKey(key)) 
			if(layerPreviewObj.get(key).isString() != null)
				setAccessControlList(layerPreviewObj.get(key).isString().stringValue());					

		key = "rows";
		if(layerPreviewObj.containsKey(key)) 
			if(layerPreviewObj.get(key).isArray() != null) {
				JSONArray list = layerPreviewObj.get(key).isArray();
				List<Map<String,String>> mapList = new ArrayList<Map<String,String>>();
				for(int i=0; i<list.size(); i++) {
					JSONObject mapObj = list.get(i).isObject();
					if(mapObj != null && mapObj.containsKey("cells")) {
						Map<String,String> map = new HashMap<String, String>();
						JSONArray cellsList = mapObj.get("cells").isArray();
						List<String> headers = getHeaders();
						if(cellsList != null && headers != null) {
							for(int n=0; n<cellsList.size(); n++) {
								if(n<headers.size()) {
									map.put(headers.get(n), cellsList.get(n).isString().stringValue());
								}
							}
						}
						mapList.add(map);
					}
				}
				setRows(mapList);
			}
		}		
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

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public List<String> getHeaders() {
		return headers;
	}

	public void setHeaders(List<String> headers) {
		this.headers = headers;
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

	public String getPreviewString() {
		return previewString;
	}

	public void setPreviewString(String previewString) {
		this.previewString = previewString;
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

	public List<Map<String, String>> getRows() {
		return rows;
	}

	public void setRows(List<Map<String, String>> rows) {
		this.rows = rows;
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
		result = prime * result + ((headers == null) ? 0 : headers.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
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
		LayerPreview other = (LayerPreview) obj;
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
		if (headers == null) {
			if (other.headers != null)
				return false;
		} else if (!headers.equals(other.headers))
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

	@Override
	public String toString() {
		return "LayerPreview [name=" + name + ", annotations=" + annotations
				+ ", id=" + id + ", etag=" + etag + ", headers=" + headers
				+ ", creationDate=" + creationDate + ", parentId=" + parentId
				+ ", previewString=" + previewString + ", uri=" + uri
				+ ", accessControlList=" + accessControlList + ", rows=" + rows
				+ "]";
	}



}
