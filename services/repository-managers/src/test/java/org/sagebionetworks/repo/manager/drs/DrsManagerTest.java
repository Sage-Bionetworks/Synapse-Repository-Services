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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class DrsManagerTest {

    @InjectMocks
    DrsManagerImpl drsManager;
    @Mock
    StackConfiguration stackConfiguration;


    @Test
    public void testGETDrsServiceInformation() {
        when(stackConfiguration.getStack()).thenReturn("dev");
        when(stackConfiguration.getStackInstance()).thenReturn("417.0.1");
        ServiceInformation serviceInformation = drsManager.getServiceInformation();
        assertNotNull(serviceInformation);
        assertEquals(createExpectedServiceInformation(), serviceInformation);
    }

    private ServiceInformation createExpectedServiceInformation() {
        final ServiceInformation serviceInformation = new ServiceInformation();
        serviceInformation.setId(DrsManagerImpl.REVERSE_DOMAIN_NOTATION);
        serviceInformation.setName(DrsManagerImpl.SERVICE_NAME);
        PackageInformation drsPackageInformation = new PackageInformation();
        drsPackageInformation.setGroup(DrsManagerImpl.DRS_GROUP);
        drsPackageInformation.setArtifact(DrsManagerImpl.DRS_ARTIFACT);
        drsPackageInformation.setVersion(DrsManagerImpl.DRS_VERSION);
        serviceInformation.setType(drsPackageInformation);
        serviceInformation.setDescription(DrsManagerImpl.DESCRIPTION);
        OrganizationInformation organization = new OrganizationInformation();
        organization.setName(DrsManagerImpl.ORGANIZATION_NAME);
        organization.setUrl(DrsManagerImpl.ORGANIZATION_URL);
        serviceInformation.setOrganization(organization);
        serviceInformation.setContactUrl(DrsManagerImpl.CONTACT_URL);
        serviceInformation.setDocumentationUrl(DrsManagerImpl.DOCUMENTATION_URL);
        serviceInformation.setCreatedAt(DrsManagerImpl.CREATED_AT);
        serviceInformation.setUpdatedAt(DrsManagerImpl.UPDATED_AT);
        serviceInformation.setEnvironment("dev");
        serviceInformation.setVersion("417.0.1");
        serviceInformation.setUrl(DrsManagerImpl.DRS_URL);

        return serviceInformation;
    }

}
