package org.sagebionetworks.repo.manager.agent.specialist.entitymetadata;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager.PushFileRequest;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager.PushFileResult;
import org.sagebionetworks.repo.manager.agent.specialist.JSONEntityResultConverter;
import org.sagebionetworks.repo.manager.agent.specialist.ToolResponse;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.SessionFileMetadata;
import org.sagebionetworks.repo.model.agent.SessionFileMetadataBatch;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.service.EntityService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Tools available to the entity metadata specialist. They retrieve and interpret entity metadata,
 * annotations, and schema information on behalf of the calling user. Reads go through
 * {@link EntityService} (the service layer, which enriches entities with type-specific metadata),
 * and every method operates as the user carried in the {@link ToolContext} so the user's
 * permissions are always respected.
 */
@Service
public class EntityMetadataSpecialistTools {

	private final EntityService entityService;
	private final CodeInterpreterFileManager codeInterpreterFileManager;

	public EntityMetadataSpecialistTools(EntityService entityService,
			CodeInterpreterFileManager codeInterpreterFileManager) {
		this.entityService = entityService;
		this.codeInterpreterFileManager = codeInterpreterFileManager;
	}

	@Tool(description = "Get the details of a Synapse entity including its type, name, parent, and creation "
			+ "information. Use this to answer questions about what an entity is and where it lives.",
			resultConverter = JSONEntityResultConverter.class)
	public ToolResponse<Entity> getEntityDetails(
			@ToolParam(description = "A Synapse entity ID such as 'syn123' or 'syn123.5' for a specific version", required = true) String entityId,
			ToolContext toolContext) {
		UserInfo userInfo = extractUserInfo(toolContext);
		if (userInfo == null) {
			return new ToolResponse<>("No user context available");
		}
		try {
			IdAndVersion idAndVersion = IdAndVersion.parse(entityId);
			String synId = "syn" + idAndVersion.getId();
			Entity entity;
			if (idAndVersion.getVersion().isPresent()) {
				entity = entityService.getEntityForVersion(userInfo.getId(), synId, idAndVersion.getVersion().get());
			} else {
				entity = entityService.getEntity(userInfo.getId(), synId);
			}
			return new ToolResponse<>(entity);
		} catch (Exception e) {
			return new ToolResponse<>("Error getting details for entity '" + entityId + "': " + e.getMessage());
		}
	}

	@Tool(description = "Get all annotations for a Synapse entity, including annotations derived from a bound "
			+ "JSON schema. Use this to answer questions such as 'What annotations does syn123 have?'.",
			resultConverter = JSONEntityResultConverter.class)
	public ToolResponse<Annotations> getAnnotations(
			@ToolParam(description = "A Synapse entity ID such as 'syn123'", required = true) String entityId,
			ToolContext toolContext) {
		UserInfo userInfo = extractUserInfo(toolContext);
		if (userInfo == null) {
			return new ToolResponse<>("No user context available");
		}
		try {
			String synId = "syn" + IdAndVersion.parse(entityId).getId();
			Annotations annotations = entityService.getEntityAnnotations(userInfo.getId(), synId, true);
			return new ToolResponse<>(annotations);
		} catch (Exception e) {
			return new ToolResponse<>("Error getting annotations for entity '" + entityId + "': " + e.getMessage());
		}
	}

	@Tool(description = "Get the JSON schema binding for a Synapse entity, if one is bound. The binding "
			+ "identifies the schema $id that validates the entity and whether derived annotations are enabled.",
			resultConverter = JSONEntityResultConverter.class)
	public ToolResponse<JsonSchemaObjectBinding> getSchemaBinding(
			@ToolParam(description = "A Synapse entity ID such as 'syn123'", required = true) String entityId,
			ToolContext toolContext) {
		UserInfo userInfo = extractUserInfo(toolContext);
		if (userInfo == null) {
			return new ToolResponse<>("No user context available");
		}
		try {
			String synId = "syn" + IdAndVersion.parse(entityId).getId();
			JsonSchemaObjectBinding binding = entityService.getBoundSchema(userInfo.getId(), synId);
			return new ToolResponse<>(binding);
		} catch (Exception e) {
			return new ToolResponse<>("Error getting schema binding for entity '" + entityId + "': " + e.getMessage());
		}
	}

