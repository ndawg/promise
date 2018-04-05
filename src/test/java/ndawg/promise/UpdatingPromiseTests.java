package ndawg.promise;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class UpdatingPromiseTests {
	
	@Test
	public void basicReceive() {
		AtomicBoolean got = new AtomicBoolean();
		
		UpdatingPromise<Object, String> promise = UpdatingPromise.empty(String.class).receive(str -> {
			System.out.println("Got: " + str);
			got.set(true);
		});
		promise.attempt(pr -> {
			pr.push("test");
			return "";
		});
		promise.join();
		
		assert got.get();
	}
	
}