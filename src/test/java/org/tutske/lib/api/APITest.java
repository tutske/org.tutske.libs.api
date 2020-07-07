package org.tutske.lib.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.tutske.lib.utils.Functions.*;
import static org.tutske.lib.api.Method.*;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.EnumSet;
import java.util.function.Consumer;


public class APITest {

	@Test
	public void it_should_configure_apis () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.version ("a.1.0").route ("v1:users", "/users", name -> name);
		});

		String id = router.toId (GET, "a.1.0", "/users", new String [] { "users" });
		assertThat (id, is ("v1:users"));
	}

	@Test
	public void it_should_configure_apis_on_handlers () {
		Consumer<ApiRouter<String, String>> handler = mock (Consumer.class);
		API.configure (handler, api -> {
			api.version ("a.1.0").route ("v1:users", "/users", name -> name);
		});

		ArgumentCaptor<ApiRouter> captor = ArgumentCaptor.forClass (ApiRouter.class);
		verify (handler).accept (captor.capture ());
		ApiRouter<String, String> router = captor.getValue ();

		assertThat (router.toId (GET, "a.1.0", "/users", new String [] { "users" }), is ("v1:users"));
	}

	@Test
	public void it_should_make_a_distinction_between_routes () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("v1:users", "/users", name -> name);
			api.route ("v1:roles", "/roles", name -> name);
		});

		String id;
		id = router.toId (GET, "current", "/roles", new String [] { "roles" });
		assertThat (id, is ("v1:roles"));

		id = router.toId (GET, "current", "/users", new String [] { "users" });
		assertThat (id, is ("v1:users"));
	}

	@Test
	public void it_should_configure_groups () {
		ApiRouter<String, String> router = API.configure (base -> {
			base.group ("/api", api -> {
				api.version ("a.1.0").route ("a.1.0:users", "/users", name -> name);
			});
		});

		String id = router.toId (GET, "a.1.0", "/api/users", new String [] { "api", "users" });
		assertThat (id, is ("a.1.0:users"));
	}

	@Test
	public void it_should_configure_the_base_group_when_group_is_empty () {
		ApiRouter<String, String> router = API.configure (base -> {
			base.group ("/", api -> api.version ("a.1.0").route ("a.1.0:users", "/users", name -> name));
			base.group ("", api -> api.version ("a.1.0").route ("a.1.0:companies", "/companies", name -> name));
			base.group (null, api -> api.version ("a.1.0").route ("a.1.0:accounts", "/accounts", name -> name));
		});

		assertThat (router.toId (GET, "a.1.0", "/users", new String [] { "users" }), is ("a.1.0:users"));
		assertThat (router.toId (GET, "a.1.0", "/companies", new String [] { "companies" }), is ("a.1.0:companies"));
		assertThat (router.toId (GET, "a.1.0", "/accounts", new String [] { "accounts" }), is ("a.1.0:accounts"));
	}

	@Test
	public void it_should_configure_the_group_main_path_if_the_route_is_empty () {
		ApiRouter<String, String> router = API.configure (base -> {
			base.group ("/users", api -> api.version ("a.1.0").route ("a.1.0:users", "/", name -> name));
			base.group ("/companies", api -> api.version ("a.1.0").route ("a.1.0:companies", "", name -> name));
			base.group ("/accounts", api -> api.version ("a.1.0").route ("a.1.0:accounts", (String) null, name -> name));
		});

		assertThat (router.toId (GET, "a.1.0", "/users", new String [] { "users" }), is ("a.1.0:users"));
		assertThat (router.toId (GET, "a.1.0", "/companies", new String [] { "companies" }), is ("a.1.0:companies"));
		assertThat (router.toId (GET, "a.1.0", "/accounts", new String [] { "accounts" }), is ("a.1.0:accounts"));
	}

	@Test
	public void it_should_route_different_versions_of_the_same_endpoint () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.version ("a.1.0").route ("a.1.0:users", "/users", name -> name);
			api.version ("a.2.0").route ("a.2.0:users", "/users", name -> name);
		});

		String id;
		id = router.toId (GET, "a.1.0", "/users", new String [] { "users" });
		assertThat (id, is ("a.1.0:users"));

		id = router.toId (GET, "a.2.0", "/users", new String [] { "users" });
		assertThat (id, is ("a.2.0:users"));
	}

	@Test
	public void it_should_prefer_id_without_versions_if_version_is_not_found () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.version ("a.1.0").route ("a.1.0:users", "/users", name -> name);
			api.route ("users", "/users", name -> name);
		});

		String id = router.toId (GET, "a.2.0", "/users", new String [] { "users" });
		assertThat (id, is ("users"));
	}

	@Test
	public void it_should_prefer_id_without_version_when_defined_first () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("users", "/users", name -> name);
			api.version ("a.1.0").route ("a.1.0:users", "/users", name -> name);
		});

		String id = router.toId (GET, "a.2.0", "/users", new String [] { "users" });
		assertThat (id, is ("users"));
	}

	@Test
	public void it_should_prefer_id_with_specified_version_when_defined_first () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.version ("a.1.0").route ("a.1.0:users", "/users", name -> name);
			api.route ("users", "/users", name -> name);
		});

		String id = router.toId (GET, "a.1.0", "/users", new String [] { "users" });
		assertThat (id, is ("a.1.0:users"));
	}

	@Test
	public void it_should_prefer_id_with_specified_version_when_defined_last () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("users", "/users", name -> name);
			api.version ("a.1.0").route ("a.1.0:users", "/users", name -> name);
		});

		String id = router.toId (GET, "a.1.0", "/users", new String [] { "users" });
		assertThat (id, is ("a.1.0:users"));
	}

	@Test
	public void it_should_prefer_id_with_the_right_method () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("fetch-users", "/users", EnumSet.of (GET), name -> name);
			api.route ("create-users", "/users", EnumSet.of (POST), name -> name);
		});

		String id = router.toId (POST, "current", "/users", new String [] { "users" });
		assertThat (id, is ("create-users"));
	}

	@Test
	public void it_should_use_get_method_by_default () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("users", "/users", name -> name);
		});

		String id;
		id = router.toId (GET, "current", "/users", new String [] { "users" });
		assertThat (id, is ("users"));

		id = router.toId (POST, "current", "/users", new String [] { "users" });
		assertThat (id, nullValue ());
	}

	@Test
	public void it_should_route_urls_with_variables () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("user", "/users/:id", name -> name);
		});

		String id = router.toId (GET, "current", "/users/1", new String [] { "users", "1" });
		assertThat (id, is ("user"));
	}

	@Test
	public void it_should_route_descriptors_with_a_tail () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("files", "/files/::path", name -> name);
		});

		String id = router.toId (GET, "current", "/files/path/to/file.txt",
			API.splitParts ("/files/path/to/file.txt")
		);
		assertThat (id, is ("files"));
	}

	@Test
	public void it_should_list_all_identifiers_in_the_api () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("files", "/files/::path", name -> name);
			api.route ("users", "/users", name -> name);
			api.route ("roles", "/roles", name -> name);
		});

		assertThat (router.getIdentifiers (), containsInAnyOrder ("files", "users", "roles"));
	}

	@Test
	public void it_should_get_the_coresponding_handler_for_a_route () {
		RiskyFn<String, String> handler = name -> name;

		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("/files/::path", name -> name);
			api.route ("/users", name -> name);
			api.route ("/users/:id", handler);
			api.route ("/roles", name -> name);
		});

		String id = router.toId (GET, "current", "/users/1", API.splitParts ("/users/1"));
		assertThat (router.getHandler (id), is (handler));
	}

	@Test
	public void it_should_split_a_descriptor () {
		String [] parts = API.splitParts ("/path/to/file.ext");
		assertThat (parts, arrayContaining ("path", "to", "file.ext"));
	}


	@Test (expected = Exception.class)
	public void it_should_complain_when_it_ends_with_a_trailing_slash () {
		API.splitParts ("/path/to/dir/");
	}

	@Test
	public void it_should_not_complain_when_save_splitting_a_descriptor () {
		String [] parts = API.saveSplitParts ("/path/to/dir/");
		assertThat (parts, arrayContaining ("path", "to", "dir"));
	}

	@Test
	public void it_should_save_split_without_the_trailing_slash () {
		String [] parts = API.saveSplitParts ("/path/to/dir");
		assertThat (parts, arrayContaining ("path", "to", "dir"));
	}

	@Test
	public void it_should_split_the_root_in_an_array_with_empty_string () {
		String [] parts = API.saveSplitParts ("/");
		assertThat (parts, arrayContaining (""));
	}

}
