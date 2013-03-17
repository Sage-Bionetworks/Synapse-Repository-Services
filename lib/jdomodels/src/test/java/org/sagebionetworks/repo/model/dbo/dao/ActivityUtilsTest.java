package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.persistence.DBOActivity;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.provenance.Used;
import org.sagebionetworks.repo.model.provenance.UsedEntity;
import org.sagebionetworks.repo.model.provenance.UsedURL;

public class ActivityUtilsTest {

	private static Activity createDTO() {
		Activity dto = new Activity();
		dto.setId("123456");
		dto.setEtag("0");		
		dto.setCreatedBy("555");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("666");
		dto.setModifiedOn(new Date());		
		UsedEntity usedEnt = new UsedEntity();
		Reference ref = new Reference();
		ref.setTargetId("syn123");
		ref.setTargetVersionNumber((long)1);
		usedEnt.setReference(ref);
		usedEnt.setWasExecuted(true);
		Set<Used> used = new HashSet<Used>();
		UsedURL ux = new UsedURL();
		ux.setUrl("http://url.com");
		used.add(ux);
		used.add(usedEnt);
		dto.setUsed(used);
		return dto;
	}
	
	@Test
	public void testRoundtrip() throws Exception {
		Activity dto = createDTO();			
		DBOActivity dbo = new DBOActivity();
		ActivityUtils.copyDtoToDbo(dto, dbo);
		Activity dto2 = ActivityUtils.copyDboToDto(dbo);
		assertEquals(dto, dto2);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testRoundtripWithNulls() throws Exception {
		Activity dto = createDTO();
		dto.setId(null);
		DBOActivity dbo = new DBOActivity();
		ActivityUtils.copyDtoToDbo(dto, dbo);
		fail("IllegalArgument should have been thrown");
	}

}
