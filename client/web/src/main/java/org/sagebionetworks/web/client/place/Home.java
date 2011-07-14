package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class Home extends Place{
	
	public static final String PLACE_STRING = "Home";
	
	public Home(String token) {
	}

	public String toToken() {
		// no tokens needed for home page (yet)
		return "";
	}
	
	public static class Tokenizer implements PlaceTokenizer<Home> {
        @Override
        public String getToken(Home place) {
            return place.toToken();
        }

        @Override
        public Home getPlace(String token) {
            return new Home(token);
        }
    }

}
