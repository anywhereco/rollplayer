package dev.infernity.rollplayer.rollplayerlib3;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import static dev.infernity.rollplayer.rollplayerlib3.Expression.isNumber;

public class Expression {
    ArrayList<String> tokenStream;
    int pointer;
    String errorReturn;

    public Expression (ArrayList<String> tokens) {
        tokenStream = new ArrayList<>();
        tokenStream.addAll(tokens);
        pointer = 0;
        tokenStream.add("EOF"); // just exists to avoid StringOutOfBoundsException throws when peeking
    }

    protected String peek() {
        return tokenStream.get(pointer);
    }
    protected boolean peek(String s) {
        return peek().equals(s);
    }
    protected String consume(){
        String output = peek();
        pointer++;
        return output;
    }
    protected void consume(String s) {
        if(peek(s)) {
            errorReturn = "";
            consume();
        }
        else errorReturn = "Expected "+s+", got "+peek();
    }
    protected static boolean isNumber(String s) {
        if (s.matches("\\d+\\.?\\d+")) return true;
        if (s.matches("\\d\\.?\\d+[eE]\\d+")) return true;
        return false;
    }

    /**
     * Standard conditional tester for modularity
     * @param testValue Value to be tested, takes double or int
     * @param conditions Must be fed as (operator)(number) separated by commas. Valid operators are <, <=, >, >=, !=, =, x:y
     * @return String Boolean result of test against conditions
     * <p> Errors are returned prefaced by "ERR "
     */
    protected static String satisfiesConditions(double testValue, String conditions) {
        String[] conditionArray = conditions.split(",");

        for(String cond : conditionArray) {
            if (isNumber(cond)) cond = "=" + cond; // {3,4,5} is {=3,=4,=5}

            // ranges {3:5} are formatted differently so it gets a special treatment
            if (cond.matches("\\d+\\.?\\d+:\\d+\\.?\\d+")) {
                String[] ends = cond.split(":");
                double lowBound, highBound;
                if (Double.parseDouble(ends[0]) > Double.parseDouble(ends[1])) {
                    lowBound = Double.parseDouble(ends[1]);
                    highBound = Double.parseDouble(ends[0]);
                } else {
                    lowBound = Double.parseDouble(ends[0]);
                    highBound = Double.parseDouble(ends[1]);
                }

                if (testValue > lowBound && testValue < highBound) return "true";
                else continue;
            }

            double conditionValue;
            switch(cond.charAt(0)) {
                case '>':
                    if(cond.length() < 2) return "ERR Condition < with no number is invalid";
                    if(cond.charAt(1) == '=') {
                        if(cond.length() < 3) return "ERR Condition <= with no number is invalid";
                        if(!isNumber(cond.substring(2))) return "ERR Condition <= succeeded by invalid number";
                        conditionValue = Double.parseDouble(cond.substring(2));
                        if(testValue >= conditionValue) {
                            return "true";
                        } else continue;
                    } else {
                        if(!isNumber(cond.substring(1))) return "ERR Condition < succeeded by invalid number";
                        conditionValue = Double.parseDouble(cond.substring(1));
                        if(testValue > conditionValue) {
                            return "true";
                        } else continue;
                    }

                case '<':
                    if(cond.length() < 2) return "ERR Condition > with no number is invalid";
                    if(cond.charAt(1) == '=') {
                        if(cond.length() < 3) return "ERR Condition >= with no number is invalid";
                        if(!isNumber(cond.substring(2))) return "ERR Condition >= succeeded by invalid number";
                        conditionValue = Double.parseDouble(cond.substring(2));
                        if(testValue <= conditionValue) {
                            return "true";
                        } else continue;
                    } else {
                        if(!isNumber(cond.substring(1))) return "ERR Condition > succeeded by invalid number";
                        conditionValue = Double.parseDouble(cond.substring(1));
                        if(testValue < conditionValue) {
                            return "true";
                        } else continue;
                    }

                case '!':
                    if(cond.charAt(1) != '=') return "ERR Condition ! with no = is invalid";
                    if(cond.length() < 3) return "ERR Condition != with no number is invalid";
                    if(!isNumber(cond.substring(2))) return "ERR Condition != succeeded by invalid number";
                    conditionValue = Double.parseDouble(cond.substring(2));
                    if(testValue != conditionValue) {
                        return "true";
                    } else continue;

                case '=':
                    if(cond.length() < 2) return "ERR Condition = with no number is invalid";
                    if(!isNumber(cond.substring(1))) return "ERR Condition = succeeded by invalid number";
                    conditionValue = Double.parseDouble(cond.substring(1));
                    if(testValue == conditionValue) {
                        return "true";
                    } else continue;

                default:
                    return "ERR Illegal condition: " + cond.charAt(0) + "\nCheck that your input doesn't have typos";
            }
        }
        return "false";
    }

}

class DiceRoller extends Expression{
    private final String minmax;

    public DiceRoller (ArrayList<String> tokens, String minmax) {
        super(tokens);
        this.minmax = minmax;
        pointer++;
        tokenStream.addFirst("BOF");
    }

