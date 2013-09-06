package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;

public class UserGroupUtilsTest {

	@Test
	public void testRoundtrip() throws Exception {
		UserGroup dto = new UserGroup();
		// TODO set all the fields
		dto.setId("1001");
		dto.setName("foo@domain.org");
		dto.setCreationDate(new Date());
		dto.setIsIndividual(true);
		dto.setUri("/userGroup");
		dto.setEtag("Bloop");
		DBOUserGroup dbo = new DBOUserGroup();
		UserGroupUtils.copyDtoToDbo(dto, dbo);
		UserGroup dto2 = new UserGroup();
		UserGroupUtils.copyDboToDto(dbo, dto2);
		dto2.setUri("/userGroup");
		assertEquals(dto, dto2);
	}

}
