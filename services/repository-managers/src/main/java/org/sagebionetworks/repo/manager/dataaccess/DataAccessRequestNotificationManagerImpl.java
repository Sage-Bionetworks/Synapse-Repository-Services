package org.sagebionetworks.repo.manager.dataaccess;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TeamConstants;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class DataAccessRequestNotificationManagerImpl implements DataAccessRequestNotificationManager {

	private static final Logger LOG = LogManager.getLogger(DataAccessRequestNotificationManagerImpl.class);

	private final RequestManager dataAccessRequestManager;
	private final AccessControlListDAO aclDao;

	@Autowired
	public DataAccessRequestNotificationManagerImpl(RequestManager dataAccessRequestManager) {
		super();
		this.dataAccessRequestManager = dataAccessRequestManager;
		this.aclDao = null;
	}

	@Override
	public void dataAccessRequestCreatedOrUpdated(String dataAccessRequestId) {
		ValidateArgument.required(dataAccessRequestId, "dataAccessRequestId");
		try {
			RequestInterface request = dataAccessRequestManager.getRequestForSubmission(dataAccessRequestId);
			AccessControlList acl = aclDao.get(request.getAccessRequirementId(), ObjectType.ACCESS_REQUIREMENT);
			List<Long> nonActReviewerPrincipalIds = acl.getResourceAccess().stream()
					.filter(r -> r.getAccessType().contains(ACCESS_TYPE.REVIEW_SUBMISSIONS))
					.map(r -> r.getPrincipalId()).filter(p -> !TeamConstants.ACT_TEAM_ID.equals(p))
					.collect(Collectors.toList());
			
			if(nonActReviewerPrincipalIds.isEmpty()) {
				return;
			}
			
		} catch (NotFoundException e) {
			LOG.info("Will not send notification: " + e.getMessage());
		}
	}

}
