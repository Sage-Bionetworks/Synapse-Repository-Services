package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class ResearchProjectManagerImplTest {

	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	@Mock
	private ResearchProjectDAO mockResearchProjectDao;
	@Mock
	private UserInfo mockUser;
	@Mock
	private ManagedACTAccessRequirement mockAccessRequirement;

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
		when(mockResearchProjectDao.create(any(ResearchProject.class))).thenReturn(researchProject);
		when(mockResearchProjectDao.getUserOwnResearchProject(accessRequirementId, userId)).thenReturn(researchProject);
		when(mockResearchProjectDao.get(researchProjectId)).thenReturn(researchProject);
		when(mockResearchProjectDao.getForUpdate(researchProjectId)).thenReturn(researchProject);
		when(mockResearchProjectDao.update(any(ResearchProject.class))).thenReturn(researchProject);
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(mockAccessRequirement);
	}

	private ResearchProject createNewResearchProject() {
		ResearchProject dto = new ResearchProject();
		dto.setId(researchProjectId);
		dto.setCreatedBy(userId);
		dto.setCreatedOn(createdOn);
		dto.setModifiedBy(userId);
		dto.setModifiedOn(modifiedOn);
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
		assertEquals(researchProject, manager.create(mockUser, createNewResearchProject()));
		ArgumentCaptor<ResearchProject> captor = ArgumentCaptor.forClass(ResearchProject.class);
		verify(mockResearchProjectDao).create(captor.capture());
		ResearchProject toCreate = captor.getValue();
		assertEquals(researchProjectId, toCreate.getId());
		assertEquals(userId, toCreate.getCreatedBy());
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
	}

	@Test
	public void testPrepareCreationFields() {
		String createdBy = "222";
		ResearchProject prepared = manager.prepareCreationFields(researchProject, createdBy);
		assertEquals(createdBy, prepared.getModifiedBy());
		assertEquals(createdBy, prepared.getCreatedBy());
		assertEquals(researchProjectId, prepared.getId());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetWithNullUserInfo() {
		manager.getUserOwnResearchProjectForUpdate(null, accessRequirementId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetWithNullAccessRequirementId() {
		manager.getUserOwnResearchProjectForUpdate(mockUser, null);
	}

	@Test
	public void testGetNotFound() {
		when(mockResearchProjectDao.getUserOwnResearchProject(anyString(), anyString())).thenThrow(new NotFoundException());
		ResearchProject rp = manager.getUserOwnResearchProjectForUpdate(mockUser, accessRequirementId);
		assertNotNull(rp);
		assertEquals(accessRequirementId, rp.getAccessRequirementId());
	}

	@Test
	public void testGet() {
		assertEquals(researchProject, manager.getUserOwnResearchProjectForUpdate(mockUser, accessRequirementId));
		verify(mockResearchProjectDao).getUserOwnResearchProject(accessRequirementId, userId);
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
		when(mockResearchProjectDao.getForUpdate(anyString())).thenThrow(new NotFoundException());
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
		assertEquals(userId, updated.getModifiedBy());
		assertEquals(projectLead, updated.getProjectLead());
		assertEquals(institution, updated.getInstitution());
		assertEquals("new intendedDataUseStatement", updated.getIntendedDataUseStatement());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateOrUpdateWithNullResearchProject() {
		manager.createOrUpdate(mockUser, null);
	}

	@Test
	public void testCreateOrUpdateWithNullId() {
		researchProject.setId(null);
		assertEquals(researchProject, manager.createOrUpdate(mockUser, researchProject));
		ArgumentCaptor<ResearchProject> captor = ArgumentCaptor.forClass(ResearchProject.class);
		verify(mockResearchProjectDao).create(captor.capture());
		ResearchProject toCreate = captor.getValue();
		assertEquals(userId, toCreate.getCreatedBy());
		assertEquals(userId, toCreate.getModifiedBy());
		assertEquals(projectLead, toCreate.getProjectLead());
		assertEquals(institution, toCreate.getInstitution());
		assertEquals(idu, toCreate.getIntendedDataUseStatement());
	}

	@Test
	public void testCreateOrUpdateWithId() {
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setIntendedDataUseStatement("new intendedDataUseStatement");
		assertEquals(researchProject, manager.createOrUpdate(mockUser, toUpdate));
		ArgumentCaptor<ResearchProject> captor = ArgumentCaptor.forClass(ResearchProject.class);
		verify(mockResearchProjectDao).update(captor.capture());
		ResearchProject updated = captor.getValue();
		assertEquals(researchProjectId, updated.getId());
		assertEquals(userId, updated.getCreatedBy());
		assertEquals(userId, updated.getModifiedBy());
		assertEquals(projectLead, updated.getProjectLead());
		assertEquals(institution, updated.getInstitution());
		assertEquals("new intendedDataUseStatement", updated.getIntendedDataUseStatement());
	}
}
