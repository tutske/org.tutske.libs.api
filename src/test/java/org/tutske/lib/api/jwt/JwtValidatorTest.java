package org.tutske.lib.api.jwt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.tutske.lib.api.exceptions.InvalidJwtException;
import org.tutske.lib.json.Json;
import org.tutske.lib.json.Mappers;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;


public class JwtValidatorTest {

	private static final ObjectMapper mapper = Mappers.mapper (m -> {
		Mappers.serialize (m, Instant.class, (v, g, provider) -> {
			g.writeNumber (v.getEpochSecond ());
		});
	});

	private static Instant now = Instant.ofEpochMilli (1_000_000_000L);
	private static Instant yesterday = now.minus (1, ChronoUnit.DAYS);
	private static Instant tomorrow = now.plus (1, ChronoUnit.DAYS);

	private static JsonWebToken createToken (String sub) {
		return createToken (sub, yesterday, tomorrow);
	}

	private static JsonWebToken createToken (String sub, Instant iat, Instant exp) {
		return createToken (sub, Json.objectNode ("iat", iat, "exp", exp));
	}

	private static JsonWebToken createToken (String sub, ObjectNode data) {
		return JsonWebToken.fromMappedData (mapper,
			Json.purgeNulls (
				Json.merge (
					Json.objectNode ("sub", sub),
					Json.objectNode ("iat", yesterday, "exp", tomorrow),
					data
				)
			)
		);
	}

	private Clock clock = when (mock (Clock.class).instant ()).thenReturn (now).getMock ();
	private JwtValidator.Config base = new JwtValidator.Config () {{
		validate = (jwt, hash) -> true;
	}};

	@Test
	public void it_should_call_the_rest_of_the_chain_if_the_token_in_not_valid () throws Exception {
		JsonWebToken token = createToken ("john.doe@example.com");
		JwtValidator validator = new JwtValidator (clock, new JwtValidator.Config () {{
			validate = (jwt, hash) -> true;
		}});

		validator.assureValid (token);
	}

	@Test
	public void it_should_validate_missing_tokens () {
		new JwtValidator (clock, new JwtValidator.Config ()).assureValid ((JsonWebToken) null);
	}

	@Test
	public void it_should_reject_tokens_when_validate_method_fails () {
		JwtValidator.Config config = new JwtValidator.Config () {{
			validate = (jwt, hash) -> false;
			forceToken = false;
		}};

		JsonWebToken token = createToken ("john");

		assertThrows (InvalidJwtException.class, () -> {
			new JwtValidator (clock, config).assureValid (token);
		});
	}

	@Test
	public void it_should_nat_validate_missing_tokens_in_strict_mode () {
		JwtValidator.Config config = new JwtValidator.Config () {{
			validate = (jwt, hash) -> true;
			forceToken = true;
		}};
		assertThrows (InvalidJwtException.class, () -> {
			new JwtValidator (clock, config).assureValid ((JsonWebToken) null);
		});
	}

	@Test
	public void it_should_nat_validate_missing_string_tokens_in_strict_mode () {
		JwtValidator.Config config = new JwtValidator.Config (base) {{ forceToken = true; }};
		assertThrows (InvalidJwtException.class, () -> {
			new JwtValidator (clock, config).assureValid ((String) null);
		});
	}

	@Test
	public void it_should_reject_tokens_with_an_invalid_exp_time () {
		JsonWebToken token = createToken ("john", yesterday, yesterday);
		assertThrows (InvalidJwtException.class, () -> {
			new JwtValidator (clock, base).assureValid (token);
		});
	}

	@Test
	public void it_should_reject_tokens_with_an_invalid_iat_time () {
		JsonWebToken token = createToken ("john", tomorrow, tomorrow);
		assertThrows (InvalidJwtException.class, () -> {
			new JwtValidator (clock, base).assureValid (token);
		});
	}

	@Test
	public void it_should_reject_tokens_with_an_invalid_nbt_time () {
		JsonWebToken token = createToken ("john", Json.objectNode ("nbt", tomorrow));
		assertThrows (InvalidJwtException.class, () -> {
			new JwtValidator (clock, base).assureValid (token);
		});
	}

	@Test
	public void it_should_reject_tokens_without_an_exp_time () {
		JsonWebToken token = createToken ("john", Json.objectNode ("exp", null));
		assertThrows (InvalidJwtException.class, () -> {
			new JwtValidator (clock, base).assureValid (token);
		});
	}

