package org.sagebionetworks.doi.datacite;

/**
 * Constants for creating DataCite metadata.
 */
class DataciteMetadataConstants {


	/**
	 * DOI Publisher is always Synapse.
	 */
	static final String PUBLISHER_VALUE = "Synapse";

	//XML, Datacite required attributes
	static final String NAMESPACE = "xmlns";
	static final String NAMESPACE_VALUE = "http://datacite.org/schema/kernel-4";
	static final String NAMESPACE_PREFIX = "xmlns:xsi";
	static final String NAMESPACE_PREFIX_VALUE = "http://www.w3.org/2001/XMLSchema-instance";
	static final String SCHEMA_LOCATION = "xsi:schemaLocation";
	static final String SCHEMA_LOCATION_VALUE = "http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4.1/metadata.xsd";

	// Tag names and attributes specific to DataCite schema
	static final String RESOURCE = "resource";
	static final String IDENTIFIER = "identifier";
	static final String IDENTIFIER_TYPE = "identifierType";
	static final String IDENTIFIER_TYPE_VALUE = "DOI";
	static final String CREATOR = "creator";
	static final String CREATOR_NAME = "creatorName";
	static final String NAME_IDENTIFIER = "nameIdentifier";
	static final String NAME_IDENTIFIER_SCHEME = "nameIdentifierScheme";
	static final String SCHEME_URI = "schemeURI";
	static final String CREATORS = "creators";
	static final String TITLE = "title";
	static final String TITLES = "titles";
	static final String PUBLISHER = "publisher";
	static final String PUBLICATION_YEAR = "publicationYear";
	static final String RESOURCE_TYPE = "resourceType";
	static final String RESOURCE_TYPE_GENERAL = "resourceTypeGeneral";

	// URIs for name identifier schemes
	static final String ORCID_URI = "http://orcid.org/";
	static final String ISNI_URI = "http://www.isni.org/";
}
