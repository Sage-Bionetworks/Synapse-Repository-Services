package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class ResearchProjectManagerImplTest {

	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	@Mock
	private ResearchProjectDAO mockResearchProjectDao;
	@Mock
	private UserInfo mockUser;
	@Mock
	private ManagedACTAccessRequirement mockAccessRequirement;
	@InjectMocks
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

	@BeforeEach
	public void before() {
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

	@Test
	public void testCreateWithNullUserInfo() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.create(null, createNewResearchProject());
		});
		
		assertEquals("The user is required.", ex.getMessage());
	}

	@Test
	public void testCreateWithNullResearchProject() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.create(mockUser, null);
		});
		
		assertEquals("The research project is required.", ex.getMessage());
	}

	@Test
	public void testCreateWithNullAccessRequirementId() {
		ResearchProject toCreate = createNewResearchProject();
		toCreate.setAccessRequirementId(null);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.create(mockUser, toCreate);
		});
		
		assertEquals("The accessRequirementId is required.", ex.getMessage());
	}

	@Test
	public void testCreateWithNullProjectLead() {
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		
		ResearchProject toCreate = createNewResearchProject();
		toCreate.setProjectLead(null);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.create(mockUser, toCreate);
		});
		
		assertEquals("The projectLead must contains more than 0 characters.", ex.getMessage());
	}

	@Test
	public void testCreateWithNullInstitution() {
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		
		ResearchProject toCreate = createNewResearchProject();
		toCreate.setInstitution(null);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.create(mockUser, toCreate);
		});
		
		assertEquals("The insitution must contains more than 0 characters.", ex.getMessage());
	}

	@Test
	public void testCreateWithNullIDUWhenRequired() {
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		when(mockAccessRequirement.getIsIDURequired()).thenReturn(true);
		
		ResearchProject toCreate = createNewResearchProject();
		toCreate.setIntendedDataUseStatement(null);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.create(mockUser, toCreate);
		});

		assertEquals("The intended data use statement must contains more than 0 characters.", ex.getMessage());
	}
	
	@Test
	public void testCreateWithNullIDUWhenOptional() {
		when(mockUser.getId()).thenReturn(Long.valueOf(userId));
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		
		// The AR defines the IDU as optional
		when(mockAccessRequirement.getIsIDURequired()).thenReturn(false);
		when(mockResearchProjectDao.create(any())).thenReturn(researchProject);
		
		ResearchProject toCreate = createNewResearchProject();
		
		// It should now be possible to have a nullable IDU
		toCreate.setIntendedDataUseStatement(null);
		
		// Call under test
		manager.create(mockUser, toCreate);
		
		verify(mockResearchProjectDao).create(toCreate);

	}

	@Test
	public void testCreateWithEmptyProjectLead() {
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		ResearchProject toCreate = createNewResearchProject();
		toCreate.setProjectLead("");
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.create(mockUser, toCreate);
		});

		assertEquals("The projectLead must contains more than 0 characters.", ex.getMessage());
	}

	@Test
	public void testCreateWithEmptyInstitution() {
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		
		ResearchProject toCreate = createNewResearchProject();
		toCreate.setInstitution("");
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.create(mockUser, toCreate);
		});

		assertEquals("The insitution must contains more than 0 characters.", ex.getMessage());
	}

	@Test
	public void testCreateWithEmptyIDUWhenRequired() {
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		when(mockAccessRequirement.getIsIDURequired()).thenReturn(true);
		
		ResearchProject toCreate = createNewResearchProject();
		toCreate.setIntendedDataUseStatement("");
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.create(mockUser, toCreate);
		});

		assertEquals("The intended data use statement must contains more than 0 characters.", ex.getMessage());
	}

	@Test
	public void testCreateWithNotACTAccessRequirementId() {
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(new TermsOfUseAccessRequirement());

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.create(mockUser, createNewResearchProject());
		});

		assertEquals("A research project can only be associated with an ManagedACTAccessRequirement.", ex.getMessage());

	}

	@Test
	public void testCreate() {
		when(mockUser.getId()).thenReturn(Long.valueOf(userId));
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		when(mockResearchProjectDao.create(any())).thenReturn(researchProject);
		
		// Call under test
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
		
		// Call under test
		ResearchProject prepared = manager.prepareUpdateFields(researchProject, modifiedBy);
		assertEquals(modifiedBy, prepared.getModifiedBy());
	}

	@Test
	public void testPrepareCreationFields() {
		String createdBy = "222";
		
		// Call under test
		ResearchProject prepared = manager.prepareCreationFields(researchProject, createdBy);
		assertEquals(createdBy, prepared.getModifiedBy());
		assertEquals(createdBy, prepared.getCreatedBy());
		assertEquals(researchProjectId, prepared.getId());
	}

	@Test
	public void testGetWithNullUserInfo() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.getUserOwnResearchProjectForUpdate(null, accessRequirementId);
		});

		assertEquals("The user is required.", ex.getMessage());
	}

	@Test
	public void testGetWithNullAccessRequirementId() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.getUserOwnResearchProjectForUpdate(mockUser, null);
		});

		assertEquals("The accessRequirementId is required.", ex.getMessage());
	}

	@Test
	public void testGetNotFound() {
		when(mockResearchProjectDao.getUserOwnResearchProject(anyString(), anyString())).thenThrow(new NotFoundException());
		
		// Call under test
		ResearchProject rp = manager.getUserOwnResearchProjectForUpdate(mockUser, accessRequirementId);
		assertNotNull(rp);
		assertEquals(accessRequirementId, rp.getAccessRequirementId());
	}

	@Test
	public void testGet() {
		when(mockUser.getId()).thenReturn(Long.valueOf(userId));
		when(mockResearchProjectDao.getUserOwnResearchProject(accessRequirementId, userId)).thenReturn(researchProject);
		
		// Call under test
		assertEquals(researchProject, manager.getUserOwnResearchProjectForUpdate(mockUser, accessRequirementId));
		verify(mockResearchProjectDao).getUserOwnResearchProject(accessRequirementId, userId);
	}

	@Test
	public void testUpdateWithNullUserInfo() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.update(null, createNewResearchProject());
		});

		assertEquals("The user is required.", ex.getMessage());
	}

	@Test
	public void testUpdatWeithNullResearchProject() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.update(mockUser, null);
		});

		assertEquals("The research project is required.", ex.getMessage());
	}

	@Test
	public void testUpdateWithNullAccessRequirementId() {
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setAccessRequirementId(null);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.update(mockUser, toUpdate);
		});

		assertEquals("The accessRequirementId is required.", ex.getMessage());
	}

	@Test
	public void testUpdateWithNullProjectLead() {
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setProjectLead(null);
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.update(mockUser, toUpdate);
		});

		assertEquals("The projectLead must contains more than 0 characters.", ex.getMessage());
	}

	@Test
	public void testUpdateWithNullInstitution() {
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setInstitution(null);
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.update(mockUser, toUpdate);
		});

		assertEquals("The insitution must contains more than 0 characters.", ex.getMessage());
	}

	@Test
	public void testUpdateWithNullIDUWhenRequired() {
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		when(mockAccessRequirement.getIsIDURequired()).thenReturn(true);
		
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setIntendedDataUseStatement(null);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.update(mockUser, toUpdate);
		});

		assertEquals("The intended data use statement must contains more than 0 characters.", ex.getMessage());
	}
	
	@Test
	public void testUpdateWithNullIDUWhenOptional() {
		when(mockUser.getId()).thenReturn(Long.valueOf(userId));
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		when(mockAccessRequirement.getIsIDURequired()).thenReturn(false);
		when(mockResearchProjectDao.getForUpdate(anyString())).thenReturn(researchProject);
		
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setIntendedDataUseStatement(null);

		// Call under test
		manager.update(mockUser, toUpdate);
		
		verify(mockResearchProjectDao).update(toUpdate);
	}

	@Test
	public void testUpdateNotFound() {
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		when(mockResearchProjectDao.getForUpdate(anyString())).thenThrow(new NotFoundException());

		ResearchProject toUpdate = createNewResearchProject();
		NotFoundException ex = assertThrows(NotFoundException.class, () -> {			
			// Call under test
			manager.update(mockUser, toUpdate);
		});

		assertEquals("The resource you are attempting to access cannot be found", ex.getMessage());
	}

	@Test
	public void testUpdateCreatedBy() {
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		when(mockResearchProjectDao.getForUpdate(researchProjectId)).thenReturn(researchProject);
		
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setCreatedBy("333");
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.update(mockUser, toUpdate);
		});

		assertEquals("accessRequirementId, createdOn and createdBy fields cannot be edited.", ex.getMessage());
	}

	@Test
	public void testUpdateCreatedOn() {
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(mockAccessRequirement);
		when(mockResearchProjectDao.getForUpdate(researchProjectId)).thenReturn(researchProject);
		
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setCreatedOn(new Date(0L));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.update(mockUser, toUpdate);
		});

		assertEquals("accessRequirementId, createdOn and createdBy fields cannot be edited.", ex.getMessage());
	}

	@Test
	public void testUpdateAccessRequirementId() {
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		when(mockResearchProjectDao.getForUpdate(researchProjectId)).thenReturn(researchProject);
		
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setAccessRequirementId("444");
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.update(mockUser, toUpdate);
		});

		assertEquals("accessRequirementId, createdOn and createdBy fields cannot be edited.", ex.getMessage());
	}

	@Test
	public void testUpdateWithOutdatedEtag() {
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		when(mockResearchProjectDao.getForUpdate(researchProjectId)).thenReturn(researchProject);
		
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setEtag("oldEtag");
		ConflictingUpdateException ex = assertThrows(ConflictingUpdateException.class, () -> {			
			// Call under test
			manager.update(mockUser, toUpdate);
		});

		assertEquals("The resource you are attempting to edit has changed since you last fetched the object", ex.getMessage());
	}

	@Test
	public void testUpdateUnauthorized() {
		when(mockUser.getId()).thenReturn(555L);
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		when(mockResearchProjectDao.getForUpdate(researchProjectId)).thenReturn(researchProject);
				
		ResearchProject toUpdate = createNewResearchProject();
		UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			manager.update(mockUser, toUpdate);
		});

		assertEquals("Only the owner can perform this action.", ex.getMessage());
	}

	@Test
	public void testUpdate() {
		when(mockUser.getId()).thenReturn(Long.valueOf(userId));
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(mockAccessRequirement);
		when(mockResearchProjectDao.getForUpdate(anyString())).thenReturn(researchProject);
		when(mockResearchProjectDao.update(any())).thenReturn(researchProject);
		
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setIntendedDataUseStatement("new intendedDataUseStatement");
		
		// Call under test
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

	@Test
	public void testCreateOrUpdateWithNullResearchProject() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.createOrUpdate(mockUser, null);
		});

		assertEquals("The research project is required.", ex.getMessage());
	}

	@Test
	public void testCreateOrUpdateWithNullId() {
		when(mockUser.getId()).thenReturn(Long.valueOf(userId));
		when(mockResearchProjectDao.create(any(ResearchProject.class))).thenReturn(researchProject);
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(mockAccessRequirement);
		
		researchProject.setId(null);
		
		// Call under test
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
		when(mockUser.getId()).thenReturn(Long.valueOf(userId));
		when(mockAccessRequirementDao.get(anyString())).thenReturn(mockAccessRequirement);
		when(mockResearchProjectDao.getForUpdate(anyString())).thenReturn(researchProject);
		when(mockResearchProjectDao.update(any())).thenReturn(researchProject);
		
		ResearchProject toUpdate = createNewResearchProject();
		toUpdate.setIntendedDataUseStatement("new intendedDataUseStatement");
		
		// Call under test
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
