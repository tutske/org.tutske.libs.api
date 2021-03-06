package org.tutske.lib.api.jwt;

import static org.tutske.lib.json.Json.objectNode;

import org.tutske.lib.api.exceptions.InvalidJwtException;

import java.time.Clock;
import java.time.Instant;
import java.util.function.BiFunction;


public class JwtValidator {

	public static class Config {
		protected BiFunction<JsonWebToken, String, Boolean> validate = (jwt, hash) -> false;

		protected Boolean strict = null;
		protected Boolean forceToken = null;
		protected Boolean forceEXP = null;
		protected Boolean forceIAT = null;
		protected Boolean forceNBT = null;

		public Config () {
			strict = false;
			forceToken = false;
			forceEXP = true;
			forceIAT = false;
			forceNBT = false;
		}

		public Config (Config config) {
			this ();
			merge (config);
		}

		private void merge (Config config) {
			if ( config.validate != null ) { validate = config.validate; }

			if ( config.strict != null ) { strict = config.strict; }
			if ( config.forceToken != null ) { forceToken = config.forceToken; }
			if ( config.forceEXP != null ) { forceEXP = config.forceEXP; }
			if ( config.forceIAT != null ) { forceIAT = config.forceIAT; }
			if ( config.forceNBT != null ) { forceNBT = config.forceNBT; }
		}
	}

	private final Config config = new Config ();
	private Clock clock;

	public JwtValidator (Config config) {
		this (Clock.systemUTC (), config);
	}

	public JwtValidator (Clock clock, Config config) {
		this.clock = clock;
		this.config.merge (config);
	}

	public boolean checkValid (JsonWebToken token) {
		return checkValid (token, this.config);
	}

	public boolean checkValid (JsonWebToken token, Config config) {
		try {
			assureValid (token, config);
			return true;
		} catch ( InvalidJwtException e ) {
			return false;
		}
	}

	public void assureValid (String header) {
		assureValid (header, config);
	}

	public void assureValid (String header, Config config) {
		if ( header == null || header.isEmpty () ) {
			if ( ! config.forceToken ) { return; }
			throw new InvalidJwtException ("No authorization token provided",
				objectNode ("token", header == null ? "missing" : header)
			);
		}

		JsonWebToken token;
		try { token = JsonWebToken.fromTokenString (header); }
		catch (Exception ignore) {
			throw new InvalidJwtException ("Failed to parse header " + header+ " as jwt.",
				objectNode ("token", header)
			);
		}

		assureValid (token, config);
	}

	public void assureValid (JsonWebToken token) {
		assureValid (token, this.config);
	}

	public void assureValid (JsonWebToken token, Config config) {
		if ( token == null && config.forceToken ) throw new InvalidJwtException (
			"Jwt token is empty or missing"
		);

		if ( token == null ) { return; }

		String authorization = token.getAs (JsonWebToken.Keys.Authentication, String.class);
		if ( ! config.validate.apply (token, authorization) ) throw new InvalidJwtException (
			"Token is not issued by a trusted source, validation does not match",
			objectNode ("token", token, "authorization", authorization)
		);

		Timed timed = getTimes (token);
		Instant now = clock.instant ();

		if ( config.forceEXP && timed.exp == null ) throw new InvalidJwtException (
			"No expiration time present",
			objectNode ("token", token)
		);

		if ( timed.exp != null && timed.exp.isBefore (now) ) throw new InvalidJwtException (
			"Expiration time is not valid", objectNode (
				"now", now,
				"exp", timed.exp,
				"token", token
			)
		);

		if ( config.forceIAT && timed.iat == null ) throw new InvalidJwtException (
			"No issued at time present",
			objectNode ("token", token)
		);

		if ( timed.iat != null && timed.iat.isAfter (now) ) throw new InvalidJwtException (
			"Issued at time is not valid", objectNode (
				"now", now,
				"iat", timed.iat,
				"token", token
			)
		);

		if ( config.forceNBT && timed.nbt == null ) throw new InvalidJwtException (
			"No not before time present",
			objectNode ("token", token)
		);

		if ( timed.nbt != null && timed.nbt.isAfter (now) ) throw new InvalidJwtException (
			"Not before time is not valid", objectNode (
				"now", now,
				"nbt", timed.nbt,
				"token", token
			)
		);
	}

	private Timed getTimes (JsonWebToken token) {
		return token.payloadAs (Timed.class);
	}

	public static class Timed {
		public Instant exp;
		public Instant iat;
		public Instant nbt;
	}

}
