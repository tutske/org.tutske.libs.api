package org.tutske.lib.api.jwt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;
import static org.tutske.lib.api.jwt.JsonWebToken.Keys.*;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.tutske.lib.json.Mappers;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;


public class JsonWebTokenTest {

	private final JsonWebToken token = JsonWebToken.fromTokenString ("hhhh.bbbb.ssss");

	@Test
	public void it_should_be_in_base64 () {
		assertThat (token.toString (), not (containsString ("{")));
		assertThat (token.toString (), is ("hhhh.bbbb.ssss"));
	}

	@Test
	public void it_should_parse_its_own_representations () {
		JsonWebToken parsed = JsonWebToken.fromTokenString (token.toString ());
		assertThat (parsed.toString (), is ("hhhh.bbbb.ssss"));
	}

	@Test
	public void it_should_skip_empty_parts_in_the_string_representation () {
		JsonWebToken modified = token.with (Authentication, new byte [] {});
		assertThat (modified.toString (), is ("hhhh.bbbb"));
	}

	@Test
	public void it_should_skip_null_parts_in_the_string_representation () {
		JsonWebToken modified = token.with (Authentication, (byte []) null);
		assertThat (modified.toString (), is ("hhhh.bbbb"));
	}

	@Test
	public void it_should_give_the_parts_base64_encoded () {
		assertThat (token.getEncoded (Authentication), is ("ssss"));
	}

	@Test
	public void it_should_give_parts_as_json_objects () {
		JsonWebToken token = JsonWebToken.fromJson (j ("{ 'key': 'value' }"));
		assertThat (token.getJson (Payload).has ("key"), is (true));
		assertThat (token.getJson (Payload).path ("key").asText (), is ("value"));
	}

	@Test
	public void it_should_give_parts_as_json_arrays () {
		JsonWebToken token = JsonWebToken.fromJson (j ("[ 'first', 'second' ]"));
		ArrayNode array = token.getJson (Payload);
		assertThat (array.path (0).asText (), is ("first"));
		assertThat (array.path (1).asText (), is ("second"));
	}

	@Test
	public void it_should_propagate_exceptions_when_deserializing_to_json () throws IOException {
		ObjectMapper mapper = mock (ObjectMapper.class);
		doThrow (new IOException ("Force Fail")).when (mapper).readTree (any (byte [].class));

		RuntimeException ex = assertThrows (RuntimeException.class, () -> {
			JsonWebToken.fromMappedJson (mapper, j ("{ 'key': 'value' }")).getJson (Payload);
		});

		assertThat (ex.getMessage (), is ("Force Fail"));
	}

	@Test
	public void it_should_keep_the_old_values_when_creating_a_token_with_different_values () {
		JsonWebToken changed = token.with (Authentication, new byte [] { (byte) 0xa2, (byte) 0x47 });
		assertThat (changed.get (Payload), is (token.get (Payload)));
	}

	@Test
	public void it_should_have_the_new_value_when_creating_a_token_with_different_values () {
		JsonWebToken changed = token.with (Payload, new byte [] { (byte) 0xa2, (byte) 0x47 });
		assertThat (changed.get (Payload), is (new byte [] { (byte) 0xa2, (byte) 0x47 }));
	}

	@Test
	public void it_should_create_a_token_from_string_without_signature () {
		JsonWebToken token = JsonWebToken.fromTokenString ("aaa.aaa");
		assertThat (token.get (Authentication), is (new byte [] {}));
		assertThat (token.get (Payload), not (nullValue ()));
	}

	@Test
	public void it_should_create_a_token_from_string_with_signature () {
		JsonWebToken token = JsonWebToken.fromTokenString ("aaa.aaa.aaa");
		assertThat (token.get (Authentication), not (nullValue ()));
		assertThat (token.get (Payload), not (nullValue ()));
	}

	@Test
	public void it_should_get_the_payload_from_the_token () {
		String body = j ("{ 'principal': 'jhon.doe@example.com' }");
		JsonWebToken token = JsonWebToken.fromJson (body);
		assertThat (token.getAs (Payload, String.class), is (body));
	}

