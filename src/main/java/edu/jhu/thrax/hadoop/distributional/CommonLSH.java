package edu.jhu.thrax.hadoop.distributional;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.jerboa.sim.SLSH;

public class CommonLSH {
  
  private static final Logger LOG = LoggerFactory.getLogger(CommonLSH.class);

  public static SLSH getSLSH(Configuration conf) {
    SLSH slsh = null;
    try {
      slsh = new SLSH(true);
    } catch (Exception e) {
      LOG.error(e.getMessage());
      System.exit(1);
    }
    return slsh;
  }

}
