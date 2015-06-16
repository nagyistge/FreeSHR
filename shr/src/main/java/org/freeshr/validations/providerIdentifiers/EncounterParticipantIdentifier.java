package org.freeshr.validations.providerIdentifiers;

import org.apache.commons.lang3.StringUtils;
import org.freeshr.utils.CollectionUtils;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Resource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class EncounterParticipantIdentifier extends ClinicalResourceProviderIdentifier {

    @Override
    protected boolean validates(Resource resource) {
        return (resource instanceof Encounter);
    }

    @Override
    protected List<String> extractUrls(Resource resource) {
        List<Encounter.EncounterParticipantComponent> participants = ((Encounter) resource).getParticipant();
        String url = null;
        if (!CollectionUtils.isEmpty(participants)) {
            url = participants.get(0).getIndividual().getReference();
            url = url == null ? StringUtils.EMPTY : url;
        }
        return url == null ? null : Arrays.asList(url);
    }
}
