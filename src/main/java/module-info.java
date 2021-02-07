module org.tutske.libs.api {

	exports org.tutske.lib.api.data;
	exports org.tutske.lib.api.exceptions;
	exports org.tutske.lib.api.jwt;
	exports org.tutske.lib.api;

	/* explicit modules */
	requires org.slf4j;
	requires org.tutske.libs.json;
	requires org.tutske.libs.utils;

	/* named automatic modules */
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.datatype.jdk8;
	requires com.fasterxml.jackson.datatype.jsr310;

}
