package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_AUTHORIZATION_CODE_CODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_AUTHORIZATION_CODE_REQUEST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_OAUTH_AUTHORIZATION_CODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OAUTH_AUTHORIZATION_CODE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class DBOAuthorizationCode implements DatabaseObject<DBOAuthorizationCode> {
	private String authCode;
	private byte[] authorizationRequest;
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("authCode", COL_OAUTH_AUTHORIZATION_CODE_CODE, true),
			new FieldColumn("authorizationRequest", COL_OAUTH_AUTHORIZATION_CODE_REQUEST)
	};

	@Override
	public TableMapping<DBOAuthorizationCode> getTableMapping() {
		return new TableMapping<DBOAuthorizationCode>(){
			@Override
			public DBOAuthorizationCode mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOAuthorizationCode dbo = new DBOAuthorizationCode();
				dbo.setAuthCode(rs.getString(COL_OAUTH_AUTHORIZATION_CODE_CODE));
				dbo.setAuthorizationRequest(rs.getBytes(COL_OAUTH_AUTHORIZATION_CODE_REQUEST));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_OAUTH_AUTHORIZATION_CODE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_OAUTH_AUTHORIZATION_CODE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOAuthorizationCode> getDBOClass() {
				return DBOAuthorizationCode.class;
			}
		};
	}
	
	public String getAuthCode() {
		return authCode;
	}
	public void setAuthCode(String authCode) {
		this.authCode = authCode;
	}
	public byte[] getAuthorizationRequest() {
		return authorizationRequest;
	}
	public void setAuthorizationRequest(byte[] authorizationRequest) {
		this.authorizationRequest = authorizationRequest;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((authCode == null) ? 0 : authCode.hashCode());
		result = prime * result + Arrays.hashCode(authorizationRequest);
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
		DBOAuthorizationCode other = (DBOAuthorizationCode) obj;
		if (authCode == null) {
			if (other.authCode != null)
				return false;
		} else if (!authCode.equals(other.authCode))
			return false;
		if (!Arrays.equals(authorizationRequest, other.authorizationRequest))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DBOAuthorizationCode [authCode=" + authCode + ", authorizationRequest="
				+ Arrays.toString(authorizationRequest) + "]";
	}


}
