package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.portals.CreateOrUpdatePortalRequest;
import org.sagebionetworks.repo.model.portals.Portal;

@ExtendWith(ITTestExtension.class)
public class ITPortalTest {

	private SynapseAdminClient adminClient;
	
	public ITPortalTest(SynapseAdminClient adminClient) {
		this.adminClient = adminClient;
	}
	
	@Test
	public void testRoundTrip(SynapseClient client) throws Exception {
		
		CreateOrUpdatePortalRequest request = new CreateOrUpdatePortalRequest()
			.setName("My Portal")
			.setUrl("https://myportal.synapse.org");
		
		// Only an admin can create a portal
		assertThrows(SynapseForbiddenException.class, () -> {			
			client.createPortal(request);
		});
		
		Portal portal = adminClient.createPortal(request);
		
		// Any user can read
		assertEquals(portal, client.getPortal(portal.getId()));
		
		assertThrows(SynapseForbiddenException.class, () -> {
			client.updatePortal(portal.getId(), request.setName("My Portal Updated"));
		});
		
		AccessControlList acl = adminClient.getPortalAcl(portal.getId());
		
		acl.getResourceAccess().add(new ResourceAccess()
			.setPrincipalId(Long.valueOf(client.getMyProfile().getOwnerId()))
			.setAccessType(Set.of(ACCESS_TYPE.UPDATE, ACCESS_TYPE.DELETE))
		);
		
		adminClient.updatePortalAcl(acl);
		
		client.updatePortal(portal.getId(), request.setName("My Portal Updated"));
		
		client.deletePortal(portal.getId());
	}
}
