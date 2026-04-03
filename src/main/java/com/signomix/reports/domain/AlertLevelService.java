package com.signomix.reports.domain;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AlertLevelService {

    @Inject
    Logger logger;

    // Klasa pomocnicza do reprezentacji reguły (odpowiednik JS-owego obiektu 'rule')
    public static class ComparatorRule {

        public String varName = null;
        public int comparator1 = 0;
        public double value1 = Double.NaN;
        public int comparator2 = 0;
        public double value2 = Double.NaN;
        public Long delay = null;
    }

    /**
     * Calculates alert level
     *
     * @param definition alert definition string
     * @param value      is value to check against definition
     * @param tstamp     timestamp of the value update (in milliseconds)
     * @return 3==notResponding, 2==alert, 1==warning, otherwise 0 or -1 when definition not provided
     *
     * Alert definition format:
     * "alertDef:warningDef#notRespondingDef@measurementName"
     * All parts are optional. alertDef and warningDef are in format "<operator><number>", e.g. ">30", "<=100", "!=50". notRespondingDef is in format "<number><unit>", e.g. "30s", "5m".
     * If measurementName is provided, it can be used to identify which measurement the alert definition applies to, but in this implementation it's not used for alert level calculation.
     */
    public int getAlertLevel(String definition, Double value, Long tstamp) {
        if (definition == null || definition.trim().isEmpty()) {
            logger.debug("Alert definition is null or empty. Returning -1.");
            return -1;
        }

        long delay = 0;

        // Usuń wszystkie spacje i znaki niedrukowalne
        String def = definition.replaceAll("\\s+", "");

        String[] defs = def.split(":");
        String alertDef = defs.length > 0 ? defs[0] : "";
        String warningDef = defs.length > 1 ? defs[1] : "";

        // the last part of defs may include measurement name and not responding rule
        ComparatorRule notRespondingRule = getNotRespondingRule(
            defs[defs.length - 1]
        );

        if (isNotResponding(notRespondingRule, tstamp)) {
            logger.debug(
                "Value is considered not responding based on the definition. Returning 3."
            );
            return 3;
        }

        //String measurementName = getMeasurementName(defs[defs.length - 1]);
        int alertCheck = checkRule(value, alertDef);
        int warningCheck = checkRule(value, warningDef);
        logger.debug(
            "Alert check result: " +
                alertCheck +
                ", Warning check result: " +
                warningCheck
        );
        if (alertCheck == -1 && warningCheck == -1) {
            return 3;
        }
        if (alertCheck == 1) {
            return 2;
        }
        if (warningCheck == 1) {
            return 1;
        }

        return 0;
    }

    public ComparatorRule getNotRespondingRule(String definition) {
        ComparatorRule rule = new ComparatorRule();
        if (definition == null || definition.trim().isEmpty()) {
            return rule;
        }

        String defStr = definition.trim();
        //if definition includes string "@" or "#" or ":" remove it and everything after it
        int hashIndex = defStr.indexOf('#');
        if (hashIndex > 0) {
            // Jeśli "#" jest obecny, to odetnij wszystko przed #, włącznie z nim
            defStr = defStr.substring(hashIndex + 1);
        } else {
            return rule;
        }
        int atIndex = defStr.indexOf('@');
        if (atIndex > 0) {
            // Jeśli "@" jest obecny, to odetnij wszystko po @, włącznie z nim
            defStr = defStr.substring(0, atIndex);
        }

        char timeUnit = defStr.charAt(defStr.length() - 1);
        int multiplier = 1;

        if (timeUnit == 'm') {
            multiplier = 60;
        } else if (timeUnit == 's') {
            multiplier = 1;
        } else {
            // Jeśli ostatni znak nie jest 's' ani 'm', to nie mamy poprawnej reguły, więc zwracamy rule z value1 = NaN.
            return rule;
        }

        try {
            // value1 holds delay in milliseconds
            // Poprawiony bug z JS: defStr.length() - 1 odcina jedną literę na końcu (np. 's'),
            // a nie 2 litery jak w oryginalnym kodzie JS (length - 2 obcinało '30s' do '3'!).
            String numericPart = defStr.substring(0, defStr.length() - 1);
            rule.value1 = 1000.0 * multiplier * Double.parseDouble(numericPart);
        } catch (NumberFormatException err) {
            // ignoruj (podobnie jak puste catch w JS)
        }

        return rule;
    }

    public boolean isNotResponding(ComparatorRule rule, Long tstamp) {
        if (tstamp == null || tstamp == 0) {
            return true;
        }
        // Jeśli rule.delay jest null, to nie mamy reguły, więc nie możemy stwierdzić, że jest not responding. Zwracamy false.
        if (rule.delay == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        return (currentTime - tstamp) > rule.delay.longValue();
    }

    private String getMeasurementName(String definition) {
        if (definition == null || definition.trim().isEmpty()) {
            return "";
        }
        //if definition includes string "#" or ":" remove it and everything after it
        int hashIndex = definition.indexOf('#');
        if (hashIndex > 0) {
            definition = definition.substring(0, hashIndex);
        }
        int colonIndex = definition.indexOf(':');
        if (colonIndex > 0) {
            definition = definition.substring(0, colonIndex);
        }
        return definition.trim();
    }

    public int checkRule(Double value, String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            return -1;
        }

        // Usuń pierwszą literę, jeśli reguła zaczyna się od litery
        if (rule.matches("^[a-zA-Z].*")) {
            rule = rule.substring(1);
        }

        if (value == null || Double.isNaN(value)) {
            return -1;
        }

        Pattern pattern = Pattern.compile(
            "^([<>=!]=?|==)\\s*(-?\\d+(\\.\\d+)?)$"
        );
        Matcher matcher = pattern.matcher(rule);

        if (!matcher.matches()) {
            return -1;
        }

        String operator = matcher.group(1);
        double number = Double.parseDouble(matcher.group(2));
        boolean result = false;

        switch (operator) {
            case "<":
                result = value < number;
                break;
            case "<=":
                result = value <= number;
                break;
            case ">":
                result = value > number;
                break;
            case ">=":
                result = value >= number;
                break;
            case "=":
            case "==":
                result = value == number;
                break;
            case "!=":
                result = value != number;
                break;
            default:
                return -1;
        }

        return result ? 1 : 0;
    }
}
