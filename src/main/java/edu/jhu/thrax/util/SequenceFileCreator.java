package edu.jhu.thrax.util;

import java.net.URI;
import java.util.Scanner;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.MapFile;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile.Writer.Option;
public class SequenceFileCreator
{
  public static void main(String [] argv) throws Exception {
    LongWritable k = new LongWritable();
    Text v = new Text();

    URI uri = URI.create(argv[0]);
    Configuration conf = new Configuration();
    @SuppressWarnings("unused")
    FileSystem fs = FileSystem.get(uri, conf);
    Path path = new Path(argv[0]);
    Option fileOption = SequenceFile.Writer.file(path);
    Option keyClassOpt = MapFile.Writer.keyClass(Text.class);
    org.apache.hadoop.io.SequenceFile.Writer.Option valClassOpt = SequenceFile.Writer.valueClass(LongWritable.class);
    SequenceFile.Writer writer = SequenceFile.createWriter(conf, fileOption, keyClassOpt, valClassOpt);

    long current = 0;
    Scanner scanner = new Scanner(System.in, "UTF-8");
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      k.set(current);
      v.set(line);
      writer.append(k, v);
      current++;
    }
    scanner.close();
    writer.close();
    return;
  }
}