	@Test
	public void it_should_make_a_web_token_from_any_object () {
		JsonWebToken token = JsonWebToken.fromData (Collections.singletonMap ("sub", "john.doe@example.com"));
		assertThat (token.getAs (Payload, String.class), containsString ("john.doe@example.com"));
	}

	@Test
	public void it_should_know_when_it_has_a_key () {
		JsonWebToken token = JsonWebToken.fromTokenString ("aaa.bbbb.ccc");
		assertThat (token.has (Authentication), is (true));
	}

	@Test
	public void it_should_know_when_it_does_not_have_a_key () {
		JsonWebToken token = JsonWebToken.fromTokenString ("aaaa.bbb");
		assertThat (token.has (Authentication), is (false));
	}

	@Test
	public void it_should_know_when_the_key_points_to_an_empty_value () {
		JsonWebToken modified = token.with (Authentication, new byte [] {});
		assertThat (modified.has (Authentication), is (false));
	}

	@Test
	public void it_should_know_when_the_key_points_to_a_null_value () {
		JsonWebToken modified = token.with (Authentication, (byte []) null);
		assertThat (modified.has (Authentication), is (false));
	}

	@Test
	public void it_should_turn_a_token_back_into_the_data () {
		JsonWebToken token = JsonWebToken.fromData (Collections.singletonMap ("sub", "jhon.doe@example.com"));
		Map<?, ?> retrieved = token.payloadAs (Map.class);
		assertThat (retrieved, hasKey ("sub"));
		assertThat (retrieved.get ("sub"), is ("jhon.doe@example.com"));
	}

	@Test
	public void it_should_turn_a_token_back_into_typed_data () {
		JsonWebToken token = JsonWebToken.fromData (Collections.singletonMap ("sub", "jhon.doe@example.com"));
		Map<String, Object> retrieved = token.payloadAs (new TypeReference<Map<String, Object>> () {});
		assertThat (retrieved, hasKey ("sub"));
		assertThat (retrieved.get ("sub"), is ("jhon.doe@example.com"));
	}

	@Test
	public void it_should_get_the_parts_as_different_classes () {
		JsonWebToken token = JsonWebToken.fromJson (j ("{'principal': 'jhon.doe@example.com'}"));
		assertThat (token.getAs (Headers, Map.class), not (nullValue ()));
	}

	@Test
	public void it_should_get_the_parts_as_different_typed_classes () {
		JsonWebToken token = JsonWebToken.fromJson (j ("{'principal': 'jhon.doe@example.com'}"));
		assertThat (token.getAs (Headers, new TypeReference<Map<String, Object>> () {}), not (nullValue ()));
	}

	@Test (expected = IllegalArgumentException.class)
	public void it_should_complain_when_creating_a_token_with_incorrect_parts () {
		new JsonWebToken (new ObjectMapper (), new byte [2288][]);
	}

	@Test (expected = IllegalArgumentException.class)
	public void it_should_complain_when_creating_from_a_string_with_incorrect_parts () {
		JsonWebToken.fromTokenString ("..........................................");
	}

	@Test
	public void it_should_construct_full_tokens () {
		Base64.Encoder encoder = Base64.getUrlEncoder ();
		JsonWebToken token = JsonWebToken.fromTokenString ("" +
			encoder.encodeToString ("headers".getBytes ()) + "." +
			encoder.encodeToString ("encrypted-key".getBytes ()) + "." +
			encoder.encodeToString ("iv".getBytes ()) + "." +
			encoder.encodeToString ("payload".getBytes ()) + "." +
			encoder.encodeToString ("authentication".getBytes ())
		);

		assertThat (token.getAs (Headers, String.class), is ("headers"));
		assertThat (token.getAs (EncryptedKey, String.class), is ("encrypted-key"));
		assertThat (token.getAs (Iv, String.class), is ("iv"));
		assertThat (token.getAs (Payload, String.class), is ("payload"));
		assertThat (token.getAs (Authentication, String.class), is ("authentication"));
	}

