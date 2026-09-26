package my.MrxSiN.twitterhideads;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runtime proof that an adaptively selected boundary really renders posts.
 *
 * Structure alone does not prove semantics, so a boundary starts
 * {@link State#WITNESSING}. The first invocation whose model identifies a post
 * confirms it; {@code window} invocations without one reject it, after which
 * the caller unhooks the boundary. Once decided, the state never changes.
 */
final class BoundaryWitness {
    enum State {
        WITNESSING,
        CONFIRMED,
        REJECTED
    }

    private final int window;
    private final AtomicInteger observed = new AtomicInteger();
    private final AtomicReference<State> state =
            new AtomicReference<>(State.WITNESSING);

    BoundaryWitness(int window) {
        if (window < 1) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.window = window;
    }

    State state() {
        return state.get();
    }

    /**
     * Records one invocation and returns the state transition it caused, or
     * {@code null} when the state did not change.
     */
    State record(boolean identifiesPost) {
        if (state.get() != State.WITNESSING) {
            return null;
        }
        if (identifiesPost) {
            return state.compareAndSet(State.WITNESSING, State.CONFIRMED)
                    ? State.CONFIRMED
                    : null;
        }
        if (observed.incrementAndGet() >= window
                && state.compareAndSet(State.WITNESSING, State.REJECTED)) {
            return State.REJECTED;
        }
        return null;
    }

    int observed() {
        return observed.get();
    }
}
