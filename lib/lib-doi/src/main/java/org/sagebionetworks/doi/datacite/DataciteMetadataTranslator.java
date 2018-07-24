package org.sagebionetworks.doi.datacite;

import org.sagebionetworks.repo.model.doi.v2.DataciteMetadata;

public interface DataciteMetadataTranslator {

	/*
	 * Translates a Doi object into a Datacite Metadata XML String
	 * Adherent to Datacite Schema v4.1
	 */
	String translate(DataciteMetadata doi, String doiUri);
}
