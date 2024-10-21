package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TOS_AGREEMENT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TOS_AGREEMENT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TOS_AGREEMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TOS_AGREEMENT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_CREATION_DATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TOS_AGREEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.auth.TermsOfServiceAgreement;
import org.sagebionetworks.util.TemporaryCode;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@TemporaryCode(author = "marco", comment = "This needs to be removed after a release, together with the DBOTermsOfUseAgreement")
public class DBOTermsOfServiceAgreementMigrationDao {

	private IdGenerator idGenerator;
	
	public DBOTermsOfServiceAgreementMigrationDao(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}
	
	public List<UserGroup> getUsersWithoutAgreement(JdbcTemplate jdbcTemplate, List<Long> userIds) {
		String sql = "SELECT"
			+ " U." + COL_USER_GROUP_ID + ","
			+ " U." + COL_USER_GROUP_CREATION_DATE 
			+ " FROM " + TABLE_USER_GROUP + " U LEFT JOIN " + TABLE_TOS_AGREEMENT + " A"
			+ " ON U." + COL_USER_GROUP_ID + " = A." + COL_TOS_AGREEMENT_CREATED_BY
			+ " WHERE U." + COL_USER_GROUP_ID + " IN (" + String.join(",", Collections.nCopies(userIds.size(), "?")) + ")"
			+ " AND A." + COL_TOS_AGREEMENT_ID + " IS NULL";
			
		return jdbcTemplate.query(sql, (rs,  i) -> {
			Timestamp creationDate = rs.getTimestamp(COL_USER_GROUP_CREATION_DATE);
			
			return new UserGroup()
				.setId(rs.getString(COL_USER_GROUP_ID))
				.setCreationDate(creationDate == null ? null : new Date(creationDate.getTime()));
			
		}, userIds.toArray());
		
	}
	
	public void batchAddTermsOfServiceAgreement(JdbcTemplate jdbcTemplate, List<TermsOfServiceAgreement> batch) {
		String sql = "INSERT INTO " + TABLE_TOS_AGREEMENT + "("
			+ COL_TOS_AGREEMENT_ID + ", "
			+ COL_TOS_AGREEMENT_CREATED_ON + ", "
			+ COL_TOS_AGREEMENT_CREATED_BY + ", "
			+ COL_TOS_AGREEMENT_VERSION + ")" 
			+ " VALUES (?, ?, ?, ?)";
		
		List<Long> batchIds = batch.stream().map( agreement -> 
			idGenerator.generateNewId(IdType.TOS_AGREEMENT_ID)
		).collect(Collectors.toList());
		
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				TermsOfServiceAgreement agreement = batch.get(i);
				
				ps.setLong(1, batchIds.get(i));
				ps.setTimestamp(2, new Timestamp(agreement.getAgreedOn().getTime()));
				ps.setLong(3, agreement.getUserId());
				ps.setString(4, agreement.getVersion());
			}
			
			@Override
			public int getBatchSize() {
				return batch.size();
			}
		});
	}

}
