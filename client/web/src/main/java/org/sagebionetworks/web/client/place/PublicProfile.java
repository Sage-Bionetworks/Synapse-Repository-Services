package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class PublicProfile extends Place {

	private String token;

	public PublicProfile(String token) {
		this.token = token;
	}

	public String toToken() {
		return token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<PublicProfile> {
        @Override
        public String getToken(PublicProfile place) {
            return place.toToken();
        }

        @Override
        public PublicProfile getPlace(String token) {
            return new PublicProfile(token);
        }
    }

}
