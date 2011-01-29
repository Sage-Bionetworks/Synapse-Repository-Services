package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class AllDatasets extends Place{
	
	public AllDatasets(String token) {
	}

	public String toToken() {
		// For now we do not need any tokens for datasets.
		return "";
	}
	
	public static class Tokenizer implements PlaceTokenizer<AllDatasets> {
        @Override
        public String getToken(AllDatasets place) {
            return place.toToken();
        }

        @Override
        public AllDatasets getPlace(String token) {
            return new AllDatasets(token);
        }
    }

}
