package org.sagebionetworks.repo.model.dbo.file.download.v2;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.download.MeetAccessRequirement;
import org.sagebionetworks.repo.model.download.RequestDownload;

import static org.junit.jupiter.api.Assertions.*;

public class FileActionRequiredTest {
	private static final long FILE_ID = 101;
	private static final long AR_ID = 202;
	private static final long BENEFACTOR_ID = 303;
	
	@Test
	void testValidMeetAccessRequirement() {
		FileActionRequired far = new FileActionRequired();
		MeetAccessRequirement action = new MeetAccessRequirement();
		action.setAccessRequirementId(AR_ID);
		far.withFileId(FILE_ID).withAction(action);
		
		assertTrue(far.isValid());
	}

	@Test
	void testINValidMeetAccessRequirement_NO_AR_ID() {
		FileActionRequired far = new FileActionRequired();
		MeetAccessRequirement action = new MeetAccessRequirement();
		action.setAccessRequirementId(null);
		far.withFileId(FILE_ID).withAction(action);
		
		assertFalse(far.isValid());
	}

	@Test
	void testINValid_NO_ACTION() {
		FileActionRequired far = new FileActionRequired();
		far.withAction(null);
		
		assertFalse(far.isValid());
	}

	@Test
	void testValidRequestDownload() {
		FileActionRequired far = new FileActionRequired();
		RequestDownload action = new RequestDownload();
		action.setBenefactorId(BENEFACTOR_ID);
		far.withFileId(FILE_ID).withAction(action);
		
		assertTrue(far.isValid());
	}

	@Test
	void testINValidRequestDownload_NO_BENEFACTOR_ID() {
		FileActionRequired far = new FileActionRequired();
		RequestDownload action = new RequestDownload();
		action.setBenefactorId(null);
		far.withFileId(FILE_ID).withAction(action);
		
		assertFalse(far.isValid());
	}

}
