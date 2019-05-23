package org.tutske.lib.api.exceptions;


public class AuthorizationFailure extends ResponseException {

	{
		type = "/authorization_failure";
		title = "You are not authorizated to access the url";
		status = 403;
	}

	public AuthorizationFailure () {
	}

	public AuthorizationFailure (String message) {
		super (message);
	}

	public AuthorizationFailure (String message, Throwable cause) {
		super (message, cause);
	}

	public AuthorizationFailure (Throwable cause) {
		super (cause);
	}

	public AuthorizationFailure (ExceptionData data) {
		super (data);
	}

	public AuthorizationFailure (String message, ExceptionData data) {
		super (message, data);
	}

}
