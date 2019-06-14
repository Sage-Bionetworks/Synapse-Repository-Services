package org.sagebionetworks.repo.model.dbo.file;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_COMPOSER_PART_ERROR_DETAILS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_COMPOSER_PART_RANGE_LOWER_BOUND;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_COMPOSER_PART_RANGE_UPPER_BOUND;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_COMPOSER_PART_UPLOAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MULTIPART_UPLOAD;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MULTIPART_UPLOAD_COMPOSER_PART_STATE;

import java.util.List;
import java.util.UUID;

import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.file.MultipartUploadState;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class MultipartUploadComposerDAOImpl implements MultipartUploadComposerDAO {

	private static final String PARAM_UPLOAD_ID = "uploadId";
	private static final String PARAM_FILE_HANDLE_ID = "fhId";
	private static final String PARAM_ETAG = "etag";
	private static final String PARAM_MULTIPART_STATE = "multipartState";
	private static final String PARAM_LOWER_BOUND = "lowerBound";
	private static final String PARAM_UPPER_BOUND = "upperBound";

	private static final String SQL_SELECT_PARTS_BY_UPLOAD_ID =
			"SELECT * FROM " + TABLE_MULTIPART_UPLOAD_COMPOSER_PART_STATE
			+ " WHERE " + COL_MULTIPART_COMPOSER_PART_UPLOAD_ID + " = :" + PARAM_UPLOAD_ID;


	private static final String SQL_SELECT_CONTIGS_FOR_UPDATE =
			"SELECT * FROM " + TABLE_MULTIPART_UPLOAD_COMPOSER_PART_STATE
			+ " WHERE " + COL_MULTIPART_COMPOSER_PART_UPLOAD_ID + " = :" + PARAM_UPLOAD_ID
			+ " AND (" + COL_MULTIPART_COMPOSER_PART_RANGE_LOWER_BOUND + " = :" + PARAM_LOWER_BOUND
			+ " OR " + COL_MULTIPART_COMPOSER_PART_RANGE_UPPER_BOUND + " = :" + PARAM_UPPER_BOUND
			+ ") ORDER BY " + COL_MULTIPART_COMPOSER_PART_RANGE_LOWER_BOUND + " ASC FOR UPDATE";

	private static final String SQL_DELETE_PARTS_IN_RANGE =
			"DELETE FROM " + TABLE_MULTIPART_UPLOAD_COMPOSER_PART_STATE
			+ " WHERE " + COL_MULTIPART_COMPOSER_PART_UPLOAD_ID + " = :" + PARAM_UPLOAD_ID
			+ " AND " + COL_MULTIPART_COMPOSER_PART_RANGE_LOWER_BOUND + " >= :" + PARAM_LOWER_BOUND
			+ " AND " + COL_MULTIPART_COMPOSER_PART_RANGE_UPPER_BOUND + " <= :" + PARAM_UPPER_BOUND;

	private static final String SQL_DELETE_ALL_PARTS = "DELETE FROM "
			+ TABLE_MULTIPART_UPLOAD_COMPOSER_PART_STATE + " WHERE "
			+ COL_MULTIPART_COMPOSER_PART_UPLOAD_ID + " = :" + PARAM_UPLOAD_ID;

	private static final String SQL_SET_COMPLETE = "UPDATE "
			+ TABLE_MULTIPART_UPLOAD + " SET " + COL_MULTIPART_FILE_HANDLE_ID
			+ " = :" + PARAM_FILE_HANDLE_ID + " , " + COL_MULTIPART_UPLOAD_ETAG + " = :" + PARAM_ETAG + " , "
			+ COL_MULTIPART_STATE + " = :" + PARAM_MULTIPART_STATE + " WHERE " + COL_MULTIPART_UPLOAD_ID
			+ " = :" + PARAM_UPLOAD_ID;

	private static final String SQL_UPDATE_ETAG = "UPDATE "
			+ TABLE_MULTIPART_UPLOAD + " SET " + COL_MULTIPART_UPLOAD_ETAG
			+ " = :" + PARAM_ETAG + " WHERE " + COL_MULTIPART_UPLOAD_ID + " = :" + PARAM_UPLOAD_ID;

	private static final String SQL_TRUNCATE_ALL = "DELETE FROM "
			+ TABLE_MULTIPART_UPLOAD + " WHERE " + COL_MULTIPART_UPLOAD_ID
			+ " > -1";

	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	@Autowired
	private MultipartUploadDAO multipartUploadDAO;

	@Autowired
	private DBOBasicDao basicDao;

	private RowMapper<DBOMultipartUploadComposerPartState> rowMapper = (rs, rowNum) -> {
		DBOMultipartUploadComposerPartState dbo = new DBOMultipartUploadComposerPartState();
		dbo.setUploadId(rs.getLong(COL_MULTIPART_COMPOSER_PART_UPLOAD_ID));
		dbo.setPartRangeUpperBound(rs.getInt(COL_MULTIPART_COMPOSER_PART_RANGE_UPPER_BOUND));
		dbo.setPartRangeLowerBound(rs.getInt(COL_MULTIPART_COMPOSER_PART_RANGE_LOWER_BOUND));
		dbo.setErrorDetails(rs.getBytes(COL_MULTIPART_COMPOSER_PART_ERROR_DETAILS));
		return dbo;
	};

	@Override
	public void addPartToUpload(String uploadId, int lowerBound, int upperBound) {
		ValidateArgument.required(uploadId, "UploadId");
		// update the etag of the master row.
		updateEtag(uploadId);
		// update the part state
		DBOMultipartUploadComposerPartState partState = new DBOMultipartUploadComposerPartState();
		partState.setUploadId(Long.parseLong(uploadId));
		partState.setPartRangeLowerBound(lowerBound);
		partState.setPartRangeUpperBound(upperBound);
		partState.setErrorDetails(null);
		basicDao.createOrUpdate(partState);
	}

	@Override
	public List<DBOMultipartUploadComposerPartState> getAddedParts(String uploadId) {
		MapSqlParameterSource param = new MapSqlParameterSource().addValue(PARAM_UPLOAD_ID, uploadId);
		return namedJdbcTemplate.query(SQL_SELECT_PARTS_BY_UPLOAD_ID, param, rowMapper);
	}

	@Override
	public void deletePartsInRange(String uploadId, int lowerBound, int upperBound) {
		namedJdbcTemplate.update(SQL_DELETE_PARTS_IN_RANGE, new MapSqlParameterSource()
				.addValue(PARAM_UPLOAD_ID, uploadId)
				.addValue(PARAM_LOWER_BOUND, lowerBound)
				.addValue(PARAM_UPPER_BOUND, upperBound));
	}

	@Override
	public List<DBOMultipartUploadComposerPartState> getContiguousParts(String uploadId, int partNumber) {
		MapSqlParameterSource param = new MapSqlParameterSource()
				.addValue(PARAM_UPLOAD_ID, uploadId)
				.addValue(PARAM_LOWER_BOUND, partNumber + 1)
				.addValue(PARAM_UPPER_BOUND, partNumber - 1);
		return namedJdbcTemplate.query(SQL_SELECT_CONTIGS_FOR_UPDATE, param, rowMapper);
	}

	@Override
	public CompositeMultipartUploadStatus setUploadComplete(String uploadId, String fileHandleId) {
		ValidateArgument.required(uploadId, "UploadId");
		ValidateArgument.required(fileHandleId, "FileHandleId");
		String newEtag = UUID.randomUUID().toString();
		MultipartUploadState state = MultipartUploadState.COMPLETED;

		MapSqlParameterSource updateParam = new MapSqlParameterSource()
				.addValue(PARAM_FILE_HANDLE_ID, fileHandleId)
				.addValue(PARAM_ETAG, newEtag)
				.addValue(PARAM_MULTIPART_STATE, state.name())
				.addValue(PARAM_UPLOAD_ID, uploadId);

		namedJdbcTemplate.update(SQL_SET_COMPLETE, updateParam);
		// delete all of the parts for this file
		namedJdbcTemplate.update(SQL_DELETE_ALL_PARTS,
				new MapSqlParameterSource().addValue(PARAM_UPLOAD_ID, uploadId));
		return multipartUploadDAO.getUploadStatus(uploadId);
	}

	@Override
	public void truncateAll() {
		namedJdbcTemplate.update(SQL_TRUNCATE_ALL, new MapSqlParameterSource());
	}

	/**
	 * Update the etag for the given upload.
	 *
	 * @param uploadId
	 */
	private void updateEtag(String uploadId) {
		ValidateArgument.required(uploadId, "UploadId");
		String newEtag = UUID.randomUUID().toString();
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PARAM_ETAG, newEtag);
		param.addValue(PARAM_UPLOAD_ID, uploadId);
		namedJdbcTemplate.update(SQL_UPDATE_ETAG, param);
	}
}
