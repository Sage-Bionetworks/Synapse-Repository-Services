package org.sagebionetworks.web.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Simple metadata about a layer
 * 
 * @author jmhill
 *
 */
public class LayerLink implements IsSerializable {
	
	/**
	 * 
	 * We need to define what these mean.
	 *
	 */
	public enum Type implements IsSerializable {
		C(0x01),
		G(0x02),
		E(0x04);
		// Assign a mask to each type.
		int mask = 0;
		Type(int mask){
			this.mask = mask;
		};
		public int getMask(){
			return this.mask;
		}
	}
	
	private Type type = null;
	private String uri = null;
	private String id = null;
	
	/**
	 * The default constructor is required.
	 */
	public LayerLink(){
	}
	
	public LayerLink(String id, Type type, String uri) {
		super();
		this.id = id;
		this.type = type;
		this.uri = uri;
	}
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		LayerLink other = (LayerLink) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
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
		return "LayerLink [type=" + type + ", uri=" + uri + "]";
	}
	
}
