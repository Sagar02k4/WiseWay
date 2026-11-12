package com.wiseway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class WiseWayApplication {
  public static void main(String[] args) {
    createDatabaseDirectory();
    SpringApplication.run(WiseWayApplication.class, args);
  }

  private static void createDatabaseDirectory() {
    var configuredPath = System.getenv().getOrDefault("WISEWAY_DB_PATH", "./data/wiseway.db");
    Path databasePath = Paths.get(configuredPath).toAbsolutePath();
    try {
      Files.createDirectories(databasePath.getParent());
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to create the WiseWay database directory: " + databasePath.getParent(), exception);
    }
  }
}
