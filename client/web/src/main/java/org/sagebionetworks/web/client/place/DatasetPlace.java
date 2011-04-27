package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class DatasetPlace extends Place {
	
	private String token;

	public DatasetPlace(String token) {
		this.token = token;
	}

	public String toToken() {
		return token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<DatasetPlace> {
        @Override
        public String getToken(DatasetPlace place) {
            return place.toToken();
        }

        @Override
        public DatasetPlace getPlace(String token) {
            return new DatasetPlace(token);
        }
    }

}