	@Test
	public void it_should_create_empty_tokens () {
		JsonWebToken token = JsonWebToken.emptyToken ();
		assertThat (token.getAs (Headers, ObjectNode.class).path ("typ").asText (), is ("JWT"));
		assertThat (token.payload ().size (), is (0));
		assertThat (token.get (Authentication).length, is (0));
	}

	@Test
	public void it_should_create_an_empty_token_from_an_empty_token_string () {
		JsonWebToken token = JsonWebToken.fromTokenString ("");
		assertThat (token.getAs (Headers, ObjectNode.class).path ("typ").asText (), is ("JWT"));
		assertThat (token.payload ().size (), is (0));
		assertThat (token.get (Authentication).length, is (0));
	}

	@Test
	public void it_should_create_an_empty_token_from_blank_token_string () {
		JsonWebToken token = JsonWebToken.fromTokenString ("  \t \n   \t\t\r\n");
		assertThat (token.getAs (Headers, ObjectNode.class).path ("typ").asText (), is ("JWT"));
		assertThat (token.payload ().size (), is (0));
		assertThat (token.get (Authentication).length, is (0));
	}

	@Test
	public void it_should_create_empty_tokens_from_null_token_strings () {
		JsonWebToken token = JsonWebToken.fromTokenString (null);
		assertThat (token.getAs (Headers, ObjectNode.class).path ("typ").asText (), is ("JWT"));
		assertThat (token.payload ().size (), is (0));
		assertThat (token.get (Authentication).length, is (0));
	}

	@Test
	public void it_should_allow_swapping_the_payload_with_json () {
		JsonWebToken token = JsonWebToken.emptyToken ().withJson (Payload, j ("{ 'name': 'John Doe' }"));
		assertThat (token.payload ().path ("name").asText (), is ("John Doe"));
	}

	@Test
	public void it_should_allow_swapping_the_payload_with_data () {
		JsonWebToken token = JsonWebToken.emptyToken ().with (
			Payload, Collections.singletonMap ("name", "John Doe")
		);
		assertThat (token.payload ().path ("name").asText (), is ("John Doe"));
	}

	@Test
	public void it_should_allow_swapping_the_payload_with_encoded_data () {
		JsonWebToken provider = JsonWebToken.fromJson (j ("{ 'from': 'provider' }"));
		String encoded = provider.getEncoded (Payload);

		JsonWebToken token = JsonWebToken.emptyToken ().withEncoded (Payload, encoded);

		assertThat (token.payload ().path( "from").asText (), is ("provider"));
	}

	@Test
	public void it_should_allow_swapping_the_payload_with_custom_serialized_data () {
		ObjectMapper mapper = Mappers.mapper (m -> Mappers.serialize (
			m, Map.class, JsonWebTokenTest::uppercaseMapSerializer
		));;

		JsonWebToken token = JsonWebToken.emptyToken ().withMapper (mapper).with (
			Payload, Collections.singletonMap ("name", "John Doe")
		);

		assertThat (token.payload ().path ("name").asText (), is ("JOHN DOE"));
	}

	@Test
	public void it_should_propagate_exceptions_as_runtime_exceptions_when_setting_payload_data ()
	throws JsonProcessingException {
		ObjectMapper mapper = mock (ObjectMapper.class);
		doThrow (new JsonGenerationException ("Force Fail")).when (mapper).writeValueAsBytes (any ());

		RuntimeException ex = assertThrows (RuntimeException.class, () -> {
			JsonWebToken.emptyToken ().withMapper (mapper).with (Payload, new Object ());
		});

		assertThat (ex.getMessage (), is ("Force Fail"));
	}

	@Test
	public void it_should_propagate_exceptions_when_setting_faulty_json_data () {
		assertThrows (RuntimeException.class, () -> {
			JsonWebToken.emptyToken ().withJson (Payload, j ("{ 'error': 'not closed' "));
		});
	}

