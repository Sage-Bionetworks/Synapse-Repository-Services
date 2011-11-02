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
	private EntityType benefactorType = null;

	/**
	 * 
	 * @param message
	 * @param benefactorType
	 * @param benefactorId
	 */
	public ACLInheritanceException(String message, EntityType benefactorType, String benefactorId) {
		super(message);
		if(benefactorType == null) throw new IllegalArgumentException("Benefactor type cannot be null");
		if(benefactorId == null) throw new IllegalArgumentException("The benefactor ID cannot be null");
		this.benefactorType = benefactorType;
		this.benefactorId = benefactorId;
	}
	
	/**
	 * Created with a redirect URL.
	 * @param redirectedUrl
	 */
	public ACLInheritanceException(String message) {
		if(message == null) throw new IllegalArgumentException("Message cannot be null");
		// Parse the type and ID from the the string
		this.benefactorType = EntityType.getLastTypeInUrl(message);
		int start = message.indexOf(benefactorType.getUrlPrefix()) + benefactorType.getUrlPrefix().length()+1;
		int end = message.indexOf("/acl");
		if(end < start){
			new IllegalArgumentException("");
		}
		this.benefactorId = message.substring(start, end);
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

	public EntityType getBenefactorType() {
		return benefactorType;
	}

	public void setBenefactorType(EntityType benefactorType) {
		this.benefactorType = benefactorType;
	}

	
}
