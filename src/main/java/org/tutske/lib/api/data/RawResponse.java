package org.tutske.lib.api.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;


public class RawResponse {

	public static RawResponse of (byte [] bytes) {
		return new RawResponse (bytes);
	}

	public static RawResponse of (InputStream stream) {
		return new RawResponse (stream);
	}

	public static void configureJacksonMapper (SimpleModule module) {
		module.addSerializer (RawResponse.class, new RawResponse.JacksonSerializer ());
	}

	public static class JacksonSerializer extends StdSerializer<RawResponse> {
		public JacksonSerializer () { super (RawResponse.class); }
		@Override public void serialize (RawResponse value, JsonGenerator gen, SerializerProvider provider)
		throws IOException {
			Object target = gen.getOutputTarget ();
			if ( target instanceof OutputStream ) { write ((OutputStream) target, value); }
			else { writeEncoded (gen, value); }
		}

		private void write (OutputStream out, RawResponse response) throws IOException {
			if ( response.stream != null ) { response.stream.transferTo (out); }
			else if ( response.bytes != null ) { out.write (response.bytes); }
		}

		private void writeEncoded (JsonGenerator gen, RawResponse response) throws IOException {
			if ( response.bytes != null ) { gen.writeRawValue (Base64.getEncoder ().encodeToString (response.bytes)); }
			else if ( response.stream != null ) {
				response.stream.transferTo (Base64.getEncoder ().wrap (new OutputStream () {
					@Override public void write (int b) throws IOException {
						gen.writeRaw ((char) b);
					}
					@Override public void write (byte [] bytes, int off, int len) throws IOException {
						gen.writeRaw (new String (bytes, off, len, StandardCharsets.US_ASCII));
					}
				}));
			}
		}
	}

	private final byte [] bytes;
	private final InputStream stream;

	private RawResponse (byte[] bytes) {
		this.bytes = bytes;
		this.stream = null;
	}

	private RawResponse (InputStream stream) {
		this.bytes = null;
		this.stream = stream;
	}

}
