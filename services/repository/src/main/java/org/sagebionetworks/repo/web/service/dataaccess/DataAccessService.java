package org.sagebionetworks.repo.web.service.dataaccess;

import org.sagebionetworks.repo.model.dataaccess.ChangeOwnershipRequest;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;

public interface DataAccessService {

	ResearchProject create(Long userId, ResearchProject toCreate);

	ResearchProject update(Long userId, ResearchProject toUpdate);

	ResearchProject getUserOwnResearchProject(Long userId, String accessRequirementId);

	ResearchProject changeOwnership(Long userId, ChangeOwnershipRequest request);

}
