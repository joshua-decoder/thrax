package edu.jhu.thrax.hadoop.features.pivot;

import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.io.FloatWritable;

import edu.jhu.thrax.hadoop.datatypes.FeatureMap;
import edu.jhu.thrax.hadoop.features.mapred.SourcePhraseGivenTargetandLHSFeature;
import edu.jhu.thrax.hadoop.features.mapred.TargetPhraseGivenSourceandLHSFeature;

public class PivotedSourcePhraseGivenTargetAndLHSFeature extends PivotedNegLogProbFeature {

  public static final String NAME = SourcePhraseGivenTargetandLHSFeature.NAME; 
  
  public String getName() {
    return NAME;
  }

  public Set<String> getPrerequisites() {
    Set<String> prereqs = new HashSet<String>();
    prereqs.add(SourcePhraseGivenTargetandLHSFeature.NAME);
    prereqs.add(TargetPhraseGivenSourceandLHSFeature.NAME);
    return prereqs;
  }

  public FloatWritable pivot(FeatureMap src, FeatureMap tgt) {
    float fge = ((FloatWritable) src.get(TargetPhraseGivenSourceandLHSFeature.NAME)).get();
    float egf = ((FloatWritable) tgt.get(SourcePhraseGivenTargetandLHSFeature.NAME)).get();

    return new FloatWritable(egf + fge);
  }

  @Override
  public Set<String> getLowerBoundLabels() {
    Set<String> lower_bound_labels = new HashSet<String>();
    lower_bound_labels.add(TargetPhraseGivenSourceandLHSFeature.NAME);
    lower_bound_labels.add(SourcePhraseGivenTargetandLHSFeature.NAME);
    return lower_bound_labels;
  }

  @Override
  public Set<String> getUpperBoundLabels() {
    return null;
  }
}
