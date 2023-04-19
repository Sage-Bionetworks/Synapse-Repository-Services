package org.sagebionetworks.controller.model;

import org.springframework.web.bind.annotation.RequestMethod;

/**
 * The possible CRUD operations performed in a request.
 * 
 * @author lli
 *
 */
public enum Operation {
	get(RequestMethod.GET), post(RequestMethod.POST), delete(RequestMethod.DELETE), put(RequestMethod.PUT);

	private RequestMethod requestMethod;

	private Operation(RequestMethod method) {
		this.requestMethod = method;
	}

	public static Operation get(Object method) {
		if (method instanceof RequestMethod) {
			return get((RequestMethod) method);
		}
		throw new IllegalArgumentException();
	}

	public static Operation get(RequestMethod method) {
		for (Operation opp : Operation.values()) {
			if (opp.requestMethod.equals(method)) {
				return opp;
			}
		}
		throw new IllegalArgumentException();
	}
}