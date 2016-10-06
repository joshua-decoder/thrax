package edu.jhu.thrax.tools;

import java.io.BufferedWriter;
import java.util.Locale;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.io.SequenceFile.Reader.Option;

import edu.jhu.jerboa.util.FileManager;

public class SequenceToGrammar {

  private static final Logger LOG = LoggerFactory.getLogger(SequenceToGrammar.class);

  private static void usage() {
    String usage = "Usage: java edu.jhu.thrax.tools.SequenceToGrammar"
        + "\t -i sequence_file \t Sequence file from Thrax grammar extraction."
        + "\t -o output_file   \t Output grammar file name.";
    LOG.error(usage);
  }

  public static void main(String[] args) throws Exception {
    String input_file = null;
    String output_file = null;

    if (args.length < 4 || args[0].toLowerCase(Locale.ROOT).equals("-h")) {
      usage();
      System.exit(0);
    }
    for (int i = 0; i < args.length; i++) {
      if ("-i".equals(args[i]) && (i < args.length - 1)) {
        input_file = args[++i];
      } else if ("-o".equals(args[i]) && (i < args.length - 1)) {
        output_file = args[++i];
      }
    }
    if (input_file == null) {
      LOG.error("No input file specified.");
      usage();
      System.exit(0);
    }
    if (output_file == null) {
      LOG.error("No output file specified.");
      usage();
      System.exit(0);
    }

    Text rule_string = new Text();
    Configuration config = new Configuration();
    Path path = new Path(input_file);
    Option fFile = SequenceFile.Reader.file(path);
    SequenceFile.Reader reader = new SequenceFile.Reader(config, fFile);

    BufferedWriter grammar_writer = FileManager.getWriter(output_file);
    long rule_count = 0;
    while (reader.next(rule_string)) {
      grammar_writer.write(rule_string.toString());
      grammar_writer.newLine();
      rule_count++;
    }
    reader.close();
    grammar_writer.close();
    LOG.info("Merged {} rules.", rule_count);
  }
}
