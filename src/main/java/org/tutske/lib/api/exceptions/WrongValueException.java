package org.tutske.lib.api.exceptions;


public class WrongValueException extends ResponseException {

	{
		type = "/wrong_value";
		title = "wrong value";
		status = 417;
	}

	public WrongValueException () {
	}

	public WrongValueException (String message) {
		super (message);
	}

	public WrongValueException (String message, Throwable cause) {
		super (message, cause);
	}

	public WrongValueException (Throwable cause) {
		super (cause);
	}

	public WrongValueException (ExceptionData data) {
		super (data);
	}

	public WrongValueException (String message, ExceptionData data) {
		super (message, data);
	}

}
