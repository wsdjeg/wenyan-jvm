package cn.wenyan.compiler.script.libs;

public enum Syntax {

    COMMENT("comment"),
    VAR_DEFINE("var_define"),
    FOR_NUMBER("for_number"),
    FOR_EACH("for_each"),
    FOR_END("for_end"),
    IF("if"),
    IF_END("if_end"),
    IF_BREAK("if_break"),
    WHILE_TRUE("while_true"),
    ELSE("else"),
    BREAK("break"),
    RETURN("return"),
    FUNCTION_END("function_end"),
    DEFINE_FUNCTION("define_function"),
    IMPORT("import"),
    IMPORT_STATIC("import_static"),
    MATH_ADD("math_+"),
    MATH_LESS("math_-"),
    MATH_MULTI("math_*"),
    MATH_EXCEPT("math_/"),
    MATH_REMAIN("math_%"),
    BIGGER(">"),
    SMALLER("<"),
    NOT_BIG_THAN("<="),
    NOT_SMALL_THAN(">="),
    TRUE("true"),
    FALSE("false"),
    EQUAL("=="),
    OR("||"),
    AND("&&"),
    PRINT("print"),
    NEGATE("!"),
    NEGATE_EQUAL("!="),
    CHANGE("change"),
    REPLACE_ARRAY("replace_array"),
    FUNCTION_ARGS_SPLIT("function_args_split"),
    IMPORT_STATIC_SEPARATE("import_static_separate"),//是否把import static给分开，如果不分开%{method}%会组合全部的'方悟xxx之义'
    IMPORT_SPLIT("import_split"),//如果开启了不分开，则这是分隔符
    STRING("string"),
    ARRAY_ADD("array_add"),
    INNER_FUNCTION("inner_function"),
    INNER_FUNCTION_NO_ARGS("inner_function_no_args"),
    SLICE("slice"),
    SIZE("size"),
    RUN_FUNCTION("run_function"),
    OBJECT_INNER("object_inner"),
    NUMBER_SUGAR("number_sugar"),//取余数时，对于数字的隐性强转
    STRING_APPEND("string_append"),
    ARRAY_GET("array_get"),

    ARRAY_TYPE("array_type"),
    INT_TYPE("int_type"),
    STRING_TYPE("string_type"),
    BOOL_TYPE("bool_type"),
    DOUBLE_TYPE("double_type"),
    VAR_TYPE("var_type"),
    OBJECT_TYPE("object_type"),

    FUNCTION_ARG_DEFINE("function_arg_define"),

    NULL("null"),

    DEFINE_ARRAY("define_array"),

    DEFINE_INT("define_int"),

    DEFINE_STRING("define_string"),

    IMPORT_WITH("import_with"),

    NOT("not"),

    REQUIRE_SCRIPT("require script"),

    CONTINUE("continue"),

    ELSE_IF("else_if"),

    CONCAT("concat"),
    TRY("try"),
    THROW("throw"),
    CATCH("catch"),
    EXCEPTION_IF("exception_if"),
    CATCH_END("catch_end"),
    SHELL_VAR("shell_var"),
    DEFINE_OBJECT("define_object"),
    DELETE("delete"),
    DEFINE("define"),
    GIVE_FUNCTION("give_function"),
    DEFINE_GIVE_FUNCTION("def_give");



    private String type;

    Syntax(String type){
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
