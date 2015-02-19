package org.freeshr;

import org.apache.commons.lang.StringUtils;
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
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.StringReader;

public class Mapper {
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
            bundleEle.appendChild(createEntry(document, extractElementAndAddId(document, feed, i)));
        }

        document.removeChild(feed);
        document.appendChild(bundleEle);

        serialize(deserialize(document));
        printXml(document);
    }


    private Element getComposition(Document document, Element feed) {
        Element composition = extractElementAndAddId(document, feed, 0);

        Element author = document.createElement("author");
        author.appendChild(createTagWithValue(document, "reference", "Organization/f001"));
        author.appendChild(createTagWithValue(document, "display", "BMC"));
        composition.insertBefore(author, getNodeAsElement(composition, "encounter", 0));

        Element confidentiality = document.createElement("confidentiality");
        confidentiality.appendChild(createTagWithValue(document, "system", "http://hl7.org/fhir/v3/Confidentiality"));
        confidentiality.appendChild(createTagWithValue(document, "code", "L"));
        confidentiality.appendChild(createTagWithValue(document, "display", "low"));
        composition.insertBefore(confidentiality, author);

        composition.insertBefore(createType(document, "http://hl7.org/fhir/vs/doc-codes", "11488-4"), confidentiality);

        return composition;
    }

    private Element getEncounter(Document document, Element feed) {
        Element encounter = extractElementAndAddId(document, feed, 1);
        Node subject = encounter.getElementsByTagName("subject").item(0);
        document.renameNode(subject, subject.getNamespaceURI(), "patient");

        Element indication = getNodeAsElement(encounter, "indication", 0);
        encounter.removeChild(indication);

        return encounter;
    }

    private Element extractElementAndAddId(Document document, Element feed, int resourceIndex) {
        Element entry = getNodeAsElement(feed, "entry", resourceIndex);
        Element resource = getContent(entry);
        Element id = getNodeAsElement(entry, "id", 0);

        Element identifier = getNodeAsElement(resource, "identifier", 0);

        resource.insertBefore(createTagWithValue(document, "id", id.getTextContent()), identifier);
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
        return (Element) parentElement.getElementsByTagName(childName).item(index);
    }
    
    private Element getContent(Element entry){
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
        return createTagWithValue(document, "id", StringUtils.substringAfterLast(id.getTextContent(), "/"));
    }

    private Element createEntry(Document document, Element resourceContent) {
        Element entry = document.createElement("entry");
        Element resource = document.createElement("resource");
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
        new XmlParser(true).compose(os, bundle);
    }

    private Bundle deserialize(Document document) throws TransformerException {
        return new BundleDeserializer().deserialize(getXml(document));
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
