package edu.jhu.thrax.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.jerboa.util.FileManager;
import edu.jhu.thrax.util.io.LineReader;

public class JudgeParaphrases {
  
  private static final Logger LOG = LoggerFactory.getLogger(JudgeParaphrases.class);

  public static void main(String[] args) {

    String input = null;
    String output = null;

    for (int i = 0; i < args.length; i++) {
      if ("-i".equals(args[i]) && (i < args.length - 1)) {
        input = args[++i];
      } else if ("-o".equals(args[i]) && (i < args.length - 1)) {
        output = args[++i];
      }
    }

    if (input == null) {
      LOG.error("No input file specified.");
      return;
    }
    if (output == null) {
      LOG.error("No output file specified.");
      return;
    }

    LineReader reader = null;
    BufferedWriter writer = null;
    Scanner user = null;
    try {
      reader = new LineReader(input);
      writer = FileManager.getWriter(output);
      user = new Scanner(System.in, "UTF-8");
      while (reader.hasNext()) {
        String pp = reader.next().trim();
        LOG.info("{}\t", pp);
        String score = user.next().trim();
        if (score.toLowerCase(Locale.ROOT).equals("quit") || score.toLowerCase(Locale.ROOT).equals("exit"))
          break;
        writer.write(score + "\t" + pp + "\n");
      }
      reader.close();
      writer.close();
    } catch (IOException e) {
      LOG.error(e.getMessage());
    }
  }

}
