package org.tutske.lib.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.tutske.lib.utils.Functions.*;

import org.junit.Test;
import org.mockito.InOrder;

import java.util.EnumSet;
import java.util.function.Consumer;
import java.util.function.Function;


public class APIChainTest {

	Consumer<String> notify = mock (Consumer.class);

	@Test
	public void it_should_not_give_a_filter_when_no_target_can_be_found () {
		ApiRouter<String, String> router = API.configure (api -> {
		});

		Function<String, String> chain = router.createChain (Request.Method.GET, "current", "/users", API.splitParts ("/users"));
		assertThat (chain, nullValue ());
	}

	@Test
	public void it_should_give_a_handler_when_no_filters_ar_present () {
		RiskyFn<String, String> handler = mock (RiskyFn.class);

		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("/users", handler);
		});

		Function<String, String> chain = router.createChain (Request.Method.GET, "current", "/users", API.splitParts ("/users"));
		chain.apply ("john");

		verify (handler).apply ("john");
	}

	@Test
	public void it_should_call_registered_filters_that_match () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("/users", EnumSet.of (Request.Method.POST), name -> name);
			api.filter ("/::path", filter (notify));
		});

		Function<String, String> chain = router.createChain (Request.Method.POST, "current", "/users", API.splitParts ("/users"));
		chain.apply ("john");

		verify (notify).accept ("john");
	}

	@Test
	public void it_should_not_call_filters_that_dont_match () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.route ("/users", EnumSet.of (Request.Method.POST), name -> name);
			api.filter ("/api/::path", filter (notify));
		});

		Function<String, String> chain = router.createChain (Request.Method.POST, "current", "/users", API.splitParts ("/users"));
		chain.apply ("john");

		verify (notify, times (0)).accept ("john");
	}

	@Test
	public void it_should_call_filter_inside_a_group () {
		ApiRouter<String, String> router = API.configure (base -> {
			base.group ("/api", api -> {
				api.filter ("/::path", filter (notify));
				api.route ("/users", EnumSet.of (Request.Method.POST), name -> name);
			});
		});

		Function<String, String> chain = router.createChain (Request.Method.POST, "current", "/api/users", API.splitParts ("/api/users"));
		chain.apply ("john");

		verify (notify).accept ("john");
	}

	@Test
	public void it_should_not_match_a_filter_in_a_different_group () {
		ApiRouter<String, String> router = API.configure (base -> {
			base.group ("/api", api -> {
				api.filter ("/::path", filter (notify));
			});
			base.group ("/admin", admin -> {
				admin.route ("/users", EnumSet.of (Request.Method.POST), name -> name);
			});
		});

		Function<String, String> chain = router.createChain (Request.Method.POST, "current", "/admin/users", API.splitParts ("/admin/users"));
		chain.apply ("john");

		verify (notify, times (0)).accept ("john");
	}

	@Test
	public void it_should_filter_on_the_root_path () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.filter ("/::path", filter (notify));
			api.route ("/", name -> name);
		});

		Function<String, String> chain = router.createChain (Request.Method.GET, "current", "/", API.splitParts ("/"));
		chain.apply ("john");

		verify (notify).accept ("john");
	}

	@Test
	public void it_should_call_multiple_filters_in_order () {
		ApiRouter<String, String> router = API.configure (base -> {
			base.filter ("/::path", filter (name -> notify.accept ("first")));
			base.filter ("/api/::path", filter (name -> notify.accept ("second")));

			base.group ("/api", api -> {
				api.filter ("/::path", filter (name -> notify.accept ("third")));
				api.filter ("/users/::path", filter (name -> notify.accept ("forth")));

				api.route ("/users", name -> name);
			});
		});

		InOrder ordered = inOrder (notify);
		Function<String, String> chain = router.createChain (Request.Method.GET, "current", "/api/users", API.splitParts ("/api/users"));
		chain.apply ("john");

		ordered.verify (notify).accept ("first");
		ordered.verify (notify).accept ("second");
		ordered.verify (notify).accept ("third");
		ordered.verify (notify).accept ("forth");
	}

	@Test
	public void it_should_only_match_filters_with_tail_if_first_part_matches () {
		ApiRouter<String, String> router = API.configure (api -> {
			api.filter ("/api/users/:game/::path", filter (notify));
			api.route ("/api/users", name -> name);
		});

		Function<String, String> chain = router.createChain (Request.Method.GET, "current", "/api/users", API.splitParts ("/api/users"));
		chain.apply ("john");

		verify (notify, times (0)).accept ("john");
	}

	private <REQ, RES> Filter<REQ, RES> filter (Consumer<REQ> consumer) {
		return (req, chain) -> {
			consumer.accept (req);
			return chain.apply (req);
		};
	}

}
