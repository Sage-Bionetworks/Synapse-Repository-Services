package org.sagebionetworks.repo.model.jdo;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.repo.model.AnnotationNameSpace;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.table.AnnotationDTO;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.ValidateArgument;

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
	 * Merge all of the annotations in the map into a single set.
	 * @param nodeId
	 * @param dto
	 * @return
	 */
	public static Annotations mergeAnnotations(NamedAnnotations dto){
		Annotations merged = new Annotations();
		if(dto != null){
			Iterator<AnnotationNameSpace> it = dto.nameIterator();
			while(it.hasNext()){
				Annotations toMerge = dto.getAnnotationsForName(it.next());
				merged.addAll(toMerge);
			}
		}
		return merged;
	}
	
	/**
	 * Prepare annotations to be written to the database.
	 * 
	 * @param annos
	 * @param ownerId
	 * @return
	 */
	public static Annotations prepareAnnotationsForDBReplacement(
			NamedAnnotations annos, String ownerId) {
		// Replace the annotations in the tables
		Annotations merged = JDOSecondaryPropertyUtils.mergeAnnotations(annos);
		merged.setId(ownerId);
		// We need to add all data types to the strings, to support mixed query
		merged = JDOSecondaryPropertyUtils.addAllToStrings(merged);
		return merged;
	}
	
	/**
	 * Build a set of annotations where all of the key-value-pairs are distinct.
	 * @param annos
	 */
	public static Annotations buildDistinctAnnotations(Annotations annos){
		// The resulting annotations will have no duplicates.
		Annotations distinct = new Annotations();
		distinct.setId(annos.getId());
		distinct.setStringAnnotations(buildDistinctMap(annos.getStringAnnotations()));
		distinct.setDoubleAnnotations(buildDistinctMap(annos.getDoubleAnnotations()));
		distinct.setLongAnnotations(buildDistinctMap(annos.getLongAnnotations()));
		distinct.setDateAnnotations(buildDistinctMap(annos.getDateAnnotations()));
		// Do not include blobs.
//		distinct.setBlobAnnotations(buildDistinctMap(annos.getBlobAnnotations()));
		return distinct;
	}
	
	/**
	 * Build a copy of the passed map that contains only distinct key-value pairs
	 * @param original
	 * @return
	 */
	public static <T> Map<String, List<T>> buildDistinctMap(Map<String, List<T>> original){
		 Map<String, List<T>> distinct = new HashMap<String, List<T>>();
		 Set<String> set = new HashSet<String>();
		 for(String key: original.keySet()){
			 List<T> values = original.get(key);
			 if(values != null){
				 for(T value: values){
					 if(value != null){
						 String compoundKey = key+"+"+value;
						 if(set.add(compoundKey)){
							 List<T> list = distinct.get(key);
							 if(list == null){
								 list = new LinkedList<T>();
								 distinct.put(key, list);
							 }
							 list.add(value);
						 }						 
					 }
				 }
			 }
		 }
		 return distinct;
	}
	
	/**
	 * Add all annotaions to strings.
	 * @param annos
	 * @return
	 */
	public static Annotations addAllToStrings(Annotations annos){
		// Add all longs
		if(annos.getLongAnnotations() != null){
			for(String key: annos.getLongAnnotations().keySet()){
				List<Long> list = annos.getLongAnnotations().get(key);
				if(list != null){
					for(Long value: list){
						if(value != null){
							annos.addAnnotation(key, Long.toString(value));
						}
					}
				}
			}
		}
		// Doubles
		if(annos.getDoubleAnnotations() != null){
			for(String key: annos.getDoubleAnnotations().keySet()){
				List<Double> list = annos.getDoubleAnnotations().get(key);
				if(list != null){
					for(Double value: list){
						if(value != null){
							annos.addAnnotation(key, Double.toString(value));
						}
					}
				}
			}
		}
		// dates
		if(annos.getDateAnnotations() != null){
			for(String key: annos.getDateAnnotations().keySet()){
				List<Date> list = annos.getDateAnnotations().get(key);
				if(list != null){
					for(Date value: list){
						if(value != null){
							annos.addAnnotation(key, Long.toString(value.getTime()));
						}
					}
				}
			}
		}
		return annos;
	}
	
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

	public static String toXml(NamedAnnotations dto) throws IOException{
		StringWriter writer = new StringWriter();
		try{
			XStream xstream = createXStream();
			xstream.toXML(dto, writer);
			return writer.toString();
		}finally{
			writer.close();
		}
	}
	
	public static String toXml(Map<String, Set<Reference>> references) throws IOException{
		StringWriter writer = new StringWriter();
		try{
			XStream xstream = createXStream();
			xstream.toXML(references, writer);
			return writer.toString();
		}finally{
			writer.close();
		}
	}
	
	public static NamedAnnotations fromXml(String xml) throws IOException{
		StringReader reader = new StringReader(xml);
		try{
			XStream xstream = createXStream();
			return (NamedAnnotations) xstream.fromXML(reader);
		}finally{
			
		}
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
	 * Create a new Annotations object from the JDO.
	 * @param jdo
	 * @return
	 * @throws IOException 
	 */
	public static NamedAnnotations createFromJDO(DBORevision rev) throws IOException{
		if(rev == null) throw new IllegalArgumentException("JDOAnnotations cannot be null");
		return decompressedAnnotations(rev.getAnnotations());
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
			// add primary
			addAnnotations(entityId, maxAnnotationChars, map, annos.getPrimaryAnnotations());
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
