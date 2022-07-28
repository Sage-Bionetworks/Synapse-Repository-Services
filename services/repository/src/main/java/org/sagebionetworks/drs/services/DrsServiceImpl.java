package org.sagebionetworks.drs.services;

import org.sagebionetworks.repo.manager.drs.DrsManager;
import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DrsServiceImpl implements DrsService {

    private DrsManager drsManager;

    @Autowired
    public DrsServiceImpl(DrsManager drsManager) {
        super();
        this.drsManager = drsManager;
    }

    @Override
    public ServiceInformation getServiceInformation() {
        return drsManager.getServiceInformation();
    }
}
