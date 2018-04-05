package ndawg.promise;

import ndawg.promise.impl.DefaultPromise;
import ndawg.promise.internal.PromiseUtil;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A Promise is a more fluid wrapper around Java's {@link CompletableFuture},
 * with a few key differences.
 * <br><br>
 * Almost all methods in this class execute their given functions using
 * the Executor that this Promise was created with. For example, {@link #then(Function)}
 * performs the Function using the Executor. If no Executor was specified,
 * it uses {@link PromiseUtil#getExecutor()}. The executor can be re-assigned after
 * creation by using the {@link #executor(Executor)} method.
 * <br><br>
 * It is important to remember that in an asynchronous context, errors are suppressed.
 * Without the use of {@link #join()} or {@link #get()}, an error could occur and never
 * be shown. Use of the {@link #error(Consumer)} message is recommended to receive errors.
 * <br><br>
 * Promises are immutable. Any method returning another Promise will return a new
 * instance, unless otherwise specified.
 *
 * @param <T> The type of Object being returned by this Promise.
 */
public interface Promise<T> extends Future<T> {
	
	/**
	 * A friendly alias for the {@link #builder()} method that allows for cleaner
	 * static importing of Promise functionality.
	 *
	 * @return A new {@link PromiseBuilder} instance, which is configured to use
	 * {@link PromiseUtil#getExecutor()} by default.
	 */
	static PromiseBuilder promise() {
		return builder();
	}
	
	/**
	 * @return A new {@link PromiseBuilder} instance, which is configured to use
	 * {@link PromiseUtil#getExecutor()} by default.
	 */
	static PromiseBuilder builder() {
		return PromiseUtil.getBuilder();
	}
	
	/**
	 * Creates a Promise which has already been resolved to the given value.
	 *
	 * @param value The value to resolve the Promise with.
	 * @return The already resolved Promise.
	 */
	static <T> Promise<T> value(T value) {
		return PromiseUtil.getBuilder().value(value);
	}
	
	/**
	 * Creates a Promise which has already been rejected with the given value.
	 *
	 * @param error The value to reject the Promise with.
	 * @return The already rejected Promise.
	 */
	static <T> Promise<T> rejected(Throwable error) {
		Promise<T> empty = empty();
		empty.reject(error);
		return empty;
	}
	
	/**
	 * @param executor The Executor to use for operations within this Promise instance.
	 * @return An empty, incomplete Promise. This Promise can be resolved by
	 * using {@link Promise#resolve} or {@link Promise#reject}.
	 */
	static <T> Promise<T> empty(Executor executor) {
		return builder().executor(executor).empty();
	}
	
	/**
	 * @return An empty, incomplete Promise. This Promise can be resolved by
	 * using {@link Promise#resolve} or {@link Promise#reject}.
	 */
	static <T> Promise<T> empty() {
		return PromiseUtil.getBuilder().empty();
	}
	
	/**
	 * Creates a new Promise instance out of the given Supplier. The Supplier
	 * will begin being executed immediately with the given Executor.
	 *
	 * @param supplier A function that produces the value of this Promise.
	 * @param executor The Executor to use for async operations.
	 * @return A new Promise instance.
	 */
	static <T> Promise<T> of(Supplier<T> supplier, Executor executor) {
		return builder().executor(executor).of(supplier);
	}
	
	/**
	 * Creates a new Promise instance out of the given Supplier. The Supplier
	 * will begin being executed immediately with the {@link PromiseUtil#getExecutor}
	 * as it's executor.
	 *
	 * @param supplier A function that produces the value of this Promise.
	 * @return A new Promise instance.
	 */
	static <T> Promise<T> of(Supplier<T> supplier) {
		return PromiseUtil.getBuilder().of(supplier);
	}
	
	/**
	 * Creates a new Promise instance out of the given Callable. The Callable
	 * will begin being executed immediately with the {@link PromiseUtil#getExecutor}
	 * as it's executor.
	 *
	 * If the callable throws an error, the Promise will be rejected accordingly.
	 *
	 * @param callable A callable that produces the value of this Promise.
	 * @param executor The Executor to use for async operations.
	 * @return A new Promise instance.
	 */
	static <T> Promise<T> call(Callable<T> callable, Executor executor) {
		return builder().executor(executor).call(callable);
	}
	
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
	static <T> Promise<T> call(Callable<T> callable) {
		return PromiseUtil.getBuilder().call(callable);
	}
	
	/**
	 * Creates a new Promise instance out of the given Runnable. The Runnable will
	 * begin being executed immediately with the given Executor.
	 *
	 * @param runnable A function that produces the value of this Promise.
	 * @param executor The Executor to use for async operations.
	 * @return A new Promise instance.
	 */
	static Promise<Void> run(Runnable runnable, Executor executor) {
		return builder().executor(executor).run(runnable);
	}
	
	/**
	 * Creates a new Promise instance out of the given Runnable. The Runnable will
	 * begin being executed immediately with the {@link PromiseUtil#getExecutor}
	 * as it's executor.
	 *
	 * @param runnable A function that produces the value of this Promise.
	 * @return A new Promise instance.
	 */
	static Promise<Void> run(Runnable runnable) {
		return PromiseUtil.getBuilder().run(runnable);
	}
	
	/**
	 * Creates a new Promise instance out of the given Collection. The collection
	 * can contain Suppliers, Callables, Promises, or CompletableFutures. If an object
	 * in the collection cannot be converted to a Promise, an error is thrown.
	 * <p>
	 * The Promise returned will complete when every object in the collection completes.
	 *
	 * @param objects The objects to complete.
	 * @param executor The Executor to use for async operations.
	 * @return A Promise that will be completed when every object in the given
	 * collection is finished.
	 */
	static Promise<Void> all(Collection<?> objects, Executor executor) {
		return builder().executor(executor).all(objects);
	}
	
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
	static Promise<Void> all(Collection<?> objects) {
		return PromiseUtil.getBuilder().all(objects);
	}
	
	/**
	 * Creates a new Promise instance out of the given Collection. The collection
	 * can contain Suppliers, Callables, Promises, or CompletableFutures. If an object
	 * in the collection cannot be converted to a Promise, an error is thrown.
	 * <p>
	 * The Promise returned will complete when one of the objects in the collection
	 * completes, in no specific order.
	 *
	 * @param objects The objects to complete.
	 * @param executor The Executor to use for async operations.
	 * @return A Promise that will be completed when one of the objects in the
	 * given collection is finished.
	 */
	static Promise<Object> any(Collection<?> objects, Executor executor) {
		return builder().executor(executor).any(objects);
	}
	
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
	static Promise<Object> any(Collection<?> objects) {
		return PromiseUtil.getBuilder().any(objects);
	}
	
	/**
	 * Creates a new Promise instance that will resolve itself after the
	 * given amount of time is up. Resolution is scheduled and will not
	 * block any thread, including one from the given Executor.
	 *
	 * @param delay The delay to wait.
	 * @param unit The TimeUnit to wait in.
	 * @param executor The Executor to use for async operations.
	 * @return A new Promise.
	 */
	static Promise<Void> wait(long delay, TimeUnit unit, Executor executor) {
		return builder().executor(executor).wait(delay, unit);
	}
	
	/**
	 * Creates a new Promise instance that will resolve itself after the
	 * given amount of time is up. Resolution is scheduled and will not
	 * block any thread.
	 *
	 * @param delay The delay to wait.
	 * @param unit The TimeUnit to wait in.
	 * @return A new Promise.
	 */
	static Promise<Void> wait(long delay, TimeUnit unit) {
		return PromiseUtil.getBuilder().wait(delay, unit);
	}
	
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
	static <T, R> Promise<Collection<R>> map(Collection<T> objects, Function<T, Promise<R>> function) {
		return PromiseUtil.getBuilder().map(objects, function);
	}
	
	/**
	 * @param future The future instance to wrap.
	 * @param executor The Executor to use for async operations.
	 * @return A new Promise instance that uses the given future for it's
	 * underlying operations.
	 */
	static <T> Promise<T> wrap(CompletableFuture<T> future, Executor executor) {
		return builder().executor(executor).wrap(future);
	}
	
	/**
	 * @param future The future instance to wrap.
	 * @return A new Promise instance that uses the given future for it's
	 * underlying operations.
	 */
	static <T> Promise<T> wrap(CompletableFuture<T> future) {
		return new DefaultPromise<>(future, PromiseUtil.getExecutor());
	}
	
	/**
	 * Wraps a CompletionStage instance as a new Promise. Resolution and
	 * rejection of the returned Promise is equivalent to the CompletionStage's.
	 * Results and rejections will be passed without unwrapping (ie errors will be
	 * instances of CompletionExceptions) as soon as they are available.
	 * Cancellation is not supported.
	 *
	 * It is assumed that the given CompletionStage does not support
	 * {@link CompletionStage#toCompletableFuture()}. If it does, the
	 * {@link Promise#wrap(CompletableFuture)} method should be used instead.
	 *
	 * @param stage The CompletionStage instance to wrap.
	 * @param executor The Executor to use for async operations.
	 * @param <T> The type of result to return.
	 * @return A new Promise instance.
	 */
	static <T> Promise<T> wrap(CompletionStage<T> stage, Executor executor) {
		return builder().executor(executor).wrap(stage);
	}
	
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
	static <T> Promise<T> wrap(CompletionStage<T> stage) {
		return PromiseUtil.getBuilder().wrap(stage);
	}
	
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
	 * @param executor The Executor to use for async operations.
	 * @param <T> The type of result to return.
	 * @return A new Promise instance.
	 */
	static <T> Promise<T> wrap(Future<T> future, long delay, TimeUnit unit, Executor executor) {
		return builder().executor(executor).wrap(future, delay, unit);
	}
	
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
	static <T> Promise<T> wrap(Future<T> future, long delay, TimeUnit unit) {
		return PromiseUtil.getBuilder().wrap(future, delay, unit);
	}
	
	/**
	 * Applies the given Function to the result of the Promise, when it
	 * becomes available, and transforms the result to the result of the function.
	 * <p>
	 * If this Promise was created with a specific {@link Executor} instance,
	 * it is used to execute the function, otherwise the {@link PromiseUtil#getExecutor}
	 * is used.
	 *
	 * @param function The function to apply to the result.
	 * @return The new Promise instance.
	 */
	<R> Promise<R> then(Function<? super T, ? extends R> function);
	
	/**
	 * Applies the given Function to the result of the Promise, when it becomes available,
	 * and joins the resulting Promise instance to this one. This blocks the current Thread
	 * the Promise is executing on.
	 * <p>
	 * If this Promise was created with a specific {@link Executor} instance,
	 * it is used to execute the function, otherwise the {@link PromiseUtil#getExecutor}
	 * is used.
	 *
	 * @param function The function to apply to the result.
	 * @return The new Promise instance.
	 */
	<R> Promise<R> chain(Function<? super T, ? extends Promise<R>> function);
	
	/**
	 * Assigns a success and failure handler to this Promise. In the event of the
	 * Promise completing normally, the first Consumer receives the result.
	 * In the event of an error producing a value, the second Consumer receives the error.
	 * <p>
	 * If an error is encountered, it will be suppressed and passed to the Consumer
	 * instead of being re-thrown by {@link #get()} and similar methods. Instead,
	 * they will return null.
	 * <p>
	 * If this Promise was created with a specific {@link Executor} instance,
	 * it is used to call the consumers, otherwise the {@link PromiseUtil#getExecutor}
	 * is used.
	 *
	 * @param success The Consumer that will receive the success value, which could be null.
	 * @param failure The Consumer that will receive the error.
	 */
	Promise<T> handle(Consumer<? super T> success, Consumer<? super Throwable> failure);
	
	/**
	 * In the event of a rejection, the given Function is called to
	 * produce an alternative value, and the error is suppressed.
	 *
	 * @param function The function to use to compute the value of this
	 * Promise if an error is encountered.
	 * @return The new Promise instance.
	 */
	Promise<T> catching(Function<Throwable, ? extends T> function);
	
	/**
	 * Assigns a Consumer that will receive the result of this Promise
	 * if it completes without an exception.
	 * <p>
	 * If this Promise was created with a specific {@link Executor} instance,
	 * it is used to execute the consumer, otherwise the
	 * {@link PromiseUtil#getExecutor} is used.
	 *
	 * @param consumer The consumer that will receive the result.
	 * @return The new Promise instance.
	 */
	Promise<Void> done(Consumer<? super T> consumer);
	
	/**
	 * Assigns a Consumer to receive an error. This does nothing to
	 * suppress it, and Promises will still fail on an error.
	 *
	 * @param consumer The Consumer to receive the error.
	 * @return The new Promise instance.
	 */
	Promise<T> error(Consumer<Throwable> consumer);
	
	/**
	 * Switches the Executor to the given instance. Following calls will be made
	 * using this Executor.
	 *
	 * @param executor The Executor to use.
	 * @return The new Promise instance, with the Executor switched.
	 */
	Promise<T> executor(Executor executor);
	
	/**
	 * Attempts to immediately obtain the result. If the Promise has
	 * completed exceptionally, the error is thrown. If the Promise has
	 * finished gracefully, that value is returned. Otherwise, the given
	 * fallback is returned.
	 *
	 * @param fallback The fallback to receive.
	 * @return The result of the Promise, or the fallback.
	 */
	T get(T fallback);
	
	/**
	 * Attempts to immediately obtain the result. If the Promise has
	 * completed exceptionally, the error is thrown. If the Promise has finished
	 * gracefully, that value is returned. Otherwise, a blocking
	 * wait is made to let the Promise finish. If it doesn't finish
	 * within the given amount of time, the fallback is returned.
	 * <p>
	 * This method relies on {@link #get}, but suppresses the TimeoutException
	 * if it is thrown.
	 *
	 * @param timeout the maximum time to wait.
	 * @param unit the time unit of the timeout argument.
	 * @param fallback The result of the Promise, or the fallback.
	 * @return The result of the Promise, or the fallback.
	 */
	T get(long timeout, TimeUnit unit, T fallback) throws ExecutionException, InterruptedException;
	
	/**
	 * Blocks until the Promise finishes or encounters an error.
	 * In the event of an exception, it is re-thrown as a {@link CompletionException}
	 * with the underlying error as the cause.
	 *
	 * @return The result value.
	 * @throws CancellationException If the Promise is cancelled.
	 * @throws CompletionException If the Promise encounters an error while completing.
	 */
	T join();
	
	/**
	 * If not already completed, sets the value returned by {@link #get()}
	 * and related methods to the given value. This will also trigger anything
	 * downstream that is waiting for the value.
	 *
	 * @param result The result value.
	 */
	void resolve(T result);
	
	/**
	 * If not already completed, causes invocations of {@link #get()} and
	 * related methods to throw the given exception.
	 *
	 * @param exception The exception occurred while obtaining the result.
	 * This cannot be null.
	 */
	void reject(Throwable exception);
	
	/**
	 * A convenience method that executes the given Callable in an attempt
	 * to resolve this Promise instance. If an exception is encountered within
	 * the Callable, the Promise fails. Otherwise the
	 * returned value is used to complete it.
	 * <p>
	 * If this Promise was created with a specific {@link Executor} instance,
	 * it is used to execute the consumer, otherwise the
	 * {@link PromiseUtil#getExecutor} is used.
	 *
	 * @param callable The Callable that should return the value of this Promise.
	 */
	void attempt(Callable<T> callable);
	
	/**
	 * @return The underlying CompletableFuture instance.
	 */
	CompletableFuture<T> toCompletableFuture();
	
}
