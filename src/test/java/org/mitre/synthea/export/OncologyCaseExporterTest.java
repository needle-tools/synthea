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
import org.mitre.synthea.engine.OncologyScenario.Scenario;

public class OncologyCaseExporterTest {
  private static final LocalDate SNAPSHOT = LocalDate.of(2025, 6, 30);

  private JsonObject generate(int index, Scenario scenario) {
    return OncologyCaseExporter.export(OncologyScenario.simulate(42, index,
        SNAPSHOT, scenario));
  }

  @Test
  public void everyScenarioHasAClosedReadableGraph() {
    for (Scenario scenario : Scenario.values()) {
      for (int seed = 0; seed < 10; seed++) {
        JsonObject bundle = generate(seed, scenario);
        Set<String> identities = new HashSet<>();
        for (JsonElement entry : bundle.getAsJsonArray("entry")) {
          JsonObject resource = entry.getAsJsonObject().getAsJsonObject("resource");
          assertTrue(identities.add(resource.get("resourceType").getAsString()
              + "/" + resource.get("id").getAsString()));
        }
        referencesResolve(bundle, identities);
        FhirContext.forR4Cached().newJsonParser().parseResource(bundle.toString());
        assertEquals(bundle, generate(seed, scenario));
      }
    }
  }

  private void referencesResolve(JsonElement element, Set<String> identities) {
    if (element.isJsonArray()) {
      element.getAsJsonArray().forEach(child -> referencesResolve(child, identities));
    } else if (element.isJsonObject()) {
      JsonObject object = element.getAsJsonObject();
      // A CarePlan.activity.reference contains a Reference whose own reference is the URI.
      if (object.has("reference") && object.get("reference").isJsonPrimitive()) {
        assertTrue(object.get("reference").getAsString(),
            identities.contains(object.get("reference").getAsString()));
      }
      object.entrySet().forEach(entry -> referencesResolve(entry.getValue(), identities));
    }
  }

  @Test
  public void simulationMaintainsClinicalAndTemporalRelationships() {
    for (Scenario scenario : Scenario.values()) {
      for (int patient = 0; patient < 3; patient++) {
        var journey = OncologyScenario.simulate(42, patient, SNAPSHOT, scenario);
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
    JsonObject bundle = generate(0, Scenario.TOXICITY);
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
    for (var entry : generate(0, Scenario.TOXICITY).getAsJsonArray("entry")) {
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
    var entries = generate(0, Scenario.LOST_FOLLOWUP).getAsJsonArray("entry");
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
    var weights = new java.util.TreeMap<LocalDate, Double>();
    var dates = new java.util.ArrayList<LocalDate>();
    for (var entry : generate(0, Scenario.DELAYED_RESTART)
        .getAsJsonArray("entry")) {
      var resource = entry.getAsJsonObject().getAsJsonObject("resource");
      if ("Observation".equals(resource.get("resourceType").getAsString()) && resource.has("code")
          && "Measured body weight".equals(resource.getAsJsonObject("code").get("text")
              .getAsString())) {
        var coding = resource.getAsJsonArray("category").get(0).getAsJsonObject()
            .getAsJsonArray("coding").get(0).getAsJsonObject();
        assertEquals("vital-signs", coding.get("code").getAsString());
        weights.put(LocalDate.parse(resource.get("effectiveDateTime").getAsString()),
            resource.getAsJsonObject("valueQuantity").get("value").getAsDouble());
      }
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
    assertEquals(java.util.List.of(8 * weights.get(dates.get(0)), 6 * weights.get(dates.get(1)),
        8 * weights.get(dates.get(2))), doses.get("trastuzumab"));
    assertEquals(dates.get(1).plusWeeks(6), dates.get(2));
  }

  @Test
  public void routineSurveillanceHasItsOwnPlanOrderAndFutureDueDate() {
    for (int index = 0; index < 12; index++) {
      Map<String, JsonObject> resources = new HashMap<>();
      for (var entry : generate(index, Scenario.ROUTINE_SURVEILLANCE).getAsJsonArray("entry")) {
        var r = entry.getAsJsonObject().getAsJsonObject("resource");
        resources.put(r.get("resourceType").getAsString() + "/" + r.get("id").getAsString(), r);
      }
      var task = resources.values().stream().filter(r -> "Task".equals(r.get("resourceType")
          .getAsString())).findFirst().orElseThrow();
      var plan = resources.get(task.getAsJsonArray("basedOn").get(0).getAsJsonObject()
          .get("reference").getAsString());
      assertEquals("active", plan.get("status").getAsString());
      var order = resources.get(plan.getAsJsonArray("activity").get(0).getAsJsonObject()
          .getAsJsonObject("reference").get("reference").getAsString());
      var due = order.get("occurrenceDateTime").getAsString();
      assertTrue(LocalDate.parse(due).isAfter(SNAPSHOT));
      assertEquals(due, task.getAsJsonObject("restriction").getAsJsonObject("period")
          .get("end").getAsString());
      var treatment = resources.values().stream().filter(r -> r.has("title")
          && r.get("title").getAsString().equals("Primary breast cancer treatment"))
          .findFirst().orElseThrow();
      assertEquals("completed", treatment.get("status").getAsString());
      assertTrue(LocalDate.parse(treatment.getAsJsonObject("period").get("end").getAsString())
          .isBefore(SNAPSHOT));
    }
  }

  @Test
  public void mixedPatientsAreReproducibleAndHaveDifferentClinicalCourses() {
    var pathways = new java.util.ArrayList<Scenario>();
    var otherSeed = new java.util.ArrayList<Scenario>();
    var patients = new HashSet<String>();
    for (int index = 0; index < 60; index++) {
      var journey = OncologyScenario.simulate(42, index, SNAPSHOT);
      pathways.add(journey.scenario());
      otherSeed.add(OncologyScenario.simulate(73, index, SNAPSHOT).scenario());
      var bundle = OncologyCaseExporter.export(journey);
      assertEquals(generate(index, journey.scenario()), bundle);
      var patientIds = java.util.stream.StreamSupport.stream(
          bundle.getAsJsonArray("entry").spliterator(), false)
          .map(e -> e.getAsJsonObject().getAsJsonObject("resource"))
          .filter(r -> r.get("resourceType").getAsString().equals("Patient"))
          .map(r -> r.get("id").getAsString()).toList();
      assertEquals(1, patientIds.size());
      assertTrue(patients.add(patientIds.get(0)));
    }
    assertTrue(pathways.containsAll(java.util.List.of(Scenario.UNCOMPLICATED, Scenario.TOXICITY)));
    assertNotEquals(pathways, otherSeed);
  }

  @Test
  public void partialHistoricalDatesRetainTheirPrecision() {
    var partial = generate(0, Scenario.PARTIAL_DATES);
    boolean foundPartialDate = false;
    for (var entry : partial.getAsJsonArray("entry")) {
      var resource = entry.getAsJsonObject().getAsJsonObject("resource");
      foundPartialDate |= resource.has("onsetDateTime")
          && resource.get("onsetDateTime").getAsString().length() == 7;
    }
    assertTrue(foundPartialDate);
  }
}
