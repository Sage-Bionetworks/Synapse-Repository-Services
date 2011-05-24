package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class ProjectsHome extends Place{
	
	private String token;

	public ProjectsHome(String token) {
		this.token = token;
	}

	public String toToken() {
		return token;
	}
	
	public static class Tokenizer implements PlaceTokenizer<ProjectsHome> {
        @Override
        public String getToken(ProjectsHome place) {
            return place.toToken();
        }

        @Override
        public ProjectsHome getPlace(String token) {
            return new ProjectsHome(token);
        }
    }

}
