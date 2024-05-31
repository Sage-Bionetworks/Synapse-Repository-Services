package org.sagebionetworks.simpleHttpClient;

public class SimpleHttpClientConfig {

	/**
	 * A timeout value of zero is interpreted as an infinite timeout.
     * A negative value is interpreted as undefined (system default).
	 */
	public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS = -1;
	public static final int DEFAULT_CONNECT_TIMEOUT_MS = -1;
	public static final int DEFAULT_SOCKET_TIMEOUT_MS = -1;

	/**
	 * Returns the timeout in milliseconds used when requesting a connection
     * from the connection manager. A timeout value of zero is interpreted
     * as an infinite timeout.
	 */
	private int connectionRequestTimeoutMs;
	/**
	 * Determines the timeout in milliseconds until a connection is established.
     * A timeout value of zero is interpreted as an infinite timeout.
	 */
	private int connectTimeoutMs;
	/**
	 * Defines the socket timeout ({@code SO_TIMEOUT}) in milliseconds,
     * which is the timeout for waiting for data  or, put differently,
     * a maximum period inactivity between two consecutive data packets).
	 */
	private int socketTimeoutMs;

	public SimpleHttpClientConfig() {
		connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
		socketTimeoutMs = DEFAULT_SOCKET_TIMEOUT_MS;
		connectionRequestTimeoutMs = DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS;
	}

	public int getConnectTimeoutMs() {
		return connectTimeoutMs;
	}

	public void setConnectTimeoutMs(int connectionTimeoutMs) {
		this.connectTimeoutMs = connectionTimeoutMs;
	}

	public int getSocketTimeoutMs() {
		return socketTimeoutMs;
	}

	public void setSocketTimeoutMs(int socketTimeoutMs) {
		this.socketTimeoutMs = socketTimeoutMs;
	}

	public int getConnectionRequestTimeoutMs() {
		return connectionRequestTimeoutMs;
	}

	public void setConnectionRequestTimeoutMs(int connectionRequestTimeoutMs) {
		this.connectionRequestTimeoutMs = connectionRequestTimeoutMs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + connectionRequestTimeoutMs;
		result = prime * result + connectTimeoutMs;
		result = prime * result + socketTimeoutMs;
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
		SimpleHttpClientConfig other = (SimpleHttpClientConfig) obj;
		if (connectionRequestTimeoutMs != other.connectionRequestTimeoutMs)
			return false;
		if (connectTimeoutMs != other.connectTimeoutMs)
			return false;
		if (socketTimeoutMs != other.socketTimeoutMs)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SimpleHttpClientConfig [connectionRequestTimeoutMs=" + connectionRequestTimeoutMs
				+ ", connectionTimeoutMs=" + connectTimeoutMs + ", socketTimeoutMs=" + socketTimeoutMs + "]";
	}
}
