package org.sagebionetworks.repo.model.jdo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.table.AnnotationDTO;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.util.Pair;

import com.thoughtworks.xstream.XStream;

/**
 * Helper utilities for converting between JDOAnnotations and Annotations (DTO).
 * 
 * @author jmhill
 *
 */
public class JDOSecondaryPropertyUtils {

	public static final Charset UTF8 = Charset.forName("UTF-8");

	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder()
			.omitField(Annotations.class, "id")
			.omitField(Annotations.class, "etag")
			.omitField(NamedAnnotations.class, "id")
			.omitField(NamedAnnotations.class, "etag")
			.allowTypes(NamedAnnotations.class, Annotations.class, Reference.class)
			.allowTypeHierarchy(Object.class) //TODO: remove if not necessary later in refactor
			.alias("annotations", Annotations.class)
			.alias("name-space", NamedAnnotations.class)
			.build();

	/**
	 * Convert the passed annotations to a compressed (zip) byte array
	 * @param dto
	 * @return compressed annotations
	 * @throws IOException 
	 */
	public static byte[] compressAnnotations(NamedAnnotations dto) throws IOException{
		return compressObject(dto == null || dto.isEmpty() ? null : dto);
	}

	public static byte[] compressObject(Object dto) throws IOException {
		return compressObject(X_STREAM, dto);
	}

	public static byte[] compressObject(UnmodifiableXStream customXStream, Object dto) throws IOException {
		if(dto == null) return null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream zipper = new GZIPOutputStream(out);
		try(Writer zipWriter = new OutputStreamWriter(zipper, UTF8);){
			X_STREAM.toXML(dto, zipWriter);
		}
		return out.toByteArray();
	}
	
	/**
	 * Convert the passed reference to a compressed (zip) byte array
	 * @param dto
	 * @return the compressed reference
	 */
	public static byte[] compressReference(Reference dto) {
		try {
			return compressObject(dto);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Read the compressed (zip) byte array into the Annotations.
	 * @param zippedBytes
	 * @return the resurrected Annotations
	 * @throws IOException 
	 */
	public static NamedAnnotations decompressedAnnotations(byte[] zippedBytes) throws IOException{
		Object o = decompressedObject(zippedBytes);
		if (o==null) return new NamedAnnotations();
		return (NamedAnnotations)o;
	}
		
	public static Object decompressedObject(byte[] zippedBytes) throws IOException{
		return decompressedObject(X_STREAM, zippedBytes);
	}


	public static Object decompressedObject(UnmodifiableXStream customXStream, byte[] zippedBytes) throws IOException{
		if(zippedBytes != null){
			ByteArrayInputStream in = new ByteArrayInputStream(zippedBytes);
			try(GZIPInputStream unZipper = new GZIPInputStream(in);){
				return customXStream.fromXML(unZipper);
			}
		}
		return null;
	}


	/**
	 * Read the compressed (zip) byte array into the Reference.
	 * @param zippedByes
	 * @return the resurrected Reference
	 * @throws IOException 
	 */
	public static Reference decompressedReference(byte[] zippedByes) throws IOException{
		return (Reference) decompressedObject(zippedByes);
	}

	/**
	 * Get a single string value from a list of objects.
	 * @param values
	 * @param maxAnnotationChars the maximum number of characters for any annotation value.
	 * @return
	 */
	public static String getSingleString(List values, int maxAnnotationChars){
		if(values == null){
			return null;
		}
		if(values.isEmpty()){
			return null;
		}
		Object value = values.get(0);
		if(value == null){
			return null;
		}
		String stringValue = null;
		if(value instanceof Date){
			stringValue = ""+((Date)value).getTime();
		}else{
			stringValue = value.toString();
		}
		if(stringValue.length() > maxAnnotationChars){
			stringValue = stringValue.substring(0, maxAnnotationChars);
		}
		return stringValue;
	}

	/**
	 * Translate from NamedAnnotations to a list of AnnotationDTO.
	 * @param annos
	 * @param maxAnnotationChars the maximum number of characters for any annotation value.
	 * @return
	 */
	public static List<AnnotationDTO> translate(Long entityId, NamedAnnotations annos, int maxAnnotationChars) {
		LinkedHashMap<String, AnnotationDTO> map = new LinkedHashMap<>();
		if(annos != null){
			// add additional
			addAnnotations(entityId, maxAnnotationChars, map, annos.getAdditionalAnnotations());
		}
		// build the results from the map
		List<AnnotationDTO> results = new LinkedList<AnnotationDTO>();
		for(String key: map.keySet()){
			results.add(map.get(key));
		}
		return results;
	}

	static void addAnnotations(Long entityId, int maxAnnotationChars,
			LinkedHashMap<String, AnnotationDTO> map, Annotations additional) {
		for(String key: additional.getStringAnnotations().keySet()){
			List values = additional.getStringAnnotations().get(key);
			String value = getSingleString(values, maxAnnotationChars);
			if(value != null){
				map.put(key, new AnnotationDTO(entityId, key, AnnotationType.STRING, value));
			}
		}
		// longs
		for(String key: additional.getLongAnnotations().keySet()){
			List values = additional.getLongAnnotations().get(key);
			String value = getSingleString(values, maxAnnotationChars);
			if(value != null){
				map.put(key, new AnnotationDTO(entityId, key, AnnotationType.LONG, value));
			}
		}
		// doubles
		for(String key: additional.getDoubleAnnotations().keySet()){
			List values = additional.getDoubleAnnotations().get(key);
			String value = getSingleString(values, maxAnnotationChars);
			if(value != null){
				map.put(key, new AnnotationDTO(entityId, key, AnnotationType.DOUBLE, value));
			}
		}
		// dates
		for(String key: additional.getDateAnnotations().keySet()){
			List values = additional.getDateAnnotations().get(key);
			String value = getSingleString(values, maxAnnotationChars);
			if(value != null){
				map.put(key, new AnnotationDTO(entityId, key, AnnotationType.DATE, value));
			}
		}
	}
}
