package edu.jhu.thrax.distributional;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.thrax.distributional.FeatureTypes.Label;
import edu.jhu.thrax.distributional.FeatureTypes.Type;
import edu.jhu.thrax.util.FormatUtils;

public class FeatureSet {
  
  private static final Logger LOG = LoggerFactory.getLogger(FeatureSet.class);

  private Set<FeatureClass> features;

  private boolean active[][];

  public FeatureSet() {
    features = new HashSet<FeatureClass>();
    active = new boolean[Type.values().length][Label.values().length];
  }

  public void addFeatureClass(String entry) {
    String[] fields = FormatUtils.P_DASH.split(entry);
    for (String f : fields) {
      LOG.info(f);
    }
  }

  public void addFeatureSet(FeatureSet set) {
    for (FeatureClass fc : set.features)
      this.features.add(fc);

    for (int i = 0; i < active.length; ++i)
      for (int j = 0; j < active[i].length; ++j)
        active[i][j] = active[i][j] || set.active[i][j];
  }

  public boolean active(Type type, Label label) {
    return active[type.code][label.code];
  }


}
