package org.sagebionetworks.table.cluster;

/**
 * Information about a single database connection.
 * 
 * @author jhill
 *
 */
public class InstanceInfo {
	
	public static final String DATABASE_URL_NAME_TEMPALTE = "jdbc:mysql://%1$s/%2$s?rewriteBatchedStatements=true";
	
	private String endpoint;
	private String schema;
	private String url;
	
	/**
	 * New from endpoint and schema.
	 * @param endpoint
	 * @param schema
	 */
	public InstanceInfo(String endpoint, String schema) {
		super();
		this.endpoint = endpoint;
		this.schema = schema;
		this.url = String.format(DATABASE_URL_NAME_TEMPALTE, endpoint, schema);
	}
	
	/**
	 * The database endpoint.
	 * @return
	 */
	public String getEndpoint() {
		return endpoint;
	}
	/**
	 * The database schema.
	 * @return
	 */
	public String getSchema() {
		return schema;
	}
	
	/**
	 * The full JDBC connection url for the database.
	 * @return
	 */
	public String getUrl() {
		return url;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((url == null) ? 0 : url.hashCode());
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
		InstanceInfo other = (InstanceInfo) obj;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "InstanceInfo [endpoint=" + endpoint + ", schema=" + schema
				+ ", url=" + url + "]";
	}
	
}
