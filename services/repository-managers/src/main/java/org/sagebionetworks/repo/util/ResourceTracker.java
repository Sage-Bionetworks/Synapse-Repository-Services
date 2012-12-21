package org.sagebionetworks.repo.util;


/**
 * This is a fast-fail resource tracker.  When the requested resources cannot be allocated,
 * it fails instantly rather than blocks until the resources become available. 
 * @author John
 *
 */
public class ResourceTracker {
	
	private volatile long maxResource;
	private volatile long allocated;
	
	/**
	 * Create a
	 * @param maxResource
	 * @param allocated
	 */
	public ResourceTracker(long maxResource) {
		super();
		this.maxResource = maxResource;
		this.allocated = 0;
	}

	/**
	 * Attempt to checkout a given amount of the resources.
	 * Important: This should not be called directly and is only exposed as protected for testing.
	 * @param bytesToCheckout
	 * @throws ServiceUnavailableException
	 */
	protected synchronized Long attemptCheckout(long amountToCheckout) throws ResourceTempoarryUnavailable, ExceedsMaximumResources {
		// does the request exceed the maximum?
		if(amountToCheckout > maxResource){
			// We will never be able to allocate this amount.
			throw new ExceedsMaximumResources(String.format("Cannot allocate %1$d resource because it exceed the maximum of %2$d", amountToCheckout, maxResource));
		}
		long remaining = maxResource - allocated;
		if(remaining < amountToCheckout){
			// Cannot allocate the given amount at this time.
			throw new ResourceTempoarryUnavailable();
		}
		// Allocate the amount
		allocated += amountToCheckout;
		return new Long(amountToCheckout);
	}
	
	/**
	 * Check-in the given resource amount. Only resources that have been checked out should be checked in.
	 * Important: This should not be called directly and is only exposed as protected for testing.
	 * @param allocatedAmount
	 */
	protected synchronized void checkin(long toCheckin){
		if(allocated < toCheckin) throw new IllegalArgumentException("Cannot check-in more resources than have been checked-out.");
		allocated -= toCheckin; 
	}
	
	/**
	 * Attempt to allocate and hold the requested resources amount during the run of the passed runnable.
	 * 
	 * Note: This method "thread-safe" and designed to be called by concurrent threads. This method does not block,
	 * if resources cannot be allocated.  Instead it is designed to fail-fast.
	 * 
	 * @throws ResourceTempoarryUnavailable - Thrown when there currently are not enough resources to available.  For this case try again later.
	 * @throws ExceedsMaximumResources - Thrown when the requested allocation exceeds the maximum. Even if 100% of the resources were available
	 * there would not be enough to allocate the requested amount. 
	 * 
	 *  Important: DO NOT MAKE THIS METHODS SYNCHRONIZED! Doing so would change it to blocking rather than fail-fast.
	 */
	public void allocateAndUseResources(Runnable consumer, long amountToCheckout) throws ResourceTempoarryUnavailable, ExceedsMaximumResources {
		if(consumer == null) throw new IllegalArgumentException("Consumer cannot be null");
		Long checkedOut = null;
		try{
			// Checkout the resources
			checkedOut = attemptCheckout(amountToCheckout);
			// Let the consumer run.
			consumer.run();
		} finally{
			// If the resources were checked-out then they must be checked back in.
			if(checkedOut != null){
				checkin(checkedOut);
			}
		}
	}
	
	
	/**
	 * The maximum amount of resources that can be allocated.
	 * @return
	 */
	public long getMaxResource() {
		return maxResource;
	}

	/**
	 * The current allocated amount.  This is provide for informational purposes only,
	 * and should not be used to make any business decisions.
	 * 
	 * @return
	 */
	public long getAllocated() {
		return allocated;
	}



	/**
	 * Thrown when the resources are temporarily unavailable.  
	 * @author John
	 *
	 */
	public static class ResourceTempoarryUnavailable extends RuntimeException {

		private static final long serialVersionUID = 3161191200224977763L;

		public ResourceTempoarryUnavailable() {
			super();
		}

		public ResourceTempoarryUnavailable(String message, Throwable cause) {
			super(message, cause);
		}

		public ResourceTempoarryUnavailable(String message) {
			super(message);
		}

		public ResourceTempoarryUnavailable(Throwable cause) {
			super(cause);
		}
		
	}
	
	/**
	 * Thrown when the resources are temporarily unavailable.  
	 * @author John
	 *
	 */
	public static class ExceedsMaximumResources extends RuntimeException {

		private static final long serialVersionUID = 3161100;

		public ExceedsMaximumResources() {
			super();
		}

		public ExceedsMaximumResources(String message, Throwable cause) {
			super(message, cause);
		}

		public ExceedsMaximumResources(String message) {
			super(message);
		}

		public ExceedsMaximumResources(Throwable cause) {
			super(cause);
		}
		
	}
}
