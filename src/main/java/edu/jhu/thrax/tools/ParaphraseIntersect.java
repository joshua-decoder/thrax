package edu.jhu.thrax.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.jerboa.util.FileManager;
import edu.jhu.thrax.util.FormatUtils;
import edu.jhu.thrax.util.io.LineReader;

public class ParaphraseIntersect {

  private static final Logger LOG = LoggerFactory.getLogger(ParaphraseIntersect.class);
  private static final Pattern P_SPACE = Pattern.compile("\\s+");
  private static final Pattern P_EQUAL = Pattern.compile("=");

  public static void main(String[] args) {

    String grammar_file = null;
    String reference_file = null;
    String weight_file = null;
    String output_file = null;

    int threshold = 0;

    boolean identity = false;

    for (int i = 0; i < args.length; i++) {
      if ("-g".equals(args[i]) && (i < args.length - 1)) {
        grammar_file = args[++i];
      } else if ("-r".equals(args[i]) && (i < args.length - 1)) {
        reference_file = args[++i];
      } else if ("-w".equals(args[i]) && (i < args.length - 1)) {
        weight_file = args[++i];
      } else if ("-o".equals(args[i]) && (i < args.length - 1)) {
        output_file = args[++i];
      } else if ("-t".equals(args[i]) && (i < args.length - 1)) {
        threshold = Integer.parseInt(args[++i]);
      } else if ("-i".equals(args[i])) {
        identity = true;
      }
    }

    if (grammar_file == null) {
      LOG.error("No grammar specified.");
      return;
    }
    if (reference_file == null) {
      LOG.error("No reference file specified.");
      return;
    }
    if (weight_file == null) {
      LOG.error("No weight file specified.");
      return;
    }
    if (output_file == null) {
      LOG.error("No output file specified.");
      return;
    }

    HashMap<String, Double> weights = new HashMap<String, Double>();

    try {
      LineReader weights_reader = new LineReader(weight_file);
      while (weights_reader.hasNext()) {
        String line = weights_reader.next().trim();
        if (line.isEmpty()) continue;
        String[] fields = P_SPACE.split(line);
        weights.put(fields[0], Double.parseDouble(fields[1]));
      }
      weights_reader.close();


      ArrayList<Double> missed = new ArrayList<Double>();
      ArrayList<Double> found = new ArrayList<Double>();

      LineReader gread = new LineReader(grammar_file);
      LineReader rread = new LineReader(reference_file);

      String rline = null;
      
      // TODO: fix sorting to comply with UNIX sort. Likely: LC_COLLATE=C and String.compareTo()
      Collator comp = Collator.getInstance(Locale.US);

      LOG.error("[");
      int num_references = 0;
      while (gread.hasNext()) {
        String rule_line = gread.next().trim();
        String[] fields = FormatUtils.P_DELIM.split(rule_line);
        if (rule_line.contains("[X") || (!identity && fields[3].contains("Identity=1"))) continue;

        String rule = fields[0] + " ||| " + fields[1] + " ||| " + fields[2];

        double score = 0;
        String[] features = P_SPACE.split(fields[3]);
        for (String f : features) {
          String[] parts = P_EQUAL.split(f);
          if (weights.containsKey(parts[0])) {
            // TODO: awful hack. fix this.
            double value = Math.abs(Double.parseDouble(parts[1]));
            score += weights.get(parts[0]) * value;
          }
        }

        while (rread.hasNext() && (rline == null || comp.compare(rule, rline) > 0)) {
          String line = rread.next().trim();
          String[] rfs = FormatUtils.P_DELIM.split(line);
          double rarity = Double.parseDouble(rfs[3]);
          int count = (int) Math.round(1 - Math.log(rarity));
          if (count >= threshold && !line.contains("[X") && (identity || !rfs[1].equals(rfs[2]))) {
            rline = rfs[0] + " ||| " + rfs[1] + " ||| " + rfs[2];
            num_references++;
          }
        }
        
        if (comp.compare(rule, rline) == 0) {
          found.add(score);
        } else {
          missed.add(score);
        }
      }
      gread.close();
      LOG.error("]");

      while (rread.hasNext()) {
        rread.next();
        num_references++;
      }
      rread.close();

      double[] matched = new double[found.size()];
      int i = 0;
      for (double s : found)
        matched[i++] = s;
      found = null;

      i = 0;
      double[] unmatched = new double[missed.size()];
      for (double s : missed)
        unmatched[i++] = s;
      missed = null;

      int num_correct = matched.length;
      int num_paraphrases = matched.length + unmatched.length;

      LOG.info("References:  {}", num_references);
      LOG.info("Matched:     {}", num_correct);
      LOG.info("Unmatched:   {}", (num_references - num_correct));
      LOG.info("Nonmatching: {}", unmatched.length);

      Arrays.sort(matched);
      Arrays.sort(unmatched);

      int m = 0, u = 0;
      BufferedWriter score_writer = FileManager.getWriter(output_file);
      while (m < matched.length && u < unmatched.length) {
        if (matched[m] < unmatched[u]) {
          // Score - Recall - Precision - Count
          if (m % 20 == 0)
            score_writer.write(matched[m] + "\t" + (num_correct / (double) num_references) + "\t"
                + (num_correct / (double) num_paraphrases) + "\t" + num_paraphrases + "\n");
          m++;
          num_correct--;
        } else {
          u++;
        }
        num_paraphrases--;
      }
      score_writer.close();
    } catch (IOException e) {
      LOG.error(e.getMessage());
    }
  }
}
