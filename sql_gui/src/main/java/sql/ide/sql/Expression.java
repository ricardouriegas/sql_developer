package sql.ide.sql;

import java.util.*;

/**
 * This class should represent a node in the AST
 * i use the term Expression, 
 * because in compilers the AST is a tree of expressions
 */
/**
 * The reason i have a tree class is bc it isn't owned by any other class
 * exists for communicate the parser and interpreter
 */
/**
 * Here we use metaprogramming to make thee code more dynamiccc
 */
abstract class Expression {
  interface Visitor<R> {
    R visitBinaryExpression(Binary expr);
    R visitGroupingExpression(Grouping expr);
    R visitLiteralExpression(Literal expr);
    R visitUnaryExpression(Unary expr);
    R visitFunctionCallExpression(FunctionCall expr);
  }

  static class Binary extends Expression {
    Binary(Expression left, Token operator, Expression right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpression(this);
    }

    final Expression left;
    final Token operator;
    final Expression right;
  }

  static class Grouping extends Expression {
    Grouping(Expression expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpression(this);
    }

    final Expression expression;
  }

  static class Literal extends Expression {
    Literal(Object value, Boolean isColumnName) {
      this.value = value;
      this.isColumnName = isColumnName;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpression(this);
    }

    final Object value;
    final Boolean isColumnName;
  }

  static class Unary extends Expression {
    Unary(Token operator, Expression right) {
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpression(this);
    }

    final Token operator;
    final Expression right;
  }

  static class FunctionCall extends Expression {
    FunctionCall(Token name, List<Expression> arguments) {
      this.name = name;
      this.arguments = arguments;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFunctionCallExpression(this);
    }

    final Token name;
    final List<Expression> arguments;
  }

  @Override 
  public String toString() {
    // for now just print the AST
    // TODO: print in a more natural way
    return accept(new AstPrinter());
  }

  abstract <R> R accept(Visitor<R> visitor);
}
