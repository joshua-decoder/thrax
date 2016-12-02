package edu.jhu.thrax.lexprob;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;

public class SequenceFileTableEntryIterator implements Iterator<TableEntry> {
  
  private final SequenceFile.Reader reader;
  
  private final LongWritable pair = new LongWritable();
  private final FloatWritable d = new FloatWritable(0.0f);
  
  private Optional<TableEntry> lookahead = Optional.empty();
  private boolean finishedReading = false;
  
  public SequenceFileTableEntryIterator(SequenceFile.Reader reader) {
    this.reader = reader;
  }

  @Override
  public boolean hasNext() {
    if (lookahead.isPresent()) {
      return true;
    }
    lookahead = tryReadNext();
    if (lookahead.isPresent()) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public TableEntry next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    TableEntry nextEntry = lookahead.get();
    lookahead = Optional.empty();
    return nextEntry;
  }
  
  private Optional<TableEntry> tryReadNext() {
    if (finishedReading) {
      return Optional.empty();
    }
    try {
      boolean gotNew = reader.next(pair, d);
      if (gotNew) {
        // there was something to read
        return Optional.of(new TableEntry(pair, d));
      } else {
        finishedReading = true;
        return Optional.empty();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
