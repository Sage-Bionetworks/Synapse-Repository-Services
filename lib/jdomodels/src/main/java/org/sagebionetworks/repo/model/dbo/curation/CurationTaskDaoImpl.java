package org.sagebionetworks.repo.model.dbo.curation;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_DATA_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_INSTRUCTIONS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_TASK_PROPERTIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CURATION_TASK;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.CurationTaskProperties;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CurationTaskDaoImpl implements CurationTaskDao {

    private final JdbcTemplate jdbcTemplate;
    private final IdGenerator idGenerator;

    public CurationTaskDaoImpl(JdbcTemplate jdbcTemplate, IdGenerator idGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.idGenerator = idGenerator;
    }

    private static final RowMapper<CurationTask> CURATION_TASK_ROW_MAPPER = (rs, rowNum) ->
            new CurationTask().setTaskId(rs.getLong(COL_CURATION_TASK_ID))
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
                    );

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
                + COL_CURATION_TASK_CREATED_ON + ", "
                + COL_CURATION_TASK_MODIFIED_ON + ", "
                + COL_CURATION_TASK_ETAG
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), UUID())";

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
                    dbo.getTaskPropertiesJson()
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
                + COL_CURATION_TASK_TASK_PROPERTIES + " = ? "
                + "WHERE " + COL_CURATION_TASK_ID + " = ? ";

        try {
            jdbcTemplate.update(sql,
                    userId,
                    Timestamp.from(Instant.now()),
                    dbo.getDataType(),
                    dbo.getInstructions(),
                    dbo.getTaskPropertiesJson(),
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
                .setTaskPropertiesJson(JDOSecondaryPropertyUtils.createJSONFromObject(dto.getTaskProperties()));
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
