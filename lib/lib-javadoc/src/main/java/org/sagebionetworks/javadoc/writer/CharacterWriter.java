package org.sagebionetworks.javadoc.writer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Simply writes the given characaters to the writer.
 * 
 * @author jmhill
 *
 */
public class CharacterWriter implements SubWriter {

	String value;
	
	public CharacterWriter(String value){
		this.value = value;
	}

	@Override
	public void write(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeCharacters(value);
	}
}
