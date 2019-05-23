package org.tutske.lib.api.exceptions;


public class InternalException extends ResponseException {

	{
		type = "/internal-error";
		title = "The server suffered an internal error.";
		status = 500;
	}

	public InternalException () {
	}

	public InternalException (String message) {
		super (message);
	}

	public InternalException (String message, Throwable cause) {
		super (message, cause);
	}

	public InternalException (Throwable cause) {
		super (cause);
	}

	public InternalException (ExceptionData data) {
		super (data);
	}

	public InternalException (String message, ExceptionData data) {
		super (message, data);
	}

}