    @SuppressWarnings("unused")
    public DiceRoller (ArrayList<String> tokens) {
        this(tokens, "");
    }

    @Override
    protected boolean peek(String s){
        return peek().matches(s);
    }
    private String getConditions() {
        StringBuilder output = new StringBuilder();
        consume("\\{");
        if (!errorReturn.isEmpty()) return "ERR " + errorReturn;
        if (peek("}")) return "ERR Empty condition in diceroll found\nYou forgot to put stuff between the {}";
        while(!peek("\\}") && !peek("EOF")){
            output.append(consume());
        }
        consume("\\}");
        if (!errorReturn.isEmpty()) return "ERR " + errorReturn;
        return output.toString();
    }

    /**
     * Evaluates a token list for dice rolling from left to right
     * @return Rolls object containing result rolls
     * <p> Returns a Rolls object with stored error and dummy values if an error was detected
     */
    public Rolls evaluateExpression() {
        Rolls rolls;

        // establish initial roll
        {
            int rollCount = 1, rollMax, rollMin = 1;
            if (peek("d")) { //d(num)
                consume();
            } else if (isNumber(peek())) { //(num)d(num)
                rollCount = (int)Double.parseDouble(consume());
                consume("d");
                if (!errorReturn.isEmpty()) return new Rolls(errorReturn);
            } else
                return new Rolls("No roll detected in passed dice roll expression: " + tokenStream);

            if(isNumber(peek())) rollMax = (int)Double.parseDouble(consume());
            else return new Rolls("Diceroll declaration is succeeded by non-number token\nThis means you probably have a typo in your input");

            if(peek(":")) { //d(num):(num)
                consume();
                rollMin = rollMax;
                rollMax = (int)Double.parseDouble(consume());
            }
            if (minmax.equals("max")) rolls = new MaxRolls(rollCount, rollMin, rollMax);
            else if (minmax.equals("min")) rolls = new MinRolls(rollCount, rollMin, rollMax);
            else rolls = new Rolls(rollCount, rollMin, rollMax);
        }

        // -1 in condition is needed to account for EOF token at the end
        while(pointer < tokenStream.size()-1) {
            switch(consume()) {
                case "kh":
                    int keepHigher = 1;
                    if(isNumber(peek()))
                        keepHigher = (int)Double.parseDouble(consume());

                    if(peek("kl")) {
                        consume();
                        if (isNumber(peek()))
                            rolls.keepHighLow(keepHigher, (int) Double.parseDouble(consume()));
                        else
                            rolls.keepHighLow(keepHigher, 1);
                    } else
                        rolls.keepHigher(keepHigher);
                    if (rolls.isError()) return rolls; // kick it up the chain
                    break;

                case "kl":
                    int keepLower = 1;
                    if(isNumber(peek()))
                        keepLower = (int)Double.parseDouble(consume());

                    if(peek("kh")) {
                        consume();
                        if(isNumber(peek()))
                            rolls.keepHighLow((int)Double.parseDouble(consume()), keepLower);
                        else
                            rolls.keepHighLow(1, keepLower);
                    } else
                        rolls.keepLower(keepLower);
                    if (rolls.isError()) return rolls; // kick it again
                    break;

                case "bohi":
                    int boldHigher = 1;
                    if(isNumber(peek()))
                        boldHigher = (int)Double.parseDouble(consume());

                    rolls.boldHigher(boldHigher);
                    if (rolls.isError()) return rolls; // kick it up the chain
                    break;

                case "ithi":
                    int italHigher = 1;
                    if(isNumber(peek()))
                        italHigher = (int)Double.parseDouble(consume());

                    rolls.italicHigher(italHigher);
                    if (rolls.isError()) return rolls; // kick it up the chain
                    break;

                case "bolo":
                    int boldLower = 1;
                    if(isNumber(peek()))
                        boldLower = (int)Double.parseDouble(consume());

                    rolls.boldLower(boldLower);
                    if (rolls.isError()) return rolls; // kick it up the chain
                    break;

                case "itlo":
                    int italLower = 1;
                    if(isNumber(peek()))
                        italLower = (int)Double.parseDouble(consume());

                    rolls.italicLower(italLower);
                    if (rolls.isError()) return rolls; // kick it up the chain
                    break;

                case "rr":
                    String rerollCond;
                    int rerollMax = 1;
                    if(peek("\\{")) {
                        rerollCond = getConditions();
                        if (rerollCond.startsWith("ERR ")) return new Rolls(rerollCond.substring(4));
                    }
                    else return new Rolls("Missing condition following reroll operator\nRerolls need a {} condition section!");
                    if(peek(":")) {
                        consume(":");
                        if (!errorReturn.isEmpty()) return new Rolls(errorReturn);
                        rerollMax = (int)Double.parseDouble(consume());
                    }
                    rolls.reroll(rerollCond, rerollMax);
                    break;

                case "drop":
                    if(peek("\\{")) {
                        String dropConds = getConditions();
                        if (dropConds.startsWith("ERR ")) return new Rolls(dropConds.substring(4));
                        rolls.drop(dropConds);
                    } else return new Rolls("Missing condition following drop operator\nDrops need a {} condition section!");
                    break;

                case "bold":
                    if(peek("\\{")) {
                        String boldConds = getConditions();
                        if (boldConds.startsWith("ERR ")) return new Rolls(boldConds.substring(4));
                        rolls.bold(boldConds);
                    } else return new Rolls("Missing condition following bold operator\nBolds need a {} condition section!");
                    break;

                case "ital":
                    if(peek("\\{")) {
                        String italConds = getConditions();
                        if (italConds.startsWith("ERR ")) return new Rolls(italConds.substring(4));
                        rolls.italic(italConds);
                    } else return new Rolls("Missing condition following ital operator\nItals need a {} condition section!");
                    break;

                case "!":
                    String explosionCond = "";
                    int explosionMax = 10;
                    if(peek("\\{")) {
                        explosionCond = getConditions();
                        if (explosionCond.startsWith("ERR ")) return new Rolls(explosionCond.substring(4));
                    }
                    if(peek(":")) {
                        consume(":");
                        if (!errorReturn.isEmpty()) return new Rolls(errorReturn);
                        explosionMax = (int)Double.parseDouble(consume());
                    }
                    if(!explosionCond.isEmpty()) rolls.explode(explosionCond, explosionMax);
                    if (rolls.isError()) return rolls; // kick it more
                    else rolls.explode(explosionMax);
                    break;

                case "i":
                    ArrayList<String> conditions = new ArrayList<>();
                    ArrayList<String> mathTokens = new ArrayList<>();
                    if(!isNumber(peek()) && !peek("\\*")) return new Rolls("No condition found following I-mod operator\nI-mods need a roll selection section!");
                    conditions.add(consume());
                    while(peek(",")) {
                        consume();
                        if(peek("\\*") || isNumber(peek())) {
                            conditions.add(consume());
                        } else return new Rolls("Unrecognized condition in imod: " + peek() +"\nThis is probably due to a typo in the input");
                    }
                    if(!peek(":")) return new Rolls("No : found to declare math following I-mod conditions\nYou probably forgot to add a : somewhere");
                    while(peek(":")) {
                        consume();
                        if(peek("EOF")) return new Rolls("Empty math section following I-mod : declaration");
                        mathTokens.clear();
                        boolean endMath = false;
                        String[] expressionEnds = {"d", "kh", "kl", "rr", "drop", "!", "i", ":", "EOF"};
                        while (!endMath) {
                            mathTokens.add(consume());
                            for (String end : expressionEnds)
                                if (peek(end))
                                    endMath = true;
                        }
                        rolls.imod(conditions, mathTokens);
                        if (rolls.isError()) return rolls; // kick kick kick
                    }
                    break;
            }
        }
        return rolls;
    }


}

