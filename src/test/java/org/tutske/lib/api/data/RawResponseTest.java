package org.tutske.lib.api.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.tutske.lib.json.Mappers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;


public class RawResponseTest {

	@Test
	public void it_should_write_the_original_data_as_bytes_with_arbitrary_bytes () throws Exception {
		ObjectMapper mapper = Mappers.mapper (Mappers.module (m -> {
			m.addSerializer (RawResponse.class, new RawResponse.JacksonSerializer ());
		}));

		byte [] original = allBytes ();
		byte [] result = mapper.writeValueAsBytes (RawResponse.of (original));

		assertThat (result, is (original));
	}

	@Test
	public void it_should_write_the_original_data_as_string_with_arbitrary_bytes () throws Exception {
		ObjectMapper mapper = Mappers.mapper (Mappers.module (m -> {
			m.addSerializer (RawResponse.class, new RawResponse.JacksonSerializer ());
		}));

		byte [] original = allBytes ();
		String result = mapper.writeValueAsString (RawResponse.of (original));

		assertThat (Base64.getDecoder ().decode (result), is (original));
	}

	@Test
	public void it_should_write_the_original_data_to_a_stream_with_arbitrary_bytes () throws Exception {
		ObjectMapper mapper = Mappers.mapper (Mappers.module (m -> {
			m.addSerializer (RawResponse.class, new RawResponse.JacksonSerializer ());
		}));

		ByteArrayOutputStream stream = new ByteArrayOutputStream ();

		byte [] original = allBytes ();
		mapper.writeValue (stream, RawResponse.of (original));

		assertThat (stream.toByteArray (), is (original));
	}

	@Test
	public void it_should_write_the_original_stream_as_bytes_with_arbitrary_bytes () throws Exception {
		ObjectMapper mapper = Mappers.mapper (Mappers.module (m -> {
			m.addSerializer (RawResponse.class, new RawResponse.JacksonSerializer ());
		}));

		byte [] original = allBytes ();
		byte [] result = mapper.writeValueAsBytes (RawResponse.of (new ByteArrayInputStream (original)));

		assertThat (result, is (original));
	}

	@Test
	public void it_should_write_the_original_stream_as_string_with_arbitrary_bytes () throws Exception {
		ObjectMapper mapper = Mappers.mapper (Mappers.module (m -> {
			m.addSerializer (RawResponse.class, new RawResponse.JacksonSerializer ());
		}));

		byte [] original = allBytes ();
		String result = mapper.writeValueAsString (RawResponse.of (new ByteArrayInputStream (original)));

		System.out.println ("result: " + result);
		assertThat (Base64.getDecoder ().decode (result), is (original));
	}

	@Test
	public void it_should_write_the_original_stream_to_a_stream_with_arbitrary_bytes () throws Exception {
		ObjectMapper mapper = Mappers.mapper (Mappers.module (m -> {
			m.addSerializer (RawResponse.class, new RawResponse.JacksonSerializer ());
		}));

		ByteArrayOutputStream stream = new ByteArrayOutputStream ();

		byte [] original = allBytes ();
		mapper.writeValue (stream, RawResponse.of (new ByteArrayInputStream (original)));

		assertThat (stream.toByteArray (), is (original));
	}

	private static byte [] allBytes () {
		byte [] bytes = new byte [255];
		for ( byte i = Byte.MIN_VALUE; i < Byte.MAX_VALUE; i++ ) {
			bytes[i - (int) Byte.MIN_VALUE] = i;
		}
		return bytes;
	}

}
