package org.sagebionetworks.repo.web.controller;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.repo.util.JSONEntityUtil;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;


public class JSONEntityHttpMessageConverter implements	HttpMessageConverter<JSONEntity> {
	private static final String UTF_8 = "UTF-8";
	// This is the character set used by Synapse if the client does not specify one
	private static final Charset SYNAPSE_DEFAULT_CHARSET = Charset.forName(UTF_8);

	private List<MediaType> supportedMedia;
	private Set<Class <? extends JSONEntity>> classesToValidateConversion;
	/**
	 * When set to true, this message converter will attempt to convert any object to JSON.
	 */
	boolean convertAnyRequestToJson = false;
	
	/**
	 *  When set to true, this message converter will attempt to convert any object to JSON
	 *  regardless of the requested type.
	 *  
	 * @param convertAnyRequestToJson
	 */
	public void setConvertAnyRequestToJson(boolean convertAnyRequestToJson) {
		this.convertAnyRequestToJson = convertAnyRequestToJson;
	}

	public JSONEntityHttpMessageConverter(Set<Class <? extends JSONEntity>> classesToValidateConversion) {
		ValidateArgument.required(classesToValidateConversion, "classesToValidateConversion");
		supportedMedia = new ArrayList<MediaType>();
		supportedMedia.add(MediaType.APPLICATION_JSON);
		supportedMedia.add(MediaType.TEXT_PLAIN);
		this.classesToValidateConversion = classesToValidateConversion;
	}

	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		// Does the class implement JSONEntity a JSONEntity?
		if(!JSONEntityUtil.isJSONEntity(clazz)) return false;
		// Are we converting any request to json?
		if(convertAnyRequestToJson) return true;
		// Is the requested type a json type?
		return isJSONType(mediaType);
	}

	public static boolean isJSONType(MediaType type) { 
		return MediaType.APPLICATION_JSON.equalsTypeAndSubtype(type);
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return MediaType.TEXT_PLAIN.includes(mediaType) || 
				(isJSONType(mediaType) && JSONEntityUtil.isJSONEntity(clazz));
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return supportedMedia;
	}

	@Override
	public JSONEntity read(Class<? extends JSONEntity> clazz, HttpInputMessage inputMessage) throws IOException,
			HttpMessageNotReadableException, IllegalArgumentException {
		Charset charsetForDeSerializingBody = inputMessage.getHeaders().getContentType().getCharset();
		String jsonEncoded = JSONEntityHttpMessageConverterHelper.readToString(inputMessage.getBody(), charsetForDeSerializingBody);
		return JSONEntityHttpMessageConverterHelper.read(
				jsonEncoded, charsetForDeSerializingBody, clazz, classesToValidateConversion);
	}

	/**
	 * Write a string to an oupt stream
	 * @param toWrite
	 * @param out
	 * @param charSet
	 * @throws IOException
	 */
	public static long writeToStream(String toWrite, OutputStream out,	Charset charSet) throws IOException {
		try {
			if(charSet == null){
				charSet = Charset.forName(UTF_8);
			}
			BufferedOutputStream bufferd = new BufferedOutputStream(out);
			byte[] bytes = toWrite.getBytes(charSet);
			bufferd.write(bytes);
			bufferd.flush();
			return bytes.length;
		} finally {
			out.close();
		}
	}

	@Override
	public void write(JSONEntity entity, final MediaType contentType,
			HttpOutputMessage outputMessage) throws IOException,
			HttpMessageNotWritableException {
		// First write the entity to a JSON string
		try {
			MediaType contentTypeForResponseHeader = contentType;
			if (contentTypeForResponseHeader.isWildcardType() || contentTypeForResponseHeader.isWildcardSubtype()) {
				// this will leave the character set unspecified, but we fill that in below
				contentTypeForResponseHeader = MediaType.APPLICATION_JSON;
			}
			Charset charsetForSerializingBody = contentTypeForResponseHeader.getCharset();
			if (charsetForSerializingBody==null) {
				charsetForSerializingBody = SYNAPSE_DEFAULT_CHARSET;
				// Let's make it explicit in the response header
				contentTypeForResponseHeader = new MediaType(
						contentTypeForResponseHeader.getType(),
						contentTypeForResponseHeader.getSubtype(),
						charsetForSerializingBody
				);
			}
			HttpHeaders headers = outputMessage.getHeaders();
			headers.setContentType(contentTypeForResponseHeader);
			String jsonString;
			if (contentTypeForResponseHeader.includes(MediaType.TEXT_PLAIN)) {
				jsonString = convertEntityToPlainText(entity);
			} else {
				jsonString = EntityFactory.createJSONStringForEntity(entity);
			}
			long length = JSONEntityHttpMessageConverter.writeToStream(jsonString, outputMessage.getBody(), charsetForSerializingBody);
			if (headers.getContentLength() == -1) {
				headers.setContentLength(length);
			}
		} catch (JSONObjectAdapterException e) {
			throw new HttpMessageNotWritableException(e.getMessage(), e);
		}

	}
	
	public static String convertEntityToPlainText(JSONEntity entity) throws JSONObjectAdapterException {
		if (entity instanceof ErrorResponse) {
			return ((ErrorResponse)entity).getReason();
		} else {
			return EntityFactory.createJSONStringForEntity(entity);
		}
	}
}
