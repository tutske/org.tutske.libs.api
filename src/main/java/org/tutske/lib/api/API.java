package org.tutske.lib.api;

import org.tutske.lib.utils.Functions.*;
import org.tutske.lib.api.Request.Method;
import org.tutske.lib.utils.Bag;
import org.tutske.lib.utils.Exceptions;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;


public class API<REQ, RES> {

	/* -- static methods -- */

	public static <REQ, RES, T extends Consumer<ApiRouter<REQ, RES>>> T configure (
		T handler, RiskyConsumer<API<REQ, RES>> consumer
	) {
		handler.accept (configure (consumer));
		return handler;
	}

	public static <REQ, RES> ApiRouter<REQ, RES> configure (RiskyConsumer<API<REQ, RES>> consumer) {
		API<REQ, RES> api = new API<> ("", "");
		consumer.accept (api);
		return api.router;
	}

	public static String [] saveSplitParts (String descriptor) {
		if ( ! descriptor.endsWith ("/") ) { return splitParts (descriptor); }
		else { return splitParts (descriptor.substring (0, descriptor.length () - 1)); }
	}

	public static String [] splitParts (String descriptor) {
		if ( ! descriptor.startsWith ("/") ) {
			throw new RuntimeException ("descriptor should start with '/': " + descriptor);
		}

		if ( descriptor.endsWith ("/") && descriptor.length () > 1 ) {
			throw new RuntimeException ("descriptor should not end with a '/': " + descriptor);
		}

		String [] parts = descriptor.substring (1).split ("/");
		for ( int i = 0; i < parts.length; i++ ) {
			try { parts[i] = URLDecoder.decode (parts[i], "UTF-8"); }
			catch ( UnsupportedEncodingException e ) { throw Exceptions.wrap (e); }
		}

		return parts;
	}

	/* -- implementation -- */

	private final String version;
	private final String group;
	private final String groupId;
	private final InternalApiRouter<REQ, RES> router;

	public API (String version, String group) {
		this (version, "", group, new InternalApiRouter ());
	}

	public API (String version, String groupId, String group, InternalApiRouter<REQ, RES> router) {
		this.version = version;
		this.groupId = groupId;
		this.group = group;
		this.router = router;
	}

	public API<REQ, RES> version (String version) {
		return new API<> (version, groupId, group, router);
	}

	public API<REQ, RES> group (String id, String url, RiskyConsumer<API<REQ, RES>> consumer) {
		boolean isRelevant = url == null || url.isEmpty () || url.equals ("/");
		consumer.accept (isRelevant ? this : new API<> (version, id, group + url, router));
		return this;
	}

	public API<REQ, RES> group (String url, RiskyConsumer<API<REQ, RES>> consumer) {
		return group (nextId (groupId), url, consumer);
	}

	public API<REQ, RES> route (String id, String uri, EnumSet<Method> methods, RiskyFn<REQ, RES> fn) {
		if ( router.ids.containsKey (id) ) {
			throw new RuntimeException ("A route with that id already exists: " + id);
		}

		boolean isRelevant = ! (uri == null || uri.isEmpty () || uri.equals ("/"));
		String actual = isRelevant ? group + uri : group.isEmpty () ? uri : group;
		RouteDescription<REQ, RES> description = new RouteDescription<> (id, version, methods, actual, fn);
		add (router.routes, description);
		router.ids.put (id, description);

		return this;
	}

	public API<REQ, RES> route (String descriptor, EnumSet<Method> methods, RiskyFn<REQ, RES> fn) {
		return route (nextId (groupId), descriptor, methods, fn);
	}

	public API<REQ, RES> route (String identifier, String descriptor, RiskyFn<REQ, RES> fn) {
		return route (identifier, descriptor, EnumSet.of (Method.GET), fn);
	}

	public API<REQ, RES> route (String descriptor, RiskyFn<REQ, RES> fn) {
		return route (nextId (groupId), descriptor, fn);
	}

	public API<REQ, RES> filter (String descriptor, EnumSet<Method> methods, Filter<REQ, RES> filter) {
		router.filters.add (new FilterDescription<REQ, RES> (version, methods, group + descriptor, filter));
		return this;
	}

