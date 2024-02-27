package org.sagebionetworks.repo.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandleUrlRequest;
import org.sagebionetworks.repo.manager.sts.StsManager;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.download.v2.FileActionRequired;
import org.sagebionetworks.repo.model.download.ActionRequiredList;
import org.sagebionetworks.repo.model.download.EnableTwoFa;
import org.sagebionetworks.repo.model.download.RequestDownload;
import org.sagebionetworks.repo.model.sts.StsCredentials;
import org.sagebionetworks.repo.model.sts.StsPermission;
import org.sagebionetworks.repo.model.table.DefiningSqlEntityType;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.repo.model.table.ValidateDefiningSqlRequest;
import org.sagebionetworks.repo.model.table.ValidateDefiningSqlResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.metadata.AllTypesValidator;
import org.sagebionetworks.repo.web.service.metadata.MetadataProviderFactory;
import org.sagebionetworks.repo.web.service.metadata.TypeSpecificCreateProvider;
import org.sagebionetworks.repo.web.service.metadata.TypeSpecificDefiningSqlProvider;
import org.sagebionetworks.repo.web.service.metadata.TypeSpecificUpdateProvider;

@ExtendWith(MockitoExtension.class)
public class EntityServiceImplUnitTest {
	private static final String ENTITY_ID = "syn123";

	@InjectMocks
	EntityServiceImpl entityService;
	@Mock
	EntityManager mockEntityManager;
	@Mock
	HttpServletRequest mockRequest;
	@Mock
	StsManager mockStsManager;
	@Mock
	UserManager mockUserManager;
	@Mock
	FileHandleManager mockFileHandleManager;
	@Mock
	AllTypesValidator mockAllTypesValidator;
	@Mock
	MetadataProviderFactory mockMetadataProviderFactory;
	@Mock
	TypeSpecificUpdateProvider<Project> mockProjectUpdateProvider;
	@Mock
	TypeSpecificCreateProvider<Project> mockProjectCreateProvider;
	@Mock
	TypeSpecificDefiningSqlProvider<MaterializedView> mockMaterializedViewDefiningSqlProvider;
	@Mock
	EntityAuthorizationManager mockAuthManager;

	static final Long PRINCIPAL_ID = 101L;
	UserInfo userInfo = null;

