package com.github.jtail.jpa.util.rules;

import javax.persistence.criteria.AbstractQuery;

@FunctionalInterface
public interface RootPredicateRule<T> extends PredicateRule<T, T, AbstractQuery<? extends T>> {
}
