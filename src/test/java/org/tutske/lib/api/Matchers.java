package org.tutske.lib.api;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;


public class Matchers {

	public static Matcher<Client.Response> isSuccess () {
		return new BaseMatcher<> () {
			@Override public boolean matches (Object item) {
				if ( ! (item instanceof Client.Response) ) { return false; }
				Client.Response response = (Client.Response) item;
				return response.isOk () && response.status () >= 200 && response.status () < 300;
			}

			@Override
			public void describeTo (Description description) {
				description.appendText ("a successful success full reply to the request");
			}
		};
	}

}
