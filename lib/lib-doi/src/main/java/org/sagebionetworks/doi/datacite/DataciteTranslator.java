package org.sagebionetworks.doi.datacite;

import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.sagebionetworks.repo.model.doi.*;
import org.w3c.dom.Document;
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
		xml.replaceAll("\n","");
		Document dom = null;
		try {
			DocumentBuilder documentBuilder = DocumentBuilderFactoryImpl.newInstance().newDocumentBuilder();
			dom = documentBuilder.parse(new InputSource(new StringReader(xml)));
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Error occurred while configuring XML parser", e);
		} catch (SAXException | IOException e) {
			throw new RuntimeException("Error occurred while parsing metadata", e);
		}

		// There may be multiple creators or titles
		DoiMetadata doiMetadata = new DoiMetadata();
		NodeList xmlCreators = dom.getElementsByTagName("creatorName");
		List<DoiCreator> metadataCreators = new ArrayList<>();
		for (int i = 0; i < xmlCreators.getLength(); i++) {
			DoiCreator creator = new DoiCreator();
			creator.setCreatorName(xmlCreators.item(i).getTextContent());
			metadataCreators.add(creator);
		}
		doiMetadata.setCreators(metadataCreators);

		NodeList xmlTitles = dom.getElementsByTagName("title");
		List<DoiTitle> metadataTitles = new ArrayList<>();
		for (int i = 0; i < xmlTitles.getLength(); i++) {
			DoiTitle title = new DoiTitle();
			title.setTitle(xmlTitles.item(i).getTextContent());
			metadataTitles.add(title);
		}
		doiMetadata.setTitles(metadataTitles);

		// There should only be one publication year and resource type, and resource type is not guaranteed to exist
		NodeList publicationYear = dom.getElementsByTagName("publicationYear");
		doiMetadata.setPublicationYear(Long.valueOf(publicationYear.item(0).getTextContent()));

		NodeList xmlResourceType = null;
		DoiResourceType resourceType = new DoiResourceType();
		try {
			xmlResourceType = dom.getElementsByTagName("resourceType");
			resourceType.setResourceTypeGeneral(DoiResourceTypeGeneral.valueOf(xmlResourceType.item(0).getAttributes().getNamedItem("resourceTypeGeneral").getNodeValue()));
		} catch (NullPointerException e) {
			// Don't need to do anything, leave the general type as null
		}

		try {
			resourceType.setResourceTypeText(xmlResourceType.item(0).getTextContent());
		} catch (NullPointerException e) {
			// Set the text to an empty string.
			resourceType.setResourceTypeText("");
		}

		doiMetadata.setResourceType(resourceType);
		return doiMetadata;
	}
}
