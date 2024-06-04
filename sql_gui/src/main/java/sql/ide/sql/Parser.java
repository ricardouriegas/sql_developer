package sql.ide.sql;

import static sql.ide.sql.TokenType.*;

import java.util.*;

// import static java.lang.Integer.parseInt;

/**
 * A parser really has two jobs:
 * 
 * 1. Given a valid sequence of tokens, produce a corresponding syntax tree.
 * 
 * 2. Given an invalid sequence of tokens, detect any errors and tell the user
 * about their mistakes.
 */
public class Parser {
    static class ParseError extends RuntimeException {
        // This is a simple sentinel class used to unwind the parser
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // The parser
    public List<Clause> parse() {
        try {
            return program();
        } catch (ParseError error) {
            // The parser promises not to crash or hang on invalid syntax,
            // but it doesn’t promise to return a usable syntax tree
            // if an error is found of course
            synchronize();
            return null;
        }
    }

    // program = (statement ';')* EOF
    private List<Clause> program() {
        List<Clause> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(sentence());
            consume(SEMICOLON, "Expected ; after statement.");
        }
        return statements;
    }

    // statement = useStmnt | createTableStmnt | insertStmnt | updateStmnt
    // | selectStmnt | deleteStmnt | dropStmnt | showTableStmnt
    private Clause sentence() {
        if (match(USE))
            return useStmnt();
        if (match(CREATE))
            return createStmnt();
        if (match(DROP))
            return dropStmnt();
        if (match(SELECT))
            return selectStmnt();
        if (match(INSERT))
            return insertStmnt();
        if (match(UPDATE))
            return updateStmnt();
        if (match(DELETE))
            return deleteStmnt();
        if (match(SHOW))
            return showStmnt();

        throw error(peek(), "Expected statement.");
    }

    /**************************************************************************/
    /******************************** USE DATABASE ******************************/
    /**************************************************************************/
    // useStmnt = USE dbName
    // dbName = STRING
    private Clause useStmnt() {
        Token name = consume(STRING, "Expected database name after.");
        return new Clause.UseClause(name.lexeme);
    }

    /**************************************************************************/
    /******************************** CREATE TABLE ******************************/
    /**************************************************************************/

    // createTableStmnt = CREATE TABLE tableName '('columnDef (',' columnDef)*')'
    // tableName = ID
    // ID = ALPHA (ALPHA | DIGIT | '_' |)*

    private Clause createStmnt() {
        consume(TABLE, "Expected keyword TABLE after CREATE.");
        Token table_name = consume(IDENTIFIER, "Expected table name after CREATE TABLE.");
        consume(LEFT_PAREN, "Expected ( after table name.");

        List<List<String>> columnDefinition = new ArrayList<>();
        while (!check(RIGHT_PAREN)) {
            columnDefinition.add(columnDefinition());
            if (!match(COMMA))
                break;
        }

        consume(RIGHT_PAREN, "Expected ) after column definitions.");

        return new Clause.CreateClause(table_name, columnDefinition);
    }

    // columnDef = columnName dataType (constraint)?
    private List<String> columnDefinition() {
        Token column_name = consume(IDENTIFIER, "Expected column name.");
        String data_type = dataType();

        List<String> result = new ArrayList<>();
        result.add(column_name.lexeme);
        result.add(data_type);

        while (true) {
            String constraint = constraint();
            if (constraint == null)
                break;
            result.add(constraint);
        }

        return result;
    }

    // dataType = NUMBER | DATE | STRING | BOOLEAN
    private String dataType() {
        if (match(NUMBER_DATA_TYPE))
            return "NUMBER";
        if (match(STRING_DATA_TYPE))
            return "STRING";
        if (match(DATE_DATA_TYPE))
            return "DATE";
        if (match(BOOLEAN_DATA_TYPE))
            return "BOOLEAN";

        throw error(peek(), "Expected data type.");
    }

    // constraint = NOT NULL | PRIMARY KEY | UNIQUE
    private String constraint() {
        if (match(PRIMARY)) {
            if (match(KEY))
                return "PRIMARY KEY";

            throw error(peek(), "Expected KEY after PRIMARY.");
        }
        if (match(NOT)) {
            if (match(NULL))
                return "NOT NULL";

            throw error(peek(), "Expected NULL after NOT.");
        }
        if (match(UNIQUE))
            return "UNIQUE";

        return null;
    }

