package org.sagebionetworks.ids;

import java.util.UUID;

/**
 * Generates a UUID as an ETag. Note that the UUID
 * is generated randomly independent of the entity.
 */
public class UuidETagGenerator implements ETagGenerator {

	public static final String ZERO_E_TAG = new UUID(0L, 0L).toString();

	@Override
	public String generateETag() {
		return UUID.randomUUID().toString();
	}
}
