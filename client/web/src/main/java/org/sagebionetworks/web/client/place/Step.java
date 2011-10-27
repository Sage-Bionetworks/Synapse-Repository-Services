package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class Step extends Place{
	
	private String token;

	public Step(String token) {
		this.token = token;
	}

	public String toToken() {
		return token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<Step> {
        @Override
        public String getToken(Step place) {
            return place.toToken();
        }

        @Override
        public Step getPlace(String token) {
            return new Step(token);
        }
    }

}
