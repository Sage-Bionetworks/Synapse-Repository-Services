package org.sagebionetworks.repo.model.dbo.persistence.table;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;

import com.google.common.collect.Lists;

/**
 * Utilities for working with ColumModel objects and DBOColumnModel objects.
 * 
 * @author John
 *
 */
public class ColumnModelUtils {
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder()
			.alias("ColumnModel", ColumnModel.class)
			.alias("ColumnType", ColumnType.class)
			.alias("ColumnChange", ColumnChange.class)
			.allowTypes(ColumnModel.class, ColumnType.class, ColumnChange.class)
			.build();
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
			DBOColumnModel dbo = new DBOColumnModel();
			dbo.setBytes(JDOSecondaryPropertyUtils.compressObject(X_STREAM, normal));
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
			ColumnModel model = (ColumnModel) JDOSecondaryPropertyUtils.decompressObject(X_STREAM, dbo.getBytes());
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
			case STRING_LIST:
				validateListLengthForClone(clone);
				// intentional no break to also validate max size
			case STRING:
			case LINK:
				if(clone.getMaximumSize() == null){
					// Use the default value
					clone.setMaximumSize(ColumnConstants.DEFAULT_STRING_SIZE);
				}else if(clone.getMaximumSize() > ColumnConstants.MAX_ALLOWED_STRING_SIZE){
					// The max is beyond the allowed size
					throw new IllegalArgumentException("ColumnModel.maxSize for a STRING cannot exceed: "+ColumnConstants.MAX_ALLOWED_STRING_SIZE);
				} else if (clone.getMaximumSize() < 1) {
					// The max is beyond the allowed size
					throw new IllegalArgumentException("ColumnModel.maxSize for a STRING must be greater than 0");
				}
				break;
			case ENTITYID:
			case SUBMISSIONID:
			case EVALUATIONID:
			case FILEHANDLEID:
			case USERID:
			case LARGETEXT:
				if (StringUtils.isEmpty(defaultValue)) {
					defaultValue = null;
				}
				if (defaultValue != null) {
					throw new IllegalArgumentException("Columns of type " + clone.getColumnType() + " cannot have default values.");
				}
				break;
			case ENTITYID_LIST:
			case USERID_LIST:
				if (StringUtils.isEmpty(defaultValue)) {
					defaultValue = null;
				}
				if (defaultValue != null) {
					throw new IllegalArgumentException("Columns of type " + clone.getColumnType() + " cannot have default values.");
				}
			case INTEGER_LIST:
			case DATE_LIST:
			case BOOLEAN_LIST:
				validateListLengthForClone(clone);
				// intentional no break for default value validation
			case BOOLEAN:
			case DATE:
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

	static void validateListLengthForClone(ColumnModel clone){
		if(clone.getMaximumListLength() == null){
			// Use the default value
			clone.setMaximumListLength(ColumnConstants.MAX_ALLOWED_LIST_LENGTH);
		}else if(clone.getMaximumListLength() > ColumnConstants.MAX_ALLOWED_LIST_LENGTH){
			// The max is beyond the allowed size
			throw new IllegalArgumentException("ColumnModel.maximumListLength for a LIST column cannot exceed: "+ColumnConstants.MAX_ALLOWED_LIST_LENGTH);
		} else if (clone.getMaximumListLength() < 2) {
			// The max is beyond the allowed size
			throw new IllegalArgumentException("ColumnModel.maximumListLength for a LIST column must be at least 2");
		}
	}

	
	/**
	 * Create a list DBOBoundColumnOrdinal where the order of the list is preserved.
	 * @param tableIdString
	 * @param ids
	 * @return
	 */
	public static List<DBOBoundColumnOrdinal> createDBOBoundColumnOrdinalList(IdAndVersion idAndVersion, List<ColumnModel> columns){
		List<DBOBoundColumnOrdinal> list = new LinkedList<DBOBoundColumnOrdinal>();
		// Keep the order of the columns
		int index = 0;
		for(ColumnModel column: columns){
			Long id = Long.parseLong(column.getId());
			DBOBoundColumnOrdinal bc = new DBOBoundColumnOrdinal();
			bc.setColumnId(id);
			bc.setObjectId(idAndVersion.getId());
			bc.setObjectVersion(idAndVersion.getVersion().orElse(DBOBoundColumnOrdinal.DEFAULT_NULL_VERSION));
			bc.setOrdinal(new Long(index));
			list.add(bc);
			index++;
		}
		return list;
	}
	
	/**
	 * Write the list of schema changes to the given output stream as a GZIP.
	 * 
	 * @param changes
	 * @param out
	 * @throws IOException
	 */
	public static void writeSchemaChangeToGz(List<ColumnChange> changes, OutputStream out) throws IOException{
		try(GZIPOutputStream zipOut = new GZIPOutputStream(out);){
			X_STREAM.toXML(changes, zipOut);
			zipOut.flush();
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
		try(GZIPInputStream zipIn = new GZIPInputStream(input);){
			return (List<ColumnChange>) X_STREAM.fromXML(zipIn);
		}
	}
}
