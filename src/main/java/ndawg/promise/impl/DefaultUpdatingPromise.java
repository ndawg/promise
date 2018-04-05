package ndawg.promise.impl;

import ndawg.promise.UpdatingPromise;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

public class DefaultUpdatingPromise<T, I> extends DefaultPromise<T> implements UpdatingPromise<T, I> {
	
	private Set<Consumer<I>> consumers = new LinkedHashSet<>();
	private I last = null;
	
	public DefaultUpdatingPromise(CompletableFuture<T> future, Executor executor) {
		super(future, executor);
	}
	
	@Override
	public UpdatingPromise<T, I> receive(Consumer<I> consumer) {
		consumers.add(consumer);
		return this;
	}
	
	@Override
	public void attempt(final Function<UpdatingPromise<T, I>, T> consumer) {
		super.attempt(() -> consumer.apply(this));
	}
	
	@Override
	public void push(final I info) {
		consumers.forEach(consumer -> consumer.accept(info));
		this.last = info;
	}
	
	@Override
	public I getLastUpdate() {
		return this.last;
	}
	
}
