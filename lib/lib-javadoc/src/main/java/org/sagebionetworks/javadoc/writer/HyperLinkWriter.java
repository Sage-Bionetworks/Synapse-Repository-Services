package org.sagebionetworks.javadoc.writer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * A simple Hyper Link writer
 * 
 * @author jmhill
 *
 */
public class HyperLinkWriter implements SubWriter {
	
	String href;
	String text;

	/**
	 * <a href="href">text</a> 
	 * @param href
	 * @param text
	 */
	public HyperLinkWriter(String href, String text) {
		super();
		this.href = href;
		this.text = text;
	}

	@Override
	public void write(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement("a");
		writer.writeAttribute("href", href);
		writer.writeCharacters(text);
		writer.writeEndElement();
	}

}
