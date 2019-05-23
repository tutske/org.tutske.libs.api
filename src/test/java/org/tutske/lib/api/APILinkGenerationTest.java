package org.tutske.lib.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;


public class APILinkGenerationTest {

	@Test
	public void it_should_create_a_link_to_fixed_urls () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("users", "/users", name -> name);
		});

		String url = router.linkTo ("users", Collections.emptyMap ());
		assertThat (url, is ("/users"));
	}

	@Test
	public void it_should_create_a_link_to_urls_with_variable_parts () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("user", "/users/:id", name -> name);
		});

		String url = router.linkTo ("user", new HashMap<String, Object> () {{ put ("id", 1); }});
		assertThat (url, is ("/users/1"));
	}

	@Test
	public void it_should_create_a_link_to_a_url_with_a_tail () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("files", "/files/::path", name -> name);
		});

		String url = router.linkTo ("files", new HashMap<String, Object> () {{
			put ("path", Paths.get ("path/to/file.ext"));
		}});

		assertThat (url, is ("/files/path/to/file.ext"));
	}

	@Test
	public void it_only_prefix_tails_with_a_slash_when_not_yet_present_in_params () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("files", "/files/::path", name -> name);
		});

		String url = router.linkTo ("files", new HashMap<String, Object> () {{
			put ("path", Paths.get ("/path/to/file.ext"));
		}});

		assertThat (url, is ("/files/path/to/file.ext"));
	}

	@Test
	public void it_should_uri_encode_parts () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("user", "/users/:id", name -> name);
		});

		String url = router.linkTo ("user", new HashMap<String, Object> () {{ put ("id", "a/+b"); }});
		assertThat (url, is ("/users/a%2F%2Bb"));
	}

	@Test
	public void it_should_generate_url_when_created_with_groups () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.group ("/users", users -> {
				users.route ("users", "/", name -> name);
			});
		});

		String url = router.linkTo ("users", Collections.emptyMap ());
		assertThat (url, is ("/users"));
	}

	@Test
	public void it_should_create_a_link_to_urls_with_variable_parts_when_created_with_groups () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.group ("/users", users -> {
				users.route ("user", "/:id", name -> name);
			});
		});

		String url = router.linkTo ("user", new HashMap<String, Object> () {{ put ("id", 1); }});
		assertThat (url, is ("/users/1"));
	}

	@Test
	public void it_should_generate_a_link_when_variable_parts_in_group () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.group ("/users/:id", users -> {
				users.route ("ping-user", "/ping", name -> name);
			});
		});

		String url = router.linkTo ("ping-user", new HashMap<String, Object> () {{ put ("id", 1); }});
		assertThat (url, is ("/users/1/ping"));
	}

	@Test
	public void it_should_generate_a_link_when_varbiale_parts_in_both_group_and_route () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.group ("/users/:id", users -> {
				users.route ("game", "/:game", name -> name);
			});
		});

		String url = router.linkTo ("game", new HashMap<String, Object> () {{
			put ("id", 1); put ("game", "pong");
		}});
		assertThat (url, is ("/users/1/pong"));
	}

	@Test
	public void it_should_generate_a_link_with_a_tail_when_created_in_a_group () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.group ("/files/:section", section -> {
				section.route ("files", "/::path", name -> name);
			});
		});

		String url = router.linkTo ("files", new HashMap<String, Object> () {{
			put ("section", "books"); put ("path", "/path/to/file.ext");
		}});
		assertThat (url, is ("/files/books/path/to/file.ext"));

	}

}
