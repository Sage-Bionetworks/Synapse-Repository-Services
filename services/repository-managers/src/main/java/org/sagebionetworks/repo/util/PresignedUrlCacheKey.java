package org.sagebionetworks.repo.util;

/**
 * Note that the cache key must be userId+s3Key+method because its not okay to give out
 * urls signed for one user to another user
 * 
 * @author deflaux
 * 
 */
public class PresignedUrlCacheKey {
	private final Long userId;
	private final String s3Key;
	private final String method;

	/**
	 * @param userId
	 * @param s3Key
	 * @param method
	 */
	public PresignedUrlCacheKey(Long userId, String s3Key, String method) {
		super();
		this.userId = userId;
		this.s3Key = s3Key;
		this.method = method;
	}

	/**
	 * @return the userId
	 */
	public Long getUserId() {
		return userId;
	}

	/**
	 * @return the s3Key
	 */
	public String getS3Key() {
		return s3Key;
	}

	/**
	 * @return the method
	 */
	public String getMethod() {
		return method;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((s3Key == null) ? 0 : s3Key.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
		PresignedUrlCacheKey other = (PresignedUrlCacheKey) obj;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (s3Key == null) {
			if (other.s3Key != null)
				return false;
		} else if (!s3Key.equals(other.s3Key))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PresignedUrlCacheKey [userId=" + userId + ", s3Key=" + s3Key
				+ ", method=" + method + "]";
	}

}
