package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
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

	@Test
	public void testCreateBatchDBOSubjectAccessRequirementWithNullAccessRequirementId() {
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			AccessRequirementUtils.createBatchDBOSubjectAccessRequirement(null, new LinkedList<RestrictableObjectDescriptor>());
		});
	}

	@Test
	public void testCreateBatchDBOSubjectAccessRequirementWithNullRodList() {
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			AccessRequirementUtils.createBatchDBOSubjectAccessRequirement(0L, null);
		});
	}

	@Test
	public void testCreateBatchDBOSubjectAccessRequirementWithEmptyRodList() {
		assertTrue(AccessRequirementUtils.createBatchDBOSubjectAccessRequirement(0L, new LinkedList<RestrictableObjectDescriptor>()).isEmpty());
	}

	@Test
	public void testCopyDBOSubjectsToDTOSubjectsWithNullList() {
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			AccessRequirementUtils.copyDBOSubjectsToDTOSubjects(null);
		});
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
	
	@Test
	public void testValidateFieldsNull(){
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			AccessRequirementUtils.validateFields(null);
		});
	}
	
	@Test
	public void testValidateFieldsNullAccessType(){
		AccessRequirement dto = createDTO();
		dto.setAccessType(null);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			AccessRequirementUtils.validateFields(dto);
		});
	}
	
	@Test
	public void testValidateFieldsNullConcreteType(){
		AccessRequirement dto = createDTO();
		dto.setConcreteType(null);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			AccessRequirementUtils.validateFields(dto);
		});
	}
	
	@Test
	public void testValidateFieldsWrongConcreteType(){
		AccessRequirement dto = createDTO();
		dto.setConcreteType("not.correct");
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			AccessRequirementUtils.validateFields(dto);
		});
	}
	
	@Test
	public void testValidateFieldsNullCreatedBy(){
		AccessRequirement dto = createDTO();
		dto.setCreatedBy(null);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			AccessRequirementUtils.validateFields(dto);
		});
	}
	
	@Test
	public void testValidateFieldsNullCreatedOn(){
		AccessRequirement dto = createDTO();
		dto.setCreatedOn(null);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			AccessRequirementUtils.validateFields(dto);
		});
	}
	
	@Test
	public void testValidateFieldsNullEtag(){
		AccessRequirement dto = createDTO();
		dto.setEtag(null);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			AccessRequirementUtils.validateFields(dto);
		});
	}
	
	@Test
	public void testValidateFieldsNullId(){
		AccessRequirement dto = createDTO();
		dto.setId(null);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			AccessRequirementUtils.validateFields(dto);
		});
	}
	
	@Test
	public void testValidateFieldsNullModifiedBy(){
		AccessRequirement dto = createDTO();
		dto.setModifiedBy(null);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			AccessRequirementUtils.validateFields(dto);
		});
	}
	
	@Test
	public void testValidateFieldsNullModifiedOn(){
		AccessRequirement dto = createDTO();
		dto.setModifiedOn(null);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			AccessRequirementUtils.validateFields(dto);
		});
	}
	
	@Test
	public void testValidateFieldsNullVersionNumber(){
		AccessRequirement dto = createDTO();
		dto.setVersionNumber(null);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			AccessRequirementUtils.validateFields(dto);
		});
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
	
	@Test
	public void testExtractAllFileHandleIdsWithNoFileHandle() {
		AccessRequirement dto = new ManagedACTAccessRequirement();
		
		Set<String> expected = Collections.emptySet();
		
		// Call under test
		Set<String> result = AccessRequirementUtils.extractAllFileHandleIds(dto);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testExtractAllFileHandleIdsWithFileHandle() {
		AccessRequirement dto = new ManagedACTAccessRequirement().setDucTemplateFileHandleId("123");
		
		Set<String> expected = Collections.singleton("123");
		
		// Call under test
		Set<String> result = AccessRequirementUtils.extractAllFileHandleIds(dto);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testExtractAllFileHandleIdsWithNotManaged() {
		AccessRequirement dto = createDTO();
		
		Set<String> expected = Collections.emptySet();
		
		// Call under test
		Set<String> result = AccessRequirementUtils.extractAllFileHandleIds(dto);
		
		assertEquals(expected, result);
	}
}
