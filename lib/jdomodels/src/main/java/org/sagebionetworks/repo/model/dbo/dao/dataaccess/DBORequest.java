package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_ACCESSOR_CHANGES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_VERSION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_ATTACHMENTS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_CONCRETE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_DUC_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_EDUC_CONTENT_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_EDUC_ENVELOPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_INSTITUTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_IRB_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_PRINCIPAL_INVESTIGATOR;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_PUBLICATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_RESEARCH_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_SCHEMA_DATA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_SIGNING_OFFICIAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_SUMMARY_OF_USE;
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
			new FieldColumn("accessRequirementVersionNumber", COL_DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_VERSION_NUMBER),
			new FieldColumn("concreteType", COL_DATA_ACCESS_REQUEST_CONCRETE_TYPE),
			new FieldColumn("ducFileHandleId", COL_DATA_ACCESS_REQUEST_DUC_FILE_HANDLE_ID).withHasFileHandleRef(true),
			new FieldColumn("irbFileHandleId", COL_DATA_ACCESS_REQUEST_IRB_FILE_HANDLE_ID).withHasFileHandleRef(true),
			new FieldColumn("attachmentsJson", COL_DATA_ACCESS_REQUEST_ATTACHMENTS).withHasFileHandleRef(true).withIsFileHandleIdJsonList(true),
			new FieldColumn("accessorChangesJson", COL_DATA_ACCESS_REQUEST_ACCESSOR_CHANGES),
			new FieldColumn("institution", COL_DATA_ACCESS_REQUEST_INSTITUTION),
			new FieldColumn("principalInvestigatorJson", COL_DATA_ACCESS_REQUEST_PRINCIPAL_INVESTIGATOR),
			new FieldColumn("signingOfficialJson", COL_DATA_ACCESS_REQUEST_SIGNING_OFFICIAL),
			new FieldColumn("schemaDataJson", COL_DATA_ACCESS_REQUEST_SCHEMA_DATA),
			new FieldColumn("publication", COL_DATA_ACCESS_REQUEST_PUBLICATION),
			new FieldColumn("summaryOfUse", COL_DATA_ACCESS_REQUEST_SUMMARY_OF_USE),
			new FieldColumn("eDucEnvelopeId", COL_DATA_ACCESS_REQUEST_EDUC_ENVELOPE_ID),
			new FieldColumn("eDucContentHash", COL_DATA_ACCESS_REQUEST_EDUC_CONTENT_HASH)
		};

	/**
	 * Bridge: decodes the request content that a previous stack stored as a XStream blob, only used by {@link #getTranslator()}.
	 * Removed in the next stack, see the follow-up cleanup ticket.
	 */
	private static final UnmodifiableXStream REQUEST_XSTREAM = UnmodifiableXStream.builder()
			.allowTypeHierarchy(RequestInterface.class)
			.allowTypesByWildcard(new String[] {"org.sagebionetworks.repo.model.**"})
			.build();

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
	 * request content as a XStream blob. {@link #getTranslator()} converts it into the columns below and clears it. It is removed
	 * in the next stack, see the follow-up cleanup ticket.
	 */
	private byte[] requestSerialized;
	private Long accessRequirementVersionNumber;
	private String concreteType;
	private Long ducFileHandleId;
	private Long irbFileHandleId;
	private String attachmentsJson;
	private String accessorChangesJson;
	private String institution;
	private String principalInvestigatorJson;
	private String signingOfficialJson;
	private String schemaDataJson;
	private String publication;
	private String summaryOfUse;
	private String eDucEnvelopeId;
	private String eDucContentHash;

	@Override
	public String toString() {
		return "DBORequest [id=" + id + ", accessRequirementId=" + accessRequirementId
				+ ", researchProjectId=" + researchProjectId + ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn + ", etag=" + etag
				+ ", requestSerialized=" + Arrays.toString(requestSerialized)
				+ ", accessRequirementVersionNumber=" + accessRequirementVersionNumber
				+ ", concreteType=" + concreteType
				+ ", ducFileHandleId=" + ducFileHandleId
				+ ", irbFileHandleId=" + irbFileHandleId
				+ ", attachmentsJson=" + attachmentsJson
				+ ", accessorChangesJson=" + accessorChangesJson
				+ ", institution=" + institution
				+ ", principalInvestigatorJson=" + principalInvestigatorJson
				+ ", signingOfficialJson=" + signingOfficialJson
				+ ", schemaDataJson=" + schemaDataJson
				+ ", publication=" + publication
				+ ", summaryOfUse=" + summaryOfUse
				+ ", eDucEnvelopeId=" + eDucEnvelopeId + ", eDucContentHash=" + eDucContentHash + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = Arrays.hashCode(requestSerialized);
		result = prime * result + Objects.hash(id, accessRequirementId, researchProjectId, createdBy, createdOn, modifiedBy, modifiedOn, etag, accessRequirementVersionNumber, concreteType, ducFileHandleId, irbFileHandleId, attachmentsJson, accessorChangesJson, institution, principalInvestigatorJson, signingOfficialJson, schemaDataJson, publication, summaryOfUse, eDucEnvelopeId, eDucContentHash);
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
				&& Objects.equals(accessRequirementVersionNumber, other.accessRequirementVersionNumber)
				&& Objects.equals(concreteType, other.concreteType)
				&& Objects.equals(ducFileHandleId, other.ducFileHandleId)
				&& Objects.equals(irbFileHandleId, other.irbFileHandleId)
				&& Objects.equals(attachmentsJson, other.attachmentsJson)
				&& Objects.equals(accessorChangesJson, other.accessorChangesJson)
				&& Objects.equals(institution, other.institution)
				&& Objects.equals(principalInvestigatorJson, other.principalInvestigatorJson)
				&& Objects.equals(signingOfficialJson, other.signingOfficialJson)
				&& Objects.equals(schemaDataJson, other.schemaDataJson)
				&& Objects.equals(publication, other.publication)
				&& Objects.equals(summaryOfUse, other.summaryOfUse)
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

	public Long getAccessRequirementVersionNumber() {
		return accessRequirementVersionNumber;
	}

	public void setAccessRequirementVersionNumber(Long accessRequirementVersionNumber) {
		this.accessRequirementVersionNumber = accessRequirementVersionNumber;
	}

	public String getConcreteType() {
		return concreteType;
	}

	public void setConcreteType(String concreteType) {
		this.concreteType = concreteType;
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

	public String getAccessorChangesJson() {
		return accessorChangesJson;
	}

	public void setAccessorChangesJson(String accessorChangesJson) {
		this.accessorChangesJson = accessorChangesJson;
	}

	public String getInstitution() {
		return institution;
	}

	public void setInstitution(String institution) {
		this.institution = institution;
	}

	public String getPrincipalInvestigatorJson() {
		return principalInvestigatorJson;
	}

	public void setPrincipalInvestigatorJson(String principalInvestigatorJson) {
		this.principalInvestigatorJson = principalInvestigatorJson;
	}

	public String getSigningOfficialJson() {
		return signingOfficialJson;
	}

	public void setSigningOfficialJson(String signingOfficialJson) {
		this.signingOfficialJson = signingOfficialJson;
	}

	public String getSchemaDataJson() {
		return schemaDataJson;
	}

	public void setSchemaDataJson(String schemaDataJson) {
		this.schemaDataJson = schemaDataJson;
	}

	public String getPublication() {
		return publication;
	}

	public void setPublication(String publication) {
		this.publication = publication;
	}

	public String getSummaryOfUse() {
		return summaryOfUse;
	}

	public void setSummaryOfUse(String summaryOfUse) {
		this.summaryOfUse = summaryOfUse;
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
				dbo.setConcreteType(rs.getString(COL_DATA_ACCESS_REQUEST_CONCRETE_TYPE));
				dbo.setAccessRequirementVersionNumber(
						getNullableLong(rs, COL_DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_VERSION_NUMBER));
				dbo.setDucFileHandleId(getNullableLong(rs, COL_DATA_ACCESS_REQUEST_DUC_FILE_HANDLE_ID));
				dbo.setIrbFileHandleId(getNullableLong(rs, COL_DATA_ACCESS_REQUEST_IRB_FILE_HANDLE_ID));
				dbo.setAttachmentsJson(rs.getString(COL_DATA_ACCESS_REQUEST_ATTACHMENTS));
				dbo.setAccessorChangesJson(rs.getString(COL_DATA_ACCESS_REQUEST_ACCESSOR_CHANGES));
				dbo.setInstitution(rs.getString(COL_DATA_ACCESS_REQUEST_INSTITUTION));
				dbo.setPrincipalInvestigatorJson(rs.getString(COL_DATA_ACCESS_REQUEST_PRINCIPAL_INVESTIGATOR));
				dbo.setSigningOfficialJson(rs.getString(COL_DATA_ACCESS_REQUEST_SIGNING_OFFICIAL));
				dbo.setSchemaDataJson(rs.getString(COL_DATA_ACCESS_REQUEST_SCHEMA_DATA));
				dbo.setPublication(rs.getString(COL_DATA_ACCESS_REQUEST_PUBLICATION));
				dbo.setSummaryOfUse(rs.getString(COL_DATA_ACCESS_REQUEST_SUMMARY_OF_USE));
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
	 * {@link #getRequestSerialized() XStream blob} into the columns of the table. The blob, the XStream configuration and the
	 * conversion are removed in the next stack, see the follow-up cleanup ticket.
	 */
	@Override
	public MigratableTableTranslation<DBORequest, DBORequest> getTranslator() {
		return new MigratableTableTranslation<DBORequest, DBORequest>() {
			@Override
			public DBORequest createDatabaseObjectFromBackup(DBORequest backup) {
				if (backup.getRequestSerialized() == null) {
					return backup;
				}
				if (backup.hasContentColumns()) {
					throw new IllegalArgumentException(
							String.format("Both '%s' and the request content columns are not null", "requestSerialized"));
				}
				RequestInterface dto = readRequestSerialized(backup.getRequestSerialized());
				RequestUtils.copyContentToDbo(dto, backup);
				if (backup.getEDucEnvelopeId() == null && dto.getEDucSignatureEnvelopeId() != null) {
					backup.setEDucEnvelopeId(dto.getEDucSignatureEnvelopeId());
				}
				backup.setRequestSerialized(null);
				return backup;
			}

			@Override
			public DBORequest createBackupFromDatabaseObject(DBORequest dbo) {
				return dbo;
			}
		};
	}

	private boolean hasContentColumns() {
		return concreteType != null || accessRequirementVersionNumber != null || ducFileHandleId != null
				|| irbFileHandleId != null || attachmentsJson != null || accessorChangesJson != null || institution != null
				|| principalInvestigatorJson != null || signingOfficialJson != null || schemaDataJson != null
				|| publication != null || summaryOfUse != null;
	}

	/**
	 * Bridge: decodes the XStream blob. Removed in the next stack, see the follow-up cleanup ticket.
	 */
	private static RequestInterface readRequestSerialized(byte[] serialized) {
		try {
			RequestInterface dto = (RequestInterface) JDOSecondaryPropertyUtils.decompressObject(REQUEST_XSTREAM, serialized);
			// The blob stored the free-form schemaData as JSON text
			if (dto.getSchemaData() instanceof String schemaDataJson) {
				dto.setSchemaData(OpaqueJsonColumnCodecUtil.deserialize(schemaDataJson, "Request.schemaData"));
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
