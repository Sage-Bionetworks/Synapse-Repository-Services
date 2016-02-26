package org.sagebionetworks.table.cluster;

/**
 * The current state of a table's index.
 *
 */
public class IndexState {

	long currentVersionNumber;
	String schemaMD5Hex;
	
	/**
	 * 
	 * @param currentVersionNumber The current version applied to the index.
	 * @param schemaMD5Hex The MD5 Hex of the current schema.
	 */
	public IndexState(long currentVersionNumber, String schemaMD5Hex) {
		super();
		this.currentVersionNumber = currentVersionNumber;
		this.schemaMD5Hex = schemaMD5Hex;
	}

	/**
	 * The current version applied to the index.
	 * @return
	 */
	public long getCurrentVersionNumber() {
		return currentVersionNumber;
	}

	/**
	 * The MD5 Hex of the current schema.
	 * 
	 * @return
	 */
	public String getSchemaMD5Hex() {
		return schemaMD5Hex;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (int) (currentVersionNumber ^ (currentVersionNumber >>> 32));
		result = prime * result
				+ ((schemaMD5Hex == null) ? 0 : schemaMD5Hex.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IndexState other = (IndexState) obj;
		if (currentVersionNumber != other.currentVersionNumber)
			return false;
		if (schemaMD5Hex == null) {
			if (other.schemaMD5Hex != null)
				return false;
		} else if (!schemaMD5Hex.equals(other.schemaMD5Hex))
			return false;
		return true;
	}

}
