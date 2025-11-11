package dev.infernity.rollplayer.rollplayerlib3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class Expression {
    ArrayList<String> tokenStream;
    int pointer;

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
    protected void consume(String s) throws IllegalArgumentException {
        if(peek(s)) consume();
        else throw new IllegalArgumentException("Expected "+s+", got "+peek());
    }
    protected boolean isNumber(String s) {
        if (s.matches("\\d+.\\d+")) return true;
        if (s.matches("\\d.\\d+[eE]\\d+")) return true;
        return false;
    }

    /**
     * Standard conditional tester for modularity
     * @param testValue Value to be tested, takes double or int
     * @param conditions Must be fed as (operator)(number) separated by commas. Valid operators are <, <=, >, >=, !=, and =
     * @return Boolean result of test against conditions
     * @throws IllegalArgumentException For unrecognized operator character
     */
    protected static boolean satisfiesConditions(double testValue, String conditions) throws IllegalArgumentException {
        String[] conditionArray = conditions.split(",");

        for(String cond : conditionArray) {
            double conditionValue;
            switch(cond.charAt(0)) {
                case '>':
                    if(cond.length() < 2) throw new IllegalArgumentException("Condition < with no number is invalid");
                    if(cond.charAt(1) == '=') {
                        if(cond.length() < 3) throw new IllegalArgumentException("Condition <= with no number is invalid");
                        conditionValue = Double.parseDouble(cond.substring(2));
                        if(testValue >= conditionValue) {
                            return true;
                        } else continue;
                    } else {
                        conditionValue = Double.parseDouble(cond.substring(1));
                        if(testValue > conditionValue) {
                            return true;
                        } else continue;
                    }

                case '<':
                    if(cond.length() < 2) throw new IllegalArgumentException("Condition > with no number is invalid");
                    if(cond.charAt(1) == '=') {
                        if(cond.length() < 3) throw new IllegalArgumentException("Condition >= with no number is invalid");
                        conditionValue = Double.parseDouble(cond.substring(2));
                        if(testValue <= conditionValue) {
                            return true;
                        } else continue;
                    } else {
                        conditionValue = Double.parseDouble(cond.substring(1));
                        if(testValue < conditionValue) {
                            return true;
                        } else continue;
                    }

                case '!':
                    if(cond.charAt(1) != '=') throw new IllegalArgumentException("Condition ! with no = is invalid");
                    if(cond.length() < 3) throw new IllegalArgumentException("Condition != with no number is invalid");
                    conditionValue = Double.parseDouble(cond.substring(2));
                    if(testValue != conditionValue) {
                        return true;
                    } else continue;

                case '=':
                    if(cond.length() < 2) throw new IllegalArgumentException("Condition = with no number is invalid");
                    conditionValue = Double.parseDouble(cond.substring(1));
                    if(testValue == conditionValue) {
                        return true;
                    } else continue;

                default:
                    throw new IllegalArgumentException("Illegal condition: " + cond.charAt(0));
            }
        }
        return false;
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

    public DiceRoller (ArrayList<String> tokens) {
        this(tokens, "");
    }

    @Override
    protected boolean peek(String s){
        return peek().matches(s);
    }
    private String getConditions(){
        StringBuilder output = new StringBuilder();
        consume("\\{");
        if (peek("}")) throw new IllegalArgumentException("Empty condition in diceroll found");
        while(!peek("\\}") && !peek("EOF")){
            output.append(consume());
        }
        consume("\\}");
        return output.toString();
    }

    /**
     * Evaluates a token list for dice rolling from left to right
     * @return Rolls object containing result rolls
     * @throws IllegalArgumentException Missing condition throws
     */
    public Rolls evaluateExpression() throws IllegalArgumentException {
        Rolls rolls;

        // establish initial roll
        {
            int rollCount = 1, rollMax, rollMin = 1;
            if (peek("d")) { //d(num)
                consume();
            } else if (isNumber(peek())) { //(num)d(num)
                rollCount = (int)Double.parseDouble(consume());
                consume("d");
            } else
                throw new IllegalArgumentException("No roll detected in passed dice roll expression: " + tokenStream);
            rollMax = (int)Double.parseDouble(consume());
            if(peek(":")) { //d(num):(num)
                consume();
                rollMin = rollMax;
                rollMax = (int)Double.parseDouble(consume());
            }
            if (minmax.isEmpty()) rolls = new Rolls(rollCount, rollMin, rollMax);
            else rolls = new Rolls(rollCount, rollMin, rollMax, minmax);
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
                    break;

                case "rr":
                    String rerollCond;
                    int rerollMax = 1;
                    if(peek("\\{")) rerollCond = getConditions();
                    else throw new IllegalArgumentException("Missing condition following reroll operator");
                    if(peek(":")) {
                        consume(":");
                        rerollMax = (int)Double.parseDouble(consume());
                    }
                    rolls.reroll(rerollCond, rerollMax);
                    break;

                case "drop":
                    if(peek("\\{")) {
                        rolls.drop(getConditions());
                    } else throw new IllegalArgumentException("Missing condition following drop operator");
                    break;

                case "!":
                    String explosionCond = "";
                    int explosionMax = 10;
                    if(peek("\\{")) explosionCond = getConditions();
                    if(peek(":")) {
                        consume(":");
                        explosionMax = (int)Double.parseDouble(consume());
                    }
                    if(!explosionCond.isEmpty()) rolls.explode(explosionCond, explosionMax);
                    else rolls.explode(explosionMax);
                    break;

                case "i":
                    ArrayList<String> conditions = new ArrayList<>();
                    ArrayList<String> mathTokens = new ArrayList<>();
                    if(!isNumber(peek()) && !peek("\\*")) throw new IllegalArgumentException("No condition found following I-mod operator");
                    conditions.add(consume());
                    while(peek(",")) {
                        consume();
                        if(peek("\\*") || isNumber(peek())) {
                            conditions.add(consume());
                        } else throw new IllegalArgumentException("Unrecognized condition in imod: " + peek());
                    }
                    if(!peek(":")) throw new IllegalArgumentException("No : found to declare math following I-mod conditions");
                    while(peek(":")) {
                        consume();
                        if(peek("EOF")) throw new IllegalArgumentException("Empty math section following I-mod : declaration");
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
                    }
                    break;
            }
        }
        return rolls;
    }


}

