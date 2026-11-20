package org.mitre.synthea.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.mitre.synthea.engine.OncologyClinicalValidator;
import org.mitre.synthea.engine.OncologyScenario;
import org.mitre.synthea.engine.OncologyScenario.Event;
import org.mitre.synthea.engine.OncologyScenario.Journey;
import org.mitre.synthea.engine.OncologyScenario.Scenario;

/** Portable FHIR R4 files for the deliberately sampled oncology scenario simulator. */
public final class OncologyCaseExporter {
  private static final String BASE = "https://synthetichealth.github.io/synthea/oncology";
  private static final String WORKBENCH =
      "https://gyn-projekte.github.io/tumorboard-werkbank/fhir/CodeSystem/";
  private static final String EPISODE_EXTENSION =
      "http://hl7.org/fhir/StructureDefinition/workflow-episodeOfCare";
  private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();

  private final Journey journey;
  private final Map<String, JsonObject> resources = new LinkedHashMap<>();
  private final Map<String, String> eventRefs = new LinkedHashMap<>();
  private final Map<String, String> specimens = new LinkedHashMap<>();
  private String patient;
  private String episode;

  private OncologyCaseExporter(Journey journey) {
    this.journey = journey;
  }

  private static JsonObject object(Object... pairs) {
    JsonObject result = new JsonObject();
    for (int i = 0; i < pairs.length; i += 2) {
      Object value = pairs[i + 1];
      if (value != null) {
        result.add((String) pairs[i], value instanceof JsonElement
            ? (JsonElement) value : JSON.toJsonTree(value));
      }
    }
    return result;
  }

  private static JsonArray array(JsonElement... values) {
    JsonArray result = new JsonArray();
    for (JsonElement value : values) {
      result.add(value);
    }
    return result;
  }

  private static JsonObject concept(String text) {
    return object("text", text);
  }

  private static JsonObject coded(String system, String code, String text) {
    return object("text", text, "coding", array(object("system", system, "code", code)));
  }

  private static JsonObject ref(String reference) {
    return object("reference", reference);
  }

  private static JsonObject milligrams(double value) {
    return object("value", value, "unit", "mg",
        "system", "http://unitsofmeasure.org", "code", "mg");
  }

  private String id(String key) {
    return UUID.nameUUIDFromBytes(("oncology-v4:" + journey.seed() + ":" + journey.index()
        + ":" + journey.scenario() + ":" + journey.asOf()
        + ":" + key).getBytes(StandardCharsets.UTF_8)).toString();
  }

  private JsonObject add(String type, String key, Object... fields) {
    JsonObject result = object("resourceType", type, "id", id(key));
    object(fields).entrySet().forEach(entry -> result.add(entry.getKey(), entry.getValue()));
    resources.put(type + "/" + id(key), result);
    return result;
  }

  private static String reference(JsonObject resource) {
    return resource.get("resourceType").getAsString() + "/" + resource.get("id").getAsString();
  }

  private String date(Event event) {
    // Preserve explicit month-only historical information instead of fabricating a day.
    if (journey.scenario() == Scenario.PARTIAL_DATES
        && (event.kind().equals("assessment") || event.kind().equals("condition"))) {
      return event.date().toString().substring(0, 7);
    }
    return event.date().toString();
  }

