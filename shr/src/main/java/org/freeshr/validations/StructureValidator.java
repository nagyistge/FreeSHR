package org.freeshr.validations;

import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.freeshr.domain.ErrorMessageBuilder.FEED_MUST_HAVE_COMPOSITION;
import static org.freeshr.domain.ErrorMessageBuilder.buildValidationMessage;

@Component
public class StructureValidator implements Validator<Bundle> {
    @Override
    public List<ValidationMessage> validate(Bundle bundle) {
        List<ValidationMessage> validationMessages = new ArrayList<>();

        BundleEntryComponent compositionEntry = hasCompositionWithEncounter(bundle.getEntry());

        if (compositionEntry == null) {
            validationMessages.add(buildValidationMessage("Feed", ResourceValidator.INVALID, FEED_MUST_HAVE_COMPOSITION, OperationOutcome.IssueSeverity.ERROR));
            return validationMessages;
        }

        List<String> compositionSectionIds = identifySectionIdsFromComposition(compositionEntry);
        List<String> entryReferenceIds = verifyEntryReferenceIds(bundle.getEntry(), compositionSectionIds, validationMessages);
        compositionSectionIds.removeAll(entryReferenceIds);

        //Add error for each section with no entry.
        for (String entryReferenceId : compositionSectionIds) {
            validationMessages.add(buildValidationMessage(entryReferenceId, ResourceValidator.INVALID, String.format("No entry present for the section with id %s", entryReferenceId), OperationOutcome.IssueSeverity.ERROR));
        }

        return validationMessages;
    }

    private List<String> verifyEntryReferenceIds(List<BundleEntryComponent> entryList,
                                                 List<String> compositionSectionIds,
                                                 List<ValidationMessage> validationMessages) {
        List<String> resourceDetailsList = new ArrayList<>();

        for (BundleEntryComponent atomEntry : entryList) {
            if (!atomEntry.getResource().getResourceType().equals(ResourceType.Composition)) {
                String identifier = ((Identifier) atomEntry.getResource().getChildByName("identifier").getValues()
                        .get(0)).getValue();
                resourceDetailsList.add(identifier);

                if (compositionSectionIds.contains(identifier)) continue;

                validationMessages.add(buildValidationMessage(identifier, ResourceValidator.INVALID, String.format("Entry with id %s is not present in the composition section list.",
                        identifier), OperationOutcome.IssueSeverity.ERROR));
            }
        }
        return resourceDetailsList;
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

    private BundleEntryComponent hasCompositionWithEncounter(List<BundleEntryComponent> entryList) {
        BundleEntryComponent compositionEntry = null;
        for (BundleEntryComponent entry : entryList) {
            Resource resource = entry.getResource();
            if (resource.getResourceType().equals(ResourceType.Composition)) {
                compositionEntry = resource.getChildByName("encounter").hasValues() ? entry : null;
                break;
            }
        }
        return compositionEntry;
    }

}

