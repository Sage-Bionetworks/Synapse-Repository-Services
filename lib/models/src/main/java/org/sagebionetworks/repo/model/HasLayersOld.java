package org.sagebionetworks.repo.model;
/**
 * Any object that can have layers, should implement this class.
 * 
 * @author jmhill
 *
 */
public interface HasLayersOld extends NodeableOld {
	
	/**
	 * The URL of the layers that belong to this object.
	 * 
	 * @param layers
	 */
	public void setLayers(String layers);
	
	/**
	 *  The URL of the layers that belong to this object.
	 * @return
	 */
	public String getLayers();

}
