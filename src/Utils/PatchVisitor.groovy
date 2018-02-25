package Utils

import groovy.json.JsonSlurper
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.SourceUnit

/**
 * Created by jackjia on 6/29/16.
 */
public class PatchVisitor extends ClassCodeVisitorSupport {
    private List<String> validMethods
    public PatchVisitor (List<String> list){
        validMethods = list
    }
    @Override
    public void visitMethod(MethodNode methodNode){
        Map methodCalls = new HashMap()
        if(methodNode.getName()!= 'run' && methodNode.getName()!= 'main'){
            if(methodNode.getCode().getClass() == BlockStatement){
                BlockStatement block = methodNode.getCode()
                block.getStatements().each{
                    stmt->
                        //Collecting function calls, so the
                        if(stmt.getClass()==ExpressionStatement){
                            if(stmt.asType(ExpressionStatement).getExpression().getClass() == MethodCallExpression){
                                MethodCallExpression exp = stmt.asType(ExpressionStatement).getExpression()
                                /* adding additional variables to log the runtime call stack */
                                def methodName = exp.getMethodAsString()?.toLowerCase()
                                if(methodName in validMethods){
                                    def annotation = stmt.getStatementLabel()
                                    def id = new JsonSlurper().parseText(annotation).get('id')
                                    methodCalls.put(id,methodName)
                                }
                            }
                        }
                }
                methodCalls.each {
                    key,value->
                        List existingStatements = methodNode.getCode().getStatements()
                        existingStatements.add(Builder.buildAssignment2State(value+'_predecessor',methodNode.getName()))
                        //println(key+' '+value)
                }
            }
        }
        super.visitMethod(methodNode)
    }
    @Override
    public void visitBlockStatement(BlockStatement blockStatement) {
        if(blockStatement.getLineNumber()>0){
            //Patching logic for storing the calling function for each method
            //println new JsonSlurper().parseText(annotation)
            List<Statement> existingStatements = blockStatement.getStatements()
            ListIterator<Statement> iter = existingStatements.listIterator()
            while(iter.hasNext()){
                Statement stmt = iter.next()
                def tag = Helper.getLabelField(stmt.getStatementLabel(),'tag')
                def id = Helper.getLabelField(stmt.getStatementLabel(),'id')
                def label = stmt.getStatementLabel()
                if(tag=='sink'){
                    iter.remove()
                    //iter.add(Builder.buildDeclaringExpression('local_'+id,label))
                    //iter.add(Builder.buildHttpPost("http://141.212.110.244:80/stbackend/service.php","local_"+id))
                }
            }
        }
        super.visitBlockStatement(blockStatement)
    }
    @Override
    public void visitIfElse(IfStatement ifStatement){
        println('debug:'+ifStatement.getLineNumber())
        if(ifStatement.getLineNumber()>0){
            if(ifStatement.getIfBlock().getClass()==BlockStatement) {
                List<Statement> existingStatements = ifStatement.getIfBlock().getStatements()
                ListIterator<Statement> iter = existingStatements.listIterator()
                while (iter.hasNext()) {
                    Statement stmt = iter.next()
                    def tag = Helper.getLabelField(stmt.getStatementLabel(),'tag')
                    def id = Helper.getLabelField(stmt.getStatementLabel(),'id')
                    def label = stmt.getStatementLabel()
                    if (tag == 'sink') {
                        iter.remove()
                        //iter.add(Builder.buildDeclaringExpression('local_'+id,label))
                        //iter.add(Builder.buildHttpPost("http://141.212.110.244:80/stbackend/service.php", "local_"+id))
                    }
                }
            }
            if(ifStatement.getElseBlock().getClass()==BlockStatement){
                List<Statement> existingStatements = ifStatement.getElseBlock().getStatements()
                ListIterator<Statement> iter = existingStatements.listIterator()
                while (iter.hasNext()) {
                    Statement stmt = iter.next()
                    def tag = Helper.getLabelField(stmt.getStatementLabel(),'tag')
                    def id = Helper.getLabelField(stmt.getStatementLabel(),'id')
                    def label = stmt.getStatementLabel()
                    if (tag == 'sink') {
                        iter.remove()
                        //iter.add(Builder.buildDeclaringExpression('local_'+id,label))
                        //iter.add(Builder.buildHttpPost("http://141.212.110.244:80/stbackend/service.php", "loacl_"+id))
                    }
                }
            }
        }
        super.visitIfElse(ifStatement)
    }
    @Override
    protected SourceUnit getSourceUnit() {
        return null;
    }
}
