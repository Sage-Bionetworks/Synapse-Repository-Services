package org.sagebionetworks.repo.model.dbo.persistence.table;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import com.thoughtworks.xstream.XStream;

/**
 * Utilities for working with ColumModel objects and DBOColumnModel objects.
 * 
 * @author John
 *
 */
public class ColumnModelUtlis {

	/**
	 * Translate from a DTO to DBO.
	 * @param dto
	 * @return
	 */
	public static DBOColumnModel createDBOFromDTO(ColumnModel dto){
		try {
			// First normalize the DTO
			ColumnModel normal = createNormalizedClone(dto);
			String hash = calculateHash(normal);
			// Create the bytes
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			GZIPOutputStream zip;
			zip = new GZIPOutputStream(out);
			XStream xstream = createXStream();
			xstream.toXML(normal, zip);
			zip.flush();
			zip.close();
			DBOColumnModel dbo = new DBOColumnModel();
			dbo.setBytes(out.toByteArray());
			dbo.setName(normal.getName());
			dbo.setHash(hash);
			if(dto.getId() != null){
				dbo.setId(Long.parseLong(dto.getId()));
			}
			return dbo;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Translate from the DBO to the DTO.
	 * @param dbo
	 * @return
	 */
	public static ColumnModel createDTOFromDBO(DBOColumnModel dbo){
		if(dbo == null) throw new IllegalArgumentException("DBOColumnModel cannot be null");
		if(dbo.getId() == null) throw new IllegalArgumentException("DBOColumnModel.id cannot be null");
		try {
			// First read the bytes.
			XStream xstream = createXStream();
			ByteArrayInputStream in = new ByteArrayInputStream(dbo.getBytes());
			GZIPInputStream zip = new GZIPInputStream(in);
			ColumnModel model = (ColumnModel) xstream.fromXML(zip, new ColumnModel());
			model.setId(Long.toString(dbo.getId()));
			return model;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * Calculate the hash from an object
	 * @param dbo
	 * @return
	 */
	public static String calculateHash(ColumnModel model){
		if(model == null) throw new IllegalArgumentException("ColumnModel cannot be null");
		try {
			// First normalize the model
			ColumnModel normal = createNormalizedClone(model);
			// To JSON
			byte[] jsonBytes = EntityFactory.createJSONStringForEntity(normal).getBytes("UTF-8");
			return calculateSha256(jsonBytes);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 
	}
	/**
	 * Calculate a SHA-256 from the passed bytes.
	 * @param jsonBytes
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	private static String calculateSha256(byte[] bytes)
			throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(bytes);
		byte[] mdbytes = md.digest();
		//convert the byte to hex format method 1
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < mdbytes.length; i++) {
		  sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}
	
	/**
	 * Create a normalized clone of a column model.  This is used to create the hash of the column model.
	 * @param toClone
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	public static ColumnModel createNormalizedClone(ColumnModel toClone) {
		if(toClone == null) throw new IllegalArgumentException("ColumnModel cannot be null");
		if(toClone.getName() == null) throw new IllegalArgumentException("ColumnModel.name cannot be null");
		if(toClone.getColumnType() == null) throw new IllegalArgumentException("ColumnModel.columnType cannot be null");
		try {
			// Create a clone from the JSON
			String json = EntityFactory.createJSONStringForEntity(toClone);
			ColumnModel clone = EntityFactory.createEntityFromJSONString(json, ColumnModel.class);
			// The ID is not part of the normalized form.
			clone.setId(null);
			// to lower on the name
			clone.setName(clone.getName().toLowerCase());
			// Default to lower.
			if(clone.getDefaultValue() != null){
				clone.setDefaultValue(clone.getDefaultValue().toLowerCase());
			}
			if(clone.getEnumValues() != null){
				List<String> newList = new LinkedList<String>();
				for(String enumValue: clone.getEnumValues()){
					newList.add(enumValue.toLowerCase());
				}
				Collections.sort(newList);
				clone.setEnumValues(newList);
			}
			return clone;
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static XStream createXStream(){
		XStream xstream = new XStream();
		xstream.alias("ColumnModel", ColumnModel.class);
		xstream.alias("ColumnType", ColumnType.class);
		return xstream;
	}
}
