package org.sagebionetworks.doi.datacite;

import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.CREATOR;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.CREATORS;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.CREATOR_NAME;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.IDENTIFIER;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.IDENTIFIER_TYPE;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.IDENTIFIER_TYPE_VALUE;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.ISNI_URI;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.NAMESPACE;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.NAMESPACE_PREFIX;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.NAMESPACE_PREFIX_VALUE;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.NAMESPACE_VALUE;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.NAME_IDENTIFIER;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.NAME_IDENTIFIER_SCHEME;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.ORCID_URI;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.PUBLICATION_YEAR;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.PUBLISHER;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.PUBLISHER_VALUE;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.RESOURCE;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.RESOURCE_TYPE;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.RESOURCE_TYPE_GENERAL;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.SCHEMA_LOCATION;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.SCHEMA_LOCATION_VALUE;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.SCHEME_URI;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.TITLE;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.TITLES;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.xerces.dom.DocumentImpl;
import org.sagebionetworks.repo.model.doi.v2.DataciteMetadata;
import org.sagebionetworks.repo.model.doi.v2.DoiCreator;
import org.sagebionetworks.repo.model.doi.v2.DoiNameIdentifier;
import org.sagebionetworks.repo.model.doi.v2.DoiResourceType;
import org.sagebionetworks.repo.model.doi.v2.DoiTitle;
import org.sagebionetworks.repo.model.doi.v2.NameIdentifierScheme;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

/*
 * Translates our DoiV2 object into well-formed DataCite XML.
 */
public class DataciteMetadataTranslatorImpl implements DataciteMetadataTranslator {

	private static final String VALID_ORCID_REGEX = "[0-9]{4}\\-[0-9]{4}\\-[0-9]{4}-[0-9]{3}[0-9, X]";
	private static final String VALID_ORCID_DESCRIPTION = "ORCID IDs must be 16 digits, separated into 4 chunks of 4 digits by dashes.";
	static Pattern ORCID_PARSER = Pattern.compile(VALID_ORCID_REGEX);

	public String translate(DataciteMetadata metadata, final String doiUri) {
		return translateUtil(metadata, doiUri);
	}

	static String translateUtil(DataciteMetadata doi, final String doiUri) {
		validateAdherenceToDataciteSchema(doi);
		Document dom = createXmlDom(doi, doiUri);
		return xmlToString(dom);
	}

	static String xmlToString(Document dom) {
		OutputFormat format = new OutputFormat(dom);
		StringWriter xml = new StringWriter();
		XMLSerializer serializer = new XMLSerializer(xml, format);

		try {
			serializer.serialize(dom);
		} catch (IOException e) {
			throw new RuntimeException("Error occurred while serializing metadata", e);
		}
		return xml.toString();
	}

	static Document createXmlDom(DataciteMetadata doi, final String doiUri) {
		Document dom = new DocumentImpl();
		Element resource = dom.createElement(RESOURCE);
		resource.setAttribute(NAMESPACE_PREFIX, NAMESPACE_PREFIX_VALUE);
		resource.setAttribute(NAMESPACE, NAMESPACE_VALUE);
		resource.setAttribute(SCHEMA_LOCATION, SCHEMA_LOCATION_VALUE);
		dom.appendChild(resource);

		resource.appendChild(createIdentifierElement(dom, doiUri));
		resource.appendChild(createCreatorsElement(dom, doi.getCreators()));
		resource.appendChild(createTitlesElement(dom, doi.getTitles()));
		resource.appendChild(createPublisherElement(dom));
		resource.appendChild(createPublicationYearElement(dom, String.valueOf(doi.getPublicationYear())));
		resource.appendChild(createResourceTypeElement(dom, doi.getResourceType()));
		return dom;
	}


	static Element createIdentifierElement(Document dom, String doiUri) {
		Element identifier = dom.createElement(IDENTIFIER);
		identifier.setAttribute(IDENTIFIER_TYPE, IDENTIFIER_TYPE_VALUE);
		identifier.setTextContent(doiUri);
		return identifier;
	}

	static Element createCreatorElement(Document dom, DoiCreator creator) {
		Element creatorElement = dom.createElement(CREATOR);
		Element creatorName = dom.createElement(CREATOR_NAME);
		creatorName.setTextContent(creator.getCreatorName());
		creatorElement.appendChild(creatorName);
		if (creator.getNameIdentifiers() != null) {
			for (DoiNameIdentifier nameIdentifier : creator.getNameIdentifiers()) {
				Element nameIdElement = dom.createElement(NAME_IDENTIFIER);
				nameIdElement.setTextContent(nameIdentifier.getIdentifier());
				nameIdElement.setAttribute(NAME_IDENTIFIER_SCHEME, nameIdentifier.getNameIdentifierScheme().name());
				nameIdElement.setAttribute(SCHEME_URI, getSchemeUri(nameIdentifier.getNameIdentifierScheme()));
				creatorElement.appendChild(nameIdElement);
			}
		}
		return creatorElement;
	}

	static Element createCreatorsElement(Document dom, List<DoiCreator> creators) {
		Element creatorsElement = dom.createElement(CREATORS);
		for (DoiCreator creator : creators) {
			creatorsElement.appendChild(createCreatorElement(dom, creator));
		}
		return creatorsElement;
	}

	static Element createTitleElement(Document dom, DoiTitle title) {
		Element titleElement = dom.createElement(TITLE);
		titleElement.setTextContent(title.getTitle());
		return titleElement;
	}

