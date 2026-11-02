package com.gingy.repaso.gr.service;

import com.gingy.repaso.gr.domain.DictionaryEntry;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reads a FreeDict TEI-P5 dictionary file (for example freedict-ell-spa-*.tei,
 * which is generated from WikDict) and turns it into Greek/Spanish pairs.
 *
 * Each entry has a Greek headword in form/orth and Spanish translations in
 * sense/cit[@type='trans'][@xml:lang='es']/quote. A quote can hold several
 * translations separated by ",", ";" or "/", so each one becomes its own pair.
 */
@Component
public class TeiDictionaryParser {

    private static final String TEI_NS = "http://www.tei-c.org/ns/1.0";
    private static final String XML_NS = "http://www.w3.org/XML/1998/namespace";
    private static final Pattern TRANSLATION_SEPARATORS = Pattern.compile("[,;/]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /** The dictionary source label (stored on every row) and its pairs. */
    public record ParsedDictionary(String source, List<DictionaryEntry> entries) {
    }

    public ParsedDictionary parse(Path file) {
        Document doc = readDocument(file);
        Element root = doc.getDocumentElement();

        String source = "FreeDict+WikDict ell-spa " + editionOf(root);

        List<DictionaryEntry> entries = new ArrayList<>();
        NodeList entryNodes = doc.getElementsByTagNameNS(TEI_NS, "entry");
        for (int i = 0; i < entryNodes.getLength(); i++) {
            Element entry = (Element) entryNodes.item(i);
            String greek = clean(textOf(child(child(entry, "form"), "orth")));
            if (greek.isEmpty()) {
                continue;
            }
            for (String spanish : spanishTranslations(entry)) {
                entries.add(new DictionaryEntry(source, greek, spanish));
            }
        }
        return new ParsedDictionary(source, entries);
    }

    /** Spanish translations of one entry, de-duplicated, in document order. */
    private Set<String> spanishTranslations(Element entry) {
        Set<String> translations = new LinkedHashSet<>();
        NodeList cits = entry.getElementsByTagNameNS(TEI_NS, "cit");
        for (int i = 0; i < cits.getLength(); i++) {
            Element cit = (Element) cits.item(i);
            boolean isSpanishTranslation = "trans".equals(cit.getAttribute("type"))
                    && "es".equals(cit.getAttributeNS(XML_NS, "lang"));
            if (!isSpanishTranslation) {
                continue;
            }
            for (Element quote : childElements(cit, "quote")) {
                for (String part : TRANSLATION_SEPARATORS.split(textOf(quote))) {
                    String translation = clean(part);
                    if (!translation.isEmpty()) {
                        translations.add(translation);
                    }
                }
            }
        }
        return translations;
    }

    private String editionOf(Element root) {
        Element edition = child(child(child(child(root, "teiHeader"), "fileDesc"), "editionStmt"), "edition");
        String value = clean(textOf(edition));
        return value.isEmpty() ? "unknown" : value;
    }

    private Document readDocument(Path file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            // Do not fetch the DTD referenced by the file, and do not resolve external entities.
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return factory.newDocumentBuilder().parse(file.toFile());
        } catch (IOException | SAXException | javax.xml.parsers.ParserConfigurationException e) {
            throw new IllegalArgumentException("Cannot read TEI dictionary file: " + file, e);
        }
    }

    private static Element child(Element parent, String localName) {
        if (parent == null) {
            return null;
        }
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && localName.equals(e.getLocalName())) {
                return e;
            }
        }
        return null;
    }

    private static List<Element> childElements(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && localName.equals(e.getLocalName())) {
                result.add(e);
            }
        }
        return result;
    }

    private static String textOf(Node node) {
        return node == null ? "" : node.getTextContent();
    }

    private static String clean(String s) {
        return WHITESPACE.matcher(s.trim()).replaceAll(" ");
    }
}
