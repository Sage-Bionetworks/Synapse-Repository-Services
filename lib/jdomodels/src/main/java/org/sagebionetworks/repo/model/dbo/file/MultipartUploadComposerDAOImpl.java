package org.sagebionetworks.repo.model.dbo.file;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_COMPOSER_PART_RANGE_LOWER_BOUND;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_COMPOSER_PART_RANGE_UPPER_BOUND;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_COMPOSER_PART_UPLOAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MULTIPART_UPLOAD;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MULTIPART_UPLOAD_COMPOSER_PART_STATE;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class MultipartUploadComposerDAOImpl implements MultipartUploadComposerDAO {

	private static final String PARAM_UPLOAD_ID = "uploadId";
	private static final String PARAM_LOWER_BOUND = "lowerBound";
	private static final String PARAM_UPPER_BOUND = "upperBound";

	private static final String SQL_SELECT_PARTS_BY_UPLOAD_ID =
			"SELECT * FROM " + TABLE_MULTIPART_UPLOAD_COMPOSER_PART_STATE
			+ " WHERE " + COL_MULTIPART_COMPOSER_PART_UPLOAD_ID + " = :" + PARAM_UPLOAD_ID;

	private static final String SQL_SELECT_PARTS_IN_RANGE =
			"SELECT * FROM " + TABLE_MULTIPART_UPLOAD_COMPOSER_PART_STATE
			+ " WHERE " + COL_MULTIPART_COMPOSER_PART_UPLOAD_ID + " = :" + PARAM_UPLOAD_ID
			+ " AND " + COL_MULTIPART_COMPOSER_PART_RANGE_LOWER_BOUND + " >= :" + PARAM_LOWER_BOUND
			+ " AND " + COL_MULTIPART_COMPOSER_PART_RANGE_UPPER_BOUND + " <= :" + PARAM_UPPER_BOUND
			+ " ORDER BY " + COL_MULTIPART_COMPOSER_PART_RANGE_LOWER_BOUND + " ASC";

	private static final String SQL_DELETE_PARTS_IN_RANGE =
			"DELETE FROM " + TABLE_MULTIPART_UPLOAD_COMPOSER_PART_STATE
			+ " WHERE " + COL_MULTIPART_COMPOSER_PART_UPLOAD_ID + " = :" + PARAM_UPLOAD_ID
			+ " AND " + COL_MULTIPART_COMPOSER_PART_RANGE_LOWER_BOUND + " >= :" + PARAM_LOWER_BOUND
			+ " AND " + COL_MULTIPART_COMPOSER_PART_RANGE_UPPER_BOUND + " <= :" + PARAM_UPPER_BOUND;

	private static final String SQL_DELETE_ALL_PARTS = "DELETE FROM "
			+ TABLE_MULTIPART_UPLOAD_COMPOSER_PART_STATE + " WHERE "
			+ COL_MULTIPART_COMPOSER_PART_UPLOAD_ID + " = :" + PARAM_UPLOAD_ID;

	private static final String SQL_TRUNCATE_ALL = "DELETE FROM "
			+ TABLE_MULTIPART_UPLOAD + " WHERE " + COL_MULTIPART_UPLOAD_ID
			+ " > -1";

	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	@Autowired
	private DBOBasicDao basicDao;

	private RowMapper<DBOMultipartUploadComposerPartState> rowMapper = (rs, rowNum) -> {
		DBOMultipartUploadComposerPartState dbo = new DBOMultipartUploadComposerPartState();
		dbo.setUploadId(rs.getLong(COL_MULTIPART_COMPOSER_PART_UPLOAD_ID));
		dbo.setPartRangeUpperBound(rs.getLong(COL_MULTIPART_COMPOSER_PART_RANGE_UPPER_BOUND));
		dbo.setPartRangeLowerBound(rs.getLong(COL_MULTIPART_COMPOSER_PART_RANGE_LOWER_BOUND));
		return dbo;
	};

	@WriteTransaction
	@Override
	public void addPartToUpload(String uploadId, long lowerBound, long upperBound) {
		ValidateArgument.required(uploadId, "UploadId");
		DBOMultipartUploadComposerPartState partState = new DBOMultipartUploadComposerPartState();
		partState.setUploadId(Long.parseLong(uploadId));
		partState.setPartRangeLowerBound(lowerBound);
		partState.setPartRangeUpperBound(upperBound);
		basicDao.createOrUpdate(partState);
	}

	@Override
	public List<DBOMultipartUploadComposerPartState> getAddedParts(Long uploadId) {
		MapSqlParameterSource param = new MapSqlParameterSource().addValue(PARAM_UPLOAD_ID, uploadId);
		return namedJdbcTemplate.query(SQL_SELECT_PARTS_BY_UPLOAD_ID, param, rowMapper);
	}

	@MandatoryWriteTransaction
	@Override
	public List<DBOMultipartUploadComposerPartState> getAddedPartRanges(Long uploadId, Long lowerBound, Long upperBound) {
		MapSqlParameterSource param = new MapSqlParameterSource()
				.addValue(PARAM_UPLOAD_ID, uploadId)
				.addValue(PARAM_LOWER_BOUND, lowerBound)
				.addValue(PARAM_UPPER_BOUND, upperBound);
		return namedJdbcTemplate.query(SQL_SELECT_PARTS_IN_RANGE, param, rowMapper);
	}

	@WriteTransaction
	@Override
	public void deletePartsInRange(String uploadId, long lowerBound, long upperBound) {
		namedJdbcTemplate.update(SQL_DELETE_PARTS_IN_RANGE, new MapSqlParameterSource()
				.addValue(PARAM_UPLOAD_ID, uploadId)
				.addValue(PARAM_LOWER_BOUND, lowerBound)
				.addValue(PARAM_UPPER_BOUND, upperBound));
	}

	@WriteTransaction
	@Override
	public void deleteAllParts(String uploadId) {
		ValidateArgument.required(uploadId, "UploadId");
		namedJdbcTemplate.update(SQL_DELETE_ALL_PARTS,
				new MapSqlParameterSource().addValue(PARAM_UPLOAD_ID, uploadId));
	}

	@WriteTransaction
	@Override
	public void truncateAll() {
		namedJdbcTemplate.update(SQL_TRUNCATE_ALL, new MapSqlParameterSource());
	}
}
