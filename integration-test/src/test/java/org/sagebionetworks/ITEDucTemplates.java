package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.educ.EDucTemplateListRequest;
import org.sagebionetworks.repo.model.educ.EDucTemplatePage;

@ExtendWith(ITTestExtension.class)
public class ITEDucTemplates {

	private final SynapseAdminClient adminSynapse;
	private final StackConfiguration config;

	public ITEDucTemplates(SynapseAdminClient adminSynapse, StackConfiguration config) {
		this.adminSynapse = adminSynapse;
		this.config = config;
	}

	@Test
	public void testListEDucTemplates() throws SynapseException {
		assumeTrue(config.getDocuSignEnabled(),
				"DocuSign integration is disabled — skipping eDUC template IT test.");

		// call under test
		EDucTemplatePage page = adminSynapse.listEDucTemplates(new EDucTemplateListRequest());

		assertNotNull(page);
		assertNotNull(page.getResults());
	}
}
