package org.sagebionetworks.doi.datacite;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.NameIdentifierScheme;

import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.*;

public class DataciteUtils {

	static String generateDoiUri(Doi doi) {
		String uri = "";
		uri += DOI_URI_PREFIX;
		uri += getPrefix(doi.getObjectType());
		uri += doi.getObjectId();
		if (doi.getObjectVersion() != null) {
			uri += "." + doi.getObjectVersion();
		}
		return uri;
	}

	static String getSchemeUri(NameIdentifierScheme scheme) {
		String uri = null;
		switch (scheme) {
			case ORCID:
				uri = ORCID_URI;
				break;
			case ISNI:
				uri = ISNI_URI;
				break;
			default:
				throw new IllegalArgumentException("Could not resolve URI for unknown name identifier scheme: " + scheme.name());
		}
		return uri;
	}

	static String getPrefix(ObjectType type) {
		String prefix = null;
		switch (type) {
			case ENTITY:
				prefix = ENTITY_PREFIX;
				break;
			default:
				throw new IllegalArgumentException("Could not find prefix for object type: " + type.name());
				// Add cases for new object types if/when they we decide to support them.
		}
		return prefix;
	}
}
