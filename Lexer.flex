import java_cup.runtime.Symbol;

%%

%class Lexer
%public
%cup
%unicode
%line
%column

%{
  private Symbol symbol(int type) {
    return new Symbol(type, yyline + 1, yycolumn + 1, yytext());
  }

  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline + 1, yycolumn + 1, value);
  }

  public int erroresLexicos = 0;
%}

/* Expresiones Regulares Base */
LetraMin          = [a-z]
LetraMay          = [A-Z]
Digito            = [0-9]
Id                = {LetraMin}({LetraMin}|{LetraMay}|{Digito}|_)*
Entero            = {Digito}+
Decimal           = {Digito}+\.{Digito}+
Cadena            = \"([^\"\\\n\r]|(\\.))*\"
Espacio           = [ \t\r\n\f]+
Comentario        = \/\/[^\r\n]*

/* Expresiones de Error Léxico Específico */
/* Se excluye ';' en CadenaSinCerrar para permitir sincronización sintáctica en la misma línea */
CadenaSinCerrar   = \"([^\"\\\r\n;]|(\\.))*
DecimalInvalido   = {Digito}+\.
IdInvalido        = {LetraMay}[a-zA-Z0-9_]*

%%

<YYINITIAL> {
    /* Palabras Reservadas */
    "INICIO"          { return symbol(sym.PR_INICIO); }
    "FIN"             { return symbol(sym.PR_FIN); }
    "ENTERO"          { return symbol(sym.PR_TIPO_ENT); }
    "DECIMAL"         { return symbol(sym.PR_TIPO_DEC); }
    "CADENA"          { return symbol(sym.PR_TIPO_CAD); }
    "BOOLEANO"        { return symbol(sym.PR_TIPO_BOOL); }
    "VERDADERO"       { return symbol(sym.LIT_VERDADERO); }
    "FALSO"           { return symbol(sym.LIT_FALSO); }
    "SI"              { return symbol(sym.PR_SI); }
    "SINO"            { return symbol(sym.PR_SINO); }
    "FINSI"           { return symbol(sym.PR_FINSI); }
    "MIENTRAS"        { return symbol(sym.PR_MIENTRAS); }
    "FINMIENTRAS"     { return symbol(sym.PR_FIN_MIE); }
    "LEER"            { return symbol(sym.PR_LEER); }
    "IMPRIMIR"        { return symbol(sym.PR_IMPRIMIR); }

    /* Operadores Lógicos */
    "Y"               { return symbol(sym.OP_Y); }
    "O"               { return symbol(sym.OP_O); }
    "NO"              { return symbol(sym.OP_NO); }

    /* Operadores Aritméticos */
    "+"               { return symbol(sym.OP_SUMA); }
    "-"               { return symbol(sym.OP_RESTA); }
    "*"               { return symbol(sym.OP_MULT); }
    "/"               { return symbol(sym.OP_DIV); }

    /* Operador de Asignación */
    "="               { return symbol(sym.OP_ASIGNA); }

    /* Operadores Relacionales */
    "=="              { return symbol(sym.OP_IGUAL); }
    "!="              { return symbol(sym.OP_DIFERENTE); }
    "<="              { return symbol(sym.OP_MENOR_IGUAL); }
    ">="              { return symbol(sym.OP_MAYOR_IGUAL); }
    "<"               { return symbol(sym.OP_MENOR); }
    ">"               { return symbol(sym.OP_MAYOR); }

    /* Delimitadores */
    ";"               { return symbol(sym.DEL_P_COMA); }
    ","               { return symbol(sym.DEL_COMA); }
    "("               { return symbol(sym.DEL_PAR_IZQ); }
    ")"               { return symbol(sym.DEL_PAR_DER); }

    /* Literales e Identificadores */
    {Decimal}         { return symbol(sym.LIT_DECIMAL, yytext()); }
    {Entero}          { return symbol(sym.LIT_ENTERO, yytext()); }
    {Cadena}          { return symbol(sym.LIT_CADENA, yytext()); }
    {Id}              { return symbol(sym.ID, yytext()); }

    /* Elementos Ignorados */
    {Espacio}         { /* Ignorar */ }
    {Comentario}      { /* Ignorar */ }

    /* Errores Léxicos Específicos */
    {DecimalInvalido} {
        erroresLexicos++;
        return symbol(sym.error, yytext());
    }

    {CadenaSinCerrar} {
        erroresLexicos++;
        return symbol(sym.error, yytext());
    }

    {IdInvalido} {
        erroresLexicos++;
        return symbol(sym.error, yytext());
    }

    /* Error Léxico General */
    . { 
        erroresLexicos++;
        return symbol(sym.error, yytext());
    }
}