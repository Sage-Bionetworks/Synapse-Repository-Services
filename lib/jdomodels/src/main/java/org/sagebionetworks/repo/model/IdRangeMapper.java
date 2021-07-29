package org.sagebionetworks.repo.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

/**
 * {@link RowMapper} for an {@link IdRange}, the first and second columns in the result set row should be the min and max respectively
 */
public class IdRangeMapper implements RowMapper<IdRange> {
	
	@Override
	public IdRange mapRow(ResultSet rs, int rowNum) throws SQLException {
		long minId = rs.getLong(1);
	
		if (rs.wasNull()) {
			minId = -1;
		}
	
		long maxId = rs.getLong(2);
	
		if (rs.wasNull()) {
			maxId = -1;
		}
	
		return new IdRange(minId, maxId);
	};

}
