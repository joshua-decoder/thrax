// Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
package edu.jhu.thrax.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class HdfsUtils {
  
  private HdfsUtils() {};
  
  public static <E> void writeObjectToFs(Configuration conf, E object, Path outPath) throws IOException {
    FileSystem hdfs = FileSystem.get(conf);
    
    ObjectOutputStream oos = null;
    try {
      FSDataOutputStream out = hdfs.create(outPath);
      oos = new ObjectOutputStream(out);
      oos.writeObject(object);
    } finally {
      if (oos != null) {
        oos.close();
      }
    }
  }
  
  public static <E> E readObjectFromFs(Configuration conf, Path inPath) throws IOException,ClassNotFoundException {
    FileSystem hdfs = FileSystem.get(conf);

    ObjectInputStream ois = null;
    try {
      FSDataInputStream in = hdfs.open(inPath);
      ois = new ObjectInputStream(in);
      @SuppressWarnings("unchecked")
      E object = (E) ois.readObject();
      return object;
    } finally {
      if (ois != null) {
        ois.close();
      }
    }
  }
}