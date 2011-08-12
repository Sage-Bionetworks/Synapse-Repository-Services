package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.BackupRestoreStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.jdo.persistence.JDOBackupRestoreStatus;

public class BackupRestoreStatusUtilTest {
	
	@Test
	public void testRoundTrip() throws DatastoreException{
		// Make a round trip from the DTO->JDO->DTO
		BackupRestoreStatus dto = new BackupRestoreStatus();
		dto.setId("12");
		dto.setStatus(BackupRestoreStatus.STATUS.COMPLETED.name());
		dto.setType(BackupRestoreStatus.TYPE.BACKUP.name());
		dto.setStartedBy("someAdmin@sagebase.org");
		dto.setStartedOn(new Date());
		dto.setProgresssMessage("Finally finished!");
		dto.setProgresssCurrent(12l);
		dto.setProgresssTotal(100l);
		dto.setTotalTimeMS(1234l);
		dto.setErrorDetails("Does not really have an error");
		dto.setErrorMessage("Short message");
		dto.setBackupUrl("https://somehost:port/buck/file.zip");

		// Now upate a new JDO from the DTO
		JDOBackupRestoreStatus jdo = new JDOBackupRestoreStatus();
		jdo.setId(new Long(12));
		BackupRestoreStatusUtil.updateJdoFromDto(dto, jdo);
		// Create a clone from the JDO
		BackupRestoreStatus clone = BackupRestoreStatusUtil.createDtoFromJdo(jdo);
		assertEquals(dto, clone);
	}

	@Test
	public void testRoundTripWithOptionalNulls() throws DatastoreException{
		// Make a round trip from the DTO->JDO->DTO
		BackupRestoreStatus dto = new BackupRestoreStatus();
		dto.setId("12");
		dto.setStatus(BackupRestoreStatus.STATUS.COMPLETED.name());
		dto.setType(BackupRestoreStatus.TYPE.BACKUP.name());
		dto.setStartedBy("someAdmin@sagebase.org");
		dto.setStartedOn(new Date());
		dto.setProgresssMessage("Finally finished!");
		dto.setProgresssCurrent(12l);
		dto.setProgresssTotal(100l);
		dto.setTotalTimeMS(1234l);
		dto.setErrorDetails(null);
		dto.setErrorMessage(null);
		dto.setBackupUrl(null);

		// Now upate a new JDO from the DTO
		JDOBackupRestoreStatus jdo = new JDOBackupRestoreStatus();
		jdo.setId(new Long(12));
		BackupRestoreStatusUtil.updateJdoFromDto(dto, jdo);
		// Create a clone from the JDO
		BackupRestoreStatus clone = BackupRestoreStatusUtil.createDtoFromJdo(jdo);
		assertEquals(dto, clone);
	}
}
