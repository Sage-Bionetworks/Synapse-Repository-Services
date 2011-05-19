package org.sagebionetworks.repo.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author bhoff
 * 
 */
@SuppressWarnings("rawtypes")
@XmlRootElement
public class Annotations implements Base {
	private String id; // for its parent entity
	private String uri;
	private String etag;
	private Date creationDate;
	private Map<String, Collection<String>> stringAnnotations;
	private Map<String, Collection<Double>> doubleAnnotations;
	private Map<String, Collection<Long>> longAnnotations;
	private Map<String, Collection<Date>> dateAnnotations;
	private Map<String, Collection<byte[]>> blobAnnotations;
	
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

	public Map<String, Collection<Double>> getDoubleAnnotations() {
		return doubleAnnotations;
	}

	public void setDoubleAnnotations(
			Map<String, Collection<Double>> doubleAnnotations) {
		this.doubleAnnotations = doubleAnnotations;
	}

	public Map<String, Collection<Long>> getLongAnnotations() {
		return longAnnotations;
	}
	
	public void setLongAnnotations(
			Map<String, Collection<Long>> longAnnotations) {
		this.longAnnotations = longAnnotations;
	}

	public Map<String, Collection<Date>> getDateAnnotations() {
		return dateAnnotations;
	}

	public void setDateAnnotations(Map<String, Collection<Date>> dateAnnotations) {
		this.dateAnnotations = dateAnnotations;
	}
	
	public Map<String, Collection<byte[]>> getBlobAnnotations() {
		return blobAnnotations;
	}

	public void setBlobAnnotations(Map<String, Collection<byte[]>> blobAnnotations) {
		this.blobAnnotations = blobAnnotations;
	}

	/**
	 * Get a value from the annoations.
	 * @param key
	 * @return
	 */
	public Object getSingleValue(String key){
		// Look in each set
		if(this.stringAnnotations != null){
			Collection<String> result = this.stringAnnotations.get(key);
			if(result != null){
				if(result.size() == 1) return result.iterator().next();
				return result;
			}
		}
		if(this.dateAnnotations != null){
			Collection<Date> result = this.dateAnnotations.get(key);
			if(result != null){
				if(result.size() == 1) return result.iterator().next();
				return result;
			}
		}
		if(this.longAnnotations != null){
			Collection<Long> result = this.longAnnotations.get(key);
			if(result != null){
				if(result.size() == 1) return result.iterator().next();
				return result;
			}
		}
		if(this.doubleAnnotations != null){
			Collection<Double> result = this.doubleAnnotations.get(key);
			if(result != null){
				if(result.size() == 1) return result.iterator().next();
				return result;
			}
		}
		if(this.blobAnnotations != null){
			Collection<byte[]> result = this.blobAnnotations.get(key);
			if(result != null){
				if(result.size() == 1) return result.iterator().next();
				return result;
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
			throw new IllegalArgumentException("Unknown annotatoin type: "+value.getClass().getName());
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
	
	public void replaceAnnotation(String key, String value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		if(this.stringAnnotations == null){
			this.stringAnnotations = new HashMap<String, Collection<String>>();
		}
		Collection<String> current = new ArrayList<String>();
		this.stringAnnotations.put(key, current);
		current.add(value);	
	}
	
	public void replaceAnnotation(String key, Long value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		if(this.longAnnotations == null){
			this.longAnnotations = new HashMap<String, Collection<Long>>();
		}
		Collection<Long> current = new ArrayList<Long>();
		this.longAnnotations.put(key, current);
		current.add(value);	
	}
	
	public void replaceAnnotation(String key, Date value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		if(this.dateAnnotations == null){
			this.dateAnnotations = new HashMap<String, Collection<Date>>();
		}
		Collection<Date> current = new ArrayList<Date>();
		this.dateAnnotations.put(key, current);
		current.add(value);	
	}
	
	public void replaceAnnotation(String key, Double value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		if(this.doubleAnnotations == null){
			this.doubleAnnotations = new HashMap<String, Collection<Double>>();
		}
		Collection<Double> current = new ArrayList<Double>();
		this.doubleAnnotations.put(key, current);
		current.add(value);	
	}
	
	public void replaceAnnotation(String key, byte[] value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		if(this.blobAnnotations == null){
			this.blobAnnotations = new HashMap<String, Collection<byte[]>>();
		}
		Collection<byte[]> current = new ArrayList<byte[]>();
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
		if(this.stringAnnotations == null){
			this.stringAnnotations = new HashMap<String, Collection<String>>();
		}
		Collection<String> current = this.stringAnnotations.get(key);
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
		if(this.blobAnnotations == null){
			this.blobAnnotations = new HashMap<String, Collection<byte[]>>();
		}
		Collection<byte[]> current = this.blobAnnotations.get(key);
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
		if(this.longAnnotations == null){
			this.longAnnotations = new HashMap<String, Collection<Long>>();
		}
		Collection<Long> current = this.longAnnotations.get(key);
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
		if(this.doubleAnnotations == null){
			this.doubleAnnotations = new HashMap<String, Collection<Double>>();
		}
		Collection<Double> current = this.doubleAnnotations.get(key);
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
		if(this.dateAnnotations == null){
			this.dateAnnotations = new HashMap<String, Collection<Date>>();
		}
		Collection<Date> current = this.dateAnnotations.get(key);
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
		return "Annotations [id=" + id + ", uri=" + uri + ", etag=" + etag
				+ ", creationDate=" + creationDate + ", stringAnnotations="
				+ stringAnnotations + ", doubleAnnotations="
				+ doubleAnnotations + ", longAnnotations=" + longAnnotations
				+ ", dateAnnotations=" + dateAnnotations + ", blobAnnotations="
				+ blobAnnotations + "]";
	}

}
