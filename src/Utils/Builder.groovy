package Utils

import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types

import java.lang.reflect.Type

/**
 * Created by jackjia on 6/26/16.
 */
public class Builder {
    public static ASTNode buildMethodCall(){
        List<ASTNode> nodes = new AstBuilder().buildFromSpec {
            expression{
                methodCall{
                    variable "advice"
                    constant "before"
                    argumentList {
                        constant 'aa':String.class
                    }
                }
            }
        }
        nodes.first()
    }
    public static ASTNode buildStatement(String stat){
        String s = stat
        List<ASTNode> nodes = new AstBuilder().buildFromString(stat)
        nodes.first()
    }
    public static ASTNode buildDeclaringExpression(String left,String right){
        def assignment = new DeclarationExpression(
                new VariableExpression(left),
                Token.newSymbol(Types.EQUAL,0,0),
                new ConstantExpression(right)
        )
        ExpressionStatement exp = new ExpressionStatement(assignment)
        exp
    }
    public static ASTNode buildAssignment2State(String left, String right){
        def leftExpression = new PropertyExpression(
                new VariableExpression('state'),
                new ConstantExpression(left)
        )
        def assignment = new BinaryExpression(
                leftExpression,
                Token.newSymbol(Types.EQUAL,0,0),
                new ConstantExpression(right)
        )
        ExpressionStatement exp = new ExpressionStatement(assignment)
        exp
    }

    public static ASTNode add2contextList(String left, String right){
        def leftExpression = new PropertyExpression(
                new VariableExpression('state'),
                new ConstantExpression(left)
        )
        def rightExpression = new PropertyExpression(
                new VariableExpression('state'),
                new ConstantExpression(right)
        )
        def assignment = new BinaryExpression(
                leftExpression,
                Token.newSymbol(Types.LEFT_SHIFT,0,0),
                rightExpression
        )
        ExpressionStatement exp = new ExpressionStatement(assignment)
        exp
    }

    public static ASTNode add2controlList(String left, String right){
        def leftExpression = new PropertyExpression(
                new VariableExpression('state'),
                new ConstantExpression(left)
        )
        def assignment = new BinaryExpression(
                leftExpression,
                Token.newSymbol(Types.LEFT_SHIFT,0,0),
                new ConstantExpression(right)
        )
        ExpressionStatement exp = new ExpressionStatement(assignment)
        exp
    }

    public static ASTNode add2variableList(String left, String right){
        def rightExpression = new VariableExpression(right)
        def map_var = new PropertyExpression(
                new VariableExpression('state'),
                new ConstantExpression('variableList')
        )
        def map_field = new ConstantExpression(left)
        def leftExpression = new BinaryExpression(
                map_var,
                Token.newSymbol(30,0,0),
                map_field
        )
        def assignment = new BinaryExpression(
                leftExpression,
                Token.newSymbol(Types.EQUAL,0,0),
                rightExpression
        )
        ExpressionStatement exp = new ExpressionStatement(assignment)
        exp
    }

    public static ASTNode buildSendRequestStatement() {
        def postCall = new MethodCallExpression(
                new VariableExpression('this'),
                new ConstantExpression('sendRequest'),
                new ArgumentListExpression(
                        new ConstantExpression(''),
                        new VariableExpression('state.history')
                )
        )
        ExpressionStatement exp = new ExpressionStatement(postCall)
        exp
    }

    public static ASTNode buildAssignmentList2State(String left, List right){
        def leftExpression = new PropertyExpression(
                new VariableExpression('state'),
                new ConstantExpression(left)
        )
        def assignment = new BinaryExpression(
                leftExpression,
                Token.newSymbol(Types.EQUAL,0,0),
                new ListExpression(right)
        )
        ExpressionStatement exp = new ExpressionStatement(assignment)
        exp
    }

    public static ASTNode buildAssignmentMap2State(String left, Map right){
        def leftExpression = new PropertyExpression(
                new VariableExpression('state'),
                new ConstantExpression(left)
        )
        def assignment = new BinaryExpression(
                leftExpression,
                Token.newSymbol(Types.EQUAL,0,0),
                new ListExpression(right)
        )
        ExpressionStatement exp = new ExpressionStatement(assignment)
        exp
    }

    public static ASTNode buildHttpPost(String url, String variable){
        def postCall = new MethodCallExpression(
                new VariableExpression('this'),
                new ConstantExpression('httpPost'),
                new ArgumentListExpression(
                        new ConstantExpression(url),
                        new VariableExpression(variable)
                )
        )
        ExpressionStatement exp = new ExpressionStatement(postCall)
        exp
    }
    public static ASTNode buildCallbackMappings(String methodName){
        def mapExp = new MapExpression(
                [
                        new MapEntryExpression(
                                new ConstantExpression('PUT'),
                                new ConstantExpression('onPermissionResponse')
                        )
                ]
        )
        def innerCall = new MethodCallExpression(
                new VariableExpression('this'),
                new ConstantExpression('path'),
                new ArgumentListExpression(
                        new ClosureExpression(
                                Parameter.EMPTY_ARRAY,
                                new BlockStatement(
                                        [
                                                new ExpressionStatement(mapExp)
                                        ],
                                        new VariableScope()
                                )
                        )
                )
        )
        def mappingsCall = new MethodCallExpression(
                new VariableExpression('this'),
                new ConstantExpression('mappings'),
                new ArgumentListExpression(
                        new ClosureExpression(
                                Parameter.EMPTY_ARRAY,   //will raise a strange NPE error
                                new BlockStatement(
                                        [
                                                new ExpressionStatement(innerCall)
                                        ],
                                        new VariableScope()
                                )
                        )
                )
        )
        ExpressionStatement exp = new ExpressionStatement(mappingsCall)
        exp
    }
}