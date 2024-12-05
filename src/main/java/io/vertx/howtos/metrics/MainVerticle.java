package io.vertx.howtos.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.micrometer.MicrometerMetricsFactory;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.PrometheusScrapingHandler;
import io.vertx.micrometer.VertxPrometheusOptions;

import java.util.concurrent.ThreadLocalRandom;

public class MainVerticle extends VerticleBase {

  // tag::greetings[]
  private static final String[] GREETINGS = {
    "Hello world!",
    "Bonjour monde!",
    "Hallo Welt!",
    "Hola Mundo!"
  };
  // end::greetings[]

  @Override
  public Future<?> start() {
    // tag::consumer[]
    Future<Void> registration = vertx.eventBus().consumer("greetings", msg -> {
      // Simulate processing time between 20ms and 100ms
      long delay = ThreadLocalRandom.current().nextLong(80) + 20L;
      vertx.setTimer(delay, l -> {
        // Choose greeting
        String greeting = GREETINGS[ThreadLocalRandom.current().nextInt(GREETINGS.length)];
        msg.reply(greeting);
      });
    }).completion();
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
    Future<HttpServer> httpServer = vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080);
    // end::http[]

    return Future.join(registration, httpServer);
  }

  // tag::main[]
  public static void main(String[] args) {
    /*
     We can configure the metrics registry to enable histogram buckets
     for percentile approximations.
     */
    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
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

    Vertx vertx = Vertx.builder()
      .with(new VertxOptions().setMetricsOptions(new MicrometerMetricsOptions()
        .setEnabled(true)
        .setPrometheusOptions(new VertxPrometheusOptions()
          .setEnabled(true))
      ))
      .withMetrics(new MicrometerMetricsFactory(registry))
      .build();

    vertx.deployVerticle(new MainVerticle()).await();
    System.out.println("Verticle started");
  }
  // end::main[]
}
