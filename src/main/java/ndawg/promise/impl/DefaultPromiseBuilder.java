package ndawg.promise.impl;

import ndawg.promise.Promise;
import ndawg.promise.PromiseBuilder;
import ndawg.promise.internal.PromiseUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class DefaultPromiseBuilder implements PromiseBuilder {
	
	private final Executor executor;
	
	public DefaultPromiseBuilder(Executor executor) {
		this.executor = executor;
	}
	
	@Override
	public PromiseBuilder executor(Executor executor) {
		return new DefaultPromiseBuilder(executor); // implemented like this for now for future immutability
	}
	
	@Override
	public <T> Promise<T> value(T value) {
		Promise<T> empty = empty();
		empty.resolve(value);
		return empty;
	}
	
	@Override
	public Promise<?> error(Throwable error) {
		Promise<?> empty = empty();
		empty.reject(error);
		return empty;
	}
	
	@Override
	public <T> Promise<T> empty() {
		return new DefaultPromise<>(new CompletableFuture<>(), executor);
	}
	
	@Override
	public <T> Promise<T> of(Supplier<T> supplier) {
		return new DefaultPromise<>(CompletableFuture.supplyAsync(supplier, executor), executor);
	}
	
	@Override
	public <T> Promise<T> call(Callable<T> callable) {
		Promise<T> promise = empty();
		promise.attempt(callable);
		return promise;
	}
	
	@Override
	public Promise<Void> run(Runnable runnable) {
		return new DefaultPromise<>(CompletableFuture.runAsync(runnable, executor), executor);
	}
	
	@Override
	public Promise<Void> all(Collection<?> objects) {
		return new DefaultPromise<>(CompletableFuture.allOf(PromiseUtil.toFutures(objects)), executor);
	}
	
	@Override
	public Promise<Object> any(Collection<?> objects) {
		return new DefaultPromise<>(CompletableFuture.anyOf(PromiseUtil.toFutures(objects)), executor);
	}
	
	@Override
	public Promise<Void> wait(long delay, TimeUnit unit) {
		return new ScheduledPromise<>(new CompletableFuture<>(), executor, delay, unit);
	}
	
	// this is somewhat unnecessary as it doesn't create any Promises itself
	@Override
	public <T, R> Promise<Collection<R>> map(Collection<T> objects, Function<T, Promise<R>> function) {
		Promise<Collection<R>> promise = empty();
		Collection<R> results = new ArrayList<>();
		
		for (T object : objects) {
			function.apply(object).error(promise::reject).done(res -> {
				results.add(res);
				
				if (results.size() == objects.size())
					promise.resolve(results);
			});
		}
		
		return promise;
	}
	
	@Override
	public <T> Promise<T> wrap(CompletableFuture<T> future) {
		return new DefaultPromise<>(future, executor);
	}
	
	@Override
	public <T> Promise<T> wrap(CompletionStage<T> stage) {
		Promise<T> promise = new DefaultPromise<T>(new CompletableFuture<>(), executor) {
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				throw new UnsupportedOperationException("Wrapped CompletionStage cannot be cancelled.");
			}
		};
		stage.whenCompleteAsync((res, err) -> {
			if (res != null)
				promise.resolve(res);
			else if (err != null)
				promise.reject(err);
			else
				promise.resolve(null);
		}, executor);
		return promise;
	}
	
	@Override
	public <T> Promise<T> wrap(Future<T> future, long delay, TimeUnit unit) {
		Promise<T> promise = new WrappedFuturePromise<>(empty(), future);
		promise.attempt(() -> {
			try {
				return future.get(delay, unit);
			} catch (ExecutionException e) {
				// Intercept an ExecutionException and re-throw as a CompletionException
				// Otherwise, the Promise would be rejected with an
				// ExecutionException(CompletionException(Actual Error))
				throw new CompletionException(e.getCause());
			}
		});
		return promise;
	}
	
	@Override
	public PromiseBuilder clone() {
		return new DefaultPromiseBuilder(this.executor);
	}
	
	/**
	 * A waiting Promise that can be cancelled.
	 *
	 * @param <T> The type of Promise.
	 */
	private class ScheduledPromise<T> extends DefaultPromise<T> {
		
		private final ScheduledFuture<?> future;
		
		ScheduledPromise(CompletableFuture<T> future, Executor executor, long delay, TimeUnit unit) {
			super(future, executor);
			this.future = PromiseUtil.getScheduler().schedule(() -> this.resolve(null), delay, unit);
		}
		
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return future.cancel(mayInterruptIfRunning);
		}
		
	}
	
}
