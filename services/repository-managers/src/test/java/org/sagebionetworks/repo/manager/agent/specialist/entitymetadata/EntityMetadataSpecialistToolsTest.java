package org.sagebionetworks.repo.manager.agent.specialist.entitymetadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager.PushFileRequest;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager.PushFileResult;
import org.sagebionetworks.repo.manager.agent.specialist.ToolResponse;
import org.sagebionetworks.repo.manager.agent.specialist.entitymetadata.EntityMetadataSpecialistTools.FileToAdd;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.SessionFileMetadata;
import org.sagebionetworks.repo.model.agent.SessionFileMetadataBatch;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.service.EntityService;
import org.springframework.ai.chat.model.ToolContext;

@ExtendWith(MockitoExtension.class)
public class EntityMetadataSpecialistToolsTest {

	@Mock
	private EntityService mockEntityService;

	@Mock
	private CodeInterpreterFileManager mockCodeInterpreterFileManager;

	private EntityMetadataSpecialistTools tools;
	private UserInfo userInfo;
	private ToolContext toolContext;
	private ToolContext toolContextWithSession;

	@BeforeEach
	public void setup() {
		tools = new EntityMetadataSpecialistTools(mockEntityService, mockCodeInterpreterFileManager);
		userInfo = new UserInfo(false, 101L);
		toolContext = new ToolContext(Map.of("userInfo", userInfo));
		toolContextWithSession = new ToolContext(Map.of("userInfo", userInfo, "sessionId", "session-123"));
	}

	@Test
	public void testGetEntityDetailsWithValidId() {
		Project project = new Project();
		project.setId("syn123");
		project.setName("MyProject");
		when(mockEntityService.getEntity(userInfo.getId(), "syn123")).thenReturn(project);

		// call under test
		ToolResponse<Entity> response = tools.getEntityDetails("syn123", toolContext);

		assertNull(response.getErrorMessage());
		assertEquals(project, response.getResponseBody());
		verify(mockEntityService).getEntity(userInfo.getId(), "syn123");
	}

	@Test
	public void testGetEntityDetailsWithVersion() {
		Folder folder = new Folder();
		folder.setId("syn456");
		folder.setName("VersionedFolder");
		when(mockEntityService.getEntityForVersion(userInfo.getId(), "syn456", 3L)).thenReturn(folder);

		// call under test
		ToolResponse<Entity> response = tools.getEntityDetails("syn456.3", toolContext);

		assertNull(response.getErrorMessage());
		assertEquals(folder, response.getResponseBody());
		verify(mockEntityService).getEntityForVersion(userInfo.getId(), "syn456", 3L);
	}

	@Test
	public void testGetEntityDetailsWithNoUserInfo() {
		ToolContext noUserContext = new ToolContext(Map.of());

		// call under test
		ToolResponse<Entity> response = tools.getEntityDetails("syn123", noUserContext);

		assertNull(response.getResponseBody());
		assertEquals("No user context available", response.getErrorMessage());
		verifyNoInteractions(mockEntityService);
	}

	@Test
	public void testGetEntityDetailsWithInvalidId() {
		// call under test
		ToolResponse<Entity> response = tools.getEntityDetails("not_a_valid_id!", toolContext);

		assertNull(response.getResponseBody());
		assertNotNull(response.getErrorMessage());
		verifyNoInteractions(mockEntityService);
	}

	@Test
	public void testGetEntityDetailsWithException() {
		when(mockEntityService.getEntity(userInfo.getId(), "syn123")).thenThrow(new RuntimeException("Not found"));

		// call under test
		ToolResponse<Entity> response = tools.getEntityDetails("syn123", toolContext);

		assertNull(response.getResponseBody());
		assertEquals("Error getting details for entity 'syn123': Not found", response.getErrorMessage());
	}

	@Test
	public void testGetAnnotationsWithValidId() {
		Annotations annotations = new Annotations().setId("syn123").setEtag("etag-1");
		AnnotationsV2TestUtils.putAnnotations(annotations, "sample", "SAMPLE_1", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "count", "42", AnnotationsValueType.LONG);
		when(mockEntityService.getEntityAnnotations(userInfo.getId(), "syn123", true)).thenReturn(annotations);

		// call under test
		ToolResponse<Annotations> response = tools.getAnnotations("syn123", toolContext);

		assertNull(response.getErrorMessage());
		assertEquals(annotations, response.getResponseBody());
		verify(mockEntityService).getEntityAnnotations(userInfo.getId(), "syn123", true);
	}

