package com.tblf.gitdiff;

import com.google.common.collect.Range;

/**
 * Creates {@link Range}
 * @param <C>
 */
public class RangeFactory<C extends Comparable> {

    /**
     * Creates a {@link Range} object depending on the value of the parameters
     * @param from the first value
     * @param to the second value
     * @return a {@link Range}
     */
    public Range<C> createOpenOrSingletonRange(C from, C to) {

        if (from.equals(to))
            return Range.singleton(from);
        else
            return Range.open(from, to);

    }
}
