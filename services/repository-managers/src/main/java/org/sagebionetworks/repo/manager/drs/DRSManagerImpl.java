package org.sagebionetworks.repo.manager.drs;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.drs.DRSPackageInformation;
import org.sagebionetworks.repo.model.drs.OrganizationInformation;
import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DRSManagerImpl implements DRSManager{
    public static final String REVERSE_DOMAIN_NOTATION ="org.sagebase.prod.repo-prod";
    public static final String SERVICE_NAME ="DRS Service";
    public static final String CONTACT_URL ="https://sagebionetworks.jira.com/servicedesk/customer/portal/9";
    public static final String DOCUMENTATION_URL ="https://docs.synapse.org";
    public static final String DRS_URL = "https://repo-prod.prod.sagebase.org";
    public static final String DRS_GROUP = "org.ga4gh";
    public static final String DRS_ARTIFACT = "drs";
    public static final String DRS_VERSION = "1.2.0";
    public static final String ORGANIZATION_NAME = "Sage Bionetworks";
    public static final String ORGANIZATION_URL = "https://www.sagebionetworks.org";
    public static final String DESCRIPTION = "This service provides information about implementation of DRS specification";

    @Autowired
    StackConfiguration stackConfiguration;


    @Override
    public ServiceInformation getServiceInformation() {
        ServiceInformation result = new ServiceInformation();
        result.setId(REVERSE_DOMAIN_NOTATION);
        result.setName(SERVICE_NAME);
        DRSPackageInformation drsPackageInformation = new DRSPackageInformation();
        drsPackageInformation.setGroup(DRS_GROUP);
        drsPackageInformation.setArtifact(DRS_ARTIFACT);
        drsPackageInformation.setVersion(DRS_VERSION);
        result.setType(drsPackageInformation);
        result.setDescription(DESCRIPTION);
        OrganizationInformation organization = new OrganizationInformation();
        organization.setName(ORGANIZATION_NAME);
        organization.setUrl(ORGANIZATION_URL);
        result.setOrganization(organization);
        result.setContactUrl(CONTACT_URL);
        result.setDocumentationUrl(DOCUMENTATION_URL); // should it be confluence page or drs specification url
        // result.setCreatedAt();
        // result.setUpdatedAt();
        result.setEnvironment(stackConfiguration.getStack());
        result.setVersion(stackConfiguration.getStackInstance());
        result.setUrl(DRS_URL);
        return result;
    }
}
