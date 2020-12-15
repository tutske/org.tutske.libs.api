package org.tutske.lib.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.tutske.lib.api.Matchers.isSuccess;
import static org.tutske.lib.api.Method.GET;
import static org.tutske.lib.utils.Functions.fn;

import org.junit.Test;
import org.tutske.lib.api.data.RawResponse;
import org.tutske.lib.api.data.StringResponse;
import org.tutske.lib.json.Json;

import java.util.EnumSet;
import java.util.function.Function;


public abstract class RoundTripTest {

	public static class DirectClientRoundTripTest extends RoundTripTest {
		@Override Client createClientFor (String path, API.Producer<? extends Request> api) throws Exception {
			return Utils.createClientFor (path, api);
		}
	}

	public static class ProxyClientRoundTripTest extends RoundTripTest {
		private final Function<Client, API.Producer<?>> proxy  = client -> api -> {
			api.route ("/::path", EnumSet.of (GET), req -> client
				.request (req.method (), req.uri ())
				.withQuery (req.queryParams ())
				.withHeaders (req.headers ())
				.thenCompose (fn (res -> {
					System.out.println ("proxy for client: " + client + " (" + req.method () + " " + req.uri () + ")");
					return req.reply (res.status (), res.headers (), res.inputstream ());
				}))
			);
		};

		@Override public Client createClientFor (String path, API.Producer<? extends Request> api) throws Exception {
			Client pClient = Utils.createClientFor (api);
			Client client = Utils.createClientFor (path, proxy.apply (pClient));
			return Utils.closableClient (client, pClient);
		};
	}

	abstract Client createClientFor (String path, API.Producer<? extends Request> api) throws Exception;
	protected Client createClientFor (API.Producer<? extends Request> api) throws Exception {
		return createClientFor ("", api);
	}

	@Test
	public void it_should_call_on_an_http_api () throws Exception {
		API.Producer<Request> server = api -> api.route ("/", EnumSet.of (GET), req -> req.reply (
			Json.objectNode ("status", "ok")
		));

		try ( Client client = createClientFor (server) ) {
			Client.Response response = client.request (GET, "/").send ().get ();

			assertThat (response, isSuccess ());
			assertThat (response.json ().get ("status").asText (), is ("ok"));
		}
	}

	@Test
	public void it_should_handle_string_responses () throws Exception {
		API.Producer<Request> server  = api -> api.route ("/", EnumSet.of (GET), req -> req.reply (
			new StringResponse ("test")
		));

		try ( Client client = createClientFor (server) ) {
			Client.Response response = client.request (GET, "/").send ().get ();

			assertThat (response, isSuccess ());
			assertThat (response.body (), is ("test"));
		}
	}

	@Test
	public void it_should_handle_raw_responses () throws Exception {
		API.Producer<Request> server  = api -> api.route ("/", EnumSet.of (GET), req -> req.reply (
			RawResponse.of ("This is the text".getBytes ())
		));

		try ( Client client = createClientFor (server) ) {
			Client.Response response = client.request (GET, "/").send ().get ();

			assertThat (response, isSuccess ());
			assertThat (response.body (), is ("This is the text"));
		}
	}

	@Test
	public void it_should_allow_query_keys_with_special_characters () throws Exception {
		API.Producer<Request> server  = api -> api.route ("/", EnumSet.of (GET), req -> req.reply (
			Json.objectNode (req.queryParams ())
		));

		try ( Client client = createClientFor (server) ) {
			Client.Response response = client.request (GET, "/").withQuery ("eq=ual", "value").send ().get ();

			assertThat (response, isSuccess ());
			assertThat (response.json ().get ("eq=ual"), is (Json.valueOf ("value")));
		}
	}

