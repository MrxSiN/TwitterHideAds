package kotlinx.collections.immutable;

import java.util.List;

/** Stand-in for the R8-stripped persistent-list interface: no members survive. */
public interface ImmutableListMarker<E> extends List<E> {
}
