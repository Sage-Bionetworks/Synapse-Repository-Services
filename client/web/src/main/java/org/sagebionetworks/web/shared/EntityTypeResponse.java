package org.sagebionetworks.web.shared;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.rpc.IsSerializable;

public class EntityTypeResponse implements IsSerializable {

	private String name = "default";
	private String id;
	private String type;

	public EntityTypeResponse() {	
	}

	public EntityTypeResponse(JSONObject object) {
		String key = null; 
		
		key = "name";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setName(object.get(key).isString().stringValue());		
	
		key = "id";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setId(object.get(key).isString().stringValue());		
		
		key = "type";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null) 
				setType(object.get(key).isString().stringValue());					

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		EntityTypeResponse other = (EntityTypeResponse) obj;
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
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "EntityTypeResponse [name=" + name + ", id=" + id + ", type="
				+ type + "]";
	}


}
