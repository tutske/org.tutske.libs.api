package org.tutske.lib.api.exceptions;


public class InputException extends ResponseException {

	{
		type = "/wrong-input";
		title = "Invalid input provided";
		status = 417;
	}

	public InputException () {
		this ("Invalid input.");
	}

	public InputException (String message) {
		super (message);
	}

	public InputException (String message, Throwable cause) {
		super (message, cause);
	}

	public InputException (Throwable cause) {
		super (cause);
	}

	public InputException (ExceptionData data) {
		super (data);
	}

	public InputException (String message, ExceptionData data) {
		super (message, data);
	}

}
