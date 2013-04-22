package org.sagebionetworks.javadoc;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Utilities for writing HTML
 * @author John
 *
 */
public class HTMLUtils {
	
	/**
	 * Write an HTML table
	 * @param writer
	 * @param header - the table headers
	 * @param table - the table data.
	 * @throws XMLStreamException 
	 */
	public static void writeTable(XMLStreamWriter writer, String[] header, List<String[]> data) throws XMLStreamException{
		writer.writeStartElement("table");
		writer.writeAttribute("border", "1");
		// Write the headers
		writer.writeStartElement("tr");
		for(int i=0; i<header.length; i++){
			writeElement(writer, "th", header[i]);
		}
		writer.writeEndElement(); // tr
		// Now write the rest of the table
		for(String[] row: data){
			writer.writeStartElement("tr");
			// Write the columns for this row
			for(int col=0;col<row.length; col++){
				writeElement(writer, "td", row[col]);
			}
			writer.writeEndElement(); // tr
		}
		
		writer.writeEndElement(); // table
	}

	/**
	 * Write a single element with a body.
	 * @param writer
	 * @param header
	 * @throws XMLStreamException 
	 */
	public static void writeElement(XMLStreamWriter writer, String element, String body) throws XMLStreamException{
		writer.writeStartElement(element);
		writer.writeCharacters(body);
		writer.writeEndElement();
	}
	
	/**
	 * Create the final HTML from the template.
	 * @param pathToRoot
	 * @param body
	 * @throws IOException 
	 */
	public static String createHTMFromTempalte(String pathToRoot, String body) throws IOException{
		// Load the template
		String template = CopyBaseFiles.loadHTMLTemplateAsString();
		// Set the path
		template = template.replaceAll("path_to_root", pathToRoot);
		// set the body
		return template.replaceAll("main_page_body", body);
	}
}
