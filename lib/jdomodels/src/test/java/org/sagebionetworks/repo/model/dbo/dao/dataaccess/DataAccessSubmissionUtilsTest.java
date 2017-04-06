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
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;

public class DataAccessSubmissionUtilsTest {

	@Test
	public void testRoundTrip() {
		DataAccessSubmission dto = DataAccessSubmissionTestUtils.createSubmission();
		DBODataAccessSubmission dbo = new DBODataAccessSubmission();
		DataAccessSubmissionUtils.copyDtoToDbo(dto, dbo);
		DBODataAccessSubmissionStatus status = DataAccessSubmissionUtils.getDBOStatus(dto);

		DataAccessSubmission newDto = DataAccessSubmissionUtils.copyDboToDto(dbo, status);
		assertEquals(dto, newDto);

		status.setState("APPROVED");
		newDto = DataAccessSubmissionUtils.copyDboToDto(dbo, status);
		dto.setState(DataAccessSubmissionState.APPROVED);
		assertEquals(dto, newDto);
	}

	@Test
	public void testCreateDBODataAccessSubmissionAccessor() {
		IdGenerator mockIdGenerator = Mockito.mock(IdGenerator.class);
		when(mockIdGenerator.generateNewId(IdType.DATA_ACCESS_SUBMISSION_ACCESSOR_ID)).thenReturn(1L, 2L, 3L, 4L, 5L);
		DataAccessSubmission dto = DataAccessSubmissionTestUtils.createSubmission();

		List<DBODataAccessSubmissionAccessor> accessors = DataAccessSubmissionUtils.createDBODataAccessSubmissionAccessor(dto, mockIdGenerator);
		Set<String> accessorsSet = new HashSet<String>();
		for (DBODataAccessSubmissionAccessor accessor : accessors) {
			assertNotNull(accessor.getId());
			assertNotNull(accessor.getEtag());
			assertEquals(dto.getId(), accessor.getCurrentSubmissionId().toString());
			assertEquals(dto.getAccessRequirementId(), accessor.getAccessRequirementId().toString());
			accessorsSet.add(accessor.getAccessorId().toString());
		}
		assertEquals(accessorsSet, new HashSet<String>(dto.getAccessors()));
	}

}