  private JsonObject event(Event event) {
    String date = date(event);
    JsonObject result;
    switch (event.kind()) {
      case "specimen":
        result = add("Specimen", event.key(), "status", event.status(), "subject", ref(patient),
            "type", concept(event.sample().equals("germline") ? "Blood" : "Tissue"),
            "collection", object("collectedDateTime", date, "bodySite", concept(
                event.value() != null ? event.value()
                    : event.sample().equals("metastatic") ? "Liver" : "Breast")));
        specimens.put(event.sample(), reference(result));
        break;
      case "condition":
      case "problem":
        result = add("Condition", event.key(), "subject", ref(patient), "onsetDateTime", date,
            "clinicalStatus", coded("http://terminology.hl7.org/CodeSystem/condition-clinical",
                event.status(), event.status()),
            "verificationStatus", coded("http://terminology.hl7.org/CodeSystem/condition-ver-status",
                "confirmed", "Confirmed"),
            "code", event.kind().equals("condition")
                ? coded("http://snomed.info/sct", "254837009", event.description())
                : concept(event.description()));
        break;
      case "request":
      case "molecular":
      case "investigation":
      case "therapy-request":
        result = add("ServiceRequest", event.key(), "status", event.status(), "intent", "order",
            "subject", ref(patient), "authoredOn", date, "code", concept(event.description()));
        if (event.kind().equals("request") && journey.scenario() != Scenario.AWAITING_PATHOLOGY) {
          result.addProperty("status", "completed");
        }
        if (!event.kind().equals("request") && !event.kind().equals("investigation")) {
          result.add("category", array(coded(WORKBENCH + "workflow",
              event.kind().equals("molecular") ? "molecular" : "therapy", event.description())));
        }
        if (event.sample() != null) {
          result.add("specimen", array(ref(specimens.get(event.sample()))));
        }
        break;
      case "molecular-report":
      case "report":
        result = add("DiagnosticReport", event.key(), "status", event.status(),
            "subject", ref(patient), "code", concept(event.kind().equals("molecular-report")
                ? "HER2 dual-probe in situ hybridization" : "Histology"), "effectiveDateTime", date,
            "specimen", array(ref(specimens.get(event.sample()))));
        if (event.kind().equals("molecular-report")) {
          result.add("conclusionCode", array(concept(event.value())));
        }
        if (event.status().equals("final")) {
          result.addProperty("conclusion", event.description());
        }
        break;
      case "er":
      case "pr":
      case "her2":
      case "ecog":
      case "stage":
      case "histology":
      case "hemoglobin":
      case "weight":
      case "lvef":
      case "anc":
      case "platelets":
      case "er-percent":
      case "pr-percent":
      case "percent":
        String loinc = null;
        switch (event.kind()) {
          case "er":
            loinc = "85337-4";
            break;
          case "pr":
            loinc = "85339-0";
            break;
          case "her2":
            loinc = "85319-2";
            break;
          case "ecog":
            loinc = "89247-1";
            break;
          case "hemoglobin":
            loinc = "718-7";
            break;
          case "weight":
            loinc = "29463-7";
            break;
          default:
            break;
        }
        result = add("Observation", event.key(), "status", "final", "subject", ref(patient),
            "effectiveDateTime", date, "code", loinc == null
                ? concept(event.kind().equals("stage") ? "TNM" : event.description())
                : coded("http://loinc.org", loinc, event.description()));
        if (event.kind().equals("ecog")) {
          result.addProperty("valueInteger", Integer.parseInt(event.value()));
        } else if (event.kind().equals("weight")) {
          result.add("category", array(coded(
              "http://terminology.hl7.org/CodeSystem/observation-category",
              "vital-signs", "Vital signs")));
          result.add("valueQuantity", object("value", Double.parseDouble(event.value()),
              "unit", "kg", "system", "http://unitsofmeasure.org", "code", "kg"));
        } else if (event.kind().equals("hemoglobin")) {
          result.add("valueQuantity", object("value", Double.parseDouble(event.value()),
              "unit", "g/dL", "system", "http://unitsofmeasure.org", "code", "g/dL"));
        } else if (java.util.Set.of("lvef", "anc", "platelets", "er-percent", "pr-percent",
            "percent").contains(event.kind())) {
          String unit = java.util.Set.of("anc", "platelets")
              .contains(event.kind()) ? "10*9/L" : "%";
          result.add("valueQuantity", object("value", Double.parseDouble(event.value()),
              "unit", unit, "system", "http://unitsofmeasure.org", "code", unit));
        } else {
          result.add("valueCodeableConcept", concept(event.value()));
        }
        if (event.sample() != null) {
          result.add("specimen", ref(specimens.get(event.sample())));
        }
        break;
      case "imaging":
        result = add("DiagnosticReport", event.key(), "status", event.status(),
            "subject", ref(patient), "effectiveDateTime", date, "code", concept("Staging imaging"),
            "conclusion", event.description(), "category", array(coded(
                "http://terminology.hl7.org/CodeSystem/v2-0074", "RAD", "Radiology")));
        break;
      case "board":
        result = add("CarePlan", event.key(), "status", event.status(), "intent", "plan",
            "subject", ref(patient), "created", date, "description", event.description(),
            "category", array(coded(WORKBENCH + "workflow", "tumorboard", "Tumorboard")));
        break;
      case "task":
        result = add("Task", event.key(), "status", event.status(), "intent", "order",
            "for", ref(patient), "focus", ref(episode), "description", event.description(),
            "code", concept("Review"), "authoredOn", date,
            "performerType", array(concept(event.value() == null ? "CASE_MANAGER" : event.value())),
            "restriction", object("period", object("end", date)));
        if (event.status().equals("on-hold")) {
          result.add("statusReason", concept(event.description()));
        }
        break;
      case "therapy":
      case "procedure":
        result = add("Procedure", event.key(), "status", event.status(),
            "subject", ref(patient), "performedDateTime", date,
            "code", concept(event.description()));
        break;
      case "course":
        result = add("Procedure", event.key(), "status", event.status(), "subject", ref(patient),
            "performedPeriod", object("start", date, "end", event.value()),
            "code", concept(event.description()));
        break;
      case "medication-order":
        result = add("MedicationRequest", event.key(), "status", event.status(), "intent", "order",
            "subject", ref(patient), "authoredOn", date,
            "medicationCodeableConcept", concept(event.value()),
            "note", array(object("text", event.description())));
        if ("letrozole".equals(event.value())) {
          result.add("dosageInstruction", array(object("text", "2.5 mg orally once daily",
              "route", concept("Oral"),
              "timing", object("repeat", object("frequency", 1, "period", 1, "periodUnit", "d")),
              "doseAndRate", array(object("doseQuantity", milligrams(2.5))))));
        }
        if (event.status().equals("on-hold")) {
          result.add("statusReason", concept(
              "Persistent grade 2 sensory neuropathy; review pending"));
        } else if (event.status().equals("stopped")) {
          result.add("statusReason", concept("Biopsy-confirmed metastatic progression"));
        }
        break;
      case "medication":
        result = add("MedicationAdministration", event.key(), "status", event.status(),
            "subject", ref(patient), "effectiveDateTime", date,
            "context", ref(episode),
            "medicationCodeableConcept", concept(event.value()),
            "note", array(object("text", event.description())));
        boolean weightedInfusion = journey.scenario() == Scenario.DELAYED_RESTART
            && event.value().equals("trastuzumab");
        if ("pertuzumab".equals(event.value()) || weightedInfusion) {
          boolean loading = journey.events().stream().noneMatch(previous ->
              previous.kind().equals("medication") && event.value().equals(previous.value())
                  && previous.date().isBefore(event.date())
                  && previous.date().isAfter(event.date().minusWeeks(6)));
          double amount;
          if (weightedInfusion) {
            Event weight = journey.events().stream().filter(e -> e.kind().equals("weight")
                && e.date().equals(event.date())).findFirst().orElseThrow();
            amount = Double.parseDouble(weight.value()) * (loading ? 8 : 6);
          } else {
            amount = loading ? 840 : 420;
          }
          result.add("dosage", object("route", concept("Intravenous infusion"),
              "dose", milligrams(amount)));
        }
        break;
      case "assessment":
        result = add("ClinicalImpression", event.key(), "status", "completed",
            "subject", ref(patient), "effectiveDateTime", date, "summary", event.description());
        break;
      default:
        throw new IllegalArgumentException("Unknown event kind: " + event.kind());
    }
    // Published R4 extension contexts. Other members use native references and the case List.
    if (java.util.Set.of("Observation", "DiagnosticReport", "ServiceRequest", "CarePlan",
        "ClinicalImpression", "Condition", "Procedure")
        .contains(result.get("resourceType").getAsString())) {
      result.add("extension",
          array(object("url", EPISODE_EXTENSION, "valueReference", ref(episode))));
    }
    eventRefs.put(event.key(), reference(result));
    return result;
  }

