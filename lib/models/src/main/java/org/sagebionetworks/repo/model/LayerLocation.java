package org.sagebionetworks.repo.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author deflaux
 *
 */
@XmlRootElement
public class LayerLocation {

	private String type;
	private String path;
	private String md5sum;
	// TODO probably need a collection of string properties
	
	/**
	 * Allowable location type names
	 * 
	 * TODO do we want to encode allowable values here?
	 */
	public enum LocationTypeNames {
		/**
		 * 
		 */
		awss3, 
		/**
		 * 
		 */
		awsebs, 
		/**
		 * 
		 */
		sage;
	}

	/**
	 * Default constructor
	 */
	public LayerLocation() {
	}

	/**
	 * @param type
	 * @param path
	 */
	public LayerLocation(String type, String path, String md5sum) {
		super();
		this.type = type;
		this.path = path;
		this.md5sum = md5sum;
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

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((md5sum == null) ? 0 : md5sum.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
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
		LayerLocation other = (LayerLocation) obj;
		if (md5sum == null) {
			if (other.md5sum != null)
				return false;
		} else if (!md5sum.equals(other.md5sum))
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
		return true;
	}


}
