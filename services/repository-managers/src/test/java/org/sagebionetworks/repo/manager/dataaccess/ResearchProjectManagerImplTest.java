package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.ChangeOwnershipRequest;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class ResearchProjectManagerImplTest {

	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private IdGenerator mockIdGenerator;
	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	@Mock
	private ResearchProjectDAO mockResearchProjectDao;
	@Mock
	private UserInfo mockUser;

	private ResearchProjectManagerImpl manager;
	private String accessRequirementId;
	private String userId;
	private String researchProjectId;
	private ResearchProject researchProject;
	private Date createdOn;
	private Date modifiedOn;
	private String etag;
	private String projectLead;
	private String institution;
	private String idu;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		manager = new ResearchProjectManagerImpl();
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(manager, "idGenerator", mockIdGenerator);
		ReflectionTestUtils.setField(manager, "accessRequirementDao", mockAccessRequirementDao);
		ReflectionTestUtils.setField(manager, "researchProjectDao", mockResearchProjectDao);

		userId = "1";
		accessRequirementId = "2";
		researchProjectId = "3";
		createdOn = new Date();
		modifiedOn = new Date();
		etag = "etag";
		projectLead = "projectLead";
		institution = "institution";
		idu = "intendedDataUseStatement";
		researchProject = createNewResearchProject();

		when(mockUser.getId()).thenReturn(1L);
		when(mockIdGenerator.generateNewId(TYPE.RESEARCH_PROJECT_ID)).thenReturn(3L);
		when(mockResearchProjectDao.create(any(ResearchProject.class))).thenReturn(researchProject);
		when(mockResearchProjectDao.get(accessRequirementId, userId)).thenReturn(researchProject);
		when(mockResearchProjectDao.get(researchProjectId)).thenReturn(researchProject);
		when(mockResearchProjectDao.getForUpdate(researchProjectId, etag)).thenReturn(researchProject);
		when(mockResearchProjectDao.update(any(ResearchProject.class))).thenReturn(researchProject);
		when(mockResearchProjectDao.changeOwnership(anyString(), anyString(), anyString(), anyLong(), anyString()))
				.thenReturn(researchProject);
	}

	private ResearchProject createNewResearchProject() {
		ResearchProject dto = new ResearchProject();
		dto.setId(researchProjectId);
		dto.setCreatedBy(userId);
		dto.setCreatedOn(createdOn);
		dto.setModifiedBy(userId);
		dto.setModifiedOn(modifiedOn);
		dto.setOwnerId(userId);
		dto.setEtag(etag);
		dto.setProjectLead(projectLead);
		dto.setInstitution(institution);
		dto.setIntendedDataUseStatement(idu);
		dto.setAccessRequirementId(accessRequirementId);
		return dto;
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullUserInfo() {
		manager.create(null, createNewResearchProject());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullResearchProject() {
		manager.create(mockUser, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullAccessRequirementId() {
		ResearchProject toCreate = createNewResearchProject();
		toCreate.setAccessRequirementId(null);
		manager.create(mockUser, toCreate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullProjectLead() {
		ResearchProject toCreate = createNewResearchProject();
		toCreate.setProjectLead(null);
		manager.create(mockUser, toCreate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullInstitution() {
		ResearchProject toCreate = createNewResearchProject();
		toCreate.setInstitution(null);
		manager.create(mockUser, toCreate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullIDU() {
		ResearchProject toCreate = createNewResearchProject();
		toCreate.setIntendedDataUseStatement(null);
		manager.create(mockUser, toCreate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithEmptyProjectLead() {
		ResearchProject toCreate = createNewResearchProject();
		toCreate.setProjectLead("");
		manager.create(mockUser, toCreate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithEmptyInstitution() {
		ResearchProject toCreate = createNewResearchProject();
		toCreate.setInstitution("");
		manager.create(mockUser, toCreate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithEmptyIDU() {
		ResearchProject toCreate = createNewResearchProject();
		toCreate.setIntendedDataUseStatement("");
		manager.create(mockUser, toCreate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNotACTAccessRequirementId() {
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(new TermsOfUseAccessRequirement());
		manager.create(null, createNewResearchProject());
	}

	@Test
	public void testCreate() {
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(new ACTAccessRequirement());
		assertEquals(researchProject, manager.create(mockUser, createNewResearchProject()));
		ArgumentCaptor<ResearchProject> captor = ArgumentCaptor.forClass(ResearchProject.class);
		verify(mockResearchProjectDao).create(captor.capture());
		ResearchProject toCreate = captor.getValue();
		assertEquals(researchProjectId, toCreate.getId());
		assertEquals(userId, toCreate.getCreatedBy());
		assertEquals(userId, toCreate.getOwnerId());
		assertEquals(userId, toCreate.getModifiedBy());
		assertEquals(projectLead, toCreate.getProjectLead());
		assertEquals(institution, toCreate.getInstitution());
		assertEquals(idu, toCreate.getIntendedDataUseStatement());
	}

	@Test
	public void testPrepareUpdateFields() {
		String modifiedBy = "111";
		ResearchProject prepared = manager.prepareUpdateFields(researchProject, modifiedBy);
		assertEquals(modifiedBy, prepared.getModifiedBy());
		assertFalse(prepared.getEtag().equals(etag));
	}

	@Test
	public void testPrepareCreationFields() {
		String createdBy = "222";
		ResearchProject prepared = manager.prepareCreationFields(researchProject, createdBy);
		assertEquals(createdBy, prepared.getModifiedBy());
		assertEquals(createdBy, prepared.getCreatedBy());
		assertEquals(createdBy, prepared.getOwnerId());
		assertFalse(prepared.getEtag().equals(etag));
		assertEquals(researchProjectId, prepared.getId());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetWithNullUserInfo() {
		manager.get(null, accessRequirementId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetWithNullAccessRequirementId() {
		manager.get(mockUser, null);
	}

	@Test (expected = NotFoundException.class)
	public void testGetNotFound() {
		when(mockResearchProjectDao.get(anyString(), anyString())).thenThrow(new NotFoundException());
		manager.get(mockUser, accessRequirementId);
	}

	@Test
	public void testGet() {
		assertEquals(researchProject, manager.get(mockUser, accessRequirementId));
		verify(mockResearchProjectDao).get(accessRequirementId, userId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithNullUserInfo() {
		manager.update(null, createNewResearchProject());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdatWeithNullResearchProject() {
		manager.update(mockUser, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithNullAccessRequirementId() {
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setAccessRequirementId(null);
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithNullProjectLead() {
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setProjectLead(null);
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithNullInstitution() {
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setInstitution(null);
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithNullIDU() {
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setIntendedDataUseStatement(null);
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = NotFoundException.class)
	public void testUpdateNotFound() {
		ResearchProject toUpdate = createNewResearchProject();
		when(mockResearchProjectDao.getForUpdate(anyString(), anyString())).thenThrow(new NotFoundException());
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateOwnerId() {
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setOwnerId("222");
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateCreatedBy() {
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setCreatedBy("333");
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateCreatedOn() {
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setCreatedOn(new Date(0L));
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateAccessRequirementId() {
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setAccessRequirementId("444");
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = ConflictingUpdateException.class)
	public void testUpdateWithOutdatedEtag() {
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setEtag("oldEtag");
		when(mockResearchProjectDao.getForUpdate(researchProjectId, "oldEtag")).thenThrow(new ConflictingUpdateException());
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = UnauthorizedException.class)
	public void testUpdateUnauthorized() {
		ResearchProject toUpdate = createNewResearchProject();
		when(mockUser.getId()).thenReturn(555L);
		manager.update(mockUser, toUpdate);
	}

	@Test
	public void testUpdate() {
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setIntendedDataUseStatement("new intendedDataUseStatement");
		assertEquals(researchProject, manager.update(mockUser, toUpdate));
		ArgumentCaptor<ResearchProject> captor = ArgumentCaptor.forClass(ResearchProject.class);
		verify(mockResearchProjectDao).update(captor.capture());
		ResearchProject updated = captor.getValue();
		assertEquals(researchProjectId, updated.getId());
		assertEquals(userId, updated.getCreatedBy());
		assertEquals(userId, updated.getOwnerId());
		assertEquals(userId, updated.getModifiedBy());
		assertEquals(projectLead, updated.getProjectLead());
		assertEquals(institution, updated.getInstitution());
		assertEquals("new intendedDataUseStatement", updated.getIntendedDataUseStatement());
	}

	@Test (expected = IllegalArgumentException.class)
	public void changeOwnershipWithNullUserInfo() {
		ChangeOwnershipRequest request = new ChangeOwnershipRequest();
		manager.changeOwnership(null, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void changeOwnershipWithNullRequest() {
		manager.changeOwnership(mockUser, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void changeOwnershipWithNullResearchProjectId() {
		ChangeOwnershipRequest request = new ChangeOwnershipRequest();
		request.setNewOwnerId("666");
		manager.changeOwnership(mockUser, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void changeOwnershipWithNullNewOwnerId() {
		ChangeOwnershipRequest request = new ChangeOwnershipRequest();
		request.setResearchProjectId(researchProjectId);
		manager.changeOwnership(mockUser, request);
	}

	@Test (expected = UnauthorizedException.class)
	public void changeOwnershipUnauthorized() {
		ChangeOwnershipRequest request = new ChangeOwnershipRequest();
		request.setNewOwnerId("666");
		request.setResearchProjectId(researchProjectId);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(false);
		manager.changeOwnership(mockUser, request);
	}

	@Test
	public void changeOwnership() {
		ChangeOwnershipRequest request = new ChangeOwnershipRequest();
		String newOwnerId = "666";
		request.setNewOwnerId(newOwnerId);
		request.setResearchProjectId(researchProjectId);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		assertEquals(researchProject, manager.changeOwnership(mockUser, request));
		verify(mockResearchProjectDao).changeOwnership(eq(researchProjectId), eq(newOwnerId), eq(userId), anyLong(), anyString());
	}
}
