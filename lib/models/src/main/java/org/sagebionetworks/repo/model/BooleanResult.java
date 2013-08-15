package org.sagebionetworks.repo.model;

public class BooleanResult  {
	private boolean result;
	
	public BooleanResult() {}

	public BooleanResult(boolean result) {
		super();
		this.result = result;
	}

	/**
	 * @return the result
	 */
	public boolean getResult() {
		return result;
	}

	/**
	 * @param result the result to set
	 */
	public void setResult(boolean result) {
		this.result = result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.result ? 1231 : 1237);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof BooleanResult))
			return false;
		BooleanResult other = (BooleanResult) obj;
		if (result != other.result)
			return false;
		return true;
	}
	
	
}
