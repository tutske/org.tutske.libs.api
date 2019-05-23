package org.tutske.lib.api;


public class Filters {

	public static <REQ extends Request, RES> Filter<REQ, RES> allowOriginFilter (String origin) {
		return (req, chain) -> {
			if ( req.getHeader ("origin") != null ) {
				req.setHeader ("Access-Control-Allow-Origin", origin);
				req.setHeader ("Access-Control-Allow-Credentials", "true");
				req.setHeader ("Access-Control-Allow-Methods", req.getHeader ("Access-Control-Request-Method"));
				req.setHeader ("Access-Control-Allow-Headers", req.getHeader ("Access-Control-Request-Headers"));
			}
			return chain.apply (req);
		};
	}

}
