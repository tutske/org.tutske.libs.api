package org.tutske.lib.api.exceptions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Map;


public class ResponseException extends RuntimeException {

	private static String BASE_HOST = null;
	private static String BASE_URL = null;

	public static void configureBaseUrl (String baseHost, String baseUrl) {
		BASE_HOST = BASE_HOST == null && baseHost != null ? baseHost : BASE_HOST;
		BASE_URL = BASE_URL == null && baseUrl != null ? baseUrl : BASE_URL;

		if ( BASE_HOST != null && BASE_HOST.endsWith ("/") ) {
			BASE_HOST = BASE_HOST.substring (0, BASE_HOST.length () - 1);
		}

		if ( BASE_URL != null && BASE_URL.equals ("/") ) {
			BASE_URL = null;
		}
	}

	protected final ExceptionData data = new ExceptionData ();
	protected String type;
	protected String title;
	protected int status;

	{
		type = "/interal_server_error";
		title = "Internal Server Error";
		status = 500;
	}

	public ResponseException () {}
	public ResponseException (String message) { super (message); }
	public ResponseException (String message, Throwable cause) { super (message, cause); }
	public ResponseException (Throwable cause) { super (cause); }

	public ResponseException (ExceptionData data) {
		this.data.putAll (data);
	}
	public ResponseException (String message, ExceptionData data) {
		this (message);
		this.data.putAll (data);
	}

	public int getStatusCode () {
		return this.status;
	}

	public void addExtra (ExceptionData extra) {
		this.data.putAll (extra);
	}

	public String toTypeUrl (String baseHost, String baseUrl) {
		String h = BASE_HOST != null ? BASE_HOST : baseHost != null && ! baseHost.isEmpty () ? baseHost : "";
		String b = BASE_URL != null ? BASE_URL : baseUrl != null && ! baseUrl.isEmpty () ? baseUrl : "";
		return h + b + (type.startsWith ("/") ? type : "/" + type);
	}

	public static class ResponseExceptionSerializer extends StdSerializer<ResponseException> {
		public ResponseExceptionSerializer () { super (ResponseException.class); }

		@Override public void serialize (ResponseException value, JsonGenerator gen, SerializerProvider provider)
		throws IOException {
			gen.writeStartObject ();
			gen.writeStringField ("type", value.toTypeUrl (null, null));
			gen.writeStringField ("title", value.title);
			gen.writeNumberField ("status", value.getStatusCode ());
			gen.writeStringField ("detail", value.getMessage ());
			for ( Map.Entry<String, Object> entry : value.data.entrySet () ) {
				gen.writeObjectField (entry.getKey (), entry.getValue ());
			}
			gen.writeEndObject ();
		}
	}

}
