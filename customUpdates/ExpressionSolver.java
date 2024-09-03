package at.jku.isse.gitecco.core.solver;

import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;
import org.anarres.cpp.Token;
import org.anarres.cpp.featureExpr.*;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.nary.cnf.LogOp;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;

import java.util.*;

/**
 * Class for finding positive solutions for preprocessor conditions
 */
public class ExpressionSolver {
    private String expr;
    private Model model;
    private final List<IntVar> vars;
    private final Stack<Variable> stack;
    private boolean isIntVar = false;

    /**
     * Create new solver with a given expression to solve.
     *
     * @param expr
     */
    public ExpressionSolver(String expr) {
        this.expr = expr;
        this.model = new Model();
        this.vars = new LinkedList<>();
        this.stack = new Stack<>();
    }

    /**
     * Create new empty solver.
     */
    public ExpressionSolver() {
        this.expr = "";
        this.model = new Model();
        this.vars = new LinkedList<>();
        this.stack = new Stack<>();
    }

    /**
     * Resets the solver so a new expression can be solved.
     */
    public void reset() {
        this.expr = "";
        this.model = new Model();
        this.vars.clear();
        this.stack.clear();
    }

    /**
     * Resets the solver and assigns a new expression to solve.
     *
     * @param expr
     */
    public void reset(String expr) {
        this.expr = expr;
        this.model = new Model();
        this.vars.clear();
        this.stack.clear();
    }

    /**
     * Sets a new expression for the solver.
     *
     * @param expr
     */
    public void setExpr(String expr) {
        this.expr = expr;
    }

    public Model getModel() {
        return this.model;
    }

    /**
     * Solves the expression currently assigned to this solver.
     * Returns a Map with the Feature as key and the value to be assigned as an Integer.
     *
     * @return The Map with the solution.
     */
    public Map<Feature, Integer> solve() {
        Map<Feature, Integer> assignments = new HashMap<>();

        if (this.expr.contains("'A' == '\\301'"))
            this.expr = this.expr.replace("'A' == '\\301'", "A == 301");
        if (this.expr.contains("__has_feature(address_sanitizer)"))
            this.expr = this.expr.replace("__has_feature(address_sanitizer)", "__has_feature)");
        if (this.expr.contains("QT_VERSION_CHECK(5, 0, 0)"))
            this.expr = this.expr.replace("QT_VERSION_CHECK(5, 0, 0)", "QT_VERSION_CHECK");
        if (this.expr.contains("'$' == 0x24 && '@' == 0x40 && '`' == 0x60 && '~' == 0x7e"))
            return null;
        // Add the parsed problem to the solver model
        model.post(getBoolVarFromExpr(this.expr).extension());

        // Actual solving
        Solution solution = model.getSolver().findSolution();
        if (solution != null) {
            for (IntVar var : vars) {
                try {
                    assignments.put(new Feature(var.getName()), solution.getIntVal(var));
                } catch (SolverException se) {
                    System.out.println("var name: " + var.getName() + " exception:" + se);
                }
            }
        } else {
            // System.err.println("DEAD CODE: No solution found for " + expr);
            return null;
        }

        return Collections.unmodifiableMap(assignments);
    }

    /**
     * Builds a constraint from the given set of FeatureImplications for one feature.
     * This will build a chain of if then else expressions for the solver model.
     *
     * @param feature
     * @param implications
     */
    public void addClause(Feature feature, Queue<FeatureImplication> implications) {
        LogOp elsePart = null;

        while (!implications.isEmpty()) {
            FeatureImplication im = implications.remove();
            BoolVar ifPart = getBoolVarFromExpr(im.getCondition());
            BoolVar thenPart = getBoolVarFromExpr(im.getValue());

            if (elsePart == null) {
                elsePart = LogOp.ifThenElse(ifPart, thenPart, getBoolVarFromExpr(feature.getName()).not());
            } else {
                elsePart = LogOp.ifThenElse(ifPart, thenPart, elsePart);
            }
        }
        if (elsePart != null) model.addClauses(elsePart);
    }

    /**
     * Helper method for parsing and traversing an expression given as String
     * If the last variable on the stack is not a boolean variable
     * we can assume the condition string was only a single number literal.
     * Thus we can translate this to a boolean variable just like the C preprocessor --> > 0 true else false.
     *
     * @param expr
     * @return
     */
    public BoolVar getBoolVarFromExpr(String expr) {
        isIntVar = false;
        if (expr.contains(" - 0"))
            expr = expr.replace(" - 0", "");
        if ((expr.contains("(])")))
            expr = expr.replace("(]) &&", "b4_location_if &&");
        if (expr.contains("]") && !expr.contains("[")) {
            expr = expr.replace("]", "b4_location_if");
        }
        if (expr.contains("b4_locations_if")) {
            expr = expr.replace("[", "");
            expr = expr.replace("]", "");
        }
        if (expr.contains("GLIB_CHECK_VERSION(")) {
            expr = "GLIB_CHECK_VERSION";
        }
        traverse(new FeatureExpressionParser(expr).parse());
        Variable var = stack.pop();

        return var.asBoolVar();
    }

