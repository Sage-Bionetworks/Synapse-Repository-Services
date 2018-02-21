package org.sagebionetworks.repo.model.jdo;

import java.io.BufferedOutputStream;
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

	/**
	 * Convert the passed annotations to a compressed (zip) byte array
	 * @param dto
	 * @return compressed annotations
	 * @throws IOException 
	 */
	public static byte[] compressAnnotations(NamedAnnotations dto) throws IOException{
		return compressObject(dto);
	}
	
	public static byte[] compressObject(Object dto) throws IOException{
		if(dto == null) return null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BufferedOutputStream buff = new BufferedOutputStream(out);
		GZIPOutputStream zipper = new GZIPOutputStream(buff);
		Writer zipWriter = new OutputStreamWriter(zipper, UTF8);
		try{
			XStream xstream = createXStream();
			xstream.toXML(dto, zipWriter);
		}finally{
			IOUtils.closeQuietly(zipWriter);
		}
		return out.toByteArray();
	}
	
	public static byte[] compressObject(Object dto, String classAlias) throws IOException{
		if(dto == null) return null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BufferedOutputStream buff = new BufferedOutputStream(out);
		GZIPOutputStream zipper = new GZIPOutputStream(buff);
		Writer zipWriter = new OutputStreamWriter(zipper, UTF8);
		try{
			XStream xstream = createXStream();
			xstream.alias(classAlias, dto.getClass());
			xstream.toXML(dto, zipWriter);
		}finally{
			IOUtils.closeQuietly(zipWriter);
		}
		return out.toByteArray();
	}
	
	/**
	 * Convert the passed reference to a compressed (zip) byte array
	 * @param dto
	 * @return the compressed reference
	 * @throws IOException 
	 */
	public static byte[] compressReference(Reference dto) throws IOException{
		if(dto == null) return null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BufferedOutputStream buff = new BufferedOutputStream(out);
		GZIPOutputStream zipper = new GZIPOutputStream(buff);
		Writer zipWriter = new OutputStreamWriter(zipper, UTF8);
		try{
			XStream xstream = createXStream();
			xstream.toXML(dto, zipWriter);
		}finally{
			IOUtils.closeQuietly(zipWriter);
		}
		return out.toByteArray();
	}
	
	/**
	 * Convert the passed references to a compressed (zip) byte array
	 * @param dto
	 * @return the compressed references
	 * @throws IOException 
	 */
	@Deprecated
	public static byte[] compressReferences(Map<String, Set<Reference>> dto) throws IOException{
		if(dto == null) return null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BufferedOutputStream buff = new BufferedOutputStream(out);
		GZIPOutputStream zipper = new GZIPOutputStream(buff);
		Writer zipWriter = new OutputStreamWriter(zipper, UTF8);
		try{
			XStream xstream = createXStream();
			xstream.toXML(dto, zipWriter);
		}finally{
			IOUtils.closeQuietly(zipWriter);
		}
		return out.toByteArray();
	}

	public static XStream createXStream() {
		XStream xstream = new XStream();
		xstream.alias("annotations", Annotations.class);
		xstream.alias("name-space", NamedAnnotations.class);
		return xstream;
	}
	
	/**
	 * Read the compressed (zip) byte array into the Annotations.
	 * @param zippedByes
	 * @return the resurrected Annotations
	 * @throws IOException 
	 */
	public static NamedAnnotations decompressedAnnotations(byte[] zippedByes) throws IOException{
		Object o = decompressedObject(zippedByes);
		if (o==null) return new NamedAnnotations();
		return (NamedAnnotations)o;
	}
		
	public static Object decompressedObject(byte[] zippedByes) throws IOException{
		if(zippedByes != null){
			ByteArrayInputStream in = new ByteArrayInputStream(zippedByes);
			GZIPInputStream unZipper = new GZIPInputStream(in);
			try{
				XStream xstream = createXStream();
				if(zippedByes != null){
					return xstream.fromXML(unZipper);
				}
			}finally{
				unZipper.close();
			}			
		}
		return null;
	}

	public static Object decompressedObject(byte[] zippedByes, List<Pair<String,Class>> aliases) throws IOException{
		if(zippedByes != null){
			ByteArrayInputStream in = new ByteArrayInputStream(zippedByes);
			GZIPInputStream unZipper = new GZIPInputStream(in);
			try{
				XStream xstream = createXStream();
				for (Pair<String,Class> pair : aliases) {
					xstream.alias(pair.getFirst(), pair.getSecond());
				}
				if(zippedByes != null){
					return xstream.fromXML(unZipper);
				}
			}finally{
				unZipper.close();
			}
		}
		return null;
	}

	public static Object decompressedObject(byte[] zippedByes, String classAlias, Class aliasedClass) throws IOException{
		Pair<String,Class> pair = new Pair<String,Class>(classAlias, aliasedClass);
		return decompressedObject(zippedByes, Collections.singletonList(pair));
	}

	/**
	 * Read the compressed (zip) byte array into the Reference.
	 * @param zippedByes
	 * @return the resurrected Reference
	 * @throws IOException 
	 */
	public static Reference decompressedReference(byte[] zippedByes) throws IOException{
		if(zippedByes != null){
			ByteArrayInputStream in = new ByteArrayInputStream(zippedByes);
			GZIPInputStream unZipper = new GZIPInputStream(in);
			try{
				XStream xstream = createXStream();
				if(zippedByes != null){
					return (Reference) xstream.fromXML(unZipper);
				}
			}finally{
				unZipper.close();
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
	@Deprecated
	public static Map<String, Set<Reference>> decompressedReferences(byte[] zippedByes) throws IOException{
		if(zippedByes != null){
			ByteArrayInputStream in = new ByteArrayInputStream(zippedByes);
			GZIPInputStream unZipper = new GZIPInputStream(in);
			try{
				XStream xstream = createXStream();
				if(zippedByes != null){
					return (Map<String, Set<Reference>>) xstream.fromXML(unZipper);
				}
			}finally{
				unZipper.close();
			}			
		}
		// Return an empty map.
		return new HashMap<String, Set<Reference>>();
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
