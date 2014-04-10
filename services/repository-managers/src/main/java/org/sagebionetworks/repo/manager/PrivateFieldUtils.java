package org.sagebionetworks.repo.manager;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.schema.LinkDescription;
import org.sagebionetworks.schema.LinkDescription.LinkRel;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;

public class PrivateFieldUtils {
	
	/**
	 * 
	 * @param userInfo
	 * @param type
	 * @param jsonEntity Note this is treated as MUTABLE
	 */
	@SuppressWarnings("unchecked")
	public static void clearPrivateFields(Object jsonEntity) {
		if (jsonEntity==null || !(jsonEntity instanceof JSONEntity)) return;
		Class<? extends JSONEntity> type = (Class<? extends JSONEntity>)jsonEntity.getClass();
		ObjectSchema schema = SchemaCache.getSchema(type);
		Map<String, ObjectSchema> schemaProperties = schema.getProperties();	
		for (String propertyName : schemaProperties.keySet()) {
			Field field;
			try {
				field = type.getDeclaredField(propertyName);
				field.setAccessible(true);
			} catch (NoSuchFieldException e) {
				throw new RuntimeException(e);
			}
			if (!isPublic(propertyName, schema)) {
				try {
					field.set(jsonEntity, null);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			} else {
				// though the field is public, a sub-field might not be, so 'recurse'
				try {
					Object fieldValue = field.get(jsonEntity);
					if (fieldValue instanceof Collection) {
						for (Object elem : (Collection)fieldValue) {
							clearPrivateFields(elem);
						}
					} else {
						clearPrivateFields(fieldValue);
					}
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}				
	}
	
	public static final String PUBLIC_PROPERTY_LINK = "http://synapse.sagebase.org/access/public";
	/**
	 * Determines from the schema whether a field is public
	 * @param property
	 * @return
	 */
	public static <T extends JSONEntity> boolean isPublic(String property, ObjectSchema schema) {
		if (property.equals("concreteType")) return true;
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


}
