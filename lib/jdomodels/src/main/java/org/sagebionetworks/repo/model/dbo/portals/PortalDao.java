package org.sagebionetworks.repo.model.dbo.portals;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.portals.Portal;

public interface PortalDao {
	
	void bootstrap();

	Portal createPortal(Long userId, String name, String endpoint);
	
	Portal updatePortal(Long userId, String portalId, String name, String endpoint);
	
	void deletePortal(String portalId);
	
	Optional<Portal> getPortal(String id);
	
	List<Portal> getPortalPage(long limit, long offset);

	// For testing
	void truncateAll();
	
}
