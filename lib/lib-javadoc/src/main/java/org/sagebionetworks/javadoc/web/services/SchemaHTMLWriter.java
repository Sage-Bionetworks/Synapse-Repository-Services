package org.sagebionetworks.javadoc.web.services;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.FileUtils;
import org.sagebionetworks.javadoc.BasicFileUtils;
import org.sagebionetworks.javadoc.HTMLUtils;
import org.sagebionetworks.javadoc.writer.CharacterWriter;
import org.sagebionetworks.javadoc.writer.HyperLinkWriter;
import org.sagebionetworks.javadoc.writer.SubWriter;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Write a JSON schema to HTML
 * 
 * @author John
 * 
 */
public class SchemaHTMLWriter {

	/**
	 * Write the JSON schema to an HTML file.
	 * 
	 * @param outputFile
	 *            - the destination file
	 * @param jsonSchema
	 *            - the raw schema in JSON
	 * @throws IOException
	 * @throws XMLStreamException 
	 * @throws JSONObjectAdapterException 
	 */
	public static void write(File outputFile, ObjectSchema schema, String name)
			throws IOException, XMLStreamException, JSONObjectAdapterException {
		if (outputFile == null)
			throw new IllegalArgumentException("The outputFile cannot be null");
		if (schema == null)
			throw new IllegalArgumentException("Schema cannot be null");
		// Get the body HTML
		String body = writeToHTML(schema);
		String pathToRoot = BasicFileUtils.pathToRoot(name);
		String pageHTML = HTMLUtils.createHTMFromTempalte(pathToRoot, body);
		// Write the file
		FileUtils.writeStringToFile(outputFile, pageHTML);
	}



	/**
	 * Write the schema to HTML
	 * @param schema
	 * @return
	 * @throws XMLStreamException
	 */
	public static String writeToHTML(ObjectSchema schema)	throws XMLStreamException {
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		StringWriter stringWriter = new StringWriter();
		XMLStreamWriter writer = factory.createXMLStreamWriter(stringWriter);
		writer.writeStartDocument();

		// Write the ID as a header
		writeElement(writer, "h2", schema.getName());
		writeElement(writer, "h5", schema.getId());

		// Write the properties to a table
		if(schema.getEnum() != null){
			writeEnumToTable(writer, schema.getEnum());
		}else{
			writePropertiesToTable(writer, schema.getProperties());
		}
		writer.writeEndDocument();// end
		// done
		writer.flush();
		writer.close();
		String bodyHTML = stringWriter.toString();
		System.out.println(bodyHTML);
		return bodyHTML;
	}
	
	/**
	 * Write enumerations to the table.
	 * @param writer
	 * @param enums
	 * @throws XMLStreamException
	 */
	private static void writeEnumToTable(XMLStreamWriter writer, String[] enums) throws XMLStreamException {
		String[] headers = new String[]{"Enumeration"};
		List<SubWriter[]> data = new LinkedList<SubWriter[]>();
		for(String enumName: enums){
			SubWriter[] row = new SubWriter[]{
					new CharacterWriter(enumName),
			};
			data.add(row);
		}
		// Write this as a table
		HTMLUtils.writeTable(writer, headers, data);
	}



	/**
	 * Write a header to the stream
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
	 * Write the properties to a table
	 * @param writer
	 * @param properties
	 * @throws XMLStreamException
	 */
	public static void writePropertiesToTable(XMLStreamWriter writer, Map<String, ObjectSchema> properties) throws XMLStreamException{
		String[] headers = new String[]{"name","description","type"};
		List<SubWriter[]> data = new LinkedList<SubWriter[]>();
		// For each 
		Iterator<Entry<String, ObjectSchema>> it = properties.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, ObjectSchema> entry = it.next();
			ObjectSchema prop = entry.getValue();
			String description = prop.getDescription();
			if(description == null){
				description = "";
			}
			SubWriter typeWriter;
			if(prop.getId() != null){
				final String id = prop.getId();
				typeWriter = new HyperLinkWriter("${"+id+"}", id);
			}else{
				typeWriter =new CharacterWriter(prop.getType().name());
			}			
			SubWriter[] row = new SubWriter[]{
					new CharacterWriter(entry.getKey()),
					new CharacterWriter(description),
					typeWriter,
			};
			data.add(row);
		}
		// Write this as a table
		HTMLUtils.writeTable(writer, headers, data);
	}

}
