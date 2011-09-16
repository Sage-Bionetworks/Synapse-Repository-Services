package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class PhenoEdit extends Place{
	
	private String token;

	public PhenoEdit(String token) {
		this.token = token;
	}

	public String toToken() {
		return token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<PhenoEdit> {
        @Override
        public String getToken(PhenoEdit place) {
            return place.toToken();
        }

        @Override
        public PhenoEdit getPlace(String token) {
            return new PhenoEdit(token);
        }
    }

}
