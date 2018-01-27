package company;

import io.restassured.RestAssured;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Validator {

    private static final Pattern ETERNAL_LINK = Pattern.compile("https:\\/\\/eternalwarcry.com\\/decks\\/details\\/([\\w-]+)\\/([\\w-=?#]+)");
    private static final String TOURNEY_IDENTIFIER = "<i class=\"fa fa-trophy\">";

    public static void main(String[] args) {
        List<File> csvFiles = Optional.ofNullable(new File("").getAbsoluteFile()
                .listFiles(file -> file.getName().contains(".csv"))).map(Arrays::asList).orElse(new ArrayList<>());
        if(csvFiles.isEmpty()) xenoLog("No CSV files found. Please ensure the CSV file is in the same folder as the jar");
        else csvFiles.forEach(file -> {
                    try {
                        xenoLog("PROCESSING FILE: " + file.getName());
                        Stream<String> stream = Files.lines(Paths.get(file.getName()));
                        //if(stream.findFirst())

                        stream.skip(1).map(s -> s.replaceAll("\"", ""))
                                .filter(Validator::hasAnyInformation).forEach(Validator::validate);
                        xenoLog("FILE DONE: " + file.getName());
                    } catch (IOException e) {
                        xenoLog("PROCESSING ERROR: " + file.getName());
                    }
                });
    }

    private static void validate(String data) {
        List<String> validLinks = Arrays.stream(data.split("\\,")).filter(Validator::isEternalWarcyLink).collect(Collectors.toList());
        if(validLinks.isEmpty())
        {
            xenoLog("No Eternal deck links detected in: \n" + data);
        }
        //TODO:: check exact number of links are present ... will need passed input of number of expected links
        else
        {
            List<String> nonTournamentDecks = validLinks.stream().filter(Validator::isNotTournamentDeck).collect(Collectors.toList());
            if(!nonTournamentDecks.isEmpty())
                xenoLog("Decklink(s) \n" + nonTournamentDecks.stream().collect(Collectors.joining("\n")) +
                "\nAre not valid tournament decks from:\n" + data.replaceAll(ETERNAL_LINK.pattern(), "..."));
            else
                System.out.print(".");
        }
    }

    private static void xenoLog(String message)
    {
        System.out.println("\n---===---\n" + message);
    }

    private static Boolean isEternalWarcyLink (String link) {
        return ETERNAL_LINK.matcher(link).matches();
        }

    private static Boolean isNotTournamentDeck (String link) {
        return !RestAssured.given().get(link).getBody().asString().contains(TOURNEY_IDENTIFIER);
    }

    private static Boolean hasAnyInformation(String line) {
        return Arrays.stream(line.split("\\,")).filter(l -> !l.trim().isEmpty()).count() > 0;
    }
}
