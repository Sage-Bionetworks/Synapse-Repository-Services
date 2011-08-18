package org.sagebionetworks.web.client;

import org.sagebionetworks.web.client.cookie.CookieKeys;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.mvp.AppPlaceHistoryMapper;
import org.sagebionetworks.web.client.place.Home;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.inject.Inject;

public class GlobalApplicationStateImpl implements GlobalApplicationState {

	private PlaceController placeController;
	private CookieProvider cookieProvider;
	private AppPlaceHistoryMapper appPlaceHistoryMapper;
	
	@Inject
	public GlobalApplicationStateImpl(CookieProvider cookieProvider) {
		this.cookieProvider = cookieProvider;
	}

	@Override
	public PlaceController getPlaceController() {
		return placeController;
	}

	@Override
	public void setPlaceController(PlaceController placeController) {
		this.placeController = placeController;
	}

	@Override
	public Place getLastPlace() {
		String historyValue = cookieProvider.getCookie(CookieKeys.LAST_PLACE);
		return getPlaceFromHistoryValue(historyValue);		
	}

	@Override
	public void setLastPlace(Place lastPlace) {
		cookieProvider.setCookie(CookieKeys.LAST_PLACE, appPlaceHistoryMapper.getToken(lastPlace));
	}

	@Override
	public Place getCurrentPlace() {
		String historyValue = cookieProvider.getCookie(CookieKeys.CURRENT_PLACE);
		return getPlaceFromHistoryValue(historyValue);		
	}

	@Override
	public void setCurrentPlace(Place currentPlace) {		
		cookieProvider.setCookie(CookieKeys.CURRENT_PLACE, appPlaceHistoryMapper.getToken(currentPlace));
	}

	@Override
	public void setAppPlaceHistoryMapper(AppPlaceHistoryMapper appPlaceHistoryMapper) {
		this.appPlaceHistoryMapper = appPlaceHistoryMapper;
	}

	@Override
	public AppPlaceHistoryMapper getAppPlaceHistoryMapper() {
		return appPlaceHistoryMapper;
	}

	/*
	 * Private Methods
	 */
	private Place getPlaceFromHistoryValue(String historyValue) {
		if(historyValue != null) {
			Place place = appPlaceHistoryMapper.getPlace(historyValue);
			return place;
		}
		return null;
	}
	
}
