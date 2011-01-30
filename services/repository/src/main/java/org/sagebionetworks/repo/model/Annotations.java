package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author bhoff
 * 
 */
@XmlRootElement
public class Annotations implements Base {
	private String id; // for its parent entity
	private String uri;
	private String etag;
	private Date creationDate;
	private Map<String, Collection<String>> stringAnnotations;
	private Map<String, Collection<Float>> floatAnnotations;
	private Map<String, Collection<Date>> dateAnnotations;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * @param uri
	 *            the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * @return the etag
	 */
	public String getEtag() {
		return etag;
	}

	/**
	 * @param etag
	 *            the etag to set
	 */
	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Map<String, Collection<String>> getStringAnnotations() {
		return stringAnnotations;
	}

	public void setStringAnnotations(
			Map<String, Collection<String>> stringAnnotations) {
		this.stringAnnotations = stringAnnotations;
	}

	public Map<String, Collection<Float>> getFloatAnnotations() {
		return floatAnnotations;
	}

	public void setFloatAnnotations(
			Map<String, Collection<Float>> floatAnnotations) {
		this.floatAnnotations = floatAnnotations;
	}

	public Map<String, Collection<Date>> getDateAnnotations() {
		return dateAnnotations;
	}

	public void setDateAnnotations(Map<String, Collection<Date>> dateAnnotations) {
		this.dateAnnotations = dateAnnotations;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result
				+ ((dateAnnotations == null) ? 0 : dateAnnotations.hashCode());
		result = prime
				* result
				+ ((floatAnnotations == null) ? 0 : floatAnnotations.hashCode());
		result = prime
				* result
				+ ((stringAnnotations == null) ? 0 : stringAnnotations
						.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
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
		Annotations other = (Annotations) obj;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
			return false;
		if (dateAnnotations == null) {
			if (other.dateAnnotations != null)
				return false;
		} else if (!dateAnnotations.equals(other.dateAnnotations))
			return false;
		if (floatAnnotations == null) {
			if (other.floatAnnotations != null)
				return false;
		} else if (!floatAnnotations.equals(other.floatAnnotations))
			return false;
		if (stringAnnotations == null) {
			if (other.stringAnnotations != null)
				return false;
		} else if (!stringAnnotations.equals(other.stringAnnotations))
			return false;
		return true;
	}

}
