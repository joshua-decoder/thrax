package edu.jhu.thrax.lexprob;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;

import edu.jhu.thrax.util.ChainedIterators;

/**
 * A base class for lexical probability tables that will be read from a Hadoop sequence file that is
 * held on disk. This class serves to hide all the horrible Hadoop filesystem plumbing from more
 * concrete implementations of the lexprob table.
 * 
 * The constructor calls initialize with an Iterable that will range over all the (Long,Double)
 * pairs in a file glob.
 */
public abstract class SequenceFileLexprobTable {
  protected FileSystem fs;
  protected URI uri;
  protected FileStatus[] files;

  public SequenceFileLexprobTable(Configuration conf, String fileGlob) throws IOException {
    uri = URI.create(fileGlob);
    fs = FileSystem.get(uri, conf);
    files = fs.globStatus(new Path(fileGlob));
    if (files.length == 0) throw new IOException("no files found in lexprob glob:" + fileGlob);
    Arrays.sort(files); // some implementations (like local FS) don't return a sorted list of files
  }

  protected abstract void initialize(Iterable<TableEntry> entries);

  public abstract float get(int car, int cdr);

  public abstract boolean contains(int car, int cdr);
  
  /**
   * Return an Iterable that will range over all the entries in a series of globbed files.
   * 
   * @param fs the FileSystem
   * @param conf a Hadoop configuration file (to describe the filesystem)
   * @param files an array of FileStatus from getGlobStatus
   * @return an Iterable over all entries in all files in the files glob
   */
  protected static Iterable<TableEntry> getSequenceFileIterator(FileSystem fs,
      Configuration conf, FileStatus[] files) {
    return new Iterable<TableEntry>() {
      
      @Override
      public Iterator<TableEntry> iterator() {
        Iterator<? extends Iterator<TableEntry>> fileIterators = Arrays.asList(files).stream()
            .map(file -> {
              try {
                return new SequenceFile.Reader(fs, file.getPath(), conf);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
            .map(seqFile -> new SequenceFileTableEntryIterator(seqFile))
            .collect(Collectors.toList()).iterator();
        return new ChainedIterators<TableEntry>(fileIterators);
      }
    };
  }
}
