package org.mitre.synthea.export;

import ca.uhn.fhir.context.FhirContext;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceCategory;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceCriticality;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceType;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.hl7.fhir.r4.model.Appointment.AppointmentStatus;
import org.hl7.fhir.r4.model.Appointment.ParticipationStatus;
import org.hl7.fhir.r4.model.Basic;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CarePlan.CarePlanActivityComponent;
import org.hl7.fhir.r4.model.CarePlan.CarePlanActivityDetailComponent;
import org.hl7.fhir.r4.model.CarePlan.CarePlanActivityStatus;
import org.hl7.fhir.r4.model.CarePlan.CarePlanIntent;
import org.hl7.fhir.r4.model.CarePlan.CarePlanStatus;
import org.hl7.fhir.r4.model.CareTeam;
import org.hl7.fhir.r4.model.CareTeam.CareTeamParticipantComponent;
import org.hl7.fhir.r4.model.CareTeam.CareTeamStatus;
import org.hl7.fhir.r4.model.Claim.ClaimStatus;
import org.hl7.fhir.r4.model.Claim.DiagnosisComponent;
import org.hl7.fhir.r4.model.Claim.InsuranceComponent;
import org.hl7.fhir.r4.model.Claim.ItemComponent;
import org.hl7.fhir.r4.model.Claim.ProcedureComponent;
import org.hl7.fhir.r4.model.Claim.SupportingInformationComponent;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Coverage.CoverageStatus;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Device.DeviceNameType;
import org.hl7.fhir.r4.model.Device.FHIRDeviceStatus;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContextComponent;
import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.Dosage.DosageDoseAndRateComponent;
import org.hl7.fhir.r4.model.Encounter.EncounterHospitalizationComponent;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.RemittanceOutcome;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.TotalComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.Use;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Goal;
import org.hl7.fhir.r4.model.Goal.GoalLifecycleStatus;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.hl7.fhir.r4.model.ImagingStudy.ImagingStudySeriesComponent;
import org.hl7.fhir.r4.model.ImagingStudy.ImagingStudySeriesInstanceComponent;
import org.hl7.fhir.r4.model.ImagingStudy.ImagingStudyStatus;
import org.hl7.fhir.r4.model.CommunicationRequest;
import org.hl7.fhir.r4.model.CommunicationRequest.CommunicationRequestStatus;
import org.hl7.fhir.r4.model.CommunicationRequest.CommunicationPriority;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;
import org.hl7.fhir.r4.model.Immunization.ImmunizationStatus;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Location.LocationPositionComponent;
import org.hl7.fhir.r4.model.Location.LocationStatus;
import org.hl7.fhir.r4.model.Media;
import org.hl7.fhir.r4.model.Media.MediaStatus;
import org.hl7.fhir.r4.model.Medication.MedicationStatus;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.MedicationAdministration.MedicationAdministrationDosageComponent;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestIntent;
import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Narrative.NarrativeStatus;
import org.hl7.fhir.r4.model.Observation.ObservationComponentComponent;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.ContactComponent;
import org.hl7.fhir.r4.model.Patient.PatientCommunicationComponent;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PositiveIntType;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Procedure.ProcedureStatus;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Provenance.ProvenanceAgentComponent;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Schedule;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.SimpleQuantity;
import org.hl7.fhir.r4.model.Slot;
import org.hl7.fhir.r4.model.Slot.SlotStatus;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.SupplyDelivery;
import org.hl7.fhir.r4.model.SupplyDelivery.SupplyDeliveryStatus;
import org.hl7.fhir.r4.model.SupplyDelivery.SupplyDeliverySuppliedItemComponent;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.hl7.fhir.r4.model.Timing;
import org.hl7.fhir.r4.model.Timing.TimingRepeatComponent;
import org.hl7.fhir.r4.model.Timing.UnitsOfTime;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.codesystems.DoseRateType;

import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

import org.mitre.synthea.engine.Components;
import org.mitre.synthea.engine.Components.Attachment;
import org.mitre.synthea.export.rif.CodeMapper;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.helpers.RandomNumberGenerator;
import org.mitre.synthea.helpers.SimpleCSV;
import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.identity.Entity;
import org.mitre.synthea.world.agents.Clinician;
import org.mitre.synthea.world.agents.Payer;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.concepts.Claim;
import org.mitre.synthea.world.concepts.ClinicianSpecialty;
import org.mitre.synthea.world.concepts.Costs;
import org.mitre.synthea.world.concepts.HealthRecord;
import org.mitre.synthea.world.concepts.HealthRecord.CarePlan;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.EncounterType;
import org.mitre.synthea.world.concepts.HealthRecord.ImagingStudy;
import org.mitre.synthea.world.concepts.HealthRecord.Medication;
import org.mitre.synthea.world.concepts.HealthRecord.Observation;
import org.mitre.synthea.world.concepts.HealthRecord.Procedure;
import org.mitre.synthea.world.concepts.HealthRecord.Report;
import org.mitre.synthea.world.geography.Location;

public class FhirR4 {
  // HAPI FHIR warns that the context creation is expensive, and should be performed
  // per-application, not per-record
  private static final FhirContext FHIR_CTX = FhirContext.forR4();

  private static final String SNOMED_URI = "http://snomed.info/sct";
  private static final String LOINC_URI = "http://loinc.org";
  private static final String RXNORM_URI = "http://www.nlm.nih.gov/research/umls/rxnorm";
  private static final String CVX_URI = "http://hl7.org/fhir/sid/cvx";
  private static final String DISCHARGE_URI = "http://www.nubc.org/patient-discharge";
  private static final String SYNTHEA_EXT = "http://synthetichealth.github.io/synthea/";
  private static final String UNITSOFMEASURE_URI = "http://unitsofmeasure.org";
  private static final String DICOM_DCM_URI = "http://dicom.nema.org/resources/ontology/DCM";
  private static final String MEDIA_TYPE_URI = "http://terminology.hl7.org/CodeSystem/media-type";
  protected static final String SYNTHEA_IDENTIFIER = "https://github.com/synthetichealth/synthea";

  @SuppressWarnings("rawtypes")
  private static final Map raceEthnicityCodes = loadRaceEthnicityCodes();
  @SuppressWarnings("rawtypes")
  private static final Map languageLookup = loadLanguageLookup();

  protected static boolean TRANSACTION_BUNDLE =
      Config.getAsBoolean("exporter.fhir.transaction_bundle");

  protected static boolean HISTORY_EXPORT =
      Config.getAsBoolean("exporter.fhir.history_export", false);

  protected static boolean USE_US_CORE_IG =
      Config.getAsBoolean("exporter.fhir.use_us_core_ig");
  protected static String US_CORE_VERSION =
      Config.get("exporter.fhir.us_core_version", "6.1.0");

  private static Table<String, String, String> US_CORE_MAPPING;
  private static final Table<String, String, String> US_CORE_3_MAPPING;
  private static final Table<String, String, String> US_CORE_4_MAPPING;
  private static final Table<String, String, String> US_CORE_5_MAPPING;
  private static final Table<String, String, String> US_CORE_6_MAPPING;
  private static final Table<String, String, String> US_CORE_7_MAPPING;

  public static enum USCoreVersion {
    v311, v400, v501, v610, v700
  }

  protected static boolean useUSCore3() {
    boolean useUSCore3 = USE_US_CORE_IG && US_CORE_VERSION.startsWith("3");
    if (useUSCore3) {
      US_CORE_MAPPING = US_CORE_3_MAPPING;
    }
    return useUSCore3;
  }

  protected static boolean useUSCore4() {
    boolean useUSCore4 = USE_US_CORE_IG && US_CORE_VERSION.startsWith("4");
    if (useUSCore4) {
      US_CORE_MAPPING = US_CORE_4_MAPPING;
    }
    return useUSCore4;
  }

  protected static boolean useUSCore5() {
    boolean useUSCore5 = USE_US_CORE_IG && US_CORE_VERSION.startsWith("5");
    if (useUSCore5) {
      US_CORE_MAPPING = US_CORE_5_MAPPING;
    }
    return useUSCore5;
  }

  protected static boolean useUSCore6() {
    boolean useUSCore6 = USE_US_CORE_IG && US_CORE_VERSION.startsWith("6");
    if (useUSCore6) {
      US_CORE_MAPPING = US_CORE_6_MAPPING;
    }
    return useUSCore6;
  }

  protected static boolean useUSCore7() {
    boolean useUSCore7 = USE_US_CORE_IG && US_CORE_VERSION.startsWith("7");
    if (useUSCore7) {
      US_CORE_MAPPING = US_CORE_7_MAPPING;
    }
    return useUSCore7;
  }

  private static final String COUNTRY_CODE = Config.get("generate.geography.country_code");
    private static final String CAREPLAN_CATEGORY_SYSTEM = Config.get(
      "exporter.fhir.careplan.category.system",
      "US".equalsIgnoreCase(COUNTRY_CODE)
        ? "http://hl7.org/fhir/us/core/CodeSystem/careplan-category"
        : "http://terminology.hl7.org/CodeSystem/care-plan-category");
    private static final boolean NORMALIZE_PHONE_FOR_COUNTRY = Config.getAsBoolean(
      "exporter.fhir.normalize_phone_for_country", true);
    private static final String NON_US_FALLBACK_PHONE = Config.get(
      "exporter.fhir.non_us_fallback_phone", "+49 345 000000");
  private static final String PASSPORT_URI = Config.get("generate.geography.passport_uri", "http://hl7.org/fhir/sid/passport-USA");

  private static final HashSet<Class<? extends Resource>> includedResources = new HashSet<>();
  private static final HashSet<Class<? extends Resource>> excludedResources = new HashSet<>();

  static {
    reloadIncludeExclude();

    Map<String, Table<String, String, String>> usCoreMappings =
        loadMappingWithVersions("us_core_mapping.csv", "3", "4", "5", "6", "7");

    US_CORE_3_MAPPING = usCoreMappings.get("3");
    US_CORE_4_MAPPING = usCoreMappings.get("4");
    US_CORE_5_MAPPING = usCoreMappings.get("5");
    US_CORE_6_MAPPING = usCoreMappings.get("6");
    US_CORE_7_MAPPING = usCoreMappings.get("7");

    if (US_CORE_VERSION.startsWith("3")) {
      US_CORE_MAPPING = US_CORE_3_MAPPING;
    } else if (US_CORE_VERSION.startsWith("4")) {
      US_CORE_MAPPING = US_CORE_4_MAPPING;
    } else if (US_CORE_VERSION.startsWith("5")) {
      US_CORE_MAPPING = US_CORE_5_MAPPING;
    } else if (US_CORE_VERSION.startsWith("6")) {
      US_CORE_MAPPING = US_CORE_6_MAPPING;
    } else if (US_CORE_VERSION.startsWith("7")) {
      US_CORE_MAPPING = US_CORE_7_MAPPING;
    }
  }

  static void reloadIncludeExclude() {
    includedResources.clear();
    excludedResources.clear();
    String includedResourcesStr = Config.get("exporter.fhir.included_resources", "").trim();
    String excludedResourcesStr = Config.get("exporter.fhir.excluded_resources", "").trim();

    List<Class<? extends Resource>> includedResourcesList = Collections.emptyList();
    List<Class<? extends Resource>> excludedResourcesList = Collections.emptyList();

    if (!includedResourcesStr.isEmpty() && !excludedResourcesStr.isEmpty()) {
      System.err.println(
          "FHIR exporter: Included and Excluded resource settings are both set -- ignoring both");
    } else if (!includedResourcesStr.isEmpty()) {
      includedResourcesList = propStringToList(includedResourcesStr);
    } else if (!excludedResourcesStr.isEmpty()) {
      excludedResourcesList = propStringToList(excludedResourcesStr);
    }

    includedResources.addAll(includedResourcesList);
    excludedResources.addAll(excludedResourcesList);
  }

  static boolean shouldExport(Class<? extends Resource> resourceType) {
    return (includedResources.isEmpty() || includedResources.contains(resourceType))
            && !excludedResources.contains(resourceType);
  }

  /**
   * Helper function to convert a string of resource type names
   *  from synthea.properties into a list of FHIR ResourceTypes.
   * @param propString String directly from Config, ex "Patient,Condition , Procedure"
   * @return normalized list of filenames as strings
   */
  private static List<Class<? extends Resource>> propStringToList(String propString) {
    List<String> resourceTypes = Arrays.asList(propString.split(","));

    // normalize filenames by trimming, convert to resource class
    @SuppressWarnings("unchecked")
    List<Class<? extends Resource>> resourceClasses = resourceTypes.stream().map(f ->  {
      try {
        return (Class<? extends Resource>)Class.forName("org.hl7.fhir.r4.model." + f.trim());
      } catch (ClassNotFoundException | ClassCastException e) {
        throw new RuntimeException("Type " + f
          + " listed in the FHIR include/exclude list is not a valid FHIR resource type", e);
      }
    }).collect(Collectors.toList());

    return resourceClasses;
  }

  @SuppressWarnings("rawtypes")
  private static Map loadRaceEthnicityCodes() {
    String filename = "race_ethnicity_codes.json";
    try {
      String json = Utilities.readResource(filename);
      Gson g = new Gson();
      return g.fromJson(json, HashMap.class);
    } catch (Exception e) {
      System.err.println("ERROR: unable to load json: " + filename);
      e.printStackTrace();
      throw new ExceptionInInitializerError(e);
    }
  }

  @SuppressWarnings("rawtypes")
  private static Map loadLanguageLookup() {
    String filename = "language_lookup.json";
    try {
      String json = Utilities.readResource(filename);
      Gson g = new Gson();
      return g.fromJson(json, HashMap.class);
    } catch (Exception e) {
      System.err.println("ERROR: unable to load json: " + filename);
      e.printStackTrace();
      throw new ExceptionInInitializerError(e);
    }
  }


