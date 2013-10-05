package org.sagebionetworks.ids;

import java.util.UUID;

import org.sagebionetworks.repo.model.TaggableEntity;
import org.springframework.beans.factory.InitializingBean;

/**
 * Generates a UUID as an ETag. Note that the UUID
 * is generated randomly independent of the entity.
 */
public class UuidETagGenerator implements ETagGenerator, InitializingBean {

	public static final String ZERO_E_TAG = new UUID(0L, 0L).toString();

	@Override
	public String generateETag(TaggableEntity entity) {
		return UUID.randomUUID().toString();
	}

	@Override
	public String generateETag() {
		return UUID.randomUUID().toString();
	};

	@Override
	public void afterPropertiesSet() throws Exception {}
}
