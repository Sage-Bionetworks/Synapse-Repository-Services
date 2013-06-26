package org.sagebionetworks.javadoc.velocity.schema;

/**
 * All type are either a single type or an array of types.  Any array of types can also be unique
 * which means it is a set (as opposed to a list). If a type has an href, then it is a link to
 * another type.  All types have a display name.
 * 
 * @author John
 *
 */
public class TypeReference {
	
	private boolean isArray;
	private boolean isUnique;
	private String display;
	private String href;
	
	public TypeReference(boolean isArray, boolean isUnique, String display,
			String href) {
		super();
		this.isArray = isArray;
		this.isUnique = isUnique;
		this.display = display;
		this.href = href;
	}
	
	public boolean getIsArray() {
		return isArray;
	}
	public void setIsArray(boolean isArray) {
		this.isArray = isArray;
	}
	public String getDisplay() {
		return display;
	}
	public void setDisplay(String display) {
		this.display = display;
	}
	public String getHref() {
		return href;
	}
	public void setHref(String href) {
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
		result = prime * result + ((display == null) ? 0 : display.hashCode());
		result = prime * result + ((href == null) ? 0 : href.hashCode());
		result = prime * result + (isArray ? 1231 : 1237);
		result = prime * result + (isUnique ? 1231 : 1237);
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
		TypeReference other = (TypeReference) obj;
		if (display == null) {
			if (other.display != null)
				return false;
		} else if (!display.equals(other.display))
			return false;
		if (href == null) {
			if (other.href != null)
				return false;
		} else if (!href.equals(other.href))
			return false;
		if (isArray != other.isArray)
			return false;
		if (isUnique != other.isUnique)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "TypeReference [isArray=" + isArray + ", isUnique=" + isUnique
				+ ", display=" + display + ", href=" + href + "]";
	}


}
