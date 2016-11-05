package edu.jhu.thrax.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSetFilter {
  private List<String> testSentences;
  private Map<String, Set<Integer>> sentencesByWord;
  private Set<String> ngrams;
  private static final Logger LOG = LoggerFactory.getLogger(TestSetFilter.class);

  // for caching of accepted rules
  private String lastSourceSide;
  private boolean acceptedLastSourceSide;

  private final String NT_REGEX = "\\[[^\\]]+?\\]";

  public int cached = 0;
  public int RULE_LENGTH = 12;
  public boolean verbose = false;
  public boolean parallel = false;
  public boolean fast = false;

  public TestSetFilter() {
    testSentences = new ArrayList<String>();
    sentencesByWord = new HashMap<String, Set<Integer>>();
    acceptedLastSourceSide = false;
    lastSourceSide = null;
  }

  public void setVerbose(boolean value) {
    verbose = value;
  }

  public void setParallel(boolean value) {
    parallel = value;
  }

  public void setFast(boolean value) {
    fast = value;
  }

  public void setRuleLength(int value) {
    RULE_LENGTH = value;
  }

  private void getTestSentences(String filename) {
    try {
      @SuppressWarnings("resource")
      Scanner scanner = new Scanner(new File(filename), "UTF-8");
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        addSentenceToWordHash(sentencesByWord, line, testSentences.size());
        testSentences.add(line);
      }
    } catch (FileNotFoundException e) {
      LOG.error("Could not open {}\n", e.getMessage());
    }

    if (verbose) LOG.info("Added {} sentences.\n", testSentences.size());

    ngrams = getTestNGrams(testSentences);
  }

  /**
   * setSentence()
   * 
   * Sets a single sentence against which the grammar is filtered. Used in filtering the grammar on
   * the fly at runtime.
   */
  public void setSentence(String sentence) {
    if (testSentences == null) testSentences = new ArrayList<String>();

    if (sentencesByWord == null) sentencesByWord = new HashMap<String, Set<Integer>>();

    // reset the list of sentences and the hash mapping words to
    // sets of sentences they appear in
    testSentences.clear();
    sentencesByWord.clear();
    // fill in the hash with the current sentence
    addSentenceToWordHash(sentencesByWord, sentence, 0);
    // and add the sentence
    testSentences.add(sentence);

    ngrams = getTestNGrams(testSentences);
  }

  /**
   * filterGrammarToFile
   * 
   * Filters a large grammar against a single sentence, and writes the resulting grammar to a file.
   * The input grammar is assumed to be compressed, and the output file is also compressed.
   */
  public void filterGrammarToFile(String fullGrammarFile, String sentence,
      String filteredGrammarFile, boolean fast) {

    LOG.error("filterGrammarToFile({},{},{},{})\n", fullGrammarFile,
        sentence, filteredGrammarFile, (fast ? "fast" : "exact"));

    this.fast = fast;
    setSentence(sentence);

    try {
      @SuppressWarnings("resource")
      Scanner scanner =
          new Scanner(new GZIPInputStream(new FileInputStream(fullGrammarFile)), "UTF-8");
      int rulesIn = 0;
      int rulesOut = 0;
      boolean verbose = false;
      if (verbose) LOG.info("Processing rules...");

      PrintWriter out =
          new PrintWriter(
              new OutputStreamWriter(
                  new GZIPOutputStream(
                      new FileOutputStream(filteredGrammarFile)), StandardCharsets.UTF_8));

      // iterate over all lines in the grammar
      while (scanner.hasNextLine()) {
        if (verbose) {
          if ((rulesIn + 1) % 2000 == 0) {
            LOG.info(".");
          }
          if ((rulesIn + 1) % 100000 == 0) {
            LOG.info(" [{}] ", (rulesIn + 1) );
          }
        }
        rulesIn++;
        String rule = scanner.nextLine();
        if (inTestSet(rule)) {
          out.println(rule);
          rulesOut++;
        }
      }

      out.close();

      if (verbose) {
        LOG.info("[INFO] Total rules read: {}", rulesIn);
        LOG.info("[INFO] Rules kept: {}", rulesOut);
        LOG.info("[INFO] Rules dropped: {}", (rulesIn - rulesOut));
      }
    } catch (FileNotFoundException e) {
      LOG.error("* FATAL: could not open {}\n", e.getMessage());
    } catch (IOException e) {
      LOG.error("* FATAL: could not write to {}\n", e.getMessage());
    }
  }

  public Pattern getPattern(String rule) {
    String[] parts = FormatUtils.P_DELIM.split(rule);
    if (parts.length != 4) {
      return null;
    }
    String source = parts[1].trim();
    String pattern = Pattern.quote(source);
    pattern = pattern.replaceAll(NT_REGEX, "\\\\E.+\\\\Q");
    pattern = pattern.replaceAll("\\\\Q\\\\E", "");
    pattern = "(?:^|\\s)" + pattern + "(?:$|\\s)";
    return Pattern.compile(pattern);
  }

  /**
   * Top-level filter, responsible for calling the fast or exact version.
   */
  public boolean inTestSet(String rule) {
    String[] parts = FormatUtils.P_DELIM.split(rule);
    if (parts.length != 4) return false;

    String sourceSide = parts[1].trim();
    if (!sourceSide.equals(lastSourceSide)) {
      lastSourceSide = sourceSide;
      acceptedLastSourceSide = fast ? inTestSetFast(rule) : inTestSetExact(rule);
    } else {
      cached++;
    }

    return acceptedLastSourceSide;
  }



  private boolean inTestSetFast(String rule) {

    String[] parts = FormatUtils.P_DELIM.split(rule);
    String source = parts[1];

    for (String chunk : source.split(NT_REGEX)) {
      chunk = chunk.trim();
      /* Important: you need to make sure the string isn't empty. */
      if (!chunk.equals("") && !ngrams.contains(chunk)) return false;
    }
    return true;
  }

  private boolean inTestSetExact(String rule) {
    if (inTestSetFast(rule)) {
      Pattern pattern = getPattern(rule);
      for (int i : getSentencesForRule(sentencesByWord, rule)) {
        if (pattern.matcher(testSentences.get(i)).find()) {
          return true;
        }
      }
      return hasAbstractSource(rule) > 1;
    }
    return false;
  }

  private static void addSentenceToWordHash(Map<String, Set<Integer>> sentencesByWord,
      String sentence, int index) {
    String[] tokens = sentence.split("\\s+");
    for (String t : tokens) {
      if (sentencesByWord.containsKey(t))
        sentencesByWord.get(t).add(index);
      else {
        Set<Integer> set = new HashSet<Integer>();
        set.add(index);
        sentencesByWord.put(t, set);
      }
    }
  }

  private Set<Integer> getSentencesForRule(Map<String, Set<Integer>> sentencesByWord, String rule) {
    String[] parts = FormatUtils.P_DELIM.split(rule);
    if (parts.length != 4) return Collections.emptySet();
    String source = parts[1].trim();
    List<Set<Integer>> list = new ArrayList<Set<Integer>>();
    for (String t : source.split("\\s+")) {
      if (t.matches(NT_REGEX)) continue;
      if (sentencesByWord.containsKey(t))
        list.add(sentencesByWord.get(t));
      else
        return Collections.emptySet();
    }
    return intersect(list);
  }

  /**
   * Determines whether a rule is an abstract rule. An abstract rule is one that has no terminals on
   * its source side.
   * 
   * If the rule is abstract, the rule's arity is returned. Otherwise, 0 is returned.
   */
  private int hasAbstractSource(String rule) {
    String[] parts = FormatUtils.P_DELIM.split(rule);
    if (parts.length != 4) return 0;
    String source = parts[1].trim();
    int nonterminalCount = 0;
    for (String t : source.split("\\s+")) {
      if (!t.matches(NT_REGEX)) return 0;
      nonterminalCount++;
    }
    return nonterminalCount;
  }

  private static <T> Set<T> intersect(List<Set<T>> list) {
    if (list.isEmpty()) return Collections.emptySet();
    Set<T> result = new HashSet<T>(list.get(0));
    for (int i = 1; i < list.size(); i++) {
      result.retainAll(list.get(i));
      if (result.isEmpty()) return Collections.emptySet();
    }
    if (result.isEmpty()) return Collections.emptySet();
    return result;
  }

  private Set<String> getTestNGrams(List<String> sentences) {
    if (sentences.isEmpty()) return Collections.emptySet();
    Set<String> result = new HashSet<String>();
    for (String s : sentences)
      result.addAll(getNGramsUpToLength(RULE_LENGTH, s));
    return result;
  }

  private static Set<String> getNGramsUpToLength(int length, String sentence) {
    if (length < 1) return Collections.emptySet();
    String[] tokens = sentence.trim().split("\\s+");
    int maxOrder = length < tokens.length ? length : tokens.length;
    Set<String> result = new HashSet<String>();
    for (int order = 1; order <= maxOrder; order++) {
      for (int start = 0; start < tokens.length - order + 1; start++)
        result.add(createNGram(tokens, start, order));
    }
    return result;
  }

  private static String createNGram(String[] tokens, int start, int order) {
    if (order < 1 || start + order > tokens.length) {
      return "";
    }
    String result = tokens[start];
    for (int i = 1; i < order; i++)
      result += " " + tokens[start + i];
    return result;
  }

  public static void main(String[] argv) {
    // do some setup
    if (argv.length < 1) {
      String usage = ("usage: TestSetFilter [-v|-p|-f|-n N] <test set1> [test set2 ...]\n"
          + "    -v    verbose output\n"
          + "    -p    parallel compatibility\n"
          + "    -f    fast mode\n"
          + "    -n    max n-gram to compare to (default 12)\n");
      LOG.error(usage);
      return;
    }

    TestSetFilter filter = new TestSetFilter();

    for (int i = 0; i < argv.length; i++) {
      if (argv[i].equals("-v")) {
        filter.setVerbose(true);
        continue;
      } else if (argv[i].equals("-p")) {
        filter.setParallel(true);
        continue;
      } else if (argv[i].equals("-f")) {
        filter.setFast(true);
        continue;
      } else if (argv[i].equals("-n")) {
        filter.setRuleLength(Integer.parseInt(argv[i + 1]));
        i++;
        continue;
      }
      filter.getTestSentences(argv[i]);
    }

    @SuppressWarnings("resource")
    Scanner scanner = new Scanner(System.in, "UTF-8");
    int rulesIn = 0;
    int rulesOut = 0;
    if (filter.verbose) {
      LOG.info("Processing rules...");
      if (filter.fast) LOG.info("Using fast version...");
      LOG.info("Using at max {} n-grams...", filter.RULE_LENGTH);
    }
    while (scanner.hasNextLine()) {
      if (filter.verbose) {
        if ((rulesIn + 1) % 2000 == 0) {
          LOG.info(".");
        }
        if ((rulesIn + 1) % 100000 == 0) {
          LOG.info(" [{}]", (rulesIn + 1));
        }
      }
      rulesIn++;
      String rule = scanner.nextLine();

      if (filter.inTestSet(rule)) {
        LOG.info(rule);
        if (filter.parallel);
        rulesOut++;
      } else if (filter.parallel) {
        LOG.info("");
      }
    }
    if (filter.verbose) {
      LOG.info("[INFO] Total rules read: {}", rulesIn);
      LOG.info("[INFO] Rules kept: {}", rulesOut);
      LOG.info("[INFO] Rules dropped: {}", (rulesIn - rulesOut));
      LOG.info("[INFO] cached queries: {}", filter.cached);
    }

    return;
  }
}
