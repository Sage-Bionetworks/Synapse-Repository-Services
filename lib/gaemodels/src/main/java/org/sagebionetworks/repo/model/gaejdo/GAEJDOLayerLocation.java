package org.sagebionetworks.repo.model.gaejdo;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.LayerLocation;

import com.google.appengine.api.datastore.Key;

/**
 * Layer location metadata
 * 
 * @author deflaux
 * 
 */
@PersistenceCapable(detachable = "false")
public class GAEJDOLayerLocation {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key id;

	@Persistent
	private String type;

	@Persistent
	private String path;
	
	// this is the backwards pointer for the 1-1 owned relationship
	@Persistent
	private GAEJDOLayerLocations owner;

	/**
	 * Default constructor
	 */
	public GAEJDOLayerLocation() {
	}

	/**
	 * @param type
	 * @param path
	 */
	public GAEJDOLayerLocation(String type, String path) {
		super();
		this.type = type;
		this.path = path;
	}

	/**
	 * @param location
	 */
	public GAEJDOLayerLocation(LayerLocation location) {
		super();
		this.type = location.getType();
		this.path = location.getPath();
	}

	/**
	 * @return the id
	 */
	public Key getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Key id) {
		this.id = id;
	}

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
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}



	/**
	 * @param owner the owner to set
	 */
	public void setOwner(GAEJDOLayerLocations owner) {
		this.owner = owner;
	}

	/**
	 * @return the owner
	 */
	public GAEJDOLayerLocations getOwner() {
		return owner;
	}

	/**
	 * @return a LayerLocation instantiated with the values of this {@link GAEJDOLayerLocation}
	 */
	public LayerLocation toLayerLocation() {
		return new LayerLocation(this.type, this.path);
	}


}
