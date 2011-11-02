package org.sagebionetworks.web.shared;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * This is a data transfer object that will be populated from REST JSON.
 * 
 */
public class Annotations implements IsSerializable {

	private String id;
	private Date creationDate;
	private String etag;
	private Map<String, List<String>> stringAnnotations;
	private Map<String, List<Long>> longAnnotations;
	private Map<String, List<Date>> dateAnnotations;
	private Map<String, List<Double>> doubleAnnotations;
	private Map<String, List<byte[]>> blobAnnotations;
	private String uri;
	
	public Annotations() {		
		stringAnnotations = new HashMap<String, List<String>>();
		longAnnotations = new HashMap<String, List<Long>>();
		dateAnnotations = new HashMap<String, List<Date>>();
		doubleAnnotations = new HashMap<String, List<Double>>();
		blobAnnotations = new HashMap<String, List<byte[]>>();
	}
	
	public Annotations(JSONObject object) {
		//if(object == null) object = new JSONObject();
		if(object == null) return;
		
		
		String key = null; 
		
		key = "id";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setId(object.get(key).isString().stringValue());		
		
		key = "creationDate";
		if(object.containsKey(key)) 
			if(object.get(key).isNumber() != null)
				setCreationDate(new Date(new Double(object.get(key).isNumber().doubleValue()).longValue()));
		
		key = "etag";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setEtag(object.get(key).isString().stringValue());		

		key = "stringAnnotations";
		setStringAnnotations(new HashMap<String, List<String>>());
		if(object.containsKey(key)) {
			if(object.get(key).isObject() != null) {
				JSONObject annotationObj = object.get(key).isObject();
				for(String annotationKey : annotationObj.keySet()) {
					if(annotationObj.get(annotationKey).isArray() != null) {
						JSONArray valueArray = annotationObj.get(annotationKey).isArray();
						for(int i=0; i<valueArray.size(); i++) {
							if(valueArray.get(i).isString() != null)								
								this.addStringAnnotation(annotationKey, valueArray.get(i).isString().stringValue());
						}
					}
				}
			}
		}
		
		key = "longAnnotations";
		setLongAnnotations(new HashMap<String, List<Long>>());
		if(object.containsKey(key)) {
			if(object.get(key).isObject() != null) {
				JSONObject annotationObj = object.get(key).isObject();
				for(String annotationKey : annotationObj.keySet()) {
					if(annotationObj.get(annotationKey).isArray() != null) {
						JSONArray valueArray = annotationObj.get(annotationKey).isArray();
						for(int i=0; i<valueArray.size(); i++) {
							if(valueArray.get(i).isNumber() != null)								
								this.addLongAnnotation(annotationKey, new Double(valueArray.get(i).isNumber().doubleValue()).longValue());
						}
					}
				}
			}
		}

		key = "dateAnnotations";
		setDateAnnotations(new HashMap<String, List<Date>>());
		if(object.containsKey(key)) {
			if(object.get(key).isObject() != null) {
				JSONObject annotationObj = object.get(key).isObject();
				for(String annotationKey : annotationObj.keySet()) {
					if(annotationObj.get(annotationKey).isArray() != null) {
						JSONArray valueArray = annotationObj.get(annotationKey).isArray();
						for(int i=0; i<valueArray.size(); i++) {
							if(valueArray.get(i).isNumber() != null)								
								this.addDateAnnotation(annotationKey, new Date(new Double(valueArray.get(i).isNumber().doubleValue()).longValue()));
						}
					}
				}
			}
		}
		
		key = "doubleAnnotations";
		setDoubleAnnotations(new HashMap<String, List<Double>>());
		if(object.containsKey(key)) {
			if(object.get(key).isObject() != null) {
				JSONObject annotationObj = object.get(key).isObject();
				for(String annotationKey : annotationObj.keySet()) {
					if(annotationObj.get(annotationKey).isArray() != null) {
						JSONArray valueArray = annotationObj.get(annotationKey).isArray();
						for(int i=0; i<valueArray.size(); i++) {
							if(valueArray.get(i).isNumber() != null)								
								this.addDoubleAnnotation(annotationKey, valueArray.get(i).isNumber().doubleValue());
						}
					}
				}
			}
		}

		// no blob annotations yet
		setBlobAnnotations(new HashMap<String, List<byte[]>>());
		
		key = "uri";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setUri(object.get(key).isString().stringValue());
		
	}
	
