package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
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

	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(Reference.class).build();

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
		rev.setReference(compressReference(dto.getReference()));
	}

	/**
	 * Convert the passed reference to a compressed (zip) byte array
	 * @param dto
	 * @return the compressed reference
	 */
	public static byte[] compressReference(Reference dto) {
		try {
			return JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Translate a Node to DBONode.
	 * @param dto
	 * @return
	 */
	public static DBONode translateNodeToDBONode(Node dto) {
		DBONode dbo = new DBONode();
		dbo.setName(dto.getName());
		dbo.setAlias(translateAlias(dto.getAlias()));
		dbo.setCreatedBy(dto.getCreatedByPrincipalId());
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setCurrentRevNumber(dto.getVersionNumber());
		dbo.setId(translateNodeId(dto.getId()));
		dbo.setParentId(translateNodeId(dto.getParentId()));
		dbo.setType(dto.getNodeType().name());
		dbo.seteTag(dto.getETag());
		return dbo;
	}
	
	/**
	 * Translate a Node to DBORevision.
	 * @param dto
	 * @return
	 */
	public static DBORevision transalteNodeToDBORevision(Node dto) {
		DBORevision dbo = new DBORevision();
		dbo.setOwner(translateNodeId(dto.getId()));
		dbo.setRevisionNumber(translateVersionNumber(dto.getVersionNumber()));
		dbo.setActivityId(translateActivityId(dto.getActivityId()));
		dbo.setColumnModelIds(createByteForIdList(dto.getColumnModelIds()));
		dbo.setComment(translateVersionComment(dto.getVersionComment()));
		dbo.setFileHandleId(translateFileHandleId(dto.getFileHandleId()));
		dbo.setLabel(translateVersionLabel(dto.getVersionLabel()));
		dbo.setModifiedBy(dto.getModifiedByPrincipalId());
		dbo.setModifiedOn(dto.getModifiedOn().getTime());
		dbo.setColumnModelIds(createByteForIdList(dto.getColumnModelIds()));
		dbo.setScopeIds(createByteForIdList(dto.getScopeIds()));
		dbo.setReference(compressReference(dto.getReference()));
		return dbo;
	}
	
	/**
	 * Create the bytes for a given list of ColumnModel IDs
	 * @param idList
	 * @return
	 */
	public static byte[] createByteForIdList(List<String> idList) {
		if(idList == null) {
			return null;
		}
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
			dto.setIsLatestVersion(rev.getRevisionNumber().equals(jdo.getCurrentRevNumber()));
		}
		if(rev.getFileHandleId() != null){
			dto.setFileHandleId(rev.getFileHandleId().toString());
		}
		if(rev.getActivityId() != null) {
			dto.setActivityId(rev.getActivityId().toString());
		} 
		
		try {
			dto.setReference((Reference) JDOSecondaryPropertyUtils.decompressObject(X_STREAM, rev.getReference()));
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
	

	/**
	 * Translate the provided alias.
	 * @param alias
	 * @return
	 */
	public static String translateAlias(String alias) {
		if(StringUtils.isEmpty(alias)) {
			return null;
		}
		return alias;
	}
	
	/**
	 * Translate a string activity ID to a long.
	 * @param activityId
	 * @return
	 */
	public static Long translateActivityId(String activityId) {
		if(activityId == null) {
			return null;
		}
		if(NodeDAO.DELETE_ACTIVITY_VALUE.equals(activityId)){
			return null;
		}
		return Long.parseLong(activityId);
	}
	
	/**
	 * Translate a node ID to a long.
	 * 
	 * @param nodeId
	 * @return
	 */
	public static Long translateNodeId(String nodeId) {
		if(nodeId == null) {
			return null;
		}
		return KeyFactory.stringToKey(nodeId);
	}
	
	/**
	 * Translate a string file handle ID to a long.
	 * @param fileId
	 * @return
	 */
	public static Long translateFileHandleId(String fileId) {
		if(fileId == null) {
			return null;
		}
		return Long.parseLong(fileId);
	}
	
	/**
	 * Translate the version comment with size check.
	 * 
	 * @param comment
	 * @return
	 */
	public static String translateVersionComment(String comment) {
		if(comment == null) {
			return null;
		}
		if (comment.length() > DBORevision.MAX_COMMENT_LENGTH) {
			throw new IllegalArgumentException("Version comment length exceeds "+DBORevision.MAX_COMMENT_LENGTH+".");
		}
		return comment;
	}
	
	/**
	 * Translate the provide version label
	 * @param label
	 * @return
	 */
	public static String translateVersionLabel(String label) {
		if(label == null) {
			return NodeConstants.DEFAULT_VERSION_LABEL;
		}
		return label;
	}
	
	/**
	 * Translate the given version number.
	 * @param versionNumber
	 * @return
	 */
	public static Long translateVersionNumber(Long versionNumber) {
		if(versionNumber == null || versionNumber < 1) {
			return NodeConstants.DEFAULT_VERSION_NUMBER;
		}
		return versionNumber;
	}

	/**
	 * Determines if a given bucket is Synapse storage.
	 */
	public static Boolean isBucketSynapseStorage(String bucketName) {
		if (bucketName == null) return null;
		return bucketName.equals(StackConfigurationSingleton.singleton().getS3Bucket());
	}
}
