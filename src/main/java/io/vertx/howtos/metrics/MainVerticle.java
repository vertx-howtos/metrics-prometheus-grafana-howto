package io.vertx.howtos.metrics;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.ext.web.Router;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.PrometheusScrapingHandler;
import io.vertx.micrometer.VertxPrometheusOptions;

import java.util.concurrent.ThreadLocalRandom;

public class MainVerticle extends AbstractVerticle {

  private static final String[] GREETINGS = {
    "Hello world!",
    "Bonjour monde!",
    "Hallo Welt!",
    "Hola Mundo!"
  };

  @Override
  public void start() {
    vertx.eventBus().consumer("greetings", msg -> {
      // Simulate processing time between 20ms and 100ms
      long delay = ThreadLocalRandom.current().nextLong(80) + 20L;
      vertx.setTimer(delay, l -> {
        // Choose greeting
        String greeting = GREETINGS[ThreadLocalRandom.current().nextInt(GREETINGS.length)];
        msg.reply(greeting);
      });
    });

    Router router = Router.router(vertx);
    router.route("/metrics").handler(PrometheusScrapingHandler.create());
    router.get("/greeting").handler(rc -> {
      vertx.eventBus().<String>request("greetings", null, ar -> {
        if (ar.succeeded()) {
          Message<String> message = ar.result();
          rc.response().putHeader("content-type", "text/plain").end(message.body());
        } else {
          ar.cause().printStackTrace();
          rc.fail(500);
        }
      });
    });

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080);
  }

  // tag::main[]
  public static void main(String[] args) {
    MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
      .setEnabled(true)
      .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true));
    VertxOptions vertxOptions = new VertxOptions()
      .setMetricsOptions(metricsOptions);
    Vertx vertx = Vertx.vertx(vertxOptions);
    vertx.deployVerticle(new MainVerticle());
  }
  // end::main[]
}
