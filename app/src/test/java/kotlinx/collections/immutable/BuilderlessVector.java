package kotlinx.collections.immutable;

import java.util.AbstractList;

/** A kotlinx list type without the builder shape; copying must fail open. */
public final class BuilderlessVector<E> extends AbstractList<E> implements ImmutableListMarker<E> {
    @Override
    public E get(int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int size() {
        return 0;
    }
}
