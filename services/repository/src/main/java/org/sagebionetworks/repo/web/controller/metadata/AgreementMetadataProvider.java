package org.sagebionetworks.repo.web.controller.metadata;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Agreement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 *
 */
public class AgreementMetadataProvider implements
		TypeSpecificMetadataProvider<Agreement> {

	private static final String PARENT_AGREEMENT_NAME = "ParentAgreement";
	private static final Logger log = Logger
			.getLogger(AgreementMetadataProvider.class.getName());

	private static final Long fakeVersionNumber = -1L;
	private volatile static String parentAgreementId = null;


	@Override
	public void addTypeSpecificMetadata(Agreement entity,
			HttpServletRequest request, UserInfo userInfo, EventType eventType)
			throws NotFoundException, DatastoreException, UnauthorizedException {
	}

	@Override
	public void validateEntity(Agreement entity, EntityEvent event)
			throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		if (null == entity.getDatasetId()) {
			throw new InvalidModelException("datasetId cannot be null");
		}
		if (null == entity.getEulaId()) {
			throw new InvalidModelException("eulaId cannot be null");
		}
		if ((null != entity.getCreatedBy()) && !entity.getCreatedBy().equals(event.getUserInfo().getUser().getId())) {
			throw new InvalidModelException("createdBy must be " + event.getUserInfo().getUser().getId());			
		}

		// The system is responsible for setting the versions of the dataset and
		// eula once those objects are versionable PLFM-326
//		Dataset dataset = (Dataset) entityManager.getEntity(event.getUserInfo(), entity
//				.getDatasetId(), ObjectType.dataset.getClassForType());
//		Eula eula = (Eula) entityManager.getEntity(event.getUserInfo(),
//				entity.getEulaId(), ObjectType.eula.getClassForType());
		// entity.setDatasetVersionNumber(dataset.getVersionNumber());
		// entity.setEulaVersionNumber(eula.getVersionNumber());
		entity.setDatasetVersionNumber(fakeVersionNumber);
		entity.setEulaVersionNumber(fakeVersionNumber);

	}

	@Override
	public void entityDeleted(Agreement deleted) {
		// TODO Auto-generated method stub
	}


}
