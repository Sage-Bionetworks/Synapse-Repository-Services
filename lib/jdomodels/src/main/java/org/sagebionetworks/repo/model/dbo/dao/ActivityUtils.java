package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.util.Date;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dbo.persistence.DBOActivity;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.provenance.Activity;

public class ActivityUtils {
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(Activity.class).build();

	public static void copyDtoToDbo(Activity dto, DBOActivity dbo) throws DatastoreException {
		if(dto.getId() == null) throw new IllegalArgumentException("id can not be null");
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.seteTag(dto.getEtag());
		if (dto.getCreatedBy() != null)
			dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));		
		if(dto.getCreatedOn() != null)
			dbo.setCreatedOn(dto.getCreatedOn().getTime());		
		if (dto.getModifiedBy()==null) throw new InvalidModelException("modifiedBy may not be null");
			dbo.setModifiedBy(Long.parseLong(dto.getModifiedBy()));
		if (dto.getModifiedOn()==null) throw new InvalidModelException("modifiedOn may not be null");
			dbo.setModifiedOn(dto.getModifiedOn().getTime());
		copyToSerializedField(dto, dbo);
	}

	public static Activity copyDboToDto(DBOActivity dbo) throws DatastoreException {
		if(dbo.getId() == null) throw new IllegalArgumentException("id can not be null");
		Activity dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId().toString());
		dto.setEtag(dbo.getEtag());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setModifiedBy(dbo.getModifiedBy().toString());
		dto.setModifiedOn(new Date(dbo.getModifiedOn()));
		return dto;
	}

	private static void copyToSerializedField(Activity dto, DBOActivity dbo) throws DatastoreException {
		try {
			dbo.setSerializedObject(JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	private static Activity copyFromSerializedField(DBOActivity dbo) throws DatastoreException {
		try {
			return (Activity)JDOSecondaryPropertyUtils.decompressObject(X_STREAM, dbo.getSerializedObject());
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	
}
