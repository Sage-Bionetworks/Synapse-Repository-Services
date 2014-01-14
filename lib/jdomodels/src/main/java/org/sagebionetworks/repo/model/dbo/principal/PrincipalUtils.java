package org.sagebionetworks.repo.model.dbo.principal;

import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroupBackup;

/**
 * Simple translation util.
 * @author John
 *
 */
public class PrincipalUtils {

	/**
	 * Translate from a backup to a dbo.
	 * @param dbo
	 * @return
	 */
	public static DBOUserGroupBackup createBackup(DBOUserGroup dbo){
		DBOUserGroupBackup backup = new DBOUserGroupBackup();
		backup.setCreationDate(dbo.getCreationDate());
		backup.setEtag(dbo.getEtag());
		backup.setId(dbo.getId());
		backup.setIsIndividual(dbo.getIsIndividual());
		return backup;
	}
	
	/**
	 * Translate a backup from a DBO.
	 * @param backup
	 * @return
	 */
	public static DBOUserGroup createDBO(DBOUserGroupBackup backup){
		DBOUserGroup dbo = new DBOUserGroup();
		dbo.setCreationDate(backup.getCreationDate());
		dbo.setEtag(backup.getEtag());
		dbo.setId(backup.getId());
		dbo.setIsIndividual(backup.getIsIndividual());
		return dbo;
	}
}
