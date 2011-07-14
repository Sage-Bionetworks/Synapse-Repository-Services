package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class DatasetsHome extends Place{
	
	public static final String PLACE_STRING = "DatasetsHome";
	private String token;

	public DatasetsHome(String token) {
		this.token = token;
	}

	public String toToken() {
		return token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<DatasetsHome> {
        @Override
        public String getToken(DatasetsHome place) {
            return place.toToken();
        }

        @Override
        public DatasetsHome getPlace(String token) {
            return new DatasetsHome(token);
        }
    }

}