	@Test
	public void it_should_construct_a_token_from_json_payload () {
		JsonWebToken token = JsonWebToken.fromJson (j ("{ 'name': 'John Doe' }"));
		assertThat (token.payload ().has ("name"), is (true));
		assertThat (token.payload ().path ("name").asText (), is ("John Doe"));
	}

	@Test
	public void it_should_construct_a_token_from_json_payload_and_json_header () {
		JsonWebToken token = JsonWebToken.fromJson (
			j ("{ 'alg': 'ecdsa' }"),
			j ("{ 'name': 'John Doe' }")
		);

		ObjectNode header = token.getAs (Headers, ObjectNode.class);
		assertThat (header.has ("alg"), is (true));
		assertThat (header.path ("alg").asText (), is ("ecdsa"));
	}

	@Test
	public void it_should_construct_a_token_from_json_playload_with_custom_mapper () {
		ObjectMapper mapper = Mappers.mapper (m -> Mappers.deserialize (
			m, Map.class, (p, ctx) -> Collections.singletonMap ("id", "fixed")
		));

		JsonWebToken token = JsonWebToken.fromMappedJson (mapper, j ("{ 'name': 'John Doe' }"));

		Map<String, ?> map = token.payloadAs (Map.class);
		assertThat (map.entrySet (), hasSize (1));
		assertThat (map.get ("id"), is ("fixed"));
	}

	@Test
	public void it_should_construct_a_token_from_json_payload_and_json_header_with_custom_mapper () {
		ObjectMapper mapper = Mappers.mapper (m -> Mappers.deserialize (
			m, Map.class, (p, ctx) -> Collections.singletonMap ("id", "fixed")
		));

		JsonWebToken token = JsonWebToken.fromMappedJson (
			mapper,
			j ("{ 'alg': 'ecdsa' }"),
			j ("{ 'name': 'John Doe' }")
		);

		Map<String, ?> map = token.getAs (Headers, Map.class);
		assertThat (map.entrySet (), hasSize (1));
		assertThat (map.get ("id"), is ("fixed"));
	}

	@Test
	public void it_should_construct_a_token_from_object_payload () {
		JsonWebToken token = JsonWebToken.fromData (Collections.singletonMap ("name", "John Doe"));
		assertThat (token.payload ().has ("name"), is (true));
		assertThat (token.payload ().path ("name").asText (), is ("John Doe"));
	}

	@Test
	public void it_should_construct_a_token_from_object_payload_and_object_header () {
		JsonWebToken token = JsonWebToken.fromData (
			Collections.singletonMap ("alg", "ecdsa"),
			Collections.singletonMap ("name", "John doe")
		);

		ObjectNode header = token.getAs (Headers, ObjectNode.class);
		assertThat (header.has ("alg"), is (true));
		assertThat (header.path ("alg").asText (), is ("ecdsa"));
	}

	@Test
	public void it_should_construct_a_token_from_object_playload_with_custom_mapper () {
		ObjectMapper mapper = Mappers.mapper (m -> Mappers.deserialize (
			m, Map.class, (p, ctx) -> Collections.singletonMap ("id", "fixed")
		));

		JsonWebToken token = JsonWebToken.fromMappedData (
			mapper,
			Collections.singletonMap ("name", "John doe")
		);

		Map<String, ?> map = token.payloadAs (Map.class);
		assertThat (map.entrySet (), hasSize (1));
		assertThat (map.get ("id"), is ("fixed"));
	}

	@Test
	public void it_should_construct_a_token_from_object_payload_and_object_header_with_custom_mapper () {
		ObjectMapper mapper = Mappers.mapper (m -> Mappers.deserialize (
			m, Map.class, (p, ctx) -> Collections.singletonMap ("id", "fixed")
		));

		JsonWebToken token = JsonWebToken.fromMappedData (
			mapper,
			Collections.singletonMap ("alg", "ecdsa"),
			Collections.singletonMap ("name", "John doe")
		);

		Map<String, ?> map = token.getAs (Headers, Map.class);
		assertThat (map.entrySet (), hasSize (1));
		assertThat (map.get ("id"), is ("fixed"));
	}

