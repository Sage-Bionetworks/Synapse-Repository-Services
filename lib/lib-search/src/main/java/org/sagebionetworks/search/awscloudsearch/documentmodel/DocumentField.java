package org.sagebionetworks.search.awscloudsearch.documentmodel;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

public class DocumentField {
	@XmlAttribute(name="name")
	String name;

	@XmlValue
	String content;

	public DocumentField(){

	}

	public DocumentField(String name, String content){
		this.name = name;
		this.content = content;
	}

}
