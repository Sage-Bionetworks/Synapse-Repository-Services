package org.sagebionetworks.repo.model.dbo.file;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_BUCKET_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CONTENT_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_BUCKET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_NUMBER_OF_PARTS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_PART_ERROR_DETAILS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_PART_MD5_HEX;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_PART_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_PART_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_PART_UPLOAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_REQUEST_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_REQUEST_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_SOURCE_FILE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_SOURCE_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_STARTED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_STARTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_REQUEST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MULTIPART_UPLOAD;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MULTIPART_UPLOAD_PART_STATE;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.file.MultipartUploadState;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.PartErrors;
import org.sagebionetworks.repo.model.file.PartMD5;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MultipartUploadDAOImpl implements MultipartUploadDAO {

	private static final String SQL_DELETE_BY_ID = "DELETE FROM " + TABLE_MULTIPART_UPLOAD
			+ " WHERE " + COL_MULTIPART_UPLOAD_ID + " = ?";

	private static final String SQL_DELETE_ALL_PARTS = "DELETE FROM " + TABLE_MULTIPART_UPLOAD_PART_STATE + " WHERE "
			+ COL_MULTIPART_PART_UPLOAD_ID + " = ?";

	private static final String SQL_SET_COMPLETE = "UPDATE " + TABLE_MULTIPART_UPLOAD + " SET "
			+ COL_MULTIPART_FILE_HANDLE_ID + " = ? , " + COL_MULTIPART_UPLOAD_ETAG + " = ? , " + COL_MULTIPART_STATE + " = ?, " + COL_MULTIPART_UPDATED_ON + " = ?" 
			+ " WHERE " + COL_MULTIPART_UPLOAD_ID + " = ?";

	private static final String SQL_SELECT_BLOB = "SELECT " + COL_MULTIPART_UPLOAD_REQUEST + " FROM "
			+ TABLE_MULTIPART_UPLOAD + " WHERE " + COL_MULTIPART_UPLOAD_ID + " = ?";

	private static final String SQL_UPDATE_ETAG = "UPDATE " + TABLE_MULTIPART_UPLOAD + " SET "
			+ COL_MULTIPART_UPLOAD_ETAG + " = ?, " + COL_MULTIPART_UPDATED_ON + " = ?"
			+ " WHERE " + COL_MULTIPART_UPLOAD_ID + " = ?";

	private static final String SQL_SELECT_ADDED_PART_NUMBERS = "SELECT " + COL_MULTIPART_PART_NUMBER + " FROM "
			+ TABLE_MULTIPART_UPLOAD_PART_STATE + " WHERE " + COL_MULTIPART_PART_UPLOAD_ID + " = ? AND "
			+ COL_MULTIPART_PART_MD5_HEX + " IS NOT NULL";

	private static final String SQL_SELECT_PART_MD5S = "SELECT " + COL_MULTIPART_PART_NUMBER + ", "
			+ COL_MULTIPART_PART_MD5_HEX + " FROM " + TABLE_MULTIPART_UPLOAD_PART_STATE + " WHERE "
			+ COL_MULTIPART_PART_UPLOAD_ID + " = ? AND " + COL_MULTIPART_PART_MD5_HEX + " IS NOT NULL ORDER BY "
			+ COL_MULTIPART_PART_NUMBER + " ASC";

	private static final String SQL_SELECT_PART_ERROR = "SELECT " + COL_MULTIPART_PART_NUMBER + ", "
			+ COL_MULTIPART_PART_ERROR_DETAILS + " FROM " + TABLE_MULTIPART_UPLOAD_PART_STATE + " WHERE "
			+ COL_MULTIPART_PART_UPLOAD_ID + " = ? AND " + COL_MULTIPART_PART_ERROR_DETAILS + " IS NOT NULL ORDER BY "
			+ COL_MULTIPART_PART_NUMBER + " ASC";

	private static final String SQL_TRUNCATE_ALL = "DELETE FROM " + TABLE_MULTIPART_UPLOAD + " WHERE "
			+ COL_MULTIPART_UPLOAD_ID + " > -1";

	private static final String STATUS_SELECT = "U." + COL_MULTIPART_UPLOAD_ID + ",U." + COL_MULTIPART_STARTED_BY + ",U."
			+ COL_MULTIPART_STARTED_ON + ",U." + COL_MULTIPART_UPDATED_ON + ",U." + COL_MULTIPART_FILE_HANDLE_ID + ",U."
			+ COL_MULTIPART_SOURCE_FILE_ETAG + ",U." + COL_MULTIPART_STATE + ",U." + COL_MULTIPART_UPLOAD_TOKEN + ",U."
			+ COL_MULTIPART_UPLOAD_TYPE + ",U." + COL_MULTIPART_BUCKET + ",U." + COL_MULTIPART_KEY + ",U."
			+ COL_MULTIPART_NUMBER_OF_PARTS + ",U." + COL_MULTIPART_UPLOAD_ETAG + ",U." + COL_MULTIPART_REQUEST_TYPE + ",U."
			+ COL_MULTIPART_PART_SIZE + ",U." + COL_MULTIPART_SOURCE_FILE_HANDLE_ID + ",F." + COL_FILES_BUCKET_NAME 
			+ ",F." + COL_FILES_KEY + ",F."+ COL_FILES_CONTENT_SIZE;
	
	private static final String LEFT_JOIN_ON_SOURCE_FILE = " U LEFT JOIN " + TABLE_FILES + " F "
			+ "ON U." + COL_MULTIPART_SOURCE_FILE_HANDLE_ID + " = F." + COL_FILES_ID;

	private static final String SELECT_BY_ID = "SELECT " + STATUS_SELECT 
			+ " FROM " + TABLE_MULTIPART_UPLOAD + LEFT_JOIN_ON_SOURCE_FILE
			+ " WHERE U." + COL_MULTIPART_UPLOAD_ID + " = ?";

	private static final String SELECT_BY_USER_AND_HASH = "SELECT " + STATUS_SELECT 
			+ " FROM " + TABLE_MULTIPART_UPLOAD + LEFT_JOIN_ON_SOURCE_FILE
			+ " WHERE U." + COL_MULTIPART_STARTED_BY + " = ? AND U." + COL_MULTIPART_REQUEST_HASH + " = ?";
	
	private static final String SQL_UPDATE_UPLOAD_HASH_BY_USER_ID_AND_HASH = "UPDATE " + TABLE_MULTIPART_UPLOAD + " SET "
			+ COL_MULTIPART_REQUEST_HASH + " = ? , " + COL_MULTIPART_UPLOAD_ETAG + " = ?, " + COL_MULTIPART_UPDATED_ON + " = ?" 
			+ " WHERE " + COL_MULTIPART_STARTED_BY + " = ? AND " + COL_MULTIPART_REQUEST_HASH + " = ?";
	
	private static final String SQL_SELECT_BATCH_OLDER_THAN = "SELECT " + COL_MULTIPART_UPLOAD_ID + " FROM " + TABLE_MULTIPART_UPLOAD
			+ " WHERE " + COL_MULTIPART_UPDATED_ON + " < (NOW() - INTERVAL ? DAY) ORDER BY " + COL_MULTIPART_UPDATED_ON + " LIMIT ?";
	
	private static final String SQL_SELECT_BATCH = "SELECT " + COL_MULTIPART_UPLOAD_ID + " FROM " + TABLE_MULTIPART_UPLOAD
			+ " ORDER BY " + COL_MULTIPART_UPDATED_ON + " LIMIT ?";

	private static final RowMapper<CompositeMultipartUploadStatus> STATUS_MAPPER = (rs, rowNum) -> {
		CompositeMultipartUploadStatus dto = new CompositeMultipartUploadStatus();
		MultipartUploadStatus mus = new MultipartUploadStatus();
		mus.setUploadId(rs.getString(COL_MULTIPART_UPLOAD_ID));
		mus.setStartedBy(rs.getString(COL_MULTIPART_STARTED_BY));
		mus.setStartedOn(new Date(rs.getTimestamp(COL_MULTIPART_STARTED_ON).getTime()));
		mus.setUpdatedOn(new Date(rs.getTimestamp(COL_MULTIPART_UPDATED_ON).getTime()));
		mus.setResultFileHandleId(rs.getString(COL_MULTIPART_FILE_HANDLE_ID));
		mus.setState(MultipartUploadState.valueOf(rs.getString(COL_MULTIPART_STATE)));
		dto.setMultipartUploadStatus(mus);
		dto.setUploadType(UploadType.valueOf(rs.getString(COL_MULTIPART_UPLOAD_TYPE)));
		dto.setUploadToken(rs.getString(COL_MULTIPART_UPLOAD_TOKEN));
		dto.setBucket(rs.getString(COL_MULTIPART_BUCKET));
		dto.setKey(rs.getString(COL_MULTIPART_KEY));
		dto.setNumberOfParts((int) rs.getLong(COL_MULTIPART_NUMBER_OF_PARTS));
		dto.setEtag(rs.getString(COL_MULTIPART_UPLOAD_ETAG));
		dto.setRequestType(MultiPartRequestType.valueOf(rs.getString(COL_MULTIPART_REQUEST_TYPE)));
		dto.setPartSize(rs.getLong(COL_MULTIPART_PART_SIZE));
		
		// The following are pulled in from the source file handle id
		dto.setSourceFileHandleId(rs.getLong(COL_MULTIPART_SOURCE_FILE_HANDLE_ID));
		
		if (rs.wasNull()) {
			dto.setSourceFileHandleId(null);
		} else {
			dto.setSourceFileEtag(rs.getString(COL_MULTIPART_SOURCE_FILE_ETAG));
			dto.setSourceFileSize(rs.getLong(COL_FILES_CONTENT_SIZE));
			dto.setSourceBucket(rs.getString(COL_FILES_BUCKET_NAME));
			dto.setSourceKey(rs.getString(COL_FILES_KEY));
		}
		
		return dto;
	};

	private static final RowMapper<String> REQUEST_MAPPER = (rs, rowNum) -> {
		Blob blob = rs.getBlob(COL_MULTIPART_UPLOAD_REQUEST);
		byte[] bytes = blob.getBytes(1, (int) blob.length());
		return new String(bytes, StandardCharsets.UTF_8);
	};

	private static final RowMapper<PartErrors> PART_ERRORS_MAPPER = (rs, rowNum) -> {
		int partNumber = rs.getInt(COL_MULTIPART_PART_NUMBER);
		Blob blob = rs.getBlob(COL_MULTIPART_PART_ERROR_DETAILS);
		String error = new String(blob.getBytes(1, (int) blob.length()), StandardCharsets.UTF_8);
		return new PartErrors(partNumber, error);
	};

	private static final RowMapper<PartMD5> PART_MD5_MAPPER = (rs, rowNum) -> {
		int partNumber = rs.getInt(COL_MULTIPART_PART_NUMBER);
		String partMD5Hex = rs.getString(COL_MULTIPART_PART_MD5_HEX);
		return new PartMD5(partNumber, partMD5Hex);
	};

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DBOBasicDao basicDao;

	@Override
	public CompositeMultipartUploadStatus getUploadStatus(Long userId, String hash) {
		ValidateArgument.required(userId, "UserId");
		ValidateArgument.required(hash, "RequestHash");
		try {
			return this.jdbcTemplate.queryForObject(SELECT_BY_USER_AND_HASH, STATUS_MAPPER, userId, hash);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO#getUploadStatus (java.lang.String)
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

	@Override
	@WriteTransaction
	public void deleteUploadStatus(String uploadId) {
		ValidateArgument.required(uploadId, "The uploadId");
		jdbcTemplate.update(SQL_DELETE_BY_ID, uploadId);
	}
	
	@Override
	@WriteTransaction
	public void setUploadStatusHash(long userId, String oldHash, String newHash) {
		ValidateArgument.required(userId, "The userId");
		ValidateArgument.required(oldHash, "The oldHash");
		ValidateArgument.required(oldHash, "The newHash");
		String newEtag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_UPDATE_UPLOAD_HASH_BY_USER_ID_AND_HASH, newHash, newEtag, new Date(), userId, oldHash);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO#createUploadStatus (long, java.lang.String,
	 * org.sagebionetworks.repo.model.file.MultipartUploadRequest)
	 */
	@WriteTransaction
	@Override
	public CompositeMultipartUploadStatus createUploadStatus(CreateMultipartRequest createRequest) {
		ValidateArgument.required(createRequest, "CreateMultipartRequest");
		ValidateArgument.required(createRequest.getUserId(), "UserId");
		ValidateArgument.required(createRequest.getHash(), "RequestHash");
		ValidateArgument.required(createRequest.getUploadToken(), "UploadToken");
		ValidateArgument.required(createRequest.getBucket(), "Bucket");
		ValidateArgument.required(createRequest.getKey(), "Key");
		ValidateArgument.required(createRequest.getNumberOfParts(), "NumberOfParts");
		ValidateArgument.required(createRequest.getPartSize(), "partSize");
		
		if (createRequest.getSourceFileHandleId() != null) {
			ValidateArgument.required(createRequest.getSourceFileEtag(), "sourceFileEtag");
		}

		DBOMultipartUpload dbo = new DBOMultipartUpload();
		
		dbo.setId(idGenerator.generateNewId(IdType.MULTIPART_UPLOAD_ID));
		dbo.setEtag(UUID.randomUUID().toString());
		dbo.setRequestHash(createRequest.getHash());
		dbo.setStartedBy(createRequest.getUserId());
		dbo.setState(MultipartUploadState.UPLOADING.name());
		dbo.setRequestBlob(extractBytes(createRequest.getRequestBody()));
		dbo.setStartedOn(new Date(System.currentTimeMillis()));
		dbo.setUpdatedOn(dbo.getStartedOn());
		dbo.setUploadToken(createRequest.getUploadToken());
		dbo.setUploadType(createRequest.getUploadType().toString());
		dbo.setBucket(createRequest.getBucket());
		dbo.setKey(createRequest.getKey());
		dbo.setNumberOfParts(createRequest.getNumberOfParts());
		dbo.setPartSize(createRequest.getPartSize());
		
		if (createRequest.getSourceFileHandleId() == null) {
			dbo.setRequestType(MultiPartRequestType.UPLOAD.name());
		} else {
			dbo.setRequestType(MultiPartRequestType.COPY.name());
			dbo.setSourceFileHandleId(Long.valueOf(createRequest.getSourceFileHandleId()));
			dbo.setSourceFileEtag(createRequest.getSourceFileEtag());
		}

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
			return this.jdbcTemplate.queryForObject(SELECT_BY_ID, STATUS_MAPPER, id);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("MultipartUploadStatus cannot be found for id: " + id);
		}
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update(SQL_TRUNCATE_ALL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO#getPartsState (java.lang.String, int)
	 */
	@Override
	public String getPartsState(String uploadId, int numberOfParts) {
		ValidateArgument.required(uploadId, "UploadId");
		validatePartNumber(numberOfParts);
		char[] chars = new char[numberOfParts];
		// start with each part missing
		Arrays.fill(chars, '0');
		List<Integer> addedParts = jdbcTemplate.queryForList(SQL_SELECT_ADDED_PART_NUMBERS, Integer.class, uploadId);
		for (Integer partNumber : addedParts) {
			chars[partNumber - 1] = '1';
		}
		return new String(chars);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO#addPartToUpload (java.lang.String, int,
	 * java.lang.String)
	 */
	@WriteTransaction
	@Override
	public void addPartToUpload(String uploadId, int partNumber, String partMD5Hex) {
		ValidateArgument.required(uploadId, "UploadId");
		ValidateArgument.required(partMD5Hex, "PartMD5Hex");
		validatePartNumber(partNumber);
		// update the etag of the master row.
		updateEtag(uploadId);
		// update the part state
		DBOMultipartUploadPartState partState = new DBOMultipartUploadPartState();
		partState.setUploadId(Long.parseLong(uploadId));
		partState.setPartNumber(partNumber);
		partState.setPartMD5Hex(partMD5Hex);
		partState.setErrorDetails(null);
		basicDao.createOrUpdate(partState);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO#setPartToFailed (java.lang.String, int,
	 * java.lang.String)
	 */
	@WriteTransaction
	@Override
	public void setPartToFailed(String uploadId, int partNumber, String errorDetails) {
		ValidateArgument.required(uploadId, "UploadId");
		ValidateArgument.required(errorDetails, "ErrorDetails");
		validatePartNumber(partNumber);
		// update the etag of the master row.
		updateEtag(uploadId);
		// update the part state
		DBOMultipartUploadPartState partState = new DBOMultipartUploadPartState();
		partState.setUploadId(Long.parseLong(uploadId));
		partState.setPartNumber(partNumber);
		partState.setPartMD5Hex(null);
		try {
			partState.setErrorDetails(errorDetails.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		basicDao.createOrUpdate(partState);
	}

	/**
	 * Validate that a part number is within range.
	 * 
	 * @param partNumber
	 */
	private void validatePartNumber(int partNumber) {
		if (partNumber < 1 || partNumber > 10 * 1000) {
			throw new IllegalArgumentException("Part number must be between 1 and 10,000 (inclusive).");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO#getAddedPartMD5s (java.lang.String)
	 */
	@Override
	public List<PartMD5> getAddedPartMD5s(final String uploadId) {
		ValidateArgument.required(uploadId, "UploadId");
		return jdbcTemplate.query(SQL_SELECT_PART_MD5S, PART_MD5_MAPPER, uploadId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO#getPartErrors (java.lang.String)
	 */
	@Override
	public List<PartErrors> getPartErrors(String uploadId) {
		ValidateArgument.required(uploadId, "UploadId");
		return jdbcTemplate.query(SQL_SELECT_PART_ERROR, PART_ERRORS_MAPPER, uploadId);
	}

	/**
	 * Update the etag for the given upload.
	 * 
	 * @param uploadId
	 */
	private void updateEtag(String uploadId) {
		ValidateArgument.required(uploadId, "UploadId");
		String newEtag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_UPDATE_ETAG, newEtag, new Date(), uploadId);
	}

	@Override
	public String getUploadRequest(String uploadId) {
		ValidateArgument.required(uploadId, "UploadId");

		try {
			return jdbcTemplate.queryForObject(SQL_SELECT_BLOB, REQUEST_MAPPER, uploadId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Could not find uploadId: " + uploadId, e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO#setUploadComplete (java.lang.String,
	 * java.lang.String)
	 */
	@WriteTransaction
	@Override
	public CompositeMultipartUploadStatus setUploadComplete(String uploadId, String fileHandleId) {
		ValidateArgument.required(uploadId, "UploadId");
		ValidateArgument.required(fileHandleId, "FileHandleId");
		String newEtag = UUID.randomUUID().toString();
		MultipartUploadState state = MultipartUploadState.COMPLETED;
		jdbcTemplate.update(SQL_SET_COMPLETE, fileHandleId, newEtag, state.name(), new Date(), uploadId);
		// delete all of the parts for this file
		jdbcTemplate.update(SQL_DELETE_ALL_PARTS, uploadId);
		return getUploadStatus(uploadId);
	}

	@Override
	public List<String> getUploadsModifiedBefore(int numberOfDays, long batchSize) {
		ValidateArgument.requirement(numberOfDays >= 0, "The number of days must be equal or greater than zero.");
		ValidateArgument.requirement(batchSize > 0, "The batchSize must be greater than zero.");
		
		return jdbcTemplate.queryForList(SQL_SELECT_BATCH_OLDER_THAN, String.class, numberOfDays, batchSize);
	}
	
	@Override
	public List<String> getUploadsOrderByUpdatedOn(long batchSize) {
		ValidateArgument.requirement(batchSize > 0, "The batchSize must be greater than zero.");
		
		return jdbcTemplate.queryForList(SQL_SELECT_BATCH, String.class, batchSize);
	}
	
}
