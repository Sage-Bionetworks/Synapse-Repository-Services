package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_BUCKET_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_IS_PREVIEW;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_METADATA_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_PREVIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_STORAGE_LOCATION_ID;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.FileMetadataUtils;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

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
	private static final String SQL_SELECT_CREATORS = "SELECT " + COL_FILES_CREATED_BY + "," + COL_FILES_ID + " FROM " + TABLE_FILES
			+ " WHERE " + COL_FILES_ID + " IN ( " + IDS_PARAM + " )";
	private static final String SQL_SELECT_BATCH = "SELECT * FROM " + TABLE_FILES + " WHERE " + COL_FILES_ID + " IN ( " + IDS_PARAM + " )";
	private static final String SQL_SELECT_PREVIEW_ID = "SELECT "+COL_FILES_PREVIEW_ID+" FROM "+TABLE_FILES+" WHERE "+COL_FILES_ID+" = ?";
	private static final String UPDATE_PREVIEW_AND_ETAG = "UPDATE "+TABLE_FILES+" SET "+COL_FILES_PREVIEW_ID+" = ? ,"+COL_FILES_ETAG+" = ? WHERE "+COL_FILES_ID+" = ?";
	private static final String UPDATE_MARK_FILE_AS_PREVIEW  = "UPDATE "+TABLE_FILES+" SET "+COL_FILES_IS_PREVIEW+" = ? ,"+COL_FILES_ETAG+" = ? WHERE "+COL_FILES_ID+" = ?";
	private static final String UPDATE_STORAGE_LOCATION_ID_BATCH = "UPDATE " + TABLE_FILES + " SET " + COL_FILES_STORAGE_LOCATION_ID + " = :id, " 
			+ COL_FILES_ETAG + " = UUID() WHERE " + COL_FILES_STORAGE_LOCATION_ID + " IN ( " + IDS_PARAM + " )";

	/**
	 * Used to detect if a file object already exists.
	 */
	private static final String SQL_DOES_EXIST = "SELECT "+COL_FILES_ID+" FROM "+TABLE_FILES+" WHERE "+COL_FILES_ID+" = ?";

	private static final String SQL_COUNT_REFERENCES = "SELECT COUNT(*) FROM " + TABLE_FILES + " WHERE "
			+ COL_FILES_METADATA_TYPE + " = ? AND "
			+ COL_FILES_BUCKET_NAME + " = ? AND `"
			+ COL_FILES_KEY + "` = ?";
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;
		
	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	private TableMapping<DBOFileHandle> rowMapping = new DBOFileHandle().getTableMapping();

	@Override
	public FileHandle get(String id) throws DatastoreException, NotFoundException {
		DBOFileHandle dbo = getDBO(id);
		return FileMetadataUtils.createDTOFromDBO(dbo);
	}

	private DBOFileHandle getDBO(String id) throws NotFoundException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_FILES_ID.toLowerCase(), id);
		DBOFileHandle dbo = basicDao.getObjectByPrimaryKey(DBOFileHandle.class, param);
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
			throw new DataIntegrityViolationException("Cannot delete a file handle that has been assigned to an owner object. FileHandle id: "+id, e);
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
			transactionalMessenger.sendMessageAfterCommit(fileId, ObjectType.FILE, newEtag, ChangeType.UPDATE);
			
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

	@Deprecated
	@Override
	public Multimap<String, String> getHandleCreators(List<String> fileHandleIds) throws NotFoundException {
		final Multimap<String, String> resultMap = ArrayListMultimap.create();

		// because we are using an IN clause and the number of incoming fileHandleIds is undetermined, we need to batch
		// the selects here
		for (List<String> fileHandleIdsBatch : Lists.partition(fileHandleIds, SqlConstants.MAX_LONGS_PER_IN_CLAUSE / 2)) {
			namedJdbcTemplate.query(SQL_SELECT_CREATORS, new SinglePrimaryKeySqlParameterSource(fileHandleIdsBatch),
					rs -> {
						String creator = rs.getString(COL_FILES_CREATED_BY);
						String id = rs.getString(COL_FILES_ID);
						resultMap.put(creator, id);
					});
		}
		return resultMap;
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
	public String getPreviewFileHandleId(String fileHandleId)
			throws NotFoundException {
		if(fileHandleId == null) throw new IllegalArgumentException("fileHandleId cannot be null");
		try{
			// Lookup the creator.
			long previewId = jdbcTemplate.queryForObject(SQL_SELECT_PREVIEW_ID, Long.class, fileHandleId);
			if(previewId > 0){
				return Long.toString(previewId);
			}else{
				throw new NotFoundException("A preview does not exist for: "+fileHandleId);
			}
		}catch(EmptyResultDataAccessException | NullPointerException e){
			// This occurs when the file handle does not exist
			throw new NotFoundException("The FileHandle does not exist: "+fileHandleId);
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
		Map<String, FileHandle> resultMap = Maps.newHashMap();

		// because we are using an IN clause and the number of incoming fileHandleIds is undetermined, we need to batch
		// the selects here
		for (List<String> fileHandleIdsBatch : Iterables.partition(idsList, 100)) {
			List<DBOFileHandle> handles = namedJdbcTemplate.query(SQL_SELECT_BATCH,
					new SinglePrimaryKeySqlParameterSource(fileHandleIdsBatch), rowMapping);
			for (DBOFileHandle handle : handles) {
				resultMap.put(handle.getIdString(), FileMetadataUtils.createDTOFromDBO(handle));
			}
		}
		return resultMap;
	}

	@Override
	public long getNumberOfReferencesToFile(String metadataType, String bucketName, String key) {
		try {
			return jdbcTemplate.queryForObject(SQL_COUNT_REFERENCES, Long.class, metadataType, bucketName, key);
		} catch (NullPointerException e) {
			return 0L;
		}
	}

	@Override
	public long getCount() throws DatastoreException {
		try {
			return jdbcTemplate.queryForObject(SQL_COUNT_ALL_FILES, Long.class);
		} catch (NullPointerException e) {
			return 0L;
		}
	}
	
	@Override
	public long getMaxId() throws DatastoreException {
		try {
			return jdbcTemplate.queryForObject(SQL_MAX_FILE_ID, Long.class);
		} catch (NullPointerException e) {
			return 0L;
		}
	}

	@WriteTransaction
	@Override
	public void createBatch(List<FileHandle> list) {
		List<DBOFileHandle> dbos = FileMetadataUtils.createDBOsFromDTOs(list);
		for (DBOFileHandle dbo : dbos) {
			transactionalMessenger.sendMessageAfterCommit(dbo, ChangeType.CREATE);
		}
		basicDao.createBatch(dbos);
	}

	@WriteTransaction
	@Override
	public void truncateTable() {
		jdbcTemplate.update("DELETE FROM "+TABLE_FILES+" WHERE "+COL_FILES_ID+" > -1");
	}
	
	@WriteTransaction
	@Override
	public void updateStorageLocationBatch(Set<Long> currentStorageLocationIds, Long targetStorageLocationId) {
		for (List<Long> locationIdsBatch : Iterables.partition(currentStorageLocationIds, SqlConstants.MAX_LONGS_PER_IN_CLAUSE / 2)) {
			MapSqlParameterSource parameters = new MapSqlParameterSource()
					.addValue("id", targetStorageLocationId)
					.addValue("ids", locationIdsBatch);
			namedJdbcTemplate.update(UPDATE_STORAGE_LOCATION_ID_BATCH, parameters);
		}
	}

}
