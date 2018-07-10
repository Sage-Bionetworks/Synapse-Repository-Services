package org.sagebionetworks.doi.datacite;

import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.sagebionetworks.repo.model.doi.*;
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

public class DataciteTranslator {

	/*
	 * Translates a Datacite XML String into a DoiMetadata object
	 * Works for XML adherent to Datacite Schemas 2.2 and 4.1
	 */
	public DoiMetadata translate(String xml) {
		Document dom = null;
		try {
			DocumentBuilder documentBuilder = DocumentBuilderFactoryImpl.newInstance().newDocumentBuilder();
			dom = documentBuilder.parse(new InputSource(new StringReader(xml)));
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Error occurred while configuring XML parser", e);
		} catch (SAXException | IOException e) {
			throw new RuntimeException("Error occurred while parsing metadata", e);
		}

		DoiMetadata doiMetadata = new DoiMetadata();

		doiMetadata.setCreators(getCreators(dom));
		doiMetadata.setTitles(getTitles(dom));
		doiMetadata.setPublicationYear(getPublicationYear(dom));
		doiMetadata.setResourceType(getResourceType(dom));

		return doiMetadata;
	}

	DoiNameIdentifier getNameIdentifier(Element nameIdElement) {
		DoiNameIdentifier id = new DoiNameIdentifier();
		id.setIdentifier(nameIdElement.getTextContent());
		id.setNameIdentifierScheme(NameIdentifierSchemes.valueOf(nameIdElement.getAttributes()
				.getNamedItem("nameIdentifierScheme").getNodeValue()));
		return id;
	}

	DoiCreator getCreator(Element creatorElement) {
		DoiCreator creator = new DoiCreator();
		creator.setCreatorName(creatorElement.getElementsByTagName("creatorName").item(0).getTextContent());

		NodeList idNodes = creatorElement.getElementsByTagName("nameIdentifier");
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

	List<DoiCreator> getCreators(Document dom) {
		List<DoiCreator> creators = new ArrayList<>();
		Element creatorsElement = (Element)dom.getElementsByTagName("creators").item(0);
		NodeList creatorList = creatorsElement.getElementsByTagName("creator");
		for (int i = 0; i < creatorList.getLength(); i++) {
			creators.add(getCreator((Element)creatorList.item(i)));
		}
		return creators;
	}

	DoiTitle getTitle(Node titleXml) {
		DoiTitle title = new DoiTitle();
		title.setTitle(titleXml.getTextContent());
		return title;
	}

	List<DoiTitle> getTitles(Document dom) {
		List<DoiTitle> titles = new ArrayList<>();
		NodeList titleList = ((Element)dom.getElementsByTagName("titles").item(0)).getElementsByTagName("title");
		for (int i = 0; i < titleList.getLength(); i++) {
			titles.add(getTitle(titleList.item(i)));
		}
		return titles;
	}

	long getPublicationYear(Document dom) {
		return Long.valueOf(dom.getElementsByTagName("publicationYear").item(0).getTextContent());
	}

	DoiResourceType getResourceType(Document dom) {
		DoiResourceType resourceType = null;
		Node xmlResourceType = dom.getElementsByTagName("resourceType").item(0);
		if (xmlResourceType != null) {
			resourceType = new DoiResourceType();
			resourceType.setResourceTypeGeneral(DoiResourceTypeGeneral.valueOf(xmlResourceType
					.getAttributes().getNamedItem("resourceTypeGeneral").getNodeValue()));
		}
		return resourceType;
	}

}