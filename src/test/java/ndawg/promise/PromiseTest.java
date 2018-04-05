package ndawg.promise;

import com.diffplug.common.base.Errors;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

public class PromiseTest {
	
	@Test
	public void cleanEmpty() {
		Promise<Integer> empty = Promise.empty();
		assert !empty.isDone();
		assert !empty.isCancelled();
	}
	
	@Test
	public void initalValue() {
		Promise<Integer> value = Promise.value(5);
		assert value.isDone();
		assert value.join() == 5;
	}
	
	@Test
	public void initalError() {
		Promise<Integer> error = Promise.rejected(new IllegalStateException());
		assert error.isDone();
		assertThrows(error::join).hasCauseInstanceOf(IllegalStateException.class);
	}
	
	@Test
	public void emptySuccess() {
		Promise<Integer> empty = Promise.empty();
		empty.resolve(5);
		assert empty.join() == 5;
	}
	
	@Test
	public void emptyFailure() {
		Promise<Integer> empty = Promise.empty();
		empty.reject(new IllegalStateException());
		assertThrows(empty::join).isInstanceOf(CompletionException.class).hasCauseInstanceOf(IllegalStateException.class);
	}
	
	@Test
	public void simpleSuccess() {
		assert Promise.of(() -> 5).join() == 5;
		assert Promise.of(() -> 5).then(i -> i + 5).join() == 10;
		assert Promise.of(() -> 5).then(i -> i + 5).chain(i -> Promise.of(() -> i + 5)).join() == 15;
	}
	
	@Test
	public void simpleSuccessExecutor() {
		// Here the single threaded executor is intentionally re-used to ensure that a
		// chained call to a Promise on the same executor does not block execution.
		ExecutorService exec = Executors.newSingleThreadExecutor();
		assert Promise.of(() -> 5, exec).join() == 5;
		assert Promise.of(() -> 5, exec).then(i -> i + 5).join() == 10;
		assert Promise.of(() -> 5, exec).then(i -> i + 5).chain(i -> Promise.of(() -> i + 5, exec)).join() == 15;
		
		assert exec.shutdownNow().isEmpty(); // No lingering tasks.
	}
	
	@Test
	public void singleDepthException() {
		assertThrows(() -> {
			Promise.of(() -> { throw new IllegalStateException("Dummy error"); }).join();
		}).isInstanceOf(CompletionException.class).hasCauseInstanceOf(IllegalStateException.class);
	}
	
	@Test
	public void multiDepthException() {
		assertThrows(() -> {
			Promise.of(() -> 5).then(Function.identity()).then(i -> {
				throw new IllegalStateException("Dummy error");
			}).join();
		}).isInstanceOf(CompletionException.class).hasCauseInstanceOf(IllegalStateException.class);
	}
	
	@Test
	public void doneSuccess() {
		final int[] got = new int[1];
		Promise.of(() -> 5).done(i -> got[0] = i);
		await().until(() -> got[0] == 5);
	}
	
	@Test
	public void doneError() {
		// This test verifies that on a Promise's rejection, the #done
		// hook is not sent "null" or any other value
		AtomicReference<Object> obj = new AtomicReference<>("");
		Promise<Void> promise = Promise.of(() -> {
			throw new IllegalArgumentException("Dummy error");
		}).done(obj::set);
		
		await().until(promise::isDone);
		assert obj.get().equals("");
	}
	
	@Test
	public void doneChain() {
		Promise<Integer> p = Promise.empty();
		Promise<Void> chain = p.done(Errors.rethrow().wrap(i -> {
			Thread.sleep(100);
		}));
		p.resolve(5);
		
		// The chained #done should not finish til it's own code executes
		assert !chain.isDone();
		
		await().until(chain::isDone);
	}
	
	@Test
	public void promiseCollection() {
		final boolean[] pass = {false};
		
		List<Object> promises = new LinkedList<>();
		// Runnable conversion
		promises.add(Errors.rethrow().wrap(() -> Thread.sleep(1)));
		// Promise conversion
		promises.add(Promise.run(Errors.rethrow().wrap(() -> Thread.sleep(50))));
		// Supplier conversion
		promises.add(Errors.rethrow().wrap(() -> {
			Thread.sleep(100);
			pass[0] = true;
			return null;
		}));
		
		Promise.all(promises).join();
		
		assert pass[0];
	}
	