	@Test
	public void testGetAnnotationsWithNoUserInfo() {
		ToolContext noUserContext = new ToolContext(Map.of());

		// call under test
		ToolResponse<Annotations> response = tools.getAnnotations("syn123", noUserContext);

		assertNull(response.getResponseBody());
		assertEquals("No user context available", response.getErrorMessage());
		verifyNoInteractions(mockEntityService);
	}

	@Test
	public void testGetAnnotationsWithException() {
		when(mockEntityService.getEntityAnnotations(userInfo.getId(), "syn123", true))
				.thenThrow(new RuntimeException("Access denied"));

		// call under test
		ToolResponse<Annotations> response = tools.getAnnotations("syn123", toolContext);

		assertNull(response.getResponseBody());
		assertEquals("Error getting annotations for entity 'syn123': Access denied", response.getErrorMessage());
	}

	@Test
	public void testGetSchemaBindingWithValidId() {
		JsonSchemaObjectBinding binding = new JsonSchemaObjectBinding();
		binding.setObjectId(123L);
		when(mockEntityService.getBoundSchema(userInfo.getId(), "syn123")).thenReturn(binding);

		// call under test
		ToolResponse<JsonSchemaObjectBinding> response = tools.getSchemaBinding("syn123", toolContext);

		assertNull(response.getErrorMessage());
		assertEquals(binding, response.getResponseBody());
		verify(mockEntityService).getBoundSchema(userInfo.getId(), "syn123");
	}

	@Test
	public void testGetSchemaBindingWithNoUserInfo() {
		ToolContext noUserContext = new ToolContext(Map.of());

		// call under test
		ToolResponse<JsonSchemaObjectBinding> response = tools.getSchemaBinding("syn123", noUserContext);

		assertNull(response.getResponseBody());
		assertEquals("No user context available", response.getErrorMessage());
		verifyNoInteractions(mockEntityService);
	}

	@Test
	public void testGetChildrenWithValidId() {
		EntityChildrenResponse children = new EntityChildrenResponse().setNextPageToken("next-token");
		when(mockEntityService.getChildren(eq(userInfo.getId()), any(EntityChildrenRequest.class))).thenReturn(children);

		// call under test
		ToolResponse<EntityChildrenResponse> response = tools.getChildren("syn123", null, toolContext);

		assertNull(response.getErrorMessage());
		assertEquals(children, response.getResponseBody());

		ArgumentCaptor<EntityChildrenRequest> requestCaptor = ArgumentCaptor.forClass(EntityChildrenRequest.class);
		verify(mockEntityService).getChildren(eq(userInfo.getId()), requestCaptor.capture());
		assertEquals("syn123", requestCaptor.getValue().getParentId());
	}

	@Test
	public void testGetChildrenWithNextPageToken() {
		EntityChildrenResponse children = new EntityChildrenResponse();
		when(mockEntityService.getChildren(eq(userInfo.getId()), any(EntityChildrenRequest.class))).thenReturn(children);

		// call under test
		tools.getChildren("syn123", "page-2", toolContext);

		ArgumentCaptor<EntityChildrenRequest> requestCaptor = ArgumentCaptor.forClass(EntityChildrenRequest.class);
		verify(mockEntityService).getChildren(eq(userInfo.getId()), requestCaptor.capture());
		assertEquals("page-2", requestCaptor.getValue().getNextPageToken());
	}

	@Test
	public void testGetChildrenWithNoUserInfo() {
		ToolContext noUserContext = new ToolContext(Map.of());

		// call under test
		ToolResponse<EntityChildrenResponse> response = tools.getChildren("syn123", null, noUserContext);

		assertNull(response.getResponseBody());
		assertEquals("No user context available", response.getErrorMessage());
		verifyNoInteractions(mockEntityService);
	}

