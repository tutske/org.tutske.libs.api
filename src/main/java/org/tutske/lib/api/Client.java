package org.tutske.lib.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.tutske.lib.json.Json;
import org.tutske.lib.json.JsonException;
import org.tutske.lib.utils.Bag;
import org.tutske.lib.utils.Exceptions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;


public interface Client extends AutoCloseable {

	public Request request (Method method, String path);
	default public Request request (Method method, String path, Parameter ... params) {
		return request (method, path).with (params);
	}
	default public Request request (Method method, String path, List<Parameter> params) {
		return request (method, path).with (params);
	}

	default public <T> T extractWrapped (Class<T> clazz) {
		throw new JsonException ("Class not supported for extraction",
			Json.objectNode ("class", clazz.getCanonicalName ())
		);
	}

	public interface Parameter {
	}

	public abstract class BaseParameter implements Parameter{
		public final String key;
		public final Object value;

		public BaseParameter (String key, Object value) {
			this.key = key;
			this.value = value;
		}
	}

	public class QueryParameter extends BaseParameter {
		public QueryParameter (String key, Object value) {
			super (key, value);
		}
	}

	public class HeaderParameter extends BaseParameter {
		public HeaderParameter (String key, Object value) {
			super (key, value);
		}
	}

	public interface Request {
		public Request with (List<Parameter> params);
		default public Request with (Parameter param) { return with (Collections.singletonList (param)); }
		default public Request with (Parameter ... params) { return this.with (Arrays.asList (params)); }

		default public Request withHeader (String key, Object value) { return with (new HeaderParameter (key, value)); }
		default public Request withQuery (String key, Object value) { return with (new QueryParameter (key, value)); }

		default public Request withHeaders (Map<String, ?> headers) {
			headers.forEach (this::withHeader);
			return this;
		}

		default public Request withQuery (Map<String, ?> query) {
			query.forEach (this::withQuery);
			return this;
		}

		public CompletableFuture<Client.Response> send ();

		public CompletableFuture<Client.Response> send (InputStream stream);
		public CompletableFuture<Client.Response> send (byte [] payload);
		public CompletableFuture<Client.Response> send (JsonNode payload);
		default public CompletableFuture<Client.Response> send (String payload) {
			return send (payload.getBytes (StandardCharsets.UTF_8));
		}

		default public <T> CompletableFuture<T> send (Function<? super Response, ? extends T> fn) {
			return send ().thenApply (fn);
		}

		default public <T> CompletableFuture<T> thenApply (String payload, Function<? super Client.Response, ? extends T> fn) {
			return send (payload).thenApply (fn);
		}
		default public <T> CompletableFuture<T> thenApply (byte [] payload, Function<? super Client.Response, ? extends T> fn) {
			return send (payload).thenApply (fn);
		}
		default public <T> CompletableFuture<T> thenApply (InputStream payload, Function<? super Client.Response, ? extends T> fn) {
			return send (payload).thenApply (fn);
		}
		default public <T> CompletableFuture<T> thenApply (JsonNode payload, Function<? super Client.Response, ? extends T> fn) {
			return send (payload).thenApply (fn);
		}

		default public <T> CompletableFuture<T> thenCompose (Function<? super Client.Response, ? extends CompletionStage<T>> fn) {
			return send ().thenCompose (fn);
		}
		default public <T> CompletableFuture<T> thenCompose (String payload, Function<? super Client.Response, ? extends CompletionStage<T>> fn) {
			return send (payload).thenCompose (fn);
		}
		default public <T> CompletableFuture<T> thenCompose (byte [] payload, Function<? super Client.Response, ? extends CompletionStage<T>> fn) {
			return send (payload).thenCompose (fn);
		}
		default public <T> CompletableFuture<T> thenCompose (InputStream payload, Function<? super Client.Response, ? extends CompletionStage<T>> fn) {
			return send (payload).thenCompose (fn);
		}
		default public <T> CompletableFuture<T> thenCompose (JsonNode payload, Function<? super Client.Response, ? extends CompletionStage<T>> fn) {
			return send (payload).thenCompose (fn);
		}

		default public <T> T extractWrapped (Class<T> clazz) {
			throw new JsonException ("Class not supported for extraction",
				Json.objectNode ("class", clazz.getCanonicalName ())
			);
		}
	}

	public static interface Response {
		public int status ();

		public boolean isClientError ();
		public boolean isServerError ();

		default public boolean isError () { return isClientError () || isServerError (); }
		default public boolean isOk () { return ! isError (); }

		public Bag<String, String> headers ();

		default public String body () { return body (StandardCharsets.UTF_8); }
		default public String body (Charset charset) { return new String (bytes (), charset); }

		default public byte [] bytes () {
			try ( InputStream in = inputstream () ) {
				return in.readAllBytes ();
			} catch ( IOException e ) {
				throw Exceptions.wrap (e);
			}
		}

		public <T> T json (Class<T> clazz);
		default public JsonNode json () {
			return json (JsonNode.class);
		}

		default public InputStream inputstream () throws IOException {
			return new ByteArrayInputStream (bytes ());
		}

		default public <T> T extractWrapped (Class<T> clazz) {
			throw new JsonException ("Class not supported for extraction",
				Json.objectNode ("class", clazz.getCanonicalName ())
			);
		}
	}

}
