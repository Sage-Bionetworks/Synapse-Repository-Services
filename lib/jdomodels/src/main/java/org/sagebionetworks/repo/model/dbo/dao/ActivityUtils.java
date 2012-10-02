package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.persistence.DBOActivity;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.provenance.Activity;

public class ActivityUtils {

	public static void copyDtoToDbo(Activity dto, DBOActivity dbo) throws DatastoreException {
		if(dto.getId() == null) throw new IllegalArgumentException("id can not be null");
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.seteTag(dto.getEtag());
		copyToSerializedField(dto, dbo);
	}

	public static <T extends Activity> T copyDboToDto(DBOActivity dbo) throws DatastoreException {
		if(dbo.getId() == null) throw new IllegalArgumentException("id can not be null");
		T dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId().toString());
		dto.setEtag(dbo.geteTag());
		return dto;
	}

	private static void copyToSerializedField(Activity dto, DBOActivity dbo) throws DatastoreException {
		try {
			dbo.setSerializedObject(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private static <T extends Activity> T copyFromSerializedField(DBOActivity dbo) throws DatastoreException {
		try {
			return (T)JDOSecondaryPropertyUtils.decompressedObject(dbo.getSerializedObject());
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	
}
