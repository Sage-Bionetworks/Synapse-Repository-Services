package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirementRevision;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSubjectAccessRequirement;

public class AccessRequirementUtilsTest {

	public static RestrictableObjectDescriptor createRestrictableObjectDescriptor(String id) {
		return createRestrictableObjectDescriptor(id, RestrictableObjectType.ENTITY);
	}

	public static RestrictableObjectDescriptor createRestrictableObjectDescriptor(String id, RestrictableObjectType type) {
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(id);
		rod.setType(type);
		return rod;
	}

	private static AccessRequirement createDTO() {
		TermsOfUseAccessRequirement dto = new TermsOfUseAccessRequirement();
		dto.setId(101L);
		dto.setEtag("0");
		dto.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{createRestrictableObjectDescriptor("syn999")}));
		dto.setCreatedBy("555");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("666");
		dto.setModifiedOn(new Date());
		dto.setConcreteType("org.sagebionetworks.repo.model.TermsOfUseAcessRequirement");
		dto.setAccessType(ACCESS_TYPE.DOWNLOAD);	
		dto.setTermsOfUse("foo");
		return dto;
	}

	@Test
	public void testRoundtrip() throws Exception {
		AccessRequirement dto = createDTO();
			
		DBOAccessRequirement dbo = new DBOAccessRequirement();
		AccessRequirementUtils.copyDtoToDbo(dto, dbo);
		List<RestrictableObjectDescriptor> nodeIds = new ArrayList<RestrictableObjectDescriptor>();
		for (RestrictableObjectDescriptor s : dto.getSubjectIds()) nodeIds.add(s);
		AccessRequirement dto2 = AccessRequirementUtils.copyDboToDto(dbo, nodeIds);
		assertEquals(dto, dto2);
	}

	@Test
	public void testRoundtripWithNulls() throws Exception {
		AccessRequirement dto = createDTO();
		dto.setId(null);
		DBOAccessRequirement dbo = new DBOAccessRequirement();
		AccessRequirementUtils.copyDtoToDbo(dto, dbo);
		List<RestrictableObjectDescriptor> nodeIds = new ArrayList<RestrictableObjectDescriptor>();
		for (RestrictableObjectDescriptor s : dto.getSubjectIds()) nodeIds.add(s);
		AccessRequirement dto2 = AccessRequirementUtils.copyDboToDto(dbo, nodeIds);
		assertEquals(dto, dto2);
	}

	@Test
	public void testCopyDBOarToDBOarr() throws Exception {
		AccessRequirement dto = createDTO();
		DBOAccessRequirement dbo = new DBOAccessRequirement();
		AccessRequirementUtils.copyDtoToDbo(dto, dbo);
		DBOAccessRequirementRevision dboARR = AccessRequirementUtils.copyDBOAccessRequirementToDBOAccessRequirementRevision(dbo);
		assertNotNull(dboARR);
		assertEquals(dbo.getId(), dboARR.getOwnerId());
		assertEquals(dbo.getCurrentRevNumber(), dboARR.getNumber());
		assertEquals(dbo.getModifiedBy(), dboARR.getModifiedBy());
		assertEquals(dbo.getModifiedOn(), dboARR.getModifiedOn());
		assertEquals(dbo.getAccessType(), dboARR.getAccessType());
		assertEquals(dbo.getConcreteType(), dboARR.getConcreteType());
		assertEquals(dbo.getSerializedEntity(), dboARR.getSerializedEntity());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateBatchDBOSubjectAccessRequirementWithNullAccessRequirementId() {
		AccessRequirementUtils.createBatchDBOSubjectAccessRequirement(null, new LinkedList<RestrictableObjectDescriptor>());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateBatchDBOSubjectAccessRequirementWithNullRodList() {
		AccessRequirementUtils.createBatchDBOSubjectAccessRequirement(0L, null);
	}

	@Test
	public void testCreateBatchDBOSubjectAccessRequirementWithEmptyRodList() {
		assertTrue(AccessRequirementUtils.createBatchDBOSubjectAccessRequirement(0L, new LinkedList<RestrictableObjectDescriptor>()).isEmpty());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCopyDBOSubjectsToDTOSubjectsWithNullList() {
		AccessRequirementUtils.copyDBOSubjectsToDTOSubjects(null);
	}

	@Test
	public void testCopyDBOSubjectsToDTOSubjectsWithEmptyList() {
		assertTrue(AccessRequirementUtils.copyDBOSubjectsToDTOSubjects(new LinkedList<DBOSubjectAccessRequirement>()).isEmpty());
	}

	@Test
	public void testSubjectAccessRequirementRoundTrip() {
		RestrictableObjectDescriptor rod1 = createRestrictableObjectDescriptor("syn1", RestrictableObjectType.ENTITY);
		RestrictableObjectDescriptor rod2 = createRestrictableObjectDescriptor("2", RestrictableObjectType.TEAM);
		Long requirementId = 3L;
		List<DBOSubjectAccessRequirement> dbos = AccessRequirementUtils.createBatchDBOSubjectAccessRequirement(requirementId, Arrays.asList(rod1, rod2));
		assertNotNull(dbos);
		assertEquals(2, dbos.size());
		assertEquals(requirementId, dbos.get(0).getAccessRequirementId());
		assertEquals(requirementId, dbos.get(1).getAccessRequirementId());
		assertEquals(Arrays.asList(rod1, rod2), AccessRequirementUtils.copyDBOSubjectsToDTOSubjects(dbos));
	}
}
