package edu.jhu.thrax.hadoop.features.mapred;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.thrax.hadoop.datatypes.Annotation;
import edu.jhu.thrax.hadoop.datatypes.RuleWritable;
import edu.jhu.thrax.hadoop.features.mapred.coc.CountOfCountsEstimator;
import edu.jhu.thrax.hadoop.jobs.ExtractionJob;
import edu.jhu.thrax.hadoop.jobs.ThraxJob;
import edu.jhu.thrax.util.HdfsUtils;

@SuppressWarnings("rawtypes")
public class CountOfRuleCountsEstimationJob implements ThraxJob {
  
  private static final Logger LOG = LoggerFactory.getLogger(CountOfRuleCountsEstimationJob.class);
  
  //single reducer, as this is where we carry out the regression for which we need all data in a central location
  private static final int SINGLE_REDUCER = 1;
  
  public static final String NAME = "rule_count_of_counts";
  
  public static final String COUNT_OF_COUNT_ESTIMATOR_OUTPUT_PATH = "count-of-counts-estimator";
  
  @Override
  public String getName() {
    return NAME;
  }
  
  @Override
  public String getOutputSuffix() {
    return getName();
  }
  
  @Override
  public Set<Class<? extends ThraxJob>> getPrerequisites() {
    Set<Class<? extends ThraxJob>> result = new HashSet<>();
    result.add(ExtractionJob.class);
    return result;
  }
  
  @Override
  public Job getJob(Configuration conf) throws IOException {
    String name = getName();
    Job job = Job.getInstance(conf, name);
    job.setJarByClass(this.getClass());

    job.setMapperClass(this.mapperClass());
    job.setCombinerClass(IntSumReducer.class);
    job.setReducerClass(CountOfCountsRegressionReducer.class);

    job.setInputFormatClass(SequenceFileInputFormat.class);
    job.setMapOutputKeyClass(IntWritable.class);
    job.setMapOutputValueClass(IntWritable.class);
    
    job.setOutputFormatClass(SequenceFileOutputFormat.class);
    job.setOutputKeyClass(IntWritable.class);
    job.setOutputValueClass(IntWritable.class);
    
    job.setNumReduceTasks(SINGLE_REDUCER);

    FileInputFormat.setInputPaths(job, new Path(conf.get("thrax.work-dir") + "rules"));
    FileOutputFormat.setOutputPath(job, new Path(conf.get("thrax.work-dir") + name));
    return job;
  }

  public Class<? extends Mapper> mapperClass() {
    return CustomMap.class;
  }

  private static class CustomMap extends Mapper<RuleWritable, Annotation, IntWritable, IntWritable> {
    
    private final static IntWritable ONE = new IntWritable(1);
    
    protected void map(RuleWritable key, Annotation value, Context context) throws IOException,
        InterruptedException {
      IntWritable count = new IntWritable(value.count());
      context.write(count, ONE);
    }
  }

  /**
   * Writes counts of counts and produces a linear regression of the log-log plot of the data.
   */
  private static class CountOfCountsRegressionReducer extends IntSumReducer<IntWritable> {
    
    private Map<Integer, Integer> countOfCounts = new HashMap<>();
    
    public void reduce(IntWritable key, Iterable<IntWritable> values, Context context)
        throws IOException, InterruptedException {
      if (countOfCounts.containsKey(key.get())) {
        throw new RuntimeException(
            String.format(Locale.ROOT, "Duplicate key %d in counts of counts.", key.get()));
      }
      
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      countOfCounts.put(key.get(), sum);
    }
    
    @Override
    protected void cleanup(
        Reducer<IntWritable, IntWritable, IntWritable, IntWritable>.Context context)
        throws IOException, InterruptedException {
      CountOfCountsEstimator estimator = CountOfCountsEstimator.regress(countOfCounts);
      LOG.error("Created CountOfCountsEstimator with slope {} and intercept {}",
              estimator.getSlope(), estimator.getIntercept());
      
      Configuration conf = context.getConfiguration();
      Path outPath = new Path(conf.getRaw("thrax.work-dir"), COUNT_OF_COUNT_ESTIMATOR_OUTPUT_PATH);
      
      HdfsUtils.writeObjectToFs(conf, estimator, outPath);
    }
  }
}