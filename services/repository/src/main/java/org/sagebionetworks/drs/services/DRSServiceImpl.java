package org.sagebionetworks.drs.services;

import org.sagebionetworks.repo.manager.drs.DRSManager;
import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DRSServiceImpl implements DRSService{

    @Autowired
    DRSManager drsManager;

    @Override
    public ServiceInformation getServiceInformation() {
        return drsManager.getServiceInformation();
    }
}
