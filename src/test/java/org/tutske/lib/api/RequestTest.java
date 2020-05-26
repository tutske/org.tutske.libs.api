package org.tutske.lib.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;
import org.tutske.lib.utils.Bag;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class RequestTest {

	@Test
	public void it_should_get_the_body_as_a_string () {
		Request request = bytesRequest ("The Body Content".getBytes ());
		assertThat (request.body (), is ("The Body Content"));
	}

	@Test
	public void it_should_give_an_input_stream_with_the_bytes () throws IOException {
		Request request = bytesRequest ("The Body Content".getBytes ());

		try ( InputStream in = request.inputstream () ) {
			byte [] bytes = new byte ["The Body Content".getBytes ().length];
			int amount = in.read (bytes);

			assertThat (amount, is (bytes.length));
			assertThat (bytes, is ("The Body Content".getBytes ()));
		}
	}

	@Test
	public void it_should_give_bytes_corresponding_to_the_stream_content () {
		Request request = outputstreamRequest (new ByteArrayInputStream ("The Body Content".getBytes ()));
		assertThat (request.bytes (), is ("The Body Content".getBytes ()));
	}

	@Test
	public void it_should_give_string_content_corresponding_to_the_stream_content () {
		Request request = outputstreamRequest (new ByteArrayInputStream ("The Body Content".getBytes ()));
		assertThat (request.body (), is ("The Body Content"));
	}

	public static Request bytesRequest (byte [] body) {
		return new EmptyRequest () {
			@Override public byte [] bytes () {
				return body;
			}
		};
	}

	public static Request outputstreamRequest (InputStream stream) {
		return new EmptyRequest () {
			@Override public InputStream inputstream () {
				return stream;
			}
		};
	}

	public static class EmptyRequest implements Request {
		@Override public Method method () { return null; }
		@Override public String uri () { return null; }
		@Override public Bag<String, String> pathParams () { return null; }
		@Override public Bag<String, String> queryParams () { return null; }
		@Override public Bag<String, String> headers () { return null; }
		@Override public Bag<String, Object> context () { return null; }
		@Override public void setHeader (String header, String value) { }
		@Override public void setStatus (int status) { }
		@Override public <T> T json (Class<T> clazz) { return null; }
		@Override public OutputStream outputstream () { return null; }
		@Override public <T> T extractWrapped (Class<T> clazz) { return null; }
	}

}
