package org.sagebionetworks.repo.model.jdo;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;

import com.thoughtworks.xstream.XStream;

/**
 * Helper utilities for converting between JDOAnnotations and Annotations (DTO).
 * 
 * @author jmhill
 *
 */
public class JDOSecondaryPropertyUtils {
	

	/**
	 * Merge all of the annotations in the map into a single set.
	 * @param nodeId
	 * @param dto
	 * @return
	 */
	public static Annotations mergeAnnotations(NamedAnnotations dto){
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
		if(dto == null) return null;
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
		if(dto == null) return null;
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
	public static NamedAnnotations createFromJDO(DBORevision rev) throws IOException{
		if(rev == null) throw new IllegalArgumentException("JDOAnnotations cannot be null");
		return decompressedAnnotations(rev.getAnnotations());
	}
	

}
