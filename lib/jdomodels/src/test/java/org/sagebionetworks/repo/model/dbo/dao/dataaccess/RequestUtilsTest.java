package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.dataaccess.Renewal;

public class RequestUtilsTest {

	@Test
	public void testCopyDtoToDboRoundTrip() {
		Renewal dto = RequestTestUtils.createNewRenewal();

		DBORequest dbo = new DBORequest();
		RequestUtils.copyDtoToDbo(dto, dbo);
		Renewal newDto = (Renewal) RequestUtils.copyDboToDto(dbo);
		assertEquals(dto, newDto);
	}
}
