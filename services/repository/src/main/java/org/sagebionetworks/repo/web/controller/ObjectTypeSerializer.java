package org.sagebionetworks.repo.web.controller;

import java.io.InputStream;
import java.io.OutputStream;

import org.sagebionetworks.schema.adapter.JSONEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

public interface ObjectTypeSerializer extends HttpMessageConverter<Object> {

	/**
	 * Read the entity from the input stream and create a JSONEntity
	 * @param <T>
	 * @param body
	 * @param headers
	 * @param clazz
	 * @param type
	 * @return
	 */
	public <T extends JSONEntity> T deserialize(final InputStream body,	final HttpHeaders headers, Class<? extends T> clazz, MediaType type);
	
	/**
	 * Write the passed JSONEntity to the OutputStream
	 * @param <T>
	 * @param body
	 * @param headers
	 * @param toSerializer
	 * @param type
	 */
	public <T extends JSONEntity> void serializer(final OutputStream body, final HttpHeaders headers, T toSerializer, MediaType type);

}
