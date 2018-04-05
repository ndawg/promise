package ndawg.promise;

import ndawg.promise.internal.PromiseScheduler;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PromiseSchedulerTests {
	
	@Test
	public void shutdownRunnable() {
		ExecutorService dummy = Executors.newSingleThreadExecutor();
		PromiseScheduler scheduler = new PromiseScheduler(dummy);
		scheduler.schedule(() -> {}, 5, TimeUnit.MINUTES);
		scheduler.shutdown();
		assert dummy.isShutdown();
	}
	
	@Test
	public void shutdownCallable() {
		ExecutorService dummy = Executors.newSingleThreadExecutor();
		PromiseScheduler scheduler = new PromiseScheduler(dummy);
		scheduler.schedule(() -> 10, 5, TimeUnit.MINUTES);
		scheduler.shutdown();
		assert dummy.isShutdown();
	}
	
}