	@Test
	public void thenThreading() {
		// Should probably set a custom thread here
		final boolean[] pass = {false};
		final Thread[] thread = new Thread[1];
		
		Promise.run(() -> {
			thread[0] = Thread.currentThread();
		}).then(o -> {
			pass[0] = Thread.currentThread() == thread[0];
			return null;
		}).join();
		
		assert pass[0];
	}
	
	@Test
	public void executorPermanence() {
		MockExec exec = new MockExec();
		
		// Make sure it uses the mock executor to begin with.
		Promise.of(() -> 1, exec).join();
		assert exec.started == 1;
		
		// Make sure there's no interference for any reason.
		Promise.of(() -> 1).join();
		assert exec.started == 1;
		
		// Verify #then
		Promise.of(() -> 1, exec).then(Function.identity()).join();
		assert exec.started == 3;
		
		// Verify #chain
		Promise.of(() -> 1, exec).chain(i -> Promise.of(() -> null)).join();
		assert exec.started == 5;
		
		// Verify #handle gracefully
		Promise<Integer> p = Promise.of(() -> 1, exec);
		p.handle(i -> {}, e -> {});
		p.join();
		assert exec.started == 7;
		
		// Verify #handle errors
		p = Promise.of(() -> { throw new IllegalStateException(); }, exec);
		p.handle(i -> {}, e -> {});
		await().until(p::isDone);
		assert exec.started == 9;
		
		// Verify #done
		p = Promise.of(() -> 1, exec);
		p.done(i -> {});
		p.join();
		assert exec.started == 11;
		
		// Verify #attempt
		Promise.of(() -> 1, exec).attempt(() -> 1);
		assert exec.started == 13;
	}
	
	@Test
	public void handleErrorBranch() {
		List<Boolean> list = new ArrayList<>();
		
		Promise.of(() -> {
			throw new IllegalStateException();
		}).handle(o -> {}, e -> {
			list.add(true);
		});
		
		Promise.of(() -> {
			throw new IllegalStateException();
		}, new MockExec()).handle(o -> {}, e -> {
			list.add(true);
		});
		
		await().until(() -> list.size() == 2);
		assert list.get(0);
		assert list.get(1);
	}
	
	@Test
	public void chainedErrorHandling() {
		assertThrows(() -> {
			Promise.of(() -> 5).chain(i -> Promise.of(() -> {
				throw new IllegalArgumentException();
			})).join();
		});
	}
	
	@Test
	public void switchingExecutors() {
		MockExec exec = new MockExec();
		for (int i = 0; i < 15; i++) {
			Promise.run(() -> {}).then(Function.identity()).executor(exec).then(Function.identity()).join();
		}
		assert exec.started == 15;
	}
	
	@Test
	public void chaining() {
		assert Promise.of(() -> 5).chain(i -> {
			assert i == 5;
			return Promise.of(() -> 10);
		}).chain(i -> {
			assert i == 10;
			return Promise.of(() -> 15);
		}).join() == 15;
	}
	
	@Test
	public void erroring() {
		AtomicReference<Throwable> err = new AtomicReference<>();
		AtomicReference<Integer> res = new AtomicReference<>();
		
		Promise.of(() -> {
			throw new IllegalStateException();
		}).handle(r -> {}, err::set).catching(er -> {
			res.set(10);
			return null;
		});
		
		await().until(() -> err.get() != null);
		assert err.get() instanceof CompletionException;
		assert err.get().getCause() instanceof IllegalStateException;
		
		assert res.get() == 10;
	}
	
	@Test
	public void errorRedirection() {
		AtomicReference<Throwable> err = new AtomicReference<>();
		
		Promise.of(() -> {
			throw new IllegalStateException();
		}).error(err::set);
		
		await().until(() -> err.get() != null);
		assert err.get() instanceof CompletionException;
		assert err.get().getCause() instanceof IllegalStateException;
	}
	
	@Test
	public void mapSuccess() {
		Collection<String> strs = Arrays.asList("a", "b", "c");
		Collection<String> res = Promise.map(strs, s -> Promise.of((Supplier<String>) s::toUpperCase)).join();
		assert res.containsAll(Arrays.asList("A", "B", "C"));
	}
	
	@Test
	public void mapError() {
		Collection<String> strs = Arrays.asList("a", "b", "c");
		
		assertThrows(() -> {
			Promise.map(strs, s -> Promise.of(() -> {
				throw new IllegalArgumentException("Dummy error");
			})).join();
		});
	}
	
