package org.sagebionetworks.drs.services;

import org.sagebionetworks.repo.model.drs.GA4GHInformation;
import org.sagebionetworks.repo.model.drs.SageInformation;
import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.springframework.stereotype.Service;

@Service
public class DRSServiceImpl implements DRSService{

    @Override
    public ServiceInformation getServiceInformation() {
         ServiceInformation result = new ServiceInformation();
         result.setId("org.sagebase.prod.repo-prod");
         result.setName("DRS Service");
         GA4GHInformation ga4GHInformation = new GA4GHInformation();
         result.setType(ga4GHInformation);
         result.setDescription("This service provides implementation of DRS specification");
         SageInformation organization = new SageInformation();
         result.setOrganization(organization);
         result.setContactUrl("https://sagebionetworks.jira.com/servicedesk/customer/portal/9");
         result.setDocumentationUrl("https://docs.synapse.org"); // should it be confluence page or drs specification url
        // result.setCreatedAt();
        // result.setUpdatedAt();
         result.setEnvironment("PROD"); //check
         result.setVersion("417.0.1"); //check
        return result;
    }
}