	@Test
	public void it_should_propagate_exceptions_from_mapper_as_runtime_exceptions_with_object_payload ()
	throws JsonProcessingException {
		ObjectMapper mapper = mock (ObjectMapper.class);
		doThrow (new JsonGenerationException ("Force Fail")).when (mapper).writeValueAsBytes (any ());

		RuntimeException ex = assertThrows (RuntimeException.class, () -> {
			JsonWebToken.fromMappedData (mapper, new Object ());
		});

		assertThat (ex.getMessage (), is ("Force Fail"));
	}

	@Test
	public void it_should_propagate_exceptions_from_mapper_as_runtime_exceptions_with_object_payload_and_object_header()
	throws JsonProcessingException {
		ObjectMapper mapper = mock (ObjectMapper.class);
		doThrow (new JsonGenerationException ("Force Fail")).when (mapper).writeValueAsBytes (any ());

		RuntimeException ex = assertThrows (RuntimeException.class, () -> {
			JsonWebToken.fromMappedData (mapper, new Object (), new Object ());
		});

		assertThat (ex.getMessage (), is ("Force Fail"));
	}

	@Test
	public void it_should_propagate_exceptions_from_getting_deserialized_headers_as_map () throws IOException {
		ObjectMapper mapper = mock (ObjectMapper.class);
		doThrow (new IOException ("Force Fail")).when (mapper).readValue (any (byte [].class), any (Class.class));

		RuntimeException ex = assertThrows (RuntimeException.class, () -> {
			JsonWebToken.emptyToken ().withMapper (mapper).getAs (Headers, Map.class);
		});

		assertThat (ex.getMessage (), is ("Force Fail"));
	}

	@Test
	public void it_should_propagate_exceptions_from_getting_deserialized_headers_typed_map () throws IOException {
		ObjectMapper mapper = mock (ObjectMapper.class);
		doThrow (new IOException ("Force Fail")).when (mapper).readValue (any (byte [].class), any (TypeReference.class));

		RuntimeException ex = assertThrows (RuntimeException.class, () -> {
			JsonWebToken.emptyToken ().withMapper (mapper).getAs (
				Headers, new TypeReference<Map<String, String>> () {}
			);
		});

		assertThat (ex.getMessage (), is ("Force Fail"));
	}

	@Test
	public void it_should_propagate_exceptions_from_getting_deserialized_payload_as_map ()
	throws IOException {
		ObjectMapper mapper = mock (ObjectMapper.class);
		doThrow (new IOException ("Force Fail")).when (mapper).readValue (any (byte [].class), any (Class.class));

		RuntimeException ex = assertThrows (RuntimeException.class, () -> {
			JsonWebToken.emptyToken ().withMapper (mapper).payloadAs (Map.class);
		});

		assertThat (ex.getMessage (), is ("Force Fail"));
	}

	@Test
	public void it_should_propagate_exceptions_from_getting_deserialized_payload_as_typed_map ()
	throws IOException {
		ObjectMapper mapper = mock (ObjectMapper.class);
		doThrow (new IOException ("Force Fail")).when (mapper).readValue (any (byte [].class), any (TypeReference.class));

		RuntimeException ex = assertThrows (RuntimeException.class, () -> {
			JsonWebToken.emptyToken ().withMapper (mapper).payloadAs (
				new TypeReference<Map<String, Object>> () {}
			);
		});

		assertThat (ex.getMessage (), is ("Force Fail"));
	}

	@Test
	public void it_should_propagate_exceptions_from_getting_deserialized_payload_typed_map () throws IOException {
		ObjectMapper mapper = mock (ObjectMapper.class);
		doThrow (new IOException ("Force Fail")).when (mapper).readTree (any (byte [].class));

		RuntimeException ex = assertThrows (RuntimeException.class, () -> {
			JsonWebToken.emptyToken ().withMapper (mapper).payload ();
		});

		assertThat (ex.getMessage (), is ("Force Fail"));
	}

