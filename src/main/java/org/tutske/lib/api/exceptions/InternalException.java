package org.tutske.lib.api.exceptions;


import com.fasterxml.jackson.databind.node.ObjectNode;


public class InternalException extends ResponseException {

	{
		type = "/internal-error";
		title = "The server suffered an internal error.";
		status = 500;
	}

	public InternalException () { super (); }
	public InternalException (String message) { super (message); }
	public InternalException (String message, Throwable cause) { super (message, cause); }
	public InternalException (Throwable cause) { super (cause); }

	public InternalException (ObjectNode data) { super (data); }
	public InternalException (String message, ObjectNode data) { super (message, data); }

}
