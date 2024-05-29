package org.sagebionetworks.repo.model;

import java.util.Objects;
import java.util.Optional;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

public class HierarchyInfo {

	private String path;
	private String benefactorId;
	private String projectId;

	public String getPath() {
		return path;
	}

	public HierarchyInfo setPath(String path) {
		this.path = path;
		return this;
	}

	public String getBenefactorId() {
		return benefactorId;
	}

	public HierarchyInfo setBenefactorId(String benefactorId) {
		this.benefactorId = benefactorId;
		return this;
	}

	public String getProjectId() {
		return projectId;
	}

	public HierarchyInfo setProjectId(String projectId) {
		this.projectId = projectId;
		return this;
	}

	/**
	 * Parse the provide JSON.
	 * 
	 * @param json
	 * @return {@link Optional#empty()} if the provided JSON is null.
	 */
	public static Optional<HierarchyInfo> parseHierachyInfoJson(String json) {
		if (json == null) {
			return Optional.empty();
		}
		JSONObject object = new JSONObject(json);
		HierarchyInfo info = new HierarchyInfo().setPath(object.getString("path"));
		if (!object.isNull("benefactorId")) {
			info.setBenefactorId(KeyFactory.keyToString(object.getLong("benefactorId")));
		}
		if (!object.isNull("projectId")) {
			info.setProjectId(KeyFactory.keyToString(object.getLong("projectId")));
		}
		return Optional.of(info);
	}

	@Override
	public int hashCode() {
		return Objects.hash(benefactorId, path, projectId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HierarchyInfo other = (HierarchyInfo) obj;
		return Objects.equals(benefactorId, other.benefactorId) && Objects.equals(path, other.path)
				&& Objects.equals(projectId, other.projectId);
	}

	@Override
	public String toString() {
		return "HierarchyInfo [path=" + path + ", benefactorId=" + benefactorId + ", projectId=" + projectId + "]";
	}

}
