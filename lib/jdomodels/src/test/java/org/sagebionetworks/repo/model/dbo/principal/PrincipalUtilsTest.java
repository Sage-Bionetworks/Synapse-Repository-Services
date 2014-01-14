package org.sagebionetworks.repo.model.dbo.principal;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroupBackup;

public class PrincipalUtilsTest {

	@Test
	public void testRoundTrip(){
		DBOUserGroup dbo = new DBOUserGroup();
		dbo.setCreationDate(new Date());
		dbo.setEtag("etag");
		dbo.setId(new Long(123));
		dbo.setIsIndividual(Boolean.FALSE);
		
		// Backup 
		DBOUserGroupBackup backup = PrincipalUtils.createBackup(dbo);
		assertNotNull(backup);
		// and back
		DBOUserGroup clone = PrincipalUtils.createDBO(backup);
		assertEquals(dbo, clone);
	}
}
