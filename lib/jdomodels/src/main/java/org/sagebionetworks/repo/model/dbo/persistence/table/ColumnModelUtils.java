package org.sagebionetworks.repo.model.dbo.persistence.table;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.table.cluster.utils.ColumnConstants;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;

/**
 * Utilities for working with ColumModel objects and DBOColumnModel objects.
 * 
 * @author John
 *
 */
public class ColumnModelUtils {
	
	/**
	 * The default maximum number of characters for a string.
	 */
	public static Long DEFAULT_MAX_STRING_SIZE = 50L;
	public static final Charset UTF8 = Charset.forName("UTF-8");

	/**
	 * Translate from a DTO to DBO.
	 * @param dto
	 * @return
	 */
	public static DBOColumnModel createDBOFromDTO(ColumnModel dto, int maxEnumValues) {
		try {
			// First normalize the DTO
			ColumnModel normal = createNormalizedClone(dto, maxEnumValues);
			String hash = calculateHash(normal);
			// Create the bytes
			ByteArrayOutputStream out = new ByteArrayOutputStream(200);
			GZIPOutputStream zip = new GZIPOutputStream(out);
			Writer zipWriter = new OutputStreamWriter(zip, UTF8);
			XStream xstream = createXStream();
			xstream.toXML(normal, zipWriter);
			IOUtils.closeQuietly(zipWriter);
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
			results.add(ColumnModelUtils.createDTOFromDBO(dbo));
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
	public static ColumnModel createNormalizedClone(ColumnModel toClone, int maxEnumValues) {
		if(toClone == null) throw new IllegalArgumentException("ColumnModel cannot be null");
		if(toClone.getName() == null) throw new IllegalArgumentException("ColumnModel.name cannot be null");
		if(toClone.getColumnType() == null) throw new IllegalArgumentException("ColumnModel.columnType cannot be null");
		try {
			// Create a clone from the JSON
			String json = EntityFactory.createJSONStringForEntity(toClone);
			ColumnModel clone = EntityFactory.createEntityFromJSONString(json, ColumnModel.class);
			String defaultValue = clone.getDefaultValue();
			if (defaultValue != null) {
				defaultValue = defaultValue.trim();
			}
			switch (clone.getColumnType()) {
			case STRING:
			case LINK:
				if(clone.getMaximumSize() == null){
					// Use the default value
					clone.setMaximumSize(DEFAULT_MAX_STRING_SIZE);
				}else if(clone.getMaximumSize() > ColumnConstants.MAX_ALLOWED_STRING_SIZE){
					// The max is beyond the allowed size
					throw new IllegalArgumentException("ColumnModel.maxSize for a STRING cannot exceed: "+ColumnConstants.MAX_ALLOWED_STRING_SIZE);
				} else if (clone.getMaximumSize() < 1) {
					// The max is beyond the allowed size
					throw new IllegalArgumentException("ColumnModel.maxSize for a STRING must be greater than 0");
				}
				break;
			case ENTITYID:
			case FILEHANDLEID:
			case USERID:
				if (StringUtils.isEmpty(defaultValue)) {
					defaultValue = null;
				}
				if (defaultValue != null) {
					throw new IllegalArgumentException("Columns of type ENTITYID, FILEHANDLEID, and USERID cannot have default values: "
							+ defaultValue);
				}
				break;
			case BOOLEAN:
			case DATE:
			case LARGETEXT:
			case INTEGER:
			case DOUBLE:
				if (StringUtils.isEmpty(defaultValue)) {
					defaultValue = null;
				}
				break;
			default:
				throw new IllegalArgumentException("Unexpected ColumnType " + clone.getColumnType());
			}
			// The ID is not part of the normalized form.
			clone.setId(null);
			// to lower on the name
			clone.setName(clone.getName().trim());

			// normalize enum values
			if(clone.getEnumValues() != null){
				if (clone.getEnumValues().size() > maxEnumValues) {
					throw new IllegalArgumentException("Maximum allowed enum values is " + maxEnumValues + ". This enum has "
							+ clone.getEnumValues().size() + " values");
				}
				List<String> oldList = clone.getEnumValues();
				// set enums temporary to null, so we can check the individual enum values themselves
				clone.setEnumValues(null);
				List<String> newList = Lists.newArrayListWithCapacity(oldList.size());
				for (String enumValue : oldList) {
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
				}
			}

			// Default to lower.
			if (defaultValue != null) {
				// normalize the default value
				clone.setDefaultValue(TableModelUtils.validateValue(defaultValue, clone));
			} else {
				clone.setDefaultValue(null);
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
		xstream.alias("ColumnChange", ColumnChange.class);
		return xstream;
	}
	
	/**
	 * Create a list of DBOBoundColumn from the passed list of IDs.
	 * @param tableId
	 * @param ids
	 * @return
	 */
	public static List<DBOBoundColumn> createDBOBoundColumnList(Long objectId, List<ColumnModel> columns){
		List<DBOBoundColumn> list = new LinkedList<DBOBoundColumn>();
		long now = System.currentTimeMillis();
		// Add each id
		for(ColumnModel column: columns){
			DBOBoundColumn bc = new DBOBoundColumn();
			bc.setColumnId(Long.parseLong(column.getId()));
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
	public static List<DBOBoundColumnOrdinal> createDBOBoundColumnOrdinalList(Long objectId, List<ColumnModel> columns){
		List<DBOBoundColumnOrdinal> list = new LinkedList<DBOBoundColumnOrdinal>();
		// Keep the order of the columns
		int index = 0;
		for(ColumnModel column: columns){
			Long id = Long.parseLong(column.getId());
			DBOBoundColumnOrdinal bc = new DBOBoundColumnOrdinal();
			bc.setColumnId(id);
			bc.setObjectId(objectId);
			bc.setOrdinal(new Long(index));
			list.add(bc);
			index++;
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
	
	/**
	 * Write the list of schema changes to the given output stream as a GZIP.
	 * 
	 * @param changes
	 * @param out
	 * @throws IOException
	 */
	public static void writeSchemaChangeToGz(List<ColumnChange> changes, OutputStream out) throws IOException{
		GZIPOutputStream zipOut = null;
		try{
			zipOut = new GZIPOutputStream(out);
			XStream xstream = createXStream();
			xstream.toXML(changes, zipOut);
			zipOut.flush();
		}finally{
			IOUtils.closeQuietly(zipOut);
		}
	}
	
	/**
	 * Read the schema change from the given GZIP input stream.
	 * 
	 * @param input
	 * @return
	 * @throws IOException
	 */
	public static List<ColumnChange> readSchemaChangeFromGz(InputStream input) throws IOException{
		GZIPInputStream zipIn = null;
		try{
			zipIn = new GZIPInputStream(input);
			XStream xstream = createXStream();
			return (List<ColumnChange>) xstream.fromXML(zipIn);
		}finally{
			IOUtils.closeQuietly(zipIn);
		}
	}
}
