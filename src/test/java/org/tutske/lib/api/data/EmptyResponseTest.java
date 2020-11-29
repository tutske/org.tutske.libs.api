package org.tutske.lib.api.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.tutske.lib.json.Mappers;

import java.io.ByteArrayOutputStream;


public class EmptyResponseTest {

	@Test
	public void it_should_provide_no_bytes_for_the_empty_response () throws Exception {
		ObjectMapper mapper = Mappers.mapper (Mappers.module (m -> {
			m.addSerializer (EmptyResponse.class, new EmptyResponse.JacksonSerializer ());
		}));

		byte [] result = mapper.writeValueAsBytes (EmptyResponse.getInstance ());

		assertThat (result.length, is (0));
	}

	@Test
	public void it_should_provide_no_bytes_for_the_empty_response_on_a_stream () throws Exception {
		ObjectMapper mapper = Mappers.mapper (Mappers.module (m -> {
			m.addSerializer (EmptyResponse.class, new EmptyResponse.JacksonSerializer ());
		}));

		ByteArrayOutputStream stream = new ByteArrayOutputStream ();
		mapper.writeValue (stream, EmptyResponse.getInstance ());

		assertThat (stream.toByteArray ().length, is (0));
	}

	@Test
	public void it_should_provide_empty_string_for_the_empty_response () throws Exception {
		ObjectMapper mapper = Mappers.mapper (Mappers.module (m -> {
			m.addSerializer (EmptyResponse.class, new EmptyResponse.JacksonSerializer ());
		}));

		String result = mapper.writeValueAsString (EmptyResponse.getInstance ());

		assertThat (result, isEmptyString ());
	}

}
