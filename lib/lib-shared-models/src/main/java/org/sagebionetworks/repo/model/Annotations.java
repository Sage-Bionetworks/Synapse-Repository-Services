package org.sagebionetworks.repo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.schema.adapter.AdapterCollectionUtils;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Primary container object for Annotations on a Synapse object.
 * 
 * Note: this class may be deprecated for replacement by auto-generated class
 * org.sagebionetworks.repo.model.annotation.Annotations
 * 
 * @author bhoff
 * 
 */
@SuppressWarnings("rawtypes")
public class Annotations implements JSONEntity, Serializable {
	private static final String JSON_BLOB_ANNOTATIONS = "blobAnnotations";
	private static final String JSON_DATE_ANNOTATIONS = "dateAnnotations";
	private static final String JSON_LONG_ANNOTATIONS = "longAnnotations";
	private static final String JSON_DOUBLE_ANNOTATIONS = "doubleAnnotations";
	private static final String JSON_STRING_ANNOTATIONS = "stringAnnotations";
	private static final String JSON_ETAG = "etag";
	public static final String JSON_ID = "id";
	private String id; // for its parent entity
	private String etag;
	private Map<String, List<String>> stringAnnotations;
	private Map<String, List<Double>> doubleAnnotations;
	private Map<String, List<Long>> longAnnotations;
	private Map<String, List<Date>> dateAnnotations;
	private Map<String, List<byte[]>> blobAnnotations;
	
	public Annotations(){
		initailzeMaps();
	}

