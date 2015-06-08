package org.freeshr;

import org.apache.commons.lang3.StringUtils;
import org.freeshr.utils.BundleDeserializer;
import org.freeshr.utils.FileUtil;
import org.hl7.fhir.instance.formats.XmlParser;
import org.hl7.fhir.instance.model.Bundle;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

public class Mapper {
    public static List<String> resourceThatReadsSubject = Arrays.asList("Composition", "Observation", "DiagnosticOrder", "DiagnosticReport", "Specimen");

    @Test
    public void parse() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        DocumentBuilder documentBuilder = dbf.newDocumentBuilder();

        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(FileUtil.asString("input.xml")));
        Document document = documentBuilder.parse(is);

        Element feed = (Element) document.getElementsByTagName("feed").item(0);

        Element bundleEle = document.createElement("Bundle");
        bundleEle.setAttribute("xmlns", "http://hl7.org/fhir");

        Element id = getIdForBundle(document, feed);
        bundleEle.appendChild(id);

        Element metaUpdated = getMetaUpdated(document, feed);
        bundleEle.appendChild(metaUpdated);

        bundleEle.appendChild(createTagWithValue(document, "type", "document"));

        bundleEle.appendChild(createEntry(document, getComposition(document, feed)));
        bundleEle.appendChild(createEntry(document, getEncounter(document, feed)));

        NodeList entries = document.getElementsByTagName("entry");

        for (int i = 2; i < entries.getLength(); i++) {
            bundleEle.appendChild(createEntry(document, processResource(document, feed, i)));
        }

        document.removeChild(feed);
        document.appendChild(bundleEle);

        String xml = removeUnwantedAttributes(document);
        serialize(deserialize(xml));
    }

    private String removeUnwantedAttributes(Document document) throws TransformerException {
        String xml = getXml(document);
        return StringUtils.replace(xml, " xmlns=\"\"", StringUtils.EMPTY);
    }


    private Element getComposition(Document document, Element feed) {
        Element composition = extractResourceAndAddId(document, feed, 0);

        Element author = document.createElement("author");
        author.appendChild(createTagWithValue(document, "reference", "Organization/f001"));
        author.appendChild(createTagWithValue(document, "display", "BMC"));
        composition.insertBefore(author, getNodeAsElement(composition, "encounter", 0));

        Element confidentialityElement = getNodeAsElement(composition, "confidentiality", 0);
        String confidentialityValue = "N";
        if(confidentialityElement != null) {
            Element code = getNodeAsElement(confidentialityElement, "code", 0);
            confidentialityValue = code.getAttribute("value");
            composition.removeChild(confidentialityElement);
        }
        Element confidentiality = document.createElement("confidentiality");
        confidentiality.setAttribute("value", confidentialityValue);
        composition.insertBefore(confidentiality, author);

        composition.insertBefore(createType(document, "http://hl7.org/fhir/vs/doc-codes", "11488-4"), confidentiality);

        return composition;
    }

    private Element getEncounter(Document document, Element feed) {
        Element encounter = processResource(document, feed, 1);

        Element indication = getNodeAsElement(encounter, "indication", 0);
        encounter.removeChild(indication);

        return encounter;
    }

    private Element processResource(Document document, Element feed, int resourceIndex) {
        Element resource = extractResourceAndAddId(document, feed, resourceIndex);
        String resourceName = resource.getTagName();
        if (!resourceThatReadsSubject.contains(resourceName))
            renameNode(document, resource, "subject", "patient");
        if ("Observation".equals(resourceName)) {
            renameNode(document, resource, "name", "code");
        }
        if ("Condition".equals(resourceName)) {
            renameNode(document, resource, "status", "clinicalStatus");
            Element dateAsserted = getNodeAsElement(resource, "dateAsserted", 0);
            if(dateAsserted != null) {
                String dateAssertedValue = dateAsserted.getAttribute("value");
                dateAsserted.removeAttribute("value");
                dateAsserted.setAttribute("value", dateAssertedValue.replace(dateAssertedValue, StringUtils.substringBefore(dateAssertedValue, "T")));
            }
        }
        if ("Immunization".equals(resourceName)) {
            renameNode(document, resource, "refusedIndicator", "wasNotGiven");
            renameNode(document, resource, "refusalReason", "reasonNotGiven");
        }
        if ("Procedure".equals(resourceName)) {
            renameNode(document, resource, "date", "performedPeriod");
            convertAttributeToChildWithTagName(document, resource, "outcome", "value", "text");
            convertAttributeToChildWithTagName(document, resource, "followUp", "value", "text");

        }
        return resource;
    }

    private void convertAttributeToChildWithTagName(Document document, Element resource, String elementName, String attribute, String childTagName) {
        Element outcome = getNodeAsElement(resource, elementName, 0);
        String value = outcome.getAttribute(attribute);
        outcome.removeAttribute(attribute);
        outcome.appendChild(createTagWithValue(document, childTagName, value));
    }

    private Element extractResourceAndAddId(Document document, Element feed, int resourceIndex) {
        Element entry = getNodeAsElement(feed, "entry", resourceIndex);
        Element resource = getContent(entry);
        Element id = getNodeAsElement(entry, "id", 0);

        Element identifier = getNodeAsElement(resource, "identifier", 0);

        resource.insertBefore(createTagWithValue(document, "id", id.getTextContent()), identifier);
        return resource;
    }

    private Element renameNode(Document document, Element resource, String from, String patient) {
        Element subject = getNodeAsElement(resource, from, 0);
        if(subject != null) {
            document.renameNode(subject, subject.getNamespaceURI(), patient);
        }
        return resource;
    }

    private Element createType(Document doc, String system, String code) {
        Element type = doc.createElement("type");
        type.appendChild(getCoding(doc, system, code));
        return type;
    }

    private Element getCoding(Document doc, String system, String code) {
        Element coding = doc.createElement("coding");
        coding.appendChild(createTagWithValue(doc, "system", system));
        coding.appendChild(createTagWithValue(doc, "code", code));
        return coding;
    }

    private Element getNodeAsElement(Element parentElement, String childName, int index) {
        NodeList elementsByTagName = getNodeList(parentElement, childName);
        if(elementsByTagName.getLength() > 0) {
            return (Element) elementsByTagName.item(index);
        }
        return null;
    }

    private NodeList getNodeList(Element parentElement, String childName) {
        return parentElement.getElementsByTagName(childName);
    }

    private Element getContent(Element entry) {
        return (Element) entry.getElementsByTagName("content").item(0).getChildNodes().item(1);

    }

    private Element getMetaUpdated(Document document, Element feed) {
        Node updated = feed.getElementsByTagName("updated").item(0);
        Element meta = document.createElement("meta");
        meta.appendChild(createTagWithValue(document, "lastUpdated", updated.getTextContent()));
        return meta;
    }

    private Element getIdForBundle(Document document, Element feed) {
        Node id = feed.getElementsByTagName("id").item(0);
        return createTagWithValue(document, "id", id.getTextContent());
    }

    private Element createEntry(Document document, Element resourceContent) {
        Element entry = document.createElement("entry");
        Element resource = document.createElement("resource");

        Element id = getNodeAsElement(resourceContent, "id", 0);
        entry.setAttribute("id", id.getAttribute("value"));

        resource.appendChild(resourceContent);
        entry.appendChild(resource);
        return entry;

    }

    private Element createTagWithValue(Document doc, String name, String value) {
        Element element = doc.createElement(name);
        element.setAttribute("value", value);
        return element;
    }

    private void printXml(Document document) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(document);
        StreamResult console = new StreamResult(System.out);
        transformer.transform(source, console);
    }

    private void serialize(Bundle bundle) throws Exception {
        FileOutputStream os = new FileOutputStream("output.xml");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        new XmlParser(false).compose(byteArrayOutputStream, bundle, true);
        os.write(byteArrayOutputStream.toByteArray());
    }

    private Bundle deserialize(String xml) throws Exception {
        return new BundleDeserializer().deserialize(xml);
    }

    private String getXml(Document doc) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Result res = new StreamResult(os);
        transformer.transform(source, res);
        return os.toString();
    }
}
