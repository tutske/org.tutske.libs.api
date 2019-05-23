package org.tutske.lib.api.exceptions;


public class AuthenticationFailure extends ResponseException {

	{
		type = "/authentication-failure";
		title = "Authentication Failure";
		status = 403;
	}

	public AuthenticationFailure () {
		this ("Invalid credentials.");
	}

	public AuthenticationFailure (String message) {
		super (message);
	}

	public AuthenticationFailure (String message, Throwable cause) {
		super (message, cause);
	}

	public AuthenticationFailure (Throwable cause) {
		super (cause);
	}

	public AuthenticationFailure (ExceptionData data) {
		super (data);
	}

	public AuthenticationFailure (String message, ExceptionData data) {
		super (message, data);
	}

}
