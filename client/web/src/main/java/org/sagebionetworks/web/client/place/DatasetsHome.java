package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class DatasetsHome extends Place{
	
	public DatasetsHome(String token) {
	}

	public String toToken() {
		// For now we do not need any tokens for datasets.
		return "";
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
