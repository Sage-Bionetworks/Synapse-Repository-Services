package org.sagebionetworks.drs.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.drs.DRSManager;
import org.sagebionetworks.repo.model.drs.ServiceInformation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class DRSServiceImplTest {

    @Mock
    DRSManager drsManager;
    @Mock
    ServiceInformation serviceInformation;
    @InjectMocks
    DRSServiceImpl drsService;


    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);
        when(drsService.getServiceInformation()).thenReturn(serviceInformation);
    }

    @Test
    public void testGETDRSServiceInformation() {
        ServiceInformation serviceInformation = drsManager.getServiceInformation();
        assertNotNull(serviceInformation);
    }
}