class Rolls{
    private double[] rolls;
    private final int minRoll;
    private final int maxRoll;
    private final String minmax;
    private final static Random rng = new Random();

    Rolls(double[] rolls, int min, int max, String minmax) {
        this.rolls = rolls;
        this.minRoll = min;
        this.maxRoll = max;
        this.minmax = minmax;
    }

    Rolls(double[] rolls, int die) {
        this(rolls, 1, die, "");
    }

    Rolls(int rollCount, int min, int max) throws IllegalArgumentException{
        if(rollCount > 100) throw new IllegalArgumentException("Rollplayer will not roll more than 100 dice at once");
        this(new double[rollCount], min, max, "");
        for (int i = 0; i < rollCount; i++) {
            rolls[i] = rollNumber();
        }
    }

    Rolls(int rollCount, int die) {
        this(rollCount, 1, die);
    }

    /** @param minmax Should only ever be given "min" or "max" */
    Rolls(int rollCount, int min, int max, String minmax) throws IllegalArgumentException {
        double[] rolls = new double[rollCount];
        this(rolls, min, max, minmax);
        switch (minmax) {
            case "min" -> Arrays.fill(rolls, min);
            case "max" -> Arrays.fill(rolls, max);
            default -> throw new IllegalArgumentException("Improper minmax rolls call: " + minmax);
        }
    }

    public double rollNumber() {
        /*example roll logic
        * 50 to 100
        * 100-50 = 50
        * max roll 49 + 50 + 1 = 100
        * min roll 0 + 50 = 50
        */
        return rng.nextInt(maxRoll - minRoll + 1) + minRoll;
    }

    public double[] getRolls(){
        return rolls;
    }

    public double getSum() {
        double output = 0;
        for(double i : rolls)
            output += i;
        return output;
    }

    public void keepHigher(int high) throws IllegalArgumentException{
        if(high > rolls.length || high < 1) throw new IllegalArgumentException(String.format("Cannot keep %d rolls of %d", high, rolls.length));
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
    }

