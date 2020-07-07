package org.tutske.lib.api;


public enum Method {

	CONNECT, DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT, TRACE, UNKNOWN;

	public static Method of (String value) {
		try { return Method.valueOf (value.toUpperCase ()); }
		catch ( IllegalArgumentException e ) { return UNKNOWN; }
	}

}
