package kotlinx.collections.immutable;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mirrors the X 12.28.0 shape: the renamed builder() and a clear()-like
 * method live on the implementation class, not on the interface.
 */
public class FakePersistentVector<E> extends AbstractList<E> implements ImmutableListMarker<E> {
    private final List<E> elements;

    public FakePersistentVector(List<E> elements) {
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements));
    }

    /** Renamed builder(). */
    public FakeVectorBuilder<E> c() {
        return new FakeVectorBuilder<>(elements);
    }

    /** Renamed clear(): returns a persistent list, so it is not a builder. */
    public FakePersistentVector<E> f() {
        return new FakePersistentVector<>(Collections.<E>emptyList());
    }

    @Override
    public E get(int index) {
        return elements.get(index);
    }

    @Override
    public int size() {
        return elements.size();
    }
}
