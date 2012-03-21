package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

public class UserGroupUtilsTest {

	@Test
	public void testRoundtrip() throws Exception {
		UserGroup dto = new UserGroup();
		// TODO set all the fields
		dto.setId(KeyFactory.keyToString(1001L));
		dto.setName("foo@domain.org");
		dto.setCreationDate(new Date());
		dto.setEtag("1");
		dto.setIndividual(true);
		dto.setUri("/userGroup");
		DBOUserGroup dbo = new DBOUserGroup();
		UserGroupUtils.copyDtoToDbo(dto, dbo);
		UserGroup dto2 = new UserGroup();
		UserGroupUtils.copyDboToDto(dbo, dto2);
		assertEquals(dto, dto2);
	}

}
