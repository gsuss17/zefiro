%%

%class Lexer
%public
%type Token
%unicode
%line
%column

%{
  private Token token(String tipo) {
    return new Token(tipo, yytext(), yyline + 1, yycolumn + 1);
  }

  // Contador para saber si hubo fallos léxicos
  public int erroresLexicos = 0;
%}

/* Expresiones Regulares Base */
LetraMin      = [a-z]
LetraMay      = [A-Z]
Digito        = [0-9]
Id            = {LetraMin}({LetraMin}|{LetraMay}|{Digito}|_)*
Entero        = {Digito}+
Decimal       = {Digito}+\.{Digito}+
Cadena        = \"([^\"\\\n\r]|(\\.))*\"
Espacio       = [ \t\r\n\f]+
Comentario    = \/\/[^\r\n]*

%%

<YYINITIAL> {
    /* Palabras Reservadas */
    "INICIO"          { return token("PR_INICIO"); }
    "FIN"             { return token("PR_FIN"); }
    "ENTERO"          { return token("PR_TIPO_ENT"); }
    "DECIMAL"         { return token("PR_TIPO_DEC"); }
    "CADENA"          { return token("PR_TIPO_CAD"); }
    "BOOLEANO"        { return token("PR_TIPO_BOOL"); }
    "VERDADERO"       { return token("LIT_VERDADERO"); }
    "FALSO"           { return token("LIT_FALSO"); }
    "SI"              { return token("PR_SI"); }
    "SINO"            { return token("PR_SINO"); }
    "FINSI"           { return token("PR_FINSI"); }
    "MIENTRAS"        { return token("PR_MIENTRAS"); }
    "FINMIENTRAS"     { return token("PR_FIN_MIE"); }
    "LEER"            { return token("PR_LEER"); }
    "IMPRIMIR"        { return token("PR_IMPRIMIR"); }

    /* Operadores Aritméticos */
    "+"               { return token("OP_SUMA"); }
    "-"               { return token("OP_RESTA"); }
    "*"               { return token("OP_MULT"); }
    "/"               { return token("OP_DIV"); }

    /* Operador de Asignación */
    "="               { return token("OP_ASIGNA"); }

    /* Operadores Relacionales */
    "=="              { return token("OP_IGUAL"); }
    "!="              { return token("OP_DIFERENTE"); }
    "<="              { return token("OP_MENOR_IGUAL"); }
    ">="              { return token("OP_MAYOR_IGUAL"); }
    "<"               { return token("OP_MENOR"); }
    ">"               { return token("OP_MAYOR"); }

    /* Operadores Lógicos */
    "Y"               { return token("OP_Y"); }
    "O"               { return token("OP_O"); }
    "NO"              { return token("OP_NO"); }

    /* Delimitadores */
    ";"               { return token("DEL_P_COMA"); }
    ","               { return token("DEL_COMA"); }
    "("               { return token("DEL_PAR_IZQ"); }
    ")"               { return token("DEL_PAR_DER"); }

    /* Literales e Identificadores */
    {Decimal}         { return token("LIT_DECIMAL"); }
    {Entero}          { return token("LIT_ENTERO"); }
    {Cadena}          { return token("LIT_CADENA"); }
    {Id}              { return token("ID"); }

    /* Elementos ignorados */
    {Espacio}         { /* Ignorar */ }
    {Comentario}      { /* Ignorar */ }

    /* Manejo de errores léxicos */
    . { 
        erroresLexicos++;
        System.err.println("Error léxico en Línea: " + (yyline + 1) + ", Columna: " + (yycolumn + 1) + ". Carácter no válido: " + yytext());
        return token("ERROR"); 
    }
}