    public void keepLower(int low) throws IllegalArgumentException{
        if(low > rolls.length || low < 1) throw new IllegalArgumentException(String.format("Cannot keep %d rolls of %d", low, rolls.length));
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
    }

    public void keepHighLow(int high, int low) throws IllegalArgumentException{
        if (high+low > rolls.length || high < -1 || low < -1) throw new IllegalArgumentException(String.format("Cannot keep %d higher and %d lower rolls of %d", high, low, rolls.length));
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

    /**
     * Rerolls dice that fulfill given conditions once
     * @param conditions Must be fed as (operator)(number) separated by commas. Valid operators are <, <=, >, >=, !=, and =
     */
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
            for (int retries = 0; retries < maxRepeats; retries++)
                if (Expression.satisfiesConditions(output[index], conditions)) {
                    output[index] = rollNumber();
                } else break;
        rolls = output;
    }

    /**
     * Removes rolls that satisfy input conditions removed
     * @param conditions Must be fed as (operator)(number) separated by commas. Valid operators are <, <=, >, >=, !=, and =
     */
    public void drop(String conditions) {
        ArrayList<Double> output = new ArrayList<>();
        for (double i : rolls)
            if (!Expression.satisfiesConditions(i, conditions))
                output.add(i);
        rolls = output.stream().mapToDouble(d -> d).toArray();
    }

    /**
     * Removes rolls that do not satisfy input conditions
     * @param conditions Must be fed as (operator)(number) separated by commas. Valid operators are <, <=, >, >=, !=, and =
     */
    public void keep(String conditions) {
        ArrayList<Double> output = new ArrayList<>();
        for (double i : rolls)
            if (Expression.satisfiesConditions(i, conditions))
                output.add(i);
        rolls = output.stream().mapToDouble(d -> d).toArray();
    }

    /**
     * Rolls an extra die for every die that satisfies given condition list. Those rolls can continue to cascade
     * @param conditions    Must be fed as (operator)(number) separated by commas. Valid operators are <, <=, >, >=, !=, and =
     * @param maxExplosions Maximum number of explosion dice rolls
     */
    public void explode(String conditions, int maxExplosions) throws IllegalArgumentException {
        if (maxExplosions > 50) throw new IllegalArgumentException("Rollplayer cannot set maximum explosion cap above 50");
        ArrayList<Double> output = new ArrayList<>();
        for(double i : rolls){
            output.add(i);
        }

        int explosionsRemaining = 0, explosionsCounter = 0;
        for (double i : rolls)
            if (Expression.satisfiesConditions(i, conditions))
                explosionsRemaining++;

        while (explosionsRemaining > 0 && explosionsCounter < maxExplosions) {
            explosionsCounter++;
            output.add(rollNumber());
            if (Expression.satisfiesConditions(output.getLast(), conditions))
                continue;
            explosionsRemaining--;
        }
        rolls = output.stream().mapToDouble(d -> d).toArray();
    }

    public void explode(int maxExplosions) {
        if (!minmax.equals("min") && !minmax.equals("max"))
            explode("=" + maxRoll, maxExplosions);
    }

    public void imod(ArrayList<String> conditions, ArrayList<String> mathTokens) throws IllegalArgumentException{
        ArrayList<String> math = new ArrayList<>();
        for(String cond : conditions) {
            if(!cond.equals("*")) {
                math.clear(); // don't make my dummy mistake of setting the references equal
                math.addAll(mathTokens);
                int rollIndex = (int)Double.parseDouble(cond) - 1; // reminder that tokens will be in double format
                if (rollIndex >= rolls.length || rollIndex < 0) throw new IllegalArgumentException(String.format("I-mod index out of bounds: %d of %d", rollIndex+1, rolls.length));
                math.addFirst("" + rolls[rollIndex]);
                rolls[rollIndex] = new MathSolver(math).evaluate();
            } else {
                for(int i = 0; i < rolls.length; i++) {
                    math.clear();
                    math.addAll(mathTokens);
                    math.addFirst("" + rolls[i]);
                    rolls[i] = new MathSolver(math).evaluate();
                }
            }
        }
    }
}