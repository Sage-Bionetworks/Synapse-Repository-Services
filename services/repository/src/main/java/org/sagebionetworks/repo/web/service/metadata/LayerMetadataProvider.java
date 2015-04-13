package org.sagebionetworks.repo.web.service.metadata;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.Data;
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
 * Provides layer specific metadata.
 * 
 * @author jmhill
 * 
 */
public class LayerMetadataProvider implements TypeSpecificMetadataProvider<Data>, EntityValidator<Data> {

	private static final Logger log = Logger
			.getLogger(LayerMetadataProvider.class.getName());

	@Autowired
	EntityManager entityManager;

	@Override
	public void addTypeSpecificMetadata(Data entity,
			HttpServletRequest request, UserInfo user, EventType eventType) {

		// As a side-effect, update a provenance record, if specified
		String stepId = request
				.getParameter(ServiceConstants.STEP_TO_UPDATE_PARAM);
		if (null != stepId) {
			updateProvenanceRecord(stepId, entity, user, eventType);
		}

	}

	@Override
	public void validateEntity(Data entity, EntityEvent event) {
//		if (entity.getType() == null) {
//			throw new IllegalArgumentException("Layer.type cannot be null");
//		}
		if (entity.getParentId() == null) {
			throw new IllegalArgumentException("Layer.parentId cannot be null");
		}
	}

	/**
	 * Helper for updating a provenance record as a side effect of CRUD on a
	 * layer
	 * 
	 * Note that this side-effect is happening in a separate transaction from
	 * the primary request but that is okay because this is only additive and
	 * also idempotent. This operation could only fail _and_should_fail_ if:
	 * 
	 * (1) another concurrent request deleted the step or
	 * 
	 * (2) another concurrent request deleted the layer.
	 * 
	 * These sorts of failures are not fatal and should not cause the user's
	 * primary request to fail.
	 * 
	 * THIS SHOULD BE DELETED FOR ANALYSIS CLEANUP
	 */
	@WriteTransaction
	private void updateProvenanceRecord(String stepId, Data entity,
			UserInfo user, EventType eventType) {
		
		// Deleting a layer has no effect on provenance
		if (EventType.DELETE == eventType) return;
		
		try {
			Step step = (Step) entityManager.getEntity(user, stepId,
					EntityType.step.getClassForType());
			Reference reference = new Reference();
			reference.setTargetId(entity.getId());
			reference.setTargetVersionNumber(entity.getVersionNumber());
			if (EventType.CREATE == eventType 
					|| EventType.UPDATE == eventType
					|| EventType.NEW_VERSION == eventType) {
				step.getOutput().add(reference);
			}
			else if (EventType.GET == eventType) {
				step.getInput().add(reference);
			}
			else {
				log.warning("Failed to update provenance record for unhandled layer event type: " + eventType);
			}
			entityManager.updateEntity(user, step, false, null);
		} 
		// Sorry for the big catch block, its not a good habit to just catch Exception
		// Note with Java SE 7 we'll be able to catch more than one type of exception with one exception handler 
		// http://download.oracle.com/javase/tutorial/essential/exceptions/catch.html
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
