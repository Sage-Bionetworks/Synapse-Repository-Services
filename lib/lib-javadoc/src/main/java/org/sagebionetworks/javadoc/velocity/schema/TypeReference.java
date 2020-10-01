package org.sagebionetworks.javadoc.velocity.schema;

import java.util.Arrays;
import java.util.Objects;

/**
 * All type are either a single type or an array of types or a map of type to type. Any array of types can also be
 * unique which means it is a set (as opposed to a list). If a type has an href, then it is a link to another type. All
 * types have a display name.
 * 
 * @author John
 * 
 */
public class TypeReference {
	
	private String id;
	private boolean isArray;
	private boolean isMap;
	private boolean isUnique;
	private String[] display;
	private String[] href;
	
	public TypeReference(String id, boolean isArray, boolean isUnique, boolean isMap, String[] display, String[] href) {
		this.id = id;
		this.isArray = isArray;
		this.isMap = isMap;
		this.isUnique = isUnique;
		this.display = display;
		this.href = href;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public boolean getIsArray() {
		return isArray;
	}
	public void setIsArray(boolean isArray) {
		this.isArray = isArray;
	}

	public boolean getIsMap() {
		return isMap;
	}

	public void setIsMap(boolean isMap) {
		this.isMap = isMap;
	}

	public String[] getDisplay() {
		return display;
	}

	public void setDisplay(String[] display) {
		this.display = display;
	}

	public String[] getHref() {
		return href;
	}

	public void setHref(String[] href) {
		this.href = href;
	}
	
	public boolean getIsUnique() {
		return isUnique;
	}
	
	public void setIsUnique(boolean isUnique) {
		this.isUnique = isUnique;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(display);
		result = prime * result + Arrays.hashCode(href);
		result = prime * result + Objects.hash(id, isArray, isMap, isUnique);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TypeReference other = (TypeReference) obj;
		return Arrays.equals(display, other.display) && Arrays.equals(href, other.href) && Objects.equals(id, other.id)
				&& isArray == other.isArray && isMap == other.isMap && isUnique == other.isUnique;
	}

	@Override
	public String toString() {
		return "TypeReference [id=" + id + ", isArray=" + isArray + ", isMap=" + isMap + ", isUnique=" + isUnique
				+ ", display=" + Arrays.toString(display) + ", href=" + Arrays.toString(href) + "]";
	}

	
}
