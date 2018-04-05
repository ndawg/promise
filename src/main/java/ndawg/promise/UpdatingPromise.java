package ndawg.promise;

import ndawg.promise.impl.DefaultUpdatingPromise;
import ndawg.promise.internal.PromiseUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A Promise that can receive status updates as it is completing.
 *
 * @param <T> The result type of the Promise.
 * @param <I> The information type being received.
 */
public interface UpdatingPromise<T, I> extends Promise<T> {
	
	/**
	 * @param updates The type of updates to receive.
	 * @return An empty, incomplete UpdatingPromise. This Promise
	 * can be resolved by using {@link Promise#resolve} or {@link Promise#reject}.
	 */
	static <T, I> UpdatingPromise<T, I> empty(Class<I> updates) {
		return new DefaultUpdatingPromise<>(new CompletableFuture<T>(), PromiseUtil.getExecutor());
	}
	
	/**
	 * Creates a new UpdatingPromise instance out of the given Supplier.
	 * The Supplier will begin being executed immediately with the given Executor.
	 *
	 * @param supplier A function that produces the value of this Promise.
	 * @param updates The type of updates to receive.
	 * @return A new UpdatingPromise instance.
	 */
	static <T, I> UpdatingPromise<T, I> of(Supplier<T> supplier, Class<I> updates) {
		return of(supplier, PromiseUtil.getExecutor(), updates);
	}
	
	/**
	 * Creates a new UpdatingPromise instance out of the given Supplier.
	 * The Supplier will begin being executed immediately with the given Executor.
	 *
	 * @param supplier A function that produces the value of this Promise.
	 * @param executor The Executor to use for async operations.
	 * @param updates The type of updates to receive.
	 * @return A new UpdatingPromise instance.
	 */
	static <T, I> UpdatingPromise<T, I> of(Supplier<T> supplier, Executor executor, Class<I> updates) {
		return new DefaultUpdatingPromise<>(CompletableFuture.supplyAsync(supplier), executor);
	}
	
	/**
	 * Creates a new UpdatingPromise instance out of the given Supplier.
	 * The Supplier will begin being executed immediately with the given Executor.
	 *
	 * @param runnable A Runnable that performs the operations of this Promise.
	 * @param executor The Executor to use for async operations.
	 * @param updates The type of updates to receive.
	 * @return A new UpdatingPromise instance.
	 */
	static <I> UpdatingPromise<Void, I> of(Runnable runnable, Executor executor, Class<I> updates) {
		return new DefaultUpdatingPromise<>(CompletableFuture.runAsync(runnable, executor), executor);
	}
	
	/**
	 * Creates a new UpdatingPromise instance out of the given Supplier.
	 * The Supplier will begin being executed immediately.
	 *
	 * @param runnable A Runnable that performs the operations of this Promise.
	 * @param updates The type of updates to receive.
	 * @return A new UpdatingPromise instance.
	 */
	static <I> UpdatingPromise<Void, I> of(Runnable runnable, Class<I> updates) {
		return new DefaultUpdatingPromise<>(CompletableFuture.runAsync(runnable), PromiseUtil.getExecutor());
	}
	
	/**
	 * Registers a Consumer to receive the information from the process of this Promise.
	 *
	 * @param consumer The Consumer to receive information.
	 * @return This instance for chaining.
	 */
	UpdatingPromise<T, I> receive(Consumer<I> consumer);
	
	/**
	 * A convenience method that executes the given Consumer in an attempt to
	 * resolve this Promise instance. If an exception is encountered within the
	 * Callable, the Promise fails. Otherwise the returned value is used to complete it.
	 *
	 * @param function The Consumer that should return the value of this Promise,
	 * and receives this object for convenience.
	 */
	void attempt(Function<UpdatingPromise<T, I>, T> function);
	
	/**
	 * Pushes information to this Promise, alerting any registered Consumers.
	 *
	 * @param info The information to push.
	 */
	void push(I info);
	
	/**
	 * @return The last update pushed, if there is one. Otherwise null.
	 */
	I getLastUpdate();
}
