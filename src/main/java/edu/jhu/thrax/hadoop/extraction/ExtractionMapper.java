package edu.jhu.thrax.hadoop.extraction;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.thrax.hadoop.datatypes.AlignedRuleWritable;
import edu.jhu.thrax.hadoop.datatypes.Annotation;
import edu.jhu.thrax.util.Vocabulary;

public class ExtractionMapper extends Mapper<LongWritable, Text, AlignedRuleWritable, Annotation> {
  private static final Logger LOG = LoggerFactory.getLogger(ExtractionMapper.class);
  private RuleWritableExtractor extractor;

  protected void setup(Context context) throws IOException, InterruptedException {
    Configuration conf = context.getConfiguration();
    String vocabulary_path = conf.getRaw("thrax.work-dir") + "vocabulary/part-*";
    Vocabulary.initialize(conf, vocabulary_path);

    // TODO: static initializer call for what Annotation actually carries would go here.
    extractor = RuleWritableExtractorFactory.create(context);
    if (extractor == null) {
      LOG.error("WARNING: could not create rule extractor as configured!");
    }
  }

  protected void map(LongWritable key, Text value, Context context) throws IOException,
      InterruptedException {
    if (extractor == null) return;
    for (AnnotatedRule ar : extractor.extract(value))
      context.write(new AlignedRuleWritable(ar.rule, ar.f2e), ar.annotation);
    context.progress();
  }
}
