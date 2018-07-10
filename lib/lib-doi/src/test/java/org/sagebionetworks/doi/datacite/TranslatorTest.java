package org.sagebionetworks.doi.datacite;

import org.apache.commons.io.IOUtils;
import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.doi.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class TranslatorTest {

	private DocumentBuilder documentBuilder;

	@Before
	public void before() throws Exception {
		documentBuilder = DocumentBuilderFactoryImpl.newInstance().newDocumentBuilder();
	}

	@Test
	public void getNameIdTest() throws Exception {
		DataciteTranslator translator = new DataciteTranslator();

		// Creator element with just a name, no identifiers
		Document dom = documentBuilder.parse(new InputSource(new StringReader("<nameIdentifier schemeURI=\"http://orcid.org/\" nameIdentifierScheme=\"ORCID\">0123-4567-8987-789X</nameIdentifier>")));
		Element id = (Element)dom.getElementsByTagName("nameIdentifier").item(0);
		DoiNameIdentifier expected = new DoiNameIdentifier();

		expected.setIdentifier("0123-4567-8987-789X");
		expected.setNameIdentifierScheme(NameIdentifierSchemes.ORCID);

		// Unit under test
		assertEquals(expected, translator.getNameIdentifier(id));
	}

	@Test
	public void getCreatorTest() throws Exception {
		DataciteTranslator translator = new DataciteTranslator();

		// Creator element with just a name
		Document dom = documentBuilder.parse(new InputSource(new StringReader("<creator><creatorName>Author</creatorName></creator>")));
		Element creator = (Element)dom.getElementsByTagName("creator").item(0);
		DoiCreator expected = new DoiCreator();
		expected.setCreatorName("Author");
		// Unit under test
		assertEquals(expected, translator.getCreator(creator));
	}

	@Test
	public void getCreatorWithMultipleIdentifiersTest() throws Exception {
		DataciteTranslator translator = new DataciteTranslator();
		Document dom = documentBuilder.parse(new InputSource(new StringReader(
				"<creator><creatorName>Author</creatorName><nameIdentifier schemeURI=\"http://orcid.org/\" nameIdentifierScheme=\"ORCID\">0123-4567-8987-789X</nameIdentifier>" +
						"            <nameIdentifier schemeURI=\"http://www.insi.org/\" nameIdentifierScheme=\"ISNI\">9876-5432-10123-456X</nameIdentifier></creator>"
		)));
		Element creator = (Element)dom.getElementsByTagName("creator").item(0);

		DoiCreator expected = new DoiCreator();
		expected.setCreatorName("Author");

		DoiNameIdentifier id1 = new DoiNameIdentifier();
		id1.setNameIdentifierScheme(NameIdentifierSchemes.ORCID);
		id1.setIdentifier("0123-4567-8987-789X");
		DoiNameIdentifier id2 = new DoiNameIdentifier();
		id2.setNameIdentifierScheme(NameIdentifierSchemes.ISNI);
		id2.setIdentifier("9876-5432-10123-456X");

		List<DoiNameIdentifier> ids = new ArrayList<>();
		ids.add(id1);
		ids.add(id2);

		expected.setNameIdentifiers(ids);
		// Unit under test
		assertEquals(expected, translator.getCreator(creator));
	}

	@Test
	public void getCreatorsTest() throws Exception {
		DataciteTranslator translator = new DataciteTranslator();

		Document dom = documentBuilder.parse(new InputSource(new StringReader("<creators><creator><creatorName>Author 1</creatorName></creator><creator><creatorName>2, Author</creatorName></creator></creators>")));
		List<DoiCreator> expected = new ArrayList<>();
		DoiCreator creator1 = new DoiCreator();
		creator1.setCreatorName("Author 1");
		DoiCreator creator2 = new DoiCreator();
		creator2.setCreatorName("2, Author");
		expected.add(creator1);
		expected.add(creator2);

		assertEquals(expected, translator.getCreators(dom));
	}

	@Test
	public void getTitlesTest() throws Exception {
		DataciteTranslator translator = new DataciteTranslator();

		Document dom = documentBuilder.parse(new InputSource(new StringReader("<titles><title>Title 1</title><title>Title 2</title></titles>")));
		List<DoiTitle> expected = new ArrayList<>();
		DoiTitle t1 = new DoiTitle();
		DoiTitle t2 = new DoiTitle();
		t1.setTitle("Title 1");
		t2.setTitle("Title 2");
		expected.add(t1);
		expected.add(t2);

		assertEquals(expected, translator.getTitles(dom));
	}

	@Test
	public void getPubYearTest() throws Exception {
		DataciteTranslator translator = new DataciteTranslator();
		Document dom = documentBuilder.parse(new InputSource(new StringReader("<publicationYear>1929</publicationYear>")));
		long expected = 1929;
		assertEquals(expected, translator.getPublicationYear(dom));
	}

	@Test
	public void getResourceTypeTest() throws Exception {
		DataciteTranslator translator = new DataciteTranslator();
		Document dom = documentBuilder.parse(new InputSource(new StringReader("<resourceType resourceTypeGeneral=\"Dataset\"></resourceType>")));

		DoiResourceType expected = new DoiResourceType();
		expected.setResourceTypeGeneral(DoiResourceTypeGeneral.Dataset);
		assertEquals(expected, translator.getResourceType(dom));
	}


	// Tests the entire class
	@Test
	public void translateEntireDomTest() throws Exception{
		DoiMetadata expectedMetadata = new DoiMetadata();
		// Set fields to match the XML resource loaded below
		// Creators
		DoiCreator creator1 = new DoiCreator();
		creator1.setCreatorName("Last, First");
		DoiNameIdentifier nameId1 = new DoiNameIdentifier();
		nameId1.setIdentifier("0123-4567-8987-789X");
		nameId1.setNameIdentifierScheme(NameIdentifierSchemes.ORCID);
		DoiNameIdentifier nameId2 = new DoiNameIdentifier();
		nameId2.setIdentifier("9876-5432-10123-456X");
		nameId2.setNameIdentifierScheme(NameIdentifierSchemes.ISNI);
		List<DoiNameIdentifier> nameIds = new ArrayList<>();
		nameIds.add(nameId1);
		nameIds.add(nameId2);
		creator1.setNameIdentifiers(nameIds);
		DoiCreator creator2 = new DoiCreator();
		creator2.setCreatorName("Sample name");
		List<DoiCreator> creators = new ArrayList<>();
		creators.add(creator1);
		creators.add(creator2);
		expectedMetadata.setCreators(creators);
		// Titles
		DoiTitle title1 = new DoiTitle();
		DoiTitle title2 = new DoiTitle();
		title1.setTitle("Some title 1");
		title2.setTitle("Some other title 2");
		List<DoiTitle> titles = new ArrayList<>();
		titles.add(title1);
		titles.add(title2);
		expectedMetadata.setTitles(titles);
		// Publication year
		expectedMetadata.setPublicationYear(2000L);
		// Resource type
		DoiResourceType resourceType = new DoiResourceType();
		resourceType.setResourceTypeGeneral(DoiResourceTypeGeneral.Dataset);
		expectedMetadata.setResourceType(resourceType);

		// Load the resource containing XML
		ClassLoader loader = this.getClass().getClassLoader();
		String xml = IOUtils.toString(loader.getResourceAsStream("DataciteSample1.xml"));

		DataciteTranslator translator = new DataciteTranslator();
		// Unit under test
		DoiMetadata metadata = translator.translate(xml);
		assertEquals(expectedMetadata, metadata);
	}


}
