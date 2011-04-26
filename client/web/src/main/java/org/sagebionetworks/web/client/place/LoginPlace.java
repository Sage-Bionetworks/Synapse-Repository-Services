package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class LoginPlace extends Place {
	
	private String token;
	private Place forwardTo;

	public LoginPlace(String token) {
		this.token = token;
	}

	public LoginPlace(Place place) {
		this.forwardTo = place;
	}

	public String toToken() {
		return token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<LoginPlace> {
        @Override
        public String getToken(LoginPlace place) {
            return place.toToken();
        }

        @Override
        public LoginPlace getPlace(String token) {
            return new LoginPlace(token);
        }
    }

	public Place getForwardPlace() {
		return forwardTo;
	}

}

