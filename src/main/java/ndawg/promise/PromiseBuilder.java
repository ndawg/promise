package ndawg.promise;

import ndawg.promise.internal.PromiseUtil;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides methods for launching many different types of Promises. Can be configured
 * to run Promises on a certain executor.
 */
public interface PromiseBuilder extends Cloneable {
	
	/**
	 * Sets the Executor for this PromiseBuilder to use when launching new Promises.
	 * By default, this is set to {@link PromiseUtil#getExecutor()}.
	 *
	 * @param executor The executor to use.
	 * @return A new PromiseBuilder instance configured to use the given executor.
	 */
	PromiseBuilder executor(Executor executor);
	
	/**
	 * Creates a Promise which has already been resolved to the given value.
	 *
	 * @param value The value to resolve the Promise with.
	 * @return The already resolved Promise.
	 */
	<T> Promise<T> value(T value);
	
	/**
	 * Creates a Promise which has already been rejected with the given value.
	 *
	 * @param error The value to reject the Promise with.
	 * @return The already rejected Promise.
	 */
	Promise<?> error(Throwable error);
	
	/**
	 * @return An empty and incomplete Promise. This Promise can be resolved by
	 * using {@link Promise#resolve} or {@link Promise#reject}.
	 */
	<T> Promise<T> empty();
	
	/**
	 * Creates a new Promise instance out of the given Supplier. The Supplier
	 * will begin being executed immediately with the {@link PromiseUtil#getExecutor}
	 * as it's executor.
	 *
	 * @param supplier A function that produces the value of this Promise.
	 * @return A new Promise instance.
	 */
	<T> Promise<T> of(Supplier<T> supplier);
	
	/**
	 * Creates a new Promise instance out of the given Callable. The Callable
	 * will begin being executed immediately with the {@link PromiseUtil#getExecutor}
	 * as it's executor.
	 *
	 * If the callable throws an error, the Promise will be rejected accordingly.
	 *
	 * @param callable A callable that produces the value of this Promise.
	 * @return A new Promise instance.
	 */
	<T> Promise<T> call(Callable<T> callable);
	
	/**
	 * Creates a new Promise instance out of the given Runnable. The Runnable will
	 * begin being executed immediately with the {@link PromiseUtil#getExecutor}
	 * as it's executor.
	 *
	 * @param runnable A function that produces the value of this Promise.
	 * @return A new Promise instance.
	 */
	Promise<Void> run(Runnable runnable);
	
	/**
	 * Creates a new Promise instance out of the given Collection. The collection
	 * can contain Suppliers, Callables, Promises, or CompletableFutures. If an object
	 * in the collection cannot be converted to a Promise, an error is thrown.
	 * <p>
	 * The Promise returned will complete when every object in the collection completes.
	 *
	 * @param objects The objects to complete.
	 * @return A Promise that will be completed when every object in the given
	 * collection is finished.
	 */
	Promise<Void> all(Collection<?> objects);
	
	/**
	 * Creates a new Promise instance out of the given Collection. The collection
	 * can contain Suppliers, Callables, Promises, or CompletableFutures. If an object
	 * in the collection cannot be converted to a Promise, an error is thrown.
	 * <p>
	 * The Promise returned will complete when one of the objects in the collection
	 * completes, in no specific order.
	 *
	 * @param objects The objects to complete.
	 * @return A Promise that will be completed when one of the objects in the
	 * given collection is finished.
	 */
	Promise<Object> any(Collection<?> objects);
	
	/**
	 * Creates a new Promise instance that will resolve itself after the
	 * given amount of time is up. Resolution is scheduled and will not
	 * block any thread.
	 *
	 * @param delay The delay to wait.
	 * @param unit The TimeUnit to wait in.
	 * @return A new Promise.
	 */
	Promise<Void> wait(long delay, TimeUnit unit);
	
	/**
	 * Creates a new Promise instance out of the given Collection's items by mapping
	 * every item to an individual Promise using the given function. The order of processing
	 * is not guaranteed. If any of the Promises are rejected, the returned Promise
	 * will be rejected as well.
	 *
	 * @param objects The objects to process into Promises.
	 * @param function A function that will produce a Promise for each Object.
	 * @return A Promise with a Collection of the results of each Promise produced by the function.
	 */
	<T, R> Promise<Collection<R>> map(Collection<T> objects, Function<T, Promise<R>> function);
	
	/**
	 * @param future The future instance to wrap.
	 * @return A new Promise instance that uses the given future for it's
	 * underlying operations.
	 */
	<T> Promise<T> wrap(CompletableFuture<T> future);
	
	/**
	 * Wraps a CompletionStage instance as a new Promise. Resolution and
	 * rejection of the returned Promise is equivalent to the CompletionStage's.
	 * Results and rejections will be passed without unwrapping (ie errors will be
	 * instances of CompletionExceptions) as soon as they are available.
	 *
	 * It is assumed that the given CompletionStage does not support
	 * {@link CompletionStage#toCompletableFuture()}. If it does, the
	 * {@link Promise#wrap(CompletableFuture)} method should be used instead.
	 *
	 * @param stage The CompletionStage instance to wrap.
	 * @param <T> The type of result to return.
	 * @return A new Promise instance.
	 */
	<T> Promise<T> wrap(CompletionStage<T> stage);
	
	/**
	 * Wraps a Future instance as a new Promise. The timing restrictions of
	 * this method ensure that the Promise never goes unresolved. A call to
	 * {@link Future#get(long, TimeUnit)} is made with the given parameters.
	 *
	 * <ul>
	 * <li>If the Future completes in time, the Promise is resolved to the result.</li>
	 * <li>If the Future fails to complete in time, the Promise is rejected with a
	 * CompletionException that is caused by a TimeoutException.</li>
	 * <li>If the Future completes exceptionally, the Promise is rejected with a
	 * CompletionException that is caused by the error encountered.</li>
	 * </ul>
	 *
	 * @param future The future to wrap.
	 * @param delay The amount of time to wait for a result.
	 * @param unit The time unit to use when waiting for a result.
	 * @param <T> The type of result to return.
	 * @return A new Promise instance.
	 */
	<T> Promise<T> wrap(Future<T> future, long delay, TimeUnit unit);
	
	/**
	 * @return A new PromiseBuilder instance that inherits the settings of this builder.
	 */
	PromiseBuilder clone();
}