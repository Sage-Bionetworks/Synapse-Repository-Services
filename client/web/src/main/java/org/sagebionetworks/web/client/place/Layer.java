package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class Layer extends Place {
	
	private String token;

	public Layer(String token) {
		this.token = token;
	}

	public String toToken() {
		return token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<Layer> {
        @Override
        public String getToken(Layer place) {
            return place.toToken();
        }

        @Override
        public Layer getPlace(String token) {
            return new Layer(token);
        }
    }

}
