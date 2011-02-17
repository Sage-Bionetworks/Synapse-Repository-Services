package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;


public class DynamicTest extends Place {
	
	private String token;
	
	public DynamicTest(String token) {
		this.token = token;
	}

	public String toToken() {
		return this.token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<DynamicTest> {
        @Override
        public String getToken(DynamicTest place) {
            return place.toToken();
        }

        @Override
        public DynamicTest getPlace(String token) {
            return new DynamicTest(token);
        }
    }

}
