package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.persistence.DBOActivity;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.provenance.ActivityType;

public class ActivityUtilsTest {

	private static Activity createDTO() {
		Activity dto = new Activity();
		dto.setId("123456");
		dto.setEtag("0");		
		dto.setCreatedBy("555");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("666");
		dto.setModifiedOn(new Date());
		dto.setActivityType(ActivityType.CODE_EXECUTION);
		Reference ref = new Reference();
		ref.setTargetId("syn123");
		ref.setTargetVersionNumber((long)1);
		Set<Reference> used = new HashSet<Reference>();
		used.add(ref);
		dto.setUsed(used);
		Reference executedEntity = new Reference();
		executedEntity.setTargetId("syn456");
		executedEntity.setTargetVersionNumber((long)1);
		dto.setExecutedEntity(executedEntity);		
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
