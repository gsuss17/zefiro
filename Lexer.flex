import java_cup.runtime.Symbol;

/**
 * Especificación léxica para el lenguaje Zefiro (JFlex).
 * Emite tokens compatibles con el analizador sintáctico generado por CUP.
 */
%%

%class Lexer
%public
%cup
%unicode
%line
%column

%{
  public int erroresLexicos = 0;

  // Posición de apertura para reportar comentarios de bloque sin cerrar en su origen
  private int comentarioLinea = 0;
  private int comentarioColumna = 0;

  private Symbol symbol(int type) {
    return new Symbol(type, yyline + 1, yycolumn + 1, yytext());
  }

  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline + 1, yycolumn + 1, value);
  }

  private Symbol errorLexico(String lexema) {
    erroresLexicos++;
    return symbol(sym.error, lexema);
  }

  private Symbol errorLexico(int linea, int columna, String lexema) {
    erroresLexicos++;
    return new Symbol(sym.error, linea, columna, lexema);
  }
%}

%state COMENTARIO_BLOQUE

/* --- Expresiones Regulares Base --- */
LetraMin          = [a-z]
LetraMay          = [A-Z]
Digito            = [0-9]
Id                = {LetraMin}({LetraMin}|{LetraMay}|{Digito}|_)*
Entero            = {Digito}+
Decimal           = {Digito}+\.{Digito}+
Cadena            = \"([^\"\\\n\r]|(\\.))*\"
Espacio           = [ \t\r\n\f\u00A0\uFEFF]+
Comentario        = \/\/[^\r\n]*

/* --- Patrones de Error Léxico Específico --- */
DecimalInvalido   = ({Digito}+\.)|(\.{Digito}+)
CadenaSinCerrar   = \"([^\"\\\r\n]|(\\.))*\\?
IdInvalido        = {LetraMay}[a-zA-Z0-9_]*

%%

<YYINITIAL> {
    /* Palabras Reservadas (preceden a IdInvalido para reconocerse antes) */
    "INICIO"          { return symbol(sym.PR_INICIO); }
    "FIN"             { return symbol(sym.PR_FIN); }
    "ENTERO"          { return symbol(sym.PR_TIPO_ENT); }
    "DECIMAL"         { return symbol(sym.PR_TIPO_DEC); }
    "CADENA"          { return symbol(sym.PR_TIPO_CAD); }
    "BOOLEANO"        { return symbol(sym.PR_TIPO_BOOL); }
    "VACIO"           { return symbol(sym.PR_VACIO); }
    "VERDADERO"       { return symbol(sym.LIT_VERDADERO); }
    "FALSO"           { return symbol(sym.LIT_FALSO); }
    "SI"              { return symbol(sym.PR_SI); }
    "SINO"            { return symbol(sym.PR_SINO); }
    "FINSI"           { return symbol(sym.PR_FINSI); }
    "MIENTRAS"        { return symbol(sym.PR_MIENTRAS); }
    "FINMIENTRAS"     { return symbol(sym.PR_FIN_MIE); }
    "PARA"            { return symbol(sym.PR_PARA); }
    "FINPARA"         { return symbol(sym.PR_FIN_PARA); }
    "ROMPER"          { return symbol(sym.PR_ROMPER); }
    "CONTINUAR"       { return symbol(sym.PR_CONTINUAR); }
    "FUNCION"         { return symbol(sym.PR_FUNCION); }
    "FINFUNCION"      { return symbol(sym.PR_FIN_FUNCION); }
    "RETORNAR"        { return symbol(sym.PR_RETORNAR); }
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
    "%"               { return symbol(sym.OP_MOD); }

    /* Operador de Asignación */
    "="               { return symbol(sym.OP_ASIGNA); }

    /* Operadores Relacionales (compuestos antes que simples) */
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

    /* Espacios y Comentarios */
    {Espacio}         { /* Ignorar */ }
    {Comentario}      { /* Ignorar comentario de línea */ }
    "/*"              {
        comentarioLinea = yyline + 1;
        comentarioColumna = yycolumn + 1;
        yybegin(COMENTARIO_BLOQUE);
    }

    /* Recuperación de Errores Léxicos */
    {DecimalInvalido} { return errorLexico(yytext()); }
    {CadenaSinCerrar} { return errorLexico(yytext()); }
    {IdInvalido}      { return errorLexico(yytext()); }
    .                 { return errorLexico(yytext()); }
}

<COMENTARIO_BLOQUE> {
    "*/"              { yybegin(YYINITIAL); }
    [^*]+             { /* Ignorar contenido del comentario */ }
    "*"               { /* Ignorar asterisco suelto */ }
    <<EOF>>           {
        yybegin(YYINITIAL);
        return errorLexico(comentarioLinea, comentarioColumna, "/*");
    }
}
