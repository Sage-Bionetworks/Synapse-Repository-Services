package org.sagebionetworks.repo.model.dbo.curation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class DBOCurationTask implements MigratableDatabaseObject<DBOCurationTask, DBOCurationTask> {

    private static final FieldColumn[] FIELDS = new FieldColumn[]{
            new FieldColumn("id", SqlConstants.COL_CURATION_TASK_ID, true).withIsBackupId(true),
            new FieldColumn("etag", SqlConstants.COL_CURATION_TASK_ETAG).withIsEtag(true),
            new FieldColumn("dataType", SqlConstants.COL_CURATION_TASK_DATA_TYPE),
            new FieldColumn("projectId", SqlConstants.COL_CURATION_TASK_PROJECT_ID),
            new FieldColumn("instructions", SqlConstants.COL_CURATION_TASK_INSTRUCTIONS),
            new FieldColumn("createdBy", SqlConstants.COL_CURATION_TASK_CREATED_BY),
            new FieldColumn("createdOn", SqlConstants.COL_CURATION_TASK_CREATED_ON),
            new FieldColumn("modifiedBy", SqlConstants.COL_CURATION_TASK_MODIFIED_BY),
            new FieldColumn("modifiedOn", SqlConstants.COL_CURATION_TASK_MODIFIED_ON),
            new FieldColumn("taskPropertiesJson", SqlConstants.COL_CURATION_TASK_TASK_PROPERTIES),
            new FieldColumn("assigneeId", SqlConstants.COL_CURATION_TASK_ASSIGNEE),
            new FieldColumn("state", SqlConstants.COL_CURATION_TASK_STATE),
            new FieldColumn("executionDetailsJson", SqlConstants.COL_CURATION_TASK_EXECUTION_DETAILS),
            new FieldColumn("stateUpdatedBy", SqlConstants.COL_CURATION_TASK_STATE_UPDATED_BY),
            new FieldColumn("stateUpdatedOn", SqlConstants.COL_CURATION_TASK_STATE_UPDATED_ON)
    };

    private static final TableMapping<DBOCurationTask> TABLE_MAPPING = new TableMapping<>() {

        @Override
        public DBOCurationTask mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new DBOCurationTask()
                    .setId(rs.getLong(SqlConstants.COL_CURATION_TASK_ID))
                    .setDataType(rs.getString(SqlConstants.COL_CURATION_TASK_DATA_TYPE))
                    .setProjectId(rs.getLong(SqlConstants.COL_CURATION_TASK_PROJECT_ID))
                    .setInstructions(rs.getString(SqlConstants.COL_CURATION_TASK_INSTRUCTIONS))
                    .setEtag(rs.getString(SqlConstants.COL_CURATION_TASK_ETAG))
                    .setCreatedBy(rs.getLong(SqlConstants.COL_CURATION_TASK_CREATED_BY))
                    .setCreatedOn(rs.getTimestamp(SqlConstants.COL_CURATION_TASK_CREATED_ON))
                    .setModifiedBy(rs.getLong(SqlConstants.COL_CURATION_TASK_MODIFIED_BY))
                    .setModifiedOn(rs.getTimestamp(SqlConstants.COL_CURATION_TASK_MODIFIED_ON))
                    .setTaskPropertiesJson(rs.getString(SqlConstants.COL_CURATION_TASK_TASK_PROPERTIES))
                    .setAssigneeId(rs.getLong(SqlConstants.COL_CURATION_TASK_ASSIGNEE))
                    .setState(rs.getString(SqlConstants.COL_CURATION_TASK_STATE))
                    .setExecutionDetailsJson(rs.getString(SqlConstants.COL_CURATION_TASK_EXECUTION_DETAILS))
                    .setStateUpdatedBy(rs.getLong(SqlConstants.COL_CURATION_TASK_STATE_UPDATED_BY))
                    .setStateUpdatedOn(rs.getTimestamp(SqlConstants.COL_CURATION_TASK_STATE_UPDATED_ON));
        }

        @Override
        public String getTableName() {
            return SqlConstants.TABLE_CURATION_TASK;
        }

        @Override
        public FieldColumn[] getFieldColumns() {
            return FIELDS;
        }

        @Override
        public String getDDLFileName() {
            return SqlConstants.DDL_CURATION_TASK;
        }

        @Override
        public Class<? extends DBOCurationTask> getDBOClass() {
            return DBOCurationTask.class;
        }
    };

    private Long id;
    private String dataType;
    private Long projectId;
    private String instructions;
    private String etag;
    private Long createdBy;
    private Timestamp createdOn;
    private Long modifiedBy;
    private Timestamp modifiedOn;
    private String taskPropertiesJson;
    private Long assigneeId;
    private String state;
    private String executionDetailsJson;
    private Long stateUpdatedBy;
    private Timestamp stateUpdatedOn;

    public DBOCurationTask() {
    }

    public Long getId() {
        return id;
    }

    public DBOCurationTask setId(Long id) {
        this.id = id;
        return this;
    }

    public String getDataType() {
        return dataType;
    }

    public DBOCurationTask setDataType(String dataType) {
        this.dataType = dataType;
        return this;
    }

    public Long getProjectId() {
        return projectId;
    }

    public DBOCurationTask setProjectId(Long projectId) {
        this.projectId = projectId;
        return this;
    }

    public String getInstructions() {
        return instructions;
    }

    public DBOCurationTask setInstructions(String instructions) {
        this.instructions = instructions;
        return this;
    }

    public String getEtag() {
        return etag;
    }

    public DBOCurationTask setEtag(String etag) {
        this.etag = etag;
        return this;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public DBOCurationTask setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public DBOCurationTask setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    public Long getModifiedBy() {
        return modifiedBy;
    }

    public DBOCurationTask setModifiedBy(Long modifiedBy) {
        this.modifiedBy = modifiedBy;
        return this;
    }

    public Date getModifiedOn() {
        return modifiedOn;
    }

    public DBOCurationTask setModifiedOn(Timestamp modifiedOn) {
        this.modifiedOn = modifiedOn;
        return this;
    }

    public String getTaskPropertiesJson() {
        return taskPropertiesJson;
    }

    public DBOCurationTask setTaskPropertiesJson(String taskPropertiesJson) {
        this.taskPropertiesJson = taskPropertiesJson;
        return this;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public DBOCurationTask setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
        return this;
    }

    public String getState() {
        return state;
    }

    public DBOCurationTask setState(String state) {
        this.state = state;
        return this;
    }

    public String getExecutionDetailsJson() {
        return executionDetailsJson;
    }

    public DBOCurationTask setExecutionDetailsJson(String executionDetailsJson) {
        this.executionDetailsJson = executionDetailsJson;
        return this;
    }

    public Long getStateUpdatedBy() {
        return stateUpdatedBy;
    }

    public DBOCurationTask setStateUpdatedBy(Long stateUpdatedBy) {
        this.stateUpdatedBy = stateUpdatedBy;
        return this;
    }

    public Date getStateUpdatedOn() {
        return stateUpdatedOn;
    }

    public DBOCurationTask setStateUpdatedOn(Timestamp stateUpdatedOn) {
        this.stateUpdatedOn = stateUpdatedOn;
        return this;
    }

    @Override
    public TableMapping<DBOCurationTask> getTableMapping() {
        return TABLE_MAPPING;
    }

    @Override
    public MigrationType getMigratableTableType() {
        return MigrationType.CURATION_TASK;
    }

    @Override
    public MigratableTableTranslation<DBOCurationTask, DBOCurationTask> getTranslator() {
        return new MigratableTableTranslation<DBOCurationTask, DBOCurationTask>() {
            @Override
            public DBOCurationTask createDatabaseObjectFromBackup(DBOCurationTask backup) {
                if (backup.getState() == null) {
                    backup.setState("NOT_STARTED");
                }
                return backup;
            }

            @Override
            public DBOCurationTask createBackupFromDatabaseObject(DBOCurationTask dbo) {
                return dbo;
            }
        };
    }

    @Override
    public Class<? extends DBOCurationTask> getBackupClass() {
        return DBOCurationTask.class;
    }

    @Override
    public Class<? extends DBOCurationTask> getDatabaseObjectClass() {
        return DBOCurationTask.class;
    }

    @Override
    public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
        return Collections.emptyList();
    }

    @Override
    public int hashCode() {
        return Objects.hash(assigneeId, createdBy, createdOn, dataType, etag, executionDetailsJson, id, instructions,
                modifiedBy, modifiedOn, projectId, state, stateUpdatedBy, stateUpdatedOn,
                taskPropertiesJson);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DBOCurationTask other = (DBOCurationTask) obj;
        return Objects.equals(assigneeId, other.assigneeId) && Objects.equals(createdBy, other.createdBy)
                && Objects.equals(createdOn, other.createdOn) && Objects.equals(dataType, other.dataType)
                && Objects.equals(etag, other.etag)
                && Objects.equals(executionDetailsJson, other.executionDetailsJson)
                && Objects.equals(id, other.id) && Objects.equals(instructions, other.instructions)
                && Objects.equals(modifiedBy, other.modifiedBy) && Objects.equals(modifiedOn, other.modifiedOn)
                && Objects.equals(projectId, other.projectId) && Objects.equals(state, other.state)
                && Objects.equals(stateUpdatedBy, other.stateUpdatedBy)
                && Objects.equals(stateUpdatedOn, other.stateUpdatedOn)
                && Objects.equals(taskPropertiesJson, other.taskPropertiesJson);
    }

    @Override
    public String toString() {
        return "DBOCurationTask [id=" + id + ", dataType=" + dataType + ", projectId=" + projectId + ", instructions="
                + instructions + ", etag=" + etag + ", createdBy=" + createdBy + ", createdOn=" + createdOn
                + ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn + ", taskPropertiesJson="
                + taskPropertiesJson + ", assigneeId=" + assigneeId + ", state=" + state + ", executionDetailsJson="
                + executionDetailsJson + ", stateUpdatedBy=" + stateUpdatedBy + ", stateUpdatedOn=" + stateUpdatedOn
                + "]";
    }

}
