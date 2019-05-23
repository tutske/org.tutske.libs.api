module org.tutske.libs.api {

	exports org.tutske.lib.api;
	exports org.tutske.lib.api.exceptions;
	exports org.tutske.lib.api.jwt;

	/* explicit modules */
	requires org.tutske.libs.utils;
	requires org.slf4j;

	/* named automatic modules */
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.datatype.jdk8;
	requires com.fasterxml.jackson.datatype.jsr310;

}
