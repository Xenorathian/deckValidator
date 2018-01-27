package company;

import io.restassured.RestAssured;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Validator {
    private static final Pattern ETERNAL_LINK = Pattern.compile("https:\\/\\/eternalwarcry.com\\/decks\\/details\\/([\\w-]+)\\/([\\w-]+)");
    private static final Pattern DISCORD_NAME_MARKER = Pattern.compile("#\\d{4}");
    private static final Pattern ETERNAL_NAME = Pattern.compile("\\w+\\+\\d{4}");
    private static final String TOURNEY_IDENTIFIER = "<i class=\"fa fa-trophy\">";

    public static void main(String[] args) {
        List<File> csvFiles = Optional.ofNullable(new File("").getAbsoluteFile()
                .listFiles(Validator::isCsv)).map(Arrays::asList).orElse(new ArrayList<>());
        if (csvFiles.isEmpty())
            xenoLog("No CSV files found. Please ensure the CSV file is in the same folder as the jar");
        else
            csvFiles.forEach(file -> {
                xenoLog("PROCESSING FILE: " + file.getName());
                fileContent(file.getName()).stream().skip(1)
                        .map(Validator::eternalWarcryLinks)
                        .filter(Validator::hasEternalWarcryLink)
                        .forEach(Validator::validate);
                xenoLog("FILE DONE: " + file.getName());
            });
    }

    private static void validate(Map.Entry<String, List<String>> links) {
        List<String> nonTournamentDecks = links.getValue().stream().filter(Validator::isNotTournamentDeck).collect(Collectors.toList());
        if (!nonTournamentDecks.isEmpty())
            xenoLog("Decklink(s) \n" + nonTournamentDecks.stream().collect(Collectors.joining("\n")) +
                    "\nAre not valid tournament decks from " + diskordNickName(links.getKey()));
        else
            System.out.print(".");
    }

    private static boolean hasAnyInformation(String line) {
        return Arrays.stream(line.split("\\,")).map(Validator::removeQuotationMarks).filter(l -> !l.trim().isEmpty()).count() > 0;
    }

    private static Boolean isCsv(File file) {
        return file.getName().contains(".csv");
    }

    private static Map.Entry<String, List<String>> eternalWarcryLinks(String line) {
        Matcher matcher = ETERNAL_LINK.matcher(line);
        List<String> links = new ArrayList<>();
        while (matcher.find()) {
            links.add(matcher.group());
        }
        return new HashMap<String, List<String>>() {{
            put(line, links);
        }}.entrySet().iterator().next();
    }

    private static Boolean hasEternalWarcryLink(Map.Entry<String, List<String>> links) {
        if (links.getValue().isEmpty())
            xenoLog("No Eternal link from " + diskordNickName(links.getKey()));
        return !links.getValue().isEmpty();
    }

    private static void xenoLog(String message) {
        System.out.println("\n---===---\n" + message);
    }

    private static Boolean isNotTournamentDeck(String link) {
        return !RestAssured.given().get(link).getBody().asString().contains(TOURNEY_IDENTIFIER);
    }

    private static List<String> fileContent(String fileName) {
        try {
            return Files.lines(Paths.get(fileName)).filter(Validator::hasAnyInformation).collect(Collectors.toList());
        } catch (IOException e) {
            xenoLog("No information found in file " + fileName);
            return new ArrayList<>();
        }
    }

    private static String eternalNickName(String line) {
        Matcher matcher = ETERNAL_NAME.matcher(line);
        List<String> possibleNames = new ArrayList<>();
        while (matcher.find()) {
            possibleNames.add(matcher.group());
        }
        String name = possibleNames.stream().distinct().collect(Collectors.joining("/ "));
        if (name.isEmpty()) return line;
        else return name;
    }

    private static String diskordNickName(String line) {
        Matcher matcher = DISCORD_NAME_MARKER.matcher(line);
        List<String> possibleNamePostfixes = new ArrayList<>();
        while (matcher.find()) {
            possibleNamePostfixes.add(matcher.group());
        }
        List<String> possibleNames = Arrays.stream(line.split("[\\,\\;]{1}")).map(Validator::removeQuotationMarks)
                .filter(field -> possibleNamePostfixes.stream().anyMatch(field::contains)).collect(Collectors.toList());
        String name = possibleNames.stream().distinct().collect(Collectors.joining("/ "));
        if (name.isEmpty()) return "\n" + line;
        else return name;
    }

    private static String removeQuotationMarks(String line) {
        return line.replaceAll("\"", "");
    }
}
