package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Date;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFavorite;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.schema.ObjectSchema;

public class UserProfileUtils {
	
	public static void copyDtoToDbo(UserProfile dto, DBOUserProfile dbo) throws DatastoreException{
		if (dto.getOwnerId()==null) {
			dbo.setOwnerId(null);
		} else {
			dbo.setOwnerId(Long.parseLong(dto.getOwnerId()));
		}
		dbo.seteTag(dto.getEtag());
		try {
			dbo.setProperties(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if(dto.getProfilePicureFileHandleId()!= null){
			dbo.setPictureId(Long.parseLong(dto.getProfilePicureFileHandleId()));
		}
	}
	
	public static UserProfile deserialize(byte[] b) {
		Object decompressed = null;
		try {
			decompressed = JDOSecondaryPropertyUtils.decompressedObject(b);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		UserProfile dto = null;
		if (decompressed instanceof UserProfile) {
			dto = (UserProfile) decompressed;
		} else if (decompressed instanceof NamedAnnotations) {
			// Support the old way of serializing the UserProfile
			ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
			dto = new UserProfile();
			SchemaSerializationUtils.mapAnnotationsToDtoFields(b, dto, schema);
		} else {
			throw new RuntimeException("Unsupported object type " + decompressed.getClass());
		}
		return dto;
	}
	
	public static UserProfile convertDboToDto(DBOUserProfile dbo) throws DatastoreException {
		UserProfile dto = deserialize(dbo.getProperties());
		if (dbo.getOwnerId()==null) {
			dto.setOwnerId(null);
		} else {
			dto.setOwnerId(dbo.getOwnerId().toString());
		}
		dto.setEtag(dbo.geteTag());
		if(dbo.getPictureId() != null){
			dto.setProfilePicureFileHandleId(dbo.getPictureId().toString());
		}
		return dto;
	}
	
	
	/*
	 * Favorite methods
	 */
	public static String getFavoriteId(Favorite favorite) {
		if(favorite == null) return null;
		return getFavoriteId(favorite.getPrincipalId(), favorite.getEntityId());
	}
	
	public static String getFavoriteId(String principalId, String entityId) {
		if(principalId != null && entityId != null)
			return principalId + "-" + entityId;
		return null;		
	}
	
	public static String getFavoritePrincipalIdFromId(String id) {
		if(id != null) {
			String[] parts = id.split("-");
			if(parts != null && parts.length >= 1) {
				return parts[0];
			}
		}
		return null;
	}

	public static String getFavoriteEntityIdFromId(String id) {
		if(id != null) {
			String[] parts = id.split("-");
			if(parts != null && parts.length >= 2) {
				return parts[1];
			}
		}
		return null;
	}

	public static void copyDtoToDbo(Favorite dto, DBOFavorite dbo) throws DatastoreException {
		if(dto.getPrincipalId() == null) throw new IllegalArgumentException("principalId can not be null");
		if(dto.getEntityId() == null) throw new IllegalArgumentException("entityId can not be null");
		dbo.setPrincipalId(Long.parseLong(dto.getPrincipalId()));
		dbo.setNodeId(KeyFactory.stringToKey(dto.getEntityId()));		
		if(dto.getCreatedOn() != null)
			dbo.setCreatedOn(dto.getCreatedOn().getTime());		
	}

	public static Favorite copyDboToDto(DBOFavorite dbo) throws DatastoreException {
		if(dbo.getPrincipalId() == null) throw new IllegalArgumentException("principalId can not be null");
		if(dbo.getNodeId() == null) throw new IllegalArgumentException("nodeId can not be null");
		Favorite dto = new Favorite();
		dto.setPrincipalId(dbo.getPrincipalId().toString());
		dto.setEntityId(KeyFactory.keyToString(dbo.getNodeId()));
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		return dto;
	}
	
	public static void fillUserGroupHeaderFromUserProfileBlob(Blob upProperties, String userName, UserGroupHeader ugh) throws SQLException {
		UserProfile up = UserProfileUtils.deserialize(upProperties.getBytes(1, (int) upProperties.length()));
		ugh.setFirstName(up.getFirstName());
		ugh.setLastName(up.getLastName());
		ugh.setPic(up.getPic());
		ugh.setUserName(userName);
	}

}
