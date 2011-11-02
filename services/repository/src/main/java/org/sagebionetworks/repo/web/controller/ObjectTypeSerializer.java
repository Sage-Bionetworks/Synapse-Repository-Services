package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.sagebionetworks.repo.model.Entity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Utility for serializing/deserializing objects using the same HttpMessageConverters
 * that the controllers use.
 * 
 * @author jmhill
 * 
 */
public class ObjectTypeSerializer {

	List<HttpMessageConverter<?>> messageConverters;
	

	/**
	 * This is injected via Spring
	 * @param messageConverterList
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverterList) {
		this.messageConverters = messageConverterList;
		// Add the entity converter first
		messageConverters.add(0, new JSONEntityHttpMessageConverter());
	}

	/**
	 * Deserialize the body of an HttpRequest entity from an http call.
	 * 
	 * @param <T>
	 * @param in
	 * @param headers
	 * @param clazz
	 *            The class to deserialize into.
	 * @param type
	 *            The media type of the message.
	 * @return
	 * @throws HttpMessageNotReadableException
	 * @throws IOException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <T extends Entity> T deserialize(final InputStream body,
			final HttpHeaders headers, Class<? extends T> clazz, MediaType type) {
		HttpInputMessage message = new HttpInputMessage() {
			@Override
			public HttpHeaders getHeaders() {
				return headers;
			}

			@Override
			public InputStream getBody() throws IOException {
				return body;
			}
		};
		// Find the type for this media type
		for (HttpMessageConverter messageConverter : messageConverters) {
			if (messageConverter.canRead(clazz, type)) {
				try {
					return (T) messageConverter.read(clazz, message);
				} catch (HttpMessageNotReadableException e) {
					throw new IllegalArgumentException(e);
				} catch (IOException e) {
					throw new IllegalArgumentException(e);
				}
			}
		}
		throw new IllegalArgumentException(
				"Cannot find a messageConverters for type: " + type);
	}
	
	/**
	 * Write an object to the given stream.
	 * @param <T>
	 * @param body
	 * @param headers
	 * @param toSerializer
	 * @param type
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T extends Entity> void serializer(final OutputStream body,
			final HttpHeaders headers, T toSerializer, MediaType type) {
		HttpOutputMessage message = new HttpOutputMessage() {
			
			@Override
			public HttpHeaders getHeaders() {
				return headers;
			}
			@Override
			public OutputStream getBody() throws IOException {
				return body;
			}
		};
		// Find the type for this media type
		for (HttpMessageConverter messageConverter : messageConverters) {
			if (messageConverter.canWrite(toSerializer.getClass(), type)) {
				try {
					messageConverter.write(toSerializer, type, message);
					return;
				} catch (HttpMessageNotReadableException e) {
					throw new IllegalArgumentException(e);
				} catch (IOException e) {
					throw new IllegalArgumentException(e);
				}
			}
		}
		throw new IllegalArgumentException(
				"Cannot find a messageConverters for type: " + type);
	}

}
