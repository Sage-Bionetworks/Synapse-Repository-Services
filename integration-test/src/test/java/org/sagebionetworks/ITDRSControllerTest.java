package org.sagebionetworks;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.drs.ServiceInformation;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@ExtendWith(ITTestExtension.class)
public class ITDRSControllerTest {

    private SynapseClient synapse;

    public ITDRSControllerTest(SynapseClient synapse) {
        this.synapse = synapse;
    }

    @Test
    public void testGETDRSServiceInfo() throws SynapseException {
        ServiceInformation serviceInformation = synapse.getDRSServiceInfo();
        assertNotNull(serviceInformation);
        assertThat(serviceInformation.getId(), is("org.sagebase.prod.repo-prod"));
        assertThat(serviceInformation.getName(), is("Sage Bionetworks Synapse DRS API"));
        assertThat(serviceInformation.getUrl(), is("https://repo-prod.prod.sagebase.org"));
        assertThat(serviceInformation.getDocumentationUrl(), is("https://docs.synapse.org"));
        assertThat(serviceInformation.getContactUrl(), is("https://sagebionetworks.jira.com/servicedesk/customer/portal/9"));
        assertThat(serviceInformation.getOrganization().getName(), is("Sage Bionetworks"));
        assertThat(serviceInformation.getOrganization().getUrl(), is("https://www.sagebionetworks.org"));
        assertThat(serviceInformation.getType().getGroup(), is("org.ga4gh"));
        assertThat(serviceInformation.getType().getArtifact(), is("drs"));
        assertThat(serviceInformation.getType().getVersion(), is("1.2.0"));
    }
}
