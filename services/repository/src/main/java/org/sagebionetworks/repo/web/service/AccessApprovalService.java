package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoRequest;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoResponse;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRevokeRequest;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessApprovalService {

	AccessApproval createAccessApproval(Long userId, AccessApproval accessApproval)
			throws DatastoreException, UnauthorizedException, NotFoundException, InvalidModelException, IOException;

	AccessApproval getAccessApproval(Long userId, String approvalId)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	void revokeAccessApprovals(Long userId, String accessRequirementId, String accessorId);

	AccessorGroupResponse listAccessorGroup(Long userId, AccessorGroupRequest request);

	void revokeGroup(Long userId, AccessorGroupRevokeRequest request);

	BatchAccessApprovalInfoResponse getBatchAccessApprovalInfo(Long userId, BatchAccessApprovalInfoRequest request);

	AccessApprovalNotificationResponse listNotificationsRequest(Long userId, AccessApprovalNotificationRequest request);

}