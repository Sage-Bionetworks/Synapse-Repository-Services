package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class Project extends Place{
	
	public static final String PLACE_STRING = "Project";
	
	private String token;

	public Project(String token) {
		this.token = token;
	}

	public String toToken() {
		return token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<Project> {
        @Override
        public String getToken(Project place) {
            return place.toToken();
        }

        @Override
        public Project getPlace(String token) {
            return new Project(token);
        }
    }

}
