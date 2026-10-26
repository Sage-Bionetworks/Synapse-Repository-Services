package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_ATTACHMENTS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_SCHEMA_DATA_ATTACHMENTS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_DUC_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_EDUC_CONTENT_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_EDUC_ENVELOPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_IRB_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_REQUEST_JSON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_RESEARCH_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DATA_ACCESS_REQUEST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_REQUEST;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.search.OpaqueJsonColumnCodecUtil;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBORequest implements MigratableDatabaseObject<DBORequest, DBORequest>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_DATA_ACCESS_REQUEST_ID, true).withIsBackupId(true),
			new FieldColumn("accessRequirementId", COL_DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_ID),
			new FieldColumn("researchProjectId", COL_DATA_ACCESS_REQUEST_RESEARCH_PROJECT_ID),
			new FieldColumn("createdBy", COL_DATA_ACCESS_REQUEST_CREATED_BY),
			new FieldColumn("createdOn", COL_DATA_ACCESS_REQUEST_CREATED_ON),
			new FieldColumn("modifiedBy", COL_DATA_ACCESS_REQUEST_MODIFIED_BY),
			new FieldColumn("modifiedOn", COL_DATA_ACCESS_REQUEST_MODIFIED_ON),
			new FieldColumn("etag", COL_DATA_ACCESS_REQUEST_ETAG).withIsEtag(true),
			new FieldColumn("ducFileHandleId", COL_DATA_ACCESS_REQUEST_DUC_FILE_HANDLE_ID).withHasFileHandleRef(true),
			new FieldColumn("irbFileHandleId", COL_DATA_ACCESS_REQUEST_IRB_FILE_HANDLE_ID).withHasFileHandleRef(true),
			new FieldColumn("attachmentsJson", COL_DATA_ACCESS_REQUEST_ATTACHMENTS).withHasFileHandleRef(true).withIsFileHandleIdJsonList(true),
			new FieldColumn("schemaDataAttachmentsJson", COL_DATA_ACCESS_REQUEST_SCHEMA_DATA_ATTACHMENTS).withHasFileHandleRef(true).withIsFileHandleIdJsonList(true),
			new FieldColumn("requestJson", COL_DATA_ACCESS_REQUEST_REQUEST_JSON),
			new FieldColumn("eDucEnvelopeId", COL_DATA_ACCESS_REQUEST_EDUC_ENVELOPE_ID),
			new FieldColumn("eDucContentHash", COL_DATA_ACCESS_REQUEST_EDUC_CONTENT_HASH)
		};

	/**
	 * Bridge: decodes the request content that a previous stack stored as a XStream blob, only used by {@link #getTranslator()}.
	 * Removed in the next stack, see PLFM-10061.
	 */
	private static final UnmodifiableXStream REQUEST_XSTREAM = UnmodifiableXStream.builder()
			.allowTypeHierarchy(RequestInterface.class)
			.allowTypesByWildcard(new String[] {"org.sagebionetworks.repo.model.**"})
			.build();

	/**
	 * Bridge: the description of the free-form schemaData that the XStream blob stored as JSON text. Removed in the next stack,
	 * see PLFM-10061.
	 */
	private static final String SCHEMA_DATA = "Request.schemaData";

	private Long id;
	private Long accessRequirementId;
	private Long researchProjectId;
	private Long createdBy;
	private Long createdOn;
	private Long modifiedBy;
	private Long modifiedOn;
	private String etag;
	/**
	 * Bridge field: it has no column and is only populated when reading a backup written by a stack that still stored the
	 * request content as a XStream blob. {@link #fromLegacyBackup(DBORequest)} converts it into the columns below. It is
	 * removed in the next stack, see PLFM-10061.
	 */
	private byte[] requestSerialized;
	private Long ducFileHandleId;
	private Long irbFileHandleId;
	private String attachmentsJson;
	private String schemaDataAttachmentsJson;
	private String requestJson;
	private String eDucEnvelopeId;
	private String eDucContentHash;

	@Override
	public String toString() {
		return "DBORequest [id=" + id + ", accessRequirementId=" + accessRequirementId
				+ ", researchProjectId=" + researchProjectId + ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn + ", etag=" + etag
				+ ", requestSerialized=" + Arrays.toString(requestSerialized)
				+ ", ducFileHandleId=" + ducFileHandleId
				+ ", irbFileHandleId=" + irbFileHandleId
				+ ", attachmentsJson=" + attachmentsJson
				+ ", schemaDataAttachmentsJson=" + schemaDataAttachmentsJson
				+ ", requestJson=" + requestJson
				+ ", eDucEnvelopeId=" + eDucEnvelopeId + ", eDucContentHash=" + eDucContentHash + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = Arrays.hashCode(requestSerialized);
		result = prime * result + Objects.hash(id, accessRequirementId, researchProjectId, createdBy, createdOn, modifiedBy, modifiedOn, etag, ducFileHandleId, irbFileHandleId, attachmentsJson, schemaDataAttachmentsJson, requestJson, eDucEnvelopeId, eDucContentHash);
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
		DBORequest other = (DBORequest) obj;
		return Arrays.equals(requestSerialized, other.requestSerialized)
				&& Objects.equals(id, other.id)
				&& Objects.equals(accessRequirementId, other.accessRequirementId)
				&& Objects.equals(researchProjectId, other.researchProjectId)
				&& Objects.equals(createdBy, other.createdBy)
				&& Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(modifiedBy, other.modifiedBy)
				&& Objects.equals(modifiedOn, other.modifiedOn)
				&& Objects.equals(etag, other.etag)
				&& Objects.equals(ducFileHandleId, other.ducFileHandleId)
				&& Objects.equals(irbFileHandleId, other.irbFileHandleId)
				&& Objects.equals(attachmentsJson, other.attachmentsJson)
				&& Objects.equals(schemaDataAttachmentsJson, other.schemaDataAttachmentsJson)
				&& Objects.equals(requestJson, other.requestJson)
				&& Objects.equals(eDucEnvelopeId, other.eDucEnvelopeId)
				&& Objects.equals(eDucContentHash, other.eDucContentHash);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getAccessRequirementId() {
		return accessRequirementId;
	}

	public void setAccessRequirementId(Long accessRequirementId) {
		this.accessRequirementId = accessRequirementId;
	}

	public Long getResearchProjectId() {
		return researchProjectId;
	}

	public void setResearchProjectId(Long researchProjectId) {
		this.researchProjectId = researchProjectId;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Long getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public byte[] getRequestSerialized() {
		return requestSerialized;
	}

	public void setRequestSerialized(byte[] requestSerialized) {
		this.requestSerialized = requestSerialized;
	}

	public Long getDucFileHandleId() {
		return ducFileHandleId;
	}

	public void setDucFileHandleId(Long ducFileHandleId) {
		this.ducFileHandleId = ducFileHandleId;
	}

	public Long getIrbFileHandleId() {
		return irbFileHandleId;
	}

	public void setIrbFileHandleId(Long irbFileHandleId) {
		this.irbFileHandleId = irbFileHandleId;
	}

	public String getAttachmentsJson() {
		return attachmentsJson;
	}

	public void setAttachmentsJson(String attachmentsJson) {
		this.attachmentsJson = attachmentsJson;
	}

	public String getSchemaDataAttachmentsJson() {
		return schemaDataAttachmentsJson;
	}

	public void setSchemaDataAttachmentsJson(String schemaDataAttachmentsJson) {
		this.schemaDataAttachmentsJson = schemaDataAttachmentsJson;
	}

	public String getRequestJson() {
		return requestJson;
	}

	public void setRequestJson(String requestJson) {
		this.requestJson = requestJson;
	}

	public String getEDucEnvelopeId() {
		return eDucEnvelopeId;
	}

	public void setEDucEnvelopeId(String eDucEnvelopeId) {
		this.eDucEnvelopeId = eDucEnvelopeId;
	}

	public String getEDucContentHash() {
		return eDucContentHash;
	}

	public void setEDucContentHash(String eDucContentHash) {
		this.eDucContentHash = eDucContentHash;
	}

	@Override
	public TableMapping<DBORequest> getTableMapping() {
		return new TableMapping<DBORequest>(){

			@Override
			public DBORequest mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBORequest dbo = new DBORequest();
				dbo.setId(rs.getLong(COL_DATA_ACCESS_REQUEST_ID));
				dbo.setAccessRequirementId(rs.getLong(COL_DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_ID));
				dbo.setResearchProjectId(getNullableLong(rs, COL_DATA_ACCESS_REQUEST_RESEARCH_PROJECT_ID));
				dbo.setCreatedBy(rs.getLong(COL_DATA_ACCESS_REQUEST_CREATED_BY));
				dbo.setCreatedOn(rs.getLong(COL_DATA_ACCESS_REQUEST_CREATED_ON));
				dbo.setModifiedBy(rs.getLong(COL_DATA_ACCESS_REQUEST_MODIFIED_BY));
				dbo.setModifiedOn(rs.getLong(COL_DATA_ACCESS_REQUEST_MODIFIED_ON));
				dbo.setEtag(rs.getString(COL_DATA_ACCESS_REQUEST_ETAG));
				dbo.setDucFileHandleId(getNullableLong(rs, COL_DATA_ACCESS_REQUEST_DUC_FILE_HANDLE_ID));
				dbo.setIrbFileHandleId(getNullableLong(rs, COL_DATA_ACCESS_REQUEST_IRB_FILE_HANDLE_ID));
				dbo.setAttachmentsJson(rs.getString(COL_DATA_ACCESS_REQUEST_ATTACHMENTS));
				dbo.setSchemaDataAttachmentsJson(rs.getString(COL_DATA_ACCESS_REQUEST_SCHEMA_DATA_ATTACHMENTS));
				dbo.setRequestJson(rs.getString(COL_DATA_ACCESS_REQUEST_REQUEST_JSON));
				dbo.setEDucEnvelopeId(rs.getString(COL_DATA_ACCESS_REQUEST_EDUC_ENVELOPE_ID));
				dbo.setEDucContentHash(rs.getString(COL_DATA_ACCESS_REQUEST_EDUC_CONTENT_HASH));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_DATA_ACCESS_REQUEST;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DATA_ACCESS_REQUEST;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBORequest> getDBOClass() {
				return DBORequest.class;
			}
			
		};
	}

	private static Long getNullableLong(ResultSet rs, String columnName) throws SQLException {
		long value = rs.getLong(columnName);
		return rs.wasNull() ? null : value;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DATA_ACCESS_REQUEST;
	}

	/**
	 * The translator converts a backup written by a stack that still stored the request content in the
	 * {@link #getRequestSerialized() XStream blob} into the columns of the table, see {@link #fromLegacyBackup(DBORequest)}.
	 */
	@Override
	public MigratableTableTranslation<DBORequest, DBORequest> getTranslator() {
		return new MigratableTableTranslation<DBORequest, DBORequest>() {
			@Override
			public DBORequest createDatabaseObjectFromBackup(DBORequest backup) {
				return backup.getRequestSerialized() == null ? backup : fromLegacyBackup(backup);
			}

			@Override
			public DBORequest createBackupFromDatabaseObject(DBORequest dbo) {
				return dbo;
			}
		};
	}

	/**
	 * Bridge: converts a backup that stores the request content in the XStream blob into a new row that stores it in the
	 * columns and the document. Removed in the next stack with the blob, see PLFM-10061.
	 * 
	 * @throws IllegalArgumentException If the backup {@link #hasNewColumnsAndLegacyBlob(DBORequest) has both} the blob and the
	 *                                  columns written from it
	 */
	static DBORequest fromLegacyBackup(DBORequest backup) {
		if (hasNewColumnsAndLegacyBlob(backup)) {
			throw new IllegalArgumentException("Both 'requestSerialized' and the request document or file handle columns are not null");
		}
		RequestInterface blob = readLegacyBlob(backup.getRequestSerialized());
		return deduplicateInFavorOfBackupColumns(backup, blob);
	}

	/**
	 * Only a stack that already wrote the document and file handle columns from the blob can produce a backup with both, so
	 * such a backup is a double migration and is rejected.
	 */
	private static boolean hasNewColumnsAndLegacyBlob(DBORequest backup) {
		return backup.getRequestSerialized() != null && (backup.getRequestJson() != null || backup.getDucFileHandleId() != null
				|| backup.getIrbFileHandleId() != null || backup.getAttachmentsJson() != null
				|| backup.getSchemaDataAttachmentsJson() != null);
	}

	/**
	 * The blob repeats the values of the columns that sat beside it. Each value is stored once: the backup columns win, and
	 * the blob only supplies the fields that had no column, plus the envelope id when its column is not set.
	 */
	private static DBORequest deduplicateInFavorOfBackupColumns(DBORequest backup, RequestInterface blob) {
		DBORequest dbo = new DBORequest();
		dbo.setId(backup.getId());
		dbo.setAccessRequirementId(backup.getAccessRequirementId());
		dbo.setResearchProjectId(backup.getResearchProjectId());
		dbo.setCreatedBy(backup.getCreatedBy());
		dbo.setCreatedOn(backup.getCreatedOn());
		dbo.setModifiedBy(backup.getModifiedBy());
		dbo.setModifiedOn(backup.getModifiedOn());
		dbo.setEtag(backup.getEtag());
		dbo.setEDucEnvelopeId(backup.getEDucEnvelopeId() != null ? backup.getEDucEnvelopeId() : blob.getEDucSignatureEnvelopeId());
		dbo.setEDucContentHash(backup.getEDucContentHash());
		dbo.setDucFileHandleId(RequestUtils.toLong(blob.getDucFileHandleId()));
		dbo.setIrbFileHandleId(RequestUtils.toLong(blob.getIrbFileHandleId()));
		dbo.setAttachmentsJson(JDOSecondaryPropertyUtils.writeStringListToJson(blob.getAttachments()));
		dbo.setRequestJson(JDOSecondaryPropertyUtils.createJSONFromObject(RequestUtils.toContent(blob)));
		return dbo;
	}

	/**
	 * Bridge: decodes the XStream blob. Removed in the next stack, see PLFM-10061.
	 */
	private static RequestInterface readLegacyBlob(byte[] serialized) {
		try {
			RequestInterface dto = (RequestInterface) JDOSecondaryPropertyUtils.decompressObject(REQUEST_XSTREAM, serialized);
			// The blob stored the free-form schemaData as JSON text
			if (dto.getSchemaData() instanceof String schemaDataJson) {
				dto.setSchemaData(OpaqueJsonColumnCodecUtil.deserialize(schemaDataJson, SCHEMA_DATA));
			}
			return dto;
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	@Override
	public Class<? extends DBORequest> getBackupClass() {
		return DBORequest.class;
	}

	@Override
	public Class<? extends DBORequest> getDatabaseObjectClass() {
		return DBORequest.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?, ?>> list = new LinkedList<>();
		list.add(new DBORequestUser());
		return list;
	}

}