	public String toJson() {
		JSONObject object = new JSONObject();
		
		if(getId() != null) object.put("id", new JSONString(getId()));
		if(getCreationDate() != null) object.put("creationDate", new JSONNumber(getCreationDate().getTime()));
		if(getEtag() != null) object.put("etag", new JSONString(getEtag()));
		if(getUri() != null) object.put("uri", new JSONString(getUri()));
		
		JSONObject annotObj = null;
		
		annotObj = new JSONObject();
		Map<String, List<String>> annotmapS = getStringAnnotations();
		for(String key : annotmapS.keySet()) {
			List<String> values = annotmapS.get(key);
			JSONArray valArray = new JSONArray();
			for(int i=0; i<values.size(); i++) {
				valArray.set(i, new JSONString(values.get(i)));
			}
			annotObj.put(key, valArray);			
		}
		object.put("stringAnnotations", annotObj);
		
		Map<String, List<Long>> annotmapL = getLongAnnotations();
		for(String key : annotmapL.keySet()) {
			List<Long> values = annotmapL.get(key);
			JSONArray valArray = new JSONArray();
			for(int i=0; i<values.size(); i++) {
				valArray.set(i, new JSONNumber(values.get(i)));
			}
			annotObj.put(key, valArray);			
		}
		object.put("longAnnotations", annotObj);
		
		Map<String, List<Double>> annotmapD = getDoubleAnnotations();
		for(String key : annotmapD.keySet()) {
			List<Double> values = annotmapD.get(key);
			JSONArray valArray = new JSONArray();
			for(int i=0; i<values.size(); i++) {
				valArray.set(i, new JSONNumber(values.get(i)));
			}
			annotObj.put(key, valArray);			
		}
		object.put("doubleAnnotations", annotObj);
		
		Map<String, List<Date>> annotmapDt = getDateAnnotations();
		for(String key : annotmapDt.keySet()) {
			List<Date> values = annotmapDt.get(key);
			JSONArray valArray = new JSONArray();
			for(int i=0; i<values.size(); i++) {
				valArray.set(i, new JSONNumber(values.get(i).getTime()));
			}
			annotObj.put(key, valArray);			
		}
		object.put("dateAnnotations", annotObj);
		
		return object.toString();		
	}
		
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
		// Add a new value
		List<String> values = map.get(key);
		if(null == values) {
			values = new ArrayList<String>();
			map.put(key, values);
		}
		values.add(value);
	}
	
	public void addBlobAnnotation(String key, byte[] value) {
		// First get the strings
		Map<String, List<byte[]>> map = getBlobAnnotations();
		if(map == null){
			map = new HashMap<String, List<byte[]>>();
			setBlobAnnotations(map);
		}
		// Add a new value
		List<byte[]> values = map.get(key);
		if(null == values) {
			values = new ArrayList<byte[]>();
			map.put(key, values);
		}
		values.add(value);
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
		// Add a new value
		List<Date> values = map.get(key);
		if(null == values) {
			values = new ArrayList<Date>();
			map.put(key, values);
		}
		values.add(value);
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
		// Add a new value
		List<Long> values = map.get(key);
		if(null == values) {
			values = new ArrayList<Long>();
			map.put(key, values);
		}
		values.add(value);
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
		// Add a new value
		List<Double> values = map.get(key);
		if(null == values) {
			values = new ArrayList<Double>();
			map.put(key, values);
		}
		values.add(value);
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
