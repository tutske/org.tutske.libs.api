package org.tutske.lib.api.exceptions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tutske.lib.api.jwt.JsonWebToken;
import org.tutske.lib.json.Json;


public class InvalidJwtException extends ResponseException {

	public static int STATUS_CODE = AuthenticationFailure.STATUS_CODE;

	{
		type = "/invalid_jwt";
		title = "The provided jwt could not be validated";
		status = STATUS_CODE;
	}

	public InvalidJwtException () { super ("Invalid jwt."); }
	public InvalidJwtException (String message) { super (message); }
	public InvalidJwtException (String message, Throwable cause) { super (message, cause); }
	public InvalidJwtException (Throwable cause) { super (cause); }

	public InvalidJwtException (ObjectNode data) { super (data); }
	public InvalidJwtException (String message, ObjectNode data) { super (message, data); }

	public InvalidJwtException (JsonWebToken jwt) {
		super ("Invalid jwt.", Json.objectNode ("token", jwt));
	}

}
