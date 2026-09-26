package kotlinx.collections.immutable;

import java.util.ArrayList;
import java.util.List;

/** Renamed PersistentList.Builder: a mutable list with a renamed build(). */
public final class FakeVectorBuilder<E> extends ArrayList<E> {
    FakeVectorBuilder(List<E> initial) {
        super(initial);
    }

    public FakePersistentVector<E> b() {
        return new FakePersistentVector<>(this);
    }
}
