/*   ______  ____   ____  ____    ____  _____
 * .' ____ \|_  _| |_  _||_   \  /   _||_   _|
 * | (___ \_| \ \   / /    |   \/   |    | |
 *  _.____`.   \ \ / /     | |\  /| |    | |   _
 * | \____) |   \ ' /     _| |_\/_| |_  _| |__/ |
 *  \______.'    \_/     |_____||_____||________|
 *
 *         Stack Virtual Machine Language
 *     Copyright (C) 2024 Christian Alexander
 */
package kakkoiichris.svml.lang.parser

import kakkoiichris.svml.lang.Directory
import kakkoiichris.svml.lang.Source
import kakkoiichris.svml.lang.lexer.Context
import kakkoiichris.svml.lang.lexer.Lexer
import kakkoiichris.svml.lang.lexer.Token
import kakkoiichris.svml.lang.lexer.TokenType
import kakkoiichris.svml.linker.Linker
import kakkoiichris.svml.util.svmlError
import java.util.*

class Parser(lexer: Lexer, private val optimize: Boolean) {
    private val lexers = Stack<Lexer>()

    private lateinit var token: Token

    init {
        lexers.push(lexer)
    }

    fun parse(): Node.Program {
        Linker

        return program()
    }

    private fun here() = token.context

    private fun source() = here().source

    private fun matchAny(vararg types: TokenType) =
        types.any { it == token.type }

    private fun match(type: TokenType) =
        type == token.type

    private inline fun <reified T : TokenType> match() =
        T::class.isInstance(token.type)

    private fun step() {
        if (lexers.isEmpty()) return

        if (lexers.peek().hasNext()) {
            token = lexers.peek().next()
        }
    }

    private fun skip(type: TokenType) =
        if (match(type)) {
            step()

            true
        }
        else false

    private fun mustSkip(type: TokenType) {
        if (!skip(type)) {
            svmlError("Expected a '$type', but encountered a '${token.type}'", source(), here())
        }
    }

    private inline fun <reified T : TokenType> get(): T? {
        if (match<T>()) {
            val token = token

            step()

            return token.type as T
        }

        return null
    }

    private fun program(): Node.Program {
        //importFile(Node.Name(Context.none(), TokenType.Name("common")))

        step()

        val location = here()
        val source = source()

        val statements = mutableListOf<Node>()

        while (lexers.isNotEmpty()) {
            while (!match(TokenType.End)) {
                if (match(TokenType.Keyword.IMPORT)) {
                    import()

                    continue
                }

                if (match(TokenType.Keyword.ALIAS)) {
                    typeAlias()

                    continue
                }

                statements += when {
                    matchAny(TokenType.Keyword.LET, TokenType.Keyword.VAR) -> declare()

                    match(TokenType.Keyword.FUNCTION)                      -> function()

                    else                                                   -> svmlError(
                        "Only imports and declarations allowed at the file level",
                        source(),
                        here()
                    )
                }
            }

            lexers.pop()

            step()
        }

        return Node.Program(location, statements)
    }

    private fun import() {
        mustSkip(TokenType.Keyword.IMPORT)

        val name = name()

        importFile(name)

        mustSkip(TokenType.Symbol.SEMICOLON)
    }

    private fun importFile(name: Node.Name) {
        val file = if (Linker.hasFile(name.name.value))
            Linker.getFile(name.name.value)
        else
            Directory.getFile(name.name.value)

        if (!file.exists()) {
            svmlError("Cannot import file '${name.name.value}'", source(), name.context)
        }

        val source = Source.of(file)

        val lexer = Lexer(source)

        lexers.push(lexer)
    }

    private fun typeAlias() {
        mustSkip(TokenType.Keyword.ALIAS)

        val name = name()

        mustSkip(TokenType.Symbol.EQUAL)

        val type = type()

        mustSkip(TokenType.Symbol.SEMICOLON)

        DataType.addAlias(name, type, source())
    }

