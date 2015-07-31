package se.tre.freki.labels;

import static com.google.common.util.concurrent.Futures.transform;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * An IdLookupStrategy defines some custom behavior to use when attempting to lookup the ID behind a
 * name.
 */
public interface IdLookupStrategy {
  /**
   * Fetch the ID behind the provided name using the provided {@link LabelClientTypeContext}
   * instance.
   *
   * @param labelClientTypeContext The LabelClientTypeContext instance to use for looking up the ID
   * @param name The name to find the ID behind
   * @return A future that on completion will contains the ID behind the name or a {@link
   * LabelException} if it does not exist
   */
  @Nonnull
  ListenableFuture<LabelId> getId(final LabelClientTypeContext labelClientTypeContext,
                                  final String name);

  /**
   * The most basic id lookup strategy that just fetches the ID behind the provided name without
   * providing any special behavior.
   */
  class SimpleIdLookupStrategy implements IdLookupStrategy {
    public static final IdLookupStrategy instance = new SimpleIdLookupStrategy();

    @Nonnull
    @Override
    public ListenableFuture<LabelId> getId(final LabelClientTypeContext labelClientTypeContext,
                                           final String name) {
      return transform(labelClientTypeContext.getId(name),
          new ToLabelIdOrThrow(name, labelClientTypeContext.type()));
    }
  }

  /**
   * An ID lookup strategy that will create an ID for the provided name if it does not already
   * exist.
   */
  class CreatingIdLookupStrategy implements IdLookupStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(CreatingIdLookupStrategy.class);
    public static final IdLookupStrategy instance = new CreatingIdLookupStrategy();

    @Nonnull
    @Override
    public ListenableFuture<LabelId> getId(final LabelClientTypeContext labelClientTypeContext,
                                           final String name) {
      return transform(labelClientTypeContext.getId(name),
          new AsyncFunction<Optional<LabelId>, LabelId>() {
            @Override
            public ListenableFuture<LabelId> apply(final Optional<LabelId> input) throws Exception {
              if (!input.isPresent()) {
                LOG.info("Creating missing label with name {} in context {}",
                    name, labelClientTypeContext);
                return labelClientTypeContext.createId(name);
              }

              return Futures.immediateFuture(input.get());
            }
          });
    }
  }

  /**
   * An ID lookup strategy that supports wildcards.
   *
   * <p>If the provided name is equal to "*" it will be interpreted as a
   * wildcard and it will return immediately with a future that contains {@code null}.
   *
   * <p>If the provided name is not interpreted as a wildcard as described above then a regular
   * lookup will be done.
   */
  class WildcardIdLookupStrategy implements IdLookupStrategy {
    public static final IdLookupStrategy instance = new WildcardIdLookupStrategy();

    @Nonnull
    @Override
    public ListenableFuture<LabelId> getId(final LabelClientTypeContext labelClientTypeContext,
                                           final String name) {
      if ("*".equals(name)) {
        return Futures.immediateFuture(null);
      }

      return transform(labelClientTypeContext.getId(name),
          new ToLabelIdOrThrow(name, labelClientTypeContext.type()));
    }
  }

  class ToLabelIdOrThrow implements AsyncFunction<Optional<LabelId>, LabelId> {
    private final String name;
    private final LabelType type;

    public ToLabelIdOrThrow(final String name,
                            final LabelType type) {
      this.name = name;
      this.type = type;
    }

    @Override
    public ListenableFuture<LabelId> apply(final Optional<LabelId> id) throws Exception {
      if (!id.isPresent()) {
        return Futures.immediateFailedFuture(
            new LabelException(name, type, "No label of type " + type + " with name " + name));
      }

      return Futures.immediateFuture(id.get());
    }
  }
}
