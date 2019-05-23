package org.tutske.lib.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.tutske.lib.utils.Functions.*;
import static org.tutske.lib.api.Request.Method.*;

import org.junit.Test;

import java.util.EnumSet;


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
	public void it_should_understard_groups_with_just_a_root_path () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.group ("/", root -> {
				root.route ("user", "/users/:id", name -> name);
			});
		});

		String id = router.toId (GET, "current", "/users/1", API.splitParts ("/users/1"));
		assertThat (id, is ("user"));
	}

}
