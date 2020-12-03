package org.tutske.lib.api.exceptions;

import com.fasterxml.jackson.databind.node.ObjectNode;


public class AuthorizationFailure extends ResponseException {

	public static int STATUS_CODE = 401;

	{
		type = "/authorization_failure";
		title = "You are not authorizated to access the url";
		status = STATUS_CODE;
	}


	public AuthorizationFailure () { super (); }
	public AuthorizationFailure (String message) { super (message); }
	public AuthorizationFailure (String message, Throwable cause) { super (message, cause); }
	public AuthorizationFailure (Throwable cause) { super (cause); }

	public AuthorizationFailure (ObjectNode data) { super (data); }
	public AuthorizationFailure (String message, ObjectNode data) { super (message, data); }

}
