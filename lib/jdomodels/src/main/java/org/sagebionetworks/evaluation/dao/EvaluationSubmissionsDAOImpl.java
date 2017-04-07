package org.sagebionetworks.evaluation.dao;

import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_SUBMISSIONS_ETAG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_SUBMISSIONS_EVAL_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_EVALUATION_SUBMISSIONS;

import java.util.UUID;

import org.sagebionetworks.evaluation.dbo.EvaluationSubmissionsDBO;
import org.sagebionetworks.evaluation.model.EvaluationSubmissions;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.evaluation.EvaluationSubmissionsDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.sagebionetworks.repo.transactions.WriteTransaction;

public class EvaluationSubmissionsDAOImpl implements EvaluationSubmissionsDAO {
	private static final String SELECT_FOR_EVALUATION = "SELECT * FROM "+TABLE_EVALUATION_SUBMISSIONS+
			" WHERE "+COL_EVALUATION_SUBMISSIONS_EVAL_ID+" = ?";
	
	private static final String SELECT_FOR_EVALUATION_FOR_UPDATE = SELECT_FOR_EVALUATION+" FOR UPDATE";

	private static final String UPDATE_ETAG_FOR_EVALUATION = "UPDATE "+TABLE_EVALUATION_SUBMISSIONS+
			" SET "+COL_EVALUATION_SUBMISSIONS_ETAG+" = ?"+ " WHERE "+
			COL_EVALUATION_SUBMISSIONS_EVAL_ID+" = ?";
	
	private static final String DELETE_FOR_EVALUATION = "DELETE FROM "+TABLE_EVALUATION_SUBMISSIONS+
			" WHERE "+COL_EVALUATION_SUBMISSIONS_EVAL_ID+" = ?";
	
	private static final RowMapper<EvaluationSubmissionsDBO> EVAL_SUB_ROW_MAPPER = 
			(new EvaluationSubmissionsDBO()).getTableMapping();

	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;
	
	public EvaluationSubmissionsDAOImpl() {}
	
	// for testing
	public EvaluationSubmissionsDAOImpl(
			DBOBasicDao basicDao,
			IdGenerator idGenerator,
			JdbcTemplate jdbcTemplate,
			TransactionalMessenger transactionalMessenger
			) {
		this.basicDao=basicDao;
		this.idGenerator=idGenerator;
		this.jdbcTemplate=jdbcTemplate;
		this.transactionalMessenger=transactionalMessenger;
	}

	public static void copyDtoToDbo(EvaluationSubmissions dto, EvaluationSubmissionsDBO dbo) {
		dbo.setId(dto.getId());
		dbo.setEvaluationId(dto.getEvaluationId());
		dbo.setEtag(dto.getEtag());
	}
	
	public static void copyDboToDto(EvaluationSubmissionsDBO dbo, EvaluationSubmissions dto) {
		dto.setId(dbo.getId());
		dto.setEtag(dbo.getEtag());
		dto.setEvaluationId(dbo.getEvaluationId());
	}

	@Override
	@WriteTransaction
	public EvaluationSubmissions createForEvaluation(long evaluationId)
			throws DatastoreException {
		EvaluationSubmissionsDBO dbo = new EvaluationSubmissionsDBO();
		dbo.setId(idGenerator.generateNewId(IdType.EVALUATION_SUBMISSION_ID));
		dbo.setEvaluationId(evaluationId);
		// Generate a new eTag and CREATE message
		dbo.setEtag(UUID.randomUUID().toString());
		
		// create DBO
		dbo = basicDao.createNew(dbo);
		
        // send change message
		sendChangeMessage(evaluationId, dbo.getEtag(), ChangeType.CREATE);
		
		EvaluationSubmissions dto = new EvaluationSubmissions();
		copyDboToDto(dbo, dto);
		return dto;
	}
	
	private void sendChangeMessage(Long evaluationId, String etag, ChangeType changeType) {
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(changeType);
		message.setObjectType(ObjectType.EVALUATION_SUBMISSIONS);
		message.setObjectId(evaluationId.toString());
		message.setObjectEtag(etag);
		transactionalMessenger.sendMessageAfterCommit(message);
	}
	
	@Override
	public EvaluationSubmissions getForEvaluationIfExists(long evaluationId) {
		try {
			EvaluationSubmissionsDBO dbo = jdbcTemplate.queryForObject(SELECT_FOR_EVALUATION, new Object[]{evaluationId}, EVAL_SUB_ROW_MAPPER);
			EvaluationSubmissions dto = new EvaluationSubmissions();
			copyDboToDto(dbo, dto);
			return dto;		
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	@Override
	@WriteTransaction
	public EvaluationSubmissions lockAndGetForEvaluation(long evaluationId)
			throws NotFoundException {
		try {
			EvaluationSubmissionsDBO dbo = jdbcTemplate.queryForObject(SELECT_FOR_EVALUATION_FOR_UPDATE, new Object[]{evaluationId}, EVAL_SUB_ROW_MAPPER);
			EvaluationSubmissions dto = new EvaluationSubmissions();
			copyDboToDto(dbo, dto);
			return dto;		
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Cannot find EvaluationSubmissions for evaluation " + evaluationId);
		}
	}
	
	@Override
	@WriteTransaction
	public String updateEtagForEvaluation(long evaluationId, boolean sendChangeMessage, ChangeType changeType)
			throws DatastoreException, NotFoundException {
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(UPDATE_ETAG_FOR_EVALUATION, new Object[]{etag, evaluationId});
		if (sendChangeMessage) sendChangeMessage(evaluationId, etag, changeType);
		return etag;
	}

	@Override
	@WriteTransaction
	public void deleteForEvaluation(long evaluationId)
			throws DatastoreException, NotFoundException {
		jdbcTemplate.update(DELETE_FOR_EVALUATION, new Object[]{evaluationId});
		sendChangeMessage(evaluationId, null, ChangeType.DELETE);
	}

}
