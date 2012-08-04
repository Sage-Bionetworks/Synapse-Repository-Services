package org.sagebionetworks.repo.web.controller;

import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.EntityClassHelper;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.http.MediaType;

public class MediaTypeHelper {
	private static final List<MediaType> SUPPORTED_MEDIA_TYPES = Arrays.asList(new MediaType[] {
			MediaType.APPLICATION_JSON,
			});
	
	public static boolean isSupported(MediaType mt) {
		for (MediaType supported : SUPPORTED_MEDIA_TYPES) {
			if (supported.getType().equals(mt.getType()) && supported.getSubtype().equals(mt.getSubtype())) return true;
		}
		return false;
	}
	
	public static String entityType(String httpRequestBody, MediaType mediaType) throws JSONObjectAdapterException {
		if (!isSupported(mediaType)) throw new IllegalArgumentException("Unsupported media type: "+mediaType);
		JSONObjectAdapter jsonObjectAdapter = (new JSONObjectAdapterImpl()).createNew(httpRequestBody);
		return EntityClassHelper.entityType(jsonObjectAdapter);
	}
}
