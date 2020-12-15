package org.tutske.lib.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.tutske.lib.json.Json;
import org.tutske.lib.json.JsonException;
import org.tutske.lib.utils.Bag;
import org.tutske.lib.utils.Exceptions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


public interface Request {

	public static Bag<String, String> decodeInto (Bag<String, String> bag, String querystring) {
		if ( querystring == null || querystring.isEmpty () ) { return bag; }

		for ( String part : querystring.split ("&") ) {
			String [] split = part.split ("=", 2);
			String key = decodeQueryString (split[0]);

			if ( split.length == 2 && ! split[1].isEmpty () ) {
				String value = decodeQueryString (split[1]);
				bag.add (key, value);
			} else {
				bag.add (key);
			}
		}
		return bag;
	}

	public static Bag<String, String> decode (String querystring) {
		return decodeInto (new Bag<> (), querystring);
	}

	public static String decodeQueryString (String encoded) {
		try { return URLDecoder.decode (encoded, StandardCharsets.UTF_8); }
		catch ( Exception e ) { throw Exceptions.wrap (e); }
	}


	public Method method ();
	public String uri ();

	public Bag<String, String> pathParams ();
	public Bag<String, String> queryParams ();
	public Bag<String, String> headers ();
	public Bag<String, Object> context ();

	public void setHeader (String header, String value);
	public void setStatus (int status);

	default public String body () {
		return body (StandardCharsets.UTF_8);
	}

	default public String body (Charset charset) {
		return new String (bytes (), charset);
	}

	default public byte [] bytes () {
		try ( InputStream in = inputstream () ) {
			return in.readAllBytes ();
		} catch ( IOException e ) {
			throw new RuntimeException (e);
		}
	}

	public <T> T json (Class<T> clazz);
	default public JsonNode json () {
		return json (JsonNode.class);
	}

	default public InputStream inputstream () throws IOException {
		return new ByteArrayInputStream (bytes ());
	}

	public OutputStream outputstream () throws IOException;

	default public <T> T extractWrapped (Class<T> clazz) {
		throw new JsonException ("Class not supported for extraction",
			Json.objectNode ("class", clazz.getCanonicalName ())
		);
	}

	CompletableFuture<Void> reply (int status, Map<String, ?> headers, Object payload);
	default CompletableFuture<Void> reply (int status, Object payload) {
		return reply (status, Collections.emptyMap (), payload);
	}
	default CompletableFuture<Void> reply (Map<String, ?> headers, Object payload) {
		return reply (200, headers, payload);
	}
	default CompletableFuture<Void> reply (Object payload) {
		return reply (200, Collections.emptyMap (), payload);
	}

	CompletableFuture<Void> reply (int status, Map<String, ?> headers, InputStream in);
	default CompletableFuture<Void> reply (int status, InputStream in) {
		return reply (status, Collections.emptyMap (), in);
	}
	default CompletableFuture<Void> reply (Map<String, ?> headers, InputStream in) {
		return reply (200, headers, in);
	}
	default CompletableFuture<Void> reply (InputStream in) {
		return reply (200, Collections.emptyMap (), in);
	}
	default CompletableFuture<Void> reply (int status, Map<String, ?> headers, byte [] payload) {
		return reply (status, headers, new ByteArrayInputStream (payload));
	}
	default CompletableFuture<Void> reply (int status, byte [] payload) {
		return reply (status, Collections.emptyMap (), new ByteArrayInputStream (payload));
	}
	default CompletableFuture<Void> reply (Map<String, ?> headers, byte [] payload) {
		return reply (200, headers, new ByteArrayInputStream (payload));
	}
	default CompletableFuture<Void> reply (byte [] payload) {
		return reply (200, Collections.emptyMap (), new ByteArrayInputStream (payload));
	}
	default CompletableFuture<Void> reply (int status) {
		return reply (status, Collections.emptyMap (), new byte [] {});
	}

}
