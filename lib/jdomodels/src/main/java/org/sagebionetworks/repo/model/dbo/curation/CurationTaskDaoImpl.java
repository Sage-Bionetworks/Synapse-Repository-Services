package org.sagebionetworks.repo.model.dbo.curation;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURATION_TASK_MODIFIED_ON;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.CurationTaskProperties;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CurationTaskDaoImpl implements CurationTaskDao {

    private final JdbcTemplate jdbcTemplate;
    private final IdGenerator idGenerator;
    private final DBOBasicDao basicDao;

    public CurationTaskDaoImpl(JdbcTemplate jdbcTemplate, IdGenerator idGenerator, DBOBasicDao basicDao) {
        this.jdbcTemplate = jdbcTemplate;
        this.idGenerator = idGenerator;
        this.basicDao = basicDao;
    }

    private static final RowMapper<CurationTask> CURATION_TASK_ROW_MAPPER = (rs, rowNum) ->
            new CurationTask().setTaskId(String.valueOf(rs.getLong(COL_CURATION_TASK_ID)))
                    .setDataType(rs.getString(SqlConstants.COL_CURATION_TASK_DATA_TYPE))
                    .setProjectId(KeyFactory.keyToString(rs.getLong(SqlConstants.COL_CURATION_TASK_PROJECT_ID)))
                    .setInstructions(rs.getString(SqlConstants.COL_CURATION_TASK_INSTRUCTIONS))
                    .setEtag(rs.getString(COL_CURATION_TASK_ETAG))
                    .setCreatedBy(rs.getString(COL_CURATION_TASK_CREATED_BY))
                    .setCreatedOn(new Date(rs.getTimestamp(COL_CURATION_TASK_CREATED_ON).getTime()))
                    .setModifiedBy(rs.getString(COL_CURATION_TASK_MODIFIED_BY))
                    .setModifiedOn(new Date(rs.getTimestamp(COL_CURATION_TASK_MODIFIED_ON).getTime()))
                    .setTaskProperties(
                            JDOSecondaryPropertyUtils.createObjectFromJSON(CurationTaskProperties.class, rs.getString(SqlConstants.COL_CURATION_TASK_TASK_PROPERTIES))
                    );


    @Override
    @WriteTransaction
    public CurationTask createCurationTask(Long userId, CurationTask toCreate) {
        Instant now = Instant.now();
        toCreate.setTaskId(idGenerator.generateNewId(IdType.CURATION_TASK_ID).toString())
                .setEtag(UUID.randomUUID().toString())
                .setCreatedBy(String.valueOf(userId))
                .setCreatedOn(Timestamp.from(now))
                .setModifiedBy(String.valueOf(userId))
                .setModifiedOn(Timestamp.from(now));

        DBOCurationTask dbo = mapToDbo(toCreate);

        try {
            basicDao.createNew(dbo);
        } catch (IllegalArgumentException e) {
            if (e.getCause() instanceof DuplicateKeyException) {
                throw new IllegalArgumentException("A curation task with the specified data type already exists in this project.", e);
            }
            throw e;
        }

        return getCurationTask(dbo.getId().toString()).orElseThrow(() -> new IllegalStateException("The curation task was not created."));
    }

    @Override
    @WriteTransaction
    public CurationTask updateCurationTask(Long userId, CurationTask toUpdate) {
        String currentEtag = getEtagForCurationTaskForUpdate(toUpdate.getTaskId());

        if (!currentEtag.equals(toUpdate.getEtag())) {
            throw new ConflictingUpdateException("The curation task was updated since you last fetched it, please fetch it again and reapply your changes.");
        }

        DBOCurationTask dbo = mapToDbo(toUpdate)
                .setEtag(UUID.randomUUID().toString())
                .setModifiedBy(userId)
                .setModifiedOn(Timestamp.from(Instant.now()));

        try {
            basicDao.update(dbo);
        } catch (IllegalArgumentException e) {
            handleUniquenessConstraintViolation(e);
        }
        return getCurationTask(toUpdate.getTaskId()).orElseThrow(() -> new IllegalStateException("The curation task was not updated."));
    }

    @Override
    public Optional<CurationTask> getCurationTask(String taskId) {
        String sql = "SELECT * FROM " + SqlConstants.TABLE_CURATION_TASK + " WHERE "
                + SqlConstants.COL_CURATION_TASK_ID + " = ?";

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, CURATION_TASK_ROW_MAPPER, Long.parseLong(taskId)));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @WriteTransaction
    public void deleteCurationTask(String taskId) {
        basicDao.deleteObjectByPrimaryKey(DBOCurationTask.class, new SinglePrimaryKeySqlParameterSource(Long.parseLong(taskId)));
    }

    @Override
    public List<CurationTask> getCurationTasks(Long projectId, long limit, long offset) {
        String sql = "SELECT * FROM " + SqlConstants.TABLE_CURATION_TASK + " WHERE "
                + SqlConstants.COL_CURATION_TASK_PROJECT_ID + " = ? "
                + "ORDER BY " + SqlConstants.COL_CURATION_TASK_ID + " LIMIT ? OFFSET ?";

        return jdbcTemplate.query(sql, CURATION_TASK_ROW_MAPPER, projectId, limit, offset);
    }

    @WriteTransaction
    String getEtagForCurationTaskForUpdate(String taskId) {
        String sql = "SELECT " + COL_CURATION_TASK_ETAG + " FROM " + SqlConstants.TABLE_CURATION_TASK + " WHERE "
                + COL_CURATION_TASK_ID + " = ? "
                + "FOR UPDATE";

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> rs.getString(COL_CURATION_TASK_ETAG), taskId);
    }

    private static void handleUniquenessConstraintViolation(IllegalArgumentException e) {
        Throwable cause = e.getCause();
        if (cause instanceof DuplicateKeyException && cause.getMessage() != null && cause.getMessage().contains("CURATION_TASK_DATA_TYPE_PROJECT_ID")) {
            throw new IllegalArgumentException("A curation task with the specified data type already exists in this project.", e);
        }
        throw e;
    }

    private static DBOCurationTask mapToDbo(CurationTask dto) {
        DBOCurationTask dbo = new DBOCurationTask()
                .setId(Long.parseLong(dto.getTaskId()))
                .setDataType(dto.getDataType())
                .setProjectId(KeyFactory.stringToKey(dto.getProjectId()))
                .setInstructions(dto.getInstructions())
                .setEtag(dto.getEtag())
                .setCreatedBy(Long.parseLong(dto.getCreatedBy()))
                .setCreatedOn(new Timestamp(dto.getCreatedOn().getTime()))
                .setModifiedBy(Long.parseLong(dto.getModifiedBy()))
                .setModifiedOn(new Timestamp(dto.getModifiedOn().getTime()))
                .setTaskPropertiesJson(JDOSecondaryPropertyUtils.createJSONFromObject(dto.getTaskProperties()));

        return dbo;
    }
}
