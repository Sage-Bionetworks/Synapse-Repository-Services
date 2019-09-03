package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import io.jsonwebtoken.Jwt;

public class JWTTypeSerializerImpl implements JWTTypeSerializer {
	
	private static final String APPLICATION_TYPE = "application";
	private static final String JWT_SUBTYPE = "jwt";
	private static final MediaType APPLICATION_JWT_MEDIA_TYPE = new MediaType(APPLICATION_TYPE, JWT_SUBTYPE);
	private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) { 
		return false;	
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return APPLICATION_JWT_MEDIA_TYPE.isCompatibleWith(mediaType);	
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Arrays.asList(new MediaType[] {APPLICATION_JWT_MEDIA_TYPE});
	}

	@Override
	public String read(Class<? extends String> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		throw new IllegalArgumentException("Cannot read "+APPLICATION_JWT_MEDIA_TYPE);
	}

	@Override
	public void write(String t, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		if (!canWrite(t.getClass(), contentType)) throw new IllegalArgumentException("Cannot write "+contentType);
		Charset charsetForSerializingBody = contentType.getCharset()==null ? UTF_8_CHARSET : contentType.getCharset();
		MediaType contentTypeForResponseHeader = new MediaType(
				APPLICATION_JWT_MEDIA_TYPE.getType(),
				APPLICATION_JWT_MEDIA_TYPE.getSubtype(),
				charsetForSerializingBody);
		HttpHeaders headers = outputMessage.getHeaders();
		headers.setContentType(contentTypeForResponseHeader);

		long length = JSONEntityHttpMessageConverter.writeToStream(t, outputMessage.getBody(), charsetForSerializingBody);
		if (headers.getContentLength() == -1) {
			headers.setContentLength(length);
		}
	}

	@Override
	public String deserialize(InputStream body, HttpHeaders headers, MediaType type) {
		if (!canRead(Jwt.class, type)) throw new IllegalArgumentException("Cannot read "+type);
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
			return read(String.class, message);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void serializer(OutputStream body, HttpHeaders headers, String toSerializer, MediaType type) {
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
		try {
			write(toSerializer, type, message);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
