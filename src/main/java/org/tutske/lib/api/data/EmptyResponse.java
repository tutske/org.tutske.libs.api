package org.tutske.lib.api.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;


public class EmptyResponse {

	public static final EmptyResponse INSTANCE = new EmptyResponse ();

	public static EmptyResponse getInstance() { return INSTANCE; }

	public static void configureJacksonMapper (SimpleModule module) {
		module.addSerializer (EmptyResponse.class, new EmptyResponse.JacksonSerializer ());
	}

	public static class JacksonSerializer extends StdSerializer<EmptyResponse> {
		public JacksonSerializer () { super (EmptyResponse.class); }
		@Override public void serialize (EmptyResponse value, JsonGenerator gen, SerializerProvider provider)
		throws IOException {
		}
	}

}
