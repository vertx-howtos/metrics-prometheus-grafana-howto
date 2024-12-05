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
  implementation(platform("io.vertx:vertx-stack-depchain:5.0.0.CR2"))
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-micrometer-metrics")
  implementation("io.micrometer:micrometer-registry-prometheus:1.14.1")
}

application {
  mainClass = "io.vertx.howtos.metrics.MainVerticle"
}
