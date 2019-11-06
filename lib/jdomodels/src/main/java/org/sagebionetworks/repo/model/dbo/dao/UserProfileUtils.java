package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Date;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFavorite;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.Settings;

public class UserProfileUtils {
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(UserProfile.class).build();


	public static void copyDtoToDbo(UserProfile dto, DBOUserProfile dbo) throws DatastoreException{
		if (dto.getOwnerId()==null) {
			dbo.setOwnerId(null);
		} else {
			dbo.setOwnerId(Long.parseLong(dto.getOwnerId()));
		}
		dbo.seteTag(dto.getEtag());
		try {
			dbo.setProperties(JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		String profilePictureFileHandleId = dto.getProfilePicureFileHandleId();
		if (dto.getProfilePicureFileHandleId() == null) {
			dbo.setPictureId(null);
		} else {
			dbo.setPictureId(Long.parseLong(profilePictureFileHandleId));
		}
		dbo.setEmailNotification(true);
		if(dto.getNotificationSettings() != null && dto.getNotificationSettings().getSendEmailNotifications() != null){
			dbo.setEmailNotification(dto.getNotificationSettings().getSendEmailNotifications());
		}
		try {
			if (dto.getFirstName() != null) {
				dbo.setFirstName(dto.getFirstName().getBytes("UTF-8"));
			}
			if (dto.getLastName() != null) {
				dbo.setLastName(dto.getLastName().getBytes("UTF-8"));
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static UserProfile deserialize(byte[] b) {
		try {
			return (UserProfile) JDOSecondaryPropertyUtils.decompressObject(X_STREAM, b);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
		if(dto.getNotificationSettings() == null){
			dto.setNotificationSettings(new Settings());
		}
		dto.getNotificationSettings().setSendEmailNotifications(dbo.isEmailNotification());
		try {
			if (dbo.getFirstName() != null) {
				dto.setFirstName(new String(dbo.getFirstName(), "UTF-8"));
			}
			if (dbo.getLastName() != null) {
				dto.setLastName(new String(dbo.getLastName(), "UTF-8"));
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		if(dbo.getCreatedOn() != null) {
			dto.setCreatedOn(new Date(dbo.getCreatedOn()));
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
		ugh.setUserName(userName);
	}

}