	@Tool(description = "List the child entities of a Synapse container (Project or Folder). Results are paged; "
			+ "if the response includes a nextPageToken, pass it back to retrieve the next page.",
			resultConverter = JSONEntityResultConverter.class)
	public ToolResponse<EntityChildrenResponse> getChildren(
			@ToolParam(description = "A Synapse entity ID of the parent container such as 'syn123'", required = true) String entityId,
			@ToolParam(description = "A page token from a previous response to get the next page of children", required = false) String nextPageToken,
			ToolContext toolContext) {
		UserInfo userInfo = extractUserInfo(toolContext);
		if (userInfo == null) {
			return new ToolResponse<>("No user context available");
		}
		try {
			String synId = "syn" + IdAndVersion.parse(entityId).getId();
			EntityChildrenResponse children = entityService.getChildren(userInfo.getId(), new EntityChildrenRequest()
					.setParentId(synId)
					.setIncludeTypes(List.of(EntityType.values()))
					.setIncludeTotalChildCount(true)
					.setSortBy(SortBy.MODIFIED_ON)
					.setSortDirection(Direction.DESC)
					.setNextPageToken(nextPageToken));
			return new ToolResponse<>(children);
		} catch (Exception e) {
			return new ToolResponse<>("Error getting children for entity '" + entityId + "': " + e.getMessage());
		}
	}

	@Tool(description = "Report the size, content type, and code-interpreter-session eligibility of one or more "
			+ "Synapse files (each a FileEntity or RecordSet) WITHOUT adding them to the session. Use this to "
			+ "decide whether to add or skip a file, and to explain why a file cannot be added: the user may lack "
			+ "download permission, the file may be too large (limit 100 MB), or its type may not be supported "
			+ "(allowed: PDF, CSV, TXT, JSON). Size and content type are only available when the user can download "
			+ "the file. Each result includes a fileHandleAssociation; pass the associations of the files you want "
			+ "to add to addFilesToSession.",
			resultConverter = JSONEntityResultConverter.class)
	public ToolResponse<SessionFileMetadataBatch> getFilesMetadata(
			@ToolParam(description = "The FileEntity or RecordSet IDs to describe (e.g. 'syn123' or 'syn123.5')", required = true) List<String> entityIds,
			ToolContext toolContext) {
		UserInfo userInfo = extractUserInfo(toolContext);
		if (userInfo == null) {
			return new ToolResponse<>("No user context available");
		}
		if (entityIds == null || entityIds.isEmpty()) {
			return new ToolResponse<>("No entity IDs were provided");
		}
		try {
			// One result per input entity, kept in input order. Entities that are not FileEntities or
			// RecordSets are reported here; the rest are described as a batch (which enforces download
			// authorization and reads size/type from the file handle).
			SessionFileMetadata[] metadata = new SessionFileMetadata[entityIds.size()];
			List<FileHandleAssociation> associations = new ArrayList<>(entityIds.size());
			List<Integer> toEntityIndex = new ArrayList<>(entityIds.size());

			for (int i = 0; i < entityIds.size(); i++) {
				String entityId = entityIds.get(i);
				try {
					associations.add(resolveFileAssociation(userInfo, entityId));
					toEntityIndex.add(i);
				} catch (UnsupportedFileEntityException e) {
					metadata[i] = new SessionFileMetadata().setEntityId(entityId).setCanDownload(false)
							.setCanAddToSession(false).setReason(e.getMessage());
				}
			}

			if (!associations.isEmpty()) {
				List<SessionFileMetadata> resolved = codeInterpreterFileManager.getFileMetadataBatch(userInfo,
						associations);
				for (int j = 0; j < toEntityIndex.size(); j++) {
					metadata[toEntityIndex.get(j)] = resolved.get(j).setEntityId(entityIds.get(toEntityIndex.get(j)));
				}
			}

			return new ToolResponse<>(new SessionFileMetadataBatch().setResults(List.of(metadata)));
		} catch (Exception e) {
			return new ToolResponse<>("Error getting file metadata: " + e.getMessage());
		}
	}

