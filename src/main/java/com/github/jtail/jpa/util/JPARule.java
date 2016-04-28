package com.github.jtail.jpa.util;

import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * A very generic functional interface to use with JPA criteria API callback injections.
 */
@FunctionalInterface
public interface JPARule<T> {
    Predicate apply(CriteriaBuilder cb, AbstractQuery<? extends T> q, Root<? extends T> t);
}
