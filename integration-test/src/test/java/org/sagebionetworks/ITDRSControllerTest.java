package org.sagebionetworks;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.drs.ServiceInformation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ITTestExtension.class)
public class ITDRSControllerTest {

    final private SynapseClient synapse;

    public ITDRSControllerTest(SynapseClient synapse) {
        this.synapse = synapse;
    }

    @Test
    public void testGETDRSServiceInfo() throws SynapseException {
        ServiceInformation serviceInformation = synapse.getDRSServiceInfo();
        assertNotNull(serviceInformation);
        assertEquals(serviceInformation.getId(), "org.sagebase.prod.repo-prod");
        assertEquals(serviceInformation.getName(), "Sage Bionetworks Synapse DRS API");
        assertEquals(serviceInformation.getUrl(), "https://repo-prod.prod.sagebase.org");
        assertEquals(serviceInformation.getDocumentationUrl(), "https://docs.synapse.org");
        assertEquals(serviceInformation.getContactUrl(), "https://sagebionetworks.jira.com/servicedesk/customer/portal/9");
        assertEquals(serviceInformation.getOrganization().getName(), "Sage Bionetworks");
        assertEquals(serviceInformation.getOrganization().getUrl(), "https://www.sagebionetworks.org");
        assertEquals(serviceInformation.getType().getGroup(), "org.ga4gh");
        assertEquals(serviceInformation.getType().getArtifact(), "drs");
        assertEquals(serviceInformation.getType().getVersion(), "1.2.0");
    }
}