    /**************************************************************************/
    /******************************** DROP TABLE ******************************/
    /**************************************************************************/
    // dropStmnt = DROP TABLE tableName
    private Clause dropStmnt() {
        consume(TABLE, "Expected keyword TABLE after DROP.");
        Token name = consume(IDENTIFIER, "Expected table name at drop sentence.");
        return new Clause.DropClause(name);
    }

    /**************************************************************************/
    /******************************** SELECT **********************************/
    /**************************************************************************/
    // selectStmnt = selectClause (fromClause)? (whereClause)? (groupbyClause)?
    // (orderbyClause)? (limitClause)?
    // whereClause = WHERE expression
    // groupbyClause = GROUP BY expression (',' expression)*
    // orderbyClause = ORDER BY expression (',' expression)*
    // limitClause = LIMIT addExpression
    private Clause selectStmnt() {
        // list of expressions and distinct boolean
        Pair<List<Pair<Expression, Token>>, Boolean> select_clause = selectClause();

        // TODO: Implement the from correctly. Should be able of have a subquery
        Token from_clause = null;
        if (match(FROM)) {
            from_clause = fromClause();
        }

        Expression where_expression = null;
        if (match(WHERE)) {
            where_expression = whereClause();
        }

        // TODO: Implement the group by clause
        Expression groupby_expression = null;
        if (match(GROUP)) {
            consume(BY, "Expected BY after GROUP.");
            groupby_expression = expression();
        }

        List<String> columns_order = null;
        if (match(ORDER)) {
            consume(BY, "Expected BY after ORDER.");
            columns_order = orderBy();
        }

        int limit = -1;
        if (match(LIMIT)) {
            limit = limitClause();
        }

        return new Clause.SelectClause(
                select_clause,
                from_clause,
                where_expression,
                columns_order,
                limit);
    }

    // selectClause = SELECT (DISTINCT)? selectList
    private Pair<List<Pair<Expression, Token>>, Boolean> selectClause() {
        boolean distinct = match(DISTINCT);
        List<Pair<Expression, Token>> selectList = selectList();
        if (distinct == true) {
            if (selectList.size() > 1) {
                throw error(peek(), "Expected only one column after DISTINCT.");
            }
        }

        return new Pair<>(selectList, distinct);

    }

    // selectList = '*' | (expression (AS ID)? (',' expression (AS ID)?)*)
    private List<Pair<Expression, Token>> selectList() {
        List<Pair<Expression, Token>> columns = new ArrayList<>();
        Pair<Expression, Token> column = null;

        if (match(STAR)) {
            columns.add(new Pair<>(new Expression.Literal("*", false), null));
            return columns;
        }

        column = new Pair<>(expression(), null);
        if (match(AS)) {
            if (match(IDENTIFIER))
                // alias = previous();
                columns.add(new Pair<>(column.getX(), previous()));
            else
                ErrorHandler.error(peek(), "Expected alias after AS.");
        } else {
            columns.add(column);
        }

        while (match(COMMA)) {
            // columns.add(new Pair<>(expression(), alias));
            column = new Pair<>(expression(), null);
            if (match(AS)) {
                if (match(IDENTIFIER))
                    columns.add(new Pair<>(column.getX(), previous()));
                else
                    ErrorHandler.error(peek(), "Expected alias after AS.");
            } else {
                columns.add(column);
            }
        }

        return columns;
    }

    // fromClause = FROM (tableName | '(' selectStmnt ')')
    private Token fromClause() {
        // if (match(LEFT_PAREN)) {
        // Clause select = selectStmnt();
        // consume(RIGHT_PAREN, "Expected ) after select statement.");
        // return select;
        // }

        return consume(IDENTIFIER, "Expected table name after FROM.");
    }

    // <WHERE_CLAUSE>::= WHERE <EXPRESSION>
    private Expression whereClause() {
        return expression();
    }

    // expression = orExpression
    // orExpression = andExpression (OR andExpression)*
    private Expression expression() {
        Expression andExpression = andExpression();

        while (match(OR)) {
            Token operator = previous();
            Expression right = andExpression();
            andExpression = new Expression.Binary(andExpression, operator, right);
        }

        return andExpression;
    }

