package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
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
public class NodeUtils {
	
	private static final String COLUMN_ID_DELIMITER = ",";
	
	public static final String ROOT_ENTITY_ID = StackConfigurationSingleton.singleton().getRootFolderEntityId();

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
		if(dto.getId() != null){
			jdo.setId(KeyFactory.stringToKey(dto.getId()));
		}
		if(dto.getCreatedOn() != null){
			jdo.setCreatedOn(dto.getCreatedOn().getTime());
		}
		if (dto.getCreatedByPrincipalId() != null){
			jdo.setCreatedBy(dto.getCreatedByPrincipalId());
		}
		if(dto.getParentId() != null){
			jdo.setParentId(KeyFactory.stringToKey(dto.getParentId()));
		}
		jdo.setAlias(StringUtils.isEmpty(dto.getAlias()) ? null : dto.getAlias());
		if (dto.getModifiedByPrincipalId()==null) throw new InvalidModelException("modifiedByPrincipalId may not be null");
		rev.setModifiedBy(dto.getModifiedByPrincipalId());
		if (dto.getModifiedOn()==null) throw new InvalidModelException("modifiedOn may not be null");
		rev.setModifiedOn(dto.getModifiedOn().getTime());
		
		if (dto.getVersionComment()!=null && dto.getVersionComment().length()>DBORevision.MAX_COMMENT_LENGTH) 
			throw new IllegalArgumentException("Version comment length exceeds "+DBORevision.MAX_COMMENT_LENGTH+".");
		
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
			rev.setColumnModelIds(createByteForIdList(dto.getColumnModelIds()));
		}
		if(dto.getScopeIds() != null){
			rev.setScopeIds(createByteForIdList(dto.getScopeIds()));
		}
		try {
			rev.setReference(JDOSecondaryPropertyUtils.compressReference(dto.getReference()));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	/**
	 * Create the bytes for a given list of ColumnModel IDs
	 * @param idList
	 * @return
	 */
	public static byte[] createByteForIdList(List<String> idList) {
		if(idList == null) throw new IllegalArgumentException("idList cannot be null");
		StringBuilder builder = new StringBuilder();
		int count = 0;
		for(String id: idList){
			if(count >0){
				builder.append(COLUMN_ID_DELIMITER);
			}
			// the value must be a long
			long value = KeyFactory.stringToKey(id);
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
	 * @param idListBytes
	 * @return
	 */
	public static List<String> createIdListFromBytes(byte[] idListBytes) {
		if(idListBytes == null) throw new IllegalArgumentException("idListBytes cannot be null");
		try {
			List<String> result = new LinkedList<String>();
			String string = new String(idListBytes, "UTF-8");
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
	 * Create a DTO from the JDO
	 * @param jdo
	 * @return
	 * @throws DatastoreException 
	 */
	public static Node copyFromJDO(DBONode jdo, DBORevision rev) throws DatastoreException{
		Node dto = new Node();
		 copyFromJDO(dto, jdo, rev);
		return dto;
	}
	
	/**
	 * Copy data from the passed DBOs to the passed dto.
	 * 
	 * @param dto
	 * @param jdo
	 * @param rev
	 * @throws DatastoreException
	 */
	public static void copyFromJDO(Node dto, DBONode jdo, DBORevision rev) throws DatastoreException{
		dto.setName(jdo.getName());
		if(jdo.getId() != null){
			dto.setId(KeyFactory.keyToString(jdo.getId()));
		}
		if(jdo.getParentId() != null){
			dto.setParentId(KeyFactory.keyToString(jdo.getParentId()));
		}
		if(jdo.getEtag() != null){
			dto.setETag(jdo.getEtag());
		}
		if(jdo.getType() != null){
			dto.setNodeType(EntityType.valueOf(jdo.getType()));
		}
		dto.setCreatedOn(new Date(jdo.getCreatedOn()));
		dto.setCreatedByPrincipalId(jdo.getCreatedBy());
		dto.setAlias(jdo.getAlias());
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
			dto.setColumnModelIds(createIdListFromBytes(rev.getColumnModelIds()));
		}
		if(rev.getScopeIds() != null){
			dto.setScopeIds(createIdListFromBytes(rev.getScopeIds()));
		}
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
	
	/**
	 * Is the given type a project or folder?
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isProjectOrFolder(EntityType type){
		return EntityType.project.equals(type)
				|| EntityType.folder.equals(type);
	}
	
	/**
	 * Is the given entity ID root?
	 * 
	 * @param entityId
	 * @return
	 */
	public static boolean isRootEntityId(String entityId){
		return KeyFactory.equals(ROOT_ENTITY_ID, entityId);
	}
	
}
