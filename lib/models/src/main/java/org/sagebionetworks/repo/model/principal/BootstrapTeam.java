package org.sagebionetworks.repo.model.principal;

import java.util.List;

public class BootstrapTeam {

	private String id;
	private String name;
	private String description;
	private String icon;
	private Boolean canPublicJoin;
	private List<String> initialMembers;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	public Boolean getCanPublicJoin() {
		return canPublicJoin;
	}
	public void setCanPublicJoin(Boolean canPublicJoin) {
		this.canPublicJoin = canPublicJoin;
	}
	public List<String> getInitialMembers() {
		return initialMembers;
	}
	public void setInitialMembers(List<String> initialMembers) {
		this.initialMembers = initialMembers;
	}
	
}