	@Tool(description = "Copy one or more Synapse files into the code interpreter session so they can be inspected "
			+ "or processed. Identify each file by the fileHandleAssociation obtained from getFilesMetadata. Each "
			+ "file must be downloadable by the user, of a supported type (PDF, CSV, TXT, JSON), and no larger than "
			+ "100 MB; files that fail these checks are reported as failures rather than added. Returns a per-file "
			+ "report of which files were added and which could not be, with the reason.")
	public String addFilesToSession(
			@ToolParam(description = "The files to add, each pairing a fileHandleAssociation (from getFilesMetadata) "
					+ "with the session path where it should appear (e.g. 'entity_metadata_specialist/data.csv')", required = true) List<FileToAdd> files,
			ToolContext toolContext) {
		UserInfo userInfo = extractUserInfo(toolContext);
		if (userInfo == null) {
			return "Error: No user context available";
		}
		String sessionId = extractSessionId(toolContext);
		if (sessionId == null) {
			return "Error: No code interpreter session ID available";
		}
		if (files == null || files.isEmpty()) {
			return "Error: No files were provided to add to the session";
		}
		try {
			List<PushFileRequest> pushRequests = new ArrayList<>(files.size());
			for (FileToAdd file : files) {
				pushRequests.add(new PushFileRequest(file.fileHandleAssociation(), file.sessionPath()));
			}

			// The file manager enforces download authorization, the type whitelist, and the size limit
			// per-file; results line up positionally with the requests.
			List<PushFileResult> results = codeInterpreterFileManager.pushFileHandlesToSession(userInfo, pushRequests,
					sessionId);
			StringBuilder report = new StringBuilder();
			for (int i = 0; i < files.size(); i++) {
				FileToAdd file = files.get(i);
				String label = "'" + file.fileHandleAssociation().getAssociateObjectId() + "' at '" + file.sessionPath() + "'";
				PushFileResult result = results.get(i);
				report.append(result.isError() ? "Failed to add " + label + ": " + result.error()
						: "Added " + label).append("\n");
			}
			return report.toString().trim();
		} catch (Exception e) {
			return "Error adding files to session: " + e.getMessage();
		}
	}

	/**
	 * Resolves a FileEntity or RecordSet ID to a FileHandleAssociation for its content. RecordSet
	 * extends FileEntity and its CSV is exposed through the FileEntity association type, so both use
	 * {@link FileHandleAssociateType#FileEntity}. Loading through {@link EntityService} enforces READ as
	 * the calling user; download authorization is enforced later when the file handle is resolved.
	 *
	 * @throws UnsupportedFileEntityException if the entity is not a FileEntity or RecordSet.
	 */
	private FileHandleAssociation resolveFileAssociation(UserInfo userInfo, String entityId) {
		IdAndVersion idAndVersion = IdAndVersion.parse(entityId);
		String synId = "syn" + idAndVersion.getId();
		Entity entity;
		if (idAndVersion.getVersion().isPresent()) {
			entity = entityService.getEntityForVersion(userInfo.getId(), synId, idAndVersion.getVersion().get());
		} else {
			entity = entityService.getEntity(userInfo.getId(), synId);
		}
		// RecordSet extends FileEntity, so this admits both; a RecordSet's CSV is referenced through the
		// FileEntity association type using the RecordSet's own id as the associated object id.
		if (!(entity instanceof FileEntity fileEntity)) {
			throw new UnsupportedFileEntityException("Entity '" + entityId + "' is not a FileEntity or RecordSet.");
		}
		return new FileHandleAssociation()
				.setFileHandleId(fileEntity.getDataFileHandleId())
				.setAssociateObjectType(FileHandleAssociateType.FileEntity)
				.setAssociateObjectId(synId);
	}

	/** Signals that an entity is not a FileEntity or RecordSet and so has no addable file content. */
	private static class UnsupportedFileEntityException extends RuntimeException {
		UnsupportedFileEntityException(String message) {
			super(message);
		}
	}

	/**
	 * A single file to add to the code interpreter session.
	 *
	 * @param fileHandleAssociation The association identifying the file's content, as returned by
	 *                              getFilesMetadata
	 * @param sessionPath           The path where the file should appear in the session filesystem
	 */
	public record FileToAdd(FileHandleAssociation fileHandleAssociation, String sessionPath) {}

	private UserInfo extractUserInfo(ToolContext toolContext) {
		return (UserInfo) toolContext.getContext().get("userInfo");
	}

	private String extractSessionId(ToolContext toolContext) {
		return (String) toolContext.getContext().get("sessionId");
	}

}
