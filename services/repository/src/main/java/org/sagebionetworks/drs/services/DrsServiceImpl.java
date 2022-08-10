package org.sagebionetworks.drs.services;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.drs.DrsManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.drs.DrsObject;
import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class DrsServiceImpl implements DrsService {

    private DrsManager drsManager;

    private UserManager userManager;

    @Autowired
    public DrsServiceImpl(DrsManager drsManager, UserManager userManager) {
        super();
        this.drsManager = drsManager;
        this.userManager = userManager;
    }

    @Override
    public ServiceInformation getServiceInformation() {
        return drsManager.getServiceInformation();
    }

    @Override
    public DrsObject getDrsObject(final Long userId, final String id)
            throws NotFoundException, DatastoreException, UnauthorizedException {
        final UserInfo userInfo = userManager.getUserInfo(userId);
        return drsManager.getDrsObject(userInfo, id);
    }
}
