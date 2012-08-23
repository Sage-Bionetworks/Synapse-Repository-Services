package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.schema.ObjectSchema;

public class UserProfileUtils {
	
	public static void copyDtoToDbo(UserProfile dto, DBOUserProfile dbo, ObjectSchema schema) throws DatastoreException{
		if (dto.getOwnerId()==null) {
			dbo.setOwnerId(null);
		} else {
			dbo.setOwnerId(Long.parseLong(dto.getOwnerId()));
		}
		dbo.seteTag(dto.getEtag());
		dbo.setProperties(SchemaSerializationUtils.mapDtoFieldsToAnnotations(dto, schema));
	}
	
	public static void copyDboToDto(DBOUserProfile dbo, UserProfile dto, ObjectSchema schema) throws DatastoreException {		
		SchemaSerializationUtils.mapAnnotationsToDtoFields(dbo.getProperties(), dto, schema);
		if (dbo.getOwnerId()==null) {
			dto.setOwnerId(null);
		} else {
			dto.setOwnerId(dbo.getOwnerId().toString());
		}
		dto.setEtag(dbo.geteTag());
	}
}
