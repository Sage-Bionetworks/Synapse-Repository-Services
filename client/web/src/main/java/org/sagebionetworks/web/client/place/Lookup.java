package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class Lookup extends Place{
	
	private String token;

	public Lookup(String token) {
		this.token = token;
	}

	public String toToken() {
		return token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<Lookup> {
        @Override
        public String getToken(Lookup place) {
            return place.toToken();
        }

        @Override
        public Lookup getPlace(String token) {
            return new Lookup(token);
        }
    }

}
