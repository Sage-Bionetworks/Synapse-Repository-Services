package org.sagebionetworks.web.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Basic information about a column returned from the Search API.
 * 
 * @author jmhill
 *
 */
public class ColumnInfo implements IsSerializable, HeaderData {
	
	/**
	 * These are the base primitive types supported by the API.
	 *
	 */
	public enum Type {
		String,
		Boolean,
		Long,
		Double,
		StringArray,
		BooleanArray,
		LongArray,
		DoubleArray,
	}
	
	private String id;
	private Type type;
	private String displayName;
	private String description;
	
	public ColumnInfo(){}
	
	public ColumnInfo(String id, String type, String displayName,String description) {
		super();
		this.id = id;
		this.type = Type.valueOf(type);
		this.displayName = displayName;
		this.description = description;
	}
	@Override
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getType() {
		return type.name();
	}
	public void setType(String type) {
		this.type = Type.valueOf(type);
	}
	@Override
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	@Override
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public String getSortId() {
		// By default this is the same as the id.
		return id;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result
				+ ((displayName == null) ? 0 : displayName.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		ColumnInfo other = (ColumnInfo) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (displayName == null) {
			if (other.displayName != null)
				return false;
		} else if (!displayName.equals(other.displayName))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	
}
