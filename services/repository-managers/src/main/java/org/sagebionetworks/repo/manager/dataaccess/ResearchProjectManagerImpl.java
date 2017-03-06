package org.sagebionetworks.repo.manager.dataaccess;

import java.util.Date;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.ChangeOwnershipRequest;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class ResearchProjectManagerImpl implements ResearchProjectManager {

	public static final int EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT = 0;

	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private AccessRequirementDAO accessRequirementDao;
	@Autowired
	private ResearchProjectDAO researchProjectDao;

	@WriteTransactionReadCommitted
	@Override
	public ResearchProject create(UserInfo userInfo, ResearchProject toCreate) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(toCreate, "ResearchProject");
		ValidateArgument.required(toCreate.getAccessRequirementId(), "ResearchProject.accessRequirementId");
		ValidateArgument.requirement(toCreate.getProjectLead() != null
				&& toCreate.getProjectLead().length() > EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT,
				"ResearchProject.projectLead must contains more than "+EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT+" characters.");
		ValidateArgument.requirement(toCreate.getInstitution() != null
				&& toCreate.getInstitution().length() > EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT,
				"ResearchProject.projectLead must contains more than "+EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT+" characters.");
		ValidateArgument.requirement(toCreate.getIntendedDataUseStatement() != null
				&& toCreate.getIntendedDataUseStatement().length() > EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT,
				"ResearchProject.projectLead must contains more than "+EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT+" characters.");
		ValidateArgument.requirement(accessRequirementDao.get(toCreate.getAccessRequirementId()).getConcreteType()
				.equals(ACTAccessRequirement.class.getName()),
				"A ResearchProject can only associate with an ACTAccessRequirement.");
		toCreate = prepareCreationFields(toCreate, userInfo.getId().toString());
		return researchProjectDao.create(toCreate);
	}

	public ResearchProject prepareCreationFields(ResearchProject toCreate, String createdBy) {
		toCreate.setId(idGenerator.generateNewId(TYPE.RESEARCH_PROJECT_ID).toString());
		toCreate.setCreatedBy(createdBy);
		toCreate.setOwnerId(createdBy);
		toCreate.setCreatedOn(new Date());
		toCreate = prepareUpdateFields(toCreate, createdBy);
		return toCreate;
	}

	@Override
	public ResearchProject get(UserInfo userInfo, String accessRequirementId) throws NotFoundException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");

		return researchProjectDao.get(accessRequirementId, userInfo.getId().toString());
	}

	@WriteTransactionReadCommitted
	@Override
	public ResearchProject update(UserInfo userInfo, ResearchProject toUpdate)
			throws NotFoundException, UnauthorizedException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(toUpdate, "ResearchProject");
		ValidateArgument.required(toUpdate.getAccessRequirementId(), "ResearchProject.accessRequirementId");
		ValidateArgument.requirement(toUpdate.getProjectLead() != null
				&& toUpdate.getProjectLead().length() > EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT,
				"ResearchProject.projectLead must contains more than "+EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT+" characters.");
		ValidateArgument.requirement(toUpdate.getInstitution() != null
				&& toUpdate.getInstitution().length() > EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT,
				"ResearchProject.projectLead must contains more than "+EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT+" characters.");
		ValidateArgument.requirement(toUpdate.getIntendedDataUseStatement() != null
				&& toUpdate.getIntendedDataUseStatement().length() > EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT,
				"ResearchProject.projectLead must contains more than "+EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT+" characters.");

		ResearchProject original = researchProjectDao.get(toUpdate.getId());

		ValidateArgument.requirement(toUpdate.getOwnerId().equals(original.getOwnerId())
				&& toUpdate.getCreatedBy().equals(original.getCreatedBy())
				&& toUpdate.getCreatedOn().equals(original.getCreatedOn())
				&& toUpdate.getAccessRequirementId().equals(original.getAccessRequirementId()),
				"OwnerId, accessRequirementId, createdOn and createdBy fields cannot be editted.");

		if (!original.getOwnerId().equals(userInfo.getId().toString())) {
				throw new UnauthorizedException("Only owner can perform this action.");
		}
		if (!toUpdate.getEtag().equals(original.getEtag())) {
			throw new ConflictingUpdateException();
		}

		toUpdate = prepareUpdateFields(toUpdate, userInfo.getId().toString());
		return researchProjectDao.update(toUpdate);
	}

	public ResearchProject prepareUpdateFields(ResearchProject toUpdate, String modifiedBy) {
		toUpdate.setModifiedBy(modifiedBy);
		toUpdate.setModifiedOn(new Date());
		toUpdate.setEtag(UUID.randomUUID().toString());
		return toUpdate;
	}

	@WriteTransactionReadCommitted
	@Override
	public ResearchProject changeOwnership(UserInfo userInfo, ChangeOwnershipRequest request)
			throws NotFoundException, UnauthorizedException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "ChangeOwnershipRequest");
		ValidateArgument.required(request.getResearchProjectId(), "ChangeOwnershipRequest.researchProjectId");
		ValidateArgument.required(request.getNewOwnerId(), "ChangeOwnershipRequest.newOwnerId");

		if(!(authorizationManager.isACTTeamMemberOrAdmin(userInfo))){
			throw new UnauthorizedException("Only an ACT member can change ownership of a ResearchProject");
		}
		return researchProjectDao.changeOwnership(request.getResearchProjectId(),
				request.getNewOwnerId(), userInfo.getId().toString(),
				System.currentTimeMillis(), UUID.randomUUID().toString());
	}

}
