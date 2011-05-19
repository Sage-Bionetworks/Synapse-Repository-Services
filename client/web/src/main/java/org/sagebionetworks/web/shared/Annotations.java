package org.sagebionetworks.web.shared;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * This is a data transfer object that will be populated from REST JSON.
 * 
 */
public class Annotations implements IsSerializable {

	private Date creationDate;
	private Map<String, List<Date>> dateAnnotations;
	private Map<String, List<Double>> doubleAnnotations;
	private String etag;
	private String id;
	private Map<String, List<Long>> longAnnotations;
	private Map<String, List<String>> stringAnnotations;
	private Map<String, List<byte[]>> blobAnnotations;
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
	
	public Map<String, List<byte[]>> getBlobAnnotations() {
		return blobAnnotations;
	}
	public void setBlobAnnotations(Map<String, List<byte[]>> blobAnnotations) {
		this.blobAnnotations = blobAnnotations;
	}
	/**
	 * Add an an annotation.
	 * @param anno
	 * @param key
	 * @param value must be String, Long, Double, or Date.
	 */
	public void addAnnotation(String key, Object value) {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		// Add the correct type
		if(value instanceof String){
			addStringAnnotation(key, (String) value);
		}else if(value instanceof Date){
			addDateAnnotation(key, (Date)value);
		}else if(value instanceof Long){
			addLongAnnotation(key, (Long) value);
		}else if(value instanceof Double){
			addDoubleAnnotation(key, (Double) value);
		}else if(value instanceof byte[]){
			addBlobAnnotation(key, (byte[]) value);
		}else{
			throw new IllegalArgumentException("Unkown object type: "+value.getClass().getName());
		}
	}
	
	/**
	 * Add a single String annotation.
	 * @param anno
	 * @param key
	 * @param value
	 */
	public void addStringAnnotation(String key, String value) {
		// First get the strings
		Map<String, List<String>> map = getStringAnnotations();
		if(map == null){
			map = new HashMap<String, List<String>>();
			setStringAnnotations(map);
		}
		// Create a new value
		List<String> values = new ArrayList<String>();
		values.add(value);
		map.put(key, values);
	}
	
	public void addBlobAnnotation(String key, byte[] value) {
		// First get the strings
		Map<String, List<byte[]>> map = getBlobAnnotations();
		if(map == null){
			map = new HashMap<String, List<byte[]>>();
			setBlobAnnotations(map);
		}
		// Create a new value
		List<byte[]> values = new ArrayList<byte[]>();
		values.add(value);
		map.put(key, values);
	}
	
	/**
	 * Add a single date annotation.
	 * @param anno
	 * @param key
	 * @param value
	 */
	public void addDateAnnotation(String key, Date value){
		// First get the map
		Map<String, List<Date>> map = getDateAnnotations();
		if(map == null){
			map = new HashMap<String, List<Date>>();
			setDateAnnotations(map);
		}
		// Create a new value
		List<Date> values = new ArrayList<Date>();
		values.add(value);
		map.put(key, values);
	}
	
	/**
	 * Add a single long annotation.
	 * @param anno
	 * @param key
	 * @param value
	 */
	public void addLongAnnotation(String key, Long value){
		// First get the map
		Map<String, List<Long>> map = getLongAnnotations();
		if(map == null){
			map = new HashMap<String, List<Long>>();
			setLongAnnotations(map);
		}
		// Create a new value
		List<Long> values = new ArrayList<Long>();
		values.add(value);
		map.put(key, values);
	}
	
	/**
	 * Add a single double annotation.
	 * @param anno
	 * @param key
	 * @param value
	 */
	public void addDoubleAnnotation(String key, Double value){
		// First get the map
		Map<String, List<Double>> map = getDoubleAnnotations();
		if(map == null){
			map = new HashMap<String, List<Double>>();
			setDoubleAnnotations(map);
		}
		// Create a new value
		List<Double> values = new ArrayList<Double>();
		values.add(value);
		map.put(key, values);
	}

	/**
	 * Find 
	 * @param anno
	 * @param key
	 * @return
	 */
	public Object findFirstAnnotationValue(String key) {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		// Look in each type
		// Double
		Map<String, List<Double>> doubleMap = getDoubleAnnotations();
		if(doubleMap != null){
			List<Double> list = doubleMap.get(key);
			if(list != null && list.size() > 0){
				// Return the first
				return list.get(0);
			}
		}
		// String
		Map<String, List<String>> stringMap = getStringAnnotations();
		if(stringMap != null){
			List<String> list = stringMap.get(key);
			if(list != null && list.size() > 0){
				// Return the first
				return list.get(0);
			}
		}
		// Date
		Map<String, List<Date>> dateMap = getDateAnnotations();
		if(dateMap != null){
			List<Date> list = dateMap.get(key);
			if(list != null && list.size() > 0){
				// Return the first
				return list.get(0);
			}
		}
		// Long
		Map<String, List<Long>> longMap = getLongAnnotations();
		if(longMap != null){
			List<Long> list = longMap.get(key);
			if(list != null && list.size() > 0){
				// Return the first
				return list.get(0);
			}
		}
		// byte
		Map<String, List<byte[]>> byteMap = getBlobAnnotations();
		if(byteMap != null){
			List<byte[]> list = byteMap.get(key);
			if(list != null && list.size() > 0){
				// Return the first
				return list.get(0);
			}
		}
		// It does not exist
		return null;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((blobAnnotations == null) ? 0 : blobAnnotations.hashCode());
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
		Annotations other = (Annotations) obj;
		if (blobAnnotations == null) {
			if (other.blobAnnotations != null)
				return false;
		} else if (!blobAnnotations.equals(other.blobAnnotations))
			return false;
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
		return "Annotations [creationDate=" + creationDate
				+ ", dateAnnotations=" + dateAnnotations
				+ ", doubleAnnotations=" + doubleAnnotations + ", etag=" + etag
				+ ", id=" + id + ", longAnnotations=" + longAnnotations
				+ ", stringAnnotations=" + stringAnnotations
				+ ", blobAnnotations=" + blobAnnotations + ", uri=" + uri + "]";
	}

}
