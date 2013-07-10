package org.sagebionetworks.javadoc.writer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Abstraction for writing sub-xml data to a larger file.
 * 
 * @author jmhill
 *
 */
public interface SubWriter {
	
	/**
	 * When called write any XML needed to the passed writer.
	 * 
	 * @param writer
	 * @throws XMLStreamException
	 */
	public void write(XMLStreamWriter writer) throws XMLStreamException;
}
