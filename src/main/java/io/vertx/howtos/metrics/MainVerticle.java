package io.vertx.howtos.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.ext.web.Router;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.PrometheusScrapingHandler;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;

import java.util.concurrent.ThreadLocalRandom;

public class MainVerticle extends AbstractVerticle {

  // tag::greetings[]
  private static final String[] GREETINGS = {
    "Hello world!",
    "Bonjour monde!",
    "Hallo Welt!",
    "Hola Mundo!"
  };
  // end::greetings[]

  @Override
  public void start() {
    // tag::consumer[]
    vertx.eventBus().consumer("greetings", msg -> {
      // Simulate processing time between 20ms and 100ms
      long delay = ThreadLocalRandom.current().nextLong(80) + 20L;
      vertx.setTimer(delay, l -> {
        // Choose greeting
        String greeting = GREETINGS[ThreadLocalRandom.current().nextInt(GREETINGS.length)];
        msg.reply(greeting);
      });
    });
    // end::consumer[]

    // tag::router[]
    Router router = Router.router(vertx);
    // end::router[]
    // tag::handler[]
    router.get("/greeting").handler(rc -> {
      vertx.eventBus().<String>request("greetings", null)
        .map(Message::body)
        .onSuccess(greeting -> rc.response().putHeader("content-type", "text/plain").end(greeting))
        .onFailure(throwable -> {
          throwable.printStackTrace();
          rc.fail(500);
        });
    });
    // end::handler[]
    // tag::scraping[]
    router.route("/metrics").handler(PrometheusScrapingHandler.create());
    // end::scraping[]

    // tag::http[]
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080);
    // end::http[]
  }

  // tag::main[]
  public static void main(String[] args) {
    MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
      .setEnabled(true)
      .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true));
    VertxOptions vertxOptions = new VertxOptions()
      .setMetricsOptions(metricsOptions);
    Vertx vertx = Vertx.vertx(vertxOptions);

    /*
     After the Vert.x instance has been created,
     we can configure the metrics registry to enable histogram buckets
     for percentile approximations.
     */
    PrometheusMeterRegistry registry = (PrometheusMeterRegistry) BackendRegistries.getDefaultNow();
    registry.config().meterFilter(
      new MeterFilter() {
        @Override
        public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
          return DistributionStatisticConfig.builder()
            .percentilesHistogram(true)
            .build()
            .merge(config);
        }
      });

    vertx.deployVerticle(new MainVerticle());
  }
  // end::main[]
}
