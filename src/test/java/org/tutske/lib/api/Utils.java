package org.tutske.lib.api;

import java.net.URISyntaxException;
import java.util.List;


public class Utils {

	public static Client createClientFor (API.Producer<? extends Request> api) throws URISyntaxException {
		return createClientFor ("", api);
	}

	public static Client createClientFor (String path, API.Producer<? extends Request> api) throws URISyntaxException {
		if ( path == null ) {
			throw new IllegalArgumentException ("uri segment cannot be null");
		}
		if ( ! ( path.isEmpty () || path.startsWith ("/") || path.startsWith ("?") || path.startsWith ("#") ) ) {
			throw new IllegalArgumentException ("uri segment must start with a '/', '?' or '#'");
		}

		return DirectClient.fromApi (path, api);
	}

	public static Client closableClient (Client client, AutoCloseable ... closables) {
		return new Client () {
			@Override public Request request (Method method, String path) {
				return client.request (method, path);
			}

			@Override public Request request (Method method, String path, Parameter... params) {
				return client.request (method, path, params);
			}

			@Override public Request request (Method method, String path, List<Parameter> params) {
				return client.request (method, path, params);
			}

			@Override public <T> T extractWrapped (Class<T> clazz) {
				return client.extractWrapped (clazz);
			}

			@Override public void close () throws Exception {
				for ( AutoCloseable closable : closables ) { closable.close (); }
				client.close ();
			}
		};
	}

}
