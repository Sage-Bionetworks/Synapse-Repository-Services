package org.sagebionetworks.repo.web.controller.metadata;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.model.Analysis;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 */
public class AnalysisMetadataProvider implements
		TypeSpecificMetadataProvider<Analysis> {

	private static final Logger log = Logger
			.getLogger(AnalysisMetadataProvider.class.getName());

	@Autowired
	EntityManager entityManager;
	@Autowired
	EntityPermissionsManager entityPermissionsManager;


	@Override
	public void addTypeSpecificMetadata(Analysis entity,
			HttpServletRequest request, UserInfo userInfo, EventType eventType)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		// As a side-effect, update the parent of the provenance record
		updateProvenanceRecord(request
				.getParameter(ServiceConstants.STEP_TO_UPDATE_PARAM), entity,
				userInfo, eventType);
	}

	@Override
	public void validateEntity(Analysis entity, EntityEvent event)
			throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {
	}

	@Override
	public void entityDeleted(Analysis deleted) {
		// TODO Auto-generated method stub
	}

	/**
	 * Helper for updating a provenance record as a side effect of CRUD on an
	 * analysis
	 * 
	 * Note that this side-effect is happening in a separate transaction from
	 * the primary request but that is okay because this is only additive and
	 * also idempotent. This operation could only fail _and_should_fail_ if:
	 * 
	 * (1) another concurrent request deleted the step or
	 * 
	 * (2) another concurrent request deleted the analysis.
	 * 
	 * These sorts of failures are not fatal and should not cause the user's
	 * primary request to fail.
	 * 
	 * THIS SHOULD BE DELETED FOR ANALYSIS CLEANUP
	 */
	
	@Deprecated
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	private void updateProvenanceRecord(String stepId, Analysis entity,
			UserInfo user, EventType eventType) {

		if (null == stepId || EventType.CREATE != eventType) {
			// Nothing to do here
			return;
		}

		try {
			Step step = entityManager.getEntity(user, stepId,
					Step.class);
			step.setParentId(entity.getId());
			entityManager.updateEntity(user, step, false, null);
			entityPermissionsManager.restoreInheritance(stepId, user);
		}
		// Sorry for the big catch block, its not a good habit to just catch
		// Exception
		// Note with Java SE 7 we'll be able to catch more than one type of
		// exception with one exception handler
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