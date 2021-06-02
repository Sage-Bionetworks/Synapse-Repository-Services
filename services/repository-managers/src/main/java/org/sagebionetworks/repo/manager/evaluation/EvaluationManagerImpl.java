package org.sagebionetworks.repo.manager.evaluation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.EvaluationFilter;
import org.sagebionetworks.evaluation.dao.EvaluationSubmissionsDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dbo.EvaluationRoundTranslationUtil;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimit;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimitType;
import org.sagebionetworks.evaluation.model.EvaluationRoundListRequest;
import org.sagebionetworks.evaluation.model.EvaluationRoundListResponse;
import org.sagebionetworks.evaluation.model.SubmissionQuota;
import org.sagebionetworks.evaluation.model.TeamSubmissionEligibility;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NameValidation;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class EvaluationManagerImpl implements EvaluationManager {

	@Autowired
	private EvaluationDAO evaluationDAO;

	@Autowired
	private NodeDAO nodeDAO;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private AuthorizationManager authorizationManager;

	@Autowired
	private EvaluationPermissionsManager evaluationPermissionsManager;
	
	@Autowired
	private EvaluationSubmissionsDAO evaluationSubmissionsDAO;

	@Autowired
	private SubmissionDAO submissionDAO;

	@Autowired
	private SubmissionEligibilityManager submissionEligibilityManager;

	static final String NON_EXISTENT_ROUND_ID = "-1";

	@Override
	@WriteTransaction
	public Evaluation createEvaluation(UserInfo userInfo, Evaluation eval) 
			throws DatastoreException, InvalidModelException, NotFoundException {

		UserInfo.validateUserInfo(userInfo);
		validateQuota(eval.getQuota());

		final String nodeId = eval.getContentSource();
		if (nodeId == null || nodeId.isEmpty()) {
			throw new IllegalArgumentException("Evaluation " + eval.getId() +
					" is missing content source (are you sure there is Synapse entity for it?).");
		}
		if (!authorizationManager.canAccess(userInfo, nodeId, ObjectType. ENTITY, ACCESS_TYPE.CREATE).isAuthorized()) {
			throw new UnauthorizedException("User " + userInfo.getId().toString() +
					" must have " + ACCESS_TYPE.CREATE.name() + " right on the entity " +
					nodeId + " in order to create a evaluation based on it.");
		}

		if (!nodeDAO.getNodeTypeById(nodeId).equals(EntityType.project)) {
			throw new IllegalArgumentException("Evaluation " + eval.getId() +
					" could not be created because the parent entity " + nodeId +  " is not a project.");
		}

		// Create the evaluation
		eval.setName(NameValidation.validateName(eval.getName()));
		eval.setId(idGenerator.generateNewId(IdType.EVALUATION_ID).toString());
		eval.setCreatedOn(new Date());
		String principalId = userInfo.getId().toString();
		String id = evaluationDAO.create(eval, Long.parseLong(principalId));

		// Create the default ACL
		AccessControlList acl =  AccessControlListUtil.
				createACLToGrantEvaluationAdminAccess(eval.getId(), userInfo, new Date());
		evaluationPermissionsManager.createAcl(userInfo, acl);
		
		evaluationSubmissionsDAO.createForEvaluation(KeyFactory.stringToKey(id));

		return evaluationDAO.get(id);
	}

	@Override
	public Evaluation getEvaluation(UserInfo userInfo, String id)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		ValidateArgument.required(id, "Evaluation ID");

		return validateEvaluationAccess(userInfo, id, ACCESS_TYPE.READ);
	}
	
	@Override
	public List<Evaluation> getEvaluationsByContentSource(UserInfo userInfo, String id, ACCESS_TYPE accessType, boolean activeOnly, List<Long> evaluationIds, long limit, long offset)
			throws DatastoreException, NotFoundException {
		ValidateArgument.required(id, "Entity ID");
		ValidateArgument.required(userInfo, "User info");
		ValidateArgument.required(accessType, "The access type");
		
		Long now = activeOnly ? System.currentTimeMillis() : null;
		
		EvaluationFilter filter = new EvaluationFilter(userInfo, accessType)
				.withTimeFilter(now)
				.withContentSourceFilter(id)
				.withIdsFilter(evaluationIds);
		
		return evaluationDAO.getAccessibleEvaluations(filter, limit, offset);
	}

	@Override
	public List<Evaluation> getEvaluations(UserInfo userInfo, ACCESS_TYPE accessType, boolean activeOnly, List<Long> evaluationIds, long limit, long offset)
			throws DatastoreException, NotFoundException {
		ValidateArgument.required(userInfo, "User info");
		ValidateArgument.required(accessType, "The access type");
		
		Long now = activeOnly ? System.currentTimeMillis() : null;
		
		EvaluationFilter filter = new EvaluationFilter(userInfo, accessType)
				.withTimeFilter(now)
				.withIdsFilter(evaluationIds);
		
		return evaluationDAO.getAccessibleEvaluations(filter, limit, offset);
	}

	@Override
	public List<Evaluation> getAvailableEvaluations(UserInfo userInfo, boolean activeOnly, List<Long> evaluationIds, long limit, long offset)
			throws DatastoreException, NotFoundException {
		return getEvaluations(userInfo, ACCESS_TYPE.SUBMIT, activeOnly, evaluationIds, limit, offset);
	}

	@Override
	public Evaluation findEvaluation(UserInfo userInfo, String name)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		ValidateArgument.required(name, "Name");
		String evalId = evaluationDAO.lookupByName(name);

		try {
			return validateEvaluationAccess(userInfo, evalId, ACCESS_TYPE.READ);
		} catch(NotFoundException | UnauthorizedException e){
			throw new NotFoundException("No Evaluation found with name " + name);
		}
	}
	
	@Override
	@WriteTransaction
	public Evaluation updateEvaluation(UserInfo userInfo, Evaluation eval)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		// validate arguments
		ValidateArgument.required(eval, "Evaluation");
		final String evalId = eval.getId();
		validateQuota(eval.getQuota());

		// fetch the existing Evaluation and validate changes		
		Evaluation old = validateEvaluationAccess(userInfo, evalId,ACCESS_TYPE.UPDATE);
		validateEvaluation(old, eval);

		ValidateArgument.requirement(eval.getQuota() == null || !evaluationDAO.hasEvaluationRounds(evalId),
				"DEPRECATED! SubmissionQuota is a DEPRECATED feature and can not co-exist with EvaluationRounds." +
					" You must first delete your Evaluation's EvaluationRounds in order to use SubmissionQuota.");

		// perform the update
		evaluationDAO.update(eval);
		return evaluationDAO.get(evalId);
	}
	
	@Override
	public void updateEvaluationEtag(String evalId) throws NotFoundException {
		Evaluation comp = evaluationDAO.get(evalId);
		if (comp == null) throw new NotFoundException("No Evaluation found with id " + evalId);
		evaluationDAO.update(comp);
	}

	@Override
	@WriteTransaction
	public void deleteEvaluation(UserInfo userInfo, String id) throws DatastoreException, NotFoundException, UnauthorizedException {
		ValidateArgument.required(id, "Evaluation ID");
		validateEvaluationAccess(userInfo, id, ACCESS_TYPE.DELETE);
		evaluationPermissionsManager.deleteAcl(userInfo, id);
		// lock out multi-submission access (e.g. batch updates)
		evaluationSubmissionsDAO.deleteForEvaluation(Long.parseLong(id));
		evaluationDAO.delete(id);
	}

	private static void validateQuota(SubmissionQuota quota){
		if (quota == null){
			return;
		}

		//Submission limit is not a required field
		ValidateArgument.requirement(quota.getSubmissionLimit() == null || quota.getSubmissionLimit() >= 0,
				"submissionLimit must be non-negative");

		//numberOfRounds, roundDurationMillis, and firstRoundStart are all required
		ValidateArgument.requirement(quota.getNumberOfRounds() != null && quota.getNumberOfRounds() >= 0,
				"numberOfRounds must be defined and be non-negative");

		ValidateArgument.requirement(quota.getRoundDurationMillis() != null && quota.getRoundDurationMillis() >= 0,
				"roundDurationMillis must be defined and non-negative");

		ValidateArgument.required(quota.getFirstRoundStart() , "firstRoundStart");
	}

	private static void validateEvaluation(Evaluation oldEval, Evaluation newEval) {
		if (!oldEval.getOwnerId().equals(newEval.getOwnerId())) {
			throw new InvalidModelException("Cannot overwrite Evaluation Owner ID");
		}
		if (!oldEval.getCreatedOn().equals(newEval.getCreatedOn())) {
			throw new InvalidModelException("Cannot overwrite CreatedOn date");
		}
	}

	@Override
	public TeamSubmissionEligibility getTeamSubmissionEligibility(UserInfo userInfo, String evalId, String teamId) throws NumberFormatException, DatastoreException, NotFoundException
	{
		evaluationPermissionsManager.canCheckTeamSubmissionEligibility(userInfo,  evalId,  teamId).checkAuthorizationOrElseThrow();
		Date now = new Date();
		return submissionEligibilityManager.getTeamSubmissionEligibility(evaluationDAO.get(evalId), teamId, now);
	}

	@WriteTransaction
	@Override
	public EvaluationRound createEvaluationRound(UserInfo userInfo, EvaluationRound evaluationRound){
		// Creating evaluation rounds are seen as updates to the Evaluation itself
		Evaluation evaluation = validateEvaluationAccess(userInfo, evaluationRound.getEvaluationId(), ACCESS_TYPE.UPDATE);
		validateNoExistingQuotaDefined(evaluation);

		Instant now = Instant.now();
		// verify start of round
		// allow 1 second leeway for cases where someone wants to create a round starting "now"
		if(now.minus(1, ChronoUnit.SECONDS).isAfter(evaluationRound.getRoundStart().toInstant())){
			throw new IllegalArgumentException("Can not create an EvaluationRound with a start date in the past.");
		}
		// verify end of round
		if( now.isAfter(evaluationRound.getRoundEnd().toInstant()) ){
			throw new IllegalArgumentException("Can not create an EvaluationRound with an end date in the past.");
		}

		validateNoDateRangeOverlap(evaluationRound, NON_EXISTENT_ROUND_ID);
		validateEvaluationRoundLimits(evaluationRound.getLimits());

		evaluationRound.setId(idGenerator.generateNewId(IdType.EVALUATION_ROUND_ID).toString());


		return evaluationDAO.createEvaluationRound(evaluationRound);
	}

	@WriteTransaction
	@Override
	public EvaluationRound updateEvaluationRound(UserInfo userInfo, EvaluationRound evaluationRound){

		Evaluation evaluation = validateEvaluationAccess(userInfo, evaluationRound.getEvaluationId(), ACCESS_TYPE.UPDATE);
		validateNoExistingQuotaDefined(evaluation);

		Instant now = Instant.now();
		EvaluationRound storedRound = evaluationDAO.getEvaluationRound(evaluationRound.getEvaluationId(), evaluationRound.getId());
		// verify updating start of round
		if( !storedRound.getRoundStart().equals(evaluationRound.getRoundStart())
				&& now.isAfter(evaluationRound.getRoundStart().toInstant())
				&& submissionDAO.hasSubmissionForEvaluationRound(evaluationRound.getEvaluationId(), evaluationRound.getId())){
				throw new IllegalArgumentException("Can not update an EvaluationRound's start date after it has already started and Submissions have been made");
		}
		// verify updating end of round
		if( !storedRound.getRoundEnd().equals(evaluationRound.getRoundEnd())
				&& now.isAfter(evaluationRound.getRoundEnd().toInstant()) ){
			throw new IllegalArgumentException("Can not update an EvaluationRound's end date to a time in the past.");
		}

		validateNoDateRangeOverlap(evaluationRound, evaluationRound.getId());
		validateEvaluationRoundLimits(evaluationRound.getLimits());

		evaluationDAO.updateEvaluationRound(evaluationRound);
		return evaluationDAO.getEvaluationRound(evaluationRound.getEvaluationId(), evaluationRound.getId());
	}

	@WriteTransaction
	@Override
	public void deleteEvaluationRound(UserInfo userInfo, String evaluationId, String evaluationRoundId){
		// Deleting evaluation rounds are seen as updates to the Evaluation itself
		validateEvaluationAccess(userInfo, evaluationId, ACCESS_TYPE.UPDATE);

		EvaluationRound round = evaluationDAO.getEvaluationRound(evaluationId, evaluationRoundId);
		if(Instant.now().isAfter(round.getRoundStart().toInstant())
			&& submissionDAO.hasSubmissionForEvaluationRound(evaluationId, evaluationRoundId)){
			throw new IllegalArgumentException("Can not delete an EvaluationRound after it has already started and Submissions have been made");
		}

		evaluationDAO.deleteEvaluationRound(evaluationId, evaluationRoundId);
	}


	@Override
	public EvaluationRound getEvaluationRound(UserInfo userInfo, String evaluationId, String evaluationRoundId){
		validateEvaluationAccess(userInfo, evaluationId, ACCESS_TYPE.READ);
		return evaluationDAO.getEvaluationRound(evaluationId, evaluationRoundId);
	}

	@Override
	public EvaluationRoundListResponse getAllEvaluationRounds(UserInfo userInfo, String evaluationId, EvaluationRoundListRequest request){
		ValidateArgument.required(request, "request");
		ValidateArgument.requiredNotBlank(evaluationId, "evaluationId");

		validateEvaluationAccess(userInfo, evaluationId, ACCESS_TYPE.READ);

		NextPageToken nextPageToken = new NextPageToken(request.getNextPageToken());

		List<EvaluationRound> rounds = evaluationDAO.getAssociatedEvaluationRounds(evaluationId, nextPageToken.getLimitForQuery(), nextPageToken.getOffset());

		//build response
		String newNextPageToken = nextPageToken.getNextPageTokenForCurrentResults(rounds);
		EvaluationRoundListResponse response = new EvaluationRoundListResponse();
		response.setNextPageToken(newNextPageToken);
		response.setPage(rounds);

		return response;
	}


	Evaluation validateEvaluationAccess(UserInfo userInfo, String evaluationId, ACCESS_TYPE accessType){
		// validate arguments
		UserInfo.validateUserInfo(userInfo);

		// verify the Evaluation exists
		// NotFoundError is thrown by DAO if Evaluation does not exist
		Evaluation eval = evaluationDAO.get(evaluationId);

		// validate permissions
		if (!evaluationPermissionsManager.hasAccess(userInfo, evaluationId, accessType).isAuthorized()) {
			throw new UnauthorizedException("User " + userInfo.getId().toString() +
					" is not authorized to "+ accessType.name() +" evaluation " + evaluationId +
					" (" + eval.getName() + ")");
		}

		return eval;
	}

	void validateNoDateRangeOverlap(EvaluationRound evaluationRound, String currentRoundId){
		Instant roundStart = evaluationRound.getRoundStart().toInstant();
		Instant roundEnd = evaluationRound.getRoundEnd().toInstant();

		if(roundStart.isAfter(roundEnd)){
			throw new IllegalArgumentException("EvaluationRound can not end before it starts");
		}

		List<EvaluationRound> overlappingRounds = evaluationDAO.overlappingEvaluationRounds(
				evaluationRound.getEvaluationId(),
				currentRoundId,
				// roundStart and roundEnd are guaranteed not null by schema definition
				roundStart,
				roundEnd
		);

		if(!overlappingRounds.isEmpty()){
			List<String> overlappingRoundIds = overlappingRounds.stream().map(EvaluationRound::getId).collect(Collectors.toList());
			throw new IllegalArgumentException("This round's date range overlaps with the following round IDs: " + overlappingRoundIds);
		}
	}

	/**
	 * Verifies that the current Evaluation does not have a SubmissionQuota defined.
	 * SubmissionQuotas and Evaluation rounds are different ways of defining rounds withing an Evaluation,
	 * and therefore can not be used in conjunction.
	 *
	 * The eventual goal is to migrate all round definitions over to the EvaluationRounds
	 * once all clients have removed support for SubmissionQuotas and added support for EvaluationRounds.
	 * @param evaluation
	 * @return
	 */
	void validateNoExistingQuotaDefined(Evaluation evaluation){
		ValidateArgument.requirement(evaluation.getQuota() == null,"A SubmissionQuota, which is deprecated," +
				" must not be defined for an Evaluation." +
					" You must first remove your Evaluation's SubmissionQuota or" +
					" convert the SubmissionQuota into EvaluationRounds automatically to via the EvaluationRound migration service");

	}

	void validateEvaluationRoundLimits(List<EvaluationRoundLimit> evaluationRoundLimits){
		if(CollectionUtils.isEmpty(evaluationRoundLimits)){
			// nothing to validate
			return;
		}

		Set<EvaluationRoundLimitType> limitTypes = EnumSet.noneOf(EvaluationRoundLimitType.class);
		for(EvaluationRoundLimit limit : evaluationRoundLimits){
			if(limit == null){
				throw new IllegalArgumentException("EvaluationRoundLimit can not be null");
			}

			EvaluationRoundLimitType limitType = limit.getLimitType();
			if(limitTypes.contains(limitType)){
				throw new IllegalArgumentException("You may only have 1 limit of type: " + limitType);
			}
			limitTypes.add(limitType);

			if(limit.getMaximumSubmissions() < 0){
				throw new IllegalArgumentException("maxSubmissions must be a positive integer");
			}
		}
	}

	@Override
	@WriteTransaction
	public void migrateSubmissionQuota(UserInfo userInfo, String evaluationId){
		Evaluation evaluation = validateEvaluationAccess(userInfo, evaluationId,ACCESS_TYPE.UPDATE);

		ValidateArgument.requirement(evaluation.getQuota() != null, "The evaluation does not have a SubmissionQuota to convert");

		List<EvaluationRound> rounds = EvaluationRoundTranslationUtil.fromSubmissionQuota(evaluation, idGenerator);

		for(EvaluationRound round : rounds) {
			evaluationDAO.createEvaluationRound(round);
		}

		//once rounds are created, set the evaluationQuota to null
		evaluation.setQuota(null);
		evaluationDAO.update( evaluation);
	}

}