  private JsonObject bundle() {
    patient = reference(add("Patient", "patient", "gender", "female",
        "birthDate", journey.birthDate().toString(), "name", array(object("use", "official",
            "family", "Synthetic", "given",
            array(JSON.toJsonTree("Case " + (journey.index() + 1)))))));
    episode = reference(add("EpisodeOfCare", "episode", "status", "active", "patient", ref(patient),
        "type", array(coded(WORKBENCH + "case-family", "BREAST", "Breast oncology"))));
    JsonObject pathway = add("CarePlan", "pathway", "status", "active", "intent", "plan",
        "subject", ref(patient), "title", journey.scenario().name(),
        "description", "Synthetic breast oncology journey; snapshot " + journey.asOf(),
        "category", array(coded(WORKBENCH + "workflow", "pathway", "Therapy pathway")),
        "extension", array(object("url", EPISODE_EXTENSION, "valueReference", ref(episode))));
    for (Event event : journey.events()) {
      event(event);
    }
    // Connect reports, completed work and dependent activities after all events exist.
    for (Event event : journey.events()) {
      JsonObject resource = resources.get(eventRefs.get(event.key()));
      if (event.kind().equals("report")) {
        JsonArray findings = new JsonArray();
        for (Event finding : journey.events()) {
          if (event.sample().equals(finding.sample())
              && java.util.Set.of("er", "pr", "her2", "er-percent", "pr-percent", "percent",
                  "histology", "stage").contains(finding.kind())) {
            findings.add(ref(eventRefs.get(finding.key())));
          }
        }
        if (!findings.isEmpty()) {
          resource.add("result", findings);
        }
      }
      if (event.cause() == null) {
        continue;
      }
      String cause = eventRefs.get(event.cause());
      if (cause == null) {
        throw new IllegalStateException("Missing causal event: " + event.cause());
      }
      switch (event.kind()) {
        case "task":
          boolean completed = event.status().equals("completed");
          JsonObject source = resources.get(cause);
          String state = source.has("status") ? source.get("status").getAsString() : "";
          boolean outstanding = (cause.startsWith("Task/") || cause.startsWith("ServiceRequest/"))
              && !state.equals("completed");
          JsonArray parameters = array(object("type", coded(WORKBENCH + "workflow",
              outstanding && !completed ? "dependency" : "evidence", "Supporting resource"),
              "valueReference", ref(cause)));
          if (!completed && "PHYSICIAN".equals(event.value())) {
            for (String field : java.util.List.of("requires_medical_validation",
                "abschlussnachweis_noetig")) {
              parameters.add(object("type", coded(WORKBENCH + "workflow", field, field),
                  "valueBoolean", true));
            }
          }
          resource.add(completed ? "output" : "input", parameters);
          break;
        case "report":
        case "molecular-report":
        case "therapy":
        case "therapy-request":
        case "medication-order":
          resource.add("basedOn", array(ref(cause)));
          break;
        case "medication":
          resource.add("request", ref(cause));
          break;
        case "procedure":
          resource.add("basedOn", array(ref(cause)));
          break;
        case "board":
        case "assessment":
          resource.add("supportingInfo", array(ref(cause)));
          break;
        default:
          break;
      }
    }
    if (eventRefs.containsKey("diagnosis")) {
      JsonObject diagnosis = resources.get(eventRefs.get("diagnosis"));
      journey.events().stream().filter(e -> e.key().equals("board-summary")).findFirst()
          .ifPresent(summary -> diagnosis.add("note",
              array(object("text", summary.description()))));
      resources.get(episode).add("diagnosis",
          array(object("condition", ref(eventRefs.get("diagnosis")))));
      pathway.add("addresses", array(ref(eventRefs.get("diagnosis"))));
    }
    // Highlight the latest findings for handover and retain links to the dated source records.
    LocalDate since = journey.events().stream().filter(e -> e.kind().equals("board")
        && e.status().equals("completed") && e.date().isBefore(journey.asOf()))
        .map(Event::date).max(LocalDate::compareTo).orElse(journey.asOf().minusDays(30));
    JsonArray updates = new JsonArray();
    JsonArray notes = new JsonArray();
    for (Event item : journey.events()) {
      if (item.kind().equals("task")) {
        resources.get(eventRefs.get(item.key())).add("basedOn", array(ref(reference(pathway))));
      }
      if (item.date().isBefore(since) || !java.util.Set.of("report", "molecular-report", "imaging", "problem",
          "molecular", "investigation", "board", "medication-order").contains(item.kind())) {
        continue;
      }
      updates.add(object("item", ref(eventRefs.get(item.key()))));
      notes.add(object("text", item.date() + " — " + item.description()
          + " (" + item.status() + ")"));
    }
    if (!updates.isEmpty()) {
      add("List", "handover", "status", "current", "mode", "snapshot", "subject", ref(patient),
          "date", journey.asOf().toString(), "code", coded(WORKBENCH + "workflow", "changes",
              "Recent findings and decisions"), "entry", updates, "note", notes);
    }
    JsonArray members = new JsonArray();
    for (String member : resources.keySet()) {
      members.add(object("item", ref(member)));
    }
    add("List", "members", "status", "current", "mode", "working", "subject", ref(patient),
        "date", journey.asOf().toString(), "title", "Oncology case resources", "entry", members);
    JsonArray entries = new JsonArray();
    for (Map.Entry<String, JsonObject> entry : resources.entrySet()) {
      entries.add(object("fullUrl", BASE + "/" + entry.getKey(), "resource", entry.getValue()));
    }
    return object("resourceType", "Bundle", "id", id("bundle"), "type", "collection",
        "timestamp", journey.asOf() + "T00:00:00Z", "meta", object("tag",
            array(object("system", WORKBENCH + "dataset", "code", "synthetic"))), "entry", entries);
  }

