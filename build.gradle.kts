plugins {
  java
  application
}

repositories {
  mavenCentral()
}

dependencies {
  val vertxVersion = "4.0.0"
  implementation("io.vertx:vertx-web:${vertxVersion}")
  implementation("io.vertx:vertx-micrometer-metrics:${vertxVersion}")
  implementation("io.micrometer:micrometer-registry-prometheus:1.5.2")
}

application {
  mainClassName = "io.vertx.howtos.metrics.MainVerticle"
}

tasks.wrapper {
  gradleVersion = "6.8"
}
