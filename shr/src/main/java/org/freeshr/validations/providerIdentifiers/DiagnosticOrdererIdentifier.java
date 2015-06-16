package org.freeshr.validations.providerIdentifiers;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.DiagnosticOrder;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.Reference;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DiagnosticOrdererIdentifier extends ClinicalResourceProviderIdentifier {

    @Override
    protected boolean validates(Resource resource) {
        return (resource instanceof DiagnosticOrder);
    }

    @Override
    protected List<String> extractUrls(Resource resource) {
        Reference orderer = ((DiagnosticOrder) resource).getOrderer();
        String url = null;
        if (orderer != null) {
            url = orderer.getReference() == null ? StringUtils.EMPTY : orderer.getReference();
        }
        return url == null ? null : Arrays.asList(url);
    }
}


