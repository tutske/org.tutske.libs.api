package org.tutske.lib.api.jwt;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.tutske.lib.api.exceptions.InvalidJwtException;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;


public class JwtValidatorStringTest {

	private static Instant now = Instant.ofEpochMilli (1_000_000_000L);
	private static Instant yesterday = now.minus (1, ChronoUnit.DAYS);
	private static Instant tomorrow = now.plus (1, ChronoUnit.DAYS);

	private static class Auth {
		public String sub;
		public Instant iat;
		public Instant exp;
		public Instant nbt;

		public Auth (String sub) {
			this (sub, yesterday, tomorrow);
		}

		public Auth (String sub, Instant iat, Instant exp) {
			this.sub = sub;
			this.iat = iat;
			this.exp = exp;
		}
	}

	private Clock clock = when (mock (Clock.class).instant ()).thenReturn (now).getMock ();
	private JwtValidator.Config base = new JwtValidator.Config () {{
		validate = (jwt, hash) -> true;
	}};

	@Test
	public void it_should_call_the_rest_of_the_chain_if_the_token_in_not_valid () throws Exception {
		JsonWebToken token = JsonWebToken.fromData (new Auth ("john.doe@example.com"));
		JwtValidator validator = new JwtValidator (clock, new JwtValidator.Config () {{
			validate = (jwt, hash) -> true;
		}});

		validator.assureValid (token.toString ());
	}

	@Test
	public void it_should_validate_missing_string_tokens () {
		new JwtValidator (clock, new JwtValidator.Config ()).assureValid ((String) null);
	}

	@Test (expected = InvalidJwtException.class)
	public void it_should_reject_tokens_when_validate_method_fails () {
		JwtValidator.Config config = new JwtValidator.Config () {{
			validate = (jwt, hash) -> false;
			forceToken = false;
		}};
		JsonWebToken token = JsonWebToken.fromData (new Auth ("john"));
		new JwtValidator (clock, config).assureValid (token.toString ());
	}

	@Test (expected = InvalidJwtException.class)
	public void it_should_nat_validate_missing_string_tokens_in_strict_mode () {
		JwtValidator.Config config = new JwtValidator.Config (base) {{ forceToken = true; }};
		new JwtValidator (clock, config).assureValid ((String) null);
	}

	@Test (expected = InvalidJwtException.class)
	public void it_should_reject_tokens_with_an_invalid_exp_time () {
		JsonWebToken token = JsonWebToken.fromData (new Auth ("john", yesterday, yesterday));
		new JwtValidator (clock, base).assureValid (token.toString ());
	}

	@Test (expected = InvalidJwtException.class)
	public void it_should_reject_tokens_with_an_invalid_iat_time () {
		JsonWebToken token = JsonWebToken.fromData (new Auth ("john", tomorrow, tomorrow));
		new JwtValidator (clock, base).assureValid (token.toString ());
	}

	@Test (expected = InvalidJwtException.class)
	public void it_should_reject_tokens_with_an_invalid_nbt_time () {
		Auth auth = new Auth ("john");
		auth.nbt = tomorrow;
		new JwtValidator (clock, base).assureValid (JsonWebToken.fromData (auth).toString ());
	}

	@Test (expected = InvalidJwtException.class)
	public void it_should_reject_tokens_without_an_exp_time () {
		Auth auth = new Auth ("john");
		auth.exp = null;
		new JwtValidator (clock, base).assureValid (JsonWebToken.fromData (auth).toString ());
	}

	@Test
	public void it_should_accept_tokens_without_an_exp_time_in_lenient_mode () {
		Auth auth = new Auth ("john");
		auth.exp = null;
		JwtValidator.Config config = new JwtValidator.Config (base) {{ forceEXP = false; }};
		new JwtValidator (clock, config).assureValid (JsonWebToken.fromData (auth).toString ());
	}

	@Test
	public void it_should_reject_tokens_without_an_iat_time () {
		Auth auth = new Auth ("john");
		auth.iat = null;
		new JwtValidator (clock, base).assureValid (JsonWebToken.fromData (auth).toString ());
	}

	@Test (expected = InvalidJwtException.class)
	public void it_should_accept_tokens_without_an_iat_time_in_force_mode () {
		Auth auth = new Auth ("john");
		auth.iat = null;
		JwtValidator.Config config = new JwtValidator.Config (base) {{ forceIAT = true; }};
		new JwtValidator (clock, config).assureValid (JsonWebToken.fromData (auth).toString ());
	}

	@Test
	public void it_should_reject_tokens_without_an_nbt_time () {
		Auth auth = new Auth ("john");
		auth.nbt = null;
		new JwtValidator (clock, base).assureValid (JsonWebToken.fromData (auth).toString ());
	}

	@Test (expected = InvalidJwtException.class)
	public void it_should_accept_tokens_without_an_nbt_time_in_force_mode () {
		Auth auth = new Auth ("john");
		auth.nbt = null;
		JwtValidator.Config config = new JwtValidator.Config (base) {{ forceNBT = true; }};
		new JwtValidator (clock, config).assureValid (JsonWebToken.fromData (auth).toString ());
	}

	@Test (expected = InvalidJwtException.class)
	public void it_should_fail_on_an_unparsable_token () {
		new JwtValidator (clock, base).assureValid ("not.a.valid.token");
	}

}
