package org.tutske.lib.api.exceptions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.tutske.lib.json.JsonException;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;


public class ResponseException extends JsonException {

	public static int STATUS_CODE = 500;
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

	protected String type;
	protected String title;
	protected int status;

	{
		type = "/internal_server_error";
		title = "Internal Server Error";
		status = STATUS_CODE;
	}

	public ResponseException () {}
	public ResponseException (String message) { super (message); }
	public ResponseException (String message, Throwable cause) { super (message, cause); }
	public ResponseException (Throwable cause) { super (cause); }

	public ResponseException (ObjectNode data) { super (data); }
	public ResponseException (String message, ObjectNode data) { super (message, data); }
	public ResponseException (int status, String message, ObjectNode data) {
		super (message, data);
		this.status = status;
	}

	public int getStatusCode () {
		return this.status;
	}

	public String toTypeUrl (String baseHost, String baseUrl) {
		String h = BASE_HOST != null ? BASE_HOST : baseHost != null && ! baseHost.isEmpty () ? baseHost : "";
		String b = BASE_URL != null ? BASE_URL : baseUrl != null && ! baseUrl.isEmpty () ? baseUrl : "";
		return h + b + (type.startsWith ("/") ? type : "/" + type);
	}

	public static class JacksonSerializer extends StdSerializer<ResponseException> {
		public JacksonSerializer () { super (ResponseException.class); }

		@Override public void serialize (ResponseException value, JsonGenerator gen, SerializerProvider provider)
		throws IOException {
			gen.writeStartObject ();
			gen.writeNumberField ("status", value.getStatusCode ());
			gen.writeStringField ("type", value.toTypeUrl (null, null));
			gen.writeStringField ("title", value.title);
			gen.writeStringField ("detail", value.getMessage ());

			Iterator<Map.Entry<String, JsonNode>> it = value.data.fields ();
			while ( it.hasNext () ) {
				Map.Entry<String, JsonNode> field = it.next ();
				gen.writeObjectField (field.getKey (), field.getValue ());
			}

			gen.writeEndObject ();
		}
	}

	public static void configureJacksonMapper (SimpleModule module) {
		module.addSerializer (ResponseException.class, new JacksonSerializer ());
	}

}
