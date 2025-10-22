package org.sagebionetworks.repo.manager.grid.create;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;

public class CreateGridHandlerResult {

	private GridSession gridSession;
	private GridReplica gridReplica;

	public GridSession getGridSession() {
		return gridSession;
	}

	public CreateGridHandlerResult setGridSession(GridSession gridSession) {
		this.gridSession = gridSession;
		return this;
	}

	public GridReplica getGridReplica() {
		return gridReplica;
	}

	public CreateGridHandlerResult setGridReplica(GridReplica gridReplica) {
		this.gridReplica = gridReplica;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(gridReplica, gridSession);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CreateGridHandlerResult other = (CreateGridHandlerResult) obj;
		return Objects.equals(gridReplica, other.gridReplica) && Objects.equals(gridSession, other.gridSession);
	}

	@Override
	public String toString() {
		return "CreateGridResult [gridSession=" + gridSession + ", gridReplica=" + gridReplica + "]";
	}

}