class Rolls{
    double[] rolls;
    boolean[] bolds;
    boolean[] italics;
    final int minRoll;
    final int maxRoll;
    final static Random rng = new Random();
    String errorCode;

    Rolls(String error) {
        this.rolls = new double[]{0};
        this.bolds = new boolean[]{false};
        this.italics = new boolean[]{false};
        this.minRoll = 0;
        this.maxRoll = 0;
        this.errorCode = error;
    }

    Rolls(double[] rolls, int min, int max) {
        this.rolls = rolls;
        this.bolds = new boolean[rolls.length];
        this.italics = new boolean[rolls.length];
        this.minRoll = min;
        this.maxRoll = max;
        this.errorCode = "";
    }

    @SuppressWarnings("unused")
    Rolls(double[] rolls, int die) {
        this(rolls, 1, die);
    }

    Rolls(int rollCount, int min, int max) {
        this(new double[rollCount], min, max);
        if(rollCount > 100) {
            errorCode = "Rollplayer will not roll more than 100 dice at once";
            return;
        }
        for (int i = 0; i < rollCount; i++) {
            rolls[i] = rollNumber();
        }
        this.bolds = new boolean[rollCount];
        this.italics = new boolean[rollCount];
    }

    @SuppressWarnings("unused")
    Rolls(int rollCount, int die) {
        this(rollCount, 1, die);
    }

    public boolean isError() { return !errorCode.isEmpty();}
    public String getError() { return errorCode; }

    public double rollNumber() {
        /*example roll logic
        * 50 to 100
        * 100-50 = 50
        * max roll 49 + 50 + 1 = 100
        * min roll 0 + 50 = 50
        */
        if (maxRoll < minRoll) {
            errorCode = "Roll upper bound is less than lower bound\nCheck the numbers after the letter \"d\"";
            return 1;
        }
        return rng.nextInt(maxRoll - minRoll + 1) + minRoll;
    }

    public ArrayList<String> getRolls(){
        ArrayList<String> formattedRolls = new ArrayList<>();
        for(int i = 0; i < rolls.length; i++) {
            String formattedRoll = "" + rolls[i];
            if(bolds[i]) formattedRoll = "**" + formattedRoll + "**";
            if(italics[i]) formattedRoll = "*" + formattedRoll + "*";
            formattedRolls.add(formattedRoll);
        }
        return formattedRolls;
    }

