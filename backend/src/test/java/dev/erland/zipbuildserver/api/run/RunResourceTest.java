package dev.erland.zipbuildserver.api.run;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RunResourceTest {
    @Test
    void createsQueuedRunAndReturnsSummary() throws IOException {
        String sessionId = given()
                .contentType("application/json")
                .body("{}")
                .when().post("/api/sessions")
                .then()
                .statusCode(200)
                .extract().path("id");

        Path zip = createNodeZip();

        String packageId = given()
                .multiPart("file", "node-project.zip", zip.toFile(), "application/zip")
                .when().post("/api/sessions/{sessionId}/packages", sessionId)
                .then()
                .statusCode(201)
                .body("status", equalTo("ACCEPTED"))
                .extract().path("id");

        String runId = given()
                .contentType("application/json")
                .body("""
                        {
                          "packageId": "%s"
                        }
                        """.formatted(packageId))
                .when().post("/api/sessions/{sessionId}/runs", sessionId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("sessionId", equalTo(sessionId))
                .body("sourcePackageId", equalTo(packageId))
                .body("status", equalTo("QUEUED"))
                .body("planId", equalTo("node-default"))
                .body("commands", hasSize(0))
                .extract().path("id");

        given()
                .when().get("/api/runs/{runId}/summary", runId)
                .then()
                .statusCode(200)
                .body("runId", equalTo(runId))
                .body("status", equalTo("QUEUED"))
                .body("planId", equalTo("node-default"));
    }

    @Test
    void rejectedPackageCannotCreateRun() throws IOException {
        String sessionId = given()
                .contentType("application/json")
                .body("{}")
                .when().post("/api/sessions")
                .then()
                .statusCode(200)
                .extract().path("id");

        Path zip = createUnsafeZip();

        String packageId = given()
                .multiPart("file", "unsafe.zip", zip.toFile(), "application/zip")
                .when().post("/api/sessions/{sessionId}/packages", sessionId)
                .then()
                .statusCode(201)
                .body("status", equalTo("REJECTED"))
                .extract().path("id");

        given()
                .contentType("application/json")
                .body("""
                        {
                          "packageId": "%s"
                        }
                        """.formatted(packageId))
                .when().post("/api/sessions/{sessionId}/runs", sessionId)
                .then()
                .statusCode(400);
    }

    private static Path createNodeZip() throws IOException {
        Path zip = Files.createTempFile("node-project", ".zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            output.putNextEntry(new ZipEntry("package.json"));
            output.write("""
                    {
                      "scripts": {
                        "test": "echo test",
                        "build": "echo build"
                      }
                    }
                    """.getBytes());
            output.closeEntry();
        }
        return zip;
    }

    private static Path createUnsafeZip() throws IOException {
        Path zip = Files.createTempFile("unsafe", ".zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            output.putNextEntry(new ZipEntry("../escape.txt"));
            output.write("escape".getBytes());
            output.closeEntry();
        }
        return zip;
    }
}
