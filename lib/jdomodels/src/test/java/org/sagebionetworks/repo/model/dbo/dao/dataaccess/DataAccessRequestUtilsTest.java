package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRenewal;

public class DataAccessRequestUtilsTest {

	@Test
	public void testCopyDtoToDboRoundTrip() {
		DataAccessRenewal dto = DataAccessRequestTestUtils.createNewDataAccessRenewal();

		DBODataAccessRequest dbo = new DBODataAccessRequest();
		DataAccessRequestUtils.copyDtoToDbo(dto, dbo);
		DataAccessRenewal newDto = (DataAccessRenewal) DataAccessRequestUtils.copyDboToDto(dbo);
		assertEquals(dto, newDto);
	}
}
