package org.sagebionetworks.repo.manager.dataaccess;

import java.util.Date;

import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class ResearchProjectManagerImpl implements ResearchProjectManager {

	public static final int EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT = 0;

	@Autowired
	private AccessRequirementDAO accessRequirementDao;
	@Autowired
	private ResearchProjectDAO researchProjectDao;

	@WriteTransaction
	@Override
	public ResearchProject create(UserInfo userInfo, ResearchProject toCreate) {
		ValidateArgument.required(userInfo, "userInfo");
		validateResearchProject(toCreate);
		AccessRequirement ar = accessRequirementDao.get(toCreate.getAccessRequirementId());
		ValidateArgument.requirement(ar instanceof ManagedACTAccessRequirement,
				"A ResearchProject can only associate with an ManagedACTAccessRequirement.");
		toCreate = prepareCreationFields(toCreate, userInfo.getId().toString());
		return researchProjectDao.create(toCreate);
	}

	/**
	 * @param toValidate
	 */
	public void validateResearchProject(ResearchProject toValidate) {
		ValidateArgument.required(toValidate, "ResearchProject");
		ValidateArgument.required(toValidate.getAccessRequirementId(), "ResearchProject.accessRequirementId");
		ValidateArgument.requirement(toValidate.getProjectLead() != null
				&& toValidate.getProjectLead().length() > EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT,
				"ResearchProject.projectLead must contains more than "+EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT+" characters.");
		ValidateArgument.requirement(toValidate.getInstitution() != null
				&& toValidate.getInstitution().length() > EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT,
				"ResearchProject.projectLead must contains more than "+EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT+" characters.");
		ValidateArgument.requirement(toValidate.getIntendedDataUseStatement() != null
				&& toValidate.getIntendedDataUseStatement().length() > EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT,
				"ResearchProject.projectLead must contains more than "+EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT+" characters.");
	}

	public ResearchProject prepareCreationFields(ResearchProject toCreate, String createdBy) {
		toCreate.setCreatedBy(createdBy);
		toCreate.setCreatedOn(new Date());
		toCreate = prepareUpdateFields(toCreate, createdBy);
		return toCreate;
	}

	@Override
	public ResearchProject getUserOwnResearchProjectForUpdate(UserInfo userInfo, String accessRequirementId) throws NotFoundException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		try {
			return researchProjectDao.getUserOwnResearchProject(accessRequirementId, userInfo.getId().toString());
		} catch (NotFoundException e) {
			return createNewResearchProject(accessRequirementId);
		}
	}

	private ResearchProject createNewResearchProject(String accessRequirementId) {
		ResearchProject rp = new ResearchProject();
		rp.setAccessRequirementId(accessRequirementId);
		return rp;
	}

	@WriteTransaction
	@Override
	public ResearchProject update(UserInfo userInfo, ResearchProject toUpdate)
			throws NotFoundException, UnauthorizedException {
		ValidateArgument.required(userInfo, "userInfo");
		validateResearchProject(toUpdate);

		ResearchProject original = researchProjectDao.getForUpdate(toUpdate.getId());
		if (!original.getEtag().equals(toUpdate.getEtag())) {
			throw new ConflictingUpdateException();
		}

		ValidateArgument.requirement(toUpdate.getCreatedBy().equals(original.getCreatedBy())
				&& toUpdate.getCreatedOn().equals(original.getCreatedOn())
				&& toUpdate.getAccessRequirementId().equals(original.getAccessRequirementId()),
				"accessRequirementId, createdOn and createdBy fields cannot be editted.");

		if (!original.getCreatedBy().equals(userInfo.getId().toString())) {
				throw new UnauthorizedException("Only owner can perform this action.");
		}

		toUpdate = prepareUpdateFields(toUpdate, userInfo.getId().toString());
		return researchProjectDao.update(toUpdate);
	}

	public ResearchProject prepareUpdateFields(ResearchProject toUpdate, String modifiedBy) {
		toUpdate.setModifiedBy(modifiedBy);
		toUpdate.setModifiedOn(new Date());
		return toUpdate;
	}

	@WriteTransaction
	@Override
	public ResearchProject createOrUpdate(UserInfo userInfo, ResearchProject toCreateOrUpdate) {
		ValidateArgument.required(toCreateOrUpdate, "toCreateOrUpdate");
		if (toCreateOrUpdate.getId() == null) {
			return create(userInfo, toCreateOrUpdate);
		} else {
			return update(userInfo, toCreateOrUpdate);
		}
	}
}
