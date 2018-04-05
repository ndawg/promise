package ndawg.promise.impl;

import ndawg.promise.Promise;
import ndawg.promise.internal.PromiseUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A wrapper around a CompletableFuture that allows some data
 * to be transferred between instances.
 *
 * @param <T> The result type.
 */
public class DefaultPromise<T> implements Promise<T> {
	
	// Use final fields to enforce immutable principles
	private final CompletableFuture<T> future;
	private final Executor executor;
	
	public DefaultPromise(CompletableFuture<T> future, Executor executor) {
		this.future = future;
		this.executor = executor != null ? executor : PromiseUtil.getExecutor();
	}
	
	/**
	 * Convenience method that chains the Promise and returns a new Promise instance
	 * with the same Executor instance as this one.
	 *
	 * @param supplier The Supplier that produces a CompletableFuture instance.
	 * @return A DefaultPromise instance that wraps the given Object.
	 */
	private <R> DefaultPromise<R> chaining(Supplier<CompletableFuture<R>> supplier) {
		return new DefaultPromise<>(supplier.get(), this.executor);
	}
	
	@Override
	public <R> Promise<R> then(final Function<? super T, ? extends R> function) {
		return chaining(() -> future.thenApplyAsync(function, this.executor));
	}
	
	@Override
	public <R> Promise<R> chain(final Function<? super T, ? extends Promise<R>> function) {
		return chaining(() -> future.thenComposeAsync(res -> {
			Promise<R> promise = function.apply(res);
			if (promise == null)
				throw new IllegalStateException("Chained function returned a null Promise");
			return promise.toCompletableFuture();
		}, this.executor));
	}
	
	@Override
	public Promise<T> handle(Consumer<? super T> success, final Consumer<? super Throwable> failure) {
		BiConsumer<? super T, ? super Throwable> cons = (res, err) -> {
			if (err != null && failure != null) {
				failure.accept(unwrap(err));
			} else if (success != null) {
				success.accept(res);
			}
		};
		
		return this.chaining(() -> future.whenCompleteAsync(cons, this.executor));
	}
	
	@Override
	public Promise<T> catching(final Function<Throwable, ? extends T> function) {
		return chaining(() -> future.exceptionally(function));
	}
	
	public Promise<T> error(final Consumer<Throwable> consumer) {
		// Can't be rejected by null
		BiFunction<T, Throwable, T> function = (a, b) -> {
			if (b != null)
				consumer.accept(b);
			return a;
		};
		
		return chaining(() -> future.handle(function));
	}
	
	@Override
	public Promise<T> executor(Executor executor) {
		return new DefaultPromise<>(future, executor);
	}
	
	@Override
	public Promise<Void> done(Consumer<? super T> consumer) {
		return chaining(() -> future.thenAcceptAsync(consumer, this.executor));
	}
	
	@Override
	public void resolve(T result) {
		future.complete(result);
	}
	
	@Override
	public void reject(Throwable exception) {
		if (exception == null)
			throw new IllegalArgumentException("Attempted to reject with a null exception");
		future.completeExceptionally(exception);
	}
	
	@Override
	public void attempt(final Callable<T> callable) {
		this.executor.execute(() -> {
			try {
				resolve(callable.call());
			} catch (Throwable err) {
				reject(err);
			}
		});
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return future.cancel(mayInterruptIfRunning);
	}
	
	@Override
	public boolean isCancelled() {
		return future.isCancelled();
	}
	
	@Override
	public boolean isDone() {
		return future.isDone();
	}
	
	@Override
	public T join() {
		return future.join();
	}
	
	@Override
	public T get() throws ExecutionException, InterruptedException {
		return future.get();
	}
	
	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return future.get(timeout, unit);
	}
	
	@Override
	public T get(long timeout, TimeUnit unit, T fallback) throws ExecutionException, InterruptedException {
		try {
			return get(timeout, unit);
		} catch (TimeoutException e) {
			return fallback;
		}
	}
	
	@Override
	public T get(T fallback) {
		return future.getNow(fallback);
	}
	
	public CompletableFuture<T> toCompletableFuture() {
		return future;
	}
	
	private Throwable unwrap(Throwable err) {
		return err;
	}
	
}
