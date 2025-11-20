package dev.infernity.rollplayer.rollplayerlib3;

import java.util.ArrayList;
import java.util.function.DoubleBinaryOperator;

@FunctionalInterface
interface Node{
    enum BinaryOp implements DoubleBinaryOperator {
        PLUS     ("+", Double::sum),
        MINUS    ("-", (l, r) -> l - r),
        TIMES    ("*", (l, r) -> l * r),
        DIVIDE   ("/", (l, r) -> l / r),
        EXPONENT ("^", Math::pow);

        private final String symbol;
        private final DoubleBinaryOperator operation;

        BinaryOp (final String symbol, final DoubleBinaryOperator operation) {
            this.symbol = symbol;
            this.operation = operation;
        }
        public String getSymbol() {
            return symbol;
        }

        @Override
        public double applyAsDouble(final double left, final double right) {
            return operation.applyAsDouble(left, right);
        }
    }
    double evaluate();
}

/**
 * An AST (Abstract Syntax Tree) parser for math processing in Rollplayer.
 * <p>Construct it with an ArrayList of String tokens, sanitized in the way that Parser.stringTokenizer() outputs. This will generate the tree.
 * <p>Once the object is constructed, the evaluate() method will evaluate the math expression and return it as a double. If the tree encountered an error while parsing, this will return 0.
 * <p>If the constructor encounters an error while parsing, isError() will return true and its sister method getError() will output the error String.
 * This should be caught before you try to evaluate the AST unless you are absolutely sure your input will work.
 */
public class MathSolver extends Expression {
    Node rootNode;
    String errorCode;

    public MathSolver(ArrayList<String> tokens) {
        super(tokens);
        rootNode = parseAdd(); // despite the confusing name this generates the entire tree
        if (rootNode instanceof ErrorNode) errorCode = ((ErrorNode) rootNode).getError();
        if (!peek("EOF")) errorCode = "Reached end of math expression while solving: " + tokenStream.subList(pointer, tokenStream.size());
    }

    public double evaluate() {
        return rootNode.evaluate();
    }

    public boolean isError() {
        return errorCode != null;
    }
    public String getError() {
        return errorCode;
    }

    //for more information on ASTs i used this lecture: https://andrewcmyers.github.io/oodds/lecture.html?id=parsing

    // operations by tier
    // +- */ ^ number ()

    // Add > Mult (+- Mult)*
    private Node parseAdd() {
        Node e = parseMult();
        if (e instanceof ErrorNode) return e;
        while (peek(Node.BinaryOp.PLUS.getSymbol()) || peek(Node.BinaryOp.MINUS.getSymbol())) {
            if(peek(Node.BinaryOp.PLUS.getSymbol())) {
                consume();
                Node f = parseMult();
                if (f instanceof ErrorNode) return f;
                e = new Binary(Node.BinaryOp.PLUS, e, f);
            } else {
                consume();
                Node f = parseMult();
                if (f instanceof ErrorNode) return f;
                e = new Binary(Node.BinaryOp.MINUS, e, f);
            }
        }
        return e;
    }

    // Mult > Pow (*/ Pow)*
    private Node parseMult() {
        Node e = parsePow();
        if (e instanceof ErrorNode) return e;
        while (peek(Node.BinaryOp.TIMES.getSymbol()) || peek(Node.BinaryOp.DIVIDE.getSymbol())) {
            if(peek(Node.BinaryOp.TIMES.getSymbol())) {
                consume();
                Node f = parsePow();
                if (f instanceof ErrorNode) return f;
                e = new Binary(Node.BinaryOp.TIMES, e, f);
            } else {
                consume();
                Node f = parsePow();
                if (f instanceof ErrorNode) return f;
                e = new Binary(Node.BinaryOp.DIVIDE, e, f);
            }
        }
        return e;
    }

    // Pow > Num (^ Num)*
    private Node parsePow() {
        Node e = parseNum();
        if (e instanceof ErrorNode) return e;
        while (peek(Node.BinaryOp.EXPONENT.getSymbol())) {
            consume();
            Node f = parseNum();
            if (f instanceof ErrorNode) return f;
            e = new Binary(Node.BinaryOp.EXPONENT, e, f);
        }
        return e;
    }

    // Num > double | (Add)
    private Node parseNum() {
        if (isNumber(peek())) {
            Node e = new Number(Double.parseDouble(consume()));
            if(isNumber(peek())) return new ErrorNode("MathSolver has been passed two successive number tokens\nThis is likely because the expression has a roll modifier which has been improperly followed by a number");
            return e;
        }
        else {
            consume("(");
            if (!errorReturn.isEmpty()) {
                if (peek("EOF")) return new ErrorNode("Reached end of math expression while solving: " + tokenStream);
                else return new ErrorNode("Neither number nor parenthesis found as argument: " + peek() + " in " + tokenStream);
            }
            Node e = parseAdd();
            consume(")");
            if (!errorReturn.isEmpty()) return new ErrorNode(errorReturn);
            return e;
        }
    }
}

record Binary(BinaryOp operator, Node left, Node right) implements Node {
    public double evaluate() {
        return operator.applyAsDouble(left.evaluate(), right.evaluate());
    }
}

record Number(double value) implements Node {
    public double evaluate() {
        return value;
    }
}

record ErrorNode(String error) implements Node {
    public double evaluate() {
        return 0;
    }
    public String getError() {
        return error;
    }
}