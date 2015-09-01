package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.util.JSONEntityUtil;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

/**
 * Utility for serializing/deserializing objects using the same HttpMessageConverters
 * that the services use.
 * 
 * @author jmhill
 * 
 */
public class ObjectTypeSerializerImpl implements ObjectTypeSerializer{
	
	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	MappingJacksonHttpMessageConverter jacksonConverter = new MappingJacksonHttpMessageConverter();
	JSONEntityHttpMessageConverter jsonEntityConverter = new JSONEntityHttpMessageConverter();
	

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
	@Override
	@SuppressWarnings({ "unchecked" })
	public <T extends JSONEntity> T deserialize(final InputStream body,	final HttpHeaders headers, Class<? extends T> clazz, MediaType type) {
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
		try {
			return (T) jsonEntityConverter.read(clazz, message);
		} catch (HttpMessageNotReadableException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Write an object to the given stream.
	 * @param <T>
	 * @param body
	 * @param headers
	 * @param toSerializer
	 * @param type
	 */
	public <T extends JSONEntity> void serializer(final OutputStream body,
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
		try {
			jsonEntityConverter.write(toSerializer, type, message);
		} catch (HttpMessageNotReadableException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return jacksonConverter.canRead(clazz, mediaType) || 
				jsonEntityConverter.canRead(clazz, mediaType);
	}
	
	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return jacksonConverter.canWrite(clazz, mediaType) ||
				jsonEntityConverter.canWrite(clazz, mediaType);
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		List<MediaType> result = new ArrayList<MediaType>();
		result.addAll(jacksonConverter.getSupportedMediaTypes());
		result.addAll(jsonEntityConverter.getSupportedMediaTypes());
		return result;
	}


	@SuppressWarnings("unchecked")
	@Override
	public Object read(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
		if(JSONEntityUtil.isJSONEntity(clazz)){
			return jsonEntityConverter.read((Class<? extends JSONEntity>) clazz, inputMessage);
		}else{
			return jacksonConverter.read(clazz, inputMessage);
		}
	}


	@Override
	public void write(Object t, MediaType contentType,	HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
		// Is it an JSON entity
		if(t instanceof JSONEntity){
			JSONEntity entity = (JSONEntity) t;
			jsonEntityConverter.write(entity, contentType, outputMessage);
		}else{
			// Let Jackson write non-JSONEntity
			jacksonConverter.write(t, contentType, outputMessage);
		}
		
	}

}
