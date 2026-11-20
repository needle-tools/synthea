package org.mitre.synthea.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.Test;
import org.mitre.synthea.engine.OncologyScenario.Event;
import org.mitre.synthea.engine.OncologyScenario.Journey;
import org.mitre.synthea.engine.OncologyScenario.Scenario;

public class OncologyClinicalValidatorTest {
  private static final LocalDate SNAPSHOT = LocalDate.of(2025, 6, 30);

  @Test
  public void seededCohortsMeetTheClinicalContract() {
    for (int seed = 0; seed < 300; seed++) {
      for (Scenario scenario : Scenario.values()) {
        Journey journey = OncologyScenario.simulate(seed, seed % 13, SNAPSHOT, scenario);
        assertEquals(List.of(), OncologyClinicalValidator.validate(journey, journey.events()));
        if (scenario == Scenario.TOXICITY) {
          assertEquals(2, journey.events().stream().filter(e ->
              e.kind().equals("medication") && e.value().equals("docetaxel")).count());
        }
      }
    }
  }

  private Journey journey(Scenario scenario) {
    return OncologyScenario.simulate(42, 0, SNAPSHOT, scenario);
  }

  private void rejects(Journey journey, String rule, UnaryOperator<List<Event>> mutate) {
    List<String> issues = OncologyClinicalValidator.validate(journey,
        mutate.apply(new ArrayList<>(journey.events())));
    assertTrue(rule + ": " + issues, issues.contains(rule));
  }

  private Event change(Event event, LocalDate date, String value) {
    return new Event(event.key(), event.kind(), date, event.status(), event.description(),
        event.sample(), value, event.cause());
  }

  @Test
  public void rejectsBiologicallyIncompatibleTreatment() {
    rejects(journey(Scenario.TOXICITY), "her2-eligibility", events -> {
      events.replaceAll(e -> e.key().equals("primary-her2")
          ? change(e, e.date(), "IHC 1+") : e);
      return events;
    });
    rejects(journey(Scenario.RECURRENCE), "endocrine-eligibility", events -> {
      events.replaceAll(e -> e.key().equals("primary-er")
          ? change(e, e.date(), "Negative") : e);
      return events;
    });
    rejects(journey(Scenario.RECURRENCE), "endocrine-eligibility", events -> {
      events.removeIf(e -> e.key().equals("menopause"));
      return events;
    });
  }

  @Test
  public void rejectsUnsafeTreatmentAndInventedCompletion() {
    Journey toxicity = journey(Scenario.TOXICITY);
    rejects(toxicity, "toxicity-hold", events -> {
      events.replaceAll(e -> e.key().equals("cycle-2-docetaxel")
          ? change(e, SNAPSHOT, e.value()) : e);
      return events;
    });
    rejects(toxicity, "cardiac-monitoring", events -> {
      events.removeIf(e -> e.kind().equals("lvef"));
      return events;
    });
    rejects(toxicity, "cardiac-monitoring", events -> {
      Event cycle = events.stream().filter(e -> e.key().equals("cycle-2-trastuzumab"))
          .findFirst().orElseThrow();
      events.add(new Event("new-echo", "lvef", cycle.date().minusDays(1), "final",
          "Left ventricular ejection fraction", null, "35", null));
      return events;
    });
    rejects(toxicity, "pretreatment-counts", events -> {
      events.replaceAll(e -> e.kind().equals("anc") ? change(e, e.date(), "0.5") : e);
      return events;
    });
    rejects(toxicity, "tchp-combination", events -> {
      events.removeIf(e -> e.kind().equals("medication") && e.value().equals("carboplatin"));
      return events;
    });
    rejects(toxicity, "course-status", events -> {
      events.replaceAll(e -> e.key().equals("therapy-order")
          ? new Event(e.key(), e.kind(), e.date(), "completed", e.description(),
              e.sample(), e.value(), e.cause()) : e);
      return events;
    });
  }

