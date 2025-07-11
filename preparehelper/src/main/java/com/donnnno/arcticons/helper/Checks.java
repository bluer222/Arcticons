package com.donnnno.arcticons.helper;

import static java.lang.System.exit;
import static java.lang.System.getProperty;
import static java.lang.System.out;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class Checks {

    public static void main(String[] args) {
        String rootDir = getProperty("user.dir");
        Path rootPath = Paths.get(rootDir);
        String rootDirName = rootPath.getFileName().toString();
        if (rootDirName.equals("preparehelper")) {
            rootDir = "..";
        }
        String valuesDir = rootDir + "/app/src/main/res/values";
        String appFilter = rootDir + "/newicons/appfilter.xml";
        String changelogXml = valuesDir + "/changelog.xml";
        String generatedDir = rootDir + "/generated";
        String sourceDir = rootDir + "/icons/white"; // Assuming this is where your main icons are
        String newIconsDir = rootDir + "/newicons"; // Assuming this is for new or additional icons

        int check = 0;
        check += (checkXml(appFilter) ? 1 : 0);
        check += (missingDrawable(appFilter, sourceDir, newIconsDir) ? 1 : 0);
        check += (duplicateEntry(appFilter) ? 1 : 0);

        // Process icons in both directories
        check += (checkSVG(sourceDir) ? 1 : 0);
        check += (checkSVG(newIconsDir) ? 1 : 0);

        if (check != 0) {
            System.out.printf("Exiting program because %d checks failed.%n", check);
            exit(0);
        } else {
            System.out.println("All checks passed. SVG stroke widths updated.");
        }
    }

    public static void startChecks(String appFilter, String sourceDir, String newIconsDir) {
        int check = 0;
        check += (checkXml(appFilter) ? 1 : 0);
        check += (missingDrawable(appFilter, sourceDir, newIconsDir) ? 1 : 0);
        check += (duplicateEntry(appFilter) ? 1 : 0);
        check += (checkSVG(newIconsDir) ? 1 : 0);
        check += (checkSVG(sourceDir) ? 1 : 0);

        if (check != 0) {
            System.out.printf("Exiting program because %d checks failed.%n", check);
            exit(0);
        }
    }

    public static boolean checkXml(String path) {
        List<String> defect = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String line;
            Pattern pattern = Pattern.compile("(()|(<(item|calendar) component=\"(ComponentInfo\\{.*/.*}|:[A-Z_]*)\" (drawable|prefix)=\".*\"\\s?/>)|(^\\s*$)|(</?resources>)|(<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>))");
            while ((line = br.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (!matcher.find()) {
                    defect.add(line);
                }
            }
            if (!defect.isEmpty()) {
                out.println("\n\n______ Found defect appfilter entries ______\n\n");
                for (String defectLine : defect) {
                    out.println(defectLine);
                }
                out.println("\n\n____ Please check these first before proceeding ____\n\n");
                //return true;
            }

        } catch (IOException e) {
            out.println("Error reading file: " + e.getMessage());
        }
        return false;
    }

    public static boolean duplicateEntry(String path) {
        List<String> components = new ArrayList<>();

        try {
            File inputFile = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("item");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element item = (Element) node;
                    if (item.getAttribute("prefix").isEmpty()) {
                        String component = item.getAttribute("component");
                        components.add(component);
                    }
                }
            }

            Set<String> duplicates = new HashSet<>();
            Set<String> seen = new HashSet<>();
            for (String component : components) {
                if (!seen.add(component)) {
                    duplicates.add(component);
                }
            }

            if (!duplicates.isEmpty()) {
                out.println("\n\n______ Found duplicate appfilter entries ______\n\n");
                for (String duplicate : duplicates) {
                    out.println("\t" + duplicate);
                }
                out.println("\n\n____ Please check these first before proceeding ____\n\n");
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean missingDrawable(String appfilterPath, String whiteDir, String otherDir) {
        List<Element> missingDrawables = new ArrayList<>();
        try {
            File inputFile = new File(appfilterPath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("item");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element item = (Element) node;
                    if (item.getAttribute("prefix").isEmpty()) {
                        String drawable = item.getAttribute("drawable");
                        Path whitePath = Paths.get(whiteDir, drawable + ".svg");
                        Path otherPath = Paths.get(otherDir, drawable + ".svg");
                        if (!Files.exists(whitePath) && !Files.exists(otherPath)) {
                            missingDrawables.add(item);
                        }
                    }
                }
            }

            if (!missingDrawables.isEmpty()) {
                out.println("\n\n______ Found non existent drawables ______\n");
                out.println("Possible causes are typos or completely different naming of the icon\n\n");
                for (Element item : missingDrawables) {
                    String itemString = convertElementToString(item);
                    out.println(itemString);
                }
                out.println("\n\n____ Please check these first before proceeding ____\n\n");
                return true;
            }

        } catch (Exception e) {
            out.println("Error occurred: " + e.getMessage());
        }
        return false;
    }

    private static String convertElementToString(Element element) {
        try {
            StringWriter writer = new StringWriter();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(element), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            out.println("Error occurred: " + e.getMessage());
            return "";
        }
    }

    public static boolean checkSVG(String dir) {
        Map<String, List<String>> strokeAttr = new HashMap<>();

        try {
            File folder = new File(dir);
            System.out.println("Processing SVG files in directory: " + folder.getAbsolutePath());
            File[] files = folder.listFiles((dir1, name) -> name.endsWith(".svg"));
            if (files != null) {
                System.out.println("Found " + files.length + " SVG files.");
                for (File file : files) {
                    String fileName = file.getName();
                    System.out.println("Processing file: " + fileName);
                    String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                    boolean modified = false; // Flag to track if content was modified

                    // 1. Handle explicit stroke-width attributes
                    Pattern explicitStrokeWidthPattern = Pattern.compile("(?<strokestr>stroke-width(?:=\"|: ?))(?<number>\\d*(?:.\\d+)?)(?=[p\"; }/])");
                    Matcher explicitStrokeWidthMatcher = explicitStrokeWidthPattern.matcher(content);
                    StringBuilder result = new StringBuilder();
                    while (explicitStrokeWidthMatcher.find()) {
                        String replacement = replaceStroke(explicitStrokeWidthMatcher);
                        explicitStrokeWidthMatcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                        modified = true;
                    }
                    explicitStrokeWidthMatcher.appendTail(result);
                    content = result.toString();

                    // 2. Handle stroke-width within <style> blocks (CSS)
                    // This pattern captures the entire <style> block content.
                    Pattern styleBlockPattern = Pattern.compile("<style>(.*?)</style>", Pattern.DOTALL);
                    Matcher styleBlockMatcher = styleBlockPattern.matcher(content);
                    StringBuilder updatedContentBuilder = new StringBuilder();

                    boolean styleBlockFound = false;
                    while (styleBlockMatcher.find()) {
                        styleBlockFound = true;
                        String styleContent = styleBlockMatcher.group(1); // The content inside <style>
                        String modifiedStyleContent = styleContent;

                        // Regex to find any CSS rule (selector { properties })
                        Pattern anyCssRulePattern = Pattern.compile("([^\\{]+?\\{.*?\\})", Pattern.DOTALL); // Captures selector + block
                        Matcher anyCssRuleMatcher = anyCssRulePattern.matcher(styleContent);
                        StringBuilder tempStyleRulesBuilder = new StringBuilder();
                        boolean ruleContentModified = false;

                        while(anyCssRuleMatcher.find()){
                            String fullRule = anyCssRuleMatcher.group(0); // e.g., ".cls-1{fill:none;stroke:#fff;stroke-linecap:round;stroke-linejoin:round}"

                            // Extract the properties part (content inside the braces)
                            Pattern propertiesPattern = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL);
                            Matcher propertiesMatcher = propertiesPattern.matcher(fullRule);
                            String propertiesContent = "";
                            if (propertiesMatcher.find()) {
                                propertiesContent = propertiesMatcher.group(1);
                            } else {
                                // Should not happen if fullRule came from anyCssRuleMatcher
                                anyCssRuleMatcher.appendReplacement(tempStyleRulesBuilder, Matcher.quoteReplacement(fullRule));
                                continue;
                            }

                            String newPropertiesContent = propertiesContent.trim(); // Trim to handle whitespace

                            // First, add a semicolon if the last property doesn't have one
                            if (!newPropertiesContent.isEmpty() && !newPropertiesContent.endsWith(";")) {
                                newPropertiesContent += ";";
                                ruleContentModified = true;
                            }

                            // Now, apply the stroke-width logic
                            if (newPropertiesContent.contains("stroke-width:")) {
                                // Replace existing stroke-width with 2
                                newPropertiesContent = newPropertiesContent.replaceAll("stroke-width:\\s*\\d*\\.?\\d*(px)?;?", "stroke-width:2;");
                                ruleContentModified = true;
                            } else {
                                // Add stroke-width:2; if not present
                                if (newPropertiesContent.contains("stroke:")) {
                                    // Insert stroke-width after stroke property or at end of properties
                                    // This regex finds 'stroke:...' and inserts after its semicolon, or before the last char if no semicolon
                                    Pattern strokePropInProperties = Pattern.compile("stroke:[^;]+;?");
                                    Matcher strokePropMatcher = strokePropInProperties.matcher(newPropertiesContent);
                                    if (strokePropMatcher.find()) {
                                        String matchedStrokeProp = strokePropMatcher.group(0);
                                        if (matchedStrokeProp.endsWith(";")) {
                                            newPropertiesContent = newPropertiesContent.replace(matchedStrokeProp, matchedStrokeProp + "stroke-width:2;");
                                        } else {
                                            newPropertiesContent = newPropertiesContent.replace(matchedStrokeProp, matchedStrokeProp + ";stroke-width:2;");
                                        }
                                    } else {
                                        // Fallback if stroke is present but not matched by above (e.g. no semicolon)
                                        // Try to insert before the last semicolon or at the end
                                        if (!newPropertiesContent.isEmpty()) {
                                            // Find last semicolon and insert before it
                                            int lastSemicolon = newPropertiesContent.lastIndexOf(';');
                                            if (lastSemicolon != -1) {
                                                newPropertiesContent = newPropertiesContent.substring(0, lastSemicolon) + "stroke-width:2;" + newPropertiesContent.substring(lastSemicolon);
                                            } else {
                                                // No semicolons at all, just append (should have been caught by the first semicolon fix)
                                                newPropertiesContent += "stroke-width:2;";
                                            }
                                        } else {
                                            newPropertiesContent += "stroke-width:2;"; // For empty rule blocks
                                        }
                                    }
                                    ruleContentModified = true;
                                } else {
                                    // If 'stroke' property is also missing, add both stroke and stroke-width.
                                    // Insert at the beginning of the properties content
                                    newPropertiesContent = "stroke:#fff;stroke-width:2;" + newPropertiesContent;
                                    ruleContentModified = true;
                                }
                            }

                            // Reassemble the full rule
                            String updatedFullRule = fullRule.replaceFirst("\\{.*?\\}", "{" + newPropertiesContent + "}");
                            anyCssRuleMatcher.appendReplacement(tempStyleRulesBuilder, Matcher.quoteReplacement(updatedFullRule));
                        }
                        anyCssRuleMatcher.appendTail(tempStyleRulesBuilder);

                        if (ruleContentModified) {
                            modifiedStyleContent = tempStyleRulesBuilder.toString();
                            modified = true; // Mark overall content as modified
                        }

                        // Replace the original <style> content with the modified one
                        styleBlockMatcher.appendReplacement(updatedContentBuilder, Matcher.quoteReplacement("<style>" + modifiedStyleContent + "</style>"));
                    }
                    styleBlockMatcher.appendTail(updatedContentBuilder);

                    if (styleBlockFound) {
                        content = updatedContentBuilder.toString();
                    }

                    // Only write if something was actually modified
                    if (modified) {
                        FileUtils.write(file, content, StandardCharsets.UTF_8);
                        System.out.println("Modified SVG and fixed semicolon for: " + fileName);
                    } else {
                        System.out.println("No stroke-width or semicolon modification needed for: " + fileName);
                    }

                    // Perform regex checks on the SVG content (after modification)
                    checkAttributes(fileName, content, strokeAttr);
                }
            } else {
                System.out.println("No SVG files found or directory is empty/invalid: " + folder.getAbsolutePath());
            }

            if (!strokeAttr.isEmpty()) {
                out.println("\n\n______ Found SVG with wrong line attributes ______\n");
                for (String svg : strokeAttr.keySet()) {
                    out.println("\n" + svg + ":");
                    for (String attr : strokeAttr.get(svg)) {
                        out.println("\t" + attr);
                    }
                }
                out.println("\n\n____ Please check these first before proceeding ____\n\n");
                //return true;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return true; // Indicate failure
        }
        return false;
    }

    private static void checkAttributes(String file, String content, Map<String, List<String>> strokeAttr) {
        // Regex patterns for various attributes
        Pattern strokeColorPattern = Pattern.compile("stroke(?:=\"|:)(?:rgb[^a]|#).*?(?=[\"; ])");
        Pattern fillColorPattern = Pattern.compile("fill(?:=\"|:)(?:rgb[^a]|#).*?(?=[\"; ])");
        Pattern strokeOpacityPattern = Pattern.compile("stroke-opacity(?:=\"|:).*?(?=[\"; ])");
        Pattern fillOpacityPattern = Pattern.compile("fill-opacity(?:=\"|:).*?(?=[\"; ])");
        Pattern strokeRGBAPattern = Pattern.compile("stroke(?:=\"|:)rgba.*?(?=[\"; ])");
        Pattern fillRGBAPattern = Pattern.compile("fill(?:=\"|:)rgba.*?(?=[\"; ])");
        Pattern strokeWidthPattern = Pattern.compile("stroke-width(?:=\"|:) ?.*?(?=[\"; ])");
        Pattern lineCapPattern = Pattern.compile("stroke-linecap(?:=\"|:).*?(?=[\";}])");
        Pattern lineJoinPattern = Pattern.compile("stroke-linejoin(?:=\"|:).*?(?=[\";}])");

        Matcher strokeColorMatcher = strokeColorPattern.matcher(content);
        Matcher fillColorMatcher = fillColorPattern.matcher(content);
        Matcher strokeOpacityMatcher = strokeOpacityPattern.matcher(content);
        Matcher fillOpacityMatcher = fillOpacityPattern.matcher(content);
        Matcher strokeRGBAMatcher = strokeRGBAPattern.matcher(content);
        Matcher fillRGBAMatcher = fillRGBAPattern.matcher(content);
        Matcher strokeWidthMatcher = strokeWidthPattern.matcher(content);
        Matcher lineCapMatcher = lineCapPattern.matcher(content);
        Matcher lineJoinMatcher = lineJoinPattern.matcher(content);

        List<String> validColors = Arrays.asList(
                "stroke:#ffffff", "stroke:#fff", "stroke:#FFFFFF",
                "stroke=\"#ffffff", "stroke=\"#fff", "stroke=\"#FFFFFF",
                "stroke=\"white",
                "fill:#ffffff", "fill:#fff", "fill:#FFFFFF",
                "fill=\"#ffffff", "fill=\"#fff", "fill=\"#FFFFFF"
        );
        List<String> validOpacities = Arrays.asList(
                "stroke-opacity=\"0", "stroke-opacity=\"0%", "stroke-opacity=\"1",
                "stroke-opacity=\"100%", "stroke-opacity:1", "stroke-opacity:0",
                "fill-opacity=\"0", "fill-opacity=\"0%", "fill-opacity=\"1",
                "fill-opacity=\"100%", "fill-opacity:1", "fill-opacity:0"
        );
        List<String> validStrokeWidth = Arrays.asList(
                "stroke-width:2","stroke-width:2px","stroke-width:0px",
                "stroke-width:0","stroke-width=\"2","stroke-width=\"0",
                "stroke-width: 0px", "stroke-width: 2px"
        );
        List<String> validLineJoinCap = Arrays.asList(
                "stroke-linejoin:round","stroke-linejoin=\"round","stroke-linejoin: round",
                "stroke-linecap:round","stroke-linecap=\"round","stroke-linecap: round"
        );

        checkAttributes(file, strokeColorMatcher, strokeAttr,validColors);
        checkAttributes(file, fillColorMatcher, strokeAttr,validColors);
        checkAttributes(file, strokeOpacityMatcher, strokeAttr,validOpacities);
        checkAttributes(file, fillOpacityMatcher, strokeAttr,validOpacities);

        checkAttributes(file, strokeWidthMatcher, strokeAttr,validStrokeWidth);
        checkAttributes(file, lineCapMatcher, strokeAttr,validLineJoinCap);
        checkAttributes(file, lineJoinMatcher, strokeAttr,validLineJoinCap);

        checkRGBAAttributes(file, strokeRGBAMatcher, strokeAttr);
        checkRGBAAttributes(file, fillRGBAMatcher, strokeAttr);

    }

    private static void checkAttributes(String file, Matcher matcher, Map<String, List<String>> strokeAttr,List <String> validAttributes) {
        while (matcher.find()) {
            String attr = matcher.group();
            if (!validAttributes.contains(attr.trim())) {
                addToStrokeAttr(file, attr, strokeAttr);
            }
        }
    }

    private static void checkRGBAAttributes(String file, Matcher matcher, Map<String, List<String>> strokeAttr) {
        while (matcher.find()) {
            String rgba = matcher.group();
            if (!isValidRGBA(rgba)) {
                addToStrokeAttr(file, rgba, strokeAttr);
            }
        }
    }

    private static boolean isValidRGBA(String rgba) {
        return !(rgba.contains("rgba(255,255,255") || rgba.endsWith(",0)") || rgba.endsWith(",1)"));
    }

    private static boolean isValidAttribute(String attribute, String attributeName) {
        if ("stroke-width".equals(attributeName)) {
            return !attribute.equals("stroke-width:2") && !attribute.equals("stroke-width=0");
        } else if ("stroke-linecap".equals(attributeName) || "stroke-linejoin".equals(attributeName)) {
            return attribute.equals("stroke-linecap: round") || attribute.equals("stroke-linejoin: round") ||attribute.equals("stroke-linecap:round") || attribute.equals("stroke-linejoin:round");
        }
        return true;
    }

    private static void addToStrokeAttr(String file, String attribute, Map<String, List<String>> strokeAttr) {
        strokeAttr.computeIfAbsent(file, k -> new ArrayList<>()).add(attribute);
    }

    private static String replaceStroke(Matcher matcher) {
        String strokeStr = matcher.group("strokestr");
        double strokeWidth = Double.parseDouble(matcher.group("number"));

        if (strokeWidth >= 0 && strokeWidth < 0.3) {
            return strokeStr + "0";
        } else {
            return strokeStr + "2";
        }
    }
}