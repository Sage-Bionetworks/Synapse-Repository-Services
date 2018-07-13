package org.sagebionetworks.doi.datacite;

import org.sagebionetworks.repo.model.doi.v2.Doi;

public interface DataciteMetadataTranslator {

	/*
	 * Translates a Doi object into a Datacite Metadata XML String
	 * Adherent to Datacite Schema v4.1
	 */
	String translate(Doi doi);
}
