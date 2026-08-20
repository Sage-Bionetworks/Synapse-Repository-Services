package org.sagebionetworks.repo.manager.curation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.AccessControlListManager;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.CurationTaskProperties;
import org.sagebionetworks.repo.model.curation.DueDateFilter;
import org.sagebionetworks.repo.model.curation.ListCurationTaskRequest;
import org.sagebionetworks.repo.model.curation.ListCurationTaskResponse;
import org.sagebionetworks.repo.model.curation.TaskBundle;
import org.sagebionetworks.repo.model.curation.TaskStatus;
import org.sagebionetworks.repo.model.curation.execution.RecordSetGenerationExecutionProperties;
import org.sagebionetworks.repo.model.curation.execution.SampleSheetGenerationExecutionProperties;
import org.sagebionetworks.repo.model.curation.metadata.FileBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.curation.metadata.GridSupportedTaskProperties;
import org.sagebionetworks.repo.model.curation.metadata.RecordBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.dbo.curation.CurationTaskDao;
import org.sagebionetworks.repo.model.grid.AuthorizationMode;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CurationTaskManagerImpl implements CurationTaskManager {

    private final CurationTaskDao curationTaskDao;
    private final AuthorizationManager authorizationManager;
    private final AccessControlListManager aclManager;
    private final EntityManager entityManager;

    @Autowired
    public CurationTaskManagerImpl(CurationTaskDao curationTaskDao, AuthorizationManager authorizationManager,
            AccessControlListManager aclManager, EntityManager entityManager) {
        this.curationTaskDao = curationTaskDao;
        this.authorizationManager = authorizationManager;
        this.aclManager = aclManager;
        this.entityManager = entityManager;
    }

    @Override
    @WriteTransaction
    public CurationTask createCurationTask(UserInfo userInfo, CurationTask curationTask) {
        validateCurationTask(userInfo, curationTask);

        authorizationManager.canAccess(userInfo, curationTask.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.CREATE).checkAuthorizationOrElseThrow();

        return curationTaskDao.createCurationTask(userInfo.getId(), curationTask);
    }

    @Override
    public CurationTask getCurationTask(UserInfo userInfo, Long taskId) {
        CurationTask task = curationTaskDao.getCurationTask(taskId).orElseThrow(() -> new NotFoundException("Task not found: " + taskId));

        authorizationManager.canAccess(userInfo, task.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();

        return task;
    }

    @Override
    @WriteTransaction
    public CurationTask updateCurationTask(UserInfo userInfo, CurationTask toUpdate) {
        validateCurationTask(userInfo, toUpdate);
        ValidateArgument.required(toUpdate.getTaskId(), "taskId");

        CurationTask existing = getCurationTask(userInfo, toUpdate.getTaskId());

        if (!existing.getProjectId().equals(toUpdate.getProjectId())) {
            throw new IllegalArgumentException("The project for a MetadataTask cannot be changed.");
        }

        authorizationManager.canAccess(userInfo, existing.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE).checkAuthorizationOrElseThrow();

        AuthorizationMode oldMode = getSuggestedAuthorizationMode(existing.getTaskProperties());
        AuthorizationMode newMode = getSuggestedAuthorizationMode(toUpdate.getTaskProperties());

        if (hasAuthorizationModeChanged(oldMode, newMode)) {
            curationTaskDao.clearActiveSessionId(toUpdate.getTaskId());
        }

        return curationTaskDao.updateCurationTask(userInfo.getId(), toUpdate);
    }

    @Override
    @WriteTransaction
    public void deleteCurationTask(UserInfo userInfo, Long taskId) {
        CurationTask existing = getCurationTask(userInfo, taskId);

        authorizationManager.canAccess(userInfo, existing.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.DELETE).checkAuthorizationOrElseThrow();

        curationTaskDao.deleteCurationTask(taskId);
    }

    @Override
    public ListCurationTaskResponse getCurationTasks(UserInfo userInfo, ListCurationTaskRequest request) {
        NextPageToken token = new NextPageToken(request.getNextPageToken());

        ValidateArgument.requirement(
                !(Boolean.TRUE.equals(request.getAssignedToMe()) && request.getAssigneeIds() != null),
                "Cannot specify both 'assignedToMe' and 'assigneeIds'.");

        List<Long> assigneeIds;
        if (Boolean.TRUE.equals(request.getAssignedToMe())) {
            assigneeIds = new ArrayList<>(userInfo.getGroups());
        } else if (request.getAssigneeIds() != null) {
            assigneeIds = request.getAssigneeIds().stream().map(Long::parseLong).collect(Collectors.toList());
        } else {
            assigneeIds = null;
        }

        Date dueDateStart = null;
        Date dueDateEnd = null;
        boolean includeUnsetDueDate = false;
        DueDateFilter dueDateFilter = request.getDueDate();
        if (dueDateFilter != null) {
            dueDateStart = dueDateFilter.getStart();
            dueDateEnd = dueDateFilter.getEnd();
            includeUnsetDueDate = Boolean.TRUE.equals(dueDateFilter.getIncludeUnset());
            ValidateArgument.requirement(
                    dueDateStart != null || dueDateEnd != null || includeUnsetDueDate,
                    "'dueDate' filter must specify at least one of: start, end, or includeUnset.");
            ValidateArgument.requirement(
                    dueDateStart == null || dueDateEnd == null || !dueDateStart.after(dueDateEnd),
                    "'dueDate.start' must not be after 'dueDate.end'.");
        }

        List<Long> accessibleProjectIds;

        if (request.getProjectId() != null) {
            authorizationManager.canAccess(userInfo, request.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ)
                    .checkAuthorizationOrElseThrow();
            accessibleProjectIds = List.of(KeyFactory.stringToKey(request.getProjectId()));
        } else {
            Set<Long> allTaskProjectIds = curationTaskDao.getDistinctProjectIds();
            if (allTaskProjectIds.isEmpty()) {
                return new ListCurationTaskResponse().setPage(List.of()).setBundlePage(List.of());
            }
            accessibleProjectIds = new ArrayList<>(
                    aclManager.getAccessibleBenefactors(userInfo, ObjectType.ENTITY, allTaskProjectIds, ACCESS_TYPE.READ));
            if (accessibleProjectIds.isEmpty()) {
                return new ListCurationTaskResponse().setPage(List.of()).setBundlePage(List.of());
            }
        }

        List<TaskBundle> bundles = curationTaskDao.getCurationTaskBundles(
                accessibleProjectIds, assigneeIds, request.getStateFilter(),
                request.getTaskIds(), dueDateStart, dueDateEnd, includeUnsetDueDate,
                token.getLimitForQuery(), token.getOffset());

        List<CurationTask> tasks = bundles.stream().map(TaskBundle::getTask).collect(Collectors.toList());

        ListCurationTaskResponse response = new ListCurationTaskResponse();
        response.setPage(tasks);
        response.setBundlePage(bundles);
        response.setNextPageToken(token.getNextPageTokenForCurrentResults(tasks));
        return response;
    }


    @Override
    public TaskStatus getTaskStatus(UserInfo userInfo, Long taskId) {
        CurationTask task = curationTaskDao.getCurationTask(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found: " + taskId));

        authorizationManager.canAccess(userInfo, task.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ)
                .checkAuthorizationOrElseThrow();

        return curationTaskDao.getTaskStatus(taskId);
    }

	@Override
    public void validateUpdateTaskStatus(UserInfo userInfo, CurationTask task) {
        ValidateArgument.required(userInfo, "userInfo");
        ValidateArgument.required(task, "task");

        boolean hasUpdateAccess = authorizationManager
                .canAccess(userInfo, task.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)
                .isAuthorized();

        boolean isAssignee = task.getAssigneePrincipalId() != null
                && isAuthorizedAssignee(userInfo, Long.parseLong(task.getAssigneePrincipalId()));

        if (!hasUpdateAccess && !isAssignee) {
            throw new UnauthorizedException("You must have UPDATE access on the project or be an assignee of the task.");
        }
    }

	@Override
    @WriteTransaction
    public TaskStatus updateTaskStatus(UserInfo userInfo, Long taskId, TaskStatus statusUpdate) {
        ValidateArgument.required(statusUpdate, "statusUpdate");
        ValidateArgument.required(statusUpdate.getState(), "state");
        ValidateArgument.required(statusUpdate.getEtag(), "etag");

        CurationTask task = curationTaskDao.getCurationTask(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found: " + taskId));

        validateUpdateTaskStatus(userInfo, task);

        return curationTaskDao.updateTaskStatus(userInfo.getId(), taskId, statusUpdate);
    }

    private boolean isAuthorizedAssignee(UserInfo user, Long assigneeId) {
        return AuthorizationUtils.isUserCreatorOrAdmin(user, assigneeId.toString())
                || user.getGroups().contains(assigneeId);
    }

    /**
     * Returns true if the suggestedAuthorizationMode changed between the old and new values.
     */
    boolean hasAuthorizationModeChanged(AuthorizationMode oldMode, AuthorizationMode newMode) {
        return !Objects.equals(oldMode, newMode);
    }

    /**
     * Returns the suggestedAuthorizationMode from the given task properties, or null if not set or not applicable.
     */
    private static AuthorizationMode getSuggestedAuthorizationMode(CurationTaskProperties properties) {
        if (properties instanceof GridSupportedTaskProperties) {
            return ((GridSupportedTaskProperties) properties).getSuggestedAuthorizationMode();
        }
        return null;
    }

    private void validateCurationTask(UserInfo userInfo, CurationTask task) {
        ValidateArgument.required(task, "MetadataTask");
        ValidateArgument.required(task.getProjectId(), "projectId");
        ValidateArgument.required(task.getDataType(), "dataType");
        ValidateArgument.required(task.getTaskProperties(), "taskProperties");

        if (task.getTaskProperties() instanceof FileBasedMetadataTaskProperties) {
            FileBasedMetadataTaskProperties fileBasedMetadataTaskProperties = (FileBasedMetadataTaskProperties) task.getTaskProperties();
            ValidateArgument.required(fileBasedMetadataTaskProperties.getFileViewId(), "fileViewId");
            ValidateArgument.required(fileBasedMetadataTaskProperties.getUploadFolderId(), "uploadFolderId");

            EntityType typeOfSpecifiedView = entityManager.getEntityType(userInfo, fileBasedMetadataTaskProperties.getFileViewId());
            ValidateArgument.requirement(EntityType.entityview.equals(typeOfSpecifiedView), "The fileViewId must be an EntityView.");

            EntityType typeOfSpecifiedFolder = entityManager.getEntityType(userInfo, fileBasedMetadataTaskProperties.getUploadFolderId());
            ValidateArgument.requirement(EntityType.folder.equals(typeOfSpecifiedFolder) || EntityType.project.equals(typeOfSpecifiedFolder),
                    "The uploadFolderId must be a Folder or Project.");
        } else if (task.getTaskProperties() instanceof RecordBasedMetadataTaskProperties) {
            RecordBasedMetadataTaskProperties recordBasedMetadataTaskProperties = (RecordBasedMetadataTaskProperties) task.getTaskProperties();
            ValidateArgument.required(recordBasedMetadataTaskProperties.getRecordSetId(), "recordSetId");

            EntityType typeOfSpecifiedRecordSet = entityManager.getEntityType(userInfo, recordBasedMetadataTaskProperties.getRecordSetId());
            ValidateArgument.requirement(EntityType.recordset.equals(typeOfSpecifiedRecordSet), "The recordSetId must be a RecordSet.");
        } else if (task.getTaskProperties() instanceof SampleSheetGenerationExecutionProperties) {
            SampleSheetGenerationExecutionProperties sampleSheetProperties = (SampleSheetGenerationExecutionProperties) task.getTaskProperties();
            ValidateArgument.required(sampleSheetProperties.getInputTaskId(), "inputTaskId");
            ValidateArgument.required(sampleSheetProperties.getDestinationTaskId(), "destinationTaskId");

            // The input task must supply the source FileView and the destination task must receive the
            // generated RecordSet. Both referenced tasks must be of the expected type and belong to the
            // same project as the generation task.
            validateReferencedTask(sampleSheetProperties.getInputTaskId(), "inputTaskId",
                    FileBasedMetadataTaskProperties.class, task.getProjectId());
            validateReferencedTask(sampleSheetProperties.getDestinationTaskId(), "destinationTaskId",
                    RecordBasedMetadataTaskProperties.class, task.getProjectId());
        } else if (task.getTaskProperties() instanceof RecordSetGenerationExecutionProperties recordSetProperties) {
            ValidateArgument.required(recordSetProperties.getFolderId(), "folderId");
            ValidateArgument.required(recordSetProperties.getInstructions(), "instructions");
            ValidateArgument.required(recordSetProperties.getDestinationTaskId(), "destinationTaskId");

            // The input is a raw Folder synID (not a task reference); the destination task must receive
            // the generated RecordSet and belong to the same project as the generation task.
            EntityType typeOfSpecifiedFolder = entityManager.getEntityType(userInfo, recordSetProperties.getFolderId());
            ValidateArgument.requirement(EntityType.folder.equals(typeOfSpecifiedFolder),
                    "The folderId must be a Folder.");

            validateReferencedTask(recordSetProperties.getDestinationTaskId(), "destinationTaskId",
                    RecordBasedMetadataTaskProperties.class, task.getProjectId());
        } else {
            throw new IllegalArgumentException("Unknown CurationTaskProperties concreteType: " + task.getTaskProperties().getConcreteType());
        }
    }

    /**
     * Validates that a referenced task exists, carries task properties of the expected type, and
     * belongs to the same project as the generation task.
     */
    private void validateReferencedTask(Long referencedTaskId, String fieldName,
            Class<? extends CurationTaskProperties> expectedType, String projectId) {
        CurationTask referencedTask = curationTaskDao.getCurationTask(referencedTaskId)
                .orElseThrow(() -> new IllegalArgumentException("The " + fieldName + " task does not exist: " + referencedTaskId));
        ValidateArgument.requirement(expectedType.isInstance(referencedTask.getTaskProperties()),
                "The " + fieldName + " must reference a task with " + expectedType.getSimpleName() + ".");
        ValidateArgument.requirement(projectId.equals(referencedTask.getProjectId()),
                "The " + fieldName + " must reference a task in the same project.");
    }
}
