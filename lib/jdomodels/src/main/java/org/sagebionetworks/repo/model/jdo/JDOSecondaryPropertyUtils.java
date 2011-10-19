package org.sagebionetworks.repo.model.jdo;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.jdo.persistence.JDOBlobAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODateAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODoubleAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLongAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.jdo.persistence.JDORevision;
import org.sagebionetworks.repo.model.jdo.persistence.JDOStringAnnotation;

import com.thoughtworks.xstream.XStream;

/**
 * Helper utilities for converting between JDOAnnotations and Annotations (DTO).
 * 
 * @author jmhill
 *
 */
public class JDOSecondaryPropertyUtils {
	

	/**
	 * Update the JDO from the DTO
	 * @param dto
	 * @param jdo
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
	public static void updateFromJdoFromDto(NamedAnnotations dto, JDONode jdo, JDORevision rev) throws IOException {
		updateAnnotationsFromDto(mergeAnnotations(dto), jdo);
		// Compress the annotations and save them in a blob
		rev.setAnnotations(compressAnnotations(dto));
	}
	
	/**
	 * Merge all of the annotations in the map into a single set.
	 * @param nodeId
	 * @param dto
	 * @return
	 */
	private static Annotations mergeAnnotations(NamedAnnotations dto){
		Annotations merged = new Annotations();
		if(dto != null){
			Iterator<String> it = dto.nameIterator();
			while(it.hasNext()){
				Annotations toMerge = dto.getAnnotationsForName(it.next());
				merged.addAll(toMerge);
			}
		}
		return merged;
	}
	
	@SuppressWarnings("unchecked")
	public static void updateAnnotationsFromDto(NamedAnnotations dto, JDONode jdo) {
		// Merge all of the annotations
		Annotations merged = mergeAnnotations(dto);
		updateAnnotationsFromDto(merged, jdo);
	}
	/**
	 * This version will update the annotations on the node.  This will update the annotation tables used
	 * for query.
	 * @param dto
	 * @param jdo
	 */
	@SuppressWarnings("unchecked")
	public static void updateAnnotationsFromDto(Annotations dto, JDONode jdo) {
		jdo.setStringAnnotations((Set<JDOStringAnnotation>)createFromMap(jdo, dto.getStringAnnotations()));
		jdo.setDateAnnotations((Set<JDODateAnnotation>)createFromMap(jdo, dto.getDateAnnotations()));
		jdo.setLongAnnotations((Set<JDOLongAnnotation>)createFromMap(jdo, dto.getLongAnnotations()));
		jdo.setDoubleAnnotations((Set<JDODoubleAnnotation>)createFromMap(jdo, dto.getDoubleAnnotations()));
		jdo.setBlobAnnotations((Set<JDOBlobAnnotation>)createFromMap(jdo, dto.getBlobAnnotations()));
	}
	
	/**
	 * Convert the passed annotations to a compressed (zip) byte array
	 * @param dto
	 * @return compressed annotations
	 * @throws IOException 
	 */
	public static byte[] compressAnnotations(NamedAnnotations dto) throws IOException{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BufferedOutputStream buff = new BufferedOutputStream(out);
		GZIPOutputStream zipper = new GZIPOutputStream(buff);
		try{
			XStream xstream = createXStream();
			xstream.toXML(dto, zipper);
			zipper.flush();
			zipper.close();
			return out.toByteArray();
		}finally{
			zipper.flush();
			zipper.close();
		}
	}
	
	/**
	 * Convert the passed references to a compressed (zip) byte array
	 * @param dto
	 * @return the compressed references
	 * @throws IOException 
	 */
	public static byte[] compressReferences(Map<String, Set<Reference>> dto) throws IOException{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BufferedOutputStream buff = new BufferedOutputStream(out);
		GZIPOutputStream zipper = new GZIPOutputStream(buff);
		try{
			XStream xstream = createXStream();
			xstream.toXML(dto, zipper);
			zipper.flush();
			zipper.close();
			return out.toByteArray();
		}finally{
			zipper.flush();
			zipper.close();
		}
	}

	public static String toXml(NamedAnnotations dto) throws IOException{
		StringWriter writer = new StringWriter();
		try{
			XStream xstream = createXStream();
			xstream.toXML(dto, writer);
			return writer.toString();
		}finally{
			
		}
	}
	
