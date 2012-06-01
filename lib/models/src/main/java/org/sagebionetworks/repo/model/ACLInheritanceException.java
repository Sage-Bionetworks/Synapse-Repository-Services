package org.sagebionetworks.repo.model;

/**
 * Thrown when attempting to access an ACL from a resources that inherits is it permissions from a benefactor.
 * When thrown the ID of the benefactor will be provided.
 *
 */
public class ACLInheritanceException extends Exception {
	
	public static final String DEFAULT_MSG_PREFIX = "The requested ACL does not exist. This entity inherits its permissions from: ";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String benefactorId = null;
//	private EntityType benefactorType = null;

	/**
	 * 
	 * @param message
	 * @param benefactorType
	 * @param benefactorId
	 */
	public ACLInheritanceException(String message, String benefactorId) {
		super(message);
		if(benefactorId == null) throw new IllegalArgumentException("The benefactor ID cannot be null");
		this.benefactorId = benefactorId;
	}
	
	/**
	 * Created with a redirect URL.
	 * @param redirectedUrl
	 */
	public ACLInheritanceException(String message) {
		if(message == null) throw new IllegalArgumentException("Message cannot be null");
		// Parse the type and ID from the the string
		this.benefactorId = message.split("/entity/")[1].split("/acl")[0];
	}

	/**
	 * When thrown this is the ID of the benefactor.
	 * @return
	 */
	public String getBenefactorId() {
		return benefactorId;
	}

	/**
	 * When thrown this is the ID of the benefactor.
	 * @param benefactorId
	 */
	public void setBenefactorId(String benefactorId) {
		this.benefactorId = benefactorId;
	}
	
}
