package org.sagebionetworks.repo.model.dbo.persistence.table;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.Closer;

import com.thoughtworks.xstream.XStream;

/**
 * Utilities for working with ColumModel objects and DBOColumnModel objects.
 * 
 * @author John
 *
 */
public class ColumnModelUtlis {
	
	/**
	 * The default maximum number of characters for a string.
	 */
	public static Long DEFAULT_MAX_STRING_SIZE = 50L;
	/**
	 * The maximum allowed value for the number characters for a string.
	 */
	public static Long MAX_ALLOWED_STRING_SIZE = 1000L;

	public static final Charset UTF8 = Charset.forName("UTF-8");

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
			ByteArrayOutputStream out = new ByteArrayOutputStream(200);
			GZIPOutputStream zip = new GZIPOutputStream(out);
			Writer zipWriter = new OutputStreamWriter(zip, UTF8);
			XStream xstream = createXStream();
			xstream.toXML(normal, zipWriter);
			Closer.closeQuietly(zipWriter, zip, out);
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
	 * Translate from the DBO to the DTO.
	 * @param dbo
	 * @return
	 */
	public static List<ColumnModel> createDTOFromDBO(List<DBOColumnModel> dbos){
		if(dbos == null) throw new IllegalArgumentException("DBOColumnModel cannot be null");
		List<ColumnModel> results = new LinkedList<ColumnModel>();
		for(DBOColumnModel dbo: dbos){
			results.add(ColumnModelUtlis.createDTOFromDBO(dbo));
		}
		return results;
	}
	
	
	/**
	 * Calculate the hash from an object
	 * @param dbo
	 * @return
	 */
	static String calculateHash(ColumnModel normalizedModel) {
		if (normalizedModel == null)
			throw new IllegalArgumentException("ColumnModel cannot be null");
		try {
			// To JSON
			byte[] jsonBytes = EntityFactory.createJSONStringForEntity(normalizedModel).getBytes("UTF-8");
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
			// Is this a string?
			if(ColumnType.STRING.equals(clone.getColumnType())){
				if(clone.getMaximumSize() == null){
					// Use the default value
					clone.setMaximumSize(DEFAULT_MAX_STRING_SIZE);
				}else if(clone.getMaximumSize() > MAX_ALLOWED_STRING_SIZE){
					// The max is beyond the allowed size
					throw new IllegalArgumentException("ColumnModel.maxSize for a STRING cannot exceed: "+MAX_ALLOWED_STRING_SIZE);
				} else if (clone.getMaximumSize() < 1) {
					// The max is beyond the allowed size
					throw new IllegalArgumentException("ColumnModel.maxSize for a STRING must be greater than 0");
				}
			}
			// The ID is not part of the normalized form.
			clone.setId(null);
			// to lower on the name
			clone.setName(clone.getName().trim());
			// Default to lower.
			if(clone.getDefaultValue() != null){
				// normalize the default value
				clone.setDefaultValue(TableModelUtils.normalizeDefaultValue(clone.getDefaultValue().trim(), clone));
			}
			if(clone.getEnumValues() != null){
				List<String> newList = new LinkedList<String>();
				for(String enumValue: clone.getEnumValues()){
					enumValue = enumValue.trim();
					if (enumValue.isEmpty()) {
						if (clone.getColumnType() == ColumnType.STRING) {
							newList.add(enumValue);
						}
					} else {
						enumValue = TableModelUtils.validateValue(enumValue, clone);
						newList.add(enumValue);
					}
				}
				if (!newList.isEmpty()) {
					Collections.sort(newList);
					clone.setEnumValues(newList);
				} else {
					clone.setEnumValues(null);
				}
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
	
	/**
	 * Create a list of DBOBoundColumn from the passed list of IDs.
	 * @param tableId
	 * @param ids
	 * @return
	 */
	public static List<DBOBoundColumn> createDBOBoundColumnList(Long objectId, List<String> ids){
		List<DBOBoundColumn> list = new LinkedList<DBOBoundColumn>();
		long now = System.currentTimeMillis();
		// Add each id
		for(String id: ids){
			DBOBoundColumn bc = new DBOBoundColumn();
			bc.setColumnId(Long.parseLong(id));
			bc.setObjectId(objectId);
			bc.setUpdatedOn(now);
			list.add(bc);
		}
		return list;
	}
	
	/**
	 * Create a list DBOBoundColumnOrdinal where the order of the list is preserved.
	 * @param tableIdString
	 * @param ids
	 * @return
	 */
	public static List<DBOBoundColumnOrdinal> createDBOBoundColumnOrdinalList(Long objectId, List<String> ids){
		List<DBOBoundColumnOrdinal> list = new LinkedList<DBOBoundColumnOrdinal>();
		// Keep the order of the columns
		for(int i=0; i<ids.size(); i++){
			Long id = Long.parseLong(ids.get(i));
			DBOBoundColumnOrdinal bc = new DBOBoundColumnOrdinal();
			bc.setColumnId(id);
			bc.setObjectId(objectId);
			bc.setOrdinal(new Long(i));
			list.add(bc);
		}
		return list;
	}
	
	/**
	 * Sort the passed list of DBOs by column Id.
	 * @param toSort
	 */
	public static void sortByColumnId(List<DBOBoundColumn> toSort){
		Collections.sort(toSort, new Comparator<DBOBoundColumn>(){
			@Override
			public int compare(DBOBoundColumn o1, DBOBoundColumn o2) {
				return o1.columnId.compareTo(o2.columnId);
			}});
	}
}
