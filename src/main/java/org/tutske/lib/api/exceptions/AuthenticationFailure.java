package org.tutske.lib.api.exceptions;

import com.fasterxml.jackson.databind.node.ObjectNode;


public class AuthenticationFailure extends ResponseException {

	{
		type = "/authentication-failure";
		title = "Authentication Failure";
		status = 403;
	}

	public AuthenticationFailure () { this ("Invalid credentials."); }
	public AuthenticationFailure (String message) { super (message); }
	public AuthenticationFailure (String message, Throwable cause) { super (message, cause); }
	public AuthenticationFailure (Throwable cause) { super (cause); }

	public AuthenticationFailure (ObjectNode data) { super (data); }
	public AuthenticationFailure (String message, ObjectNode data) { super (message, data); }

}
