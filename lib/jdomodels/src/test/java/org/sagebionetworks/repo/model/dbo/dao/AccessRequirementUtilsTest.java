package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

import com.google.common.collect.Lists;

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
		dto.setSubjectIds(Lists.newArrayList(createRestrictableObjectDescriptor("syn999")));
		dto.setCreatedBy("555");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("666");
		dto.setModifiedOn(new Date());
		dto.setConcreteType(TermsOfUseAccessRequirement.class.getName());
		dto.setAccessType(ACCESS_TYPE.DOWNLOAD);
		dto.setTermsOfUse("foo");
		dto.setVersionNumber(1L);
		return dto;
	}

	@Test
	public void testRoundtrip() throws Exception {
		AccessRequirement dto = createDTO();
		RestrictableObjectDescriptor rod = dto.getSubjectIds().get(0);
		// add a duplicate
		dto.getSubjectIds().add(rod);
			
		DBOAccessRequirement dboRequirement = new DBOAccessRequirement();
		DBOAccessRequirementRevision dboRevision = new DBOAccessRequirementRevision();
		AccessRequirementUtils.copyDtoToDbo(dto, dboRequirement, dboRevision);
		AccessRequirement dto2 = AccessRequirementUtils.copyDboToDto(dboRequirement, dboRevision);
		assertEquals(dto, dto2);
		assertEquals(1, dto.getSubjectIds().size());
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
		List<RestrictableObjectDescriptor> rodList = AccessRequirementUtils.copyDBOSubjectsToDTOSubjects(dbos);
		assertNotNull(rodList);
		assertEquals(2, rodList.size());
		assertTrue(rodList.contains(rod1));
		assertTrue(rodList.contains(rod2));
	}
	
	@Test
	public void testValidateFieldsValid(){
		AccessRequirement dto = createDTO();
		AccessRequirementUtils.validateFields(dto);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateFieldsNull(){
		AccessRequirementUtils.validateFields(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateFieldsNullAccessType(){
		AccessRequirement dto = createDTO();
		dto.setAccessType(null);
		AccessRequirementUtils.validateFields(dto);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateFieldsNullConcreteType(){
		AccessRequirement dto = createDTO();
		dto.setConcreteType(null);
		AccessRequirementUtils.validateFields(dto);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateFieldsWrongConcreteType(){
		AccessRequirement dto = createDTO();
		dto.setConcreteType("not.correct");
		AccessRequirementUtils.validateFields(dto);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateFieldsNullCreatedBy(){
		AccessRequirement dto = createDTO();
		dto.setCreatedBy(null);
		AccessRequirementUtils.validateFields(dto);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateFieldsNullCreatedOn(){
		AccessRequirement dto = createDTO();
		dto.setCreatedOn(null);
		AccessRequirementUtils.validateFields(dto);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateFieldsNullEtag(){
		AccessRequirement dto = createDTO();
		dto.setEtag(null);
		AccessRequirementUtils.validateFields(dto);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateFieldsNullId(){
		AccessRequirement dto = createDTO();
		dto.setId(null);
		AccessRequirementUtils.validateFields(dto);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateFieldsNullModifiedBy(){
		AccessRequirement dto = createDTO();
		dto.setModifiedBy(null);
		AccessRequirementUtils.validateFields(dto);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateFieldsNullModifiedOn(){
		AccessRequirement dto = createDTO();
		dto.setModifiedOn(null);
		AccessRequirementUtils.validateFields(dto);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateFieldsNullVersionNumber(){
		AccessRequirement dto = createDTO();
		dto.setVersionNumber(null);
		AccessRequirementUtils.validateFields(dto);
	}
	
	@Test
	public void testGetUniqueRestrictableObjectDescriptor(){
		List<RestrictableObjectDescriptor> start = Lists.newArrayList(
				createRestrictableObjectDescriptor("syn999"),
				createRestrictableObjectDescriptor("syn111"),
				createRestrictableObjectDescriptor("syn111")
				);
		List<RestrictableObjectDescriptor> expected = Lists.newArrayList(
				createRestrictableObjectDescriptor("syn999"),
				createRestrictableObjectDescriptor("syn111")
				);
		List<RestrictableObjectDescriptor> result = AccessRequirementUtils.getUniqueRestrictableObjectDescriptor(start);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetUniqueRestrictableObjectDescriptorNull(){
		List<RestrictableObjectDescriptor> start = null;
		List<RestrictableObjectDescriptor> result = AccessRequirementUtils.getUniqueRestrictableObjectDescriptor(start);
		assertEquals(null, result);
	}
}
