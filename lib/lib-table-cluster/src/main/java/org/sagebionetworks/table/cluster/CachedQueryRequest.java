package org.sagebionetworks.table.cluster;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.sagebionetworks.repo.model.table.SelectColumn;

public class CachedQueryRequest implements TranslatedQuery {

	private List<SelectColumn> selectColumns;
	private Map<String, ?> parameters;
	private String outputSQL;
	private boolean includesRowIdAndVersion = false;
	private boolean includeEntityEtag = false;
	private String singleTableId;
	private int expiresInSec = 60;

	public static CachedQueryRequest clone(TranslatedQuery toClone) {
		return new CachedQueryRequest().setSelectColumns(toClone.getSelectColumns())
				.setParameters(toClone.getParameters()).setOutputSQL(toClone.getOutputSQL())
				.setIncludesRowIdAndVersion(toClone.getIncludesRowIdAndVersion())
				.setIncludeEntityEtag(toClone.getIncludeEntityEtag())
				.setSingleTableId(toClone.getSingleTableId());
	}
	
	@Override
	public List<SelectColumn> getSelectColumns() {
		return selectColumns;
	}

	@Override
	public Map<String, ?> getParameters() {
		return parameters;
	}

	@Override
	public String getOutputSQL() {
		return outputSQL;
	}

	@Override
	public boolean getIncludesRowIdAndVersion() {
		return includesRowIdAndVersion;
	}

	@Override
	public boolean getIncludeEntityEtag() {
		return includeEntityEtag;
	}

	@Override
	public String getSingleTableId() {
		return singleTableId;
	}

	public CachedQueryRequest setIncludesRowIdAndVersion(boolean includesRowIdAndVersion) {
		this.includesRowIdAndVersion = includesRowIdAndVersion;
		return this;
	}

	public CachedQueryRequest setIncludeEntityEtag(boolean includeEntityEtag) {
		this.includeEntityEtag = includeEntityEtag;
		return this;
	}

	public CachedQueryRequest setSelectColumns(List<SelectColumn> selectColumns) {
		this.selectColumns = selectColumns;
		return this;
	}

	public CachedQueryRequest setParameters(Map<String, ?> parameters) {
		this.parameters = parameters;
		return this;
	}

	public CachedQueryRequest setOutputSQL(String outputSQL) {
		this.outputSQL = outputSQL;
		return this;
	}

	public CachedQueryRequest setSingleTableId(String singleTableId) {
		this.singleTableId = singleTableId;
		return this;
	}
	
	public int getExpiresInSec() {
		return expiresInSec;
	}

	public CachedQueryRequest setExpiresInSec(int expiresInSec) {
		this.expiresInSec = expiresInSec;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(expiresInSec, includeEntityEtag, includesRowIdAndVersion, outputSQL, parameters,
				selectColumns, singleTableId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CachedQueryRequest other = (CachedQueryRequest) obj;
		return expiresInSec == other.expiresInSec && includeEntityEtag == other.includeEntityEtag
				&& includesRowIdAndVersion == other.includesRowIdAndVersion
				&& Objects.equals(outputSQL, other.outputSQL) && Objects.equals(parameters, other.parameters)
				&& Objects.equals(selectColumns, other.selectColumns)
				&& Objects.equals(singleTableId, other.singleTableId);
	}

	@Override
	public String toString() {
		return "CachedQueryRequest [selectColumns=" + selectColumns + ", parameters=" + parameters + ", outputSQL="
				+ outputSQL + ", includesRowIdAndVersion=" + includesRowIdAndVersion + ", includeEntityEtag="
				+ includeEntityEtag + ", singleTableId=" + singleTableId + ", expiresInSec=" + expiresInSec + "]";
	}
	
	

}
