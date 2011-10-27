package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class StepsHome extends Place{
	
	private String token;

	public StepsHome(String token) {
		this.token = token;
	}

	public String toToken() {
		return token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<StepsHome> {
        @Override
        public String getToken(StepsHome place) {
            return place.toToken();
        }

        @Override
        public StepsHome getPlace(String token) {
            return new StepsHome(token);
        }
    }

}