	@Test
	public void it_should_accept_tokens_without_an_exp_time_in_lenient_mode () {
		JsonWebToken token = createToken ("john", Json.objectNode ("exp", null));
		JwtValidator.Config config = new JwtValidator.Config (base) {{ forceEXP = false; }};
		new JwtValidator (clock, config).assureValid (token);
	}

	@Test
	public void it_should_accept_tokens_without_an_iat_time () {
		JsonWebToken token = createToken ("john", Json.objectNode ("iat", null));
		new JwtValidator (clock, base).assureValid (token);
	}

	@Test
	public void it_reject_reject_tokens_without_an_iat_time_in_force_mode () {
		JsonWebToken token = createToken ("john", Json.objectNode ("iat", null));
		JwtValidator.Config config = new JwtValidator.Config (base) {{ forceIAT = true; }};
		assertThrows (InvalidJwtException.class, () -> {
			new JwtValidator (clock, config).assureValid (token);
		});
	}

	@Test
	public void it_reject_reject_tokens_with_an_invalid_iat_time_in_force_mode () {
		JsonWebToken token = createToken ("john", Json.objectNode ("iat", tomorrow));
		JwtValidator.Config config = new JwtValidator.Config (base) {{ forceIAT = true; }};
		assertThrows (InvalidJwtException.class, () -> {
			new JwtValidator (clock, config).assureValid (token);
		});
	}

	@Test
	public void it_should_accept_tokens_without_an_nbt_time () {
		JsonWebToken token = createToken ("john", Json.objectNode ("nbt", null));
		new JwtValidator (clock, base).assureValid (token);
	}

	@Test
	public void it_should_accept_tokens_with_a_valid_nbt_time () {
		JsonWebToken token = createToken ("john", Json.objectNode ("nbt", yesterday));
		new JwtValidator (clock, base).assureValid (token);
	}

	@Test
	public void it_should_reject_tokens_without_an_nbt_time_in_force_mode () {
		JsonWebToken token = createToken ("john", Json.objectNode ("nbt", null));
		JwtValidator.Config config = new JwtValidator.Config (base) {{ forceNBT = true; }};
		assertThrows (InvalidJwtException.class, () -> {
			new JwtValidator (clock, config).assureValid (token);
		});
	}

	@Test
	public void it_should_reject_tokens_with_an_invalid_nbt_time_in_force_mode () {
		JsonWebToken token = createToken ("john", Json.objectNode ("nbt", tomorrow));
		JwtValidator.Config config = new JwtValidator.Config (base) {{ forceNBT = true; }};
		assertThrows (InvalidJwtException.class, () -> {
			new JwtValidator (clock, config).assureValid (token);
		});
	}

	@Test
	public void it_should_not_validate_any_tokens_with_the_default_configuration () {
		assertThrows (InvalidJwtException.class, () -> {
			new JwtValidator (clock, new JwtValidator.Config ()).assureValid (createToken ("john"));
		});
	}

	@Test
	public void it_should_not_merge_in_config_values_when_set_to_null () {
		JwtValidator.Config modifications = new JwtValidator.Config () {{
			validate = null;
			strict = null;
			forceToken = null;
			forceEXP = null;
			forceIAT = null;
			forceNBT = null;
		}};

		JwtValidator.Config result = new JwtValidator.Config (modifications) {{
			strict = ! strict;
			forceToken = ! forceToken;
			forceEXP = ! forceEXP;
			forceIAT = ! forceIAT;
			forceNBT = ! forceNBT;
		}};

		assertThat (result.strict, is (! new JwtValidator.Config ().strict));
	}

	@Test
	public void it_should_check_if_a_token_is_valid () {
		boolean valid = new JwtValidator (clock, base).checkValid (createToken ("john"));
		assertThat (valid, is (true));
	}

	@Test
	public void it_should_check_if_a_token_is_invalid () {
		boolean valid = new JwtValidator (clock, base).checkValid (createToken ("john", yesterday, yesterday));
		assertThat (valid, is (false));
	}

	@Test
	public void it_should_not_validate_empty_representations_in_force_mode () {
		JwtValidator.Config config = new JwtValidator.Config (base) {{ forceToken = true; }};
		assertThrows (InvalidJwtException.class, () -> {
			new JwtValidator (clock, config).assureValid ("");
		});
	}

	@Test
	public void it_should_not_validate_null_representations_in_force_mode () {
		JwtValidator.Config config = new JwtValidator.Config (base) {{ forceToken = true; }};
		assertThrows (InvalidJwtException.class, () -> {
			new JwtValidator (clock, config).assureValid ((String) null);
		});
	}

}
