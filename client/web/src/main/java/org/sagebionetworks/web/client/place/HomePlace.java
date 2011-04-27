package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class HomePlace extends Place{
	
	public HomePlace(String token) {
	}

	public String toToken() {
		// no tokens needed for home page (yet)
		return "";
	}
	
	public static class Tokenizer implements PlaceTokenizer<HomePlace> {
        @Override
        public String getToken(HomePlace place) {
            return place.toToken();
        }

        @Override
        public HomePlace getPlace(String token) {
            return new HomePlace(token);
        }
    }

}
