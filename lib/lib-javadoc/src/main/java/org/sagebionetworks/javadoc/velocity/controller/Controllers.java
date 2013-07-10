package org.sagebionetworks.javadoc.velocity.controller;

import java.util.List;

public class Controllers {

	List<ControllerModel> controllers;

	public List<ControllerModel> getControllers() {
		return controllers;
	}

	public void setControllers(List<ControllerModel> controllers) {
		this.controllers = controllers;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((controllers == null) ? 0 : controllers.hashCode());
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
		Controllers other = (Controllers) obj;
		if (controllers == null) {
			if (other.controllers != null)
				return false;
		} else if (!controllers.equals(other.controllers))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Controllers [controllers=" + controllers + "]";
	}
	
}
