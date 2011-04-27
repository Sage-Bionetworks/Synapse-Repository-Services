package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class DatasetsHomePlace extends Place{
	
	private String token;

	public DatasetsHomePlace(String token) {
		this.token = token;
	}

	public String toToken() {
		return token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<DatasetsHomePlace> {
        @Override
        public String getToken(DatasetsHomePlace place) {
            return place.toToken();
        }

        @Override
        public DatasetsHomePlace getPlace(String token) {
            return new DatasetsHomePlace(token);
        }
    }

}
