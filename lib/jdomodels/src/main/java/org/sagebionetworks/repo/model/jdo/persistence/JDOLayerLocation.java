package org.sagebionetworks.repo.model.jdo.persistence;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.LayerLocation;



/**
 * Layer location metadata
 * 
 * @author deflaux
 * 
 */
@PersistenceCapable(detachable = "false")
public class JDOLayerLocation {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;

	@Persistent
	private String type;

	@Persistent
	private String path;
	
	@Persistent
	private String md5sum;
	
	// this is the backwards pointer for the 1-1 owned relationship
	@Persistent
	private JDOLayerLocations owner;

	/**
	 * Default constructor
	 */
	public JDOLayerLocation() {
	}

	/**
	 * @param type
	 * @param path
	 * @param md5sum
	 */
	public JDOLayerLocation(String type, String path, String md5sum) {
		super();
		this.type = type;
		this.path = path;
		this.md5sum = md5sum;
	}

	/**
	 * @param location
	 */
	public JDOLayerLocation(LayerLocation location) {
		super();
		this.type = location.getType();
		this.path = location.getPath();
		this.md5sum = location.getMd5sum();
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
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

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(JDOLayerLocations owner) {
		this.owner = owner;
	}

	/**
	 * @return the owner
	 */
	public JDOLayerLocations getOwner() {
		return owner;
	}

	/**
	 * @return a LayerLocation instantiated with the values of this {@link JDOLayerLocation}
	 */
	public LayerLocation toLayerLocation() {
		return new LayerLocation(this.type, this.path, this.md5sum);
	}


}
