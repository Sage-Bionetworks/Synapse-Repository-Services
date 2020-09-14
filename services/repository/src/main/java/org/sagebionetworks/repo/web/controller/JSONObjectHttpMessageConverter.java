package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class JSONObjectHttpMessageConverter implements HttpMessageConverter<JSONObject> {

	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return JSONObject.class.equals(clazz);
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return JSONObject.class.equals(clazz);
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		List<MediaType> supportedMedia = new ArrayList<MediaType>();
		supportedMedia.add(MediaType.APPLICATION_JSON);
		supportedMedia.add(MediaType.TEXT_PLAIN);
		return supportedMedia;
	}

	@Override
	public JSONObject read(Class<? extends JSONObject> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		String json = IOUtils.toString(inputMessage.getBody(), StandardCharsets.UTF_8);
		return new JSONObject(json);
	}

	@Override
	public void write(JSONObject t, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		HttpHeaders headers = outputMessage.getHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String json = t.toString();
		IOUtils.write(json, outputMessage.getBody(), StandardCharsets.UTF_8);
	}

}