	public API<REQ, RES> filter (String descriptor, Filter<REQ, RES> filter) {
		return filter (descriptor, EnumSet.allOf (Method.class), filter);
	}

	/* -- utility -- */

	private static final AtomicLong id = new AtomicLong ();
	private static String nextId () { return String.valueOf (id.incrementAndGet ()); }
	private static String nextId (String group) { return group + "-" + nextId (); }

	private static class InternalApiRouter<REQ, RES> implements ApiRouter<REQ, RES> {
		private Map<String, Object> routes = new HashMap<> ();
		private Map<String, RouteDescription<REQ, RES>> ids = new HashMap<> ();
		private List<FilterDescription<REQ, RES>> filters = new LinkedList<> ();

		@Override public List<String> getIdentifiers () {
			return new LinkedList<> (ids.keySet ());
		}

		@Override public String toId (Method method, String version, String url, String [] parts) {
			RouteDescription description = find (routes, method, version, parts);
			return description == null ? null : description.id;
		}

		@Override public Bag<String, String> extractMatches (String identifier, String url, String [] parts) {
			RouteDescription<REQ, RES> description = ids.get (identifier);
			return description == null ? null : API.extractMatches (description, url, parts);
		}

		@Override public String linkTo (String identifier, Map<String, Object> params) {
			RouteDescription<REQ, RES> description = ids.get (identifier);
			return description == null ? null : API.linkTo (description, params);
		}

		@Override public Function<REQ, RES> getHandler (String identifier) {
			RouteDescription<REQ, RES> description = ids.get (identifier);
			return description == null ? null : description.handler;
		}

		@Override public Function<REQ, RES> createChain (Method method, String version, String url, String [] parts) {
			String id = toId (method, version, url, parts);
			return id == null ? null : createChain (method, version, url, parts, ids.get (id).handler);
		}

		@Override public Function<REQ, RES> createChain (Method method, String version, String url, String [] parts, Function<REQ, RES> fn) {
			List<Filter<REQ, RES>> selected = filters.stream ()
				.filter (f -> f.matches (version, method, url, parts))
				.map (f -> f.filter)
				.collect (Collectors.toList ());
			return new Chain<> (fn, selected);
		}
	}

	private static class FilterDescription<REQ, RES> {
		private final String version;
		private final EnumSet<Method> methods;
		private final String descriptor;
		private final Filter<REQ, RES> filter;

		private final String [] parts;
		private final boolean [] shouldMatch;
		private final boolean hasTail;

		public FilterDescription (String version, EnumSet<Method> methods, String descriptor, Filter<REQ, RES> filter) {
			this.version = version;
			this.methods = methods;
			this.descriptor = descriptor;
			this.filter = filter;

			this.parts = API.splitParts (descriptor);
			this.shouldMatch = new boolean [parts.length];
			this.hasTail = this.parts.length > 0 && this.parts[this.parts.length - 1].startsWith ("::");

			processDescriptor ();
		}

		private void processDescriptor () {
			for ( int i = 0; i < parts.length; i++ ) {
				this.shouldMatch[i] = ! parts[i].startsWith (":");
			}
		}

		private boolean matches (String version, Method method, String descriptor, String [] parts) {
			if ( ! this.methods.contains (method) ) { return false; }

			if ( ! this.version.isEmpty () && ! this.version.equals (version) ) { return false; }

			if ( this.parts.length < parts.length && ! this.hasTail ) { return false; }
			if ( (this.hasTail ? this.parts.length - 1 : this.parts.length) > parts.length ) { return false; }

			for ( int i = 0; i < this.parts.length; i++ ) {
				if ( this.shouldMatch[i] && ! this.parts[i].equals (parts[i]) ) { return false; }
			}

			return true;
		}
	}

	private static class RouteDescription<REQ, RES> {
		private final String id;
		private final String version;
		private final EnumSet<Method> methods;
		private final String descriptor;
		private final RiskyFn<REQ, RES> handler;

		private final String [] parts;
		private final String [] keys;
		private final boolean [] shouldMatch;
		private final boolean hasTail;

