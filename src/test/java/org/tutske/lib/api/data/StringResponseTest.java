package org.tutske.lib.api.data;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.tutske.lib.json.Mappers;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;


public class StringResponseTest {

	@Test
	public void it_should_write_the_original_data () throws Exception {
		ObjectMapper mapper = Mappers.mapper (Mappers.module (m -> {
			m.addSerializer (StringResponse.class, new StringResponse.JacksonSerializer ());
		}));

		byte [] result = mapper.writeValueAsBytes (StringResponse.of ("test"));

		assertThat (new String (result, StandardCharsets.UTF_8), is ("test"));
	}

	@Test
	public void it_should_write_the_original_data_as_string () throws Exception {
		ObjectMapper mapper = Mappers.mapper (Mappers.module (m -> {
			m.addSerializer (StringResponse.class, new StringResponse.JacksonSerializer ());
		}));

		String result = mapper.writeValueAsString (StringResponse.of ("test"));

		assertThat (result, is ("test"));
	}

	@Test
	public void it_should_write_the_original_data_to_a_stream () throws Exception {
		ObjectMapper mapper = Mappers.mapper (Mappers.module (m -> {
			m.addSerializer (StringResponse.class, new StringResponse.JacksonSerializer ());
		}));

		ByteArrayOutputStream stream = new ByteArrayOutputStream ();
		mapper.writeValue (stream, StringResponse.of ("test"));

		assertThat (stream.toByteArray (), is ("test".getBytes ()));
	}



}
