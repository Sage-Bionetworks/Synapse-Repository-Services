package org.sagebionetworks.repo.model.query.jdo.modelv2;


public class AnnotationColumn implements Column {

	String name;
	
	public AnnotationColumn(String inSelect) {
		this.name = inSelect;
	}
}
