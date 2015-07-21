package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

/**
 * Translates JDOs and DTOs.
 * 
 * @author jmhill
 *
 */
class NodeUtils {
	

		
	private static final String COLUMN_ID_DELIMITER = ",";

	/**
	 * Used to update an existing object
	 * @param dto
	 * @param jdo
	 * @param rev 
	 * @return
	 * @throws DatastoreException 
	 */
	public static void updateFromDto(Node dto, DBONode jdo, DBORevision rev, boolean deleteActivityId) throws DatastoreException, InvalidModelException {
		jdo.setName(dto.getName());
		if(dto.getDescription() !=  null){
			try {
				jdo.setDescription(dto.getDescription().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new DatastoreException(e);
			}
		}else{
			jdo.setDescription(null);
		}

		if(dto.getId() != null){
			jdo.setId(KeyFactory.stringToKey(dto.getId()));
		}
		if(dto.getCreatedOn() != null){
			jdo.setCreatedOn(dto.getCreatedOn().getTime());
		}
		if (dto.getCreatedByPrincipalId() != null){
			jdo.setCreatedBy(dto.getCreatedByPrincipalId());
		}
		if (dto.getModifiedByPrincipalId()==null) throw new InvalidModelException("modifiedByPrincipalId may not be null");
		rev.setModifiedBy(dto.getModifiedByPrincipalId());
		if (dto.getModifiedOn()==null) throw new InvalidModelException("modifiedOn may not be null");
		rev.setModifiedOn(dto.getModifiedOn().getTime());
		rev.setComment(dto.getVersionComment());
		if(dto.getVersionLabel() != null){
			rev.setLabel(dto.getVersionLabel());
		} 	
		if(dto.getFileHandleId() != null){
			rev.setFileHandleId(KeyFactory.stringToKey(dto.getFileHandleId()));
		}else{
			rev.setFileHandleId(null);
		}
		// bring in activity id, if set
		if(deleteActivityId) {
			rev.setActivityId(null);
		} else if(dto.getActivityId() != null) {
			rev.setActivityId(Long.parseLong(dto.getActivityId()));
		}
		
		if(dto.getColumnModelIds() != null){
			rev.setColumnModelIds(createColumnModelBytesFromList(dto.getColumnModelIds()));
		}
		
		try {
			rev.setReference(JDOSecondaryPropertyUtils.compressReference(dto.getReference()));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	/**
	 * Create the bytes for a given list of ColumnModel IDs
	 * @param columnModelIds
	 * @return
	 */
	private static byte[] createColumnModelBytesFromList(List<String> columnModelIds) {
		if(columnModelIds == null) throw new IllegalArgumentException("columnModelIds cannot be null");
		StringBuilder builder = new StringBuilder();
		int count = 0;
		for(String id: columnModelIds){
			if(count >0){
				builder.append(COLUMN_ID_DELIMITER);
			}
			// the value must be a long
			long value = Long.parseLong(id);
			builder.append(value);
			count++;
		}
		try {
			return builder.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Create a the list of column model ID from bytes.
	 * @param columnModelIds
	 * @return
	 */
	private static List<String> createColumnModelListFromBytes(byte[] columnModelIds) {
		if(columnModelIds == null) throw new IllegalArgumentException("columnModelIds cannot be null");
		try {
			List<String> result = new LinkedList<String>();
			String string = new String(columnModelIds, "UTF-8");
			if (string.isEmpty()) {
				return result;
			}
			String[] split = string.split(COLUMN_ID_DELIMITER);
			for(String stringId: split){
				// The value must be a long
				long value = Long.parseLong(stringId);
				result.add(Long.toString(value));
			}
			return result;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Replace all fields from the dto.
	 * @param dto
	 * @param jdo
	 * @throws DatastoreException
	 */
	public static void replaceFromDto(Node dto, DBONode jdo) throws DatastoreException {
		jdo.setName(dto.getName());
		if(dto.getDescription() != null){
			try {
				jdo.setDescription(dto.getDescription().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new DatastoreException(e);
			}
		}else{
			jdo.setDescription(null);
		}
		if(dto.getId() != null){
			jdo.setId(KeyFactory.stringToKey(dto.getId()));
		}
		if(dto.getCreatedOn() != null){
			jdo.setCreatedOn(dto.getCreatedOn().getTime());
		}
		jdo.setCreatedBy(dto.getCreatedByPrincipalId());
		jdo.seteTag(KeyFactory.urlDecode(dto.getETag()));
		jdo.setCurrentRevNumber(dto.getVersionNumber());
		jdo.setType(dto.getNodeType().name());
		if(dto.getParentId() != null){
			jdo.setParentId(KeyFactory.stringToKey(dto.getParentId()));
		}else{
			jdo.setParentId(null);
		}

	}
	
	/**
	 * Create a DTO from the JDO
	 * @param jdo
	 * @return
	 * @throws DatastoreException 
	 */
	public static Node copyFromJDO(DBONode jdo, DBORevision rev) throws DatastoreException{
		Node dto = new Node();
		dto.setName(jdo.getName());
		if(jdo.getDescription() != null){
			try {
				dto.setDescription(new String(jdo.getDescription(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new DatastoreException(e);
			}
		}else{
			dto.setDescription(null);
		}
		if(jdo.getId() != null){
			dto.setId(KeyFactory.keyToString(jdo.getId()));
		}
		if(jdo.getParentId() != null){
			dto.setParentId(KeyFactory.keyToString(jdo.getParentId()));
		}
		if (jdo.getProjectId() != null) {
			dto.setProjectId(KeyFactory.keyToString(jdo.getProjectId()));
		}
		if(jdo.getEtag() != null){
			dto.setETag(jdo.getEtag());
		}
		if(jdo.getType() != null){
			dto.setNodeType(EntityType.valueOf(jdo.getType()));
		}
		dto.setCreatedOn(new Date(jdo.getCreatedOn()));
		dto.setCreatedByPrincipalId(jdo.getCreatedBy());
		dto.setModifiedByPrincipalId(rev.getModifiedBy());
		dto.setModifiedOn(new Date(rev.getModifiedOn()));
		dto.setVersionComment(rev.getComment());
		dto.setVersionLabel(rev.getLabel());
		if(rev.getRevisionNumber() != null){
			dto.setVersionNumber(rev.getRevisionNumber());
		}
		if(rev.getFileHandleId() != null){
			dto.setFileHandleId(rev.getFileHandleId().toString());
		}
		if(rev.getActivityId() != null) {
			dto.setActivityId(rev.getActivityId().toString());
		} 
		
		try {
			dto.setReference(JDOSecondaryPropertyUtils.decompressedReference(rev.getReference()));
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		if(rev.getColumnModelIds() != null){
			dto.setColumnModelIds(createColumnModelListFromBytes(rev.getColumnModelIds()));
		}
		return dto;
	}
	
	/**
	 * A valid node is not null and has not null values for the following fields:
	 * + id
	 * + name
	 * + nodeType
	 * + etag
	 * + createdByPrincipalId
	 * + createdOn
	 * + modifiedByPrincipalId
	 * + modifiedOn
	 * 
	 * @param node
	 * @return true if node is valid, false otherwise.
	 */
	public static boolean isValidNode(Node node) {
		if (node == null ||
				node.getCreatedByPrincipalId() == null ||
				node.getCreatedOn() == null ||
				node.getETag() == null ||
				node.getId() == null ||
				node.getModifiedByPrincipalId() == null ||
				node.getModifiedOn() == null ||
				node.getName() == null ||
				node.getNodeType() == null) 
			return false;
		return true;
	}
	
}
