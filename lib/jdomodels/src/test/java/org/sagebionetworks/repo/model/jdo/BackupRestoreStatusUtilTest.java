package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.DaemonType;
import org.sagebionetworks.repo.model.dbo.persistence.DBODaemonStatus;

public class BackupRestoreStatusUtilTest {

	@Test
	public void testRoundTrip() throws DatastoreException{
		String userGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
		// Make a round trip from the DTO->JDO->DTO
		BackupRestoreStatus dto = new BackupRestoreStatus();
		dto.setId("12");
		dto.setStatus(DaemonStatus.COMPLETED);
		dto.setType(DaemonType.BACKUP);
		dto.setStartedBy(userGroupId);
		dto.setStartedOn(new Date());
		dto.setProgresssMessage("Finally finished!");
		dto.setProgresssCurrent(12l);
		dto.setProgresssTotal(100l);
		dto.setTotalTimeMS(1234l);
		dto.setErrorDetails("Does not really have an error");
		dto.setErrorMessage("Short message");
		dto.setBackupUrl("https://somehost:port/buck/file.zip");
		dto.setLog("These are the notes that we kept");

		// Now upate a new JDO from the DTO
		DBODaemonStatus jdo = new DBODaemonStatus();
		jdo.setId(new Long(12));
		BackupRestoreStatusUtil.updateJdoFromDto(dto, jdo);
		// Create a clone from the JDO
		BackupRestoreStatus clone = BackupRestoreStatusUtil.createDtoFromJdo(jdo);
		assertEquals(dto, clone);
	}

	@Test
	public void testRoundTripWithOptionalNulls() throws DatastoreException{
		String userGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
		// Make a round trip from the DTO->JDO->DTO
		BackupRestoreStatus dto = new BackupRestoreStatus();
		dto.setId("12");
		dto.setStatus(DaemonStatus.COMPLETED);
		dto.setType(DaemonType.BACKUP);
		dto.setStartedBy(userGroupId);
		dto.setStartedOn(new Date());
		dto.setProgresssMessage("Finally finished!");
		dto.setProgresssCurrent(12l);
		dto.setProgresssTotal(100l);
		dto.setTotalTimeMS(1234l);
		dto.setErrorDetails(null);
		dto.setErrorMessage(null);
		dto.setBackupUrl(null);
		dto.setLog(null);

		// Now upate a new JDO from the DTO
		DBODaemonStatus jdo = new DBODaemonStatus();
		jdo.setId(new Long(12));
		BackupRestoreStatusUtil.updateJdoFromDto(dto, jdo);
		// Create a clone from the JDO
		BackupRestoreStatus clone = BackupRestoreStatusUtil.createDtoFromJdo(jdo);
		assertEquals(dto, clone);
	}
}
