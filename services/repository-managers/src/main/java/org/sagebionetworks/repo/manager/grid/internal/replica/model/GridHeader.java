package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.grid.ReplicaSelectionModel;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.semver4j.Semver;

public class GridHeader {

	private String sessionId;
	private Long replicaId;
	private LogicalTimestamp nodeId;
	private Semver documentVersion;
	private List<Column> orderedColumns;
	private LogicalTimestamp rowsId;
	private LogicalTimestamp columnOrderArrId;
	private LogicalTimestamp columnNamesVecId;
	private Long clockSequenceMaximum;
	private ReplicaSelectionModel replicaSelectionModel;

	public LogicalTimestamp getColumnOrderArrId() {
		return columnOrderArrId;
	}

	public GridHeader setColumnOrderArrId(LogicalTimestamp columnOrderArrId) {
		this.columnOrderArrId = columnOrderArrId;
		return this;
	}

	public LogicalTimestamp getColumnNamesVecId() {
		return columnNamesVecId;
	}

	public GridHeader setColumnNamesVecId(LogicalTimestamp columnNamesVecId) {
		this.columnNamesVecId = columnNamesVecId;
		return this;
	}

	public Semver getDocumentVersion() {
		return documentVersion;
	}

	public GridHeader setDocumentVersion(Semver documentVersion) {
		this.documentVersion = documentVersion;
		return this;
	}

	public List<Column> getOrderedColumns() {
		return orderedColumns;
	}

	public GridHeader setOrderedColumns(List<Column> orderedColumns) {
		this.orderedColumns = orderedColumns;
		return this;
	}

	public LogicalTimestamp getRowsId() {
		return rowsId;
	}

	public GridHeader setRowsId(LogicalTimestamp rowsId) {
		this.rowsId = rowsId;
		return this;
	}

	public GridHeader setNodeId(LogicalTimestamp nodeId) {
		this.nodeId = nodeId;
		return this;
	}

	public String getSessionId() {
		return sessionId;
	}

	public GridHeader setSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	public Long getReplicaId() {
		return replicaId;
	}

	public GridHeader setReplicaId(Long replicaId) {
		this.replicaId = replicaId;
		return this;
	}

	public LogicalTimestamp getNodeId() {
		return nodeId;
	}

	/**
	 * The maximum sequence number from the replica's clock at the time of the
	 * query.
	 * 
	 * @return
	 */
	public Long getClockSequenceMaximum() {
		return clockSequenceMaximum;
	}

	/**
	 * The maximum sequence number from the replica's clock at the time of the
	 * query.
	 * 
	 * @return
	 */
	public GridHeader setClockSequenceMaximum(Long clockSequenceMaximum) {
		this.clockSequenceMaximum = clockSequenceMaximum;
		return this;
	}

	public ReplicaSelectionModel getReplicaSelectionModel() {
		return replicaSelectionModel;
	}

	public GridHeader setReplicaSelectionModel(ReplicaSelectionModel replicaSelectionModel) {
		this.replicaSelectionModel = replicaSelectionModel;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(clockSequenceMaximum, columnNamesVecId, columnOrderArrId, documentVersion, nodeId,
				orderedColumns, replicaId, replicaSelectionModel, rowsId, sessionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GridHeader other = (GridHeader) obj;
		return Objects.equals(clockSequenceMaximum, other.clockSequenceMaximum)
				&& Objects.equals(columnNamesVecId, other.columnNamesVecId)
				&& Objects.equals(columnOrderArrId, other.columnOrderArrId)
				&& Objects.equals(documentVersion, other.documentVersion) && Objects.equals(nodeId, other.nodeId)
				&& Objects.equals(orderedColumns, other.orderedColumns) && Objects.equals(replicaId, other.replicaId)
				&& Objects.equals(replicaSelectionModel, other.replicaSelectionModel)
				&& Objects.equals(rowsId, other.rowsId) && Objects.equals(sessionId, other.sessionId);
	}

	@Override
	public String toString() {
		return "GridHeader [sessionId=" + sessionId + ", replicaId=" + replicaId + ", nodeId=" + nodeId
				+ ", documentVersion=" + documentVersion + ", orderedColumns=" + orderedColumns + ", rowsId=" + rowsId
				+ ", columnOrderArrId=" + columnOrderArrId + ", columnNamesVecId=" + columnNamesVecId
				+ ", clockSequenceMaximum=" + clockSequenceMaximum + ", replicaSelectionModel=" + replicaSelectionModel
				+ "]";
	}

}
