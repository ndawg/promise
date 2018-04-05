package ndawg.promise.internal;

import ndawg.promise.Promise;
import ndawg.promise.PromiseBuilder;
import ndawg.promise.impl.DefaultPromiseBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

public class PromiseUtil {
	
	private static ExecutorService common;
	private static PromiseScheduler scheduler;
	private static PromiseBuilder builder;
	static {
		// Determine which executor to use based on parallelism
		if (ForkJoinPool.getCommonPoolParallelism() > 1) {
			common = ForkJoinPool.commonPool();
		} else {
			common = Executors.newCachedThreadPool();
		}
		scheduler = new PromiseScheduler(common);
		builder = new DefaultPromiseBuilder(common);
	}
	
	/**
	 * @return The common pool that async operations are submitted to unless
	 * another Executor is specified. In most situations this is the
	 * {@link ForkJoinPool#commonPool()}, unless parallelism is not supported,
	 * in which case a cached thread pool is used.
	 */
	public static ExecutorService getExecutor() {
		return common;
	}
	
	/**
	 * @return A ScheduledExecutorService that routes it's final execution requests back
	 * to {@link #getExecutor()}.
	 */
	public static PromiseScheduler getScheduler() {
		return scheduler;
	}
	
	/**
	 * @return The default PromiseBuilder instance, which uses {@link #getExecutor()}.
	 */
	public static PromiseBuilder getBuilder() {
		return builder;
	}
	
	/**
	 * Attempts to coerce every Object in the given Collection to a Promise
	 * instance. If an item cannot be converted, an error is thrown.
	 *
	 * @param objects The objects to convert.
	 * @return An array of the CompletableFuture instances, if successful.
	 */
	public static CompletableFuture<?>[] toFutures(Collection<?> objects) {
		final List<CompletableFuture> futures = new ArrayList<>();
		objects.forEach(it -> {
			if (it instanceof CompletableFuture) {
				futures.add((CompletableFuture) it);
			} else if (it instanceof Promise) {
				futures.add(((Promise) it).toCompletableFuture());
			} else if (it instanceof Supplier) {
				futures.add(Promise.of((Supplier<?>) it).toCompletableFuture());
			} else if (it instanceof Runnable) {
				futures.add(Promise.run((Runnable) it).toCompletableFuture());
			} else {
				throw new IllegalArgumentException("Given object of type " + it.getClass() + " cannot be converted to Promise");
			}
		});
		return futures.toArray(new CompletableFuture[futures.size()]);
	}
	
}
