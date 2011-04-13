package org.sagebionetworks.repo.model.jdo.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;




/**
 * Container class for layer locations
 * 
 * @author deflaux
 */
@PersistenceCapable(detachable = "false")
public class JDOLayerLocations {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;

	@Element(dependent = "true")
	private Set<JDOLayerLocation> layerLocations;

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @param layerLocations the layerLocations to set
	 */
	public void setLayerLocations(Set<JDOLayerLocation> layerLocations) {
		this.layerLocations = layerLocations;
	}

	/**
	 * @return the layerLocations
	 */
	public Set<JDOLayerLocation> getLayerLocations() {
		return layerLocations;
	}

	public static JDOLayerLocations newJDOLayerLocations() {
		JDOLayerLocations obj = new JDOLayerLocations();
		obj.setLayerLocations(new HashSet<JDOLayerLocation>());
		return obj;
	}

}
