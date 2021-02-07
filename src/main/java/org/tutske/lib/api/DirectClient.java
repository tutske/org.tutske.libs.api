package org.tutske.lib.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tutske.lib.api.data.EmptyResponse;
import org.tutske.lib.api.data.RawResponse;
import org.tutske.lib.api.data.StringResponse;
import org.tutske.lib.api.exceptions.ResponseException;
import org.tutske.lib.json.Mappers;
import org.tutske.lib.utils.Bag;
import org.tutske.lib.utils.Exceptions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class DirectClient implements Client, Consumer<ApiRouter<org.tutske.lib.api.Request, CompletableFuture<Void>>> {

	private static Logger logger = LoggerFactory.getLogger (DirectClient.class);

	public static DirectClient fromApi (API.Producer<?> api) {
		return API.configure (new DirectClient (null, ""), (API.Producer) api);
	}

	public static DirectClient fromApi (String root, API.Producer<?> api) {
		return API.configure (new DirectClient (null, root), (API.Producer) api);
	}

	public static DirectClient fromApi (ObjectMapper mapper, API.Producer<?> api) {
		return API.configure (new DirectClient (mapper, ""), (API.Producer) api);
	}

	public static DirectClient fromApi (String root, ObjectMapper mapper, API.Producer<?> api) {
		return API.configure (new DirectClient (mapper, root), (API.Producer) api);
	}

	private final ObjectMapper mapper;
	private final String root;
	private final List<Parameter> parameters = new ArrayList<> ();
	private ApiRouter<org.tutske.lib.api.Request, CompletableFuture<Void>> router;

	private DirectClient (ObjectMapper mapper, String root) {
		this.root = root.endsWith ("/") ? root.substring (0, root.length () - 1) : root;
		this.mapper = Mappers.configure (mapper == null ? Mappers.mapper () : mapper.copy (),
			EmptyResponse::configureJacksonMapper,
			RawResponse::configureJacksonMapper,
			ResponseException::configureJacksonMapper,
			StringResponse::configureJacksonMapper
		);
	}

	@Override
	public Client.Request request (Method method, String path) {
		return new Request (mapper, router, parameters, method, root, path);
	}

	@Override
	public void close () throws Exception {
	}

	@Override
	public void accept (ApiRouter<org.tutske.lib.api.Request, CompletableFuture<Void>> router) {
		this.router = router;
	}

	public static class Request implements Client.Request {
		private final ObjectMapper mapper;
		private final List<Parameter> defaults;
		private final ApiRouter<org.tutske.lib.api.Request, CompletableFuture<Void>> router;

		private final Method method;
		private final String root;
		private final String path;
		private final List<Parameter> params = new ArrayList<> ();

		public Request (
			ObjectMapper mapper, ApiRouter<org.tutske.lib.api.Request, CompletableFuture<Void>> router, List<Parameter> defaults,
			Method method, String root, String path
		) {
			this.mapper = mapper;
			this.router = router;
			this.defaults = defaults;
			this.method = method;
			this.root = root;
			this.path = path;
		}

		@Override public Client.Request with (List<Parameter> params) {
			this.params.addAll (params);
			return this;
		}

		@Override public CompletableFuture<Client.Response> send () {
			return send ((InputStream) null);
		}

		@Override public CompletableFuture<Client.Response> send (byte[] payload) {
			return send (payload == null || payload.length == 0 ? null : new ByteArrayInputStream (payload));
		}

		@Override public CompletableFuture<Client.Response> send (JsonNode payload) {
			try { return send (mapper.writeValueAsBytes (payload)); }
			catch ( IOException e ) { throw Exceptions.wrap (e); }
		}

		@Override public CompletableFuture<Client.Response> send (InputStream stream) {
			try { return execute (stream); }
			catch ( Exception e ) { throw Exceptions.wrap (e); }
		}

		private String createQuerySegment () {
			return Stream.concat (defaults.stream (), params.stream ())
				.filter (QueryParameter.class::isInstance)
				.map (QueryParameter.class::cast)
				.map (param -> (
					URLEncoder.encode (param.key, StandardCharsets.UTF_8) + "=" +
						URLEncoder.encode (String.valueOf (param.value), StandardCharsets.UTF_8)
				))
				.collect (Collectors.joining ("&"))
				;
		}

		private URI createUri () throws Exception {
			URI uri = new URI ("http://localhost" + root);
			URI base = uri.resolve (uri.getRawPath () + path);

			String query = Stream.of (uri.getRawQuery (), base.getRawQuery (), createQuerySegment ())
				.filter (s -> s != null && ! s.isEmpty ())
				.collect (Collectors.joining ("&"));

			return new URI ("http://localhost" + base.getRawPath () + "?" + query);
		}

		private CompletableFuture<Client.Response> execute (InputStream stream) throws Exception {
			URI uri = createUri ();
			String [] parts = API.splitParts (uri.getRawPath ().isEmpty () ? "/" : uri.getRawPath ());
			String version = "current";

			String id = router.toId (method, version, path, parts);
			if ( id == null ) { return CompletableFuture.completedFuture (new Response (mapper, 404)); }

			Bag<String, String> p = router.extractMatches (id, uri.getRawPath (), parts);
			Bag<String, String> q = org.tutske.lib.api.Request.decode (uri.getRawQuery ());
			ApiRequest request = new ApiRequest (method, uri.getRawPath (), p, q, stream, mapper);
			CompletableFuture<Void> future = CompletableFuture
				.supplyAsync (() -> router.createChain (method, version, path, parts))
				.thenComposeAsync (chain -> chain.apply (request));

			CompletableFuture<Client.Response> response = new CompletableFuture<> ();
			future.whenComplete ((value, throwable) -> {
				if ( throwable != null ) {
					logger.info ("", throwable);
					throwable.printStackTrace ();
				}
				response.complete (new Response (mapper, request, (
					throwable instanceof CompletionException ? throwable.getCause () :
					throwable
				)));
			});

			return response;
		}
	}

	public static class Response implements Client.Response {
		private final ObjectMapper mapper;
		private final int status;
		private final ApiRequest request;
		private final Throwable throwable;

		private byte [] bytes;
		private JsonNode json;

		public Response (ObjectMapper mapper, int status) {
			this (status, mapper, null, null);
		}

		public Response (ObjectMapper mapper, ApiRequest request, Throwable throwable) {
			this (
				(
					throwable instanceof ResponseException ? ((ResponseException) throwable).getStatusCode () :
					throwable == null ? request.status :
					500
				),
				mapper,
				request,
				throwable
			);
		}

		public Response (int status, ObjectMapper mapper, ApiRequest request, Throwable throwable) {
			this.mapper = mapper;
			this.status = status;
			this.request = request;
			this.throwable = (
				throwable == null ? null :
				throwable instanceof ResponseException ? throwable :
				new ResponseException (throwable)
			);
		}

		@Override public int status () { return status; }
		@Override public boolean isClientError () { return status >= 400 && status < 500; }
		@Override public boolean isServerError () { return status >= 500 && status < 600; }
		@Override public Bag<String, String> headers () {
			return request == null ? new Bag<> () : request.responseHeaders;
		}

		@Override
		public byte [] bytes () {
			if ( bytes == null ) {
				try { bytes = inputstream ().readAllBytes (); }
				catch ( IOException e ) { throw Exceptions.wrap (e); }
			}
			return bytes;
		}

		@Override
		public InputStream inputstream () throws IOException {
			return (
				bytes != null ? new ByteArrayInputStream (bytes) :
				throwable != null ? new ByteArrayInputStream (mapper.writeValueAsBytes (throwable)) :
				request == null ? new ByteArrayInputStream (new byte [] {}) :
				request.responseStream != null ? request.responseStream :
				request.responsePayload == null ? new ByteArrayInputStream (new byte [] {}) :
				new ByteArrayInputStream (mapper.writeValueAsBytes (request.responsePayload))
			);
		}

		@Override
		public JsonNode json () {
			if ( json == null ) {
				try {
					json = (
						request != null && request.responsePayload instanceof JsonNode ? (JsonNode) request.responsePayload :
						mapper.readTree (bytes ())
					);
				} catch ( IOException e ) {
					throw Exceptions.wrap (e);
				}
			}

			return json;
		}

		@Override
		public <T> T json (Class<T> clazz) {
			if ( clazz.isInstance (throwable) ) { return (T) throwable; }
			if ( clazz.isInstance (request.responsePayload) ) { return (T) request.responsePayload; }

			try { return mapper.readValue (json ().traverse (), clazz); }
			catch ( Exception e ) { throw Exceptions.wrap (e); }
		}
	}

	public static class ApiRequest implements org.tutske.lib.api.Request {
		private final ObjectMapper mapper;

		private final Method method;
		private final String uri;
		private final InputStream stream;

		private final Bag<String, String> path = new Bag<> ();
		private final Bag<String, String> query = new Bag<> ();
		private final Bag<String, String> headers = new Bag<> ();
		private final Bag<String, Object> context = new Bag<> ();

		private int status = 0;
		private final Bag<String, String> responseHeaders = new Bag<> ();
		private Object responsePayload;
		private InputStream responseStream;

		public ApiRequest (Method method, String uri, Bag<String, String> data, Bag<String, String> query, InputStream stream, ObjectMapper mapper) {
			this.mapper = mapper;
			this.method = method;
			this.stream = stream;
			this.uri = uri;
			this.path.putAll (data);
			this.query.putAll (query);
		}

		@Override public Method method () { return this.method; }
		@Override public String uri () { return this.uri; }
		@Override public Bag<String, String> pathParams () { return path; }
		@Override public Bag<String, String> queryParams () { return query; }
		@Override public Bag<String, String> headers () { return headers; }
		@Override public Bag<String, Object> context () { return context; }

		@Override public void setHeader (String header, String value) {
			this.responseHeaders.put (header, value);
		}

		@Override public void setStatus (int status) {
			this.status = status;
		}

		@Override public JsonNode json () {
			try { return mapper.readTree (bytes ()); }
			catch ( IOException e ) { throw Exceptions.wrap (e); }
		}

		@Override public <T> T json (Class<T> clazz) {
			try { return mapper.readValue (bytes (), clazz); }
			catch ( IOException e ) { throw Exceptions.wrap (e); }
		}

		@Override public InputStream inputstream () throws IOException {
			return stream;
		}

		@Override public OutputStream outputstream () throws IOException {
			throw new ResponseException ("Writing to output stream on direct api is not supported");
		}

		@Override
		public CompletableFuture<Void> reply (int status, Map<String, ?> headers, Object payload) {
			this.responsePayload = payload;
			return completeReply (status, headers);
		}

		@Override
		public CompletableFuture<Void> reply (int status, Map<String, ?> headers, InputStream in) {
			if ( responseStream == null ) { this.responseStream = in; }
			return completeReply (status, headers);
		}

		private CompletableFuture<Void> completeReply (int status, Map<String, ?> headers) {
			if ( this.status == 0 ) { setStatus (status); }

			for ( Map.Entry<String, ?> header : headers.entrySet () ) {
				if ( header.getKey () == null ) { continue; }
				this.responseHeaders.put (header.getKey (), String.valueOf (header.getValue ()));
			}

			return CompletableFuture.completedFuture (null);
		}
	}

}
