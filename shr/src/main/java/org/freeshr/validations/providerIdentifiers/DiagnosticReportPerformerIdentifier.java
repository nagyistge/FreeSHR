package org.freeshr.validations.providerIdentifiers;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.DiagnosticReport;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.Reference;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DiagnosticReportPerformerIdentifier extends ClinicalResourceProviderIdentifier {
    @Override
    protected boolean validates(Resource resource) {
        return (resource instanceof DiagnosticReport);
    }

    @Override
    protected List<String> extractUrls(Resource resource) {
        Reference performer = ((DiagnosticReport) resource).getPerformer();
        String url = null;
        if (performer != null) {
            url = performer.getReference() == null ? StringUtils.EMPTY : performer.getReference();
        }
        return url == null ? null : Arrays.asList(url);
    }
}


