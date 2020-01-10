package org.sagebionetworks.repo.model.dbo.trash;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

public final class TrashedEntityUtils {

	public static List<TrashedEntity> convertDboToDto(List<DBOTrashedEntity> dboList) {

		if (dboList == null) {
			throw new IllegalArgumentException("DBO list cannot be null.");
		}

		List<TrashedEntity> trashList = new ArrayList<TrashedEntity>(dboList.size());
		for (DBOTrashedEntity dbo : dboList) {
			trashList.add(convertDboToDto(dbo));
		}
		return trashList;
	}

	public static TrashedEntity convertDboToDto(DBOTrashedEntity dbo) {

		if (dbo == null) {
			throw new IllegalArgumentException("DBO cannot be null.");
		}

		TrashedEntity trash = new TrashedEntity();
		trash.setEntityId(KeyFactory.keyToString(dbo.getId()));
		trash.setEntityName(dbo.getNodeName());
		trash.setOriginalParentId(KeyFactory.keyToString(dbo.getParentId()));
		trash.setDeletedByPrincipalId(dbo.getDeletedBy().toString());
		trash.setDeletedOn(dbo.getDeletedOn());
		return trash;
	}
}
