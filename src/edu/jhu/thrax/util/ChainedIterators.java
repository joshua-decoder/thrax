package edu.jhu.thrax.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ChainedIterators<T> implements Iterator<T> {
  
  private Iterator<? extends Iterator<T>> iteratorOfIterators;
  private Iterator<T> currentIterator;
  private boolean finished = false;
  
  public ChainedIterators(Iterator<? extends Iterator<T>> iteratorOfIterators) {
    this.iteratorOfIterators = iteratorOfIterators;
    moveToNextIterator();
  }
  
  public ChainedIterators(Collection<? extends Iterator<T>> iteratorOfIterators) {
    this.iteratorOfIterators = iteratorOfIterators.iterator();
    moveToNextIterator();
  }
  
  @Override
  public boolean hasNext() {
    if (finished) {
      return false;
    }
    if (currentIterator.hasNext()) {
      return true;
    } else {
      moveToNextIterator();
      return !finished;
    }
  }

  @Override
  public T next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return currentIterator.next();
  }
  
  private void moveToNextIterator() {
    while (iteratorOfIterators.hasNext()) {
      currentIterator = iteratorOfIterators.next();
      if (currentIterator.hasNext()) {
        finished = false;
        return;
      }
    }
    finished = true;
  }

}