	@Test
	public void testAddFilesToSessionWithSuccess() {
		FileHandleAssociation a1 = new FileHandleAssociation().setFileHandleId("222")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId("syn123");
		FileHandleAssociation a2 = new FileHandleAssociation().setFileHandleId("333")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId("syn456");

		PushFileResult result1 = new PushFileResult(new PushFileRequest(a1, "meta/a.csv"), null, null);
		PushFileResult result2 = new PushFileResult(new PushFileRequest(a2, "meta/b.csv"), null, null);
		when(mockCodeInterpreterFileManager.pushFileHandlesToSession(eq(userInfo), any(), eq("session-123")))
				.thenReturn(List.of(result1, result2));

		List<FileToAdd> files = List.of(new FileToAdd(a1, "meta/a.csv"), new FileToAdd(a2, "meta/b.csv"));

		// call under test
		String response = tools.addFilesToSession(files, toolContextWithSession);

		assertTrue(response.contains("Added 'syn123' at 'meta/a.csv'"), "Got: " + response);
		assertTrue(response.contains("Added 'syn456' at 'meta/b.csv'"), "Got: " + response);

		// The associations are forwarded to the batch push unchanged, paired with their session paths.
		ArgumentCaptor<List<PushFileRequest>> pushCaptor = ArgumentCaptor.forClass(List.class);
		verify(mockCodeInterpreterFileManager).pushFileHandlesToSession(eq(userInfo), pushCaptor.capture(), eq("session-123"));
		List<PushFileRequest> pushRequests = pushCaptor.getValue();
		assertEquals(2, pushRequests.size());
		assertEquals(a1, pushRequests.get(0).association());
		assertEquals("meta/a.csv", pushRequests.get(0).sessionPath());
		assertEquals(a2, pushRequests.get(1).association());
		// The tool does not resolve entities; that is done by getFilesMetadata.
		verifyNoInteractions(mockEntityService);
	}

	@Test
	public void testAddFilesToSessionWithFailure() {
		FileHandleAssociation association = new FileHandleAssociation().setFileHandleId("222")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId("syn123");
		PushFileResult failure = new PushFileResult(new PushFileRequest(association, "meta/a.csv"), null,
				"You do not have permission to download this file.");
		when(mockCodeInterpreterFileManager.pushFileHandlesToSession(eq(userInfo), any(), eq("session-123")))
				.thenReturn(List.of(failure));

		// call under test
		String response = tools.addFilesToSession(List.of(new FileToAdd(association, "meta/a.csv")), toolContextWithSession);

		assertTrue(response.contains("Failed to add 'syn123' at 'meta/a.csv': You do not have permission to download this file."),
				"Got: " + response);
	}

	@Test
	public void testAddFilesToSessionWithNoUserInfo() {
		ToolContext noUserContext = new ToolContext(Map.of("sessionId", "session-123"));
		FileHandleAssociation association = new FileHandleAssociation().setFileHandleId("222")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId("syn123");

		// call under test
		String response = tools.addFilesToSession(List.of(new FileToAdd(association, "meta/a.csv")), noUserContext);

		assertEquals("Error: No user context available", response);
		verifyNoInteractions(mockCodeInterpreterFileManager);
		verifyNoInteractions(mockEntityService);
	}

	@Test
	public void testAddFilesToSessionWithNoSessionId() {
		FileHandleAssociation association = new FileHandleAssociation().setFileHandleId("222")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId("syn123");

		// call under test
		String response = tools.addFilesToSession(List.of(new FileToAdd(association, "meta/a.csv")), toolContext);

		assertEquals("Error: No code interpreter session ID available", response);
		verifyNoInteractions(mockCodeInterpreterFileManager);
		verifyNoInteractions(mockEntityService);
	}

	@Test
	public void testAddFilesToSessionWithEmptyFiles() {
		// call under test
		String response = tools.addFilesToSession(List.of(), toolContextWithSession);

		assertEquals("Error: No files were provided to add to the session", response);
		verifyNoInteractions(mockCodeInterpreterFileManager);
		verifyNoInteractions(mockEntityService);
	}

	@Test
	public void testGetFilesMetadataWithFileEntityAndRecordSet() {
		// A FileEntity and a RecordSet both resolve to a FileEntity association from their dataFileHandleId.
		when(mockEntityService.getEntity(userInfo.getId(), "syn123"))
				.thenReturn(new FileEntity().setId("syn123").setDataFileHandleId("222"));
		when(mockEntityService.getEntityForVersion(userInfo.getId(), "syn456", 2L))
				.thenReturn(new RecordSet().setId("syn456").setDataFileHandleId("333"));

		SessionFileMetadata m1 = new SessionFileMetadata().setCanDownload(true).setCanAddToSession(true)
				.setContentType("text/csv").setContentSizeBytes(100L);
		SessionFileMetadata m2 = new SessionFileMetadata().setCanDownload(true).setCanAddToSession(true)
				.setContentType("application/json").setContentSizeBytes(50L);
		when(mockCodeInterpreterFileManager.getFileMetadataBatch(eq(userInfo), any()))
				.thenReturn(List.of(m1, m2));

		// call under test
		ToolResponse<SessionFileMetadataBatch> response = tools.getFilesMetadata(List.of("syn123", "syn456.2"), toolContext);

		assertNull(response.getErrorMessage());
		List<SessionFileMetadata> results = response.getResponseBody().getResults();
		assertEquals(2, results.size());
		// Each result carries the originating entity id.
		assertEquals("syn123", results.get(0).getEntityId());
		assertEquals("syn456.2", results.get(1).getEntityId());

		// Both entities resolve to FileEntity associations built from their dataFileHandleId.
		ArgumentCaptor<List<FileHandleAssociation>> captor = ArgumentCaptor.forClass(List.class);
		verify(mockCodeInterpreterFileManager).getFileMetadataBatch(eq(userInfo), captor.capture());
		List<FileHandleAssociation> associations = captor.getValue();
		assertEquals(2, associations.size());
		assertEquals("222", associations.get(0).getFileHandleId());
		assertEquals(FileHandleAssociateType.FileEntity, associations.get(0).getAssociateObjectType());
		assertEquals("syn123", associations.get(0).getAssociateObjectId());
		assertEquals("333", associations.get(1).getFileHandleId());
		assertEquals(FileHandleAssociateType.FileEntity, associations.get(1).getAssociateObjectType());
		assertEquals("syn456", associations.get(1).getAssociateObjectId());
	}