	public Annotations(JSONObjectAdapter jsonObject) {
		super();
		try {
			initializeFromJSONObject(jsonObject);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public boolean isEmpty(){
		return (stringAnnotations == null || stringAnnotations.isEmpty())
				&& (doubleAnnotations == null || doubleAnnotations.isEmpty())
				&& (longAnnotations == null || longAnnotations.isEmpty())
				&& (dateAnnotations == null || dateAnnotations.isEmpty())
				&& (blobAnnotations == null || blobAnnotations.isEmpty());
	}

	/**
	 * XStream uses the same mechanism as Java serialization so our
	 * constructor never gets called.  To ensure that the maps
	 * are always initialized we must implement this method.
	 * @return
	 */
	private Object readResolve() {
		this.initailzeMaps();
		return this;
	}

	public void initailzeMaps() {
		if(this.stringAnnotations == null){
			this.stringAnnotations = new HashMap<String, List<String>>();
		}
		if(this.doubleAnnotations == null){
			this.doubleAnnotations = new HashMap<String, List<Double>>();
		}
		if(this.longAnnotations == null){
			this.longAnnotations = new HashMap<String, List<Long>>();
		}
		if(this.dateAnnotations == null){
			this.dateAnnotations = new HashMap<String, List<Date>>();
		}
		if(this.blobAnnotations == null){
			this.blobAnnotations = new HashMap<String, List<byte[]>>();
		}
	}
	
	
	/**
	 * Create Annotations with all of the maps initialized.
	 * @return
	 */
	public static Annotations createInitialized(){
		Annotations annos = new Annotations();
		annos.initailzeMaps();
		return annos;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public Map<String, List<String>> getStringAnnotations() {
		return stringAnnotations;
	}

	public void setStringAnnotations(
			Map<String, List<String>> stringAnnotations) {
		this.stringAnnotations = stringAnnotations;
	}

	public Map<String, List<Double>> getDoubleAnnotations() {
		return doubleAnnotations;
	}

	public void setDoubleAnnotations(
			Map<String, List<Double>> doubleAnnotations) {
		this.doubleAnnotations = doubleAnnotations;
	}

	public Map<String, List<Long>> getLongAnnotations() {
		return longAnnotations;
	}
	
	public void setLongAnnotations(
			Map<String, List<Long>> longAnnotations) {
		this.longAnnotations = longAnnotations;
	}

	public Map<String, List<Date>> getDateAnnotations() {
		return dateAnnotations;
	}

	/**
	 * @param dateAnnotations the dateAnnotations to set
	 */
	public void setDateAnnotations(Map<String, List<Date>> dateAnnotations) {
		this.dateAnnotations = dateAnnotations;
	}

	public Map<String, List<byte[]>> getBlobAnnotations() {
		return blobAnnotations;
	}


	/**
	 * @param blobAnnotations the blobAnnotations to set
	 */
	public void setBlobAnnotations(Map<String, List<byte[]>> blobAnnotations) {
		this.blobAnnotations = blobAnnotations;
	}

	public Set<String> keySet(){
		HashSet<String> keys = new HashSet<String>();
		keys.addAll(this.stringAnnotations.keySet());
		keys.addAll(this.blobAnnotations.keySet());
		keys.addAll(this.dateAnnotations.keySet());
		keys.addAll(this.doubleAnnotations.keySet());
		keys.addAll(this.longAnnotations.keySet());
		return keys;
	}
	
	/**
	 * Get a value from any of the maps.
	 * @param key
	 * @return
	 */
	public List getAllValues(String key) {
		List result = null;
		result = this.stringAnnotations.get(key);
		if (result != null)
			return result;
		result = this.blobAnnotations.get(key);
		if (result != null)
			return result;
		result = this.dateAnnotations.get(key);
		if (result != null)
			return result;
		result = this.doubleAnnotations.get(key);
		if (result != null)
			return result;
		result = this.longAnnotations.get(key);
		if (result != null)
			return result;
		return null;
	}
	
	public void addAll(Annotations toMerge) {
		this.stringAnnotations.putAll(toMerge.stringAnnotations);
		this.blobAnnotations.putAll(toMerge.blobAnnotations);
		this.dateAnnotations.putAll(toMerge.dateAnnotations);
		this.doubleAnnotations.putAll(toMerge.doubleAnnotations);
		this.longAnnotations.putAll(toMerge.longAnnotations);
	}

	/**
	 * Get a the first value from the annoations.
	 * @param key
	 * @return
	 */
	public Object getSingleValue(String key){
		// Look in each set
		if(this.stringAnnotations != null){
			List<String> result = this.stringAnnotations.get(key);
			if(result != null && !result.isEmpty()){
				return result.get(0);
			}
		}
		if(this.dateAnnotations != null){
			List<Date> result = this.dateAnnotations.get(key);
			if(result != null && !result.isEmpty()){
				return result.get(0);
			}
		}
		if(this.longAnnotations != null){
			List<Long> result = this.longAnnotations.get(key);
			if(result != null && !result.isEmpty()){
				return result.get(0);
			}
		}
		if(this.doubleAnnotations != null){
			List<Double> result = this.doubleAnnotations.get(key);
			if(result != null && !result.isEmpty()){
				return result.get(0);
			}
		}
		if(this.blobAnnotations != null){
			List<byte[]> result = this.blobAnnotations.get(key);
			if(result != null && !result.isEmpty()){
				return result.get(0);
			}
		}
		// did not find it.
		return null;
	}
	
	
	public void replaceAnnotation(String key, Object value) {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		if(value instanceof String){
			replaceAnnotation(key, (String)value);
		}else if(value instanceof Date){
			replaceAnnotation(key, (Date)value);
		}else if(value instanceof Long){
			replaceAnnotation(key, (Long)value);
		}else if(value instanceof Double){
			replaceAnnotation(key, (Double)value);
		}else if(value instanceof Boolean){
			replaceAnnotation(key, ((Boolean)value).toString());
		}else if(value instanceof byte[]){
			replaceAnnotation(key, ((byte[])value));
		}else if(value instanceof Collection ){
			Collection col = (Collection) value;
			Iterator it = col.iterator();
			// Add each
			int count = 0;
			while(it.hasNext()){
				// The first is a replace
				if(count == 0){
					replaceAnnotation(key, it.next());
				}else{
					// The rest are adds.
					addAnnotation(key, it.next());
				}
				count++;
			}
		}
		else{
			throw new IllegalArgumentException("Unknown annotation type: "+value.getClass().getName());
		}
	}
	
	/**
	 * Add a value of object.  Note: Must be of the supported types: {String, Long, Double, Date}.
	 * @param key
	 * @param value
	 */
	public void addAnnotation(String key, Object value) {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		if(value instanceof String){
			addAnnotation(key, (String)value);
		}else if(value instanceof Date){
			addAnnotation(key, (Date)value);
		}else if(value instanceof Long){
			addAnnotation(key, (Long)value);
		}else if(value instanceof Double){
			addAnnotation(key, (Double)value);
		}else if(value instanceof Boolean){
			addAnnotation(key, ((Boolean)value).toString());
		}else if(value instanceof byte[]){
			addAnnotation(key, ((byte[])value));
		}else if(value instanceof Collection ){
			Collection col = (Collection) value;
			Iterator it = col.iterator();
			// Add each
			while(it.hasNext()){
				addAnnotation(key, it.next());
			}
		}
		else{
			throw new IllegalArgumentException("Unknown annotatoin type: "+value.getClass().getName());
		}
		
	}
	
	public List deleteAnnotation(String key) {
		// Look in each set
		if((this.stringAnnotations != null) && (this.stringAnnotations.containsKey(key))){
			List<String> result = this.stringAnnotations.remove(key);
			return result;
		}
		if((this.dateAnnotations != null) && (this.dateAnnotations.containsKey(key))){
			List<Date> result = this.dateAnnotations.remove(key);
			return result;
		}
		if((this.longAnnotations != null) && (this.longAnnotations.containsKey(key))){
			List<Long> result = this.longAnnotations.remove(key);
			return result;
		}
		if((this.doubleAnnotations != null) && (this.doubleAnnotations.containsKey(key))){
		List<Double> result = this.doubleAnnotations.remove(key);
			return result;
		}
		if((this.blobAnnotations != null) && (this.blobAnnotations.containsKey(key))){
			List<byte[]> result = this.blobAnnotations.remove(key);
			return result;
		}
		// did not find it.
		return (List)null;
	}
	
	public void replaceAnnotation(String key, String value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		List<String> current = new ArrayList<String>();
		this.stringAnnotations.put(key, current);
		current.add(value);	
	}
	
	public void replaceAnnotation(String key, Long value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		List<Long> current = new ArrayList<Long>();
		this.longAnnotations.put(key, current);
		current.add(value);	
	}
	
	public void replaceAnnotation(String key, Date value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		if(value instanceof java.sql.Date) {
			throw new IllegalArgumentException("Wrong Date type: 'java.sql.Date'");
		}
		List<Date> current = new ArrayList<Date>();
		this.dateAnnotations.put(key, current);
		current.add(value);	
	}
	
	public void replaceAnnotation(String key, Double value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		List<Double> current = new ArrayList<Double>();
		this.doubleAnnotations.put(key, current);
		current.add(value);	
	}
	
	public void replaceAnnotation(String key, byte[] value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		List<byte[]> current = new ArrayList<byte[]>();
		this.blobAnnotations.put(key, current);
		current.add(value);	
	}
	
	/**
	 * Helper for adding a string
	 * @param key
	 * @param value
	 */
	public void addAnnotation(String key, String value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		List<String> current = this.stringAnnotations.get(key);
		if(current == null){
			current = new ArrayList<String>();
			this.stringAnnotations.put(key, current);
		}
		current.add(value);	
	}
	
	/**
	 * Helper for adding a string
	 * @param key
	 * @param value
	 */
	public void addAnnotation(String key, byte[] value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		List<byte[]> current = this.blobAnnotations.get(key);
		if(current == null){
			current = new ArrayList<byte[]>();
			this.blobAnnotations.put(key, current);
		}
		current.add(value);	
	}
	
	/**
	 * Helper for adding a long
	 * @param key
	 * @param value
	 */
	public void addAnnotation(String key, Long value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		List<Long> current = this.longAnnotations.get(key);
		if(current == null){
			current = new ArrayList<Long>();
			this.longAnnotations.put(key, current);
		}
		current.add(value);		
	}
	
	/**
	 * Helper for adding a double
	 * @param key
	 * @param value
	 */
	public void addAnnotation(String key, Double value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		List<Double> current = this.doubleAnnotations.get(key);
		if(current == null){
			current = new ArrayList<Double>();
			this.doubleAnnotations.put(key, current);
		}
		current.add(value);
	}
	/**
	 * Helper for adding a date
	 * @param key
	 * @param value
	 */
	public void addAnnotation(String key, Date value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		List<Date> current = this.dateAnnotations.get(key);
		if(current == null){
			current = new ArrayList<Date>();
			this.dateAnnotations.put(key, current);
		}
		current.add(value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((blobAnnotations == null) ? 0 : blobAnnotations.hashCode());
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
		return result;
	}
	
	/**
	 * A default Collection<byte[]> will return false unless the two arrays are the
	 * same instance.  This does not work for testing, so we do a deep equals on blob arrays.
	 * @param other
	 * @return
	 */
	public boolean blobEquals(Map<String, List<byte[]>> other){
		if (blobAnnotations == null) 
			if (other != null)
				return false;
		if(blobAnnotations.size() != other.size()) return false;
		Iterator<String> it = blobAnnotations.keySet().iterator();
		while(it.hasNext()){
			String key = it.next();
			List<byte[]> thisCol = blobAnnotations.get(key);
			List<byte[]> otherCol = other.get(key);
			if(thisCol == null)
				if(otherCol != null) return false;
			if(thisCol.size() != otherCol.size()) return false;
			Iterator<byte[]> thisIt = thisCol.iterator();
			Iterator<byte[]> otherIt = thisCol.iterator();
			while(thisIt.hasNext()){
				byte[] thisArray = thisIt.next();
				byte[] otherArray = otherIt.next();
				if(thisArray == null)
					if(otherArray != null)
						return false;
				if(!Arrays.equals(thisArray,otherArray)) return false;
			}
		}
		
		return true;
	}

	public boolean dateEquals(Map<String, List<Date>> other){
		if (dateAnnotations == null) 
			if (other != null)
				return false;
		if(dateAnnotations.size() != other.size()) return false;
		Iterator<String> it = dateAnnotations.keySet().iterator();
		while(it.hasNext()){
			String key = it.next();
			List<Date> thisCol = dateAnnotations.get(key);
			List<Date> otherCol = other.get(key);
			if(thisCol == null)
				if(otherCol != null) return false;
			if(thisCol.size() != otherCol.size()) return false;
			Iterator<Date> thisIt = thisCol.iterator();
			Iterator<Date> otherIt = thisCol.iterator();
			while(thisIt.hasNext()){
				Date thisDate = thisIt.next();
				Date otherDate = otherIt.next();

				if(thisDate == null)
					if(otherDate != null)
						return false;
				if(thisDate.getTime() != otherDate.getTime()) return false;
			}
		}
		return true;
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
		} else if (!blobEquals(other.blobAnnotations))
			return false;
		if (dateAnnotations == null) {
			if (other.dateAnnotations != null)
				return false;
		} else if (!dateEquals(other.dateAnnotations))
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
		return true;
	}
	
	@Override
	public String toString() {
		return "Annotations [id=" + id + ", etag=" + etag
				+ ", stringAnnotations="
				+ stringAnnotations + ", doubleAnnotations="
				+ doubleAnnotations + ", longAnnotations=" + longAnnotations
				+ ", dateAnnotations=" + dateAnnotations + ", blobAnnotations="
				+ blobAnnotations + "]";
	}

	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		if(toInitFrom.has(JSON_ID)){
			this.id = toInitFrom.getString(JSON_ID);
		}
		if(toInitFrom.has(JSON_ETAG)){
			this.etag = toInitFrom.getString(JSON_ETAG);
		}
		if(toInitFrom.has(JSON_STRING_ANNOTATIONS)){
			JSONObjectAdapter object = toInitFrom.getJSONObject(JSON_STRING_ANNOTATIONS);
			this.stringAnnotations = AdapterCollectionUtils.createMapOfCollection(object, String.class);
		}
		if(toInitFrom.has(JSON_DOUBLE_ANNOTATIONS)){
			JSONObjectAdapter object = toInitFrom.getJSONObject(JSON_DOUBLE_ANNOTATIONS);
			this.doubleAnnotations = AdapterCollectionUtils.createMapOfCollection(object, Double.class);
		}
		if(toInitFrom.has(JSON_LONG_ANNOTATIONS)){
			JSONObjectAdapter object = toInitFrom.getJSONObject(JSON_LONG_ANNOTATIONS);
			this.longAnnotations = AdapterCollectionUtils.createMapOfCollection(object, Long.class);
		}
		if(toInitFrom.has(JSON_DATE_ANNOTATIONS)){
			JSONObjectAdapter object = toInitFrom.getJSONObject(JSON_DATE_ANNOTATIONS);
			this.dateAnnotations = AdapterCollectionUtils.createMapOfCollection(object, Date.class);
		}
		if(toInitFrom.has(JSON_BLOB_ANNOTATIONS)){
			JSONObjectAdapter object = toInitFrom.getJSONObject(JSON_BLOB_ANNOTATIONS);
			this.blobAnnotations = AdapterCollectionUtils.createMapOfCollection(object, byte[].class);
		}
		return toInitFrom;
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
		if(this.id != null){
			writeTo.put(JSON_ID, this.id);
		}
		if(this.etag != null){
			writeTo.put(JSON_ETAG, this.etag);
		}
		if(this.stringAnnotations != null){
			JSONObjectAdapter object = writeTo.createNew();
			writeTo.put(JSON_STRING_ANNOTATIONS, object);
			AdapterCollectionUtils.writeToAdapter(object, this.stringAnnotations, String.class);
		}
		if(this.doubleAnnotations != null){
			JSONObjectAdapter object = writeTo.createNew();
			writeTo.put(JSON_DOUBLE_ANNOTATIONS, object);
			AdapterCollectionUtils.writeToAdapter(object, this.doubleAnnotations, Double.class);
		}
		if(this.longAnnotations != null){
			JSONObjectAdapter object = writeTo.createNew();
			writeTo.put(JSON_LONG_ANNOTATIONS, object);
			AdapterCollectionUtils.writeToAdapter(object, this.longAnnotations, Long.class);
		}
		if(this.dateAnnotations != null){
			JSONObjectAdapter object = writeTo.createNew();
			writeTo.put(JSON_DATE_ANNOTATIONS, object);
			AdapterCollectionUtils.writeToAdapter(object, this.dateAnnotations, Date.class);
		}
		if(this.blobAnnotations != null){
			JSONObjectAdapter object = writeTo.createNew();
			writeTo.put(JSON_BLOB_ANNOTATIONS, object);
			AdapterCollectionUtils.writeToAdapter(object, this.blobAnnotations, byte[].class);
		}
		return writeTo;
	}

}