	@Test
	public void identity() {
		// All Promise methods should return new Promise instances.
		Promise<String> promise = Promise.empty();
		
		assert promise != promise.then(Function.identity());
		assert promise != promise.chain(s -> Promise.<String>empty());
		assert promise != promise.handle(o -> {}, e -> {});
		assert promise != promise.catching(o -> null);
		assert promise != promise.error(e -> {});
	}
	
	@Test
	public void waitingThread() {
		MockExec exec = new MockExec();
		Promise.builder().executor(exec).wait(10, TimeUnit.MILLISECONDS).then(Function.identity()).join();
		assert exec.started == 1;
	}
	
	@Test
	public void mappingSuccess() {
		Collection<String> objects = Arrays.asList("a", "b", "c");
		Collection<String> mapped = Promise.map(objects, l -> Promise.of(l::toUpperCase)).join();
		assert mapped.containsAll(Arrays.asList("A", "B", "C")) && mapped.size() == 3;
	}
	
	@Test
	public void mappingError() {
		Collection<String> objects = Arrays.asList("a", "b", "c");
		
		Promise<Collection<String>> promise = Promise.map(objects, l -> {
			if (l.equals("b"))
				return Promise.of(() -> { throw new IllegalStateException(); });
			else
				return Promise.of(l::toUpperCase);
		});
		
		await().until(promise::isDone);
		assertThrows(promise::join).hasCauseInstanceOf(IllegalStateException.class);
	}
	
	@Test
	public void futureWrappingSuccess() {
		Future<Integer> future = Executors.newSingleThreadExecutor().submit(() -> 5);
		Promise<Integer> promise = Promise.wrap(future, 1, TimeUnit.SECONDS);
		assert promise.join() == 5;
		assert promise.isDone();
		assert !promise.isCancelled();
	}
	
	@Test
	public void futureWrappingTimeout() {
		Future<?> future = ForkJoinTask.adapt(() -> { while (true); });
		
		assertThrows(() -> {
			Promise.wrap(future, 1, TimeUnit.SECONDS).join();
		}).isInstanceOf(CompletionException.class).hasCauseInstanceOf(TimeoutException.class);
	}
	
	@Test
	public void futureWrappingErrors() {
		Future<Integer> future = Executors.newSingleThreadExecutor().submit(() -> { throw new IllegalArgumentException("Dummy"); });
		
		assertThrows(() -> {
			Promise.wrap(future, 1, TimeUnit.SECONDS).join();
		}).isInstanceOf(CompletionException.class).hasCauseInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void futureWrappingCancellation() {
		Future<?> future = ForkJoinTask.adapt(() -> { while (true); });
		Promise<?> p = Promise.wrap(future, 10, TimeUnit.SECONDS);
		future.cancel(true);
		
		assertThrows(p::join).isInstanceOf(CancellationException.class);
	}
	
	@Test
	public void completionStageWrappingSuccess() {
		CompletionStage<Integer> stage = CompletableFuture.supplyAsync(() -> 5);
		assert Promise.wrap(stage).join() == 5;
	}
	
	@Test
	public void completionStageWrappingTimeout() {
		CompletionStage<?> stage = new CompletableFuture<>();
		
		assertThrows(() -> {
			Promise.wrap(stage).get(1, TimeUnit.SECONDS);
		}).isInstanceOf(TimeoutException.class);
	}
	
	@Test
	public void completionStageWrappingErrors() {
		CompletionStage<Integer> stage = CompletableFuture.supplyAsync(() -> { throw new IllegalStateException(); });
		
		assertThrows(() -> {
			Promise.wrap(stage).get(1, TimeUnit.SECONDS);
		}).isInstanceOf(ExecutionException.class).hasCauseInstanceOf(IllegalStateException.class);
		
		assertThrows(() -> {
			Promise.wrap(stage).join();
		}).isInstanceOf(CompletionException.class).hasCauseInstanceOf(IllegalStateException.class);
	}
	
	public static AbstractThrowableAssert<?, ? extends Throwable> assertThrows(ThrowableAssert.ThrowingCallable shouldRaiseThrowable) {
		return assertThatThrownBy(shouldRaiseThrowable);
	}
	
	private class MockExec implements Executor {
		
		int started;
		
		@Override
		public void execute(Runnable command) {
			ForkJoinPool.commonPool().execute(command);
			started++;
		}
	}
	
}