    public double getSum() {
        double output = 0;
        for(double i : rolls)
            output += i;
        return output;
    }

    /* this (and also keeplower) breaks italics and bolds behavior so i just empty them
     * if i had the foresight to make the roll lists more robustly track bolds and italics alongside
     * their corresponding roll then this would not really be a problem
     * also i don't want to make a more robust system just to accomodate bold/italic then keep
     */
    public void keepHigher(int high) {
        if(high > rolls.length || high < 1) {
            return; // keeping more rolls than are in the list
        }
        ArrayList<Double> sortedRolls = new ArrayList<>();
        for (double d : rolls) sortedRolls.add(d);
        sortedRolls.sort(null);
        ArrayList<Double> output = new ArrayList<>(Collections.nCopies(rolls.length, null));

        for (int highest = 0; highest < high; highest++) {
            double highValue = sortedRolls.get(sortedRolls.size() - highest - 1);
            for (int index = 0; index < rolls.length; index++)
                if (rolls[index] == highValue && output.get(index) == null) {
                    output.set(index, highValue);
                    break;
                }
        }

        output.removeAll(Collections.singleton(null));
        rolls = output.stream().mapToDouble(d -> d).toArray();
        bolds = new boolean[rolls.length];
        italics = new boolean[rolls.length];
    }

    public void keepLower(int low) {
        if(low > rolls.length || low < 1) {
            return; // keeping more rolls than are in the list
        }
        ArrayList<Double> sortedRolls = new ArrayList<>();
        for (double d : rolls) sortedRolls.add(d);
        sortedRolls.sort(null);
        ArrayList<Double> output = new ArrayList<>(Collections.nCopies(rolls.length, null));

        for (int lower = 0; lower < low; lower++) {
            double lowValue = sortedRolls.get(lower);
            for (int index = 0; index < rolls.length; index++)
                if (rolls[index] == lowValue && output.get(index) == null) {
                    output.set(index, lowValue);
                    break;
                }
        }

        output.removeAll(Collections.singleton(null));
        rolls = output.stream().mapToDouble(d -> d).toArray();
        bolds = new boolean[rolls.length];
        italics = new boolean[rolls.length];
    }

    public void keepHighLow(int high, int low) {
        if (high+low > rolls.length || high < -1 || low < -1) {
            return; // keeping more rolls than are in the list
        }
        ArrayList<Double> sortedRolls = new ArrayList<>();
        for (double d : rolls) sortedRolls.add(d);
        sortedRolls.sort(null);
        ArrayList<Double> output = new ArrayList<>(Collections.nCopies(rolls.length, null));

        for (int highest = 0; highest < high; highest++) {
            double highValue = sortedRolls.get(sortedRolls.size() - highest - 1);
            for (int index = 0; index < rolls.length; index++)
                if (rolls[index] == highValue && output.get(index) == null) {
                    output.set(index, highValue);
                    break;
                }
        }

        for (int lower = 0; lower < low; lower++) {
            double lowValue = sortedRolls.get(lower);
            for (int index = 0; index < rolls.length; index++)
                if (rolls[index] == lowValue && output.get(index) == null) {
                    output.set(index, lowValue);
                    break;
                }
        }

        output.removeAll(Collections.singleton(null));
        rolls = output.stream().mapToDouble(d -> d).toArray();
    }

    public void bold(String conditions){
        for (int i = 0; i < rolls.length; i++) {
            String result = Expression.satisfiesConditions(rolls[i], conditions);
            if (result.startsWith("ERR ")) {
                errorCode = result.substring(4);
                return;
            }
            if (result.equals("true"))
                bolds[i] = true;
        }
    }

    public void italic(String conditions){
        for (int i = 0; i < rolls.length; i++) {
            String result = Expression.satisfiesConditions(rolls[i], conditions);
            if (result.startsWith("ERR ")) {
                errorCode = result.substring(4);
                return;
            }
            if (result.equals("true"))
                italics[i] = true;
        }
    }

    public void boldHigher(int high) {
        ArrayList<Double> sortedRolls = new ArrayList<>();
        for (double d : rolls) sortedRolls.add(d);
        sortedRolls.sort(null);
        double highestRoll = sortedRolls.get(rolls.length - high);

        for(int i = 0; i < rolls.length; i++)
            if(rolls[i] >= highestRoll)
                bolds[i] = true;
    }

    public void italicHigher(int high) {
        ArrayList<Double> sortedRolls = new ArrayList<>();
        for (double d : rolls) sortedRolls.add(d);
        sortedRolls.sort(null);
        double highestRoll = sortedRolls.get(rolls.length - high);

        for(int i = 0; i < rolls.length; i++)
            if(rolls[i] >= highestRoll)
                italics[i] = true;
    }

