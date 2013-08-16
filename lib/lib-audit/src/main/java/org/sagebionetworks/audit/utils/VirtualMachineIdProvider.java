package org.sagebionetworks.audit.utils;

/**
 * A simple class that creates and provides a single VIMD that can be used to
 * identity a machine in the cluster.
 * 
 * @author jmhill
 * 
 */
public class VirtualMachineIdProvider {
	// Create a new ID once on class loading.
	private static String VMID = new java.rmi.dgc.VMID().toString();

	/**
	 * Get the VMID singleton.
	 * 
	 * @return
	 */
	public static String getVMID() {
		return VMID;
	}
}
