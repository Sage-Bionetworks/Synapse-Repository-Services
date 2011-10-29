package org.sagebionetworks.web.shared;

/**
 * This is the first version of a triple that can be used to describe the
 * execution environment of a step in an analysis. I suspect this will change
 * over time as it moves from being merely human-readable to information
 * programmatically useful.
 * 
 * @author deflaux
 * 
 */
public class EnvironmentDescriptor {
	private String type;
	private String name;
	private String quantifier;
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the quantifier
	 */
	public String getQuantifier() {
		return quantifier;
	}
	/**
	 * @param quantifier the quantifier to set
	 */
	public void setQuantifier(String quantifier) {
		this.quantifier = quantifier;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((quantifier == null) ? 0 : quantifier.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EnvironmentDescriptor other = (EnvironmentDescriptor) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (quantifier == null) {
			if (other.quantifier != null)
				return false;
		} else if (!quantifier.equals(other.quantifier))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "EnvironmentDescriptor [name=" + name + ", quantifier="
				+ quantifier + ", type=" + type + "]";
	}
}
