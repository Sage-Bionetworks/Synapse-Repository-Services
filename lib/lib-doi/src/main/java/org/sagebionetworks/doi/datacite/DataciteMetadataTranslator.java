package org.sagebionetworks.doi.datacite;

import org.sagebionetworks.repo.model.doi.DataciteDoi;

public interface DataciteMetadataTranslator {

	/*
	 * Translates a Doi object into a Datacite Metadata XML String
	 * Adherent to Datacite Schema v4.1
	 */
	String translate(DataciteDoi doi);
}