		public RouteDescription (String id, String version, EnumSet<Method> methods, String descriptor, RiskyFn<REQ, RES> handler) {
			this.id = id;
			this.version = version;
			this.methods = methods;
			this.descriptor = descriptor;
			this.handler = handler;

			this.parts = API.splitParts (descriptor);
			this.shouldMatch = new boolean [parts.length];
			this.keys = new String [parts.length];
			this.hasTail = this.parts.length > 0 && this.parts[this.parts.length - 1].startsWith ("::");

			processDescriptor ();
		}

		private void processDescriptor () {
			for ( int i = 0; i < parts.length; i++ ) {
				String part = parts[i];
				this.shouldMatch[i] = ! part.startsWith (":");
				this.keys[i] = part.startsWith ("::") ? part.substring (2) : part.startsWith (":") ? part.substring (1) : part;
			}
		}
	}

	private static <RES, REQ> void add (Map<String, Object> data, RouteDescription<REQ, RES> description) {
		String [] parts = splitParts (description.descriptor);

		for ( int i = 0; i < parts.length; i++ ) {
			String part = parts[i];

			if ( part.startsWith ("::") ) {
				if ( i < parts.length - 1 ) {
					throw new RuntimeException ("tail should be at end: " + description.descriptor);
				}
				data = (Map) data.computeIfAbsent ("*", (p) -> new HashMap<String, Object> ());
			} else if ( part.startsWith (":") ) {
				data = (Map) data.computeIfAbsent ("*", (p) -> new HashMap<String, Object> ());
			} else {
				data = (Map) data.computeIfAbsent (part, (p) -> new HashMap<String, Object> ());
			}
		}

		for ( Method method : description.methods ) {
			String key = method + ":" + description.version;
			if ( data.containsKey (key) ) {
				throw new RuntimeException ("A different path already matches");
			}

			data.put (key, description);
		}
	}

	private static <REQ, RES> RouteDescription<REQ, RES> find (Map<String, Object> data, Method method, String version, String [] parts) {
		boolean needsTail = false;
		for ( String part : parts ) {
			Object retrieved = data.get (part);

			if ( retrieved == null ) { retrieved = data.get ("*"); }
			if ( retrieved == null ) { needsTail = true; break; }

			if ( ! (retrieved instanceof Map) ) { needsTail = true; break; }
			data = (Map) retrieved;
		}

		Object retrieved = data.get (method + ":" + version);
		if ( retrieved == null ) { retrieved = data.get (method + ":"); }

		if ( ! (retrieved instanceof RouteDescription) ) { return null; }

		RouteDescription<REQ, RES> descriptor = (RouteDescription) retrieved;
		if ( needsTail && ! descriptor.hasTail ) { return null; }
		return descriptor;
	}

	private static String linkTo (RouteDescription<?, ?> description, Map<String, Object> params) {
		StringBuilder builder = new StringBuilder ();

		int last = description.parts.length - 1;
		for ( int i = 0; i < description.parts.length; i++ ) {
			if ( description.shouldMatch[i] ) { builder.append ("/").append (description.parts[i]); }
			else if ( last == i && description.hasTail ) {
				String value = String.valueOf (params.get (description.keys[i]));
				if ( ! value.startsWith ("/") ) { builder.append ("/"); }
				builder.append (value);
			} else {
				String value = String.valueOf (params.get (description.keys[i]));
				try { builder.append ("/").append (URLEncoder.encode (value, "UTF-8")); }
				catch ( UnsupportedEncodingException e ) { throw Exceptions.wrap (e); }
			}
		}

		return builder.toString ();
	}

	private static Bag<String, String> extractMatches (RouteDescription<?, ?> description, String url, String [] parts) {
		Bag<String, String> extracted = new Bag<> ();

		int last = description.parts.length - 1;
		for ( int i = 0; i < description.parts.length; i++ ) {
			if ( description.shouldMatch [i] ) { continue; }
			if ( i == last && description.hasTail ) { extracted.add (description.keys[i], join (url, parts, i)); }
			else { extracted.add (description.keys[i], parts[i]); }
		}

		return extracted;
	}

	private static String join (String url, String [] parts, int i) {
		int sum = 0;
		for ( int j = 0; j < i; j++ ) { sum += parts[j].length () + 1; }
		return url.substring (sum);
	}

}
