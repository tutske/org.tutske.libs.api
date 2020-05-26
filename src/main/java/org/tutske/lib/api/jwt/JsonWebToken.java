package org.tutske.lib.api.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tutske.lib.json.Mappers;
import org.tutske.lib.utils.Exceptions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.Function;


public class JsonWebToken {

	public static enum Keys {
		Headers, EncryptedKey, Iv, Payload, Authentication
	}

	private static final byte [] DEFAULT_HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes (StandardCharsets.UTF_8);
	private static final ObjectMapper DEFAULT_MAPPER = Mappers.mapper ();
	private static final JsonWebToken EMPTY_WEB_TOKEN = new JsonWebToken (DEFAULT_MAPPER, createEmptyData ());

	private static final Base64.Decoder decoder = Base64.getUrlDecoder ();
	private static final Base64.Encoder encoder = Base64.getUrlEncoder ();

	public static JsonWebToken emptyToken () {
		return EMPTY_WEB_TOKEN;
	}

	public static JsonWebToken fromJson (String json) {
		return fromBytes (DEFAULT_HEADER, json.getBytes (StandardCharsets.UTF_8));
	}

	public static JsonWebToken fromJson (String header, String json) {
		return fromBytes (header.getBytes (StandardCharsets.UTF_8), json.getBytes (StandardCharsets.UTF_8));
	}

	public static JsonWebToken fromMappedJson (ObjectMapper mapper, String json) {
		return fromMappedBytes (mapper, DEFAULT_HEADER, json.getBytes (StandardCharsets.UTF_8));
	}

	public static JsonWebToken fromMappedJson (ObjectMapper mapper, String header, String json) {
		return fromMappedBytes (mapper, header.getBytes (StandardCharsets.UTF_8), json.getBytes (StandardCharsets.UTF_8));
	}

	public static JsonWebToken fromData (Object data) {
		return fromMappedData (DEFAULT_MAPPER, data);
	}

	public static JsonWebToken fromData (Object header, Object data) {
		return fromMappedData (DEFAULT_MAPPER, header, data);
	}

	public static JsonWebToken fromMappedData (ObjectMapper mapper, Object data) {
		try { return fromMappedBytes (mapper, DEFAULT_HEADER, mapper.writeValueAsBytes (data)); }
		catch ( Exception e ) { throw Exceptions.wrap (e); }
	}

	public static JsonWebToken fromMappedData (ObjectMapper mapper, Object header, Object data) {
		try { return fromMappedBytes (mapper, mapper.writeValueAsBytes (header), mapper.writeValueAsBytes (data)); }
		catch ( Exception e ) { throw Exceptions.wrap (e); }
	}

	public static JsonWebToken fromTokenString (String token) {
		return fromTokenString (DEFAULT_MAPPER, token);
	}

	public static JsonWebToken fromTokenString (ObjectMapper mapper, String token) {
		if ( token == null || token.isBlank () ) { return EMPTY_WEB_TOKEN; }

		String [] parts = token.split ("\\.");
		byte [][] data = createEmptyData ();

		if ( parts.length == 2 ) {
			data[Keys.Headers.ordinal ()] = decoder.decode (parts[0]);
			data[Keys.Payload.ordinal ()] = decoder.decode (parts[1]);
		} else if ( parts.length == 3 ) {
			data[Keys.Headers.ordinal ()] = decoder.decode (parts[0]);
			data[Keys.Payload.ordinal ()] = decoder.decode (parts[1]);
			data[Keys.Authentication.ordinal ()] = decoder.decode (parts[2]);
		} else if ( parts.length == Keys.values ().length ) {
			for ( Keys key : Keys.values () ) {
				data[key.ordinal ()] = decoder.decode (parts[key.ordinal ()]);
			}
		} else {
			throw new IllegalArgumentException ("Token `" + token + "` has wrong number of parts.");
		}

		return new JsonWebToken (mapper, data);
	}

	public static JsonWebToken fromBytes (byte [] header, byte [] payload) {
		return fromMappedBytes (DEFAULT_MAPPER, header, payload);
	}

