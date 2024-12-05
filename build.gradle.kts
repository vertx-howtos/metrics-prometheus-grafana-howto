plugins {
  java
  application
}

repositories {
  mavenCentral()
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:4.4.0"))
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-micrometer-metrics")
  implementation("io.micrometer:micrometer-registry-prometheus:1.10.5")
}

application {
  mainClass = "io.vertx.howtos.metrics.MainVerticle"
}
