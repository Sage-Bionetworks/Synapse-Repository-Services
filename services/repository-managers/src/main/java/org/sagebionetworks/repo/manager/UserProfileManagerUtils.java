package org.sagebionetworks.repo.manager;

import java.lang.reflect.Field;
import java.util.Map;

import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.schema.LinkDescription;
import org.sagebionetworks.schema.LinkDescription.LinkRel;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;

public class UserProfileManagerUtils {
	
	public static final String PUBLIC_PROPERTY_LINK = "http://synapse.sagebase.org/access/public";
	/**
	 * Determines from the schema whether a field is public
	 * @param property
	 * @return
	 */
	public static <T extends JSONEntity> boolean isPublic(String property, ObjectSchema schema) {
		//profile picture is always public, even though AttachmentData might not be
		if (property.equals("pic")) return true;
		Map<String, ObjectSchema> schemaProperties = schema.getProperties();	
		ObjectSchema propertySchema = schemaProperties.get(property);
		if (propertySchema==null) throw new RuntimeException("No property "+property+" found in "+schema.getName()+" schema.");
		LinkDescription[] linkDescriptions = propertySchema.getLinks();
		if (linkDescriptions==null || linkDescriptions.length==0) return false; // by default a property is not public
		for (LinkDescription ld : linkDescriptions) {
			if (ld.getRel().equals(LinkRel.DESCRIBED_BY) && PUBLIC_PROPERTY_LINK.equals(ld.getHref())) return true;
		}
		return false;
	}

	public static boolean isOwnerOrAdmin(UserInfo userInfo, String ownerId) {
		if (userInfo == null) return false;
		if (userInfo.isAdmin()) return true;
		if (ownerId != null && ownerId.equals(userInfo.getId().toString())) return true;
		return false;
	}

	/**
	 * 
	 * @param userInfo
	 * @param userProfile Note this is treated as MUTABLE
	 */
	public static void clearPrivateFields(UserInfo userInfo, UserProfile userProfile) {		
		if (userProfile != null) {
			boolean canSeePrivate = UserProfileManagerUtils.isOwnerOrAdmin(userInfo, userProfile.getOwnerId());
			if (!canSeePrivate) {
				clearPrivateFields(userInfo, UserProfile.class, userProfile);			
			}
		}
	}
	
	/**
	 * 
	 * @param userInfo
	 * @param userGroupHeader Note this is treated as MUTABLE
	 */
	public static void clearPrivateFields(UserInfo userInfo, UserGroupHeader userGroupHeader) {		
		if (userGroupHeader != null) {
			boolean canSeePrivate = UserProfileManagerUtils.isOwnerOrAdmin(userInfo, userGroupHeader.getOwnerId());
			if (!canSeePrivate) {
				clearPrivateFields(userInfo, UserGroupHeader.class, userGroupHeader);		
			}
		}
	}
	
	/**
	 * 
	 * @param userInfo
	 * @param type
	 * @param jsonEntity Note this is treated as MUTABLE
	 */
	public static <T extends JSONEntity> void clearPrivateFields(UserInfo userInfo, Class<T> type, T jsonEntity) {									
		ObjectSchema schema = SchemaCache.getSchema(type);
		Map<String, ObjectSchema> schemaProperties = schema.getProperties();	
		for (String propertyName : schemaProperties.keySet()) {
			if (!isPublic(propertyName, schema)) {
				try {
					Field field = type.getDeclaredField(propertyName);
					field.setAccessible(true);
					field.set(jsonEntity, null);
				} catch (NoSuchFieldException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}				
	}	

}
 