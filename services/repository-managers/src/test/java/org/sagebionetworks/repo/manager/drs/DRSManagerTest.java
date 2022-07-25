package org.sagebionetworks.repo.manager.drs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.drs.ServiceInformation;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class DRSManagerTest {

    @Mock
    DRSManager drsManager;
    @Mock
    StackConfiguration stackConfiguration;


    @BeforeEach
    public void before() {
        when(stackConfiguration.getStack()).thenReturn("dev");
        when(stackConfiguration.getStackInstance()).thenReturn("417.0.1");
        drsManager = new DRSManagerImpl(stackConfiguration);
    }

    @Test
    public void testGETDRSServiceInformation() {
        ServiceInformation serviceInformation = drsManager.getServiceInformation();
        assertNotNull(serviceInformation);
        assertThat(serviceInformation.getId(), is(DRSManagerImpl.REVERSE_DOMAIN_NOTATION));
        assertThat(serviceInformation.getName(), is(DRSManagerImpl.SERVICE_NAME));
        assertThat(serviceInformation.getType().getGroup(), is(DRSManagerImpl.DRS_GROUP));
        assertThat(serviceInformation.getType().getArtifact(), is(DRSManagerImpl.DRS_ARTIFACT));
        assertThat(serviceInformation.getType().getVersion(), is(DRSManagerImpl.DRS_VERSION));
        assertThat(serviceInformation.getDescription(), is(DRSManagerImpl.DESCRIPTION));
        assertThat(serviceInformation.getOrganization().getName(), is(DRSManagerImpl.ORGANIZATION_NAME));
        assertThat(serviceInformation.getOrganization().getUrl(), is(DRSManagerImpl.ORGANIZATION_URL));
        assertThat(serviceInformation.getContactUrl(), is(DRSManagerImpl.CONTACT_URL));
        assertThat(serviceInformation.getDocumentationUrl(), is(DRSManagerImpl.DOCUMENTATION_URL));
        assertThat(serviceInformation.getEnvironment(), is("dev"));
        assertThat(serviceInformation.getVersion(), is("417.0.1"));
        assertThat(serviceInformation.getUrl(), is(DRSManagerImpl.DRS_URL));
    }

}
