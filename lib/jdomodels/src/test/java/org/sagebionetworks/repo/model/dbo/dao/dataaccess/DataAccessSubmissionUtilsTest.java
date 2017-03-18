package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;

public class DataAccessSubmissionUtilsTest {

	@Test
	public void testRoundTrip() {
		DataAccessSubmission dto = DataAccessSubmissionTestUtils.createSubmission();
		DBODataAccessSubmission dbo = new DBODataAccessSubmission();
		DataAccessSubmissionUtils.copyDtoToDbo(dto, dbo);
		DBODataAccessSubmissionStatus status = DataAccessSubmissionUtils.getDBOStatus(dto);

		List<DBODataAccessSubmissionAccessor> accessors = DataAccessSubmissionUtils.getDBOAccessors(dto);
		Set<String> accessorsSet = new HashSet<String>();
		for (DBODataAccessSubmissionAccessor accessor : accessors) {
			assertEquals(dto.getId(), accessor.getSubmissionId().toString());
			accessorsSet.add(accessor.getAccessorId().toString());
		}
		assertEquals(accessorsSet, new HashSet<String>(dto.getAccessors()));

		DataAccessSubmission newDto = DataAccessSubmissionUtils.copyDboToDto(dbo, status);
		assertEquals(dto, newDto);

		status.setState(DataAccessSubmissionState.APPROVED);
		newDto = DataAccessSubmissionUtils.copyDboToDto(dbo, status);
		dto.setState(DataAccessSubmissionState.APPROVED);
		assertEquals(dto, newDto);
	}

}
