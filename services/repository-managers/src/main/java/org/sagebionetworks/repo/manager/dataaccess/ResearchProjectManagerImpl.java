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
import org.springframework.stereotype.Service;

@Service
public class ResearchProjectManagerImpl implements ResearchProjectManager {

	public static final int EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT = 0;

	private AccessRequirementDAO accessRequirementDao;
	
	private ResearchProjectDAO researchProjectDao;
	
	@Autowired
	public ResearchProjectManagerImpl(AccessRequirementDAO accessRequirementDao, ResearchProjectDAO researchProjectDao) {
		this.accessRequirementDao = accessRequirementDao;
		this.researchProjectDao = researchProjectDao;
	}

	@WriteTransaction
	@Override
	public ResearchProject create(UserInfo userInfo, ResearchProject toCreate) {
		ValidateArgument.required(userInfo, "The user");
		validateResearchProject(toCreate);
		toCreate = prepareCreationFields(toCreate, userInfo.getId().toString());
		return researchProjectDao.create(toCreate);
	}

	/**
	 * @param toValidate
	 */
	public void validateResearchProject(ResearchProject toValidate) {
		ValidateArgument.required(toValidate, "The research project");
		ValidateArgument.required(toValidate.getAccessRequirementId(), "The accessRequirementId");
		
		AccessRequirement ar = accessRequirementDao.get(toValidate.getAccessRequirementId());
		
		ValidateArgument.requirement(ar instanceof ManagedACTAccessRequirement, "A research project can only be associated with an ManagedACTAccessRequirement.");
		
		ValidateArgument.requirement(toValidate.getProjectLead() != null && toValidate.getProjectLead().length() > EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT, 
				"The projectLead must contains more than "+EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT+" characters.");
		ValidateArgument.requirement(toValidate.getInstitution() != null && toValidate.getInstitution().length() > EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT,
				"The insitution must contains more than "+EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT+" characters.");
		
		if (((ManagedACTAccessRequirement) ar).getIsIDURequired()) {
			ValidateArgument.requirement(toValidate.getIntendedDataUseStatement() != null && toValidate.getIntendedDataUseStatement().length() > EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT,
					"The intended data use statement must contains more than "+EXCLUSIVE_LOWER_BOUND_CHAR_LIMIT+" characters.");
		}
	}

	public ResearchProject prepareCreationFields(ResearchProject toCreate, String createdBy) {
		toCreate.setCreatedBy(createdBy);
		toCreate.setCreatedOn(new Date());
		toCreate = prepareUpdateFields(toCreate, createdBy);
		return toCreate;
	}

	@Override
	public ResearchProject getUserOwnResearchProjectForUpdate(UserInfo userInfo, String accessRequirementId) throws NotFoundException {
		ValidateArgument.required(userInfo, "The user");
		ValidateArgument.required(accessRequirementId, "The accessRequirementId");
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
		ValidateArgument.required(userInfo, "The user");
		ValidateArgument.required(toUpdate, "The research project");

		ResearchProject original = researchProjectDao.getForUpdate(toUpdate.getId());

		toUpdate.setCreatedOn(original.getCreatedOn());
		toUpdate.setCreatedBy(original.getCreatedBy());
		toUpdate.setAccessRequirementId(original.getAccessRequirementId());
		
		validateResearchProject(toUpdate);
		
		if (!original.getEtag().equals(toUpdate.getEtag())) {
			throw new ConflictingUpdateException();
		}
		
		if (!original.getCreatedBy().equals(userInfo.getId().toString())) {
				throw new UnauthorizedException("Only the owner can perform this action.");
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
		ValidateArgument.required(toCreateOrUpdate, "The research project");
		if (toCreateOrUpdate.getId() == null) {
			return create(userInfo, toCreateOrUpdate);
		} else {
			return update(userInfo, toCreateOrUpdate);
		}
	}
}
