package org.sagebionetworks.auth.controller;

import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.web.util.UriComponentsBuilder;

public class EndpointHelper {

	public static String getEndpoint(UriComponentsBuilder uriComponentsBuilder) {
		return uriComponentsBuilder.fragment(null).replaceQuery(null).path(UrlHelpers.AUTH_PATH).build().toString();	
	}


}
