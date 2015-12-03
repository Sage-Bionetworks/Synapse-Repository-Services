package org.sagebionetworks.repo.model.dbo.file;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_REQUEST_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_STARTED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MULTIPART_UPLOAD;

import org.sagebionetworks.repo.model.file.Multipart.State;

import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class MultipartUploadDAOImpl implements MultipartUploadDAO {
	
	private static final String SELECT_BY_USER_AND_HASH = "SELECT * FROM "+TABLE_MULTIPART_UPLOAD+" WHERE "+COL_MULTIPART_STARTED_BY+" = ? AND "+COL_MULTIPART_REQUEST_HASH+" = ?";

	private static final String SELECT_BY_ID = "SELECT * FROM "+TABLE_MULTIPART_UPLOAD+" WHERE "+COL_MULTIPART_UPLOAD_ID+" = ?";

	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	RowMapper<DBOMultipartUpload> rowMapper = new DBOMultipartUpload().getTableMapping();

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO#getUploadStatus(long, java.lang.String)
	 */
	@Override
	public MultipartUploadStatus getUploadStatus(long userId, String hash) {
		if(hash == null){
			throw new IllegalArgumentException("Hash cannot be null");
		}
		try {
			DBOMultipartUpload dbo = this.jdbcTemplate.queryForObject(SELECT_BY_USER_AND_HASH, rowMapper, userId, hash);
			return toDTO(dbo);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO#getUploadStatus(java.lang.String)
	 */
	@Override
	public MultipartUploadStatus getUploadStatus(String idString) {
		if(idString == null){
			throw new IllegalArgumentException("UploadId cannot be null");
		}
		try {
			long id = Long.parseLong(idString);
			return getUploadStatus(id);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid UploadId.");
		}

	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO#deleteUploadStatus(long, java.lang.String)
	 */
	@WriteTransactionReadCommitted
	@Override
	public void deleteUploadStatus(long userId, String hash) {
		if(hash == null){
			throw new IllegalArgumentException("Hash cannot be null");
		}
		this.jdbcTemplate.update("DELETE FROM "+TABLE_MULTIPART_UPLOAD+" WHERE "+COL_MULTIPART_STARTED_BY+" = ? AND "+COL_MULTIPART_REQUEST_HASH+" = ?", userId, hash);

	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO#createUploadStatus(long, java.lang.String, org.sagebionetworks.repo.model.file.MultipartUploadRequest)
	 */
	@WriteTransactionReadCommitted
	@Override
	public MultipartUploadStatus createUploadStatus(long userId, String hash, String providerId,
			MultipartUploadRequest request) {
		DBOMultipartUpload dbo = new DBOMultipartUpload();
		dbo.setId(idGenerator.generateNewId(TYPE.MULTIPART_UPLOAD_ID));
		dbo.setEtag(UUID.randomUUID().toString());
		dbo.setRequestHash(hash);
		dbo.setProviderUploadId(providerId);
		dbo.setStartedBy(userId);
		dbo.setFileName(request.getFileName());
		dbo.setFileMD5Hex(request.getFileName());
		dbo.setPartSize(request.getPartSizeBytes());
		dbo.setFileSize(request.getFileSizeBytes());
		dbo.setFileMD5Hex(request.getMd5Hex());
		dbo.setState(State.UPLOADING.name());
		dbo.setStorageLocationId(request.getStorageLocationId());
		basicDao.createNew(dbo);
		return getUploadStatus(dbo.getId());
	}
	
	public MultipartUploadStatus getUploadStatus(long id) {
		try {
			DBOMultipartUpload dbo = this.jdbcTemplate.queryForObject(SELECT_BY_ID, rowMapper, id);
			return toDTO(dbo);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("MultipartUploadStatus cannot be found for id: "+id);
		}
	}
	
	/**
	 * Convert the dbo to a dto.
	 * @param dbo
	 * @return
	 */
	public static MultipartUploadStatus toDTO(DBOMultipartUpload dbo){
		MultipartUploadStatus dto = new MultipartUploadStatus();
		if(dbo.getFileHandleId() != null){
			dto.setResultFileHandleId(dbo.getFileHandleId().toString());
		}
		dto.setStartedBy(dbo.getStartedBy().toString());
		dto.setStartedOn(dbo.getStartedOn());
		dto.setState(State.valueOf(dbo.getState()));
		dto.setUpdatedOn(dbo.getUpdatedOn());
		dto.setUploadId(dbo.getId().toString());
		return dto;
	}

}
