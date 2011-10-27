package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class Analysis extends Place{
	
	private String token;

	public Analysis(String token) {
		this.token = token;
	}

	public String toToken() {
		return token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<Analysis> {
        @Override
        public String getToken(Analysis place) {
            return place.toToken();
        }

        @Override
        public Analysis getPlace(String token) {
            return new Analysis(token);
        }
    }

}
