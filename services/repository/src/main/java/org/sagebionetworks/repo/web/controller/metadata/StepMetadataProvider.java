package org.sagebionetworks.repo.web.controller.metadata;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EnvironmentDescriptor;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 *
 */
public class StepMetadataProvider implements TypeSpecificMetadataProvider<Step> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	@SuppressWarnings("unchecked")
	private static final TypeReference descriptorTypeRef = new TypeReference<Set<EnvironmentDescriptor>>() {
	};

	@SuppressWarnings("unchecked")
	@Override
	public void addTypeSpecificMetadata(Step entity,
			HttpServletRequest request, UserInfo userInfo, EventType eventType)
			throws NotFoundException, DatastoreException, UnauthorizedException {

//		// Clear the blob and set the environmentDescriptors
//		if (entity.getEnvironmentDescriptorsBlob() != null) {
//			Set<EnvironmentDescriptor> descriptors;
//			try {
//				descriptors = (Set<EnvironmentDescriptor>) OBJECT_MAPPER
//						.readValue(new String(entity
//								.getEnvironmentDescriptorsBlob(), "UTF-8"),
//								descriptorTypeRef);
//				entity.setEnvironmentDescriptors(descriptors);
//				entity.setEnvironmentDescriptorsBlob(null);
//			} catch (JsonParseException e) {
//				throw new DatastoreException(e);
//			} catch (JsonMappingException e) {
//				throw new DatastoreException(e);
//			} catch (UnsupportedEncodingException e) {
//				throw new DatastoreException(e);
//			} catch (IOException e) {
//				throw new DatastoreException(e);
//			}
//		}
	}

	@Override
	public void validateEntity(Step entity, EntityEvent event)
			throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {

		if (null == entity.getStartDate()) {
			if (EventType.CREATE == event.getType()) {
				entity.setStartDate(new Date()); // set the startDate to now
			} else {
				throw new InvalidModelException(
						"startDate cannot changed to null");
			}
		}

		// TODO when no parentId is specified, these currently go under the root
		// folder. Instead do we want these to go in a folder owned by the user
		// that we create on their behalf?
		
		// Clear the environmentDescriptors and set the blob
//		if (entity.getEnvironmentDescriptors() != null) {
//				try {
//					entity.setEnvironmentDescriptorsBlob(OBJECT_MAPPER.writeValueAsBytes(entity.getEnvironmentDescriptors()));
//				} catch (JsonGenerationException e) {
//					throw new DatastoreException(e);
//				} catch (JsonMappingException e) {
//					throw new DatastoreException(e);
//				} catch (IOException e) {
//					throw new DatastoreException(e);
//				}
//				entity.setEnvironmentDescriptors(null);
//		}
	}

	@Override
	public void entityDeleted(Step deleted) {
		// TODO Auto-generated method stub
	}
}