	@Test
	public void testGetFilesMetadataWithNonFileEntity() {
		// A Project is neither a FileEntity nor a RecordSet: it is reported as ineligible without a batch call.
		when(mockEntityService.getEntity(userInfo.getId(), "syn123")).thenReturn(new Project().setId("syn123"));

		// call under test
		ToolResponse<SessionFileMetadataBatch> response = tools.getFilesMetadata(List.of("syn123"), toolContext);

		assertNull(response.getErrorMessage());
		List<SessionFileMetadata> results = response.getResponseBody().getResults();
		assertEquals(1, results.size());
		assertEquals("syn123", results.get(0).getEntityId());
		assertFalse(results.get(0).getCanAddToSession());
		assertTrue(results.get(0).getReason().contains("not a FileEntity or RecordSet"), "Got: " + results.get(0).getReason());
		verifyNoInteractions(mockCodeInterpreterFileManager);
	}

	@Test
	public void testGetFilesMetadataWithMixOfFileAndNonFileEntity() {
		// syn123 is a FileEntity (described via the batch); syn999 is a Project (reported locally as ineligible).
		when(mockEntityService.getEntity(userInfo.getId(), "syn123"))
				.thenReturn(new FileEntity().setId("syn123").setDataFileHandleId("222"));
		when(mockEntityService.getEntity(userInfo.getId(), "syn999")).thenReturn(new Project().setId("syn999"));

		SessionFileMetadata m1 = new SessionFileMetadata().setCanDownload(true).setCanAddToSession(true);
		when(mockCodeInterpreterFileManager.getFileMetadataBatch(eq(userInfo), any())).thenReturn(List.of(m1));

		// call under test
		ToolResponse<SessionFileMetadataBatch> response = tools.getFilesMetadata(List.of("syn123", "syn999"), toolContext);

		List<SessionFileMetadata> results = response.getResponseBody().getResults();
		assertEquals(2, results.size());
		assertEquals("syn123", results.get(0).getEntityId());
		assertTrue(results.get(0).getCanAddToSession());
		assertEquals("syn999", results.get(1).getEntityId());
		assertFalse(results.get(1).getCanAddToSession());

		// Only the valid file is forwarded to the batch metadata call.
		ArgumentCaptor<List<FileHandleAssociation>> captor = ArgumentCaptor.forClass(List.class);
		verify(mockCodeInterpreterFileManager).getFileMetadataBatch(eq(userInfo), captor.capture());
		assertEquals(1, captor.getValue().size());
		assertEquals("222", captor.getValue().get(0).getFileHandleId());
	}

	@Test
	public void testGetFilesMetadataWithNoUserInfo() {
		ToolContext noUserContext = new ToolContext(Map.of());

		// call under test
		ToolResponse<SessionFileMetadataBatch> response = tools.getFilesMetadata(List.of("syn123"), noUserContext);

		assertNull(response.getResponseBody());
		assertEquals("No user context available", response.getErrorMessage());
		verifyNoInteractions(mockEntityService);
		verifyNoInteractions(mockCodeInterpreterFileManager);
	}

	@Test
	public void testGetFilesMetadataWithEmptyIds() {
		// call under test
		ToolResponse<SessionFileMetadataBatch> response = tools.getFilesMetadata(List.of(), toolContext);

		assertNull(response.getResponseBody());
		assertEquals("No entity IDs were provided", response.getErrorMessage());
		verifyNoInteractions(mockEntityService);
		verifyNoInteractions(mockCodeInterpreterFileManager);
	}
}