	@Test
	public void it_should_combine_multiple_sources_of_query_parameters () throws Exception {
		API.Producer<Request> server  = api -> api.route ("/query", EnumSet.of (GET), req -> req.reply (
			Json.objectNode (req.queryParams ())
		));

		try ( Client client = createClientFor ("?from_client=client", server) ) {
			Client.Response response = client.request (GET, "/query?from_request=request")
				.withQuery ("eq=ual", "value")
				.withQuery ("am&p", "special")
				.withQuery ("equal", "val=ue")
				.withQuery ("amp", "am&p")
				.send ().get ();

			assertThat (response, isSuccess ());
			assertThat (response.json ().get ("eq=ual"), is (Json.valueOf ("value")));
			assertThat (response.json ().get ("am&p"), is (Json.valueOf ("special")));
			assertThat (response.json ().get ("equal"), is (Json.valueOf ("val=ue")));
			assertThat (response.json ().get ("amp"), is (Json.valueOf ("am&p")));
			assertThat (response.json ().get ("from_client"), is (Json.valueOf ("client")));
			assertThat (response.json ().get ("from_request"), is (Json.valueOf ("request")));
		}
	}

	@Test
	public void it_should_combine_multiple_sources_of_query_parameters_2 () throws Exception {
		API.Producer<Request> server  = api -> api.route ("/query", EnumSet.of (GET), req -> req.reply (
			Json.objectNode (req.queryParams ())
		));

		try ( Client client = createClientFor ("?from_client=client", server) ) {
			Client.Response response = client.request (GET, "/query?eq=ual=s").send ().get ();

			assertThat (response, isSuccess ());
			assertThat (response.json ().get ("eq"), is (Json.valueOf ("ual=s")));
		}
	}

	@Test
	public void it_should_handle_clients_with_a_sub_path () throws Exception {
		API.Producer<Request> server  = api -> api.route ("/with/sub/path", EnumSet.of (GET), req -> req.reply (
			Json.objectNode ("status", "ok")
		));

		try ( Client client = createClientFor ("/with", server) ) {
			Client.Response response = client.request (GET, "/sub/path").send ().get ();

			assertThat (response, isSuccess ());
			assertThat (response.json ().get ("status"), is (Json.valueOf ("ok")));
		}
	}

	@Test
	public void it_should_handle_clients_with_a_sub_path_with_trailing_slash () throws Exception {
		API.Producer<Request> server  = api -> api.route ("/with/sub/path", EnumSet.of (GET), req -> req.reply (
			Json.objectNode ("status", "ok")
		));

		try ( Client client = createClientFor ("/with/", server) ) {
			Client.Response response = client.request (GET, "/sub/path").send ().get ();

			assertThat (response, isSuccess ());
			assertThat (response.json ().get ("status"), is (Json.valueOf ("ok")));
		}
	}

	@Test
	public void it_should_give_the_reply_code () throws Exception {
		API.Producer<Request> server  = api -> api.route ("/with/sub/path", EnumSet.of (GET), req -> req.reply (
			456, Json.objectNode ("status", "nok")
		));

		try ( Client client = createClientFor ("/with/", server) ) {
			Client.Response response = client.request (GET, "/sub/path").send ().get ();
			assertThat (response.status (), is (456));
			assertThat (response.isOk (), is (false));
			assertThat (response.isClientError (), is (true));
			assertThat (response.isServerError (), is (false));
		}
	}

	@Test
	public void it_should_provide_all_values_for_query_parameters_with_the_same_key () throws Exception {
		API.Producer<Request> server  = api -> api.route ("/", EnumSet.of (GET), req -> {
			return req.reply (Json.objectNode ("key", req.queryParams ().getAll ("key")));
		});

		try ( Client client = createClientFor (server) ) {
			Client.Response response = client.request (GET, "?key=one&key=two").send ().get ();
			assertThat (response.json ().get ("key"), is (Json.arrayNode ("one", "two")));
		}
	}

	@Test
	public void it_should_accept_special_encoded_characters_in_the_url_path () throws Exception {
		API.Producer<Request> server  = api -> api.route ("/:client/:request", EnumSet.of (GET), req -> {
			return req.reply (Json.objectNode (
				"client", req.pathParams ().get ("client"),
				"request", req.pathParams ().get ("request")
			));
		});

		try ( Client client = createClientFor ("/exc%21amation", server) ) {
			Client.Response response = client.request (GET, "/s%2Fash").send ().get ();
			assertThat (response.json ().get ("client"), is (Json.valueOf ("exc!amation")));
			assertThat (response.json ().get ("request"), is (Json.valueOf ("s/ash")));
		}
	}

}