	@Test
	public void it_should_retrieved_the_header_as_json () {
		JsonWebToken token = JsonWebToken.fromJson (j ("{ 'alg': 'ecdsa', 'key': 'value' }"), j ("{}"));
		ObjectNode headers = token.headers ();
		assertThat (headers.has ("key"), is (true));
		assertThat (headers.path ("key").asText (), is ("value"));
	}

	@Test
	public void it_should_retrieved_the_header_as_json_array () {
		JsonWebToken token = JsonWebToken.fromJson (j ("[ 'first', 'second' ]"), j ("{}"));
		ArrayNode array = token.headers ();
		assertThat (array.path (0).asText (), is ("first"));
		assertThat (array.path (1).asText (), is ("second"));
	}

	@Test
	public void it_should_deserialize_headers_into_a_map () {
		JsonWebToken token = JsonWebToken.fromJson (j ("{ 'alg': 'ecdsa', 'key': 'value' }"), j ("{}"));
		Map<String, String> headers = token.headersAs (Map.class);
		assertThat (headers, hasEntry ("key", "value"));
	}

	@Test
	public void it_should_deserialize_headers_into_a_typed_map () {
		TypeReference<Map<String, String>> type = new TypeReference<> () {};
		JsonWebToken token = JsonWebToken.fromJson (j ("{ 'alg': 'ecdsa', 'key': 'value' }"), j ("{}"));
		assertThat (token.headersAs (type).keySet (), containsInAnyOrder ("alg", "key"));
		assertThat (token.headersAs (type).values (), containsInAnyOrder ("ecdsa", "value"));
	}

	@Test
	public void it_should_propagate_exceptions_from_deserializing_headers () throws IOException {
		ObjectMapper mapper = mock (ObjectMapper.class);
		doThrow (new IOException ("Force Fail")).when (mapper).readTree (any (byte [].class));

		RuntimeException ex = assertThrows (RuntimeException.class, () -> {
			JsonWebToken.emptyToken ().withMapper (mapper).headers ();
		});

		assertThat (ex.getMessage (), is ("Force Fail"));
	}

	@Test
	public void it_should_propagate_exceptions_from_deserializing_headers_to_class () throws IOException {
		ObjectMapper mapper = mock (ObjectMapper.class);
		doThrow (new IOException ("Force Fail")).when (mapper).readValue (any (byte [].class), any (Class.class));

		RuntimeException ex = assertThrows (RuntimeException.class, () -> {
			JsonWebToken.emptyToken ().withMapper (mapper).headersAs (Map.class);
		});

		assertThat (ex.getMessage (), is ("Force Fail"));
	}

	@Test
	public void it_should_propagate_exceptions_from_deserializing_headers_to_typed_class () throws IOException {
		ObjectMapper mapper = mock (ObjectMapper.class);
		doThrow (new IOException ("Force Fail")).when (mapper).readValue (any (byte [].class), any (TypeReference.class));

		RuntimeException ex = assertThrows (RuntimeException.class, () -> {
			TypeReference<Map<String, String>> type = new TypeReference<> () {};
			JsonWebToken.emptyToken ().withMapper (mapper).headersAs (type);
		});

		assertThat (ex.getMessage (), is ("Force Fail"));
	}

	public static void uppercaseMapSerializer (Map<String, ?> value, JsonGenerator gen, SerializerProvider provider)
	throws IOException {
		gen.writeStartObject ();;
		for ( Map.Entry<?, ?> entry : value.entrySet () ) {
			gen.writeStringField (
				String.valueOf (entry.getKey ()),
				String.valueOf (entry.getValue ()).toUpperCase ()
			);
		}
		gen.writeEndObject ();;
	}

	public static String j (String json) {
		return json.replaceAll ("'", "\"");
	}

}
