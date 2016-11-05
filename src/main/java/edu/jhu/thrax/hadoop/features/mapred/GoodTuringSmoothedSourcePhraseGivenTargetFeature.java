package edu.jhu.thrax.hadoop.features.mapred;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;

import edu.jhu.thrax.hadoop.comparators.PrimitiveArrayMarginalComparator;
import edu.jhu.thrax.hadoop.datatypes.FeaturePair;
import edu.jhu.thrax.hadoop.datatypes.PrimitiveUtils;
import edu.jhu.thrax.hadoop.datatypes.RuleWritable;
import edu.jhu.thrax.hadoop.features.mapred.coc.CountOfCountsEstimator;
import edu.jhu.thrax.hadoop.features.mapred.coc.GoodTuringSmoother;
import edu.jhu.thrax.hadoop.jobs.ThraxJob;
import edu.jhu.thrax.util.HdfsUtils;
import edu.jhu.thrax.util.Vocabulary;

@SuppressWarnings("rawtypes")
public class GoodTuringSmoothedSourcePhraseGivenTargetFeature extends MapReduceFeature {

  public static final String NAME = "f_given_e_phrase_gt_smoothed";
  public static final String LABEL = "p_gt(f|e)";

  public String getName() {
    return NAME;
  }

  public String getLabel() {
    return LABEL;
  }
  
  @Override
  public Set<Class<? extends ThraxJob>> getPrerequisites() {
    Set<Class<? extends ThraxJob>> parentPrerequisites = super.getPrerequisites();
    Set<Class<? extends ThraxJob>> prerequisites = new HashSet<Class<? extends ThraxJob>>(parentPrerequisites.size()+1);
    prerequisites.add(CountOfRuleCountsEstimationJob.class);
    return prerequisites;
  }

  public Class<? extends WritableComparator> sortComparatorClass() {
    return SourcePhraseGivenTargetFeature.Comparator.class;
  }

  public Class<? extends Partitioner> partitionerClass() {
    return RuleWritable.TargetPartitioner.class;
  }

  public Class<? extends Mapper> mapperClass() {
    return SourcePhraseGivenTargetFeature.Map.class;
  }

  public Class<? extends Reducer> reducerClass() {
    return Reduce.class;
  }

  private static class Reduce extends Reducer<RuleWritable, IntWritable, RuleWritable, FeaturePair> {
    private int marginal;
    private FloatWritable prob;
    
    private GoodTuringSmoother goodTuringSmoother;

    protected void setup(Context context) throws IOException, InterruptedException {
      Configuration conf = context.getConfiguration();
      String vocabulary_path = conf.getRaw("thrax.work-dir") + "vocabulary/part-*";
      Vocabulary.initialize(conf, vocabulary_path);
      
      Path inPath = new Path(conf.getRaw("thrax.work-dir"),
          CountOfRuleCountsEstimationJob.COUNT_OF_COUNT_ESTIMATOR_OUTPUT_PATH);
      try {
        goodTuringSmoother = new GoodTuringSmoother(HdfsUtils.<CountOfCountsEstimator>readObjectFromFs(conf, inPath));
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    protected void reduce(RuleWritable key, Iterable<IntWritable> values, Context context)
        throws IOException, InterruptedException {
      if (Arrays.equals(key.source, PrimitiveArrayMarginalComparator.MARGINAL)) {
        marginal = 0;
        for (IntWritable x : values)
          marginal += x.get();
        return;
      }
      if (key.lhs == PrimitiveUtils.MARGINAL_ID) {
        int count = 0;
        for (IntWritable x : values)
          count += x.get();
        
        double smoothedCount = goodTuringSmoother.smoothedCount(count);
        
        prob = new FloatWritable((float) -Math.log(smoothedCount / (float) marginal));
        return;
      }
      context.write(key, new FeaturePair(Vocabulary.id(LABEL), prob));
    }

  }

  private static final FloatWritable ZERO = new FloatWritable(0.0f);

  public void unaryGlueRuleScore(int nt, java.util.Map<Integer, Writable> map) {
    map.put(Vocabulary.id(LABEL), ZERO);
  }

  public void binaryGlueRuleScore(int nt, java.util.Map<Integer, Writable> map) {
    map.put(Vocabulary.id(LABEL), ZERO);
  }
}
