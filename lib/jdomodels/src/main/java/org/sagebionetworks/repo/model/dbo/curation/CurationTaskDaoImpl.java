package org.sagebionetworks.repo.model.dbo.curation;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_ASSIGNEE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_DATA_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_DUE_DATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_EXECUTION_DETAILS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_INSTRUCTIONS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_STATE_UPDATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_STATE_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_TASK_PROPERTIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CURATION_TASK;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.CurationTaskProperties;
import org.sagebionetworks.repo.model.curation.TaskBundle;
import org.sagebionetworks.repo.model.curation.TaskExecutionDetails;
import org.sagebionetworks.repo.model.curation.TaskState;
import org.sagebionetworks.repo.model.curation.TaskStatus;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CurationTaskDaoImpl implements CurationTaskDao {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final IdGenerator idGenerator;

    public CurationTaskDaoImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate, IdGenerator idGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.idGenerator = idGenerator;
    }

    private static final RowMapper<CurationTask> CURATION_TASK_ROW_MAPPER = (rs, rowNum) -> {
        Timestamp dueDate = rs.getTimestamp(COL_CURATION_TASK_DUE_DATE);
        return new CurationTask()
                .setTaskId(rs.getLong(COL_CURATION_TASK_ID))
                .setDataType(rs.getString(COL_CURATION_TASK_DATA_TYPE))
                .setProjectId(KeyFactory.keyToString(rs.getLong(COL_CURATION_TASK_PROJECT_ID)))
                .setInstructions(rs.getString(COL_CURATION_TASK_INSTRUCTIONS))
                .setEtag(rs.getString(COL_CURATION_TASK_ETAG))
                .setCreatedBy(rs.getString(COL_CURATION_TASK_CREATED_BY))
                .setCreatedOn(new Date(rs.getTimestamp(COL_CURATION_TASK_CREATED_ON).getTime()))
                .setModifiedBy(rs.getString(COL_CURATION_TASK_MODIFIED_BY))
                .setModifiedOn(new Date(rs.getTimestamp(COL_CURATION_TASK_MODIFIED_ON).getTime()))
                .setTaskProperties(
                        JDOSecondaryPropertyUtils.createObjectFromJSON(CurationTaskProperties.class, rs.getString(COL_CURATION_TASK_TASK_PROPERTIES))
                )
                .setAssigneePrincipalId(rs.getString(COL_CURATION_TASK_ASSIGNEE))
                .setDueDate(dueDate != null ? new Date(dueDate.getTime()) : null);
    };

    private static final RowMapper<TaskStatus> TASK_STATUS_ROW_MAPPER = (rs, rowNum) -> {
        String executionDetailsJson = rs.getString(COL_CURATION_TASK_EXECUTION_DETAILS);
        TaskExecutionDetails executionDetails = executionDetailsJson != null
                ? JDOSecondaryPropertyUtils.createObjectFromJSON(TaskExecutionDetails.class, executionDetailsJson)
                : null;
        Long stateUpdatedBy = rs.getObject(COL_CURATION_TASK_STATE_UPDATED_BY, Long.class);
        Timestamp stateUpdatedOn = rs.getTimestamp(COL_CURATION_TASK_STATE_UPDATED_ON);
        return new TaskStatus()
                .setTaskId(rs.getLong(COL_CURATION_TASK_ID))
                .setState(TaskState.valueOf(rs.getString(COL_CURATION_TASK_STATE)))
                .setExecutionDetails(executionDetails)
                .setLastUpdatedBy(stateUpdatedBy != null ? stateUpdatedBy.toString() : null)
                .setLastUpdatedOn(stateUpdatedOn != null ? new Date(stateUpdatedOn.getTime()) : null)
                .setEtag(rs.getString(COL_CURATION_TASK_ETAG));
    };

    private static final RowMapper<TaskBundle> TASK_BUNDLE_ROW_MAPPER = (rs, rowNum) -> {
        CurationTask task = CURATION_TASK_ROW_MAPPER.mapRow(rs, rowNum);
        TaskStatus status = TASK_STATUS_ROW_MAPPER.mapRow(rs, rowNum);
        return new TaskBundle().setTask(task).setStatus(status);
    };

    @Override
    @WriteTransaction
    public CurationTask createCurationTask(Long userId, CurationTask toCreate) {
        String sql = "INSERT INTO " + TABLE_CURATION_TASK + " ("
                + COL_CURATION_TASK_ID + ", "
                + COL_CURATION_TASK_CREATED_BY + ", "
                + COL_CURATION_TASK_MODIFIED_BY + ", "
                + COL_CURATION_TASK_DATA_TYPE + ", "
                + COL_CURATION_TASK_PROJECT_ID + ", "
                + COL_CURATION_TASK_INSTRUCTIONS + ", "
                + COL_CURATION_TASK_TASK_PROPERTIES + ", "
                + COL_CURATION_TASK_ASSIGNEE + ", "
                + COL_CURATION_TASK_DUE_DATE + ", "
                + COL_CURATION_TASK_CREATED_ON + ", "
                + COL_CURATION_TASK_MODIFIED_ON + ", "
                + COL_CURATION_TASK_ETAG
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), UUID())";

        Long id = idGenerator.generateNewId(IdType.CURATION_TASK_ID);

        DBOCurationTask dbo = mapToDbo(toCreate);

        try {
            jdbcTemplate.update(sql,
                    id,
                    userId,
                    userId,
                    dbo.getDataType(),
                    dbo.getProjectId(),
                    dbo.getInstructions(),
                    dbo.getTaskPropertiesJson(),
                    dbo.getAssigneeId(),
                    dbo.getDueDate()
            );
        } catch (DuplicateKeyException e) {
            handleUniquenessConstraintViolation(e);
        }

        return getCurationTask(id).orElseThrow(() -> new IllegalStateException("The curation task was not created."));
    }

    @Override
    @WriteTransaction
    public CurationTask updateCurationTask(Long userId, CurationTask toUpdate) {
        String currentEtag = getEtagForCurationTaskForUpdate(toUpdate.getTaskId());

        if (!currentEtag.equals(toUpdate.getEtag())) {
            throw new ConflictingUpdateException("The curation task was updated since you last fetched it, please fetch it again and reapply your changes.");
        }

        DBOCurationTask dbo = mapToDbo(toUpdate);
        String sql = "UPDATE " + TABLE_CURATION_TASK + " SET "
                + COL_CURATION_TASK_ETAG + " = UUID(), "
                + COL_CURATION_TASK_MODIFIED_BY + " = ?, "
                + COL_CURATION_TASK_MODIFIED_ON + " = ?, "
                + COL_CURATION_TASK_DATA_TYPE + " = ?, "
                + COL_CURATION_TASK_INSTRUCTIONS + " = ?, "
                + COL_CURATION_TASK_TASK_PROPERTIES + " = ?, "
                + COL_CURATION_TASK_ASSIGNEE + " = ?, "
                + COL_CURATION_TASK_DUE_DATE + " = ? "
                + "WHERE " + COL_CURATION_TASK_ID + " = ? ";

        try {
            jdbcTemplate.update(sql,
                    userId,
                    Timestamp.from(Instant.now()),
                    dbo.getDataType(),
                    dbo.getInstructions(),
                    dbo.getTaskPropertiesJson(),
                    dbo.getAssigneeId(),
                    dbo.getDueDate(),
                    dbo.getId());
        } catch (DuplicateKeyException e) {
            handleUniquenessConstraintViolation(e);
        }
        return getCurationTask(toUpdate.getTaskId()).orElseThrow(() -> new IllegalStateException("The curation task was not updated."));
    }

    @Override
    public Optional<CurationTask> getCurationTask(Long taskId) {
        String sql = "SELECT * FROM " + TABLE_CURATION_TASK + " WHERE "
                + COL_CURATION_TASK_ID + " = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, CURATION_TASK_ROW_MAPPER, taskId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @WriteTransaction
    public void deleteCurationTask(Long taskId) {
        jdbcTemplate.update("DELETE FROM " + TABLE_CURATION_TASK + " WHERE " + COL_CURATION_TASK_ID + " = ?", taskId);
    }

    @Override
    public List<CurationTask> getCurationTasks(Long projectId, long limit, long offset) {
        String sql = "SELECT * FROM " + TABLE_CURATION_TASK + " WHERE "
                + COL_CURATION_TASK_PROJECT_ID + " = ? "
                + "ORDER BY " + COL_CURATION_TASK_ID + " LIMIT ? OFFSET ?";

        return jdbcTemplate.query(sql, CURATION_TASK_ROW_MAPPER, projectId, limit, offset);
    }

    @Override
    public TaskStatus getTaskStatus(Long taskId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT ID, STATE, EXECUTION_DETAILS, STATE_UPDATED_BY, STATE_UPDATED_ON, ETAG"
                            + " FROM CURATION_TASK WHERE ID = ?",
                    TASK_STATUS_ROW_MAPPER, taskId);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("A curation task with ID " + taskId + " does not exist.");
        }
    }

    @Override
    @WriteTransaction
    public TaskStatus updateTaskStatus(Long userId, Long taskId, TaskStatus statusUpdate) {
        String currentEtag = getEtagForCurationTaskForUpdate(taskId);

        if (!currentEtag.equals(statusUpdate.getEtag())) {
            throw new ConflictingUpdateException(
                    "The task status was updated since you last fetched it, please fetch it again and reapply your changes.");
        }

        String executionDetailsJson = statusUpdate.getExecutionDetails() != null
                ? JDOSecondaryPropertyUtils.createJSONFromObject(statusUpdate.getExecutionDetails())
                : null;

        jdbcTemplate.update(
                "UPDATE CURATION_TASK SET STATE = ?, EXECUTION_DETAILS = ?,"
                        + " STATE_UPDATED_BY = ?, STATE_UPDATED_ON = NOW(3), ETAG = UUID()"
                        + " WHERE ID = ?",
                statusUpdate.getState().name(),
                executionDetailsJson,
                userId,
                taskId);

        return getTaskStatus(taskId);
    }

    @Override
    @MandatoryWriteTransaction
    public void clearActiveSessionId(Long taskId) {
        jdbcTemplate.update(
                "UPDATE CURATION_TASK"
                        + " SET EXECUTION_DETAILS = JSON_REMOVE(EXECUTION_DETAILS, '$.activeSessionId')"
                        + " WHERE ID = ? AND EXECUTION_DETAILS IS NOT NULL",
                taskId);
    }

    @Override
    public List<TaskBundle> getCurationTaskBundles(List<Long> projectIds, List<Long> assigneeIds,
            List<TaskState> stateFilter, List<Long> taskIds, Date dueDateStart, Date dueDateEnd,
            boolean includeUnsetDueDate, long limit, long offset) {
        ValidateArgument.requiredNotEmpty(projectIds, "projectIds");

        StringBuilder sql = new StringBuilder("SELECT * FROM CURATION_TASK WHERE PROJECT_ID IN (:projectIds)");
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("projectIds", projectIds);

        if (assigneeIds != null && !assigneeIds.isEmpty()) {
            sql.append(" AND ASSIGNEE IN (:assigneeIds)");
            params.addValue("assigneeIds", assigneeIds);
        }

        if (stateFilter != null && !stateFilter.isEmpty()) {
            List<String> stateNames = stateFilter.stream().map(TaskState::name).collect(Collectors.toList());
            sql.append(" AND STATE IN (:stateFilter)");
            params.addValue("stateFilter", stateNames);
        }

        if (taskIds != null && !taskIds.isEmpty()) {
            sql.append(" AND " + COL_CURATION_TASK_ID + " IN (:taskIds)");
            params.addValue("taskIds", taskIds);
        }

        boolean hasStart = dueDateStart != null;
        boolean hasEnd = dueDateEnd != null;
        boolean hasDueDateFilter = hasStart || hasEnd || includeUnsetDueDate;
        if (hasDueDateFilter) {
            if (hasStart) params.addValue("dueDateStart", new Timestamp(dueDateStart.getTime()));
            if (hasEnd) params.addValue("dueDateEnd", new Timestamp(dueDateEnd.getTime()));

            boolean hasRange = hasStart || hasEnd;
            if (includeUnsetDueDate && hasRange) {
                sql.append(" AND (DUE_DATE IS NULL OR (");
                if (hasStart) sql.append("DUE_DATE >= :dueDateStart");
                if (hasStart && hasEnd) sql.append(" AND ");
                if (hasEnd) sql.append("DUE_DATE <= :dueDateEnd");
                sql.append("))");
            } else if (includeUnsetDueDate) {
                sql.append(" AND DUE_DATE IS NULL");
            } else {
                if (hasStart) sql.append(" AND DUE_DATE >= :dueDateStart");
                if (hasEnd) sql.append(" AND DUE_DATE <= :dueDateEnd");
            }
        }

        sql.append(" ORDER BY ID LIMIT :limit OFFSET :offset");
        params.addValue("limit", limit);
        params.addValue("offset", offset);

        return namedJdbcTemplate.query(sql.toString(), params, TASK_BUNDLE_ROW_MAPPER);
    }

    @Override
    public Set<Long> getDistinctProjectIds() {
        return new HashSet<>(jdbcTemplate.queryForList(
                "SELECT DISTINCT PROJECT_ID FROM CURATION_TASK", Long.class));
    }

    @MandatoryWriteTransaction
    String getEtagForCurationTaskForUpdate(Long taskId) {
        String sql = "SELECT " + COL_CURATION_TASK_ETAG + " FROM " + TABLE_CURATION_TASK + " WHERE "
                + COL_CURATION_TASK_ID + " = ? "
                + "FOR UPDATE";
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> rs.getString(COL_CURATION_TASK_ETAG), taskId);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("A curation task with ID " + taskId + " does not exist.");
        }
    }

    private static void handleUniquenessConstraintViolation(DuplicateKeyException e) {
        if (e.getMessage() != null && e.getMessage().contains("CURATION_TASK_DATA_TYPE_PROJECT_ID")) {
            throw new IllegalArgumentException("A curation task with the specified data type already exists in this project.", e);
        }
        throw e;
    }

    private static DBOCurationTask mapToDbo(CurationTask dto) {
        DBOCurationTask dbo = new DBOCurationTask()
                .setId(dto.getTaskId())
                .setDataType(dto.getDataType())
                .setProjectId(KeyFactory.stringToKey(dto.getProjectId()))
                .setInstructions(dto.getInstructions())
                .setEtag(dto.getEtag())
                .setTaskPropertiesJson(JDOSecondaryPropertyUtils.createJSONFromObject(dto.getTaskProperties()))
                .setAssigneeId(dto.getAssigneePrincipalId() != null ? Long.parseLong(dto.getAssigneePrincipalId()) : null)
                .setDueDate(dto.getDueDate() != null ? new Timestamp(dto.getDueDate().getTime()) : null);
        if (dto.getCreatedBy() != null) {
            dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
        }
        if (dto.getCreatedOn() != null) {
            dbo.setCreatedOn(new Timestamp(dto.getCreatedOn().getTime()));
        }
        if (dto.getModifiedBy() != null) {
            dbo.setModifiedBy(Long.parseLong(dto.getModifiedBy()));
        }
        if (dto.getModifiedOn() != null) {
            dbo.setModifiedOn(new Timestamp(dto.getModifiedOn().getTime()));
        }
        return dbo;
    }
}