	Project project;

	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false);
		userInfo.setId(PRINCIPAL_ID);

		project = new Project();
		project.setId("syn123");
	}

	@Test
	public void testGetFileRedirectURLForCurrentVersion() {
		String entityId = "999";
		String fileHandleId = "111";
		when(mockUserManager.getUserInfo(PRINCIPAL_ID)).thenReturn(userInfo);
		when(mockEntityManager.getFileHandleIdForVersion(userInfo, entityId, null))
				.thenReturn(fileHandleId);
		String url = "http://foo.bar";
		when(mockFileHandleManager.getRedirectURLForFileHandle(any(FileHandleUrlRequest.class))).thenReturn(url);
		assertEquals(url, entityService.getFileRedirectURLForCurrentVersion(PRINCIPAL_ID, entityId));
	}

	@Test
	public void testGetFileRedirectURLForVersion() {
		String entityId = "999";
		String fileHandleId = "111";
		Long version = 1L;
		when(mockUserManager.getUserInfo(PRINCIPAL_ID)).thenReturn(userInfo);
		when(mockEntityManager.getFileHandleIdForVersion(userInfo, entityId, version)).thenReturn(fileHandleId);
		String url = "http://foo.bar";
		when(mockFileHandleManager.getRedirectURLForFileHandle(any(FileHandleUrlRequest.class))).thenReturn(url);
		assertEquals(url, entityService.getFileRedirectURLForVersion(PRINCIPAL_ID, entityId, version));
	}

	@Test
	public void testFireCreate() {
		when(mockUserManager.getUserInfo(PRINCIPAL_ID)).thenReturn(userInfo);
		when(mockMetadataProviderFactory.getMetadataProvider(EntityType.project)).thenReturn(Optional.of(mockProjectCreateProvider));
		// Call under test.
		entityService.createEntity(userInfo.getId(), project, null);
		verify(mockProjectCreateProvider).entityCreated(userInfo, project);
		verify(mockProjectUpdateProvider, never()).entityUpdated(any(UserInfo.class), any(Project.class), anyBoolean());
	}

	@Test
	public void testUpdateEntity_NullId() {
		project.setId(null);

		// Method under test.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> entityService.updateEntity(userInfo.getId(), project,
				false, null));
		assertEquals("Updated Entity cannot have a null id", ex.getMessage());
		verifyZeroInteractions(mockEntityManager);
	}

	@Test
	public void testFireUpdate() {
		boolean newVersion = true;
		when(mockUserManager.getUserInfo(PRINCIPAL_ID)).thenReturn(userInfo);
		when(mockMetadataProviderFactory.getMetadataProvider(EntityType.project)).thenReturn(Optional.of(mockProjectUpdateProvider));
		when(mockEntityManager.updateEntity(userInfo, project, newVersion, null)).thenReturn(newVersion);
		// Call under test.
		entityService.updateEntity(userInfo.getId(), project, newVersion, null);
		verify(mockProjectCreateProvider, never()).entityCreated(any(UserInfo.class), any(Project.class));
		verify(mockProjectUpdateProvider).entityUpdated(userInfo, project, newVersion);
	}

	@Test
	public void testFireUpdateNoNewVersion() {
		boolean newVersion = false;
		when(mockUserManager.getUserInfo(PRINCIPAL_ID)).thenReturn(userInfo);
		when(mockMetadataProviderFactory.getMetadataProvider(EntityType.project)).thenReturn(Optional.of(mockProjectUpdateProvider));
		when(mockEntityManager.updateEntity(userInfo, project, newVersion, null)).thenReturn(newVersion);
		// Call under test.
		entityService.updateEntity(userInfo.getId(), project, newVersion, null);
		verify(mockProjectCreateProvider, never()).entityCreated(any(UserInfo.class), any(Project.class));
		verify(mockProjectUpdateProvider).entityUpdated(userInfo, project, newVersion);
	}

	/**
	 * The new version parameter might be false, but we will still create a new
	 * version under some conditions such as changing a file's handle ID. When a new
	 * new version is created for such cases, the value must be forwarded to the
	 * entity update.
	 */
	@Test
	public void testFireUpdateTriggersNewVersion() {
		boolean newVersionParameter = false;
		final boolean wasNewVersionCreated = true;
		when(mockUserManager.getUserInfo(PRINCIPAL_ID)).thenReturn(userInfo);
		when(mockMetadataProviderFactory.getMetadataProvider(EntityType.project)).thenReturn(Optional.of(mockProjectUpdateProvider));
		when(mockEntityManager.updateEntity(userInfo, project, newVersionParameter, null))
				.thenReturn(wasNewVersionCreated);
		// Call under test.
		entityService.updateEntity(userInfo.getId(), project, newVersionParameter, null);
		verify(mockProjectCreateProvider, never()).entityCreated(any(UserInfo.class), any(Project.class));
		verify(mockProjectUpdateProvider).entityUpdated(userInfo, project, wasNewVersionCreated);
	}

	@Test
	public void getTemporaryCredentialsForEntity() {
		// Mock dependencies.
		when(mockUserManager.getUserInfo(PRINCIPAL_ID)).thenReturn(userInfo);

		StsCredentials managerResult = new StsCredentials();
		when(mockStsManager.getTemporaryCredentials(userInfo, ENTITY_ID, StsPermission.read_only)).thenReturn(
				managerResult);

		// Method under test.
		StsCredentials serviceResult = entityService.getTemporaryCredentialsForEntity(PRINCIPAL_ID, ENTITY_ID,
				StsPermission.read_only);
		assertSame(managerResult, serviceResult);
		verify(mockStsManager).getTemporaryCredentials(userInfo, ENTITY_ID, StsPermission.read_only);
	}

	@Test
	public void testHasAccessNullAccessType() {
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			entityService.hasAccess(ENTITY_ID, PRINCIPAL_ID, null);
		});
		assertEquals("AccessType cannot be null", thrown.getMessage());
	}

	@Test
	public void testHasAccessInvalidAccessType() {
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			entityService.hasAccess(ENTITY_ID, PRINCIPAL_ID, "invalidAccessType");
		});
		assertEquals("No enum constant org.sagebionetworks.repo.model.ACCESS_TYPE.invalidAccessType", thrown.getMessage());
	}
	
	@Test
	public void testGetActionRequiredForDownload() {
		
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mockAuthManager.getActionsRequiredForDownload(any(), any())).thenReturn(List.of(
			new FileActionRequired().withAction(new EnableTwoFa()).withFileId(123),
			new FileActionRequired().withAction(new RequestDownload()).withFileId(123)
		));
		
		ActionRequiredList expected = new ActionRequiredList().setActions(List.of(
			new EnableTwoFa(),
			new RequestDownload()
		));
		
		// Call under test
		ActionRequiredList result = entityService.getActionsRequiredForDownload(PRINCIPAL_ID, ENTITY_ID);
		
		assertEquals(expected, result);
		
		verify(mockAuthManager).getActionsRequiredForDownload(userInfo, List.of(123L));
	}

	@Test
	public void testValidateDefiningSql() {
		ValidateDefiningSqlRequest mockRequest = new ValidateDefiningSqlRequest()
				.setDefiningSql("SELECT * FROM syn123")
				.setEntityType(DefiningSqlEntityType.materializedview);
		ValidateDefiningSqlResponse expected = new ValidateDefiningSqlResponse().setIsValid(true);
	
		
		when(mockMetadataProviderFactory.getMetadataProvider(any())).thenReturn(Optional.of(mockMaterializedViewDefiningSqlProvider));

		// Call under test
		ValidateDefiningSqlResponse response = entityService.validateDefiningSql(mockRequest);

		verify(mockMetadataProviderFactory).getMetadataProvider(EntityType.materializedview);
		verify(mockMaterializedViewDefiningSqlProvider).validateDefiningSql("SELECT * FROM syn123");

		assertEquals(expected, response);
	}

	@Test
	public void testValidateDefiningSqlWithNullProviders() {
		ValidateDefiningSqlRequest request = new ValidateDefiningSqlRequest()
				.setDefiningSql("SELECT * FROM syn123")
				.setEntityType(DefiningSqlEntityType.materializedview);

		when(mockMetadataProviderFactory.getMetadataProvider(any())).thenReturn(Optional.empty());

		String errorMessage = assertThrows(IllegalStateException.class, () -> {
			// Call under test
			entityService.validateDefiningSql(request);
		}).getMessage();

		verify(mockMetadataProviderFactory).getMetadataProvider(EntityType.materializedview);
		verify(mockMaterializedViewDefiningSqlProvider, never()).validateDefiningSql(any());

		assertEquals("No provider found for the given entity type: materializedview", errorMessage);
	}

	@Test 
	public void testValidateDefiningSqlWithInvalidProviders() {
		ValidateDefiningSqlRequest request = new ValidateDefiningSqlRequest()
				.setDefiningSql("SELECT * FROM syn123")
				.setEntityType(DefiningSqlEntityType.materializedview);

		when(mockMetadataProviderFactory.getMetadataProvider(any())).thenReturn(Optional.of(mockProjectCreateProvider));

		String errorMessage = assertThrows(IllegalStateException.class, () -> {
			// call under test
			entityService.validateDefiningSql(request);
		}).getMessage();

		verify(mockMetadataProviderFactory).getMetadataProvider(EntityType.materializedview);
		verify(mockMaterializedViewDefiningSqlProvider, never()).validateDefiningSql(any());

		assertEquals("The given entity has provider that is not of type TypeSpecificDefiningSqlProvider.", errorMessage);
	}

	@Test
	public void testValidateDefiningSqlWithInvalidSql() {
		ValidateDefiningSqlRequest request = new ValidateDefiningSqlRequest()
				.setDefiningSql("this is invalid sql")
				.setEntityType(DefiningSqlEntityType.materializedview);

		when(mockMetadataProviderFactory.getMetadataProvider(any())).thenReturn(Optional.of(mockMaterializedViewDefiningSqlProvider));
		doThrow(new IllegalArgumentException("Encountered \" <regular_identifier> \"invalid\""))
			.when(mockMaterializedViewDefiningSqlProvider).validateDefiningSql(request.getDefiningSql());

		// Call under test
		ValidateDefiningSqlResponse response = entityService.validateDefiningSql(request);

		verify(mockMetadataProviderFactory).getMetadataProvider(EntityType.materializedview);
		verify(mockMaterializedViewDefiningSqlProvider).validateDefiningSql("this is invalid sql");

		assertFalse(response.getIsValid());
		assertEquals("Encountered \" <regular_identifier> \"invalid\"", response.getInvalidReason());
	}
	
	@Test
	public void testValidateDefiningSqlWithNonExistentDependencies() {
		ValidateDefiningSqlRequest request = new ValidateDefiningSqlRequest()
				.setDefiningSql("SELECT * FROM syn192")
				.setEntityType(DefiningSqlEntityType.materializedview);
		
		when(mockMetadataProviderFactory.getMetadataProvider(any())).thenReturn(Optional.of(mockMaterializedViewDefiningSqlProvider));
		doThrow(new NotFoundException("Resource '192' does not exist"))
				.when(mockMaterializedViewDefiningSqlProvider).validateDefiningSql(request.getDefiningSql());
		
		// Call under test
		String errorMessage = assertThrows(NotFoundException.class, () -> {
			entityService.validateDefiningSql(request);	
		}).getMessage();
		
		verify(mockMetadataProviderFactory).getMetadataProvider(EntityType.materializedview);
		verify(mockMaterializedViewDefiningSqlProvider).validateDefiningSql("SELECT * FROM syn192");
		
		assertEquals("Resource '192' does not exist", errorMessage);
	}
	
	@Test
	public void testValidateDefiningSqlWithNullRequest() {
		ValidateDefiningSqlRequest request = null;
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			entityService.validateDefiningSql(request);	
		}).getMessage();
		
		assertEquals("request is required.", errorMessage);
	}
	
	@Test
	public void testValidateDefiningSqlWithNullEntityType() {
		ValidateDefiningSqlRequest request = new ValidateDefiningSqlRequest()
				.setDefiningSql("SELECT * FROM syn123")
				.setEntityType(null);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			entityService.validateDefiningSql(request);	
		}).getMessage();
		
		assertEquals("entityType is required.", errorMessage); 
	}
}