    private fun statement() = when {
        matchAny(TokenType.Keyword.LET, TokenType.Keyword.VAR) -> declare()

        match(TokenType.Keyword.IF)                            -> `if`()

        match(TokenType.Keyword.WHILE)                         -> `while`()

        match(TokenType.Keyword.DO)                            -> `do`()

        match(TokenType.Keyword.FOR)                           -> `for`()

        match(TokenType.Keyword.BREAK)                         -> `break`()

        match(TokenType.Keyword.CONTINUE)                      -> `continue`()

        match(TokenType.Keyword.FUNCTION)                      -> function()

        match(TokenType.Keyword.RETURN)                        -> `return`()

        else                                                   -> expression()
    }

    private fun type(): Type {
        val context = here()
        val source = source()

        val baseType = when {
            skip(TokenType.Keyword.VOID)  -> DataType.Primitive.VOID

            skip(TokenType.Keyword.BOOL)  -> DataType.Primitive.BOOL

            skip(TokenType.Keyword.INT)   -> DataType.Primitive.INT

            skip(TokenType.Keyword.FLOAT) -> DataType.Primitive.FLOAT

            skip(TokenType.Keyword.CHAR)  -> DataType.Primitive.CHAR

            match<TokenType.Name>()       -> {
                val name = name()

                if (DataType.hasAlias(name)) {
                    DataType.Alias(name)
                }
                else {
                    DataType.User(name)
                }
            }

            else                          -> svmlError(
                "Invalid base type beginning with '${token.type}'",
                source,
                token.context
            )
        }

        var type: DataType = baseType

        while (skip(TokenType.Symbol.LEFT_SQUARE)) {
            val sizeNode = if (!match(TokenType.Symbol.RIGHT_SQUARE)) value() else null

            mustSkip(TokenType.Symbol.RIGHT_SQUARE)

            if (sizeNode != null && sizeNode.getDataType(source) != DataType.Primitive.INT) {
                svmlError("Array size must be an int", source, sizeNode.context)
            }

            val size = sizeNode?.value?.value?.toInt() ?: -1

            type = DataType.Array(type, size)
        }

        return Type(context, TokenType.Type(type))
    }

    private fun declare(): Node {
        val context = here()
        val source = source()

        val constant = skip(TokenType.Keyword.LET)

        if (!constant) {
            mustSkip(TokenType.Keyword.VAR)
        }

        val mutable = skip(TokenType.Keyword.MUT)

        val name = name()

        var type = if (skip(TokenType.Symbol.COLON)) type() else null

        val node = if (skip(TokenType.Symbol.EQUAL)) expr() else null

        if (type == null && node == null) {
            svmlError("Variable declaration must either be explicitly typed or assigned to", source, context)
        }

        if (type == null && node != null) {
            type = Type(Context.none(), TokenType.Type(node.getDataType(source())!!))
        }

        type!!

        if (mutable && !DataType.isArray(type.type.value, type.context.source)) {
            System.err.println("Mutable modifier is redundant!")
        }

        if (node != null && !DataType.isEquivalent(type.type.value, node.getDataType(source)!!, source)) {
            svmlError(
                "Cannot declare a variable of type '${type.type.value}' with value of type '${
                    node.getDataType(
                        source
                    )
                }'", source, node.context
            )
        }

        mustSkip(TokenType.Symbol.SEMICOLON)

        if (DataType.isArray(type.type.value, source)) {
            return Node.DeclareArray(context, node)
        }

