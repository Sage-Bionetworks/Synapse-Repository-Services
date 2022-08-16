package org.sagebionetworks.drs.services;

import org.sagebionetworks.repo.manager.drs.DrsManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.drs.DrsObject;
import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class DrsServiceImpl implements DrsService {

    private final DrsManager drsManager;

    @Autowired
    public DrsServiceImpl(final DrsManager drsManager) {
        super();
        this.drsManager = drsManager;
    }

    @Override
    public ServiceInformation getServiceInformation() {
        return drsManager.getServiceInformation();
    }

    @Override
    public DrsObject getDrsObject(final Long userId, final String id)
            throws NotFoundException, DatastoreException, UnauthorizedException, IllegalArgumentException {
        return drsManager.getDrsObject(userId, id);
    }
}