  public static JsonObject export(Journey journey) {
    OncologyClinicalValidator.requireValid(journey);
    return new OncologyCaseExporter(journey).bundle();
  }

  /** CLI: OUTPUT COUNT SEED AS_OF [mixed|SCENARIO]. */
  public static void main(String[] args) throws IOException {
    if (args.length < 4 || args.length > 5) {
      throw new IllegalArgumentException(
          "OUTPUT COUNT SEED AS_OF [mixed|SCENARIO]");
    }
    Path output = Path.of(args[0]);
    int count = Integer.parseInt(args[1]);
    long seed = Long.parseLong(args[2]);
    LocalDate asOf = LocalDate.parse(args[3]);
    if (count < 1 || count > 10000) {
      throw new IllegalArgumentException("Count must be between 1 and 10000");
    }
    String scenario = args.length > 4 ? args[4] : "mixed";
    Scenario focus = scenario.equals("mixed") ? null
        : Scenario.valueOf(scenario.toUpperCase(Locale.ROOT).replace('-', '_'));
    Files.createDirectories(output);
    try (var existing = Files.list(output)) {
      if (existing.findAny().isPresent()) {
        throw new IllegalArgumentException("Choose an empty output directory");
      }
    }
    JsonArray cases = new JsonArray();
    for (int index = 0; index < count; index++) {
      Journey journey = focus == null ? OncologyScenario.simulate(seed, index, asOf)
          : OncologyScenario.simulate(seed, index, asOf, focus);
      JsonObject bundle = export(journey);
      String filename = bundle.get("id").getAsString() + ".json";
      Path target = output.resolve(filename);
      // Never replace an existing cohort implicitly.
      Files.writeString(target, JSON.toJson(bundle) + "\n", StandardCharsets.UTF_8,
          java.nio.file.StandardOpenOption.CREATE_NEW);
      cases.add(object("file", filename, "scenario", journey.scenario().name(),
          "resources", bundle.getAsJsonArray("entry").size()));
    }
    Files.writeString(output.resolve("manifest.json"), JSON.toJson(object("model", "oncology-v4",
        "clinicalContract", "breast-scenarios-v4", "clinicalValidation", "passed",
        "seed", seed, "asOf", asOf.toString(), "cases", cases)) + "\n", StandardCharsets.UTF_8,
        java.nio.file.StandardOpenOption.CREATE_NEW);
    System.out.println("Generated " + count + " synthetic oncology case files in " + output);
  }
}
