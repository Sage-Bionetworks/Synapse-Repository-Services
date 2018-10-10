package org.sagebionetworks.search.awscloudsearch.documentmodel;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "delete")
public class DeleteDocument implements Document{
	String id;

	@Override
	@XmlAttribute(name = "id")
	public String getId() {
		return this.id;
	}
}
