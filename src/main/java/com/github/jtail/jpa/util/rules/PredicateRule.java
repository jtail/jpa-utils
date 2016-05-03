package com.github.jtail.jpa.util.rules;

import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * A generic interface to use with JPA criteria API callback injections.
 *
 * @param <U>
 * @param <V>
 * @param <T>
 */
public interface PredicateRule<U, V, T extends AbstractQuery<? extends V>> {
    /**
     * Builds predicate.
     *
     * @param cb criteria builder
     * @param root root type in the from clause.
     * @param query
     * @return
     */
    Predicate apply(CriteriaBuilder cb, Root<? extends U> root, T query);
}
