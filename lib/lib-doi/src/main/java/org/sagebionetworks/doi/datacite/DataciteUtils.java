package org.sagebionetworks.doi.datacite;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.doi.v2.NameIdentifierScheme;

import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.*;

class DataciteUtils {
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

	/*
	 * Generates a doiUri from the scheme {DOI_URI_PREFIX}/{object type prefix}{objectId}<.{version}>
	 */
	static String generateDoiUri(DoiAssociation association) {
		String uri = "";
		uri += DOI_URI_PREFIX + "/";
		uri += getObjectTypePrefix(association.getObjectType());
		uri += getObjectIdVersionSuffix(association);
		return uri;
	}

	/*
	 * Maps the object type to the DOI prefix.
	 * For example, an Entity will give "syn", which gives the "syn" in "10.1234/syn{id}
	 */
	static String getObjectTypePrefix(ObjectType type) {
		String prefix = null;
		switch (type) {
			case ENTITY:
				prefix = ENTITY_PREFIX;
				break;
			default:
				throw new IllegalArgumentException("Could not find prefix for object type: " + type.name());
				// Add cases for new object types if/when we decide to support them.
		}
		return prefix;
	}

	static String getObjectIdVersionSuffix(DoiAssociation association) {
		String suffix = association.getObjectId();
		if (association.getObjectVersion() != null) {
			suffix += "." + association.getObjectVersion();
		}
		return suffix;
	}
}
