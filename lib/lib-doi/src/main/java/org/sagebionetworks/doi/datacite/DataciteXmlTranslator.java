package org.sagebionetworks.doi.datacite;

import org.sagebionetworks.repo.model.doi.v2.Doi;

public interface DataciteXmlTranslator {
	/*
	 * Translates a Datacite XML String into a Doi object
	 * Works for XML adherent to Datacite Schemas 2.2 and 4.1
	 */
	Doi translate(String xml);
}