    public void boldLower(int low) {
        ArrayList<Double> sortedRolls = new ArrayList<>();
        for (double d : rolls) sortedRolls.add(d);
        sortedRolls.sort(null);
        double lowestRoll = sortedRolls.get(low);

        for(int i = 0; i < rolls.length; i++)
            if(rolls[i] <= lowestRoll)
                bolds[i] = true;
    }

    public void italicLower(int low) {
        ArrayList<Double> sortedRolls = new ArrayList<>();
        for (double d : rolls) sortedRolls.add(d);
        sortedRolls.sort(null);
        double lowestRoll = sortedRolls.get(low);

        for(int i = 0; i < rolls.length; i++)
            if(rolls[i] <= lowestRoll)
                italics[i] = true;
    }

    /**
     * Rerolls dice that fulfill given conditions once
     * @param conditions Must be fed as (operator)(number) separated by commas. Valid operators are <, <=, >, >=, !=, and =
     */
    @SuppressWarnings("unused")
    public void reroll(String conditions) {
        reroll(conditions, 1);
    }

    /**
     * Rerolls dice that fulfill given conditions up to maxRepeats times
     * @param conditions Must be fed as (operator)(number) separated by commas. Valid operators are <, <=, >, >=, !=, and =
     * @param maxRepeats Maximum number of times a die can be rerolled
     */
    public void reroll(String conditions, int maxRepeats) {
        double[] output = rolls;
        for (int index = 0; index < rolls.length; index++)
            for (int retries = 0; retries < maxRepeats; retries++) {
                String result = Expression.satisfiesConditions(output[index], conditions);
                if (result.startsWith("ERR ")) {
                    errorCode = result.substring(4);
                    return;
                }
                if (result.equals("true")) {
                    output[index] = rollNumber();
                } else break;
            }
        rolls = output;
    }

    /**
     * Removes rolls that satisfy input conditions removed
     * @param conditions Must be fed as (operator)(number) separated by commas. Valid operators are <, <=, >, >=, !=, and =
     */
    public void drop(String conditions) {
        ArrayList<Double> output = new ArrayList<>();
        ArrayList<Boolean> newBolds = new ArrayList<>();
        ArrayList<Boolean> newItals = new ArrayList<>();
        for (int i = 0; i < rolls.length; i++) {
            String result = Expression.satisfiesConditions(i, conditions);
            if (result.startsWith("ERR ")) {
                errorCode = result.substring(4);
                return;
            }
            if (!result.equals("true")) {
                output.add(rolls[i]);
                newBolds.add(bolds[i]);
                newItals.add(italics[i]);
            }
        }
        rolls = output.stream().mapToDouble(d -> d).toArray();
        bolds = ArrayUtils.toPrimitive(newBolds.toArray(ArrayUtils.EMPTY_BOOLEAN_OBJECT_ARRAY));
        italics = ArrayUtils.toPrimitive(newItals.toArray(ArrayUtils.EMPTY_BOOLEAN_OBJECT_ARRAY));
    }

    /**
     * Removes rolls that do not satisfy input conditions
     * @param conditions Must be fed as (operator)(number) separated by commas. Valid operators are <, <=, >, >=, !=, and =
     */
    @SuppressWarnings("unused")
    public void keep(String conditions) {
        ArrayList<Double> output = new ArrayList<>();
        for (double i : rolls) {
            String result = Expression.satisfiesConditions(i, conditions);
            if (result.startsWith("ERR ")) {
                errorCode = result.substring(4);
                return;
            }
            if (result.equals("true"))
                output.add(i);
            }
        rolls = output.stream().mapToDouble(d -> d).toArray();
    }

    /**
     * Rolls an extra die for every die that satisfies given condition list. Those rolls can continue to cascade
     * @param conditions    Must be fed as (operator)(number) separated by commas. Valid operators are <, <=, >, >=, !=, and =
     * @param maxExplosions Maximum number of explosion dice rolls
     */
    public void explode(String conditions, int maxExplosions) {
        if (maxExplosions > 50) {
            errorCode = "Rollplayer cannot set maximum explosion cap above 50";
            return;
        }
        ArrayList<Double> output = new ArrayList<>();
        ArrayList<Boolean> newBolds = new ArrayList<>();
        ArrayList<Boolean> newItals = new ArrayList<>();
        for(int i = 0; i < rolls.length; i++){
            output.add(rolls[i]);
            newBolds.add(bolds[i]);
            newItals.add(italics[i]);
        }

        int explosionsRemaining = 0, explosionsCounter = 0;
        for (double i : rolls) {
            String result = Expression.satisfiesConditions(i, conditions);
            if (result.startsWith("ERR ")) {
                errorCode = result.substring(4);
                return;
            }
            if (result.equals("true"))
                explosionsRemaining++;
        }

        while (explosionsRemaining > 0 && explosionsCounter < maxExplosions) {
            explosionsCounter++;
            output.add(rollNumber());
            newBolds.add(false);
            newItals.add(false);

            String result = Expression.satisfiesConditions(output.getLast(), conditions);
            if (result.startsWith("ERR ")) {
                errorCode = result.substring(4);
                return;
            }
            if (result.equals("true"))
                continue;
            explosionsRemaining--;
        }
        rolls = output.stream().mapToDouble(d -> d).toArray();
        bolds = ArrayUtils.toPrimitive(newBolds.toArray(ArrayUtils.EMPTY_BOOLEAN_OBJECT_ARRAY));
        italics = ArrayUtils.toPrimitive(newItals.toArray(ArrayUtils.EMPTY_BOOLEAN_OBJECT_ARRAY));
    }

