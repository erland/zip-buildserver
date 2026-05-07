package info.isaksson.erland.zipbuildserver.api.session;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SessionResourceTest {
    @Test
    void createsReadsListsAndClosesSession() {
        String sessionId = given()
                .contentType("application/json")
                .body("""
                        {
                          "label": "Assistant verification run",
                          "retentionPolicy": "default"
                        }
                        """)
                .when().post("/api/sessions")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("label", equalTo("Assistant verification run"))
                .body("status", equalTo("OPEN"))
                .body("retentionPolicy", equalTo("default"))
                .extract().path("id");

        given()
                .when().get("/api/sessions/{sessionId}", sessionId)
                .then()
                .statusCode(200)
                .body("id", equalTo(sessionId))
                .body("status", equalTo("OPEN"));

        given()
                .when().get("/api/sessions")
                .then()
                .statusCode(200)
                .body("sessions.findAll { it.id == '%s' }".formatted(sessionId), hasSize(1));

        given()
                .when().post("/api/sessions/{sessionId}/close", sessionId)
                .then()
                .statusCode(200)
                .body("id", equalTo(sessionId))
                .body("status", equalTo("CLOSED"))
                .body("closedAt", notNullValue());
    }

    @Test
    void unknownSessionReturnsControlledNotFound() {
        given()
                .when().get("/api/sessions/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404)
                .body("code", equalTo("not_found"));
    }
}
