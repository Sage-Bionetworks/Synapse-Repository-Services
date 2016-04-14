package org.sagebionetworks.repo.web.controller;

import java.io.EOFException;

/**
 * list of exception handlers to test. One on one relationship with BaseController, and BaseControllerTest makes sure
 * all handlers are represented here. The actual test happens in IT500SynapseJavaClient
 */
public class ExceptionHandlers {

	public static class ExceptionType {
		public final String name;
		public final boolean isRuntimeException;
		public final String concreteClassName;

		public ExceptionType(String name, boolean isRuntimeException) {
			this.name = name;
			this.isRuntimeException = isRuntimeException;
			this.concreteClassName = null;
		}

		public ExceptionType(String name, boolean isRuntimeException, String concreteClassName) {
			this.name = name;
			this.isRuntimeException = isRuntimeException;
			this.concreteClassName = concreteClassName;
		}
	}

	public static class TestEntry {
		public final int statusCode;
		public final ExceptionType[] exceptions;

		public TestEntry(int statusCode, ExceptionType... exceptions) {
			this.statusCode = statusCode;
			this.exceptions = exceptions;
		}
	}

	public static final TestEntry[] testEntries = new TestEntry[] {
		new TestEntry(202,
				new ExceptionType("org.sagebionetworks.repo.model.table.TableUnavilableException", false),
				new ExceptionType("org.sagebionetworks.repo.model.NotReadyException", false)),
		new TestEntry(404,
				new ExceptionType("org.sagebionetworks.repo.web.NotFoundException", true),
				new ExceptionType("org.sagebionetworks.repo.manager.trash.EntityInTrashCanException", true),
				new ExceptionType("org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException", false),
				new ExceptionType("org.sagebionetworks.repo.model.ACLInheritanceException", false)),
		new TestEntry(503,
				new ExceptionType("org.springframework.dao.TransientDataAccessException", true, "org.springframework.dao.DeadlockLoserDataAccessException"),
				new ExceptionType("org.sagebionetworks.repo.web.ServiceUnavailableException", false)),
		new TestEntry(412,
				new ExceptionType("org.sagebionetworks.repo.model.ConflictingUpdateException", true)),
		new TestEntry(403,
				new ExceptionType("org.sagebionetworks.repo.model.UnauthorizedException", true),
				new ExceptionType("org.sagebionetworks.repo.manager.trash.ParentInTrashCanException", true),
				new ExceptionType("org.sagebionetworks.repo.model.TermsOfUseException", true)),
		new TestEntry(401,
				new ExceptionType("org.sagebionetworks.repo.model.UnauthenticatedException", true)),
		new TestEntry(409,
				new ExceptionType("org.sagebionetworks.repo.model.NameConflictException", true)),
		new TestEntry(400,
				new ExceptionType(IllegalArgumentException.class.getName(), true),
				new ExceptionType("org.sagebionetworks.repo.model.InvalidModelException", true),
				new ExceptionType("org.springframework.beans.TypeMismatchException", true),
				new ExceptionType("org.springframework.web.bind.annotation.support.HandlerMethodInvocationException", true),
				new ExceptionType("org.springframework.http.converter.HttpMessageNotReadableException", true),
				new ExceptionType("org.sagebionetworks.repo.model.AsynchJobFailedException", false),
				new ExceptionType("org.springframework.web.bind.MissingServletRequestParameterException", false),
				new ExceptionType("org.sagebionetworks.schema.adapter.JSONObjectAdapterException", false),
				new ExceptionType(EOFException.class.getName(), false),
				new ExceptionType("org.sagebionetworks.repo.queryparser.ParseException", false),
				new ExceptionType("org.sagebionetworks.utils.HttpClientHelperException", false)),
		new TestEntry(406,
				new ExceptionType("org.springframework.web.HttpMediaTypeNotAcceptableException", false)),
		new TestEntry(415,
				new ExceptionType("org.springframework.web.HttpMediaTypeNotSupportedException", false)),
		new TestEntry(500,
				new ExceptionType("org.sagebionetworks.repo.model.DatastoreException", true),
				new ExceptionType(IllegalStateException.class.getName(), true),
				new ExceptionType(NullPointerException.class.getName(), true),
				new ExceptionType("javax.servlet.ServletException", false),
				new ExceptionType("org.springframework.web.util.NestedServletException", false),
				new ExceptionType(Exception.class.getName(), false)),
		new TestEntry(429,
				new ExceptionType("org.sagebionetworks.repo.model.TooManyRequestsException", true)),
		new TestEntry(423,
				new ExceptionType("org.sagebionetworks.repo.model.LockedException", true)) };
}
