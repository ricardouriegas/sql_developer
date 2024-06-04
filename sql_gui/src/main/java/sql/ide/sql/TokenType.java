package sql.ide.sql;
// AS, MOD, SHOW, DIV, UCASE, FLOOR, ROUND, RAND, COUNT, DISTINCT, MIN, MAX, SUM, AVG
enum TokenType {
    
    // Data Types for CREATE and string
    NUMBER_DATA_TYPE, BOOLEAN_DATA_TYPE, DATE_DATA_TYPE, STRING_DATA_TYPE,
    
    // Data Definition (DDL)
    CREATE, DROP, USE, 
    
    // Data Manipulation (DML)
    SELECT, INSERT, UPDATE, DELETE,
    
    // Keywords
    WHERE, FROM, ORDER, BY, LIMIT, VALUES, INTO, AND, OR, NOT, NULL,
    TRUE, FALSE, PRIMARY, KEY, DATABASE, TABLE, ASC, DESC, SET, UNIQUE,
    AS, GROUP, IS, PIPE_PIPE,

    // LITERALS (a literal is a representative of a fixed value)
    // AN IDENTIFIER IS ONLY A secuenve of characters THAT REPRESENTS A NAME OF A TABLE, COLUMN, ETC.
    // An STRING is a sequence of characters between double quotes or simple quotes
    NUMBER, STRING, IDENTIFIER,
    
    // One character tokens
    LEFT_PAREN, RIGHT_PAREN, COMMA, MINUS, PLUS, SLASH, STAR, SEMICOLON, MOD, DIV,
    UCASE, LCASE, CAPITALIZE, 
    FLOOR, ROUND, RAND, 
    COUNT, DISTINCT, MIN, MAX, SUM, AVG, CEIL,
    
    // Operators
    BANG_EQUAL, BANG, EQUAL_EQUAL, EQUAL, PORCENTAJE,
    LESS_EQUAL, LESS, GREATER_EQUAL, GREATER,
    
    // Others
    SHOW, TABLES,

    // End of File
    EOF
}