    /**
     * Helper Method
     * Traverses the expression tree which was created before by the FeatureExpressionParser.
     *
     * @param expr the expression tree to be parsed.
     */
    public void traverse(FeatureExpression expr) {
        try {
            if (expr == null) return;
            ExpressionSolver ex = new ExpressionSolver();
            BoolVar boolVar;
            if (expr instanceof Name) {
                String name = ((Name) expr).getToken().getText();
                Variable check = checkVars(model, name);
                if (check == null) {
                    if (isIntVar) {
                        IntVar iv = model.intVar(name, Short.MIN_VALUE, Short.MAX_VALUE);
                        vars.add(iv);
                        stack.push(iv);
                    } else {
                        IntVar iv = model.intVar(name, 0, 1);
                        BoolVar bv = iv.ne(0).boolVar();
                        vars.add(iv);
                        stack.push(bv);
                    }
                } else {
                    if (check instanceof IntVar && !isIntVar) {
                        stack.push(((IntVar) check).ne(0).boolVar());
                    } else {
                        stack.push(check);
                    }
                }
            } else if (expr instanceof AssignExpr) {
                System.err.println("AssignExpr should not appear in a normal condition!");
            } else if (expr instanceof NumberLiteral) {
                try {
                    String numText = ((NumberLiteral) expr).getToken().getText().replaceAll("L", "");

                    // Handle "U" suffix specifically
                    if (numText.endsWith("U")) {
                        numText = numText.replace("U", "");
                    }

                    // Parse the number after processing
                    stack.push(model.intVar(Double.valueOf(numText).intValue()));
                } catch (NumberFormatException e) {
                    try {
                        String numText = ((NumberLiteral) expr).getToken().getText().replaceAll("L", "").replace("U", "");
                        stack.push(model.intVar(Long.decode(numText).intValue()));
                    } catch (NumberFormatException e1) {
                        System.err.println("the given number format is not compatible with the solver!" +
                                "\n number: " + ((NumberLiteral) expr).getToken().getText());
                        stack.push(model.intVar(Long.decode(((NumberLiteral) expr).getToken().getText().replaceAll("0000UL", "").replace("U", "")).intValue()));
                    }
                }
                isIntVar = true;
            } else if (expr instanceof SingleTokenExpr) {
                IntVar right;
                IntVar left;
                BoolVar bright;
                BoolVar bleft;
                SingleTokenExpr e = (SingleTokenExpr) expr;
                switch (e.getToken().getType()) {
                    case Token.GE:      //greater than or equal ">="
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.ge(right).boolVar());
                        break;
                    case Token.EQ:      //equal "=="
                        if (isIntVar) {
                            right = stack.pop().asIntVar();
                            left = stack.pop().asIntVar();
                            stack.push(left.eq(right).intVar());
                        } else {
                            bright = stack.pop().asBoolVar();
                            bleft = stack.pop().asBoolVar();
                            stack.push(bleft.eq(bright).boolVar());
                        }
                        break;
                    case Token.LE:      //less than or equal "<="
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.le(right).boolVar());
                        break;
                    case Token.LAND:    //and "&&"
                        bright = stack.pop().asBoolVar();
                        bleft = stack.pop().asBoolVar();
                        stack.push(bleft.and(bright).boolVar());
                        break;
                    case Token.LT:      //less than "<"
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.lt(right).boolVar());
                        break;
                    case Token.GT:      //greater than ">"
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.gt(right).boolVar());
                        break;
                    case Token.LOR:     //or "||"
                        bright = stack.pop().asBoolVar();
                        bleft = stack.pop().asBoolVar();
                        stack.push(bleft.or(bright).boolVar());
                        break;
                    case Token.NE:      //not equal "!="
                        if (isIntVar) {
                            right = stack.pop().asIntVar();
                            left = stack.pop().asIntVar();
                            stack.push(left.ne(right).intVar());
                        } else {
                            bright = stack.pop().asBoolVar();
                            bleft = stack.pop().asBoolVar();
                            stack.push(bleft.ne(bright).boolVar());
                        }
                        break;
                    case Token.PLUS:    //plus "+"
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.add(right).intVar());
                        break;
                    case Token.MINUS:   //minus "-"
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.sub(right).intVar());
                        break;
                    case Token.BAND:    //bitwise and "&"
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.and(right).intVar());
                        break;
                    case Token.BOR:     //bitwise or "|"
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.or(right).intVar());
                        break;
                    case Token.BXOR:    //bitwise xor "^"
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.xor(right).intVar());
                        break;
                    case Token.LNOT:    //not "!"
                        bright = stack.pop().asBoolVar();
                        stack.push(bright.not().boolVar());
                        break;
                    case Token.BNOT:    //bitwise not "~"
                        right = stack.pop().asIntVar();
                        stack.push(right.not().intVar());
                        break;
                }
            } else if (expr instanceof UnaryExpr) {
                traverse(((UnaryExpr) expr).getExpr());
                switch (((UnaryExpr) expr).getToken().getType()) {
                    case Token.MINUS:
                        IntVar var = stack.pop().asIntVar();
                        stack.push(var.neg().intVar());
                        break;
                    case Token.PLUS:
                        break;
                    case Token.LNOT:
                        BoolVar varBool = stack.pop().asBoolVar();
                        stack.push(varBool.not());
                        break;
                    case Token.BNOT:
                        IntVar varInt = stack.pop().asIntVar();
                        stack.push(varInt.not().intVar());
                        break;
                }
            } else if (expr instanceof BinaryExpr) {
                traverse(((BinaryExpr) expr).getLeft());
                traverse(((BinaryExpr) expr).getRight());
                SingleTokenExpr s = new SingleTokenExpr(((BinaryExpr) expr).getToken());
                traverse(s);
            }
        } catch (Exception ex) {
            System.out.println(expr.toString());
            ex.printStackTrace();
        }
    }

    /**
     * Helper method for checking if a variable with the given name already exists in the model.
     *
     * @param model
     * @param varName
     * @return
     */
    private static Variable checkVars(Model model, String varName) {
        Variable[] vars = model.getVars();
        for (Variable var : vars) {
            if (var.getName().equals(varName))
                return var;
        }
        return null;
    }
}
