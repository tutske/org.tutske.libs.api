package org.tutske.lib.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.tutske.lib.api.data.EmptyResponse;
import org.tutske.lib.api.exceptions.NotFoundException;
import org.tutske.lib.json.Json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;


public class DirectClientTest {

	@Test
	public void it_should_handle_api_which_reply_with_null () throws Exception {
		Client client = DirectClient.fromApi (api -> {
			api.route ("/test", req -> null);
		});

		Client.Response response = client.request (Method.GET, "/test").send ().get ();
		assertThat (response.isServerError (), is (true));
	}

	@Test
	public void it_should_create_requests_for_the_api () throws Exception {
		Client client = DirectClient.fromApi (api -> api.route ("/test", req -> {
			return req.reply (Json.objectNode ("status", "ok"));
		}));

		Client.Request request = client.request (Method.GET, "/test");
		assertThat (request, not (nullValue ()));
	}

	@Test
	public void it_should_send_requests_to_the_api () throws Exception {
		Client client = DirectClient.fromApi (api -> api.route ("/test", req -> {
			return req.reply (Json.objectNode ("status", "nok"));
		}));

		CompletableFuture<Client.Response> future = client.request (Method.GET, "/test").send ();
		assertThat (future, not (nullValue ()));
	}

	@Test
	public void it_should_use_an_api_to_get_a_response () throws Exception {
		Client client = DirectClient.fromApi (api -> api.route ("/test", req -> {
			return req.reply (Json.objectNode ("status", "ok"));
		}));

		Client.Response response = client.request (Method.GET, "/test").send ().get ();
		assertThat (response.status (), is (200));
	}

	@Test
	public void it_should_show_the_response_status () throws Exception {
		Client client = DirectClient.fromApi (api -> {
			api.route ("/test", req -> req.reply (
				417, Json.objectNode ("status", "nok")
			));
		});

		Client.Response response = client.request (Method.GET, "/test").send ().get ();
		assertThat (response.status (), is (417));
	}

	@Test
	public void it_should_produce_the_response_body () throws Exception {
		Client client = DirectClient.fromApi (api -> {
			api.route ("/test", req -> req.reply (
				Json.objectNode ("status", "ok")
			));
		});

		Client.Response response = client.request (Method.GET, "/test").send ().get ();
		assertThat (response.json ().get ("status"), is (Json.valueOf ("ok")));
	}

	@Test
	public void it_should_produce_the_response_body_from_the_request () throws Exception {
		Client client = DirectClient.fromApi (api -> api.route ("/test", req -> {
			return req.reply (Json.objectNode ("original", req.json ()));
		}));

		JsonNode payload = Json.objectNode ("key", "value");
		Client.Response response = client.request (Method.GET, "/test").send (payload).get ();

		assertThat (response.status (), is (200));
		assertThat (response.json ().path ("original").path ("key"), is (Json.valueOf ("value")));
	}

	@Test
	public void it_should_feed_bytes_from_the_client_to_the_api () throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream ();

		Client client = DirectClient.fromApi (api -> api.route ("/test", req -> {
			try ( InputStream in = req.inputstream () ) { in.transferTo (out); }
			return req.reply (200, EmptyResponse.getInstance ());
		}));

		ByteArrayInputStream in = new ByteArrayInputStream ("This Is The Content".getBytes ());
		Client.Response response = client.request (Method.GET, "/test").send (in).get ();

		assertThat (response.status (), is (200));
		assertThat (out.toByteArray (), is ("This Is The Content".getBytes ()));
	}

	@Test
	public void it_should_feed_bytes_from_the_api_to_the_client () throws Exception {
		Client client = DirectClient.fromApi (api -> api.route ("/test", req -> {
			InputStream in = new ByteArrayInputStream ("This Is The Response Payload".getBytes ());
			return req.reply (200, in);
		}));

		Client.Response response = client.request (Method.GET, "/test").send ().get ();

		assertThat (response.status (), is (200));
		assertThat (response.inputstream ().readAllBytes (), is ("This Is The Response Payload".getBytes ()));
	}

	@Test
	public void it_should_handle_exceptions_in_the_api () throws Exception {
		Client client = DirectClient.fromApi (api -> api.route ("/test", req -> {
			throw new NotFoundException ();
		}));

		Client.Response response = client.request (Method.GET, "/test").send ().get ();

		assertThat (response.status (), is (NotFoundException.STATUS_CODE));
		assertThat (response.json ().get ("title"), is (Json.valueOf ("Not Found")));
	}

}