    // andExpression = notExpression (AND notExpression)*
    private Expression andExpression() {
        Expression notExpression = notExpression();

        while (match(AND)) {
            Token operator = previous();
            Expression right = notExpression();
            notExpression = new Expression.Binary(notExpression, operator, right);
        }

        return notExpression;
    }

    // notExpression = (NOT)? isNullExpression
    private Expression notExpression() {
        if (match(NOT)) {
            Token operator = previous();
            Expression right = isNullExpression();
            return new Expression.Unary(operator, right);
        }

        return isNullExpression();
    }

    // isNullExpression = equalExpression (IS (NOT)? NULL)?
    private Expression isNullExpression() {
        Expression left = equalExpression();

        if (match(IS)) {
            Token operator = previous();
            if (match(NOT)) {
                operator = previous();
                consume(NULL, "Expected NULL after NOT.");
            } else {
                consume(NULL, "Expected NULL after IS.");
            }
            return new Expression.Unary(operator, left);
        }

        return left;
    }

    // equalExpression = compareExpression (('=' | '!=') compareExpression)*
    private Expression equalExpression() {
        Expression left = compareExpression();

        while (match(EQUAL_EQUAL, BANG_EQUAL, EQUAL)) {
            Token operator = previous();
            Expression right = compareExpression();
            left = new Expression.Binary(left, operator, right);
        }

        return left;
    }

    // compareExpression = concatExpression (('>' | '>=' | '<' | '<=')
    // concatExpression)*
    private Expression compareExpression() {
        Expression left = concatExpression();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expression right = concatExpression();
            left = new Expression.Binary(left, operator, right);
        }

