package org.sagebionetworks.web.client;

import org.sagebionetworks.web.client.mvp.AppPlaceHistoryMapper;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;

public interface GlobalApplicationState {

	
	/**
	 * Gets the place changer for the application
	 * @return
	 */	
	public PlaceChanger getPlaceChanger();
	
	/**
	 * Sets the place controller (should only be used in the onModuleLoad() method of Portal) 
	 * @param placeController
	 */
	public void setPlaceController(PlaceController placeController);	

	/**
	 * Holds the last visited place
	 * @return
	 */
	public Place getLastPlace();
	
	/**
	 * Sets the last visited place (should only used in the AppActivityMapper) 
	 * @param lastPlace
	 */
	public void setLastPlace(Place lastPlace);
	
	/**
	 * Holds the current place
	 * @return
	 */
	public Place getCurrentPlace();
	
	/**
	 * Sets the last visited place (should only used in the AppActivityMapper) 
	 * @param lastPlace
	 */
	public void setCurrentPlace(Place currentPlace);
	
	/**
	 * Sets the App Place History Mapper
	 * @param appPlaceHistoryMapper
	 */
	public void setAppPlaceHistoryMapper(AppPlaceHistoryMapper appPlaceHistoryMapper);
	
	/**
	 * Gets the App Place History Mapper
	 * @return AppPlaceHistoryMapper
	 */
	public AppPlaceHistoryMapper getAppPlaceHistoryMapper();
	
}
