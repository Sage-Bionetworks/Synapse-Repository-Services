package org.sagebionetworks.repo.web.service.dataaccess;

import org.sagebionetworks.repo.model.dataaccess.ChangeOwnershipRequest;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequest;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;

public interface DataAccessService {

	ResearchProject create(Long userId, ResearchProject toCreate);

	ResearchProject update(Long userId, ResearchProject toUpdate);

	ResearchProject getUserOwnResearchProject(Long userId, String accessRequirementId);

	ResearchProject changeOwnership(Long userId, ChangeOwnershipRequest request);

	DataAccessRequest create(Long userId, DataAccessRequest toCreate);

	DataAccessRequestInterface update(Long userId, DataAccessRequestInterface toUpdate);

	DataAccessRequestInterface getUserOwnCurrentRequest(Long userId, String requirementId);

	DataAccessRequestInterface getRequestForUpdate(Long userId, String requirementId);

}
