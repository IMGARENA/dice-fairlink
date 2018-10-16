/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.iterators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

public class RandomisedCyclicIterator<T> implements Iterator<T> {

  private final List<T> elements;
  private Iterator<T> iterator;

  protected RandomisedCyclicIterator(Collection<? extends T> collection) {
    this.elements = new ArrayList<>(collection.size());
    this.elements.addAll(collection);
    Collections.shuffle(elements, ThreadLocalRandom.current());
    this.iterator = this.elements.iterator();
  }

  public static <T> RandomisedCyclicIterator<T> of(Collection<? extends T> collection) {
    return new RandomisedCyclicIterator<>(collection);
  }

  @Override
  public boolean hasNext() {
    return !elements.isEmpty();
  }

  @Override
  public synchronized T next() {
    if (!iterator.hasNext()) {
      iterator = elements.iterator();
      if (!iterator.hasNext()) {
        throw new NoSuchElementException();
      }
    }
    T next = iterator.next();
    return next;
  }

}