package org.sagebionetworks.evaluation.dao;

import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_CONTENT_SOURCE;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_OWNER_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_CONTRIBUTOR_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_CONTRIBUTOR_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_DOCKER_DIGEST;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_DOCKER_REPO_NAME;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_ENTITY_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_ENTITY_VERSION;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_EVAL_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_EVAL_ROUND_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_NAME;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_SUBMITTER_ALIAS;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_TEAM_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_USER_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ANNOTATIONS;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ETAG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_STATUS;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_VERSION;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_EVALUATION;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBMISSION;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBMISSION_CONTRIBUTOR;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBSTATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ELEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_CONTROL_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GROUP_MEMBERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS_TYPE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.dbo.SubmissionContributorDBO;
import org.sagebionetworks.evaluation.dbo.SubmissionDBO;
import org.sagebionetworks.evaluation.dbo.SubmissionStatusDBO;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionContributor;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.query.SQLConstants;
import org.sagebionetworks.repo.model.table.ObjectAnnotationDTO;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;


public class SubmissionDAOImpl implements SubmissionDAO {

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;

	private static final String ID = DBOConstants.PARAM_SUBMISSION_ID;
	private static final String USER_ID = DBOConstants.PARAM_SUBMISSION_USER_ID;
	private static final String EVAL_ID = DBOConstants.PARAM_SUBMISSION_EVAL_ID;
	private static final String STATUS = DBOConstants.PARAM_SUBSTATUS_STATUS;
	private static final String CRC = "CRC";
	private static final String PROJECT_ID = "PROJECT_ID";
	private static final String CREATED_BY = "CREATED_BY";
	private static final String MODIFIED_BY = "MODIFIED_BY";

	private static final String SELECT_ALL = "SELECT *";
	private static final String SELECT_COUNT = "SELECT COUNT(*)";
	private static final String LIMIT_OFFSET = 			
			" LIMIT :"+ SQLConstants.LIMIT_PARAM_NAME +
			" OFFSET :" + SQLConstants.OFFSET_PARAM_NAME;

	private static final String BY_USER_SQL = 
			" FROM "+ SQLConstants.TABLE_SUBMISSION +
			" WHERE "+ SQLConstants.COL_SUBMISSION_USER_ID + "=:"+ USER_ID;

	private static final String BY_EVALUATION_SQL = 
			" FROM "+ SQLConstants.TABLE_SUBMISSION +
			" WHERE "+ SQLConstants.COL_SUBMISSION_EVAL_ID + "=:"+ EVAL_ID;

	private static final String BUNDLES_BY_EVALUATION_SQL = 
			" FROM "+ SQLConstants.TABLE_SUBMISSION + " s, " +
			SQLConstants.TABLE_SUBSTATUS + " r " +
			" WHERE s."+ SQLConstants.COL_SUBMISSION_EVAL_ID + "=:"+ EVAL_ID +
			" AND s." + SQLConstants.COL_SUBMISSION_ID + "= r."+ SQLConstants.COL_SUBSTATUS_SUBMISSION_ID;

	private static final String BUNDLES_BY_EVAL_AND_STATUS_SQL = 
			" FROM "+ SQLConstants.TABLE_SUBMISSION + " s, " +
			SQLConstants.TABLE_SUBSTATUS + " r " +
			" WHERE s."+ SQLConstants.COL_SUBMISSION_EVAL_ID + "=:"+ EVAL_ID +
			" AND s." + SQLConstants.COL_SUBMISSION_ID + "= r."+ SQLConstants.COL_SUBSTATUS_SUBMISSION_ID+
			" AND r." + SQLConstants.COL_SUBSTATUS_STATUS + "=:" + STATUS;

	private static final String BUNDLES_BY_EVAL_AND_USER_SQL = 
			" FROM "+ SQLConstants.TABLE_SUBMISSION + " s, " +
			SQLConstants.TABLE_SUBSTATUS + " r " +
			" WHERE s."+ SQLConstants.COL_SUBMISSION_EVAL_ID + "=:"+ EVAL_ID +
			" AND s." + SQLConstants.COL_SUBMISSION_ID + "= r."+ SQLConstants.COL_SUBSTATUS_SUBMISSION_ID+
			" AND s." + SQLConstants.COL_SUBMISSION_USER_ID + "=:" + USER_ID;

	private static final String BY_EVAL_AND_USER_SQL = 
			" FROM "+ SQLConstants.TABLE_SUBMISSION +
			" WHERE "+ SQLConstants.COL_SUBMISSION_USER_ID + "=:"+ USER_ID +
			" AND " + SQLConstants.COL_SUBMISSION_EVAL_ID + "=:"+ EVAL_ID;

	private static final String BY_EVAL_AND_STATUS_SQL = 
			" FROM "+ SQLConstants.TABLE_SUBMISSION + " n" +
					" INNER JOIN " + SQLConstants.TABLE_SUBSTATUS + " r" +
					" ON n." + SQLConstants.COL_SUBMISSION_ID + " = r." + SQLConstants.COL_SUBSTATUS_SUBMISSION_ID +
					" WHERE n."+ SQLConstants.COL_SUBMISSION_EVAL_ID + "=:"+ EVAL_ID +
					" AND r." + SQLConstants.COL_SUBSTATUS_STATUS + "=:" + STATUS;

	private static final String SELECT_BY_USER_SQL = 
			SELECT_ALL + BY_USER_SQL + LIMIT_OFFSET;

	private static final String SELECT_BY_EVALUATION_SQL = 
			SELECT_ALL + BY_EVALUATION_SQL + LIMIT_OFFSET;

