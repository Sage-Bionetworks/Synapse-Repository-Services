package org.sagebionetworks.search.awscloudsearch.documentmodel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.sagebionetworks.search.awscloudsearch.SynapseToCloudSearchField;

@XmlRootElement(name="add")
public class AddDocument implements Document{
	String id;
	List<DocumentField> documentFields = new ArrayList();

	@XmlElement(name = "field")
	public Collection<DocumentField> getFields(){
		return documentFields;
	}

	@Override
	@XmlAttribute
	public String getId() {
		return this.id;
	}

	public AddDocument withId(String id){
		this.id = id;
		return this;
	}

	public AddDocument withFieldValue(SynapseToCloudSearchField field, String value){
		documentFields.add(new DocumentField(field.getCloudSearchFieldName(), value));
		return this;
	}
}
