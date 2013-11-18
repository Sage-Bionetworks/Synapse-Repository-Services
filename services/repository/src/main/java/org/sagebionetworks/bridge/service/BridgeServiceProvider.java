package org.sagebionetworks.bridge.service;

import org.sagebionetworks.repo.web.service.table.TableServices;

/**
 * Abstraction for the service providers.
 *
 */
public interface BridgeServiceProvider {

	public CommunityService getCommunityService();
}
