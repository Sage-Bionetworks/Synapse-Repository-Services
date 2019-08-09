package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.MimeType;

import com.nimbusds.jose.util.IOUtils;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

public class JWTTypeSerializerImpl implements JWTTypeSerializer {
	
	private static final String APPLICATION_TYPE = "application";
	private static final String JWT_SUBTYPE = "jwt";
	private static final MediaType APPLICATION_JWT_MEDIA_TYPE = new MediaType(APPLICATION_TYPE, JWT_SUBTYPE);
	private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");
		

	private static boolean canReadOrWrite(Class<?> clazz, MimeType mediaType) { 
		return clazz.equals(JWT.class) &&
				(mediaType==null || APPLICATION_JWT_MEDIA_TYPE.isCompatibleWith(mediaType));	
	}
	
	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) { 
		return canReadOrWrite(clazz, mediaType);	
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return canReadOrWrite(clazz, mediaType);	
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Arrays.asList(new MediaType[] {APPLICATION_JWT_MEDIA_TYPE});
	}

	@Override
	public Object read(Class<? extends Object> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		if (!JWT.class.equals(clazz)) throw new IllegalArgumentException("Cannot read "+JWT.class);
		Charset charset = inputMessage.getHeaders().getContentType().getCharset();
		if (charset==null) charset = UTF_8_CHARSET; 
		try {
		return JWTParser.parse(IOUtils.readInputStreamToString(inputMessage.getBody(), charset));
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void write(Object t, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		if (!canReadOrWrite(t.getClass(), contentType)) throw new IllegalArgumentException("Cannot write "+contentType);
		JWT jwt = (JWT)t;
		Charset charsetForSerializingBody = contentType.getCharset()==null ? UTF_8_CHARSET : contentType.getCharset();
		MediaType contentTypeForResponseHeader = new MediaType(
				APPLICATION_JWT_MEDIA_TYPE.getType(),
				APPLICATION_JWT_MEDIA_TYPE.getSubtype(),
				charsetForSerializingBody);
		HttpHeaders headers = outputMessage.getHeaders();
		headers.setContentType(contentTypeForResponseHeader);

		long length = JSONEntityHttpMessageConverter.writeToStream(jwt.serialize(), outputMessage.getBody(), charsetForSerializingBody);
		if (headers.getContentLength() == -1) {
			headers.setContentLength(length);
		}
	}

	@Override
	public JWT deserialize(InputStream body, HttpHeaders headers, MediaType type) {
		if (!canRead(JWT.class, type)) throw new IllegalArgumentException("Cannot read "+type);
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
			return (JWT)read(JWT.class, message);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void serializer(OutputStream body, HttpHeaders headers, JWT toSerializer, MediaType type) {
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
