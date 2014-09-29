package org.freeshr.application.fhir;


import org.springframework.stereotype.Component;

@Component
public class FhirValidator {

//    private FhirMessageFilter fhirMessageFilter;
//    private TRConceptLocator trConceptLocator;
//    private SHRProperties shrProperties;
//
//    @Autowired
//    public FhirValidator(TRConceptLocator trConceptLocator, SHRProperties shrProperties, FhirMessageFilter fhirMessageFilter) {
//        this.trConceptLocator = trConceptLocator;
//        this.shrProperties = shrProperties;
//        this.fhirMessageFilter = fhirMessageFilter;
//    }
//
//    public EncounterValidationResponse validate(String sourceXML) {
//        try {
//            return validate(sourceXML, shrProperties.getValidationFilePath());
//        } catch (URISyntaxException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private EncounterValidationResponse validate(String sourceXml, String definitionsZipPath) {
//        List<ValidationMessage> outputs = new ArrayList<>();
//        outputs.addAll(validateDocument(definitionsZipPath, sourceXml));
//        return fhirMessageFilter.filterMessagesSevereThan(outputs, OperationOutcome.IssueSeverity.warning);
//    }
//
//    private List<ValidationMessage> validateDocument(String definitionsZipPath, String sourceXml) {
//        try {
//            return new InstanceValidator(definitionsZipPath, null, trConceptLocator).validateInstance(document(sourceXml).getDocumentElement());
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private Document document(String sourceXml) throws ParserConfigurationException, SAXException, IOException {
//        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        factory.setNamespaceAware(true);
//        factory.setValidating(false);
//        DocumentBuilder builder = factory.newDocumentBuilder();
//        return builder.parse(new ByteArrayInputStream(sourceXml.getBytes()));
//    }
}
