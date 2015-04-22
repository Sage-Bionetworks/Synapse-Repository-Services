package org.sagebionetworks.repo.manager.trash;

public class ParentInTrashCanException extends RuntimeException {

	private static final long serialVersionUID = -4218320662080445999L;
	
	public ParentInTrashCanException(String message) {
		super(message);
	}
}
