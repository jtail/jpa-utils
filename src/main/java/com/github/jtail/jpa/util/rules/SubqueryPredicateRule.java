package com.github.jtail.jpa.util.rules;

import javax.persistence.criteria.Subquery;

/**
 *
 */
public interface SubqueryPredicateRule<U, V> extends PredicateRule<U, V, Subquery<V>> {
}
