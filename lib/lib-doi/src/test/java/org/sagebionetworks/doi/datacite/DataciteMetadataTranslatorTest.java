package org.sagebionetworks.doi.datacite;

import org.apache.xerces.dom.DocumentImpl;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.doi.v2.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.*;
import static org.sagebionetworks.doi.datacite.DataciteMetadataTranslatorImpl.*;

public class DataciteMetadataTranslatorTest {

	private DataciteMetadata metadata;
	private Document dom;
	private List<DoiCreator> creators;
	private DoiCreator c1;
	private DoiCreator c2;
	private DoiNameIdentifier nameId1;
	private DoiNameIdentifier nameId2;
	private List<DoiTitle> titles;
	private DoiTitle t1;
	private DoiTitle t2;

	private long publicationYear = 2000L;

	private String uri = "10.1234/syn0000000";

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
		nameId1 = new DoiNameIdentifier();
		nameId1.setIdentifier("0123-4567-8987-789X");
		nameId1.setNameIdentifierScheme(NameIdentifierScheme.ORCID);
		nameId2 = new DoiNameIdentifier();
		nameId2.setIdentifier("9876-5432-1012-456X");
		nameId2.setNameIdentifierScheme(NameIdentifierScheme.ISNI);
		List<DoiNameIdentifier> ids = new ArrayList<>();
		ids.add(nameId1);
		ids.add(nameId2);
		c1.setNameIdentifiers(ids);
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

		// Test the name identifiers
		assertEquals(2, actual.getElementsByTagName(NAME_IDENTIFIER).getLength());
		Element nameIdActual = (Element) actual.getElementsByTagName(NAME_IDENTIFIER).item(0);
		assertEquals(nameId1.getNameIdentifierScheme().name(), nameIdActual.getAttribute(NAME_IDENTIFIER_SCHEME));
		assertEquals(nameId1.getIdentifier(), nameIdActual.getTextContent());

		nameIdActual = (Element) actual.getElementsByTagName(NAME_IDENTIFIER).item(1);
		assertEquals(nameId2.getNameIdentifierScheme().name(), nameIdActual.getAttribute(NAME_IDENTIFIER_SCHEME));
		assertEquals(nameId2.getIdentifier(), nameIdActual.getTextContent());

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
}
