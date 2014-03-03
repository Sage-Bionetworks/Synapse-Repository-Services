package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUploadDaemonStatus;
import org.sagebionetworks.repo.model.file.State;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;


public class UploadDaemonStatusUtilsTest {
	
	@Test
	public void testRoundTrip(){
		UploadDaemonStatus dto = new UploadDaemonStatus();
		dto.setRunTimeMS(10L);
		dto.setErrorMessage("error");
		dto.setFileHandleId("123");
		dto.setDaemonId("456");
		dto.setPercentComplete(15.0);
		dto.setStartedBy("987");
		dto.setStartedOn(new Date());
		dto.setState(State.COMPLETED);
		// DBO
		DBOUploadDaemonStatus dbo = UploadDaemonStatusUtils.createDBOFromDTO(dto);
		// Clone 
		UploadDaemonStatus clone = UploadDaemonStatusUtils.createDTOFromDBO(dbo);
		assertEquals(dto, clone);
	}
	
	@Test
	public void testRoundTripNulls(){
		UploadDaemonStatus dto = new UploadDaemonStatus();
		dto.setRunTimeMS(null);
		dto.setErrorMessage(null);
		dto.setFileHandleId(null);
		dto.setDaemonId(null);
		dto.setPercentComplete(null);
		dto.setStartedBy(null);
		dto.setStartedOn(null);
		dto.setState(State.FAILED);
		// DBO
		DBOUploadDaemonStatus dbo = UploadDaemonStatusUtils.createDBOFromDTO(dto);
		// Clone 
		UploadDaemonStatus clone = UploadDaemonStatusUtils.createDTOFromDBO(dbo);
		assertEquals(dto, clone);
	}

}