        return left;
    }

    // concatExpression = addExpression ('||' addExpression)*
    private Expression concatExpression() {
        Expression left = addExpression();

        while (match(PIPE_PIPE)) {
            Token operator = previous();
            Expression right = addExpression();
            left = new Expression.Binary(left, operator, right);
        }

        return left;
    }

    // addExpression = multiplyExpression (('+'|'-') multiplyExpression)*
    private Expression addExpression() {
        Expression left = multiplyExpression();

        while (match(PLUS, MINUS)) {
            Token operator = previous();
            Expression right = multiplyExpression();
            left = new Expression.Binary(left, operator, right);
        }

        return left;
    }

    // multiplyExpression = unaryExpression (('*'|'/'|'div'|'mod'|'%')
    // unaryExpression)*
    private Expression multiplyExpression() {
        Expression left = unaryExpression();

        while (match(STAR, SLASH, DIV, MOD, PORCENTAJE)) {
            Token operator = previous();
            Expression right = unaryExpression();
            left = new Expression.Binary(left, operator, right);
        }

        return left;
    }

    // unaryExpression = ('-' unaryExpression) | funcCallExpression |
    // primaryExpression
    // funcCallExpression = funcName ('(' (expression (',' expression)*)? ')')*
    // primaryExpression = TRUE | FALSE | NULL | NUMBER | STRING | ID | '*' |
    // '(' expression ')'

    // # Functions
    // funcName = UCASE | FLOOR | ROUND | RAND | COUNT | MIN | MAX | SUM | AVG
    private Expression unaryExpression() {
        if (match(MINUS)) {
            Token operator = previous();
            Expression right = unaryExpression();
            return new Expression.Unary(operator, right);
        }

        if (match(
                UCASE, LCASE, CAPITALIZE, FLOOR, ROUND, CEIL, RAND, COUNT, MIN, MAX, SUM, AVG)) {
            Token funcName = previous();
            consume(LEFT_PAREN, "Expected ( after function name.");
            List<Expression> arguments = new ArrayList<>();
            if (!check(RIGHT_PAREN)) {
                arguments.add(expression());
                while (match(COMMA)) {
                    arguments.add(expression());
                }
            }

            consume(RIGHT_PAREN, "Expected ) after function arguments.");
            return new Expression.FunctionCall(funcName, arguments);
        }

        if (match(TRUE, FALSE, NULL, NUMBER, STRING, IDENTIFIER, STAR)) {
            if (previous().type == IDENTIFIER)
                return new Expression.Literal(previous().literal, true);
            return new Expression.Literal(previous().literal, false);
        }

        if (match(LEFT_PAREN)) {
            Expression expression = expression();
            consume(RIGHT_PAREN, "Expected ) after expression.");
            return new Expression.Grouping(expression);
        }

        throw error(peek(), "Expected expression.");

    }

    // private Expression funcCallExpression() {
    // if (match(UCASE, FLOOR, ROUND, RAND, COUNT, MIN, MAX, SUM, AVG)) {
    // Token funcName = previous();
    // consume(LEFT_PAREN, "Expected ( after function name.");
    // List<Expression> arguments = new ArrayList<>();
    // if (!check(RIGHT_PAREN)) {
    // arguments.add(expression());
    // while (match(COMMA)) {
    // arguments.add(expression());
    // }
    // }
    // consume(RIGHT_PAREN, "Expected ) after function arguments.");
    // return new Expression.FunctionCall(funcName, arguments);
    // }

    // throw error(peek(), "Expected expression.");
    // }

    // // <TERM>::= <FACTOR> (( "-" | "+" ) <FACTOR>)*
    // private Expression term() {
    // Expression left = factor();
    // while (match(MINUS, PLUS)) {
    // Token operator = previous();
    // Expression right = factor();
    // left = new Expression.Binary(left, operator, right);
    // }
    // return left;
    // }

    // // <FACTOR>::= <OPERAND> (( "/" | "*" ) <OPERAND>)*
    // private Expression factor() {
    // Expression left = operand();
    // while (match(SLASH, STAR)) {
    // Token operator = previous();
    // Expression right = operand();
    // left = new Expression.Binary(left, operator, right);
    // }
    // return left;
    // }

    // // <OPERAND>::= <NUMBER> | <STRING> | TRUE | FALSE | NULL | NOT NULL |
    // // IDENTIFIER | "(" <EXPRESSION> ")"
    // private Expression operand() {
    // if (match(NUMBER, STRING, TRUE, FALSE, NULL, NOT)) {
    // if (!match(NULL)) {
    // if (prevwhereClause = WHERE expressionious().type == NOT) {
    // consume(NULL, "Expected NULL after NOT.");
    // }
    // }
    // return new Expression.Literal(previous().literal, false);
    // }
    // if (match(IDENTIFIER)) {
    // return new Expression.Literal(previous().lexeme, true);
    // }

    // if (match(LEFT_PAREN)) {
    // Expression expression = expression();
    // consume(RIGHT_PAREN, "Expected ) after expression.");
    // return new Expression.Grouping(expression);
    // }

    // throw error(peek(), "Expected operand.");
    // }

    // orderbyClause = ORDER BY expression orderType (, expression orderType)*
    // orderType = ASC | DESC
    private List<String> orderBy() {
        List<String> columns = new ArrayList<>();
        columns.add(expression().toString());
        if (orderType()) {
            columns.add(previous().lexeme);
        }

        while (match(COMMA)) {
            columns.add(expression().toString());
            if (orderType()) {
                columns.add(previous().lexeme);
            }
        }

        return columns;
    }

    // <ORDER_TYPE>::= ASC | DESC
    private boolean orderType() {
        if (match(ASC, DESC)) {
            return true;
        }
        return false;
    }

    // limitClause = LIMIT addExpression
    private int limitClause() {
        return (int) (double) consume(NUMBER, "Expected number.").literal;
    }

    /**************************************************************************/
    /******************************* INSERT ***********************************/
    /**************************************************************************/
    // insertStmnt = INSERT INTO tableName
    // ('(' columnName (',' columnName)* ')') query
    private Clause insertStmnt() {
        consume(INTO, "Expected keyword INTO after INSERT.");
        Token table_name = consume(IDENTIFIER, "Expected table name.");
        consume(LEFT_PAREN, "Expected ( after table name.");

        List<String> columns = new ArrayList<>();
        List<Expression> values = new ArrayList<>();

        columns.add(consume(IDENTIFIER, "Expected column name.").lexeme);
        while (match(COMMA)) {
            columns.add(consume(IDENTIFIER, "Expected column name.").lexeme);
        }

        consume(RIGHT_PAREN, "Expected ) after column names.");

        // query
        // TODO: for now we are just going to use the values clause not the subquery
        // Object query = query();

        consume(VALUES, "Expected keyword VALUES after column names.");
        consume(LEFT_PAREN, "Expected ( before values.");

        values.add(expression());

        while (match(COMMA)) {
            values.add(expression());
        }

        consume(RIGHT_PAREN, "Expected ) after values.");

        // check that columns and values have the same length
        if (columns.size() != values.size()) {
            throw error(peek(), "Expected same number of columns and values.");
        }

        // transform into a hashmap
        // HashMap<String, Object> valuesMap = new HashMap<>();
        // for (int i = 0; i < columns.size(); i++) {
        // valuesMap.put(columns.get(i), query);
        // }
        HashMap<String, Expression> valuesMap = new HashMap<>(); // <column_name, value>
        for (int i = 0; i < columns.size(); i++) {
            valuesMap.put(columns.get(i), values.get(i));
        }

        return new Clause.InsertClause(table_name, valuesMap);

    }

    // query = (valuesClause | selectStmnt)
    // valuesClause = VALUES '(' expression (',' expression)* ')'
    private Object query() {
        if (match(VALUES)) {
            consume(LEFT_PAREN, "Expected ( after VALUES.");
            List<Expression> values = new ArrayList<>();
            values.add(expression());
            while (match(COMMA)) {
                values.add(expression());
            }
            consume(RIGHT_PAREN, "Expected ) after values.");
            return values;
        }

        return selectStmnt();
    }

    // <VALUE>::= <DIGIT> | <STRING> | TRUE | FALSE | NULL
    private Token value() {
        if (match(NUMBER, STRING, TRUE, FALSE, NULL)) {
            return previous();
        }

        ErrorHandler.error(peek(), "Expected value.");
        return null;
    }

    /**************************************************************************/
    /**************************** UPDATE **********************************/
    /**************************************************************************/
    // updateStmnt = UPDATE tableName setClause whereClause
    private Clause updateStmnt() {
        Token table_name = consume(IDENTIFIER, "Expected table name.");
        HashMap<String, Expression> setClause = setClause();

        consume(WHERE, "Expected WHERE after SET.");
        Expression where_expression = whereClause();

        return new Clause.UpdateClause(table_name, setClause, where_expression);
    }

    // setClause = SET columnName '=' expression
    // (',' columnName '=' expression)*
    private HashMap<String, Expression> setClause() {
        HashMap<String, Expression> valuesMap = new HashMap<>();
        consume(SET, "Expected keyword SET after UPDATE.");
        String column_name = consume(IDENTIFIER, "Expected column name.").lexeme;
        consume(EQUAL, "Expected = after column name.");
        valuesMap.put(column_name, expression());

        while (match(COMMA)) {
            column_name = consume(IDENTIFIER, "Expected column name.").lexeme;
            consume(EQUAL, "Expected = after column name.");
            valuesMap.put(column_name, expression());
        }
        return valuesMap;

    }

    /**************************************************************************/
    /**************************** DELETE **********************************/
    /**************************************************************************/
    // deleteStmnt = DELETE FROM tableName (whereClause)?
    private Clause deleteStmnt() {
        consume(FROM, "Expected keyword FROM after DELETE.");
        Token table_name = consume(IDENTIFIER, "Expected table name at FROM.");

        Expression where_expression = null;
        if (match(WHERE)) {
            where_expression = whereClause();
        }

        return new Clause.DeleteClause(table_name, where_expression);
    }

    /**************************************************************************/
    /**************************** SHOW TABLES *********************************/
    /**************************************************************************/
    // showTableStmnt = SHOW TABLES
    private Clause showStmnt() {
        consume(TABLES, "Expected keyword TABLES after SHOW.");
        return new Clause.ShowClause();
    }

    /**
     * ************************************************************
     * Down here are the functions that are utilities
     * ************************************************************
     */
    /**
     * This function just consumes the next token if it is of the expected type
     * 
     * @param type
     * @param message
     * @return
     */
    private Token consume(TokenType type, String message) {
        if (check(type))
            return advance();

        throw error(peek(), message);
    }

    /**
     * This function is used to report an error and return a ParseError exception
     * 
     * @param token
     * @param message
     * @return
     */
    private ParseError error(Token token, String message) {
        ErrorHandler.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON)
                return;

            switch (peek().type) {
                case CREATE:
                case DROP:
                case SELECT:
                case INSERT:
                case UPDATE:
                case DELETE:
                case SHOW:
                    return;
            }

            advance();
        }
    }

    /**
     * This function is used to synchronize the parser after an error
     * with this i mean that it will iterate tokens until it finds a statement
     * 
     * @param types
     * @return boolean
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
