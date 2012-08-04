package org.sagebionetworks.repo.web.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.util.JSONEntityUtil;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class JSONEntityHttpMessageConverter implements
		HttpMessageConverter<JSONEntity> {

	private List<MediaType> supportedMedia;

	public JSONEntityHttpMessageConverter() {
		supportedMedia = new ArrayList<MediaType>();
		supportedMedia.add(MediaType.APPLICATION_JSON);
	}

	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return isJSONType(mediaType) && JSONEntityUtil.isJSONEntity(clazz);
	}
	
	public static boolean isJSONType(MediaType type){
		if(type == null) return false;
		if(type.getType() == null) return false;
		if(type.getSubtype() == null) return false;
		if(!"application".equals(type.getType().toLowerCase())) return false;
		if(!"json".equals(type.getSubtype().toLowerCase())) return false;
		return true;
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return isJSONType(mediaType) && JSONEntityUtil.isJSONEntity(clazz);
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return supportedMedia;
	}

	@Override
	public JSONEntity read(Class<? extends JSONEntity> clazz,
			HttpInputMessage inputMessage) throws IOException,
			HttpMessageNotReadableException {
		// First read the string
		String jsonString = JSONEntityHttpMessageConverter.readToString(inputMessage.getBody(), inputMessage.getHeaders().getContentType().getCharSet());
		try {
			return EntityFactory.createEntityFromJSONString(jsonString, clazz);
		} catch (JSONObjectAdapterException e) {
			throw new HttpMessageNotReadableException(e.getMessage());
		}
	}

	/**
	 * Read a string from an input stream
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static String readToString(InputStream in, Charset charSet)
			throws IOException {
		if(in == null) throw new IllegalArgumentException("No content to map to Object due to end of input");
		try {
			if(charSet == null){
				charSet = Charset.defaultCharset();
			}
			BufferedInputStream bufferd = new BufferedInputStream(in);
			byte[] buffer = new byte[1024];
			StringBuilder builder = new StringBuilder();
			int index = -1;
			while ((index = bufferd.read(buffer, 0, buffer.length)) > 0) {
				builder.append(new String(buffer, 0, index, charSet));
			}
			return builder.toString();
		} finally {
			in.close();
		}
	}
	
	/**
	 * Read a string from an input stream
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static String readToString(Reader reader) throws IOException {
		if(reader == null) throw new IllegalArgumentException("Reader cannot be null");
		try {
			char[] buffer = new char[1024];
			StringBuilder builder = new StringBuilder();
			int index = -1;
			while ((index = reader.read(buffer, 0, buffer.length)) > 0) {
				builder.append(buffer, 0, index);
			}
			return builder.toString();
		} finally {
			reader.close();
		}
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
				charSet = Charset.defaultCharset();
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
	public void write(JSONEntity entity, MediaType contentType,
			HttpOutputMessage outputMessage) throws IOException,
			HttpMessageNotWritableException {
		// First write the entity to a JSON string
		try {
			HttpHeaders headers = outputMessage.getHeaders();
			if (headers.getContentType() == null) {
				if (contentType == null || contentType.isWildcardType() || contentType.isWildcardSubtype()) {
					contentType = MediaType.APPLICATION_JSON;
				}
				if (contentType != null) {
					headers.setContentType(contentType);
				}
			}
			String jsonString = EntityFactory.createJSONStringForEntity(entity);
			long length = JSONEntityHttpMessageConverter.writeToStream(jsonString, outputMessage.getBody(), contentType.getCharSet());
			if (headers.getContentLength() == -1) {
				headers.setContentLength(length);
			}
		} catch (JSONObjectAdapterException e) {
			throw new HttpMessageNotWritableException(e.getMessage());
		}

	}

	/**
	 * Read an entity from the reader.
	 * @param reader
	 * @return
	 * @throws IOException 
	 * @throws JSONObjectAdapterException 
	 */
	public static Entity readEntity(Reader reader) throws IOException, JSONObjectAdapterException {
		// First read in the string
		String jsonString = readToString(reader);
		// Read it into an adapter
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonString);
		return createEntityFromeAdapter(adapter);
	}

	/**
	 * There are many things that can go wrong with this and we want to make sure the error messages
	 * are always meaningful.
	 * @param adapter
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	public static Entity createEntityFromeAdapter(JSONObjectAdapter adapter)
			throws JSONObjectAdapterException {
		// Get the entity type
		if(!adapter.has("entityType")){
			throw new IllegalArgumentException("Cannot determine the entity type.  The entityType property is null");
		}
		String typeClassName = adapter.getString("entityType");
		if(typeClassName == null){
			throw new IllegalArgumentException("Cannot determine the entity type.  The entityType property is null");
		}
		// Create a new instance using the full class name
		Entity newInstance = null;
		try {
			// 
			Class<? extends Entity> entityClass = (Class<? extends Entity>) Class.forName(typeClassName);
			newInstance = entityClass.newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Unknown entity type: "+typeClassName+". Message: "+e.getMessage());
		}
		// Populate the new instance with the JSON.
		newInstance.initializeFromJSONObject(adapter);
		return newInstance;
	}

}
