package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_BUCKET_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CONTENT_MD5;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CONTENT_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_IS_PREVIEW;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_METADATA_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_PREVIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.repo.model.BucketAndKey;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.FileHandleMetadataType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.FileMetadataUtils;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.FileHandleStatus;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.FileHandleLinkedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Basic JDBC implementation of the FileMetadataDao.
 * 
 * @author John
 *
 */
public class DBOFileHandleDaoImpl implements FileHandleDao {

	private static final String IDS_PARAM = ":ids";

	private static final String SQL_SELECT_FILES_CREATED_BY_USER = "SELECT "+COL_FILES_ID+" FROM "+TABLE_FILES+" WHERE "+COL_FILES_ID+" IN ( " + IDS_PARAM + " ) AND "+COL_FILES_CREATED_BY+" = :createdById";
	private static final String SQL_SELECT_FILE_PREVIEWS = "SELECT "+COL_FILES_PREVIEW_ID+", "+COL_FILES_ID+" FROM "+TABLE_FILES+" WHERE "+COL_FILES_PREVIEW_ID+" IN ( " + IDS_PARAM + " )";
	private static final String SQL_COUNT_ALL_FILES = "SELECT COUNT(*) FROM "+TABLE_FILES;
	private static final String SQL_MAX_FILE_ID = "SELECT MAX(ID) FROM " + TABLE_FILES;
	private static final String SQL_SELECT_CREATOR = "SELECT "+COL_FILES_CREATED_BY+" FROM "+TABLE_FILES+" WHERE "+COL_FILES_ID+" = ?";
	private static final String SQL_SELECT_BATCH = "SELECT * FROM " + TABLE_FILES + " WHERE " + COL_FILES_ID + " IN ( " + IDS_PARAM + " )";
	private static final String SQL_SELECT_PREVIEW_ID = "SELECT "+COL_FILES_PREVIEW_ID+" FROM "+TABLE_FILES+" WHERE "+COL_FILES_ID+" = ?";
	private static final String UPDATE_PREVIEW_AND_ETAG = "UPDATE "+TABLE_FILES+" SET "+COL_FILES_PREVIEW_ID+" = ? ,"+COL_FILES_ETAG+" = ? WHERE "+COL_FILES_ID+" = ?";
	private static final String UPDATE_MARK_FILE_AS_PREVIEW  = "UPDATE "+TABLE_FILES+" SET "+COL_FILES_IS_PREVIEW+" = ? ,"+COL_FILES_ETAG+" = ? WHERE "+COL_FILES_ID+" = ?";
	private static final String SQL_UPDATE_STATUS_BATCH = "UPDATE " + TABLE_FILES + " SET " + COL_FILES_STATUS + "=?, " + COL_FILES_ETAG + "=UUID(), " + COL_FILES_UPDATED_ON + "=NOW() WHERE " + COL_FILES_ID + "=? AND " + COL_FILES_STATUS + "=?";
	private static final String SQL_CHECK_BATCH_STATUS= "SELECT COUNT(*) FROM (SELECT " + COL_FILES_ID + " FROM " + TABLE_FILES + " WHERE " + COL_FILES_ID + " IN ( " + IDS_PARAM + " ) AND " + COL_FILES_STATUS + "=:" + COL_FILES_STATUS + " LIMIT 1) AS T";
	private static final String SQL_SELECT_KEY_BATCH_BY_STATUS = "SELECT DISTINCT `" + COL_FILES_KEY + "` FROM " + TABLE_FILES + " WHERE " + COL_FILES_BUCKET_NAME + "=? AND " + COL_FILES_UPDATED_ON + " < ? AND " + COL_FILES_STATUS + "= ? LIMIT ?";
	private static final String SQL_UPDATE_STATUS_BY_KEY = "UPDATE " + TABLE_FILES + " SET " + COL_FILES_STATUS + "=?, " + COL_FILES_ETAG + "=UUID(), " + COL_FILES_UPDATED_ON + "=NOW() WHERE `" + COL_FILES_KEY + "`=? AND " + COL_FILES_UPDATED_ON + "<? AND " + COL_FILES_BUCKET_NAME + "=? AND " + COL_FILES_STATUS + "=?";
	private static final String SQL_COUNT_AVAILABLE_BY_KEY = "SELECT COUNT(*) FROM " + TABLE_FILES + " WHERE `" + COL_FILES_KEY + "`=? AND " + COL_FILES_BUCKET_NAME + "=?"
			+ " AND (" + COL_FILES_STATUS + "='" + FileHandleStatus.AVAILABLE.name() + "' OR ("+ COL_FILES_STATUS + "='" + FileHandleStatus.UNLINKED.name() + "' AND " + COL_FILES_UPDATED_ON + ">=?))";
	private static final String SQL_SELECT_PREVIEW_ID_BY_KEY_AND_STATUS = "SELECT DISTINCT " + COL_FILES_PREVIEW_ID + " FROM " + TABLE_FILES + " WHERE `" + COL_FILES_KEY + "`=? AND " + COL_FILES_BUCKET_NAME + "=? AND " + COL_FILES_STATUS + "=?";
	private static final String SQL_CLEAR_PREVIEW_ID_BY_KEY_AND_STATUS = "UPDATE " + TABLE_FILES + " SET " + COL_FILES_PREVIEW_ID + " = NULL WHERE `" + COL_FILES_KEY + "`=? AND " + COL_FILES_BUCKET_NAME + "=? AND " + COL_FILES_STATUS + "=?";
	private static final String SQL_SELECT_REFERENCED_PREVIEWS_IDS = "SELECT DISTINCT " + COL_FILES_PREVIEW_ID + " FROM " + TABLE_FILES + " WHERE " + COL_FILES_PREVIEW_ID + " IN (" +IDS_PARAM + ")";
	private static final String SQL_SELECT_BUCKET_AND_KEY = "SELECT DISTINCT " + COL_FILES_BUCKET_NAME + ", `" + COL_FILES_KEY + "` FROM " + TABLE_FILES + " WHERE " + COL_FILES_ID + " IN (" +IDS_PARAM + ")";
	private static final String SQL_DELETE_BATCH = "DELETE FROM " + TABLE_FILES + " WHERE " + COL_FILES_ID + " IN (" +IDS_PARAM + ")";
	private static final String SQL_DELETE_UNAVAILABLE_BY_KEY = "DELETE FROM " + TABLE_FILES + " WHERE `" + COL_FILES_KEY + "` =? AND " + COL_FILES_BUCKET_NAME + "=? AND " + COL_FILES_STATUS + " <> '" + FileHandleStatus.AVAILABLE +"'";
	private static final String SQL_SELECT_CONTENT_SIZE_BY_KEY = "SELECT MAX("+ COL_FILES_CONTENT_SIZE + ") FROM " + TABLE_FILES + " WHERE `" + COL_FILES_KEY + "` =? AND " + COL_FILES_BUCKET_NAME + "=?";
	
