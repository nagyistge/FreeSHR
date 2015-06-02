package org.freeshr.validations.providerIdentifiers;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.Condition;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.Reference;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ConditionAsserterIdentifier extends ClinicalResourceProviderIdentifier {

    @Override
    protected boolean validates(Resource resource) {
        return (resource instanceof Condition);
    }

    @Override
    protected List<String> extractUrls(Resource resource) {
        Reference asserter = ((Condition) resource).getAsserter();
        String url = null;
        if (asserter != null) {
            url = asserter.getReference() == null ? StringUtils.EMPTY : asserter.getReference();
        }
        return url == null ? null : Arrays.asList(url);
    }
}
