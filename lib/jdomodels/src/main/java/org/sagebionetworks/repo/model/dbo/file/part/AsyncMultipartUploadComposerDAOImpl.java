package org.sagebionetworks.repo.model.dbo.file.part;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.file.DBOMultipartUploadComposerPartState;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class AsyncMultipartUploadComposerDAOImpl implements AsyncMultipartUploadComposerDAO {

	private final NamedParameterJdbcTemplate namedJdbcTemplate;
	private final DBOBasicDao basicDao;

	@Autowired
	public AsyncMultipartUploadComposerDAOImpl(NamedParameterJdbcTemplate namedJdbcTemplate, DBOBasicDao basicDao) {
		super();
		this.namedJdbcTemplate = namedJdbcTemplate;
		this.basicDao = basicDao;
	}

	@WriteTransaction
	@Override
	public void addPart(String uploadId, PartRange part) {
		ValidateArgument.required(uploadId, "UploadId");
		validatePartRange(part);

		DBOMultipartUploadComposerPartState partState = new DBOMultipartUploadComposerPartState();
		partState.setUploadId(Long.parseLong(uploadId));
		partState.setPartRangeLowerBound(part.getLowerBound());
		partState.setPartRangeUpperBound(part.getUpperBound());
		basicDao.createOrUpdate(partState);
	}

	@WriteTransaction
	@Override
	public void removePart(String uploadId, PartRange part) {
		ValidateArgument.required(uploadId, "UploadId");
		validatePartRange(part);
		basicDao.deleteObjectByPrimaryKey(DBOMultipartUploadComposerPartState.class, toParams(uploadId, part));
	}

	static MapSqlParameterSource toParams(String uploadId, PartRange part) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("uploadId", uploadId);
		param.addValue("partRangeLowerBound", part.getLowerBound());
		param.addValue("partRangeUpperBound", part.getUpperBound());
		return param;
	}

	public static void validatePartRange(PartRange part) {
		ValidateArgument.required(part, "PartRange");
		ValidateArgument.required(part.getLowerBound(), "PartRange.lowerBound");
		ValidateArgument.required(part.getUpperBound(), "PartRange.upperBound");
	}

	@Override
	public List<Compose> findContiguousParts(String uploadId, OrderBy order, int limit) {
		ValidateArgument.required(uploadId, "UploadId");
		ValidateArgument.required(order, "OrderBy");
		String sql = String.format(
				"SELECT L.PART_RANGE_LOWER_BOUND, L.PART_RANGE_UPPER_BOUND, R.PART_RANGE_LOWER_BOUND, R.PART_RANGE_UPPER_BOUND"
						+ " FROM MULTIPART_UPLOAD_COMPOSER_PART_STATE L"
						+ " JOIN MULTIPART_UPLOAD_COMPOSER_PART_STATE R"
						+ " ON(L.PART_RANGE_UPPER_BOUND = R.PART_RANGE_LOWER_BOUND-1 AND L.UPLOAD_ID = R.UPLOAD_ID)"
						+ " WHERE L.UPLOAD_ID = ? ORDER BY %s limit ?;",
				order.toSql());
		return namedJdbcTemplate.getJdbcTemplate().query(sql, (ResultSet rs, int rowNum) -> {
			return new Compose().setLeft(new PartRange().setLowerBound(rs.getLong(1)).setUpperBound(rs.getLong(2)))
					.setRight(new PartRange().setLowerBound(rs.getLong(3)).setUpperBound(rs.getLong(4)));
		}, uploadId, limit);
	}

	@WriteTransaction
	@Override
	public boolean attemptToLockParts(String uploadId, Consumer<List<PartRange>> consumer, PartRange... toLock) {
		ValidateArgument.required(uploadId, "UploadId");
		ValidateArgument.required(consumer, "consumer");
		if (toLock == null || toLock.length < 1) {
			return false;
		}
		Arrays.stream(toLock).forEach(p -> validatePartRange(p));
		List<Long[]> pairs = Arrays.stream(toLock).map(p -> new Long[] { p.getLowerBound(), p.getUpperBound() })
				.collect(Collectors.toList());
		SqlParameterSource params = new MapSqlParameterSource().addValue("uploadId", uploadId).addValue("pairs", pairs);
		String sql = "SELECT PART_RANGE_LOWER_BOUND, PART_RANGE_UPPER_BOUND FROM"
				+ " MULTIPART_UPLOAD_COMPOSER_PART_STATE WHERE UPLOAD_ID = :uploadId"
				+ " AND (PART_RANGE_LOWER_BOUND, PART_RANGE_UPPER_BOUND) IN (:pairs) FOR UPDATE SKIP LOCKED";
		List<PartRange> lockedRanges = namedJdbcTemplate.query(sql, params, (ResultSet rs, int rowNum) -> {
			return new PartRange().setLowerBound(rs.getLong(1)).setUpperBound(rs.getLong(2));
		});
		if (lockedRanges.size() == toLock.length) {
			consumer.accept(lockedRanges);
			return true;
		}
		return false;
	}

	@Override
	public boolean doesExist(String uploadId, PartRange part) {
		ValidateArgument.required(uploadId, "UploadId");
		validatePartRange(part);
		return basicDao
				.getObjectByPrimaryKeyIfExists(DBOMultipartUploadComposerPartState.class, toParams(uploadId, part))
				.isPresent();
	}

	@Override
	public List<PartRange> listAllPartsForUploadId(String uploadId) {
		ValidateArgument.required(uploadId, "UploadId");
		return namedJdbcTemplate.query(
				"SELECT PART_RANGE_LOWER_BOUND, PART_RANGE_UPPER_BOUND FROM"
						+ " MULTIPART_UPLOAD_COMPOSER_PART_STATE WHERE UPLOAD_ID = :uploadId"
						+ " ORDER BY PART_RANGE_LOWER_BOUND, PART_RANGE_UPPER_BOUND",
				new MapSqlParameterSource().addValue("uploadId", uploadId), (ResultSet rs, int rowNum) -> {
					return new PartRange().setLowerBound(rs.getLong("PART_RANGE_LOWER_BOUND"))
							.setUpperBound(rs.getLong("PART_RANGE_UPPER_BOUND"));
				});
	}

}
