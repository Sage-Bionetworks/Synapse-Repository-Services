package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class Dataset extends Place {
	
	private String token;	

	public Dataset(String token) {
		this.token = token;
	}

	public String toToken() {
		return token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<Dataset> {
        @Override
        public String getToken(Dataset place) {
            return place.toToken();
        }

        @Override
        public Dataset getPlace(String token) {
            return new Dataset(token);
        }
    }

}
