package org.tutske.lib.api.jwt;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;


public class JsonWebTokenTest {

	private JsonWebToken token = JsonWebToken.fromString ("hh.bb.ss");

	@Test
	public void it_should_be_in_base64 () {
		assertThat (token.toString (), not (containsString ("{")));
	}

	@Test
	public void it_should_keep_the_old_values_when_creating_a_token_with_different_values () {
		JsonWebToken changed = token.with (JsonWebToken.Keys.Authentication, new byte [] { (byte) 0xa2, (byte) 0x47 });
		assertThat (changed.get (JsonWebToken.Keys.Payload), is (token.get (JsonWebToken.Keys.Payload)));
	}

	@Test
	public void it_should_have_the_new_value_when_creating_a_token_with_different_values () {
		JsonWebToken changed = token.with (JsonWebToken.Keys.Payload, new byte [] { (byte) 0xa2, (byte) 0x47 });
		assertThat (changed.get (JsonWebToken.Keys.Payload), is (new byte [] { (byte) 0xa2, (byte) 0x47 }));
	}

	@Test
	public void it_should_create_a_token_from_string_without_signature () {
		JsonWebToken token = JsonWebToken.fromString ("aaa.aaa");
		assertThat (token.get (JsonWebToken.Keys.Authentication), is (new byte [] {}));
		assertThat (token.get (JsonWebToken.Keys.Payload), not (nullValue ()));
	}

	@Test
	public void it_should_create_a_token_from_string_with_signature () {
		JsonWebToken token = JsonWebToken.fromString ("aaa.aaa.aaa");
		assertThat (token.get (JsonWebToken.Keys.Authentication), not (nullValue ()));
		assertThat (token.get (JsonWebToken.Keys.Payload), not (nullValue ()));
	}

	@Test
	public void it_should_get_the_payload_from_the_token () {
		String body = "{\"principal\": \"jhon.doe@example.com\"}";
		JsonWebToken token = JsonWebToken.fromString ("." + Base64.getEncoder ().encodeToString (body.getBytes ()) + ".");

		String retrieved = token.getPayload ();

		assertThat (retrieved, is (body));
	}

	@Test
	public void it_should_allow_converting_the_payload_into_some_object () {
		String body = "{\"principal\": \"jhon.doe@example.com\"}";
		JsonWebToken token = JsonWebToken.fromString ("." + Base64.getEncoder ().encodeToString (body.getBytes ()) + ".");

		String retrieved = token.getPayload ((content) -> "calculated");

		assertThat (retrieved, is ("calculated"));
	}

	@Test
	public void it_should_call_the_converting_method_with_the_payload () {
		String body = "{\"principal\": \"jhon.doe@example.com\"}";
		JsonWebToken token = JsonWebToken.fromString ("." + Base64.getEncoder ().encodeToString (body.getBytes ()) + ".");

		Function<String, String> fn = mock (Function.class);
		String retrieved = token.getPayload (fn);

		verify (fn).apply (body);
	}

	@Test
	public void it_should_make_a_web_token_from_any_object () {
		Map<String, String> values = new LinkedHashMap<> ();
		values.put ("sub", "jhon.doe@example.com");

		JsonWebToken token = JsonWebToken.fromData (values);

		assertThat (token.getPayload (), containsString ("jhon.doe@example.com"));
	}

	@Test
	public void it_should_know_when_it_has_a_key () {
		JsonWebToken token = JsonWebToken.fromString ("aaa.bbbb.ccc");
		assertThat (token.has (JsonWebToken.Keys.Authentication), is (true));
	}

	@Test
	public void it_should_know_when_it_does_not_have_a_key () {
		JsonWebToken token = JsonWebToken.fromString ("aaaa.bbb");
		assertThat (token.has (JsonWebToken.Keys.Authentication), is (false));
	}

	@Test
	public void it_should_turn_a_token_back_into_the_data () {
		Map<String, String> values = new LinkedHashMap<> ();
		values.put ("sub", "jhon.doe@example.com");

		JsonWebToken token = JsonWebToken.fromData (values);
		Map<?, ?> retrieved = token.getPayload (Map.class);

		assertThat (retrieved, hasKey ("sub"));
		assertThat (retrieved.get ("sub"), is ("jhon.doe@example.com"));
	}

	@Test
	public void it_should_get_the_parts_as_different_classes () {
		String body = "{'principal': 'jhon.doe@example.com'}".replace ("'", "\"");
		JsonWebToken token = JsonWebToken.fromData (body);
		assertThat (token.get (JsonWebToken.Keys.Headers, Map.class), not (nullValue ()));
	}

	@Test
	public void it_should_get_the_parts_as_different_typed_classes () {
		String body = "{'principal': 'jhon.doe@example.com'}".replace ("'", "\"");
		JsonWebToken token = JsonWebToken.fromData (body);
		assertThat (token.get (JsonWebToken.Keys.Headers, new TypeReference<Map<String, Object>> () {}), not (nullValue ()));
	}

	@Test (expected = IllegalArgumentException.class)
	public void it_should_complain_when_creating_a_token_with_incorrect_parts () {
		new JsonWebToken (new byte [2288][]);
	}

	@Test (expected = IllegalArgumentException.class)
	public void it_should_complain_when_creating_from_a_string_with_incorrect_parts () {
		JsonWebToken.fromString ("..........................................");
	}

	@Test
	public void it_should_construct_full_tokens () {
		Base64.Encoder encoder = Base64.getUrlEncoder ();
		JsonWebToken token = JsonWebToken.fromString ("" +
			encoder.encodeToString ("headers".getBytes ()) + "." +
			encoder.encodeToString ("encrypted-key".getBytes ()) + "." +
			encoder.encodeToString ("iv".getBytes ()) + "." +
			encoder.encodeToString ("payload".getBytes ()) + "." +
			encoder.encodeToString ("authentication".getBytes ())
		);

		assertThat (token.get (JsonWebToken.Keys.Headers, String.class), is ("headers"));
		assertThat (token.get (JsonWebToken.Keys.EncryptedKey, String.class), is ("encrypted-key"));
		assertThat (token.get (JsonWebToken.Keys.Iv, String.class), is ("iv"));
		assertThat (token.get (JsonWebToken.Keys.Payload, String.class), is ("payload"));
		assertThat (token.get (JsonWebToken.Keys.Authentication, String.class), is ("authentication"));
	}

}
