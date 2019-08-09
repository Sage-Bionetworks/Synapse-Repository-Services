package org.sagebionetworks.repo.web.controller;

import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

import com.nimbusds.jwt.JWT;

public interface JWTTypeSerializer extends HttpMessageConverter<Object> {

	/**
	 * Read the entity from the input stream and create a JSONEntity
	 * @param <T>
	 * @param body
	 * @param headers
	 * @param clazz
	 * @param type
	 * @return
	 */
	public JWT deserialize(final InputStream body,	final HttpHeaders headers, MediaType type);
	
	/**
	 * Write the passed JSONEntity to the OutputStream
	 * @param <T>
	 * @param body
	 * @param headers
	 * @param toSerializer
	 * @param type
	 */
	public void serializer(final OutputStream body, final HttpHeaders headers, JWT toSerializer, MediaType type);

}
