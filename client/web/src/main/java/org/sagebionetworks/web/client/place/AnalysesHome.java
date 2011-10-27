package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class AnalysesHome extends Place{
	
	private String token;

	public AnalysesHome(String token) {
		this.token = token;
	}

	public String toToken() {
		return token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<AnalysesHome> {
        @Override
        public String getToken(AnalysesHome place) {
            return place.toToken();
        }

        @Override
        public AnalysesHome getPlace(String token) {
            return new AnalysesHome(token);
        }
    }

}
