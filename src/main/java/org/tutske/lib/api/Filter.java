package org.tutske.lib.api;


@FunctionalInterface
public interface Filter<S, T> {

	T call (S source, Chain<S, T> chain) throws Exception;

}