  @Test
  public void rejectsPrematureOrContradictoryFindings() {
    rejects(journey(Scenario.TOXICITY), "event-date", events -> {
      events.replaceAll(e -> e.key().equals("cycle-2-docetaxel")
          ? change(e, SNAPSHOT.plusDays(1), e.value()) : e);
      return events;
    });
    rejects(journey(Scenario.RECURRENCE), "receptor-interpretation", events -> {
      events.replaceAll(e -> e.key().equals("primary-er-percent")
          ? change(e, e.date(), "150") : e);
      return events;
    });
    rejects(journey(Scenario.RECURRENCE), "metastatic-stage", events -> {
      events.replaceAll(e -> e.key().equals("stage-recurrence")
          ? change(e, e.date(), "cM0") : e);
      return events;
    });
    rejects(journey(Scenario.RECURRENCE), "specimen-before-result", events -> {
      events.removeIf(e -> e.key().equals("metastatic-sample"));
      return events;
    });
    rejects(journey(Scenario.TOXICITY), "histology-before-treatment", events -> {
      events.replaceAll(e -> e.key().equals("pathology")
          ? change(e, SNAPSHOT, e.value()) : e);
      return events;
    });
    rejects(journey(Scenario.DELAYED_MOLECULAR), "genomic-test-eligibility", events -> {
      events.replaceAll(e -> e.key().equals("primary-her2")
          ? change(e, e.date(), "IHC 3+") : e);
      return events;
    });
  }

  @Test
  public void rejectsACompletedCourseWithMissingCycles() {
    Journey complete = java.util.stream.IntStream.range(0, 100)
        .mapToObj(seed -> OncologyScenario.simulate(seed, 0, SNAPSHOT,
            Scenario.UNCOMPLICATED))
        .filter(j -> j.events().stream().anyMatch(e -> e.key().equals("cycle-6-docetaxel")))
        .findFirst().orElseThrow();
    rejects(complete, "tchp-course-length", events -> {
      events.removeIf(e -> e.key().equals("cycle-6-docetaxel"));
      return events;
    });
    rejects(complete, "tchp-interval", events -> {
      events.replaceAll(e -> e.key().equals("cycle-6-docetaxel")
          ? change(e, e.date().plusDays(2), e.value()) : e);
      return events;
    });
  }

  @Test
  public void rejectsContradictionsInBoardSnapshots() {
    rejects(journey(Scenario.RESIDUAL_TNBC), "residual-stage", events -> {
      events.replaceAll(e -> e.key().equals("post-stage")
          ? change(e, e.date(), "ypT0 ypN0 cM0") : e);
      return events;
    });
    rejects(journey(Scenario.EQUIVOCAL_HER2), "equivocal-reflex-ish", events -> {
      events.removeIf(e -> e.key().equals("ish"));
      return events;
    });
    rejects(journey(Scenario.LATE_LIVER_LESION), "suspicious-lesion-unconfirmed", events -> {
      events.add(new Event("m1", "stage", SNAPSHOT, "final", "Metastatic disease",
          null, "M1 (liver)", null));
      return events;
    });
    rejects(journey(Scenario.PULMONARY_RELAPSE), "lung-recurrence-confirmed", events -> {
      events.replaceAll(e -> e.key().equals("lung-her2") ? change(e, e.date(), "IHC 1+") : e);
      return events;
    });
    rejects(journey(Scenario.NODE_POSITIVE_TCHP), "node-positive-evidence", events -> {
      events.removeIf(e -> e.key().equals("node-pathology"));
      return events;
    });
    rejects(journey(Scenario.HEREDITARY_REVIEW), "hereditary-workup", events -> {
      events.removeIf(e -> e.key().equals("blood"));
      return events;
    });
    rejects(journey(Scenario.HEREDITARY_REVIEW), "hereditary-workup", events -> {
      events.add(new Event("panel-result", "report", SNAPSHOT, "final", "Panel reported",
          "germline", null, null));
      return events;
    });
    rejects(journey(Scenario.PULMONARY_RELAPSE), "relapse-treatment-proposed", events -> {
      events.add(new Event("infusion", "course", SNAPSHOT, "completed", "First metastatic infusion",
          null, SNAPSHOT.toString(), null));
      return events;
    });
    rejects(journey(Scenario.DISCORDANT_MARKERS), "discordant-pathology-review", events -> {
      events.removeIf(e -> e.key().equals("reference-pathology"));
      return events;
    });
    rejects(journey(Scenario.RESIDUAL_HER2), "primary-her2-positive", events -> {
      events.replaceAll(e -> e.key().equals("primary-her2") ? change(e, e.date(), "IHC 1+") : e);
      return events;
    });
  }