	private static final String SELECT_BUNDLE_SQL = 
			SELECT_ALL + " FROM "+ SQLConstants.TABLE_SUBMISSION + " n" +
					" INNER JOIN " + SQLConstants.TABLE_SUBSTATUS + " r" +
					" ON n." + SQLConstants.COL_SUBMISSION_ID + " = r." + 
					SQLConstants.COL_SUBSTATUS_SUBMISSION_ID +
					" WHERE n."+ SQLConstants.COL_SUBMISSION_ID + "=:"+ ID;
	
	private static final String SELECT_BUNDLES_BY_EVALUATION_SQL = 
			SELECT_ALL + BUNDLES_BY_EVALUATION_SQL + LIMIT_OFFSET;
	
	private static final String SELECT_BUNDLES_BY_EVAL_AND_STATUS_SQL = 
			SELECT_ALL + BUNDLES_BY_EVAL_AND_STATUS_SQL + LIMIT_OFFSET;
	
	private static final String SELECT_BUNDLES_BY_EVAL_AND_USER_SQL = 
			SELECT_ALL + BUNDLES_BY_EVAL_AND_USER_SQL + LIMIT_OFFSET;
	
	private static final String SELECT_BY_EVAL_AND_USER_SQL = 
			SELECT_ALL + BY_EVAL_AND_USER_SQL + LIMIT_OFFSET;

	private static final String SELECT_BY_EVAL_AND_STATUS_SQL = 
			SELECT_ALL + BY_EVAL_AND_STATUS_SQL + LIMIT_OFFSET;

	private static final String COUNT_BY_USER_SQL = 
			SELECT_COUNT + BY_USER_SQL;

	private static final String COUNT_BY_EVAL_SQL = 
			SELECT_COUNT + BY_EVALUATION_SQL;

	private static final String COUNT_BY_EVAL_AND_USER_SQL = 
			SELECT_COUNT + BY_EVAL_AND_USER_SQL;

	private static final String COUNT_BY_EVAL_AND_STATUS_SQL = 
			SELECT_COUNT + BY_EVAL_AND_STATUS_SQL;

	private static final String SELECT_CONTRIBUTORS_FOR_SUBMISSIONS = 
			"SELECT * FROM "+TABLE_SUBMISSION_CONTRIBUTOR+
			" WHERE "+COL_SUBMISSION_CONTRIBUTOR_SUBMISSION_ID+" in (:"+
					COL_SUBMISSION_CONTRIBUTOR_SUBMISSION_ID+")";

	private static final String SELECT_CREATED_BY = "SELECT "+COL_SUBMISSION_USER_ID
			+ " FROM "+TABLE_SUBMISSION
			+ " WHERE "+COL_SUBMISSION_ID +"= ?";

	//------    Submission eligibility related query strings -----
	
	// Quota Query:  Count Team's submissions.
	private static final String COUNT_SUBMISSIONS_FROM_TEAM = 
			"SELECT COUNT(*) FROM "+TABLE_SUBMISSION+" sb, "+TABLE_SUBSTATUS+" ss "+
					" WHERE sb."+COL_SUBMISSION_ID+"=ss."+COL_SUBSTATUS_SUBMISSION_ID+
					" AND sb."+COL_SUBMISSION_EVAL_ID+"=:"+COL_SUBMISSION_EVAL_ID+
					" AND sb."+COL_SUBMISSION_TEAM_ID+"=:"+COL_SUBMISSION_TEAM_ID;

	private static final String CREATED_ON_BEFORE_PARAM = COL_SUBMISSION_CREATED_ON+"_BEFORE";

	private static final String SUBMISSION_CREATED_BEFORE = 
			" AND sb."+COL_SUBMISSION_CREATED_ON+"<:"+CREATED_ON_BEFORE_PARAM;

	private static final String CREATED_ON_AFTER_PARAM = COL_SUBMISSION_CREATED_ON+"_AFTER";

	private static final String SUBMISSION_CREATED_ON_OR_AFTER = 
			" AND sb."+COL_SUBMISSION_CREATED_ON+">=:"+CREATED_ON_AFTER_PARAM;

	private static final String COUNT_SUBMISSIONS_FROM_CONTRIBUTOR_FROM = 
			" FROM "+TABLE_SUBMISSION+" sb, "+TABLE_SUBSTATUS+" ss, "+TABLE_SUBMISSION_CONTRIBUTOR+" sc ";

	private static final String COUNT_SUBMISSIONS_FROM_CONTRIBUTOR_WHERE = 
			" WHERE sb."+COL_SUBMISSION_ID+"=ss."+COL_SUBSTATUS_SUBMISSION_ID+
			" AND sb."+COL_SUBMISSION_ID+"=sc."+COL_SUBMISSION_CONTRIBUTOR_SUBMISSION_ID+
			" AND sb."+COL_SUBMISSION_EVAL_ID+"=:"+COL_SUBMISSION_EVAL_ID;
	
	private static final String SUBMISSION_COUNT_LABEL = "SUBMISSION_COUNT";

	// Quota Query:  For each team member, count the submissions they contributed to.
	private static final String COUNT_SUBMISSIONS_FROM_TEAM_MEMBERS = 
			"SELECT sc."+COL_SUBMISSION_CONTRIBUTOR_PRINCIPAL_ID+
			", COUNT(sb."+COL_SUBMISSION_ID+") AS "+SUBMISSION_COUNT_LABEL+
			COUNT_SUBMISSIONS_FROM_CONTRIBUTOR_FROM+
			", "+TABLE_GROUP_MEMBERS+" gm "+
			COUNT_SUBMISSIONS_FROM_CONTRIBUTOR_WHERE+
			" AND sc."+COL_SUBMISSION_CONTRIBUTOR_PRINCIPAL_ID+"=gm."+COL_GROUP_MEMBERS_MEMBER_ID+
			" AND gm."+COL_GROUP_MEMBERS_GROUP_ID+"=:"+COL_GROUP_MEMBERS_GROUP_ID;

