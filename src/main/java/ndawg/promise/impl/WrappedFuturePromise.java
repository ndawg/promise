package ndawg.promise.impl;

import ndawg.promise.Promise;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A Promise instance that delegates Future calls to the wrapped Future instance.
 *
 * @param <T> The type of result.
 */
public class WrappedFuturePromise<T> implements Promise<T> {
	
	private final Promise<T> promise;
	private final Future<T> future;
	
	public WrappedFuturePromise(Promise<T> promise, Future<T> future) {
		this.promise = promise;
		this.future = future;
	}
	
	@Override
	public <R> Promise<R> then(Function<? super T, ? extends R> function) {
		return promise.then(function);
	}
	
	@Override
	public <R> Promise<R> chain(Function<? super T, ? extends Promise<R>> function) {
		return promise.chain(function);
	}
	
	@Override
	public Promise<T> handle(Consumer<? super T> success, Consumer<? super Throwable> failure) {
		return promise.handle(success, failure);
	}
	
	@Override
	public Promise<T> catching(Function<Throwable, ? extends T> function) {
		return promise.catching(function);
	}
	
	@Override
	public Promise<Void> done(Consumer<? super T> consumer) {
		return promise.done(consumer);
	}
	
	@Override
	public Promise<T> error(Consumer<Throwable> consumer) {
		return promise.error(consumer);
	}
	
	@Override
	public Promise<T> executor(Executor executor) {
		return promise.executor(executor);
	}
	
	@Override
	public T get(T fallback) {
		return promise.get(fallback);
	}
	
	@Override
	public T get(long timeout, TimeUnit unit, T fallback) throws ExecutionException, InterruptedException {
		return promise.get(timeout, unit, fallback);
	}
	
	@Override
	public T join() {
		return promise.join();
	}
	
	@Override
	public void resolve(T result) {
		promise.resolve(result);
	}
	
	@Override
	public void reject(Throwable exception) {
		promise.reject(exception);
	}
	
	@Override
	public void attempt(Callable<T> callable) {
		promise.attempt(callable);
	}
	
	@Override
	public CompletableFuture<T> toCompletableFuture() {
		return promise.toCompletableFuture();
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
	public T get() throws InterruptedException, ExecutionException {
		return future.get();
	}
	
	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return future.get(timeout, unit);
	}
	

}
