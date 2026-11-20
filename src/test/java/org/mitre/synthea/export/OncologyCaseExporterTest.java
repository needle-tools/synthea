package org.mitre.synthea.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
              || e.kind().equals("medication")));
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
          assertFalse(events.stream().anyMatch(e -> e.kind().equals("medication")
              && e.date().isAfter(interruption.date())));
          assertTrue(events.stream().anyMatch(e -> e.key().equals("therapy-order")
              && e.status().equals("on-hold")));
        }
      }
    }
  }

  @Test
  public void exportsClinicalTreatmentAndSafetyEvidenceAsLinkedFhir() {
    JsonObject bundle = generate(0, Scenario.TOXICITY, Complexity.SIMPLE);
    Map<String, JsonObject> resources = new HashMap<>();
    for (JsonElement entry : bundle.getAsJsonArray("entry")) {
      JsonObject resource = entry.getAsJsonObject().getAsJsonObject("resource");
      resources.put(resource.get("resourceType").getAsString() + "/"
          + resource.get("id").getAsString(), resource);
    }
    Map<String, Integer> drugs = new HashMap<>();
    for (JsonObject resource : resources.values()) {
      if (resource.get("resourceType").getAsString().equals("MedicationAdministration")) {
        String drug = resource.getAsJsonObject("medicationCodeableConcept")
            .get("text").getAsString();
        drugs.merge(drug, 1, Integer::sum);
        JsonObject order = resources.get(resource.getAsJsonObject("request")
            .get("reference").getAsString());
        assertEquals("MedicationRequest", order.get("resourceType").getAsString());
        assertEquals(drug, order.getAsJsonObject("medicationCodeableConcept")
            .get("text").getAsString());
        assertTrue(resource.getAsJsonObject("context").get("reference").getAsString()
            .startsWith("EpisodeOfCare/"));
        assertEquals("on-hold", order.get("status").getAsString());
        assertTrue(order.has("statusReason"));
        assertTrue(LocalDate.parse(order.get("authoredOn").getAsString()).compareTo(
            LocalDate.parse(resource.get("effectiveDateTime").getAsString())) <= 0);
      }
    }
    assertEquals(Map.of("docetaxel", 2, "carboplatin", 2,
        "trastuzumab", 2, "pertuzumab", 2), drugs);
    assertTrue(resources.values().stream().anyMatch(r -> r.get("resourceType").getAsString()
        .equals("Observation") && r.getAsJsonObject("code").get("text").getAsString()
        .equals("Left ventricular ejection fraction")
        && r.getAsJsonObject("valueQuantity").get("value").getAsInt() == 63));
  }

  @Test
  public void exportsFixedDosesAtTheCorrectPointInTreatment() {
    var administrations = new java.util.TreeMap<LocalDate, Double>();
    for (var entry : generate(0, Scenario.TOXICITY, Complexity.COMPLEX).getAsJsonArray("entry")) {
      var resource = entry.getAsJsonObject().getAsJsonObject("resource");
      if ("MedicationAdministration".equals(resource.get("resourceType").getAsString())
          && "pertuzumab".equals(resource.getAsJsonObject("medicationCodeableConcept")
              .get("text").getAsString())) {
        var dose = resource.getAsJsonObject("dosage").getAsJsonObject("dose");
        assertEquals("mg", dose.get("code").getAsString());
        administrations.put(LocalDate.parse(resource.get("effectiveDateTime").getAsString()),
            dose.get("value").getAsDouble());
      }
    }
    assertEquals(java.util.List.of(840.0, 420.0), java.util.List.copyOf(administrations.values()));
    var entries = generate(0, Scenario.LOST_FOLLOWUP, Complexity.STANDARD).getAsJsonArray("entry");
    var prescription = java.util.stream.StreamSupport.stream(entries.spliterator(), false)
        .map(entry -> entry.getAsJsonObject().getAsJsonObject("resource"))
        .filter(r -> r.get("resourceType").getAsString().equals("MedicationRequest"))
        .findFirst().orElseThrow();
    var dosage = prescription.getAsJsonArray("dosageInstruction").get(0).getAsJsonObject();
    var dose = dosage.getAsJsonArray("doseAndRate").get(0).getAsJsonObject()
        .getAsJsonObject("doseQuantity");
    assertEquals(2.5, dose.get("value").getAsDouble(), 0);
    assertEquals("mg", dose.get("code").getAsString());
    assertEquals("d", dosage.getAsJsonObject("timing").getAsJsonObject("repeat")
        .get("periodUnit").getAsString());
  }

  @Test
  public void sixWeekDelayRestartsBothAntibodiesAtLoadingDoses() {
    var doses = new HashMap<String, java.util.List<Double>>();
    var dates = new java.util.ArrayList<LocalDate>();
    for (var entry : generate(0, Scenario.DELAYED_RESTART, Complexity.SIMPLE)
        .getAsJsonArray("entry")) {
      var resource = entry.getAsJsonObject().getAsJsonObject("resource");
      if (!"MedicationAdministration".equals(resource.get("resourceType").getAsString())
          || !resource.has("dosage")) {
        continue;
      }
      String drug = resource.getAsJsonObject("medicationCodeableConcept").get("text").getAsString();
      var dose = resource.getAsJsonObject("dosage").getAsJsonObject("dose");
      doses.computeIfAbsent(drug, k -> new java.util.ArrayList<>())
          .add(dose.get("value").getAsDouble());
      if (drug.equals("pertuzumab")) {
        dates.add(LocalDate.parse(resource.get("effectiveDateTime").getAsString()));
      }
    }
    assertEquals(java.util.List.of(840.0, 420.0, 840.0), doses.get("pertuzumab"));
    assertEquals(java.util.List.of(544.0, 402.0, 528.0), doses.get("trastuzumab"));
    assertEquals(dates.get(1).plusWeeks(6), dates.get(2));
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
