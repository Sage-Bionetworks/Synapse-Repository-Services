/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_PROPS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_ANNOS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_USER_PROFILE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_PROFILE;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * @author brucehoff
 *
 */
public class DBOUserProfile implements DatabaseObject<DBOUserProfile> {
	private Long id;
	private String userName;
	private byte[] properties;
	private byte[] annotations;
	private Long eTag = 0L;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_USER_PROFILE_ID, true),
		new FieldColumn("userName", COL_USER_PROFILE_NAME),
		new FieldColumn("properties", COL_USER_PROFILE_PROPS_BLOB),
		new FieldColumn("annotations", COL_USER_PROFILE_ANNOS_BLOB),
		new FieldColumn("eTag", COL_USER_PROFILE_ETAG)
		};


	@Override
	public TableMapping<DBOUserProfile> getTableMapping() {
		return new TableMapping<DBOUserProfile>() {
			// Map a result set to this object
			@Override
			public DBOUserProfile mapRow(ResultSet rs, int rowNum)	throws SQLException {
				DBOUserProfile up = new DBOUserProfile();
				up.setId(rs.getLong(COL_USER_PROFILE_ID));
				up.setUserName(rs.getString(COL_USER_PROFILE_NAME));
				java.sql.Blob blob;
				blob = rs.getBlob(COL_USER_PROFILE_PROPS_BLOB);
				if(blob != null){
					up.setProperties(blob.getBytes(1, (int) blob.length()));
				}
				blob = rs.getBlob(COL_USER_PROFILE_ANNOS_BLOB);
				if(blob != null){
					up.setAnnotations(blob.getBytes(1, (int) blob.length()));
				}
				up.seteTag(rs.getLong(COL_USER_PROFILE_ETAG));
				return up;
			}

			@Override
			public String getTableName() {
				return TABLE_USER_PROFILE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_USER_PROFILE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOUserProfile> getDBOClass() {
				return DBOUserProfile.class;
			}
		};
	}


	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}


	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}


	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}


	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}


	/**
	 * @return the properties
	 */
	public byte[] getProperties() {
		return properties;
	}


	/**
	 * @param properties the properties to set
	 */
	public void setProperties(byte[] properties) {
		this.properties = properties;
	}


	/**
	 * @return the annotations
	 */
	public byte[] getAnnotations() {
		return annotations;
	}


	/**
	 * @param annotations the annotations to set
	 */
	public void setAnnotations(byte[] annotations) {
		this.annotations = annotations;
	}


	/**
	 * @return the eTag
	 */
	public Long geteTag() {
		return eTag;
	}


	/**
	 * @param eTag the eTag to set
	 */
	public void seteTag(Long eTag) {
		this.eTag = eTag;
	}
	

}
