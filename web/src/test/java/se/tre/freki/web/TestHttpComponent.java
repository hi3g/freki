package se.tre.freki.web;

import se.tre.freki.web.jackson.AnnotationMixInTest;
import se.tre.freki.web.jackson.LabelMetaMixInTest;

import dagger.Component;

import javax.inject.Singleton;

@Component(modules = HttpModule.class)
@Singleton
public interface TestHttpComponent {
  void inject(AnnotationMixInTest annotationMixInTest);

  void inject(LabelMetaMixInTest labelMetaMixInTest);
}