        return Node.DeclareSingle(context, node)
    }

    private fun `if`(): Node.If {
        val location = here()

        val branches = mutableListOf<Node.If.Branch>()

        while (true) {
            val branchLocation = here()

            val condition = if (skip(TokenType.Keyword.IF))
                expr()
            else
                null

            val body = mutableListOf<Node>()

            mustSkip(TokenType.Symbol.LEFT_BRACE)

            while (!match(TokenType.Symbol.RIGHT_BRACE)) {
                body += statement()
            }

            mustSkip(TokenType.Symbol.RIGHT_BRACE)

            branches += Node.If.Branch(branchLocation, condition, body)

            if (!skip(TokenType.Keyword.ELSE)) break
        }

        return Node.If(location, branches)
    }

    private fun `while`(): Node.While {
        val location = here()

        mustSkip(TokenType.Keyword.WHILE)

        val condition = expr()

        val label = label()

        val body = mutableListOf<Node>()

        mustSkip(TokenType.Symbol.LEFT_BRACE)

        while (!match(TokenType.Symbol.RIGHT_BRACE)) {
            body += statement()
        }

        mustSkip(TokenType.Symbol.RIGHT_BRACE)

        return Node.While(location, condition, label, body)
    }

    private fun `do`(): Node.Do {
        val location = here()

        mustSkip(TokenType.Keyword.DO)

        val label = label()

        val body = mutableListOf<Node>()

        mustSkip(TokenType.Symbol.LEFT_BRACE)

        while (!match(TokenType.Symbol.RIGHT_BRACE)) {
            body += statement()
        }

        mustSkip(TokenType.Symbol.RIGHT_BRACE)
        mustSkip(TokenType.Keyword.WHILE)

        val condition = expr()

        mustSkip(TokenType.Symbol.SEMICOLON)

        return Node.Do(location, label, body, condition)
    }

    private fun `for`(): Node.For {
        val location = here()
        val source = source()

        mustSkip(TokenType.Keyword.FOR)

        var init: Node.DeclareSingle? = null

        if (!skip(TokenType.Symbol.SEMICOLON)) {
            val name = name()

            var type = if (skip(TokenType.Symbol.COLON)) type() else null

            mustSkip(TokenType.Symbol.EQUAL)

            val node = expr()

            init = Node.DeclareSingle(name.context,  node)

            mustSkip(TokenType.Symbol.SEMICOLON)
        }

        var condition: Node? = null

        if (!skip(TokenType.Symbol.SEMICOLON)) {
            condition = expr()

            mustSkip(TokenType.Symbol.SEMICOLON)
        }

        var increment: Node? = null

        if (!matchAny(TokenType.Symbol.AT, TokenType.Symbol.LEFT_BRACE)) {
            increment = expr()
        }

        val label = label()

        val body = mutableListOf<Node>()

        mustSkip(TokenType.Symbol.LEFT_BRACE)

        while (!match(TokenType.Symbol.RIGHT_BRACE)) {
            body += statement()
        }

        mustSkip(TokenType.Symbol.RIGHT_BRACE)

        return Node.For(location, init, condition, increment, label, body)
    }

    private fun `break`(): Node.Break {
        val location = here()

        mustSkip(TokenType.Keyword.BREAK)

        val label = label()

        mustSkip(TokenType.Symbol.SEMICOLON)

        return Node.Break(location, label)
    }

    private fun `continue`(): Node.Continue {
        val location = here()

        mustSkip(TokenType.Keyword.CONTINUE)

        val label = label()

        mustSkip(TokenType.Symbol.SEMICOLON)

        return Node.Continue(location, label)
    }

    private fun function(): Node.Function {
        val context = here()
        val source = source()

        mustSkip(TokenType.Keyword.FUNCTION)

        val name = name()

        val params = mutableListOf<Node.Variable>()

        if (skip(TokenType.Symbol.LEFT_PAREN) && !skip(TokenType.Symbol.RIGHT_PAREN)) {
            do {
                val paramName = name()

                mustSkip(TokenType.Symbol.COLON)

                val paramType = type()
            }
            while (skip(TokenType.Symbol.COMMA))

            mustSkip(TokenType.Symbol.RIGHT_PAREN)
        }

        var type: Type? = null

        if (skip(TokenType.Symbol.COLON)) type = type()

        val body = mutableListOf<Node>()

        var isNative = false

        when {
            skip(TokenType.Symbol.SEMICOLON) -> {
                isNative = true

                if (type == null) {
                    type = Type(Context.none(), TokenType.Type(DataType.Primitive.VOID))
                }
            }

            skip(TokenType.Symbol.EQUAL)     -> {
                val expr = expr()

                type = Type(expr.context, TokenType.Type(expr.getDataType(source)!!))

                body += Node.Return(here(), expr)

                mustSkip(TokenType.Symbol.SEMICOLON)
            }

            else                             -> {
                mustSkip(TokenType.Symbol.LEFT_BRACE)

                if (type == null) {
                    type = Type(Context.none(), TokenType.Type(DataType.Primitive.VOID))
                }

                while (!skip(TokenType.Symbol.RIGHT_BRACE)) {
                    body += statement()
                }
            }
        }

        if (!isNative) {
            if (type.type.value == DataType.Primitive.VOID && body.last() !is Node.Return) {
                body += Node.Return(
                    Context.none(),
                    Node.Value(Context.none(), TokenType.Value(0.0, DataType.Primitive.VOID))
                )
            }

            resolveBranches(context, body)

            checkUnreachable(body)

            val primaryReturn = getPrimaryReturn(body)

            val returnType = primaryReturn.getDataType(source)

            if (!DataType.isEquivalent(returnType, type.type.value, source)) {
                svmlError("Function must return value of type '${type.type.value}'", source, primaryReturn.context)
            }

            resolveBranchReturns(returnType, body)
        }

        return Node.Function(context, name, params, type.type.value, isNative, body)
    }

    private fun resolveBranches(parentContext: Context, nodes: Nodes) {
        when (val last = nodes.lastOrNull()) {
            is Node.Return -> return

            is Node.If     -> {
                if (last.branches.last().condition != null) {
                    svmlError("Final if statement must return a value from else branch", source(), last.context)
                }

                for (branch in last.branches) {
                    resolveBranches(branch.context, branch.body)
                }
            }

            is Node.While  -> resolveBranches(last.context, last.body)

            is Node.Do     -> resolveBranches(last.context, last.body)

            is Node.For    -> resolveBranches(last.context, last.body)

            null           -> svmlError("Function does not return a value", source(), parentContext)

            else           -> svmlError("Function does not return a value", source(), last.context)
        }
    }

    private fun checkUnreachable(nodes: Nodes) {
        for ((i, node) in nodes.withIndex()) {
            if (!node.isOrHasReturns) continue

            if (i == nodes.lastIndex) continue

            if (node !is Node.If) continue

            if (!node.branches.all { it.body.lastOrNull()?.isOrHasReturns == true }) continue

            if (node.branches.last().condition != null) continue

            svmlError("Unreachable code", source(), nodes[i + 1].context)
        }
    }

    private fun getPrimaryReturn(nodes: Nodes): Node.Return =
        when (val last = nodes.last()) {
            is Node.Return -> last

            is Node.If     -> getPrimaryReturn(last.branches.first().body)

            is Node.While  -> getPrimaryReturn(last.body)

            is Node.Do     -> getPrimaryReturn(last.body)

            is Node.For    -> getPrimaryReturn(last.body)

            else           -> svmlError("Function does not have a primary return", source(), last.context)
        }

    private fun resolveBranchReturns(dataType: DataType, nodes: Nodes) {
        for (node in nodes) {
            if (node is Node.Return && node.getDataType(source()) != dataType) {
                svmlError("All paths must return the same type", source(), node.context)
            }

            return resolveBranchReturns(dataType, node.subNodes)
        }
    }

    private fun `return`(): Node.Return {
        val location = here()

        mustSkip(TokenType.Keyword.RETURN)

        var node: Node? = null

        if (!skip(TokenType.Symbol.SEMICOLON)) {
            node = expr()

            mustSkip(TokenType.Symbol.SEMICOLON)
        }

        return Node.Return(location, node)
    }

    private fun expression(): Node.Expression {
        val location = here()

        val expr = expr()

        mustSkip(TokenType.Symbol.SEMICOLON)

        return Node.Expression(location, expr)
    }

    private fun expr() =
        assign()

    private fun assign(): Node {
        var expr = or()

        if (matchAny(
                TokenType.Symbol.EQUAL,
                TokenType.Symbol.PLUS_EQUAL,
                TokenType.Symbol.DASH_EQUAL,
                TokenType.Symbol.STAR_EQUAL,
                TokenType.Symbol.SLASH_EQUAL,
                TokenType.Symbol.PERCENT_EQUAL,
                TokenType.Symbol.DOUBLE_AMPERSAND_EQUAL,
                TokenType.Symbol.DOUBLE_PIPE_EQUAL
            )
        ) {
            val (context, symbol) = token

            mustSkip(symbol)

            expr = when (symbol) {
                TokenType.Symbol.EQUAL -> when (expr) {
                    is Node.Variable -> assign(context, expr)

                    is Node.GetIndex -> assignIndex(context, expr)

                    else             -> error("Invalid assign.")
                }

                else                   -> when (expr) {
                    is Node.Variable -> desugarAssign(context, expr, symbol as TokenType.Symbol)

                    is Node.GetIndex -> desugarAssignIndex(context, expr, symbol as TokenType.Symbol)

                    else             -> error("Invalid assign desugar.")
                }
            }
        }

        return expr
    }

    private fun assign(context: Context, expr: Node.Variable): Node.Assign {
        val node = or()

        return Node.Assign(context, expr, node)
    }

    private fun assignIndex(context: Context, expr: Node.GetIndex): Node.SetIndex {
        val node = or()

        return Node.SetIndex(context, expr.variable, expr.indices, node)
    }

    private fun desugarAssign(context: Context, expr: Node.Variable, symbol: TokenType.Symbol): Node.Assign {
        val source = source()

        val node = or()

        val desugaredOperator = symbol.desugared!!

        var operator = Node.Binary.Operator[desugaredOperator]

        if (expr.dataType == DataType.Primitive.INT && node.getDataType(source) == DataType.Primitive.INT) {
            operator = operator.intVersion
        }

        val intermediate = Node.Binary(context, operator, expr, node)

        return Node.Assign(context, expr, intermediate)
    }

    private fun desugarAssignIndex(context: Context, expr: Node.GetIndex, symbol: TokenType.Symbol): Node.SetIndex {
        val source = source()

        val node = or()

        val desugaredOperator = symbol.desugared!!

        var operator = Node.Binary.Operator[desugaredOperator]

        if (expr.getDataType(source) == DataType.Primitive.INT && node.getDataType(source) == DataType.Primitive.INT) {
            operator = operator.intVersion
        }

        val intermediate = Node.Binary(context, operator, expr, node)

        return Node.SetIndex(context, expr.variable, expr.indices, intermediate)
    }

    private fun or(): Node {
        val source = source()

        var expr = and()

        while (match(TokenType.Symbol.DOUBLE_PIPE)) {
            val (location, operator) = token

            mustSkip(operator)

            expr = Node.Logical(location, Node.Logical.Operator.OR, expr, and())

            expr.getDataType(source)
        }

        return expr
    }

    private fun and(): Node {
        val source = source()

        var expr = equality()

        while (match(TokenType.Symbol.DOUBLE_AMPERSAND)) {
            val (location, operator) = token

            mustSkip(operator)

            expr = Node.Logical(location, Node.Logical.Operator.AND, expr, equality())

            expr.getDataType(source)
        }

        return expr
    }

    private fun equality(): Node {
        val source = source()

        var expr = relation()

        while (matchAny(TokenType.Symbol.DOUBLE_EQUAL, TokenType.Symbol.EXCLAMATION_EQUAL)) {
            val (location, operator) = token

            mustSkip(operator)

            expr = Node.Binary(location, Node.Binary.Operator[operator], expr, relation())

            expr.getDataType(source)
        }

        return expr
    }

    private fun relation(): Node {
        val source = source()

        var expr = additive()

        while (matchAny(
                TokenType.Symbol.LESS,
                TokenType.Symbol.LESS_EQUAL,
                TokenType.Symbol.GREATER,
                TokenType.Symbol.GREATER_EQUAL
            )
        ) {
            val (location, operator) = token

            mustSkip(operator)

            expr = Node.Binary(location, Node.Binary.Operator[operator], expr, additive())

            expr.getDataType(source)
        }

        return expr
    }

    private fun additive(): Node {
        val source = source()

        var expr = multiplicative()

        while (matchAny(TokenType.Symbol.PLUS, TokenType.Symbol.DASH)) {
            val (location, operator) = token

            mustSkip(operator)

            expr = Node.Binary(location, Node.Binary.Operator[operator], expr, multiplicative())

            expr.getDataType(source)
        }

        return expr
    }

    private fun multiplicative(): Node {
        val source = source()

        var expr = unary()

        while (matchAny(TokenType.Symbol.STAR, TokenType.Symbol.SLASH, TokenType.Symbol.PERCENT)) {
            val (context, symbol) = token

            mustSkip(symbol)

            var operator = Node.Binary.Operator[symbol]

            val right = unary()

            if (expr.getDataType(source) == DataType.Primitive.INT && right.getDataType(source) == DataType.Primitive.INT) {
                operator = operator.intVersion
            }

            expr = Node.Binary(context, operator, expr, right)

            expr.getDataType(source)
        }

        return expr
    }

    private fun unary(): Node {
        val expr = if (matchAny(TokenType.Symbol.DASH, TokenType.Symbol.EXCLAMATION, TokenType.Symbol.POUND)) {
            val (context, operator) = token

            mustSkip(operator)

            if (operator == TokenType.Symbol.POUND) {
                size(context)
            }
            else {
                Node.Unary(context, Node.Unary.Operator[operator], unary())
            }
        }
        else {
            postfix()
        }

        return expr
    }

    private fun size(context: Context): Node {
        val variable = name()

        if (skip(TokenType.Symbol.LEFT_SQUARE)) {
            val indices = mutableListOf<Node>()

            do {
                indices += expr()

                mustSkip(TokenType.Symbol.RIGHT_SQUARE)
            }
            while (skip(TokenType.Symbol.LEFT_SQUARE))

            return Node.IndexSize(context, variable, indices)
        }

        return Node.Size(context, variable)
    }

    private fun postfix(): Node {
        if (match<TokenType.Name>()) {
            val name = name()

            return when {
                match(TokenType.Symbol.LEFT_PAREN)  -> invoke(name)

                match(TokenType.Symbol.LEFT_SQUARE) -> getIndex(name)

                else                                -> name
            }
        }

        return terminal()
    }

    private fun invoke(name: Node.Name): Node {
        val context = here()
        val source = source()

        val args = mutableListOf<Node>()

        mustSkip(TokenType.Symbol.LEFT_PAREN)

        if (!skip(TokenType.Symbol.RIGHT_PAREN)) {
            do {
                args += expr()
            }
            while (skip(TokenType.Symbol.COMMA))

            mustSkip(TokenType.Symbol.RIGHT_PAREN)
        }

        return Node.Invoke(context, name, args)
    }

    private fun getIndex(target: Node.Variable): Node.GetIndex {
        val context = here()
        val source = source()

        val indices = mutableListOf<Node>()

        while (skip(TokenType.Symbol.LEFT_SQUARE)) {
            indices += expr()

            mustSkip(TokenType.Symbol.RIGHT_SQUARE)
        }

        val node = Node.GetIndex(context, target, indices)

        return node
    }

    private fun terminal() = when {
        match<TokenType.Value>()           -> value()

        match<TokenType.String>()          -> string()

        match(TokenType.Symbol.LEFT_PAREN) -> nested()

        match(TokenType.Symbol.LEFT_BRACE) -> array()

        match(TokenType.Keyword.IF)        -> conditional()

        else                               -> svmlError("Not a terminal '${token.type}'", source(), here())
    }

    private fun value(): Node.Value {
        val location = here()

        val value = get<TokenType.Value>()!!

        return Node.Value(location, value)
    }

    private fun string(): Node.String {
        val location = here()

        val value = get<TokenType.String>()!!

        return Node.String(location, value.value)
    }

    private fun name(): Node.Name {
        val location = here()

        val name = get<TokenType.Name>()!!

        return Node.Name(location, name)
    }

    private fun label() =
        if (skip(TokenType.Symbol.AT)) name() else null

    private fun nested(): Node {
        mustSkip(TokenType.Symbol.LEFT_PAREN)

        val node = expr()

        mustSkip(TokenType.Symbol.RIGHT_PAREN)

        return node
    }

    private fun array(): Node.Array {
        val location = here()

        mustSkip(TokenType.Symbol.LEFT_BRACE)

        val elements = mutableListOf<Node>()

        if (!skip(TokenType.Symbol.RIGHT_BRACE)) {
            do {
                elements += expr()
            }
            while (skip(TokenType.Symbol.COMMA))

            mustSkip(TokenType.Symbol.RIGHT_BRACE)
        }

        return Node.Array(location, elements)
    }

    private fun conditional() =
        `if`().toExpr()
}