  @Test
  public void checksTheLowPositiveBoundaryAndInfusionRestart() {
    Journey low = journey(Scenario.ER_LOW_REVIEW);
    for (String value : List.of("1", "10")) {
      var events = new ArrayList<>(low.events());
      events.replaceAll(e -> e.key().equals("primary-er-percent") ? change(e, e.date(), value) : e);
      assertEquals(List.of(), OncologyClinicalValidator.validate(low, events));
    }
    for (String value : List.of("0", "11")) {
      rejects(low, "receptor-interpretation", events -> {
        events.replaceAll(e -> e.key().equals("primary-er-percent")
            ? change(e, e.date(), value) : e);
        return events;
      });
    }
    rejects(journey(Scenario.DELAYED_RESTART), "restart-reviewed", events -> {
      events.removeIf(e -> e.key().equals("restart-clearance"));
      return events;
    });
    rejects(journey(Scenario.DELAYED_RESTART), "weight-for-infusion", events -> {
      events.removeIf(e -> e.key().equals("weight-2"));
      return events;
    });
  }

  @Test
  public void everyDocumentedTchpCycleRetainsItsMonitoring() {
    for (Scenario scenario : List.of(Scenario.UNCOMPLICATED, Scenario.TOXICITY,
        Scenario.DELAYED_RESTART)) {
      for (int patient = 0; patient < 20; patient++) {
        var events = OncologyScenario.simulate(42, patient, SNAPSHOT, scenario).events();
        for (Event infusion : events) {
          if (infusion.kind().equals("medication") && infusion.value().equals("docetaxel")) {
            for (String measurement : List.of("ecog", "hemoglobin")) {
              assertTrue(events.stream().anyMatch(e -> e.kind().equals(measurement)
                  && e.date().equals(infusion.date()) && e.status().equals("final")));
            }
          }
        }
      }
    }
  }

  @Test
  public void reflexTestResultChangesTheEvidenceForTheBoard() {
    var outcomes = new java.util.HashSet<String>();
    for (int index = 0; index < 80; index++) {
      Journey patient = OncologyScenario.simulate(42, index, SNAPSHOT, Scenario.EQUIVOCAL_HER2);
      Event decision = patient.events().stream().filter(e -> e.key().equals("decision"))
          .findFirst().orElseThrow();
      Event evidence = patient.events().stream().filter(e -> e.key().equals(decision.cause()))
          .findFirst().orElseThrow();
      outcomes.add(evidence.kind().equals("molecular") ? evidence.status() : evidence.value());
    }
    assertEquals(java.util.Set.of("active", "Amplified", "Not amplified"), outcomes);
  }

  @Test
  public void marginFindingsChangeTheCourseWithinOnePresentation() {
    boolean clear = false;
    boolean involved = false;
    for (int index = 0; index < 80; index++) {
      Journey patient = OncologyScenario.simulate(42, index, SNAPSHOT, Scenario.LOST_FOLLOWUP);
      var events = patient.events();
      Event margin = events.stream().filter(e -> e.key().equals("initial-margin"))
          .findFirst().orElseThrow();
      boolean reoperated = events.stream().anyMatch(e -> e.key().equals("re-excision"));
      assertEquals(margin.value().equals("Involved"), reoperated);
      if (reoperated) {
        involved = true;
        rejects(patient, "clear-final-margin", candidate -> {
          candidate.removeIf(e -> e.key().equals("final-margin"));
          return candidate;
        });
      } else {
        clear = true;
      }
    }
    assertTrue(clear && involved);
  }
}