    public void explode(int maxExplosions) {
        explode("=" + maxRoll, maxExplosions);
    }

    public void imod(ArrayList<String> conditions, ArrayList<String> mathTokens) {
        ArrayList<String> math = new ArrayList<>();
        for(String cond : conditions) {
            if(!cond.equals("*")) {
                math.clear(); // don't make my dummy mistake of setting the references equal
                math.addAll(mathTokens);
                int rollIndex = (int)Double.parseDouble(cond) - 1; // reminder that tokens will be in double format
                if (rollIndex >= rolls.length || rollIndex < 0) {
                    errorCode = String.format("I-mod index out of bounds: %d of %d", rollIndex+1, rolls.length);
                    return;
                }
                math.addFirst("" + rolls[rollIndex]);

                MathSolver evaluator = new MathSolver(math);
                if (evaluator.isError()) {
                    errorCode = evaluator.getError();
                    return;
                }
                rolls[rollIndex] = evaluator.evaluate();
            } else {
                for(int i = 0; i < rolls.length; i++) {
                    math.clear();
                    math.addAll(mathTokens);
                    math.addFirst("" + rolls[i]);

                    MathSolver evaluator = new MathSolver(math);
                    if (evaluator.isError()) {
                        errorCode = evaluator.getError();
                        return;
                    }
                    rolls[i] = evaluator.evaluate();
                }
            }
        }
    }
}

class MinRolls extends Rolls{
    double lowerBound;
    ArrayList<double[]> boundRanges;

    MinRolls(double[] rolls, int min, int max) {
        super(rolls, min, max);
        boundRanges = new ArrayList<>();
    }

    MinRolls(int rollCount, int min, int max) {
        this(new double[rollCount], min, max);
        if(rollCount > 100) {
            errorCode = "Rollplayer will not roll more than 100 dice at once";
            return;
        }
        for (int i = 0; i < rollCount; i++) {
            rolls[i] = min;
        }
        lowerBound = min;
    }

    /**
     * Takes in a lower bound from any conditional and modifies rolls to satisfy it
     * @param lowerBound Lower bound for rolls, EXCLUSIVE on bound
     */
    private void boundDown(double lowerBound) {
        if (lowerBound < this.lowerBound) return;
        else this.lowerBound = lowerBound;

        for(int i = 0; i < rolls.length; i++)
            if (rolls[i] < this.lowerBound)
                rolls[i] = Math.ceil(this.lowerBound); // this has edge cases with like, imod then drop. i don't care
    }

    /**
     * Adds a range bound condition to the rolls and calls boundDown if needed
     * @param bounds Lower bound inclusive and upper bound exclusive, in that order
     */
    private void boundRange(double[] bounds) {
        // if the inputted range is greater than the upperbound or less than the lowerbound then i dont care
        if (bounds[0] > maxRoll) return;
        if (bounds[1] < lowerBound) return;

        // insert the bound into the list, sorted by lowerbound
        // this is faster than just adding and then sorting but i dont care
        if (!boundRanges.isEmpty()) {
            for (int i = 0; i < boundRanges.size(); i++) {
                if (bounds[1] < boundRanges.get(i)[1]) {
                    boundRanges.add(i, bounds);
                    break;
                }
                if (i == boundRanges.size() - 1) {
                    boundRanges.add(bounds);
                    break;
                }
            }
        } else boundRanges.add(bounds);

        ArrayList<double[]> mergedOutput = new ArrayList<>();
        mergedOutput.add(boundRanges.getFirst()); // initial lowest range
        // now merge the bounds
        for (int i = 1; i < boundRanges.size(); i++) {
            double[] lastRange = mergedOutput.getLast();
            double[] nextRange = boundRanges.get(i);

            // if bounds overlap, MERGE!
            if (nextRange[0] <= lastRange[1])
                lastRange[1] = Math.max(lastRange[1], nextRange[1]);
            else
                mergedOutput.add(nextRange);
        }

        double newBound = lowerBound;
        // now iterate forwards over the ranges to see if they overlap with lowerBound
        while (mergedOutput.getFirst()[0] < newBound)
            if (mergedOutput.getFirst()[1] > newBound) {
                newBound = mergedOutput.getFirst()[1];
                mergedOutput.removeFirst(); // 3:5 bound 4
            } else {
                mergedOutput.removeFirst(); // 3:5 bound 6
            }

        // now redo the rolls
        if (newBound > lowerBound) boundDown(newBound);
    }

