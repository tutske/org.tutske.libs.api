package org.tutske.lib.api.exceptions;

import com.fasterxml.jackson.databind.node.ObjectNode;


public class NotFoundException extends ResponseException {

	public static int STATUS_CODE = 404;

	{
		type = "/not_found";
		title = "Not Found";
		status = STATUS_CODE;
	}

	public NotFoundException () { this ("Could not find requested resource"); }
	public NotFoundException (String message) { super (message); }
	public NotFoundException (String message, Throwable cause) { super (message, cause); }
	public NotFoundException (Throwable cause) { super (cause); }

	public NotFoundException (ObjectNode data) { super (data); }
	public NotFoundException (String message, ObjectNode data) { super (message, data); }

}
