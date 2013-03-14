package edu.jhu.thrax.hadoop.features;

import java.util.Map;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;
import edu.jhu.thrax.util.Vocabulary;

public class WordLengthDifferenceFeature implements SimpleFeature {

  private static final Text LABEL = new Text("WordLenDiff");
  private static final DoubleWritable ZERO = new DoubleWritable(0);

  public void score(RuleWritable r, Map<Text, Writable> map) {
    int src_length = 0;
    int src_count = 0;
    for (int tok : r.source) {
      if (!Vocabulary.nt(tok)) {
        src_length += Vocabulary.word(tok).length();
        src_count++;
      }
    }
    int tgt_length = 0;
    int tgt_count = 0;
    for (int tok : r.target) {
      if (!Vocabulary.nt(tok)) {
        tgt_length += Vocabulary.word(tok).length();
        tgt_count++;
      }
    }
    if (src_count == 0 || tgt_count == 0) {
      map.put(LABEL, ZERO);
    } else {
      double avg_src_length = (double) src_length / src_count;
      double avg_tgt_length = (double) tgt_length / tgt_count;
      map.put(LABEL, new DoubleWritable(avg_tgt_length - avg_src_length));
    }
    return;
  }

  public void unaryGlueRuleScore(Text nt, Map<Text, Writable> map) {
    map.put(LABEL, ZERO);
  }

  public void binaryGlueRuleScore(Text nt, Map<Text, Writable> map) {
    map.put(LABEL, ZERO);
  }
}
