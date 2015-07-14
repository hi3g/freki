package se.tre.freki.storage.cassandra.search;

import com.google.common.collect.AbstractIterator;

/**
 * An implementation of a NGramGenerator. Takes a String as an input and
 * returns an instance of an AbstractIterator containing all 3-grams of
 * the supplied String.
 */
class NGramGenerator extends AbstractIterator<String> {
  private final String name;
  private int nameIndex;

  NGramGenerator(final String name) {
    this.name = name;
  }

  @Override
  protected String computeNext() {
    if (nameIndex < Math.max(name.length() - 2, 1)) {
      nameIndex++;
      return name.substring(nameIndex - 1, Math.min(nameIndex + 2, name.length()));
    }

    return endOfData();
  }
}
