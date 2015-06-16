package org.freeshr.validations;

import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.freeshr.application.fhir.ValidationErrorType.STRUCTURE;
import static org.freeshr.validations.ValidationMessages.FEED_MUST_HAVE_COMPOSITION_WITH_ENCOUNTER;

@Component
public class StructureValidator implements Validator<Bundle> {
    @Override
    public List<ValidationMessage> validate(Bundle bundle) {
        List<ValidationMessage> validationMessages = new ArrayList<>();

        BundleEntryComponent compositionEntry = getCompositionEntry(bundle.getEntry());

        if(!hasEncounterEntry(compositionEntry)) {
            validationMessages.add(new ValidationMessage(null, STRUCTURE, "Feed",
                    FEED_MUST_HAVE_COMPOSITION_WITH_ENCOUNTER, OperationOutcome.IssueSeverity.ERROR));
            return validationMessages;
        }

        List<String> compositionSectionIds = identifySectionIdsFromComposition(compositionEntry);
        List<String> entryReferenceIds = verifyEntryReferenceIds(bundle.getEntry(), compositionSectionIds, validationMessages);
        compositionSectionIds.removeAll(entryReferenceIds);

        //Add error for each section with no entry.
        for (String entryReferenceId : compositionSectionIds) {

            validationMessages.add(new ValidationMessage(null, STRUCTURE, entryReferenceId, String
                    .format
                            ("No entry present" +
                                    " for the section with id %s", entryReferenceId), OperationOutcome.IssueSeverity.ERROR));
        }

        return validationMessages;
    }

    private boolean hasEncounterEntry(BundleEntryComponent compositionEntry) {
        return !((Composition)compositionEntry.getResource()).getEncounter().isEmpty();
    }

    private List<String> verifyEntryReferenceIds(List<BundleEntryComponent> entryList,
                                                 List<String> compositionSectionIds,
                                                 List<ValidationMessage> validationMessages) {
        List<String> resourceIds = new ArrayList<>();

        for (BundleEntryComponent atomEntry : entryList) {
            if (isSectionEntryRequiredInComposition(atomEntry)) {
                String id = atomEntry.getResource().getId();
                resourceIds.add(id);

                if (compositionSectionIds.contains(id)) continue;

                validationMessages.add(new ValidationMessage(null, STRUCTURE, id, String.format
                        ("Entry with id %s " +
                                        "is not present in the composition section list.",
                                id), OperationOutcome.IssueSeverity.ERROR));
            }
        }
        return resourceIds;
    }

    private boolean isSectionEntryRequiredInComposition(BundleEntryComponent entry) {
        List<ResourceType> resourceTypes = Arrays.asList(ResourceType.Composition, ResourceType.Encounter);
        return !resourceTypes.contains(entry.getResource().getResourceType());
    }

    private List<String> identifySectionIdsFromComposition(BundleEntryComponent compositionEntry) {
        List<Base> sections = compositionEntry.getResource().getChildByName("section").getValues();
        List<String> compositionSectionList = new ArrayList<>();
        for (Base section : sections) {
            Reference sectionContent = ((Composition.SectionComponent) section).getContent();
            String referenceId = sectionContent.getReference();
            compositionSectionList.add(referenceId);
        }
        return compositionSectionList;
    }

    private BundleEntryComponent getCompositionEntry(List<BundleEntryComponent> entryList) {
        for (BundleEntryComponent entry : entryList) {
            Resource resource = entry.getResource();
            if (resource.getResourceType().equals(ResourceType.Composition)) {
                return entry;
            }
        }
        return null;
    }

}

