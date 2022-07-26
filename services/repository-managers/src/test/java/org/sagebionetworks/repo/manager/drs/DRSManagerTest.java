package org.sagebionetworks.repo.manager.drs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
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
        when(stackConfiguration.getStackInstance()).thenReturn("417.0.1");
        drsManager = new DRSManagerImpl(stackConfiguration);
        ServiceInformation serviceInformation = drsManager.getServiceInformation();
        assertNotNull(serviceInformation);
        assertEquals(serviceInformation.getId(), DRSManagerImpl.REVERSE_DOMAIN_NOTATION);
        assertEquals(serviceInformation.getName(), DRSManagerImpl.SERVICE_NAME);
        assertEquals(serviceInformation.getType().getGroup(), DRSManagerImpl.DRS_GROUP);
        assertEquals(serviceInformation.getType().getArtifact(), DRSManagerImpl.DRS_ARTIFACT);
        assertEquals(serviceInformation.getType().getVersion(), DRSManagerImpl.DRS_VERSION);
        assertEquals(serviceInformation.getDescription(), DRSManagerImpl.DESCRIPTION);
        assertEquals(serviceInformation.getOrganization().getName(), DRSManagerImpl.ORGANIZATION_NAME);
        assertEquals(serviceInformation.getOrganization().getUrl(), DRSManagerImpl.ORGANIZATION_URL);
        assertEquals(serviceInformation.getContactUrl(), DRSManagerImpl.CONTACT_URL);
        assertEquals(serviceInformation.getDocumentationUrl(), DRSManagerImpl.DOCUMENTATION_URL);
        assertEquals(serviceInformation.getEnvironment(), "dev");
        assertEquals(serviceInformation.getVersion(), "417.0.1");
        assertEquals(serviceInformation.getUrl(), DRSManagerImpl.DRS_URL);
    }

}
