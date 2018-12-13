package org.sagebionetworks.doi.datacite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.ORCID_URI;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.PUBLICATION_YEAR;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.PUBLISHER;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.PUBLISHER_VALUE;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.RESOURCE;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.RESOURCE_TYPE;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.RESOURCE_TYPE_GENERAL;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.SCHEMA_LOCATION;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.SCHEMA_LOCATION_VALUE;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.TITLE;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.TITLES;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.createCreatorElement;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.createCreatorsElement;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.createIdentifierElement;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.createPublicationYearElement;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.createPublisherElement;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.createResourceTypeElement;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.createTitleElement;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.createTitlesElement;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.createXmlDom;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.getSchemeUri;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.validateAdherenceToDataciteSchema;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.validateDoiCreator;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.validateDoiCreators;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.validateDoiNameIdentifier;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.validateDoiPublicationYear;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.validateDoiResourceType;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.validateDoiTitle;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.validateDoiTitles;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.xmlToString;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.xerces.dom.DocumentImpl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.doi.v2.DataciteMetadata;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiCreator;
import org.sagebionetworks.repo.model.doi.v2.DoiNameIdentifier;
import org.sagebionetworks.repo.model.doi.v2.DoiResourceType;
import org.sagebionetworks.repo.model.doi.v2.DoiResourceTypeGeneral;
import org.sagebionetworks.repo.model.doi.v2.DoiTitle;
import org.sagebionetworks.repo.model.doi.v2.NameIdentifierScheme;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DataciteMetadataTranslatorTest {

	private DataciteMetadata metadata;
	private Document dom;
	private List<DoiCreator> creators;
	private DoiCreator c1;
	private DoiCreator c2;
	private List<DoiTitle> titles;
	private DoiTitle t1;
	private DoiTitle t2;

	private long publicationYear = 2000L;

	private String uri = "10.1234/syn0000000";
	private String validOrcid = "0000-0003-1415-9269";

	private DoiResourceType resourceType;
	private DoiResourceTypeGeneral resourceTypeGeneral = DoiResourceTypeGeneral.Dataset;


	@Before
	public void before(){
		// Create a new DOM before each test
		dom = new DocumentImpl();

		// Prepare all these objects before each test
		metadata = new Doi();
		creators = new ArrayList<>();
		c1 = new DoiCreator();
		c1.setCreatorName("Last, First");
		c2 = new DoiCreator();
		c2.setCreatorName("Sample name");
		creators.add(c1);
		creators.add(c2);
		metadata.setCreators(creators);

		titles = new ArrayList<>();
		t1 = new DoiTitle();
		t2 = new DoiTitle();
		t1.setTitle("Some title 1");
		t2.setTitle("Some other title 2");
		titles.add(t1);
		titles.add(t2);
		metadata.setTitles(titles);

		metadata.setPublicationYear(publicationYear);

		resourceType = new DoiResourceType();
		resourceType.setResourceTypeGeneral(resourceTypeGeneral);
		metadata.setResourceType(resourceType);
	}

	@Test
	public void createIdentifierElementTest() {
		String id = "10.7303/syn1234";
		Element actual = createIdentifierElement(dom, id);
		assertEquals(IDENTIFIER, actual.getTagName());
		assertEquals(id, actual.getTextContent());
		assertEquals(IDENTIFIER_TYPE_VALUE, actual.getAttribute(IDENTIFIER_TYPE));
	}

	@Test
	public void createCreatorElementTest(){
		Element actual = createCreatorElement(dom, c1);
		assertEquals(CREATOR, actual.getTagName());
		assertEquals(1, actual.getElementsByTagName(CREATOR_NAME).getLength());
		Element creatorNameActual = (Element)actual.getElementsByTagName(CREATOR_NAME).item(0);
		assertEquals(CREATOR_NAME, creatorNameActual.getTagName());
		assertEquals(c1.getCreatorName(), creatorNameActual.getTextContent());

		// Test the second creator that has no name identifiers
		actual = createCreatorElement(dom, c2);
		assertEquals(CREATOR, actual.getTagName());
		assertEquals(1, actual.getElementsByTagName(CREATOR_NAME).getLength());
		creatorNameActual = (Element)actual.getElementsByTagName(CREATOR_NAME).item(0);
		assertEquals(CREATOR_NAME, creatorNameActual.getTagName());
		assertEquals(c2.getCreatorName(), creatorNameActual.getTextContent());
		assertEquals(0, actual.getElementsByTagName(NAME_IDENTIFIER).getLength());

	}

	@Test
	public void createCreatorsElementTest(){
		Element actual = createCreatorsElement(dom, creators);
		assertEquals(CREATORS, actual.getTagName());

		// Ensure that there are two child "c1" tags
		assertEquals(2, actual.getElementsByTagName(CREATOR).getLength());
	}

	@Test
	public void createTitleElementTest(){
		Element actual = createTitleElement(dom, t1);
		assertEquals(TITLE, actual.getTagName());
		assertEquals(t1.getTitle(), actual.getTextContent());

		actual = createTitleElement(dom, t2);
		assertEquals(TITLE, actual.getTagName());
		assertEquals(t2.getTitle(), actual.getTextContent());
	}

	@Test
	public void createTitlesElementTest() {
		Element actual = createTitlesElement(dom, titles);
		assertEquals(TITLES, actual.getTagName());

		// Ensure that there are two child "title" tags
		assertEquals(2, actual.getElementsByTagName(TITLE).getLength());
	}

	@Test
	public void createPublisherElementTest() {
		Element actual = createPublisherElement(dom);

		assertEquals(PUBLISHER, actual.getTagName());
		assertEquals(PUBLISHER_VALUE, actual.getTextContent());
	}


	@Test
	public void createPublicationYearElementTest() {
		Element actual = createPublicationYearElement(dom, String.valueOf(publicationYear));

		assertEquals(PUBLICATION_YEAR, actual.getTagName());
		assertEquals(String.valueOf(publicationYear), actual.getTextContent());
	}

	@Test
	public void createResourceTypeElementTest() {
		Element actual = createResourceTypeElement(dom, resourceType);
		assertEquals(RESOURCE_TYPE, actual.getTagName());
		assertEquals(resourceType.getResourceTypeGeneral().name(), actual.getAttribute(RESOURCE_TYPE_GENERAL));
		assertEquals("", actual.getTextContent());
	}

	@Test
	public void createXmlDomTest() {
		// Ensure the DOM contains one resource tag with appropriate attributes
		Document actualDom = createXmlDom(metadata, uri);
		assertEquals(1, actualDom.getElementsByTagName(RESOURCE).getLength());

		Element actualResource = (Element)actualDom.getElementsByTagName(RESOURCE).item(0);
		assertEquals(NAMESPACE_VALUE, actualResource.getAttribute(NAMESPACE));
		assertEquals(NAMESPACE_PREFIX_VALUE, actualResource.getAttribute(NAMESPACE_PREFIX));
		assertEquals(SCHEMA_LOCATION_VALUE, actualResource.getAttribute(SCHEMA_LOCATION));

		// Ensure the resource tag has one of each of the appropriate child tags
		assertEquals(1, actualResource.getElementsByTagName(IDENTIFIER).getLength());
		assertEquals(1, actualResource.getElementsByTagName(CREATORS).getLength());
		assertEquals(1, actualResource.getElementsByTagName(TITLES).getLength());
		assertEquals(1, actualResource.getElementsByTagName(RESOURCE_TYPE).getLength());
		assertEquals(1, actualResource.getElementsByTagName(PUBLISHER).getLength());
		assertEquals(1, actualResource.getElementsByTagName(PUBLICATION_YEAR).getLength());
	}

	@Test
	public void xmlToStringTest() {
		Document dom = new DocumentImpl();
		String actual = xmlToString(dom);
		assertNotNull(actual);
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

	@Test
	public void testValidateAdherenceToSchemaPass() {
		// all fields should be set in @Before
		validateAdherenceToDataciteSchema(metadata);
	}

	@Test
	public void testValidateCreatorsListPass() {
		// Call under test
		validateDoiCreators(metadata.getCreators());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidNullCreatorsList() {
		// Call under test
		validateDoiCreators(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyCreatorsList() {
		// Call under test
		validateDoiCreators(new ArrayList<>());
	}

	@Test
	public void testValidCreatorPass() {
		DoiCreator creator = new DoiCreator();
		creator.setCreatorName("Anything");
		// Call under test
		validateDoiCreator(creator);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullCreatorName() {
		DoiCreator creator = new DoiCreator();
		creator.setCreatorName(null);
		// Call under test
		validateDoiCreator(creator);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyCreatorName() {
		DoiCreator creator = new DoiCreator();
		creator.setCreatorName("");
		// Call under test
		validateDoiCreator(creator);
	}

	@Test
	@Ignore // Remove this annotation when PLFM-5145 is complete
	public void testValidateCreatorWithNonNullNameIdentifiers() {
		DoiCreator creator = new DoiCreator();
		creator.setCreatorName("A name");

		DoiNameIdentifier id1 = new DoiNameIdentifier();
		id1.setIdentifier(validOrcid);
		id1.setNameIdentifierScheme(NameIdentifierScheme.ORCID);

		DoiNameIdentifier id2 = new DoiNameIdentifier();
		id2.setIdentifier("Another Identifier");
		id2.setNameIdentifierScheme(NameIdentifierScheme.ISNI);

		List<DoiNameIdentifier> ids = new ArrayList<>();
		ids.add(id1);
		ids.add(id2);

		creator.setNameIdentifiers(ids);
		// Call under test
		validateDoiCreator(creator);
	}

	@Test
	public void testNameIdPass() {
		DoiNameIdentifier nameIdentifier = new DoiNameIdentifier();
		nameIdentifier.setNameIdentifierScheme(NameIdentifierScheme.ORCID);
		nameIdentifier.setIdentifier(validOrcid);
		// Call under test
		validateDoiNameIdentifier(nameIdentifier);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNameIdWithoutScheme() {
		DoiNameIdentifier nameIdentifier = new DoiNameIdentifier();
		nameIdentifier.setNameIdentifierScheme(null);
		nameIdentifier.setIdentifier(validOrcid);
		// Call under test
		validateDoiNameIdentifier(nameIdentifier);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNameIdInvalidOrcidFormat() {
		DoiNameIdentifier nameIdentifier = new DoiNameIdentifier();
		nameIdentifier.setNameIdentifierScheme(NameIdentifierScheme.ORCID);
		nameIdentifier.setIdentifier("123-424-253-53X");
		// Call under test
		validateDoiNameIdentifier(nameIdentifier);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNameIdInvalidOrcidCheckDigit() {
		DoiNameIdentifier nameIdentifier = new DoiNameIdentifier();
		nameIdentifier.setNameIdentifierScheme(NameIdentifierScheme.ORCID);
		nameIdentifier.setIdentifier("0000-0003-1415-926X");
		// Call under test
		validateDoiNameIdentifier(nameIdentifier);
	}

	@Test
	public void testValidateTitlesListPass() {
		// Call under test
		validateDoiTitles(metadata.getTitles());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullTitlesList() {
		// Call under test
		validateDoiTitles(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyTitlesList() {
		// Call under test
		validateDoiTitles(new ArrayList<>());
	}

	@Test
	public void testValidateTitlePass() {
		DoiTitle title = new DoiTitle();
		title.setTitle("A Valid Title");
		// Call under test
		validateDoiTitle(title);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullTitleName() {
		DoiTitle title = new DoiTitle();
		title.setTitle(null);
		// Call under test
		validateDoiTitle(title);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyTitleName() {
		DoiTitle title = new DoiTitle();
		title.setTitle("");
		// Call under test
		validateDoiTitle(title);
	}

	@Test
	public void testValidatePublicationYearPass() {
		// Call under test
		validateDoiPublicationYear(1997L);
	}


	@Test(expected = IllegalArgumentException.class)
	public void testNullPublicationYear() {
		// Call under test
		validateDoiPublicationYear(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAncientPublicationYear() {
		// Call under test
		validateDoiPublicationYear(30L);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFuturePublicationYear() {
		// Can't mint a DOI more than 1 year in the future
		// Call under test
		validateDoiPublicationYear((((long) Calendar.getInstance().get(Calendar.YEAR)) + 2L));
	}

	@Test
	public void testValidateResourceTypePass() {
		DoiResourceType resourceType = new DoiResourceType();
		resourceType.setResourceTypeGeneral(DoiResourceTypeGeneral.Dataset);
		// Call under test
		validateDoiResourceType(resourceType);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullResourceType() {
		// Call under test
		validateDoiResourceType(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullResourceTypeGeneral() {
		DoiResourceType resourceType = new DoiResourceType();
		resourceType.setResourceTypeGeneral(null);
		// Call under test
		validateDoiResourceType(resourceType);
	}
}
