package org.sagebionetworks.doi.datacite;

import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.sagebionetworks.repo.model.doi.v2.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.*;

/*
 * Translates DataCite metadata from XML to our DoiV2 object.
 */
public class DataciteXmlTranslatorImpl implements DataciteXmlTranslator {

	public DataciteMetadata translate(String xml) {
		Document dom = parseXml(xml);
		return translateUtil(dom);
	}

	/*
	 * Translates a Datacite XML String into a DoiMetadata object
	 * Works for XML adherent to Datacite Schemas 2.2 and 4.1
	 */
	static DataciteMetadata translateUtil(Document dom) {
		DataciteMetadata doi = new Doi();

		doi.setCreators(getCreators(dom));
		doi.setTitles(getTitles(dom));
		doi.setPublicationYear(Long.valueOf(getPublicationYear(dom)));
		doi.setResourceType(getResourceType(dom));

		return doi;
	}

	static Document parseXml(String xml) {
		Document dom = null;
		try {
			DocumentBuilder documentBuilder = DocumentBuilderFactoryImpl.newInstance().newDocumentBuilder();
			dom = documentBuilder.parse(new InputSource(new StringReader(xml)));
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Error occurred while configuring XML parser", e);
		} catch (SAXException | IOException e) {
			throw new RuntimeException("Error occurred while parsing metadata", e);
		}
		return dom;
	}

	static DoiNameIdentifier getNameIdentifier(Element nameIdElement) {
		DoiNameIdentifier id = new DoiNameIdentifier();
		id.setIdentifier(nameIdElement.getTextContent());
		id.setNameIdentifierScheme(NameIdentifierScheme.valueOf(nameIdElement.getAttributes()
				.getNamedItem(NAME_IDENTIFIER_SCHEME).getNodeValue()));
		return id;
	}

	static DoiCreator getCreator(Element creatorElement) {
		DoiCreator creator = new DoiCreator();
		creator.setCreatorName(creatorElement.getElementsByTagName(CREATOR_NAME).item(0).getTextContent());

		NodeList idNodes = creatorElement.getElementsByTagName(NAME_IDENTIFIER);
		if (idNodes.getLength() > 0) {
			List<DoiNameIdentifier> ids = new ArrayList<>();
			for (int i = 0; i < idNodes.getLength(); i++) {
				DoiNameIdentifier id = getNameIdentifier((Element)idNodes.item(i));
				ids.add(id);

			}
			creator.setNameIdentifiers(ids);
		}
		return creator;
	}

	static List<DoiCreator> getCreators(Document dom) {
		List<DoiCreator> creators = new ArrayList<>();
		Element creatorsElement = (Element)dom.getElementsByTagName(CREATORS).item(0);
		NodeList creatorList = creatorsElement.getElementsByTagName(CREATOR);
		for (int i = 0; i < creatorList.getLength(); i++) {
			creators.add(getCreator((Element)creatorList.item(i)));
		}
		return creators;
	}

	static DoiTitle getTitle(Node titleXml) {
		DoiTitle title = new DoiTitle();
		title.setTitle(titleXml.getTextContent());
		return title;
	}

	static List<DoiTitle> getTitles(Document dom) {
		List<DoiTitle> titles = new ArrayList<>();
		NodeList titleList = ((Element)dom.getElementsByTagName(TITLES).item(0)).getElementsByTagName("title");
		for (int i = 0; i < titleList.getLength(); i++) {
			titles.add(getTitle(titleList.item(i)));
		}
		return titles;
	}

	static String getPublicationYear(Document dom) {
		return dom.getElementsByTagName(PUBLICATION_YEAR).item(0).getTextContent();
	}

	static DoiResourceType getResourceType(Document dom) {
		DoiResourceType resourceType = null;
		Node xmlResourceType = dom.getElementsByTagName(RESOURCE_TYPE).item(0);
		if (xmlResourceType != null) {
			resourceType = new DoiResourceType();
			resourceType.setResourceTypeGeneral(DoiResourceTypeGeneral.valueOf(xmlResourceType
					.getAttributes().getNamedItem(RESOURCE_TYPE_GENERAL).getNodeValue()));
		}
		return resourceType;
	}
}