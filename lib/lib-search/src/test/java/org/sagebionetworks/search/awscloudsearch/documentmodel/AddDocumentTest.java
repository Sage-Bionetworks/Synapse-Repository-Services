package org.sagebionetworks.search.awscloudsearch.documentmodel;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.json.XML;
import org.junit.Before;
import org.junit.Test;
import static org.sagebionetworks.search.awscloudsearch.SynapseToCloudSearchField.*;

public class AddDocumentTest {
	Marshaller xmlMarlshaller;
	AddDocument document;

	@Before
	public void setUp() throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(AddDocument.class);
		xmlMarlshaller = jaxbContext.createMarshaller();

		xmlMarlshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		document = new AddDocument();
	}

	@Test
	public void testXML() throws JAXBException {
		document.withId("syn123");
		document.withFieldValue(ACL, "testing testing 123 &&&& >> <<,");
		document.withFieldValue(ACL, "othervalue");
		document.withFieldValue(DESCRIPTION, "testing asdfasdfsdaf >> <<,");
		xmlMarlshaller.marshal(document, System.out);;
	}

}
