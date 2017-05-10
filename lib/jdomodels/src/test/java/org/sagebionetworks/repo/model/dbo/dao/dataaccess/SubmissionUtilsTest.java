package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;

public class SubmissionUtilsTest {

	@Test
	public void testRoundTrip() {
		Submission dto = SubmissionTestUtils.createSubmission();
		DBOSubmission dbo = new DBOSubmission();
		SubmissionUtils.copyDtoToDbo(dto, dbo);
		DBOSubmissionStatus status = SubmissionUtils.getDBOStatus(dto);

		Submission newDto = SubmissionUtils.copyDboToDto(dbo, status);
		assertEquals(dto, newDto);

		status.setState("APPROVED");
		newDto = SubmissionUtils.copyDboToDto(dbo, status);
		dto.setState(SubmissionState.APPROVED);
		assertEquals(dto, newDto);
	}

	@Test
	public void testCreateDBOSubmissionAccessor() {
		IdGenerator mockIdGenerator = Mockito.mock(IdGenerator.class);
		when(mockIdGenerator.generateNewId(IdType.DATA_ACCESS_SUBMISSION_ACCESSOR_ID)).thenReturn(1L, 2L, 3L, 4L, 5L);
		Submission dto = SubmissionTestUtils.createSubmission();

		List<DBOSubmissionAccessor> accessors = SubmissionUtils.createDBOSubmissionAccessor(dto, mockIdGenerator);
		Set<String> accessorsSet = new HashSet<String>();
		for (DBOSubmissionAccessor accessor : accessors) {
			assertNotNull(accessor.getId());
			assertNotNull(accessor.getEtag());
			assertEquals(dto.getId(), accessor.getCurrentSubmissionId().toString());
			assertEquals(dto.getAccessRequirementId(), accessor.getAccessRequirementId().toString());
			accessorsSet.add(accessor.getAccessorId().toString());
		}
		assertEquals(accessorsSet, new HashSet<String>(dto.getAccessors()));
	}

}
