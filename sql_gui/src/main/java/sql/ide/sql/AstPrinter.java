package sql.ide.sql;

import java.util.List;
import java.util.Map.Entry;

public class AstPrinter implements Expression.Visitor<String>, Clause.Visitor<String> {
    String print(Expression expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpression(Expression.Binary expr) {
        return parenthesize(expr.operator.lexeme,
                expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpression(Expression.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpression(Expression.Literal expr) {
        if (expr.value == null)
            return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpression(Expression.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    /**************************************************************************/
    /******************************* CLAUSE *************************************/
    public String print(Clause clause) {
        return clause.accept(this);
    }

    @Override
    public String useClause(Clause.UseClause clause) {
        // Implementation for UseClause printing
        return "USE " + clause.path;
    }

    @Override
    public String dropClause(Clause.DropClause clause) {
        // Implementation for DropClause printing
        return "DROP " + clause.token.lexeme;
    }

    @Override
    public String updateClause(Clause.UpdateClause clause) {
        // Implementation for UpdateClause printing
        StringBuilder builder = new StringBuilder();
        builder.append("UPDATE ").append(clause.table_name.lexeme).append(" ");
        builder.append("SET ");
        for (int i = 0; i < clause.valuesMap.size(); i++) {
            // HashMap<String, Expression> valuesMap
            Entry<String, Expression> entry = (Entry<String, Expression>) clause.valuesMap.entrySet().toArray()[i];
            builder.append(entry.getKey()).append(" = ").append(printExpression(entry.getValue()));

            if (i != clause.valuesMap.size() - 1) {
                builder.append(", ");
            }
        }
        builder.append(" ");
        if (clause.where_expression != null) {
            builder.append("WHERE ").append(printExpression(clause.where_expression)).append(" ");
        }
        return builder.toString();
    }

    @Override
    public String deleteClause(Clause.DeleteClause clause) {
        // Implementation for DeleteClause printing
        StringBuilder builder = new StringBuilder();
        builder.append("DELETE FROM ").append(clause.table_name.lexeme).append(" ");
        if (clause.where_expression != null) {
            builder.append("WHERE ").append(printExpression(clause.where_expression)).append(" ");
        }
        return builder.toString();
    }

    @Override
    public String selectClause(Clause.SelectClause clause) {
        // Implementation for SelectClause printing
        // Use StringBuilder to construct the string representation
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT ");

        if (clause.columns != null) {
            if (clause.columns.getY()) {
                builder.append("DISTINCT ");
            } else {
                for (int i = 0; i < clause.columns.getX().size(); i++) {
                    // Pair<List<Pair<Expression, Token>>, Boolean> columns
                    List<Pair<Expression, Token>> pair = clause.columns.getX();
                    for (int j = 0; j < pair.size(); j++) {
                        if (pair.get(j).getY() == null) 
                            builder.append(pair.get(j).getX().accept(this));
                        else
                            builder.append(pair.get(j).getX().accept(this)).append(" AS ").append(pair.get(j).getY().lexeme);
                        if (j != pair.size() - 1) {
                            builder.append(", ");
                        }
                    }

                    if (i != clause.columns.getX().size() - 1) {
                        builder.append(", ");
                    }
                }
            }
        } else {
            builder.append("* ");
        }
        if (clause.table_name != null) {
            builder.append(" FROM ").append(clause.table_name.lexeme).append(" ");
        }

        if (clause.where_expression != null) {
            builder.append("WHERE ").append(printExpression(clause.where_expression)).append(" ");
        }

        if (clause.columns_order != null && !clause.columns_order.isEmpty()) {
            builder.append("ORDER BY ").append(String.join(", ", clause.columns_order)).append(" ");
        }

        if (clause.limit != -1) {
            builder.append("LIMIT ").append(clause.limit).append(" ");
        }

        return builder.toString();
    }

    @Override
    public String insertClause(Clause.InsertClause clause) {
        // Implementation for InsertClause printing
        StringBuilder builder = new StringBuilder();
        builder.append("INSERT INTO ").append(clause.token.lexeme).append(" ");
        
        builder.append("VALUES (");
        for (int i = 0; i < clause.valuesMap.size(); i++) {
            // HashMap<String, Expression> valuesMap
            Entry<String, Expression> entry = (Entry<String, Expression>) clause.valuesMap.entrySet().toArray()[i];
            builder.append(entry.getKey()).append(" = ").append(printExpression(entry.getValue()));

            if (i != clause.valuesMap.size() - 1) {
                builder.append(", ");
            }
        }
        builder.append(") ");
        return builder.toString();
    }

    @Override 
    public String createClause(Clause.CreateClause clause) {
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE ").append(clause.name.lexeme).append(" ");
        builder.append("(");
        for (int i = 0; i < clause.columnsDefinition.size(); i++) {
            builder.append(clause.columnsDefinition.get(i).get(0)).append(" ");
            builder.append(clause.columnsDefinition.get(i).get(1)).append(" ");
            if (clause.columnsDefinition.get(i).size() == 3) {
                builder.append(clause.columnsDefinition.get(i).get(2)).append(" ");
            }
            if (i != clause.columnsDefinition.size() - 1) {
                builder.append(", ");
            }
        }
        builder.append(")");
        return builder.toString();
    }

    @Override 
    public String queryClause(Clause.QueryClause clause) {
        StringBuilder builder = new StringBuilder();
        builder.append("QUERY ").append(clause.query);
        return builder.toString();
    }

    @Override
    public String showClause() {
        return "SHOW TABLES";
    }

    private String printExpression(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof String) {
            return "'" + value + "'";
        } else if (value instanceof Number) {
            return value.toString();
        } else {
            throw new IllegalArgumentException("Unsupported value type: " + value.getClass().getSimpleName());
        }
    }    

    @Override
    public String visitFunctionCallExpression(Expression.FunctionCall expr) {
        StringBuilder builder = new StringBuilder();
        builder.append(expr.name.lexeme).append("(");
        for (int i = 0; i < expr.arguments.size(); i++) {
            builder.append(printExpression(expr.arguments.get(i)));
            if (i != expr.arguments.size() - 1) {
                builder.append(", ");
            }
        }
        builder.append(")");
        return builder.toString();
    }

    private String printExpression(Token expr) {
        if (expr instanceof Token) {
            return ((Token) expr).lexeme;
        }

        return expr.lexeme;
    }
    

    private String printExpression(Expression expr) {
        return expr.accept(this);
    }

    private String parenthesize(String name, Expression... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expression expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }

}