	// Quota Query: Count the submissions by a single contributor
	private static final String COUNT_SUBMISSIONS_FROM_CONTRIBUTOR = 
					"SELECT COUNT(sb."+COL_SUBMISSION_ID+") AS "+SUBMISSION_COUNT_LABEL+
					COUNT_SUBMISSIONS_FROM_CONTRIBUTOR_FROM+
					COUNT_SUBMISSIONS_FROM_CONTRIBUTOR_WHERE+
					" AND sc."+COL_SUBMISSION_CONTRIBUTOR_PRINCIPAL_ID+"=:"+
					COL_SUBMISSION_CONTRIBUTOR_PRINCIPAL_ID;

	private static final String GROUP_BY_CONTRIBUTOR =
			" GROUP BY sc."+COL_SUBMISSION_CONTRIBUTOR_PRINCIPAL_ID;

	private static final String SUBMIT_ELSEWHERE_CLAUSE = 
			" AND (sb."+COL_SUBMISSION_TEAM_ID+" IS NULL OR sb."+COL_SUBMISSION_TEAM_ID+
			"<> gm."+COL_GROUP_MEMBERS_GROUP_ID+") ";

	private static final String IS_TEAM_SUBMISSION_CLAUSE = 
			" AND (sb."+COL_SUBMISSION_TEAM_ID+" IS NOT NULL)";

	private static final String SUBSTATUS_IN_CLAUSE = " AND ss."+COL_SUBSTATUS_STATUS+" IN (:"+COL_SUBSTATUS_STATUS+")";

	private static final String SUBMISSIONS_WITH_DOCKER_REPO_AND_PERMISSION_SQL = 
		"SELECT COUNT(*) FROM "+
		TABLE_SUBMISSION+" s "+
		" join "+TABLE_ACCESS_CONTROL_LIST+" acl on s."+COL_SUBMISSION_EVAL_ID+"=acl."+COL_ACL_OWNER_ID+
		" join "+TABLE_RESOURCE_ACCESS+" ra on "+" acl."+COL_ACL_ID+"=ra."+COL_RESOURCE_ACCESS_OWNER+
		" join "+TABLE_RESOURCE_ACCESS_TYPE+" at on ra."+COL_RESOURCE_ACCESS_ID+"=at."+COL_RESOURCE_ACCESS_TYPE_ID+
		" WHERE "+
		"s."+COL_SUBMISSION_DOCKER_REPO_NAME+"=:"+COL_SUBMISSION_DOCKER_REPO_NAME+
		" and acl."+COL_ACL_OWNER_TYPE+"='"+ObjectType.EVALUATION+"'"+
		" and ra."+COL_RESOURCE_ACCESS_GROUP_ID+" in (:"+COL_RESOURCE_ACCESS_GROUP_ID+") "+
		" and at."+COL_RESOURCE_ACCESS_TYPE_ELEMENT+"=:"+COL_RESOURCE_ACCESS_TYPE_ELEMENT;
	
	private static final String SELECT_SUBMISSION_ID_AND_ETAG = "SELECT s." + COL_SUBMISSION_ID + ", r." + COL_SUBSTATUS_ETAG 
			+ BUNDLES_BY_EVALUATION_SQL;
	
	private static final String SELECT_SUM_CRC_SUBMISSIONS = "SELECT "+COL_SUBMISSION_EVAL_ID+","
			+ " SUM(CRC32(CONCAT(s."+COL_SUBMISSION_ID + ",'-', r."+COL_SUBSTATUS_ETAG + "))) AS " + CRC
			+ " FROM "+ TABLE_SUBMISSION + " s INNER JOIN " + TABLE_SUBSTATUS + " r"
			+ " ON (s." + COL_SUBMISSION_ID + " = r."+ COL_SUBSTATUS_SUBMISSION_ID +") "
			+ " WHERE s."+ COL_SUBMISSION_EVAL_ID + " IN (:"+ EVAL_ID +")"
			+ " GROUP BY "+COL_SUBMISSION_EVAL_ID;
	
	private static final String SELECT_SUBMISSION_DATA = "SELECT"
			+ " s." + COL_SUBMISSION_ID
			+ ", s." + COL_SUBMISSION_NAME
			+ ", r." + COL_SUBSTATUS_ETAG
			+ ", s." + COL_SUBMISSION_EVAL_ID + " AS " + SubmissionField.evaluationid.getColumnAlias()
			+ ", s." + COL_SUBMISSION_EVAL_ROUND_ID + " AS " + SubmissionField.evaluationroundid.getColumnAlias()
			+ ", e." + COL_EVALUATION_CONTENT_SOURCE + " AS " + PROJECT_ID
			+ ", r." + COL_SUBSTATUS_VERSION
			+ ", s." + COL_SUBMISSION_CREATED_ON
			+ ", s. " + COL_SUBMISSION_USER_ID + " AS " + CREATED_BY
			+ ", r." + COL_SUBSTATUS_MODIFIED_ON
			// We do not store who modified a status, just use the owner of the evaluation queue
			+ ", e." + COL_EVALUATION_OWNER_ID + " AS " + MODIFIED_BY
			// Computes the submitter either as the team if present or the user
			+ ", IFNULL(s." + COL_SUBMISSION_TEAM_ID + ", s." + COL_SUBMISSION_USER_ID + ") AS " + SubmissionField.submitterid.getColumnAlias()
			+ ", s." + COL_SUBMISSION_SUBMITTER_ALIAS + " AS " + SubmissionField.submitteralias.getColumnAlias()
			+ ", s." + COL_SUBMISSION_ENTITY_ID + " AS " + SubmissionField.entityid.getColumnAlias()
			+ ", s." + COL_SUBMISSION_ENTITY_VERSION + " AS " + SubmissionField.entityversion.getColumnAlias()
			+ ", r." + COL_SUBSTATUS_STATUS + " AS " + SubmissionField.status.getColumnAlias()
			+ ", s." + COL_SUBMISSION_DOCKER_REPO_NAME + " AS " + SubmissionField.dockerrepositoryname.getColumnAlias()
			+ ", s." + COL_SUBMISSION_DOCKER_DIGEST + " AS " + SubmissionField.dockerdigest.getColumnAlias()
			+ ", r." + COL_SUBSTATUS_ANNOTATIONS
			+ " FROM " + TABLE_SUBMISSION +" s JOIN " + TABLE_SUBSTATUS + " r JOIN " + TABLE_EVALUATION + " e"
			+ " ON (s."+COL_SUBMISSION_ID + " = r." + COL_SUBSTATUS_SUBMISSION_ID 
			+ " AND s." + COL_SUBMISSION_EVAL_ID + " = e." + COL_EVALUATION_ID + ")"
			+ " WHERE s."+COL_SUBMISSION_ID + " IN (:"+ID+ ")";
	