	/**
	 * Used to detect if a file object already exists.
	 */
	private static final String SQL_DOES_EXIST = "SELECT "+COL_FILES_ID+" FROM "+TABLE_FILES+" WHERE "+COL_FILES_ID+" = ?";

	private static final String SQL_COUNT_REFERENCES = "SELECT COUNT(*) FROM " + TABLE_FILES + " WHERE "
			+ COL_FILES_METADATA_TYPE + " = ? AND "
			+ COL_FILES_BUCKET_NAME + " = ? AND `"
			+ COL_FILES_KEY + "` = ?";
	
	private static final String SQL_IS_MATCHING_MD5 = "SELECT COUNT(S." + COL_FILES_ID + ") > 0"
			+ " FROM " + TABLE_FILES + " S JOIN " + TABLE_FILES + " T"
			+ " ON S." + COL_FILES_CONTENT_MD5 + " = T." + COL_FILES_CONTENT_MD5
			+ " WHERE S." + COL_FILES_ID + "= ?"
			+ " AND T." + COL_FILES_ID + "= ?";

	private TransactionalMessenger transactionalMessenger;
		
	private DBOBasicDao basicDao;

	private NamedParameterJdbcTemplate namedJdbcTemplate;
	
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	public DBOFileHandleDaoImpl(TransactionalMessenger transactionalMessenger, DBOBasicDao basicDao, NamedParameterJdbcTemplate namedJdbcTemplate) {
		this.transactionalMessenger = transactionalMessenger;
		this.basicDao = basicDao;
		this.namedJdbcTemplate = namedJdbcTemplate;
		this.jdbcTemplate = namedJdbcTemplate.getJdbcTemplate();
	}

	private static final TableMapping<DBOFileHandle> DBO_MAPPER = new DBOFileHandle().getTableMapping();
	
