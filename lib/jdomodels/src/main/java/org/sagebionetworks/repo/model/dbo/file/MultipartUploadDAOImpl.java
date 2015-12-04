package org.sagebionetworks.repo.model.dbo.file;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.Multipart.State;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class MultipartUploadDAOImpl implements MultipartUploadDAO {

	private static final String SQL_TRUNCATE_ALL = "DELETE FROM "
			+ TABLE_MULTIPART_UPLOAD + " WHERE " + COL_MULTIPART_UPLOAD_ID
			+ " > -1";

	private static final String STATUS_SELECT = COL_MULTIPART_UPLOAD_ID + ","
			+ COL_MULTIPART_STARTED_BY + "," + COL_MULTIPART_STARTED_ON + ","
			+ COL_MULTIPART_UPDATED_ON + "," + COL_MULTIPART_FILE_HANDLE_ID
			+ "," + COL_MULTIPART_STATE + "," + COL_MULTIPART_UPLOAD_TOKEN
			+ "," + COL_MULTIPART_BUCKET + "," + COL_MULTIPART_KEY;

	private static final String SELECT_BY_ID = "SELECT " + STATUS_SELECT
			+ " FROM " + TABLE_MULTIPART_UPLOAD + " WHERE "
			+ COL_MULTIPART_UPLOAD_ID + " = ?";

	private static final String SELECT_BY_USER_AND_HASH = "SELECT "
			+ STATUS_SELECT + " FROM " + TABLE_MULTIPART_UPLOAD + " WHERE "
			+ COL_MULTIPART_STARTED_BY + " = ? AND "
			+ COL_MULTIPART_REQUEST_HASH + " = ?";

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DBOBasicDao basicDao;

	RowMapper<CompositeMultipartUploadStatus> statusMapper = new RowMapper<CompositeMultipartUploadStatus>() {
		@Override
		public CompositeMultipartUploadStatus mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			CompositeMultipartUploadStatus dto = new CompositeMultipartUploadStatus();
			MultipartUploadStatus mus = new MultipartUploadStatus();
			mus.setUploadId(rs.getString(COL_MULTIPART_UPLOAD_ID));
			mus.setStartedBy(rs.getString(COL_MULTIPART_STARTED_BY));
			mus.setStartedOn(rs.getDate(COL_MULTIPART_STARTED_ON));
			mus.setUpdatedOn(rs.getDate(COL_MULTIPART_UPDATED_ON));
			mus.setResultFileHandleId(rs
					.getString(COL_MULTIPART_FILE_HANDLE_ID));
			mus.setState(State.valueOf(rs.getString(COL_MULTIPART_STATE)));
			dto.setMultipartUploadStatus(mus);
			dto.setUploadToken(rs.getString(COL_MULTIPART_UPLOAD_TOKEN));
			dto.setBucket(rs.getString(COL_MULTIPART_BUCKET));
			dto.setKey(rs.getString(COL_MULTIPART_KEY));
			return dto;
		}
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO#getUploadStatus
	 * (long, java.lang.String)
	 */
	@Override
	public CompositeMultipartUploadStatus getUploadStatus(Long userId,
			String hash) {
		ValidateArgument.required(userId, "UserId");
		ValidateArgument.required(hash, "RequestHash");
		try {
			return this.jdbcTemplate.queryForObject(SELECT_BY_USER_AND_HASH,
					statusMapper, userId, hash);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO#getUploadStatus
	 * (java.lang.String)
	 */
	@Override
	public CompositeMultipartUploadStatus getUploadStatus(String idString) {
		ValidateArgument.required(idString, "UploadId");
		try {
			long id = Long.parseLong(idString);
			return getUploadStatus(id);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid UploadId.");
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO#deleteUploadStatus
	 * (long, java.lang.String)
	 */
	@WriteTransactionReadCommitted
	@Override
	public void deleteUploadStatus(long userId, String hash) {
		ValidateArgument.required(userId, "UserId");
		ValidateArgument.required(hash, "RequestHash");
		this.jdbcTemplate.update("DELETE FROM " + TABLE_MULTIPART_UPLOAD
				+ " WHERE " + COL_MULTIPART_STARTED_BY + " = ? AND "
				+ COL_MULTIPART_REQUEST_HASH + " = ?", userId, hash);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO#createUploadStatus
	 * (long, java.lang.String,
	 * org.sagebionetworks.repo.model.file.MultipartUploadRequest)
	 */
	@WriteTransactionReadCommitted
	@Override
	public CompositeMultipartUploadStatus createUploadStatus(
			CreateMultipartRequest createRequest) {
		ValidateArgument.required(createRequest, "CreateMultipartRequest");
		ValidateArgument.required(createRequest.getUserId(), "UserId");
		ValidateArgument.required(createRequest.getHash(), "RequestHash");
		ValidateArgument
				.required(createRequest.getUploadToken(), "UploadToken");
		ValidateArgument.required(createRequest.getBucket(), "Bucket");
		ValidateArgument.required(createRequest.getKey(), "Key");

		DBOMultipartUpload dbo = new DBOMultipartUpload();
		dbo.setId(idGenerator.generateNewId(TYPE.MULTIPART_UPLOAD_ID));
		dbo.setEtag(UUID.randomUUID().toString());
		dbo.setRequestHash(createRequest.getHash());
		dbo.setStartedBy(createRequest.getUserId());
		dbo.setState(State.UPLOADING.name());
		dbo.setRequestBlob(extractBytes(createRequest.getRequestBody()));
		dbo.setStartedOn(new Date(System.currentTimeMillis()));
		dbo.setUpdatedOn(dbo.getStartedOn());
		dbo.setUploadToken(createRequest.getUploadToken());
		dbo.setBucket(createRequest.getBucket());
		dbo.setKey(createRequest.getKey());
		basicDao.createNew(dbo);
		return getUploadStatus(dbo.getId());
	}

	private byte[] extractBytes(String requestBody) {
		ValidateArgument.required(requestBody, "RequestBody");
		try {
			return requestBody.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the upload status given an upload id.
	 * 
	 * @param id
	 * @return
	 */
	private CompositeMultipartUploadStatus getUploadStatus(long id) {
		try {
			return this.jdbcTemplate.queryForObject(SELECT_BY_ID, statusMapper,
					id);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(
					"MultipartUploadStatus cannot be found for id: " + id);
		}
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update(SQL_TRUNCATE_ALL);
	}

}
