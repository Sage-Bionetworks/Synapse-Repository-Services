package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class Synapse extends Place{
	
	private String token;

	public Synapse(String token) {
		this.token = token;
	}

	public String toToken() {
		return token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<Synapse> {
        @Override
        public String getToken(Synapse place) {
            return place.toToken();
        }

        @Override
        public Synapse getPlace(String token) {
            return new Synapse(token);
        }
    }

}
