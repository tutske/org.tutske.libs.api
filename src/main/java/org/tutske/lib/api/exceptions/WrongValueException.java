package org.tutske.lib.api.exceptions;


import com.fasterxml.jackson.databind.node.ObjectNode;


public class WrongValueException extends ResponseException {

	{
		type = "/wrong_value";
		title = "wrong value";
		status = 417;
	}

	public WrongValueException () { super (); }
	public WrongValueException (String message) { super (message); }
	public WrongValueException (String message, Throwable cause) { super (message, cause); }
	public WrongValueException (Throwable cause) { super (cause); }

	public WrongValueException (ObjectNode data) { super (data); }
	public WrongValueException (String message, ObjectNode data) { super (message, data); }

}
