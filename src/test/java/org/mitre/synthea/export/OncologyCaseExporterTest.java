package org.mitre.synthea.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.mitre.synthea.engine.OncologyScenario;
import org.mitre.synthea.engine.OncologyScenario.Complexity;
import org.mitre.synthea.engine.OncologyScenario.Scenario;

public class OncologyCaseExporterTest {
  private static final LocalDate SNAPSHOT = LocalDate.of(2025, 6, 30);

  private JsonObject generate(int index, Scenario scenario, Complexity complexity) {
    return OncologyCaseExporter.export(OncologyScenario.simulate(42, index,
        SNAPSHOT, scenario, complexity));
  }

  @Test
  public void everyScenarioAndDepthHasAClosedReadableGraph() {
    for (Scenario scenario : Scenario.values()) {
      for (Complexity complexity : Complexity.values()) {
        for (int seed = 0; seed < 10; seed++) {
          JsonObject bundle = generate(seed, scenario, complexity);
          Set<String> identities = new HashSet<>();
          for (JsonElement entry : bundle.getAsJsonArray("entry")) {
            JsonObject resource = entry.getAsJsonObject().getAsJsonObject("resource");
            assertTrue(identities.add(resource.get("resourceType").getAsString()
                + "/" + resource.get("id").getAsString()));
          }
          referencesResolve(bundle, identities);
          FhirContext.forR4Cached().newJsonParser().parseResource(bundle.toString());
          assertEquals(bundle, generate(seed, scenario, complexity));
        }
      }
    }
  }

  private void referencesResolve(JsonElement element, Set<String> identities) {
    if (element.isJsonArray()) {
      element.getAsJsonArray().forEach(child -> referencesResolve(child, identities));
    } else if (element.isJsonObject()) {
      JsonObject object = element.getAsJsonObject();
      if (object.has("reference")) {
        assertTrue(object.get("reference").getAsString(),
            identities.contains(object.get("reference").getAsString()));
      }
      object.entrySet().forEach(entry -> referencesResolve(entry.getValue(), identities));
    }
  }

  @Test
  public void simulationMaintainsClinicalAndTemporalRelationships() {
    for (Scenario scenario : Scenario.values()) {
      for (Complexity complexity : Complexity.values()) {
        var journey = OncologyScenario.simulate(42, 0, SNAPSHOT, scenario, complexity);
        var events = journey.events();
        for (int index = 0; index < events.size(); index++) {
          var event = events.get(index);
          assertFalse(event.date().isAfter(SNAPSHOT));
          if (index > 0) {
            assertFalse(event.date().isBefore(events.get(index - 1).date()));
          }
          if (event.cause() != null) {
            var cause = events.stream().filter(e -> e.key().equals(event.cause()))
                .findFirst().orElseThrow();
            assertFalse(event.date().isBefore(cause.date()));
          }
        }
        assertTrue(events.stream().anyMatch(e -> e.kind().equals("task")));
        if (scenario == Scenario.AWAITING_PATHOLOGY) {
          assertFalse(events.stream().anyMatch(e -> e.kind().equals("condition")
              || e.kind().equals("therapy")));
          assertTrue(events.stream().anyMatch(e -> e.kind().equals("report")
              && e.status().equals("registered")));
        }
        if (scenario == Scenario.DISCORDANT_MARKERS) {
          var primary = events.stream().filter(e -> e.key().equals("primary-her2"))
              .findFirst().orElseThrow();
          var metastatic = events.stream().filter(e -> e.key().equals("metastatic-her2"))
              .findFirst().orElseThrow();
          assertNotEquals(primary.value(), metastatic.value());
          assertNotEquals(primary.sample(), metastatic.sample());
        }
        if (scenario == Scenario.TOXICITY) {
          var interruption = events.stream().filter(e -> e.key().equals("interruption"))
              .findFirst().orElseThrow();
          assertFalse(events.stream().anyMatch(e -> e.kind().equals("therapy")
              && e.date().isAfter(interruption.date())));
          assertTrue(events.stream().anyMatch(e -> e.key().equals("therapy-order")
              && e.status().equals("on-hold")));
        }
      }
    }
  }

  @Test
  public void complexityAndSeedsVaryTheJourney() {
    assertTrue(generate(0, Scenario.RECURRENCE, Complexity.COMPLEX).getAsJsonArray("entry").size()
        > generate(0, Scenario.RECURRENCE, Complexity.SIMPLE).getAsJsonArray("entry").size());
    assertNotEquals(generate(0, Scenario.UNCOMPLICATED, Complexity.STANDARD),
        generate(1, Scenario.UNCOMPLICATED, Complexity.STANDARD));
    var partial = generate(0, Scenario.PARTIAL_DATES, Complexity.STANDARD);
    boolean foundPartialDate = false;
    for (var entry : partial.getAsJsonArray("entry")) {
      var resource = entry.getAsJsonObject().getAsJsonObject("resource");
      foundPartialDate |= resource.has("onsetDateTime")
          && resource.get("onsetDateTime").getAsString().length() == 7;
    }
    assertTrue(foundPartialDate);
  }
}
