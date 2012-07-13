package org.sagebionetworks.repo.manager;

import java.lang.reflect.Field;
import java.util.Map;

import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.schema.LinkDescription;
import org.sagebionetworks.schema.LinkDescription.LinkRel;
import org.sagebionetworks.schema.ObjectSchema;

public class UserProfileManagerUtils {
	
	public static final String PUBLIC_PROPERTY_LINK = "http://synapse.sagebase.org/access/public";
	/**
	 * Determines from the UserProfile schema whether a field is public
	 * @param property
	 * @return
	 */
	public static boolean isPublic(String property) {
		ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
		Map<String, ObjectSchema> schemaProperties = schema.getProperties();	
		ObjectSchema propertySchema = schemaProperties.get(property);
		if (propertySchema==null) throw new RuntimeException("No property "+property+" found in UserProfile schema.");
		LinkDescription[] linkDescriptions = propertySchema.getLinks();
		if (linkDescriptions==null || linkDescriptions.length==0) return false; // by default a property is not public
		for (LinkDescription ld : linkDescriptions) {
			if (ld.getRel().equals(LinkRel.DESCRIBED_BY) && PUBLIC_PROPERTY_LINK.equals(ld.getHref())) return true;
		}
		return false;
	}
	
	public static boolean isOwnerOrAdmin(UserInfo userInfo, String ownerId) {
		if (userInfo.isAdmin()) return true;
		if (ownerId!=null && ownerId.equals(userInfo.getIndividualGroup().getId())) return true;
		return false;
	}
	
	/**
	 * Note the input parameter is treated as MUTABLE
	 * @param up
	 */
	public static void clearPrivateFields(UserProfile up) {
		ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
		Map<String, ObjectSchema> schemaProperties = schema.getProperties();	
		for (String propertyName : schemaProperties.keySet()) {
			if (!isPublic(propertyName)) {
				try {
					Field field = UserProfile.class.getDeclaredField(propertyName);
					field.setAccessible(true);
					field.set(up, null);
				} catch (NoSuchFieldException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
	
}
 