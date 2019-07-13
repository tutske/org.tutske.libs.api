package org.tutske.lib.api;

import org.tutske.lib.utils.Bag;


public class Filters {

	public static <REQ extends Request, RES> Filter<REQ, RES> allowOriginFilter (String origin) {
		return (req, chain) -> {
			Bag<String, String> headers = req.headers ();
			if ( headers.containsKey ("origin") ) {
				req.setHeader ("Access-Control-Allow-Origin", origin);
				req.setHeader ("Access-Control-Allow-Credentials", "true");
				req.setHeader ("Access-Control-Allow-Methods", headers.get ("Access-Control-Request-Method"));
				req.setHeader ("Access-Control-Allow-Headers", headers.get ("Access-Control-Request-Headers"));
			}
			return chain.apply (req);
		};
	}

}
