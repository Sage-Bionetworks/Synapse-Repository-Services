package org.sagebionetworks.doi.datacite;

import org.junit.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.NameIdentifierScheme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.*;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.DOI_URI_PREFIX;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.ENTITY_PREFIX;
import static org.sagebionetworks.doi.datacite.DataciteUtils.generateDoiUri;
import static org.sagebionetworks.doi.datacite.DataciteUtils.getPrefix;
import static org.sagebionetworks.doi.datacite.DataciteUtils.getSchemeUri;

public class DataciteUtilsTest {
	
	@Test
	public void generateDoiUriTest() {
		// No version number
		Doi doi = new Doi();
		String objectId = "1234";
		doi.setObjectId(objectId);
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectVersion(null);
		assertEquals(DOI_URI_PREFIX + ENTITY_PREFIX + objectId, generateDoiUri(doi));

		// With version number
		doi.setObjectId(objectId);
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectVersion(4L);
		assertEquals(DOI_URI_PREFIX + ENTITY_PREFIX + objectId + "." + Long.toString(4L), generateDoiUri(doi));
	}

	@Test
	public void testGetPrefix() {
		assertEquals(ENTITY_PREFIX, getPrefix(ObjectType.ENTITY));
	}

	@Test
	public void testNoUndefinedSchemes() {
		for (NameIdentifierScheme e : NameIdentifierScheme.values()) {
			assertNotNull(getSchemeUri(e));
		}
	}

	@Test
	public void testGetSchemeUri() {
		assertEquals(ORCID_URI, getSchemeUri(NameIdentifierScheme.ORCID));
		assertEquals(ISNI_URI, getSchemeUri(NameIdentifierScheme.ISNI));
		// If adding a new scheme, test it with its URI pair here
	}
}
