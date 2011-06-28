package org.sagebionetworks.web.client;

import com.google.gwt.place.shared.Place;

/**
 * This interface is for Widgets to pass their presenters place changing capabilities onto their view impls 
 * @author dburdick
 *
 */
public interface PlaceChanger {
	public void goTo(Place place);
}
