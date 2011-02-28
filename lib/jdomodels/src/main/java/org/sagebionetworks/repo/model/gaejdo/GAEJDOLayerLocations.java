package org.sagebionetworks.repo.model.gaejdo;

import java.util.HashSet;
import java.util.Set;

import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

/**
 * Container class for layer locations
 * 
 * @author deflaux
 */
@PersistenceCapable(detachable = "false")
public class GAEJDOLayerLocations {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key id;

	@Element(dependent = "true")
	private Set<GAEJDOLayerLocation> layerLocations;

	/**
	 * @return the id
	 */
	public Key getId() {
		return id;
	}

	/**
	 * @param id
	 */
	public void setId(Key id) {
		this.id = id;
	}

	/**
	 * @param layerLocations the layerLocations to set
	 */
	public void setLayerLocations(Set<GAEJDOLayerLocation> layerLocations) {
		this.layerLocations = layerLocations;
	}

	/**
	 * @return the layerLocations
	 */
	public Set<GAEJDOLayerLocation> getLayerLocations() {
		return layerLocations;
	}

	public static GAEJDOLayerLocations newGAEJDOLayerLocations() {
		GAEJDOLayerLocations obj = new GAEJDOLayerLocations();
		obj.setLayerLocations(new HashSet<GAEJDOLayerLocation>());
		return obj;
	}

}
