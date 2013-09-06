package org.sagebionetworks.repo.web.controller;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityClassHelper;
import org.sagebionetworks.repo.util.ControllerUtil;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class ControllerEntityClassHelper {
	private static final List<MediaType> SUPPORTED_MEDIA_TYPES = Arrays
			.asList(new MediaType[] { MediaType.APPLICATION_JSON, });

	private static boolean isSupported(MediaType mt) {
		for (MediaType supported : SUPPORTED_MEDIA_TYPES) {
			if (supported.getType().equals(mt.getType())
					&& supported.getSubtype().equals(mt.getSubtype()))
				return true;
		}
		return false;
	}

	private static JSONEntity deserialize(HttpServletRequest request,
			HttpHeaders header) throws DatastoreException {
		try {
			String requestBody = ControllerUtil.getRequestBodyAsString(request);
			return deserialize(requestBody, header.getContentType());
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}

	private static JSONEntity deserialize(String httpRequestBody,
			MediaType mediaType) throws JSONObjectAdapterException {
		if (!isSupported(mediaType))
			throw new IllegalArgumentException("Unsupported media type: "
					+ mediaType);
		JSONObjectAdapter jsonObjectAdapter = (new JSONObjectAdapterImpl())
				.createNew(httpRequestBody);
		return EntityClassHelper.deserialize(jsonObjectAdapter);
	}
}
