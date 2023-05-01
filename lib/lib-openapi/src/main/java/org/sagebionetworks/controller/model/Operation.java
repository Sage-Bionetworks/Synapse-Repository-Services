package org.sagebionetworks.controller.model;

import org.springframework.web.bind.annotation.RequestMethod;
import org.sagebionetworks.util.ValidateArgument;

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

	public static Operation get(RequestMethod method) {
		ValidateArgument.required(method, "Request mapping is required.");
		for (Operation opp : Operation.values()) {
			if (opp.requestMethod.equals(method)) {
				return opp;
			}
		}
		throw new IllegalArgumentException("No operation found for RequestMethod " + method.getClass().getName());
	}
}