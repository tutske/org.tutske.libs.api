package org.tutske.lib.api;

import org.tutske.lib.utils.Exceptions;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;


public class Chain<S, T> implements Function<S, T> {

	private int current = 0;
	private int depth = 0;
	private List<Filter<S, T>> routes;
	private Function<S, T> destination;

	public Chain (Function<S, T> destination) {
		this (destination, Collections.emptyList ());
	}

	public Chain (Function<S, T> destination, List<Filter<S, T>> routes) {
		this.destination = destination;
		this.routes = routes;
	}

	@Override
	public T apply (S source) {
		try { return riskyApply (source); }
		catch ( Exception e ) { throw Exceptions.wrap (e); }
	}

	public T riskyApply (S source) throws Exception {
		if ( current > depth ) { throw new RuntimeException ("Invalid call of filter chain"); }

		int index = current;

		current++;
		depth++;

		T result = ( index == routes.size () ?
			destination.apply (source) :
			this.routes.get (index).call (source, this)
		);

		depth--;

		return result;
	}

}
