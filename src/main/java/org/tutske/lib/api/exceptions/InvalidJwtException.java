package org.tutske.lib.api.exceptions;

import org.tutske.lib.api.jwt.JsonWebToken;


public class InvalidJwtException extends ResponseException {

	{
		type = "/invalid_jwt";
		title = "The provided jwt could not be validated";
		status = 403;
	}

	public InvalidJwtException () {
		this ("Invalid jwt.");
	}

	public InvalidJwtException (String message) {
		super (message);
	}

	public InvalidJwtException (String message, Throwable cause) {
		super (message, cause);
	}

	public InvalidJwtException (Throwable cause) {
		super (cause);
	}

	public InvalidJwtException (ExceptionData data) {
		super (data);
	}

	public InvalidJwtException (String message, ExceptionData data) {
		super (message, data);
	}

	public InvalidJwtException (JsonWebToken token) {
		this (new ExceptionData () {{
			put ("token", token == null ? "null" : token.toString ());
		}});
	}

}
