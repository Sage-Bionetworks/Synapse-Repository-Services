package org.sagebionetworks.repo.web.service.metadata;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.Code;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.sagebionetworks.repo.transactions.WriteTransaction;

/**
 * Provides Code specific metadata.
 * 
 * 
 */
public class CodeMetadataProvider implements TypeSpecificMetadataProvider<Code>, EntityValidator<Code> {

	private static final Logger log = Logger
			.getLogger(CodeMetadataProvider.class.getName());

	@Autowired
	EntityManager entityManager;

//	@Autowired
//	LayerTypeCountCache layerTypeCountCache;

	@Override
	public void addTypeSpecificMetadata(Code entity,
			HttpServletRequest request, UserInfo user, EventType eventType) {

//		// Only clear the cache for a CREATE or UPDATE event. (See
//		// http://sagebionetworks.jira.com/browse/PLFM-232)
//		if (EventType.CREATE == eventType || EventType.UPDATE == eventType) {
//			clearCountsForCode(entity);
//		}

		// As a side-effect, update a provenance record, if specified
		String stepId = request
				.getParameter(ServiceConstants.STEP_TO_UPDATE_PARAM);
		if (null != stepId) {
			updateProvenanceRecord(stepId, entity, user, eventType);
		}

	}


	@Override
	public void validateEntity(Code entity, EntityEvent event) {
		if (entity.getParentId() == null) {
			throw new IllegalArgumentException("Code.parentId cannot be null");
		}
	}

	/**
	 * Helper for updating a provenance record as a side effect of CRUD on a
	 * Code
	 * 
	 * Note that this side-effect is happening in a separate transaction from
	 * the primary request but that is okay because this is only additive and
	 * also idempotent. This operation could only fail _and_should_fail_ if:
	 * 
	 * (1) another concurrent request deleted the step or
	 * 
	 * (2) another concurrent request deleted the Code.
	 * 
	 * These sorts of failures are not fatal and should not cause the user's
	 * primary request to fail.
	 * 
	 * THIS SHOULD BE DELETED FOR ANALYSIS CLEANUP
	 */
	@Deprecated
	@WriteTransaction
	private void updateProvenanceRecord(String stepId, Code entity,
			UserInfo user, EventType eventType) {
		
		// Deleting a Code has no effect on provenance
		if (EventType.DELETE == eventType) return;
		
		try {
			Step step = (Step) entityManager.getEntity(user, stepId,
					EntityType.step.getClassForType());
			Reference reference = new Reference();
			reference.setTargetId(entity.getId());
			reference.setTargetVersionNumber(entity.getVersionNumber());
			if (EventType.CREATE == eventType 
					|| EventType.UPDATE == eventType
					|| EventType.NEW_VERSION == eventType
					|| EventType.GET == eventType) {
				step.getCode().add(reference);
			} else {
				log.warning("Failed to update provenance record for unhandled Code event type: " + eventType);
			}
			entityManager.updateEntity(user, step, false, null);
		} 
		catch (NotFoundException e) {
			log.log(Level.WARNING, "Failed to update provenance record "
					+ stepId, e);
		} catch (DatastoreException e) {
			log.log(Level.WARNING, "Failed to update provenance record "
					+ stepId, e);
		} catch (UnauthorizedException e) {
			log.log(Level.WARNING, "Failed to update provenance record "
					+ stepId, e);
		} catch (ConflictingUpdateException e) {
			log.log(Level.WARNING, "Failed to update provenance record "
					+ stepId, e);
		} catch (InvalidModelException e) {
			log.log(Level.WARNING, "Failed to update provenance record "
					+ stepId, e);
		}
	}
}
