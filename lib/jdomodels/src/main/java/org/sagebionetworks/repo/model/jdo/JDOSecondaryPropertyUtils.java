package org.sagebionetworks.repo.model.jdo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

/**
 * Helper utilities for converting between JDOAnnotations and Annotations (DTO).
 * 
 * @author jmhill
 *
 */
public class JDOSecondaryPropertyUtils {

	public static final Charset UTF8 = Charset.forName("UTF-8");

	/**
	 * Compresses the dto into compressed XML bytes using the provided customXStream
	 * @param customXStream a UnmodifiableXStream that has been set up to handle the object type
	 * @param dto the object to be serialized and compressed
	 * @return byte[] of compressed XML representing the dto object.
	 * @throws IOException
	 */
	@Deprecated
	public static byte[] compressObject(UnmodifiableXStream customXStream, Object dto) throws IOException {
		if(dto == null) return null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream zipper = new GZIPOutputStream(out);
		try(Writer zipWriter = new OutputStreamWriter(zipper, UTF8);){
			customXStream.toXML(dto, zipWriter);
		}
		return out.toByteArray();
	}

	/**
	 * Decompress and deserialize compressed XML bytes into an object using the provided customXStream
	 * @param customXStream a UnmodifiableXStream that has been set up to handle the object type
	 * @param zippedBytes byte[] of compressed XML representing the dto object.
	 * @return the object that the bytes represented
	 * @throws IOException
	 */
	@Deprecated
	public static Object decompressObject(UnmodifiableXStream customXStream, byte[] zippedBytes) throws IOException{
		if(zippedBytes == null){
			return null;
		}

		ByteArrayInputStream in = new ByteArrayInputStream(zippedBytes);
		GZIPInputStream unZipper = new GZIPInputStream(in);
		try(Reader reader = new InputStreamReader(unZipper, UTF8);){
			return customXStream.fromXML(unZipper);
		}
	}
	
	/**
	 * Wraps a call to
	 * {@link EntityFactory#createEntityFromJSONString(String, Class)} with check
	 * exceptions thrown as {@link RuntimeException}.
	 * 
	 * @param <T>
	 * @param type
	 * @param json
	 * @return
	 */
	public static <T extends JSONEntity> T createObjectFromJSON(Class<? extends T> type, String json) {
		if (json == null) {
			return null;
		}
		try {
			return EntityFactory.createEntityFromJSONString(json, type);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Wraps a call to {@link EntityFactory#createJSONStringForEntity(JSONEntity)}
	 * with checked exceptions thrown as {@link RuntimeException}
	 * 
	 * @param <T>
	 * @param object
	 * @return
	 */
	public static <T extends JSONEntity> String createJSONFromObject(T object) {
		if (object == null) {
			return null;
		}
		try {
			return EntityFactory.createJSONStringForEntity(object);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Read the given JSON into a list of the given type items.
	 * 
	 * @param <T>
	 * @param json
	 * @return
	 */
	public static <T extends JSONEntity> List<T> readJsonToEntityList(String json, Class<T> clazz) {
		if (json == null) {
			return null;
		}
		try {
			return EntityFactory.readFromJSONArrayString(json, clazz);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Write the given list of entity reference items to JSON.
	 * @param <T>
	 * @param items
	 * @return
	 */
	public static <T extends JSONEntity> String writeEntityListToJson(List<T> items) {
		if (items == null) {
			return null;
		}
		try {
			return EntityFactory.writeToJSONArrayString(items);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public static List<String> readJsonToStringList(String json) {
		if (json == null) {
			return null;
		}
		
		JSONArray array = new JSONArray(json);
		
		List<String> list = new ArrayList<String>(array.length());
		
		for (int i = 0; (i < array.length()); i++) {
			list.add(array.getString(i));
		}
		
		return list;
	}
	
	public static String writeStringListToJson(List<String> items) {
		if (items == null) {
			return null;
		}
		
		return new JSONArray(items).toString();
	}
}