    private void boundConditions(String inputConditions) {
        String[] conditions = inputConditions.split(",");

        for (String cond : conditions) {
            if (isNumber(cond)) cond = "=" + cond; // {3,4,5} is {=3,=4,=5}

            // ranges like 3:5
            if (cond.matches("\\d+\\.?\\d+:\\d+\\.?\\d+")) {
                String[] bounds = cond.split(":");

                if (Double.parseDouble(bounds[0]) == Double.parseDouble(bounds[1])) cond = "=" + bounds[0]; // 3:3
                else if (Double.parseDouble(bounds[0]) > Double.parseDouble(bounds[1])) // 5:3
                    boundRange(new double[]{Double.parseDouble(bounds[1]), Double.parseDouble(bounds[0]) + 1});
                else
                    boundRange(new double[]{Double.parseDouble(bounds[0]), Double.parseDouble(bounds[1]) + 1}); // 3:5
            }

            // Valid operators are <, <=, >, >=, !=, =
            switch (cond.charAt(0)) {
                case '<':
                    if (cond.charAt(1) == '=')
                        boundDown(Double.parseDouble(cond.substring(2)) + 1);
                    else
                        boundDown(Double.parseDouble(cond.substring(1)));
                    continue;

                case '>':
                    if (cond.charAt(1) == '=')
                        boundRange(new double[]{Double.parseDouble(cond.substring(2)) - 1, maxRoll + 1});
                    else
                        boundRange(new double[]{Double.parseDouble(cond.substring(1)), maxRoll + 1});
                    continue;

                case '!':
                    boundRange(new double[]{Double.parseDouble(cond.substring(2)), maxRoll + 1});
                    boundDown(Double.parseDouble(cond.substring(2)));
                    continue;

                case '=':
                    boundRange(new double[]{Double.parseDouble(cond.substring(1)), Double.parseDouble(cond.substring(1)) + 1});
            }
        }
    }

    @Override
    public void reroll(String conditions, int maxRepeats) {
        if (rolls.length == 0) return;
        boundConditions(conditions);
    }

    @Override
    public void drop(String inputConditions) { // this has to have its own behaviour because of course it does
        if (rolls.length == 0) return;
        String[] conditions = inputConditions.split(",");

        for (String cond : conditions) {
            if (isNumber(cond)) cond = "=" + cond; // {3,4,5} is {=3,=4,=5}

            // ranges like 3:5
            if (cond.matches("\\d+\\.?\\d+:\\d+\\.?\\d+")) {
                String[] bounds = cond.split(":");

                if (Double.parseDouble(bounds[0]) == Double.parseDouble(bounds[1])) cond = "=" + bounds[0]; // 3:3
                else if (Double.parseDouble(bounds[0]) > Double.parseDouble(bounds[1])) { // 5:3
                    if (Double.parseDouble(bounds[1]) < lowerBound && Double.parseDouble(bounds[0]) > lowerBound) {
                        rolls = new double[0];
                        bolds = new boolean[0];
                        italics = new boolean[0];
                        return;
                    }
                } else // 3:5
                    if (Double.parseDouble(bounds[0]) < lowerBound && Double.parseDouble(bounds[1]) > lowerBound) {
                        rolls = new double[0];
                        bolds = new boolean[0];
                        italics = new boolean[0];
                        return;
                    }
            }

            // Valid operators are <, <=, >, >=, !=, =
            switch (cond.charAt(0)) {
                case '<':
                    if (cond.charAt(1) == '=') {
                        if (Double.parseDouble(cond.substring(2)) >= lowerBound) {
                            rolls = new double[0];
                            bolds = new boolean[0];
                            italics = new boolean[0];
                            return;
                        }
                    } else
                        if (Double.parseDouble(cond.substring(1)) > lowerBound) {
                            rolls = new double[0];
                            bolds = new boolean[0];
                            italics = new boolean[0];
                            return;
                        }
                    continue;

                case '>':
                    if (cond.charAt(1) == '=') {
                        if (Double.parseDouble(cond.substring(2)) <= lowerBound) {
                            rolls = new double[0];
                            bolds = new boolean[0];
                            italics = new boolean[0];
                            return;
                        }
                    } else
                        if (Double.parseDouble(cond.substring(1)) < lowerBound) {
                            rolls = new double[0];
                            bolds = new boolean[0];
                            italics = new boolean[0];
                            return;
                        }
                    continue;

                case '!':
                    if (rolls.length > 0 && Double.parseDouble(cond.substring(2)) != rolls[0]) {
                        rolls = new double[0];
                        bolds = new boolean[0];
                        italics = new boolean[0];
                        return;
                    }
                    continue;

                case '=':
                    if (rolls.length > 0 && Double.parseDouble(cond.substring(1)) == rolls[0]){
                        rolls = new double[0];
                        bolds = new boolean[0];
                        italics = new boolean[0];
                        return;
                    }
            }
        }
    }

    @Override
    public void explode(int maxExplosions) {
        // this should not do anything
    }

    @Override
    public void explode(String conditions, int maxExplosions) {
        // this should not do anything
    }
}

class MaxRolls extends Rolls{
    double upperBound;
    ArrayList<double[]> boundRanges;

    MaxRolls(double[] rolls, int min, int max) {
        super(rolls, min, max);
        boundRanges = new ArrayList<>();
    }

