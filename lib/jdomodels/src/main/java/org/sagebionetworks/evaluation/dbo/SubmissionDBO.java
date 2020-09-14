package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBMISSION_CREATED_ON;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBMISSION_DOCKER_DIGEST;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBMISSION_DOCKER_REPOSITORY_NAME;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBMISSION_ENTITY_BUNDLE;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBMISSION_ENTITY_ID;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBMISSION_ENTITY_VERSION;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBMISSION_EVAL_ID;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBMISSION_EVAL_ROUND_ID;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBMISSION_ID;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBMISSION_NAME;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBMISSION_SUBMITTER_ALIAS;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBMISSION_TEAM_ID;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBMISSION_USER_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_SUBMISSIONS_EVAL_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_DOCKER_DIGEST;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_DOCKER_REPO_NAME;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_ENTITY_BUNDLE;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_ENTITY_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_ENTITY_VERSION;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_EVAL_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_EVAL_ROUND_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_NAME;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_SUBMITTER_ALIAS;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_TEAM_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_USER_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.DDL_FILE_SUBMISSION;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBMISSION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * The database object for a Submission to a Synapse Evaluation
 * 
 * @author bkng
 */
public class SubmissionDBO implements MigratableDatabaseObject<SubmissionDBO, SubmissionDBO> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn(PARAM_SUBMISSION_ID, COL_SUBMISSION_ID, true).withIsBackupId(true),
			new FieldColumn(PARAM_SUBMISSION_USER_ID, COL_SUBMISSION_USER_ID),
			new FieldColumn(PARAM_SUBMISSION_EVAL_ID, COL_SUBMISSION_EVAL_ID),
			new FieldColumn(PARAM_SUBMISSION_EVAL_ROUND_ID, COL_SUBMISSION_EVAL_ROUND_ID),
			new FieldColumn(PARAM_SUBMISSION_ENTITY_ID, COL_SUBMISSION_ENTITY_ID),
			new FieldColumn(PARAM_SUBMISSION_ENTITY_BUNDLE, COL_SUBMISSION_ENTITY_BUNDLE),
			new FieldColumn(PARAM_SUBMISSION_ENTITY_VERSION, COL_SUBMISSION_ENTITY_VERSION),
			new FieldColumn(PARAM_SUBMISSION_NAME, COL_SUBMISSION_NAME),
			new FieldColumn(PARAM_SUBMISSION_CREATED_ON, COL_SUBMISSION_CREATED_ON),
			new FieldColumn(PARAM_SUBMISSION_SUBMITTER_ALIAS, COL_SUBMISSION_SUBMITTER_ALIAS),
			new FieldColumn(PARAM_SUBMISSION_TEAM_ID, COL_SUBMISSION_TEAM_ID),
			new FieldColumn(PARAM_SUBMISSION_DOCKER_REPOSITORY_NAME, COL_SUBMISSION_DOCKER_REPO_NAME),
			new FieldColumn(PARAM_SUBMISSION_DOCKER_DIGEST, COL_SUBMISSION_DOCKER_DIGEST)
			};

	public TableMapping<SubmissionDBO> getTableMapping() {
		return new TableMapping<SubmissionDBO>() {
			// Map a result set to this object
			public SubmissionDBO mapRow(ResultSet rs, int rowNum)	throws SQLException {
				SubmissionDBO sub = new SubmissionDBO();
				sub.setId(rs.getLong(COL_SUBMISSION_ID));
				sub.setUserId(rs.getLong(COL_SUBMISSION_USER_ID));
				sub.setSubmitterAlias(rs.getString(COL_SUBMISSION_SUBMITTER_ALIAS));
				sub.setEvalId(rs.getLong(COL_SUBMISSION_EVAL_ID));

				Long evalRoundId = rs.getLong(COL_SUBMISSION_EVAL_ROUND_ID);
				evalRoundId = rs.wasNull() ? null : evalRoundId;
				sub.setEvalRoundId(evalRoundId);

				sub.setEntityId(rs.getLong(COL_SUBMISSION_ENTITY_ID));
				sub.setVersionNumber(rs.getLong(COL_SUBMISSION_ENTITY_VERSION));
				sub.setName(rs.getString(COL_SUBMISSION_NAME));
				sub.setCreatedOn(rs.getLong(COL_SUBMISSION_CREATED_ON));
				sub.setDockerRepositoryName(rs.getString(COL_SUBMISSION_DOCKER_REPO_NAME));
				sub.setDockerDigest(rs.getString(COL_SUBMISSION_DOCKER_DIGEST));
				java.sql.Blob blob = rs.getBlob(COL_SUBMISSION_ENTITY_BUNDLE);
				if(blob != null){
					sub.setEntityBundle(blob.getBytes(1, (int) blob.length()));
				}
				{
					sub.setTeamId(rs.getLong(COL_SUBMISSION_TEAM_ID));
					if (rs.wasNull()) sub.setTeamId(null);
				}
				return sub;
			}

			public String getTableName() {
				return TABLE_SUBMISSION;
			}

			public String getDDLFileName() {
				return DDL_FILE_SUBMISSION;
			}

			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			public Class<? extends SubmissionDBO> getDBOClass() {
				return SubmissionDBO.class;
			}
		};
	}
	
	private Long id;
	private Long userId;
	private String submitterAlias;
	private Long evalId;
	private Long evalRoundId;
	private Long entityId;
	private byte[] entityBundle;
	private Long versionNumber;
	private Long createdOn;
	private String name;
	private Long teamId;
	private String dockerRepositoryName;
	private String dockerDigest;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getSubmitterAlias() {
		return submitterAlias;
	}
	public void setSubmitterAlias(String submitterAlias) {
		this.submitterAlias = submitterAlias;
	}
	public Long getEvalId() {
		return evalId;
	}
	public void setEvalId(Long evalId) {
		this.evalId = evalId;
	}

	public Long getEvalRoundId() {
		return evalRoundId;
	}

	public void setEvalRoundId(Long evalRoundId) {
		this.evalRoundId = evalRoundId;
	}

	public Long getEntityId() {
		return entityId;
	}
	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}
	
	public byte[] getEntityBundle() {
		return entityBundle;
	}
	public void setEntityBundle(byte[] entityBundle) {
		this.entityBundle = entityBundle;
	}
	public Long getVersionNumber() {
		return versionNumber;
	}
	public void setVersionNumber(Long versionNumber) {
		this.versionNumber = versionNumber;
	}
	public Long getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public Long getTeamId() {
		return teamId;
	}
	public void setTeamId(Long teamId) {
		this.teamId = teamId;
	}
	
	public String getDockerRepositoryName() {
		return dockerRepositoryName;
	}
	public void setDockerRepositoryName(String dockerRepositoryName) {
		this.dockerRepositoryName = dockerRepositoryName;
	}
	public String getDockerDigest() {
		return dockerDigest;
	}
	
	public void setDockerDigest(String dockerDigest) {
		this.dockerDigest = dockerDigest;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SubmissionDBO that = (SubmissionDBO) o;
		return Objects.equals(id, that.id) &&
				Objects.equals(userId, that.userId) &&
				Objects.equals(submitterAlias, that.submitterAlias) &&
				Objects.equals(evalId, that.evalId) &&
				Objects.equals(evalRoundId, that.evalRoundId) &&
				Objects.equals(entityId, that.entityId) &&
				Arrays.equals(entityBundle, that.entityBundle) &&
				Objects.equals(versionNumber, that.versionNumber) &&
				Objects.equals(createdOn, that.createdOn) &&
				Objects.equals(name, that.name) &&
				Objects.equals(teamId, that.teamId) &&
				Objects.equals(dockerRepositoryName, that.dockerRepositoryName) &&
				Objects.equals(dockerDigest, that.dockerDigest);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(id, userId, submitterAlias, evalId, evalRoundId, entityId, versionNumber, createdOn, name, teamId, dockerRepositoryName, dockerDigest);
		result = 31 * result + Arrays.hashCode(entityBundle);
		return result;
	}

	@Override
	public String toString() {
		return "SubmissionDBO{" +
				"id=" + id +
				", userId=" + userId +
				", submitterAlias='" + submitterAlias + '\'' +
				", evalId=" + evalId +
				", evalRoundId=" + evalRoundId +
				", entityId=" + entityId +
				", entityBundle=" + Arrays.toString(entityBundle) +
				", versionNumber=" + versionNumber +
				", createdOn=" + createdOn +
				", name='" + name + '\'' +
				", teamId=" + teamId +
				", dockerRepositoryName='" + dockerRepositoryName + '\'' +
				", dockerDigest='" + dockerDigest + '\'' +
				'}';
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.SUBMISSION;
	}
	
	@Override
	public MigratableTableTranslation<SubmissionDBO, SubmissionDBO> getTranslator() {
		return new BasicMigratableTableTranslation<SubmissionDBO>();
	}
	@Override
	public Class<? extends SubmissionDBO> getBackupClass() {
		return SubmissionDBO.class;
	}
	@Override
	public Class<? extends SubmissionDBO> getDatabaseObjectClass() {
		return SubmissionDBO.class;
	}
	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?,?>> list = new LinkedList<MigratableDatabaseObject<?,?>>();
		list.add(new SubmissionFileHandleDBO());
		return list;
	}


}
