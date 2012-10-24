package org.sagebionetworks.repo.model.jdo;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.StorageLocations;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONArrayAdapterImpl;

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

	/**
	 * Extracts storage locations from annotations.
	 *
	 * @throws UnsupportedEncodingException When the blob is not encoded by UTF-8
	 * @throws JSONObjectAdapterException When the blob is not a JSON array
	 *
	 * @return Storage locations or null if the annotations do not contain any storage locations
	 */
	public static StorageLocations getStorageLocations(NamedAnnotations namedAnnos,
			Long node, Long user) throws UnsupportedEncodingException, JSONObjectAdapterException {

		if (node == null) {
			throw new NullPointerException();
		}
		if (user == null) {
			throw new NullPointerException();
		}
		if (namedAnnos == null) {
			throw new NullPointerException();
		}

		final Annotations primaryAnnos = namedAnnos.getPrimaryAnnotations();
		final Map<String, List<byte[]>> blobs = primaryAnnos.getBlobAnnotations();

		// Read attachment data from annotation blobs
		List<AttachmentData> attachmentList = new ArrayList<AttachmentData>();
		List<byte[]> attachments = blobs.get("attachments");
		if (attachments != null) {
			for (byte[] byteArray : attachments) {
				// Packed at NodeTranslationUtils.objectToBytes(). Now unpack it.
				String jsonStr = new String(byteArray, "UTF-8");
				JSONArrayAdapter jsonArray = new JSONArrayAdapterImpl(jsonStr);
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObjectAdapter jsonObj = jsonArray.getJSONObject(i);
					AttachmentData attachment = new AttachmentData();
					attachment.initializeFromJSONObject(jsonObj);
					attachmentList.add(attachment);
				}
			}
		}

		// Read location data from annotation blobs
		List<LocationData> locationList = new ArrayList<LocationData>();
		List<byte[]> locations = blobs.get("locations");
		if (locations != null) {
			for (byte[] byteArray : locations) {
				// Packed at NodeTranslationUtils.objectToBytes(). Now unpack it.
				String jsonStr = new String(byteArray, "UTF-8");
				JSONArrayAdapter jsonArray = new JSONArrayAdapterImpl(jsonStr);
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObjectAdapter jsonObj = jsonArray.getJSONObject(i);
					LocationData location = new LocationData();
					location.initializeFromJSONObject(jsonObj);
					locationList.add(location);
				}
			}
		}

		Map<String, List<String>> strAnnotations = primaryAnnos.getStringAnnotations();
		StorageLocations sl = new StorageLocations(node, user,
				Collections.unmodifiableList(attachmentList),
				Collections.unmodifiableList(locationList), strAnnotations);
		return sl;
	}
}
