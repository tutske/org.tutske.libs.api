package org.tutske.lib.api;

import org.tutske.lib.utils.Bag;

import java.util.List;
import java.util.Map;
import java.util.function.Function;


public interface ApiRouter<REQ, RES> {

	List<String> getIdentifiers ();
	String toId (Method method, String version, String url, String [] parts);
	Bag<String, String> extractMatches (String identifier, String url, String [] parts);
	String linkTo (String identifier, Map<String, Object> params);
	Function<REQ, RES> getHandler (String identifier);
	Function<REQ, RES> createChain (Method method, String version, String url, String [] parts);
	Function<REQ, RES> createChain (Method method, String version, String url, String [] parts, Function<REQ, RES> fn);

}