    MaxRolls(int rollCount, int min, int max) {
        this(new double[rollCount], min, max);
        if(rollCount > 100) {
            errorCode = "Rollplayer will not roll more than 100 dice at once";
            return;
        }
        for (int i = 0; i < rollCount; i++) {
            rolls[i] = max;
        }
        upperBound = max;
    }

    /**
     * Takes in an upper bound from any conditional and modifies rolls to satisfy it
     * @param upperBound Upper bound for rolls, EXCLUSIVE on bound
     */
    private void boundUp(double upperBound) {
        if (upperBound > this.upperBound) return;
        else this.upperBound = upperBound;

        for(int i = 0; i < rolls.length; i++)
            if (rolls[i] > this.upperBound)
                rolls[i] = (int)this.upperBound; // this has edge cases with like, imod then drop. i don't care
    }

    /**
     * Adds a range bound condition to the rolls and calls boundUp if needed
     * @param bounds Lower bound exclusive and upper bound inclusive, in that order
     */
    private void boundRange(double[] bounds) {
        // if the inputted range is greater than the upperbound or less than the lowerbound then i dont care
        if (bounds[0] > upperBound) return;
        if (bounds[1] < minRoll) return;

        // insert the bound into the list, sorted by lowerbound
        // this is faster than just adding and then sorting but i dont care
        if (!boundRanges.isEmpty()) {
            for (int i = 0; i < boundRanges.size(); i++) {
                if (bounds[1] < boundRanges.get(i)[1]) {
                    boundRanges.add(i, bounds);
                    break;
                }

                if (i == boundRanges.size() - 1) {
                    boundRanges.add(bounds);
                    break;
                }
            }
        } else boundRanges.add(bounds);

        ArrayList<double[]> mergedOutput = new ArrayList<>();
        mergedOutput.add(boundRanges.getFirst()); // initial lowest range
        // now merge the bounds
        for (int i = 1; i < boundRanges.size(); i++) {
            double[] lastRange = mergedOutput.getLast();
            double[] nextRange = boundRanges.get(i);

            // if bounds overlap, MERGE!
            if (nextRange[0] <= lastRange[1])
                lastRange[1] = Math.max(lastRange[1], nextRange[1]);
            else
                mergedOutput.add(nextRange);
        }

        double newBound = upperBound;
        // now iterate backwards over the ranges to see if they overlap with upperBound
        while (mergedOutput.getLast()[1] > newBound)
            if (mergedOutput.getLast()[0] < newBound) {
                newBound = mergedOutput.getLast()[0];
                mergedOutput.removeLast(); // 3:5 bound 4
            } else {
                mergedOutput.removeLast(); // 3:5 bound 2
            }

        // now redo the rolls
        boundUp(newBound);
    }

    private void boundConditions(String inputConditions) {
        String[] conditions = inputConditions.split(",");

        for (String cond : conditions) {
            if (isNumber(cond)) cond = "=" + cond; // {3,4,5} is {=3,=4,=5}

            // ranges like 3:5
            if (cond.matches("\\d+\\.?\\d+:\\d+\\.?\\d+")) {
                String[] bounds = cond.split(":");

                if (Double.parseDouble(bounds[0]) == Double.parseDouble(bounds[1])) cond = "=" + bounds[0]; // 3:3
                else if (Double.parseDouble(bounds[0]) > Double.parseDouble(bounds[1])) // 5:3
                    boundRange(new double[]{Double.parseDouble(bounds[1]) - 1, Double.parseDouble(bounds[0])});
                else
                    boundRange(new double[]{Double.parseDouble(bounds[0]) - 1, Double.parseDouble(bounds[1])}); // 3:5
            }

            // Valid operators are <, <=, >, >=, !=, =
            switch (cond.charAt(0)) {
                case '<':
                    if (cond.charAt(1) == '=')
                        boundRange(new double[]{minRoll - 1, Double.parseDouble(cond.substring(2))});
                    else
                        boundRange(new double[]{minRoll - 1, Double.parseDouble(cond.substring(1)) - 1});
                    continue;

                case '>':
                    if (cond.charAt(1) == '=')
                        boundUp(Double.parseDouble(cond.substring(2)) - 1);
                    else
                        boundUp(Double.parseDouble(cond.substring(1)));
                    continue;

                case '!':
                    boundRange(new double[]{minRoll - 1, Double.parseDouble(cond.substring(2)) - 1});
                    boundUp(Double.parseDouble(cond.substring(2)));
                    continue;

                case '=':
                    boundRange(new double[]{Double.parseDouble(cond.substring(1)) - 1, Double.parseDouble(cond.substring(1))});
            }
        }
    }

    @Override
    public void reroll(String conditions, int maxRepeats) {
        boundConditions(conditions);
    }

    @Override
    public void drop(String conditions) {
        boundConditions(conditions);
    }

    @Override
    public void explode(int maxExplosions) {
        // this should not do anything
    }

    @Override
    public void explode(String conditions, int maxExplosions) {
        // this should not do anything
    }
}