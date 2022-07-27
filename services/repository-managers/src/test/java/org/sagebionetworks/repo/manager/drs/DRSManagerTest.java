package org.sagebionetworks.repo.manager.drs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.drs.OrganizationInformation;
import org.sagebionetworks.repo.model.drs.PackageInformation;
import org.sagebionetworks.repo.model.drs.ServiceInformation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class DRSManagerTest {

    @InjectMocks
    DRSManagerImpl drsManager;
    @Mock
    StackConfiguration stackConfiguration;


    @Test
    public void testGETDRSServiceInformation() {
        when(stackConfiguration.getStack()).thenReturn("dev");
        ServiceInformation serviceInformation = drsManager.getServiceInformation();
        assertNotNull(serviceInformation);
        assertEquals(createExpectedServiceInformation(), serviceInformation);
    }

    private ServiceInformation createExpectedServiceInformation(){
        final ServiceInformation serviceInformation = new ServiceInformation();
        serviceInformation.setId(DRSManagerImpl.REVERSE_DOMAIN_NOTATION);
        serviceInformation.setName(DRSManagerImpl.SERVICE_NAME);
        PackageInformation drsPackageInformation = new PackageInformation();
        drsPackageInformation.setGroup(DRSManagerImpl.DRS_GROUP);
        drsPackageInformation.setArtifact(DRSManagerImpl.DRS_ARTIFACT);
        drsPackageInformation.setVersion(DRSManagerImpl.DRS_VERSION);
        serviceInformation.setType(drsPackageInformation);
        serviceInformation.setDescription(DRSManagerImpl.DESCRIPTION);
        OrganizationInformation organization = new OrganizationInformation();
        organization.setName(DRSManagerImpl.ORGANIZATION_NAME);
        organization.setUrl(DRSManagerImpl.ORGANIZATION_URL);
        serviceInformation.setOrganization(organization);
        serviceInformation.setContactUrl(DRSManagerImpl.CONTACT_URL);
        serviceInformation.setDocumentationUrl(DRSManagerImpl.DOCUMENTATION_URL);
        serviceInformation.setCreatedAt(DRSManagerImpl.CREATED_AT);
        serviceInformation.setUpdatedAt(DRSManagerImpl.UPDATED_AT);
        serviceInformation.setEnvironment("dev");
        serviceInformation.setVersion(DRSManagerImpl.DRS_RELEASE_VERSION);
        serviceInformation.setUrl(DRSManagerImpl.DRS_URL);

        return serviceInformation;
    }

}
