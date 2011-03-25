package org.sagebionetworks.web.shared;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * This is a data transfer object that will be populated from REST JSON.
 * 
 */
public class DatasetAnnotations implements IsSerializable {

	private Date creationDate;
	private Map<String, List<Date>> dateAnnotations;
	private Map<String, List<Double>> doubleAnnotations;
	private String etag;
	private String id;
	private Map<String, List<Long>> longAnnotations;
	private Map<String, List<String>> stringAnnotations;
	private String uri;
	
	
	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	public Map<String, List<Date>> getDateAnnotations() {
		return dateAnnotations;
	}
	public void setDateAnnotations(Map<String, List<Date>> dateAnnotations) {
		this.dateAnnotations = dateAnnotations;
	}
	public Map<String, List<Double>> getDoubleAnnotations() {
		return doubleAnnotations;
	}
	public void setDoubleAnnotations(Map<String, List<Double>> doubleAnnotations) {
		this.doubleAnnotations = doubleAnnotations;
	}
	public String getEtag() {
		return etag;
	}
	public void setEtag(String etag) {
		this.etag = etag;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Map<String, List<Long>> getLongAnnotations() {
		return longAnnotations;
	}
	public void setLongAnnotations(Map<String, List<Long>> longAnnotations) {
		this.longAnnotations = longAnnotations;
	}
	public Map<String, List<String>> getStringAnnotations() {
		return stringAnnotations;
	}
	public void setStringAnnotations(Map<String, List<String>> stringAnnotations) {
		this.stringAnnotations = stringAnnotations;
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
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
				+ ((doubleAnnotations == null) ? 0 : doubleAnnotations
						.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((longAnnotations == null) ? 0 : longAnnotations.hashCode());
		result = prime
				* result
				+ ((stringAnnotations == null) ? 0 : stringAnnotations
						.hashCode());
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
		DatasetAnnotations other = (DatasetAnnotations) obj;
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
		if (doubleAnnotations == null) {
			if (other.doubleAnnotations != null)
				return false;
		} else if (!doubleAnnotations.equals(other.doubleAnnotations))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (longAnnotations == null) {
			if (other.longAnnotations != null)
				return false;
		} else if (!longAnnotations.equals(other.longAnnotations))
			return false;
		if (stringAnnotations == null) {
			if (other.stringAnnotations != null)
				return false;
		} else if (!stringAnnotations.equals(other.stringAnnotations))
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
		return "DatasetAnnotations [creationDate=" + creationDate
				+ ", dateAnnotations=" + dateAnnotations
				+ ", doubleAnnotations=" + doubleAnnotations + ", etag=" + etag
				+ ", id=" + id + ", longAnnotations=" + longAnnotations
				+ ", stringAnnotations=" + stringAnnotations + ", uri=" + uri
				+ "]";
	}

}