	public static JsonWebToken fromMappedBytes (ObjectMapper mapper, byte [] header, byte [] payload) {
		byte [][] data = createEmptyData ();
		data[Keys.Headers.ordinal ()] = Arrays.copyOf (header, header.length);
		data[Keys.Payload.ordinal ()] = Arrays.copyOf (payload, payload.length);
		return new JsonWebToken (mapper, data);
	}

	private static byte [][] createEmptyData () {
		byte [][] data = new byte [Keys.values ().length][];

		data[Keys.Headers.ordinal ()] = DEFAULT_HEADER;
		data[Keys.EncryptedKey.ordinal ()] = new byte [] {};
		data[Keys.Iv.ordinal ()] = new byte [] {};
		data[Keys.Payload.ordinal ()] = new byte [] { '{', '}'};
		data[Keys.Authentication.ordinal ()] = new byte [] {};

		return data;
	}

	private final ObjectMapper mapper;
	private final byte [][] data;

	public JsonWebToken (ObjectMapper mapper, byte [][] data) {
		if ( data.length != Keys.values ().length ) {
			throw new IllegalArgumentException ("data does not have required fields");
		}
		this.mapper = mapper;
		this.data = data;
	}

	public JsonWebToken with (Keys key, Object data) {
		try { return with (key, mapper.writeValueAsBytes (data)); }
		catch ( IOException e ) { throw Exceptions.wrap (e); }
	}

	public JsonWebToken with (Keys key, String data) {
		return with (key, data.getBytes (StandardCharsets.UTF_8));
	}

	public JsonWebToken with (Keys key, byte [] field) {
		byte [][] data = new byte [this.data.length][];
		for ( Keys k : Keys.values () ) {
			if ( k == key ) { continue; }
			byte [] retrieved = get (k);
			data[k.ordinal ()] = Arrays.copyOf (retrieved, retrieved.length);
		}
		data[key.ordinal ()] = field;
		return new JsonWebToken (mapper, data);
	}

	public JsonWebToken withMapper (ObjectMapper mapper) {
		return new JsonWebToken (mapper, data);
	}

	public boolean has (Keys key) {
		byte [] part = data[key.ordinal ()];
		return part != null && part.length != 0;
	}

	public byte [] get (Keys key) {
		return data[key.ordinal ()];
	}

	public String getEncoded (Keys key) {
		return encoder.encodeToString (get (key));
	}

	public <T> T get (Keys key, Class<T> clazz) {
		if ( String.class.equals (clazz) ) { return (T) new String (get (key), StandardCharsets.UTF_8); }
		try { return mapper.readValue (get (key), clazz); }
		catch ( Exception e ) { throw Exceptions.wrap (e); }
	}

	public <T> T get (Keys key, TypeReference<T> type) {
		try { return mapper.readValue (get (key), type); }
		catch ( Exception e ) { throw Exceptions.wrap (e); }
	}

	public String getPayload () {
		return new String (get (Keys.Payload), StandardCharsets.UTF_8);
	}

	public <T> T getPayload (Function<String, T> fn) {
		return fn.apply (getPayload ());
	}

	public <T> T getPayload (Class<T> clazz) {
		try { return mapper.readValue (getPayload (), clazz); }
		catch ( Exception e ) { throw Exceptions.wrap (e); }
	}

	public ObjectNode getJsonPayload () {
		try { return (ObjectNode) mapper.readTree (get (Keys.Payload)); }
		catch ( IOException e ) { throw Exceptions.wrap (e); }
	}

	public <T> T getJsonPayload (Function<ObjectNode, T> fn) {
		return fn.apply (getJsonPayload ());
	}

	@Override
	public String toString () {
		StringBuilder builder = new StringBuilder ();

		for ( byte [] part : data ) {
			if ( part == null || part.length == 0 ) { continue; }
			builder.append (encoder.encodeToString (part)).append (".");
		}

		return builder.delete (builder.length () - 1, builder.length ()).toString ();
	}

}
