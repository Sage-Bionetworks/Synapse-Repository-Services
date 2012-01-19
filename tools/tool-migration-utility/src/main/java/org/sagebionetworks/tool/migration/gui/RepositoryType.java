package org.sagebionetworks.tool.migration.gui;


/**
 * The type of a repository.
 * @author John
 *
 */
public enum RepositoryType {
	SOURCE("Source Repository Information"),
	DESTINATION("Destination Repository Information");
	
	private String title;
	RepositoryType(String title){
		this.title = title;
	}
	
	public String getTitle(){
		return title;
	}
}