	private static final RowMapper<FileHandle> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
		return FileMetadataUtils.createDTOFromDBO(DBO_MAPPER.mapRow(rs, rowNum));
	};

	@Override
	public FileHandle get(String id) throws DatastoreException, NotFoundException {
		DBOFileHandle dbo = getDBO(id);
		return FileMetadataUtils.createDTOFromDBO(dbo);
	}

	DBOFileHandle getDBO(String id) throws NotFoundException {
		if (id == null)
			throw new IllegalArgumentException("Id cannot be null");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_FILES_ID.toLowerCase(), id);
		DBOFileHandle dbo = basicDao.getObjectByPrimaryKey(DBOFileHandle.class, param)
				.orElseThrow(() -> new NotFoundException(String.format("FileHandle: '%s' cannot be found", id)));
		return dbo;
	}

	@WriteTransaction
	@Override
	public void delete(String id) {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_FILES_ID.toLowerCase(), id);
		
		// Send the delete message
		transactionalMessenger.sendDeleteMessageAfterCommit(id, ObjectType.FILE);
		
		// Delete this object
		try{
			basicDao.deleteObjectByPrimaryKey(DBOFileHandle.class, param);
		}catch (DataIntegrityViolationException e){
			// This occurs when we try to delete a handle that is in use.
			throw new FileHandleLinkedException("Cannot delete a file handle that has been assigned to an owner object. FileHandle id: "+id, e);
		}
	}

	@WriteTransaction
	@Override
	public FileHandle createFile(FileHandle fileHandle) {
		createBatch(Collections.singletonList(fileHandle));
		return get(fileHandle.getId());
	}

	@WriteTransaction
	@Override
	public void setPreviewId(String fileId, String previewId) throws NotFoundException {
		if(fileId == null) throw new IllegalArgumentException("FileId cannot be null");
		if(!doesExist(fileId)){
			throw new NotFoundException("The fileId: "+fileId+" does not exist");
		}
		//if preview ID is set, then it must exist to continue update
		if(previewId != null && !doesExist(previewId)){
			throw new NotFoundException("The previewId: "+previewId+" does not exist");
		}
		try{
			// Set the isPreview field for the preview file handle
			String newEtag = UUID.randomUUID().toString();
			jdbcTemplate.update(UPDATE_MARK_FILE_AS_PREVIEW, true, newEtag, previewId);

			// Set the preview ID and change the etag for the original file;
			newEtag = UUID.randomUUID().toString();
			jdbcTemplate.update(UPDATE_PREVIEW_AND_ETAG, previewId, newEtag, fileId);

			// Send the update message
			transactionalMessenger.sendMessageAfterCommit(fileId, ObjectType.FILE, ChangeType.UPDATE);
			
		} catch (DataIntegrityViolationException e){
			throw new NotFoundException(e.getMessage());
		}
	}

	/**
	 * Does the given file object exist?
	 * @param id
	 * @return
	 */
	public boolean doesExist(String id){
		if(id == null) throw new IllegalArgumentException("FileId cannot be null");
		try{
			// Is this in the database.
			jdbcTemplate.queryForObject(SQL_DOES_EXIST, Long.class, id);
			return true;
		}catch(EmptyResultDataAccessException e){
			return false;
		}

	}

	@Override
	public String getHandleCreator(String fileHandleId) throws NotFoundException {
		if(fileHandleId == null) throw new IllegalArgumentException("fileHandleId cannot be null");
		try{
			// Lookup the creator.
			Long creator = jdbcTemplate.queryForObject(SQL_SELECT_CREATOR, Long.class, fileHandleId);
			return creator.toString();
		}catch(EmptyResultDataAccessException e){
			throw new NotFoundException("The FileHandle does not exist: "+fileHandleId);
		}
	}
	
	@Override
	public Set<String> getFileHandleIdsCreatedByUser(final Long createdById,
			List<String> fileHandleIds) throws NotFoundException {
		final Set<String> results = new HashSet<String>();
		for (List<String> fileHandleIdsBatch : Lists.partition(fileHandleIds, SqlConstants.MAX_LONGS_PER_IN_CLAUSE / 2)) {
			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("ids", fileHandleIdsBatch);
			parameters.addValue("createdById", createdById);
			namedJdbcTemplate.query(SQL_SELECT_FILES_CREATED_BY_USER, parameters, rs -> {
				String fileHandleId = rs.getString(COL_FILES_ID);
				results.add(fileHandleId);
			});
		}
		return results;
	}

	@Override
	public Map<String, String> getFileHandlePreviewIds(List<String> fileHandlePreviewIds) {
		if (fileHandlePreviewIds.isEmpty()) {
			return Collections.emptyMap();
		}
		final Map<String, String> results = new HashMap<String, String>();
		for (List<String> fileHandlePreviewIdsBatch : Lists.partition(fileHandlePreviewIds, SqlConstants.MAX_LONGS_PER_IN_CLAUSE / 2)) {
			MapSqlParameterSource parameters = new MapSqlParameterSource()
					.addValue("ids", fileHandlePreviewIdsBatch);
			namedJdbcTemplate.query(SQL_SELECT_FILE_PREVIEWS, parameters, rs -> {
				String fileHandlePreviewId = rs.getString(COL_FILES_PREVIEW_ID);
				String fileHandleId = rs.getString(COL_FILES_ID);
				results.put(fileHandlePreviewId, fileHandleId);
			});
		}
		return results;
	}
	
	@Override
	public Optional<String> getPreviewFileHandleId(String fileHandleId) throws NotFoundException {
		if(fileHandleId == null) throw new IllegalArgumentException("fileHandleId cannot be null");
		try{
			// Lookup the creator.
			Long previewId = jdbcTemplate.queryForObject(SQL_SELECT_PREVIEW_ID, Long.class, fileHandleId);
			
			if (previewId == null) {
				return Optional.empty();
			}
			
			return Optional.of(previewId.toString());
		} catch (EmptyResultDataAccessException e) {
			// This occurs when the file handle does not exist
			throw new NotFoundException("The FileHandle does not exist: " + fileHandleId);
		}
	}

	@Override
	public FileHandleResults getAllFileHandles(Iterable<String> ids, boolean includePreviews) throws DatastoreException, NotFoundException {
		List<FileHandle> handles = new LinkedList<FileHandle>();
		if(ids != null){
			for(String handleId: ids){
				// Look up each handle
				FileHandle handle = get(handleId);
				handles.add(handle);
				// If this handle has a preview then we fetch that as well.
				if(includePreviews && handle instanceof CloudProviderFileHandleInterface){
					String previewId = ((CloudProviderFileHandleInterface)handle).getPreviewId();
					if(previewId != null){
						FileHandle preview = get(previewId);
						handles.add(preview);
					}
				}
			}
		}
		FileHandleResults results = new FileHandleResults();
		results.setList(handles);
		return results;
	}

	@Override
	public Map<String, FileHandle> getAllFileHandlesBatch(Iterable<String> idsList) {
		Map<String, FileHandle> resultMap = new HashMap<>();

		// because we are using an IN clause and the number of incoming fileHandleIds is undetermined, we need to batch
		// the selects here
		for (List<String> fileHandleIdsBatch : Iterables.partition(idsList, 100)) {
			
			namedJdbcTemplate.query(SQL_SELECT_BATCH, new SinglePrimaryKeySqlParameterSource(fileHandleIdsBatch), ROW_MAPPER).forEach( handle -> {
				resultMap.put(handle.getId(), handle);
			});
			
		}
		return resultMap;
	}

	@Override
	public long getNumberOfReferencesToFile(FileHandleMetadataType metadataType, String bucketName, String key) {
		try {
			return jdbcTemplate.queryForObject(SQL_COUNT_REFERENCES, Long.class, metadataType.toString(), bucketName, key);
		} catch (NullPointerException e) {
			return 0L;
		}
	}

	@Override
	public long getCount() throws DatastoreException {
		return jdbcTemplate.queryForObject(SQL_COUNT_ALL_FILES, Long.class);
	}
	
	@Override
	public long getMaxId() throws DatastoreException {
		Long maxId = jdbcTemplate.queryForObject(SQL_MAX_FILE_ID, Long.class);
		
		if (maxId == null) {
			maxId = 0L;
		}
		
		return maxId;
	}

	@WriteTransaction
	@Override
	public void createBatch(List<FileHandle> list) {
		List<DBOFileHandle> dbos = FileMetadataUtils.createDBOsFromDTOs(list);
		createBatchDbo(dbos);
	}
	
	@WriteTransaction
	@Override
	public void createBatchDbo(List<DBOFileHandle> dbos) {
		for (DBOFileHandle dbo : dbos) {
			transactionalMessenger.sendMessageAfterCommit(dbo, ChangeType.CREATE);
		}
		basicDao.createBatch(dbos);
	}
	
	@Override
	public boolean isMatchingMD5(String sourceFileHandleId, String targetFileHandleId) {
		return jdbcTemplate.queryForObject(SQL_IS_MATCHING_MD5, Boolean.class, sourceFileHandleId, targetFileHandleId);
	}
	
	@Override
	public List<FileHandle> getFileHandlesBatchByStatus(List<Long> ids, FileHandleStatus status) {
		ValidateArgument.required(status, "The status");
		
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		
		String sql = SQL_SELECT_BATCH + " AND " + COL_FILES_STATUS + "=:" + COL_FILES_STATUS;
		
		MapSqlParameterSource paramSource = new MapSqlParameterSource()
				.addValue("ids", ids)
				.addValue(COL_FILES_STATUS, status.name());
		
		return namedJdbcTemplate.query(sql, paramSource, ROW_MAPPER);
	}
	
	@Override
	public List<DBOFileHandle> getDBOFileHandlesBatch(List<Long> ids, int updatedOnBeforeDays) {
		ValidateArgument.requirement(updatedOnBeforeDays >= 0, "The updatedOnBeforeDays must be greater or equal than 0");
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		
		StringBuilder sql = new StringBuilder(SQL_SELECT_BATCH);
		
		if (updatedOnBeforeDays > 0) {
			sql.append(" AND ").append(COL_FILES_UPDATED_ON).append(" < NOW() - INTERVAL ").append(updatedOnBeforeDays).append(" DAY");
		}
		
		MapSqlParameterSource paramSource = new MapSqlParameterSource("ids", ids);
		
		return namedJdbcTemplate.query(sql.toString(), paramSource, DBO_MAPPER);
	}
	
	@Override
	public List<String> getUnlinkedKeysForBucket(String bucketName, Instant modifiedBefore, int limit) {
		ValidateArgument.requiredNotBlank(bucketName, "The bucketName");
		ValidateArgument.required(modifiedBefore, "The modifiedBefore");
		ValidateArgument.requirement(limit > 0, "The limit must be greater than 0.");
		
		return jdbcTemplate.queryForList(SQL_SELECT_KEY_BATCH_BY_STATUS, String.class, bucketName, Timestamp.from(modifiedBefore), FileHandleStatus.UNLINKED.name(), limit);
	}
	
	@Override
	@WriteTransaction
	public List<Long> updateStatusForBatch(List<Long> ids, FileHandleStatus newStatus, FileHandleStatus currentStatus, int updatedOnBeforeDays) {
		ValidateArgument.required(newStatus, "The newStatus");
		ValidateArgument.required(currentStatus, "The currentStatus");
		ValidateArgument.requirement(updatedOnBeforeDays >= 0, "The updatedOnBeforeDays must be greater or equal than 0");
		
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		
		StringBuilder sql = new StringBuilder(SQL_UPDATE_STATUS_BATCH);
		
		if (updatedOnBeforeDays > 0) {
			sql.append(" AND ").append(COL_FILES_UPDATED_ON).append(" < NOW() - INTERVAL ").append(updatedOnBeforeDays).append(" DAY");
		}
		
		int[] updatedRows = jdbcTemplate.batchUpdate(sql.toString(), new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				int paramIndex = 1;
				ps.setString(paramIndex++, newStatus.name());
				ps.setLong(paramIndex++, ids.get(i));
				ps.setString(paramIndex++, currentStatus.name());
			}
			
			@Override
			public int getBatchSize() {
				return ids.size();
			}
		});
		
		List<Long> updatedIds = new ArrayList<>(ids.size());

		for (int i = 0; i < updatedRows.length; i++) {
			if (updatedRows[i] > 0) {
				updatedIds.add(ids.get(i));
			}
		}
		
		return updatedIds;
		
	}
	
	@Override
	@WriteTransaction
	public int updateStatusByBucketAndKey(String bucketName, String key, FileHandleStatus newStatus, FileHandleStatus currentStatus, Instant modifiedBefore) {
		ValidateArgument.requiredNotBlank(bucketName, "The bucketName");
		ValidateArgument.requiredNotBlank(key, "The key");
		ValidateArgument.required(newStatus, "The newStatus");
		ValidateArgument.required(currentStatus, "The currentStatus");
		ValidateArgument.required(modifiedBefore, "The modifiedBefore");
		
		int updated = 0;
		
		if (newStatus.equals(currentStatus)) {
			return updated;
		}

		return jdbcTemplate.update(SQL_UPDATE_STATUS_BY_KEY, newStatus.name(), key, Timestamp.from(modifiedBefore), bucketName, currentStatus.name());
	}
	
	@Override
	public int getAvailableOrEarlyUnlinkedFileHandlesCount(String bucketName, String key, Instant modifiedAfter) {
		ValidateArgument.requiredNotBlank(bucketName, "The bucketName");
		ValidateArgument.requiredNotBlank(key, "The key");
		ValidateArgument.required(modifiedAfter, "The modifiedAfter");
		
		return jdbcTemplate.queryForObject(SQL_COUNT_AVAILABLE_BY_KEY, Long.class, key, bucketName, Timestamp.from(modifiedAfter)).intValue();
	}
	
	@Override
	@WriteTransaction
	public Set<Long> clearPreviewByKeyAndStatus(String bucketName, String key, FileHandleStatus status) {
		ValidateArgument.requiredNotBlank(bucketName, "The bucketName");
		ValidateArgument.requiredNotBlank(key, "The key");
		ValidateArgument.required(status, "The status");
		
		List<Long> previewIds = jdbcTemplate.queryForList(SQL_SELECT_PREVIEW_ID_BY_KEY_AND_STATUS, Long.class, key, bucketName, status.name());
		
		jdbcTemplate.update(SQL_CLEAR_PREVIEW_ID_BY_KEY_AND_STATUS, key, bucketName, status.name());
		
		return new HashSet<>(previewIds);
	}
	
	@Override
	public Set<Long> getReferencedPreviews(Set<Long> previewIds) {
		ValidateArgument.required(previewIds, "The previewIds");
		
		if (previewIds.isEmpty()) {
			return Collections.emptySet();
		}
		
		SqlParameterSource params = new MapSqlParameterSource("ids", previewIds);
		
		List<Long> idList = namedJdbcTemplate.queryForList(SQL_SELECT_REFERENCED_PREVIEWS_IDS, params, Long.class);
		
		return new HashSet<>(idList);
	}
	
	@Override
	public boolean hasStatusBatch(List<Long> ids, FileHandleStatus status) {
		ValidateArgument.required(ids, "The ids batch");
		ValidateArgument.required(status, "The status");
		
		if (ids.isEmpty()) {
			return false;
		}
		
		SqlParameterSource params = new MapSqlParameterSource()
				.addValue("ids", ids)
				.addValue(COL_FILES_STATUS, status.name());
		
		return namedJdbcTemplate.queryForObject(SQL_CHECK_BATCH_STATUS, params, Long.class) > 0;
	}
	
	@Override
	public Set<BucketAndKey> getBucketAndKeyBatch(Set<Long> ids) {
		ValidateArgument.required(ids, "The id set");
		if (ids.isEmpty()) {
			return Collections.emptySet();
		}
		
		SqlParameterSource params = new MapSqlParameterSource("ids", ids);
		
		Set<BucketAndKey> result = new HashSet<>();
		
		namedJdbcTemplate.query(SQL_SELECT_BUCKET_AND_KEY, params, (RowCallbackHandler) rs -> {
            String bucket = rs.getString(1);
            String key = rs.getString(2);
            result.add(new BucketAndKey().withBucket(bucket).withtKey(key));
		});
		
		return result;
	}
	
	@Override
	@WriteTransaction
	public void deleteBatch(Set<Long> ids) {
		ValidateArgument.required(ids, "The id set");
		
		if (ids.isEmpty()) {
			return;
		}
		
		SqlParameterSource params = new MapSqlParameterSource("ids", ids);
		
		namedJdbcTemplate.update(SQL_DELETE_BATCH, params);
	}
	
	@Override
	@WriteTransaction
	public void deleteUnavailableByBucketAndKey(String bucketName, String key) {
		ValidateArgument.requiredNotBlank(bucketName, "The bucketName");
		ValidateArgument.requiredNotBlank(key, "The key");
		
		jdbcTemplate.update(SQL_DELETE_UNAVAILABLE_BY_KEY, key, bucketName);
	}
	
	@Override
	public Long getContentSizeByKey(String bucketName, String key) {
		ValidateArgument.requiredNotBlank(bucketName, "The bucketName");
		ValidateArgument.requiredNotBlank(key, "The key");
		
		return jdbcTemplate.queryForObject(SQL_SELECT_CONTENT_SIZE_BY_KEY, Long.class, key, bucketName);
	}

	@WriteTransaction
	@Override
	public void truncateTable() {
		try {
			jdbcTemplate.update("SET FOREIGN_KEY_CHECKS = ?", false);
			jdbcTemplate.update("DELETE FROM "+TABLE_FILES+" WHERE "+COL_FILES_ID+" > -1");
		}finally {
			jdbcTemplate.update("SET FOREIGN_KEY_CHECKS = ?", true);
		}
	}

}
