package org.sagebionetworks.table.cluster.description;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_BENEFACTOR;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;

public class ViewIndexDescription implements IndexDescription {

	private final IdAndVersion idAndVersion;
	private final EntityType viewType;
	private final BenefactorDescription description;
	
	public ViewIndexDescription(IdAndVersion idAndVersion, EntityType viewtype) {
		super();
		this.idAndVersion = idAndVersion;
		this.viewType = viewtype;
		ObjectType benefactorType = EntityType.submissionview.equals(viewtype)? ObjectType.EVALUATION: ObjectType.ENTITY;
		this.description = new BenefactorDescription(ROW_BENEFACTOR, benefactorType);
	}

	@Override
	public IdAndVersion getIdAndVersion() {
		return idAndVersion;
	}

	@Override
	public String getCreateOrUpdateIndexSql() {
		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE IF NOT EXISTS ");
		builder.append(SQLUtils.getTableNameForId(idAndVersion, TableType.INDEX));
		builder.append("( ");
		builder.append(ROW_ID).append(" BIGINT NOT NULL, ");
		builder.append(ROW_VERSION).append(" BIGINT NOT NULL, ");
		builder.append(ROW_ETAG).append(" varchar(36) NOT NULL, ");
		builder.append(ROW_BENEFACTOR).append(" BIGINT NOT NULL, ");
		builder.append("PRIMARY KEY (").append("ROW_ID").append(")");
		builder.append(", KEY `IDX_ETAG` (").append(ROW_ETAG).append(")");
		builder.append(", KEY `IDX_BENEFACTOR` (").append(ROW_BENEFACTOR).append(")");
		builder.append(")");
		return builder.toString();
	}

	@Override
	public List<BenefactorDescription> getBenefactors() {
		return Collections.singletonList(description);
	}
	
	@Override
	public EntityType getTableType() {
		return viewType;
	}

	@Override
	public boolean isEtagColumnIncluded() {
		return true;
	}

	@Override
	public List<String> getColumnNamesToAddToSelect(SqlType type) {
		if(!SqlType.query.equals(type)) {
			throw new IllegalArgumentException("Only 'query' is supported for views");
		}
		return Arrays.asList(ROW_ID, ROW_VERSION, ROW_ETAG);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(description, idAndVersion, viewType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ViewIndexDescription)) {
			return false;
		}
		ViewIndexDescription other = (ViewIndexDescription) obj;
		return Objects.equals(description, other.description) && Objects.equals(idAndVersion, other.idAndVersion)
				&& viewType == other.viewType;
	}

	@Override
	public String toString() {
		return "ViewIndexDescription [idAndVersion=" + idAndVersion + ", viewType=" + viewType + ", description="
				+ description + "]";
	}
}
