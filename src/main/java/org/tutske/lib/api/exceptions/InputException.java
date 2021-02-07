package org.tutske.lib.api.exceptions;

import com.fasterxml.jackson.databind.node.ObjectNode;


public class InputException extends ResponseException {

	{
		type = "/wrong-input";
		title = "Invalid input provided";
		status = 417;
	}

	public InputException () { this ("Invalid input provided."); }
	public InputException (String message) { super (message); }
	public InputException (String message, Throwable cause) { super (message, cause); }
	public InputException (Throwable cause) { super (cause); }

	public InputException (ObjectNode data) { super (data); }
	public InputException (String message, ObjectNode data) { super (message, data); }

}
