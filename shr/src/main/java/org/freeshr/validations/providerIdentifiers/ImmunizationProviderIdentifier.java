package org.freeshr.validations.providerIdentifiers;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.Immunization;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.Reference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ImmunizationProviderIdentifier extends ClinicalResourceProviderIdentifier {

    @Override
    protected boolean validates(Resource resource) {
        return (resource instanceof Immunization);
    }

    @Override
    protected List<String> extractUrls(Resource resource) {
        List<String> urls = new ArrayList<>();

        Reference requester = ((Immunization) resource).getRequester();
        String requesterUrl = null;
        if (requester != null) {
            requesterUrl = requester.getReference() == null ? StringUtils.EMPTY : requester.getReference();
        }

        Reference performer = ((Immunization) resource).getPerformer();
        String performerUrl = null;
        if (performer != null) {
            performerUrl = performer.getReference() == null ? StringUtils.EMPTY : performer.getReference();
        }

        if (requesterUrl != null) {
            urls.add(requesterUrl);
        }
        if (performerUrl != null) {
            urls.add(performerUrl);
        }
        return urls;
    }
}
