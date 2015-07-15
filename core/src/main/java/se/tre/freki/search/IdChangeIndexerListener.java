package se.tre.freki.search;

import static com.google.common.util.concurrent.Futures.addCallback;

import se.tre.freki.labels.LabelCreatedEvent;
import se.tre.freki.labels.LabelDeletedEvent;
import se.tre.freki.meta.LabelMeta;
import se.tre.freki.plugins.PluginError;
import se.tre.freki.storage.Store;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Guava {@link com.google.common.eventbus.EventBus} listener that listens for label changes and
 * notifies the {@link SearchPlugin} when appropriate.
 */
public class IdChangeIndexerListener {
  private static final Logger LOG = LoggerFactory.getLogger(IdChangeIndexerListener.class);

  private final Store store;
  private final SearchPlugin searchPlugin;
  private final PluginError pluginError;

  /**
   * Create a guava event bus listener that listens for label change events and pushes them to the
   * search plugin.
   *
   * @param store The store to read label meta information from
   * @param searchPlugin The search plugin to push the information to
   */
  public IdChangeIndexerListener(final Store store,
                                 final SearchPlugin searchPlugin) {
    this.store = store;
    this.searchPlugin = searchPlugin;
    this.pluginError = new PluginError(searchPlugin);
  }

  /**
   * The method that subscribes to {@link LabelCreatedEvent}s. You should not call this directly,
   * post messages to the event bus that this listener is registered to instead.
   *
   * @param event The published event.
   */
  @Subscribe
  @AllowConcurrentEvents
  public final void recordLabelCreated(final LabelCreatedEvent event) {
    addCallback(store.getMeta(event.getId(), event.getType()),
        new FutureCallback<LabelMeta>() {
          @Override
          public void onSuccess(final LabelMeta meta) {
            LOG.info("Indexing {}", meta);
            addCallback(searchPlugin.indexLabelMeta(meta), pluginError);
          }

          @Override
          public void onFailure(final Throwable throwable) {
            LOG.error("Unable to fetch LabelMeta object for {}[{}]",
                event.getId(), event.getType(), throwable);
          }
        });
  }

  /**
   * The method that subscribes to {@link LabelDeletedEvent}s. You should not call this directly,
   * post messages to the event bus that this listener is registered to instead.
   *
   * @param event The published event.
   */
  @Subscribe
  @AllowConcurrentEvents
  public final void recordLabelDeleted(LabelDeletedEvent event) {
    LOG.info("Removing label with id {}, type {} and name {} from search index",
        event.getId(), event.getType(), event.getName());
    addCallback(searchPlugin.deleteLabelMeta(event.getId(), event.getType()),
        pluginError);
  }
}
