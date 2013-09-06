package org.sagebionetworks.repo.manager.trash;

/**
 * When trying to read an entity in the trash can.
 */
public class EntityInTrashCanException extends RuntimeException {

	private static final long serialVersionUID = -3878964478329347878L;

	public EntityInTrashCanException(String message) {
		super(message);
	}
}