  private static Map<String, Table<String, String, String>>
      loadMappingWithVersions(String filename, String... supportedVersions) {
    Map<String, Table<String,String,String>> versions = new HashMap<>();

    for (String version : supportedVersions) {
      versions.put(version, HashBasedTable.create());
    }

    List<LinkedHashMap<String, String>> csvData;
    try {
      csvData = SimpleCSV.parse(Utilities.readResource(filename));
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    for (LinkedHashMap<String, String> line : csvData) {
      String system = line.get("SYSTEM");
      String code = line.get("CODE");
      String url = line.get("URL");
      String version = line.get("VERSION");

      for (Entry<String, Table<String, String, String>> e : versions.entrySet()) {
        String versionKey = e.getKey();
        Table<String, String, String> mappingTable = e.getValue();

        if (StringUtils.isBlank(version) || version.contains(versionKey)) {
          // blank means applies to ALL versions
          // version.contains allows for things like "4+5+6"
          mappingTable.put(system, code, url);
        }
      }
    }

    return versions;
  }

  public static FhirContext getContext() {
    return FHIR_CTX;
  }

  /**
   * Convert the given Person into a FHIR Bundle of the Patient and the
   * associated entries from their health record.
   *
   * @param person   Person to generate the FHIR JSON for
   * @param stopTime Time the simulation ended
   * @return FHIR Bundle containing the Person's health record
   */
  public static Bundle convertToFHIR(Person person, long stopTime) {
    Bundle bundle = new Bundle();
    if (TRANSACTION_BUNDLE) {
      bundle.setType(BundleType.TRANSACTION);
    } else {
      bundle.setType(BundleType.COLLECTION);
    }

    // Clear location hierarchy cache for each patient
    clearLocationCache();

    BundleEntryComponent personEntry = basicInfo(person, bundle, stopTime);

    for (Encounter encounter : person.record.encounters) {
      BundleEntryComponent encounterEntry = encounter(person, personEntry, bundle, encounter);

      // --- Enhanced Location Hierarchy ---
      // Add department/ward/room/bed locations to the encounter
      BundleEntryComponent locationEntry = null;
      if (shouldExport(org.hl7.fhir.r4.model.Location.class)) {
        Provider provider = encounter.provider;
        if (provider == null) {
          provider = person.getProvider(EncounterType.WELLNESS, encounter.start);
        }
        locationEntry = encounterLocation(bundle, provider, encounter, person);
        if (locationEntry != null) {
          org.hl7.fhir.r4.model.Encounter encounterResource =
              (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();
          EncounterLocationComponent locComp = new EncounterLocationComponent();
          locComp.setLocation(new Reference(locationEntry.getFullUrl())
              .setDisplay(((org.hl7.fhir.r4.model.Location)
                  locationEntry.getResource()).getName()));
          locComp.setStatus(
              org.hl7.fhir.r4.model.Encounter.EncounterLocationStatus.ACTIVE);
          locComp.setPhysicalType(
              ((org.hl7.fhir.r4.model.Location) locationEntry.getResource())
                  .getPhysicalType());
          locComp.setPeriod(new Period()
              .setStart(new Date(encounter.start))
              .setEnd(new Date(encounter.stop)));
          encounterResource.addLocation(locComp);
        }
      }

      if (shouldExport(Condition.class)) {
        for (HealthRecord.Entry condition : encounter.conditions) {
          condition(person, personEntry, bundle, encounterEntry, condition);
        }
      }

      if (shouldExport(AllergyIntolerance.class)) {
        for (HealthRecord.Allergy allergy : encounter.allergies) {
          allergy(personEntry, bundle, encounterEntry, allergy);
        }
      }

      final boolean shouldExportMedia = shouldExport(Media.class);
      final boolean shouldExportObservation = shouldExport(org.hl7.fhir.r4.model.Observation.class);

      for (Observation observation : encounter.observations) {
        // If the Observation contains an attachment, use a Media resource, since
        // Observation resources in v4 don't support Attachments
        if (observation.value instanceof Attachment) {
          if (shouldExportMedia) {
            media(personEntry, bundle, encounterEntry, observation);
          }
        } else if (shouldExportObservation) {
          observation(personEntry, bundle, encounterEntry, observation);
        }
      }

      if (shouldExport(org.hl7.fhir.r4.model.Procedure.class)) {
        for (Procedure procedure : encounter.procedures) {
          procedure(person, personEntry, bundle, encounterEntry, procedure);
        }
      }

      if (shouldExport(Device.class)) {
        for (HealthRecord.Device device : encounter.devices) {
          device(personEntry, bundle, device);
        }
      }

      if (shouldExport(SupplyDelivery.class)) {
        for (HealthRecord.Supply supply : encounter.supplies) {
          supplyDelivery(personEntry, bundle, supply, encounter);
        }
      }

      List<BundleEntryComponent> medReqEntries = new ArrayList<>();
      if (shouldExport(MedicationRequest.class)) {
        for (Medication medication : encounter.medications) {
          BundleEntryComponent medReqEntry = medicationRequest(person, personEntry,
              bundle, encounterEntry, encounter, medication);
          medReqEntries.add(medReqEntry);

          // Scheduled individual MedicationAdministration events (nurse medication overview)
          if (shouldExport(MedicationAdministration.class)) {
            scheduledMedicationAdministrations(person, personEntry, bundle,
                encounterEntry, medication, medReqEntry);
          }

          // CommunicationRequest: medication reminder
          if (shouldExport(CommunicationRequest.class)) {
            medicationReminder(personEntry, bundle, medReqEntry, medication, encounter);
          }
        }
      }

      // Medication-based CarePlan (schedule overview) for encounters with medications
      if (!encounter.medications.isEmpty()
          && shouldExport(org.hl7.fhir.r4.model.CarePlan.class)) {
        medicationCarePlan(personEntry, bundle, encounterEntry, encounter,
            encounter.medications);
      }

      // Nurse handover task board (medications, labs, shift handoff)
      if (shouldExport(Task.class)) {
        nurseHandoverTasks(personEntry, bundle, encounterEntry, encounter, medReqEntries);
      }

      if (shouldExport(Immunization.class)) {
        for (HealthRecord.Entry immunization : encounter.immunizations) {
          immunization(personEntry, bundle, encounterEntry, immunization);
        }
      }

      if (shouldExport(DiagnosticReport.class)) {
        for (Report report : encounter.reports) {
          report(personEntry, bundle, encounterEntry, report);
        }
      }

      if (shouldExport(org.hl7.fhir.r4.model.CarePlan.class)) {
        final boolean shouldExportCareTeam = shouldExport(CareTeam.class);
        for (CarePlan careplan : encounter.careplans) {
          BundleEntryComponent careTeamEntry = null;

          if (shouldExportCareTeam) {
            careTeamEntry = careTeam(person, personEntry, bundle, encounterEntry, careplan);
          }
          carePlan(person, personEntry, bundle, encounterEntry, encounter.provider, careTeamEntry,
                  careplan);
        }
      }

      if (shouldExport(org.hl7.fhir.r4.model.ImagingStudy.class)) {
        for (ImagingStudy imagingStudy : encounter.imagingStudies) {
          imagingStudy(personEntry, bundle, encounterEntry, imagingStudy);
        }
      }

      if (USE_US_CORE_IG && shouldExport(DiagnosticReport.class)) {
        String clinicalNoteText = ClinicalNoteExporter.export(person, encounter);
        boolean lastNote =
            (encounter == person.record.encounters.get(person.record.encounters.size() - 1));
        clinicalNote(person, personEntry, bundle, encounterEntry, clinicalNoteText, lastNote);
      }

      if (shouldExport(org.hl7.fhir.r4.model.Claim.class)) {
        // one claim per encounter
        BundleEntryComponent encounterClaim =
            encounterClaim(person, personEntry, bundle, encounterEntry, encounter);

        if (shouldExport(ExplanationOfBenefit.class)) {
          explanationOfBenefit(personEntry, bundle, encounterEntry, person,
              encounterClaim, encounter, encounter.claim);
        }
      }

      // ====== Patient Journey / Scheduling Resources ======
      // Schedule → Slot → Appointment (for scheduled encounters)
      // ServiceRequest + Task (for procedures and imaging studies)
      BundleEntryComponent scheduleEntry = null;
      BundleEntryComponent slotEntry = null;

      if (shouldExport(Schedule.class)) {
        scheduleEntry = schedule(personEntry, bundle, encounterEntry, encounter);
      }

      if (shouldExport(Slot.class) && scheduleEntry != null) {
        slotEntry = slot(bundle, scheduleEntry, encounter);
      }

      if (shouldExport(Appointment.class) && slotEntry != null
          && !isUnscheduledEncounter(encounter)) {
        BundleEntryComponent apptEntry = appointment(person, personEntry, bundle,
            encounterEntry, slotEntry, encounter);

        // Add reasonReference to the Appointment linking to Condition
        if (apptEntry != null && encounter.reason != null) {
          Appointment apptResource = (Appointment) apptEntry.getResource();
          BundleEntryComponent condEntry =
              findConditionResourceByCode(bundle, encounter.reason.code);
          if (condEntry != null) {
            apptResource.addReasonReference(new Reference(condEntry.getFullUrl()));
          }
        }

        // CommunicationRequest: appointment reminder (not for emergency encounters)
        if (apptEntry != null && shouldExport(CommunicationRequest.class)
            && !isUnscheduledEncounter(encounter)) {
          Appointment apptRes = (Appointment) apptEntry.getResource();
          if (apptRes.getStatus() != AppointmentStatus.CANCELLED
              && apptRes.getStatus() != AppointmentStatus.NOSHOW) {
            appointmentReminder(personEntry, bundle, apptEntry, encounter);
          }
        }
      }

      if (shouldExport(ServiceRequest.class)) {
        // Create ServiceRequests for each Procedure
        for (Procedure procedure : encounter.procedures) {
          Code procCode = procedure.codes.get(0);
          Code reasonCode = procedure.reasons.isEmpty() ? null : procedure.reasons.get(0);
          BundleEntryComponent srEntry = serviceRequest(personEntry, bundle,
              encounterEntry, encounter, procCode, reasonCode,
              procedure.uuid.toString(), procedure.start);

          // Create a Task to track fulfilment of this ServiceRequest
          if (shouldExport(Task.class)) {
            task(personEntry, bundle, encounterEntry, srEntry, encounter,
                procCode, procedure.start,
                procedure.stop != 0L ? procedure.stop : 0L);
          }
        }

        // Create ServiceRequests for each ImagingStudy
        for (ImagingStudy imagingStudy : encounter.imagingStudies) {
          Code imgCode = imagingStudy.codes.get(0);
          BundleEntryComponent srEntry = serviceRequest(personEntry, bundle,
              encounterEntry, encounter, imgCode, null,
              imagingStudy.uuid.toString(), imagingStudy.start);

          if (shouldExport(Task.class)) {
            task(personEntry, bundle, encounterEntry, srEntry, encounter,
                imgCode, imagingStudy.start,
                imagingStudy.stop != 0L ? imagingStudy.stop : imagingStudy.start);
          }
        }
      }

      // ====== Discharge Planning (for inpatient/SNF/hospice encounters) ======
      EncounterType encType = EncounterType.fromString(encounter.type);
      if (encType == EncounterType.INPATIENT || encType == EncounterType.SNF
          || encType == EncounterType.HOSPICE) {
        BundleEntryComponent dischargeCareTeamEntry = null;
        if (shouldExport(CareTeam.class)) {
          dischargeCareTeamEntry = dischargeCareTeam(personEntry, bundle,
              encounterEntry, encounter);
        }
        if (shouldExport(org.hl7.fhir.r4.model.CarePlan.class)) {
          dischargePlan(personEntry, bundle, encounterEntry, encounter,
              dischargeCareTeamEntry);
        }

        if (shouldExport(Task.class) || shouldExport(CommunicationRequest.class)) {
          dischargeFollowUpWorkflow(personEntry, bundle, encounterEntry, encounter,
              dischargeCareTeamEntry);
        }
      }

      // Tumorboard v1/v2 preparation and post-board follow-up plan
      if (shouldExport(org.hl7.fhir.r4.model.CarePlan.class)
          && isTumorBoardCandidate(encounter)) {
        tumorBoardCarePlans(personEntry, bundle, encounterEntry, encounter);
      }
    }

    if (USE_US_CORE_IG && shouldExport(Provenance.class)) {
      // Add Provenance to the Bundle
      provenance(bundle, person, stopTime);
    }
    return bundle;
  }

  /**
   * Convert the given Person into a JSON String, containing a FHIR Bundle of the Person and the
   * associated entries from their health record.
   *
   * @param person   Person to generate the FHIR JSON for
   * @param stopTime Time the simulation ended
   * @return String containing a JSON representation of a FHIR Bundle containing the Person's health
   *     record
   */
  public static String convertToFHIRJson(Person person, long stopTime) {
    Bundle bundle = convertToFHIR(person, stopTime);
    Boolean pretty = Config.getAsBoolean("exporter.pretty_print", true);
    String bundleJson = FHIR_CTX.newJsonParser().setPrettyPrint(pretty)
        .encodeResourceToString(bundle);

    return bundleJson;
  }

  // ====== History Export: Sequential Transaction Bundles for Simulation Replay ======

  /**
   * A single event in the appointment lifecycle history, representing one transaction
   * bundle with a timestamp and descriptive label.
   */
  public static class HistoryEvent implements Comparable<HistoryEvent> {
    /** Timestamp of this event (millis since epoch). */
    public final long timestamp;
    /** The transaction bundle for this event. */
    public final Bundle bundle;
    /** Human-readable label for this event (e.g., "appointment-booked"). */
    public final String label;
    /** Sequence index (0-based), set during export. */
    public int sequenceIndex;

    public HistoryEvent(long timestamp, Bundle bundle, String label) {
      this.timestamp = timestamp;
      this.bundle = bundle;
      this.label = label;
    }

    @Override
    public int compareTo(HistoryEvent other) {
      return Long.compare(this.timestamp, other.timestamp);
    }
  }

  /**
   * Generate sequential transaction bundles showing the full appointment lifecycle history
   * for a patient. Each bundle is a valid FHIR transaction that can be POSTed to a server
   * in order to replay the patient's scheduling journey.
   *
   * <p>The history includes:
   * <ul>
   *   <li>Bundle 000: Initial patient + organization + practitioner resources (POST)</li>
   *   <li>Bundle 001+: Per-encounter scheduling events:
   *     <ul>
   *       <li>Appointment proposed/booked (POST)</li>
   *       <li>Appointment status change to fulfilled/noshow/cancelled (PUT + Provenance)</li>
   *       <li>For cancelled: rescheduled appointment creation</li>
   *     </ul>
   *   </li>
   * </ul>
   *
   * @param person   Person to generate history for
   * @param stopTime Time the simulation ended
   * @return Ordered list of HistoryEvents, each containing a transaction bundle
   */
  public static List<HistoryEvent> convertToFHIRHistory(Person person, long stopTime) {
    List<HistoryEvent> events = new ArrayList<>();

    // ---- Event 0: Initial patient setup (all non-scheduling resources) ----
    Bundle mainBundle = convertToFHIR(person, stopTime);
    // The main bundle already has everything including final-state appointments.
    // We keep it as the "initial" bundle for non-scheduling resources.
    long earliestEncounter = Long.MAX_VALUE;
    for (Encounter enc : person.record.encounters) {
      if (enc.start < earliestEncounter) {
        earliestEncounter = enc.start;
      }
    }
    long initialTime = earliestEncounter != Long.MAX_VALUE
        ? earliestEncounter - 30L * 24 * 60 * 60 * 1000 // 30 days before first encounter
        : stopTime;

    events.add(new HistoryEvent(initialTime, mainBundle, "initial"));

    // ---- Generate per-encounter appointment lifecycle events ----
    // We need to reconstruct the appointment lifecycle for each encounter.
    // The main bundle already has final-state appointments, but for history
    // we need intermediate states (booked → arrived → fulfilled/noshow/cancelled).

    String patientHistoryRef = "Patient/" + person.attributes.get(Person.ID);

    for (Encounter encounter : person.record.encounters) {
      if (isUnscheduledEncounter(encounter)) {
        continue;
      }
      if (!shouldExport(Appointment.class) || !shouldExport(Slot.class)) {
        continue;
      }

      // Find the appointment ID deterministically (same as in appointment() method)
      String apptId = UUID.nameUUIDFromBytes(
          ("appointment-" + encounter.uuid.toString()).getBytes()).toString();
      String apptFullUrl = getUrlPrefix("Appointment") + apptId;

      // Determine the lifecycle outcome (must use same RNG seed as original export)
      // We use encounter-specific deterministic values
      double roll = deterministicRandom(encounter.uuid.toString(), "appt-lifecycle");
      AppointmentStatus finalStatus;
      String cancelReason = null;
      boolean createRescheduled = false;

      if (roll < APPT_PROB_FULFILLED) {
        finalStatus = AppointmentStatus.FULFILLED;
      } else if (roll < APPT_PROB_CANCELLED) {
        finalStatus = AppointmentStatus.CANCELLED;
        cancelReason = deterministicRandom(encounter.uuid.toString(), "cancel-reason")
            < 0.6 ? "pat" : "prov";
        createRescheduled = true;
      } else if (roll < APPT_PROB_NOSHOW) {
        finalStatus = AppointmentStatus.NOSHOW;
      } else if (roll < APPT_PROB_BOOKED) {
        finalStatus = AppointmentStatus.BOOKED;
      } else {
        finalStatus = AppointmentStatus.WAITLIST;
      }

      // Calculate lifecycle timestamps
      long bookingLeadMs = (1 + Math.abs(encounter.uuid.hashCode() % 30))
          * 24L * 60 * 60 * 1000;
      long bookedTime = encounter.start - bookingLeadMs;
      long arrivedTime = encounter.start;
      long completedTime = encounter.stop > 0 ? encounter.stop : encounter.start + 3600000;

      // Clinician references for Provenance
      String clinicianRef = null;
      if (encounter.clinician != null) {
        clinicianRef = ExportHelper.buildFhirNpiSearchUrl(encounter.clinician);
      }

      // ---- Lifecycle Event 1: Appointment BOOKED ----
      Bundle bookedBundle = createHistoryTransactionBundle();
      bookedBundle.setTimestamp(new Date(bookedTime));
      {
        Appointment bookedAppt = new Appointment();
        bookedAppt.setId(apptId);
        bookedAppt.setStatus(AppointmentStatus.BOOKED);
        bookedAppt.addServiceType(encounterServiceType(encounter));
        bookedAppt.setStart(new Date(encounter.start));
        bookedAppt.setEnd(new Date(encounter.stop));
        long durationMin = (encounter.stop - encounter.start) / (60 * 1000);
        bookedAppt.setMinutesDuration((int) Math.max(durationMin, 1));
        bookedAppt.setCreated(new Date(bookedTime));
        bookedAppt.setPriority(5);

        // Patient participant
        AppointmentParticipantComponent patPart = bookedAppt.addParticipant();
        patPart.setActor(new Reference(patientHistoryRef));
        patPart.addType(mapCodeToCodeableConcept(
            new Code("http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
                "SBJ", "subject"), null));
        patPart.setStatus(ParticipationStatus.NEEDSACTION);

        // Clinician participant
        if (clinicianRef != null) {
          AppointmentParticipantComponent clinPart = bookedAppt.addParticipant();
          clinPart.setActor(new Reference(clinicianRef));
          clinPart.addType(mapCodeToCodeableConcept(
              new Code("http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
                  "PPRF", "primary performer"), null));
          clinPart.setStatus(ParticipationStatus.ACCEPTED);
        }

        // Set meta.versionId for history tracking
        Meta meta = new Meta();
        meta.setVersionId("1");
        meta.setLastUpdated(new Date(bookedTime));
        bookedAppt.setMeta(meta);

        addHistoryEntry(bookedBundle, bookedAppt, apptId, HTTPVerb.PUT);

        // Provenance for creation
        addHistoryProvenance(bookedBundle, apptFullUrl, bookedTime,
            "CREATE", clinicianRef, "Appointment booked");
      }
      events.add(new HistoryEvent(bookedTime, bookedBundle, "appointment-booked"));

      // ---- Lifecycle Event 2: Status change (depends on outcome) ----
      if (finalStatus == AppointmentStatus.FULFILLED) {
        // 2a: Patient arrives
        Bundle arrivedBundle = createHistoryTransactionBundle();
        arrivedBundle.setTimestamp(new Date(arrivedTime));
        {
          Appointment arrivedAppt = new Appointment();
          arrivedAppt.setId(apptId);
          arrivedAppt.setStatus(AppointmentStatus.ARRIVED);
          arrivedAppt.addServiceType(encounterServiceType(encounter));
          arrivedAppt.setStart(new Date(encounter.start));
          arrivedAppt.setEnd(new Date(encounter.stop));
          arrivedAppt.setCreated(new Date(bookedTime));

          AppointmentParticipantComponent patPart = arrivedAppt.addParticipant();
          patPart.setActor(new Reference(patientHistoryRef));
          patPart.addType(mapCodeToCodeableConcept(
              new Code("http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
                  "SBJ", "subject"), null));
          patPart.setStatus(ParticipationStatus.ACCEPTED);

          Meta meta = new Meta();
          meta.setVersionId("2");
          meta.setLastUpdated(new Date(arrivedTime));
          arrivedAppt.setMeta(meta);

          addHistoryEntry(arrivedBundle, arrivedAppt, apptId, HTTPVerb.PUT);
          addHistoryProvenance(arrivedBundle, apptFullUrl, arrivedTime,
              "UPDATE", clinicianRef, "Patient arrived");
        }
        events.add(new HistoryEvent(arrivedTime, arrivedBundle, "patient-arrived"));

        // 2b: Appointment fulfilled
        Bundle fulfilledBundle = createHistoryTransactionBundle();
        fulfilledBundle.setTimestamp(new Date(completedTime));
        {
          Appointment fulfilledAppt = new Appointment();
          fulfilledAppt.setId(apptId);
          fulfilledAppt.setStatus(AppointmentStatus.FULFILLED);
          fulfilledAppt.addServiceType(encounterServiceType(encounter));
          fulfilledAppt.setStart(new Date(encounter.start));
          fulfilledAppt.setEnd(new Date(encounter.stop));
          fulfilledAppt.setCreated(new Date(bookedTime));

          AppointmentParticipantComponent patPart = fulfilledAppt.addParticipant();
          patPart.setActor(new Reference(patientHistoryRef));
          patPart.addType(mapCodeToCodeableConcept(
              new Code("http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
                  "SBJ", "subject"), null));
          patPart.setStatus(ParticipationStatus.ACCEPTED);

          Meta meta = new Meta();
          meta.setVersionId("3");
          meta.setLastUpdated(new Date(completedTime));
          fulfilledAppt.setMeta(meta);

          addHistoryEntry(fulfilledBundle, fulfilledAppt, apptId, HTTPVerb.PUT);
          addHistoryProvenance(fulfilledBundle, apptFullUrl, completedTime,
              "UPDATE", clinicianRef, "Appointment fulfilled");
        }
        events.add(new HistoryEvent(completedTime, fulfilledBundle,
            "appointment-fulfilled"));

      } else if (finalStatus == AppointmentStatus.NOSHOW) {
        // Patient no-show at appointment time
        Bundle noshowBundle = createHistoryTransactionBundle();
        noshowBundle.setTimestamp(new Date(arrivedTime + 900000)); // 15 min after start
        {
          Appointment noshowAppt = new Appointment();
          noshowAppt.setId(apptId);
          noshowAppt.setStatus(AppointmentStatus.NOSHOW);
          noshowAppt.addServiceType(encounterServiceType(encounter));
          noshowAppt.setStart(new Date(encounter.start));
          noshowAppt.setEnd(new Date(encounter.stop));
          noshowAppt.setCreated(new Date(bookedTime));

          AppointmentParticipantComponent patPart = noshowAppt.addParticipant();
          patPart.setActor(new Reference(patientHistoryRef));
          patPart.addType(mapCodeToCodeableConcept(
              new Code("http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
                  "SBJ", "subject"), null));
          patPart.setStatus(ParticipationStatus.DECLINED);

          Meta meta = new Meta();
          meta.setVersionId("2");
          meta.setLastUpdated(new Date(arrivedTime + 900000));
          noshowAppt.setMeta(meta);

          addHistoryEntry(noshowBundle, noshowAppt, apptId, HTTPVerb.PUT);
          addHistoryProvenance(noshowBundle, apptFullUrl, arrivedTime + 900000,
              "UPDATE", clinicianRef, "Patient did not show up");
        }
        events.add(new HistoryEvent(arrivedTime + 900000, noshowBundle,
            "appointment-noshow"));

      } else if (finalStatus == AppointmentStatus.CANCELLED) {
        // Cancellation happens some time after booking
        long cancelTime = bookedTime + (arrivedTime - bookedTime) / 2;
        Bundle cancelBundle = createHistoryTransactionBundle();
        cancelBundle.setTimestamp(new Date(cancelTime));
        {
          Appointment cancelledAppt = new Appointment();
          cancelledAppt.setId(apptId);
          cancelledAppt.setStatus(AppointmentStatus.CANCELLED);
          cancelledAppt.addServiceType(encounterServiceType(encounter));
          cancelledAppt.setStart(new Date(encounter.start));
          cancelledAppt.setEnd(new Date(encounter.stop));
          cancelledAppt.setCreated(new Date(bookedTime));

          // Cancellation reason
          String crDisplay = "pat".equals(cancelReason)
              ? "Patient request" : "Provider request";
          cancelledAppt.setCancelationReason(
              mapCodeToCodeableConcept(
                  new Code(CANCEL_REASON_SYSTEM, cancelReason, crDisplay),
                  CANCEL_REASON_SYSTEM));

          AppointmentParticipantComponent patPart = cancelledAppt.addParticipant();
            patPart.setActor(new Reference(patientHistoryRef));
          patPart.addType(mapCodeToCodeableConcept(
              new Code("http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
                  "SBJ", "subject"), null));
          patPart.setStatus(ParticipationStatus.DECLINED);

          Meta meta = new Meta();
          meta.setVersionId("2");
          meta.setLastUpdated(new Date(cancelTime));
          cancelledAppt.setMeta(meta);

          addHistoryEntry(cancelBundle, cancelledAppt, apptId, HTTPVerb.PUT);
          addHistoryProvenance(cancelBundle, apptFullUrl, cancelTime,
              "UPDATE", clinicianRef,
              "Appointment cancelled: " + crDisplay);
        }
        events.add(new HistoryEvent(cancelTime, cancelBundle,
            "appointment-cancelled"));

        // Rescheduled appointment
        if (createRescheduled) {
          long rescheduleDelayMs = (1 + Math.abs(
              (encounter.uuid.hashCode() * 31) % 14)) * 24L * 60 * 60 * 1000;
          long rescheduledTime = encounter.start + rescheduleDelayMs;
          String reschedApptId = UUID.nameUUIDFromBytes(
              ("appointment-rescheduled-" + encounter.uuid.toString()).getBytes()).toString();
          String reschedFullUrl = getUrlPrefix("Appointment") + reschedApptId;

          Bundle reschedBundle = createHistoryTransactionBundle();
          reschedBundle.setTimestamp(new Date(cancelTime + 3600000)); // 1hr after cancel
          {
            Appointment reschedAppt = new Appointment();
            reschedAppt.setId(reschedApptId);
            reschedAppt.setStatus(AppointmentStatus.BOOKED);
            reschedAppt.addServiceType(encounterServiceType(encounter));
            reschedAppt.setStart(new Date(rescheduledTime));
            reschedAppt.setEnd(new Date(rescheduledTime
                + (encounter.stop - encounter.start)));
            reschedAppt.setCreated(new Date(cancelTime + 3600000));
            reschedAppt.setPriority(5);

            AppointmentParticipantComponent patPart = reschedAppt.addParticipant();
            patPart.setActor(new Reference(patientHistoryRef));
            patPart.addType(mapCodeToCodeableConcept(
                new Code("http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
                    "SBJ", "subject"), null));
            patPart.setStatus(ParticipationStatus.ACCEPTED);

            Meta meta = new Meta();
            meta.setVersionId("1");
            meta.setLastUpdated(new Date(cancelTime + 3600000));
            reschedAppt.setMeta(meta);

            addHistoryEntry(reschedBundle, reschedAppt, reschedApptId, HTTPVerb.PUT);
            addHistoryProvenance(reschedBundle, reschedFullUrl, cancelTime + 3600000,
                "CREATE", clinicianRef,
                "Rescheduled appointment after cancellation");
          }
          events.add(new HistoryEvent(cancelTime + 3600000, reschedBundle,
              "appointment-rescheduled"));
        }
      }
      // For BOOKED and WAITLIST, no further status changes (still pending)
    }

    // Sort all events chronologically and assign sequence indices
    Collections.sort(events);
    for (int i = 0; i < events.size(); i++) {
      events.get(i).sequenceIndex = i;
    }

    return events;
  }

  /**
   * Generate a manifest Bundle (type=collection) that indexes all history event bundles
   * for a patient. This can be used by consumers to understand the playback sequence.
   *
   * @param events  The ordered list of history events
   * @param person  The patient
   * @return A manifest Bundle
   */
  public static Bundle createHistoryManifest(List<HistoryEvent> events, Person person) {
    Bundle manifest = new Bundle();
    manifest.setType(BundleType.COLLECTION);
    manifest.setTimestamp(new Date());

    manifest.setIdentifier(new Identifier()
        .setSystem(SYNTHEA_IDENTIFIER)
        .setValue("history-" + person.attributes.get(Person.ID)));

    Meta meta = new Meta();
    meta.addTag()
        .setSystem(SYNTHEA_EXT + "tags")
        .setCode("transaction-sequence")
        .setDisplay("Sequential transaction bundles for patient simulation replay");
    manifest.setMeta(meta);

    manifest.setTotal(events.size());

    for (HistoryEvent event : events) {
      String filename = String.format("%03d_%s_%s.json",
          event.sequenceIndex,
          new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss'Z'")
              .format(new Date(event.timestamp)),
          event.label);

      Basic indexEntry = new Basic();
      indexEntry.setCode(new CodeableConcept().setText("Transaction sequence manifest entry"));
      indexEntry.addExtension()
          .setUrl(SYNTHEA_EXT + "StructureDefinition/sequence-index")
          .setValue(new IntegerType(event.sequenceIndex));
      indexEntry.addExtension()
          .setUrl(SYNTHEA_EXT + "StructureDefinition/event-timestamp")
          .setValue(new InstantType(new Date(event.timestamp)));
      indexEntry.addExtension()
          .setUrl(SYNTHEA_EXT + "StructureDefinition/event-label")
          .setValue(new StringType(event.label));

      BundleEntryComponent entry = manifest.addEntry();
      entry.setFullUrl(filename);
      entry.setResource(indexEntry);
    }

    return manifest;
  }

  /**
   * Create a new empty transaction Bundle for a history event.
   */
  private static Bundle createHistoryTransactionBundle() {
    Bundle bundle = new Bundle();
    bundle.setType(BundleType.TRANSACTION);
    return bundle;
  }

  /**
   * Add a resource entry to a history transaction bundle with the specified HTTP verb.
   */
  private static BundleEntryComponent addHistoryEntry(Bundle bundle, Resource resource,
      String resourceId, HTTPVerb method) {
    BundleEntryComponent entry = bundle.addEntry();
    resource.setId(resourceId);
    entry.setFullUrl(getUrlPrefix(resource.fhirType()) + resourceId);
    entry.setResource(resource);

    BundleEntryRequestComponent request = entry.getRequest();
    request.setMethod(method);
    String resourceType = resource.getResourceType().name();
    if (method == HTTPVerb.PUT) {
      request.setUrl(resourceType + "/" + resourceId);
    } else {
      request.setUrl(resourceType);
    }
    entry.setRequest(request);

    // Add response metadata for history tracking
    BundleEntryResponseComponent response = entry.getResponse();
    if (resource.hasMeta() && resource.getMeta().hasVersionId()) {
      response.setEtag("W/\"" + resource.getMeta().getVersionId() + "\"");
      response.setLastModified(resource.getMeta().getLastUpdated());
    }
    response.setStatus(method == HTTPVerb.POST ? "201 Created" : "200 OK");
    entry.setResponse(response);

    return entry;
  }

  /**
   * Add a Provenance resource to a history transaction bundle that records
   * a state change with the specified activity code.
   *
   * @param bundle       The transaction bundle to add the Provenance to
   * @param targetUrl    The full URL of the target resource (e.g., Appointment)
   * @param recordedTime When this change was recorded
   * @param activityCode The v3-DataOperation code (CREATE, UPDATE, DELETE)
   * @param agentRef     Reference to the agent (Practitioner) who performed this
   * @param description  Human-readable description of the change
   */
  private static void addHistoryProvenance(Bundle bundle, String targetUrl,
      long recordedTime, String activityCode, String agentRef,
      String description) {

    Provenance prov = new Provenance();
    prov.addTarget(new Reference(targetUrl));
    prov.setRecorded(new Date(recordedTime));

    // Activity coding
    prov.setActivity(new CodeableConcept()
        .addCoding(new Coding()
            .setSystem("http://terminology.hl7.org/CodeSystem/v3-DataOperation")
            .setCode(activityCode)
            .setDisplay(activityCode.substring(0, 1)
                + activityCode.substring(1).toLowerCase())));

    // Agent
    ProvenanceAgentComponent agent = prov.addAgent();
    agent.setType(mapCodeToCodeableConcept(
        new Code("http://terminology.hl7.org/CodeSystem/provenance-participant-type",
            "author", "Author"), null));
    if (agentRef != null) {
      agent.setWho(new Reference(agentRef));
    } else {
      agent.setWho(new Reference().setDisplay("System"));
    }

    // Description as text
    prov.addReason(new CodeableConcept().setText(description));

    String provId = UUID.nameUUIDFromBytes(
        ("provenance-" + targetUrl + "-" + recordedTime + "-" + activityCode)
            .getBytes()).toString();

    addHistoryEntry(bundle, prov, provId, HTTPVerb.PUT);
  }

  /**
   * Generate a deterministic random double (0.0-1.0) from a seed string and salt.
   * Used instead of Person.rand() for history export to ensure consistent lifecycle
   * decisions independent of RNG call order.
   */
  private static double deterministicRandom(String seed, String salt) {
    long hash = UUID.nameUUIDFromBytes((seed + salt).getBytes()).getMostSignificantBits();
    return (double) (hash & 0x7FFFFFFFL) / (double) 0x7FFFFFFFL;
  }

  /**
   * Map the given Person to a FHIR Patient resource, and add it to the given Bundle.
   *
   * @param person   The Person
   * @param bundle   The Bundle to add to
   * @param stopTime Time the simulation ended
   * @return The created Entry
   */
  @SuppressWarnings("rawtypes")
  private static BundleEntryComponent basicInfo(Person person, Bundle bundle, long stopTime) {
    Patient patientResource = new Patient();

    patientResource.addIdentifier().setSystem(SYNTHEA_IDENTIFIER)
        .setValue((String) person.attributes.get(Person.ID));

    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient");
      patientResource.setMeta(meta);
    }

    Code mrnCode = new Code("http://terminology.hl7.org/CodeSystem/v2-0203", "MR", "Medical Record Number");
    patientResource.addIdentifier()
        .setType(mapCodeToCodeableConcept(mrnCode, "http://terminology.hl7.org/CodeSystem/v2-0203"))
        .setSystem("http://hospital.smarthealthit.org")
        .setValue((String) person.attributes.get(Person.ID));

    if ("US".equalsIgnoreCase(COUNTRY_CODE)) {
      Code ssnCode = new Code("http://terminology.hl7.org/CodeSystem/v2-0203", "SS", "Social Security Number");
      patientResource.addIdentifier()
        .setType(mapCodeToCodeableConcept(ssnCode, "http://terminology.hl7.org/CodeSystem/v2-0203"))
        .setSystem("http://hl7.org/fhir/sid/us-ssn")
        .setValue((String) person.attributes.get(Person.IDENTIFIER_SSN));
    }

    if (person.attributes.get(Person.IDENTIFIER_DRIVERS) != null) {
      Code driversCode = new Code("http://terminology.hl7.org/CodeSystem/v2-0203", "DL", "Driver's license number");
      patientResource.addIdentifier()
          .setType(mapCodeToCodeableConcept(driversCode, "http://terminology.hl7.org/CodeSystem/v2-0203"))
          .setSystem("urn:oid:2.16.840.1.113883.4.3.25")
          .setValue((String) person.attributes.get(Person.IDENTIFIER_DRIVERS));
    }

    if (person.attributes.get(Person.IDENTIFIER_PASSPORT) != null) {
      Code passportCode = new Code("http://terminology.hl7.org/CodeSystem/v2-0203", "PPN", "Passport Number");
      patientResource.addIdentifier()
          .setType(mapCodeToCodeableConcept(passportCode, "http://terminology.hl7.org/CodeSystem/v2-0203"))
          .setSystem(PASSPORT_URI)
          .setValue((String) person.attributes.get(Person.IDENTIFIER_PASSPORT));
    }


    if (person.attributes.get(Person.ENTITY) != null) {
      Entity entity = (Entity) person.attributes.get(Person.ENTITY);
      patientResource.addIdentifier()
          .setSystem("http://mitre.org/record_id")
          .setValue(entity.getIndividualId());
      patientResource.addIdentifier()
          .setSystem("http://mitre.org/seed_record_id")
          .setValue(String.valueOf(person.attributes.get(Person.IDENTIFIER_SEED_ID)));
      patientResource.addIdentifier()
          .setSystem("http://mitre.org/variant_record_id")
          .setValue(String.valueOf((String) person.attributes.get(Person.HOUSEHOLD)));
    }

    if (person.attributes.get(Person.CONTACT_EMAIL) != null) {
      ContactComponent contact = new ContactComponent();
      HumanName contactName = new HumanName();
      contactName.setUse(HumanName.NameUse.OFFICIAL);
      contactName.addGiven((String) person.attributes.get(Person.CONTACT_GIVEN_NAME));
      contactName.setFamily((String) person.attributes.get(Person.CONTACT_FAMILY_NAME));
      contact.setName(contactName);
      contact.addTelecom().setSystem(ContactPointSystem.EMAIL)
          .setUse(ContactPointUse.HOME)
          .setValue((String) person.attributes.get(Person.CONTACT_EMAIL));
      patientResource.addContact(contact);
    }

    if (USE_US_CORE_IG) {
      // We do not yet account for mixed race
      Extension raceExtension = new Extension(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race");
      String race = (String) person.attributes.get(Person.RACE);

      String raceDisplay;
      switch (race) {
        case "white":
          raceDisplay = "White";
          break;
        case "black":
          raceDisplay = "Black or African American";
          break;
        case "asian":
          raceDisplay = "Asian";
          break;
        case "native":
          raceDisplay = "American Indian or Alaska Native";
          break;
        case "hawaiian":
          raceDisplay = "Native Hawaiian or Other Pacific Islander";
          break;
        default:
          raceDisplay = "Other";
          break;
      }

      String raceNum = (String) raceEthnicityCodes.get(race);

      Extension raceCodingExtension = new Extension("ombCategory");
      Coding raceCoding = new Coding();
      if (raceDisplay.equals("Other")) {
        raceCoding.setSystem("http://terminology.hl7.org/CodeSystem/v3-NullFlavor");
        raceCoding.setCode("UNK");
        raceCoding.setDisplay("Unknown");
      } else {
        raceCoding.setSystem("urn:oid:2.16.840.1.113883.6.238");
        raceCoding.setCode(raceNum);
        raceCoding.setDisplay(raceDisplay);
      }
      raceCodingExtension.setValue(raceCoding);
      raceExtension.addExtension(raceCodingExtension);

      Extension raceTextExtension = new Extension("text");
      raceTextExtension.setValue(new StringType(raceDisplay));
      raceExtension.addExtension(raceTextExtension);
      patientResource.addExtension(raceExtension);

      // We do not yet account for mixed ethnicity
      Extension ethnicityExtension = new Extension(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity");
      String ethnicity = (String) person.attributes.get(Person.ETHNICITY);

      String ethnicityDisplay;
      if (ethnicity.equals("hispanic")) {
        ethnicity = "hispanic";
        ethnicityDisplay = "Hispanic or Latino";
      } else {
        ethnicity = "nonhispanic";
        ethnicityDisplay = "Not Hispanic or Latino";
      }

      String ethnicityNum = (String) raceEthnicityCodes.get(ethnicity);

      Extension ethnicityCodingExtension = new Extension("ombCategory");
      Coding ethnicityCoding = new Coding();
      ethnicityCoding.setSystem("urn:oid:2.16.840.1.113883.6.238");
      ethnicityCoding.setCode(ethnicityNum);
      ethnicityCoding.setDisplay(ethnicityDisplay);
      ethnicityCodingExtension.setValue(ethnicityCoding);

      ethnicityExtension.addExtension(ethnicityCodingExtension);
      Extension ethnicityTextExtension = new Extension("text");
      ethnicityTextExtension.setValue(new StringType(ethnicityDisplay));
      ethnicityExtension.addExtension(ethnicityTextExtension);
      patientResource.addExtension(ethnicityExtension);
    }

    String firstLanguage = (String) person.attributes.get(Person.FIRST_LANGUAGE);
    Map languageMap = (Map) languageLookup.get(firstLanguage);
    Code languageCode = new Code((String) languageMap.get("system"),
        (String) languageMap.get("code"), (String) languageMap.get("display"));
    List<PatientCommunicationComponent> communication =
        new ArrayList<PatientCommunicationComponent>();
    communication.add(new PatientCommunicationComponent(
        mapCodeToCodeableConcept(languageCode, (String) languageMap.get("system"))));
    patientResource.setCommunication(communication);

    HumanName name = patientResource.addName();
    name.setUse(HumanName.NameUse.OFFICIAL);
    name.addGiven((String) person.attributes.get(Person.FIRST_NAME));
    if (person.attributes.containsKey(Person.MIDDLE_NAME)) {
      name.addGiven((String) person.attributes.get(Person.MIDDLE_NAME));
    }
    name.setFamily((String) person.attributes.get(Person.LAST_NAME));
    if (person.attributes.get(Person.NAME_PREFIX) != null) {
      name.addPrefix((String) person.attributes.get(Person.NAME_PREFIX));
    }
    if (person.attributes.get(Person.NAME_SUFFIX) != null) {
      name.addSuffix((String) person.attributes.get(Person.NAME_SUFFIX));
    }
    if (person.attributes.get(Person.MAIDEN_NAME) != null) {
      HumanName maidenName = patientResource.addName();
      maidenName.setUse(HumanName.NameUse.MAIDEN);
      maidenName.addGiven((String) person.attributes.get(Person.FIRST_NAME));
      if (person.attributes.containsKey(Person.MIDDLE_NAME)) {
        maidenName.addGiven((String) person.attributes.get(Person.MIDDLE_NAME));
      }
      maidenName.setFamily((String) person.attributes.get(Person.MAIDEN_NAME));
      if (person.attributes.get(Person.NAME_PREFIX) != null) {
        maidenName.addPrefix((String) person.attributes.get(Person.NAME_PREFIX));
      }
      if (person.attributes.get(Person.NAME_SUFFIX) != null) {
        maidenName.addSuffix((String) person.attributes.get(Person.NAME_SUFFIX));
      }
    }

    Extension mothersMaidenNameExtension = new Extension(
        "http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName");
    String mothersMaidenName = (String) person.attributes.get(Person.NAME_MOTHER);
    mothersMaidenNameExtension.setValue(new StringType(mothersMaidenName));
    patientResource.addExtension(mothersMaidenNameExtension);

    long birthdate = (long) person.attributes.get(Person.BIRTHDATE);
    patientResource.setBirthDate(new Date(birthdate));

    Extension birthSexExtension = new Extension(
        "http://hl7.org/fhir/us/core/StructureDefinition/us-core-birthsex");
    if (person.attributes.get(Person.GENDER).equals("M")) {
      patientResource.setGender(AdministrativeGender.MALE);
      birthSexExtension.setValue(new CodeType("M"));
    } else if (person.attributes.get(Person.GENDER).equals("F")) {
      patientResource.setGender(AdministrativeGender.FEMALE);
      birthSexExtension.setValue(new CodeType("F"));
    } else if (person.attributes.get(Person.GENDER).equals("UNK")) {
      patientResource.setGender(AdministrativeGender.UNKNOWN);
    }
    if (USE_US_CORE_IG) {
      patientResource.addExtension(birthSexExtension);
    }

    String state = (String) person.attributes.get(Person.STATE);
    if (USE_US_CORE_IG) {
      state = Location.getAbbreviation(state);
    }
    Address addrResource = patientResource.addAddress();
    addrResource.addLine((String) person.attributes.get(Person.ADDRESS))
        .setCity((String) person.attributes.get(Person.CITY))
        .setPostalCode((String) person.attributes.get(Person.ZIP))
        .setState(state);
    if (COUNTRY_CODE != null) {
      addrResource.setCountry(COUNTRY_CODE);
    }

    Address birthplace = new Address();
    birthplace.setCity((String) person.attributes.get(Person.BIRTH_CITY))
        .setState((String) person.attributes.get(Person.BIRTH_STATE))
        .setCountry((String) person.attributes.get(Person.BIRTH_COUNTRY));

    Extension birthplaceExtension = new Extension(
        "http://hl7.org/fhir/StructureDefinition/patient-birthPlace");
    birthplaceExtension.setValue(birthplace);
    patientResource.addExtension(birthplaceExtension);

    if (person.attributes.get(Person.MULTIPLE_BIRTH_STATUS) != null) {
      patientResource.setMultipleBirth(
          new IntegerType((int) person.attributes.get(Person.MULTIPLE_BIRTH_STATUS)));
    } else {
      patientResource.setMultipleBirth(new BooleanType(false));
    }

    patientResource.addTelecom().setSystem(ContactPointSystem.PHONE)
        .setUse(ContactPointUse.HOME)
      .setValue(normalizePhoneForCountry((String) person.attributes.get(Person.TELECOM)));

    String maritalStatus = ((String) person.attributes.get(Person.MARITAL_STATUS));
    if (maritalStatus != null) {
      Map<String, String> maritalStatusCodes = Map.of(
          "A", "Annulled",
          "D", "Divorced",
          "I", "Interlocutory",
          "L", "Legally Separated",
          "M", "Married",
          "P", "Polygamous",
          "T", "Domestic partner",
          "U", "unmarried",
          "S", "Never Married",
          "W", "Widowed");
      String maritalStatusDisplay = maritalStatusCodes.getOrDefault(maritalStatus, maritalStatus);
      Code maritalStatusCode = new Code("http://terminology.hl7.org/CodeSystem/v3-MaritalStatus",
          maritalStatus, maritalStatusDisplay);
      patientResource.setMaritalStatus(
          mapCodeToCodeableConcept(maritalStatusCode,
              "http://terminology.hl7.org/CodeSystem/v3-MaritalStatus"));
    } else {
      Code maritalStatusCode = new Code("http://terminology.hl7.org/CodeSystem/v3-MaritalStatus",
          "S", "Never Married");
      patientResource.setMaritalStatus(
          mapCodeToCodeableConcept(maritalStatusCode,
              "http://terminology.hl7.org/CodeSystem/v3-MaritalStatus"));
    }

    Point2D.Double coord = person.getLonLat();
    if (coord != null) {
      Extension geolocation = addrResource.addExtension();
      geolocation.setUrl("http://hl7.org/fhir/StructureDefinition/geolocation");
      geolocation.addExtension("latitude", new DecimalType(coord.getY()));
      geolocation.addExtension("longitude", new DecimalType(coord.getX()));
    }

    if (!person.alive(stopTime)) {
      patientResource.setDeceased(
          convertFhirDateTime((Long) person.attributes.get(Person.DEATHDATE), true));
    }

    String generatedBySynthea =
        "Generated by <a href=\"https://github.com/synthetichealth/synthea\">Synthea</a>."
        + "Version identifier: " + Utilities.SYNTHEA_VERSION + " . "
        + "  Person seed: " + person.getSeed()
        + "  Population seed: " + person.populationSeed;

    patientResource.setText(new Narrative().setStatus(NarrativeStatus.GENERATED)
        .setDiv(new XhtmlNode(NodeType.Element).setValue(generatedBySynthea)));

    // DALY and QALY values
    // we only write the last(current) one to the patient record
    Double dalyValue = (Double) person.attributes.get("most-recent-daly");
    Double qalyValue = (Double) person.attributes.get("most-recent-qaly");
    if (dalyValue != null) {
      Extension dalyExtension = new Extension(SYNTHEA_EXT + "disability-adjusted-life-years");
      DecimalType daly = new DecimalType(dalyValue);
      dalyExtension.setValue(daly);
      patientResource.addExtension(dalyExtension);

      Extension qalyExtension = new Extension(SYNTHEA_EXT + "quality-adjusted-life-years");
      DecimalType qaly = new DecimalType(qalyValue);
      qalyExtension.setValue(qaly);
      patientResource.addExtension(qalyExtension);
    }

    return newEntry(bundle, patientResource, (String) person.attributes.get(Person.ID));
  }

  /**
   * Add a code translation (if available) of the supplied source code to the
   * supplied CodeableConcept.
   * @param codeSystem the code system of the translated code
   * @param from the source code
   * @param to the CodeableConcept to add the translation to
   * @param rand a source of randomness
   */
  private static void addTranslation(String codeSystem, Code from,
          CodeableConcept to, RandomNumberGenerator rand) {
    CodeMapper mapper = Exporter.getCodeMapper(codeSystem);
    if (mapper != null && mapper.canMap(from)) {
      Coding coding = new Coding();
      Map.Entry<String, String> mappedCode = mapper.mapToCodeAndDescription(from, rand);
      coding.setCode(mappedCode.getKey());
      coding.setDisplay(mappedCode.getValue());
      coding.setSystem(ExportHelper.getSystemURI("ICD10-CM"));
      to.addCoding(coding);
    }
  }

  /**
   * Map the given Encounter into a FHIR Encounter resource, and add it to the given Bundle.
   *
   * @param personEntry Entry for the Person
   * @param bundle      The Bundle to add to
   * @param encounter   The current Encounter
   * @return The added Entry
   */
  private static BundleEntryComponent encounter(Person person, BundleEntryComponent personEntry,
                                                Bundle bundle, Encounter encounter) {
    org.hl7.fhir.r4.model.Encounter encounterResource = new org.hl7.fhir.r4.model.Encounter();
    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-encounter");
      encounterResource.setMeta(meta);
    }

    Patient patient = (Patient) personEntry.getResource();
    encounterResource.setSubject(new Reference()
        .setReference(personEntry.getFullUrl())
        .setDisplay(patient.getNameFirstRep().getNameAsSingleString()));

    encounterResource.setStatus(EncounterStatus.FINISHED);
    if (encounter.codes.isEmpty()) {
      // wellness encounter
      encounterResource.addType().addCoding().setCode("185349003")
          .setDisplay("Encounter for check up").setSystem(SNOMED_URI);
    } else {
      Code code = encounter.codes.get(0);
      encounterResource.addType(mapCodeToCodeableConcept(code, SNOMED_URI));
    }

    Coding classCode = new Coding();
    classCode.setCode(EncounterType.fromString(encounter.type).code());
    classCode.setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode");
    encounterResource.setClass_(classCode);
    encounterResource
        .setPeriod(new Period()
            .setStart(new Date(encounter.start))
            .setEnd(new Date(encounter.stop)));

    if (encounter.reason != null) {
      encounterResource.addReasonCode().addCoding().setCode(encounter.reason.code)
          .setDisplay(encounter.reason.display).setSystem(SNOMED_URI);
      addTranslation("ICD10-CM", encounter.reason,
              encounterResource.getReasonCodeFirstRep(), person);
    }

    Provider provider = encounter.provider;
    if (provider == null) {
      // no associated provider, patient goes to wellness provider
      provider = person.getProvider(EncounterType.WELLNESS, encounter.start);
    }

    if (TRANSACTION_BUNDLE) {
      encounterResource.setServiceProvider(new Reference(
              ExportHelper.buildFhirSearchUrl("Organization", provider.getResourceID())));
    } else {
      String providerFullUrl = findProviderUrl(provider, bundle);
      if (providerFullUrl != null) {
        encounterResource.setServiceProvider(new Reference(providerFullUrl));
      } else {
        BundleEntryComponent providerOrganization = provider(bundle, provider);
        encounterResource.setServiceProvider(new Reference(providerOrganization.getFullUrl()));
      }
    }
    encounterResource.getServiceProvider().setDisplay(provider.name);
    if (USE_US_CORE_IG) {
      String referenceUrl;
      String display;
      if (TRANSACTION_BUNDLE) {
        if (encounter.type.equals(EncounterType.VIRTUAL.toString())) {
          referenceUrl = ExportHelper.buildFhirSearchUrl("Location",
              FhirR4PatientHome.getPatientHome().getId());
          display = "Patient's Home";
        } else {
          referenceUrl = ExportHelper.buildFhirSearchUrl("Location",
              provider.getResourceLocationID());
          display = provider.name;
        }
      } else {
        if (encounter.type.equals(EncounterType.VIRTUAL.toString())) {
          referenceUrl = addPatientHomeLocation(bundle);
          display = "Patient's Home";
        } else {
          referenceUrl = findLocationUrl(provider, bundle);
          display = provider.name;
        }
      }
      encounterResource.addLocation().setLocation(new Reference()
          .setReference(referenceUrl)
          .setDisplay(display));
    }

    if (encounter.clinician != null) {
      Reference practitionerRef = clinicianReference(encounter.clinician, bundle);
      if (practitionerRef != null) {
        encounterResource.addParticipant().setIndividual(practitionerRef);
      }
      encounterResource.getParticipantFirstRep().getIndividual()
          .setDisplay(encounter.clinician.getFullname());
      encounterResource.getParticipantFirstRep().addType(mapCodeToCodeableConcept(
          new Code("http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
              "PPRF", "primary performer"), null));
      encounterResource.getParticipantFirstRep().setPeriod(encounterResource.getPeriod());
    }

    if (encounter.discharge != null) {
      EncounterHospitalizationComponent hospitalization = new EncounterHospitalizationComponent();
      Code dischargeDisposition = new Code(DISCHARGE_URI, encounter.discharge.code,
          encounter.discharge.display);
      hospitalization
          .setDischargeDisposition(mapCodeToCodeableConcept(dischargeDisposition, DISCHARGE_URI));
      encounterResource.setHospitalization(hospitalization);
    }

    BundleEntryComponent entry = newEntry(bundle, encounterResource, encounter.uuid.toString());
    if (USE_US_CORE_IG) {
      // US Core Encounters should have an identifier to support the required
      // Encounter.identifier search parameter
      encounterResource.addIdentifier()
          .setUse(IdentifierUse.OFFICIAL)
          .setSystem(SYNTHEA_IDENTIFIER)
          .setValue(encounterResource.getId());
    }
    return entry;
  }

  /**
   * Find the provider entry in this bundle, and return the associated "fullUrl" attribute.
   *
   * @param provider A given provider.
   * @param bundle   The current bundle being generated.
   * @return Provider.fullUrl if found, otherwise null.
   */
  private static String findProviderUrl(Provider provider, Bundle bundle) {
    for (BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource().fhirType().equals("Organization")) {
        Organization org = (Organization) entry.getResource();
        if (org.getIdentifierFirstRep().getValue() != null
            && org.getIdentifierFirstRep().getValue().equals(provider.getResourceID())) {
          return entry.getFullUrl();
        }
      }
    }
    return null;
  }

  /**
   * Finds the "patient's home" Location resource and returns the URL. If it does not yet exist in
   * the bundle, it will create it.
   * @param bundle the bundle to look in for the patient home resource
   * @return the URL of the patient home resource
   */
  public static String addPatientHomeLocation(Bundle bundle) {
    String locationURL = null;
    for (BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource().fhirType().equals("Location")) {
        if (entry.getResource().getId().equals(FhirR4PatientHome.getPatientHome().getId())) {
          locationURL = entry.getFullUrl();
        }
      }
    }
    if (locationURL == null) {
      org.hl7.fhir.r4.model.Location location = FhirR4PatientHome.getPatientHome();
      BundleEntryComponent bec = newEntry(bundle, location, location.getId());
      locationURL = bec.getFullUrl();
    }
    return locationURL;
  }

