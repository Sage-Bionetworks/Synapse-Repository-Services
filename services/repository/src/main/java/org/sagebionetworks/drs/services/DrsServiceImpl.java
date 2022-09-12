package org.sagebionetworks.drs.services;

import org.sagebionetworks.repo.manager.drs.DrsManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.drs.AccessUrl;
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
    public DrsObject getDrsObject(final Long userId, final String id, final boolean expand)
            throws NotFoundException, DatastoreException, UnauthorizedException,
            IllegalArgumentException, UnsupportedOperationException {
        return drsManager.getDrsObject(userId, id, expand);
    }

    @Override
    public AccessUrl getAccessUrl(final Long userId, final String drsObjectId, final String accessId) throws NotFoundException, DatastoreException,
            UnauthorizedException, IllegalArgumentException {
        return drsManager.getAccessUrl(userId, drsObjectId, accessId);
    }
}
