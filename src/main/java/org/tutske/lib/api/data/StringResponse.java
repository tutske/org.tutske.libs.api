package org.tutske.lib.api.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;


public class StringResponse {

	public static StringResponse of (String content) {
		return new StringResponse (content);
	}

	public static void configureJacksonMapper (SimpleModule module) {
		module.addSerializer (StringResponse.class, new StringResponse.JacksonSerializer ());
	}

	public static class JacksonSerializer extends StdSerializer<StringResponse> {
		public JacksonSerializer () { super (StringResponse.class); }
		@Override public void serialize (StringResponse value, JsonGenerator gen, SerializerProvider provider)
		throws IOException {
			gen.writeRaw (value.content);
		}
	}

	public final String content;

	public StringResponse (String content) {
		this.content = content;
	}

}