  /**
   * Find the Location entry in this bundle for the given provider, and return the
   * "fullUrl" attribute.
   *
   * @param provider A given provider.
   * @param bundle The current bundle being generated.
   * @return Location.fullUrl if found, otherwise null.
   */
  private static String findLocationUrl(Provider provider, Bundle bundle) {
    if (provider == null) {
      return null;
    }
    for (BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource().fhirType().equals("Location")) {
        org.hl7.fhir.r4.model.Location location =
            (org.hl7.fhir.r4.model.Location) entry.getResource();
        Reference managingOrg = location.getManagingOrganization();
        if (managingOrg != null
            && managingOrg.hasIdentifier()
            && managingOrg.getIdentifier().hasValue()
            && managingOrg.getIdentifier().getValue().equals(provider.getResourceID())) {
          return entry.getFullUrl();
        }
      }
    }
    return null;
  }

  /**
   * Find the Practitioner entry in this bundle, and return the associated "fullUrl"
   * attribute.
   * @param clinician A given clinician.
   * @param bundle The current bundle being generated.
   * @return Practitioner.fullUrl if found, otherwise null.
   */
  private static String findPractitioner(Clinician clinician, Bundle bundle) {
    for (BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource().fhirType().equals("Practitioner")) {
        Practitioner doc = (Practitioner) entry.getResource();
        if (doc.getIdentifierFirstRep().getValue().equals(clinician.npi)) {
          return entry.getFullUrl();
        }
      }
    }
    return null;
  }

  /**
   * Create an entry for the given Claim, which references a Medication.
   *
   * @param person         The person being prescribed medication
   * @param personEntry     Entry for the person
   * @param bundle          The Bundle to add to
   * @param encounterEntry  The current Encounter
   * @param encounter       The Encounter
   * @param claim           the Claim object
   * @param medicationEntry  The medication Entry
   * @param medicationCodeableConcept The medication CodeableConcept
   * @return the added Entry
   */
  private static BundleEntryComponent medicationClaim(
      Person person, BundleEntryComponent personEntry,
      Bundle bundle, BundleEntryComponent encounterEntry,
      Encounter encounter, Claim claim,
      BundleEntryComponent medicationEntry, CodeableConcept medicationCodeableConcept) {

    org.hl7.fhir.r4.model.Claim claimResource = new org.hl7.fhir.r4.model.Claim();
    org.hl7.fhir.r4.model.Encounter encounterResource =
        (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();

    claimResource.setStatus(ClaimStatus.ACTIVE);
    CodeableConcept type = new CodeableConcept();
    type.getCodingFirstRep()
      .setSystem("http://terminology.hl7.org/CodeSystem/claim-type")
      .setCode("pharmacy");
    claimResource.setType(type);
    claimResource.setUse(org.hl7.fhir.r4.model.Claim.Use.CLAIM);

    // Get the insurance info at the time that the encounter occurred.
    InsuranceComponent insuranceComponent = new InsuranceComponent();
    insuranceComponent.setSequence(1);
    insuranceComponent.setFocal(true);
    insuranceComponent.setCoverage(new Reference().setDisplay(claim.getPayer().getName()));
    claimResource.addInsurance(insuranceComponent);

    // duration of encounter
    claimResource.setBillablePeriod(encounterResource.getPeriod());
    claimResource.setCreated(encounterResource.getPeriod().getEnd());

    claimResource.setPatient(new Reference(personEntry.getFullUrl()));
    claimResource.setProvider(encounterResource.getServiceProvider());

    // set the required priority
    CodeableConcept priority = new CodeableConcept();
    priority.getCodingFirstRep()
      .setSystem("http://terminology.hl7.org/CodeSystem/processpriority")
      .setCode("normal");
    claimResource.setPriority(priority);

    // add item for medication
    claimResource.addItem(new ItemComponent(new PositiveIntType(1),
          medicationCodeableConcept)
        .addEncounter(new Reference(encounterEntry.getFullUrl())));

    // add prescription.
    claimResource.setPrescription(new Reference(medicationEntry.getFullUrl()));

    Money moneyResource = new Money();
    moneyResource.setValue(claim.getTotalClaimCost());
    moneyResource.setCurrency("USD");
    claimResource.setTotal(moneyResource);

    BundleEntryComponent medicationClaimEntry =
        newEntry(bundle, claimResource, claim.uuid.toString());

    explanationOfBenefit(personEntry, bundle, encounterEntry, person,
        medicationClaimEntry, encounter, claim);

    return medicationClaimEntry;
  }

  /**
   * Create an entry for the given Claim, associated to an Encounter.
   *
   * @param person         The patient having the encounter.
   * @param personEntry    Entry for the person
   * @param bundle         The Bundle to add to
   * @param encounterEntry Entry for the Encounter
   * @param encounter      The health record encounter
   * @return the added Entry
   */
  private static BundleEntryComponent encounterClaim(
      Person person, BundleEntryComponent personEntry,
      Bundle bundle, BundleEntryComponent encounterEntry, Encounter encounter) {
    org.hl7.fhir.r4.model.Claim claimResource = new org.hl7.fhir.r4.model.Claim();
    org.hl7.fhir.r4.model.Encounter encounterResource =
        (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();
    claimResource.setStatus(ClaimStatus.ACTIVE);
    CodeableConcept type = new CodeableConcept();
    type.getCodingFirstRep().setSystem("http://terminology.hl7.org/CodeSystem/claim-type");
    EncounterType encType = EncounterType.fromString(encounter.type);
    if (encType.code().equals(EncounterType.OUTPATIENT.code())) {
      type.getCodingFirstRep().setCode("professional");
    } else {
      type.getCodingFirstRep().setCode("institutional");
    }
    claimResource.setType(type);
    claimResource.setUse(org.hl7.fhir.r4.model.Claim.Use.CLAIM);

    InsuranceComponent insuranceComponent = new InsuranceComponent();
    insuranceComponent.setSequence(1);
    insuranceComponent.setFocal(true);
    insuranceComponent.setCoverage(new Reference()
        .setDisplay(encounter.claim.getPayer().getName()));
    claimResource.addInsurance(insuranceComponent);

    // duration of encounter
    claimResource.setBillablePeriod(encounterResource.getPeriod());
    claimResource.setCreated(encounterResource.getPeriod().getEnd());

    claimResource.setPatient(new Reference()
        .setReference(personEntry.getFullUrl())
        .setDisplay((String) person.attributes.get(Person.NAME)));
    claimResource.setProvider(encounterResource.getServiceProvider());
    if (USE_US_CORE_IG) {
      claimResource.setFacility(encounterResource.getLocationFirstRep().getLocation());
    }

    // set the required priority
    CodeableConcept priority = new CodeableConcept();
    priority.getCodingFirstRep()
      .setSystem("http://terminology.hl7.org/CodeSystem/processpriority")
      .setCode("normal");
    claimResource.setPriority(priority);

    // add item for encounter
    claimResource.addItem(new ItemComponent(new PositiveIntType(1),
          encounterResource.getTypeFirstRep())
        .addEncounter(new Reference(encounterEntry.getFullUrl())));

    int itemSequence = 2;
    int conditionSequence = 1;
    int procedureSequence = 1;
    int informationSequence = 1;

    for (Claim.ClaimEntry claimEntry : encounter.claim.items) {
      HealthRecord.Entry item = claimEntry.entry;
      if (Costs.hasCost(item)) {
        // update claimItems list
        Code primaryCode = item.codes.get(0);
        String system = ExportHelper.getSystemURI(primaryCode.system);
        ItemComponent claimItem = new ItemComponent(new PositiveIntType(itemSequence),
            mapCodeToCodeableConcept(primaryCode, system));

        // calculate the cost of the procedure
        Money moneyResource = new Money();
        moneyResource.setCurrency("USD");
        moneyResource.setValue(item.getCost());
        claimItem.setNet(moneyResource);
        claimResource.addItem(claimItem);

        if (item instanceof Procedure) {
          Type procedureReference = new Reference(item.fullUrl);
          ProcedureComponent claimProcedure = new ProcedureComponent(
              new PositiveIntType(procedureSequence), procedureReference);
          claimResource.addProcedure(claimProcedure);
          claimItem.addProcedureSequence(procedureSequence);
          procedureSequence++;
        } else {
          Reference informationReference = new Reference(item.fullUrl);
          SupportingInformationComponent informationComponent =
              new SupportingInformationComponent();
          informationComponent.setSequence(informationSequence);
          informationComponent.setValue(informationReference);
          CodeableConcept category = new CodeableConcept();
          category.getCodingFirstRep()
              .setSystem("http://terminology.hl7.org/CodeSystem/claiminformationcategory")
              .setCode("info");
          informationComponent.setCategory(category);
          claimResource.addSupportingInfo(informationComponent);
          claimItem.addInformationSequence(informationSequence);
          informationSequence++;
        }
      } else {
        // assume it's a Condition, we don't have a Condition class specifically
        // add diagnosisComponent to claim
        Reference diagnosisReference = new Reference(item.fullUrl);
        DiagnosisComponent diagnosisComponent =
            new DiagnosisComponent(
                new PositiveIntType(conditionSequence), diagnosisReference);
        claimResource.addDiagnosis(diagnosisComponent);

        // update claimItems with diagnosis
        ItemComponent diagnosisItem =
            new ItemComponent(new PositiveIntType(itemSequence),
                mapCodeToCodeableConcept(item.codes.get(0), SNOMED_URI));
        diagnosisItem.addDiagnosisSequence(conditionSequence);
        claimResource.addItem(diagnosisItem);

        conditionSequence++;
      }
      itemSequence++;
    }

    Money moneyResource = new Money();
    moneyResource.setCurrency("USD");
    moneyResource.setValue(encounter.claim.getTotalClaimCost());
    claimResource.setTotal(moneyResource);

    return newEntry(bundle, claimResource, encounter.claim.uuid.toString());
  }

  /**
   * Create an explanation of benefit resource for each claim, detailing insurance
   * information.
   *
   * @param personEntry Entry for the person
   * @param bundle The Bundle to add to
   * @param encounterEntry The current Encounter
   * @param claimEntry the Claim object
   * @param person the person the health record belongs to
   * @param encounter the current Encounter as an object
   * @param claim the Claim.
   * @return the added entry
   */
  private static BundleEntryComponent explanationOfBenefit(BundleEntryComponent personEntry,
                                           Bundle bundle, BundleEntryComponent encounterEntry,
                                           Person person, BundleEntryComponent claimEntry,
                                           Encounter encounter, Claim claim) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    eob.setStatus(org.hl7.fhir.r4.model.ExplanationOfBenefit.ExplanationOfBenefitStatus.ACTIVE);
    eob.setType(new CodeableConcept()
        .addCoding(new Coding()
            .setSystem("http://terminology.hl7.org/CodeSystem/claim-type")
            .setCode("professional")
            .setDisplay("Professional")));
    eob.setUse(Use.CLAIM);
    eob.setOutcome(RemittanceOutcome.COMPLETE);

    org.hl7.fhir.r4.model.Encounter encounterResource =
        (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();

    // according to CMS guidelines claims have 12 months to be
    // billed, so we set the billable period to 1 year after
    // services have ended (the encounter ends).
    Calendar cal = Calendar.getInstance();
    cal.setTime(encounterResource.getPeriod().getEnd());
    cal.add(Calendar.YEAR, 1);

    Period billablePeriod = new Period()
        .setStart(encounterResource
            .getPeriod()
            .getEnd())
        .setEnd(cal.getTime());
    eob.setBillablePeriod(billablePeriod);

    // cost is hardcoded to be USD in claim so this should be fine as well
    Money totalCost = new Money();
    totalCost.setCurrency("USD");
    totalCost.setValue(claim.getTotalClaimCost());
    TotalComponent total = eob.addTotal();
    total.setAmount(totalCost);
    Code submitted = new Code("http://terminology.hl7.org/CodeSystem/adjudication",
        "submitted", "Submitted Amount");
    total.setCategory(mapCodeToCodeableConcept(submitted,
        "http://terminology.hl7.org/CodeSystem/adjudication"));

    // Set References
    eob.setPatient(new Reference(personEntry.getFullUrl()));
    if (USE_US_CORE_IG) {
      eob.setFacility(encounterResource.getLocationFirstRep().getLocation());
    }

    ServiceRequest referral = (ServiceRequest) new ServiceRequest()
        .setStatus(ServiceRequest.ServiceRequestStatus.COMPLETED)
        .setIntent(ServiceRequest.ServiceRequestIntent.ORDER)
        .setSubject(new Reference(personEntry.getFullUrl()))
        .setId("referral");
    CodeableConcept primaryCareRole = new CodeableConcept().addCoding(new Coding()
        .setCode("primary")
        .setSystem("http://terminology.hl7.org/CodeSystem/claimcareteamrole")
        .setDisplay("Primary provider"));
    Reference providerReference = new Reference().setDisplay("Unknown");
    if (encounter.clinician != null) {
      Reference practitionerRef = clinicianReference(encounter.clinician, bundle);
      if (practitionerRef != null && practitionerRef.hasReference()) {
        providerReference = new Reference(practitionerRef.getReference())
            .setDisplay(practitionerRef.getDisplay());
      }
    } else if (encounter.provider != null) {
      String providerUrl = TRANSACTION_BUNDLE
          ? ExportHelper.buildFhirSearchUrl("Location",
                    encounter.provider.getResourceLocationID())
          : findProviderUrl(encounter.provider, bundle);
      if (providerUrl != null) {
        providerReference = new Reference(providerUrl);
      }
    }

    eob.setProvider(providerReference);
    eob.addCareTeam(new ExplanationOfBenefit.CareTeamComponent()
        .setSequence(1)
        .setProvider(providerReference)
        .setRole(primaryCareRole));
    referral.setRequester(providerReference);
    referral.addPerformer(providerReference);

    eob.addContained(referral);
    eob.setReferral(new Reference().setReference("#referral"));

    // TODO: Make Coverage separate resources for US Core 6 & 7?
    // Get the insurance info at the time that the encounter occurred.
    Payer payer = claim.getPayer();
    Coverage coverage = new Coverage();
    coverage.setId("coverage");
    coverage.setStatus(CoverageStatus.ACTIVE);
    coverage.setType(new CodeableConcept().setText(payer.getName()));
    coverage.setBeneficiary(new Reference(personEntry.getFullUrl()));
    coverage.addPayor(new Reference().setDisplay(payer.getName()));
    eob.addContained(coverage);
    ExplanationOfBenefit.InsuranceComponent insuranceComponent =
        new ExplanationOfBenefit.InsuranceComponent();
    insuranceComponent.setFocal(true);
    insuranceComponent.setCoverage(new Reference("#coverage").setDisplay(payer.getName()));
    eob.addInsurance(insuranceComponent);
    eob.setInsurer(new Reference().setDisplay(payer.getName()));

    org.hl7.fhir.r4.model.Claim claimResource =
        (org.hl7.fhir.r4.model.Claim) claimEntry.getResource();
    eob.addIdentifier()
        .setSystem("https://bluebutton.cms.gov/resources/variables/clm_id")
        .setValue(claimResource.getId());
    // Hardcoded group id
    eob.addIdentifier()
        .setSystem("https://bluebutton.cms.gov/resources/identifier/claim-group")
        .setValue("99999999999");
    eob.setClaim(new Reference().setReference(claimEntry.getFullUrl()));
    eob.setCreated(encounterResource.getPeriod().getEnd());
    eob.setType(claimResource.getType());

    List<ExplanationOfBenefit.DiagnosisComponent> eobDiag = new ArrayList<>();
    for (DiagnosisComponent claimDiagnosis : claimResource.getDiagnosis()) {
      ExplanationOfBenefit.DiagnosisComponent diagnosisComponent =
          new ExplanationOfBenefit.DiagnosisComponent();
      diagnosisComponent.setDiagnosis(claimDiagnosis.getDiagnosis());
      diagnosisComponent.getType().add(new CodeableConcept()
          .addCoding(new Coding()
              .setCode("principal")
              .setSystem("http://terminology.hl7.org/CodeSystem/ex-diagnosistype")));
      diagnosisComponent.setSequence(claimDiagnosis.getSequence());
      diagnosisComponent.setPackageCode(claimDiagnosis.getPackageCode());
      eobDiag.add(diagnosisComponent);
    }
    eob.setDiagnosis(eobDiag);

    List<ExplanationOfBenefit.ProcedureComponent> eobProc = new ArrayList<>();
    for (ProcedureComponent proc : claimResource.getProcedure()) {
      ExplanationOfBenefit.ProcedureComponent p = new ExplanationOfBenefit.ProcedureComponent();
      p.setDate(proc.getDate());
      p.setSequence(proc.getSequence());
      p.setProcedure(proc.getProcedure());
    }
    eob.setProcedure(eobProc);

    List<ExplanationOfBenefit.ItemComponent> eobItem = new ArrayList<>();
    double totalPayment = 0;
    // Get all the items info from the claim
    for (ItemComponent item : claimResource.getItem()) {
      ExplanationOfBenefit.ItemComponent itemComponent = new ExplanationOfBenefit.ItemComponent();
      itemComponent.setSequence(item.getSequence());
      itemComponent.setQuantity(item.getQuantity());
      itemComponent.setUnitPrice(item.getUnitPrice());
      itemComponent.setCareTeamSequence(item.getCareTeamSequence());
      itemComponent.setDiagnosisSequence(item.getDiagnosisSequence());
      itemComponent.setInformationSequence(item.getInformationSequence());
      itemComponent.setNet(item.getNet());
      itemComponent.setEncounter(item.getEncounter());
      itemComponent.setServiced(encounterResource.getPeriod());
      itemComponent.setCategory(new CodeableConcept().addCoding(new Coding()
          .setSystem("https://bluebutton.cms.gov/resources/variables/line_cms_type_srvc_cd")
          .setCode("1")
          .setDisplay("Medical care")));
      itemComponent.setProductOrService(item.getProductOrService());

      // Location of service, can use switch statement based on
      // encounter type
      String code;
      String display;
      CodeableConcept location = new CodeableConcept();
      EncounterType encounterType = EncounterType.fromString(encounter.type);
      switch (encounterType) {
        case AMBULATORY:
          code = "21";
          display = "Inpatient Hospital";
          break;
        case EMERGENCY:
          code = "20";
          display = "Urgent Care Facility";
          break;
        case INPATIENT:
          code = "21";
          display = "Inpatient Hospital";
          break;
        case URGENTCARE:
          code = "20";
          display = "Urgent Care Facility";
          break;
        case WELLNESS:
          code = "19";
          display = "Off Campus-Outpatient Hospital";
          break;
        default:
          code = "21";
          display = "Inpatient Hospital";
      }
      location.addCoding()
          .setCode(code)
          .setSystem("http://terminology.hl7.org/CodeSystem/ex-serviceplace")
          .setDisplay(display);
      itemComponent.setLocation(location);

      // Adjudication
      if (item.hasNet()) {

        // Assume that the patient has already paid deductible and
        // has 20/80 coinsurance
        ExplanationOfBenefit.AdjudicationComponent coinsuranceAmount =
            new ExplanationOfBenefit.AdjudicationComponent();
        coinsuranceAmount.getCategory()
            .getCoding()
            .add(new Coding()
                .setCode("https://bluebutton.cms.gov/resources/variables/line_coinsrnc_amt")
                .setSystem("https://bluebutton.cms.gov/resources/codesystem/adjudication")
                .setDisplay("Line Beneficiary Coinsurance Amount"));
        coinsuranceAmount.getAmount()
            .setValue(0.2 * item.getNet().getValue().doubleValue()) //20% coinsurance
            .setCurrency("USD");

        ExplanationOfBenefit.AdjudicationComponent lineProviderAmount =
            new ExplanationOfBenefit.AdjudicationComponent();
        lineProviderAmount.getCategory()
            .getCoding()
            .add(new Coding()
                .setCode("https://bluebutton.cms.gov/resources/variables/line_prvdr_pmt_amt")
                .setSystem("https://bluebutton.cms.gov/resources/codesystem/adjudication")
                .setDisplay("Line Provider Payment Amount"));
        lineProviderAmount.getAmount()
            .setValue(0.8 * item.getNet().getValue().doubleValue())
            .setCurrency("USD");

        // assume the allowed and submitted amounts are the same for now
        ExplanationOfBenefit.AdjudicationComponent submittedAmount =
            new ExplanationOfBenefit.AdjudicationComponent();
        submittedAmount.getCategory()
            .getCoding()
            .add(new Coding()
                .setCode("https://bluebutton.cms.gov/resources/variables/line_sbmtd_chrg_amt")
                .setSystem("https://bluebutton.cms.gov/resources/codesystem/adjudication")
                .setDisplay("Line Submitted Charge Amount"));
        submittedAmount.getAmount()
            .setValue(item.getNet().getValue())
            .setCurrency("USD");

        ExplanationOfBenefit.AdjudicationComponent allowedAmount =
            new ExplanationOfBenefit.AdjudicationComponent();
        allowedAmount.getCategory()
            .getCoding()
            .add(new Coding()
                .setCode("https://bluebutton.cms.gov/resources/variables/line_alowd_chrg_amt")
                .setSystem("https://bluebutton.cms.gov/resources/codesystem/adjudication")
                .setDisplay("Line Allowed Charge Amount"));
        allowedAmount.getAmount()
            .setValue(item.getNet().getValue())
            .setCurrency("USD");

        ExplanationOfBenefit.AdjudicationComponent indicatorCode =
            new ExplanationOfBenefit.AdjudicationComponent();
        indicatorCode.getCategory()
            .getCoding()
            .add(new Coding()
                .setCode("https://bluebutton.cms.gov/resources/variables/line_prcsg_ind_cd")
                .setSystem("https://bluebutton.cms.gov/resources/codesystem/adjudication")
                .setDisplay("Line Processing Indicator Code"));

        // assume deductible is 0
        ExplanationOfBenefit.AdjudicationComponent deductibleAmount =
            new ExplanationOfBenefit.AdjudicationComponent();
        deductibleAmount.getCategory()
            .getCoding()
            .add(new Coding()
                .setCode("https://bluebutton.cms.gov/resources/variables/line_bene_ptb_ddctbl_amt")
                .setSystem("https://bluebutton.cms.gov/resources/codesystem/adjudication")
                .setDisplay("Line Beneficiary Part B Deductible Amount"));
        deductibleAmount.getAmount()
            .setValue(0)
            .setCurrency("USD");

        List<ExplanationOfBenefit.AdjudicationComponent> adjudicationComponents = new ArrayList<>();
        adjudicationComponents.add(coinsuranceAmount);
        adjudicationComponents.add(lineProviderAmount);
        adjudicationComponents.add(submittedAmount);
        adjudicationComponents.add(allowedAmount);
        adjudicationComponents.add(deductibleAmount);
        adjudicationComponents.add(indicatorCode);

        itemComponent.setAdjudication(adjudicationComponents);
        // the total payment is what the insurance ends up paying
        totalPayment += 0.8 * item.getNet().getValue().doubleValue();
      }
      eobItem.add(itemComponent);
    }
    eob.setItem(eobItem);

    // This will throw a validation error no matter what.  The
    // payment section is required, and it requires a value.
    // The validator will complain that if there is a value, the payment
    // needs a code, but it will also complain if there is a code.
    // There is no way to resolve this error.
    Money payment = new Money();
    payment.setValue(totalPayment)
        .setCurrency("USD");
    eob.setPayment(new ExplanationOfBenefit.PaymentComponent()
        .setAmount(payment));

    String uuid = ExportHelper.buildUUID(person, claim.mainEntry.entry.start,
        "ExplanationOfBenefit for Claim" + claim.uuid);
    return newEntry(bundle, eob, uuid);
  }

  /**
   * Map the Condition into a FHIR Condition resource, and add it to the given Bundle.
   *
   * @param personEntry    The Entry for the Person
   * @param bundle         The Bundle to add to
   * @param encounterEntry The current Encounter entry
   * @param condition      The Condition
   * @return The added Entry
   */
  private static BundleEntryComponent condition(
          RandomNumberGenerator rand,
          BundleEntryComponent personEntry, Bundle bundle, BundleEntryComponent encounterEntry,
          HealthRecord.Entry condition) {
    Condition conditionResource = new Condition();

    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      if (useUSCore5() || useUSCore6() || useUSCore7()) {
        meta.addProfile(
            "http://hl7.org/fhir/us/core/StructureDefinition/us-core-condition-encounter-diagnosis");
      } else {
        meta.addProfile(
            "http://hl7.org/fhir/us/core/StructureDefinition/us-core-condition");
      }
      conditionResource.setMeta(meta);
      conditionResource.addCategory(new CodeableConcept().addCoding(new Coding(
          "http://terminology.hl7.org/CodeSystem/condition-category", "encounter-diagnosis",
          "Encounter Diagnosis")));
    }

    conditionResource.setSubject(new Reference(personEntry.getFullUrl()));
    conditionResource.setEncounter(new Reference(encounterEntry.getFullUrl()));

    Code code = condition.codes.get(0);
    CodeableConcept concept = mapCodeToCodeableConcept(code, SNOMED_URI);
    addTranslation("ICD10-CM", code, concept, rand);
    conditionResource.setCode(concept);

    CodeableConcept verification = new CodeableConcept();
    verification.getCodingFirstRep()
      .setCode("confirmed")
      .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status");
    conditionResource.setVerificationStatus(verification);

    CodeableConcept status = new CodeableConcept();
    status.getCodingFirstRep()
      .setCode("active")
      .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical");
    conditionResource.setClinicalStatus(status);

    conditionResource.setOnset(convertFhirDateTime(condition.start, true));
    conditionResource.setRecordedDate(new Date(condition.start));

    if (condition.stop != 0) {
      conditionResource.setAbatement(convertFhirDateTime(condition.stop, true));
      status.getCodingFirstRep().setCode("resolved");
    }

    BundleEntryComponent conditionEntry =
        newEntry(bundle, conditionResource, condition.uuid.toString());
    condition.fullUrl = conditionEntry.getFullUrl();
    return conditionEntry;
  }

  /**
   * Map the Condition into a FHIR AllergyIntolerance resource, and add it to the given Bundle.
   *
   * @param personEntry    The Entry for the Person
   * @param bundle         The Bundle to add to
   * @param encounterEntry The current Encounter entry
   * @param allergy        The Allergy Entry
   * @return The added Entry
   */
  private static BundleEntryComponent allergy(
          BundleEntryComponent personEntry, Bundle bundle, BundleEntryComponent encounterEntry,
          HealthRecord.Allergy allergy) {

    AllergyIntolerance allergyResource = new AllergyIntolerance();
    allergyResource.setRecordedDate(new Date(allergy.start));

    CodeableConcept status = new CodeableConcept();
    status.getCodingFirstRep()
      .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical");
    allergyResource.setClinicalStatus(status);

    if (allergy.stop == 0) {
      status.getCodingFirstRep().setCode("active");
    } else {
      status.getCodingFirstRep().setCode("inactive");
    }

    if (allergy.allergyType == null
        || allergy.allergyType.equalsIgnoreCase("allergy")) {
      allergyResource.setType(AllergyIntoleranceType.ALLERGY);
    } else {
      allergyResource.setType(AllergyIntoleranceType.INTOLERANCE);
    }
    AllergyIntoleranceCategory category = null;
    if (allergy.category != null) {
      switch (allergy.category) {
        case "food":
          category = AllergyIntoleranceCategory.FOOD;
          break;
        case "medication":
          category = AllergyIntoleranceCategory.MEDICATION;
          break;
        case "environment":
          category = AllergyIntoleranceCategory.ENVIRONMENT;
          break;
        case "biologic":
          category = AllergyIntoleranceCategory.BIOLOGIC;
          break;
        default:
          category = AllergyIntoleranceCategory.MEDICATION;
      }
    }
    allergyResource.addCategory(category);

    allergyResource.setCriticality(AllergyIntoleranceCriticality.LOW);

    CodeableConcept verification = new CodeableConcept();
    verification.getCodingFirstRep()
      .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-verification")
      .setCode("confirmed");
    allergyResource.setVerificationStatus(verification);

    allergyResource.setPatient(new Reference(personEntry.getFullUrl()));
    Code code = allergy.codes.get(0);
    allergyResource.setCode(mapCodeToCodeableConcept(code, SNOMED_URI));

    if (allergy.reactions != null) {
      List<Code> sortedReactions = new ArrayList<>(allergy.reactions.keySet());
      sortedReactions.sort((a,b) -> a.code.compareTo(b.code));
      sortedReactions.forEach(manifestation -> {
        AllergyIntolerance.AllergyIntoleranceReactionComponent reactionComponent =
            new AllergyIntolerance.AllergyIntoleranceReactionComponent();
        reactionComponent.addManifestation(mapCodeToCodeableConcept(manifestation, SNOMED_URI));
        HealthRecord.ReactionSeverity severity = allergy.reactions.get(manifestation);
        if (severity != null) {
          switch (severity) {
            case MILD:
              reactionComponent.setSeverity(AllergyIntolerance.AllergyIntoleranceSeverity.MILD);
              break;
            case MODERATE:
              reactionComponent.setSeverity(AllergyIntolerance.AllergyIntoleranceSeverity.MODERATE);
              break;
            case SEVERE:
              reactionComponent.setSeverity(AllergyIntolerance.AllergyIntoleranceSeverity.SEVERE);
              break;
            default:
              // do nothing
          }
        }
        allergyResource.addReaction(reactionComponent);
      });
    }

    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-allergyintolerance");
      allergyResource.setMeta(meta);
    }
    BundleEntryComponent allergyEntry = newEntry(bundle, allergyResource, allergy.uuid.toString());
    allergy.fullUrl = allergyEntry.getFullUrl();
    return allergyEntry;
  }


  /**
   * Map the given Observation into a FHIR Observation resource, and add it to the given Bundle.
   *
   * @param personEntry    The Person Entry
   * @param bundle         The Bundle to add to
   * @param encounterEntry The current Encounter entry
   * @param observation    The Observation
   * @return The added Entry
   */
  private static BundleEntryComponent observation(
          BundleEntryComponent personEntry, Bundle bundle, BundleEntryComponent encounterEntry,
          Observation observation) {
    org.hl7.fhir.r4.model.Observation observationResource =
        new org.hl7.fhir.r4.model.Observation();

    observationResource.setSubject(new Reference(personEntry.getFullUrl()));
    observationResource.setEncounter(new Reference(encounterEntry.getFullUrl()));

    observationResource.setStatus(ObservationStatus.FINAL);

    Code code = observation.codes.get(0);
    observationResource.setCode(mapCodeToCodeableConcept(code, LOINC_URI));
    // add extra codes, if there are any...
    if (observation.codes.size() > 1) {
      for (int i = 1; i < observation.codes.size(); i++) {
        code = observation.codes.get(i);
        Coding coding = new Coding();
        coding.setCode(code.code);
        coding.setDisplay(code.display);
        coding.setSystem(LOINC_URI);
        observationResource.getCode().addCoding(coding);
      }
    }

    // map the code to the official display, ex "vital-signs" --> "Vital Signs"
    // in all cases the text is the same just with these two differences- space/hyphen and caps
    // https://terminology.hl7.org/5.0.0/CodeSystem-observation-category.html
    String categoryDisplay = null;
    if (observation.category != null) {
      categoryDisplay = StringUtils.capitalize(observation.category.replace('-', ' '));
    }

    observationResource.addCategory().addCoding().setCode(observation.category)
        .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
        .setDisplay(categoryDisplay);

    if (observation.value != null) {
      Type value = mapValueToFHIRType(observation.value, observation.unit);
      observationResource.setValue(value);
    } else if (observation.observations != null && !observation.observations.isEmpty()) {
      // multi-observation (ex blood pressure)
      for (Observation subObs : observation.observations) {
        ObservationComponentComponent comp = new ObservationComponentComponent();
        comp.setCode(mapCodeToCodeableConcept(subObs.codes.get(0), LOINC_URI));
        Type value = mapValueToFHIRType(subObs.value, subObs.unit);
        comp.setValue(value);
        observationResource.addComponent(comp);
      }
    }

    observationResource.setEffective(convertFhirDateTime(observation.start, true));
    observationResource.setIssued(new Date(observation.start));

    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      // add the specific profile based on code
      String codeMappingUri = US_CORE_MAPPING.get(LOINC_URI, code.code);
      if (codeMappingUri != null) {
        meta.addProfile(codeMappingUri);
        if (!codeMappingUri.contains("/us/core/") && observation.category.equals("vital-signs")) {
          meta.addProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-vital-signs");
        }
      } else if (observation.report != null && observation.category.equals("laboratory")) {
        meta.addProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-lab");
      }


      if (observation.category != null) {
        if (useUSCore6() || useUSCore7()) {
          switch (observation.category) {
            case "imaging":
              meta.addProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-clinical-result");
              break;
            case "social-history":
              if (code.code.equals("82810-3")) {
                meta.addProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-pregnancystatus");
              } else {
                meta.addProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-simple-observation");
              }

              break;
            case "survey":
              meta.addProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-screening-assessment");
              break;
            case "exam":
              meta.addProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-clinical-result");
              break;
            case "laboratory":
              meta.addProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-lab");
              break;
            default:
              // do nothing
          }
        } else if (useUSCore5()) {
          switch (observation.category) {
            case "imaging":
              meta.addProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-imaging");
              break;
            case "social-history":
              meta.addProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-social-history");
              break;
            case "survey":
              meta.addProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-survey");
              // note that the -sdoh-assessment profile is a subset of -survey,
              // those are handled by code in US_CORE_MAPPING above
              break;
            case "exam":
              // this one is a little nebulous -- are all exams also clinical tests?
              meta.addProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-clinical-test");

              observationResource.addCategory().addCoding().setCode("clinical-test")
                  .setSystem("http://hl7.org/fhir/us/core/CodeSystem/us-core-observation-category")
                  .setDisplay("Clinical Test");
              break;
            default:
              // do nothing
          }
        }
      }

      if (meta.hasProfile()) {
        observationResource.setMeta(meta);
      }
    }

    BundleEntryComponent entry = newEntry(bundle, observationResource, observation.uuid.toString());
    observation.fullUrl = entry.getFullUrl();
    return entry;
  }

  static Type mapValueToFHIRType(Object value, String unit) {
    if (value == null) {
      return null;
    } else if (value instanceof Condition) {
      Code conditionCode = ((HealthRecord.Entry) value).codes.get(0);
      return mapCodeToCodeableConcept(conditionCode, SNOMED_URI);
    } else if (value instanceof Code) {
      return mapCodeToCodeableConcept((Code) value, SNOMED_URI);
    } else if (value instanceof String) {
      return new StringType((String) value);
    } else if (value instanceof Number) {
      double dblVal = ((Number) value).doubleValue();
      PlainBigDecimal bigVal = new PlainBigDecimal(dblVal);
      return new Quantity().setValue(bigVal)
          .setCode(unit).setSystem(UNITSOFMEASURE_URI)
          .setUnit(unit);
    } else if (value instanceof Components.SampledData) {
      return mapValueToSampledData((Components.SampledData) value, unit);
    } else if (value instanceof Boolean) {
      return new BooleanType((Boolean) value);
    } else {
      throw new IllegalArgumentException("unexpected observation value class: "
          + value.getClass().toString() + "; " + value);
    }
  }

  /**
   * Maps a Synthea internal SampledData object to the FHIR standard SampledData
   * representation.
   *
   * @param value Synthea internal SampledData instance
   * @param unit Observation unit value
   * @return
   */
  static org.hl7.fhir.r4.model.SampledData mapValueToSampledData(
      Components.SampledData value, String unit) {

    org.hl7.fhir.r4.model.SampledData recordData = new org.hl7.fhir.r4.model.SampledData();
    recordData.setOrigin(new Quantity().setValue(value.originValue)
        .setCode(unit).setSystem(UNITSOFMEASURE_URI)
        .setUnit(unit));

    // Use the period from the first series. They should all be the same.
    // FHIR output is milliseconds so we need to convert from TimeSeriesData seconds.
    recordData.setPeriod(value.series.get(0).getPeriod() * 1000);

    // Set optional fields if they were provided
    if (value.factor != null) {
      recordData.setFactor(value.factor);
    }
    if (value.lowerLimit != null) {
      recordData.setLowerLimit(value.lowerLimit);
    }
    if (value.upperLimit != null) {
      recordData.setUpperLimit(value.upperLimit);
    }

    recordData.setDimensions(value.series.size());

    recordData.setData(ExportHelper.sampledDataToValueString(value));

    return recordData;
  }

  /**
   * Map the given Procedure into a FHIR Procedure resource, and add it to the given Bundle.
   *
   * @param person         The Person
   * @param personEntry    The Person entry
   * @param bundle         Bundle to add to
   * @param encounterEntry The current Encounter entry
   * @param procedure      The Procedure
   * @return The added Entry
   */
  private static BundleEntryComponent procedure(Person person,
          BundleEntryComponent personEntry, Bundle bundle, BundleEntryComponent encounterEntry,
          Procedure procedure) {
    org.hl7.fhir.r4.model.Procedure procedureResource = new org.hl7.fhir.r4.model.Procedure();
    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-procedure");
      procedureResource.setMeta(meta);
    }
    procedureResource.setStatus(ProcedureStatus.COMPLETED);
    procedureResource.setSubject(new Reference(personEntry.getFullUrl()));
    procedureResource.setEncounter(new Reference(encounterEntry.getFullUrl()));
    if (USE_US_CORE_IG) {
      org.hl7.fhir.r4.model.Encounter encounterResource =
          (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();
      procedureResource.setLocation(encounterResource.getLocationFirstRep().getLocation());
    }

    Code code = procedure.codes.get(0);
    CodeableConcept procCode = mapCodeToCodeableConcept(code, SNOMED_URI);
    procedureResource.setCode(procCode);

    if (procedure.stop != 0L) {
      Date startDate = new Date(procedure.start);
      Date endDate = new Date(procedure.stop);
      procedureResource.setPerformed(new Period().setStart(startDate).setEnd(endDate));
    } else {
      procedureResource.setPerformed(convertFhirDateTime(procedure.start, true));
    }

    if (!procedure.reasons.isEmpty()) {
      Code reason = procedure.reasons.get(0); // Only one element in list

      BundleEntryComponent reasonCondition = findConditionResourceByCode(bundle, reason.code);
      if (reasonCondition != null) {
        procedureResource.addReasonReference()
          .setReference(reasonCondition.getFullUrl())
          .setDisplay(reason.display);
      } else {
        // we didn't find a matching Condition,
        // fallback to just reason code
        procedureResource.addReasonCode(mapCodeToCodeableConcept(reason, SNOMED_URI));
        addTranslation("ICD10-CM", reason, procedureResource.getReasonCodeFirstRep(), person);
      }
    }

    BundleEntryComponent procedureEntry =
        newEntry(bundle, procedureResource, procedure.uuid.toString());
    procedure.fullUrl = procedureEntry.getFullUrl();
    return procedureEntry;
  }

  /**
   * Map the HealthRecord.Device into a FHIR Device and add it to the Bundle.
   *
   * @param personEntry    The Person entry.
   * @param bundle         Bundle to add to.
   * @param device         The device to add.
   * @return The added Entry.
   */
  private static BundleEntryComponent device(
          BundleEntryComponent personEntry, Bundle bundle, HealthRecord.Device device) {
    Device deviceResource = new Device();
    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      meta.addProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-implantable-device");
      deviceResource.setMeta(meta);
    }
    deviceResource.addUdiCarrier()
        .setDeviceIdentifier(device.deviceIdentifier)
        .setCarrierHRF(device.udi);
    deviceResource.setStatus(FHIRDeviceStatus.ACTIVE);
    deviceResource.setDistinctIdentifier(device.deviceIdentifier);
    if (device.manufacturer != null) {
      deviceResource.setManufacturer(device.manufacturer);
    }
    if (device.model != null) {
      deviceResource.setModelNumber(device.model);
    }
    deviceResource.setManufactureDate(new Date(device.manufactureTime));
    deviceResource.setExpirationDate(new Date(device.expirationTime));
    deviceResource.setLotNumber(device.lotNumber);
    deviceResource.setSerialNumber(device.serialNumber);
    deviceResource.addDeviceName()
        .setName(device.codes.get(0).display)
        .setType(DeviceNameType.USERFRIENDLYNAME);
    deviceResource.setType(mapCodeToCodeableConcept(device.codes.get(0), SNOMED_URI));
    deviceResource.setPatient(new Reference(personEntry.getFullUrl()));
    return newEntry(bundle, deviceResource, device.uuid.toString());
  }

  /**
   * Map the JsonObject for a Supply into a FHIR SupplyDelivery and add it to the Bundle.
   *
   * @param personEntry    The Person entry.
   * @param bundle         Bundle to add to.
   * @param supply         The supplied object to add.
   * @param encounter      The encounter during which the supplies were delivered
   * @return The added Entry.
   */
  private static BundleEntryComponent supplyDelivery(
          BundleEntryComponent personEntry, Bundle bundle, HealthRecord.Supply supply,
          Encounter encounter) {

    SupplyDelivery supplyResource = new SupplyDelivery();
    supplyResource.setStatus(SupplyDeliveryStatus.COMPLETED);
    supplyResource.setPatient(new Reference(personEntry.getFullUrl()));

    CodeableConcept type = new CodeableConcept();
    type.addCoding()
      .setCode("device")
      .setDisplay("Device")
      .setSystem("http://terminology.hl7.org/CodeSystem/supply-item-type");
    supplyResource.setType(type);

    SupplyDeliverySuppliedItemComponent suppliedItem = new SupplyDeliverySuppliedItemComponent();
    suppliedItem.setItem(mapCodeToCodeableConcept(supply.codes.get(0), SNOMED_URI));
    suppliedItem.setQuantity(new Quantity(supply.quantity));

    supplyResource.setSuppliedItem(suppliedItem);

    supplyResource.setOccurrence(convertFhirDateTime(supply.start, true));

    return newEntry(bundle, supplyResource, supply.uuid.toString());
  }

  /**
   * Create a Provenance entry at the end of this Bundle that
   * targets all the entries in the Bundle.
   *
   * @param bundle The finished complete Bundle.
   * @param person The person.
   * @param stopTime The time the simulation stopped.
   * @return BundleEntryComponent containing a Provenance resource.
   */
  private static BundleEntryComponent provenance(Bundle bundle, Person person, long stopTime) {
    Provenance provenance = new Provenance();
    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-provenance");
      provenance.setMeta(meta);
    }
    for (BundleEntryComponent entry : bundle.getEntry()) {
      provenance.addTarget(new Reference(entry.getFullUrl()));
    }
    provenance.setRecorded(new Date(stopTime));

    // Provenance sources...
    int index = person.record.encounters.size() - 1;
    Clinician clinician = null;
    Provider providerOrganization = null;
    while (index >= 0 && (clinician == null || providerOrganization == null)) {
      clinician = person.record.encounters.get(index).clinician;
      providerOrganization = person.record.encounters.get(index).provider;
      index--;
    }

    if (clinician == null && providerOrganization == null) {
      providerOrganization = person.getProvider(EncounterType.WELLNESS, stopTime);
      clinician =
          providerOrganization.chooseClinicianList(ClinicianSpecialty.GENERAL_PRACTICE, person);
    } else if (clinician == null || providerOrganization == null) {
      if (clinician == null && providerOrganization != null) {
        clinician =
            providerOrganization.chooseClinicianList(ClinicianSpecialty.GENERAL_PRACTICE, person);
      } else if (clinician != null && providerOrganization == null) {
        providerOrganization = clinician.getOrganization();
        if (providerOrganization == null) {
          providerOrganization = person.getProvider(EncounterType.WELLNESS, stopTime);
        }
      }
    }

    if (clinician.getEncounterCount() == 0) {
      clinician.incrementEncounters();
    }
    if (providerOrganization.getUtilization().isEmpty()) {
      // If this provider has never been used, ensure they have at least one encounter
      // (encounter creating this Provenance record) so that the provider is exported.
      providerOrganization.incrementEncounters(EncounterType.VIRTUAL, Utilities.getYear(stopTime));
    }

    String clinicianDisplay = clinician.getFullname();

    Reference practitionerRef = clinicianReference(clinician, bundle);
    String practitionerFullUrl = practitionerRef != null
      ? practitionerRef.getReference()
      : ExportHelper.buildFhirNpiSearchUrl(clinician);

    String organizationFullUrl = TRANSACTION_BUNDLE
            ? ExportHelper.buildFhirSearchUrl("Organization",
                    providerOrganization.getResourceID())
            : findProviderUrl(providerOrganization, bundle);

    // Provenance Author...
    ProvenanceAgentComponent agent = provenance.addAgent();
    agent.setType(mapCodeToCodeableConcept(
        new Code("http://terminology.hl7.org/CodeSystem/provenance-participant-type",
            "author", "Author"), null));
    agent.setWho(new Reference()
        .setReference(practitionerFullUrl)
        .setDisplay(clinicianDisplay));
    agent.setOnBehalfOf(new Reference()
        .setReference(organizationFullUrl)
        .setDisplay(providerOrganization.name));

    // Provenance Transmitter...
    agent = provenance.addAgent();
    agent.setType(mapCodeToCodeableConcept(
        new Code("http://hl7.org/fhir/us/core/CodeSystem/us-core-provenance-participant-type",
            "transmitter", "Transmitter"), null));
    agent.setWho(new Reference()
        .setReference(practitionerFullUrl)
        .setDisplay(clinicianDisplay));
    agent.setOnBehalfOf(new Reference()
        .setReference(organizationFullUrl)
        .setDisplay(providerOrganization.name));

    // NOTE: this assumes only one Provenance per bundle.
    // If that assumption is ever not true, change the timestamp used and/or key here
    String uuid = ExportHelper.buildUUID(person, (long) person.attributes.get(Person.BIRTHDATE),
        "Provenance");
    return newEntry(bundle, provenance, uuid);
  }

  private static BundleEntryComponent immunization(
          BundleEntryComponent personEntry, Bundle bundle, BundleEntryComponent encounterEntry,
          HealthRecord.Entry immunization) {
    Immunization immResource = new Immunization();
    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-immunization");
      immResource.setMeta(meta);
    }

    immResource.setStatus(ImmunizationStatus.COMPLETED);
    immResource.setOccurrence(convertFhirDateTime(immunization.start, true));
    immResource.setVaccineCode(mapCodeToCodeableConcept(immunization.codes.get(0), CVX_URI));
    immResource.setPrimarySource(true);
    immResource.setPatient(new Reference(personEntry.getFullUrl()));
    immResource.setEncounter(new Reference(encounterEntry.getFullUrl()));
    if (USE_US_CORE_IG) {
      org.hl7.fhir.r4.model.Encounter encounterResource =
          (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();
      immResource.setLocation(encounterResource.getLocationFirstRep().getLocation());
    }

    BundleEntryComponent immunizationEntry =
        newEntry(bundle, immResource, immunization.uuid.toString());
    immunization.fullUrl = immunizationEntry.getFullUrl();

    return immunizationEntry;
  }

  /**
   * Map the given Medication to a FHIR MedicationRequest resource, and add it to the given Bundle.
   *
   * @param person         The person being prescribed medication
   * @param personEntry    The Entry for the Person
   * @param bundle         Bundle to add the Medication to
   * @param encounterEntry Current Encounter entry
   * @param encounter      The Encounter
   * @param medication     The Medication
   * @return The added Entry
   */
  private static BundleEntryComponent medicationRequest(
      Person person, BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, Encounter encounter, Medication medication) {
    MedicationRequest medicationResource = new MedicationRequest();
    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-medicationrequest");
      medicationResource.setMeta(meta);

      Code category = new Code("http://terminology.hl7.org/CodeSystem/medicationrequest-category",
          "community", "Community");
      medicationResource.addCategory(mapCodeToCodeableConcept(category, null));
    }

    medicationResource.setSubject(new Reference(personEntry.getFullUrl()));
    medicationResource.setEncounter(new Reference(encounterEntry.getFullUrl()));

    Code code = medication.codes.get(0);
    String system = code.system.equals("SNOMED-CT")
        ? SNOMED_URI
        : RXNORM_URI;
    CodeableConcept medicationCodeableConcept = mapCodeToCodeableConcept(code, system);
    medicationResource.setMedication(medicationCodeableConcept);

    if (USE_US_CORE_IG && medication.administration
        && shouldExport(org.hl7.fhir.r4.model.Medication.class)) {
      // Occasionally, rather than use medication codes, we want to use a Medication
      // Resource. We only want to do this when we use US Core, to make sure we
      // sometimes produce a resource for the us-core-medication profile, and the
      // 'administration' flag is an arbitrary way to decide without flipping a coin.
      org.hl7.fhir.r4.model.Medication drugResource =
          new org.hl7.fhir.r4.model.Medication();
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-medication");
      drugResource.setMeta(meta);
      drugResource.setCode(medicationCodeableConcept);
      drugResource.setStatus(MedicationStatus.ACTIVE);
      String drugUUID = ExportHelper.buildUUID(person, medication.start,
          "Medication Resource for " + medication.uuid);
      BundleEntryComponent drugEntry = newEntry(bundle, drugResource, drugUUID);
      medicationResource.setMedication(new Reference(drugEntry.getFullUrl()));

      // Set the MedicationRequest.category
      EncounterType type = EncounterType.fromString(encounter.type);
      if (type.code().equals(EncounterType.INPATIENT.code())) {
        CodeableConcept concept = medicationResource.getCategoryFirstRep();
        concept.setText("Inpatient");
        Coding category = concept.getCodingFirstRep();
        category.setCode("inpatient");
        category.setDisplay("Inpatient");
      } else if (type.code().equals(EncounterType.OUTPATIENT.code())) {
        CodeableConcept concept = medicationResource.getCategoryFirstRep();
        concept.setText("Outpatient");
        Coding category = concept.getCodingFirstRep();
        category.setCode("outpatient");
        category.setDisplay("Outpatient");
      }
    }

    medicationResource.setAuthoredOn(new Date(medication.start));
    medicationResource.setIntent(MedicationRequestIntent.ORDER);
    org.hl7.fhir.r4.model.Encounter encounterResource =
        (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();
    medicationResource.setRequester(encounterResource.getParticipantFirstRep().getIndividual());

    if (medication.stop != 0L) {
      medicationResource.setStatus(MedicationRequestStatus.COMPLETED);
    } else {
      medicationResource.setStatus(MedicationRequestStatus.ACTIVE);
    }

    if (!medication.reasons.isEmpty()) {
      // Only one element in list
      Code reason = medication.reasons.get(0);

      BundleEntryComponent reasonCondition = findConditionResourceByCode(bundle, reason.code);
      if (reasonCondition != null) {
        medicationResource.addReasonReference()
          .setReference(reasonCondition.getFullUrl())
          .setDisplay(reason.display);
      } else {
        // we didn't find a matching Condition,
        // fallback to just reason code
        medicationResource.addReasonCode(mapCodeToCodeableConcept(reason, SNOMED_URI));
        addTranslation("ICD10-CM", reason, medicationResource.getReasonCodeFirstRep(),
                person);
      }
    }

    if (medication.prescriptionDetails != null) {
      JsonObject rxInfo = medication.prescriptionDetails;
      Dosage dosage = new Dosage();

      dosage.setSequence(1);
      // as_needed is true if present
      dosage.setAsNeeded(new BooleanType(rxInfo.has("as_needed")));
      if (rxInfo.has("as_needed")) {
        dosage.setText("Take as needed.");
      }

      // as_needed is false
      if ((rxInfo.has("dosage")) && (!rxInfo.has("as_needed"))) {
        Timing timing = new Timing();
        TimingRepeatComponent timingRepeatComponent = new TimingRepeatComponent();
        timingRepeatComponent.setFrequency(
            rxInfo.get("dosage").getAsJsonObject().get("frequency").getAsInt());
        timingRepeatComponent.setPeriod(
            rxInfo.get("dosage").getAsJsonObject().get("period").getAsDouble());
        timingRepeatComponent.setPeriodUnit(
            convertUcumCode(rxInfo.get("dosage").getAsJsonObject().get("unit").getAsString()));
        timing.setRepeat(timingRepeatComponent);
        dosage.setTiming(timing);

        Quantity dose = new SimpleQuantity().setValue(
            rxInfo.get("dosage").getAsJsonObject().get("amount").getAsDouble());

        DosageDoseAndRateComponent dosageDetails = new DosageDoseAndRateComponent();
        dosageDetails.setType(new CodeableConcept().addCoding(
            new Coding().setCode(DoseRateType.ORDERED.toCode())
                .setSystem(DoseRateType.ORDERED.getSystem())
                .setDisplay(DoseRateType.ORDERED.getDisplay())));
        dosageDetails.setDose(dose);
        List<DosageDoseAndRateComponent> details = new ArrayList<DosageDoseAndRateComponent>();
        details.add(dosageDetails);
        dosage.setDoseAndRate(details);

        if (rxInfo.has("instructions")) {
          StringBuilder text = new StringBuilder();
          for (JsonElement instructionElement : rxInfo.get("instructions").getAsJsonArray()) {
            JsonObject instruction = instructionElement.getAsJsonObject();
            Code instructionCode = new Code(
                SNOMED_URI,
                instruction.get("code").getAsString(),
                instruction.get("display").getAsString()
            );
            text.append(instructionCode.display).append('\n');
            dosage.addAdditionalInstruction(mapCodeToCodeableConcept(instructionCode, SNOMED_URI));
          }
          if (text.length() > 0) {
            text.deleteCharAt(text.length() - 1); // delete the last newline char
            dosage.setText(text.toString());
          }
        }
      }

      List<Dosage> dosageInstruction = new ArrayList<Dosage>();
      dosageInstruction.add(dosage);
      medicationResource.setDosageInstruction(dosageInstruction);

    }

    BundleEntryComponent medicationEntry =
        newEntry(bundle, medicationResource, medication.uuid.toString());

    if (shouldExport(org.hl7.fhir.r4.model.Claim.class)) {
      // create new claim for medication
      medicationClaim(person, personEntry, bundle, encounterEntry, encounter,
          medication.claim, medicationEntry, medicationCodeableConcept);
    }

    // Create new administration for medication, if needed
    if (medication.administration && shouldExport(MedicationAdministration.class)) {
      medicationAdministration(person, personEntry, bundle, encounterEntry, medication,
              medicationResource);
    }

    return medicationEntry;
  }

  /**
   * Add a MedicationAdministration if needed for the given medication.
   *
   * @param person            The Person
   * @param personEntry       The Entry for the Person
   * @param bundle            Bundle to add the MedicationAdministration to
   * @param encounterEntry    Current Encounter entry
   * @param medication        The Medication
   * @param medicationRequest The related medicationRequest
   * @return The added Entry
   */
  private static BundleEntryComponent medicationAdministration(
      Person person, BundleEntryComponent personEntry, Bundle bundle,
          BundleEntryComponent encounterEntry, Medication medication,
          MedicationRequest medicationRequest) {

    MedicationAdministration medicationResource = new MedicationAdministration();

    medicationResource.setSubject(new Reference(personEntry.getFullUrl()));
    medicationResource.setContext(new Reference(encounterEntry.getFullUrl()));

    Code code = medication.codes.get(0);
    String system = code.system.equals("SNOMED-CT") ? SNOMED_URI : RXNORM_URI;

    medicationResource.setMedication(mapCodeToCodeableConcept(code, system));
    medicationResource.setEffective(new DateTimeType(new Date(medication.start)));

    medicationResource.setStatus(MedicationAdministration.MedicationAdministrationStatus.COMPLETED);

    if (medication.prescriptionDetails != null) {
      JsonObject rxInfo = medication.prescriptionDetails;
      MedicationAdministrationDosageComponent dosage =
          new MedicationAdministrationDosageComponent();

      // as_needed is false
      if ((rxInfo.has("dosage")) && (!rxInfo.has("as_needed"))) {
        Quantity dose = new SimpleQuantity()
            .setValue(rxInfo.get("dosage").getAsJsonObject().get("amount").getAsDouble());
        dosage.setDose((SimpleQuantity) dose);

        if (rxInfo.has("instructions")) {
          for (JsonElement instructionElement : rxInfo.get("instructions").getAsJsonArray()) {
            JsonObject instruction = instructionElement.getAsJsonObject();

            dosage.setText(instruction.get("display").getAsString());
          }
        }
      }
      if (rxInfo.has("refills")) {
        SimpleQuantity rate = new SimpleQuantity();
        rate.setValue(rxInfo.get("refills").getAsLong());
        dosage.setRate(rate);
      }
      medicationResource.setDosage(dosage);
    }

    if (!medication.reasons.isEmpty()) {
      // Only one element in list
      Code reason = medication.reasons.get(0);

      BundleEntryComponent reasonCondition = findConditionResourceByCode(bundle, reason.code);
      if (reasonCondition != null) {
        medicationResource.addReasonReference()
          .setReference(reasonCondition.getFullUrl())
          .setDisplay(reason.display);
      } else {
        // we didn't find a matching Condition,
        // fallback to just reason code
        medicationResource.addReasonCode(mapCodeToCodeableConcept(reason, SNOMED_URI));
        addTranslation("ICD10-CM", reason, medicationResource.getReasonCodeFirstRep(),
                person);
      }
    }

    String medicationAdminUUID = ExportHelper.buildUUID(person, medication.start,
        "MedicationAdministration for " + medication.uuid);
    BundleEntryComponent medicationAdminEntry =
        newEntry(bundle, medicationResource, medicationAdminUUID);
    return medicationAdminEntry;
  }

  /**
   * Map the given Report to a FHIR DiagnosticReport resource, and add it to the given Bundle.
   *
   * @param personEntry    The Entry for the Person
   * @param bundle         Bundle to add the Report to
   * @param encounterEntry Current Encounter entry
   * @param report         The Report
   * @return The added Entry
   */
  private static BundleEntryComponent report(
          BundleEntryComponent personEntry, Bundle bundle, BundleEntryComponent encounterEntry,
          Report report) {
    DiagnosticReport reportResource = new DiagnosticReport();
    boolean labsOnly = true;
    for (Observation observation : report.observations) {
      labsOnly = labsOnly && observation.category.equalsIgnoreCase("laboratory");
    }
    if (labsOnly && USE_US_CORE_IG) {
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-lab");
      reportResource.setMeta(meta);
      org.hl7.fhir.r4.model.Encounter encounterResource =
          (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();
      reportResource.addPerformer(encounterResource.getServiceProvider());
    }
    reportResource.setStatus(DiagnosticReportStatus.FINAL);
    if (labsOnly) {
      reportResource.addCategory(new CodeableConcept(
          new Coding("http://terminology.hl7.org/CodeSystem/v2-0074", "LAB", "Laboratory")));
    }
    reportResource.setCode(mapCodeToCodeableConcept(report.codes.get(0), LOINC_URI));
    reportResource.setSubject(new Reference(personEntry.getFullUrl()));
    reportResource.setEncounter(new Reference(encounterEntry.getFullUrl()));
    reportResource.setEffective(convertFhirDateTime(report.start, true));
    reportResource.setIssued(new Date(report.start));

    if (shouldExport(org.hl7.fhir.r4.model.Observation.class)) {
      // if observations are not exported, we can't reference them
      for (Observation observation : report.observations) {
        Reference reference = new Reference(observation.fullUrl);
        reference.setDisplay(observation.codes.get(0).display);
        reportResource.addResult(reference);
      }
    }

    return newEntry(bundle, reportResource, report.uuid.toString());
  }

  /**
   * Add a clinical note to the Bundle, which adds both a DocumentReference and a
   * DiagnosticReport.
   *
   * @param person         The Person
   * @param personEntry    The Entry for the Person
   * @param bundle         Bundle to add the Report to
   * @param encounterEntry Current Encounter entry
   * @param clinicalNoteText The plain text contents of the note.
   * @param currentNote If this is the most current note.
   * @return The entry for the DocumentReference.
   */
  private static void clinicalNote(Person person,
          BundleEntryComponent personEntry, Bundle bundle, BundleEntryComponent encounterEntry,
          String clinicalNoteText, boolean currentNote) {
    // We'll need the encounter...
    org.hl7.fhir.r4.model.Encounter encounter =
        (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();

    // Add a DiagnosticReport
    DiagnosticReport reportResource = new DiagnosticReport();
    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-note");
      reportResource.setMeta(meta);
    }
    reportResource.setStatus(DiagnosticReportStatus.FINAL);
    reportResource.addCategory(new CodeableConcept(
        new Coding(LOINC_URI, "34117-2", "History and physical note")));
    reportResource.getCategoryFirstRep().addCoding(
        new Coding(LOINC_URI, "51847-2", "Evaluation + Plan note"));
    reportResource.setCode(reportResource.getCategoryFirstRep());
    reportResource.setSubject(new Reference(personEntry.getFullUrl()));
    reportResource.setEncounter(new Reference(encounterEntry.getFullUrl()));
    reportResource.setEffective(encounter.getPeriod().getStartElement());
    reportResource.setIssued(encounter.getPeriod().getStart());
    if (encounter.hasParticipant()) {
      reportResource.addPerformer(encounter.getParticipantFirstRep().getIndividual());
    } else {
      reportResource.addPerformer(encounter.getServiceProvider());
    }
    reportResource.addPresentedForm()
        .setContentType("text/plain; charset=utf-8")
        .setData(clinicalNoteText.getBytes(java.nio.charset.StandardCharsets.UTF_8));

    // the note text might be exactly identical for multiple encounters,
    // so to ensure the UUID is unique use the encounter ID as the key
    // IMPORTANT: if this function is called more than once per encounter, change here and below!
    String reportUUID = ExportHelper.buildUUID(person, 0,
        "DiagnosticReport for note on encounter " + encounter.getId());
    newEntry(bundle, reportResource, reportUUID);

    if (shouldExport(DocumentReference.class)) {
      // Add a DocumentReference
      DocumentReference documentReference = new DocumentReference();
      if (USE_US_CORE_IG) {
        Meta meta = new Meta();
        meta.addProfile(
            "http://hl7.org/fhir/us/core/StructureDefinition/us-core-documentreference");
        documentReference.setMeta(meta);
      }
      if (currentNote) {
        documentReference.setStatus(DocumentReferenceStatus.CURRENT);
      } else {
        documentReference.setStatus(DocumentReferenceStatus.SUPERSEDED);
      }
      documentReference.addIdentifier()
        .setSystem("urn:ietf:rfc:3986")
        .setValue("urn:uuid:" + reportResource.getId());
      documentReference.setType(reportResource.getCategoryFirstRep());
      documentReference.addCategory(new CodeableConcept(
          new Coding("http://hl7.org/fhir/us/core/CodeSystem/us-core-documentreference-category",
              "clinical-note", "Clinical Note")));
      documentReference.setSubject(new Reference(personEntry.getFullUrl()));
      documentReference.setDate(encounter.getPeriod().getStart());
      documentReference.addAuthor(reportResource.getPerformerFirstRep());
      documentReference.setCustodian(encounter.getServiceProvider());
      documentReference.addContent()
          .setAttachment(reportResource.getPresentedFormFirstRep())
          .setFormat(
            new Coding("http://ihe.net/fhir/ihe.formatcode.fhir/CodeSystem/formatcode",
                "urn:ihe:iti:xds:2017:mimeTypeSufficient", "mimeType Sufficient"));
      documentReference.setContext(new DocumentReferenceContextComponent()
          .addEncounter(reportResource.getEncounter())
          .setPeriod(encounter.getPeriod()));

      String documentUUID = ExportHelper.buildUUID(person, 0,
          "DocumentReference for note on encounter " + encounter.getId());

      newEntry(bundle, documentReference, documentUUID);
    }
  }

  /**
   * Map the given CarePlan to a FHIR CarePlan resource, and add it to the given Bundle.
   *
   * @param person         The Person
   * @param personEntry    The Entry for the Person
   * @param bundle         Bundle to add the CarePlan to
   * @param encounterEntry Current Encounter entry
   * @param provider       The current provider
   * @param carePlan       The CarePlan to map to FHIR and add to the bundle
   * @return The added Entry
   */
  private static BundleEntryComponent carePlan(Person person,
          BundleEntryComponent personEntry, Bundle bundle, BundleEntryComponent encounterEntry,
          Provider provider, BundleEntryComponent careTeamEntry, CarePlan carePlan) {
    org.hl7.fhir.r4.model.CarePlan careplanResource = new org.hl7.fhir.r4.model.CarePlan();

    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-careplan");
      careplanResource.setMeta(meta);
      careplanResource.addCategory(mapCodeToCodeableConcept(
          new Code(CAREPLAN_CATEGORY_SYSTEM, "assess-plan",
              null), null));
    }

    String narrative = "Care Plan for ";
    careplanResource.setIntent(CarePlanIntent.ORDER);
    careplanResource.setSubject(new Reference(personEntry.getFullUrl()));
    careplanResource.setEncounter(new Reference(encounterEntry.getFullUrl()));
    if (careTeamEntry != null) {
      careplanResource.addCareTeam(new Reference(careTeamEntry.getFullUrl()));
    }

    Code code = carePlan.codes.get(0);
    careplanResource.addCategory(mapCodeToCodeableConcept(code, SNOMED_URI));
    narrative += code.display + ".";

    CarePlanActivityStatus activityStatus;
    CodeableConcept goalStatus = new CodeableConcept();
    goalStatus.getCodingFirstRep()
      .setSystem("http://terminology.hl7.org/CodeSystem/goal-achievement");

    Period period = new Period().setStart(new Date(carePlan.start));
    careplanResource.setPeriod(period);
    if (carePlan.stop != 0L) {
      period.setEnd(new Date(carePlan.stop));
      careplanResource.setStatus(CarePlanStatus.COMPLETED);
      activityStatus = CarePlanActivityStatus.COMPLETED;
      goalStatus.getCodingFirstRep().setCode("achieved");
    } else {
      careplanResource.setStatus(CarePlanStatus.ACTIVE);
      activityStatus = CarePlanActivityStatus.INPROGRESS;
      goalStatus.getCodingFirstRep().setCode("in-progress");
    }

    BundleEntryComponent reasonCondition = null;
    Code reason = null;
    if (!carePlan.reasons.isEmpty()) {
      // Only one element in list
      reason = carePlan.reasons.get(0);
      narrative += "<br/>Care plan is meant to treat " + reason.display + ".";

      reasonCondition = findConditionResourceByCode(bundle, reason.code);
      if (reasonCondition != null) {
        careplanResource.addAddresses().setReference(reasonCondition.getFullUrl());
      }
    }

    if (!carePlan.activities.isEmpty()) {
      narrative += "<br/>Activities: <ul>";
      String locationUrl = findLocationUrl(provider, bundle);

      for (Code activity : carePlan.activities) {
        narrative += "<li>" + code.display + "</li>";
        CarePlanActivityComponent activityComponent = new CarePlanActivityComponent();
        CarePlanActivityDetailComponent activityDetailComponent =
            new CarePlanActivityDetailComponent();

        activityDetailComponent.setStatus(activityStatus);
        activityDetailComponent.setLocation(new Reference()
            .setReference(locationUrl)
            .setDisplay(provider.name));

        activityDetailComponent.setCode(mapCodeToCodeableConcept(activity, SNOMED_URI));

        if (reasonCondition != null) {
          activityDetailComponent.addReasonReference().setReference(reasonCondition.getFullUrl());
        } else if (reason != null) {
          activityDetailComponent.addReasonCode(mapCodeToCodeableConcept(reason, SNOMED_URI));
          addTranslation("ICD10-CM", reason, activityDetailComponent.getReasonCodeFirstRep(),
                  person);
        }

        activityComponent.setDetail(activityDetailComponent);

        careplanResource.addActivity(activityComponent);
      }
      narrative += "</ul>";
    }


    for (JsonObject goal : carePlan.goals) {
      BundleEntryComponent goalEntry =
          careGoal(person, bundle, personEntry, carePlan.start, goalStatus, goal);
      careplanResource.addGoal().setReference(goalEntry.getFullUrl());
    }

    careplanResource.setText(new Narrative().setStatus(NarrativeStatus.GENERATED)
        .setDiv(new XhtmlNode(NodeType.Element).setValue(narrative)));

    return newEntry(bundle, careplanResource, carePlan.uuid.toString());
  }

  /**
   * Map the JsonObject into a FHIR Goal resource, and add it to the given Bundle.
   * @param person The Person
   * @param bundle The Bundle to add to
   * @param goalStatus The GoalStatus
   * @param goal The JsonObject
   * @return The added Entry
   */
  private static BundleEntryComponent careGoal(
      Person person, Bundle bundle,
      BundleEntryComponent personEntry, long carePlanStart,
      CodeableConcept goalStatus, JsonObject goal) {

    Goal goalResource = new Goal();
    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-goal");
      goalResource.setMeta(meta);
    }
    goalResource.setLifecycleStatus(GoalLifecycleStatus.ACCEPTED);
    goalResource.setAchievementStatus(goalStatus);
    goalResource.setSubject(new Reference(personEntry.getFullUrl()));

    if (goal.has("text")) {
      CodeableConcept descriptionCodeableConcept = new CodeableConcept();

      descriptionCodeableConcept.setText(goal.get("text").getAsString());
      goalResource.setDescription(descriptionCodeableConcept);
    } else if (goal.has("codes")) {
      CodeableConcept descriptionCodeableConcept = new CodeableConcept();

      JsonObject code =
          goal.get("codes").getAsJsonArray().get(0).getAsJsonObject();
      descriptionCodeableConcept.addCoding()
          .setSystem(LOINC_URI)
          .setCode(code.get("code").getAsString())
          .setDisplay(code.get("display").getAsString());

      descriptionCodeableConcept.setText(code.get("display").getAsString());
      goalResource.setDescription(descriptionCodeableConcept);
    } else if (goal.has("observation")) {
      CodeableConcept descriptionCodeableConcept = new CodeableConcept();

      // build up our own text from the observation condition, similar to the graphviz logic
      JsonObject logic = goal.get("observation").getAsJsonObject();

      String[] text = {
          logic.get("codes").getAsJsonArray().get(0)
              .getAsJsonObject().get("display").getAsString(),
          logic.get("operator").getAsString(),
          logic.get("value").getAsString()
      };

      descriptionCodeableConcept.setText(String.join(" ", text));
      goalResource.setDescription(descriptionCodeableConcept);
    }
    goalResource.addTarget().setMeasure(goalResource.getDescription())
        .setDue(new DateType(new Date(carePlanStart + Utilities.convertTime("days", 30))));

    if (goal.has("addresses")) {
      for (JsonElement reasonElement : goal.get("addresses").getAsJsonArray()) {
        if (reasonElement instanceof JsonObject) {
          JsonObject reasonObject = reasonElement.getAsJsonObject();
          String reasonCode =
              reasonObject.get("codes")
                  .getAsJsonObject()
                  .get("SNOMED-CT")
                  .getAsJsonArray()
                  .get(0)
                  .getAsString();

          BundleEntryComponent reasonCondition = findConditionResourceByCode(bundle, reasonCode);
          if (reasonCondition != null) {
            goalResource.addAddresses().setReference(reasonCondition.getFullUrl());
          }
        }
      }
    }

    // note: this ID logic assumes the person will not have 2 careplans
    // that start at the same timestep with the same description
    String resourceID = ExportHelper.buildUUID(person, carePlanStart,
        "CareGoal for " + goalResource.getDescription());

    return newEntry(bundle, goalResource, resourceID);
  }

  /**
   * Map the given CarePlan to a FHIR CareTeam resource, and add it to the given Bundle.
   *
   * @param person         The Person
   * @param personEntry    The Entry for the Person
   * @param bundle         Bundle to add the CarePlan to
   * @param encounterEntry Current Encounter entry
   * @param carePlan       The CarePlan to map to FHIR and add to the bundle
   * @return The added Entry
   */
  private static BundleEntryComponent careTeam(Person person,
          BundleEntryComponent personEntry, Bundle bundle, BundleEntryComponent encounterEntry,
          CarePlan carePlan) {

    CareTeam careTeam = new CareTeam();

    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-careteam");
      careTeam.setMeta(meta);
    }

    Period period = new Period().setStart(new Date(carePlan.start));
    careTeam.setPeriod(period);
    if (carePlan.stop != 0L) {
      period.setEnd(new Date(carePlan.stop));
      careTeam.setStatus(CareTeamStatus.INACTIVE);
    } else {
      careTeam.setStatus(CareTeamStatus.ACTIVE);
    }
    careTeam.setSubject(new Reference(personEntry.getFullUrl()));
    careTeam.setEncounter(new Reference(encounterEntry.getFullUrl()));

    if (carePlan.reasons != null && !carePlan.reasons.isEmpty()) {
      for (Code code : carePlan.reasons) {
        CodeableConcept concept = mapCodeToCodeableConcept(code, SNOMED_URI);
        addTranslation("ICD10-CM", code, concept, person);
        careTeam.addReasonCode(concept);
      }
    }

    // The first participant is the patient...
    CareTeamParticipantComponent participant = careTeam.addParticipant();
    participant.addRole(mapCodeToCodeableConcept(
        new Code(
            SNOMED_URI,
            "116154003",
            "Patient"),
        SNOMED_URI));
    Patient patient = (Patient) personEntry.getResource();
    participant.setMember(new Reference()
        .setReference(personEntry.getFullUrl())
        .setDisplay(patient.getNameFirstRep().getNameAsSingleString()));

    org.hl7.fhir.r4.model.Encounter encounter =
        (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();
    // The second participant is the practitioner...
    if (encounter.hasParticipant()) {
      participant = careTeam.addParticipant();
      participant.addRole(mapCodeToCodeableConcept(
          new Code(
              SNOMED_URI,
              "223366009",
              "Healthcare professional (occupation)"),
          SNOMED_URI));
      participant.setMember(encounter.getParticipantFirstRep().getIndividual());
    }

    // The last participant is the organization...
    participant = careTeam.addParticipant();
    participant.addRole(mapCodeToCodeableConcept(
        new Code(
            SNOMED_URI,
            "224891009",
            "Healthcare services (qualifier value)"),
        SNOMED_URI));
    participant.setMember(encounter.getServiceProvider());
    careTeam.addManagingOrganization(encounter.getServiceProvider());

    String careTeamUUID = ExportHelper.buildUUID(person, carePlan.start,
        "CareTeam for CarePlan " + carePlan.uuid);

    return newEntry(bundle, careTeam, careTeamUUID);
  }

  private static Identifier generateIdentifier(String uid) {
    Identifier identifier = new Identifier();
    identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
    identifier.setSystem("urn:ietf:rfc:3986");
    identifier.setValue("urn:oid:" + uid);
    return identifier;
  }

  private static String normalizePhoneForCountry(String phone) {
    if (phone == null) {
      return null;
    }
    if (!NORMALIZE_PHONE_FOR_COUNTRY) {
      return phone;
    }
    if ("US".equalsIgnoreCase(COUNTRY_CODE)) {
      return phone;
    }
    String trimmed = phone.trim();
    if (trimmed.startsWith("(555)") || trimmed.startsWith("555-")) {
      return NON_US_FALLBACK_PHONE;
    }
    return trimmed;
  }

  /**
   * Map the given ImagingStudy to a FHIR ImagingStudy resource, and add it to the given Bundle.
   *
   * @param personEntry    The Entry for the Person
   * @param bundle         Bundle to add the ImagingStudy to
   * @param encounterEntry Current Encounter entry
   * @param imagingStudy   The ImagingStudy to map to FHIR and add to the bundle
   * @return The added Entry
   */
  private static BundleEntryComponent imagingStudy(
          BundleEntryComponent personEntry, Bundle bundle, BundleEntryComponent encounterEntry,
          ImagingStudy imagingStudy) {
    org.hl7.fhir.r4.model.ImagingStudy imagingStudyResource =
        new org.hl7.fhir.r4.model.ImagingStudy();

    imagingStudyResource.addIdentifier(generateIdentifier(imagingStudy.dicomUid));
    imagingStudyResource.setStatus(ImagingStudyStatus.AVAILABLE);
    imagingStudyResource.setSubject(new Reference(personEntry.getFullUrl()));
    imagingStudyResource.setEncounter(new Reference(encounterEntry.getFullUrl()));
    if (USE_US_CORE_IG) {
      org.hl7.fhir.r4.model.Encounter encounterResource =
          (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();
      imagingStudyResource.setLocation(encounterResource.getLocationFirstRep().getLocation());
    }

    if (! imagingStudy.codes.isEmpty()) {
      imagingStudyResource.addProcedureCode(
              mapCodeToCodeableConcept(imagingStudy.codes.get(0), SNOMED_URI));
    }

    Date startDate = new Date(imagingStudy.start);
    imagingStudyResource.setStarted(startDate);

    // Convert the series into their FHIR equivalents
    int numberOfSeries = imagingStudy.series.size();
    imagingStudyResource.setNumberOfSeries(numberOfSeries);

    List<ImagingStudySeriesComponent> seriesResourceList =
        new ArrayList<ImagingStudySeriesComponent>();

    int totalNumberOfInstances = 0;
    int seriesNo = 1;

    for (ImagingStudy.Series series : imagingStudy.series) {
      ImagingStudySeriesComponent seriesResource = new ImagingStudySeriesComponent();

      seriesResource.setUid(series.dicomUid);
      seriesResource.setNumber(seriesNo);
      seriesResource.setStarted(startDate);

      CodeableConcept modalityConcept = mapCodeToCodeableConcept(series.modality, DICOM_DCM_URI);
      seriesResource.setModality(modalityConcept.getCoding().get(0));

      CodeableConcept bodySiteConcept = mapCodeToCodeableConcept(series.bodySite, SNOMED_URI);
      seriesResource.setBodySite(bodySiteConcept.getCoding().get(0));

      // Convert the images in each series into their FHIR equivalents
      int numberOfInstances = series.instances.size();
      seriesResource.setNumberOfInstances(numberOfInstances);
      totalNumberOfInstances += numberOfInstances;

      List<ImagingStudySeriesInstanceComponent> instanceResourceList =
          new ArrayList<ImagingStudySeriesInstanceComponent>();

      int instanceNo = 1;

      for (ImagingStudy.Instance instance : series.instances) {
        ImagingStudySeriesInstanceComponent instanceResource =
            new ImagingStudySeriesInstanceComponent();
        instanceResource.setUid(instance.dicomUid);
        instanceResource.setTitle(instance.title);
        instanceResource.setSopClass(new Coding()
            .setCode("urn:oid:" + instance.sopClass.code)
            .setSystem("urn:ietf:rfc:3986"));
        instanceResource.setNumber(instanceNo);

        instanceResourceList.add(instanceResource);
        instanceNo += 1;
      }

      seriesResource.setInstance(instanceResourceList);
      seriesResourceList.add(seriesResource);
      seriesNo += 1;
    }

    imagingStudyResource.setSeries(seriesResourceList);
    imagingStudyResource.setNumberOfInstances(totalNumberOfInstances);
    return newEntry(bundle, imagingStudyResource, imagingStudy.uuid.toString());
  }

  /**
   * Map the given Observation with attachment element to a FHIR Media resource, and add it to the
   * given Bundle.
   *
   * @param personEntry    The Entry for the Person
   * @param bundle         Bundle to add the Media to
   * @param encounterEntry Current Encounter entry
   * @param obs   The Observation to map to FHIR and add to the bundle
   * @return The added Entry
   */
  private static BundleEntryComponent media(
          BundleEntryComponent personEntry, Bundle bundle, BundleEntryComponent encounterEntry,
          Observation obs) {
    org.hl7.fhir.r4.model.Media mediaResource =
        new org.hl7.fhir.r4.model.Media();

    // Hard code as Image since we don't anticipate using video or audio any time soon
    Code mediaType = new Code("http://terminology.hl7.org/CodeSystem/media-type", "image", "Image");

    if (obs.codes != null && obs.codes.size() > 0) {
      List<CodeableConcept> reasonList = obs.codes.stream()
          .map(code -> mapCodeToCodeableConcept(code, SNOMED_URI)).collect(Collectors.toList());
      mediaResource.setReasonCode(reasonList);
    }
    mediaResource.setType(mapCodeToCodeableConcept(mediaType, MEDIA_TYPE_URI));
    mediaResource.setStatus(MediaStatus.COMPLETED);
    mediaResource.setSubject(new Reference(personEntry.getFullUrl()));
    mediaResource.setEncounter(new Reference(encounterEntry.getFullUrl()));

    Attachment content = (Attachment) obs.value;
    org.hl7.fhir.r4.model.Attachment contentResource = new org.hl7.fhir.r4.model.Attachment();

    contentResource.setContentType(content.contentType);
    contentResource.setLanguage(content.language);
    if (content.data != null) {
      contentResource.setDataElement(new org.hl7.fhir.r4.model.Base64BinaryType(content.data));
    } else {
      contentResource.setSize(content.size);
    }
    contentResource.setUrl(content.url);
    contentResource.setTitle(content.title);
    if (content.hash != null) {
      contentResource.setHashElement(new org.hl7.fhir.r4.model.Base64BinaryType(content.hash));
    }

    mediaResource.setWidth(content.width);
    mediaResource.setHeight(content.height);

    mediaResource.setContent(contentResource);

    return newEntry(bundle, mediaResource, obs.uuid.toString());
  }

  /**
   * Map the Provider into a FHIR Organization resource, and add it to the given Bundle.
   *
   * @param bundle   The Bundle to add to
   * @param provider The Provider
   * @return The added Entry
   */
  protected static BundleEntryComponent provider(Bundle bundle,
          Provider provider) {
    Organization organizationResource = new Organization();
    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-organization");
      organizationResource.setMeta(meta);
    }

    List<CodeableConcept> organizationType = new ArrayList<CodeableConcept>();
    organizationType.add(
        mapCodeToCodeableConcept(
            new Code(
                "http://terminology.hl7.org/CodeSystem/organization-type",
                "prov",
                "Healthcare Provider"),
            "http://terminology.hl7.org/CodeSystem/organization-type")
    );

    organizationResource.addIdentifier().setSystem(SYNTHEA_IDENTIFIER)
        .setValue((String) provider.getResourceID());
    organizationResource.setActive(true);
    organizationResource.setId(provider.getResourceID());
    organizationResource.setName(provider.name);
    organizationResource.setType(organizationType);

    Address address = new Address()
        .addLine(provider.address)
        .setCity(provider.city)
        .setPostalCode(provider.zip)
        .setState(provider.state);
    if (COUNTRY_CODE != null) {
      address.setCountry(COUNTRY_CODE);
    }
    organizationResource.addAddress(address);

    if (provider.phone != null && !provider.phone.isEmpty()) {
      ContactPoint contactPoint = new ContactPoint()
          .setSystem(ContactPointSystem.PHONE)
          .setValue(provider.phone);
      organizationResource.addTelecom(contactPoint);
    } else if (USE_US_CORE_IG) {
      ContactPoint contactPoint = new ContactPoint()
          .setSystem(ContactPointSystem.PHONE)
          .setValue("(555) 555-5555");
      organizationResource.addTelecom(contactPoint);
    }

    org.hl7.fhir.r4.model.Location location = null;
    if (USE_US_CORE_IG) {
      location = providerLocation(bundle, provider);
    }

    BundleEntryComponent entry = newEntry(bundle, organizationResource, provider.uuid);
    // add location to bundle *after* organization to ensure no forward reference
    if (location != null) {
      newEntry(bundle, location, provider.getResourceLocationID());
    }

    return entry;
  }

  /**
   * Map the Provider into a FHIR Location resource, and add it to the given Bundle.
   *
   * @param bundle   The Bundle to add to
   * @param provider The Provider
   * @return The added Entry or null if the bundle already contains this provider location
   */
  protected static org.hl7.fhir.r4.model.Location providerLocation(
          Bundle bundle, Provider provider) {
    org.hl7.fhir.r4.model.Location location = new org.hl7.fhir.r4.model.Location();
    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-location");
      location.setMeta(meta);
    }
    location.setStatus(LocationStatus.ACTIVE);
    location.setName(provider.name);
    // set telecom
    if (provider.phone != null && !provider.phone.isEmpty()) {
      ContactPoint contactPoint = new ContactPoint()
          .setSystem(ContactPointSystem.PHONE)
          .setValue(provider.phone);
      location.addTelecom(contactPoint);
    } else if (USE_US_CORE_IG) {
      ContactPoint contactPoint = new ContactPoint()
          .setSystem(ContactPointSystem.PHONE)
          .setValue("(555) 555-5555");
      location.addTelecom(contactPoint);
    }
    // set address
    Address address = new Address()
        .addLine(provider.address)
        .setCity(provider.city)
        .setPostalCode(provider.zip)
        .setState(provider.state);
    if (COUNTRY_CODE != null) {
      address.setCountry(COUNTRY_CODE);
    }
    location.setAddress(address);
    LocationPositionComponent position = new LocationPositionComponent();
    position.setLatitude(provider.getY());
    position.setLongitude(provider.getX());
    location.setPosition(position);
    location.addIdentifier()
        .setSystem(SYNTHEA_IDENTIFIER)
        .setValue(provider.getResourceLocationID());
    Identifier organizationIdentifier = new Identifier()
        .setSystem(SYNTHEA_IDENTIFIER)
        .setValue(provider.getResourceID());
    location.setManagingOrganization(new Reference()
        .setIdentifier(organizationIdentifier)
        .setDisplay(provider.name));
    return location;
  }

  /**
   * Map the clinician into a FHIR Practitioner resource, and add it to the given Bundle.
   * @param bundle The Bundle to add to
   * @param clinician The clinician
   * @return The added Entry
   */
  protected static BundleEntryComponent practitioner(Bundle bundle,
          Clinician clinician) {
    Practitioner practitionerResource = new Practitioner();
    if (USE_US_CORE_IG) {
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-practitioner");
      practitionerResource.setMeta(meta);
    }
    practitionerResource.addIdentifier()
            .setSystem("http://hl7.org/fhir/sid/us-npi")
            .setValue(clinician.npi);
    practitionerResource.setActive(true);
    practitionerResource.addName().setFamily(
        (String) clinician.attributes.get(Clinician.LAST_NAME))
      .addGiven((String) clinician.attributes.get(Clinician.FIRST_NAME))
      .addPrefix((String) clinician.attributes.get(Clinician.NAME_PREFIX));
    String email = (String) clinician.attributes.get(Clinician.FIRST_NAME)
        + "." + (String) clinician.attributes.get(Clinician.LAST_NAME)
        + "@example.com";
    practitionerResource.addTelecom()
        .setSystem(ContactPointSystem.EMAIL)
        .setUse(ContactPointUse.WORK)
        .setValue(email);
    if (USE_US_CORE_IG) {
      practitionerResource.getTelecomFirstRep().addExtension()
          .setUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-direct")
          .setValue(new BooleanType(true));
    }
    Address address = new Address()
        .addLine((String) clinician.attributes.get(Clinician.ADDRESS))
        .setCity((String) clinician.attributes.get(Clinician.CITY))
        .setPostalCode((String) clinician.attributes.get(Clinician.ZIP))
        .setState((String) clinician.attributes.get(Clinician.STATE));
    if (COUNTRY_CODE != null) {
      address.setCountry(COUNTRY_CODE);
    }
    practitionerResource.addAddress(address);

    if (clinician.attributes.get(Person.GENDER).equals("M")) {
      practitionerResource.setGender(AdministrativeGender.MALE);
    } else if (clinician.attributes.get(Person.GENDER).equals("F")) {
      practitionerResource.setGender(AdministrativeGender.FEMALE);
    }
    BundleEntryComponent practitionerEntry =
        newEntry(bundle, practitionerResource, clinician.getResourceID());

    if (USE_US_CORE_IG) {
      // generate an accompanying PractitionerRole resource
      PractitionerRole practitionerRole = new PractitionerRole();
      Meta meta = new Meta();
      meta.addProfile(
          "http://hl7.org/fhir/us/core/StructureDefinition/us-core-practitionerrole");
      practitionerRole.setMeta(meta);
      practitionerRole.setPractitioner(new Reference()
          .setIdentifier(new Identifier()
                  .setSystem("http://hl7.org/fhir/sid/us-npi")
                  .setValue(clinician.npi))
          .setDisplay(practitionerResource.getNameFirstRep().getNameAsSingleString()));
      practitionerRole.setOrganization(new Reference()
          .setIdentifier(new Identifier()
                  .setSystem(SYNTHEA_IDENTIFIER)
                  .setValue(clinician.getOrganization().getResourceID()))
          .setDisplay(clinician.getOrganization().name));
      practitionerRole.addCode(
          mapCodeToCodeableConcept(
              new Code("http://nucc.org/provider-taxonomy", "208D00000X", "General Practice Physician"),
              null));
      practitionerRole.addSpecialty(
          mapCodeToCodeableConcept(
              new Code("http://nucc.org/provider-taxonomy", "208D00000X", "General Practice Physician"),
              null));
      practitionerRole.addLocation()
          .setIdentifier(new Identifier()
                  .setSystem(SYNTHEA_IDENTIFIER)
                  .setValue(clinician.getOrganization().getResourceLocationID()))
          .setDisplay(clinician.getOrganization().name);
      if (clinician.getOrganization().phone != null
          && !clinician.getOrganization().phone.isEmpty()) {
        practitionerRole.addTelecom(new ContactPoint()
            .setSystem(ContactPointSystem.PHONE)
            .setValue(clinician.getOrganization().phone));
      }
      practitionerRole.addTelecom(practitionerResource.getTelecomFirstRep());

      // clinicians do not have any associated "individual seed" or "timestamp"
      // so we'll just re-use the uuid bits
      UUID origUUID = UUID.fromString(clinician.uuid);
      String uuid = ExportHelper.buildUUID(origUUID.getLeastSignificantBits(),
          origUUID.getMostSignificantBits(),
          "PractitionerRole for Clinician " + origUUID);

      newEntry(bundle, practitionerRole, uuid);
    }

    return practitionerEntry;
  }

  /**
   * Convert the unit into a UnitsOfTime.
   *
   * @param unit unit String
   * @return a UnitsOfTime representing the given unit
   */
  private static UnitsOfTime convertUcumCode(String unit) {
    // From: http://hl7.org/fhir/ValueSet/units-of-time
    switch (unit) {
      case "seconds":
        return UnitsOfTime.S;
      case "minutes":
        return UnitsOfTime.MIN;
      case "hours":
        return UnitsOfTime.H;
      case "days":
        return UnitsOfTime.D;
      case "weeks":
        return UnitsOfTime.WK;
      case "months":
        return UnitsOfTime.MO;
      case "years":
        return UnitsOfTime.A;
      default:
        return null;
    }
  }

  /**
   * Convert the timestamp into a FHIR DateType or DateTimeType.
   *
   * @param datetime Timestamp
   * @param time     If true, return a DateTime; if false, return a Date.
   * @return a DateType or DateTimeType representing the given timestamp
   */
  private static Type convertFhirDateTime(long datetime, boolean time) {
    Date date = new Date(datetime);

    if (time) {
      return new DateTimeType(date);
    } else {
      return new DateType(date);
    }
  }

  /**
   * Helper function to convert a Code into a CodeableConcept. Takes an optional system, which
   * replaces the Code.system in the resulting CodeableConcept if not null.
   *
   * @param from   The Code to create a CodeableConcept from.
   * @param system The system identifier, such as a URI. Optional; may be null.
   * @return The converted CodeableConcept
   */
  public static CodeableConcept mapCodeToCodeableConcept(Code from, String system) {
    CodeableConcept to = new CodeableConcept();
    system = system == null ? null : ExportHelper.getSystemURI(system);
    from.system = ExportHelper.getSystemURI(from.system);

    if (from.display != null) {
      to.setText(from.display);
    }

    Coding coding = new Coding();
    coding.setCode(from.code);
    coding.setDisplay(from.display);
    if (from.system == null) {
      coding.setSystem(system);
    } else {
      coding.setSystem(from.system);
    }
    coding.setVersion(from.version); // may be null

    to.addCoding(coding);

    return to;
  }

  // ========================================================================================
  // Patient Journey / Scheduling Resources
  // Schedule, Slot, Appointment, ServiceRequest (standalone), Task
  //
  // Realistic appointment lifecycle simulation:
  //   ~80% fulfilled  - patient attended
  //   ~8%  cancelled  - cancelled and rescheduled 1-14 days later
  //   ~5%  noshow     - patient did not attend
  //   ~5%  booked     - future appointment (not yet attended)
  //   ~2%  waitlisted - on a waiting list
  //
  // Rich participant model:
  //   - Primary performer (PPRF): the encounter clinician / physician
  //   - Scheduler (ATND): a nurse or admin staff from the same provider
  //   - Referring practitioner (REF): the patient's wellness/PCP clinician
  //     (for non-wellness encounters only)
  //   - Location: the encounter location
  //   - PractitionerRole: emitted with the clinician's ACTUAL specialty
  // ========================================================================================

  /** Probability thresholds for appointment lifecycle statuses (cumulative). */
  private static final double APPT_PROB_FULFILLED = 0.80;
  private static final double APPT_PROB_CANCELLED = 0.88;  // 8% cancelled
  private static final double APPT_PROB_NOSHOW    = 0.93;   // 5% noshow
  private static final double APPT_PROB_BOOKED    = 0.98;   // 5% booked (future)
  // remaining 2% = waitlisted

  /** Cancellation reason codes (FHIR appointment-cancellation-reason). */
  private static final String CANCEL_REASON_SYSTEM =
      "http://terminology.hl7.org/CodeSystem/appointment-cancellation-reason";

  // ========================================================================================
  // Location Hierarchy: Hospital → Department → Ward → Room → Bed
  //
  // Generates a realistic tree of Location resources using partOf references.
  // Department is determined from encounter type and clinician specialty.
  // Ward/Room/Bed are deterministically assigned per encounter.
  // ========================================================================================

  /** Department definitions: name → (SNOMED code, display) */
  private static final Map<String, String[]> DEPARTMENTS = new LinkedHashMap<>();
  static {
    DEPARTMENTS.put("Innere Medizin", new String[]{"309904001", "Internal medicine"});
    DEPARTMENTS.put("Chirurgie", new String[]{"394609007", "General surgery"});
    DEPARTMENTS.put("Gynäkologie", new String[]{"394586005", "Gynecology"});
    DEPARTMENTS.put("Pädiatrie", new String[]{"394537008", "Pediatric medicine"});
    DEPARTMENTS.put("Neurologie", new String[]{"394591006", "Neurology"});
    DEPARTMENTS.put("Kardiologie", new String[]{"394579002", "Cardiology"});
    DEPARTMENTS.put("Orthopädie", new String[]{"394801008", "Orthopedic surgery"});
    DEPARTMENTS.put("Urologie", new String[]{"394612005", "Urology"});
    DEPARTMENTS.put("Radiologie", new String[]{"394914008", "Radiology"});
    DEPARTMENTS.put("Anästhesie", new String[]{"394577000", "Anesthesiology"});
    DEPARTMENTS.put("Notaufnahme", new String[]{"225728007", "Emergency department"});
    DEPARTMENTS.put("Intensivstation", new String[]{"309905000", "Intensive care unit"});
    DEPARTMENTS.put("Allgemeinmedizin", new String[]{"394802001", "General medicine"});
    DEPARTMENTS.put("Onkologie", new String[]{"394593009", "Oncology"});
    DEPARTMENTS.put("Dermatologie", new String[]{"394582007", "Dermatology"});
    DEPARTMENTS.put("HNO", new String[]{"394649004", "Otorhinolaryngology"});
    DEPARTMENTS.put("Augenheilkunde", new String[]{"394594003", "Ophthalmology"});
    DEPARTMENTS.put("Psychiatrie", new String[]{"394587001", "Psychiatry"});
  }

  /** Map clinician specialty to department name. */
  private static final Map<String, String> SPECIALTY_TO_DEPT = new HashMap<>();
  static {
    SPECIALTY_TO_DEPT.put("INTERNAL MEDICINE", "Innere Medizin");
    SPECIALTY_TO_DEPT.put("GENERAL SURGERY", "Chirurgie");
    SPECIALTY_TO_DEPT.put("OBSTETRICS/GYNECOLOGY", "Gynäkologie");
    SPECIALTY_TO_DEPT.put("GYNECOLOGICAL ONCOLOGY", "Gynäkologie");
    SPECIALTY_TO_DEPT.put("PEDIATRIC MEDICINE", "Pädiatrie");
    SPECIALTY_TO_DEPT.put("NEUROLOGY", "Neurologie");
    SPECIALTY_TO_DEPT.put("CARDIOVASCULAR DISEASE (CARDIOLOGY)", "Kardiologie");
    SPECIALTY_TO_DEPT.put("CARDIAC SURGERY", "Chirurgie");
    SPECIALTY_TO_DEPT.put("CARDIAC ELECTROPHYSIOLOGY", "Kardiologie");
    SPECIALTY_TO_DEPT.put("INTERVENTIONAL CARDIOLOGY", "Kardiologie");
    SPECIALTY_TO_DEPT.put("ORTHOPEDIC SURGERY", "Orthopädie");
    SPECIALTY_TO_DEPT.put("UROLOGY", "Urologie");
    SPECIALTY_TO_DEPT.put("DIAGNOSTIC RADIOLOGY", "Radiologie");
    SPECIALTY_TO_DEPT.put("INTERVENTIONAL RADIOLOGY", "Radiologie");
    SPECIALTY_TO_DEPT.put("ANESTHESIOLOGY", "Anästhesie");
    SPECIALTY_TO_DEPT.put("EMERGENCY MEDICINE", "Notaufnahme");
    SPECIALTY_TO_DEPT.put("CRITICAL CARE (INTENSIVISTS)", "Intensivstation");
    SPECIALTY_TO_DEPT.put("GENERAL PRACTICE", "Allgemeinmedizin");
    SPECIALTY_TO_DEPT.put("FAMILY PRACTICE", "Allgemeinmedizin");
    SPECIALTY_TO_DEPT.put("HEMATOLOGY/ONCOLOGY", "Onkologie");
    SPECIALTY_TO_DEPT.put("MEDICAL ONCOLOGY", "Onkologie");
    SPECIALTY_TO_DEPT.put("DERMATOLOGY", "Dermatologie");
    SPECIALTY_TO_DEPT.put("OTOLARYNGOLOGY", "HNO");
    SPECIALTY_TO_DEPT.put("OPHTHALMOLOGY", "Augenheilkunde");
    SPECIALTY_TO_DEPT.put("PSYCHIATRY", "Psychiatrie");
    SPECIALTY_TO_DEPT.put("GASTROENTEROLOGY", "Innere Medizin");
    SPECIALTY_TO_DEPT.put("ENDOCRINOLOGY", "Innere Medizin");
    SPECIALTY_TO_DEPT.put("PULMONARY DISEASE", "Innere Medizin");
    SPECIALTY_TO_DEPT.put("NEPHROLOGY", "Innere Medizin");
    SPECIALTY_TO_DEPT.put("INFECTIOUS DISEASE", "Innere Medizin");
    SPECIALTY_TO_DEPT.put("RHEUMATOLOGY", "Innere Medizin");
    SPECIALTY_TO_DEPT.put("NURSE PRACTITIONER", "Allgemeinmedizin");
    SPECIALTY_TO_DEPT.put("PHYSICIAN ASSISTANT", "Allgemeinmedizin");
  }

  /**
   * Determine the appropriate department name for an encounter based on
   * encounter type, clinician specialty, and clinical context.
   *
   * @param encounter the encounter
   * @return department name (key into DEPARTMENTS map)
   */
  private static String determineDepartment(Encounter encounter) {
    EncounterType encType = EncounterType.fromString(encounter.type);

    // Emergency always goes to Notaufnahme
    if (encType == EncounterType.EMERGENCY) {
      return "Notaufnahme";
    }

    // If there's a clinician with a known specialty, map it
    if (encounter.clinician != null) {
      String specialty = (String) encounter.clinician.attributes.get(Clinician.SPECIALTY);
      if (specialty != null) {
        String dept = SPECIALTY_TO_DEPT.get(specialty);
        if (dept != null) {
          return dept;
        }
      }
    }

    // Encounter-type-based fallback
    if (encType == EncounterType.INPATIENT) {
      // Check if procedures indicate surgery
      for (Procedure proc : encounter.procedures) {
        String code = proc.codes.isEmpty() ? "" : proc.codes.get(0).display;
        if (code.toLowerCase().contains("surg") || code.toLowerCase().contains("operation")
            || code.toLowerCase().contains("repair") || code.toLowerCase().contains("ectomy")
            || code.toLowerCase().contains("otomy")) {
          return "Chirurgie";
        }
      }
      return "Innere Medizin";
    }

    // Wellness / ambulatory
    return "Allgemeinmedizin";
  }

  /** Track which department/ward/room/bed locations have been created per provider. */
  private static final ThreadLocal<Map<String, BundleEntryComponent>> locationCache =
      ThreadLocal.withInitial(HashMap::new);

  /**
   * Clear the location cache. Called at the start of each patient export via convertToFHIR.
   */
  private static void clearLocationCache() {
    locationCache.get().clear();
  }

  /**
   * Create or retrieve a department Location resource with the full hierarchy:
   * Hospital → Department → Ward → Room → (Bed for inpatient).
   *
   * @param bundle       The Bundle to add to
   * @param provider     The hospital/provider
   * @param encounter    The encounter (determines department, room, bed)
   * @param person       The person (for deterministic assignments)
   * @return The most specific Location entry (bed for inpatient, room otherwise)
   */
  private static BundleEntryComponent encounterLocation(Bundle bundle, Provider provider,
      Encounter encounter, Person person) {

    Map<String, BundleEntryComponent> cache = locationCache.get();

    String deptName = determineDepartment(encounter);
    String provId = provider.getResourceID();

    // --- Department level ---
    String deptKey = provId + ":dept:" + deptName;
    BundleEntryComponent deptEntry = cache.get(deptKey);
    if (deptEntry == null) {
      org.hl7.fhir.r4.model.Location dept = new org.hl7.fhir.r4.model.Location();
      dept.setStatus(LocationStatus.ACTIVE);
      dept.setName(deptName);
      dept.setMode(org.hl7.fhir.r4.model.Location.LocationMode.INSTANCE);
      String[] snomedCode = DEPARTMENTS.getOrDefault(deptName,
          new String[]{"394802001", "General medicine"});
      dept.addType(mapCodeToCodeableConcept(
          new Code(SNOMED_URI, snomedCode[0], snomedCode[1]), SNOMED_URI));
      dept.setPhysicalType(new CodeableConcept().addCoding(new Coding(
          "http://terminology.hl7.org/CodeSystem/location-physical-type", "wi", "Wing")));
      dept.setDescription(deptName + " - " + provider.name);
      // partOf → hospital
      String provLocId = provider.getResourceLocationID();
      if (TRANSACTION_BUNDLE) {
        dept.setPartOf(new Reference(
            ExportHelper.buildFhirSearchUrl("Location", provLocId)));
      } else {
        String hospLocUrl = findLocationUrl(provider, bundle);
        if (hospLocUrl != null) {
          dept.setPartOf(new Reference(hospLocUrl));
        }
      }
      dept.setManagingOrganization(new Reference()
          .setIdentifier(new Identifier()
              .setSystem(SYNTHEA_IDENTIFIER).setValue(provId))
          .setDisplay(provider.name));
      // Phone from provider
      if (provider.phone != null && !provider.phone.isEmpty()) {
        dept.addTelecom(new ContactPoint()
            .setSystem(ContactPointSystem.PHONE).setValue(provider.phone));
      }

      String deptLocId = UUID.nameUUIDFromBytes(
          (provId + "-dept-" + deptName).getBytes()).toString();
      deptEntry = newEntry(bundle, dept, deptLocId);
      cache.put(deptKey, deptEntry);
    }

    // --- Ward/Station level ---
    // Deterministic ward number from encounter UUID
    int wardNum = 1 + Math.abs(encounter.uuid.hashCode() % 3); // 3 wards per dept
    String wardName = deptName + " Station " + wardNum;
    String wardKey = provId + ":ward:" + deptName + ":" + wardNum;
    BundleEntryComponent wardEntry = cache.get(wardKey);
    if (wardEntry == null) {
      org.hl7.fhir.r4.model.Location ward = new org.hl7.fhir.r4.model.Location();
      ward.setStatus(LocationStatus.ACTIVE);
      ward.setName(wardName);
      ward.setMode(org.hl7.fhir.r4.model.Location.LocationMode.INSTANCE);
      ward.setPhysicalType(new CodeableConcept().addCoding(new Coding(
          "http://terminology.hl7.org/CodeSystem/location-physical-type", "wa", "Ward")));
      ward.setPartOf(new Reference(deptEntry.getFullUrl()));
      ward.setDescription(wardName + " - " + provider.name);

      String wardLocId = UUID.nameUUIDFromBytes(
          (provId + "-ward-" + deptName + "-" + wardNum).getBytes()).toString();
      wardEntry = newEntry(bundle, ward, wardLocId);
      cache.put(wardKey, wardEntry);
    }

    // --- Room level ---
    EncounterType encType = EncounterType.fromString(encounter.type);
    boolean hasProcedure = !encounter.procedures.isEmpty();
    int roomNum;
    String roomPrefix;

    if (hasProcedure && (encType == EncounterType.INPATIENT
        || encType == EncounterType.AMBULATORY || encType == EncounterType.OUTPATIENT)) {
      // Operating room
      roomNum = 1 + Math.abs((encounter.uuid.hashCode() * 7) % 4); // OP 1-4
      roomPrefix = "OP-Saal";
    } else if (encType == EncounterType.INPATIENT || encType == EncounterType.SNF
        || encType == EncounterType.HOSPICE) {
      // Patient room
      roomNum = 100 + wardNum * 100 + Math.abs((encounter.uuid.hashCode() * 13) % 20);
      roomPrefix = "Zimmer";
    } else {
      // Exam/treatment room
      roomNum = 1 + Math.abs((encounter.uuid.hashCode() * 11) % 8); // rooms 1-8
      roomPrefix = "Behandlungsraum";
    }

    String roomName = roomPrefix + " " + roomNum;
    String roomKey = provId + ":room:" + deptName + ":" + wardNum + ":" + roomName;
    BundleEntryComponent roomEntry = cache.get(roomKey);
    if (roomEntry == null) {
      org.hl7.fhir.r4.model.Location room = new org.hl7.fhir.r4.model.Location();
      room.setStatus(LocationStatus.ACTIVE);
      room.setName(roomName);
      room.setMode(org.hl7.fhir.r4.model.Location.LocationMode.INSTANCE);
      room.setPhysicalType(new CodeableConcept().addCoding(new Coding(
          "http://terminology.hl7.org/CodeSystem/location-physical-type", "ro", "Room")));
      room.setPartOf(new Reference(wardEntry.getFullUrl()));
      room.setDescription(roomName + " - " + wardName + " - " + provider.name);

      String roomLocId = UUID.nameUUIDFromBytes(
          (provId + "-room-" + deptName + "-" + wardNum + "-" + roomName).getBytes()).toString();
      roomEntry = newEntry(bundle, room, roomLocId);
      cache.put(roomKey, roomEntry);
    }

    // --- Bed level (inpatient/SNF/hospice only) ---
    if (encType == EncounterType.INPATIENT || encType == EncounterType.SNF
        || encType == EncounterType.HOSPICE) {
      int bedNum = 1 + Math.abs((encounter.uuid.hashCode() * 17) % 4); // 4 beds per room
      String bedName = "Bett " + roomNum + "-" + bedNum;
      String bedKey = provId + ":bed:" + deptName + ":" + wardNum + ":" + roomName + ":" + bedNum;
      BundleEntryComponent bedEntry = cache.get(bedKey);
      if (bedEntry == null) {
        org.hl7.fhir.r4.model.Location bed = new org.hl7.fhir.r4.model.Location();
        bed.setStatus(LocationStatus.ACTIVE);
        bed.setName(bedName);
        bed.setMode(org.hl7.fhir.r4.model.Location.LocationMode.INSTANCE);
        bed.setPhysicalType(new CodeableConcept().addCoding(new Coding(
            "http://terminology.hl7.org/CodeSystem/location-physical-type", "bd", "Bed")));
        bed.setPartOf(new Reference(roomEntry.getFullUrl()));
        bed.setDescription(bedName + " - " + roomName + " - " + wardName);
        // For the bed, we can set operationalStatus (occupied/unoccupied)
        // based on whether the encounter is still active
        bed.setOperationalStatus(new Coding(
            "http://terminology.hl7.org/CodeSystem/v2-0116",
            "O", "Occupied"));

        String bedLocId = UUID.nameUUIDFromBytes(
            (provId + "-bed-" + deptName + "-" + wardNum + "-" + roomName
                + "-" + bedNum).getBytes()).toString();
        bedEntry = newEntry(bundle, bed, bedLocId);
        cache.put(bedKey, bedEntry);
      }
      return bedEntry;
    }

    return roomEntry;
  }

  // ========================================================================================
  // CommunicationRequest: appointment reminders, medication reminders
  // ========================================================================================

  /**
   * Create a CommunicationRequest for an appointment reminder.
   * Sent 1 day before the appointment.
   *
   * @param personEntry   Patient entry
   * @param bundle        Bundle
   * @param apptEntry     The Appointment entry
   * @param encounter     The encounter
   * @return The created entry
   */
  private static BundleEntryComponent appointmentReminder(BundleEntryComponent personEntry,
      Bundle bundle, BundleEntryComponent apptEntry, Encounter encounter) {

    CommunicationRequest commReq = new CommunicationRequest();
    commReq.setStatus(CommunicationRequestStatus.COMPLETED);
    commReq.setPriority(CommunicationPriority.ROUTINE);

    // Category: notification
    commReq.addCategory(new CodeableConcept().addCoding(new Coding(
        "http://terminology.hl7.org/CodeSystem/communication-category",
        "notification", "Notification")));

    // Medium: phone or SMS
    commReq.addMedium(new CodeableConcept().addCoding(new Coding(
      "http://terminology.hl7.org/CodeSystem/v3-ParticipationMode",
      "SMSWRIT", "SMS")));

    commReq.setSubject(new Reference(personEntry.getFullUrl()));
    commReq.addAbout(new Reference(apptEntry.getFullUrl()));

    // Occurrence: 1 day before appointment
    long reminderTime = encounter.start - 24L * 60 * 60 * 1000;
    commReq.setOccurrence(new DateTimeType(new Date(reminderTime)));
    commReq.setAuthoredOn(new Date(reminderTime));

    // Payload: reminder text
    CodeableConcept serviceType = encounterServiceType(encounter);
    String serviceText = serviceType.hasText() ? serviceType.getText() : "Arzttermin";
    String reminderText = String.format(
        "Terminerinnerung: %s am %s. Bitte erscheinen Sie pünktlich.",
        serviceText,
        new java.text.SimpleDateFormat("dd.MM.yyyy 'um' HH:mm 'Uhr'")
            .format(new Date(encounter.start)));
    CommunicationRequest.CommunicationRequestPayloadComponent payload =
        commReq.addPayload();
    payload.setContent(new StringType(reminderText));

    // Recipient: the patient
    commReq.addRecipient(new Reference(personEntry.getFullUrl()));

    // Requester: the organization
    if (encounter.provider != null) {
      if (TRANSACTION_BUNDLE) {
        commReq.setRequester(new Reference(
            ExportHelper.buildFhirSearchUrl("Organization",
                encounter.provider.getResourceID())));
      } else {
        String provUrl = findProviderUrl(encounter.provider, bundle);
        if (provUrl != null) {
          commReq.setRequester(new Reference(provUrl));
        }
      }
    }

    String commId = UUID.nameUUIDFromBytes(
        ("comm-appt-reminder-" + encounter.uuid.toString()).getBytes()).toString();
    return newEntry(bundle, commReq, commId);
  }

  /**
   * Create a CommunicationRequest for a medication reminder.
   * Represents a scheduled reminder for the patient to take their medication.
   *
   * @param personEntry    Patient entry
   * @param bundle         Bundle
   * @param medReqEntry    The MedicationRequest entry
   * @param medication     The medication
   * @param encounter      The encounter where the medication was prescribed
   * @return The created entry
   */
  private static BundleEntryComponent medicationReminder(BundleEntryComponent personEntry,
      Bundle bundle, BundleEntryComponent medReqEntry,
      Medication medication, Encounter encounter) {

    CommunicationRequest commReq = new CommunicationRequest();
    commReq.setStatus(CommunicationRequestStatus.ACTIVE);
    commReq.setPriority(CommunicationPriority.ROUTINE);

    commReq.addCategory(new CodeableConcept().addCoding(new Coding(
        "http://terminology.hl7.org/CodeSystem/communication-category",
        "reminder", "Reminder")));

    commReq.addMedium(new CodeableConcept().addCoding(new Coding(
      "http://terminology.hl7.org/CodeSystem/v3-ParticipationMode",
      "SMSWRIT", "SMS")));

    commReq.setSubject(new Reference(personEntry.getFullUrl()));
    commReq.addAbout(new Reference(medReqEntry.getFullUrl()));

    // Timing: same as medication schedule
    Code medCode = medication.codes.get(0);
    String medName = medCode.display;

    String dosageText = "";
    if (medication.prescriptionDetails != null) {
      JsonObject rxInfo = medication.prescriptionDetails;
      if (rxInfo.has("dosage")) {
        double amount = rxInfo.get("dosage").getAsJsonObject().get("amount").getAsDouble();
        dosageText = " (" + amount + " Einheiten)";
      }
    }

    String reminderText = String.format(
        "Medikamentenerinnerung: Bitte nehmen Sie %s%s ein.",
        medName, dosageText);
    commReq.addPayload().setContent(new StringType(reminderText));

    commReq.addRecipient(new Reference(personEntry.getFullUrl()));
    commReq.setAuthoredOn(new Date(medication.start));

    // Occurrence window (R4 CommunicationRequest supports Period/dateTime)
    Period occurrencePeriod = new Period().setStart(new Date(medication.start));
    if (medication.stop != 0L) {
      occurrencePeriod.setEnd(new Date(medication.stop));
    }
    commReq.setOccurrence(occurrencePeriod);

    String commId = UUID.nameUUIDFromBytes(
        ("comm-med-reminder-" + medication.uuid.toString()).getBytes()).toString();
    return newEntry(bundle, commReq, commId);
  }

  // ========================================================================================
  // Enhanced MedicationAdministration with scheduled times
  // Generates individual administration events at specific times of day
  // ========================================================================================

  /**
   * Generate multiple MedicationAdministration resources representing
   * individual dose administrations at specific times (e.g. 08:00, 12:00, 20:00).
   * This provides data for the nurse medication overview.
   *
   * @param person        The Person
   * @param personEntry   Patient entry
   * @param bundle        Bundle
   * @param encounterEntry Encounter entry
   * @param medication    The Medication
   * @param medReqEntry   The MedicationRequest entry
   */
  private static void scheduledMedicationAdministrations(
      Person person, BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, Medication medication,
      BundleEntryComponent medReqEntry) {

    Code code = medication.codes.get(0);
    String system = code.system.equals("SNOMED-CT") ? SNOMED_URI : RXNORM_URI;
    CodeableConcept medConcept = mapCodeToCodeableConcept(code, system);

    // Determine dosage frequency from prescriptionDetails
    int timesPerDay = 1;
    double doseAmount = 1.0;
    String doseUnit = "dose";
    if (medication.prescriptionDetails != null) {
      JsonObject rxInfo = medication.prescriptionDetails;
      if (rxInfo.has("dosage")) {
        JsonObject dosage = rxInfo.get("dosage").getAsJsonObject();
        if (dosage.has("amount")) {
          doseAmount = dosage.get("amount").getAsDouble();
        }
        if (dosage.has("frequency")) {
          timesPerDay = dosage.get("frequency").getAsInt();
        }
        if (dosage.has("unit")) {
          doseUnit = dosage.get("unit").getAsString();
        }
      }
    }

    // Standard administration times based on frequency
    int[] adminHours;
    switch (timesPerDay) {
      case 1: adminHours = new int[]{8}; break;
      case 2: adminHours = new int[]{8, 20}; break;
      case 3: adminHours = new int[]{8, 12, 20}; break;
      case 4: adminHours = new int[]{6, 12, 18, 22}; break;
      default: adminHours = new int[]{8}; break;
    }

    // Generate administrations for the last 7 days of the medication period
    // (to avoid generating thousands for long-running medications)
    long medEnd = medication.stop != 0L ? medication.stop : person.record.encounters
        .get(person.record.encounters.size() - 1).start;
    long sevenDaysMs = 7L * 24 * 60 * 60 * 1000;
    long adminStart = Math.max(medication.start, medEnd - sevenDaysMs);

    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(adminStart);
    // Start at midnight
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    int adminCount = 0;
    int maxAdmins = 50; // cap per medication to prevent bundle bloat

    while (cal.getTimeInMillis() <= medEnd && adminCount < maxAdmins) {
      for (int hour : adminHours) {
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        long adminTime = cal.getTimeInMillis();
        if (adminTime < adminStart || adminTime > medEnd || adminCount >= maxAdmins) {
          continue;
        }

        MedicationAdministration admin = new MedicationAdministration();
        admin.setStatus(
            MedicationAdministration.MedicationAdministrationStatus.COMPLETED);
        admin.setSubject(new Reference(personEntry.getFullUrl()));
        admin.setContext(new Reference(encounterEntry.getFullUrl()));
        admin.setMedication(medConcept);
        admin.setEffective(new DateTimeType(new Date(adminTime)));

        // Link to the MedicationRequest
        admin.setRequest(new Reference(medReqEntry.getFullUrl()));

        // Dosage with specific amount
        MedicationAdministrationDosageComponent dosage =
            new MedicationAdministrationDosageComponent();
        dosage.setDose(new SimpleQuantity()
            .setValue(doseAmount)
            .setUnit(doseUnit)
            .setSystem(UNITSOFMEASURE_URI));
        dosage.setText(String.format("%.1f %s um %02d:00 Uhr", doseAmount, doseUnit, hour));
        admin.setDosage(dosage);

        // Performer (nurse)
        org.hl7.fhir.r4.model.Encounter encResource =
            (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();
        if (encResource.hasParticipant()) {
          MedicationAdministration.MedicationAdministrationPerformerComponent performer =
              admin.addPerformer();
          performer.setActor(encResource.getParticipantFirstRep().getIndividual());
        }

        String adminId = UUID.nameUUIDFromBytes(
            ("med-admin-" + medication.uuid + "-" + adminTime).getBytes()).toString();
        newEntry(bundle, admin, adminId);
        adminCount++;
      }
      // Advance to next day
      cal.add(Calendar.DAY_OF_MONTH, 1);
    }
  }

  // ========================================================================================
  // CarePlan enhancement: medication schedule activities with specific times
  // ========================================================================================

  /**
   * Create or enhance a CarePlan with medication schedule activities.
   * This shows "what meds are due when" for the nurse overview.
   *
   * @param personEntry   Patient entry
   * @param bundle        Bundle
   * @param encounterEntry Encounter entry
   * @param encounter     The encounter
   * @param medications   Active medications from this encounter
   * @return The CarePlan entry
   */
  private static BundleEntryComponent medicationCarePlan(
      BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, Encounter encounter,
      List<Medication> medications) {

    org.hl7.fhir.r4.model.CarePlan carePlan = new org.hl7.fhir.r4.model.CarePlan();
    carePlan.setStatus(CarePlanStatus.ACTIVE);
    carePlan.setIntent(CarePlanIntent.ORDER);
    carePlan.setSubject(new Reference(personEntry.getFullUrl()));
    carePlan.setEncounter(new Reference(encounterEntry.getFullUrl()));

    carePlan.addCategory(new CodeableConcept().addCoding(new Coding(
      CAREPLAN_CATEGORY_SYSTEM,
        "assess-plan", "Assessment and Plan of Treatment")));

    // Medication schedule category
    carePlan.addCategory(new CodeableConcept().addCoding(new Coding(
        SNOMED_URI, "430193006", "Medication management")));

    carePlan.setTitle("Medikationsplan");
    carePlan.setDescription("Medikationsplan für " + encounter.provider.name);
    carePlan.setPeriod(new Period()
        .setStart(new Date(encounter.start))
        .setEnd(encounter.stop != 0L ? new Date(encounter.stop) : null));

    for (Medication med : medications) {
      Code medCode = med.codes.get(0);
      CarePlanActivityComponent activity = carePlan.addActivity();

      CarePlanActivityDetailComponent detail = activity.getDetail();
      detail.setStatus(med.stop != 0L && med.stop < encounter.stop
          ? CarePlanActivityStatus.COMPLETED : CarePlanActivityStatus.INPROGRESS);
      detail.setKind(org.hl7.fhir.r4.model.CarePlan.CarePlanActivityKind.MEDICATIONREQUEST);
      detail.setCode(mapCodeToCodeableConcept(medCode,
          medCode.system.equals("SNOMED-CT") ? SNOMED_URI : RXNORM_URI));
      detail.setDescription(medCode.display);

      // Scheduled timing
      Timing timing = new Timing();
      TimingRepeatComponent repeat = new TimingRepeatComponent();
      int freq = 1;
      if (med.prescriptionDetails != null && med.prescriptionDetails.has("dosage")) {
        JsonObject dosage = med.prescriptionDetails.get("dosage").getAsJsonObject();
        if (dosage.has("frequency")) {
          freq = dosage.get("frequency").getAsInt();
        }
      }
      repeat.setFrequency(freq);
      repeat.setPeriod(1);
      repeat.setPeriodUnit(UnitsOfTime.D);

      // Time of day
      switch (freq) {
        case 1: repeat.addTimeOfDay("08:00:00"); break;
        case 2: repeat.addTimeOfDay("08:00:00"); repeat.addTimeOfDay("20:00:00"); break;
        case 3:
          repeat.addTimeOfDay("08:00:00");
          repeat.addTimeOfDay("12:00:00");
          repeat.addTimeOfDay("20:00:00");
          break;
        default: repeat.addTimeOfDay("08:00:00"); break;
      }

      timing.setRepeat(repeat);
      detail.setScheduled(timing);

      // Dosage amount
      if (med.prescriptionDetails != null && med.prescriptionDetails.has("dosage")) {
        JsonObject dosage = med.prescriptionDetails.get("dosage").getAsJsonObject();
        if (dosage.has("amount")) {
          SimpleQuantity qty = new SimpleQuantity();
          qty.setValue(dosage.get("amount").getAsDouble());
          if (dosage.has("unit")) {
            qty.setUnit(dosage.get("unit").getAsString());
          }
          detail.setDailyAmount(qty);
        }
      }
    }

    String cpId = UUID.nameUUIDFromBytes(
        ("med-careplan-" + encounter.uuid.toString()).getBytes()).toString();
    return newEntry(bundle, carePlan, cpId);
  }

  // ========================================================================================
  // Discharge Planning: CareTeam + discharge CarePlan with follow-up appointments
  // ========================================================================================

  /**
   * Create a discharge planning CarePlan for inpatient encounters.
   * Includes expected discharge date, follow-up appointment references,
   * and links to the CareTeam.
   *
   * @param personEntry    Patient entry
   * @param bundle         Bundle
   * @param encounterEntry Encounter entry
   * @param encounter      The inpatient encounter
   * @param careTeamEntry  The CareTeam entry (may be null)
   * @return The discharge CarePlan entry
   */
  private static BundleEntryComponent dischargePlan(
      BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, Encounter encounter,
      BundleEntryComponent careTeamEntry) {

    org.hl7.fhir.r4.model.CarePlan dischargeCp = new org.hl7.fhir.r4.model.CarePlan();
    dischargeCp.setStatus(encounter.ended ? CarePlanStatus.COMPLETED : CarePlanStatus.ACTIVE);
    dischargeCp.setIntent(CarePlanIntent.PLAN);
    dischargeCp.setSubject(new Reference(personEntry.getFullUrl()));
    dischargeCp.setEncounter(new Reference(encounterEntry.getFullUrl()));

    dischargeCp.addCategory(new CodeableConcept().addCoding(new Coding(
        SNOMED_URI, "58000006", "Discharge planning")));
    dischargeCp.setTitle("Entlassungsplanung");

    // Period: from admission to planned discharge
    long plannedDischarge = encounter.stop;
    dischargeCp.setPeriod(new Period()
        .setStart(new Date(encounter.start))
        .setEnd(new Date(plannedDischarge)));

    dischargeCp.setDescription(String.format(
        "Entlassungsplanung für stationären Aufenthalt in %s",
        encounter.provider != null ? encounter.provider.name : "Krankenhaus"));

    // Link to CareTeam
    if (careTeamEntry != null) {
      dischargeCp.addCareTeam(new Reference(careTeamEntry.getFullUrl()));
    }

    // Activity: Discharge assessment
    CarePlanActivityComponent assessActivity = dischargeCp.addActivity();
    CarePlanActivityDetailComponent assessDetail = assessActivity.getDetail();
    assessDetail.setStatus(encounter.ended
        ? CarePlanActivityStatus.COMPLETED : CarePlanActivityStatus.SCHEDULED);
    assessDetail.setKind(
        org.hl7.fhir.r4.model.CarePlan.CarePlanActivityKind.APPOINTMENT);
    assessDetail.setCode(mapCodeToCodeableConcept(
        new Code(SNOMED_URI, "183665006", "Discharge assessment"), SNOMED_URI));
    assessDetail.setDescription("Entlassungsuntersuchung und Arztbrief");

    // Scheduled discharge date
    Timing dischargeTiming = new Timing();
    TimingRepeatComponent tr = new TimingRepeatComponent();
    tr.setBounds(new Period().setEnd(new Date(plannedDischarge)));
    dischargeTiming.setRepeat(tr);
    assessDetail.setScheduled(dischargeTiming);

    // Activity: Follow-up appointment (2 weeks after discharge)
    long followUpTime = plannedDischarge + 14L * 24 * 60 * 60 * 1000;
    CarePlanActivityComponent followUpActivity = dischargeCp.addActivity();
    CarePlanActivityDetailComponent followUpDetail = followUpActivity.getDetail();
    followUpDetail.setStatus(CarePlanActivityStatus.SCHEDULED);
    followUpDetail.setKind(
        org.hl7.fhir.r4.model.CarePlan.CarePlanActivityKind.APPOINTMENT);
    followUpDetail.setCode(mapCodeToCodeableConcept(
        new Code(SNOMED_URI, "390906007", "Follow-up encounter"), SNOMED_URI));
    followUpDetail.setDescription("Nachsorgetermin");
    Timing fuTiming = new Timing();
    fuTiming.addEvent(new Date(followUpTime));
    followUpDetail.setScheduled(fuTiming);

    // Activity: Medication reconciliation
    CarePlanActivityComponent medRecActivity = dischargeCp.addActivity();
    CarePlanActivityDetailComponent medRecDetail = medRecActivity.getDetail();
    medRecDetail.setStatus(encounter.ended
        ? CarePlanActivityStatus.COMPLETED : CarePlanActivityStatus.INPROGRESS);
    medRecDetail.setCode(mapCodeToCodeableConcept(
        new Code(SNOMED_URI, "430193006", "Medication management"), SNOMED_URI));
    medRecDetail.setDescription("Medikationsabgleich bei Entlassung");

    String cpId = UUID.nameUUIDFromBytes(
        ("discharge-plan-" + encounter.uuid.toString()).getBytes()).toString();
    return newEntry(bundle, dischargeCp, cpId);
  }

  /**
   * Create an enhanced CareTeam for discharge planning with proper roles.
   *
   * @param personEntry    Patient entry
   * @param bundle         Bundle
   * @param encounterEntry Encounter entry
   * @param encounter      The encounter
   * @return The CareTeam entry
   */
  private static BundleEntryComponent dischargeCareTeam(
      BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, Encounter encounter) {

    CareTeam team = new CareTeam();
    team.setStatus(encounter.ended ? CareTeamStatus.INACTIVE : CareTeamStatus.ACTIVE);
    team.setName("Behandlungsteam - " + (encounter.provider != null
        ? encounter.provider.name : "Krankenhaus"));
    team.setSubject(new Reference(personEntry.getFullUrl()));
    team.setEncounter(new Reference(encounterEntry.getFullUrl()));
    team.setPeriod(new Period()
        .setStart(new Date(encounter.start))
        .setEnd(encounter.stop != 0L ? new Date(encounter.stop) : null));

    team.addCategory(new CodeableConcept().addCoding(new Coding(
        "http://loinc.org", "LA28866-4", "Healthcare")));

    // Participant: attending physician
    if (encounter.clinician != null) {
      CareTeamParticipantComponent physician = team.addParticipant();
      physician.addRole(mapCodeToCodeableConcept(
          new Code(SNOMED_URI, "223366009", "Attending physician"), SNOMED_URI));
      physician.setMember(clinicianReference(encounter.clinician, bundle));
      physician.setPeriod(new Period().setStart(new Date(encounter.start)));
    }

    // Participant: nursing staff (use scheduler clinician)
    Clinician nurse = findSchedulingClinician(encounter, null);
    if (nurse != null) {
      CareTeamParticipantComponent nurseParticipant = team.addParticipant();
      nurseParticipant.addRole(mapCodeToCodeableConcept(
          new Code(SNOMED_URI, "224535009", "Registered nurse"), SNOMED_URI));
      nurseParticipant.setMember(clinicianReference(nurse, bundle));
      nurseParticipant.setPeriod(new Period().setStart(new Date(encounter.start)));
    }

    // Participant: patient
    CareTeamParticipantComponent patient = team.addParticipant();
    patient.addRole(mapCodeToCodeableConcept(
        new Code(SNOMED_URI, "116154003", "Patient"), SNOMED_URI));
    patient.setMember(new Reference(personEntry.getFullUrl()));

    // Participant: organization
    if (encounter.provider != null) {
      CareTeamParticipantComponent org = team.addParticipant();
      org.addRole(mapCodeToCodeableConcept(
          new Code(SNOMED_URI, "224891009", "Healthcare services"), SNOMED_URI));
      if (TRANSACTION_BUNDLE) {
        org.setMember(new Reference(ExportHelper.buildFhirSearchUrl(
            "Organization", encounter.provider.getResourceID())));
      } else {
        String provUrl = findProviderUrl(encounter.provider, bundle);
        if (provUrl != null) {
          org.setMember(new Reference(provUrl));
        }
      }
    }

    String teamId = UUID.nameUUIDFromBytes(
        ("discharge-careteam-" + encounter.uuid.toString()).getBytes()).toString();
    return newEntry(bundle, team, teamId);
  }

  /**
   * Create nurse handover tasks for dashboard workflows.
   * Includes medication rounds, lab review and shift handoff.
   */
  private static void nurseHandoverTasks(
      BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, Encounter encounter,
      List<BundleEntryComponent> medReqEntries) {

    org.hl7.fhir.r4.model.Encounter encounterResource =
        (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();
    Reference owner = encounterResource.hasParticipant()
        ? encounterResource.getParticipantFirstRep().getIndividual()
        : null;

    if (!medReqEntries.isEmpty()) {
      Task medTask = new Task();
      medTask.setStatus(encounter.ended ? TaskStatus.COMPLETED : TaskStatus.INPROGRESS);
      medTask.setIntent(TaskIntent.ORDER);
      medTask.setPriority(Task.TaskPriority.ROUTINE);
      medTask.setCode(mapCodeToCodeableConcept(
          new Code(SNOMED_URI, "182849000", "Drug administration"), SNOMED_URI));
      medTask.setDescription("Pflegeaufgabe: Medikationsrunde und Adhärenzprüfung");
      medTask.setFor(new Reference(personEntry.getFullUrl()));
      medTask.setEncounter(new Reference(encounterEntry.getFullUrl()));
      medTask.setAuthoredOn(new Date(encounter.start));
      medTask.setExecutionPeriod(new Period()
          .setStart(new Date(encounter.start))
          .setEnd(encounter.stop != 0L ? new Date(encounter.stop) : null));
      if (owner != null) {
        medTask.setRequester(owner);
        medTask.setOwner(owner);
      }
      for (BundleEntryComponent medReqEntry : medReqEntries) {
        medTask.addBasedOn(new Reference(medReqEntry.getFullUrl()));
      }

      String medTaskId = UUID.nameUUIDFromBytes(
          ("nurse-med-round-" + encounter.uuid.toString()).getBytes()).toString();
      newEntry(bundle, medTask, medTaskId);
    }

    if (!encounter.reports.isEmpty()) {
      Task labTask = new Task();
      labTask.setStatus(encounter.ended ? TaskStatus.COMPLETED : TaskStatus.READY);
      labTask.setIntent(TaskIntent.ORDER);
      labTask.setPriority(Task.TaskPriority.ASAP);
      labTask.setCode(mapCodeToCodeableConcept(
          new Code(SNOMED_URI, "386344002", "Laboratory data interpretation"), SNOMED_URI));
      labTask.setDescription("Pflegeaufgabe: Auffällige Laborwerte prüfen und rückmelden");
      labTask.setFor(new Reference(personEntry.getFullUrl()));
      labTask.setEncounter(new Reference(encounterEntry.getFullUrl()));
      labTask.setAuthoredOn(new Date(encounter.start));
      if (owner != null) {
        labTask.setRequester(owner);
        labTask.setOwner(owner);
      }

      String labTaskId = UUID.nameUUIDFromBytes(
          ("nurse-lab-review-" + encounter.uuid.toString()).getBytes()).toString();
      newEntry(bundle, labTask, labTaskId);
    }

    Task handoffTask = new Task();
    handoffTask.setStatus(encounter.ended ? TaskStatus.COMPLETED : TaskStatus.READY);
    handoffTask.setIntent(TaskIntent.ORDER);
    handoffTask.setPriority(Task.TaskPriority.ROUTINE);
    handoffTask.setCode(mapCodeToCodeableConcept(
        new Code(SNOMED_URI, "225725005", "Nursing handover"), SNOMED_URI));
    handoffTask.setDescription("Schichtübergabe: offene Punkte, Risiken und nächste Schritte");
    handoffTask.setFor(new Reference(personEntry.getFullUrl()));
    handoffTask.setEncounter(new Reference(encounterEntry.getFullUrl()));
    handoffTask.setAuthoredOn(new Date(encounter.start));
    if (owner != null) {
      handoffTask.setRequester(owner);
      handoffTask.setOwner(owner);
    }

    String handoffTaskId = UUID.nameUUIDFromBytes(
        ("nurse-handover-" + encounter.uuid.toString()).getBytes()).toString();
    newEntry(bundle, handoffTask, handoffTaskId);
  }

  /**
   * Add discharge follow-up workflow artifacts for post-discharge coordination.
   */
  private static void dischargeFollowUpWorkflow(
      BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, Encounter encounter,
      BundleEntryComponent careTeamEntry) {
    long dischargeTime = encounter.stop != 0L ? encounter.stop : encounter.start;
    long outreachTime = dischargeTime + 48L * 60 * 60 * 1000;

    if (shouldExport(Task.class)) {
      Task followUpTask = new Task();
      followUpTask.setStatus(encounter.ended ? TaskStatus.REQUESTED : TaskStatus.DRAFT);
      followUpTask.setIntent(TaskIntent.PLAN);
      followUpTask.setPriority(Task.TaskPriority.ROUTINE);
      followUpTask.setCode(mapCodeToCodeableConcept(
          new Code(SNOMED_URI, "390906007", "Follow-up encounter"), SNOMED_URI));
      followUpTask.setDescription("Nachsorge: Telefonkontakt 48h nach Entlassung");
      followUpTask.setFor(new Reference(personEntry.getFullUrl()));
      followUpTask.setEncounter(new Reference(encounterEntry.getFullUrl()));
      followUpTask.setAuthoredOn(new Date(dischargeTime));
      followUpTask.setExecutionPeriod(new Period().setStart(new Date(outreachTime)));
      if (careTeamEntry != null) {
        followUpTask.addBasedOn(new Reference(careTeamEntry.getFullUrl()));
      }

      String followTaskId = UUID.nameUUIDFromBytes(
          ("discharge-followup-task-" + encounter.uuid.toString()).getBytes()).toString();
      newEntry(bundle, followUpTask, followTaskId);
    }

    if (shouldExport(CommunicationRequest.class)) {
      CommunicationRequest followCom = new CommunicationRequest();
      followCom.setStatus(encounter.ended
          ? CommunicationRequestStatus.ACTIVE : CommunicationRequestStatus.DRAFT);
      followCom.setPriority(CommunicationPriority.ROUTINE);
      followCom.setSubject(new Reference(personEntry.getFullUrl()));
      followCom.addRecipient(new Reference(personEntry.getFullUrl()));
      followCom.addCategory(new CodeableConcept().addCoding(new Coding(
          "http://terminology.hl7.org/CodeSystem/communication-category",
          "instruction", "Instruction")));
      followCom.addPayload().setContent(new StringType(
          "Bitte melden Sie sich 48 Stunden nach Entlassung für die Nachsorge."));
      followCom.setAuthoredOn(new Date(dischargeTime));
      followCom.setOccurrence(new DateTimeType(new Date(outreachTime)));

      String followComId = UUID.nameUUIDFromBytes(
          ("discharge-followup-comm-" + encounter.uuid.toString()).getBytes()).toString();
      newEntry(bundle, followCom, followComId);
    }
  }

  /**
   * Build tumorboard v1 (preparation) and v2 (post-board plan) CarePlans.
   */
  private static void tumorBoardCarePlans(
      BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, Encounter encounter) {
    long boardTime = encounter.start + Math.max(15L * 60 * 1000,
        (encounter.stop - encounter.start) / 2);

    org.hl7.fhir.r4.model.CarePlan preBoard = new org.hl7.fhir.r4.model.CarePlan();
    preBoard.setStatus(encounter.ended ? CarePlanStatus.COMPLETED : CarePlanStatus.ACTIVE);
    preBoard.setIntent(CarePlanIntent.PLAN);
    preBoard.setTitle("Tumorboard v1 - Vorbereitung");
    preBoard.setSubject(new Reference(personEntry.getFullUrl()));
    preBoard.setEncounter(new Reference(encounterEntry.getFullUrl()));
    preBoard.addCategory(new CodeableConcept().addCoding(new Coding(
        SNOMED_URI, "734163000", "Multidisciplinary cancer care")));
    preBoard.setDescription("Vorbereitung für prätherapeutisches Tumorboard mit Diagnostik-Review");
    preBoard.setPeriod(new Period().setStart(new Date(encounter.start)).setEnd(new Date(boardTime)));

    CarePlanActivityDetailComponent preDataReview = preBoard.addActivity().getDetail();
    preDataReview.setStatus(encounter.ended
        ? CarePlanActivityStatus.COMPLETED : CarePlanActivityStatus.SCHEDULED);
    preDataReview.setCode(mapCodeToCodeableConcept(
        new Code(SNOMED_URI, "698247007", "Multidisciplinary review"), SNOMED_URI));
    preDataReview.setDescription("Radiologie, Pathologie und Komorbiditäten für Tumorboard aufbereiten");

    CarePlanActivityDetailComponent preCaseSummary = preBoard.addActivity().getDetail();
    preCaseSummary.setStatus(encounter.ended
        ? CarePlanActivityStatus.COMPLETED : CarePlanActivityStatus.SCHEDULED);
    preCaseSummary.setCode(mapCodeToCodeableConcept(
        new Code(SNOMED_URI, "371531000", "Clinical case summary"), SNOMED_URI));
    preCaseSummary.setDescription("Tumorboard v1 Fallzusammenfassung bereitstellen");

    String preBoardId = UUID.nameUUIDFromBytes(
        ("tumorboard-v1-" + encounter.uuid.toString()).getBytes()).toString();
    BundleEntryComponent preBoardEntry = newEntry(bundle, preBoard, preBoardId);

    org.hl7.fhir.r4.model.CarePlan postBoard = new org.hl7.fhir.r4.model.CarePlan();
    postBoard.setStatus(encounter.ended ? CarePlanStatus.ACTIVE : CarePlanStatus.DRAFT);
    postBoard.setIntent(CarePlanIntent.ORDER);
    postBoard.setTitle("Tumorboard v2 - Nachbesprechung");
    postBoard.setSubject(new Reference(personEntry.getFullUrl()));
    postBoard.setEncounter(new Reference(encounterEntry.getFullUrl()));
    postBoard.addCategory(new CodeableConcept().addCoding(new Coding(
        SNOMED_URI, "734163000", "Multidisciplinary cancer care")));
    postBoard.setDescription("Therapieempfehlung und Nachsorge nach Tumorboard-Beschluss");
    postBoard.setPeriod(new Period().setStart(new Date(boardTime))
        .setEnd(encounter.stop != 0L ? new Date(encounter.stop) : null));
    postBoard.addBasedOn(new Reference(preBoardEntry.getFullUrl()));

    CarePlanActivityDetailComponent postDecision = postBoard.addActivity().getDetail();
    postDecision.setStatus(encounter.ended
        ? CarePlanActivityStatus.SCHEDULED : CarePlanActivityStatus.NOTSTARTED);
    postDecision.setCode(mapCodeToCodeableConcept(
        new Code(SNOMED_URI, "735324008", "Oncology treatment plan"), SNOMED_URI));
    postDecision.setDescription("Tumorboard v2: Therapieplan finalisieren und kommunizieren");

    CarePlanActivityDetailComponent postPrep = postBoard.addActivity().getDetail();
    postPrep.setStatus(encounter.ended
        ? CarePlanActivityStatus.SCHEDULED : CarePlanActivityStatus.NOTSTARTED);
    postPrep.setCode(mapCodeToCodeableConcept(
        new Code(SNOMED_URI, "386053000", "Evaluation procedure"), SNOMED_URI));
    postPrep.setDescription("OP-/Systemtherapie-Vorbereitung und Terminierung");

    String postBoardId = UUID.nameUUIDFromBytes(
        ("tumorboard-v2-" + encounter.uuid.toString()).getBytes()).toString();
    newEntry(bundle, postBoard, postBoardId);
  }

  /**
   * Determine if encounter looks oncology-related and should get tumorboard plans.
   */
  private static boolean isTumorBoardCandidate(Encounter encounter) {
    if (encounter == null) {
      return false;
    }

    for (HealthRecord.Entry condition : encounter.conditions) {
      for (Code code : condition.codes) {
        String text = (code.display == null ? "" : code.display).toLowerCase();
        if (text.contains("cancer") || text.contains("carcinoma")
            || text.contains("neoplasm") || text.contains("tumor")) {
          return true;
        }
      }
    }

    for (Procedure procedure : encounter.procedures) {
      for (Code code : procedure.codes) {
        String text = (code.display == null ? "" : code.display).toLowerCase();
        if (text.contains("oncolog") || text.contains("tumor")
            || text.contains("biopsy") || text.contains("resection")) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns true if the given encounter type represents an unscheduled (walk-in) visit.
   * Emergency and Urgent Care encounters are considered unscheduled.
   *
   * @param encounter the encounter to check
   * @return true if unscheduled
   */
  private static boolean isUnscheduledEncounter(Encounter encounter) {
    EncounterType type = EncounterType.fromString(encounter.type);
    return type == EncounterType.EMERGENCY || type == EncounterType.URGENTCARE;
  }

  /**
   * Map the encounter's service type to a SNOMED coded concept.
   * Uses the encounter's primary code when available, otherwise falls back
   * to a generic "consultation" code.
   *
   * @param encounter the encounter to derive service type from
   * @return CodeableConcept for the service type
   */
  private static CodeableConcept encounterServiceType(Encounter encounter) {
    if (!encounter.codes.isEmpty()) {
      return mapCodeToCodeableConcept(encounter.codes.get(0), SNOMED_URI);
    }
    // Wellness encounter - use a generic code
    Code wellness = new Code(SNOMED_URI, "185349003", "Encounter for check up");
    return mapCodeToCodeableConcept(wellness, SNOMED_URI);
  }

  /**
   * Build a FHIR Reference to a Clinician (Practitioner). Works for both
   * transaction bundles (search URL) and collection bundles (fullUrl lookup).
   *
   * @param clinician the clinician to reference
   * @param bundle    the current bundle (for collection-mode lookup)
   * @return a Reference to the Practitioner, or null if clinician is null
   */
  private static Reference clinicianReference(Clinician clinician, Bundle bundle) {
    if (clinician == null) {
      return null;
    }
    String display = clinician.getFullname();
    String url = findPractitioner(clinician, bundle);
    if (url != null) {
      return new Reference(url).setDisplay(display);
    }

    if (shouldExport(Practitioner.class)) {
      BundleEntryComponent entry = practitioner(bundle, clinician);
      return new Reference(entry.getFullUrl()).setDisplay(display);
    }

    return new Reference(ExportHelper.buildFhirNpiSearchUrl(clinician))
        .setDisplay(display);
  }

  /**
   * Try to find a clinician with a nursing / admin specialty from the encounter's
   * provider to act as the appointment scheduler. Falls back to the encounter
   * clinician if no nurse is available.
   *
   * @param encounter the encounter
   * @param person    the patient (used as RNG source)
   * @return a Clinician who "scheduled" the appointment
   */
  private static Clinician findSchedulingClinician(Encounter encounter, Person person) {
    Provider provider = encounter.provider;
    if (provider == null) {
      return encounter.clinician;
    }
    // Try nursing specialties first
    String[] schedulerSpecialties = {
        ClinicianSpecialty.NURSE_PRACTITIONER,
        ClinicianSpecialty.CLINICAL_NURSE_SPECIALIST,
        ClinicianSpecialty.CERTIFIED_NURSE_MIDWIFE,
        ClinicianSpecialty.PHYSICIAN_ASSISTANT
    };
    for (String specialty : schedulerSpecialties) {
      ArrayList<Clinician> nurses = provider.clinicianMap.get(specialty);
      if (nurses != null && !nurses.isEmpty()) {
        if (person != null) {
          return nurses.get(person.randInt(nurses.size()));
        }
        return nurses.get(0);
      }
    }
    // Fallback: use a different clinician from general practice if possible
    ArrayList<Clinician> gps =
        provider.clinicianMap.get(ClinicianSpecialty.GENERAL_PRACTICE);
    if (gps != null && gps.size() > 1 && encounter.clinician != null) {
      // Pick one who is NOT the encounter's clinician
      for (Clinician c : gps) {
        if (c.identifier != encounter.clinician.identifier) {
          return c;
        }
      }
    }
    // Last resort: the encounter clinician scheduled it themselves
    return encounter.clinician;
  }

  /**
   * Try to find a referring practitioner for this encounter.
   * For non-wellness encounters, the patient's primary care / wellness clinician
   * is used as the referrer.
   *
   * @param encounter the encounter
   * @param person    the patient
   * @return the referring Clinician, or null if this is a wellness encounter
   *         or no suitable referrer is found
   */
  private static Clinician findReferringClinician(Encounter encounter, Person person) {
    EncounterType type = EncounterType.fromString(encounter.type);
    if (type == EncounterType.WELLNESS) {
      return null; // wellness visits don't have a referrer
    }
    // Get the patient's wellness / PCP provider
    Provider wellnessProvider = person.getProvider(EncounterType.WELLNESS, encounter.start);
    if (wellnessProvider == null || wellnessProvider.equals(encounter.provider)) {
      return null; // same provider = no external referral
    }
    // Pick a GP clinician from the wellness provider
    ArrayList<Clinician> gps =
        wellnessProvider.clinicianMap.get(ClinicianSpecialty.GENERAL_PRACTICE);
    if (gps != null && !gps.isEmpty()) {
      return gps.get(person.randInt(gps.size()));
    }
    return null;
  }

  /**
   * Create (or find) a PractitionerRole resource for the given clinician
   * with their ACTUAL specialty. Emitted regardless of USE_US_CORE_IG.
   *
   * @param bundle    the bundle to add to
   * @param clinician the clinician
   * @return the PractitionerRole entry
   */
  private static BundleEntryComponent practitionerRoleWithSpecialty(
      Bundle bundle, Clinician clinician) {
    // Determine the real specialty
    String specialty = (String) clinician.attributes.get(Clinician.SPECIALTY);
    if (specialty == null) {
      specialty = ClinicianSpecialty.GENERAL_PRACTICE;
    }

    // Deterministic UUID for this clinician's practitioner role
    UUID origUUID = UUID.fromString(clinician.uuid);
    String roleUuid = ExportHelper.buildUUID(origUUID.getLeastSignificantBits(),
        origUUID.getMostSignificantBits(),
        "PractitionerRole for Clinician " + origUUID);

    // Check if we already created this PractitionerRole in this bundle
    for (BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource().fhirType().equals("PractitionerRole")
          && entry.getResource().getId().equals(roleUuid)) {
        return entry;
      }
    }

    PractitionerRole practitionerRole = new PractitionerRole();

    // Practitioner reference
    practitionerRole.setPractitioner(new Reference()
        .setIdentifier(new Identifier()
            .setSystem("http://hl7.org/fhir/sid/us-npi")
            .setValue(clinician.npi))
        .setDisplay(clinician.getFullname()));

    // Organization reference
    if (clinician.getOrganization() != null) {
      practitionerRole.setOrganization(new Reference()
          .setIdentifier(new Identifier()
              .setSystem(SYNTHEA_IDENTIFIER)
              .setValue(clinician.getOrganization().getResourceID()))
          .setDisplay(clinician.getOrganization().name));

      // Location
      practitionerRole.addLocation()
          .setIdentifier(new Identifier()
              .setSystem(SYNTHEA_IDENTIFIER)
              .setValue(clinician.getOrganization().getResourceLocationID()))
          .setDisplay(clinician.getOrganization().name);

      // Telecom from organization
      if (clinician.getOrganization().phone != null
          && !clinician.getOrganization().phone.isEmpty()) {
        practitionerRole.addTelecom(new ContactPoint()
            .setSystem(ContactPointSystem.PHONE)
            .setValue(clinician.getOrganization().phone));
      }
    }

    // Use the clinician's REAL specialty
    String cmsCode = ClinicianSpecialty.getCMSProviderSpecialtyCode(specialty);
    if (cmsCode != null) {
      practitionerRole.addCode(
          mapCodeToCodeableConcept(
              new Code("http://nucc.org/provider-taxonomy", cmsCode, specialty), null));
      practitionerRole.addSpecialty(
          mapCodeToCodeableConcept(
              new Code("http://nucc.org/provider-taxonomy", cmsCode, specialty), null));
    } else {
      // Fallback: freetext specialty
      CodeableConcept specConcept = new CodeableConcept();
      specConcept.setText(specialty);
      practitionerRole.addSpecialty(specConcept);
    }

    practitionerRole.setActive(true);
    return newEntry(bundle, practitionerRole, roleUuid);
  }

  /**
   * Create a Schedule resource representing the provider's schedule for the service type
   * of this encounter. One Schedule is created per unique provider in the bundle.
   *
   * @param personEntry   The Patient entry
   * @param bundle        The Bundle to add to
   * @param encounterEntry The Encounter entry
   * @param encounter     The source encounter
   * @return The created Schedule entry
   */
  private static BundleEntryComponent schedule(BundleEntryComponent personEntry,
      Bundle bundle, BundleEntryComponent encounterEntry, Encounter encounter) {
    Schedule scheduleResource = new Schedule();

    scheduleResource.setActive(true);

    // Service type from encounter
    scheduleResource.addServiceType(encounterServiceType(encounter));

    // Link schedule to the provider and practitioner from the encounter
    org.hl7.fhir.r4.model.Encounter encounterResource =
        (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();

    if (encounterResource.hasServiceProvider()) {
      scheduleResource.addActor(encounterResource.getServiceProvider());
    }

    if (encounterResource.hasParticipant()) {
      scheduleResource.addActor(
          encounterResource.getParticipantFirstRep().getIndividual());
    }

    // Planning horizon covers the encounter period
    if (encounterResource.hasPeriod()) {
      scheduleResource.setPlanningHorizon(encounterResource.getPeriod());
    }

    scheduleResource.setComment("Auto-generated schedule for "
        + encounterServiceType(encounter).getText());

    // Use a deterministic UUID based on provider + encounter start to avoid duplicates
    String scheduleId = UUID.nameUUIDFromBytes(
        ("schedule-" + encounter.uuid.toString()).getBytes()).toString();
    return newEntry(bundle, scheduleResource, scheduleId);
  }

  /**
   * Create a Slot resource representing a bookable time window within the Schedule
   * for this encounter. The slot covers the encounter's time period.
   *
   * @param bundle        The Bundle to add to
   * @param scheduleEntry The Schedule entry this Slot belongs to
   * @param encounter     The source encounter
   * @return The created Slot entry
   */
  private static BundleEntryComponent slot(Bundle bundle,
      BundleEntryComponent scheduleEntry, Encounter encounter) {
    Slot slotResource = new Slot();

    slotResource.setSchedule(new Reference(scheduleEntry.getFullUrl()));
    slotResource.setStatus(SlotStatus.BUSY);

    // Service type matches the schedule
    slotResource.addServiceType(encounterServiceType(encounter));

    slotResource.setStart(new Date(encounter.start));
    slotResource.setEnd(new Date(encounter.stop));

    String slotId = UUID.nameUUIDFromBytes(
        ("slot-" + encounter.uuid.toString()).getBytes()).toString();
    return newEntry(bundle, slotResource, slotId);
  }

  /**
   * Create Appointment resource(s) with realistic lifecycle simulation.
   * <p>
   * Uses the Person's RNG to probabilistically determine the appointment status:
   * ~80% fulfilled, ~8% cancelled (with a rescheduled replacement),
   * ~5% noshow, ~5% booked, ~2% waitlisted.
   * <p>
   * Each appointment includes rich participant information:
   * patient (SBJ), primary performer (PPRF), scheduler/nurse (ATND),
   * referring practitioner (REF), and location.
   * <p>
   * For cancelled appointments, a second "rescheduled" appointment is also
   * created 1-14 days later with status=fulfilled.
   *
   * @param person         The Person (for deterministic RNG)
   * @param personEntry    The Patient entry
   * @param bundle         The Bundle to add to
   * @param encounterEntry The Encounter entry
   * @param slotEntry      The Slot entry this Appointment refers to
   * @param encounter      The source encounter
   * @return The created Appointment entry (the final/active one)
   */
  private static BundleEntryComponent appointment(Person person,
      BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry,
      BundleEntryComponent slotEntry, Encounter encounter) {

    // Determine appointment lifecycle status
    double roll = person.rand();
    AppointmentStatus status;
    String cancelReason = null;
    boolean createRescheduled = false;

    if (roll < APPT_PROB_FULFILLED) {
      status = AppointmentStatus.FULFILLED;
    } else if (roll < APPT_PROB_CANCELLED) {
      status = AppointmentStatus.CANCELLED;
      cancelReason = person.rand() < 0.6 ? "pat" : "prov";
      createRescheduled = true;
    } else if (roll < APPT_PROB_NOSHOW) {
      status = AppointmentStatus.NOSHOW;
    } else if (roll < APPT_PROB_BOOKED) {
      status = AppointmentStatus.BOOKED;
    } else {
      status = AppointmentStatus.WAITLIST;
    }

    // Find additional participants
    Clinician scheduler = findSchedulingClinician(encounter, person);
    Clinician referrer = findReferringClinician(encounter, person);

    // Create PractitionerRole for the performing clinician (with real specialty)
    if (encounter.clinician != null && shouldExport(PractitionerRole.class)) {
      practitionerRoleWithSpecialty(bundle, encounter.clinician);
    }
    // Also create PractitionerRole for the scheduler if different
    if (scheduler != null && scheduler != encounter.clinician
        && shouldExport(PractitionerRole.class)) {
      practitionerRoleWithSpecialty(bundle, scheduler);
    }
    // Also create PractitionerRole for the referrer if present
    if (referrer != null && shouldExport(PractitionerRole.class)) {
      practitionerRoleWithSpecialty(bundle, referrer);
    }

    // Build the primary appointment
    BundleEntryComponent apptEntry = buildAppointmentResource(
        person, personEntry, bundle, encounterEntry, slotEntry, encounter,
        status, cancelReason, scheduler, referrer, "appointment-");

    // For cancelled appointments, create a rescheduled replacement
    if (createRescheduled) {
      // Rescheduled appointment is 1-14 days later, status = fulfilled
      long rescheduleDelayMs = (1 + person.randInt(14))
          * 24L * 60L * 60L * 1000L;
      // Temporarily shift the encounter times for the rescheduled appointment
      long origStart = encounter.start;
      long origStop = encounter.stop;
      encounter.start = origStart + rescheduleDelayMs;
      encounter.stop = origStop + rescheduleDelayMs;

      // Create a new slot for the rescheduled time
      BundleEntryComponent rescheduledSlotEntry = null;
      if (shouldExport(Slot.class)) {
        String reslotId = UUID.nameUUIDFromBytes(
            ("slot-rescheduled-" + encounter.uuid.toString()).getBytes()).toString();
        Slot reslot = new Slot();
        reslot.setSchedule(
            ((Slot) slotEntry.getResource()).getSchedule());
        reslot.setStatus(SlotStatus.BUSY);
        reslot.addServiceType(encounterServiceType(encounter));
        reslot.setStart(new Date(encounter.start));
        reslot.setEnd(new Date(encounter.stop));
        rescheduledSlotEntry = newEntry(bundle, reslot, reslotId);
      }

      BundleEntryComponent rescheduledEntry = buildAppointmentResource(
          person, personEntry, bundle, encounterEntry,
          rescheduledSlotEntry != null ? rescheduledSlotEntry : slotEntry,
          encounter, AppointmentStatus.FULFILLED,
          null, scheduler, referrer, "appointment-rescheduled-");

      // Restore original times
      encounter.start = origStart;
      encounter.stop = origStop;

      return rescheduledEntry;
    }

    return apptEntry;
  }

  /**
   * Build a single Appointment resource with all participant details.
   *
   * @param person          The Person (for RNG)
   * @param personEntry     The Patient entry
   * @param bundle          The Bundle
   * @param encounterEntry  The Encounter entry
   * @param slotEntry       The Slot entry
   * @param encounter       The source Encounter
   * @param status          The appointment status
   * @param cancelReason    Cancellation reason code (null if not cancelled)
   * @param scheduler       The clinician who scheduled the appointment
   * @param referrer        The referring clinician (may be null)
   * @param idPrefix        Prefix for the deterministic UUID
   * @return the created Appointment entry
   */
  private static BundleEntryComponent buildAppointmentResource(
      Person person, BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, BundleEntryComponent slotEntry,
      Encounter encounter, AppointmentStatus status, String cancelReason,
      Clinician scheduler, Clinician referrer, String idPrefix) {

    Appointment appointmentResource = new Appointment();

    appointmentResource.setStatus(status);

    // Service type
    appointmentResource.addServiceType(encounterServiceType(encounter));

    // Reason from encounter
    org.hl7.fhir.r4.model.Encounter encounterResource =
        (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();
    if (encounterResource.hasReasonCode()) {
      appointmentResource.addReasonCode(encounterResource.getReasonCodeFirstRep());
    }

    // Timing
    appointmentResource.setStart(new Date(encounter.start));
    appointmentResource.setEnd(new Date(encounter.stop));
    long durationMinutes = (encounter.stop - encounter.start) / (60 * 1000);
    appointmentResource.setMinutesDuration((int) Math.max(durationMinutes, 1));

    // Slot reference
    if (slotEntry != null) {
      appointmentResource.addSlot(new Reference(slotEntry.getFullUrl()));
    }

    // Cancellation reason
    if (cancelReason != null) {
      String display = cancelReason.equals("pat")
          ? "Patient request" : "Provider request";
      appointmentResource.setCancelationReason(
          mapCodeToCodeableConcept(
              new Code(CANCEL_REASON_SYSTEM, cancelReason, display),
              CANCEL_REASON_SYSTEM));
    }

    // Created timestamp (booking was made some time before the appointment)
    // Simulate booking 1-30 days before the appointment
    long bookingLeadMs = (1 + person.randInt(30)) * 24L * 60L * 60L * 1000L;
    long createdTime = Math.max(0, encounter.start - bookingLeadMs);
    appointmentResource.setCreated(new Date(createdTime));

    // Priority (1=ASAP, 5=routine, 9=low priority)
    EncounterType encType = EncounterType.fromString(encounter.type);
    if (encType == EncounterType.INPATIENT) {
      appointmentResource.setPriority(3); // higher priority for inpatient
    } else {
      appointmentResource.setPriority(5); // routine
    }

    // Description
    String desc = encounterServiceType(encounter).getText();
    if (desc != null) {
      appointmentResource.setDescription(desc);
    }

    // ---- Participants ----

    // 1. Patient (SBJ = subject)
    AppointmentParticipantComponent patientParticipant = appointmentResource.addParticipant();
    patientParticipant.setActor(new Reference(personEntry.getFullUrl()));
    patientParticipant.addType(mapCodeToCodeableConcept(
        new Code("http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
            "SBJ", "subject"), null));
    patientParticipant.setStatus(
        status == AppointmentStatus.NOSHOW
            ? ParticipationStatus.DECLINED : ParticipationStatus.ACCEPTED);
    patientParticipant.setRequired(Appointment.ParticipantRequired.REQUIRED);

    // 2. Primary performer (PPRF) - the encounter clinician / physician
    if (encounter.clinician != null) {
      AppointmentParticipantComponent practitionerParticipant =
          appointmentResource.addParticipant();
      practitionerParticipant.setActor(clinicianReference(encounter.clinician, bundle));
      practitionerParticipant.addType(mapCodeToCodeableConcept(
          new Code("http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
              "PPRF", "primary performer"), null));
      practitionerParticipant.setStatus(ParticipationStatus.ACCEPTED);
      practitionerParticipant.setRequired(Appointment.ParticipantRequired.REQUIRED);
    }

    // 3. Scheduler / nurse / admin staff (ATND = attender, who coordinated the appointment)
    if (scheduler != null) {
      AppointmentParticipantComponent schedulerParticipant =
          appointmentResource.addParticipant();
      schedulerParticipant.setActor(clinicianReference(scheduler, bundle));
      schedulerParticipant.addType(mapCodeToCodeableConcept(
          new Code("http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
              "ATND", "attender"), null));
      schedulerParticipant.setStatus(ParticipationStatus.ACCEPTED);
      schedulerParticipant.setRequired(Appointment.ParticipantRequired.INFORMATIONONLY);
    }

    // 4. Referring practitioner (REF) — only for non-wellness, external referrals
    if (referrer != null) {
      AppointmentParticipantComponent referrerParticipant =
          appointmentResource.addParticipant();
      referrerParticipant.setActor(clinicianReference(referrer, bundle));
      referrerParticipant.addType(mapCodeToCodeableConcept(
          new Code("http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
              "REF", "referrer"), null));
      referrerParticipant.setStatus(ParticipationStatus.ACCEPTED);
      referrerParticipant.setRequired(Appointment.ParticipantRequired.INFORMATIONONLY);
    }

    // 5. Location participant
    if (encounterResource.hasLocation()) {
      AppointmentParticipantComponent locationParticipant =
          appointmentResource.addParticipant();
      locationParticipant.setActor(
          encounterResource.getLocationFirstRep().getLocation());
      locationParticipant.setStatus(ParticipationStatus.ACCEPTED);
      locationParticipant.setRequired(Appointment.ParticipantRequired.REQUIRED);
    }

    String appointmentId = UUID.nameUUIDFromBytes(
        (idPrefix + encounter.uuid.toString()).getBytes()).toString();
    return newEntry(bundle, appointmentResource, appointmentId);
  }

  /**
   * Create a standalone ServiceRequest resource for a Procedure or ImagingStudy.
   * This represents a clinical order that was placed and fulfilled during the encounter.
   *
   * @param personEntry    The Patient entry
   * @param bundle         The Bundle to add to
   * @param encounterEntry The Encounter entry
   * @param encounter      The source encounter
   * @param code           The code for the ordered service
   * @param reasonCode     The reason for the order (may be null)
   * @param orderedItemUuid UUID of the ordered item (Procedure or ImagingStudy)
   * @param authoredOn     When the order was authored (millis since epoch)
   * @return The created ServiceRequest entry
   */
  private static BundleEntryComponent serviceRequest(BundleEntryComponent personEntry,
      Bundle bundle, BundleEntryComponent encounterEntry, Encounter encounter,
      Code code, Code reasonCode, String orderedItemUuid, long authoredOn) {
    ServiceRequest serviceRequestResource = new ServiceRequest();

    serviceRequestResource.setStatus(ServiceRequest.ServiceRequestStatus.COMPLETED);
    serviceRequestResource.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
    serviceRequestResource.setSubject(new Reference(personEntry.getFullUrl()));
    serviceRequestResource.setEncounter(new Reference(encounterEntry.getFullUrl()));

    // Code for the requested service
    serviceRequestResource.setCode(mapCodeToCodeableConcept(code, SNOMED_URI));

    // When the order was placed
    serviceRequestResource.setAuthoredOn(new Date(authoredOn));

    // Requester - the encounter clinician
    org.hl7.fhir.r4.model.Encounter encounterResource =
        (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();
    if (encounterResource.hasParticipant()) {
      serviceRequestResource.setRequester(
          encounterResource.getParticipantFirstRep().getIndividual());
    }

    // Performer - the provider organization
    if (encounterResource.hasServiceProvider()) {
      serviceRequestResource.addPerformer(encounterResource.getServiceProvider());
    }

    // Reason
    if (reasonCode != null) {
      serviceRequestResource.addReasonCode(mapCodeToCodeableConcept(reasonCode, SNOMED_URI));
    }

    // Category: procedure order
    Code categoryCode = new Code("http://snomed.info/sct", "386053000",
        "Evaluation procedure");
    serviceRequestResource.addCategory(mapCodeToCodeableConcept(categoryCode, SNOMED_URI));

    String serviceRequestId = UUID.nameUUIDFromBytes(
        ("servicerequest-" + orderedItemUuid).getBytes()).toString();
    return newEntry(bundle, serviceRequestResource, serviceRequestId);
  }

  /**
   * Create a Task resource representing the fulfilment of a ServiceRequest.
   * Tasks track the workflow status of clinical orders.
   *
   * @param personEntry          The Patient entry
   * @param bundle               The Bundle to add to
   * @param encounterEntry       The Encounter entry
   * @param serviceRequestEntry  The ServiceRequest this Task fulfils
   * @param encounter            The source encounter
   * @param code                 The code describing the task
   * @param taskStart            When the task started (millis since epoch)
   * @param taskEnd              When the task ended (millis since epoch, 0 if ongoing)
   * @return The created Task entry
   */
  private static BundleEntryComponent task(BundleEntryComponent personEntry,
      Bundle bundle, BundleEntryComponent encounterEntry,
      BundleEntryComponent serviceRequestEntry, Encounter encounter,
      Code code, long taskStart, long taskEnd) {
    Task taskResource = new Task();

    // Status based on whether the task is completed
    if (taskEnd != 0L) {
      taskResource.setStatus(TaskStatus.COMPLETED);
    } else {
      taskResource.setStatus(TaskStatus.INPROGRESS);
    }

    taskResource.setIntent(TaskIntent.ORDER);

    // Code
    taskResource.setCode(mapCodeToCodeableConcept(code, SNOMED_URI));

    // Description
    taskResource.setDescription("Fulfil order: " + code.display);

    // Patient
    taskResource.setFor(new Reference(personEntry.getFullUrl()));

    // Encounter
    taskResource.setEncounter(new Reference(encounterEntry.getFullUrl()));

    // Timing
    taskResource.setAuthoredOn(new Date(taskStart));
    Period executionPeriod = new Period().setStart(new Date(taskStart));
    if (taskEnd != 0L) {
      executionPeriod.setEnd(new Date(taskEnd));
    }
    taskResource.setExecutionPeriod(executionPeriod);

    // Last modified
    taskResource.setLastModified(taskEnd != 0L ? new Date(taskEnd) : new Date(taskStart));

    // Requester and Owner from encounter
    org.hl7.fhir.r4.model.Encounter encounterResource =
        (org.hl7.fhir.r4.model.Encounter) encounterEntry.getResource();
    if (encounterResource.hasParticipant()) {
      taskResource.setRequester(encounterResource.getParticipantFirstRep().getIndividual());
      taskResource.setOwner(encounterResource.getParticipantFirstRep().getIndividual());
    }

    // BasedOn - the ServiceRequest this task fulfils
    taskResource.addBasedOn(new Reference(serviceRequestEntry.getFullUrl()));

    // Focus - same ServiceRequest
    taskResource.setFocus(new Reference(serviceRequestEntry.getFullUrl()));

    String taskId = UUID.nameUUIDFromBytes(
        ("task-" + serviceRequestEntry.getResource().getId()
            + "-" + taskStart).getBytes()).toString();
    return newEntry(bundle, taskResource, taskId);
  }

  /**
   * Helper function to create an Entry for the given Resource within the given Bundle. Sets the
   * resourceID to the given ID, sets the entry's fullURL to that resourceID, and adds the entry to
   * the bundle.
   *
   * @param bundle   The Bundle to add the Entry to
   * @param resource Resource the new Entry should contain
   * @param resourceID The Resource ID to assign
   * @return the created Entry
   */
  private static BundleEntryComponent newEntry(Bundle bundle, Resource resource,
      String resourceID) {
    BundleEntryComponent entry = bundle.addEntry();

    resource.setId(resourceID);
    entry.setFullUrl(getUrlPrefix(resource.fhirType()) + resourceID);
    entry.setResource(resource);

    if (TRANSACTION_BUNDLE) {
      BundleEntryRequestComponent request = entry.getRequest();
      request.setMethod(HTTPVerb.POST);
      String resourceType = resource.getResourceType().name();
      request.setUrl(resourceType);
      if (ExportHelper.UNDUPLICATED_FHIR_RESOURCES.contains(resourceType)) {
        Property prop = entry.getResource().getNamedProperty("identifier");
        if (prop != null && prop.getValues().size() > 0) {
          Identifier identifier = (Identifier)prop.getValues().get(0);
          request.setIfNoneExist(
              "identifier=" + identifier.getSystem() + "|" + identifier.getValue());
        }
      }
      entry.setRequest(request);
    }

    return entry;
  }

  /**
   * Find a Condition resource whose primary code matches the provided code.
   * The BundleEntryComponent will be returned to allow for references.
   * @param bundle Bundle to find a resource in
   * @param code Code to find
   * @return entry for the matching Condition, or null if none is found
   */
  private static BundleEntryComponent findConditionResourceByCode(Bundle bundle, String code) {
    for (BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource().fhirType().equals("Condition")) {
        Condition condition = (Condition) entry.getResource();
        Coding coding = condition.getCode().getCoding().get(0); // Only one element in list
        if (code.equals(coding.getCode())) {
          return entry;
        }
      }
    }
    return null;
  }

  /**
   * Return either "[resourceType]/" or "urn:uuid:" as appropriate.
   * @param resourceType The resource type being referenced.
   * @return "[resourceType]/" or "urn:uuid:"
   */
  protected static String getUrlPrefix(String resourceType) {
    if (Config.getAsBoolean("exporter.fhir.bulk_data")) {
      return resourceType + "/";
    } else {
      return "urn:uuid:";
    }
  }
}
