package org.sagebionetworks.web.client.place;

import org.sagebionetworks.web.client.DisplayUtils;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class LoginPlace extends Place {
	
	private String token;	
	
	public static final String LOGOUT_TOKEN = "logout";
	public static final String LOGIN_TOKEN = DisplayUtils.DEFAULT_PLACE_TOKEN;

	public LoginPlace(String token) {
		this.token = token;
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

}

