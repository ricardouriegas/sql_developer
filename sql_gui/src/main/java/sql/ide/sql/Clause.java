package sql.ide.sql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class Clause {
    interface Visitor<R> {
        R useClause(UseClause clause);

        R createClause(CreateClause clause);

        R dropClause(DropClause clause);

        R selectClause(SelectClause clause);

        R insertClause(InsertClause clause);
        
        R updateClause(UpdateClause clause);
        
        R deleteClause(DeleteClause clause);

        R queryClause(QueryClause clause);

        R showClause();
    }

    static class UseClause extends Clause {
        UseClause(String path) {
            this.path = path;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.useClause(this);
        }

        final String path;
    }

    static class CreateClause extends Clause {
        CreateClause(Token name, List<List<String>> columnsDefinition) {
            this.name = name;
            this.columnsDefinition = columnsDefinition;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.createClause(this);
        }

        final Token name;
        final List<List<String>> columnsDefinition;
    }

    static class SelectClause extends Clause {
        SelectClause(Pair<List<Pair<Expression, Token>>, Boolean> columns, Token table_name, Expression where_expression, List<String> columns_order,
                int limit) {
            this.columns = columns;
            this.table_name = table_name;
            this.where_expression = where_expression;
            this.columns_order = columns_order;
            this.limit = limit;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.selectClause(this);
        }

        final Pair<List<Pair<Expression, Token>>, Boolean> columns;
        final Token table_name;
        final Expression where_expression;
        final List<String> columns_order;
        final int limit;
    }

    static class DropClause extends Clause {
        DropClause(Token token) {
            this.token = token;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.dropClause(this);
        }
        
        final Token token;
    }
    
    static class InsertClause extends Clause {
        InsertClause(Token token, HashMap<String, Expression> valuesMap) {
            this.token = token;
            this.valuesMap = valuesMap;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.insertClause(this);
        }
        
        final Token token;
        final HashMap<String, Expression> valuesMap;
        
    }
    
    //// new Clause.UpdateClause(table_name.lexeme, column_name.lexeme, value, where_expression);
    static class UpdateClause extends Clause {
        UpdateClause(Token table_name, HashMap<String, Expression> valuesMap, Expression where_expression) {
            this.table_name = table_name;
            this.valuesMap = valuesMap;
            this.where_expression = where_expression;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.updateClause(this);
        }
        
        final Token table_name;
        final HashMap<String, Expression> valuesMap;
        final Expression where_expression;
        
    }
    
    //// new Clause.DeleteClause(table_name.lexeme, where_expression);
    static class DeleteClause extends Clause {
        DeleteClause(Token table_name, Expression where_expression) {
            this.table_name = table_name;
            this.where_expression = where_expression;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.deleteClause(this);
        }

        final Token table_name;
        final Expression where_expression;
    }

    // query clause
    static class QueryClause extends Clause {
        QueryClause(List<Expression> query) {
            this.query = query;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return null;
        }

        final List<Expression> query;
    }

    static class ShowClause extends Clause {
        ShowClause() {
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.showClause();
        }
    }
    

    abstract <R> R accept(Visitor<R> visitor);
}