	static Element createTitlesElement(Document dom, List<DoiTitle> titles) {
		Element titlesElement = dom.createElement(TITLES);
		for (DoiTitle title : titles) {
			titlesElement.appendChild(createTitleElement(dom, title));
		}
		return titlesElement;
	}

	static Element createPublisherElement(Document dom) {
		Element publisher = dom.createElement(PUBLISHER);
		publisher.setTextContent(PUBLISHER_VALUE);
		return publisher;
	}

	static Element createPublicationYearElement(Document dom, String publicationYear) {
		Element publicationYearElement = dom.createElement(PUBLICATION_YEAR);
		publicationYearElement.setTextContent(publicationYear);
		return publicationYearElement;
	}

	static Element createResourceTypeElement(Document dom, DoiResourceType resourceType) {
		Element resourceTypeElement = dom.createElement(RESOURCE_TYPE);
		resourceTypeElement.setAttribute(RESOURCE_TYPE_GENERAL, resourceType.getResourceTypeGeneral().name());
		return resourceTypeElement;
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
	
	public static void validateAdherenceToDataciteSchema(DataciteMetadata doi) throws IllegalArgumentException {
		validateDoiCreators(doi.getCreators());
		validateDoiTitles(doi.getTitles());
		validateDoiPublicationYear(doi.getPublicationYear());
		validateDoiResourceType(doi.getResourceType());
	}

	static void validateDoiCreators(List<DoiCreator> creators) {
		if (creators == null) {
			throw new IllegalArgumentException("DOI metadata must have property \"Creators\"");
		} else if (creators.size() == 0) {
			throw new IllegalArgumentException("DOI creators must include at least one \"DoiCreator\"");
		}
		creators.forEach(DataciteMetadataTranslatorImpl::validateDoiCreator);
	}

	static void validateDoiCreator(DoiCreator creator) {
		if (creator.getCreatorName() == null) {
			throw new IllegalArgumentException("DoiCreators must have property \"Creator Name\"");
		}
		if (creator.getCreatorName().length() == 0) {
			throw new IllegalArgumentException("Creator names must be at least 1 character long.");
		}
		if (creator.getNameIdentifiers() != null) {
			throw new UnsupportedOperationException("Synapse does not currently support name identifiers.");
			// TODO: support name identifiers, see PLFM-5145
			//creator.getNameIdentifiers().forEach(DataciteMetadataTranslatorImpl::validateDoiNameIdentifier);
		}
	}

	static void validateDoiNameIdentifier(DoiNameIdentifier identifier) {
		if (identifier.getNameIdentifierScheme() == null) {
			throw new IllegalArgumentException("Name identifiers must have an included schema");
		}
		if (identifier.getNameIdentifierScheme() == NameIdentifierScheme.ORCID) {
			validateOrcidId(identifier.getIdentifier());
		}
	}

	static void validateOrcidId(String orcidId) {
		if (!ORCID_PARSER.matcher(orcidId).matches()) {
			throw new IllegalArgumentException(VALID_ORCID_DESCRIPTION);
		}
		String strippedOrcidId = orcidId.replaceAll("\\-", "");
		String actualCheckDigit = strippedOrcidId.substring(15,16);
		strippedOrcidId = strippedOrcidId.substring(0,15);
		String expectedCheckDigit = generateOrcidCheckDigit(strippedOrcidId);
		if (!expectedCheckDigit.equals(actualCheckDigit)) {
			throw new IllegalArgumentException("The provided ORCID ID " + orcidId + " is invalid. The input check digit value did not match the expected value.");
		}
	}

	/**
	 * Generates check digit as per ISO 7064 11,2.
	 * @author orcid.org
	 * see https://support.orcid.org/knowledgebase/articles/116780-structure-of-the-orcid-identifier
	 */
	static String generateOrcidCheckDigit(String baseDigits) {
		int total = 0;
		for (int i = 0; i < baseDigits.length(); i++) {
			int digit = Character.getNumericValue(baseDigits.charAt(i));
			total = (total + digit) * 2;
		}
		int remainder = total % 11;
		int result = (12 - remainder) % 11;
		return result == 10 ? "X" : String.valueOf(result);
	}

	static void validateDoiTitles(List<DoiTitle> titles) {
		if (titles == null) {
			throw new IllegalArgumentException("DOI metadata must have property \"Titles\"");
		} else if (titles.size() == 0) {
			throw new IllegalArgumentException("DOI titles must include at least one \"DoiTitle\"");
		}
		titles.forEach(DataciteMetadataTranslatorImpl::validateDoiTitle);
	}

	static void validateDoiTitle(DoiTitle title) {
		if (title.getTitle() == null) {
			throw new IllegalArgumentException("DOI Titles must have property \"Title\"");
		}
		if (title.getTitle().length() == 0) {
			throw new IllegalArgumentException("Titles must be at least 1 character long.");
		}
	}

	static void validateDoiPublicationYear(Long publicationYear) {
		if (publicationYear == null) {
			throw new IllegalArgumentException("DOI must have property \"publicationYear\"");
		}
		if (publicationYear < 1000 || publicationYear > (Calendar.getInstance().get(Calendar.YEAR) + 1)) {
			throw new IllegalArgumentException("DOI publication year must be between \"1000\" and \"" + (Calendar.getInstance().get(Calendar.YEAR) + 1) + "\"");
		}
	}

	static void validateDoiResourceType(DoiResourceType resourceType) {
		if (resourceType == null) {
			throw new IllegalArgumentException("DOI must have property \"ResourceType\"");
		}
		if (resourceType.getResourceTypeGeneral() == null) {
			throw new IllegalArgumentException("DOI Resource Type must have property \"ResourceTypeGeneral\"");
		}
	}
}