	@SuppressWarnings("unchecked")
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
	@SuppressWarnings("unchecked")
	public static NamedAnnotations decompressedAnnotations(byte[] zippedByes) throws IOException{
		if(zippedByes != null){
			ByteArrayInputStream in = new ByteArrayInputStream(zippedByes);
			GZIPInputStream unZipper = new GZIPInputStream(in);
			try{
				XStream xstream = createXStream();
				if(zippedByes != null){
					return (NamedAnnotations) xstream.fromXML(unZipper);
				}
			}finally{
				unZipper.close();
			}			
		}
		// Return an empty map.
		return new NamedAnnotations();
	}

	/**
	 * Read the compressed (zip) byte array into the References.
	 * @param zippedByes
	 * @return the resurrected References
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
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
	public static NamedAnnotations createFromJDO(JDORevision rev) throws IOException{
		if(rev == null) throw new IllegalArgumentException("JDOAnnotations cannot be null");
		return decompressedAnnotations(rev.getAnnotations());
	}
	
	/**
	 * Build up the map from the set.
	 * @param <A>
	 * @param set
	 * @return
	 */
	public static <A> Map<String, Collection<A>> createFromSet(Set<? extends JDOAnnotation<A>> set){
		Map<String, Collection<A>> map = new HashMap<String, Collection<A>>();
		if(set != null){
			Iterator<? extends JDOAnnotation<A>> it = set.iterator();
			while(it.hasNext()){
				JDOAnnotation<A> jdoAno = it.next();
				String key = jdoAno.getAttribute();
				A value = jdoAno.getValue();
				Collection<A> collection = map.get(key);
				if(collection == null){
					collection = new ArrayList<A>();
					map.put(key, collection);
				}
				collection.add(value);
			}
		}
		return map;
	}
	
	/**
	 * Create a set of JDOAnnotations from a map
	 * @param <T>
	 * @param owner
	 * @param annotation
	 * @return
	 */
	public static <T> Set<? extends JDOAnnotation<T>> createFromMap(JDONode owner, Map<String, Collection<T>> annotation){
		Set<JDOAnnotation<T>> set = new HashSet<JDOAnnotation<T>>();
		if(annotation != null){
			Iterator<String> keyIt = annotation.keySet().iterator();
			while(keyIt.hasNext()){
				String key = keyIt.next();
				Collection<T> valueColection = annotation.get(key);
				Iterator<T> valueIt = valueColection.iterator();
				while(valueIt.hasNext()){
					T value = valueIt.next();
					JDOAnnotation<T> jdo = createAnnotation(owner, key, value);
					set.add(jdo);
				}
			}
		}
		return set;
	}
	
	/**
	 * Create a single JDOAnnotation.
	 * @param <T>
	 * @param owner
	 * @param key
	 * @param value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> JDOAnnotation<T> createAnnotation(JDONode owner, String key, T value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		JDOAnnotation<T>  jdo = null;
		if(value instanceof String){
			JDOStringAnnotation temp =  new JDOStringAnnotation();
			temp.setOwner(owner);
			jdo = (JDOAnnotation<T>) temp;
		}else if(value instanceof Date){
			JDODateAnnotation temp =  new JDODateAnnotation();
			temp.setOwner(owner);
			jdo = (JDOAnnotation<T>) temp;
		}else if(value instanceof Long){
			JDOLongAnnotation temp =  new JDOLongAnnotation();
			temp.setOwner(owner);
			jdo = (JDOAnnotation<T>) temp;
		}else if(value instanceof Double){
			JDODoubleAnnotation temp =  new JDODoubleAnnotation();
			temp.setOwner(owner);
			jdo = (JDOAnnotation<T>) temp;
		}else if(value instanceof byte[]){
			JDOBlobAnnotation temp =  new JDOBlobAnnotation();
			temp.setOwner(owner);
			jdo = (JDOAnnotation<T>) temp;
		}else{
			throw new IllegalArgumentException("Unknown annoation type: "+value.getClass());
		}
		jdo.setAttribute(key);
		jdo.setValue(value);
		return jdo;
	}

}