	//------    end Submission eligibility related query strings -----

	private static final RowMapper<SubmissionDBO> SUBMISSION_ROW_MAPPER = 
			((new SubmissionDBO()).getTableMapping());
	
	private static final RowMapper<SubmissionStatusDBO> SUBMISSION_STATUS_ROW_MAPPER = 
			((new SubmissionStatusDBO()).getTableMapping());
	
	private static final RowMapper<SubmissionBundle> BUNDLE_ROW_MAPPER = new RowMapper<SubmissionBundle>(){
		@Override
		public SubmissionBundle mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			SubmissionBundle result = new SubmissionBundle();
			SubmissionDBO submissionDbo = SUBMISSION_ROW_MAPPER.mapRow(rs, rowNum);
			Submission submissionDto = new Submission();
			SubmissionUtils.copyDboToDto(submissionDbo, submissionDto);
			result.setSubmission(submissionDto);
			SubmissionStatusDBO statusDbo = SUBMISSION_STATUS_ROW_MAPPER.mapRow(rs, rowNum);
			SubmissionStatus statusDto = SubmissionUtils.convertDboToDto(statusDbo);
			result.setSubmissionStatus(statusDto);
			statusDto.setEntityId(submissionDto.getEntityId());
			statusDto.setVersionNumber(submissionDto.getVersionNumber());
			return result;
		}};

	private static final RowMapper<SubmissionContributorDBO> SUBMISSION_CONTRIBUTOR_ROW_MAPPER = 
			(new SubmissionContributorDBO()).getTableMapping();
	
	private static final String SUBTYPE_SUBMISSION = ViewObjectType.SUBMISSION.defaultSubType();
	
	@Override
	@WriteTransaction
	public String create(Submission dto) {
		ValidateArgument.required(dto, "Submission");

		// Convert to DBO
		SubmissionDBO dbo = new SubmissionDBO();
		SubmissionUtils.copyDtoToDbo(dto, dbo);

		// Ensure DBO has required information
		verifySubmissionDBO(dbo);

		// Create DBO
		try {
			dbo = basicDao.createNew(dbo);
			List<SubmissionContributorDBO> contributors = new ArrayList<SubmissionContributorDBO>();
			if (dto.getContributors()!=null) {
				for (SubmissionContributor sc : dto.getContributors()) {
					contributors.add(createContributorDbo(
							sc.getPrincipalId(), dto.getCreatedOn(), dto.getId()));						
				}
				if (!contributors.isEmpty()) basicDao.createBatch(contributors);
			}
			return dbo.getId().toString();
		} catch (Exception e) {
			throw new DatastoreException(e.getMessage() + " id=" + dbo.getId() +
					" userId=" + dto.getUserId() + " entityId=" + dto.getEntityId());
		}
	}

	private SubmissionContributorDBO createContributorDbo(String principalId, Date createdOn, String submissionid) {
		SubmissionContributorDBO dbo = new SubmissionContributorDBO();
		dbo.setId(idGenerator.generateNewId(IdType.SUBMISSION_CONTRIBUTOR_ID));
		dbo.setEtag(UUID.randomUUID().toString());
		dbo.setCreatedOn(createdOn);
		dbo.setPrincipalId(Long.parseLong(principalId));
		dbo.setSubmissionId(Long.parseLong(submissionid));
		return dbo;
	}



	@Override
	@WriteTransaction
	public void addSubmissionContributor(String submissionId, SubmissionContributor dto) {
		SubmissionContributorDBO dbo = createContributorDbo(
				dto.getPrincipalId(), dto.getCreatedOn(), submissionId);
		try {
			basicDao.createNew(dbo);
		} catch (IllegalArgumentException e) {
			if (e.getCause() instanceof DuplicateKeyException) {
				throw new IllegalArgumentException("The specified user may already be a contributor to this submission.", e);
			} else {
				throw e;
			}
		} catch (Exception e) {
			throw new DatastoreException("Failed to add contributor " + dbo.getPrincipalId() +
					" to submission " + submissionId, e);
		}
	}

	/*
	 * Given a list of submissions, retrieves the contributors and adds them
	 * in to the DTOs.  Used in the various query methods of this DAO.
	 */
	private void insertContributors(List<Submission> submissions) {
		if (submissions.isEmpty()) return;
		MapSqlParameterSource param = new MapSqlParameterSource();
		Map<String, Submission> submissionMap = new HashMap<String, Submission>();
		Set<String> submissionIds = new HashSet<String>();
		for (int i=0; i<submissions.size(); i++) {
			submissionIds.add(submissions.get(i).getId());
			submissionMap.put(submissions.get(i).getId(), submissions.get(i));
		}
		param.addValue(COL_SUBMISSION_CONTRIBUTOR_SUBMISSION_ID, submissionIds);
		// now select * from submission_contributor where submission_id in (...)
		List<SubmissionContributorDBO> contributorDbos = 
				namedJdbcTemplate.query(SELECT_CONTRIBUTORS_FOR_SUBMISSIONS, param, SUBMISSION_CONTRIBUTOR_ROW_MAPPER);
		for (SubmissionContributorDBO dbo : contributorDbos) {
			Submission sub = submissionMap.get(dbo.getSubmissionId().toString());
			if (sub==null) throw new IllegalStateException("Unrecognized submission Id "+dbo.getSubmissionId());
			Set<SubmissionContributor> contributorDtos = sub.getContributors();
			if (contributorDtos==null) {
				contributorDtos = new HashSet<SubmissionContributor>();
				sub.setContributors(contributorDtos);
			}
			contributorDtos.add(SubmissionUtils.convertDboToDto(dbo));
		}
	}
	
	private void insertContributorsInBundles(List<SubmissionBundle> bundles) {
		List<Submission> submissions = new ArrayList<Submission>(bundles.size());
		for (SubmissionBundle bundle : bundles) {
			submissions.add(bundle.getSubmission());
		}
		insertContributors(submissions);
	}


	@Override
	public Submission get(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		SubmissionDBO dbo = basicDao.getObjectByPrimaryKey(SubmissionDBO.class, param);
		Submission dto = new Submission();
		SubmissionUtils.copyDboToDto(dbo, dto);
		insertContributors(Collections.singletonList(dto));
		return dto;
	}
	
	@Override
	public SubmissionBundle getBundle(String id) {
		return getBundle(id, true);
	}
	
	@Override
	public SubmissionBundle getBundle(String id, boolean includeContributors) {
		try {
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue(ID, id);
			SubmissionBundle dto = namedJdbcTemplate.queryForObject(SELECT_BUNDLE_SQL, param, BUNDLE_ROW_MAPPER);
			if (includeContributors) {
				insertContributorsInBundles(Collections.singletonList(dto));
			}
			return dto;
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Cannot find submission or status for id " + id);
		}
	}



	@Override
	public long getCount() throws DatastoreException, NotFoundException {
		return basicDao.getCount(SubmissionDBO.class);
	}

	@Override
	public List<Submission> getAllByUser(String userId, long limit, long offset) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SQLConstants.OFFSET_PARAM_NAME, offset);
		param.addValue(SQLConstants.LIMIT_PARAM_NAME, limit);
		param.addValue(USER_ID, userId);
		List<SubmissionDBO> dbos = namedJdbcTemplate.query(SELECT_BY_USER_SQL, param, SUBMISSION_ROW_MAPPER);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			SubmissionUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		insertContributors(dtos);
		return dtos;
	}

	@Override
	public long getCountByUser(String userId) throws DatastoreException, NotFoundException {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(USER_ID, userId);
		return namedJdbcTemplate.queryForObject(COUNT_BY_USER_SQL, parameters, Long.class);
	}

	@Override
	public List<Submission> getAllByEvaluation(String evalId, long limit, long offset) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SQLConstants.OFFSET_PARAM_NAME, offset);
		param.addValue(SQLConstants.LIMIT_PARAM_NAME, limit);
		param.addValue(EVAL_ID, evalId);		
		List<SubmissionDBO> dbos = namedJdbcTemplate.query(SELECT_BY_EVALUATION_SQL, param, SUBMISSION_ROW_MAPPER);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			SubmissionUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		insertContributors(dtos);
		return dtos;
	}

	@Override
	public List<SubmissionBundle> getAllBundlesByEvaluation(String evalId, long limit, long offset) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SQLConstants.OFFSET_PARAM_NAME, offset);
		param.addValue(SQLConstants.LIMIT_PARAM_NAME, limit);
		param.addValue(EVAL_ID, evalId);		
		List<SubmissionBundle> dtos = namedJdbcTemplate.query(SELECT_BUNDLES_BY_EVALUATION_SQL, param, BUNDLE_ROW_MAPPER);
		insertContributorsInBundles(dtos);
		return dtos;
	}

	@Override
	public long getCountByEvaluation(String evalId) throws DatastoreException, NotFoundException {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(EVAL_ID, evalId);
		return namedJdbcTemplate.queryForObject(COUNT_BY_EVAL_SQL, parameters, Long.class);
	}

	@Override
	public List<Submission> getAllByEvaluationAndUser(String evalId, String principalId, long limit, long offset) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SQLConstants.OFFSET_PARAM_NAME, offset);
		param.addValue(SQLConstants.LIMIT_PARAM_NAME, limit);	
		param.addValue(USER_ID, principalId);
		param.addValue(EVAL_ID, evalId);
		List<SubmissionDBO> dbos = namedJdbcTemplate.query(SELECT_BY_EVAL_AND_USER_SQL, param, SUBMISSION_ROW_MAPPER);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			SubmissionUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		insertContributors(dtos);
		return dtos;
	}


	@Override
	public List<SubmissionBundle> getAllBundlesByEvaluationAndUser(
			String evalId, String principalId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SQLConstants.OFFSET_PARAM_NAME, offset);
		param.addValue(SQLConstants.LIMIT_PARAM_NAME, limit);
		param.addValue(EVAL_ID, evalId);		
		param.addValue(USER_ID, principalId);
		List<SubmissionBundle> dtos = namedJdbcTemplate.query(SELECT_BUNDLES_BY_EVAL_AND_USER_SQL, param, BUNDLE_ROW_MAPPER);
		insertContributorsInBundles(dtos);
		return dtos;
	}
	
	@Override
	public long getCountByEvaluationAndUser(String evalId, String userId) throws DatastoreException, NotFoundException {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(EVAL_ID, evalId);
		parameters.put(USER_ID, userId);
		return namedJdbcTemplate.queryForObject(COUNT_BY_EVAL_AND_USER_SQL, parameters, Long.class);
	}

	@Override
	public List<Submission> getAllByEvaluationAndStatus(String evalId, SubmissionStatusEnum status, long limit, long offset) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SQLConstants.OFFSET_PARAM_NAME, offset);
		param.addValue(SQLConstants.LIMIT_PARAM_NAME, limit);	
		param.addValue(EVAL_ID, evalId);
		param.addValue(STATUS, status.ordinal());
		List<SubmissionDBO> dbos = namedJdbcTemplate.query(SELECT_BY_EVAL_AND_STATUS_SQL, param, SUBMISSION_ROW_MAPPER);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			SubmissionUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		insertContributors(dtos);
		return dtos;
	}
	
	@Override
	public List<SubmissionBundle> getAllBundlesByEvaluationAndStatus(String evalId, SubmissionStatusEnum status, long limit, long offset) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SQLConstants.OFFSET_PARAM_NAME, offset);
		param.addValue(SQLConstants.LIMIT_PARAM_NAME, limit);
		param.addValue(EVAL_ID, evalId);		
		param.addValue(STATUS, status.ordinal());
		List<SubmissionBundle> dtos = namedJdbcTemplate.query(SELECT_BUNDLES_BY_EVAL_AND_STATUS_SQL, param, BUNDLE_ROW_MAPPER);
		insertContributorsInBundles(dtos);
		return dtos;
	}



	@Override
	public long getCountByEvaluationAndStatus(String evalId, SubmissionStatusEnum status) throws DatastoreException, NotFoundException {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(EVAL_ID, evalId);
		parameters.put(STATUS, status.ordinal());
		return namedJdbcTemplate.queryForObject(COUNT_BY_EVAL_AND_STATUS_SQL, parameters, Long.class);
	}

	@Override
	@WriteTransaction
	public void delete(String id) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		basicDao.deleteObjectByPrimaryKey(SubmissionDBO.class, param);
	}

	/**
	 * Ensure that a SubmissionDBO object has all required components
	 * 
	 * @param dbo
	 */
	private void verifySubmissionDBO(SubmissionDBO dbo) {
		ValidateArgument.required(dbo.getEvalId(), "Evaluation ID");
		ValidateArgument.required(dbo.getUserId(), "User ID");
		ValidateArgument.required(dbo.getEntityId(), "Entity ID");
		ValidateArgument.required(dbo.getVersionNumber(), "Entity Version");
		ValidateArgument.required(dbo.getId(), "Submission ID");
		ValidateArgument.required(dbo.getCreatedOn(), "Creation date");
	}	

	/*
	 * count the submissions for the given team in the given evaluation queue,
	 * optionally filtered by time segment and/or statuses
	 */
	@Override
	public long countSubmissionsByTeam(long evaluationId, long teamId, Date startDateIncl, Date endDateExcl, 
			Set<SubmissionStatusEnum> statuses) {
		StringBuilder sql = new StringBuilder(COUNT_SUBMISSIONS_FROM_TEAM);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBMISSION_EVAL_ID, evaluationId);
		param.addValue(COL_SUBMISSION_TEAM_ID, teamId);

		addStartEndAndStatusClauses(sql, param,startDateIncl, endDateExcl, statuses);
		String sqlString = sql.toString();
		return namedJdbcTemplate.queryForObject(sqlString, param, Long.class);
	}

	private static void addStartEndAndStatusClauses(StringBuilder sql, MapSqlParameterSource param,
			Date startDateIncl, Date endDateExcl, Set<SubmissionStatusEnum> statuses) {
		if (startDateIncl!=null) {
			sql.append(SUBMISSION_CREATED_ON_OR_AFTER);
			param.addValue(CREATED_ON_AFTER_PARAM, startDateIncl.getTime());
		}
		if (endDateExcl!=null) {
			sql.append(SUBMISSION_CREATED_BEFORE);
			param.addValue(CREATED_ON_BEFORE_PARAM, endDateExcl.getTime());
		}
		if (statuses!=null && !statuses.isEmpty()) {
			Set<Integer> statusOrdinals = new HashSet<Integer>();
			for (SubmissionStatusEnum status : statuses) statusOrdinals.add(status.ordinal());
			param.addValue(COL_SUBSTATUS_STATUS, statusOrdinals);
			sql.append(SUBSTATUS_IN_CLAUSE);
		}
	}

	/*
	 * count the submissions by the members of the given team in the given evaluation queue
	 * optionally filtered by time segment and/or statuses
	 */
	@Override
	public Map<Long,Long> countSubmissionsByTeamMembers(long evaluationId, long teamId, 
			Date startDateIncl, Date endDateExcl, Set<SubmissionStatusEnum> statuses) {
		StringBuilder sql = new StringBuilder(COUNT_SUBMISSIONS_FROM_TEAM_MEMBERS);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBMISSION_EVAL_ID, evaluationId);
		param.addValue(COL_GROUP_MEMBERS_GROUP_ID, teamId);
		addStartEndAndStatusClauses(sql, param,startDateIncl, endDateExcl, statuses);
		sql.append(GROUP_BY_CONTRIBUTOR);
		final Map<Long,Long> result = new HashMap<Long,Long>();
		String sqlString = sql.toString();
		// note: rather than get the result from the returned values of 'query()', we
		// insert directly into the desired Map data structure, 'result'.
		namedJdbcTemplate.query(sqlString, param,
				new RowMapper<Void>() {
			@Override
			public Void mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				result.put(
						rs.getLong(COL_SUBMISSION_CONTRIBUTOR_PRINCIPAL_ID), 
						rs.getLong(SUBMISSION_COUNT_LABEL));
				return null;
			}

		});
		return result;
	}

	/*
	 * return the number of submissions involving the given contributor in the given evaluation queue,
	 * optionally filtered by time segment and/or sub-statuses
	 */
	@Override
	public long countSubmissionsByContributor(long evaluationId, long contributorId, Date startDateIncl, Date endDateExcl, Set<SubmissionStatusEnum> statuses) {
		StringBuilder sql = new StringBuilder(COUNT_SUBMISSIONS_FROM_CONTRIBUTOR);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBMISSION_EVAL_ID, evaluationId);
		param.addValue(COL_SUBMISSION_CONTRIBUTOR_PRINCIPAL_ID, contributorId);
		addStartEndAndStatusClauses(sql, param, startDateIncl, endDateExcl, statuses);
		return namedJdbcTemplate.queryForObject(sql.toString(), param, Long.class);
	}

	/*
	 * list the team members in the given team who have submitted individually or on another team
	 * in the specified time interval (optional), filtered by the given sub-statuses (optional)
	 * 
	 */
	@Override
	public List<Long> getTeamMembersSubmittingElsewhere(long evaluationId, long teamId, 
			Date startDateIncl, Date endDateExcl, Set<SubmissionStatusEnum> statuses) {
		StringBuilder sql = new StringBuilder(COUNT_SUBMISSIONS_FROM_TEAM_MEMBERS);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBMISSION_EVAL_ID, evaluationId);
		param.addValue(COL_GROUP_MEMBERS_GROUP_ID, teamId);
		sql.append(SUBMIT_ELSEWHERE_CLAUSE);
		addStartEndAndStatusClauses(sql, param,startDateIncl, endDateExcl, statuses);
		sql.append(GROUP_BY_CONTRIBUTOR);
		final List<Long> result = new ArrayList<Long>();
		String sqlString = sql.toString();
		// note: rather than get the result from the returned values of 'query()', we
		// insert directly into the desired list data structure, 'result'.
		namedJdbcTemplate.query(sqlString, param,
				new RowMapper<Void>() {
			@Override
			public Void mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				if (rs.getLong(SUBMISSION_COUNT_LABEL)>0) {
					result.add(rs.getLong(COL_SUBMISSION_CONTRIBUTOR_PRINCIPAL_ID));
				}
				return null;
			}

		});
		return result;
	}

	/*
	 * Determine whether the given user has contributed to any team submission in the given
	 * evaluation, in the specified time interval (optional), filtered by the given sub-statues (optional)
	 * 
	 */
	@Override
	public boolean hasContributedToTeamSubmission(long evaluationId, long contributorId,
			Date startDateIncl, Date endDateExcl, Set<SubmissionStatusEnum> statuses) {
		StringBuilder sql = new StringBuilder(COUNT_SUBMISSIONS_FROM_CONTRIBUTOR);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBMISSION_EVAL_ID, evaluationId);
		param.addValue(COL_SUBMISSION_CONTRIBUTOR_PRINCIPAL_ID, contributorId);
		sql.append(IS_TEAM_SUBMISSION_CLAUSE);
		addStartEndAndStatusClauses(sql, param, startDateIncl, endDateExcl, statuses);
		return namedJdbcTemplate.queryForObject(sql.toString(), param, Long.class) > 0;
	}

	@Override
	public String getCreatedBy(String submissionId) {
		ValidateArgument.required(submissionId, "submissionId");
		try {
			return jdbcTemplate.queryForObject(SELECT_CREATED_BY, String.class, submissionId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}
	
	@Override
	public boolean isDockerRepoNameInAnyEvaluationWithAccess(String dockerRepoName,
			Set<Long> principalIds, ACCESS_TYPE accessType) {
		ValidateArgument.required(dockerRepoName, "dockerRepoName");
		ValidateArgument.required(principalIds, "principalIds");
		ValidateArgument.required(accessType, "accessType");
		if (principalIds.isEmpty()) return false;
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		
		param.addValue(COL_SUBMISSION_DOCKER_REPO_NAME, dockerRepoName);
		param.addValue(COL_RESOURCE_ACCESS_GROUP_ID, principalIds);
		param.addValue(COL_RESOURCE_ACCESS_TYPE_ELEMENT, accessType.name());

		Long count = namedJdbcTemplate.queryForObject(SUBMISSIONS_WITH_DOCKER_REPO_AND_PERMISSION_SQL, param, Long.class);
		
		return count > 0;
	}
	
	@Override
	public List<IdAndEtag> getSubmissionIdAndEtag(Long evaluationId) {
		ValidateArgument.required(evaluationId, "evaluationId");
		
		MapSqlParameterSource param = new MapSqlParameterSource(EVAL_ID, evaluationId);
		
		List<IdAndEtag> result = new ArrayList<>();
		
		namedJdbcTemplate.query(SELECT_SUBMISSION_ID_AND_ETAG, param, (ResultSet rs) -> {
			Long id = rs.getLong(COL_SUBMISSION_ID);
			String etag = rs.getString(COL_SUBSTATUS_ETAG);
			
			result.add(new IdAndEtag(id, etag, evaluationId));
		});

		return result;
	}
	
	
	@Override
	public Map<Long, Long> getSumOfSubmissionCRCsForEachEvaluation(List<Long> evaluationIds) {
		ValidateArgument.required(evaluationIds, "evaluationIds");
		
		if (evaluationIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		MapSqlParameterSource param = new MapSqlParameterSource(EVAL_ID, evaluationIds);
		
		Map<Long, Long> result = new HashMap<>(evaluationIds.size());
		
		namedJdbcTemplate.query(SELECT_SUM_CRC_SUBMISSIONS, param, (ResultSet rs) -> {
			Long evaluationId = rs.getLong(COL_SUBMISSION_EVAL_ID);
			Long sumCRC = rs.getLong(CRC);
			result.put(evaluationId, sumCRC);
		});
		
		return result;
	}
	
	@Override
	public List<ObjectDataDTO> getSubmissionData(List<Long> submissionIds, int maxAnnotationChars) {
		ValidateArgument.required(submissionIds, "submissionsIds");
		
		if (submissionIds.isEmpty()) {
			return Collections.emptyList();
		}
		
		MapSqlParameterSource param = new MapSqlParameterSource(ID, submissionIds);
		
		return namedJdbcTemplate.query(SELECT_SUBMISSION_DATA, param, (ResultSet rs, int index) -> {
			return mapSubmissionDataRow(rs, index, maxAnnotationChars);
		});
	}
	
	@WriteTransaction
	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_SUBMISSION);
		jdbcTemplate.update("DELETE FROM " + TABLE_EVALUATION);
	}

	@Override
	public boolean hasSubmissionForEvaluationRound(String evalId, String evalRoundId){
		MapSqlParameterSource parameterSource = new MapSqlParameterSource();
		parameterSource.addValue(DBOConstants.PARAM_SUBMISSION_EVAL_ID, evalId);
		parameterSource.addValue(DBOConstants.PARAM_SUBMISSION_EVAL_ROUND_ID, evalRoundId);

		String sql = "SELECT COUNT(*) > 0 FROM " + TABLE_SUBMISSION +
				" WHERE " + COL_SUBMISSION_EVAL_ROUND_ID + " = :" + DBOConstants.PARAM_SUBMISSION_EVAL_ROUND_ID +
				" AND " + COL_SUBMISSION_EVAL_ID + " = :" + DBOConstants.PARAM_SUBMISSION_EVAL_ID;

		return namedJdbcTemplate.queryForObject(sql, parameterSource, Boolean.class);
	}
	
	private static ObjectDataDTO mapSubmissionDataRow(ResultSet rs, int index, int maxAnnotationChars) throws SQLException {
		ObjectDataDTO data = new ObjectDataDTO();
		
		data.setId(rs.getLong(COL_SUBMISSION_ID));
		data.setName(rs.getString(COL_SUBMISSION_NAME));
		data.setEtag(rs.getString(COL_SUBSTATUS_ETAG));
		data.setParentId(rs.getLong(SubmissionField.evaluationid.getColumnAlias()));
		data.setBenefactorId(rs.getLong(SubmissionField.evaluationid.getColumnAlias()));
		data.setProjectId(rs.getLong(PROJECT_ID));
		data.setCreatedOn(new Date(rs.getLong(COL_SUBMISSION_CREATED_ON)));
		data.setCreatedBy(rs.getLong(CREATED_BY));
		data.setModifiedOn(new Date(rs.getLong(COL_SUBSTATUS_MODIFIED_ON)));
		data.setModifiedBy(rs.getLong(MODIFIED_BY));
		data.setCurrentVersion(rs.getLong(COL_SUBSTATUS_VERSION));
		data.setSubType(SUBTYPE_SUBMISSION);
	
		List<ObjectAnnotationDTO> annotations = fetchAnnotations(rs, data.getId(), maxAnnotationChars);
		
		data.setAnnotations(annotations);
	
		return data;
		
	}
	
	private static List<ObjectAnnotationDTO> fetchAnnotations(ResultSet rs, Long submissionId, int maxAnnotationChars) throws SQLException {
		
		Annotations annotations = AnnotationsV2Utils.fromJSONString(rs.getString(COL_SUBSTATUS_ANNOTATIONS));
		
		if (annotations == null) {
			annotations = AnnotationsV2Utils.emptyAnnotations();
		}
		
		// Translates the annotations on the object itself first
		List<ObjectAnnotationDTO> objectAnnotations = AnnotationsV2Utils.translate(submissionId, annotations, maxAnnotationChars);
		
		// Merge the custom default fields from the result set and return the complete list
		return mergeCustomFields(submissionId, objectAnnotations, rs);
	}

	private static List<ObjectAnnotationDTO> mergeCustomFields(Long submissionId, List<ObjectAnnotationDTO> objectAnnotations, ResultSet rs) throws SQLException {
		// Turn the list into a map so that we can override the keys
		Map<String, ObjectAnnotationDTO> map = new LinkedHashMap<>(objectAnnotations.size());
		
		objectAnnotations.forEach((annotation) ->  map.put(annotation.getKey(), annotation));
		
		// Now add the custom fields, they will override any annotation with the same name
		for (SubmissionField field : SubmissionField.values()) {
			
			// Makes sure the annotation do not override a field
			map.remove(field.getColumnName());
			
			String value = field.getValue(rs);
			
			// Skip empty fields
			if (StringUtils.isBlank(value)) {
				continue;
			}

			ObjectAnnotationDTO annotationValue = new ObjectAnnotationDTO();
			
			annotationValue.setObjectId(submissionId);
			annotationValue.setKey(field.getColumnName());
			annotationValue.setType(field.getAnnotationType());	
			annotationValue.setValue(value);
			
			map.put(field.getColumnName(), annotationValue);
		}
		
		return new ArrayList<>(map.values());
	}

}
