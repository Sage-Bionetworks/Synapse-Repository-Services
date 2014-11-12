package org.sagebionetworks.bridge.service.metadata;

import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.metadata.EntityEvent;
import org.sagebionetworks.repo.web.service.metadata.EntityValidator;
import org.sagebionetworks.repo.web.service.metadata.EventType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validation for TableEntities.
 *
 */
public class CommunityMetadataProvider implements EntityValidator<Community> {
	
	@Autowired
	TeamManager teamManager;

	@Override
	public void validateEntity(Community community, EntityEvent event) throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {
		// For create/update/new version we need to bind the columns to the entity.
		if (EventType.CREATE == event.getType() || EventType.UPDATE == event.getType()) {
			// make sure there is a team associated with the community
			if (community.getTeamId() == null) {
				throw new IllegalArgumentException("Community.teamId is required");
			}
			// throws exception if team does not exist
			teamManager.get(community.getTeamId());
		}	
	}
}
