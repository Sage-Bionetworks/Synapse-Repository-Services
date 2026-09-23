package org.sagebionetworks.repo.model.auth;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.sagebionetworks.repo.model.UnauthorizedException;

/**
 * Holds the result of an authorization check.
 * If 'authorized' is false then 'message' gives the user-presentable message for denial
 * 
 * @author brucehoff
 *
 */
public class AuthorizationStatus {
	private static final AuthorizationStatus AUTHORIZED_SINGLETON = new AuthorizationStatus(null);

	// if not null, indicates access denied
	private final RuntimeException denialException;

	// When access is denied, the id of the resource that would still permit the user
	// aggregate-level access (row-level data remains denied); null otherwise. Used by
	// the AGGREGATE_DATA / Cohort Builder tier: the caller inspects this to offer a
	// gated aggregate read of the identified source.
	private final String aggregateDataSourceId;


	//do not expose constuctor. use static methods instead
	private AuthorizationStatus(RuntimeException e) {
		this(e, null);
	}

	private AuthorizationStatus(RuntimeException e, String aggregateDataSourceId) {
		this.denialException = e;
		this.aggregateDataSourceId = aggregateDataSourceId;
	}

	/**
	 * Create a AuthorizationStatus that indicates the action action is authorized.
	 * @return AuthorizationStatus that indicates the action action is authorized.
	 */
	public static AuthorizationStatus authorized(){
		return AUTHORIZED_SINGLETON;
	}

	/**
	 * Create a AuthorizationStatus that indicates the action action is denied.
	 * Provide an {@link RuntimeException} that will be thrown when {@link #checkAuthorizationOrElseThrow()} is called
	 * @param denialException
	 * @return a new AuthorizationStatus;
	 */
	public static AuthorizationStatus accessDenied(RuntimeException denialException){
		return new AuthorizationStatus(denialException);
	}

	/**
	 * Create a AuthorizationStatus that indicates the action action is denied.
	 * Will use {@link org.sagebionetworks.repo.model.UnauthorizedException} as the underlying denialException that is
	 * thrown when {@link #checkAuthorizationOrElseThrow()} is called
	 * @param message message for the {@link org.sagebionetworks.repo.model.UnauthorizedException}
	 * @return a new AuthorizationStatus;
	 */
	public static AuthorizationStatus accessDenied(String message){
		return accessDenied(new UnauthorizedException(message));
	}

	/**
	 * Create an AuthorizationStatus that indicates the action is denied, but that the user
	 * would be permitted aggregate-level access to the identified resource (row-level data
	 * remains denied). {@link #checkAuthorizationOrElseThrow()} still throws; callers that
	 * support an aggregate-only mode branch on {@link #isAggregateAccessAllowed()} and read
	 * {@link #getAggregateDataSourceId()} before throwing.
	 *
	 * @param message               message for the {@link org.sagebionetworks.repo.model.UnauthorizedException}
	 * @param aggregateDataSourceId the id of the resource the user may read at the aggregate level
	 * @return a new AuthorizationStatus that is denied and aggregate-access-allowed.
	 */
	public static AuthorizationStatus accessDeniedButAggregateAllowed(String message, String aggregateDataSourceId){
		return new AuthorizationStatus(new UnauthorizedException(message), aggregateDataSourceId);
	}

	public void checkAuthorizationOrElseThrow(){
		if(!isAuthorized()){
			throw denialException;
		}
	}
	
	/**
	 * @param isAuthorizedOrElseSupplier A supplier for a unauthorized message in case this one is not authorized, the message will be used with {@link AuthorizationStatus#accessDenied(String)}
	 * @return This {@link AuthorizationStatus} if authorized, else an {@link AuthorizationStatus} created with the message from the given supplier
	 */
	public AuthorizationStatus isAuthorizedOrElseGet(Supplier<String> orElseSupplier) {
		return isAuthorized() ? this : AuthorizationStatus.accessDenied(orElseSupplier.get());
	}
	
	public boolean isAuthorized() {
		return denialException == null;
	}

	/**
	 * @return true if, despite access being denied, the user would be permitted
	 *         aggregate-level access to a resource. Always false when authorized.
	 */
	public boolean isAggregateAccessAllowed() {
		return aggregateDataSourceId != null;
	}

	/**
	 * @return the id of the resource the user may read at the aggregate level, or empty
	 *         when {@link #isAggregateAccessAllowed()} is false.
	 */
	public Optional<String> getAggregateDataSourceId() {
		return Optional.ofNullable(aggregateDataSourceId);
	}

	public String getMessage() {
		return denialException == null ? null : denialException.getMessage();
	}

	//The equals() here is not the conventional "check all fields members are equal" because it holds
	// a RuntimeException which does not by default @Override equals()

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AuthorizationStatus that = (AuthorizationStatus) o;
		return this.isAuthorized() == that.isAuthorized() &&
				Objects.equals(this.aggregateDataSourceId, that.aggregateDataSourceId) &&
				this.exceptionType() == that.exceptionType() &&
				Objects.equals(this.getMessage(), that.getMessage());
	}

	@Override
	public int hashCode() {
		return Objects.hash(isAuthorized(), getMessage(), exceptionType(), aggregateDataSourceId);
	}

	private Class exceptionType (){
		if (denialException == null){
			return null;
		}
		return denialException.getClass();
	}

	@Override
	public String toString() {
		return "AuthorizationStatus{" +
				"denialException=" + denialException +
				", aggregateDataSourceId=" + aggregateDataSourceId +
				'}';
	}
}
