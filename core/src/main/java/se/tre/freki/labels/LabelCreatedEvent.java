package se.tre.freki.labels;

/**
 * The event that should be published to an {@link com.google.common.eventbus.EventBus} when a label
 * has been created.
 */
public class LabelCreatedEvent extends LabelEvent {
  /**
   * Create an event for the label with the specified arguments. No arguments should be {@code
   * null}.
   *
   * @param id The id of the label that has been created
   * @param name The name of the label that has been created
   * @param type The type of the label that has been created
   */
  public LabelCreatedEvent(final LabelId id,
                           final String name,
                           final LabelType type) {
    super(id, name, type);
  }
}
