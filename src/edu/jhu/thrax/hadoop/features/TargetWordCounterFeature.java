package edu.jhu.thrax.hadoop.features;

import java.util.Map;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;
import edu.jhu.thrax.util.Vocabulary;

public class TargetWordCounterFeature implements SimpleFeature {

  public static final String NAME = "target_word_count";

  private static final IntWritable ZERO = new IntWritable(0);

  public Writable score(RuleWritable r) {
    int words = 0;
    for (int tok : r.target)
      if (!Vocabulary.nt(tok)) words++;
    return new IntWritable(words);
  }

  public String getName() {
    return NAME;
  }

  public void unaryGlueRuleScore(int nt, Map<Integer, Writable> map) {
    map.put(Vocabulary.id(NAME), ZERO);
  }

  public void binaryGlueRuleScore(int nt, Map<Integer, Writable> map) {
    map.put(Vocabulary.id(NAME), ZERO);
  }
}
